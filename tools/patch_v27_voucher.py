#!/usr/bin/env python3
"""Add a post-save shareable internal voucher for an actually committed shuttle expense."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=root/"MainActivity.java"
s=activity.read_text()
old='''       runOnUiThread(()->{Toast.makeText(this,"Shuttle purchase recorded",Toast.LENGTH_LONG).show();selectTab("Shuttles");});'''
new='''       runOnUiThread(()->{
        Toast.makeText(this,"Shuttle purchase recorded",Toast.LENGTH_LONG).show();
        selectTab("Shuttles");
        new AlertDialog.Builder(this).setTitle("Shuttle purchase saved")
         .setMessage("Recorded expense "+e.id+" · "+rupee(e.amount)+
          "\\nAn internal voucher can be shared. This is not a supplier bill and does not record another expense.")
         .setNegativeButton("Done",null)
         .setPositiveButton("Share internal voucher",(dd,ww)->
          sharePdf("Internal_Expense_Voucher_"+e.id+".pdf",()->
           PremiumV27ExpenseVoucher.make(this,e))).show();
       });'''
if s.count(old)!=1:raise AssertionError("Shuttle purchase success/UI anchor changed")
s=s.replace(old,new,1)
activity.write_text(s)
(root/"PremiumV27ExpenseVoucher.java").write_text(
 Path("src/premium/v27_4/PremiumV27ExpenseVoucher.java").read_text())
assert "PremiumV27ExpenseVoucher.make(this,e)" in s
print("PASS v27.4 additive internal voucher UI: original purchase remains one transaction")
