package com.vinayak.badminton.nativepreview;

public final class V2793ExportGateSmoke {
 static void yes(boolean v,String s){if(!v)throw new AssertionError(s);}
 public static void main(String[] args){
  PremiumV2793ExportGate g=new PremiumV2793ExportGate();
  yes(g.begin(),"First export accepted");
  yes(g.inProgress(),"Export remains reserved throughout picker");
  yes(!g.begin(),"Second exporter cannot replace pending file");
  g.finish(); // Canceled picker / wrong password / verified file save.
  yes(!g.inProgress(),"Failure path releases reservation");
  yes(g.begin(),"New independent export succeeds");
  yes(!g.begin(),"Rapid repeated taps blocked");
  g.finish();g.finish();
  yes(g.begin(),"Idempotent cleanup does not permanently lock export");
  g.finish();
  System.out.println("PASS v27.9.3 encrypted export single-flight picker protection");
 }
}
