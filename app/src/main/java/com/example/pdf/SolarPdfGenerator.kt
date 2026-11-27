package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.BrFormatters
import com.example.model.BrazilSolarData
import com.example.model.CompanySettings
import com.example.model.ConnectionType
import com.example.model.ProposalMetrics
import com.example.model.SolarCalculator
import com.example.model.SolarProposal
import com.example.model.SystemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SolarPdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private val GREEN_DEEP = Color.parseColor("#084C2E")
    private val GREEN_PRIMARY = Color.parseColor("#0F7A47")
    private val GREEN_VIBRANT = Color.parseColor("#16A34A")
    private val GREEN_MINT = Color.parseColor("#ECFDF5")
    private val GREEN_BORDER = Color.parseColor("#CBE7D6")
    private val GOLD_SUN = Color.parseColor("#F59E0B")
    private val TEXT_DARK = Color.parseColor("#0D2818")
    private val TEXT_MUTED = Color.parseColor("#4B6356")
    private val BG_LIGHT = Color.parseColor("#F7FBF8")

    fun logoFile(context: Context): File = File(context.filesDir, "company_logo.png")

    suspend fun generateProposalPdf(
        context: Context,
        proposal: SolarProposal,
        settings: CompanySettings
    ): File = withContext(Dispatchers.IO) {
        val outDir = File(context.cacheDir, "proposals").apply { mkdirs() }
        val safeCode = proposal.proposalCode.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(outDir, "Proposta_${safeCode}.pdf")

        val metrics = SolarCalculator.calculateMetrics(
            panelCount = proposal.panelCount.toInt(),
            panelPowerW = proposal.panelPowerW.toInt(),
            stateUf = proposal.state,
            kwhTariff = settings.kwhTariff,
            connectionType = ConnectionType.fromWire(proposal.connectionType),
            totalPrice = proposal.totalPrice,
            cardInstallments = proposal.cardInstallments.toInt(),
            cardFeePercent = proposal.cardFeePercent
        )

        val logoBitmap = logoFile(context).takeIf { it.exists() }?.let {
            runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull()
        }

        val doc = PdfDocument()
        try {
            when (proposal.templateModel.toInt()) {
                2 -> drawEditorialTemplate(doc, proposal, settings, metrics, logoBitmap)
                3 -> drawExecutiveSidebarTemplate(doc, proposal, settings, metrics, logoBitmap)
                4 -> drawMosaicTemplate(doc, proposal, settings, metrics, logoBitmap)
                5 -> drawCompleteTwoPageTemplate(doc, proposal, settings, metrics, logoBitmap)
                else -> drawClassicTemplate(doc, proposal, settings, metrics, logoBitmap)
            }
            FileOutputStream(outFile).use { fos ->
                doc.writeTo(fos)
            }
        } finally {
            doc.close()
            logoBitmap?.recycle()
        }
        outFile
    }

    suspend fun renderPreviewBitmaps(file: File): List<Bitmap> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps += bmp
                    }
                }
            }
        }
        bitmaps
    }

    fun getUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun sharePdf(context: Context, file: File, proposal: SolarProposal) {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Proposta Solar ${proposal.proposalCode} - ${proposal.clientName}"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Olá ${proposal.clientName}, segue sua proposta de energia solar (${BrFormatters.decimal2(proposal.powerKwp)} kWp) preparada pela equipe."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_proposal_chooser))
        )
    }

    fun openPdf(context: Context, file: File) {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.open_proposal_chooser))
        )
    }

    private fun drawClassicTemplate(
        doc: PdfDocument,
        p: SolarProposal,
        s: CompanySettings,
        m: ProposalMetrics,
        logo: Bitmap?
    ) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val c = page.canvas
        c.drawColor(Color.WHITE)

        // Top dark emerald banner
        c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 155f, fillPaint(GREEN_DEEP))
        drawCompanyHeader(c, s, p, logo, x = 32f, y = 24f, darkBg = true)

        c.drawText(
            "Proposta Comercial • Energia Solar Fotovoltaica",
            32f, 108f, textPaint(15f, Color.WHITE, bold = true)
        )
        c.drawText(
            "Cliente: ${p.clientName}  |  ${p.city.ifBlank { BrazilSolarData.findState(p.state).defaultCity }}/${p.state}",
            32f, 130f, textPaint(11f, Color.parseColor("#A7F3D0"))
        )

        // 4 KPI Cards
        drawKpiBox(c, 32f, 174f, 125f, 68f, "POTÊNCIA", "${BrFormatters.decimal2(m.powerKwp)} kWp", GREEN_MINT, GREEN_DEEP)
        drawKpiBox(c, 167f, 174f, 125f, 68f, "ECONOMIA MENSAL", BrFormatters.currency(m.estimatedMonthlySavings), GREEN_VIBRANT, Color.WHITE)
        drawKpiBox(c, 302f, 174f, 125f, 68f, "ECONOMIA ANUAL", BrFormatters.currency(m.estimatedAnnualSavings), BG_LIGHT, TEXT_DARK)
        drawKpiBox(c, 437f, 174f, 126f, 68f, "RETORNO (PAYBACK)", "${m.paybackMonths} meses", GREEN_DEEP, Color.WHITE)

        // 25 year pill
        val r25 = RectF(32f, 256f, 563f, 290f)
        c.drawRoundRect(r25, 8f, 8f, fillPaint(GREEN_MINT))
        c.drawText(
            "Economia acumulada estimada em 25 anos: ${BrFormatters.currency(m.savings25Years)}",
            46f, 278f, textPaint(12f, GREEN_DEEP, bold = true)
        )

        // Equipment section
        drawEquipmentSection(c, p, m, x = 32f, y = 310f, width = 531f)

        // Monthly generation vertical bar chart
        drawVerticalBarChart(c, m.monthlyBreakdownKwh, 32f, 445f, 531f, 165f)

        // Payment conditions
        drawPaymentSection(c, p, m, s, 32f, 630f, 531f)

        drawFooter(c, s, p)
        doc.finishPage(page)
    }

    private fun drawEditorialTemplate(
        doc: PdfDocument,
        p: SolarProposal,
        s: CompanySettings,
        m: ProposalMetrics,
        logo: Bitmap?
    ) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val c = page.canvas
        c.drawColor(BG_LIGHT)

        // Top clean accent line
        c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, fillPaint(GREEN_PRIMARY))
        drawCompanyHeader(c, s, p, logo, x = 32f, y = 26f, darkBg = false)

        c.drawText("PROPOSTA PREPARADA PARA", 32f, 96f, textPaint(10f, GREEN_PRIMARY, bold = true))
        c.drawText(p.clientName, 32f, 124f, textPaint(24f, TEXT_DARK, bold = true))
        c.drawText(
            "${p.city.ifBlank { BrazilSolarData.findState(p.state).defaultCity }} - ${p.state} • ${SystemType.fromWire(p.systemType).label} • ${ConnectionType.fromWire(p.connectionType).label}",
            32f, 144f, textPaint(10.5f, TEXT_MUTED)
        )

        // Dark featured economy card + side metrics
        val heroRect = RectF(32f, 164f, 340f, 256f)
        c.drawRoundRect(heroRect, 14f, 14f, fillPaint(GREEN_DEEP))
        c.drawText("SUA ECONOMIA ESTIMADA/MÊS", 50f, 192f, textPaint(10f, Color.parseColor("#A7F3D0"), bold = true))
        c.drawText(BrFormatters.currency(m.estimatedMonthlySavings), 50f, 226f, textPaint(24f, Color.WHITE, bold = true))
        c.drawText("Payback estimado: ${m.paybackMonths} meses", 50f, 246f, textPaint(10.5f, GOLD_SUN, bold = true))

        drawKpiBox(c, 354f, 164f, 209f, 42f, "SISTEMA / GERAÇÃO", "${BrFormatters.decimal2(m.powerKwp)} kWp • ${BrFormatters.integer(m.estimatedMonthlyKwh.toInt())} kWh/mês", Color.WHITE, TEXT_DARK)
        drawKpiBox(c, 354f, 214f, 209f, 42f, "RETORNO EM 25 ANOS", BrFormatters.currency(m.savings25Years), GREEN_MINT, GREEN_DEEP)

        drawVerticalBarChart(c, m.monthlyBreakdownKwh, 32f, 276f, 531f, 165f)
        drawEquipmentSection(c, p, m, 32f, 460f, 531f)
        drawPaymentSection(c, p, m, s, 32f, 595f, 531f)

        drawFooter(c, s, p)
        doc.finishPage(page)
    }

    private fun drawExecutiveSidebarTemplate(
        doc: PdfDocument,
        p: SolarProposal,
        s: CompanySettings,
        m: ProposalMetrics,
        logo: Bitmap?
    ) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val c = page.canvas
        c.drawColor(Color.WHITE)

        // Left deep green sidebar
        c.drawRect(0f, 0f, 185f, PAGE_HEIGHT.toFloat(), fillPaint(GREEN_DEEP))
        if (logo != null) {
            val logoRect = RectF(24f, 24f, 76f, 76f)
            c.drawBitmap(logo, null, logoRect, null)
        }
        c.drawText(s.companyName.take(20), 24f, if (logo != null) 100f else 48f, textPaint(14f, Color.WHITE, bold = true))
        c.drawText("Proposta ${p.proposalCode}", 24f, if (logo != null) 118f else 66f, textPaint(10f, Color.parseColor("#A7F3D0")))

        var sy = 155f
        val sidebarMetrics = listOf(
            "CLIENTE" to p.clientName.take(22),
            "POTÊNCIA" to "${BrFormatters.decimal2(m.powerKwp)} kWp",
            "GERAÇÃO MÉDIA" to "${BrFormatters.integer(m.estimatedMonthlyKwh.toInt())} kWh/mês",
            "MÓDULOS" to "${p.panelCount}x ${p.panelPowerW}W",
            "RETORNO" to "${m.paybackMonths} meses",
            "ECONOMIA 25 ANOS" to BrFormatters.currency(m.savings25Years)
        )
        sidebarMetrics.forEach { (k, v) ->
            c.drawText(k, 24f, sy, textPaint(8.5f, Color.parseColor("#6EE7B7"), bold = true))
            c.drawText(v, 24f, sy + 16f, textPaint(12f, Color.WHITE, bold = true))
            c.drawLine(24f, sy + 28f, 160f, sy + 28f, strokePaint(Color.parseColor("#1B6B46"), 1f))
            sy += 48f
        }

        // Right executive content
        val rx = 205f
        val rw = 358f
        c.drawText("Seu sistema solar pronto", rx, 46f, textPaint(19f, TEXT_DARK, bold = true))
        c.drawText("para gerar economia imediata", rx, 68f, textPaint(19f, GREEN_PRIMARY, bold = true))

        drawKpiBox(c, rx, 92f, 172f, 64f, "ECONOMIA / MÊS", BrFormatters.currency(m.estimatedMonthlySavings), GREEN_MINT, GREEN_DEEP)
        drawKpiBox(c, rx + 184f, 92f, 174f, 64f, "INVESTIMENTO", BrFormatters.currency(p.totalPrice), GREEN_VIBRANT, Color.WHITE)

        drawVerticalBarChart(c, m.monthlyBreakdownKwh, rx, 176f, rw, 170f)
        drawEquipmentSection(c, p, m, rx, 366f, rw)
        drawPaymentSection(c, p, m, s, rx, 505f, rw)

        c.drawText(
            "Emitido em ${todayStr()} • Validade ${s.defaultValidityDays} dias",
            rx, PAGE_HEIGHT - 28f, textPaint(9f, TEXT_MUTED)
        )
        doc.finishPage(page)
    }

    private fun drawMosaicTemplate(
        doc: PdfDocument,
        p: SolarProposal,
        s: CompanySettings,
        m: ProposalMetrics,
        logo: Bitmap?
    ) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val c = page.canvas
        c.drawColor(BG_LIGHT)

        drawCompanyHeader(c, s, p, logo, 32f, 24f, darkBg = false)

        // Mosaic top blocks
        val b1 = RectF(32f, 96f, 385f, 162f)
        c.drawRoundRect(b1, 12f, 12f, fillPaint(Color.WHITE))
        c.drawRoundRect(b1, 12f, 12f, strokePaint(GREEN_BORDER, 1.2f))
        c.drawText("CLIENTE", 48f, 118f, textPaint(9f, GREEN_PRIMARY, bold = true))
        c.drawText(p.clientName, 48f, 142f, textPaint(18f, TEXT_DARK, bold = true))

        val b2 = RectF(397f, 96f, 563f, 162f)
        c.drawRoundRect(b2, 12f, 12f, fillPaint(GREEN_DEEP))
        c.drawText("RETORNO ESTIMADO", 413f, 118f, textPaint(9f, Color.parseColor("#A7F3D0"), bold = true))
        c.drawText("~${m.paybackMonths} meses", 413f, 144f, textPaint(18f, GOLD_SUN, bold = true))

        // Row of 3 mosaic cards
        drawKpiBox(c, 32f, 174f, 168f, 62f, "POTÊNCIA", "${BrFormatters.decimal2(m.powerKwp)} kWp", Color.WHITE, TEXT_DARK)
        drawKpiBox(c, 212f, 174f, 168f, 62f, "GERAÇÃO/MÊS", "${BrFormatters.integer(m.estimatedMonthlyKwh.toInt())} kWh", Color.WHITE, TEXT_DARK)
        drawKpiBox(c, 392f, 174f, 171f, 62f, "ECONOMIA/MÊS", BrFormatters.currency(m.estimatedMonthlySavings), GREEN_MINT, GREEN_DEEP)

        // Horizontal bar chart (like Screenshot 4!)
        drawHorizontalBarChart(c, m.monthlyBreakdownKwh, 32f, 252f, 531f, 215f)

        drawEquipmentSection(c, p, m, 32f, 482f, 531f)
        drawPaymentSection(c, p, m, s, 32f, 616f, 531f)

        drawFooter(c, s, p)
        doc.finishPage(page)
    }

    private fun drawCompleteTwoPageTemplate(
        doc: PdfDocument,
        p: SolarProposal,
        s: CompanySettings,
        m: ProposalMetrics,
        logo: Bitmap?
    ) {
        // PAGE 1: Dark Emerald Cover
        val page1 = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val c1 = page1.canvas
        c1.drawColor(GREEN_DEEP)

        // Decorative sun rays arc on top-right
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#146C43")
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        c1.drawCircle(520f, 120f, 130f, sunPaint)
        c1.drawCircle(520f, 120f, 80f, sunPaint)

        if (logo != null) {
            c1.drawBitmap(logo, null, RectF(42f, 42f, 112f, 112f), null)
        }
        c1.drawText(s.companyName.uppercase(Locale("pt", "BR")), 42f, 152f, textPaint(16f, Color.parseColor("#6EE7B7"), bold = true))
        c1.drawText("PROPOSTA COMERCIAL", 42f, 240f, textPaint(13f, GOLD_SUN, bold = true))
        c1.drawText("Energia Solar", 42f, 286f, textPaint(36f, Color.WHITE, bold = true))
        c1.drawText("Fotovoltaica", 42f, 330f, textPaint(36f, Color.WHITE, bold = true))

        c1.drawText("Preparada especialmente para:", 42f, 410f, textPaint(12f, Color.parseColor("#A7F3D0")))
        c1.drawText(p.clientName, 42f, 442f, textPaint(24f, Color.WHITE, bold = true))
        c1.drawText(
            "${p.city.ifBlank { BrazilSolarData.findState(p.state).defaultCity }} - ${p.state} • Proposta ${p.proposalCode}",
            42f, 468f, textPaint(12f, Color.parseColor("#A7F3D0"))
        )

        // Bottom 3 KPI cards on cover
        drawKpiBox(c1, 42f, 650f, 160f, 82f, "POTÊNCIA DO SISTEMA", "${BrFormatters.decimal2(m.powerKwp)} kWp", Color.parseColor("#0D5E3A"), Color.WHITE)
        drawKpiBox(c1, 217f, 650f, 160f, 82f, "GERAÇÃO MENSAL", "${BrFormatters.integer(m.estimatedMonthlyKwh.toInt())} kWh", Color.parseColor("#0D5E3A"), Color.WHITE)
        drawKpiBox(c1, 392f, 650f, 161f, 82f, "ECONOMIA MENSAL", BrFormatters.currency(m.estimatedMonthlySavings), GREEN_VIBRANT, Color.WHITE)

        c1.drawText("Emitida em ${todayStr()} • Validade ${s.defaultValidityDays} dias", 42f, 792f, textPaint(10.5f, Color.parseColor("#A7F3D0")))
        doc.finishPage(page1)

        // PAGE 2: Technical + Financial Dossier
        val page2 = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
        val c2 = page2.canvas
        c2.drawColor(Color.WHITE)
        c2.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 58f, fillPaint(GREEN_DEEP))
        c2.drawText("${s.companyName} • Detalhamento Técnico e Financeiro", 32f, 36f, textPaint(14f, Color.WHITE, bold = true))

        drawKpiBox(c2, 32f, 78f, 168f, 66f, "ECONOMIA ANUAL", BrFormatters.currency(m.estimatedAnnualSavings), GREEN_MINT, GREEN_DEEP)
        drawKpiBox(c2, 212f, 78f, 168f, 66f, "RETORNO EM 25 ANOS", BrFormatters.currency(m.savings25Years), GREEN_MINT, GREEN_DEEP)
        drawKpiBox(c2, 392f, 78f, 171f, 66f, "PAYBACK ESTIMADO", "${m.paybackMonths} meses", GREEN_DEEP, Color.WHITE)

        drawEquipmentSection(c2, p, m, 32f, 164f, 531f)
        drawVerticalBarChart(c2, m.monthlyBreakdownKwh, 32f, 305f, 531f, 185f)
        drawPaymentSection(c2, p, m, s, 32f, 512f, 531f)
        drawFooter(c2, s, p)
        doc.finishPage(page2)
    }

    private fun drawCompanyHeader(
        c: Canvas,
        s: CompanySettings,
        p: SolarProposal,
        logo: Bitmap?,
        x: Float,
        y: Float,
        darkBg: Boolean
    ) {
        val textPrimary = if (darkBg) Color.WHITE else TEXT_DARK
        val textSub = if (darkBg) Color.parseColor("#A7F3D0") else TEXT_MUTED
        var textX = x
        if (logo != null) {
            c.drawBitmap(logo, null, RectF(x, y, x + 46f, y + 46f), null)
            textX += 56f
        }
        c.drawText(s.companyName, textX, y + 20f, textPaint(15f, textPrimary, bold = true))
        val contactLine = listOf(s.phone, s.email).filter { it.isNotBlank() }.joinToString(" • ")
        c.drawText(
            contactLine.ifBlank { "Energia Solar Fotovoltaica" },
            textX, y + 36f, textPaint(9.5f, textSub)
        )
        c.drawText(
            "Proposta ${p.proposalCode} • ${todayStr()}",
            PAGE_WIDTH - 195f, y + 20f, textPaint(10f, textSub, bold = true)
        )
    }

    private fun drawKpiBox(
        c: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        bgColor: Int,
        fgColor: Int
    ) {
        val rect = RectF(x, y, x + w, y + h)
        c.drawRoundRect(rect, 10f, 10f, fillPaint(bgColor))
        if (bgColor == Color.WHITE || bgColor == BG_LIGHT) {
            c.drawRoundRect(rect, 10f, 10f, strokePaint(GREEN_BORDER, 1f))
        }
        val labelColor = if (fgColor == Color.WHITE) Color.parseColor("#D1FAE5") else TEXT_MUTED
        c.drawText(label, x + 10f, y + 20f, textPaint(8f, labelColor, bold = true))
        c.drawText(value, x + 10f, y + h - 16f, textPaint(13f, fgColor, bold = true))
    }

    private fun drawEquipmentSection(
        c: Canvas,
        p: SolarProposal,
        m: ProposalMetrics,
        x: Float,
        y: Float,
        width: Float
    ) {
        val box = RectF(x, y, x + width, y + 118f)
        c.drawRoundRect(box, 10f, 10f, fillPaint(Color.WHITE))
        c.drawRoundRect(box, 10f, 10f, strokePaint(GREEN_BORDER, 1f))

        c.drawText("DIMENSIONAMENTO E EQUIPAMENTOS", x + 14f, y + 22f, textPaint(10f, GREEN_PRIMARY, bold = true))
        c.drawText(
            "• Módulos: ${p.panelCount}x ${p.panelModel} (${BrFormatters.decimal2(m.powerKwp)} kWp)",
            x + 14f, y + 44f, textPaint(10.5f, TEXT_DARK)
        )
        c.drawText(
            "• Inversor(es): ${p.inverters.joinToString(" + ").take(75)}",
            x + 14f, y + 64f, textPaint(10.5f, TEXT_DARK)
        )
        val extra = if (p.systemType == SystemType.HIBRIDO.wireValue && p.batterySummary.isNotBlank()) {
            "• Armazenamento: ${p.batterySummary}"
        } else {
            "• Padrão: ${ConnectionType.fromWire(p.connectionType).label} • Irradiação: ${BrFormatters.decimal2(m.hspUsed)} kWh/m²/dia"
        }
        c.drawText(extra, x + 14f, y + 84f, textPaint(10.5f, TEXT_MUTED))
        c.drawText(
            "• Geração estimada: ${BrFormatters.integer(m.estimatedMonthlyKwh.toInt())} kWh/mês",
            x + 14f, y + 104f, textPaint(10.5f, GREEN_DEEP, bold = true)
        )
    }

    private fun drawVerticalBarChart(
        c: Canvas,
        monthlyKwh: List<Double>,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val box = RectF(x, y, x + width, y + height)
        c.drawRoundRect(box, 10f, 10f, fillPaint(Color.WHITE))
        c.drawRoundRect(box, 10f, 10f, strokePaint(GREEN_BORDER, 1f))
        c.drawText("GERAÇÃO ESTIMADA MÊS A MÊS (kWh)", x + 14f, y + 22f, textPaint(10f, GREEN_PRIMARY, bold = true))

        val maxVal = (monthlyKwh.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val chartLeft = x + 16f
        val chartBottom = y + height - 24f
        val chartHeight = height - 62f
        val step = (width - 32f) / 12f
        val barWidth = step * 0.56f

        monthlyKwh.forEachIndexed { idx, value ->
            val ratio = (value / maxVal).toFloat().coerceIn(0.1f, 1f)
            val barH = chartHeight * ratio
            val bx = chartLeft + idx * step + (step - barWidth) / 2f
            val color = if (idx == 9) GOLD_SUN else GREEN_PRIMARY
            c.drawRoundRect(RectF(bx, chartBottom - barH, bx + barWidth, chartBottom), 4f, 4f, fillPaint(color))
            c.drawText(BrazilSolarData.monthLabels[idx], bx, chartBottom + 13f, textPaint(8f, TEXT_MUTED))
        }
    }

    private fun drawHorizontalBarChart(
        c: Canvas,
        monthlyKwh: List<Double>,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        val box = RectF(x, y, x + width, y + height)
        c.drawRoundRect(box, 10f, 10f, fillPaint(Color.WHITE))
        c.drawRoundRect(box, 10f, 10f, strokePaint(GREEN_BORDER, 1f))
        c.drawText("GERAÇÃO MENSAL ESTIMADA (kWh)", x + 14f, y + 20f, textPaint(10f, GREEN_PRIMARY, bold = true))

        val maxVal = (monthlyKwh.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val rowH = (height - 34f) / 12f
        val barAreaW = width - 115f
        monthlyKwh.forEachIndexed { idx, value ->
            val ry = y + 32f + idx * rowH
            c.drawText(BrazilSolarData.monthLabels[idx], x + 14f, ry + 9f, textPaint(8.5f, TEXT_MUTED, bold = true))
            val bw = ((value / maxVal) * barAreaW).toFloat().coerceAtLeast(16f)
            val barColor = if (idx == 9) GREEN_DEEP else GREEN_VIBRANT
            c.drawRoundRect(RectF(x + 48f, ry, x + 48f + bw, ry + rowH * 0.68f), 4f, 4f, fillPaint(barColor))
            c.drawText("${value.toInt()}", x + 54f + bw, ry + 9f, textPaint(8f, TEXT_DARK))
        }
    }

    private fun drawPaymentSection(
        c: Canvas,
        p: SolarProposal,
        m: ProposalMetrics,
        s: CompanySettings,
        x: Float,
        y: Float,
        width: Float
    ) {
        val box = RectF(x, y, x + width, y + 145f)
        c.drawRoundRect(box, 10f, 10f, fillPaint(GREEN_MINT))
        c.drawRoundRect(box, 10f, 10f, strokePaint(GREEN_BORDER, 1f))

        c.drawText("INVESTIMENTO E CONDIÇÕES DE PAGAMENTO", x + 14f, y + 22f, textPaint(10f, GREEN_DEEP, bold = true))
        c.drawText(
            "Valor Total do Projeto: ${BrFormatters.currency(p.totalPrice)}",
            x + 14f, y + 48f, textPaint(16f, GREEN_DEEP, bold = true)
        )

        var py = y + 72f
        c.drawText("• À vista (PIX / Transferência): ${BrFormatters.currency(p.totalPrice)}", x + 14f, py, textPaint(10.5f, TEXT_DARK, bold = true))
        py += 18f
        if (p.cardEnabled && p.cardInstallments > 0) {
            c.drawText(
                "• Cartão de crédito: ${p.cardInstallments}x de ${BrFormatters.currency(m.cardParcelValue)} (taxa ${BrFormatters.decimal2(p.cardFeePercent)}%)",
                x + 14f, py, textPaint(10.5f, TEXT_DARK)
            )
            py += 18f
        }
        if (p.financingEnabled) {
            c.drawText(
                "• Financiamento solar: até 60x est. de ${BrFormatters.currency(m.financing60xParcel)}/mês",
                x + 14f, py, textPaint(10.5f, TEXT_DARK)
            )
            py += 18f
        }
        if (p.boletoEnabled) {
            c.drawText(
                "• Boleto parcelado: entrada + parcelas sob aprovação",
                x + 14f, py, textPaint(10.5f, TEXT_DARK)
            )
        }
    }

    private fun drawFooter(c: Canvas, s: CompanySettings, p: SolarProposal) {
        val footerText = listOf(s.companyName, s.documentNumber, s.address, "Validade: ${s.defaultValidityDays} dias")
            .filter { it.isNotBlank() }
            .joinToString(" • ")
        c.drawLine(32f, PAGE_HEIGHT - 44f, PAGE_WIDTH - 32f, PAGE_HEIGHT - 44f, strokePaint(GREEN_BORDER, 1f))
        c.drawText(footerText.take(105), 32f, PAGE_HEIGHT - 26f, textPaint(8.5f, TEXT_MUTED))
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun todayStr(): String = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
}
