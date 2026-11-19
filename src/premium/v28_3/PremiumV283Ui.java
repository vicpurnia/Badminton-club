package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import java.util.Locale;

/** Native presentation helpers only. No database, money, file or network access. */
final class PremiumV283Ui {
 static final int INK=0xFF14243B, ACCENT=0xFF315DCC, IVORY=0xFFF5F3EE;
 static final int MUTED=0xFF59687C, LINE=0xFFDCE2EA, SOFT=0xFFE8EDF8, GOLD=0xFFD9BC7D;
 static final int SUCCESS=0xFF247052, SUCCESS_SOFT=0xFFE7F3ED;
 static final int WARNING=0xFF9A620D, WARNING_SOFT=0xFFFFF3D8;
 static final int DANGER=0xFFA53C42, DANGER_SOFT=0xFFFBEDEF;
 private PremiumV283Ui(){}

 static int dp(Context context,float value){
  return (int)(context.getResources().getDisplayMetrics().density*value+.5f);
 }
 static GradientDrawable surface(Context context,int color,int stroke,int radius){
  GradientDrawable shape=new GradientDrawable();
  shape.setColor(color);shape.setCornerRadius(dp(context,radius));
  if(stroke!=Color.TRANSPARENT)shape.setStroke(dp(context,1),stroke);
  return shape;
 }
 static void touch(View view){
  view.setMinimumHeight(dp(view.getContext(),48));
  view.setMinimumWidth(dp(view.getContext(),48));
  if(!(view instanceof EditText))view.setFocusable(true);
  view.setClipToOutline(true);
  view.setForeground(new RippleDrawable(ColorStateList.valueOf(0x24315DCC),null,null));
 }
 static EditText field(Context context){
  EditText field=new EditText(context);styleField(field);return field;
 }
 static void styleField(EditText field){
  Context context=field.getContext();
  field.setTextColor(INK);field.setHintTextColor(MUTED);field.setTextSize(15);
  field.setMinHeight(dp(context,50));
  field.setPadding(dp(context,14),dp(context,12),dp(context,14),dp(context,12));
  field.setBackground(surface(context,Color.WHITE,LINE,12));
 }
 static String initials(String name){
  if(name==null)return "?";
  String value=name.trim().replaceFirst("(?i)^dr\\.\\s*","").trim();
  if(value.isEmpty())return "?";
  String[] words=value.split("\\s+");
  String first=new String(Character.toChars(words[0].codePointAt(0)));
  String last=words.length>1?new String(Character.toChars(words[words.length-1].codePointAt(0))):"";
  return (first+last).toUpperCase(Locale.ROOT);
 }
 static String category(String title){
  String name=title.toLowerCase(Locale.ROOT);
  if(name.contains("backup")||name.contains("back up")||name.contains("restore")||name.contains("export")||
     name.contains("external folder")||name.contains("saved json"))return "Backups";
  if(name.contains("fees")||name.contains("receipt library")||name.contains("kanishk"))return "Club";
  return "Reports";
 }
 static String label(String title){
  switch(title){
   case "v28 BACKUP & RELEASE READINESS":return "Backup & restore check";
   case "v26 EXECUTIVE DASHBOARD":return "Executive dashboard";
   case "MONTHLY FEES & DUES":return title; // Existing navigation contract.
   case "CASH RECEIPT-DATE EVIDENCE":return "Cash receipt dates";
   case "ENCRYPTED BACKUP · OPTIONAL":return "Create encrypted backup";
   case "TEST / IMPORT ENCRYPTED BACKUP":return "Open encrypted backup";
   case "MONTH-END FINANCIAL REVIEW":return "Month-end review";
   case "BACK UP NOW":return "Back up now";
   case "VERIFY LAST PRIVATE BACKUP":return "Check last phone backup";
   case "VERIFY LATEST EXTERNAL BACKUP":return "Check external backup";
   case "TEST RESTORE WITHOUT CHANGING DATA":return "Test a restore";
   case "COMPARE SAVED JSON WITH CURRENT LEDGER":return "Compare a saved backup";
   case "CHOOSE EXTERNAL BACKUP FOLDER":return "Choose backup folder";
   case "RECEIPT LIBRARY · ALL MONTHS":return "Receipt library · all months";
   case "SAVE CLASSIC PDF · A4 LANDSCAPE":return "Save master PDF · A4 landscape";
   case "PRINT CLASSIC · A4 LANDSCAPE":return "Print master · A4 landscape";
   default:return title;
  }
 }
 static final class ToolRow {
  final View view;final String title,detail,category;
  ToolRow(View view,String title,String detail){
   this.view=view;this.title=title;this.detail=detail;this.category=PremiumV283Ui.category(title);
  }
 }
 static String iconKey(String label){
  String name=label.toLowerCase(Locale.ROOT);
  if(name.contains("home"))return "home";
  if(name.contains("photo"))return "camera";
  if(name.contains("shuttle")||name.contains("stock")||name.contains("pack"))return "shuttle";
  if(name.contains("member")||name.contains("player"))return "members";
  if(name.contains("backup")||name.contains("restore")||name.contains("verify"))return "backup";
  if(name.contains("collect")||name.contains("fee")||name.contains("cash"))return "collect";
  return "reports";
 }
 static ImageView icon(Context context,String key,int color,int size){
  ImageView image=new ImageView(context);
  image.setImageDrawable(new LineIcon(key,color));
  image.setScaleType(ImageView.ScaleType.FIT_CENTER);
  image.setMinimumWidth(dp(context,size));image.setMinimumHeight(dp(context,size));
  image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  return image;
 }
 private static final class LineIcon extends Drawable {
  private final String key;private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
  LineIcon(String key,int color){
   this.key=key;pen.setColor(color);pen.setStrokeWidth(1.7f);
   pen.setStyle(Paint.Style.STROKE);pen.setStrokeCap(Paint.Cap.ROUND);
   pen.setStrokeJoin(Paint.Join.ROUND);
  }
  private void line(Canvas c,float x,float y,float x2,float y2){c.drawLine(x,y,x2,y2,pen);}
  private void path(Canvas c,float... points){
   Path path=new Path();path.moveTo(points[0],points[1]);
   for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);c.drawPath(path,pen);
  }
  @Override public void draw(Canvas canvas){
   int saved=canvas.save();canvas.translate(getBounds().left,getBounds().top);
   canvas.scale(getBounds().width()/24f,getBounds().height()/24f);
   switch(key){
    case "chevron-left":path(canvas,15,5,8,12,15,19);break;
    case "chevron-right":path(canvas,9,5,16,12,9,19);break;
    case "home":
     path(canvas,3,10,12,3,21,10);path(canvas,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
    case "collect":
     canvas.drawRoundRect(new RectF(3,5,21,20),3,3,pen);line(canvas,4,8,19,8);
     line(canvas,15,11,15,17);line(canvas,12,14,18,14);break;
    case "shuttle":
     path(canvas,4,3,8,16,16,16,20,3,4,3);line(canvas,8,3,10,16);
     line(canvas,12,3,12,16);line(canvas,16,3,14,16);
     canvas.drawArc(new RectF(8,12,16,22),0,180,false,pen);break;
    case "members":
     canvas.drawCircle(9,7,3,pen);canvas.drawCircle(18,9,2.4f,pen);
     canvas.drawArc(new RectF(2,12,16,28),180,180,false,pen);
     canvas.drawArc(new RectF(14,14,23,28),205,155,false,pen);break;
    case "backup":
     Path cloud=new Path();cloud.moveTo(6,17);
     cloud.cubicTo(0,17,0,9,6,9);cloud.cubicTo(6,1,18,1,18,9);
     cloud.cubicTo(24,9,24,17,19,17);canvas.drawPath(cloud,pen);
     line(canvas,12,11,12,22);path(canvas,8,18,12,22,16,18);break;
    case "camera":
     path(canvas,4,7,8,7,10,4,15,4,17,7,21,7,21,20,3,20,3,7,4,7);
     canvas.drawCircle(12,13,4,pen);break;
    case "court":
     canvas.drawRoundRect(new RectF(3,3,21,21),1,1,pen);line(canvas,3,12,21,12);
     line(canvas,7,3,7,21);line(canvas,17,3,17,21);
     line(canvas,3,7,21,7);line(canvas,3,17,21,17);break;
    default:
     canvas.drawRoundRect(new RectF(5,3,19,22),2,2,pen);
     line(canvas,9,8,15,8);line(canvas,9,12,15,12);line(canvas,9,16,13,16);break;
   }
   canvas.restoreToCount(saved);
  }
  @Override public void setAlpha(int alpha){pen.setAlpha(alpha);invalidateSelf();}
  @Override public void setColorFilter(android.graphics.ColorFilter filter){pen.setColorFilter(filter);invalidateSelf();}
  @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
}
