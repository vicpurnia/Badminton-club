package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Verified, append-only account snapshots. No data is uploaded.
 * App-private snapshots are local safety copies, NOT uninstall-proof.
 * An opt-in Storage Access Framework tree is required for uninstall-proof backups.
 * All calls that inspect database state or write backup data must run on a worker.
 */
final class BackupGuardian {
 private static final String PREF="vinayak_badminton_backup_guardian";
 private static final String TREE="backup_tree";
 private static final String LAST_OK="last_backup_ms";
 private static final String LAST_EXTERNAL="last_external_backup_ms";
 private static final String LAST_ERROR="last_backup_error";
 private static final long DAILY=24L*60*60*1000;
 private static final int KEEP_PRIVATE=7;
 private BackupGuardian(){}

 private static SharedPreferences prefs(Context context){
  return context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
 }
 static boolean folderSelected(Context context){
  return prefs(context).getString(TREE,"").length()>0;
 }
 static void setExternalFolder(Context context,Uri treeUri,int grantedFlags) throws IOException {
  int flags=grantedFlags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  if((flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0)
   throw new IOException("The chosen backup folder did not grant write permission.");
  try {
   context.getContentResolver().takePersistableUriPermission(treeUri,flags);
  }catch(SecurityException ex){
   throw new IOException("Backup folder permission was not retained. Choose a writable Documents subfolder.",ex);
  }
  prefs(context).edit().putString(TREE,treeUri.toString()).remove(LAST_ERROR).apply();
 }
 static void clearExternalFolder(Context context){
  String old=prefs(context).getString(TREE,"");
  if(!old.isEmpty()){
   try{context.getContentResolver().releasePersistableUriPermission(Uri.parse(old),
      Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}
   catch(Exception ignored){}
  }
  prefs(context).edit().remove(TREE).remove(LAST_ERROR).apply();
 }
 static String status(Context context){
  SharedPreferences p=prefs(context);
  long stamp=p.getLong(LAST_OK,0);
  String when=stamp==0?"No verified backup yet":
   new SimpleDateFormat("dd MMM yyyy, HH:mm",Locale.ENGLISH).format(new Date(stamp));
  String location=folderSelected(context)?"External folder selected":"Only app-private copies; lost if app is uninstalled";
  String external="";
  if(folderSelected(context)){
   long done=p.getLong(LAST_EXTERNAL,0);
   external="\nExternal folder last written: "+(done==0?"Not yet":
    new SimpleDateFormat("dd MMM yyyy, HH:mm",Locale.ENGLISH).format(new Date(done)));
  }
  String err=p.getString(LAST_ERROR,"");
  if(err.length()>120)err=err.substring(0,120);
  return "Last verified copy: "+when+"\n"+location+external+
   (err.isEmpty()?"":"\nBackup warning: "+err);
 }
 private static String stamp(){
  return new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(new Date());
 }
 private static String digest(byte[] bytes) throws Exception {
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  byte[] hash=md.digest(bytes);
  StringBuilder sb=new StringBuilder(8);
  for(int i=0;i<4;i++)sb.append(String.format(Locale.ROOT,"%02x",hash[i]&0xff));
  return sb.toString();
 }
 static void maybeDaily(Context context,LedgerDatabase db) {
  if(db.dao().meta()==null)return;
  if(System.currentTimeMillis()-prefs(context).getLong(LAST_OK,0)>=DAILY)
   save(context,db,"daily",false);
 }
 /** Force is for user-triggered/manual and post-mutation backups; never overwrites. */
 static void save(Context context,LedgerDatabase db,String reason,boolean force) {
  if(db.dao().meta()==null)return;
  SharedPreferences p=prefs(context);
  if(!force && System.currentTimeMillis()-p.getLong(LAST_OK,0)<DAILY)return;
  try {
   String json=MigrationCodec.exportVerified(db);
   // Reject damaged or unverifiable exports BEFORE storing an automatic copy.
   MigrationCodec.inspect(json);
   byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
   if(bytes.length==0||bytes.length>12_000_000)throw new IOException("Unexpected backup size.");
   String name="Badminton_Verified_"+stamp()+"_"+digest(bytes)+".json";
   File dir=new File(context.getFilesDir(),"verified_backups");
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create private backup directory.");
   File dest=new File(dir,name);
   File temp=new File(dir,name+".tmp");
   try(FileOutputStream out=new FileOutputStream(temp)){
    out.write(bytes);out.flush();out.getFD().sync();
   }
   if(!temp.renameTo(dest)){temp.delete();throw new IOException("Could not finish an atomic private backup.");}
   p.edit().putLong(LAST_OK,System.currentTimeMillis()).remove(LAST_ERROR).apply();
   File[] files=dir.listFiles((folder,n)->n.startsWith("Badminton_Verified_")&&n.endsWith(".json"));
   if(files!=null&&files.length>KEEP_PRIVATE){
    Arrays.sort(files,Comparator.comparing(File::getName));
    for(int i=0;i<files.length-KEEP_PRIVATE;i++)files[i].delete();
   }
   String uriString=p.getString(TREE,"");
   if(!uriString.isEmpty()){
    try{
     Uri tree=Uri.parse(uriString);
     Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
     Uri document=DocumentsContract.createDocument(context.getContentResolver(),root,"application/json",name);
     if(document==null)throw new IOException("The selected provider could not create a backup file.");
     try(OutputStream out=context.getContentResolver().openOutputStream(document,"w")){
      if(out==null)throw new IOException("The selected provider could not write backup data.");
      out.write(bytes);out.flush();
     }
     p.edit().putLong(LAST_EXTERNAL,System.currentTimeMillis()).remove(LAST_ERROR).apply();
    }catch(Exception ex){
     // Private backup remains available; never hide a failed external backup.
     p.edit().putString(LAST_ERROR,"External folder write failed: "+ex.getMessage()).apply();
    }
   }
  }catch(Exception ex){
   p.edit().putString(LAST_ERROR,"Verified backup failed: "+ex.getMessage()).apply();
  }
 }
}
