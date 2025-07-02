package com.oasisfeng.todo.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
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
import com.oasisfeng.todo.wear.data.TodoistRepository
import kotlinx.coroutines.runBlocking

private const val RESOURCES_VERSION = "0"

/**
 * Skeleton for a tile with no images.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(requestParams: ResourcesRequest) = resources(requestParams)
    override suspend fun tileRequest(requestParams: TileRequest) = tile(this, requestParams)
}

private fun resources(requestParams: ResourcesRequest): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
}

private data class TodoItem(val title: String, val secondaryText: String)

private suspend fun tile(context: Context, requestParams: TileRequest): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder().addTimelineEntry(
        TimelineBuilders.TimelineEntry.Builder().setLayout(
            LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(requestParams, context)).build()
        ).build()
    ).build()

    return TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline).build()
}

private suspend fun tileLayout(
    requestParams: TileRequest,
    context: Context,
): LayoutElementBuilders.LayoutElement {
    val token = TokenManager.getToken(context)
    if (token.isNullOrEmpty()) {
        return PrimaryLayout.Builder(requestParams.deviceConfiguration)
            .setResponsiveContentInsetEnabled(true).setContent(
                CompactChip.Builder(
                    context, "Login", Clickable.Builder().setOnClick(
                        ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(context.packageName)
                                .setClassName(TodoistAuthActivity::class.java.name).build()
                        ).build()
                    ).build(), requestParams.deviceConfiguration
                ).build()
            ).build()
    }

    val repository = TodoistRepository.create()
    val todoItemsResult = repository.getTasks(token)

    val todoItems = todoItemsResult.getOrNull()?.map { task ->
        TodoItem(task.content, task.dueDate?.string ?: "")
    } ?: emptyList()

    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true).setContent(Column.Builder().also { column ->
            if (todoItems.isEmpty()) {
                column.addContent(
                    Text.Builder(context, "No tasks found.")
                        .setTypography(Typography.TYPOGRAPHY_BODY1)
                        .setColor(argb(Colors.DEFAULT.onSurface))
                        .build()
                )
            } else {
                todoItems.forEachIndexed { index, item ->
                    column.addContent(
                        Text.Builder(context, item.title).setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setColor(argb(Colors.DEFAULT.onSurface)).build()
                    )
                    if (item.secondaryText.isNotEmpty()) {
                        column.addContent(
                            Text.Builder(context, item.secondaryText).setTypography(Typography.TYPOGRAPHY_CAPTION1)
                                .setColor(argb(Colors.DEFAULT.onSurface)).build()
                        )
                    }
                    if (index < todoItems.size - 1)
                        column.addContent(Spacer.Builder().setHeight(dp(8f)).build())
                }
            }
        }.build()).build()
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(
    onTileResourceRequest = ::resources
) { request ->
    TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(
            TimelineBuilders.Timeline.Builder().addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder().setLayout(
                    LayoutElementBuilders.Layout.Builder().setRoot(
                        runBlocking { tileLayout(request, context) }
                    ).build()
                ).build()
            ).build()
        ).build()
}
