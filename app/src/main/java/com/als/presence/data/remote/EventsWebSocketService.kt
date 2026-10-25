package com.als.presence.data.remote

import android.util.Log
import com.als.presence.data.local.SessionManager
import com.als.presence.data.local.attendance.AttendanceDao
import com.als.presence.data.local.attendance.AttendanceEntity
import com.als.presence.data.local.notification.NotificationDao
import com.als.presence.data.local.notification.NotificationEntity
import com.als.presence.data.local.wallet.WalletDao
import com.als.presence.data.local.wallet.WalletEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class DomainWebSocketEvent {
    data class Notification(val id: String, val title: String, val message: String, val category: String) : DomainWebSocketEvent()
    data class WalletUpdate(val staffProfileId: String, val earnedCents: Long, val approvedCents: Long, val paidCents: Long) : DomainWebSocketEvent()
    data class AttendanceUpdate(val staffProfileId: String, val status: String, val clockIn: String?, val clockOut: String?) : DomainWebSocketEvent()
    data class ShiftUpdate(val action: String, val shiftId: String?) : DomainWebSocketEvent()
    data class PremisesUpdate(val action: String, val premisesId: String?) : DomainWebSocketEvent()
    data class StaffUpdate(val action: String, val staffId: String?) : DomainWebSocketEvent()
    data class Generic(val eventType: String, val payloadJson: String) : DomainWebSocketEvent()
}

@Singleton
class EventsWebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
    private val notificationDao: NotificationDao,
    private val walletDao: WalletDao,
    private val attendanceDao: AttendanceDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<DomainWebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DomainWebSocketEvent> = _events.asSharedFlow()

    @Volatile private var running = false
    @Volatile private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null

    fun start(baseUrl: String) {
        if (running) return
        running = true
        connect(baseUrl)
    }

    private fun connect(baseUrl: String) {
        scope.launch {
            val token = sessionManager.getAccessToken() ?: return@launch
            val wsUrl = convertToWsUrl(baseUrl, token)

            val request = Request.Builder()
                .url(wsUrl)
                .build()

            val client = okHttpClient.newBuilder()
                .pingInterval(25, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

            webSocket = client.newWebSocket(request, createWebSocketListener(baseUrl))
        }
    }

    private fun convertToWsUrl(baseUrl: String, token: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val wsScheme = if (cleanBase.startsWith("https://")) {
            cleanBase.replaceFirst("https://", "wss://")
        } else if (cleanBase.startsWith("http://")) {
            cleanBase.replaceFirst("http://", "ws://")
        } else {
            "ws://$cleanBase"
        }
        return "$wsScheme/api/v1/events/ws?token=$token"
    }

    private fun createWebSocketListener(baseUrl: String) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("EventsWebSocketService", "WebSocket connection opened")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                handleIncomingMessage(text)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("EventsWebSocketService", "WebSocket closing: $code / $reason")
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("EventsWebSocketService", "WebSocket closed: $code / $reason")
            scheduleReconnect(baseUrl)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w("EventsWebSocketService", "WebSocket failure", t)
            scheduleReconnect(baseUrl)
        }
    }

    private fun scheduleReconnect(baseUrl: String) {
        if (!running) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            if (running) {
                Log.d("EventsWebSocketService", "Reconnecting WebSocket...")
                connect(baseUrl)
            }
        }
    }

    fun stop() {
        running = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client signing out")
        webSocket = null
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            val root = JSONObject(text)
            val eventType = root.optString("event")
            val data = root.optJSONObject("data") ?: root.optJSONObject("payload") ?: JSONObject()

            when (eventType) {
                "notification" -> {
                    val id = data.optString("id", System.currentTimeMillis().toString())
                    val title = data.optString("title", "Notification")
                    val message = data.optString("body", data.optString("message", ""))
                    val category = data.optString("category", "SYSTEM")

                    saveNotification(id, title, message, category, text)
                    _events.emit(DomainWebSocketEvent.Notification(id, title, message, category))
                }

                "wallet_update" -> {
                    val staffProfileId = data.optString("staffProfileId")
                    if (staffProfileId.isNotBlank()) {
                        val earned = data.optLong("earnedBalanceCents", 0L)
                        val approved = data.optLong("approvedUnpaidBalanceCents", 0L)
                        val paid = data.optLong("paidBalanceCents", 0L)
                        val total = earned + approved + paid

                        walletDao.upsert(
                            WalletEntity(
                                staffProfileId = staffProfileId,
                                earnedBalanceCents = earned,
                                approvedUnpaidBalanceCents = approved,
                                paidBalanceCents = paid,
                                totalBalanceCents = total
                            )
                        )

                        saveNotification(
                            id = "wallet_${System.currentTimeMillis()}",
                            title = "Wallet Balance Updated",
                            message = "Earned: ₹${earned / 100.00}, Total Balance: ₹${total / 100.00}",
                            category = "PAYROLL",
                            rawJson = text
                        )

                        _events.emit(DomainWebSocketEvent.WalletUpdate(staffProfileId, earned, approved, paid))
                    }
                }

                "attendance_update" -> {
                    val staffProfileId = data.optString("staffProfileId")
                    val status = data.optString("status", "PRESENT")
                    val clockIn = if (data.isNull("clockIn")) null else data.optString("clockIn")
                    val clockOut = if (data.isNull("clockOut")) null else data.optString("clockOut")
                    val action = data.optString("action", "UPDATED")

                    if (staffProfileId.isNotBlank()) {
                        val record = AttendanceEntity(
                            id = data.optString("attendanceEventId", System.currentTimeMillis().toString()),
                            staffProfileId = staffProfileId,
                            status = status,
                            clockIn = clockIn,
                            clockOut = clockOut
                        )
                        attendanceDao.upsert(record)

                        val actionText = if (action == "CLOCK_IN") "Clock-In Recorded" else if (action == "CLOCK_OUT") "Clock-Out Recorded" else "Attendance Updated"
                        saveNotification(
                            id = "att_${System.currentTimeMillis()}",
                            title = actionText,
                            message = "Status: ${status.lowercase().replace('_', ' ')}",
                            category = "ATTENDANCE",
                            rawJson = text
                        )

                        _events.emit(DomainWebSocketEvent.AttendanceUpdate(staffProfileId, status, clockIn, clockOut))
                    }
                }

                "exception_raised" -> {
                    val exceptionId = data.optString("exceptionId", System.currentTimeMillis().toString())
                    val reason = data.optString("reason", "Attendance exception requiring review.")
                    val staffName = data.optString("staffName", "Staff member")

                    saveNotification(
                        id = "exc_${exceptionId}",
                        title = "Attendance Exception Raised",
                        message = "$staffName: $reason",
                        category = "ATTENDANCE",
                        rawJson = text
                    )
                    _events.emit(DomainWebSocketEvent.Generic("exception_raised", text))
                }

                "shift_update" -> {
                    val action = data.optString("action", "UPDATED")
                    val shiftId = data.optString("shiftTemplateId", data.optString("assignmentId", ""))

                    saveNotification(
                        id = "shift_${System.currentTimeMillis()}",
                        title = "Shift Schedule Updated",
                        message = "Working shift schedule $action.",
                        category = "SYSTEM",
                        rawJson = text
                    )

                    _events.emit(DomainWebSocketEvent.ShiftUpdate(action, shiftId))
                }

                "premises_update" -> {
                    val action = data.optString("action", "UPDATED")
                    val premisesId = data.optString("premisesId", "")

                    saveNotification(
                        id = "prem_${System.currentTimeMillis()}",
                        title = "Premises Updated",
                        message = "Workplace location settings updated.",
                        category = "SYSTEM",
                        rawJson = text
                    )

                    _events.emit(DomainWebSocketEvent.PremisesUpdate(action, premisesId))
                }

                "staff_update" -> {
                    val action = data.optString("action", "UPDATED")
                    val staffId = data.optString("staffId", "")

                    saveNotification(
                        id = "staff_${System.currentTimeMillis()}",
                        title = "Staff Roster Updated",
                        message = "Staff profile details updated.",
                        category = "SYSTEM",
                        rawJson = text
                    )

                    _events.emit(DomainWebSocketEvent.StaffUpdate(action, staffId))
                }

                "payroll_update" -> {
                    val status = data.optString("status", "UPDATED")
                    saveNotification(
                        id = "pay_${System.currentTimeMillis()}",
                        title = "Payroll Updated",
                        message = "Monthly payroll period is now $status.",
                        category = "PAYROLL",
                        rawJson = text
                    )
                    _events.emit(DomainWebSocketEvent.Generic("payroll_update", text))
                }

                else -> {
                    _events.emit(DomainWebSocketEvent.Generic(eventType, text))
                }
            }
        } catch (e: Exception) {
            Log.w("EventsWebSocketService", "Failed to handle message: $text", e)
        }
    }

    private suspend fun saveNotification(
        id: String,
        title: String,
        message: String,
        category: String,
        rawJson: String
    ) {
        val entity = NotificationEntity(
            id = id,
            title = title,
            message = message,
            type = category,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            payloadJson = rawJson
        )
        notificationDao.insert(entity)
    }
}
