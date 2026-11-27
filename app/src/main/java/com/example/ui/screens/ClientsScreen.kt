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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BrFormatters
import com.example.model.BrazilSolarData
import com.example.model.ConnectionType
import com.example.model.SolarClient
import com.example.ui.components.B2RDropdownSelector
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningText

@Composable
fun ClientsScreen(
    clients: List<SolarClient>,
    defaultState: String,
    onSaveClient: (SolarClient) -> Unit,
    onDeleteClient: (String) -> Unit,
    onCreateProposalForClient: (SolarClient) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var editingClient by remember { mutableStateOf<SolarClient?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }

    val filtered = remember(clients, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) clients else clients.filter {
            it.name.lowercase().contains(q) ||
                it.city.lowercase().contains(q) ||
                it.phone.lowercase().contains(q)
        }
    }

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
                    Text("Clientes", style = MaterialTheme.typography.titleLarge, color = ForestGreen)
                    Text(
                        "${clients.size} cadastrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingClient = SolarClient(state = defaultState)
                        showFormDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_client_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Novo cliente")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome, cidade ou telefone") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filtered.isEmpty()) {
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
                        Icon(
                            Icons.Filled.PeopleOutline,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Nenhum cliente encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = ForestGreen
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Cadastre seus clientes para preencher orçamentos em segundos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { client ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    client.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ForestGreen
                                )
                                val loc = listOf(client.city, client.state).filter { it.isNotBlank() }.joinToString("/")
                                Text(
                                    listOf(client.phone, loc).filter { it.isNotBlank() }.joinToString(" • ")
                                        .ifBlank { "Sem telefone/cidade" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editingClient = client
                                    showFormDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar cliente", tint = EmeraldPrimary)
                                }
                                IconButton(onClick = { onDeleteClient(client.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir cliente", tint = WarningText)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = MintContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    ConnectionType.fromWire(client.connectionType).label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (client.avgConsumptionKwh > 0.0) {
                                Surface(color = MintContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        "${client.avgConsumptionKwh.toInt()} kWh/mês",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (client.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                client.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onCreateProposalForClient(client) },
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.SolarPower, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Criar orçamento para este cliente", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog && editingClient != null) {
        ClientFormDialog(
            initial = editingClient!!,
            onDismiss = { showFormDialog = false },
            onSave = { saved ->
                onSaveClient(saved)
                showFormDialog = false
            }
        )
    }
}

@Composable
private fun ClientFormDialog(
    initial: SolarClient,
    onDismiss: () -> Unit,
    onSave: (SolarClient) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var email by remember { mutableStateOf(initial.email) }
    var stateUf by remember { mutableStateOf(initial.state.ifBlank { "GO" }) }
    var city by remember { mutableStateOf(initial.city) }
    var conn by remember { mutableStateOf(ConnectionType.fromWire(initial.connectionType)) }
    var consumption by remember {
        mutableStateOf(if (initial.avgConsumptionKwh > 0) initial.avgConsumptionKwh.toInt().toString() else "")
    }
    var notes by remember { mutableStateOf(initial.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Novo cliente" else "Editar cliente") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome completo*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("WhatsApp / Telefone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    B2RDropdownSelector(
                        label = "UF",
                        selectedText = stateUf,
                        items = BrazilSolarData.states,
                        itemLabel = { "${it.uf} · ${it.name}" },
                        onSelect = { stateUf = it.uf },
                        modifier = Modifier.width(96.dp)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Cidade") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                B2RDropdownSelector(
                    label = "PADRÃO DE LIGAÇÃO",
                    selectedText = conn.label,
                    items = ConnectionType.entries,
                    itemLabel = { it.label },
                    onSelect = { conn = it }
                )
                OutlinedTextField(
                    value = consumption,
                    onValueChange = { consumption = it },
                    label = { Text("Consumo médio (kWh/mês)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (telhado, padrão, etc.)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            state = stateUf,
                            city = city.trim(),
                            connectionType = conn.wireValue,
                            avgConsumptionKwh = BrFormatters.parseBrNumber(consumption) ?: 0.0,
                            notes = notes.trim()
                        )
                    )
                },
                enabled = name.trim().length >= 2,
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
