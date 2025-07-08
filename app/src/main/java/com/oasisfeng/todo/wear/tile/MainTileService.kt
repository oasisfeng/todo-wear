package com.oasisfeng.todo.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.oasisfeng.todo.wear.TokenManager
import com.oasisfeng.todo.wear.TodoistAuthActivity
import com.oasisfeng.todo.wear.data.Due
import com.oasisfeng.todo.wear.data.TodoistRepository
import com.oasisfeng.todo.wear.data.TodoistTask
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MAX_NUM_TASKS = 8         // Max number of tasks to show in the tile
private const val DEFAULT_FRESHNESS_INTERVAL_MILLIS = 30 * 60_000L
private const val RESOURCES_VERSION = "0"

/**
 * Skeleton for a tile with no images.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun tileRequest(requestParams: TileRequest) = buildTile(this, requestParams)
    override suspend fun resourcesRequest(requestParams: ResourcesRequest) = resources(requestParams)
}

private suspend fun buildTile(context: Context, request: TileRequest): TileBuilders.Tile {
    val timeline = buildTimeline(context, request)
    return TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(timeline).setFreshnessIntervalMillis(DEFAULT_FRESHNESS_INTERVAL_MILLIS).build()
}

private suspend fun buildTimeline(context: Context, request: TileRequest): TimelineBuilders.Timeline {
    val token = TokenManager.getToken(context)
    if (token.isNullOrEmpty())
        return TimelineBuilders.Timeline.fromLayoutElement(loginElement(request, context))

    val repository = TodoistRepository.create()
    val result = repository.getTasks(token, MAX_NUM_TASKS, "date before: +4 hours & !no time")

    val tasks = result.getOrNull().orEmpty()
    return buildTimelineForTasks(context, tasks)
}

private fun buildTimelineForTasks(context: Context, tasks: List<TodoistTask>): TimelineBuilders.Timeline {
    if (tasks.isEmpty())
        return TimelineBuilders.Timeline.fromLayoutElement(textElement(context, "No matched tasks."))

    val timeFormatter by lazy { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateTimeFormatter by lazy { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    var isFirst = true

    val timeline = TimelineBuilders.Timeline.Builder()
    val column = Column.Builder()
    val ( tasksWithTime, tasksWithoutTime ) = tasks.partition { it.due?.date?.toLocalTime() != LocalTime.MIN }
    val tasksSorted = tasksWithTime.sortedBy { it.due!!.date } + tasksWithoutTime.sortedBy { it.day_order }
    tasksSorted.forEach { task ->
        if (isFirst) isFirst = false
        else column.addContent(Spacer.Builder().setHeight(dp(8f)).build())

        val prefix = if (task.due != null) {
            val due = task.due.date
            if (due.toLocalDate().isEqual(LocalDate.now())) {   // Today
                if (due.toLocalTime() <= LocalTime.MIN) null            //  without time
                else due.toLocalTime().format(timeFormatter)            //  with time
            } else due.format(dateTimeFormatter)
        } else null

        column.addContent(textElement(context, if (prefix != null) "$prefix ${task.content}" else task.content))
        if (! task.description.isNullOrEmpty())
            column.addContent(textElement(context, task.description, Typography.TYPOGRAPHY_BODY2))
    }

    timeline.addTimelineEntry(TimelineEntry.Builder().setLayout(Layout.Builder().setRoot(column.build()).build()).build())
    return timeline.build()
}

private fun loginElement(request: TileRequest, context: Context): LayoutElementBuilders.LayoutElement
= CompactChip.Builder(context, "Login", Clickable.Builder().setOnClick(ActionBuilders.LaunchAction.Builder()
    .setAndroidActivity(ActionBuilders.AndroidActivity.Builder()
        .setPackageName(context.packageName).setClassName(TodoistAuthActivity::class.java.name).build()).build()
).build(), request.deviceConfiguration).build()

fun textElement(context: Context, text: String, typography: Int = Typography.TYPOGRAPHY_BODY1)
= Text.Builder(context, text).setTypography(typography).setColor(argb(Colors.DEFAULT.onSurface)).build()

@Preview(device = WearDevices.SMALL_ROUND) @Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(onTileResourceRequest = ::resources) { request ->
    val tasks = listOf(
        TodoistTask(content = "Yoga", description = "with neighbors",
            due = Due(date = LocalDateTime.now().minusHours(2), string = ""),
            priority = 1, day_order = 1, id = "1", project_id = "project1"),
        TodoistTask(content = "Buy milk",
            due = Due(date = LocalDateTime.now().plusMinutes(30), string = ""),
            priority = 1, day_order = 1, id = "1", project_id = "project2"),
        TodoistTask(content = "Clean the 1st floor",
            due = Due(date = LocalDateTime.now().plusHours(5), string = ""),
            priority = 1, day_order = 1, id = "1", project_id = "project1"))
    TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(buildTimelineForTasks(context, tasks)).build()
}

private fun resources(@Suppress("unused") r: ResourcesRequest)
= ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
