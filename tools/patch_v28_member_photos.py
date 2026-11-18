#!/usr/bin/env python3
"""v28.0 optional offline member portraits; never edit Room/financial JSON.

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
 private String pendingPhotoMemberId;''')

once('''private TextView avatar(String name){String initials="?";''',
'''private View memberAvatar(Member member,String displayName){
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
     return picture;
    }
   }catch(Exception ignored){/* Fall back to initials; no financial impact */ }
  }
  return avatar(displayName);
 }
 private void pickMemberPhoto(Member member){
  if(member==null||member.id==null||member.id.isEmpty()){
   error(new IllegalArgumentException("Select an existing member first"));
   return;
  }
  if(pendingPhotoMemberId!=null){
   error(new IllegalStateException("Finish or cancel the previous photo selection first"));
   return;
  }
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
  if(member==null)return;
  new AlertDialog.Builder(this)
   .setTitle("Remove local member photograph?")
   .setMessage("This removes only the photograph for this member ID on this device. "+
    "The original member, fees, receipts and history are unchanged.")
   .setNegativeButton("Keep photo",null)
   .setPositiveButton("Remove photo",(d,w)->background(()->{
    PremiumV28MemberPhotos.remove(this,member.id);
    runOnUiThread(()->selectTab("Members"));
   })).show();
 }
 private TextView avatar(String name){String initials="?";''')

once('''card.addView(avatar(item.name));''','''card.addView(memberAvatar(item.member,item.name));''')
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
   if(memberId==null){error(new IOException("Photo selection expired; no member was changed"));return;}
   if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
   final Uri chosen=data.getData();
   background(()->{
    requireImportedLedger();
    boolean exists=false;
    for(Member m:database.dao().members())if(memberId.equals(m.id)){exists=true;break;}
    if(!exists)throw new IOException("Original member ID no longer exists. Photograph was not saved.");
    PremiumV28MemberPhotos.addOrReplace(this,chosen,memberId);
    runOnUiThread(()->{
     Toast.makeText(this,"Member photo saved locally",Toast.LENGTH_SHORT).show();
     selectTab("Members");
    });
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
print("PASS v28.0 member portraits: add/change/remove, per-ID local, no Room/finance mutation")
