package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryCatalogItem
import com.example.model.BrFormatters
import com.example.model.BrazilSolarData
import com.example.model.CompanySettings
import com.example.model.ConnectionType
import com.example.model.EquipmentKind
import com.example.model.InverterCatalogItem
import com.example.model.InverterSelection
import com.example.model.PROPOSAL_TEMPLATES
import com.example.model.PanelCatalogItem
import com.example.model.SolarClient
import com.example.model.SolarEquipment
import com.example.model.SolarProposal
import com.example.model.SystemType
import com.example.ui.components.B2RDropdownSelector
import com.example.ui.components.ProposalTemplateCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarGoldSoft
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningBg
import com.example.ui.theme.WarningBorder
import com.example.ui.theme.WarningText
import com.example.viewmodel.AppSection
import com.example.viewmodel.B2RViewModel
import com.example.viewmodel.BudgetDraftState

@Composable
fun ProposalBuilderScreen(
    viewModel: B2RViewModel,
    draft: BudgetDraftState,
    settings: CompanySettings,
    clients: List<SolarClient>,
    proposals: List<SolarProposal>,
    customEquipment: List<SolarEquipment>,
    isBusy: Boolean
) {
    val context = LocalContext.current
    val metrics = remember(draft, settings) { viewModel.computeDraftMetrics(draft) }
    var showQuickAddClient by remember { mutableStateOf(false) }

    val panelOptions = remember(customEquipment) {
        BrazilSolarData.defaultPanels + customEquipment
            .filter { it.kind == EquipmentKind.MODULO.wireValue }
            .map { PanelCatalogItem(it.name, it.powerValue.toInt().coerceAtLeast(50)) }
    }
    val inverterOptions = remember(customEquipment) {
        BrazilSolarData.defaultInverters + customEquipment
            .filter { it.kind == EquipmentKind.INVERSOR.wireValue }
            .map { InverterCatalogItem(it.name, it.powerValue) }
    }
    val batteryOptions = remember(customEquipment) {
        BrazilSolarData.defaultBatteries + customEquipment
            .filter { it.kind == EquipmentKind.BATERIA.wireValue }
            .map { BatteryCatalogItem(it.name, it.powerValue) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Emerald Banner (mirrors screenshot 1 dark banner, now emerald green!)
            item {
                val totalKwp = proposals.sumOf { it.powerKwp }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = SolarGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "B2R Solar • Orçamento em segundos",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${proposals.size} propostas salvas • ${BrFormatters.decimal2(totalKwp)} kWp projetados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA7F3D0)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.navigateTo(AppSection.HISTORICO) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VibrantGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Ver histórico", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(AppSection.CONFIGURACOES) },
                                border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Minha empresa", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. Company Completion Card (mirrors screenshot 1 progress card)
            item {
                val pct = settings.completionPercent()
                if (pct < 100) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MintContainer,
                        border = BorderStroke(1.dp, SoftBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.navigateTo(AppSection.CONFIGURACOES) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                color = VibrantGreen,
                                trackColor = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Sua empresa está $pct% configurada · ${settings.missingFieldsLabel()}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ForestGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completar",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary,
                                    textDecoration = TextDecoration.Underline
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. System Type Segmented Toggle (On-grid vs Sistema híbrido — screenshot 7)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SystemTypeButton(
                            selected = draft.systemType == SystemType.ON_GRID,
                            icon = Icons.Filled.WbSunny,
                            iconTint = SolarGold,
                            title = SystemType.ON_GRID.label,
                            subtitle = SystemType.ON_GRID.subtitle,
                            onClick = { viewModel.updateDraft { it.copy(systemType = SystemType.ON_GRID) } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("system_type_on_grid")
                        )
                        SystemTypeButton(
                            selected = draft.systemType == SystemType.HIBRIDO,
                            icon = Icons.Filled.BatteryChargingFull,
                            iconTint = VibrantGreen,
                            title = SystemType.HIBRIDO.label,
                            subtitle = SystemType.HIBRIDO.subtitle,
                            onClick = { viewModel.updateDraft { it.copy(systemType = SystemType.HIBRIDO) } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("system_type_hibrido")
                        )
                    }
                }
            }

            // 4. Main Form Card: Nova proposta
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            Text(
                                text = if (draft.editingProposalId != null) {
                                    "Editar proposta ${draft.editingProposalCode}"
                                } else {
                                    "Nova proposta"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = ForestGreen
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Escolha o cliente e preencha os dados do sistema. Logo, tarifa e modelos vêm das suas Configurações.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!settings.logoConfigured) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarningBg,
                                border = BorderStroke(1.dp, WarningBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.navigateTo(AppSection.CONFIGURACOES) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = WarningText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Sua proposta está saindo sem a sua logo. Envie em Configurações.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WarningText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = SoftBorder)

                        // --- SEÇÃO CLIENTE ---
                        SectionHeaderRow(icon = Icons.Filled.Person, title = "Cliente", tint = ForestGreen)

                        val clientChoices = remember(clients) {
                            listOf<SolarClient?>(null) + clients
                        }
                        val selectedClientLabel = if (draft.selectedClientId.isBlank()) {
                            "— cliente genérico —"
                        } else {
                            clients.firstOrNull { it.id == draft.selectedClientId }?.name
                                ?: draft.clientName.ifBlank { "— cliente genérico —" }
                        }

                        B2RDropdownSelector(
                            label = "CLIENTE",
                            selectedText = selectedClientLabel,
                            items = clientChoices,
                            itemLabel = { c -> c?.name ?: "— cliente genérico —" },
                            onSelect = { viewModel.selectClientForDraft(it) },
                            testTag = "client_selector"
                        )

                        if (draft.selectedClientId.isBlank()) {
                            OutlinedTextField(
                                value = draft.clientName,
                                onValueChange = { txt -> viewModel.updateDraft { it.copy(clientName = txt) } },
                                label = { Text("Nome do cliente na proposta (opcional)") },
                                placeholder = { Text("ex.: Galpão Comercial ou João Silva") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generic_client_name_input")
                            )
                        }

                        OutlinedButton(
                            onClick = { showQuickAddClient = true },
                            border = BorderStroke(1.dp, SoftBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("quick_add_client_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cadastrar cliente", color = ForestGreen, fontWeight = FontWeight.SemiBold)
                        }

                        // CIDADE DA INSTALAÇÃO
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CIDADE DA INSTALAÇÃO (opcional · irradiação exata)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                B2RDropdownSelector(
                                    label = null,
                                    selectedText = draft.stateUf,
                                    items = BrazilSolarData.states,
                                    itemLabel = { "${it.uf} · ${it.name}" },
                                    onSelect = { st -> viewModel.updateDraft { it.copy(stateUf = st.uf) } },
                                    modifier = Modifier.width(96.dp),
                                    testTag = "state_selector"
                                )
                                OutlinedTextField(
                                    value = draft.city,
                                    onValueChange = { c -> viewModel.updateDraft { it.copy(city = c) } },
                                    placeholder = { Text("Opcional: digite a cidade") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("city_input")
                                )
                            }
                            val stInfo = BrazilSolarData.findState(draft.stateUf)
                            Text(
                                text = "Em branco, o cálculo usa a irradiação padrão de ${stInfo.name} (${stInfo.defaultCity}: ${BrFormatters.decimal2(stInfo.avgHsp)} kWh/m²/dia).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = SoftBorder)

                        // --- SEÇÃO SISTEMA ---
                        SectionHeaderRow(icon = Icons.Filled.Bolt, title = "Sistema", tint = SolarGold)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "CONSUMO MÉDIO DO CLIENTE (OPCIONAL, PARA DIMENSIONAR)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = draft.consumptionText,
                                    onValueChange = { txt -> viewModel.updateDraft { it.copy(consumptionText = txt) } },
                                    placeholder = {
                                        Text(if (draft.consumptionInReais) "ex.: 950 (R$/mês)" else "ex.: 800 (kWh/mês)")
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("consumption_input")
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MintContainer,
                                    border = BorderStroke(1.dp, SoftBorder),
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.updateDraft { it.copy(consumptionInReais = !it.consumptionInReais) }
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = if (draft.consumptionInReais) "R$" else "kWh",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.autoSizeFromConsumption() },
                                border = BorderStroke(1.dp, EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("auto_size_button")
                            ) {
                                Icon(Icons.Filled.Calculate, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Dimensionar placas automaticamente", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        B2RDropdownSelector(
                            label = "TIPO DE LIGAÇÃO DO CLIENTE",
                            selectedText = draft.connectionType.label,
                            items = ConnectionType.entries,
                            itemLabel = { it.label },
                            onSelect = { conn -> viewModel.updateDraft { it.copy(connectionType = conn) } },
                            testTag = "connection_type_selector"
                        )

                        // POTÊNCIA (KWP) auto-calculated
                        Column {
                            Text(
                                text = "POTÊNCIA (KWP)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MintContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, SoftBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${BrFormatters.decimal2(metrics.powerKwp)} kWp",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "calcula sozinho pelas placas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // QTD. PLACAS with stepper
                        Column {
                            Text(
                                text = "QTD. PLACAS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            QuantityStepperField(
                                value = draft.panelCount,
                                onValueChange = { newQty ->
                                    viewModel.updateDraft { it.copy(panelCount = newQty.coerceIn(1, 10000)) }
                                },
                                testTag = "panel_count_stepper"
                            )
                        }

                        // MODELO DAS PLACAS
                        B2RDropdownSelector(
                            label = "MODELO DAS PLACAS",
                            selectedText = draft.selectedPanelName,
                            items = panelOptions,
                            itemLabel = { "${it.name} (${it.powerWatts}W)" },
                            onSelect = { panel ->
                                viewModel.updateDraft {
                                    it.copy(
                                        selectedPanelName = panel.name,
                                        selectedPanelWatts = panel.powerWatts
                                    )
                                }
                            },
                            testTag = "panel_model_selector"
                        )

                        // INVERSOR(ES)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "INVERSOR(ES)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            draft.inverters.forEachIndexed { index, sel ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        B2RDropdownSelector(
                                            label = null,
                                            selectedText = sel.name,
                                            items = inverterOptions,
                                            itemLabel = { it.name },
                                            onSelect = { inv ->
                                                viewModel.updateDraft { d ->
                                                    val updated = d.inverters.toMutableList()
                                                    updated[index] = sel.copy(name = inv.name)
                                                    d.copy(inverters = updated)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (draft.inverters.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.updateDraft { d ->
                                                        val updated = d.inverters.toMutableList()
                                                        updated.removeAt(index)
                                                        d.copy(inverters = updated)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Remover inversor",
                                                    tint = WarningText
                                                )
                                            }
                                        }
                                    }
                                    QuantityStepperField(
                                        value = sel.quantity,
                                        onValueChange = { q ->
                                            viewModel.updateDraft { d ->
                                                val updated = d.inverters.toMutableList()
                                                updated[index] = sel.copy(quantity = q.coerceIn(1, 100))
                                                d.copy(inverters = updated)
                                            }
                                        },
                                        testTag = "inverter_qty_$index"
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateDraft { d ->
                                        d.copy(
                                            inverters = d.inverters + InverterSelection(
                                                BrazilSolarData.defaultInverters.first().name,
                                                1
                                            )
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, SoftBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("add_inverter_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Adicionar outro inversor", color = ForestGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (draft.systemType == SystemType.HIBRIDO) {
                            B2RDropdownSelector(
                                label = "BANCO DE BATERIAS (SISTEMA HÍBRIDO)",
                                selectedText = draft.selectedBattery,
                                items = batteryOptions,
                                itemLabel = { it.name },
                                onSelect = { bat -> viewModel.updateDraft { it.copy(selectedBattery = bat.name) } },
                                testTag = "battery_selector"
                            )
                        }

                        // Live sizing feedback summary pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MintContainer,
                            border = BorderStroke(1.dp, SoftBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("GERAÇÃO EST.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${BrFormatters.integer(metrics.estimatedMonthlyKwh.toInt())} kWh/mês",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("ECONOMIA/MÊS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        BrFormatters.currency(metrics.estimatedMonthlySavings),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = VibrantGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PAYBACK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${metrics.paybackMonths} meses",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = SoftBorder)

                        // --- SEÇÃO VALOR E PAGAMENTO ---
                        SectionHeaderRow(icon = Icons.Filled.Savings, title = "Valor e pagamento", tint = EmeraldPrimary)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VALOR TOTAL (R$)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Sugerir por R$/Wp",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.applySuggestedPrice() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            OutlinedTextField(
                                value = draft.totalPriceText,
                                onValueChange = { v -> viewModel.updateDraft { it.copy(totalPriceText = v) } },
                                placeholder = { Text("ex.: 33.899,00") },
                                prefix = { Text("R$ ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("total_price_input")
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "APARECEM NA PROPOSTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MintContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, SoftBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = true,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("À vista ", fontWeight = FontWeight.Bold, color = ForestGreen)
                                    Text("sempre aparece", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PaymentOptionChip(
                                    label = "Cartão",
                                    checked = draft.cardEnabled,
                                    onToggle = { viewModel.updateDraft { it.copy(cardEnabled = !it.cardEnabled) } },
                                    modifier = Modifier.weight(1f).testTag("payment_card_toggle")
                                )
                                PaymentOptionChip(
                                    label = "Financiamento",
                                    checked = draft.financingEnabled,
                                    onToggle = { viewModel.updateDraft { it.copy(financingEnabled = !it.financingEnabled) } },
                                    modifier = Modifier.weight(1.3f).testTag("payment_financing_toggle")
                                )
                            }
                            PaymentOptionChip(
                                label = "Boleto",
                                checked = draft.boletoEnabled,
                                onToggle = { viewModel.updateDraft { it.copy(boletoEnabled = !it.boletoEnabled) } },
                                modifier = Modifier.testTag("payment_boleto_toggle")
                            )
                        }

                        if (draft.cardEnabled) {
                            val parcelChoices = listOf(6, 10, 12, 18, 21, 24)
                            B2RDropdownSelector(
                                label = "PARCELAS NO CARTÃO",
                                selectedText = "${draft.cardInstallments}x de ${BrFormatters.currency(metrics.cardParcelValue)}",
                                items = parcelChoices,
                                itemLabel = { "${it}x" },
                                onSelect = { p -> viewModel.updateDraft { it.copy(cardInstallments = p) } }
                            )
                            OutlinedTextField(
                                value = draft.cardFeeText,
                                onValueChange = { f -> viewModel.updateDraft { it.copy(cardFeeText = f) } },
                                label = { Text("TAXA DO CARTÃO (%)") },
                                placeholder = { Text("ex.: 15,29") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Marcou cartão? Escolha as parcelas e informe a taxa.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = SoftBorder)

                        // --- SEÇÃO MODELO DA PROPOSTA ---
                        SectionHeaderRow(icon = Icons.Filled.Description, title = "Proposta", tint = ForestGreen)
                        Text(
                            text = "MODELO DA PROPOSTA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PROPOSAL_TEMPLATES.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { tpl ->
                                        ProposalTemplateCard(
                                            template = tpl,
                                            selected = draft.templateModel == tpl.number,
                                            onSelect = { viewModel.updateDraft { it.copy(templateModel = tpl.number) } },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MintContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 O PDF é gerado diretamente no celular em alta definição na paleta verde da B2R Solar, pronto para visualizar ou compartilhar no WhatsApp.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ForestGreen,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar (Limpar + Salvar e gerar PDF — matches screenshot 2..8!)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, SoftBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearDraft() },
                    border = BorderStroke(1.dp, SoftBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("clear_draft_button")
                ) {
                    Text("Limpar", color = ForestGreen, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { viewModel.saveAndGeneratePdf(context) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("save_generate_pdf_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (draft.editingProposalId != null) {
                                "Atualizar e gerar PDF"
                            } else {
                                "Salvar e gerar PDF"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showQuickAddClient) {
        QuickAddClientDialog(
            defaultState = draft.stateUf,
            onDismiss = { showQuickAddClient = false },
            onSave = { client ->
                viewModel.saveClient(client, selectInDraft = true)
                showQuickAddClient = false
            }
        )
    }
}

@Composable
private fun SystemTypeButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ForestGreen else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) iconTint else EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) Color.White else ForestGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = if (selected) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MintContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = ForestGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuantityStepperField(
    value: Int,
    onValueChange: (Int) -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SoftBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = ForestGreen,
                modifier = Modifier.padding(start = 6.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintContainer,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onValueChange((value - 1).coerceAtLeast(1)) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Diminuir", tint = ForestGreen)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MintContainer,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onValueChange(value + 1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Aumentar", tint = ForestGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionChip(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) MintContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (checked) VibrantGreen else SoftBorder),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = VibrantGreen)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = ForestGreen,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuickAddClientDialog(
    defaultState: String,
    onDismiss: () -> Unit,
    onSave: (SolarClient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var consumption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade ($defaultState)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = consumption,
                    onValueChange = { consumption = it },
                    label = { Text("Consumo médio (kWh/mês)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length >= 2) {
                        onSave(
                            SolarClient(
                                name = name.trim(),
                                phone = phone.trim(),
                                state = defaultState,
                                city = city.trim(),
                                avgConsumptionKwh = BrFormatters.parseBrNumber(consumption) ?: 0.0
                            )
                        )
                    }
                },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
