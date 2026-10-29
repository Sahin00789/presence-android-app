package com.als.presence.data.remote

import android.util.Log
import com.als.presence.data.model.DomainWebSocketEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsWebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<DomainWebSocketEvent>()
    val events: SharedFlow<DomainWebSocketEvent> = _events.asSharedFlow()

    fun start(baseUrl: String) {
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "api/v1/events/ws"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("EventsWS", "WebSocket connected")
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("EventsWS", "Message received: $text")
                _events.tryEmit(DomainWebSocketEvent.Event(type = "MESSAGE", payload = text))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("EventsWS", "WebSocket error", t)
            }
        })
    }

    fun stop() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}
