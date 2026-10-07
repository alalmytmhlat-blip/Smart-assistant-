package com.smartassistant.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandBlue = Color(0xFF1769E0)
private val BrandTeal = Color(0xFF0BA6A6)
private val Page = Color(0xFFF7F9FC)
private val Ink = Color(0xFF17324D)

data class DashboardItem(val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartAssistantApp() }
    }
}

@Composable
fun SmartAssistantApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandBlue,
            secondary = BrandTeal,
            background = Page,
            surface = Color.White,
            onSurface = Ink
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            DashboardScreen()
        }
    }
}

@Composable
private fun DashboardScreen() {
    var expanded by remember { mutableStateOf<String?>(null) }
    val items = listOf(
        DashboardItem("العملاء والأرصدة", "إدارة العملاء", Icons.Default.People, BrandBlue),
        DashboardItem("الاستحقاقات", "المواعيد والمتابعة", Icons.Default.EventNote, BrandTeal),
        DashboardItem("المخزون", "الأصناف والكميات", Icons.Default.Inventory2, Color(0xFF7A55D8)),
        DashboardItem("التقارير", "ملخصات وتحليلات", Icons.Default.Assessment, Color(0xFFE88A18)),
        DashboardItem("الاستيراد الذكي", "قراءة ملفات PDF", Icons.Default.FileOpen, Color(0xFF168C62)),
        DashboardItem("الرسائل", "قوالب واتساب", Icons.Default.Message, Color(0xFF1A9B72))
    )

    Scaffold(
        containerColor = Page,
        bottomBar = { BottomBar() }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Header()
            Spacer(Modifier.height(16.dp))
            AssistantCard()
            Spacer(Modifier.height(18.dp))
            SectionTitle("الوصول السريع", "كل أدواتك في مكان واحد")
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(330.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(items) { item -> DashboardCard(item) }
            }
            Spacer(Modifier.height(18.dp))
            Accordion("التنبيهات والمتابعة", expanded == "alerts") {
                expanded = if (expanded == "alerts") null else "alerts"
            }
            if (expanded == "alerts") {
                InfoRow("مواعيد اليوم", "لا توجد مواعيد متأخرة", Icons.Default.Notifications)
                InfoRow("عملاء يحتاجون متابعة", "يمكنك مراجعة القائمة الآن", Icons.Default.PersonSearch)
            }
            Accordion("إدارة البيانات", expanded == "data") {
                expanded = if (expanded == "data") null else "data"
            }
            if (expanded == "data") {
                InfoRow("النسخ الاحتياطي", "حماية بياناتك", Icons.Default.Backup)
                InfoRow("سجل العمليات", "مراجعة آخر التغييرات", Icons.Default.History)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("المساعد الذكي", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("لوحة التحكم", fontSize = 14.sp, color = Color(0xFF6B7C8F))
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = BrandBlue)
            }
        }
    }
}

@Composable
private fun AssistantCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = BrandBlue
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = .16f)
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.padding(14.dp).size(34.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("كيف أساعدك اليوم؟", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("اسأل عن العملاء، المواعيد، المخزون أو التقارير.", color = Color.White.copy(.88f), fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
        }
    }
}

@Composable
private fun DashboardCard(item: DashboardItem) {
    Surface(
        modifier = Modifier.fillMaxSize().clickable { },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            Modifier.padding(15.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = item.color.copy(alpha = .10f)) {
                Icon(item.icon, null, tint = item.color, modifier = Modifier.padding(10.dp).size(26.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(item.title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
            Text(item.subtitle, color = Color(0xFF718096), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Accordion(title: String, open: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ListAlt, null, tint = BrandBlue)
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun InfoRow(title: String, subtitle: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = BrandTeal, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(subtitle, color = Color(0xFF718096), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF718096))
    }
}

@Composable
private fun BottomBar() {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(true, {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("المساعد") })
        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Notifications, null) }, label = { Text("التنبيهات") })
        NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("الإعدادات") })
    }
}
