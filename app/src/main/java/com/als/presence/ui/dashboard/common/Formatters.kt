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
 * Formatting helpers shared by every dashboard. Money is always handled in the
 * backend's minor currency units (paise/cents) and only converted for display —
 * never for arithmetic (`AGENTS.md` §6.2).
 */
object Money {
    fun format(cents: Long?, withDecimals: Boolean = true): String {
        val value = cents ?: return "—"
        val negative = value < 0
        val absolute = if (negative) -value else value
        val whole = absolute / 100
        val fraction = absolute % 100
        val grouped = whole.toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        val body = if (withDecimals) {
            "$grouped.${fraction.toString().padStart(2, '0')}"
        } else {
            grouped
        }
        return if (negative) "-₹$body" else "₹$body"
    }

    /** Parses a human currency string ("12,500.50") into minor units. */
    fun parseToCents(raw: String): Long? {
        val cleaned = raw.replace("₹", "").replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        return cleaned.toBigDecimalOrNull()
            ?.multiply(java.math.BigDecimal(100))
            ?.setScale(0, java.math.RoundingMode.HALF_UP)
            ?.toLong()
    }
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
        "LATE", "HALF_DAY", "PENDING", "REQUESTED", "OPEN", "CALCULATED" -> TertiaryAmber
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
