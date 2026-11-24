package org.telegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.media.MediaRecorder;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AKRAM TELE local conversations & local channel notebook.
 * This data is local to this app and is NOT sent to Telegram servers.
 */
public class AkramLocalChatsActivity extends Activity {
    private static final int PICK_ATTACHMENT = 7001, AUDIO_PERMISSION = 7002;
    private SharedPreferences prefs;
    private LinearLayout list;
    private JSONArray chats, channels;
    private int recordingChat = -1;
    private MediaRecorder recorder;
    private File recordingFile;

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView button(String text) {
        TextView t = new TextView(this); t.setText(text); t.setTextColor(Color.WHITE);
        t.setTextSize(15); t.setPadding(dp(14),dp(11),dp(14),dp(11));
        t.setBackgroundColor(0xff527da3); t.setGravity(Gravity.CENTER); return t;
    }
    private EditText field(String hint, String initial) {
        EditText e=new EditText(this); e.setHint(hint); if(initial!=null)e.setText(initial); return e;
    }
    private LinearLayout vertical(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(14),dp(10),dp(14),dp(10));return t;}
    private String now(){return new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date());}

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs=getSharedPreferences("akram_local_chats",MODE_PRIVATE);
        getWindow().getDecorView().setBackgroundColor(0xfff4f6f8);
        try { chats=new JSONArray(prefs.getString("chats","[]")); } catch(Exception e) { chats=new JSONArray(); }
        try { channels=new JSONArray(prefs.getString("channels","[]")); } catch(Exception e) { channels=new JSONArray(); }
        LinearLayout root=vertical();
        TextView header=text("AKRAM TELE • مساحة المحادثات المحلية",20,0xff20252b); root.addView(header);
        LinearLayout actions=horizontal();
        TextView add=button("+ محادثة"); add.setOnClickListener(v->createChat()); actions.addView(add,new LinearLayout.LayoutParams(0,-2,1));
        TextView ch=button("إدارة القنوات"); ch.setOnClickListener(v->channelsScreen()); actions.addView(ch,new LinearLayout.LayoutParams(0,-2,1));
        TextView files=button("الوسائط"); files.setOnClickListener(v->mediaManager()); actions.addView(files,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions);
        ScrollView scroll=new ScrollView(this); list=vertical(); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); refresh();
    }
    private String lastMessage(JSONObject chat) {
        JSONArray a=chat.optJSONArray("messages");
        if(a!=null&&a.length()>0){JSONObject m=a.optJSONObject(a.length()-1); if(m!=null){String tx=m.optString("text","");String att=m.optString("attachmentName","");return tx.isEmpty()?(att.isEmpty()?"رسالة": "📎 "+att):tx;}}
        return chat.optString("preview","لا توجد رسائل");
    }
    private void refresh() {
        list.removeAllViews();
        for(int i=chats.length()-1;i>=0;i--) { final int index=i; JSONObject c=chats.optJSONObject(i); if(c==null)continue;
            LinearLayout row=vertical(); row.setPadding(dp(16),dp(12),dp(16),dp(12)); row.setBackgroundColor(Color.WHITE);
            TextView title=text(c.optString("avatar","👤")+"  "+c.optString("name","محادثة"),17,0xff20252b);
            TextView preview=text("ID: "+c.optString("id","—")+"\n"+lastMessage(c),14,0xff59636d);
            row.addView(title); row.addView(preview); row.setOnClickListener(v->openChat(index)); row.setOnLongClickListener(v->{chatOptions(index);return true;}); list.addView(row);
            View divider=new View(this); divider.setBackgroundColor(0xffe4e8eb); list.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        if(chats.length()==0)list.addView(text("لا توجد محادثات محلية. اضغط «محادثة» لإنشاء واحدة.",15,0xff66717b));
    }
    private void createChat() {
        LinearLayout form=vertical(); form.setPadding(dp(8),dp(4),dp(8),dp(4));
        EditText name=field("اسم المحادثة",null), id=field("ID مخصص (اختياري)",null), avatar=field("رمز/إيموجي", "👤");
        form.addView(name); form.addView(id); form.addView(avatar);
        new AlertDialog.Builder(this).setTitle("إنشاء محادثة محلية").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",(d,w)->{
            JSONObject obj=new JSONObject(); try { obj.put("name",name.getText().toString().trim().isEmpty()?"محادثة جديدة":name.getText().toString().trim()); obj.put("id",id.getText().toString().trim()); obj.put("avatar",avatar.getText().toString().trim()); obj.put("messages",new JSONArray()); obj.put("preview",""); chats.put(obj); save(); refresh(); } catch(Exception ignored){}
        }).show();
    }
    private void chatOptions(int index) {
        JSONObject chat=chats.optJSONObject(index); if(chat==null)return;
        new AlertDialog.Builder(this).setTitle(chat.optString("name")).setItems(new String[]{"تعديل الاسم وID والرمز","حذف المحادثة"},(d,which)->{
            if(which==0)editChat(index); else new AlertDialog.Builder(this).setMessage("حذف المحادثة المحلية ورسائلها؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(x,y)->{JSONArray n=new JSONArray();for(int i=0;i<chats.length();i++)if(i!=index)n.put(chats.opt(i));chats=n;save();refresh();}).show();
        }).show();
    }
    private void editChat(int index) {
        JSONObject c=chats.optJSONObject(index); if(c==null)return;
        LinearLayout form=vertical(); form.setPadding(dp(8),dp(4),dp(8),dp(4));
        EditText name=field("الاسم",c.optString("name")), id=field("ID",c.optString("id")), avatar=field("رمز/إيموجي",c.optString("avatar","👤"));
        form.addView(name);form.addView(id);form.addView(avatar);
        new AlertDialog.Builder(this).setTitle("تعديل المحادثة").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{c.put("name",name.getText().toString());c.put("id",id.getText().toString());c.put("avatar",avatar.getText().toString());save();refresh();}catch(Exception ignored){}}).show();
    }
    private void openChat(int idx) {
        JSONObject chat=chats.optJSONObject(idx); if(chat==null)return;
        JSONArray msgs=chat.optJSONArray("messages"); if(msgs==null)msgs=new JSONArray();
        LinearLayout box=vertical(); box.setPadding(dp(8),dp(6),dp(8),dp(6));
        ScrollView scroll=new ScrollView(this); TextView history=text("",15,0xff20252b); StringBuilder sb=new StringBuilder();
        for(int i=0;i<msgs.length();i++){JSONObject m=msgs.optJSONObject(i);if(m!=null){sb.append(m.optString("sender","أنا")).append(" • ").append(m.optString("time","")).append("\n").append(m.optString("text",""));String att=m.optString("attachmentName","");if(!att.isEmpty())sb.append("\n📎 ").append(att);String uri=m.optString("attachmentUri","");if(!uri.isEmpty())sb.append("\n").append(uri);sb.append("\n\n");}}
        history.setText(sb.length()==0?"لا توجد رسائل بعد":sb.toString()); scroll.addView(history); box.addView(scroll,new LinearLayout.LayoutParams(-1,dp(300)));
        EditText sender=field("اسم المرسل", "أنا"), input=field("نص الرسالة",null); box.addView(sender); box.addView(input);
        LinearLayout tools=horizontal();
        TextView attach=button("📎 ملف"); attach.setOnClickListener(v->{recordingChat=idx;pickAttachment();}); tools.addView(attach,new LinearLayout.LayoutParams(0,-2,1));
        TextView voice=button("🎙 صوت"); voice.setOnClickListener(v->{recordingChat=idx;voiceRecordDialog();}); tools.addView(voice,new LinearLayout.LayoutParams(0,-2,1));
        TextView edit=button("✏️ تحرير"); edit.setOnClickListener(v->editMessages(idx)); tools.addView(edit,new LinearLayout.LayoutParams(0,-2,1));
        box.addView(tools);
        new AlertDialog.Builder(this).setTitle(chat.optString("name")).setView(box).setNegativeButton("إغلاق",null).setPositiveButton("إرسال نص",(d,w)->{
            String value=input.getText().toString().trim();if(value.isEmpty()){Toast.makeText(this,"اكتب رسالة أولاً",Toast.LENGTH_SHORT).show();return;}addMessage(chat,sender.getText().toString(),value,null,null,"text");
        }).show();
    }
    private void addMessage(JSONObject chat,String sender,String body,String uri,String filename,String type){
        try{JSONArray arr=chat.optJSONArray("messages");if(arr==null)arr=new JSONArray();JSONObject m=new JSONObject();m.put("sender",sender==null||sender.trim().isEmpty()?"أنا":sender);m.put("text",body==null?"":body);m.put("time",now());m.put("attachmentUri",uri==null?"":uri);m.put("attachmentName",filename==null?"":filename);m.put("type",type==null?"text":type);arr.put(m);chat.put("messages",arr);chat.put("preview",body==null||body.isEmpty()?(filename==null?"رسالة صوتية":"📎 "+filename):body);save();refresh();Toast.makeText(this,"تم حفظ الرسالة محليًا",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"تعذر حفظ الرسالة",Toast.LENGTH_SHORT).show();}
    }
    private void pickAttachment(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivityForResult(i,PICK_ATTACHMENT);}catch(Exception e){Toast.makeText(this,"مدير الملفات غير متاح",Toast.LENGTH_SHORT).show();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_ATTACHMENT&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri u=data.getData();String name=u.getLastPathSegment();if(name==null)name=u.toString();JSONObject c=chats.optJSONObject(recordingChat);if(c!=null)addMessage(c,"أنا","",u.toString(),name,"file");
        }
    }
    private void voiceRecordDialog(){
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.RECORD_AUDIO},AUDIO_PERMISSION);return;}
        LinearLayout panel=vertical();TextView status=text("اضغط بدء، ثم إيقاف لحفظ تسجيل صوتي محلي.",15,0xff303840);panel.addView(status);
        final boolean[] active={false};
        final TextView toggle=button("بدء التسجيل");panel.addView(toggle);
        final AlertDialog dlg=new AlertDialog.Builder(this).setTitle("تسجيل رسالة صوتية").setView(panel).setNegativeButton("إلغاء",(d,w)->stopRecorder(false)).setPositiveButton("حفظ",null).create();
        toggle.setOnClickListener(v->{if(!active[0]){try{recordingFile=new File(getFilesDir(),"voice_"+System.currentTimeMillis()+".m4a");recorder=new MediaRecorder();recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setOutputFile(recordingFile.getAbsolutePath());recorder.prepare();recorder.start();active[0]=true;toggle.setText("إيقاف التسجيل");status.setText("جارٍ التسجيل… اضغط إيقاف ثم حفظ.");}catch(Exception e){stopRecorder(false);Toast.makeText(this,"تعذر بدء التسجيل",Toast.LENGTH_LONG).show();}}else{stopRecorder(true);active[0]=false;toggle.setText("تم إيقاف التسجيل");status.setText("تم إيقافه. اضغط حفظ لإضافته للمحادثة.");}});
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(active[0])stopRecorder(true);if(recordingFile!=null&&recordingFile.exists()&&recordingFile.length()>0){JSONObject c=chats.optJSONObject(recordingChat);if(c!=null)addMessage(c,"أنا","",Uri.fromFile(recordingFile).toString(),recordingFile.getName(),"voice");}dlg.dismiss();}));
        dlg.show();dlg.getButton(AlertDialog.BUTTON_POSITIVE).setText("حفظ");
    }
    private void stopRecorder(boolean keep){try{if(recorder!=null){if(keep)recorder.stop();recorder.reset();recorder.release();recorder=null;}}catch(Exception ignored){if(recorder!=null){try{recorder.release();}catch(Exception ignored2){}recorder=null;}}if(!keep&&recordingFile!=null)recordingFile.delete();}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==AUDIO_PERMISSION&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)voiceRecordDialog();else if(requestCode==AUDIO_PERMISSION)Toast.makeText(this,"إذن الميكروفون مطلوب للتسجيل",Toast.LENGTH_SHORT).show();}
    private void editMessages(int idx){
        JSONObject c=chats.optJSONObject(idx);JSONArray a=c==null?null:c.optJSONArray("messages");if(a==null||a.length()==0){Toast.makeText(this,"لا توجد رسائل لتعديلها",Toast.LENGTH_SHORT).show();return;}
        String[] items=new String[a.length()];for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);String t=m==null?"":m.optString("text","");String f=m==null?"":m.optString("attachmentName","");items[i]=(i+1)+". "+(t.isEmpty()?f:t);if(items[i].length()>55)items[i]=items[i].substring(0,55)+"…";}
        new AlertDialog.Builder(this).setTitle("اختر رسالة").setItems(items,(d,n)->messageOptions(idx,n)).show();
    }
    private void messageOptions(int chatIndex,int msgIndex){JSONObject c=chats.optJSONObject(chatIndex);JSONArray a=c==null?null:c.optJSONArray("messages");JSONObject m=a==null?null:a.optJSONObject(msgIndex);if(m==null)return;
        new AlertDialog.Builder(this).setTitle("إدارة الرسالة").setItems(new String[]{"تعديل النص","حذف الرسالة"},(d,w)->{if(w==0){EditText e=field("النص",m.optString("text"));new AlertDialog.Builder(this).setTitle("تعديل الرسالة").setView(e).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(x,y)->{try{m.put("text",e.getText().toString());save();refresh();}catch(Exception ignored){}}).show();}else{JSONArray n=new JSONArray();for(int i=0;i<a.length();i++)if(i!=msgIndex)n.put(a.opt(i));try{c.put("messages",n);save();refresh();}catch(Exception ignored){}}}).show();
    }
    private void channelsScreen(){
        LinearLayout panel=vertical();TextView create=button("+ قناة محلية");create.setOnClickListener(v->createChannel());panel.addView(create);
        ScrollView sv=new ScrollView(this);LinearLayout rows=vertical();sv.addView(rows);for(int i=0;i<channels.length();i++){JSONObject c=channels.optJSONObject(i);if(c==null)continue;final int n=i;TextView row=text("📢 "+c.optString("title")+"\n"+c.optString("description"),16,0xff222b33);row.setOnClickListener(v->channelOptions(n));rows.addView(row);}
        panel.addView(sv);new AlertDialog.Builder(this).setTitle("القنوات المحلية").setView(panel).setNegativeButton("إغلاق",null).show();
    }
    private void createChannel(){LinearLayout f=vertical();EditText title=field("اسم القناة",null),desc=field("الوصف",null);f.addView(title);f.addView(desc);new AlertDialog.Builder(this).setTitle("قناة محلية جديدة").setView(f).setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",(d,w)->{JSONObject o=new JSONObject();try{o.put("title",title.getText().toString());o.put("description",desc.getText().toString());o.put("posts",new JSONArray());channels.put(o);save();}catch(Exception ignored){}}).show();}
    private void channelOptions(int idx){JSONObject c=channels.optJSONObject(idx);if(c==null)return;new AlertDialog.Builder(this).setTitle(c.optString("title")).setItems(new String[]{"إضافة منشور","تعديل القناة","حذف القناة","عرض المنشورات"},(d,w)->{if(w==0)channelPost(idx);else if(w==1)editChannel(idx);else if(w==2){JSONArray n=new JSONArray();for(int i=0;i<channels.length();i++)if(i!=idx)n.put(channels.opt(i));channels=n;save();}else showPosts(idx);}).show();}
    private void editChannel(int idx){JSONObject c=channels.optJSONObject(idx);if(c==null)return;LinearLayout f=vertical();EditText t=field("الاسم",c.optString("title")),ds=field("الوصف",c.optString("description"));f.addView(t);f.addView(ds);new AlertDialog.Builder(this).setTitle("تعديل القناة").setView(f).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{c.put("title",t.getText().toString());c.put("description",ds.getText().toString());save();}catch(Exception ignored){}}).show();}
    private void channelPost(int idx){JSONObject c=channels.optJSONObject(idx);if(c==null)return;EditText e=field("محتوى المنشور",null);new AlertDialog.Builder(this).setTitle("منشور جديد").setView(e).setNegativeButton("إلغاء",null).setPositiveButton("نشر",(d,w)->{try{JSONArray a=c.optJSONArray("posts");if(a==null)a=new JSONArray();JSONObject p=new JSONObject();p.put("text",e.getText().toString());p.put("time",now());a.put(p);c.put("posts",a);save();}catch(Exception ignored){}}).show();}
    private void showPosts(int idx){JSONObject c=channels.optJSONObject(idx);JSONArray a=c==null?null:c.optJSONArray("posts");StringBuilder b=new StringBuilder();if(a!=null)for(int i=a.length()-1;i>=0;i--){JSONObject p=a.optJSONObject(i);if(p!=null)b.append(p.optString("time")).append("\n").append(p.optString("text")).append("\n\n");}new AlertDialog.Builder(this).setTitle("منشورات "+(c==null?"":c.optString("title"))).setMessage(b.length()==0?"لا توجد منشورات":b.toString()).setPositiveButton("إغلاق",null).show();}
    private void mediaManager(){StringBuilder b=new StringBuilder();for(int i=0;i<chats.length();i++){JSONObject c=chats.optJSONObject(i);JSONArray a=c==null?null:c.optJSONArray("messages");if(a!=null)for(int j=0;j<a.length();j++){JSONObject m=a.optJSONObject(j);if(m!=null&&!m.optString("attachmentUri","").isEmpty())b.append(c.optString("name")).append(" • ").append(m.optString("time")).append("\n").append(m.optString("type")).append(": ").append(m.optString("attachmentName")).append("\n").append(m.optString("attachmentUri")).append("\n\n");}}
        new AlertDialog.Builder(this).setTitle("الوسائط والمرفقات المحلية").setMessage(b.length()==0?"لم تحفظ مرفقات بعد":b.toString()).setPositiveButton("إغلاق",null).show();
    }
    private void save(){prefs.edit().putString("chats",chats.toString()).putString("channels",channels.toString()).apply();}
    @Override protected void onDestroy(){stopRecorder(false);super.onDestroy();}
}
