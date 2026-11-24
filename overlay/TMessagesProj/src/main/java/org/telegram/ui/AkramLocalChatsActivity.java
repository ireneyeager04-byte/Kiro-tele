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

/** AKRAM TELE local-only custom conversation prototype. */
public class AkramLocalChatsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout list;
    private JSONArray chats;
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView button(String text) {
        TextView t = new TextView(this); t.setText(text); t.setTextColor(Color.WHITE);
        t.setTextSize(16); t.setPadding(dp(16),dp(14),dp(16),dp(14));
        t.setBackgroundColor(0xff527da3); t.setGravity(Gravity.CENTER);
        return t;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=getSharedPreferences("akram_local_chats",MODE_PRIVATE);
        getWindow().getDecorView().setBackgroundColor(0xfff4f6f8);
        try { chats=new JSONArray(prefs.getString("chats","[]")); } catch(Exception e){ chats=new JSONArray(); }
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        TextView header=new TextView(this); header.setText("AKRAM TELE • محادثات محلية"); header.setTextSize(20); header.setTextColor(0xff20252b); header.setPadding(dp(18),dp(20),dp(18),dp(14)); root.addView(header);
        TextView add=button("+ إنشاء محادثة مخصصة"); add.setOnClickListener(v->createChat()); root.addView(add);
        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); refresh();
    }
    private void refresh() {
        list.removeAllViews();
        for(int i=chats.length()-1;i>=0;i--) { final int index=i; JSONObject c=chats.optJSONObject(i); if(c==null)continue;
            TextView row=new TextView(this); row.setText(c.optString("name","محادثة")+"\nID: "+c.optString("id")+" • "+c.optString("preview","لا توجد رسائل")); row.setTextColor(0xff20252b); row.setTextSize(16); row.setPadding(dp(18),dp(15),dp(18),dp(15)); row.setBackgroundColor(Color.WHITE);
            row.setOnClickListener(v->openChat(index)); list.addView(row);
        }
    }
    private void createChat() {
        LinearLayout form=new LinearLayout(this); form.setPadding(dp(8),dp(4),dp(8),dp(4)); form.setOrientation(LinearLayout.VERTICAL);
        EditText name=new EditText(this); name.setHint("اسم المحادثة"); form.addView(name);
        EditText id=new EditText(this); id.setHint("ID (اختياري)"); form.addView(id);
        new AlertDialog.Builder(this).setTitle("إنشاء محادثة محلية").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",(d,w)->{
            JSONObject obj=new JSONObject(); try { obj.put("name",name.getText().toString().trim().isEmpty()?"محادثة جديدة":name.getText().toString()); obj.put("id",id.getText().toString()); obj.put("messages",new JSONArray()); chats.put(obj); save(); refresh(); } catch(Exception ignored){}
        }).show();
    }
    private void openChat(int idx) {
        JSONObject chat=chats.optJSONObject(idx); if(chat==null)return;
        JSONArray msgs=chat.optJSONArray("messages"); if(msgs==null)msgs=new JSONArray();
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(6),dp(8),dp(6));
        TextView history=new TextView(this); history.setTextColor(0xff20252b); history.setTextSize(15); StringBuilder sb=new StringBuilder();
        for(int i=0;i<msgs.length();i++){ JSONObject m=msgs.optJSONObject(i); if(m!=null)sb.append(m.optString("sender","أنا")).append(": ").append(m.optString("text")).append("\n");}
        history.setText(sb.length()==0?"لا توجد رسائل بعد":sb.toString()); box.addView(history);
        EditText input=new EditText(this); input.setHint("اكتب رسالة محلية"); box.addView(input);
        new AlertDialog.Builder(this).setTitle(chat.optString("name")).setView(box).setNegativeButton("إغلاق",null).setPositiveButton("إضافة رسالة",(d,w)->{
            try{ JSONArray arr=chat.optJSONArray("messages"); if(arr==null)arr=new JSONArray(); JSONObject msg=new JSONObject(); msg.put("sender","أنا"); msg.put("text",input.getText().toString()); arr.put(msg); chat.put("messages",arr); chat.put("preview",input.getText().toString()); save(); refresh(); }catch(Exception ignored){}
        }).show();
    }
    private void save(){ prefs.edit().putString("chats",chats.toString()).apply(); }
}
