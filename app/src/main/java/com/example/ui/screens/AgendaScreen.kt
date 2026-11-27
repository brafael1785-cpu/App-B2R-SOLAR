package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.AgendaAppointment
import com.example.model.VisitType
import com.example.ui.components.B2RDropdownSelector
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningText

@Composable
fun AgendaScreen(
    appointments: List<AgendaAppointment>,
    onSaveAppointment: (AgendaAppointment) -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit,
    onDeleteAppointment: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Agenda", style = MaterialTheme.typography.titleLarge, color = ForestGreen)
                    val pending = appointments.count { !it.completed }
                    Text(
                        "$pending compromissos pendentes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_appointment_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agendar")
                }
            }
        }

        if (appointments.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MintContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Sua agenda está livre", style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Agende visitas técnicas, apresentações de proposta e instalações.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(appointments, key = { it.id }) { apt ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (apt.completed) MintContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = apt.completed,
                            onCheckedChange = { onToggleCompleted(apt.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = VibrantGreen)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = apt.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ForestGreen,
                                textDecoration = if (apt.completed) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = listOf(apt.dateLabel, apt.clientName).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(color = MintContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    VisitType.fromWire(apt.visitType).label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (apt.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(apt.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onDeleteAppointment(apt.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir agendamento", tint = WarningText)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveAppointment(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (AgendaAppointment) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var dateLabel by remember { mutableStateOf("28/11/2026 14:00") }
    var visitType by remember { mutableStateOf(VisitType.VISITA_TECNICA) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo agendamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título*") },
                    placeholder = { Text("ex.: Visita técnica telhado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text("Data e hora*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                B2RDropdownSelector(
                    label = "TIPO DE COMPROMISSO",
                    selectedText = visitType.label,
                    items = VisitType.entries,
                    itemLabel = { it.label },
                    onSelect = { visitType = it }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        AgendaAppointment(
                            title = title.trim(),
                            clientName = clientName.trim(),
                            dateLabel = dateLabel.trim(),
                            visitType = visitType.wireValue,
                            notes = notes.trim()
                        )
                    )
                },
                enabled = title.trim().length >= 2 && dateLabel.trim().length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("Salvar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
