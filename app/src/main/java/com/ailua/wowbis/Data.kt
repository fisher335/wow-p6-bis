package com.ailua.wowbis

import android.content.Context
import org.json.JSONObject

data class ItemInfo(
    val id: Int,
    val name: String,
    val quality: Int,
    val ilvl: Int,
    val icon: String?,
    val source: String?,
    val type: String?,
    val sub: String?,
    val sockets: Int,
    val stats: Map<String, String>,
    val set: String?,
)

data class SetPieceInfo(val id: Int, val slot: String, val name: String)
data class SetInfo(
    val name: String,
    val en: String?,
    val b2: String?,
    val b4: String?,
    val pieces: List<SetPieceInfo>,
)

data class SlotEntry(val slot: String, val ids: List<Int>)
data class SpecInfo(val key: String, val name: String, val slots: List<SlotEntry>)
data class ClassInfo(val key: String, val name: String, val specs: List<SpecInfo>)

object DataRepository {
    var classes: List<ClassInfo> = emptyList()
        private set
    var items: Map<Int, ItemInfo> = emptyMap()
        private set
    var sets: Map<String, SetInfo> = emptyMap()
        private set
    var slotCN: Map<String, String> = emptyMap()
        private set

    fun load(ctx: Context) {
        if (classes.isNotEmpty()) return
        val text = ctx.assets.open("app_data.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)

        val itemsObj = root.getJSONObject("items")
        val map = HashMap<Int, ItemInfo>()
        val keys = itemsObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val o = itemsObj.getJSONObject(k)
            map[k.toInt()] = ItemInfo(
                id = k.toInt(),
                name = o.optString("name", "未知物品"),
                quality = o.optInt("q", 0),
                ilvl = o.optInt("ilvl", 0),
                icon = if (o.isNull("icon")) null else o.optString("icon"),
                source = if (o.isNull("source")) null else o.optString("source"),
                type = if (o.isNull("type")) null else o.optString("type"),
                sub = if (o.isNull("sub")) null else o.optString("sub"),
                sockets = o.optInt("sockets", 0),
                stats = parseStats(o.optJSONObject("stats")),
                set = if (o.isNull("set")) null else o.optString("set"),
            )
        }
        items = map

        sets = parseSets(root.optJSONObject("sets"))

        val cn = HashMap<String, String>()
        val slotObj = root.optJSONObject("slotCN")
        if (slotObj != null) {
            val sk = slotObj.keys()
            while (sk.hasNext()) {
                val k = sk.next()
                cn[k] = slotObj.getString(k)
            }
        }
        slotCN = cn

        val arr = root.getJSONArray("classes")
        val list = ArrayList<ClassInfo>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val specsArr = c.getJSONArray("specs")
            val specs = ArrayList<SpecInfo>()
            for (j in 0 until specsArr.length()) {
                val s = specsArr.getJSONObject(j)
                val slotsArr = s.getJSONArray("slots")
                val slots = ArrayList<SlotEntry>()
                for (m in 0 until slotsArr.length()) {
                    val sl = slotsArr.getJSONObject(m)
                    val idArr = sl.getJSONArray("ids")
                    val ids = ArrayList<Int>()
                    for (n in 0 until idArr.length()) ids.add(idArr.getInt(n))
                    slots.add(SlotEntry(sl.getString("slot"), ids))
                }
                specs.add(SpecInfo(s.getString("key"), s.getString("name"), slots))
            }
            list.add(ClassInfo(c.getString("key"), c.getString("name"), specs))
        }
        classes = list
    }
}

private fun parseStats(o: JSONObject?): Map<String, String> {
    if (o == null) return emptyMap()
    val m = LinkedHashMap<String, String>()
    val keys = o.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        m[k] = o.optString(k)
    }
    return m
}

private fun parseSets(o: JSONObject?): Map<String, SetInfo> {
    if (o == null) return emptyMap()
    val m = LinkedHashMap<String, SetInfo>()
    val keys = o.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val s = o.getJSONObject(k)
        val piecesArr = s.optJSONArray("pieces")
        val pieces = ArrayList<SetPieceInfo>()
        if (piecesArr != null) {
            for (i in 0 until piecesArr.length()) {
                val p = piecesArr.getJSONObject(i)
                pieces.add(SetPieceInfo(p.optInt("id"), p.optString("slot"), p.optString("name")))
            }
        }
        m[k] = SetInfo(
            name = s.optString("name"),
            en = if (s.isNull("en")) null else s.optString("en"),
            b2 = if (s.isNull("b2")) null else s.optString("b2"),
            b4 = if (s.isNull("b4")) null else s.optString("b4"),
            pieces = pieces,
        )
    }
    return m
}
