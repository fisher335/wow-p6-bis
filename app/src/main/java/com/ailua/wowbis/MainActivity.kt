package com.ailua.wowbis

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataRepository.load(this)
        setContent {
            WoWBisTheme { App() }
        }
    }
}

@Composable
fun WoWBisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFC79C6E),
            onPrimary = Color(0xFF1A1A24),
            background = Color(0xFF0B0B12),
            surface = Color(0xFF15151F),
            onSurface = Color(0xFFE6E1D8),
        ),
        content = content,
    )
}

sealed interface Screen {
    data object Home : Screen
    data class Specs(val cls: ClassInfo) : Screen
    data class Bis(val cls: ClassInfo, val spec: SpecInfo) : Screen
}

@Composable
fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Bis -> Screen.Specs(s.cls)
            is Screen.Specs -> Screen.Home
            else -> Screen.Home
        }
    }

    when (val s = screen) {
        is Screen.Home -> HomeScreen(onPick = { screen = Screen.Specs(it) })
        is Screen.Specs -> SpecsScreen(
            cls = s.cls,
            onBack = { screen = Screen.Home },
            onPick = { screen = Screen.Bis(s.cls, it) },
        )
        is Screen.Bis -> BisScreen(
            cls = s.cls,
            spec = s.spec,
            onBack = { screen = Screen.Specs(s.cls) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(title: String, subtitle: String?, onBack: (() -> Unit)?) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = Color(0xFF9A9480))
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                TextButton(onClick = onBack) { Text("← 返回", color = Color(0xFFC79C6E)) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF101018),
            titleContentColor = Color(0xFFE6E1D8),
        ),
    )
}

@Composable
fun HomeScreen(onPick: (ClassInfo) -> Unit) {
    Scaffold(
        containerColor = Color(0xFF0B0B12),
        topBar = { AppBar("时光服 P6 配装", "魔兽世界 · 各职业专精 BiS", null) },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "选择职业查看 P6 阶段 BiS 装备（主选 + 2 件备选）",
                    fontSize = 13.sp,
                    color = Color(0xFF9A9480),
                    modifier = Modifier.padding(4.dp),
                )
            }
            items(DataRepository.classes, key = { it.key }) { cls ->
                ClassCard(cls) { onPick(cls) }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun ClassCard(cls: ClassInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(classColor(cls.key)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cls.name.take(1),
                    color = Color(0xFF101018),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(cls.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6E1D8))
                Text(
                    cls.specs.joinToString(" · ") { it.name },
                    fontSize = 12.sp,
                    color = Color(0xFF9A9480),
                )
            }
            Text("›", fontSize = 22.sp, color = Color(0xFFC79C6E))
        }
    }
}

@Composable
fun SpecsScreen(cls: ClassInfo, onBack: () -> Unit, onPick: (SpecInfo) -> Unit) {
    Scaffold(
        containerColor = Color(0xFF0B0B12),
        topBar = { AppBar(cls.name, "选择专精", onBack) },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(cls.specs, key = { it.key }) { spec ->
                Card(
                    onClick = { onPick(spec) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(classColor(cls.key)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            spec.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1D8),
                            modifier = Modifier.weight(1f),
                        )
                        Text("${spec.slots.size} 部位", fontSize = 12.sp, color = Color(0xFF9A9480))
                        Spacer(Modifier.width(8.dp))
                        Text("›", fontSize = 22.sp, color = Color(0xFFC79C6E))
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun BisScreen(cls: ClassInfo, spec: SpecInfo, onBack: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF0B0B12),
        topBar = { AppBar("${cls.name} · ${spec.name}", "P6 阶段 BiS", onBack) },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(spec.slots, key = { it.slot }) { entry ->
                SlotCard(cls.key, entry)
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun SlotCard(classKey: String, entry: SlotEntry) {
    val items = DataRepository.items
    val slotCN = DataRepository.slotCN[entry.slot] ?: entry.slot
    val main = items[entry.ids.firstOrNull()]
    val alts = entry.ids.drop(1).mapNotNull { items[it] }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(classColor(classKey)),
                )
                Spacer(Modifier.width(8.dp))
                Text(slotCN, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC79C6E))
                Spacer(Modifier.weight(1f))
                Text(entry.slot, fontSize = 11.sp, color = Color(0xFF6E6A5E))
            }
            Spacer(Modifier.height(10.dp))
            if (main != null) {
                ItemRow(main, entry.slot, main = true)
            }
            if (alts.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("备选", fontSize = 12.sp, color = Color(0xFF9A9480), modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                alts.forEachIndexed { idx, alt ->
                    ItemRow(alt, entry.slot, main = false)
                    if (idx != alts.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ItemRow(item: ItemInfo, slot: String, main: Boolean) {
    val color = qualityColor(item.quality)
    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(item.icon, slot, item.quality, if (main) 52.dp else 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = if (main) 16.sp else 14.sp,
                fontWeight = if (main) FontWeight.Bold else FontWeight.Normal,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.ilvl > 0) {
                    Text("物品等级 ${item.ilvl}", fontSize = 11.sp, color = Color(0xFF9A9480))
                }
                if (!item.source.isNullOrEmpty()) {
                    if (item.ilvl > 0) Text(" · ", fontSize = 11.sp, color = Color(0xFF6E6A5E))
                    Text(item.source, fontSize = 11.sp, color = Color(0xFF8FB6A0), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ItemIcon(iconName: String?, slot: String, quality: Int, size: Dp) {
    val ctx = LocalContext.current
    var bmp by remember(iconName) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(iconName) {
        bmp = if (iconName.isNullOrEmpty()) null
        else withContext(Dispatchers.IO) { IconLoader.load(ctx, iconName) }
    }
    val border = qualityColor(quality)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF0F0F18))
            .border(1.5.dp, border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                slotGlyph(slot),
                color = border,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}

fun qualityColor(q: Int): Color = when (q) {
    5 -> Color(0xFFFF8000)
    4 -> Color(0xFFA335EE)
    3 -> Color(0xFF0070DD)
    2 -> Color(0xFF1EFF00)
    1 -> Color(0xFF9D9D9D)
    else -> Color(0xFF808080)
}

fun classColor(key: String): Color = when (key) {
    "Warrior" -> Color(0xFFC79C6E)
    "Paladin" -> Color(0xFFF58CBA)
    "Death knight" -> Color(0xFFC41F3B)
    "Hunter" -> Color(0xFFABD473)
    "Rogue" -> Color(0xFFFFF569)
    "Priest" -> Color(0xFFFFFFFF)
    "Shaman" -> Color(0xFF0070DE)
    "Mage" -> Color(0xFF69CCF0)
    "Warlock" -> Color(0xFF9482C9)
    "Druid" -> Color(0xFFFF7D0A)
    else -> Color(0xFFC79C6E)
}

fun slotGlyph(slot: String): String = when (slot) {
    "Head" -> "头"
    "Neck" -> "颈"
    "Shoulder" -> "肩"
    "Back" -> "背"
    "Chest" -> "胸"
    "Wrist" -> "腕"
    "Hands" -> "手"
    "Waist" -> "腰"
    "Legs" -> "腿"
    "Feet" -> "脚"
    "Finger" -> "戒"
    "Trinket" -> "饰"
    "Two hand", "Weapon" -> "武"
    "Off hand" -> "副"
    "Ranged" -> "远"
    "Relic" -> "圣"
    else -> "?"
}
