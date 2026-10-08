package com.smartassistant.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import org.json.JSONObject
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.*

private val Blue=Color(0xFF1769E0); private val Teal=Color(0xFF0BA6A6); private val Page=Color(0xFFF7F9FC); private val Ink=Color(0xFF17324D)

data class Customer(val id:Long,val name:String,val phone:String,val balance:Double,val currency:String)
data class Appointment(val id:Long,val customer:String,val date:String,val time:String,val reason:String,val status:String)
data class Product(val id:Long,val name:String,val qty:Double,val unit:String,val warehouse:String,val category:String)

class AppDb(c:Context):SQLiteOpenHelper(c,"smart_assistant.db",null,2){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT,address TEXT DEFAULT '',notes TEXT DEFAULT '',balance REAL NOT NULL DEFAULT 0,currency TEXT NOT NULL DEFAULT 'ر.ي')")
  db.execSQL("CREATE TABLE appointments(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,date TEXT NOT NULL,time TEXT,reason TEXT,status TEXT NOT NULL DEFAULT 'upcoming',amount REAL NOT NULL DEFAULT 0,currency TEXT NOT NULL DEFAULT 'ر.ي',notes TEXT DEFAULT '')")
  db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,qty REAL NOT NULL DEFAULT 0,unit TEXT,warehouse TEXT,category TEXT,inventory_date TEXT DEFAULT '')")
  db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT)")
  db.execSQL("CREATE TABLE audit(id INTEGER PRIMARY KEY AUTOINCREMENT,action TEXT NOT NULL,details TEXT NOT NULL,date TEXT NOT NULL)")
 }
 override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){
  if(o<2){
   addColumn(db,"customers","address","TEXT DEFAULT ''");addColumn(db,"customers","notes","TEXT DEFAULT ''")
   addColumn(db,"appointments","amount","REAL NOT NULL DEFAULT 0");addColumn(db,"appointments","currency","TEXT NOT NULL DEFAULT 'ر.ي'");addColumn(db,"appointments","notes","TEXT DEFAULT ''")
   addColumn(db,"products","inventory_date","TEXT DEFAULT ''")
   db.execSQL("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY AUTOINCREMENT,action TEXT NOT NULL,details TEXT NOT NULL,date TEXT NOT NULL)")
  }
 }
 private fun addColumn(db:SQLiteDatabase,t:String,c:String,type:String){try{db.execSQL("ALTER TABLE $t ADD COLUMN $c $type")}catch(_:Exception){}}
 private fun now():String=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(Date())
 private fun audit(action:String,details:String){val v=ContentValues();v.put("action",action);v.put("details",details);v.put("date",now());writableDatabase.insert("audit",null,v)}
 fun customers():List<Customer>{val r=mutableListOf<Customer>();readableDatabase.rawQuery("SELECT id,name,phone,balance,currency FROM customers ORDER BY name",null).use{c->while(c.moveToNext())r+=Customer(c.getLong(0),c.getString(1),c.getString(2)?:"",c.getDouble(3),c.getString(4)?:"ر.ي")};return r}
 fun saveCustomer(id:Long?,n:String,p:String,b:Double,cur:String){val v=ContentValues();v.put("name",n);v.put("phone",p);v.put("balance",b);v.put("currency",cur);if(id==null){writableDatabase.insert("customers",null,v);audit("إضافة عميل",n)}else{writableDatabase.update("customers",v,"id=?",arrayOf(id.toString()));audit("تعديل عميل",n)}}
 fun saveCustomer(n:String,p:String,b:Double,cur:String){saveCustomer(null,n,p,b,cur)}
 fun deleteCustomer(id:Long){writableDatabase.delete("appointments","customer_id=?",arrayOf(id.toString()));writableDatabase.delete("customers","id=?",arrayOf(id.toString()));audit("حذف عميل",id.toString())}
 fun appointments():List<Appointment>{val r=mutableListOf<Appointment>();readableDatabase.rawQuery("SELECT a.id,c.name,a.date,a.time,a.reason,a.status FROM appointments a JOIN customers c ON c.id=a.customer_id ORDER BY a.date,a.time",null).use{c->while(c.moveToNext())r+=Appointment(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)?:"",c.getString(4)?:"",c.getString(5)?:"upcoming")};return r}
 fun saveAppointment(cid:Long,d:String,t:String,reason:String){val v=ContentValues();v.put("customer_id",cid);v.put("date",d);v.put("time",t);v.put("reason",reason);v.put("status","upcoming");writableDatabase.insert("appointments",null,v);audit("إضافة موعد",reason)}
 fun deleteAppointment(id:Long){writableDatabase.delete("appointments","id=?",arrayOf(id.toString()));audit("حذف موعد",id.toString())}
 fun updateAppointmentStatus(id:Long,status:String){val v=ContentValues();v.put("status",status);writableDatabase.update("appointments",v,"id=?",arrayOf(id.toString()));audit("تغيير حالة موعد",status)}
 fun products():List<Product>{val r=mutableListOf<Product>();readableDatabase.rawQuery("SELECT id,name,qty,unit,warehouse,category FROM products ORDER BY name",null).use{c->while(c.moveToNext())r+=Product(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3)?:"حبة",c.getString(4)?:"الرئيسي",c.getString(5)?:"عام")};return r}
 fun saveProduct(n:String,q:Double,u:String,w:String,cat:String){val v=ContentValues();v.put("name",n);v.put("qty",q);v.put("unit",u);v.put("warehouse",w);v.put("category",cat);writableDatabase.insert("products",null,v);audit("إضافة صنف",n)}
 fun updateProduct(id:Long,n:String,q:Double,u:String,w:String,cat:String){val v=ContentValues();v.put("name",n);v.put("qty",q);v.put("unit",u);v.put("warehouse",w);v.put("category",cat);writableDatabase.update("products",v,"id=?",arrayOf(id.toString()));audit("تعديل صنف",n)}
fun deleteProduct(id:Long){writableDatabase.delete("products","id=?",arrayOf(id.toString()));audit("حذف صنف",id.toString())}
 fun setting(k:String)=readableDatabase.rawQuery("SELECT value FROM settings WHERE key=?",arrayOf(k)).use{if(it.moveToFirst())it.getString(0) else ""}
 fun backupJson():String{
  val root=JSONObject()
  arrayOf("customers","appointments","products","settings").forEach{t->
   val arr=JSONArray()
   readableDatabase.rawQuery("SELECT * FROM $t",null).use{c->while(c.moveToNext()){
    val o=JSONObject();for(i in 0 until c.columnCount){if(c.isNull(i))o.put(c.getColumnName(i),JSONObject.NULL) else when(c.getType(i)){1->o.put(c.getColumnName(i),c.getLong(i));2->o.put(c.getColumnName(i),c.getDouble(i));else->o.put(c.getColumnName(i),c.getString(i))}};arr.put(o)
   }};root.put(t,arr)
  };return root.toString()
 }
 fun restoreJson(json:String){
  val root=JSONObject(json);val d=writableDatabase;d.beginTransaction()
  try{
   arrayOf("appointments","customers","products","settings","audit").forEach{d.delete(it,null,null)}
   fun restoreTable(t:String){if(!root.has(t))return;val a=root.getJSONArray(t);for(i in 0 until a.length()){val o=a.getJSONObject(i);val v=ContentValues();val it=o.keys();while(it.hasNext()){val k=it.next();if(k!="id"&&!o.isNull(k))v.put(k,o.get(k).toString())};d.insert(t,null,v)}}
   restoreTable("customers");restoreTable("appointments");restoreTable("products");restoreTable("settings");d.setTransactionSuccessful()
  }finally{d.endTransaction()}
  audit("استعادة نسخة احتياطية","تمت استعادة البيانات")
 }
 fun saveSetting(k:String,v:String){val x=ContentValues();x.put("key",k);x.put("value",v);writableDatabase.insertWithOnConflict("settings",null,x,SQLiteDatabase.CONFLICT_REPLACE)}
 fun audits():List<String>{val r=mutableListOf<String>();readableDatabase.rawQuery("SELECT date,action,details FROM audit ORDER BY id DESC LIMIT 50",null).use{c->while(c.moveToNext())r+=c.getString(0)+" • "+c.getString(1)+" • "+c.getString(2)};return r}
}
class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  setContent{App(AppDb(this))}
  if(Build.VERSION.SDK_INT>=33)requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"),7001)
 }
}
class ReminderReceiver:android.content.BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val nm=context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
  val channel="appointments"
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(android.app.NotificationChannel(channel,"تذكيرات المواعيد",android.app.NotificationManager.IMPORTANCE_HIGH))
  val customer=intent.getStringExtra("customer")?:"العميل"
  val n=androidx.core.app.NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("تذكير بالموعد").setContentText("موعد "+customer+" حان الآن").setAutoCancel(true).build()
  nm.notify(intent.getLongExtra("id",1).toInt(),n)
 }
}

@Composable fun App(db:AppDb){
 MaterialTheme(colorScheme=lightColorScheme(primary=Blue,secondary=Teal,background=Page,surface=Color.White,onSurface=Ink)){
  CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl){Root(db)}
 }
}
@Composable fun Root(db:AppDb){
 var tab by remember{mutableStateOf("home")}
 Scaffold(containerColor=Page,bottomBar={
  NavigationBar(modifier=Modifier.fillMaxWidth()){
   Nav("home","الرئيسية",Icons.Default.Home,tab,Modifier.weight(1f)){tab="home"}
   Nav("assistant","المساعد",Icons.Default.AutoAwesome,tab,Modifier.weight(1f)){tab="assistant"}
   Nav("alerts","التنبيهات",Icons.Default.Notifications,tab,Modifier.weight(1f)){tab="alerts"}
   Nav("settings","الإعدادات",Icons.Default.Settings,tab,Modifier.weight(1f)){tab="settings"}
  }
 }){p->Box(Modifier.fillMaxSize().padding(p)){when(tab){
  "home"->Home(db){tab=it};"assistant"->Assistant(db);"alerts"->Appointments(db);"settings"->Settings(db)
  "customers"->Customers(db);"inventory"->Inventory(db);"reports"->Reports(db);"messages"->Messages(db); "audit"->Audit(db)
  else->Home(db){tab=it}
 }}}
}
@Composable fun Nav(id:String,l:String,i:ImageVector,s:String,modifier:Modifier=Modifier,on:()->Unit)=TextButton(onClick=on,modifier=modifier){
 Column(horizontalAlignment=Alignment.CenterHorizontally){
  Icon(i,contentDescription=l,tint=if(s==id)Blue else Color.Gray,modifier=Modifier.size(24.dp))
  Text(l,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,fontSize=10.sp,color=if(s==id)Blue else Color.Gray)
 }
}

@Composable
fun Home(db:AppDb,go:(String)->Unit){
 val cs=db.customers();val ps=db.products();val asx=db.appointments()
 BoxWithConstraints(Modifier.fillMaxSize()){
  val width=maxWidth
  val horizontal=when{width<360.dp->12.dp;width<600.dp->16.dp;else->24.dp}
  val columns=when{width<420.dp->2;width<720.dp->3;else->4}
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=horizontal,end=horizontal,top=16.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{
    Text("المساعد الذكي",fontSize=if(width<360.dp)23.sp else if(width<600.dp)26.sp else 28.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    Text("لوحة التحكم",fontSize=14.sp,color=Color.Gray)
   }
   item{
    Surface(Modifier.fillMaxWidth().clickable{go("assistant")},RoundedCornerShape(20.dp),color=Blue){
     Row(Modifier.padding(if(width<360.dp)14.dp else 18.dp),verticalAlignment=Alignment.CenterVertically){
      Icon(Icons.Default.AutoAwesome,null,tint=Color.White,modifier=Modifier.size(if(width<360.dp)32.dp else 38.dp))
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)){
       Text("كيف أساعدك اليوم؟",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
       Text("اسأل عن العملاء والمواعيد والمخزون.",fontSize=13.sp,color=Color.White,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      }
     }
    }
   }
   item{
    val stats=listOf(
     Triple("العملاء",cs.size to Icons.Default.People,Blue),
     Triple("المواعيد",asx.size to Icons.Default.EventNote,Teal),
     Triple("الأصناف",ps.size to Icons.Default.Inventory2,Color(0xFF7A55D8)),
     Triple("بأرصدة",cs.count{it.balance!=0.0} to Icons.Default.AccountBalanceWallet,Color(0xFFE88A18))
    )
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
     stats.chunked(columns).forEach{row->
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
       row.forEach{itemData->
        Stat(itemData.first,itemData.second.first,itemData.second.second,itemData.third,Modifier.weight(1f))
       }
       repeat(columns-row.size){Spacer(Modifier.weight(1f))}
      }
     }
    }
   }
   item{Text("الوصول السريع",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink)}
   item{
    val cards=listOf(
     "customers" to ("العملاء والأرصدة" to Icons.Default.People),
     "alerts" to ("الاستحقاقات" to Icons.Default.EventNote),
     "inventory" to ("المخزون" to Icons.Default.Inventory2),
     "reports" to ("التقارير" to Icons.Default.Assessment),
     "messages" to ("الرسائل" to Icons.Default.Message),
     "audit" to ("سجل العمليات" to Icons.Default.History)
    )
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
     cards.chunked(columns).forEach{row->
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
       row.forEach{itemData->
        Quick(itemData.second.first,itemData.second.second,Modifier.weight(1f)){go(itemData.first)}
       }
       repeat(columns-row.size){Spacer(Modifier.weight(1f))}
      }
     }
    }
   }
  }
 }
}
@Composable fun Stat(t:String,n:Int,i:ImageVector,c:Color,modifier:Modifier=Modifier)=Surface(modifier.fillMaxWidth().heightIn(min=72.dp),RoundedCornerShape(14.dp),color=Color.White){Row(Modifier.fillMaxSize().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Icon(i,null,tint=c,modifier=Modifier.size(24.dp));Spacer(Modifier.width(7.dp));Column(Modifier.weight(1f)){Text(n.toString(),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1);Text(t,fontSize=11.sp,color=Color.Gray,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}}
@Composable fun Quick(t:String,i:ImageVector,modifier:Modifier=Modifier,on:()->Unit)=Surface(modifier.fillMaxWidth().heightIn(min=88.dp).clickable(onClick=on),RoundedCornerShape(14.dp),color=Color.White){Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.Center){Icon(i,null,tint=Blue);Spacer(Modifier.height(7.dp));Text(t,fontSize=14.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}

@Composable fun Customers(db:AppDb){
 var q by remember{mutableStateOf("")};var add by remember{mutableStateOf(false)};var edit by remember{mutableStateOf<Customer?>(null)};var del by remember{mutableStateOf<Customer?>(null)}
 val list=db.customers().filter{it.name.contains(q,true)||it.phone.contains(q)}
 Page("العملاء والأرصدة",Icons.Default.People){
  Outlined("بحث بالاسم أو الهاتف",q){q=it}
  Button(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("إضافة عميل")}
  LazyColumn(Modifier.fillMaxWidth().weight(1f)){items(list){c->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold,color=Ink);Text(c.phone.ifBlank{"بدون رقم"},fontSize=12.sp,color=Color.Gray);Text("الرصيد: "+fmt(c.balance)+" "+c.currency,fontWeight=FontWeight.Bold,color=Blue)}
   IconButton(onClick={edit=c}){Icon(Icons.Default.Edit,"تعديل")};IconButton(onClick={del=c}){Icon(Icons.Default.Delete,"حذف")}
  }}}}
 }
 if(add)CustomerDialog(db,null){add=false};if(edit!=null)CustomerDialog(db,edit){edit=null}
 if(del!=null)AlertDialog(onDismissRequest={del=null},title={Text("حذف العميل؟")},text={Text("سيتم حذف مواعيده أيضًا.")},confirmButton={Button(onClick={db.deleteCustomer(del!!.id);del=null}){Text("حذف")}},dismissButton={TextButton(onClick={del=null}){Text("إلغاء")}})
}
@Composable fun CustomerDialog(db:AppDb,initial:Customer?,done:()->Unit){
 var n by remember{mutableStateOf(initial?.name?:"")};var p by remember{mutableStateOf(initial?.phone?:"")};var b by remember{mutableStateOf(initial?.balance?.toString()?:"")};var c by remember{mutableStateOf(initial?.currency?:"ر.ي")}
 AlertDialog(onDismissRequest=done,title={Text(if(initial==null)"إضافة عميل" else "تعديل عميل")},text={Column(Modifier.verticalScroll(rememberScrollState())){Outlined("الاسم",n){n=it};Outlined("الهاتف",p){p=it};Outlined("الرصيد",b){b=it};Outlined("العملة",c){c=it}}},confirmButton={Button(onClick={if(n.isNotBlank()){db.saveCustomer(initial?.id,n,p,b.toDoubleOrNull()?:0.0,c);done()}}){Text("حفظ")}},dismissButton={TextButton(onClick=done){Text("إلغاء")}})
}

@Composable fun Inventory(db:AppDb){
 var add by remember{mutableStateOf(false)};var edit by remember{mutableStateOf<Product?>(null)};var del by remember{mutableStateOf<Product?>(null)};val list=db.products()
 Page("المخزون",Icons.Default.Inventory2){
  Button(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("إضافة صنف")}
  LazyColumn(Modifier.fillMaxWidth().weight(1f)){items(list){p->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold,color=Ink);Text(p.category+" • "+p.warehouse,fontSize=12.sp,color=Color.Gray);Text(fmt(p.qty)+" "+p.unit,fontWeight=FontWeight.Bold,color=if(p.qty<=5)Color.Red else Teal)}
   IconButton(onClick={edit=p}){Icon(Icons.Default.Edit,"تعديل")};IconButton(onClick={del=p}){Icon(Icons.Default.Delete,"حذف")}
  }}}}
 }
 if(add)ProductDialog(db,null){add=false};if(edit!=null)ProductDialog(db,edit){edit=null}
 if(del!=null)AlertDialog(onDismissRequest={del=null},title={Text("حذف الصنف؟")},text={Text(del!!.name)},confirmButton={Button(onClick={db.deleteProduct(del!!.id);del=null}){Text("حذف")}},dismissButton={TextButton(onClick={del=null}){Text("إلغاء")}})
}
@Composable fun ProductDialog(db:AppDb,initial:Product?,done:()->Unit){
 var n by remember{mutableStateOf(initial?.name?:"")};var q by remember{mutableStateOf(initial?.qty?.toString()?:"")};var u by remember{mutableStateOf(initial?.unit?:"حبة")};var w by remember{mutableStateOf(initial?.warehouse?:"الرئيسي")};var cat by remember{mutableStateOf(initial?.category?:"عام")}
 AlertDialog(onDismissRequest=done,title={Text(if(initial==null)"إضافة صنف" else "تعديل صنف")},text={Column(Modifier.verticalScroll(rememberScrollState())){Outlined("اسم الصنف",n){n=it};Outlined("الكمية",q){q=it};Outlined("الوحدة",u){u=it};Outlined("المخزن",w){w=it};Outlined("الفئة",cat){cat=it}}},confirmButton={Button(onClick={if(n.isNotBlank()){if(initial==null)db.saveProduct(n,q.toDoubleOrNull()?:0.0,u,w,cat) else db.updateProduct(initial.id,n,q.toDoubleOrNull()?:0.0,u,w,cat);done()}}){Text("حفظ")}},dismissButton={TextButton(onClick=done){Text("إلغاء")}})
}

@Composable fun Appointments(db:AppDb){var add by remember{mutableStateOf(false)};val cs=db.customers();val list=db.appointments();Page("الاستحقاقات والمتابعة",Icons.Default.EventNote){Button(onClick={add=true},modifier=Modifier.fillMaxWidth()){Text("موعد جديد")};LazyColumn(Modifier.fillMaxWidth().weight(1f),contentPadding=PaddingValues(vertical=4.dp)){items(list){a->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Text(a.customer,fontSize=16.sp,fontWeight=FontWeight.Bold,color=Ink);Text("${a.date} • ${a.time}",fontSize=13.sp,color=Blue);Text(a.reason,fontSize=13.sp,color=Color.Gray)}}}}};if(add)AppointmentDialog(db,cs){add=false}}
@Composable fun AppointmentDialog(db:AppDb,cs:List<Customer>,done:()->Unit){
 val context=androidx.compose.ui.platform.LocalContext.current
 var cid by remember{mutableStateOf(cs.firstOrNull()?.id?:0)};var search by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var t by remember{mutableStateOf("")};var r by remember{mutableStateOf("")}
 val matches=cs.filter{it.name.contains(search,true)}.take(5)
 AlertDialog(onDismissRequest=done,title={Text("موعد جديد")},text={Column(Modifier.verticalScroll(rememberScrollState())){
  if(cs.isEmpty())Text("أضف عميلًا أولًا.") else {
   Outlined("اكتب اسم العميل",search){search=it}
   matches.forEach{c->TextButton(onClick={cid=c.id;search=c.name},modifier=Modifier.fillMaxWidth()){Text(c.name)}}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Button(onClick={val cal=Calendar.getInstance();android.app.DatePickerDialog(context,{_,y,m,day->d=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,day)},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show()},modifier=Modifier.weight(1f)){Text(if(d.isBlank())"اختيار التاريخ" else d)}
    Button(onClick={val cal=Calendar.getInstance();android.app.TimePickerDialog(context,{_,h,m->t=String.format(Locale.US,"%02d:%02d",h,m)},cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show()},modifier=Modifier.weight(1f)){Text(if(t.isBlank())"اختيار الوقت" else t)}
   }
   Outlined("سبب الموعد",r){r=it}
  }
 }},confirmButton={Button(onClick={
   if(cid>0&&d.isNotBlank()){db.saveAppointment(cid,d,t,r);val chosen=cs.find{it.id==cid};if(chosen!=null)scheduleReminder(context,db.appointments().lastOrNull()?.id?:0,chosen.name,d,t);done()}
 }){Text("حفظ")}},dismissButton={TextButton(onClick=done){Text("إلغاء")}})
}

@Composable fun Reports(db:AppDb){val c=db.customers();val p=db.products();val a=db.appointments();Page("التقارير",Icons.Default.Assessment){R("إجمالي العملاء",c.size.toString());R("العملاء ذوو الأرصدة",c.count{it.balance!=0.0}.toString());R("إجمالي الأرصدة",fmt(c.sumOf{it.balance}));R("المواعيد",a.size.toString());R("الأصناف منخفضة المخزون",p.count{it.qty<=5}.toString())}}
@Composable fun R(t:String,v:String)=Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(t,color=Ink);Text(v,fontWeight=FontWeight.Bold,color=Blue)}}
@Composable fun Messages(db:AppDb){
 val context=androidx.compose.ui.platform.LocalContext.current
 val templates=listOf("نذكركم بموعدكم المحدد.","نذكركم بمتابعة الاستحقاق.","مرحبًا، نود الاطمئنان والمتابعة معكم.")
 Page("الرسائل",Icons.Default.Message){
  templates.forEach{msg->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Text(msg,color=Ink);Spacer(Modifier.height(8.dp));Button(onClick={val i=android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse("https://wa.me/?text="+Uri.encode(msg)));context.startActivity(i)},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Send,null);Spacer(Modifier.width(6.dp));Text("فتح واتساب بالرسالة")}}}}
 }
}
@Composable fun Audit(db:AppDb){val logs=db.audits();Page("سجل العمليات",Icons.Default.History){if(logs.isEmpty())Text("لا توجد عمليات مسجلة.",color=Color.Gray) else LazyColumn{items(logs){Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){Text(it,Modifier.padding(12.dp),fontSize=12.sp,color=Ink)}}}}}


@Composable fun Assistant(db:AppDb){var q by remember{mutableStateOf("")};var a by remember{mutableStateOf("اسألني عن بيانات التطبيق الفعلية.")};val c=db.customers();val p=db.products();val ap=db.appointments();Page("المساعد الذكي",Icons.Default.AutoAwesome){Text("اسأل بلغة طبيعية",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink);Outlined("اكتب سؤالك",q){q=it};Button(onClick={a=answer(q,c,p,ap)},modifier=Modifier.fillMaxWidth()){Text("تحليل البيانات")};Card(Modifier.fillMaxWidth().padding(top=12.dp)){Text(a,Modifier.padding(16.dp),color=Ink)}}}
fun answer(q:String,c:List<Customer>,p:List<Product>,a:List<Appointment>):String=when{q.contains("عدد")&&q.contains("عمل") -> "عدد العملاء: ${c.size}";q.contains("رصيد")||q.contains("أرصدة")->"عملاء بأرصدة: ${c.count{it.balance!=0.0}}\\nإجمالي الأرصدة: ${fmt(c.sumOf{it.balance})}";q.contains("مخزون")||q.contains("أصناف")->"الأصناف: ${p.size}\\nالمنخفضة أو النافدة: ${p.count{it.qty<=5}}";q.contains("موعد")||q.contains("اليوم")->"المواعيد المسجلة: ${a.size}";else->"لم أفهم السؤال. جرّب: كم عدد العملاء؟ أو كم إجمالي الأرصدة؟ أو ما الأصناف التي قاربت على النفاد؟"}

@Composable fun Settings(db:AppDb){
 var n by remember{mutableStateOf(db.setting("name"))};var ph by remember{mutableStateOf(db.setting("phone"))};var addr by remember{mutableStateOf(db.setting("address"))};var ok by remember{mutableStateOf(false)};var msg by remember{mutableStateOf("")};var logo by remember{mutableStateOf(db.setting("logo_uri"))}
 val context=androidx.compose.ui.platform.LocalContext.current
 val saveLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri:Uri?->if(uri!=null)try{context.contentResolver.openOutputStream(uri)?.use{it.write(db.backupJson().toByteArray(Charsets.UTF_8))};msg="تم إنشاء النسخة الاحتياطية"}catch(_:Exception){msg="تعذر إنشاء النسخة"}}
 val restoreLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null)try{context.contentResolver.openInputStream(uri)?.use{db.restoreJson(it.readBytes().toString(Charsets.UTF_8))};msg="تمت الاستعادة بنجاح"}catch(_:Exception){msg="ملف النسخة غير صالح"}}
 val logoLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null){try{context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};logo=uri.toString();db.saveSetting("logo_uri",logo);msg="تم حفظ الشعار"}}
 val bitmap=remember(logo){if(logo.isBlank())null else try{context.contentResolver.openInputStream(Uri.parse(logo)).use{BitmapFactory.decodeStream(it)}?.asImageBitmap()}catch(_:Exception){null}}
 Page("الإعدادات",Icons.Default.Settings){
  if(bitmap!=null)Image(bitmap,"الشعار",Modifier.size(96.dp).align(Alignment.CenterHorizontally)) else Icon(Icons.Default.Business,null,tint=Blue,modifier=Modifier.size(72.dp).align(Alignment.CenterHorizontally))
  Outlined("اسم المنشأة",n){n=it};Outlined("الهاتف",ph){ph=it};Outlined("العنوان",addr){addr=it}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={logoLauncher.launch(arrayOf("image/*"))},modifier=Modifier.weight(1f)){Text("اختيار الشعار")};Button(onClick={db.saveSetting("name",n);db.saveSetting("phone",ph);db.saveSetting("address",addr);ok=true},modifier=Modifier.weight(1f)){Text("حفظ")}}
  Spacer(Modifier.height(8.dp));Button(onClick={saveLauncher.launch("smart-assistant-backup.json")},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Backup,null);Spacer(Modifier.width(6.dp));Text("إنشاء نسخة احتياطية")}
  OutlinedButton(onClick={restoreLauncher.launch(arrayOf("application/json","text/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Restore,null);Spacer(Modifier.width(6.dp));Text("استعادة / استيراد نسخة")}
  if(ok)Text("تم حفظ معلومات المنشأة.",color=Teal);if(msg.isNotBlank())Text(msg,color=if(msg.contains("غير صالح")||msg.contains("تعذر"))Color.Red else Teal,fontSize=13.sp)
  Spacer(Modifier.height(8.dp));Text("البيانات محفوظة محليًا على الجهاز.",fontSize=12.sp,color=Color.Gray)
 }}

@Composable fun Page(title:String,icon:ImageVector,content:@Composable ColumnScope.()->Unit){
 val screenWidth=android.content.res.Resources.getSystem().displayMetrics.widthPixels
 val pad=when{screenWidth<360*android.util.DisplayMetrics.DENSITY_DEFAULT->12.dp;screenWidth<600*android.util.DisplayMetrics.DENSITY_DEFAULT->18.dp;else->24.dp}
 Column(Modifier.fillMaxSize().padding(horizontal=pad,vertical=14.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
   Icon(icon,null,tint=Blue);Spacer(Modifier.width(10.dp))
   Text(title,fontSize=if(screenWidth<360*android.util.DisplayMetrics.DENSITY_DEFAULT)20.sp else 24.sp,fontWeight=FontWeight.Bold,color=Ink,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
  }
  Spacer(Modifier.height(12.dp));Column(Modifier.fillMaxWidth().weight(1f),content=content)
 }
}
@Composable fun Outlined(label:String,v:String,on:(String)->Unit)=OutlinedTextField(v,on,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),singleLine=true)
fun fmt(v:Double)=String.format(Locale.US,"%,.0f",v)


fun scheduleReminder(context:Context,id:Long,customer:String,date:String,time:String){
 if(id<=0L)return
 val at=runCatching{SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).parse(date+" "+time)!!.time}.getOrNull()?:return
 if(at<=System.currentTimeMillis())return
 val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
 if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())return
 val intent=Intent(context,ReminderReceiver::class.java).apply{putExtra("customer",customer);putExtra("id",id)}
 val pi=PendingIntent.getBroadcast(context,id.toInt(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
}

