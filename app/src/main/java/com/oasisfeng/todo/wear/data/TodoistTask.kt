package com.oasisfeng.todo.wear.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class TodoistTask(
    val id: String,
    val project_id: String?,
    val section_id: String? = null,
    val parent_id: String? = null,
    val content: String,
    val description: String? = null,
    val labels: List<String> = emptyList(),
    val priority: Int,
    val day_order: Int,
    val due: Due? = null
)

@Serializable
data class Due(
    @Serializable(with = DueDateSerializer::class)
    val date: LocalDateTime,
    val timezone: String? = null,
    val is_recurring: Boolean = false,
    val string: String
)

class DueDateSerializer : KSerializer<LocalDateTime> {

    override fun deserialize(decoder: Decoder): LocalDateTime = decoder.decodeString().let {
        if (it.length == 10) LocalDateTime.of(LocalDate.parse(it), LocalTime.MIN)   // YYYY-MM-DD
        else LocalDateTime.parse(it)
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime)
    = encoder.encodeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

    override val descriptor = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)
}