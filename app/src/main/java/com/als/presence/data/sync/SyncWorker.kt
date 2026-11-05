package com.als.presence.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.als.presence.data.local.PresenceDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: PresenceDatabase,
    private val projectionEngine: SnapshotProjectionEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting SyncWorker background offline pending actions processing...")
        val pendingActions = db.pendingActionDao().getPendingActions()
        if (pendingActions.isEmpty()) {
            Log.d(TAG, "No pending offline actions found.")
            return Result.success()
        }

        var anyFailed = false
        for (action in pendingActions) {
            try {
                db.pendingActionDao().updateStatus(action.id, "SYNCING")
                Log.d(TAG, "Processing action: ${action.actionType} (${action.id})")

                // Execute action according to type
                when (action.actionType) {
                    "CLOCK_IN", "CLOCK_OUT" -> {
                        // Clock in/out requires live server verification per policy rule
                        db.pendingActionDao().updateStatus(action.id, "FAILED", "Live server required")
                    }
                    else -> {
                        db.pendingActionDao().updateStatus(action.id, "SYNCED")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing pending action ${action.id}", e)
                db.pendingActionDao().updateStatus(action.id, "FAILED", e.localizedMessage)
                anyFailed = true
            }
        }

        db.pendingActionDao().clearSynced()
        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
