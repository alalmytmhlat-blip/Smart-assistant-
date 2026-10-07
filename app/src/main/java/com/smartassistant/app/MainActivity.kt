package com.smartassistant.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

private val Blue=Color(0xFF1769E0); private val Teal=Color(0xFF0BA6A6); private val Page=Color(0xFFF7F9FC); private val Ink=Color(0xFF17324D)

data class Customer(val id:Long,val name:String,val phone:String,val balance:Double,val currency:String)
data class Appointment(val id:Long,val customer:String,val date:String,val time:String,val reason:String,val status:String)
data class Product(val id:Long,val name:String,val qty:Double,val unit:String,val warehouse:String,val category:String)

class AppDb(c:Context):SQLiteOpenHelper(c,"smart_assistant.db",null,1){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,balance REAL NOT NULL DEFAULT 0,currency TEXT NOT NULL DEFAULT 'ر.ي')")
  db.execSQL("CREATE TABLE appointments(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,date TEXT NOT NULL,time TEXT,reason TEXT,status TEXT NOT NULL DEFAULT 'upcoming')")
  db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,qty REAL NOT NULL DEFAULT 0,unit TEXT,warehouse TEXT,category TEXT)")
  db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT)")
 }
 override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){}
 fun customers():List<Customer>{val r=mutableListOf<Customer>();readableDatabase.rawQuery("SELECT id,name,phone,balance,currency FROM customers ORDER BY name",null).use{c->while(c.moveToNext())r+=Customer(c.getLong(0),c.getString(1),c.getString(2)?:"",c.getDouble(3),c.getString(4))};return r}
 fun saveCustomer(n:String,p:String,b:Double,cur:String){val v=ContentValues();v.put("name",n);v.put("phone",p);v.put("balance",b);v.put("currency",cur);writableDatabase.insert("customers",null,v)}
 fun appointments():List<Appointment>{val r=mutableListOf<Appointment>();readableDatabase.rawQuery("SELECT a.id,c.name,a.date,a.time,a.reason,a.status FROM appointments a JOIN customers c ON c.id=a.customer_id ORDER BY a.date,a.time",null).use{c->while(c.moveToNext())r+=Appointment(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)?:"",c.getString(4)?:"",c.getString(5))};return r}
 fun saveAppointment(cid:Long,d:String,t:String,reason:String){val v=ContentValues();v.put("customer_id",cid);v.put("date",d);v.put("time",t);v.put("reason",reason);v.put("status","upcoming");writableDatabase.insert("appointments",null,v)}
 fun products():List<Product>{val r=mutableListOf<Product>();readableDatabase.rawQuery("SELECT id,name,qty,unit,warehouse,category FROM products ORDER BY name",null).use{c->while(c.moveToNext())r+=Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3)?:"حبة",c.getString(4)?:"الرئيسي",c.getString(5)?:"عام")};return r}
 fun saveProduct(n:String,q:Double,u:String,w:String,cat:String){val v=ContentValues();v.put("name",n);v.put("qty",q);v.put("unit",u);v.put("warehouse",w);v.put("category",cat);writableDatabase.insert("products",null,v)}
 fun setting(k:String)=readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?",arrayOf(k)).use{if(it.moveToFirst())it.getString(0) else ""}
 fun saveSetting(k:String,v:String){val x=ContentValues();x.put("key",k);x.put("value",v);writableDatabase.insertWithOnConflict("settings",null,x,SQLiteDatabase.CONFLICT_REPLACE)}
}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App(AppDb(this))}}}

@Composable fun App(db:AppDb){
 MaterialTheme(colorScheme=lightColorScheme(primary=Blue,secondary=Teal,background=Page,surface=Color.White,onSurface=Ink)){
  CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl){Root(db)}
 }
}
@Composable fun Root(db:AppDb){
 var tab by remember{mutableStateOf("home")}
 Scaffold(containerColor=Page,bottomBar={
  NavigationBar(modifier=Modifier.fillMaxWidth()){
   Nav("home","الرئيسية",Icons.Default.Home,tab){tab="home"}
   Nav("assistant","المساعد",Icons.Default.AutoAwesome,tab){tab="assistant"}
   Nav("alerts","التنبيهات",Icons.Default.Notifications,tab){tab="alerts"}
   Nav("settings","الإعدادات",Icons.Default.Settings,tab){tab="settings"}
  }
 }){p->Box(Modifier.fillMaxSize().padding(p)){when(tab){
  "home"->Home(db){tab=it};"assistant"->Assistant(db);"alerts"->Appointments(db);"settings"->Settings(db)
  "customers"->Customers(db);"inventory"->Inventory(db);"reports"->Reports(db);"messages"->Messages()
  else->Home(db){tab=it}
 }}}
}
@Composable fun Nav(id:String,l:String,i:ImageVector,s:String,on:()->Unit){
 NavigationBarItem(selected=s==id,onClick=on,icon={Icon(i,contentDescription=l)},
  label={Text(l,maxLines=1,overflow=android@Composable fun Home(db:AppDb,go:(String)->Unit){
 val cs=db.customers();val ps=db.products();val asx=db.appointments()
 BoxWithConstraints(Modifier.fillMaxSize()){
  val width=maxWidth
  val horizontal=if(width<360.dp)12.dp else if(width<600.dp)16.dp else 24.dp
  val columns=when{width<420.dp->2;width<720.dp->3;else->4}
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal,horizontal,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{
    Text("المساعد الذكي",fontSize=if(width<360.dp)23.sp else 26.sp,fontWeight=FontWeight.Bold,color=Ink)
    Text("لوحة التحكم",fontSize=14.sp,color=Color.Gray)
   }
   item{
    Surface(Modifier.fillMaxWidth().clickable{go("assistant")},RoundedCornerShape(20.dp),color=Blue){
     Row(Modifier.padding(if(width<360.dp)14.dp else 18.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Default.AutoAwesome,null,tint=Color.White,modifier=Modifier.size(if(width<360.dp)32.dp else 38.dp))
      Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){
       Text("كيف أساعدك اليوم؟",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
       Text("اسأل عن العملاء والمواعيد والمخزون.",fontSize=13.sp,color=Color.White,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      }
     }
    }
   }
   item{
    LazyVerticalGrid(columns=GridCells.Fixed(columns),modifier=Modifier.fillMaxWidth().height(if(columns==2)150.dp else 104.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),userScrollEnabled=false){
     item{Stat("العملاء",cs.size,Icons.Default.People,Blue)}
     item{Stat("المواعيد",asx.size,Icons.Default.EventNote,Teal)}
     item{Stat("الأصناف",ps.size,Icons.Default.Inventory2,Color(0xFF7A55D8))}
     item{Stat("بأرصدة",cs.count{it.balance!=0.0},Icons.Default.AccountBalanceWallet,Color(0xFFE88A18))}
    }
   }
   item{Text("الوصول السريع",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink)}
   item{
    val cards=listOf("customers" to ("العملاء والأرصدة" to Icons.Default.People),"alerts" to ("الاستحقاقات" to Icons.Default.EventNote),"inventory" to ("المخزون" to Icons.Default.Inventory2),"reports" to ("التقارير" to Icons.Default.Assessment),"messages" to ("الرسائل" to Icons.Default.Message))
    val rows=(cards.size+columns-1)/columns
    LazyVerticalGrid(columns=GridCells.Fixed(columns),modifier=Modifier.fillMaxWidth().height((rows*100).dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp),userScrollEnabled=false){
     gridItems(cards){(id,x)->Quick(x.first,x.second){go(id)}}
    }
   }
  }
 }
}
>Quick(x.first,x.second,Modifier.weight(1f)){go(id)}}};Spacer(Modifier.height(8.dp))}
 }
}
@Composable fun Stat(t:String,n:Int,i:ImageVector,c:Color,modifier:Modifier=Modifier)=Surface(modifier.fillMaxWidth().heightIn(min=72.dp),RoundedCornerShape(14.dp),color=Color.White){Row(Modifier.fillMaxSize().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Icon(i,null,tint=c,modifier=Modifier.size(24.dp));Spacer(Modifier.width(7.dp));Column(Modifier.weight(1f)){Text(n.toString(),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1);Text(t,fontSize=11.sp,color=Color.Gray,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}}
@Composable fun Quick(t:String,i:ImageVector,modifier:Modifier=Modifier,on:()->Unit)=Surface(modifier.fillMaxWidth().heightIn(min=88.dp).clickable(onClick=on),RoundedCornerShape(14.dp),color=Color.White){Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.Center){Icon(i,null,tint=Blue);Spacer(Modifier.height(7.dp));Text(t,fontSize=14.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}

@Composable fun Customers(db:AppDb){var q by remember{mutableStateOf("")};var add by remember{mutableStateOf(false)};val list=db.customers().filter{it.name.contains(q,true)||it.phone.contains(q)};Page("العملاء والأرصدة",Icons.Default.People){Outlined("بحث",q){q=it};Button(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("إضافة عميل")};LazyColumn{items(list){c->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Text(c.name,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Ink);Text(c.phone.ifBlank{"بدون رقم"},fontSize=12.sp,color=Color.Gray);Text("الرصيد: ${fmt(c.balance)} ${c.currency}",fontSize=14.sp,fontWeight=FontWeight.Bold,color=Blue)}}}}};if(add)CustomerDialog(db){add=false}}
@Composable fun CustomerDialog(db:AppDb,done:()->Unit){var n by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};var b by remember{mutableStateOf("")};var c by remember{mutableStateOf("ر.ي")};AlertDialog(onDismissRequest=done,title={Text("إضافة عميل")},text={Column(Modifier.verticalScroll(rememberScrollState())){Outlined("الاسم",n){n=it};Outlined("الهاتف",p){p=it};Outlined("الرصيد",b){b=it};Outlined("العملة",c){c=it}}},confirmButton={Button(onClick={if(n.isNotBlank()){db.saveCustomer(n,p,b.toDoubleOrNull()?:0.0,c);done()}}){Text("حفظ")}},dismissButton={TextButton(onClick=done){Text("إلغاء")}})}

@Composable fun Inventory(db:AppDb){var add by remember{mutableStateOf(false)};val list=db.products();Page("المخزون",Icons.Default.Inventory2){Button(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("إضافة صنف")};LazyColumn{items(list){p->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(14.dp)){Column(Modifier.weight(1f)){Text(p.name,fontSize=15.sp,fontWeight=FontWeight.Bold,color=Ink);Text("${p.category} • ${p.warehouse}",fontSize=12.sp,color=Color.Gray)};Text("${fmt(p.qty)} ${p.unit}",fontSize=14.sp,fontWeight=FontWeight.Bold,color=if(p.qty<=0)Color.Red else Teal)}}}}};if(add)ProductDialog(db){add=false}}
@Composable fun ProductDialog(db:AppDb,done:()->Unit){var n by remember{mutableStateOf("")};var q by remember{mutableStateOf("")};var u by remember{mutableStateOf("حبة")};var w by remember{mutableStateOf("الرئيسي")};var cat by remember{mutableStateOf("عام")};AlertDialog(onDismissRequest=done,title={Text("إضافة صنف")},text={Column(Modifier.verticalScroll(rememberScrollState())){Outlined("اسم الصنف",n){n=it};Outlined("الكمية",q){q=it};Outlined("الوحدة",u){u=it};Outlined("المخزن",w){w=it};Outlined("الفئة",cat){cat=it}}},confirmButton={Button(onClick={if(n.isNotBlank()){db.saveProduct(n,q.toDoubleOrNull()?:0.0,u,w,cat);done()}}){Text("حفظ")}},dismissButton={TextButton(onClick=done){Text("إلغاء")}})}

@Composable fun Appointments(db:AppDb){var add by remember{mutableStateOf(false)};val cs=db.customers();val list=db.appointments();Page("الاستحقاقات والمتابعة",Icons.Default.EventNote){Button(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("موعد جديد")};LazyColumn{items(list){a->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Text(a.customer,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Ink);Text("${a.date} • ${a.time}",fontSize=13.sp,color=Blue);Text(a.reason,fontSize=13.sp,color=Color.Gray)}}}}};if(add)AppointmentDialog(db,cs){add=false}}
@Composable fun AppointmentDialog(db:AppDb,cs:List<Customer>,done:()->Unit){var cid by remember{mutableStateOf(cs.firstOrNull()?.id?:0)};var d by remember{mutableStateOf("")};var t by remember{mutableStateOf("")};var r by remember{mutableStateOf("")};AlertDialog(onDismissRequest=done,title={Text("موعد جديد")},text={Column(Modifier.verticalScroll(rememberScrollState())){if(cs.isEmpty())Text("أضف عميلًا أولًا.");else{Outlined("رقم العميل",cid.toString()){cid=it.toLongOrNull()?:cid};Outlined("التاريخ",d){d=it};Outlined("الوقت",t){t=it};Outlined("سبب الموعد",r){r=it}}}},confirmButton={Button(onClick={if(cid>0&&d.isNotBlank()){db.saveAppointment(cid,d,t,r);done()}}){Text("حفظ")}},dismissButton={TextButton(onClick=done){Text("إلغاء")}})}

@Composable fun Reports(db:AppDb){val c=db.customers();val p=db.products();val a=db.appointments();Page("التقارير",Icons.Default.Assessment){R("إجمالي العملاء",c.size.toString());R("العملاء ذوو الأرصدة",c.count{it.balance!=0.0}.toString());R("إجمالي الأرصدة",fmt(c.sumOf{it.balance}));R("المواعيد",a.size.toString());R("الأصناف منخفضة المخزون",p.count{it.qty<=5}.toString())}}
@Composable fun R(t:String,v:String)=Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(t,color=Ink);Text(v,fontWeight=FontWeight.Bold,color=Blue)}}
@Composable fun Messages()=Page("الرسائل",Icons.Default.Message){listOf("نذكركم بموعدكم المحدد.","نذكركم بمتابعة الاستحقاق.","مرحبًا، نود الاطمئنان والمتابعة معكم.").forEach{Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(it,Modifier.padding(16.dp),color=Ink)}}}

@Composable fun Assistant(db:AppDb){var q by remember{mutableStateOf("")};var a by remember{mutableStateOf("اسألني عن بيانات التطبيق الفعلية.")};val c=db.customers();val p=db.products();val ap=db.appointments();Page("المساعد الذكي",Icons.Default.AutoAwesome){Text("اسأل بلغة طبيعية",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink);Outlined("اكتب سؤالك",q){q=it};Button(onClick={a=answer(q,c,p,ap)},modifier=Modifier.fillMaxWidth()){Text("تحليل البيانات")};Card(Modifier.fillMaxWidth().padding(top=12.dp)){Text(a,Modifier.padding(16.dp),color=Ink)}}}
fun answer(q:String,c:List<Customer>,p:List<Product>,a:List<Appointment>):String=when{q.contains("عدد")&&q.contains("عمل") -> "عدد العملاء: ${c.size}";q.contains("رصيد")||q.contains("أرصدة")->"عملاء بأرصدة: ${c.count{it.balance!=0.0}}\\nإجمالي الأرصدة: ${fmt(c.sumOf{it.balance})}";q.contains("مخزون")||q.contains("أصناف")->"الأصناف: ${p.size}\\nالمنخفضة أو النافدة: ${p.count{it.qty<=5}}";q.contains("موعد")||q.contains("اليوم")->"المواعيد المسجلة: ${a.size}";else->"لم أفهم السؤال. جرّب: كم عدد العملاء؟ أو كم إجمالي الأرصدة؟ أو ما الأصناف التي قاربت على النفاد؟"}

@Composable fun Settings(db:AppDb){var n by remember{mutableStateOf(db.setting("name"))};var ph by remember{mutableStateOf(db.setting("phone"))};var ok by remember{mutableStateOf(false)};Page("الإعدادات",Icons.Default.Settings){Outlined("اسم المنشأة",n){n=it};Outlined("الهاتف",ph){ph=it};Button(onClick={db.saveSetting("name",n);db.saveSetting("phone",ph);ok=true},modifier=Modifier.fillMaxWidth()){Text("حفظ")};if(ok)Text("تم حفظ الإعدادات.",color=Teal);Spacer(Modifier.height(16.dp));Text("البيانات محفوظة محليًا على الجهاز.",fontSize=12.sp,color=Color.Gray)}}

@Composable fun Page(title:String,icon:ImageVector,content:@Composable ColumnScope.()->Unit){BoxWithConstraints(Modifier.fillMaxSize()){val pad=if(maxWidth<360.dp)12.dp else if(maxWidth<600.dp)18.dp else 24.dp;Column(Modifier.fillMaxSize().padding(horizontal=pad,vertical=14.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Blue);Spacer(Modifier.width(10.dp));Text(title,fontSize=if(maxWidth<360.dp)20.sp else 24.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))};Spacer(Modifier.height(12.dp));Column(Modifier.fillMaxSize(),content=content)}}}
@Composable fun Outlined(label:String,v:String,on:(String)->Unit)=OutlinedTextField(v,on,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),singleLine=true)
fun fmt(v:Double)=String.format(Locale.US,"%,.0f",v)
