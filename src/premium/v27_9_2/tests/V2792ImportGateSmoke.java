package com.vinayak.badminton.nativepreview;

public final class V2792ImportGateSmoke {
 static void yes(boolean ok,String why){
  if(!ok)throw new AssertionError(why);
 }
 static void rejects(Runnable attempt,String why){
  boolean failed=false;
  try{attempt.run();}catch(IllegalArgumentException expected){failed=true;}
  yes(failed,why);
 }
 public static void main(String[] args) {
  PremiumV2792ImportGate<Object> gate=new PremiumV2792ImportGate<>();
  Object first=new Object(),second=new Object();
  rejects(()->gate.stage(null),"Cannot stage unverified/absent backup");
  long a=gate.stage(first);
  yes(a>0,"Nonzero first session");
  long b=gate.stage(second);
  yes(b>a,"Backup selection advances session");
  yes(!gate.consume(first,a),"Stale confirmation must not import newer backup");
  yes(!gate.cancel(a),"Stale cancel must not clear newer backup");
  yes(!gate.consume(second,a),"Mismatched token blocked");
  yes(gate.consume(second,b),"Exact preview and session may be used once");
  yes(!gate.consume(second,b),"Double confirmation/replay must be rejected");

  Object equalButNotSame=new String("same-content");
  Object otherEqual=new String("same-content");
  long c=gate.stage(equalButNotSame);
  yes(!gate.consume(otherEqual,c),"Equal contents are not an approved identical review");
  yes(gate.cancel(c),"Current session cancel works");
  yes(!gate.consume(equalButNotSame,c),"Canceled confirmation is unusable");
  long d=gate.stage(first);
  yes(gate.consume(first,d),"A newly reviewed session is independent");
  System.out.println("PASS v27.9.2 backup confirmation identity, stale/replay/cancel fail closed");
 }
}
