package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AgendaAppointment
import com.example.model.CompanySettings
import com.example.model.ConnectionType
import com.example.model.EquipmentKind
import com.example.model.ProposalStatus
import com.example.model.SolarClient
import com.example.model.SolarEquipment
import com.example.model.SolarProposal
import com.example.model.SystemType
import com.example.model.VisitType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class B2RRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Você precisa entrar com o Google antes de acessar os dados.")
    }

    // --- COMPANY SETTINGS ---
    fun observeCompanySettings(userId: String): Flow<CompanySettings> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/settings/company"
        emitAll(
            db.collection("users").document(uid)
                .collection("settings").document("company")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CompanySettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: CompanySettings(userId = uid)
                    } else {
                        CompanySettings(userId = uid)
                    }
                }
                .catch { error ->
                    handleFlowFailure(error, OperationType.GET, path, uid)
                }
        )
    }

    suspend fun saveCompanySettings(settings: CompanySettings): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/settings/company"
        return try {
            val uid = requireUserId()
            val companyName = settings.companyName.trim().ifBlank { "B2R Solar" }.take(100)
            val state = settings.defaultState.trim().uppercase().take(2).ifBlank { "GO" }
            val docRef = db.collection("users").document(uid)
                .collection("settings").document("company")
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "companyName" to companyName,
                        "documentNumber" to settings.documentNumber.trim().take(30),
                        "phone" to settings.phone.trim().take(30),
                        "email" to settings.email.trim().take(120),
                        "address" to settings.address.trim().take(200),
                        "defaultState" to state,
                        "defaultCity" to settings.defaultCity.trim().take(80),
                        "kwhTariff" to settings.kwhTariff.coerceIn(0.1, 10.0),
                        "pricePerWp" to settings.pricePerWp.coerceIn(0.5, 20.0),
                        "defaultValidityDays" to settings.defaultValidityDays.coerceIn(1L, 180L),
                        "defaultTemplate" to settings.defaultTemplate.coerceIn(1L, 5L),
                        "logoConfigured" to settings.logoConfigured,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "companyName" to companyName,
                        "documentNumber" to settings.documentNumber.trim().take(30),
                        "phone" to settings.phone.trim().take(30),
                        "email" to settings.email.trim().take(120),
                        "address" to settings.address.trim().take(200),
                        "defaultState" to state,
                        "defaultCity" to settings.defaultCity.trim().take(80),
                        "kwhTariff" to settings.kwhTariff.coerceIn(0.1, 10.0),
                        "pricePerWp" to settings.pricePerWp.coerceIn(0.5, 20.0),
                        "defaultValidityDays" to settings.defaultValidityDays.coerceIn(1L, 180L),
                        "defaultTemplate" to settings.defaultTemplate.coerceIn(1L, 5L),
                        "logoConfigured" to settings.logoConfigured,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- CLIENTS ---
    fun observeClients(userId: String): Flow<List<SolarClient>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/clients"
        emitAll(
            db.collection("users").document(uid).collection("clients")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SolarClient::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowFailure(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun saveClient(client: SolarClient): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clients"
        return try {
            val uid = requireUserId()
            val cleanName = client.name.trim()
            require(cleanName.length >= 2) { "Informe o nome do cliente (mínimo 2 caracteres)." }
            val safeConn = ConnectionType.fromWire(client.connectionType).wireValue
            val safeState = client.state.trim().uppercase().take(2).ifBlank { "GO" }
            val collectionRef = db.collection("users").document(uid).collection("clients")

            if (client.id.isBlank()) {
                val docRef = collectionRef.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName.take(100),
                        "phone" to client.phone.trim().take(30),
                        "email" to client.email.trim().take(120),
                        "state" to safeState,
                        "city" to client.city.trim().take(80),
                        "connectionType" to safeConn,
                        "avgConsumptionKwh" to client.avgConsumptionKwh.coerceIn(0.0, 500000.0),
                        "notes" to client.notes.trim().take(400),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = collectionRef.document(client.id)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName.take(100),
                        "phone" to client.phone.trim().take(30),
                        "email" to client.email.trim().take(120),
                        "state" to safeState,
                        "city" to client.city.trim().take(80),
                        "connectionType" to safeConn,
                        "avgConsumptionKwh" to client.avgConsumptionKwh.coerceIn(0.0, 500000.0),
                        "notes" to client.notes.trim().take(400),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(client.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/clients/$clientId"
        return try {
            val uid = requireUserId()
            require(clientId.isNotBlank()) { "ID do cliente inválido." }
            val docRef = db.collection("users").document(uid).collection("clients").document(clientId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- PROPOSALS ---
    fun observeProposals(userId: String): Flow<List<SolarProposal>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/proposals"
        emitAll(
            db.collection("users").document(uid).collection("proposals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SolarProposal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowFailure(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun saveProposal(proposal: SolarProposal): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/proposals"
        return try {
            val uid = requireUserId()
            require(proposal.totalPrice >= 1.0) { "Informe o valor total da proposta." }
            require(proposal.panelCount in 1..10000) { "Quantidade de placas inválida." }
            val safeInverters = proposal.inverters
                .map { it.trim().take(140) }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("1x Inversor Solar String") }
                .take(10)

            val collectionRef = db.collection("users").document(uid).collection("proposals")
            val isNew = proposal.id.isBlank()
            val docRef = if (isNew) collectionRef.document() else collectionRef.document(proposal.id)
            docPath = docRef.path

            val safeState = proposal.state.trim().uppercase().take(2).ifBlank { "GO" }
            val safeClientName = proposal.clientName.trim().ifBlank { "Cliente genérico" }.take(100)
            val safeSysType = SystemType.fromWire(proposal.systemType).wireValue
            val safeConnType = ConnectionType.fromWire(proposal.connectionType).wireValue
            val safeStatus = ProposalStatus.fromWire(proposal.status).wireValue
            val safeCode = proposal.proposalCode.trim().ifBlank {
                "B2R-${System.currentTimeMillis().toString().takeLast(6)}"
            }.take(32)

            if (isNew) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "proposalCode" to safeCode,
                        "clientId" to proposal.clientId.trim().take(128),
                        "clientName" to safeClientName,
                        "clientPhone" to proposal.clientPhone.trim().take(30),
                        "state" to safeState,
                        "city" to proposal.city.trim().take(80),
                        "systemType" to safeSysType,
                        "connectionType" to safeConnType,
                        "consumptionInput" to proposal.consumptionInput.coerceIn(0.0, 500000.0),
                        "panelCount" to proposal.panelCount.coerceIn(1L, 10000L),
                        "panelModel" to proposal.panelModel.trim().ifBlank { "Módulo Solar 605W" }.take(140),
                        "panelPowerW" to proposal.panelPowerW.coerceIn(50L, 2000L),
                        "powerKwp" to proposal.powerKwp.coerceIn(0.1, 10000.0),
                        "inverters" to safeInverters,
                        "batterySummary" to proposal.batterySummary.trim().take(140),
                        "totalPrice" to proposal.totalPrice.coerceIn(1.0, 50000000.0),
                        "cashEnabled" to proposal.cashEnabled,
                        "cardEnabled" to proposal.cardEnabled,
                        "financingEnabled" to proposal.financingEnabled,
                        "boletoEnabled" to proposal.boletoEnabled,
                        "cardInstallments" to proposal.cardInstallments.coerceIn(0L, 60L),
                        "cardFeePercent" to proposal.cardFeePercent.coerceIn(0.0, 100.0),
                        "templateModel" to proposal.templateModel.coerceIn(1L, 5L),
                        "estimatedMonthlyKwh" to proposal.estimatedMonthlyKwh.coerceIn(0.0, 1000000.0),
                        "estimatedMonthlySavings" to proposal.estimatedMonthlySavings.coerceIn(0.0, 5000000.0),
                        "paybackMonths" to proposal.paybackMonths.coerceIn(0L, 600L),
                        "status" to safeStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "clientId" to proposal.clientId.trim().take(128),
                        "clientName" to safeClientName,
                        "clientPhone" to proposal.clientPhone.trim().take(30),
                        "state" to safeState,
                        "city" to proposal.city.trim().take(80),
                        "systemType" to safeSysType,
                        "connectionType" to safeConnType,
                        "consumptionInput" to proposal.consumptionInput.coerceIn(0.0, 500000.0),
                        "panelCount" to proposal.panelCount.coerceIn(1L, 10000L),
                        "panelModel" to proposal.panelModel.trim().ifBlank { "Módulo Solar 605W" }.take(140),
                        "panelPowerW" to proposal.panelPowerW.coerceIn(50L, 2000L),
                        "powerKwp" to proposal.powerKwp.coerceIn(0.1, 10000.0),
                        "inverters" to safeInverters,
                        "batterySummary" to proposal.batterySummary.trim().take(140),
                        "totalPrice" to proposal.totalPrice.coerceIn(1.0, 50000000.0),
                        "cashEnabled" to proposal.cashEnabled,
                        "cardEnabled" to proposal.cardEnabled,
                        "financingEnabled" to proposal.financingEnabled,
                        "boletoEnabled" to proposal.boletoEnabled,
                        "cardInstallments" to proposal.cardInstallments.coerceIn(0L, 60L),
                        "cardFeePercent" to proposal.cardFeePercent.coerceIn(0.0, 100.0),
                        "templateModel" to proposal.templateModel.coerceIn(1L, 5L),
                        "estimatedMonthlyKwh" to proposal.estimatedMonthlyKwh.coerceIn(0.0, 1000000.0),
                        "estimatedMonthlySavings" to proposal.estimatedMonthlySavings.coerceIn(0.0, 5000000.0),
                        "paybackMonths" to proposal.paybackMonths.coerceIn(0L, 600L),
                        "status" to safeStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProposalStatus(proposalId: String, status: ProposalStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/proposals/$proposalId"
        return try {
            val uid = requireUserId()
            require(proposalId.isNotBlank()) { "ID da proposta inválido." }
            val docRef = db.collection("users").document(uid).collection("proposals").document(proposalId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProposal(proposalId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/proposals/$proposalId"
        return try {
            val uid = requireUserId()
            require(proposalId.isNotBlank()) { "ID da proposta inválido." }
            val docRef = db.collection("users").document(uid).collection("proposals").document(proposalId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- AGENDA ---
    fun observeAppointments(userId: String): Flow<List<AgendaAppointment>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/appointments"
        emitAll(
            db.collection("users").document(uid).collection("appointments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AgendaAppointment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<AgendaAppointment> { it.completed }.thenByDescending { it.createdAt?.seconds ?: 0L })
                }
                .catch { error ->
                    handleFlowFailure(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun saveAppointment(appointment: AgendaAppointment): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/appointments"
        return try {
            val uid = requireUserId()
            val cleanTitle = appointment.title.trim()
            require(cleanTitle.length >= 2) { "Informe o título do compromisso." }
            val cleanDate = appointment.dateLabel.trim()
            require(cleanDate.length >= 4) { "Informe a data/hora do compromisso." }
            val safeVisit = VisitType.fromWire(appointment.visitType).wireValue
            val collectionRef = db.collection("users").document(uid).collection("appointments")

            if (appointment.id.isBlank()) {
                val docRef = collectionRef.document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle.take(120),
                        "clientName" to appointment.clientName.trim().take(100),
                        "dateLabel" to cleanDate.take(30),
                        "visitType" to safeVisit,
                        "notes" to appointment.notes.trim().take(400),
                        "completed" to appointment.completed,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = collectionRef.document(appointment.id)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "title" to cleanTitle.take(120),
                        "clientName" to appointment.clientName.trim().take(100),
                        "dateLabel" to cleanDate.take(30),
                        "visitType" to safeVisit,
                        "notes" to appointment.notes.trim().take(400),
                        "completed" to appointment.completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleAppointmentCompleted(appointmentId: String, completed: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/appointments/$appointmentId"
        return try {
            val uid = requireUserId()
            require(appointmentId.isNotBlank()) { "ID inválido." }
            val docRef = db.collection("users").document(uid).collection("appointments").document(appointmentId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/appointments/$appointmentId"
        return try {
            val uid = requireUserId()
            require(appointmentId.isNotBlank()) { "ID inválido." }
            val docRef = db.collection("users").document(uid).collection("appointments").document(appointmentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- EQUIPMENT ---
    fun observeEquipment(userId: String): Flow<List<SolarEquipment>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/equipment"
        emitAll(
            db.collection("users").document(uid).collection("equipment")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SolarEquipment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowFailure(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun saveEquipment(equipment: SolarEquipment): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/equipment"
        return try {
            val uid = requireUserId()
            val cleanName = equipment.name.trim()
            require(cleanName.length >= 2) { "Informe o nome do equipamento." }
            require(equipment.powerValue in 0.1..50000.0) { "Potência inválida." }
            val safeKind = EquipmentKind.fromWire(equipment.kind).wireValue
            val collectionRef = db.collection("users").document(uid).collection("equipment")
            val docRef = if (equipment.id.isBlank()) collectionRef.document() else collectionRef.document(equipment.id)
            docPath = docRef.path
            if (equipment.id.isBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "kind" to safeKind,
                        "name" to cleanName.take(140),
                        "powerValue" to equipment.powerValue,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "kind" to safeKind,
                        "name" to cleanName.take(140),
                        "powerValue" to equipment.powerValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteEquipment(equipmentId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/equipment/$equipmentId"
        return try {
            val uid = requireUserId()
            require(equipmentId.isNotBlank()) { "ID inválido." }
            val docRef = db.collection("users").document(uid).collection("equipment").document(equipmentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowFailure(
        error: Throwable,
        opType: OperationType,
        path: String,
        expectedUid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
        throw error
    }
}
