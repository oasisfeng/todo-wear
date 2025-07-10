package com.oasisfeng.todo.wear.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.AndroidActivity
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.oasisfeng.todo.wear.TodoistAuthActivity
import com.oasisfeng.todo.wear.TokenManager
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
private const val TODOIST_PACKAGE = "com.todoist"

/**
 * Skeleton for a tile with no images.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun tileRequest(requestParams: TileRequest) = buildTile(this, requestParams)
    override suspend fun resourcesRequest(requestParams: ResourcesRequest) = resources(requestParams)
}

private suspend fun buildTile(context: Context, request: TileRequest): TileBuilders.Tile {
    val layout = fetchAndBuildLayout(context)
    return TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(materialScope(context,
            layout = layout, allowDynamicTheme = true, deviceConfiguration = request.deviceConfiguration)))
        .setFreshnessIntervalMillis(DEFAULT_FRESHNESS_INTERVAL_MILLIS).build()
}

private suspend fun fetchAndBuildLayout(context: Context): MaterialScope.() -> LayoutElementBuilders.LayoutElement {
    val token = TokenManager.getToken(context)
        ?: return authLayout(context)

    val repository = TodoistRepository.create()
    val result = repository.getTasks(token, MAX_NUM_TASKS, "date before: +4 hours & !no time")

    val tasks = result.getOrNull().orEmpty()
    return tasksLayout(context, tasks, LocalTime.now().let { LocalTime.of(it.hour, it.minute) })
}

private fun authLayout(context: Context): MaterialScope.() -> LayoutElementBuilders.LayoutElement = {
    primaryLayout(
        titleSlot = {
            text("Todoist".layoutString) },
        mainSlot = {
            text("You need to grant access permission on the paired device first.".layoutString, maxLines = 3) },
        bottomSlot = {
            textEdgeButton(labelContent = { text("Request".layoutString) }, colors = filledButtonColors(),
                onClick = clickable(launchAuthActivityAction(context))) }
    )
}

private fun launchAuthActivityAction(context: Context) = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
    AndroidActivity.Builder().setPackageName(context.packageName).setClassName(TodoistAuthActivity::class.java.name).build()).build()

@androidx.annotation.OptIn(ProtoLayoutExperimental::class)
private fun tasksLayout(context: Context, tasks: List<TodoistTask>, timeUpdated: LocalTime): MaterialScope.() -> LayoutElementBuilders.LayoutElement = {
    primaryLayout (
        titleSlot = { text(text = ("Tasks").layoutString, typography = Typography.BODY_SMALL) },
        mainSlot = { if (tasks.isEmpty()) text("No matched tasks.".layoutString) else column {
            setWidth(expand()); setHeight(expand())
            setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
            setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setPadding(Padding.Builder().setStart(DpProp.Builder(6f).build()).build())
                .setBackground(ModifiersBuilders.Background.Builder()
                    .setCorner(shapes.small).setColor(colorScheme.primaryContainer.prop).build())
                .setClickable(ModifiersBuilders.Clickable.Builder()
                    .setOnClick(launchAction(getTodoistLaunchActivity(context))).build())
                .build())

            val dateTimeFormatter by lazy { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
            val timeFormatter by lazy { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

            val ( tasksWithTime, tasksWithoutTime ) = tasks.partition { it.due?.date?.toLocalTime() != LocalTime.MIN }
            val tasksSorted = tasksWithTime.sortedBy { it.due!!.date } + tasksWithoutTime.sortedBy { it.day_order }

            tasksSorted.forEach { task ->
                val prefix = if (task.due != null) {
                    val due = task.due.date
                    if (due.toLocalDate().isEqual(LocalDate.now())) {       // Today
                        if (due.toLocalTime() <= LocalTime.MIN) ""                  //  without time
                        else due.toLocalTime().format(timeFormatter) + " "          //  with time
                    } else due.format(dateTimeFormatter) + " "
                } else ""

                var text = prefix + task.content
                if (! task.description.isNullOrEmpty()) text += " | " + task.description
                addContent(text(text.layoutString))
            }
        }},
        bottomSlot = { text("⟲ $timeUpdated".layoutString, typography = Typography.BODY_SMALL,
            modifier = LayoutModifier.clickable(loadAction())) },
        margins = PrimaryLayoutMargins.MID_PRIMARY_LAYOUT_MARGIN
    )
}

fun getTodoistLaunchActivity(context: Context)
= context.packageManager.getLaunchIntentForPackage(TODOIST_PACKAGE)?.component
    ?: ComponentName("com.todoist", "com.todoist.activity.HomeActivity")

@Preview(device = WearDevices.LARGE_ROUND, name = "Large Round")
fun tilePreview(context: Context) = TilePreviewData { TilePreviewHelper.singleTimelineEntryTileBuilder(
    materialScope(context = context, deviceConfiguration = it.deviceConfiguration, allowDynamicTheme = true,
        layout = tasksLayout(context, buildPreviewTasks(), LocalTime.of(9, 17))
    )).build()
}

@Preview(device = WearDevices.SMALL_ROUND, name = "Empty")
fun tilePreviewEmpty(context: Context) = TilePreviewData { TilePreviewHelper.singleTimelineEntryTileBuilder(
    materialScope(context = context, deviceConfiguration = it.deviceConfiguration, allowDynamicTheme = true,
        layout = tasksLayout(context, emptyList(), LocalTime.of(9, 17))
    )).build()
}

@Preview(device = WearDevices.SMALL_ROUND, name = "Authenticate")
fun tilePreviewAuth(context: Context) = TilePreviewData { TilePreviewHelper.singleTimelineEntryTileBuilder(
    materialScope(context = context, deviceConfiguration = it.deviceConfiguration, allowDynamicTheme = true,
        layout = authLayout(context)
    )).build()
}

private fun buildPreviewTasks() = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).let { today -> listOf(
    TodoistTask(content = "Yoga", description = "with neighbors",
        due = Due(date = today.plusHours(10), string = ""),
        priority = 1, day_order = 1, id = "1", project_id = "project1"),
    TodoistTask(content = "Buy milk🥛",
        due = Due(date = today, string = ""),
        priority = 1, day_order = 2, id = "1", project_id = "project2"),
    TodoistTask(content = "Clean the 1st floor",
        due = Due(date = today.plusHours(9).plusMinutes(30), string = ""),
        priority = 1, day_order = 3, id = "1", project_id = "project1"),
    TodoistTask(content = "Switch🪫of temp sensor",
        due = Due(date = today, string = ""),
        priority = 1, day_order = 4, id = "1", project_id = "project1"),
)}

private fun resources(@Suppress("unused") r: ResourcesRequest)
= ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()

private fun column(builder: Column.Builder.() -> Unit) = Column.Builder().apply(builder).build()
