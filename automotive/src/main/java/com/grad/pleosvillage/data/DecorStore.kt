package com.grad.pleosvillage.data

import com.grad.pleosvillage.ui.art.MockFences
import com.grad.pleosvillage.ui.art.ObjType
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.VillageObject
import com.grad.pleosvillage.ui.art.VillageSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Decorate state (UI_SPEC 4.3, SPEC §5). Items are obtained with points and placed in the village. No real-money payment.
 * - Placing a new item spends its points. If it is in storage, it is placed for free.
 * - Locked items must first be obtained with points ("80포인트로 얻을까요?"). Doing so puts 1 in storage and unlocks it.
 * - Placed items can be moved, rotated (flipped horizontally), or stored. Resident houses can only be moved (not stored).
 * - Saved only when [완료] is pressed.
 * Real persistence arrives with stage 2 (judging logic). For now it is remembered only while the app is running.
 */

enum class DecorTab { Houses, Paths, Nature, Deco }

/** A single item in the drawer. */
data class DecorItem(val id: String, val type: ObjType, val name: String, val cost: Int, val tab: DecorTab, val lockedAtStart: Boolean = false)

/** A placed object. [kind] is the resident kind when it is a house. */
data class Placed(val uid: Int, val type: ObjType, val i: Int, val j: Int, val kind: ResidentKind? = null, val flip: Boolean = false)

/** One decorate state (the unit of undo). */
data class DecorState(
    val placed: List<Placed>,
    val points: Int,
    /** Item id → count in storage */
    val storage: Map<String, Int>,
    val unlocked: Set<String>,
    val nextUid: Int,
)

object DecorStore {
    val catalog = listOf(
        DecorItem("stones", ObjType.Stones, "디딤돌", 10, DecorTab.Paths),
        DecorItem("fence", ObjType.Fence, "울타리", 30, DecorTab.Paths),
        DecorItem("tree", ObjType.Tree, "나무", 30, DecorTab.Nature),
        DecorItem("flowers", ObjType.Flowers, "꽃밭", 20, DecorTab.Nature),
        DecorItem("hill", ObjType.Hill, "작은 산", 150, DecorTab.Nature, lockedAtStart = true),
        DecorItem("fountain", ObjType.Fountain, "분수", 120, DecorTab.Deco),
        DecorItem("lamp", ObjType.Lamp, "가로등", 40, DecorTab.Deco),
        DecorItem("bench", ObjType.Bench, "벤치", 50, DecorTab.Deco),
        DecorItem("lantern", ObjType.Lantern, "등불", 80, DecorTab.Deco, lockedAtStart = true),
    )

    fun itemFor(type: ObjType) = catalog.firstOrNull { it.type == type }

    /** Terrain-only map: g grass, p path, w pond, P plaza (fountain spot). Objects (t f F h) moved to [Placed]. */
    val TERRAIN = listOf(
        "g g g p g g g",
        "g g g p g g g",
        "g g g p g g g",
        "g p p P p p g",
        "g g g p g g g",
        "g g g p g g g",
        "g g w p g g g",
    )

    /** Initial layout matching mockup screen 2 (t f F h from VillageSpec.DEFAULT_MAP + lamps and bench). */
    private fun defaultPlaced(): List<Placed> {
        val out = mutableListOf<Placed>()
        VillageSpec.DEFAULT_MAP.forEachIndexed { j, row ->
            row.split(' ').forEachIndexed { i, ch ->
                val type = when (ch) { "t" -> ObjType.Tree; "f" -> ObjType.Flowers; "F" -> ObjType.Fountain; "h" -> ObjType.House; else -> null }
                if (type != null) out += Placed(out.size + 1, type, i, j, VillageSpec.DEFAULT_HOUSES[i to j])
            }
        }
        out += Placed(out.size + 1, ObjType.Lamp, 6, 3)
        out += Placed(out.size + 1, ObjType.Lamp, 0, 2)
        out += Placed(out.size + 1, ObjType.Bench, 2, 4)
        return out
    }

    private val initial = DecorState(
        placed = defaultPlaced(),
        points = 3240,
        storage = emptyMap(),
        unlocked = catalog.filter { !it.lockedAtStart }.map { it.id }.toSet(),
        nextUid = 100,
    )

    private val _saved = MutableStateFlow(initial)
    /** State saved via [완료]. The village screen draws from this value. */
    val saved: StateFlow<DecorState> = _saved.asStateFlow()

    fun save(state: DecorState) { _saved.value = state }

    /** Debug: restore the initial layout. */
    fun reset() { _saved.value = initial }

    /** Tiles where residents stand (objects can't be placed). */
    val residentTiles: Set<Pair<Int, Int>> get() = FakeRepository.residentTiles

    /** Tiles occupied by fences (fixed decoration). */
    private val fenceTiles: Set<Pair<Int, Int>> = MockFences.map {
        kotlin.math.round((it.u0 + it.u1) / 2 - .5f).toInt() to kotlin.math.round((it.v0 + it.v1) / 2 - .5f).toInt()
    }.toSet()

    /** Whether an object can be placed at (i, j). [ignoreUid] is the object being moved itself. */
    fun canPlace(state: DecorState, i: Int, j: Int, ignoreUid: Int? = null): Boolean {
        if (i !in 0 until 7 || j !in 0 until 7) return false
        val t = TERRAIN[j].split(' ')[i]
        if (t != "g" && t != "P") return false
        if ((i to j) in residentTiles || (i to j) in fenceTiles) return false
        return state.placed.none { it.uid != ignoreUid && it.i == i && it.j == j }
    }

    /** List of objects to draw in the village. */
    fun objects(state: DecorState): List<VillageObject> =
        state.placed.map { VillageObject(it.i, it.j, it.type, kind = it.kind, flip = it.flip) }
}
