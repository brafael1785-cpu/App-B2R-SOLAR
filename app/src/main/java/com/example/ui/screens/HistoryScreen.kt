package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BrFormatters
import com.example.model.ProposalStatus
import com.example.model.SolarProposal
import com.example.model.SystemType
import com.example.ui.components.ProposalStatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningText

@Composable
fun HistoryScreen(
    proposals: List<SolarProposal>,
    onGeneratePdf: (SolarProposal) -> Unit,
    onEditProposal: (SolarProposal) -> Unit,
    onDuplicateProposal: (SolarProposal) -> Unit,
    onUpdateStatus: (String, ProposalStatus) -> Unit,
    onDeleteProposal: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<ProposalStatus?>(null) }

    val filtered = remember(proposals, searchQuery, statusFilter) {
        val q = searchQuery.trim().lowercase()
        proposals.filter { p ->
            val matchStatus = statusFilter == null || p.status == statusFilter?.wireValue
            val matchQuery = q.isEmpty() ||
                p.clientName.lowercase().contains(q) ||
                p.proposalCode.lowercase().contains(q) ||
                p.city.lowercase().contains(q)
            matchStatus && matchQuery
        }
    }

    val totalPipeline = proposals.sumOf { it.totalPrice }
    val closedTotal = proposals.filter { it.status == ProposalStatus.FECHADA.wireValue }.sumOf { it.totalPrice }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL ORÇADO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                        Text(BrFormatters.currency(totalPipeline), style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VENDAS FECHADAS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                        Text(BrFormatters.currency(closedTotal), style = MaterialTheme.typography.titleLarge, color = Color(0xFF6EE7B7))
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por cliente, código ou cidade") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Todos (${proposals.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreen,
                        selectedLabelColor = Color.White
                    )
                )
                ProposalStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text(st.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
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
                        Icon(Icons.Filled.Description, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Nenhuma proposta encontrada", style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Seus orçamentos gerados aparecem aqui para reenviar PDF ou acompanhar fechamento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { p ->
                val statusEnum = ProposalStatus.fromWire(p.status)
                val isClosed = statusEnum == ProposalStatus.FECHADA
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.clientName, style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                                Text(
                                    "${p.proposalCode} • ${SystemType.fromWire(p.systemType).label} • Modelo ${p.templateModel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ProposalStatusPill(statusEnum)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${BrFormatters.decimal2(p.powerKwp)} kWp (${p.panelCount} placas)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                BrFormatters.currency(p.totalPrice),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onGeneratePdf(p) },
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("PDF")
                            }

                            if (isClosed) {
                                OutlinedButton(
                                    onClick = { onDuplicateProposal(p) },
                                    border = BorderStroke(1.dp, SoftBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Duplicar", color = ForestGreen)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onEditProposal(p) },
                                    border = BorderStroke(1.dp, SoftBorder),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Editar", color = ForestGreen)
                                }
                            }

                            if (!isClosed) {
                                IconButton(
                                    onClick = {
                                        val next = if (statusEnum == ProposalStatus.APROVADA) {
                                            ProposalStatus.FECHADA
                                        } else {
                                            ProposalStatus.APROVADA
                                        }
                                        onUpdateStatus(p.id, next)
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircleOutline,
                                        contentDescription = "Avançar status",
                                        tint = VibrantGreen
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteProposal(p.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Excluir proposta", tint = WarningText)
                            }
                        }
                    }
                }
            }
        }
    }
}
