package com.vinayak.badminton.nativepreview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import java.util.ArrayList;

/** Native form presentation and submission state only; no ledger or file access. */
final class PremiumV283FormBuilder extends AlertDialog.Builder {
 private DialogInterface.OnClickListener positive;
 private boolean retainedPositive;
 private boolean cancelable=true;

 PremiumV283FormBuilder(Context context){super(context);}

 @Override public PremiumV283FormBuilder setPositiveButton(CharSequence text,
   DialogInterface.OnClickListener listener){
  positive=listener;retainedPositive=true;
  // The builder callback always dismisses its dialog. Bind the actual control
  // after show() so validation and confirmation can leave the editor intact.
  super.setPositiveButton(text,null);return this;
 }

 @Override public PremiumV283FormBuilder setPositiveButton(int text,
   DialogInterface.OnClickListener listener){
  return setPositiveButton(getContext().getText(text),listener);
 }

 @Override public PremiumV283FormBuilder setCancelable(boolean value){
  cancelable=value;super.setCancelable(value);return this;
 }

 @Override public AlertDialog show(){
  AlertDialog dialog=super.create();
  Window window=dialog.getWindow();
  if(window!=null)window.setSoftInputMode(
   (window.getAttributes().softInputMode&~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
    |WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  dialog.show();
  window=dialog.getWindow();
  if(window==null)return dialog;
  View decor=window.getDecorView();
  FormState state=new FormState(dialog,decor,owner(getContext()),cancelable);
  decor.setTag(state);state.attach();
  if(retainedPositive){
   final DialogInterface.OnClickListener callback=positive;
   Button button=dialog.getButton(DialogInterface.BUTTON_POSITIVE);
   if(button!=null)button.setOnClickListener(view->{
    if(!state.available()||state.saving)return;
    if(callback!=null)callback.onClick(dialog,DialogInterface.BUTTON_POSITIVE);
   });
  }
  return dialog;
 }

 /** Use this for the custom form body; the native title and actions stay outside it. */
 static ScrollView body(Context context,View content){
  FormBody scroll=new FormBody(context);
  scroll.setFillViewport(false);scroll.setClipToPadding(false);
  scroll.addView(content);return scroll;
 }

 /** Call on the UI thread before scheduling one immutable reviewed submission. */
 static boolean beginSave(DialogInterface dialog){
  if(Looper.myLooper()!=Looper.getMainLooper())return false;
  FormState state=state(dialog);
  if(state==null||!state.available()||state.saving)return false;
  state.saving=true;
  state.lock(state.decor);
  state.dialog.setCancelable(false);
  state.dialog.setCanceledOnTouchOutside(false);
  return true;
 }

 /** A rejected transaction restores this same draft. Post-commit backup errors do not. */
 static void failed(DialogInterface dialog){
  FormState state=state(dialog);
  if(state==null)return;
  if(Looper.myLooper()!=Looper.getMainLooper()){
   state.decor.post(()->failed(dialog));return;
  }
  if(!state.available()||!state.saving)return;
  for(ControlState control:state.controls)control.view.setEnabled(control.enabled);
  state.controls.clear();state.saving=false;
  state.dialog.setCancelable(state.cancelable);
  state.dialog.setCanceledOnTouchOutside(state.cancelable);
 }

 /** Close only after the caller knows that its transaction was accepted. */
 static void saved(DialogInterface dialog){
  FormState state=state(dialog);
  if(state==null)return;
  if(Looper.myLooper()!=Looper.getMainLooper()){
   state.decor.post(()->saved(dialog));return;
  }
  if(state.dialog.isShowing())state.dialog.dismiss();
 }

 private static FormState state(DialogInterface dialog){
  if(!(dialog instanceof AlertDialog))return null;
  Window window=((AlertDialog)dialog).getWindow();
  if(window==null)return null;
  Object tag=window.getDecorView().getTag();
  if(!(tag instanceof FormState))return null;
  FormState state=(FormState)tag;
  return state.dialog==dialog&&!state.closed?state:null;
 }

 private static Activity owner(Context context){
  while(context instanceof ContextWrapper){
   if(context instanceof Activity)return (Activity)context;
   Context base=((ContextWrapper)context).getBaseContext();
   if(base==context)break;
   context=base;
  }
  return context instanceof Activity?(Activity)context:null;
 }

 private static final class ControlState {
  final View view;final boolean enabled;
  ControlState(View view){this.view=view;enabled=view.isEnabled();}
 }

 private static final class FormState implements ViewTreeObserver.OnGlobalLayoutListener,
   View.OnAttachStateChangeListener {
  final AlertDialog dialog;final View decor;final Activity activity;
  final boolean cancelable;final Object previousTag;
  final ArrayList<ControlState> controls=new ArrayList<>();
  final Rect visibleFrame=new Rect();
  ViewTreeObserver observer;
  boolean saving,closed;

  FormState(AlertDialog dialog,View decor,Activity activity,boolean cancelable){
   this.dialog=dialog;this.decor=decor;this.activity=activity;
   this.cancelable=cancelable;previousTag=decor.getTag();
  }

  boolean available(){
   return !closed&&dialog.isShowing()&&
    (activity==null||(!activity.isFinishing()&&!activity.isDestroyed()));
  }

  void attach(){
   observer=decor.getViewTreeObserver();observer.addOnGlobalLayoutListener(this);
   decor.addOnAttachStateChangeListener(this);
   decor.post(()->{if(!closed)onGlobalLayout();});
  }

  void lock(View view){
   if(view instanceof Button||view instanceof EditText||view instanceof CompoundButton||
     view instanceof Spinner){
    controls.add(new ControlState(view));view.setEnabled(false);
   }
   if(view instanceof ViewGroup){
    ViewGroup group=(ViewGroup)view;
    for(int index=0;index<group.getChildCount();index++)lock(group.getChildAt(index));
   }
  }

  @Override public void onGlobalLayout(){
   if(!available())return;
   FormBody body=findBody(decor);
   if(body==null||decor.getHeight()==0)return;
   decor.getWindowVisibleDisplayFrame(visibleFrame);
   if(visibleFrame.height()<=0)return;
   // Measure real native chrome, including wrapped titles/buttons at large
   // font scales. Only the form shrinks when the IME reduces the visible frame.
   int chrome=Math.max(0,decor.getHeight()-body.getHeight());
   int margin=Math.round(16*decor.getResources().getDisplayMetrics().density);
   body.limitHeight(Math.max(0,visibleFrame.height()-chrome-margin));
  }

  @Override public void onViewAttachedToWindow(View view){}

  @Override public void onViewDetachedFromWindow(View view){
   closed=true;
   if(observer!=null&&observer.isAlive())observer.removeOnGlobalLayoutListener(this);
   ViewTreeObserver current=decor.getViewTreeObserver();
   if(current!=observer&&current.isAlive())current.removeOnGlobalLayoutListener(this);
   decor.removeOnAttachStateChangeListener(this);
   controls.clear();observer=null;
   if(decor.getTag()==this)decor.setTag(previousTag);
  }

  private static FormBody findBody(View view){
   if(view instanceof FormBody)return (FormBody)view;
   if(view instanceof ViewGroup){
    ViewGroup group=(ViewGroup)view;
    for(int index=0;index<group.getChildCount();index++){
     FormBody result=findBody(group.getChildAt(index));if(result!=null)return result;
    }
   }
   return null;
  }
 }

 private static final class FormBody extends ScrollView {
  private int maxHeight=Integer.MAX_VALUE;
  FormBody(Context context){super(context);}

  void limitHeight(int height){
   if(maxHeight==height)return;
   maxHeight=height;requestLayout();
  }

  @Override protected void onMeasure(int widthSpec,int heightSpec){
   int mode=MeasureSpec.getMode(heightSpec),height=MeasureSpec.getSize(heightSpec);
   if(maxHeight!=Integer.MAX_VALUE&&(mode==MeasureSpec.UNSPECIFIED||height>maxHeight))
    heightSpec=MeasureSpec.makeMeasureSpec(maxHeight,MeasureSpec.AT_MOST);
   super.onMeasure(widthSpec,heightSpec);
  }
 }
}
