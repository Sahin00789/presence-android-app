package com.als.presence.data.sync

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.room.withTransaction
import com.als.presence.data.local.AttendanceEntity
import com.als.presence.data.local.EmployeeEntity
import com.als.presence.data.local.HolidayEntity
import com.als.presence.data.local.LeaveRequestEntity
import com.als.presence.data.local.OvertimeEntity
import com.als.presence.data.local.PremisesEntity
import com.als.presence.data.local.PresenceDatabase
import com.als.presence.data.local.SessionManager
import com.als.presence.data.local.ShiftAssignmentEntity
import com.als.presence.data.local.ShiftEntity
import com.als.presence.data.local.SyncMetadataEntity
import com.als.presence.data.model.DomainWebSocketEvent
import com.als.presence.data.remote.AttendanceApiService
import com.als.presence.data.remote.EmployeeApiService
import com.als.presence.data.remote.EventsWebSocketService
import com.als.presence.data.remote.HolidayApiService
import com.als.presence.data.remote.LeaveApiService
import com.als.presence.data.remote.OvertimeApiService
import com.als.presence.data.remote.PayrollApiService
import com.als.presence.data.remote.PremisesApiService
import com.als.presence.data.remote.ShiftApiService
import com.als.presence.data.remote.ShiftsApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val db: PresenceDatabase,
    private val webSocketService: EventsWebSocketService,
    private val projectionEngine: SnapshotProjectionEngine,
    private val employeeApi: EmployeeApiService,
    private val shiftApi: ShiftApiService,
    private val shiftsTemplateApi: ShiftsApiService,
    private val premisesApi: PremisesApiService,
    private val attendanceApi: AttendanceApiService,
    private val overtimeApi: OvertimeApiService,
    private val holidayApi: HolidayApiService,
    private val leaveApi: LeaveApiService,
    private val payrollApi: PayrollApiService,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        observeWebSocketEvents()
    }

    fun startRealtimeSync(baseUrl: String) {
        webSocketService.start(baseUrl)
        scope.launch {
            performSync(forceFullSync = false)
        }
    }

    suspend fun performSync(forceFullSync: Boolean = false): Boolean {
        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress. Skipping.")
            return false
        }

        _isSyncing.value = true
        Log.d(TAG, "Starting synchronization handshake (forceFullSync=$forceFullSync)...")
        var overallSuccess = true

        try {
            // 1. Buffer incoming WebSocket events
            webSocketService.startBuffering()

            // 2. Obtain watermark timestamp
            val watermarkTimestamp = System.currentTimeMillis()

            // 3. Fetch data from REST APIs and perform Room transactional upserts
            db.withTransaction {
                // Fetch & store employees
                runCatching {
                    val resp = employeeApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val validDtos = dtos.filter { it.id.isNotBlank() }.distinctBy { it.id }
                            val entities = validDtos.map { EmployeeEntity.fromDto(it) }
                            db.employeeDao().deleteInvalidEntries()
                            db.employeeDao().upsertAll(entities)
                        }
                    } else {
                        overallSuccess = false
                    }
                }.onFailure {
                    overallSuccess = false
                    Log.e(TAG, "Error syncing employees to Room DB", it)
                }

                // Fetch & store premises
                runCatching {
                    val resp = premisesApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { PremisesEntity.fromDto(it) }
                            db.premisesDao().upsertAll(entities)
                        }
                    } else {
                        overallSuccess = false
                    }
                }.onFailure {
                    overallSuccess = false
                    Log.e(TAG, "Error syncing premises to Room DB", it)
                }

                // Fetch & store shifts
                runCatching {
                    val resp = shiftsTemplateApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { ShiftEntity.fromDto(it) }
                            db.shiftDao().upsertAll(entities)
                        }
                    } else {
                        overallSuccess = false
                    }
                }.onFailure {
                    overallSuccess = false
                    Log.e(TAG, "Error syncing shifts to Room DB", it)
                }

                runCatching {
                    val resp = shiftApi.listAssignments()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { ShiftAssignmentEntity.fromDto(it) }
                            db.shiftAssignmentDao().upsertAll(entities)
                        }
                    } else {
                        overallSuccess = false
                    }
                }.onFailure {
                    overallSuccess = false
                    Log.e(TAG, "Error syncing shift assignments to Room DB", it)
                }

                // Fetch & store attendance records
                runCatching {
                    val resp = attendanceApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { AttendanceEntity.fromDto(it) }
                            db.attendanceDao().upsertAll(entities)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error syncing attendance to Room DB", it)
                }

                // Fetch & store holidays
                runCatching {
                    val resp = holidayApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { HolidayEntity.fromDto(it) }
                            db.holidayDao().upsertAll(entities)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error syncing holidays to Room DB", it)
                }

                // Fetch & store leaves
                runCatching {
                    val resp = leaveApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { LeaveRequestEntity.fromDto(it) }
                            db.leaveRequestDao().upsertAll(entities)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error syncing leaves to Room DB", it)
                }

                // Fetch & store overtime
                runCatching {
                    val resp = overtimeApi.list()
                    if (resp.isSuccessful) {
                        resp.body()?.let { dtos ->
                            val entities = dtos.map { OvertimeEntity.fromDto(it) }
                            db.overtimeDao().upsertAll(entities)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error syncing overtime to Room DB", it)
                }

                // Update sync metadata
                val resources = listOf("employees", "premises", "shifts", "attendance", "holidays", "leaves", "overtime", "payroll")
                resources.forEach { res ->
                    db.syncMetadataDao().upsert(
                        SyncMetadataEntity(
                            resourceName = res,
                            lastSyncTimestamp = watermarkTimestamp,
                            syncStatus = if (overallSuccess) "SUCCESS" else "PARTIAL"
                        )
                    )
                }
            }

            // 4. Drain buffered WS events and replay them
            val bufferedEvents = webSocketService.drainBufferAndStopBuffering()
            Log.d(TAG, "Replaying ${bufferedEvents.size} buffered events...")
            bufferedEvents.forEach { event ->
                handleWebSocketEvent(event)
            }

            // 5. Rebuild snapshots
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            projectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
            projectionEngine.rebuildOwnerPingsSnapshot("ALL")
            projectionEngine.rebuildOwnerReportsSnapshot(month, year, null)

            val currentUserId = sessionManager.userId
            if (!currentUserId.isNullOrBlank()) {
                projectionEngine.rebuildEmployeeHomeSnapshot(currentUserId)
                projectionEngine.rebuildEmployeeAttendanceSnapshot(currentUserId, month, year)
                projectionEngine.rebuildEmployeeSalarySnapshot(currentUserId, month, year)
            }

            Log.d(TAG, "Synchronization handshake completed successfully.")
            return overallSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error during synchronization handshake", e)
            return false
        } finally {
            _isSyncing.value = false
        }
    }

    enum class SyncDomain {
        WORKFORCE,
        PREMISES,
        SHIFTS,
        ATTENDANCE,
        LOCATIONS,
        HOLIDAYS,
        LEAVES,
        OVERTIME,
        PAYROLL,
        FULL
    }

    suspend fun performSelectiveSync(domain: SyncDomain): Boolean {
        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress. Skipping selective sync for $domain.")
            return false
        }

        _isSyncing.value = true
        Log.d(TAG, "Starting selective sync for domain: $domain...")
        var success = true
        val watermark = System.currentTimeMillis()

        try {
            when (domain) {
                SyncDomain.WORKFORCE -> {
                    db.withTransaction {
                        runCatching {
                            val resp = employeeApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val validDtos = dtos.filter { it.id.isNotBlank() }.distinctBy { it.id }
                                    val entities = validDtos.map { EmployeeEntity.fromDto(it) }
                                    db.employeeDao().deleteInvalidEntries()
                                    db.employeeDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }

                        runCatching {
                            val resp = premisesApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { PremisesEntity.fromDto(it) }
                                    db.premisesDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                    projectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
                }
                SyncDomain.PREMISES -> {
                    db.withTransaction {
                        runCatching {
                            val resp = premisesApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { PremisesEntity.fromDto(it) }
                                    db.premisesDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                }
                SyncDomain.SHIFTS -> {
                    db.withTransaction {
                        runCatching {
                            val resp = shiftsTemplateApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { ShiftEntity.fromDto(it) }
                                    db.shiftDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }

                        runCatching {
                            val resp = shiftApi.listAssignments()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { ShiftAssignmentEntity.fromDto(it) }
                                    db.shiftAssignmentDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                }
                SyncDomain.ATTENDANCE, SyncDomain.LOCATIONS -> {
                    db.withTransaction {
                        runCatching {
                            val resp = attendanceApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { AttendanceEntity.fromDto(it) }
                                    db.attendanceDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                    val calendar = Calendar.getInstance()
                    val month = calendar.get(Calendar.MONTH) + 1
                    val year = calendar.get(Calendar.YEAR)
                    projectionEngine.rebuildOwnerPingsSnapshot("ALL")
                    val currentUserId = sessionManager.userId
                    if (!currentUserId.isNullOrBlank()) {
                        projectionEngine.rebuildEmployeeHomeSnapshot(currentUserId)
                        projectionEngine.rebuildEmployeeAttendanceSnapshot(currentUserId, month, year)
                    }
                }
                SyncDomain.HOLIDAYS -> {
                    db.withTransaction {
                        runCatching {
                            val resp = holidayApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { HolidayEntity.fromDto(it) }
                                    db.holidayDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                }
                SyncDomain.LEAVES -> {
                    db.withTransaction {
                        runCatching {
                            val resp = leaveApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { LeaveRequestEntity.fromDto(it) }
                                    db.leaveRequestDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                }
                SyncDomain.OVERTIME -> {
                    db.withTransaction {
                        runCatching {
                            val resp = overtimeApi.list()
                            if (resp.isSuccessful) {
                                resp.body()?.let { dtos ->
                                    val entities = dtos.map { OvertimeEntity.fromDto(it) }
                                    db.overtimeDao().upsertAll(entities)
                                }
                            } else success = false
                        }.onFailure { success = false }
                    }
                }
                SyncDomain.PAYROLL -> {
                    val calendar = Calendar.getInstance()
                    val month = calendar.get(Calendar.MONTH) + 1
                    val year = calendar.get(Calendar.YEAR)
                    projectionEngine.rebuildOwnerReportsSnapshot(month, year, null)
                    val currentUserId = sessionManager.userId
                    if (!currentUserId.isNullOrBlank()) {
                        projectionEngine.rebuildEmployeeSalarySnapshot(currentUserId, month, year)
                    }
                }
                SyncDomain.FULL -> {
                    return performSync(forceFullSync = true)
                }
            }

            db.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    resourceName = domain.name.lowercase(),
                    lastSyncTimestamp = watermark,
                    syncStatus = if (success) "SUCCESS" else "PARTIAL"
                )
            )

            Log.d(TAG, "Selective sync completed for domain $domain with status $success.")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error during selective sync for domain $domain", e)
            return false
        } finally {
            _isSyncing.value = false
        }
    }

    private fun observeWebSocketEvents() {
        scope.launch {
            webSocketService.events.collect { event ->
                handleWebSocketEvent(event)
            }
        }
    }

    private suspend fun handleWebSocketEvent(event: DomainWebSocketEvent) {
        Log.d(TAG, "Handling WebSocket event: $event (isOwnEvent=${event.isOwnEvent})")
        if (!event.isOwnEvent) {
            val toastMsg = event.toastMessage ?: "Real-time update received"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
            }
            performSync(forceFullSync = false)
        } else {
            when (event) {
                is DomainWebSocketEvent.StaffUpdate -> {
                    projectionEngine.rebuildOwnerEmployeesSnapshot("ALL")
                }
                is DomainWebSocketEvent.AttendanceUpdate -> {
                    projectionEngine.rebuildOwnerPingsSnapshot("ALL")
                }
                is DomainWebSocketEvent.ShiftUpdate -> {
                    performSync(forceFullSync = false)
                }
                is DomainWebSocketEvent.WalletUpdate -> {
                    val calendar = Calendar.getInstance()
                    val month = calendar.get(Calendar.MONTH) + 1
                    val year = calendar.get(Calendar.YEAR)
                    projectionEngine.rebuildOwnerReportsSnapshot(month, year, null)
                }
                else -> {
                    // Generic update
                }
            }
        }
    }

    companion object {
        private const val TAG = "SyncCoordinator"
    }
}
