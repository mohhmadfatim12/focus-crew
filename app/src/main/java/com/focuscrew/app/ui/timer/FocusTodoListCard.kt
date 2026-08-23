package com.focuscrew.app.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.domain.model.SessionTask
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.BritishRacingSageContainer
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SagePillBg
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WarmLinenHigh
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun FocusTodoListCard(
    tasks: List<SessionTask>,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTaskTitle by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WhisperBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BritishRacingSageContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📋", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus Tasks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                }

                // Completion Progress Pill
                val doneCount = tasks.count { it.isCompleted }
                val totalCount = tasks.size
                Surface(
                    color = if (doneCount > 0 && doneCount == totalCount) SagePillBg else WarmLinen,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (doneCount > 0 && doneCount == totalCount) BritishRacingSage else WhisperBorder)
                ) {
                    Text(
                        text = if (totalCount == 0) "0 Tasks" else "$doneCount/$totalCount Done",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (doneCount > 0 && doneCount == totalCount) BritishRacingSage else SoftEspressoInk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Task Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    placeholder = {
                        Text(
                            text = "Add a focus task...",
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = MutedCharcoalOlive.copy(alpha = 0.7f)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = SoftEspressoInk
                    ),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTaskTitle.isNotBlank()) {
                                onAddTask(newTaskTitle)
                                newTaskTitle = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WarmLinen,
                        unfocusedContainerColor = WarmLinen,
                        focusedBorderColor = BritishRacingSage,
                        unfocusedBorderColor = WhisperBorder,
                        focusedTextColor = SoftEspressoInk,
                        unfocusedTextColor = SoftEspressoInk
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle)
                            newTaskTitle = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("+ Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Task Items List
            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        TaskItemRow(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onRemove = { onRemoveTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemRow(
    task: SessionTask,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val targetBg = if (task.isCompleted) SagePillBg.copy(alpha = 0.6f) else WarmLinen
    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(300), label = "taskBg")

    Surface(
        color = animatedBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) BritishRacingSage.copy(alpha = 0.4f) else WhisperBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Tick Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) BritishRacingSage else WarmLinenHigh)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                color = if (task.isCompleted) MutedCharcoalOlive else SoftEspressoInk,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
            )

            // Delete Task Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete task",
                    tint = MutedCharcoalOlive.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
