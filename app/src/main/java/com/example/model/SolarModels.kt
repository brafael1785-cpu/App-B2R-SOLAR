package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class SystemType(val wireValue: String, val label: String, val subtitle: String) {
    ON_GRID("ON_GRID", "On-grid", "proposta tradicional"),
    HIBRIDO("HIBRIDO", "Sistema híbrido", "com bateria");

    companion object {
        fun fromWire(raw: String): SystemType =
            entries.firstOrNull { it.wireValue == raw } ?: ON_GRID
    }
}

enum class ConnectionType(val wireValue: String, val label: String, val minKwh: Int) {
    MONOFASICO("MONOFASICO", "Monofásico (30 kWh)", 30),
    BIFASICO("BIFASICO", "Bifásico (50 kWh)", 50),
    TRIFASICO("TRIFASICO", "Trifásico (100 kWh)", 100);

    companion object {
        fun fromWire(raw: String): ConnectionType =
            entries.firstOrNull { it.wireValue == raw } ?: BIFASICO
    }
}

enum class ProposalStatus(val wireValue: String, val label: String) {
    EM_ANALISE("EM_ANALISE", "Em análise"),
    APROVADA("APROVADA", "Aprovada"),
    FECHADA("FECHADA", "Fechada"),
    RECUSADA("RECUSADA", "Recusada");

    companion object {
        fun fromWire(raw: String): ProposalStatus =
            entries.firstOrNull { it.wireValue == raw } ?: EM_ANALISE
    }
}

enum class VisitType(val wireValue: String, val label: String) {
    VISITA_TECNICA("VISITA_TECNICA", "Visita Técnica"),
    APRESENTACAO("APRESENTACAO", "Apresentação"),
    INSTALACAO("INSTALACAO", "Instalação"),
    FOLLOW_UP("FOLLOW_UP", "Follow-up");

    companion object {
        fun fromWire(raw: String): VisitType =
            entries.firstOrNull { it.wireValue == raw } ?: VISITA_TECNICA
    }
}

enum class EquipmentKind(val wireValue: String, val label: String, val unitLabel: String) {
    MODULO("MODULO", "Módulo Solar", "W"),
    INVERSOR("INVERSOR", "Inversor", "kW"),
    BATERIA("BATERIA", "Bateria", "kWh");

    companion object {
        fun fromWire(raw: String): EquipmentKind =
            entries.firstOrNull { it.wireValue == raw } ?: MODULO
    }
}

@Keep
@IgnoreExtraProperties
data class CompanySettings(
    @DocumentId val id: String = "company",
    val userId: String = "",
    val companyName: String = "B2R Solar",
    val documentNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val defaultState: String = "GO",
    val defaultCity: String = "Goiânia",
    val kwhTariff: Double = 0.95,
    val pricePerWp: Double = 2.80,
    val defaultValidityDays: Long = 15L,
    val defaultTemplate: Long = 1L,
    val logoConfigured: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun completionPercent(): Int {
        var score = 20 // base default config
        if (companyName.isNotBlank() && companyName != "B2R Solar") score += 10 else if (companyName.isNotBlank()) score += 10
        if (logoConfigured) score += 20
        if (address.isNotBlank()) score += 20
        if (phone.isNotBlank()) score += 15
        if (documentNumber.isNotBlank() || email.isNotBlank()) score += 15
        return score.coerceIn(0, 100)
    }

    fun missingFieldsLabel(): String {
        val missing = mutableListOf<String>()
        if (!logoConfigured) missing += "logo"
        if (address.isBlank()) missing += "endereço"
        if (phone.isBlank()) missing += "telefone"
        if (documentNumber.isBlank()) missing += "CNPJ"
        return when {
            missing.isEmpty() -> "perfil completo para gerar propostas!"
            missing.size == 1 -> "falta ${missing.first()}."
            else -> "faltam ${missing.dropLast(1).joinToString(", ")} e ${missing.last()}."
        }
    }
}

@Keep
@IgnoreExtraProperties
data class SolarClient(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val state: String = "GO",
    val city: String = "",
    val connectionType: String = ConnectionType.BIFASICO.wireValue,
    val avgConsumptionKwh: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SolarProposal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val proposalCode: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val state: String = "GO",
    val city: String = "",
    val systemType: String = SystemType.ON_GRID.wireValue,
    val connectionType: String = ConnectionType.BIFASICO.wireValue,
    val consumptionInput: Double = 0.0,
    val panelCount: Long = 10L,
    val panelModel: String = "",
    val panelPowerW: Long = 605L,
    val powerKwp: Double = 6.05,
    val inverters: List<String> = emptyList(),
    val batterySummary: String = "",
    val totalPrice: Double = 0.0,
    val cashEnabled: Boolean = true,
    val cardEnabled: Boolean = false,
    val financingEnabled: Boolean = false,
    val boletoEnabled: Boolean = false,
    val cardInstallments: Long = 12L,
    val cardFeePercent: Double = 15.29,
    val templateModel: Long = 1L,
    val estimatedMonthlyKwh: Double = 0.0,
    val estimatedMonthlySavings: Double = 0.0,
    val paybackMonths: Long = 0L,
    val status: String = ProposalStatus.EM_ANALISE.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AgendaAppointment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val clientName: String = "",
    val dateLabel: String = "",
    val visitType: String = VisitType.VISITA_TECNICA.wireValue,
    val notes: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SolarEquipment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = EquipmentKind.MODULO.wireValue,
    val name: String = "",
    val powerValue: Double = 605.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PanelCatalogItem(
    val name: String,
    val powerWatts: Int
)

data class InverterCatalogItem(
    val name: String,
    val powerKw: Double
)

data class BatteryCatalogItem(
    val name: String,
    val capacityKwh: Double
)

data class InverterSelection(
    val name: String,
    val quantity: Int = 1
)

data class BrazilianStateInfo(
    val uf: String,
    val name: String,
    val defaultCity: String,
    val avgHsp: Double
)

object BrazilSolarData {
    val states = listOf(
        BrazilianStateInfo("AC", "Acre", "Rio Branco", 4.70),
        BrazilianStateInfo("AL", "Alagoas", "Maceió", 5.35),
        BrazilianStateInfo("AP", "Amapá", "Macapá", 4.85),
        BrazilianStateInfo("AM", "Amazonas", "Manaus", 4.65),
        BrazilianStateInfo("BA", "Bahia", "Salvador", 5.55),
        BrazilianStateInfo("CE", "Ceará", "Fortaleza", 5.65),
        BrazilianStateInfo("DF", "Distrito Federal", "Brasília", 5.20),
        BrazilianStateInfo("ES", "Espírito Santo", "Vitória", 4.95),
        BrazilianStateInfo("GO", "Goiás", "Senador Canedo", 5.25),
        BrazilianStateInfo("MA", "Maranhão", "São Luís", 5.30),
        BrazilianStateInfo("MT", "Mato Grosso", "Cuiabá", 5.30),
        BrazilianStateInfo("MS", "Mato Grosso do Sul", "Campo Grande", 5.10),
        BrazilianStateInfo("MG", "Minas Gerais", "Belo Horizonte", 5.15),
        BrazilianStateInfo("PA", "Pará", "Belém", 4.85),
        BrazilianStateInfo("PB", "Paraíba", "João Pessoa", 5.55),
        BrazilianStateInfo("PR", "Paraná", "Curitiba", 4.50),
        BrazilianStateInfo("PE", "Pernambuco", "Recife", 5.50),
        BrazilianStateInfo("PI", "Piauí", "Teresina", 5.70),
        BrazilianStateInfo("RJ", "Rio de Janeiro", "Rio de Janeiro", 4.80),
        BrazilianStateInfo("RN", "Rio Grande do Norte", "Natal", 5.65),
        BrazilianStateInfo("RS", "Rio Grande do Sul", "Porto Alegre", 4.30),
        BrazilianStateInfo("RO", "Rondônia", "Porto Velho", 4.80),
        BrazilianStateInfo("RR", "Roraima", "Boa Vista", 5.05),
        BrazilianStateInfo("SC", "Santa Catarina", "Florianópolis", 4.35),
        BrazilianStateInfo("SP", "São Paulo", "São Paulo", 4.75),
        BrazilianStateInfo("SE", "Sergipe", "Aracaju", 5.40),
        BrazilianStateInfo("TO", "Tocantins", "Palmas", 5.45)
    )

    fun findState(uf: String): BrazilianStateInfo =
        states.firstOrNull { it.uf.equals(uf, ignoreCase = true) } ?: states.first { it.uf == "GO" }

    val defaultPanels = listOf(
        PanelCatalogItem("MODULO BIFACIAL 132 CEL. N TYPE 605W", 605),
        PanelCatalogItem("MODULO N-TYPE TOPCON 585W", 585),
        PanelCatalogItem("MODULO MONOCRISTALINO HALF-CELL 550W", 550),
        PanelCatalogItem("MODULO BIFACIAL 700W", 700),
        PanelCatalogItem("MODULO MONOCRISTALINO 460W", 460)
    )

    val defaultInverters = listOf(
        InverterCatalogItem("Inversor String 3kW Monofásico", 3.0),
        InverterCatalogItem("Inversor String 5kW Monofásico", 5.0),
        InverterCatalogItem("Inversor String 8kW Monofásico", 8.0),
        InverterCatalogItem("Inversor String 10kW Trifásico", 10.0),
        InverterCatalogItem("Inversor String 15kW Trifásico", 15.0),
        InverterCatalogItem("Inversor String 25kW Trifásico", 25.0),
        InverterCatalogItem("Microinversor 2kW (4 canais)", 2.0),
        InverterCatalogItem("Inversor Híbrido 8kW 48V", 8.0)
    )

    val defaultBatteries = listOf(
        BatteryCatalogItem("Bateria Lítio LiFePO4 5,12 kWh", 5.12),
        BatteryCatalogItem("Bateria Lítio LiFePO4 10,24 kWh", 10.24),
        BatteryCatalogItem("Banco de Baterias 15,36 kWh", 15.36)
    )

    val monthLabels = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    val monthlySeasonality = listOf(1.06, 1.04, 1.00, 0.94, 0.89, 0.86, 0.90, 1.02, 1.07, 1.08, 1.06, 1.08)
}

data class ProposalMetrics(
    val powerKwp: Double,
    val estimatedMonthlyKwh: Double,
    val estimatedMonthlySavings: Double,
    val estimatedAnnualSavings: Double,
    val savings25Years: Double,
    val paybackMonths: Int,
    val monthlyBreakdownKwh: List<Double>,
    val hspUsed: Double,
    val cardTotalWithFee: Double,
    val cardParcelValue: Double,
    val financing60xParcel: Double
)

object SolarCalculator {
    private const val PERFORMANCE_RATIO = 0.80

    fun recommendPanelCount(
        consumptionValue: Double,
        isCurrencyReais: Boolean,
        kwhTariff: Double,
        connectionType: ConnectionType,
        stateUf: String,
        panelPowerW: Int
    ): Int {
        if (consumptionValue <= 0.0 || panelPowerW <= 0) return 10
        val tariff = kwhTariff.coerceAtLeast(0.10)
        val totalKwh = if (isCurrencyReais) consumptionValue / tariff else consumptionValue
        val netKwhToGenerate = max(50.0, totalKwh - connectionType.minKwh)
        val hsp = BrazilSolarData.findState(stateUf).avgHsp
        val kwhPerPanelMonth = (panelPowerW / 1000.0) * hsp * 30.0 * PERFORMANCE_RATIO
        return ceil(netKwhToGenerate / kwhPerPanelMonth).toInt().coerceIn(2, 2000)
    }

    fun calculateMetrics(
        panelCount: Int,
        panelPowerW: Int,
        stateUf: String,
        kwhTariff: Double,
        connectionType: ConnectionType,
        totalPrice: Double,
        cardInstallments: Int,
        cardFeePercent: Double
    ): ProposalMetrics {
        val hsp = BrazilSolarData.findState(stateUf).avgHsp
        val safeCount = panelCount.coerceAtLeast(1)
        val safeWatts = panelPowerW.coerceAtLeast(50)
        val powerKwp = (safeCount * safeWatts) / 1000.0
        val monthlyKwh = powerKwp * hsp * 30.0 * PERFORMANCE_RATIO
        val tariff = kwhTariff.coerceAtLeast(0.10)
        val billableKwh = max(0.0, monthlyKwh - connectionType.minKwh * 0.35)
        val monthlySavings = billableKwh * tariff
        val annualSavings = monthlySavings * 12.0
        val savings25Years = annualSavings * 25.0 * 1.18 // conservative energy tariff inflation factor
        val paybackMonths = if (monthlySavings > 1.0 && totalPrice > 0.0) {
            (totalPrice / monthlySavings).roundToInt().coerceIn(6, 360)
        } else 0

        val monthlyBreakdown = BrazilSolarData.monthlySeasonality.map { factor ->
            (monthlyKwh * factor * 10.0).roundToLong() / 10.0
        }

        val cardTotal = totalPrice * (1.0 + (cardFeePercent.coerceAtLeast(0.0) / 100.0))
        val safeParcels = cardInstallments.coerceAtLeast(1)
        val cardParcel = if (totalPrice > 0.0) cardTotal / safeParcels else 0.0

        // Standard solar financing simulation (60x at ~1.69% a.m.)
        val i = 0.0169
        val n = 60
        val factor = Math.pow(1.0 + i, n.toDouble())
        val financing60x = if (totalPrice > 0.0) totalPrice * (i * factor) / (factor - 1.0) else 0.0

        return ProposalMetrics(
            powerKwp = (powerKwp * 100.0).roundToLong() / 100.0,
            estimatedMonthlyKwh = (monthlyKwh * 10.0).roundToLong() / 10.0,
            estimatedMonthlySavings = (monthlySavings * 100.0).roundToLong() / 100.0,
            estimatedAnnualSavings = (annualSavings * 100.0).roundToLong() / 100.0,
            savings25Years = (savings25Years * 100.0).roundToLong() / 100.0,
            paybackMonths = paybackMonths,
            monthlyBreakdownKwh = monthlyBreakdown,
            hspUsed = hsp,
            cardTotalWithFee = cardTotal,
            cardParcelValue = cardParcel,
            financing60xParcel = financing60x
        )
    }

    fun suggestedPrice(powerKwp: Double, pricePerWp: Double, isHybrid: Boolean): Double {
        val base = powerKwp * 1000.0 * pricePerWp.coerceAtLeast(1.0)
        val hybridAddon = if (isHybrid) 14500.0 else 0.0
        return ((base + hybridAddon) / 10.0).roundToLong() * 10.0
    }
}

object BrFormatters {
    private val ptBr = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBr)
    private val decimalFormat = NumberFormat.getNumberInstance(ptBr).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val integerFormat = NumberFormat.getIntegerInstance(ptBr)

    fun currency(value: Double): String = currencyFormat.format(value)
    fun decimal2(value: Double): String = decimalFormat.format(value)
    fun integer(value: Number): String = integerFormat.format(value)

    fun parseBrNumber(raw: String): Double? {
        val cleaned = raw.trim()
            .replace("R$", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val normalized = if (cleaned.contains(",")) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }
        return normalized.toDoubleOrNull()
    }
}

data class ProposalTemplateInfo(
    val number: Int,
    val title: String,
    val subtitle: String
)

val PROPOSAL_TEMPLATES = listOf(
    ProposalTemplateInfo(1, "Modelo 1 • Clássico", "Faixa verde escura no topo e cartões de economia."),
    ProposalTemplateInfo(2, "Modelo 2 • Editorial", "Página clara, nome do cliente em destaque."),
    ProposalTemplateInfo(3, "Modelo 3 • Executivo", "Barra lateral verde com resumo e régua de retorno."),
    ProposalTemplateInfo(4, "Modelo 4 • Mosaico", "Blocos arredondados e gráfico em barras."),
    ProposalTemplateInfo(5, "Modelo 5 • Completa", "Capa escura + 2 páginas: empresa, 25 anos e retorno.")
)
