package com.focuscrew.app.ui.history

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.local.entity.DistractionEntryEntity
import com.focuscrew.app.data.local.entity.SessionEntity
import com.focuscrew.app.data.repository.FocusRepository
import com.focuscrew.app.domain.streak.MilestoneBadge
import com.focuscrew.app.domain.streak.StreakCalculator
import com.focuscrew.app.domain.streak.StreakStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryTab {
    SESSIONS,
    DISTRACTIONS,
    BADGES
}

@Composable
fun HistoryScreen(
    repository: FocusRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(HistoryTab.SESSIONS) }

    val sessions by repository.getAllSessions().collectAsState(initial = emptyList())
    val distractions by repository.getAllDistractions().collectAsState(initial = emptyList())

    // Milestone 10: Calculate Streaks & Milestone Badges offline
    val streakStats = remember(sessions, distractions) {
        StreakCalculator.calculate(sessions, distractions.size)
    }

    // Calculate today's midnight timestamp
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todaySessions = sessions.filter { it.startedAt >= todayMidnight }
    val todayCompletedCount = todaySessions.count { it.endReason == "COMPLETED" }
    val todayTotalFocusMinutes = todaySessions.sumOf { it.actualDurationMs } / 60000L

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Row with Export Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Focus Activity",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Your offline study history & streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {
                    exportFocusData(context, sessions, distractions)
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "📥 Export JSON",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metric Summary Cards (Streak, Today Focus, Badges)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Current Streak",
                value = if (streakStats.currentStreakDays > 0) "🔥 ${streakStats.currentStreakDays}d" else "0d",
                subtitle = "Best: ${streakStats.bestStreakDays}d",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )

            MetricCard(
                title = "Today Focus",
                value = "${todayTotalFocusMinutes}m",
                subtitle = "$todayCompletedCount sessions",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )

            MetricCard(
                title = "Badges",
                value = "${streakStats.badges.count { it.isUnlocked }}/${streakStats.badges.size}",
                subtitle = "Achievements",
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = selectedTab == HistoryTab.SESSIONS,
                onClick = { selectedTab = HistoryTab.SESSIONS },
                label = { Text("Sessions (${sessions.size})", style = MaterialTheme.typography.labelSmall) }
            )

            FilterChip(
                selected = selectedTab == HistoryTab.DISTRACTIONS,
                onClick = { selectedTab = HistoryTab.DISTRACTIONS },
                label = { Text("Thoughts (${distractions.size})", style = MaterialTheme.typography.labelSmall) }
            )

            FilterChip(
                selected = selectedTab == HistoryTab.BADGES,
                onClick = { selectedTab = HistoryTab.BADGES },
                label = { Text("🏆 Badges (${streakStats.badges.count { it.isUnlocked }})", style = MaterialTheme.typography.labelSmall) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        when (selectedTab) {
            HistoryTab.SESSIONS -> {
                if (sessions.isEmpty()) {
                    EmptyStateCard(
                        icon = "🎯",
                        message = "No focus sessions recorded yet.\nStart a session on the Timer tab to build your streak!"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            SessionHistoryItem(
                                session = session,
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        repository.deleteSession(session.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HistoryTab.DISTRACTIONS -> {
                if (distractions.isEmpty()) {
                    EmptyStateCard(
                        icon = "💡",
                        message = "Your distraction journal is clean!\nWhen urges pop up during a session, tap 'Jot Down Thought' to park them here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(distractions, key = { it.id }) { entry ->
                            DistractionHistoryItem(
                                entry = entry,
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        repository.deleteDistraction(entry.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HistoryTab.BADGES -> {
                BadgesTabContent(streakStats = streakStats)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SessionHistoryItem(
    session: SessionEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dateText = remember(session.startedAt) { dateFormat.format(Date(session.startedAt)) }
    val durationMin = (session.actualDurationMs / 60000L).coerceAtLeast(1L)
    val plannedMin = session.plannedDurationMs / 60000L

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = session.subject,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // Status Badge
                    StatusBadge(endReason = session.endReason)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$dateText • ${session.mode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$durationMin min focused (planned ${plannedMin}m)" +
                            if (session.distractionCount > 0) " • 📝 ${session.distractionCount} thoughts" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(endReason: String?) {
    val (label, bg, text) = when (endReason) {
        "COMPLETED" -> Triple("Completed", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "EMERGENCY_STOP" -> Triple("Emergency Stop", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple("Cancelled", Color(0xFFF5F5F5), Color(0xFF616161))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = text
        )
    }
}

@Composable
fun DistractionHistoryItem(
    entry: DistractionEntryEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dateText = remember(entry.timestamp) { dateFormat.format(Date(entry.timestamp)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.thoughtText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Parked: $dateText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete thought",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(icon: String, message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 42.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun exportFocusData(
    context: Context,
    sessions: List<SessionEntity>,
    distractions: List<DistractionEntryEntity>
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val jsonBuilder = StringBuilder()
    jsonBuilder.append("{\n")
    jsonBuilder.append("  \"app\": \"FocusCrew\",\n")
    jsonBuilder.append("  \"exportDate\": \"${dateFormat.format(Date())}\",\n")
    jsonBuilder.append("  \"totalSessions\": ${sessions.size},\n")
    jsonBuilder.append("  \"totalDistractionsParked\": ${distractions.size},\n")
    jsonBuilder.append("  \"sessions\": [\n")
    sessions.forEachIndexed { index, s ->
        val safeSubject = escapeJson(s.subject)
        val safeReason = escapeJson(s.endReason ?: "UNKNOWN")
        jsonBuilder.append("    {\n")
        jsonBuilder.append("      \"id\": ${s.id},\n")
        jsonBuilder.append("      \"subject\": \"$safeSubject\",\n")
        jsonBuilder.append("      \"mode\": \"${s.mode}\",\n")
        jsonBuilder.append("      \"plannedMinutes\": ${s.plannedDurationMs / 60000L},\n")
        jsonBuilder.append("      \"actualMinutes\": ${s.actualDurationMs / 60000L},\n")
        jsonBuilder.append("      \"startedAt\": ${s.startedAt},\n")
        jsonBuilder.append("      \"endReason\": \"$safeReason\",\n")
        jsonBuilder.append("      \"distractionCount\": ${s.distractionCount}\n")
        jsonBuilder.append("    }${if (index < sessions.size - 1) "," else ""}\n")
    }
    jsonBuilder.append("  ],\n")
    jsonBuilder.append("  \"distractions\": [\n")
    distractions.forEachIndexed { index, d ->
        val safeThought = escapeJson(d.thoughtText)
        jsonBuilder.append("    {\n")
        jsonBuilder.append("      \"id\": ${d.id},\n")
        jsonBuilder.append("      \"thought\": \"$safeThought\",\n")
        jsonBuilder.append("      \"timestamp\": ${d.timestamp}\n")
        jsonBuilder.append("    }${if (index < distractions.size - 1) "," else ""}\n")
    }
    jsonBuilder.append("  ]\n")
    jsonBuilder.append("}")

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FocusCrew Data Export")
            putExtra(Intent.EXTRA_TEXT, jsonBuilder.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "Export FocusCrew Data (Offline)")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun escapeJson(str: String): String {
    return str
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\u000c", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

@Composable
fun BadgesTabContent(
    streakStats: StreakStats
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Streak Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥", fontSize = 44.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (streakStats.currentStreakDays > 0) "${streakStats.currentStreakDays}-Day Streak!" else "Start Your Focus Streak!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Best: ${streakStats.bestStreakDays} days • Total Focused: ${streakStats.totalFocusMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Discipline Milestone Badges (${streakStats.badges.count { it.isUnlocked }}/${streakStats.badges.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(streakStats.badges, key = { it.id }) { badge ->
            BadgeItemCard(badge = badge)
        }
    }
}

@Composable
fun BadgeItemCard(badge: MilestoneBadge) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) {
                Color(0xFFFFF8E1) // Soft warm gold fill
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) Color(0xFFFFD54F) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(text = badge.icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (badge.isUnlocked) Color(0xFF5D4037) else MaterialTheme.colorScheme.onSurface
                    )
                    if (badge.isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓ Unlocked",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!badge.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Progress: ${badge.progressText}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

