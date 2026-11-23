package com.example.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.districts.DistrictCatalog
import com.example.game.districts.DistrictEntry
import com.example.game.districts.Division
import com.example.game.localization.AppLanguage
import com.example.game.save.PlayerProgressState
import com.example.game.world.DistrictTravelGraph
import com.example.game.world.TravelRouteType
import com.example.ui.theme.BengalEmerald
import com.example.ui.theme.BengalEmeraldBright
import com.example.ui.theme.DhakaAmber
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceBorder
import kotlin.math.hypot

@Composable
fun BangladeshWorldMapCard(
    districts: List<DistrictEntry>,
    selectedDistrict: DistrictEntry,
    selectedDivision: Division?,
    progress: PlayerProgressState,
    lang: AppLanguage,
    onSelectDistrict: (DistrictEntry) -> Unit
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()

    val districtById = remember { DistrictCatalog.allDistricts.associateBy { it.id } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, TacticalSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.86f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0A141D), Color(0xFF081B24), Color(0xFF072330))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clipToBounds()
                    .testTag("bangladesh_interactive_map")
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            val newZoom = (zoom * zoomChange).coerceIn(1f, 2.8f)
                            val maxPan = (size.width * (newZoom - 1f)) * 0.55f
                            val nextPan = pan + panChange
                            zoom = newZoom
                            pan = Offset(
                                x = nextPan.x.coerceIn(-maxPan, maxPan),
                                y = nextPan.y.coerceIn(-maxPan, maxPan)
                            )
                        }
                    }
                    .pointerInput(districts, zoom, pan) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val cx = w * 0.5f
                            val cy = h * 0.5f

                            var bestEntry: DistrictEntry? = null
                            var bestDist = Float.MAX_VALUE
                            for (d in districts) {
                                val baseX = d.mapX * w
                                val baseY = d.mapY * h
                                val sx = (baseX - cx) * zoom + cx + pan.x
                                val sy = (baseY - cy) * zoom + cy + pan.y
                                val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
                                if (dist < bestDist && dist <= 46f) {
                                    bestDist = dist
                                    bestEntry = d
                                }
                            }
                            bestEntry?.let(onSelectDistrict)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f
                    val cy = h * 0.5f

                    fun mapPt(nx: Float, ny: Float): Offset = Offset(
                        x = (nx * w - cx) * zoom + cx + pan.x,
                        y = (ny * h - cy) * zoom + cy + pan.y
                    )

                    // Subtle tactical grid
                    for (i in 1..7) {
                        val gx = w * (i / 8f)
                        val gy = h * (i / 8f)
                        drawLine(Color.White.copy(alpha = 0.04f), Offset(gx, 0f), Offset(gx, h), 1f)
                        drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, gy), Offset(w, gy), 1f)
                    }

                    // Stylized Bangladesh geographical contour
                    val contourPoints = listOf(
                        0.16f to 0.06f, 0.25f to 0.11f, 0.38f to 0.11f, 0.44f to 0.20f,
                        0.60f to 0.24f, 0.78f to 0.23f, 0.85f to 0.30f, 0.78f to 0.41f,
                        0.67f to 0.44f, 0.71f to 0.57f, 0.86f to 0.58f, 0.90f to 0.72f,
                        0.89f to 0.84f, 0.83f to 0.92f, 0.77f to 0.87f, 0.74f to 0.73f,
                        0.62f to 0.70f, 0.58f to 0.78f, 0.50f to 0.85f, 0.41f to 0.81f,
                        0.28f to 0.82f, 0.26f to 0.68f, 0.18f to 0.54f, 0.24f to 0.44f,
                        0.12f to 0.35f, 0.18f to 0.22f
                    )
                    val landPath = Path().apply {
                        val first = mapPt(contourPoints.first().first, contourPoints.first().second)
                        moveTo(first.x, first.y)
                        for (idx in 1 until contourPoints.size) {
                            val pt = mapPt(contourPoints[idx].first, contourPoints[idx].second)
                            lineTo(pt.x, pt.y)
                        }
                        close()
                    }
                    drawPath(
                        path = landPath,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF103227), Color(0xFF0E2624))
                        )
                    )
                    drawPath(
                        path = landPath,
                        color = BengalEmeraldBright.copy(alpha = 0.45f),
                        style = Stroke(width = 2.4f)
                    )

                    // Major River Deltas (Jamuna -> Padma -> Meghna -> Bay of Bengal)
                    val jamunaPadma = Path().apply {
                        val p0 = mapPt(0.41f, 0.14f)
                        val p1 = mapPt(0.39f, 0.36f)
                        val p2 = mapPt(0.43f, 0.52f)
                        val p3 = mapPt(0.55f, 0.64f)
                        val p4 = mapPt(0.57f, 0.83f)
                        moveTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                    }
                    drawPath(jamunaPadma, color = TacticalCyan.copy(alpha = 0.32f), style = Stroke(width = 4.5f))

                    val padmaWest = Path().apply {
                        val p0 = mapPt(0.14f, 0.38f)
                        val p1 = mapPt(0.29f, 0.47f)
                        val p2 = mapPt(0.43f, 0.52f)
                        moveTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                    }
                    drawPath(padmaWest, color = TacticalCyan.copy(alpha = 0.28f), style = Stroke(width = 3.5f))

                    val meghnaEast = Path().apply {
                        val p0 = mapPt(0.76f, 0.27f)
                        val p1 = mapPt(0.62f, 0.44f)
                        val p2 = mapPt(0.55f, 0.64f)
                        moveTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                    }
                    drawPath(meghnaEast, color = TacticalCyan.copy(alpha = 0.26f), style = Stroke(width = 3.2f))

                    // Connected Travel Corridors
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    for (edge in DistrictTravelGraph.allEdges) {
                        val a = districtById[edge.fromId] ?: continue
                        val b = districtById[edge.toId] ?: continue
                        val bothUnlocked = a.id in progress.unlockedDistrictIds && b.id in progress.unlockedDistrictIds
                        val lineColor = when {
                            bothUnlocked -> BengalEmeraldBright.copy(alpha = 0.62f)
                            edge.routeType == TravelRouteType.RIVER_BOAT -> TacticalCyan.copy(alpha = 0.18f)
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                        drawLine(
                            color = lineColor,
                            start = mapPt(a.mapX, a.mapY),
                            end = mapPt(b.mapX, b.mapY),
                            strokeWidth = if (bothUnlocked) 2.2f else 1.1f,
                            pathEffect = if (edge.routeType == TravelRouteType.RIVER_BOAT) dashEffect else null
                        )
                    }

                    // All 64 District Nodes
                    for (d in DistrictCatalog.allDistricts) {
                        val pt = mapPt(d.mapX, d.mapY)
                        val inFilter = selectedDivision == null || d.division == selectedDivision
                        val isCurrent = d.id == progress.currentDistrictId
                        val isSelected = d.id == selectedDistrict.id
                        val isCompleted = progress.completionPercent(d.id) == 100
                        val isUnlocked = d.id in progress.unlockedDistrictIds
                        val alpha = if (inFilter) 1f else 0.25f

                        val fillColor = when {
                            isCompleted -> BengalEmeraldBright
                            d.isEnvironmentReady && isUnlocked -> TacticalCyan
                            isUnlocked -> DhakaAmber
                            d.isEnvironmentReady -> Color(0xFF4F7A8A)
                            else -> Color(0xFF475569)
                        }.copy(alpha = alpha)

                        if (isCurrent) {
                            drawCircle(
                                color = DhakaAmber.copy(alpha = 0.45f),
                                radius = (if (isSelected) 16f else 13f),
                                center = pt,
                                style = Stroke(width = 3f)
                            )
                        }
                        if (isSelected) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.90f),
                                radius = 12f,
                                center = pt,
                                style = Stroke(width = 2.5f)
                            )
                        }

                        val nodeRadius = when {
                            isSelected -> 8.5f
                            d.isEnvironmentReady -> 6.8f
                            else -> 4.6f
                        }
                        drawCircle(color = fillColor, radius = nodeRadius, center = pt)

                        if (d.isEnvironmentReady || isSelected || (zoom >= 1.55f && inFilter)) {
                            val label = d.name(lang)
                            val measured = textMeasurer.measure(
                                text = label,
                                style = TextStyle(
                                    color = if (isSelected) Color.White else Color(0xFFD9F3E8),
                                    fontSize = if (isSelected) 11.sp else 9.sp,
                                    fontWeight = if (isSelected || d.isEnvironmentReady) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            val tx = (pt.x - measured.size.width * 0.5f).coerceIn(4f, w - measured.size.width - 4f)
                            val ty = (pt.y - nodeRadius - measured.size.height - 2f).coerceAtLeast(2f)
                            drawText(measured, topLeft = Offset(tx, ty))
                        }
                    }
                }

                // Floating Zoom & Recenter Controls (48dp touch targets)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TacticalSurfaceBorder)
                    ) {
                        Column {
                            IconButton(
                                onClick = { zoom = (zoom + 0.45f).coerceAtMost(2.8f) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = BengalEmeraldBright)
                            }
                            IconButton(
                                onClick = {
                                    val next = (zoom - 0.45f).coerceAtLeast(1f)
                                    zoom = next
                                    if (next <= 1.02f) pan = Offset.Zero
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = DhakaAmber)
                            }
                            IconButton(
                                onClick = {
                                    zoom = 1f
                                    pan = Offset.Zero
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = "Reset Map", tint = TacticalCyan)
                            }
                        }
                    }
                }

                // Current Location Badge
                Surface(
                    color = ObsidianBg.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DhakaAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    val curEntry = DistrictCatalog.getById(progress.currentDistrictId)
                    Text(
                        text = "${curEntry.name(lang)} • ${DistrictCatalog.playableDistricts.size}/64 3D",
                        style = MaterialTheme.typography.labelSmall,
                        color = DhakaAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Map Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MapLegendDot(BengalEmeraldBright, if (lang == AppLanguage.BENGALI) "সম্পন্ন" else "100% Done")
                MapLegendDot(TacticalCyan, if (lang == AppLanguage.BENGALI) "৩ডি প্রস্তুত" else "3D Ready")
                MapLegendDot(DhakaAmber, if (lang == AppLanguage.BENGALI) "আনলক" else "Unlocked")
                MapLegendDot(Color(0xFF475569), if (lang == AppLanguage.BENGALI) "লকড" else "Locked")
            }
        }
    }
}

@Composable
private fun MapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
