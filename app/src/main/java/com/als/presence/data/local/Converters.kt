package com.als.presence.data.local

import androidx.room.TypeConverter
import com.als.presence.data.model.AttendanceSummaryDto
import com.als.presence.data.model.GpsCountsDto
import com.als.presence.data.model.GpsPingSummaryDto
import com.als.presence.data.model.PaymentRecordDto
import com.als.presence.data.model.PayrollBreakdownDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromDoubleList(value: List<Double>?): String {
        if (value == null) return "[]"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toDoubleList(value: String?): List<Double> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Double>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromPingSummaryList(value: List<GpsPingSummaryDto>?): String {
        if (value == null) return "[]"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toPingSummaryList(value: String?): List<GpsPingSummaryDto> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<GpsPingSummaryDto>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromGpsCountsDto(value: GpsCountsDto?): String {
        if (value == null) return "{}"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toGpsCountsDto(value: String?): GpsCountsDto {
        if (value.isNullOrBlank()) return GpsCountsDto()
        return runCatching { json.decodeFromString<GpsCountsDto>(value) }.getOrDefault(GpsCountsDto())
    }

    @TypeConverter
    fun fromAttendanceSummaryDto(value: AttendanceSummaryDto?): String {
        if (value == null) return "{}"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toAttendanceSummaryDto(value: String?): AttendanceSummaryDto {
        if (value.isNullOrBlank()) return AttendanceSummaryDto()
        return runCatching { json.decodeFromString<AttendanceSummaryDto>(value) }.getOrDefault(AttendanceSummaryDto())
    }

    @TypeConverter
    fun fromPayrollBreakdownDto(value: PayrollBreakdownDto?): String {
        if (value == null) return "{}"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toPayrollBreakdownDto(value: String?): PayrollBreakdownDto {
        if (value.isNullOrBlank()) return PayrollBreakdownDto()
        return runCatching { json.decodeFromString<PayrollBreakdownDto>(value) }.getOrDefault(PayrollBreakdownDto())
    }

    @TypeConverter
    fun fromPaymentRecordDto(value: PaymentRecordDto?): String {
        if (value == null) return ""
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toPaymentRecordDto(value: String?): PaymentRecordDto? {
        if (value.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<PaymentRecordDto>(value) }.getOrNull()
    }
}
