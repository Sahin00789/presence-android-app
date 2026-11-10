package com.als.presence.ui.dashboard.common

import androidx.compose.ui.graphics.Color
import com.als.presence.core.designsystem.SecondaryEmerald
import com.als.presence.core.designsystem.TertiaryAmber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Formatting helpers shared by every dashboard. Money is always handled in Indian Rupees (INR / ₹)
 * as exact whole integer units (no decimal fraction, no paise, no cents: e.g. ₹12,500).
 */
object Money {
    fun format(amount: Long?, withDecimals: Boolean = false): String {
        val value = amount ?: return "—"
        val negative = value < 0
        val absolute = if (negative) -value else value
        return if (negative) "-₹$absolute" else "₹$absolute"
    }

    /** Parses a raw number string into integer INR rupees. */
    fun parseToInr(raw: String): Long? {
        val cleaned = raw.replace("₹", "").replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        return cleaned.toBigDecimalOrNull()
            ?.setScale(0, java.math.RoundingMode.HALF_UP)
            ?.toLong()
    }

    fun parseToPaise(raw: String): Long? = parseToInr(raw)
    fun parseToCents(raw: String): Long? = parseToInr(raw)
}

object Dates {
    private val display = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val displayShort = DateTimeFormatter.ofPattern("dd MMM")
    private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a")

    fun todayIso(): String = LocalDate.now().toString()

    fun isoDaysAgo(days: Long): String = LocalDate.now().minusDays(days).toString()

    fun isoDaysAhead(days: Long): String = LocalDate.now().plusDays(days).toString()

    fun monthStartIso(): String = LocalDate.now().withDayOfMonth(1).toString()

    fun formatDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            LocalDate.parse(iso.take(10)).format(display)
        } catch (_: Exception) {
            iso.take(10)
        }
    }

    fun formatDateShort(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            LocalDate.parse(iso.take(10)).format(displayShort)
        } catch (_: Exception) {
            iso.take(10)
        }
    }

    fun formatTimestamp(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val parsed = parseInstant(raw) ?: return raw.take(16).replace('T', ' ')
        val local = parsed.atZone(ZoneId.systemDefault())
        return "${local.format(display)} • ${local.format(timeFormat)}"
    }

    fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        if (raw.contains('T')) {
            val parsed = parseInstant(raw) ?: return raw
            return parsed.atZone(ZoneId.systemDefault()).format(timeFormat)
        }
        return try {
            LocalTime.parse(raw.take(8)).format(timeFormat)
        } catch (_: Exception) {
            raw
        }
    }

    /** Rolls a `HH:mm[:ss]` shift window into readable text. */
    fun formatWindow(start: String?, end: String?): String {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return "—"
        return "${formatTime(start)} – ${formatTime(end)}"
    }

    fun formatMinutes(minutes: Int?): String {
        val total = minutes ?: 0
        if (total <= 0) return "0h 0m"
        val hours = total / 60
        val rest = total % 60
        return "${hours}h ${rest}m"
    }

    fun dayName(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            LocalDate.parse(iso.take(10)).dayOfWeek.name.lowercase()
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Fetches full English day name (e.g. "Monday", "Tuesday") by date.
     * If [dateIso] is null or blank, defaults to the current day.
     */
    fun getDayNameByDate(dateIso: String? = null): String {
        val targetDate = if (dateIso.isNullOrBlank()) {
            LocalDate.now()
        } else {
            try {
                LocalDate.parse(dateIso.take(10))
            } catch (_: Exception) {
                LocalDate.now()
            }
        }
        return targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * Checks if a shift's assigned [shiftDayName] or [daysOfWeek] list matches the target date.
     * Returns `true` if the shift applies to [targetDateIso]'s day of week.
     */
    fun isShiftMatchingDay(
        shiftDayName: String?,
        targetDateIso: String? = null,
        daysOfWeek: List<Int>? = null
    ): Boolean {
        val dayName = getDayNameByDate(targetDateIso)
        val dn = shiftDayName?.trim() ?: ""

        if (dn.isEmpty() || dn.equals("ALL", ignoreCase = true) ||
            dn.equals("Everyday", ignoreCase = true) || dn.equals("Every Day", ignoreCase = true)
        ) {
            return true
        }

        if (!daysOfWeek.isNullOrEmpty()) {
            val targetLocalDate = if (targetDateIso.isNullOrBlank()) LocalDate.now() else runCatching { LocalDate.parse(targetDateIso.take(10)) }.getOrDefault(LocalDate.now())
            val javaDow = targetLocalDate.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
            val dow0Based = if (javaDow == 7) 0 else javaDow // 0 (Sun) .. 6 (Sat)
            if (daysOfWeek.contains(javaDow) || daysOfWeek.contains(dow0Based)) {
                return true
            }
        }

        val shortDayName = dayName.take(3)
        return dn.contains(dayName, ignoreCase = true) || dn.contains(shortDayName, ignoreCase = true)
    }

    /**
     * Checks if a shift matches the specified day and its time window is currently running or upcoming.
     */
    fun isShiftMatchingRunningOrUpcoming(
        shiftDayName: String?,
        startTime: String?,
        endTime: String?,
        targetDateIso: String? = null,
        daysOfWeek: List<Int>? = null,
        bufferMinutesBeforeStart: Long = 120L,
        bufferMinutesAfterEnd: Long = 120L
    ): Boolean {
        val matchesDay = isShiftMatchingDay(shiftDayName, targetDateIso, daysOfWeek)
        if (!matchesDay) return false
        return isWithinTimeWindow(
            startTimeRaw = startTime,
            endTimeRaw = endTime,
            bufferMinutesBeforeStart = bufferMinutesBeforeStart,
            bufferMinutesAfterEnd = bufferMinutesAfterEnd
        )
    }

    fun isToday(iso: String?): Boolean {
        if (iso.isNullOrBlank()) return false
        return iso.take(10) == todayIso()
    }

    fun cleanPremisesName(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return raw.split(",", limit = 2)[0].trim()
    }

    fun formatDaysOfWeek(days: List<Int>?): String {
        if (days.isNullOrEmpty()) return "All Days"
        if (days.size == 7) return "Everyday"
        val sorted = days.distinct().sorted()
        if (sorted == listOf(1, 2, 3, 4, 5)) return "Mon – Fri"
        if (sorted == listOf(0, 6)) return "Sat & Sun"
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return sorted.mapNotNull { dayLabels.getOrNull(it) }.joinToString(", ")
    }

    /**
     * Checks if current time has passed the half-day threshold for a shift.
     * If [halfDayThresholdRaw] is provided (e.g. "11:00:00"), compares against that.
     * Otherwise defaults to 4 hours (240 minutes) after [startTimeRaw].
     */
    fun isPastHalfDayThreshold(startTimeRaw: String?, halfDayThresholdRaw: String? = null): Boolean {
        if (startTimeRaw.isNullOrBlank()) return false
        return try {
            val now = LocalTime.now()
            val parseTime = { raw: String ->
                if (raw.contains('T')) {
                    OffsetDateTime.parse(raw).toLocalTime()
                } else {
                    LocalTime.parse(raw.take(8))
                }
            }
            val start = parseTime(startTimeRaw)
            val threshold = if (!halfDayThresholdRaw.isNullOrBlank()) {
                parseTime(halfDayThresholdRaw)
            } else {
                start.plusHours(4)
            }
            if (start.isBefore(threshold)) {
                now.isAfter(threshold)
            } else {
                now.isAfter(threshold) || now.isBefore(start)
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if a session (shift or overtime) on [workDateIso] has completed its scheduled time window.
     * Returns true if the session date is in the past, or if it is today and current time has passed
     * the session's end time.
     */
    fun isSessionOver(
        workDateIso: String?,
        endTimeRaw: String?,
        startTimeRaw: String? = null
    ): Boolean {
        if (workDateIso.isNullOrBlank()) return false
        val today = todayIso()
        val sessionDate = workDateIso.take(10)
        if (sessionDate > today) return false

        if (endTimeRaw.isNullOrBlank()) return false
        return try {
            val now = LocalTime.now()
            val parseTime = { raw: String ->
                if (raw.contains('T')) {
                    OffsetDateTime.parse(raw).toLocalTime()
                } else {
                    LocalTime.parse(raw.take(8))
                }
            }
            val end = parseTime(endTimeRaw)
            val start = startTimeRaw?.let { runCatching { parseTime(it) }.getOrNull() }

            val isOvernight = start != null && start.isAfter(end)
            when {
                sessionDate < today -> {
                    if (isOvernight && sessionDate == isoDaysAgo(1)) {
                        now.isAfter(end)
                    } else {
                        true
                    }
                }
                else -> { // sessionDate == today
                    if (isOvernight) {
                        false // Overnight shift starting today ends tomorrow
                    } else {
                        now.isAfter(end)
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if current time is within specified shift/overtime window
     * with an optional buffer before start time and after end time (in minutes).
     */
    fun isWithinTimeWindow(
        startTimeRaw: String?,
        endTimeRaw: String?,
        bufferMinutesBeforeStart: Long = 120L,
        bufferMinutesAfterEnd: Long = 120L
    ): Boolean {
        if (startTimeRaw.isNullOrBlank() || endTimeRaw.isNullOrBlank()) {
            return true
        }
        return try {
            val now = LocalTime.now()

            val parseTime = { raw: String ->
                if (raw.contains('T')) {
                    OffsetDateTime.parse(raw).toLocalTime()
                } else {
                    LocalTime.parse(raw.take(8))
                }
            }

            val start = parseTime(startTimeRaw)
            val end = parseTime(endTimeRaw)

            val windowStart = start.minusMinutes(bufferMinutesBeforeStart)
            val windowEnd = end.plusMinutes(bufferMinutesAfterEnd)

            if (windowStart.isAfter(windowEnd)) {
                now.isAfter(windowStart) || now.isBefore(windowEnd)
            } else {
                !now.isBefore(windowStart) && !now.isAfter(windowEnd)
            }
        } catch (_: Exception) {
            true
        }
    }

    /** Calculates late minutes given a clock-in time and assigned shift start time. */
    fun calculateLateMinutes(clockIn: String?, assignedStartTime: String?): Int {
        if (clockIn.isNullOrBlank() || assignedStartTime.isNullOrBlank()) return 0
        return try {
            val clockInTime = if (clockIn.contains('T')) {
                val parsed = parseInstant(clockIn) ?: return 0
                parsed.atZone(ZoneId.systemDefault()).toLocalTime()
            } else {
                LocalTime.parse(clockIn.take(8))
            }
            val startTime = LocalTime.parse(assignedStartTime.take(8))
            val diffSeconds = clockInTime.toSecondOfDay() - startTime.toSecondOfDay()
            if (diffSeconds > 0) diffSeconds / 60 else 0
        } catch (_: Exception) {
            0
        }
    }

    /** Formats late minutes into human-readable duration text (e.g. "Late: 15 mins", "Late: 1 hr 10 mins"). */
    fun formatLateMinutes(minutes: Int): String {
        if (minutes <= 0) return ""
        val hrs = minutes / 60
        val mins = minutes % 60
        return when {
            hrs == 0 -> "Late: $mins mins"
            mins == 0 -> "Late: $hrs hr"
            else -> "Late: $hrs hr $mins mins"
        }
    }

    /** Helper to format late duration directly from clock-in and assigned start time. */
    fun formatLateText(clockIn: String?, assignedStartTime: String?, lateMinutes: Int = 0): String {
        if (lateMinutes > 0) return formatLateMinutes(lateMinutes)
        val mins = calculateLateMinutes(clockIn, assignedStartTime)
        return formatLateMinutes(mins)
    }

    private fun parseInstant(raw: String): Instant? = try {
        OffsetDateTime.parse(raw).toInstant()
    } catch (_: DateTimeParseException) {
        try {
            Instant.parse(raw)
        } catch (_: Exception) {
            null
        }
    }
}

object StatusStyle {
    fun color(status: String?): Color = when (status?.uppercase()) {
        "PRESENT", "APPROVED", "COMPLETED", "PAID", "ACTIVE", "IN_PROGRESS" -> SecondaryEmerald
        "HALF_DAY", "PENDING", "REQUESTED", "OPEN", "CALCULATED" -> TertiaryAmber
        "LEAVE", "LEAVE_REQUESTED" -> Color(0xFF0284C7)
        "ABSENT", "REJECTED", "CANCELLED", "FAILED" -> Color(0xFFDC2626)
        else -> Color(0xFF64748B)
    }

    fun label(status: String?): String = when (status?.uppercase()) {
        "IN_PROGRESS" -> "In progress"
        "HALF_DAY" -> "Half day"
        else -> (status ?: "Unknown")
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
    }
}
