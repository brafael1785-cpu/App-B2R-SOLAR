package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BrFormatters
import com.example.model.BrazilSolarData
import com.example.model.EquipmentKind
import com.example.model.SolarEquipment
import com.example.ui.components.B2RDropdownSelector
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningText

@Composable
fun EquipmentScreen(
    customEquipment: List<SolarEquipment>,
    onSaveEquipment: (SolarEquipment) -> Unit,
    onDeleteEquipment: (String) -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Equipamentos", style = MaterialTheme.typography.titleLarge, color = ForestGreen)
                    Text(
                        "Catálogo padrão + itens personalizados no orçamento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Novo")
                }
            }
        }

        if (customEquipment.isNotEmpty()) {
            item {
                Text("Seus equipamentos salvos", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
            }
            items(customEquipment, key = { it.id }) { eq ->
                val kind = EquipmentKind.fromWire(eq.kind)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(eq.name, style = MaterialTheme.typography.titleSmall, color = ForestGreen)
                            Text(
                                "${kind.label} • ${BrFormatters.decimal2(eq.powerValue)} ${kind.unitLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteEquipment(eq.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remover equipamento", tint = WarningText)
                        }
                    }
                }
            }
        }

        item {
            Text("Catálogo padrão B2R Solar", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
        }
        items(BrazilSolarData.defaultPanels) { panel ->
            CatalogReadonlyCard(
                title = panel.name,
                subtitle = "Módulo Solar • ${panel.powerWatts} Wp"
            )
        }
        items(BrazilSolarData.defaultInverters) { inv ->
            CatalogReadonlyCard(
                title = inv.name,
                subtitle = "Inversor • ${BrFormatters.decimal2(inv.powerKw)} kW"
            )
        }
        items(BrazilSolarData.defaultBatteries) { bat ->
            CatalogReadonlyCard(
                title = bat.name,
                subtitle = "Bateria • ${BrFormatters.decimal2(bat.capacityKwh)} kWh"
            )
        }
    }

    if (showAddDialog) {
        var kind by remember { mutableStateOf(EquipmentKind.MODULO) }
        var name by remember { mutableStateOf("") }
        var powerText by remember { mutableStateOf("605") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar equipamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    B2RDropdownSelector(
                        label = "TIPO",
                        selectedText = kind.label,
                        items = EquipmentKind.entries,
                        itemLabel = { it.label },
                        onSelect = {
                            kind = it
                            powerText = when (it) {
                                EquipmentKind.MODULO -> "605"
                                EquipmentKind.INVERSOR -> "8.0"
                                EquipmentKind.BATERIA -> "5.12"
                            }
                        }
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Modelo / Marca*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = powerText,
                        onValueChange = { powerText = it },
                        label = { Text("Potência / Capacidade (${kind.unitLabel})*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pv = BrFormatters.parseBrNumber(powerText) ?: 0.0
                        if (name.trim().length >= 2 && pv >= 0.1) {
                            onSaveEquipment(
                                SolarEquipment(
                                    kind = kind.wireValue,
                                    name = name.trim(),
                                    powerValue = pv
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = name.trim().length >= 2 && (BrFormatters.parseBrNumber(powerText) ?: 0.0) >= 0.1,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
                ) {
                    Text("Adicionar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CatalogReadonlyCard(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MintContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, SoftBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = ForestGreen)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
