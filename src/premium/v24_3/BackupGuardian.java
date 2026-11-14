package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
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
 static final int MAX_BACKUP_BYTES=12_000_000;
 private BackupGuardian(){}

 private static SharedPreferences prefs(Context context){
  return context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
 }
 static String lastError(Context context){
  return prefs(context).getString(LAST_ERROR,"");
 }
 static boolean folderSelected(Context context){
  return prefs(context).getString(TREE,"").length()>0;
 }
 static void setExternalFolder(Context context,Uri treeUri,int grantedFlags) throws IOException {
  int flags=grantedFlags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  if((flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
   throw new IOException("The selected folder needs both read and write permission to verify copies.");
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
 static String recoveryReadiness(Context context){
  SharedPreferences p=prefs(context);
  long local=p.getLong(LAST_OK,0);
  long external=p.getLong(LAST_EXTERNAL,0);
  if(local<=0)
   return "No verified automatic snapshot yet. Open Reports and choose BACK UP NOW.";
  if(!folderSelected(context))
   return "Automatic copies are only inside this app. To recover after uninstall, choose an external backup folder or export a verified JSON file outside the app.";
  if(external<=0)
   return "External folder selected, but no external copy has been read back and verified. Use BACK UP NOW and review any warnings.";
  if(external<local)
   return "The last verified external copy is older than the latest app-private snapshot. The newest changes may not be safely backed up outside this app.";
  String error=p.getString(LAST_ERROR,"");
  if(error!=null&&!error.isEmpty())
   return "Backup warning detected: "+error;
  String time=new SimpleDateFormat("dd MMM yyyy, HH:mm",Locale.ENGLISH).format(new Date(external));
  return "Last automatic external backup was saved and verified on "+time+". Keep another verified JSON copy in a trusted private location.";
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

 private static byte[] readAndVerify(InputStream in,long expectedLength,byte[] expectedHash) throws Exception {
  if(in==null)throw new IOException("Backup file could not be reopened for verification.");
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();
  byte[] block=new byte[16384];
  int n;
  while((n=in.read(block))!=-1){
   if(n==0)continue;
   if(buffer.size()+n>MAX_BACKUP_BYTES)throw new IOException("Backup exceeds 12 MB during verification.");
   buffer.write(block,0,n);
   md.update(block,0,n);
  }
  byte[] data=buffer.toByteArray();
  if(data.length!=expectedLength||!MessageDigest.isEqual(md.digest(),expectedHash))
   throw new IOException("Backup copy failed SHA-256 integrity verification.");
  // A correctly copied file must ALSO remain decodable as a verified ledger.
  MigrationCodec.inspect(new String(data,StandardCharsets.UTF_8));
  return data;
 }
 /** Verify a manually exported SAF JSON copy before the UI reports success. */
 static void verifySavedCopy(Context context,Uri document,File original) throws Exception {
  if(original==null||!original.isFile()||original.length()<=0||original.length()>MAX_BACKUP_BYTES)
   throw new IOException("Original verified account export is unavailable or too large.");
  byte[] hash=sha256File(original);
  try(InputStream opened=context.getContentResolver().openInputStream(document)){
   readAndVerify(opened,original.length(),hash);
  }
 }
 private static byte[] sha256(byte[] bytes) throws Exception {
  return MessageDigest.getInstance("SHA-256").digest(bytes);
 }
 private static String checksumShort(byte[] hash){
  StringBuilder result=new StringBuilder(8);
  for(int i=0;i<4;i++)result.append(String.format(Locale.ROOT,"%02x",hash[i]&0xff));
  return result.toString();
 }
 /** Verify a stored app-private snapshot without importing or changing any account. */
 static String verifyLatestPrivate(Context context) throws Exception {
  File dir=new File(context.getFilesDir(),"verified_backups");
  File[] all=dir.listFiles((folder,name)->name.startsWith("Badminton_Verified_")&&name.endsWith(".json"));
  if(all==null||all.length==0)throw new IOException("No private backup found. Use BACK UP NOW first.");
  Arrays.sort(all,Comparator.comparing(File::getName));
  File latest=all[all.length-1];
  long length=latest.length();
  if(length<=0||length>MAX_BACKUP_BYTES)throw new IOException("Latest backup has an unexpected size.");
  // The name ends with a digest prefix. Compare the entire file's SHA256 prefix as well.
  byte[] bytes;
  try(FileInputStream in=new FileInputStream(latest)){
   bytes=readAndVerify(in,length,sha256File(latest));
  }
  String prefix=latest.getName().replaceAll("^.*_([0-9a-f]{8})\\.json$","$1");
  if(prefix.equals(latest.getName())||!prefix.equals(checksumShort(sha256(bytes))))
   throw new IOException("Latest backup filename and checksum disagree.");
  return "Verified "+all.length+" private snapshot(s).\nLatest: "+latest.getName()+
   "\nBytes: "+length+"\nSHA-256 verified; JSON import validation passed."+
   "\nThese app-private snapshots are lost if the app is uninstalled.";
 }
 private static byte[] sha256File(File file) throws Exception {
  MessageDigest hash=MessageDigest.getInstance("SHA-256");
  try(FileInputStream in=new FileInputStream(file)){
   byte[] b=new byte[16384];int n;
   while((n=in.read(b))!=-1)if(n>0)hash.update(b,0,n);
  }
  return hash.digest();
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
   if(bytes.length==0||bytes.length>MAX_BACKUP_BYTES)throw new IOException("Unexpected backup size.");
   String name="Badminton_Verified_"+stamp()+"_"+digest(bytes)+".json";
   File dir=new File(context.getFilesDir(),"verified_backups");
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create private backup directory.");
   File dest=new File(dir,name);
   File temp=new File(dir,name+".tmp");
   try(FileOutputStream out=new FileOutputStream(temp)){
    out.write(bytes);out.flush();out.getFD().sync();
   }
   try(FileInputStream check=new FileInputStream(temp)){
    readAndVerify(check,bytes.length,sha256(bytes));
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
    Uri created=null;
    try{
     Uri tree=Uri.parse(uriString);
     Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
     created=DocumentsContract.createDocument(context.getContentResolver(),root,"application/json",name);
     if(created==null)throw new IOException("The selected provider could not create a backup file.");
     try(OutputStream out=context.getContentResolver().openOutputStream(created,"w")){
      if(out==null)throw new IOException("The selected provider could not write backup data.");
      out.write(bytes);out.flush();
     }
     // Verify the provider returned the same bytes. Do not claim a completed
     // external backup based only on output-stream close.
     try(InputStream check=context.getContentResolver().openInputStream(created)){
      readAndVerify(check,bytes.length,sha256(bytes));
     }
     p.edit().putLong(LAST_EXTERNAL,System.currentTimeMillis()).remove(LAST_ERROR).apply();
    }catch(Exception ex){
     // Best-effort removal of an incomplete external copy; prior backups stay.
     if(created!=null)try{DocumentsContract.deleteDocument(context.getContentResolver(),created);}
      catch(Exception ignored){}
     p.edit().putString(LAST_ERROR,"External folder verification failed: "+ex.getMessage()).apply();
    }
   }
  }catch(Exception ex){
   p.edit().putString(LAST_ERROR,"Verified backup failed: "+ex.getMessage()).apply();
  }
 }
}
