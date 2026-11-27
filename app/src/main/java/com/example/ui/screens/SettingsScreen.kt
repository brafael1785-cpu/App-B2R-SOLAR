package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BrFormatters
import com.example.model.BrazilSolarData
import com.example.model.CompanySettings
import com.example.model.PROPOSAL_TEMPLATES
import com.example.pdf.SolarPdfGenerator
import com.example.ui.components.B2RDropdownSelector
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.VibrantGreen
import com.example.ui.theme.WarningText
import java.io.FileOutputStream

@Composable
fun SettingsScreen(
    settings: CompanySettings,
    isBusy: Boolean,
    onSaveSettings: (CompanySettings) -> Unit
) {
    val context = LocalContext.current
    var companyName by remember(settings) { mutableStateOf(settings.companyName) }
    var documentNumber by remember(settings) { mutableStateOf(settings.documentNumber) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var email by remember(settings) { mutableStateOf(settings.email) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var defaultState by remember(settings) { mutableStateOf(settings.defaultState) }
    var defaultCity by remember(settings) { mutableStateOf(settings.defaultCity) }
    var kwhTariffText by remember(settings) { mutableStateOf(BrFormatters.decimal2(settings.kwhTariff)) }
    var pricePerWpText by remember(settings) { mutableStateOf(BrFormatters.decimal2(settings.pricePerWp)) }
    var validityDaysText by remember(settings) { mutableStateOf(settings.defaultValidityDays.toString()) }
    var defaultTemplate by remember(settings) { mutableStateOf(settings.defaultTemplate.toInt().coerceIn(1, 5)) }
    var logoVersion by remember { mutableIntStateOf(0) }

    val logoFile = remember(context) { SolarPdfGenerator.logoFile(context) }
    val logoBitmap = remember(logoVersion, settings.logoConfigured) {
        if (logoFile.exists()) {
            runCatching { BitmapFactory.decodeFile(logoFile.absolutePath) }.getOrNull()
        } else null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(logoFile).use { output ->
                        input.copyTo(output)
                    }
                }
                logoVersion++
                onSaveSettings(
                    settings.copy(
                        companyName = companyName.ifBlank { "B2R Solar" },
                        documentNumber = documentNumber,
                        phone = phone,
                        email = email,
                        address = address,
                        defaultState = defaultState,
                        defaultCity = defaultCity,
                        logoConfigured = true
                    )
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MintContainer,
                border = BorderStroke(1.dp, SoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val pct = settings.completionPercent()
                    Text(
                        "Perfil da empresa: $pct% configurado",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestGreen
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        color = VibrantGreen,
                        trackColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Logo da empresa (aparece no PDF)", style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoBitmap != null) {
                                Image(
                                    bitmap = logoBitmap.asImageBitmap(),
                                    contentDescription = "Logo da empresa",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Filled.Business, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("pick_logo_button")
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (logoBitmap != null) "Trocar logo" else "Enviar logo")
                            }
                            if (logoBitmap != null) {
                                OutlinedButton(
                                    onClick = {
                                        logoFile.delete()
                                        logoVersion++
                                        onSaveSettings(settings.copy(logoConfigured = false))
                                    },
                                    border = BorderStroke(1.dp, SoftBorder)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = WarningText, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remover", color = WarningText)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dados comerciais e parâmetros solares", style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nome da empresa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = documentNumber,
                        onValueChange = { documentNumber = it },
                        label = { Text("CNPJ / CPF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / WhatsApp") },
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
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Endereço completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        B2RDropdownSelector(
                            label = "UF PADRÃO",
                            selectedText = defaultState,
                            items = BrazilSolarData.states,
                            itemLabel = { "${it.uf} · ${it.name}" },
                            onSelect = { defaultState = it.uf },
                            modifier = Modifier.width(100.dp)
                        )
                        OutlinedTextField(
                            value = defaultCity,
                            onValueChange = { defaultCity = it },
                            label = { Text("Cidade padrão") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = kwhTariffText,
                            onValueChange = { kwhTariffText = it },
                            label = { Text("Tarifa (R$/kWh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pricePerWpText,
                            onValueChange = { pricePerWpText = it },
                            label = { Text("Preço ref. (R$/Wp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = validityDaysText,
                        onValueChange = { validityDaysText = it },
                        label = { Text("Validade padrão da proposta (dias)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    B2RDropdownSelector(
                        label = "MODELO PADRÃO DO PDF",
                        selectedText = PROPOSAL_TEMPLATES.firstOrNull { it.number == defaultTemplate }?.title ?: "Modelo 1",
                        items = PROPOSAL_TEMPLATES,
                        itemLabel = { it.title },
                        onSelect = { defaultTemplate = it.number }
                    )

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    companyName = companyName.trim().ifBlank { "B2R Solar" },
                                    documentNumber = documentNumber.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    defaultState = defaultState,
                                    defaultCity = defaultCity.trim(),
                                    kwhTariff = (BrFormatters.parseBrNumber(kwhTariffText) ?: 0.95).coerceIn(0.1, 10.0),
                                    pricePerWp = (BrFormatters.parseBrNumber(pricePerWpText) ?: 2.80).coerceIn(0.5, 20.0),
                                    defaultValidityDays = (validityDaysText.toLongOrNull() ?: 15L).coerceIn(1L, 180L),
                                    defaultTemplate = defaultTemplate.toLong(),
                                    logoConfigured = logoFile.exists()
                                )
                            )
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar configurações", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
