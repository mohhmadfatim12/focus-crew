package com.focuscrew.app.ui.taskalarm

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import com.focuscrew.app.data.local.entity.TaskAlarmEntity
import com.focuscrew.app.data.repository.TaskAlarmRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TaskFilterTab {
    ALL, TODAY, UPCOMING, OVERDUE, COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAlarmScreen(
    repository: TaskAlarmRepository,
    onStartFocusTimer: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allTasks by repository.allTasks.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(TaskFilterTab.ALL) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskAlarmEntity?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskAlarmEntity?>(null) }

    val now = System.currentTimeMillis()
    val calNow = Calendar.getInstance()
    val todayYear = calNow.get(Calendar.YEAR)
    val todayDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)

    fun isToday(timeMillis: Long): Boolean {
        val c = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        return c.get(Calendar.YEAR) == todayYear && c.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
    }

    // Counts for executive stats
    val overdueCount = allTasks.count { !it.isCompleted && it.isEnabled && it.scheduledTimeMillis < now && !isToday(it.scheduledTimeMillis) }
    val todayCount = allTasks.count { !it.isCompleted && isToday(it.scheduledTimeMillis) }
    val upcomingCount = allTasks.count { !it.isCompleted && it.scheduledTimeMillis > now && !isToday(it.scheduledTimeMillis) }
    val completedCount = allTasks.count { it.isCompleted }

    // Filtered tasks
    val filteredTasks = allTasks.filter { task ->
        val matchesCategory = selectedCategory == "All" || task.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

        val matchesTab = when (selectedTab) {
            TaskFilterTab.ALL -> true
            TaskFilterTab.TODAY -> isToday(task.scheduledTimeMillis) && !task.isCompleted
            TaskFilterTab.UPCOMING -> task.scheduledTimeMillis > now && !isToday(task.scheduledTimeMillis) && !task.isCompleted
            TaskFilterTab.OVERDUE -> task.scheduledTimeMillis < now && !isToday(task.scheduledTimeMillis) && !task.isCompleted
            TaskFilterTab.COMPLETED -> task.isCompleted
        }

        matchesCategory && matchesSearch && matchesTab
    }

    val categories = listOf("All", "Coding", "Work", "Study", "Health", "Personal", "Other")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title and Backup/Restore action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Task Alarm & Reminders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% Offline • Local SQLite • Exact Alarms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showBackupDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("💾 Backup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Executive Metrics Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Today",
                        count = todayCount,
                        badgeColor = Color(0xFF2563EB),
                        isSelected = selectedTab == TaskFilterTab.TODAY,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == TaskFilterTab.TODAY) TaskFilterTab.ALL else TaskFilterTab.TODAY
                        }
                    )
                    MetricCard(
                        title = "Upcoming",
                        count = upcomingCount,
                        badgeColor = Color(0xFF0D9488),
                        isSelected = selectedTab == TaskFilterTab.UPCOMING,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == TaskFilterTab.UPCOMING) TaskFilterTab.ALL else TaskFilterTab.UPCOMING
                        }
                    )
                    MetricCard(
                        title = "Overdue",
                        count = overdueCount,
                        badgeColor = Color(0xFFDC2626),
                        isSelected = selectedTab == TaskFilterTab.OVERDUE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == TaskFilterTab.OVERDUE) TaskFilterTab.ALL else TaskFilterTab.OVERDUE
                        }
                    )
                    MetricCard(
                        title = "Done",
                        count = completedCount,
                        badgeColor = Color(0xFF16A34A),
                        isSelected = selectedTab == TaskFilterTab.COMPLETED,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTab = if (selectedTab == TaskFilterTab.COMPLETED) TaskFilterTab.ALL else TaskFilterTab.COMPLETED
                        }
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks or routines...", fontSize = 14.sp) },
                    leadingIcon = { Text("🔍", fontSize = 16.sp) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            // Category Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.clickable { selectedCategory = category }
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Empty State
            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏰", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching tasks found" else "No tasks in this section",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to schedule your Coding or daily routine alarm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCardItem(
                        task = task,
                        now = now,
                        onToggleCompleted = { completed ->
                            scope.launch { repository.setTaskCompleted(task.id, completed) }
                        },
                        onToggleEnabled = { enabled ->
                            scope.launch { repository.setTaskEnabled(task.id, enabled) }
                        },
                        onSnooze = { minutes ->
                            scope.launch {
                                repository.snoozeTask(task.id, minutes)
                                Toast.makeText(context, "Snoozed '${task.title}' for $minutes min", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEdit = {
                            editingTask = task
                            showCreateSheet = true
                        },
                        onDelete = {
                            taskToDelete = task
                        },
                        onStartFocus = {
                            onStartFocusTimer(task.title)
                        }
                    )
                }
            }
        }

        // Floating Action Button to create task
        FloatingActionButton(
            onClick = {
                editingTask = null
                showCreateSheet = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("➕", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // Task Creation / Editing Sheet
    if (showCreateSheet) {
        CreateEditTaskBottomSheet(
            taskToEdit = editingTask,
            onDismiss = {
                showCreateSheet = false
                editingTask = null
            },
            onSave = { newTask ->
                scope.launch {
                    if (editingTask == null) {
                        repository.insertTask(newTask)
                    } else {
                        repository.updateTask(newTask)
                    }
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val timeStr = timeFormat.format(Date(newTask.scheduledTimeMillis))
                    val calNow = Calendar.getInstance()
                    val targetCal = Calendar.getInstance().apply { timeInMillis = newTask.scheduledTimeMillis }
                    val dayStr = if (targetCal.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)) "Today" else "Tomorrow"
                    Toast.makeText(context, "✅ Alarm saved for $dayStr at $timeStr! Will ring on time.", Toast.LENGTH_LONG).show()
                    showCreateSheet = false
                    editingTask = null
                }
            }
        )
    }

    // Delete Confirmation Dialog (Triggered on hold/long press or delete button)
    if (taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Text("Delete Alarm", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure to delete this alarm?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⏰ " + task.title,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (task.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteTask(task.id)
                            Toast.makeText(context, "🗑️ Deleted '${task.title}'", Toast.LENGTH_SHORT).show()
                        }
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        BackupRestoreDialog(
            repository = repository,
            onDismiss = { showBackupDialog = false }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    badgeColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) badgeColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) badgeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCardItem(
    task: TaskAlarmEntity,
    now: Long,
    onToggleCompleted: (Boolean) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSnooze: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartFocus: () -> Unit = {}
) {
    val isOverdue = !task.isCompleted && task.isEnabled && task.scheduledTimeMillis < now
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val dateObj = Date(task.scheduledTimeMillis)
    val timeText = timeFormat.format(dateObj)
    val dateText = dateFormat.format(dateObj)

    var showSnoozeMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            } else if (isOverdue) {
                Color(0xFFFEF2F2)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isOverdue) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Done Checkbox
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompleted(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF16A34A),
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Enable/Disable switch
                Switch(
                    checked = task.isEnabled && !task.isCompleted,
                    onCheckedChange = { onToggleEnabled(it) },
                    enabled = !task.isCompleted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time, Recurrence & Category badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Time Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOverdue) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "⏰ $timeText • $dateText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) Color(0xFFB91C1C) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Recurrence Badge
                    if (task.recurrence != "NONE") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ) {
                            val recLabel = when (task.recurrence) {
                                "DAILY" -> "🔄 Daily"
                                "WEEKLY" -> "📅 Weekly"
                                "WEEKDAYS" -> "💼 Mon-Fri"
                                "CUSTOM_WEEKDAYS" -> "🗓️ Custom"
                                else -> task.recurrence
                            }
                            Text(
                                text = recLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Action buttons: Focus, Snooze, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!task.isCompleted) {
                        TextButton(
                            onClick = onStartFocus,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("🎯 Focus", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                    }

                    if (!task.isCompleted && task.isEnabled) {
                        Box {
                            TextButton(
                                onClick = { showSnoozeMenu = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("💤 Snooze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = showSnoozeMenu,
                                onDismissRequest = { showSnoozeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("5 minutes") },
                                    onClick = { onSnooze(5); showSnoozeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("10 minutes") },
                                    onClick = { onSnooze(10); showSnoozeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("15 minutes") },
                                    onClick = { onSnooze(15); showSnoozeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("30 minutes") },
                                    onClick = { onSnooze(30); showSnoozeMenu = false }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("✏️", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("🗑️", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditTaskBottomSheet(
    taskToEdit: TaskAlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskAlarmEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val calendar = remember {
        Calendar.getInstance().apply {
            if (taskToEdit != null) {
                timeInMillis = taskToEdit.scheduledTimeMillis
            } else {
                // Default to next hour
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var recurrence by remember { mutableStateOf(taskToEdit?.recurrence ?: "NONE") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "Coding") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "HIGH") }
    var isExactAlarm by remember { mutableStateOf(taskToEdit?.isExactAlarm ?: true) }
    var customWeekdays by remember { mutableStateOf(taskToEdit?.customWeekdays ?: "2,3,4,5,6") }

    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
    val canScheduleExact = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    // Dynamic calculations for countdown & time preview
    val previewTargetCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, selectedHour)
        set(Calendar.MINUTE, selectedMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val diffMillis = previewTargetCal.timeInMillis - System.currentTimeMillis()
    val diffMinutes = (diffMillis / (1000 * 60)).coerceAtLeast(1)
    val diffHours = diffMinutes / 60
    val remMinutes = diffMinutes % 60
    val countdownText = if (diffHours > 0) {
        "rings in ${diffHours}h ${remMinutes}m"
    } else {
        "rings in ${remMinutes}m"
    }
    val isTodayAlarm = previewTargetCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val dayPrefix = if (isTodayAlarm) "Today" else "Tomorrow"
    val formattedPreviewTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(previewTargetCal.time)

    val performSave: () -> Unit = {
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val effectiveTitle = if (title.isBlank()) {
            "Alarm at $formattedPreviewTime"
        } else {
            title.trim()
        }

        val entity = taskToEdit?.copy(
            title = effectiveTitle,
            description = description.trim(),
            scheduledTimeMillis = targetCal.timeInMillis,
            recurrence = recurrence,
            customWeekdays = customWeekdays,
            category = category,
            priority = priority,
            isExactAlarm = isExactAlarm,
            updatedAtMillis = System.currentTimeMillis()
        ) ?: TaskAlarmEntity(
            title = effectiveTitle,
            description = description.trim(),
            scheduledTimeMillis = targetCal.timeInMillis,
            recurrence = recurrence,
            customWeekdays = customWeekdays,
            category = category,
            priority = priority,
            isExactAlarm = isExactAlarm
        )

        onSave(entity)
    }

    val quickPresets = listOf(
        "Coding" to (22 to 0),     // 10:00 PM
        "Review Notes" to (20 to 0), // 8:00 PM
        "Deep Work" to (9 to 0),    // 9:00 AM
        "Exercise" to (7 to 0)      // 7:00 AM
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & TOP SAVE BUTTON (Immediately visible when popup opens!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (taskToEdit == null) "Schedule Task Alarm" else "Edit Task Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "🔔 Exact-Time Ring Guarantee",
                        fontSize = 12.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 💾 1. TOP SAVE BUTTON - IMMEDIATELY VISIBLE ON POPUP OPEN
                Button(
                    onClick = performSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("💾 Save", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }

            // Warning if exact alarm permission not granted on Android 12+
            if (!canScheduleExact) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarm Permission Needed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Tap here to allow alarms to ring precisely on time even during device sleep.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // Quick suggestion presets chips
            Text("Quick Presets:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPresets.forEach { (presetTitle, time) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable {
                            title = presetTitle
                            selectedHour = time.first
                            selectedMinute = time.second
                            category = if (presetTitle == "Coding") "Coding" else "Work"
                        }
                    ) {
                        Text(
                            text = "$presetTitle (${if (time.first > 12) "${time.first - 12}:00 PM" else "${time.first}:00 AM"})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                placeholder = { Text("e.g. Coding (or leave blank for Alarm at $formattedPreviewTime)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("e.g. Finish feature implementation") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Time Selector (Hour & Minute incrementers) + DEDICATED SAVE BUTTON INSIDE TIME CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Alarm Time:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) { Text("▲", fontSize = 16.sp) }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedHour),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }) { Text("▼", fontSize = 16.sp) }
                            Text("Hour (24h)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(":", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        // Minute Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) { Text("▲", fontSize = 16.sp) }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { selectedMinute = if (selectedMinute < 5) 55 else selectedMinute - 5 }) { Text("▼", fontSize = 16.sp) }
                            Text("Minute", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Formatted 12-hour preview badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = formattedPreviewTime,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ring Status Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⏰ Will ring $dayPrefix at $formattedPreviewTime ($countdownText)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 💾 2. DEDICATED SAVE BUTTON RIGHT INSIDE THE ALARM TIME PICKER CARD
                    Button(
                        onClick = performSave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "💾 Save Alarm for $formattedPreviewTime",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Recurrence Row
            Text("Recurrence Rule:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val recurrenceOptions = listOf(
                "NONE" to "Once",
                "DAILY" to "Daily",
                "WEEKLY" to "Weekly",
                "WEEKDAYS" to "Mon-Fri"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recurrenceOptions.forEach { (key, label) ->
                    val isSelected = recurrence == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { recurrence = key }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Exact Alarm Switch & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Exact-Time Ring", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Rings precisely at the exact minute", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isExactAlarm,
                    onCheckedChange = { isExactAlarm = it }
                )
            }

            // 💾 3. CONFIRM & SAVE ALARM BUTTON (Bottom Primary Action, Always Enabled)
            Button(
                onClick = performSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (taskToEdit == null) "💾 Confirm & Save Alarm ($formattedPreviewTime)" else "💾 Save Changes ($formattedPreviewTime)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BackupRestoreDialog(
    repository: TaskAlarmRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var jsonText by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup & Restore Tasks (JSON)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("All tasks are stored in your local SQLite database. You can export them to JSON text or paste JSON to restore.", fontSize = 13.sp)

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("Paste JSON backup here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val exported = repository.exportToJson()
                                jsonText = exported
                                clipboardManager.setText(AnnotatedString(exported))
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export JSON")
                    }

                    Button(
                        onClick = {
                            if (jsonText.isBlank()) {
                                Toast.makeText(context, "Please paste JSON backup first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                try {
                                    val count = repository.importFromJson(jsonText)
                                    Toast.makeText(context, "Successfully restored $count tasks!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid JSON: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
