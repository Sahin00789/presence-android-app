package com.als.presence.core.location

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager fallback for active-shift GPS checks. The foreground service
 * performs the ~10-minute cadence; this worker guarantees a check even if the
 * service was killed (WorkManager's minimum periodic interval is 15 minutes).
 */
@HiltWorker
class LocationMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val monitor: ActiveShiftLocationMonitor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        monitor.runOnce()
        return Result.success()
    }
}
