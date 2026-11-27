package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.B2RRepository
import com.example.model.AgendaAppointment
import com.example.model.BrFormatters
import com.example.model.BrazilSolarData
import com.example.model.CompanySettings
import com.example.model.ConnectionType
import com.example.model.EquipmentKind
import com.example.model.InverterSelection
import com.example.model.ProposalMetrics
import com.example.model.ProposalStatus
import com.example.model.SolarCalculator
import com.example.model.SolarClient
import com.example.model.SolarEquipment
import com.example.model.SolarProposal
import com.example.model.SystemType
import com.example.pdf.SolarPdfGenerator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppSection {
    ORCAMENTO,
    CLIENTES,
    HISTORICO,
    AGENDA,
    CONFIGURACOES,
    EQUIPAMENTOS
}

data class BudgetDraftState(
    val editingProposalId: String? = null,
    val editingProposalCode: String = "",
    val systemType: SystemType = SystemType.ON_GRID,
    val selectedClientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val stateUf: String = "GO",
    val city: String = "",
    val consumptionText: String = "",
    val consumptionInReais: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.BIFASICO,
    val panelCount: Int = 20,
    val selectedPanelName: String = BrazilSolarData.defaultPanels.first().name,
    val selectedPanelWatts: Int = BrazilSolarData.defaultPanels.first().powerWatts,
    val inverters: List<InverterSelection> = listOf(
        InverterSelection(BrazilSolarData.defaultInverters[3].name, 1)
    ),
    val selectedBattery: String = BrazilSolarData.defaultBatteries.first().name,
    val totalPriceText: String = "33899,00",
    val cashEnabled: Boolean = true,
    val cardEnabled: Boolean = false,
    val financingEnabled: Boolean = false,
    val boletoEnabled: Boolean = false,
    val cardInstallments: Int = 12,
    val cardFeeText: String = "15,29",
    val templateModel: Int = 1
)

data class GeneratedPdfPreview(
    val proposal: SolarProposal,
    val file: File,
    val bitmaps: List<Bitmap>
)

class B2RViewModel(
    private val repository: B2RRepository,
    private val currentUserId: String
) : ViewModel() {

    val companySettings: StateFlow<UiState<CompanySettings>> = repository
        .observeCompanySettings(currentUserId)
        .asUiState("configurações")

    val clients: StateFlow<UiState<List<SolarClient>>> = repository
        .observeClients(currentUserId)
        .asUiState("clientes")

    val proposals: StateFlow<UiState<List<SolarProposal>>> = repository
        .observeProposals(currentUserId)
        .asUiState("propostas")

    val appointments: StateFlow<UiState<List<AgendaAppointment>>> = repository
        .observeAppointments(currentUserId)
        .asUiState("agenda")

    val equipment: StateFlow<UiState<List<SolarEquipment>>> = repository
        .observeEquipment(currentUserId)
        .asUiState("equipamentos")

    private val _currentSection = MutableStateFlow(AppSection.ORCAMENTO)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    private val _draft = MutableStateFlow(BudgetDraftState())
    val draft: StateFlow<BudgetDraftState> = _draft.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _pdfPreview = MutableStateFlow<GeneratedPdfPreview?>(null)
    val pdfPreview: StateFlow<GeneratedPdfPreview?> = _pdfPreview.asStateFlow()

    fun navigateTo(section: AppSection) {
        _currentSection.value = section
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun dismissPdfPreview() {
        _pdfPreview.value = null
    }

    fun currentSettingsValue(): CompanySettings {
        return (companySettings.value as? UiState.Success)?.data ?: CompanySettings(userId = currentUserId)
    }

    fun computeDraftMetrics(draftState: BudgetDraftState = _draft.value): ProposalMetrics {
        val settings = currentSettingsValue()
        val price = BrFormatters.parseBrNumber(draftState.totalPriceText) ?: 0.0
        val cardFee = BrFormatters.parseBrNumber(draftState.cardFeeText) ?: 0.0
        return SolarCalculator.calculateMetrics(
            panelCount = draftState.panelCount,
            panelPowerW = draftState.selectedPanelWatts,
            stateUf = draftState.stateUf,
            kwhTariff = settings.kwhTariff,
            connectionType = draftState.connectionType,
            totalPrice = price,
            cardInstallments = draftState.cardInstallments,
            cardFeePercent = cardFee
        )
    }

    fun updateDraft(transform: (BudgetDraftState) -> BudgetDraftState) {
        _draft.update(transform)
    }

    fun selectClientForDraft(client: SolarClient?) {
        if (client == null) {
            _draft.update {
                it.copy(
                    selectedClientId = "",
                    clientName = "",
                    clientPhone = ""
                )
            }
        } else {
            _draft.update {
                it.copy(
                    selectedClientId = client.id,
                    clientName = client.name,
                    clientPhone = client.phone,
                    stateUf = client.state.ifBlank { it.stateUf },
                    city = client.city,
                    connectionType = ConnectionType.fromWire(client.connectionType),
                    consumptionText = if (client.avgConsumptionKwh > 0.0) {
                        client.avgConsumptionKwh.toInt().toString()
                    } else it.consumptionText,
                    consumptionInReais = false
                )
            }
        }
    }

    fun autoSizeFromConsumption() {
        val d = _draft.value
        val value = BrFormatters.parseBrNumber(d.consumptionText)
        if (value == null || value <= 0.0) {
            _feedbackMessage.value = "Informe o consumo em kWh/mês ou R$ para dimensionar."
            return
        }
        val settings = currentSettingsValue()
        val recommended = SolarCalculator.recommendPanelCount(
            consumptionValue = value,
            isCurrencyReais = d.consumptionInReais,
            kwhTariff = settings.kwhTariff,
            connectionType = d.connectionType,
            stateUf = d.stateUf,
            panelPowerW = d.selectedPanelWatts
        )
        val newKwp = (recommended * d.selectedPanelWatts) / 1000.0
        val suggested = SolarCalculator.suggestedPrice(
            powerKwp = newKwp,
            pricePerWp = settings.pricePerWp,
            isHybrid = d.systemType == SystemType.HIBRIDO
        )
        _draft.update {
            it.copy(
                panelCount = recommended,
                totalPriceText = BrFormatters.decimal2(suggested)
            )
        }
        _feedbackMessage.value = "Sistema dimensionado: $recommended placas (${BrFormatters.decimal2(newKwp)} kWp)."
    }

    fun applySuggestedPrice() {
        val d = _draft.value
        val settings = currentSettingsValue()
        val kwp = (d.panelCount * d.selectedPanelWatts) / 1000.0
        val suggested = SolarCalculator.suggestedPrice(
            powerKwp = kwp,
            pricePerWp = settings.pricePerWp,
            isHybrid = d.systemType == SystemType.HIBRIDO
        )
        _draft.update { it.copy(totalPriceText = BrFormatters.decimal2(suggested)) }
    }

    fun clearDraft() {
        val settings = currentSettingsValue()
        _draft.value = BudgetDraftState(
            stateUf = settings.defaultState.ifBlank { "GO" },
            city = "",
            templateModel = settings.defaultTemplate.toInt().coerceIn(1, 5)
        )
    }

    fun loadProposalIntoDraft(proposal: SolarProposal, duplicate: Boolean = false) {
        val parsedInverters = proposal.inverters.map { raw ->
            val match = Regex("^(\\d+)x\\s+(.+)$").find(raw.trim())
            if (match != null) {
                InverterSelection(
                    name = match.groupValues[2],
                    quantity = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                )
            } else {
                InverterSelection(raw, 1)
            }
        }.ifEmpty { listOf(InverterSelection(BrazilSolarData.defaultInverters.first().name, 1)) }

        _draft.value = BudgetDraftState(
            editingProposalId = if (duplicate) null else proposal.id,
            editingProposalCode = if (duplicate) "" else proposal.proposalCode,
            systemType = SystemType.fromWire(proposal.systemType),
            selectedClientId = proposal.clientId,
            clientName = proposal.clientName,
            clientPhone = proposal.clientPhone,
            stateUf = proposal.state,
            city = proposal.city,
            consumptionText = if (proposal.consumptionInput > 0.0) proposal.consumptionInput.toInt().toString() else "",
            connectionType = ConnectionType.fromWire(proposal.connectionType),
            panelCount = proposal.panelCount.toInt().coerceAtLeast(1),
            selectedPanelName = proposal.panelModel,
            selectedPanelWatts = proposal.panelPowerW.toInt().coerceAtLeast(50),
            inverters = parsedInverters,
            selectedBattery = proposal.batterySummary.ifBlank { BrazilSolarData.defaultBatteries.first().name },
            totalPriceText = BrFormatters.decimal2(proposal.totalPrice),
            cashEnabled = proposal.cashEnabled,
            cardEnabled = proposal.cardEnabled,
            financingEnabled = proposal.financingEnabled,
            boletoEnabled = proposal.boletoEnabled,
            cardInstallments = proposal.cardInstallments.toInt().coerceAtLeast(1),
            cardFeeText = BrFormatters.decimal2(proposal.cardFeePercent),
            templateModel = proposal.templateModel.toInt().coerceIn(1, 5)
        )
        _currentSection.value = AppSection.ORCAMENTO
    }

    fun saveAndGeneratePdf(context: Context) {
        val d = _draft.value
        val price = BrFormatters.parseBrNumber(d.totalPriceText)
        if (price == null || price < 1.0) {
            _feedbackMessage.value = "Informe um valor total válido (R$) para gerar a proposta."
            return
        }
        val metrics = computeDraftMetrics(d)
        val settings = currentSettingsValue()
        val consumption = BrFormatters.parseBrNumber(d.consumptionText) ?: 0.0
        val cardFee = BrFormatters.parseBrNumber(d.cardFeeText) ?: 0.0
        val proposalCountNow = ((proposals.value as? UiState.Success)?.data?.size ?: 0) + 1
        val code = d.editingProposalCode.ifBlank {
            "B2R-${2026}-${proposalCountNow.toString().padStart(3, '0')}"
        }
        val proposal = SolarProposal(
            id = d.editingProposalId ?: "",
            userId = currentUserId,
            proposalCode = code,
            clientId = d.selectedClientId,
            clientName = d.clientName.trim().ifBlank { "Cliente genérico" },
            clientPhone = d.clientPhone.trim(),
            state = d.stateUf,
            city = d.city.trim(),
            systemType = d.systemType.wireValue,
            connectionType = d.connectionType.wireValue,
            consumptionInput = consumption,
            panelCount = d.panelCount.toLong(),
            panelModel = d.selectedPanelName,
            panelPowerW = d.selectedPanelWatts.toLong(),
            powerKwp = metrics.powerKwp,
            inverters = d.inverters.map { "${it.quantity}x ${it.name}" },
            batterySummary = if (d.systemType == SystemType.HIBRIDO) d.selectedBattery else "",
            totalPrice = price,
            cashEnabled = true,
            cardEnabled = d.cardEnabled,
            financingEnabled = d.financingEnabled,
            boletoEnabled = d.boletoEnabled,
            cardInstallments = if (d.cardEnabled) d.cardInstallments.toLong() else 0L,
            cardFeePercent = if (d.cardEnabled) cardFee else 0.0,
            templateModel = d.templateModel.toLong(),
            estimatedMonthlyKwh = metrics.estimatedMonthlyKwh,
            estimatedMonthlySavings = metrics.estimatedMonthlySavings,
            paybackMonths = metrics.paybackMonths.toLong(),
            status = ProposalStatus.EM_ANALISE.wireValue
        )

        viewModelScope.launch {
            _isBusy.value = true
            val saveRes = repository.saveProposal(proposal)
            saveRes.onSuccess { savedId ->
                val finalProposal = proposal.copy(id = savedId)
                runCatching {
                    val file = SolarPdfGenerator.generateProposalPdf(context, finalProposal, settings)
                    val bitmaps = SolarPdfGenerator.renderPreviewBitmaps(file)
                    _pdfPreview.value = GeneratedPdfPreview(finalProposal, file, bitmaps)
                }.onFailure { err ->
                    Log.e(TAG, "Erro ao gerar PDF", err)
                    _feedbackMessage.value = "Orçamento salvo, mas houve erro ao montar o PDF: ${err.localizedMessage}"
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Erro ao salvar proposta."
            }
            _isBusy.value = false
        }
    }

    fun generatePdfForExisting(context: Context, proposal: SolarProposal) {
        val settings = currentSettingsValue()
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                val file = SolarPdfGenerator.generateProposalPdf(context, proposal, settings)
                val bitmaps = SolarPdfGenerator.renderPreviewBitmaps(file)
                _pdfPreview.value = GeneratedPdfPreview(proposal, file, bitmaps)
            }.onFailure { err ->
                Log.e(TAG, "Erro ao gerar PDF existente", err)
                _feedbackMessage.value = "Falha ao gerar PDF: ${err.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun saveClient(client: SolarClient, selectInDraft: Boolean = false) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveClient(client)
                .onSuccess { newId ->
                    _feedbackMessage.value = "Cliente salvo com sucesso."
                    if (selectInDraft) {
                        selectClientForDraft(client.copy(id = newId))
                    }
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Erro ao salvar cliente."
                }
            _isBusy.value = false
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            repository.deleteClient(clientId)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao excluir cliente." }
        }
    }

    fun updateProposalStatus(proposalId: String, status: ProposalStatus) {
        viewModelScope.launch {
            repository.updateProposalStatus(proposalId, status)
                .onSuccess { _feedbackMessage.value = "Status atualizado para ${status.label}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar status." }
        }
    }

    fun deleteProposal(proposalId: String) {
        viewModelScope.launch {
            repository.deleteProposal(proposalId)
                .onSuccess { _feedbackMessage.value = "Proposta excluída." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao excluir proposta." }
        }
    }

    fun saveAppointment(appointment: AgendaAppointment) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveAppointment(appointment)
                .onSuccess { _feedbackMessage.value = "Agendamento salvo." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao salvar agendamento." }
            _isBusy.value = false
        }
    }

    fun toggleAppointmentCompleted(appointmentId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleAppointmentCompleted(appointmentId, completed)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar agenda." }
        }
    }

    fun deleteAppointment(appointmentId: String) {
        viewModelScope.launch {
            repository.deleteAppointment(appointmentId)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao excluir agendamento." }
        }
    }

    fun saveCompanySettings(settings: CompanySettings) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveCompanySettings(settings)
                .onSuccess { _feedbackMessage.value = "Configurações da empresa salvas!" }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao salvar configurações." }
            _isBusy.value = false
        }
    }

    fun saveEquipment(equipment: SolarEquipment) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveEquipment(equipment)
                .onSuccess { _feedbackMessage.value = "Equipamento adicionado ao catálogo." }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao salvar equipamento." }
            _isBusy.value = false
        }
    }

    fun deleteEquipment(equipmentId: String) {
        viewModelScope.launch {
            repository.deleteEquipment(equipmentId)
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Erro ao remover equipamento." }
        }
    }

    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Erro ao observar $label", rootError)
                emit(UiState.Error(rootError.message ?: "Erro ao carregar $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private companion object {
        const val TAG = "B2RViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
