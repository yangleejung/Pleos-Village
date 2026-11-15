package com.grad.pleosvillage.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.vehicle.GearPosition
import com.grad.pleosvillage.vehicle.Reading
import com.grad.pleosvillage.vehicle.VehicleState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fixed colors chosen for contrast of at least 4.5:1 (against a white background).
private val Background = Color(0xFFF4F5F7)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF111111)   // contrast ~18:1
private val TextSecondary = Color(0xFF4A4F57) // contrast ~8:1
private val OnColor = Color(0xFF0B6B2E)       // contrast ~6.6:1
private val WarnColor = Color(0xFFB3261E)     // contrast ~6.5:1
private val MutedColor = Color(0xFF5F6368)    // contrast ~6:1

private data class DiagRow(val name: String, val reading: Reading<*>, val highlight: Highlight = Highlight.None)
private enum class Highlight { None, On, Warn }

/**
 * Vehicle data diagnostics screen. Portrait (1 app window) and landscape (2 app windows) show the same items;
 * only the column count changes with window width.
 */
@Composable
fun DiagnosticsScreen(state: VehicleState, modifier: Modifier = Modifier) {
    val sections = buildSections(state)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "차량 데이터 진단",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
        items(sections, key = { it.first }) { (title, rows) ->
            SectionCard(title, rows)
        }
    }
}

private fun buildSections(s: VehicleState): List<Pair<String, List<DiagRow>>> {
    val gearHighlight = (s.gear as? Reading.Value)?.let {
        if (it.value == GearPosition.PARK) Highlight.On else Highlight.None
    } ?: Highlight.None
    return listOf(
        "주행 규제" to listOf(DiagRow("DISTRACTION_OPT", s.drivingRestricted, onIf(s.drivingRestricted) { it })),
        "기어" to listOf(DiagRow("GEAR_SELECTION", s.gear, gearHighlight)),
        "와이퍼" to areaRows(s.wiperSupport, s.wiper) { it == "ON" },
        "하향등" to listOf(DiagRow("LOW_BEAM", s.lowBeam, onIf(s.lowBeam) { it == "ON" })),
        "좌석 탑승" to areaRows(s.seatOccupancySupport, s.seatOccupancy) { it },
        "조향각" to listOf(DiagRow("STEERING_ANGLE", s.steeringAngle)),
        "성에 제거" to areaRows(s.defrosterSupport, s.defroster) { it },
    )
}

private fun <T> areaRows(
    support: Reading<Unit>,
    values: Map<String, Reading<T>>,
    warn: Boolean = false,
    isActive: (T) -> Boolean,
): List<DiagRow> {
    if (values.isEmpty()) return listOf(DiagRow("지원 여부", support))
    return values.toSortedMap().map { (area, reading) ->
        val active = (reading as? Reading.Value<T>)?.let { isActive(it.value) } == true
        DiagRow(area, reading, if (!active) Highlight.None else if (warn) Highlight.Warn else Highlight.On)
    }
}

private fun <T> onIf(r: Reading<T>, isActive: (T) -> Boolean): Highlight =
    if ((r as? Reading.Value<T>)?.let { isActive(it.value) } == true) Highlight.On else Highlight.None

@Composable
private fun SectionCard(title: String, rows: List<DiagRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        rows.forEach { ReadingRow(it) }
    }
}

@Composable
private fun ReadingRow(row: DiagRow) {
    val (text, color) = describe(row)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            row.name,
            fontSize = 16.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = color)
            (row.reading as? Reading.Value<*>)?.let {
                Text(timeFormat.format(Date(it.updatedAtMillis)), fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)

private fun describe(row: DiagRow): Pair<String, Color> = when (val r = row.reading) {
    Reading.Checking -> "확인 중…" to MutedColor
    Reading.Waiting -> "값 기다리는 중" to MutedColor
    is Reading.Unsupported -> "미지원 (${r.reason})" to MutedColor
    is Reading.Error -> "오류: ${r.message}" to WarnColor
    is Reading.Value<*> -> {
        val text = when (val v = r.value) {
            true -> "예"
            false -> "아니오"
            is Float -> "%.1f°".format(v)
            else -> v.toString()
        }
        text to when (row.highlight) {
            Highlight.On -> OnColor
            Highlight.Warn -> WarnColor
            Highlight.None -> TextPrimary
        }
    }
}
