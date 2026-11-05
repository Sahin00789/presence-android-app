package com.als.presence.data.remote

import android.util.Log
import com.als.presence.data.local.SessionManager
import com.als.presence.data.model.DomainWebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsWebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) {
    private var webSocket: WebSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _events = MutableSharedFlow<DomainWebSocketEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<DomainWebSocketEvent> = _events.asSharedFlow()

    private var lastBaseUrl: String? = null
    private var isExplicitlyStopped = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null

    private var isBuffering = false
    private val bufferedEventsQueue = ConcurrentLinkedQueue<DomainWebSocketEvent>()

    fun startBuffering() {
        isBuffering = true
        bufferedEventsQueue.clear()
        Log.d("EventsWS", "Started buffering WebSocket events for sync handshake")
    }

    fun drainBufferAndStopBuffering(): List<DomainWebSocketEvent> {
        isBuffering = false
        val list = mutableListOf<DomainWebSocketEvent>()
        while (bufferedEventsQueue.isNotEmpty()) {
            bufferedEventsQueue.poll()?.let { list.add(it) }
        }
        Log.d("EventsWS", "Stopped buffering and drained ${list.size} events")
        return list
    }

    fun start(baseUrl: String) {
        lastBaseUrl = baseUrl
        isExplicitlyStopped = false
        reconnectJob?.cancel()
        connectInternal(baseUrl)
    }

    private fun connectInternal(baseUrl: String) {
        if (webSocket != null) {
            try {
                webSocket?.close(1000, "Reconnecting")
            } catch (e: Exception) {
                Log.w("EventsWS", "Error closing old socket", e)
            }
            webSocket = null
        }

        val formattedBaseUrl = if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        val wsUrl = formattedBaseUrl.replace("http://", "ws://").replace("https://", "wss://") + "api/v1/events/ws"
        val request = Request.Builder().url(wsUrl).build()

        Log.d("EventsWS", "Connecting to WebSocket: $wsUrl")
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("EventsWS", "WebSocket connected successfully")
                _isConnected.value = true
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("EventsWS", "Message received: $text")
                try {
                    val json = JSONObject(text)
                    val eventType = json.optString("event", json.optString("type", "message"))

                    if (eventType.equals("pong", ignoreCase = true) ||
                        eventType.equals("connected", ignoreCase = true) ||
                        eventType.equals("subscribed", ignoreCase = true) ||
                        eventType.equals("unsubscribed", ignoreCase = true)
                    ) {
                        Log.d("EventsWS", "Received control message: $eventType")
                        return
                    }

                    val dataObj = json.optJSONObject("data")

                    val actorId = dataObj?.optString("actorId")?.takeIf { it.isNotBlank() }
                        ?: dataObj?.optString("actorUserId")?.takeIf { it.isNotBlank() }
                        ?: dataObj?.optString("userId")?.takeIf { it.isNotBlank() }
                        ?: dataObj?.optString("createdBy")?.takeIf { it.isNotBlank() }
                        ?: dataObj?.optString("triggeredBy")?.takeIf { it.isNotBlank() }
                        ?: dataObj?.optString("performedBy")?.takeIf { it.isNotBlank() }
                        ?: json.optString("actorId").takeIf { it.isNotBlank() }
                        ?: json.optString("userId").takeIf { it.isNotBlank() }

                    val currentUserId = sessionManager.userId

                    val isOwnEvent = if (!actorId.isNullOrBlank() && !currentUserId.isNullOrBlank()) {
                        actorId.equals(currentUserId, ignoreCase = true)
                    } else {
                        false
                    }

                    val title = dataObj?.optString("title")?.takeIf { it.isNotBlank() }
                    val body = dataObj?.optString("body")?.takeIf { it.isNotBlank() }
                    val messageStr = dataObj?.optString("message")?.takeIf { it.isNotBlank() }
                    val action = dataObj?.optString("action")?.takeIf { it.isNotBlank() }
                    val status = dataObj?.optString("status")?.takeIf { it.isNotBlank() }
                    val reason = dataObj?.optString("reason")?.takeIf { it.isNotBlank() }

                    val customToastText = when {
                        !title.isNullOrBlank() && !body.isNullOrBlank() -> "$title: $body"
                        !title.isNullOrBlank() -> title
                        !body.isNullOrBlank() -> body
                        !messageStr.isNullOrBlank() -> messageStr
                        !reason.isNullOrBlank() -> "Alert: $reason"
                        eventType.contains("premises", ignoreCase = true) -> "Premises updated${if (!action.isNullOrBlank()) ": $action" else ""}"
                        eventType.contains("attendance", ignoreCase = true) -> "Attendance update${if (!action.isNullOrBlank() || !status.isNullOrBlank()) ": ${action ?: status}" else ""}"
                        eventType.contains("staff", ignoreCase = true) || eventType.contains("employee", ignoreCase = true) -> "Staff details updated${if (!action.isNullOrBlank()) ": $action" else ""}"
                        eventType.contains("shift", ignoreCase = true) -> "Shift update${if (!action.isNullOrBlank()) ": $action" else ""}"
                        eventType.contains("wallet", ignoreCase = true) || eventType.contains("ledger", ignoreCase = true) || eventType.contains("salary", ignoreCase = true) -> "Salary & ledger updated"
                        else -> "Real-time update received"
                    }

                    val domainEvent = when {
                        eventType.contains("premises", ignoreCase = true) ->
                            DomainWebSocketEvent.PremisesUpdate(isOwnEvent = isOwnEvent, toastMessage = customToastText)
                        eventType.contains("attendance", ignoreCase = true) ->
                            DomainWebSocketEvent.AttendanceUpdate(isOwnEvent = isOwnEvent, toastMessage = customToastText)
                        eventType.contains("staff", ignoreCase = true) || eventType.contains("employee", ignoreCase = true) ->
                            DomainWebSocketEvent.StaffUpdate(isOwnEvent = isOwnEvent, toastMessage = customToastText)
                        eventType.contains("shift", ignoreCase = true) ->
                            DomainWebSocketEvent.ShiftUpdate(isOwnEvent = isOwnEvent, toastMessage = customToastText)
                        eventType.contains("wallet", ignoreCase = true) || eventType.contains("ledger", ignoreCase = true) || eventType.contains("salary", ignoreCase = true) ->
                            DomainWebSocketEvent.WalletUpdate(isOwnEvent = isOwnEvent, toastMessage = customToastText)
                        else ->
                            DomainWebSocketEvent.Generic(isOwnEvent = isOwnEvent, toastMessage = customToastText)
                    }

                    if (isBuffering) {
                        bufferedEventsQueue.add(domainEvent)
                    } else {
                        _events.tryEmit(domainEvent)
                    }
                } catch (e: Exception) {
                    Log.w("EventsWS", "Failed to parse JSON event message", e)
                    val fallback = DomainWebSocketEvent.Generic(isOwnEvent = false, toastMessage = "Real-time update received")
                    if (isBuffering) {
                        bufferedEventsQueue.add(fallback)
                    } else {
                        _events.tryEmit(fallback)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("EventsWS", "WebSocket closing: $code / $reason")
                _isConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("EventsWS", "WebSocket closed: $code / $reason")
                _isConnected.value = false
                if (!isExplicitlyStopped) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("EventsWS", "WebSocket failure: ${t.localizedMessage}", t)
                _isConnected.value = false
                if (!isExplicitlyStopped) {
                    scheduleReconnect()
                }
            }
        })
    }

    fun reconnect() {
        val url = lastBaseUrl ?: PRESENCE_BASE_URL
        start(url)
    }

    private fun scheduleReconnect() {
        if (isExplicitlyStopped) return
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            delay(5000)
            if (!isExplicitlyStopped && !_isConnected.value) {
                Log.d("EventsWS", "Executing scheduled WebSocket reconnection...")
                val url = lastBaseUrl ?: PRESENCE_BASE_URL
                connectInternal(url)
            }
        }
    }

    fun ping() {
        try {
            webSocket?.send("""{"action":"ping"}""")
        } catch (e: Exception) {
            Log.w("EventsWS", "Failed to send ping", e)
        }
    }

    fun stop() {
        isExplicitlyStopped = true
        _isConnected.value = false
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "Client disconnect")
        } catch (e: Exception) {
            Log.w("EventsWS", "Error closing socket on stop", e)
        }
        webSocket = null
    }
}
