package com.als.presence.ui.dashboard.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.als.presence.data.local.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Local notification centre. Notifications are never stored in PostgreSQL:
 * the backend pushes them over SSE and Android persists them in Room so the
 * employee can read or clear them while offline.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val dao: NotificationDao
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = dao.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markAllRead() {
        viewModelScope.launch { dao.markAllRead() }
    }

    fun clearAll() {
        viewModelScope.launch { dao.clearAll() }
    }
}
