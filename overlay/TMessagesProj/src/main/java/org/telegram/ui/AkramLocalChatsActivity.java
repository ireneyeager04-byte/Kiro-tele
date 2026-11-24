package org.telegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** AKRAM TELE local custom conversations. Prototype storage is device-local. */
public class AkramLocalChatsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout list;
    private JSONArray chats;
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView button(String text) {
        TextView t = new TextView(this); t.setText(text); t.setTextColor(Color.WHITE);
        t.setTextSize(16); t.setPadding(dp(16),dp(13),dp(16),dp(13));
        t.setBackgroundColor(0xff527da3); t.setGravity(Gravity.CENTER);
        return t;
    }
    private EditText field(String hint, String initial) {
        EditText e=new EditText(this); e.setHint(hint); if(initial!=null)e.setText(initial);
        return e;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=getSharedPreferences("akram_local_chats",MODE_PRIVATE);
        getWindow().getDecorView().setBackgroundColor(0xfff4f6f8);
        try { chats=new JSONArray(prefs.getString("chats","[]")); } catch(Exception e) { chats=new JSONArray(); }
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        TextView header=new TextView(this); header.setText("AKRAM TELE • محادثات مخصصة"); header.setTextSize(20); header.setTextColor(0xff20252b); header.setPadding(dp(18),dp(20),dp(18),dp(14)); root.addView(header);
        TextView add=button("+ إنشاء محادثة"); add.setOnClickListener(v->createChat()); root.addView(add);
        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); refresh();
    }
    private String lastMessage(JSONObject chat) {
        JSONArray a=chat.optJSONArray("messages");
        if(a!=null&&a.length()>0){JSONObject m=a.optJSONObject(a.length()-1); if(m!=null)return m.optString("text","");}
        return chat.optString("preview","لا توجد رسائل");
    }
    private void refresh() {
        list.removeAllViews();
        for(int i=chats.length()-1;i>=0;i--) { final int index=i; JSONObject c=chats.optJSONObject(i); if(c==null)continue;
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(16),dp(12),dp(16),dp(12)); row.setBackgroundColor(Color.WHITE);
            TextView title=new TextView(this); title.setText(c.optString("avatar","👤")+"  "+c.optString("name","محادثة")); title.setTextColor(0xff20252b); title.setTextSize(17);
            TextView preview=new TextView(this); preview.setText("ID: "+c.optString("id","—")+"\n"+lastMessage(c)); preview.setTextColor(0xff59636d); preview.setTextSize(14);
            row.addView(title); row.addView(preview); row.setOnClickListener(v->openChat(index)); row.setOnLongClickListener(v->{chatOptions(index);return true;}); list.addView(row);
            TextView divider=new TextView(this); divider.setHeight(dp(1)); divider.setBackgroundColor(0xffe4e8eb); list.addView(divider);
        }
    }
    private void createChat() {
        LinearLayout form=new LinearLayout(this); form.setPadding(dp(8),dp(4),dp(8),dp(4)); form.setOrientation(LinearLayout.VERTICAL);
        EditText name=field("اسم المحادثة",null), id=field("ID مخصص (اختياري)",null), avatar=field("رمز/إيموجي الصورة", "👤");
        form.addView(name); form.addView(id); form.addView(avatar);
        new AlertDialog.Builder(this).setTitle("إنشاء محادثة مخصصة").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",(d,w)->{
            JSONObject obj=new JSONObject(); try { obj.put("name",name.getText().toString().trim().isEmpty()?"محادثة جديدة":name.getText().toString().trim()); obj.put("id",id.getText().toString().trim()); obj.put("avatar",avatar.getText().toString()); obj.put("messages",new JSONArray()); chats.put(obj); save(); refresh(); } catch(Exception ignored){}
        }).show();
    }
    private void chatOptions(int index) {
        JSONObject chat=chats.optJSONObject(index); if(chat==null)return;
        new AlertDialog.Builder(this).setTitle(chat.optString("name")).setItems(new String[]{"تعديل الاسم وID والرمز","حذف المحادثة"},(d,which)->{
            if(which==0)editChat(index); else new AlertDialog.Builder(this).setMessage("حذف هذه المحادثة ورسائلها المحلية؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(x,y)->{JSONArray n=new JSONArray();for(int i=0;i<chats.length();i++)if(i!=index)n.put(chats.opt(i));chats=n;save();refresh();}).show();
        }).show();
    }
    private void editChat(int index) {
        JSONObject c=chats.optJSONObject(index); if(c==null)return;
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(8),dp(4),dp(8),dp(4));
        EditText name=field("الاسم",c.optString("name")), id=field("ID",c.optString("id")), avatar=field("رمز/إيموجي",c.optString("avatar","👤"));
        form.addView(name);form.addView(id);form.addView(avatar);
        new AlertDialog.Builder(this).setTitle("تعديل المحادثة").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{c.put("name",name.getText().toString());c.put("id",id.getText().toString());c.put("avatar",avatar.getText().toString());save();refresh();}catch(Exception ignored){}}).show();
    }
    private void openChat(int idx) {
        JSONObject chat=chats.optJSONObject(idx); if(chat==null)return;
        JSONArray msgs=chat.optJSONArray("messages"); if(msgs==null)msgs=new JSONArray();
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(6),dp(8),dp(6));
        ScrollView scroll=new ScrollView(this); TextView history=new TextView(this); history.setTextColor(0xff20252b); history.setTextSize(15); StringBuilder sb=new StringBuilder();
        for(int i=0;i<msgs.length();i++){ JSONObject m=msgs.optJSONObject(i); if(m!=null)sb.append(m.optString("sender","أنا")).append(" • ").append(m.optString("time","")).append("\n").append(m.optString("text")).append("\n\n");}
        history.setText(sb.length()==0?"لا توجد رسائل بعد":sb.toString()); scroll.addView(history); box.addView(scroll,new LinearLayout.LayoutParams(-1,dp(280)));
        EditText sender=field("اسم المرسل", "أنا"), input=field("نص الرسالة",null); box.addView(sender); box.addView(input);
        new AlertDialog.Builder(this).setTitle(chat.optString("name")).setView(box).setNegativeButton("إغلاق",null).setNeutralButton("تعديل المحادثة",(d,w)->editChat(idx)).setPositiveButton("إضافة رسالة",(d,w)->{
            try{ JSONArray arr=chat.optJSONArray("messages"); if(arr==null)arr=new JSONArray(); JSONObject msg=new JSONObject(); msg.put("sender",sender.getText().toString().trim().isEmpty()?"أنا":sender.getText().toString()); msg.put("text",input.getText().toString()); msg.put("time",new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date())); arr.put(msg); chat.put("messages",arr); chat.put("preview",input.getText().toString()); save(); refresh(); }catch(Exception ignored){}
        }).show();
    }
    private void save(){ prefs.edit().putString("chats",chats.toString()).apply(); }
}
