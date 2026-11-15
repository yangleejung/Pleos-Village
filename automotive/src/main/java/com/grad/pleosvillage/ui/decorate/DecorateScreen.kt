package com.grad.pleosvillage.ui.decorate

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.DecorItem
import com.grad.pleosvillage.data.DecorState
import com.grad.pleosvillage.data.DecorStore
import com.grad.pleosvillage.data.DecorTab
import com.grad.pleosvillage.data.Placed
import com.grad.pleosvillage.ui.art.Bubble
import com.grad.pleosvillage.ui.art.BubbleStyle
import com.grad.pleosvillage.ui.art.MockVillageObjects
import com.grad.pleosvillage.ui.art.MockVillageSpec
import com.grad.pleosvillage.ui.art.MockVillageSpecNarrow
import com.grad.pleosvillage.ui.art.ObjType
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.SkyItem
import com.grad.pleosvillage.ui.art.TileMark
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageCanvas
import com.grad.pleosvillage.ui.art.VillageObject
import com.grad.pleosvillage.ui.art.VillageSpec
import com.grad.pleosvillage.ui.art.drawEls
import com.grad.pleosvillage.ui.art.objectElements
import com.grad.pleosvillage.ui.art.sliceFit
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.DrawerCard
import com.grad.pleosvillage.ui.design.IconCircleButton
import com.grad.pleosvillage.ui.design.LabelValue
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScreenTitle
import com.grad.pleosvillage.ui.design.TabPill
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.numberStyle
import com.grad.pleosvillage.ui.settings.ConfirmDialog
import com.grad.pleosvillage.ui.model.eulReul
import com.grad.pleosvillage.ui.theme.PleosVillageTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Decorate mode. UI_SPEC 4.3, mockup screen 6.
 * - Drag an item from the drawer below onto a tile. If dragging is hard, tap the item and then tap a tile.
 *   Picking a tile shows a translucent preview and a "○○, 여기 둘까요?" bubble; [여기 놓기] confirms.
 *   Green tiles are free; red tiles are already occupied.
 * - Tapping a placed object shows [옮기기] [돌리기] [보관함으로]. Resident houses can only be moved. Residents cannot be stored.
 * - Locked items are unlocked with points ("80포인트로 얻을까요?"). There is no real-money payment.
 * - Saved only when [완료] is pressed. Pressing back asks "저장할까요?".
 * - Narrow layout: the drawer sits at the bottom of the screen (at most half the height).
 */

/** What the user is currently doing. */
private sealed interface Mode {
    data object Idle : Mode
    /** Choosing a tile for an item picked from the drawer. */
    data class Placing(val item: DecorItem, val tile: Pair<Int, Int>? = null) : Mode
    /** One placed object is selected. */
    data class Selected(val uid: Int) : Mode
    /** Choosing a tile to move a placed object to. */
    data class Moving(val uid: Int, val tile: Pair<Int, Int>? = null) : Mode
}

/** Undo/redo history. */
private class History(start: DecorState) {
    var list by mutableStateOf(listOf(start))
    var index by mutableIntStateOf(0)
    val current get() = list[index]
    val canUndo get() = index > 0
    val canRedo get() = index < list.lastIndex
    fun push(s: DecorState) { list = list.take(index + 1) + s; index = list.lastIndex }
    fun undo() { if (canUndo) index-- }
    fun redo() { if (canRedo) index++ }
}

private val DecorTab.label get() = when (this) {
    DecorTab.Houses -> R.string.decor_tab_houses
    DecorTab.Paths -> R.string.decor_tab_paths
    DecorTab.Nature -> R.string.decor_tab_nature
    DecorTab.Deco -> R.string.decor_tab_deco
}

private fun fmt(n: Int) = NumberFormat.getIntegerInstance(Locale.KOREA).format(n)

@Composable
fun DecorateScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val wide = LocalWidthClass.current == WidthClass.Wide
    val c = LocalVillageColors.current
    val history = remember { History(DecorStore.saved.value) }
    val state = history.current
    var mode by remember { mutableStateOf<Mode>(Mode.Idle) }
    var tab by remember { mutableStateOf(DecorTab.Nature) }
    var unlockAsk by remember { mutableStateOf<DecorItem?>(null) }
    var askSave by remember { mutableStateOf(false) }
    // Dragging: finger position in screen (root) coordinates and the dragged item
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var dragItem by remember { mutableStateOf<DecorItem?>(null) }
    var mapOrigin by remember { mutableStateOf(Offset.Zero) }
    var mapSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val changed = state != DecorStore.saved.value
    BackHandler { if (changed) askSave = true else onDone() }

    val lowPoints = stringResource(R.string.decor_low_points)
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    /* ---------- State changes ---------- */

    fun placeNew(item: DecorItem, i: Int, j: Int) {
        if (!DecorStore.canPlace(state, i, j)) return
        val stored = state.storage[item.id] ?: 0
        if (stored == 0 && state.points < item.cost) { toast(lowPoints.format(item.cost)); return }
        val p = Placed(state.nextUid, item.type, i, j)
        history.push(
            state.copy(
                placed = state.placed + p,
                points = if (stored > 0) state.points else state.points - item.cost,
                storage = if (stored > 0) state.storage + (item.id to stored - 1) else state.storage,
                nextUid = state.nextUid + 1,
            ),
        )
        mode = Mode.Idle
    }

    fun move(uid: Int, i: Int, j: Int) {
        if (!DecorStore.canPlace(state, i, j, ignoreUid = uid)) return
        history.push(state.copy(placed = state.placed.map { if (it.uid == uid) it.copy(i = i, j = j) else it }))
        mode = Mode.Selected(uid)
    }

    fun flip(uid: Int) = history.push(state.copy(placed = state.placed.map { if (it.uid == uid) it.copy(flip = !it.flip) else it }))

    fun store(uid: Int) {
        val p = state.placed.first { it.uid == uid }
        val item = DecorStore.itemFor(p.type) ?: return
        history.push(state.copy(placed = state.placed.filter { it.uid != uid }, storage = state.storage + (item.id to (state.storage[item.id] ?: 0) + 1)))
        mode = Mode.Idle
    }

    fun unlock(item: DecorItem) {
        if (state.points < item.cost) { toast(lowPoints.format(item.cost)); return }
        history.push(state.copy(points = state.points - item.cost, unlocked = state.unlocked + item.id, storage = state.storage + (item.id to (state.storage[item.id] ?: 0) + 1)))
        mode = Mode.Placing(item)
    }

    fun pickItem(item: DecorItem) {
        if (item.id !in state.unlocked) unlockAsk = item else mode = Mode.Placing(item)
    }

    /* ---------- Drawing ---------- */

    val base = if (wide) DECOR_SPEC else DECOR_SPEC_NARROW
    val placingName = when (val m = mode) {
        is Mode.Placing -> m.item.name
        is Mode.Moving -> state.placed.firstOrNull { it.uid == m.uid }?.let { nameOf(it) }
        else -> null
    }
    val target: Pair<Int, Int>? = when (val m = mode) { is Mode.Placing -> m.tile; is Mode.Moving -> m.tile; else -> null }
    val movingUid = (mode as? Mode.Moving)?.uid
    val targetOk = target?.let { (i, j) -> DecorStore.canPlace(state, i, j, ignoreUid = movingUid) } ?: false
    val bubbleText = placingName?.let { if (targetOk) stringResource(R.string.decor_bubble_ok, it) else stringResource(R.string.decor_bubble_no) }
    val objects = DecorStore.objects(state).map { o ->
        val p = state.placed.first { it.i == o.i && it.j == o.j && it.type == o.type }
        if (p.uid == movingUid && target != null) o.copy(ghost = true) else o
    }
    val ghost: VillageObject? = target?.let { (i, j) ->
        when (val m = mode) {
            is Mode.Placing -> VillageObject(i, j, m.item.type, ghost = true)
            is Mode.Moving -> state.placed.firstOrNull { it.uid == m.uid }?.let { VillageObject(i, j, it.type, kind = it.kind, flip = it.flip, ghost = true) }
            else -> null
        }
    }
    val selectedTile = (mode as? Mode.Selected)?.let { m -> state.placed.firstOrNull { it.uid == m.uid }?.let { it.i to it.j } }
    val spec = base.copy(
        extras = RESIDENTS + objects + listOfNotNull(ghost),
        marks = buildMap {
            if (target != null) put(target, if (targetOk) TileMark.Ok else TileMark.No)
            if (selectedTile != null) put(selectedTile, TileMark.Ok)
        },
        bubbles = if (target != null && bubbleText != null) listOf(Bubble(target.first, target.second, bubbleText, base.bubbleStyle)) else emptyList(),
    )

    /** Point in map coordinates (px, relative to the map's top-left) → tile. */
    fun tileAtLocal(x: Float, y: Float): Pair<Int, Int>? {
        val (k, dx, dy) = sliceFit(spec, mapSize.width, mapSize.height)
        return spec.tileAt((x - dx) / k, (y - dy) / k)
    }

    fun onMapTap(tile: Pair<Int, Int>?) {
        when (val m = mode) {
            is Mode.Placing -> if (tile != null) mode = m.copy(tile = tile)
            is Mode.Moving -> if (tile != null) mode = m.copy(tile = tile)
            else -> {
                val hit = tile?.let { (i, j) -> state.placed.firstOrNull { it.i == i && it.j == j } }
                mode = if (hit != null) Mode.Selected(hit.uid) else Mode.Idle
            }
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Village (grid visible). Wide layout draws in full app-area coordinates like the mockup (the island sits above the drawer).
        Box(
            Modifier.fillMaxSize().padding(bottom = if (wide) 0.dp else 250.dp)
                .onGloballyPositioned { mapOrigin = it.positionInRoot(); mapSize = androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat()) }
                .pointerInput(state, mode) {
                    detectTapGestures { pos -> onMapTap(tileAtLocal(pos.x, pos.y)) }
                }
                .pointerInput(state) {
                    // Drag a placed object to move it
                    var uid: Int? = null
                    detectDragGestures(
                        onDragStart = { pos ->
                            val t = tileAtLocal(pos.x, pos.y)
                            val hit = t?.let { (i, j) -> state.placed.firstOrNull { it.i == i && it.j == j } }
                            uid = hit?.uid
                            if (hit != null) mode = Mode.Moving(hit.uid, t)
                        },
                        onDrag = { change, _ ->
                            val u = uid ?: return@detectDragGestures
                            val t = tileAtLocal(change.position.x, change.position.y)
                            if (t != null) mode = Mode.Moving(u, t)
                        },
                        onDragEnd = { uid = null },
                        onDragCancel = { uid = null },
                    )
                },
        ) {
            VillageCanvas(spec, Modifier.fillMaxSize())
        }

        Header(state, history, wide, mode, onDone = { DecorStore.save(state); onDone() })

        // Button row for the placed object / target tile
        ActionBar(
            mode, state, targetOk,
            onConfirm = {
                when (val m = mode) {
                    is Mode.Placing -> m.tile?.let { (i, j) -> placeNew(m.item, i, j) }
                    is Mode.Moving -> m.tile?.let { (i, j) -> move(m.uid, i, j) }
                    else -> {}
                }
            },
            onMove = { (mode as? Mode.Selected)?.let { mode = Mode.Moving(it.uid) } },
            onFlip = { (mode as? Mode.Selected)?.let { flip(it.uid) } },
            onStore = { (mode as? Mode.Selected)?.let { store(it.uid) } },
            onCancel = { mode = Mode.Idle },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (wide) 250.dp else 262.dp),
        )

        Drawer(
            state, tab, { tab = it }, mode,
            onPick = ::pickItem,
            onPickHouse = { uid -> mode = Mode.Selected(uid) },
            onDrag = { item, rootPos ->
                dragItem = item; dragPos = rootPos
                if (item.id in state.unlocked) {
                    val t = tileAtLocal(rootPos.x - mapOrigin.x, rootPos.y - mapOrigin.y)
                    mode = Mode.Placing(item, t)
                }
            },
            onDragEnd = { item ->
                if (item.id !in state.unlocked) unlockAsk = item
                dragItem = null; dragPos = null
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Item being dragged (under the finger)
        val dp = dragPos; val di = dragItem
        if (dp != null && di != null) {
            val half = with(LocalDensity.current) { 40.dp.toPx() }
            Box(Modifier.offset { IntOffset((dp.x - half).roundToInt(), (dp.y - half * 1.6f).roundToInt()) }) {
                ItemArt(di.type, null, Modifier.size(80.dp), alpha = .8f)
            }
        }
    }

    unlockAsk?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.decor_unlock_title, item.cost),
            body = stringResource(R.string.decor_unlock_body, item.name.eulReul(), fmt(state.points)),
            ok = stringResource(R.string.decor_unlock_ok),
            onDismiss = { unlockAsk = null },
            onOk = { unlockAsk = null; unlock(item) },
        )
    }
    if (askSave) {
        ConfirmDialog(
            title = stringResource(R.string.decor_save_title),
            body = stringResource(R.string.decor_save_body),
            ok = stringResource(R.string.decor_save_ok),
            cancel = stringResource(R.string.decor_save_no),
            // Tapping outside only closes the dialog and keeps decorating. Only [저장 안 함] discards and exits.
            onDismiss = { askSave = false },
            onCancel = { askSave = false; onDone() },
            onOk = { askSave = false; DecorStore.save(state); onDone() },
        )
    }
}

private fun nameOf(p: Placed): String = DecorStore.itemFor(p.type)?.name ?: p.kind?.let { "${it.residentName}의 집" } ?: "집"

/* ---------------- Header: title, undo, redo, hint, points, done ---------------- */

@Composable
private fun Header(state: DecorState, history: History, wide: Boolean, mode: Mode, onDone: () -> Unit) {
    val c = LocalVillageColors.current
    val hint = when (mode) {
        is Mode.Placing, is Mode.Moving -> stringResource(R.string.decor_hint_place)
        is Mode.Selected -> stringResource(R.string.decor_hint_selected)
        Mode.Idle -> stringResource(R.string.decor_hint_idle)
    }
    Column(Modifier.fillMaxWidth().padding(start = if (wide) 44.dp else 20.dp, end = if (wide) 44.dp else 20.dp, top = if (wide) 22.dp else 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            ScreenTitle(stringResource(R.string.decor_title), Modifier.padding(end = 8.dp), size = if (wide) 32.sp else 26.sp)
            IconCircleButton(VIcon.Undo, { history.undo() }, Modifier.then(if (history.canUndo) Modifier else Modifier.alphaDim()))
            IconCircleButton(VIcon.Redo, { history.redo() }, Modifier.then(if (history.canRedo) Modifier else Modifier.alphaDim()))
            if (wide) Text(hint, style = bodyStyle(14.sp), color = c.fg2, modifier = Modifier.padding(start = 8.dp).weight(1f))
            else Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                Text(stringResource(R.string.points), style = bodyStyle(14.sp), color = c.fg2)
                Text(fmt(state.points), style = numberStyle(20.sp), color = c.fg)
            }
            VButton(stringResource(R.string.decor_done), onDone, height = 52.dp, fontSize = 18.sp)
        }
        if (!wide) Text(hint, style = bodyStyle(13.sp), color = c.fg2)
    }
}

private fun Modifier.alphaDim() = this.alpha(0.4f)

/* ---------------- Button row ---------------- */

@Composable
private fun ActionBar(
    mode: Mode,
    state: DecorState,
    targetOk: Boolean,
    onConfirm: () -> Unit,
    onMove: () -> Unit,
    onFlip: () -> Unit,
    onStore: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    if (mode == Mode.Idle) return
    VCard(modifier, contentPadding = PaddingValues(horizontal = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (mode) {
                is Mode.Placing -> {
                    val stored = state.storage[mode.item.id] ?: 0
                    val price = if (stored > 0) stringResource(R.string.decor_from_storage) else stringResource(R.string.decor_cost, mode.item.cost)
                    Text(
                        if (mode.tile == null) stringResource(R.string.decor_pick_tile, mode.item.name) else "${mode.item.name} · $price",
                        style = bodyStyle(15.sp, FontWeight.SemiBold), color = Ui.Ink, modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    if (mode.tile != null) VButton(stringResource(R.string.decor_place_here), onConfirm, height = 48.dp, fontSize = 16.sp, enabled = targetOk)
                    VButton(stringResource(R.string.cancel), onCancel, primary = false, height = 48.dp, fontSize = 16.sp)
                }
                is Mode.Moving -> {
                    val name = state.placed.firstOrNull { it.uid == mode.uid }?.let { nameOf(it) } ?: ""
                    Text(stringResource(R.string.decor_pick_tile, name), style = bodyStyle(15.sp, FontWeight.SemiBold), color = Ui.Ink, modifier = Modifier.padding(horizontal = 10.dp))
                    if (mode.tile != null) VButton(stringResource(R.string.decor_move_here), onConfirm, height = 48.dp, fontSize = 16.sp, enabled = targetOk)
                    VButton(stringResource(R.string.cancel), onCancel, primary = false, height = 48.dp, fontSize = 16.sp)
                }
                is Mode.Selected -> {
                    val p = state.placed.firstOrNull { it.uid == mode.uid }
                    Text(p?.let { nameOf(it) } ?: "", style = bodyStyle(15.sp, FontWeight.SemiBold), color = Ui.Ink, modifier = Modifier.padding(horizontal = 10.dp))
                    VButton(stringResource(R.string.decor_move), onMove, primary = false, height = 48.dp, fontSize = 16.sp)
                    VButton(stringResource(R.string.decor_flip), onFlip, primary = false, height = 48.dp, fontSize = 16.sp)
                    // Resident houses cannot be stored
                    if (p?.type != ObjType.House) VButton(stringResource(R.string.decor_store), onStore, primary = false, height = 48.dp, fontSize = 16.sp)
                    VButton(stringResource(R.string.close), onCancel, height = 48.dp, fontSize = 16.sp)
                }
                Mode.Idle -> {}
            }
        }
    }
}

/* ---------------- Drawer ---------------- */

@Composable
private fun Drawer(
    state: DecorState,
    tab: DecorTab,
    onTab: (DecorTab) -> Unit,
    mode: Mode,
    onPick: (DecorItem) -> Unit,
    onPickHouse: (Int) -> Unit,
    onDrag: (DecorItem, Offset) -> Unit,
    onDragEnd: (DecorItem) -> Unit,
    modifier: Modifier,
) {
    val c = LocalVillageColors.current
    val wide = LocalWidthClass.current == WidthClass.Wide
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().padding(start = if (wide) 30.dp else 12.dp, end = if (wide) 30.dp else 12.dp, bottom = if (wide) 24.dp else 12.dp)
            .clip(shape).background(c.surface2).border(Dimens.Outline, Ui.Ol, shape)
            .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DecorTab.entries.forEach { t -> TabPill(stringResource(t.label), t == tab, { onTab(t) }) }
            Spacer(Modifier.weight(1f))
            if (wide) Text(stringResource(R.string.decor_drawer_note), style = bodyStyle(13.sp), color = c.fg2)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (tab == DecorTab.Houses) {
                state.placed.filter { it.type == ObjType.House }.forEach { h ->
                    HouseCard(h, selected = (mode as? Mode.Selected)?.uid == h.uid) { onPickHouse(h.uid) }
                }
            } else {
                DecorStore.catalog.filter { it.tab == tab }.forEach { item ->
                    val locked = item.id !in state.unlocked
                    val picked = (mode as? Mode.Placing)?.item?.id == item.id
                    val stored = state.storage[item.id] ?: 0
                    var origin by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        Modifier.onGloballyPositioned { origin = it.positionInRoot() }
                            .pointerInput(item, locked) {
                                detectDragGestures(
                                    onDrag = { change, _ -> onDrag(item, origin + change.position) },
                                    onDragEnd = { onDragEnd(item) },
                                    onDragCancel = { onDragEnd(item) },
                                )
                            },
                    ) {
                        DrawerCard(item.name, item.cost, { onPick(item) }, locked = locked, selected = picked, stored = stored) {
                            ItemArt(item.type, null, Modifier.size(width = 96.dp, height = 74.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseCard(h: Placed, selected: Boolean, onClick: () -> Unit) {
    val c = LocalVillageColors.current
    Column(Modifier.width(112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier.fillMaxWidth().height(92.dp).clip(shape).background(if (selected) Ui.Sel else Ui.White).border(Dimens.Outline, Ui.Ol, shape)
                .pointerInput(h.uid) { detectTapGestures { onClick() } },
            contentAlignment = Alignment.Center,
        ) { ItemArt(ObjType.House, h.kind, Modifier.size(width = 96.dp, height = 74.dp)) }
        Text(nameOf(h), style = com.grad.pleosvillage.ui.design.juaStyle(15.sp), color = c.fg, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
        Text(stringResource(R.string.decor_house_move_only), style = bodyStyle(13.sp, FontWeight.SemiBold), color = c.fg2)
    }
}

/** Item art inside a drawer card. Draws the same art as the village fitted to a 112×86 box (anchored at bottom center). */
@Composable
fun ItemArt(type: ObjType, kind: ResidentKind?, modifier: Modifier, alpha: Float = 1f) {
    Canvas(modifier) {
        val k = size.width / 112f
        val big = type == ObjType.House || type == ObjType.Tree || type == ObjType.Lamp
        val s = if (big) .95f else 1.2f
        translate(size.width / 2, size.height * (if (big) .98f else .82f)) {
            scale(k * s, k * s, Offset.Zero) {
                drawEls(objectElements(type, kind, 1f), k * s, Dimens.Outline.toPx(), alpha)
            }
        }
    }
}

/* ---------------- Village for decorating (positions from mockup screen 6) ---------------- */

private val RESIDENTS = MockVillageObjects.filter { it.type == ObjType.Resident }

private val DECOR_SPEC: VillageSpec = MockVillageSpec.copy(
    map = DecorStore.TERRAIN, ox = 610f, oy = 112f, tw = 96f, edit = true, plaza = true,
    sky = listOf(
        SkyItem.Cloud(84f, 156f, 1.05f), SkyItem.Star(214f, 250f, 6f), SkyItem.Spark(1040f, 300f, 8f),
        SkyItem.Star(1010f, 430f, 5f), SkyItem.Bub(236f, 330f, 4f), SkyItem.Bub(250f, 318f, 2.6f), SkyItem.Spark(140f, 420f, 6f),
    ),
    moon = Triple(1060f, 200f, 20f),
    bubbleStyle = BubbleStyle(dx = -30f, by = 56f, ay = 46f),
    bubbles = emptyList(),
)

private val DECOR_SPEC_NARROW: VillageSpec = MockVillageSpecNarrow.copy(
    map = DecorStore.TERRAIN, edit = true, oy = 150f, bubbleStyle = BubbleStyle(dx = 0f, by = 46f, ay = 38f), bubbles = emptyList(),
)

/* ---------------- Previews ---------------- */

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun DecorateWidePreview() = PleosVillageTheme(night = false) { DesignScaled { DecorateScreen {} } }

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun DecorateNarrowPreview() = PleosVillageTheme(night = false) { DesignScaled { DecorateScreen {} } }
