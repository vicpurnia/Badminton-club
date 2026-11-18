#!/usr/bin/env python3
"""v28.2 optional offline member portraits, directly from the member list.

Run on generated v27.9.10 app MainActivity after all legacy overlays.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java";s=p.read_text(encoding="utf-8")
def once(a,b):
 global s
 n=s.count(a)
 if n!=1:raise AssertionError(f"Member photo source anchor changed ({n}): {a[:90]}")
 s=s.replace(a,b,1)

once(''' private File pendingEncryptedExport;''',''' private File pendingEncryptedExport;
 private static final int PICK_MEMBER_PHOTO_V28=2012;
 private String pendingPhotoMemberId;
 private boolean memberPhotoInProgress;''')

once('''reportMonth=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());layout();selectTab("Home");''',
'''reportMonth=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());
  restoreMemberPhotoSelection(b);layout();selectTab(pendingPhotoMemberId==null?"Home":"Members");''')

once('''private TextView avatar(String name){String initials="?";''',
'''@Override protected void onSaveInstanceState(Bundle state){
  super.onSaveInstanceState(state);
  if(pendingPhotoMemberId!=null){
   state.putString("v28.photo.memberId",pendingPhotoMemberId);
   state.putString("v28.photo.search",searchText);
   state.putString("v28.photo.filter",memberFilter);
   state.putString("v28.photo.month",reportMonth);
  }
 }
 private void restoreMemberPhotoSelection(Bundle state){
  if(state==null)return;
  String id=state.getString("v28.photo.memberId");
  if(id==null||id.trim().isEmpty()||id.length()>256)return;
  pendingPhotoMemberId=id;
  searchText=state.getString("v28.photo.search","");
  memberFilter=state.getString("v28.photo.filter","All");
  reportMonth=state.getString("v28.photo.month",reportMonth);
 }
 private View memberAvatar(Member member,String displayName){
  if(member!=null){
   try{
    Bitmap photo=PremiumV28MemberPhotos.thumbnail(this,member.id);
    if(photo!=null){
     ImageView picture=new ImageView(this);
     picture.setImageBitmap(photo);
     picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
     picture.setAdjustViewBounds(false);
     picture.setLayoutParams(new LinearLayout.LayoutParams(dp(44),dp(44)));
     GradientDrawable border=new GradientDrawable();
     border.setColor(PALE);border.setShape(GradientDrawable.OVAL);
     picture.setBackground(border);
     picture.setClipToOutline(true);
     picture.setContentDescription("Photo of "+displayName);
     picture.setTag("member-photo-image:"+member.id);
     return picture;
    }
   }catch(Exception ignored){/* Fall back to initials; no financial impact */ }
  }
  TextView initials=avatar(displayName);
  initials.setContentDescription("Member initials for "+displayName);
  return initials;
 }
 private TextView memberPhotoAction(Member member,String displayName){
  boolean hasPortrait=false;
  try{hasPortrait=PremiumV28MemberPhotos.has(this,member.id);}catch(Exception ignored){}
  TextView action=inlineButton(hasPortrait?"Photo options":"Add photo",GREEN,
   ()->showMemberPhotoOptions(member,displayName));
  action.setMinHeight(dp(48));
  action.setFocusable(true);
  action.setTag("member-photo-action:"+member.id);
  action.setContentDescription((hasPortrait?"Photo options for ":"Add photo for ")+displayName);
  return action;
 }
 private boolean memberPhotoBusy(){
  if(pendingPhotoMemberId==null&&!memberPhotoInProgress)return false;
  Toast.makeText(this,"Finish the current photo selection or save first",Toast.LENGTH_SHORT).show();
  return true;
 }
 private void showMemberPhotoOptions(Member member,String displayName){
  if(memberPhotoBusy())return;
  try{
   if(!PremiumV28MemberPhotos.has(this,member.id)){pickMemberPhoto(member);return;}
   LinearLayout body=col();pad(body,18,12);
   body.addView(text("Photo · "+displayName,17,DARK,true));gap(body,12);
   Bitmap photo=PremiumV28MemberPhotos.thumbnail(this,member.id);
   if(photo!=null){
    ImageView preview=new ImageView(this);
    preview.setImageBitmap(photo);
    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    preview.setContentDescription("Photo of "+displayName);
    LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(192),dp(192));
    size.gravity=Gravity.CENTER_HORIZONTAL;body.addView(preview,size);gap(body,14);
   }
   body.addView(text("Saved on this phone. Photos are not included in ledger backups.",12,GRAY,false));
   gap(body,14);
   Dialog options=new Dialog(this);
   options.requestWindowFeature(Window.FEATURE_NO_TITLE);
   body.addView(button("Change photo",()->{options.dismiss();pickMemberPhoto(member);}),fill());gap(body,8);
   body.addView(button("Remove photo",()->{options.dismiss();deleteMemberPhoto(member);}),fill());gap(body,8);
   body.addView(button("Done",options::dismiss),fill());
   ScrollView scroll=new ScrollView(this);scroll.addView(body);
   options.setTitle("Photo · "+displayName);options.setContentView(scroll);options.show();
   Window window=options.getWindow();
   if(window!=null)window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(28),dp(420)),
    WindowManager.LayoutParams.WRAP_CONTENT);
  }catch(Exception issue){error(issue);}
 }
 private void pickMemberPhoto(Member member){
  if(member==null||member.id==null||member.id.isEmpty()){
   error(new IllegalArgumentException("Select an existing member first"));
   return;
  }
  if(memberPhotoBusy())return;
  pendingPhotoMemberId=member.id;
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);
  pick.setType("image/*");
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,PICK_MEMBER_PHOTO_V28);}
  catch(Exception issue){
   pendingPhotoMemberId=null;
   error(new IOException("Android photo picker could not be opened",issue));
  }
 }
 private void deleteMemberPhoto(Member member){
  if(member==null||memberPhotoBusy())return;
  new AlertDialog.Builder(this)
   .setTitle("Remove local member photograph?")
   .setMessage("This removes only the photograph for this member ID on this device. "+
    "The original member, fees, receipts and history are unchanged.")
   .setNegativeButton("Keep photo",null)
   .setPositiveButton("Remove photo",(d,w)->{
    if(memberPhotoBusy())return;
    memberPhotoInProgress=true;
    final int screen=screenEpoch;
    background(()->{
     try{
      PremiumV28MemberPhotos.remove(this,member.id);
      runOnUiThread(()->{
       if(!isFinishing()&&!isDestroyed()&&screen==screenEpoch)selectTab("Members");
      });
     }finally{runOnUiThread(()->memberPhotoInProgress=false);}
    });
   }).show();
 }
 private TextView avatar(String name){String initials="?";''')

once('''card.addView(avatar(item.name));''','''card.addView(memberAvatar(item.member,item.name));''')
once('''      pressable(card,()->memberWallet(item.member,item.monthly,item.lifetime,item.donated));
      panel(results,card);''',
'''      pressable(card,()->memberWallet(item.member,item.monthly,item.lifetime,item.donated));
      LinearLayout memberPanel=col();memberPanel.addView(card);gap(memberPanel,10);
      memberPanel.addView(memberPhotoAction(item.member,item.name),fill());
      panel(results,memberPanel);''')
once('''LinearLayout head=row();head.addView(avatar(m.name));''',
     '''LinearLayout head=row();head.addView(memberAvatar(m,displayName));''')

once('''    gap(body,16);
    body.addView(button("Edit member profile / archive safely",()->{memberDialog.dismiss();openMemberProfile(m);}),fill());gap(body,8);''',
     '''    gap(body,16);
    boolean hasPortrait=false;
    try{hasPortrait=PremiumV28MemberPhotos.has(this,m.id);}catch(Exception ignored){}
    body.addView(button(hasPortrait?"📷 Change member photo":"📷 Add member photo",
     ()->{memberDialog.dismiss();pickMemberPhoto(m);}),fill());gap(body,8);
    if(hasPortrait){
     body.addView(button("Remove local member photo",()->{
      memberDialog.dismiss();deleteMemberPhoto(m);
     }),fill());gap(body,8);
    }
    body.addView(text("Optional photograph · private to this device · linked to the existing member ID. "+
     "Photos are NOT included in the verified financial JSON or encrypted ledger backup. "+
     "Preserve them separately before changing phones.",11,GRAY,false));gap(body,13);
    body.addView(button("Edit member profile / archive safely",()->{memberDialog.dismiss();openMemberProfile(m);}),fill());gap(body,8);''')

once('''@Override protected void onActivityResult(int req,int resultCode,Intent data){super.onActivityResult(req,resultCode,data);
''',
     '''@Override protected void onActivityResult(int req,int resultCode,Intent data){super.onActivityResult(req,resultCode,data);
  if(req==PICK_MEMBER_PHOTO_V28){
   final String memberId=pendingPhotoMemberId;pendingPhotoMemberId=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
   if(memberId==null){error(new IOException("Photo selection expired; no member was changed"));return;}
   final Uri chosen=data.getData();
   final int screen=screenEpoch;
   memberPhotoInProgress=true;
   background(()->{
    try{
     requireImportedLedger();
     boolean exists=false;
     for(Member m:database.dao().members())if(memberId.equals(m.id)){exists=true;break;}
     if(!exists)throw new IOException("Original member ID no longer exists. Photograph was not saved.");
     PremiumV28MemberPhotos.addOrReplace(this,chosen,memberId);
     runOnUiThread(()->{
      if(isFinishing()||isDestroyed())return;
      Toast.makeText(this,"Member photo saved",Toast.LENGTH_SHORT).show();
      if(screen==screenEpoch)selectTab("Members");
     });
    }finally{runOnUiThread(()->memberPhotoInProgress=false);}
   });
   return;
  }
''')

if "PremiumV28MemberPhotos.addOrReplace" not in s or "PICK_MEMBER_PHOTO_V28" not in s:
 raise AssertionError("Optional member photo SAF picker not installed")
p.write_text(s,encoding="utf-8")
(root/"PremiumV28MemberPhotos.java").write_text(
 Path("src/premium/v28_0/PremiumV28MemberPhotos.java").read_text(encoding="utf-8"),
 encoding="utf-8")
print("PASS v28.2 member list: direct photo actions, picker identity recovery, no Room/finance mutation")
