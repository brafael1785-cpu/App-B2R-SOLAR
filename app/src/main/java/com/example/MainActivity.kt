package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.signOut
import com.example.data.B2RRepository
import com.example.model.CompanySettings
import com.example.pdf.SolarPdfGenerator
import com.example.ui.components.B2RNavigationStrip
import com.example.ui.components.B2RTopHeader
import com.example.ui.components.PdfPreviewDialog
import com.example.ui.components.SolarHelpDialog
import com.example.ui.screens.AgendaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.EquipmentScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProposalBuilderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.B2RSolarTheme
import com.example.ui.theme.VibrantGreen
import com.example.viewmodel.AppSection
import com.example.viewmodel.B2RViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            B2RSolarTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedB2RApp(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedB2RApp(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: B2RViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                B2RViewModel(B2RRepository(db), currentUserId)
            }
        }
    )

    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val settingsState by viewModel.companySettings.collectAsStateWithLifecycle()
    val clientsState by viewModel.clients.collectAsStateWithLifecycle()
    val proposalsState by viewModel.proposals.collectAsStateWithLifecycle()
    val appointmentsState by viewModel.appointments.collectAsStateWithLifecycle()
    val equipmentState by viewModel.equipment.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val pdfPreview by viewModel.pdfPreview.collectAsStateWithLifecycle()

    var showHelpDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    if (currentSection != AppSection.ORCAMENTO) {
        BackHandler {
            viewModel.navigateTo(AppSection.ORCAMENTO)
        }
    }

    val settings = (settingsState as? UiState.Success)?.data ?: CompanySettings(userId = currentUserId)
    val clients = (clientsState as? UiState.Success)?.data ?: emptyList()
    val proposals = (proposalsState as? UiState.Success)?.data ?: emptyList()
    val appointments = (appointmentsState as? UiState.Success)?.data ?: emptyList()
    val equipment = (equipmentState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                B2RTopHeader(
                    proposalsCount = proposals.size,
                    onLogoClick = { viewModel.navigateTo(AppSection.ORCAMENTO) },
                    onSignOutClick = {
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                )

                B2RNavigationStrip(
                    currentSection = currentSection,
                    clientsCount = clients.size,
                    proposalsCount = proposals.size,
                    agendaCount = appointments.count { !it.completed },
                    onSelectSection = { viewModel.navigateTo(it) },
                    onShowHelp = { showHelpDialog = true }
                )

                val anyError = listOf(settingsState, clientsState, proposalsState, appointmentsState)
                    .filterIsInstance<UiState.Error>()
                    .firstOrNull()

                if (anyError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Erro ao sincronizar dados: ${anyError.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else if (settingsState is UiState.Loading && clientsState is UiState.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VibrantGreen)
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentSection) {
                            AppSection.ORCAMENTO -> ProposalBuilderScreen(
                                viewModel = viewModel,
                                draft = draft,
                                settings = settings,
                                clients = clients,
                                proposals = proposals,
                                customEquipment = equipment,
                                isBusy = isBusy
                            )

                            AppSection.CLIENTES -> ClientsScreen(
                                clients = clients,
                                defaultState = settings.defaultState,
                                onSaveClient = { viewModel.saveClient(it) },
                                onDeleteClient = { viewModel.deleteClient(it) },
                                onCreateProposalForClient = { client ->
                                    viewModel.selectClientForDraft(client)
                                    viewModel.navigateTo(AppSection.ORCAMENTO)
                                }
                            )

                            AppSection.HISTORICO -> HistoryScreen(
                                proposals = proposals,
                                onGeneratePdf = { viewModel.generatePdfForExisting(context, it) },
                                onEditProposal = { viewModel.loadProposalIntoDraft(it, duplicate = false) },
                                onDuplicateProposal = { viewModel.loadProposalIntoDraft(it, duplicate = true) },
                                onUpdateStatus = { id, st -> viewModel.updateProposalStatus(id, st) },
                                onDeleteProposal = { viewModel.deleteProposal(it) }
                            )

                            AppSection.AGENDA -> AgendaScreen(
                                appointments = appointments,
                                onSaveAppointment = { viewModel.saveAppointment(it) },
                                onToggleCompleted = { id, done -> viewModel.toggleAppointmentCompleted(id, done) },
                                onDeleteAppointment = { viewModel.deleteAppointment(it) }
                            )

                            AppSection.CONFIGURACOES -> SettingsScreen(
                                settings = settings,
                                isBusy = isBusy,
                                onSaveSettings = { viewModel.saveCompanySettings(it) }
                            )

                            AppSection.EQUIPAMENTOS -> EquipmentScreen(
                                customEquipment = equipment,
                                onSaveEquipment = { viewModel.saveEquipment(it) },
                                onDeleteEquipment = { viewModel.deleteEquipment(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    pdfPreview?.let { preview ->
        PdfPreviewDialog(
            preview = preview,
            onDismiss = { viewModel.dismissPdfPreview() },
            onShare = { SolarPdfGenerator.sharePdf(context, preview.file, preview.proposal) },
            onOpenExternal = { SolarPdfGenerator.openPdf(context, preview.file) }
        )
    }

    if (showHelpDialog) {
        SolarHelpDialog(onDismiss = { showHelpDialog = false })
    }
}
