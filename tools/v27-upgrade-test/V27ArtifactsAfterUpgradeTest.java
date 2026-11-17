package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.core.content.FileProvider;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Signed v25.9.1 -> v27.9 synthetic upgrade follow-on.
 * Exercises actual Android PDF/FileProvider and encrypted-backup staging.
 *
 * It does not launch a picker, touch real club data, create ledger entries,
 * mutate original cash, or replace/import installed Room state.
 */
@RunWith(AndroidJUnit4.class)
public final class V27ArtifactsAfterUpgradeTest {
 private static final String DB="badminton_room_v18.db";
 private static final String MEMBER_ID="ci-upgrade-synthetic-member-20261114";
 private static final String PAYMENT_ID="pay-ci-upgrade-synthetic-20261114";
 private static final String EXPENSE_ID="ci-upgrade-synthetic-expense-20261114";
 private static final String PERIOD="2026-08";

 private static void assertValidPdf(File pdf,int width,int height)throws Exception{
  assertNotNull("PDF must be generated",pdf);
  assertTrue("PDF must exist and contain data",pdf.isFile()&&pdf.length()>200);
  try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);
      PdfRenderer reader=new PdfRenderer(fd)){
   assertEquals("Generated document must have one page",1,reader.getPageCount());
   try(PdfRenderer.Page page=reader.openPage(0)){
    assertEquals("Expected PDF page width",width,page.getWidth());
    assertEquals("Expected PDF page height",height,page.getHeight());
   }
  }
 }

 private static void assertFileProviderRead(Context context,File file)throws Exception{
  assertNotNull(file);
  Uri content=FileProvider.getUriForFile(context,context.getPackageName()+".files",file);
  assertEquals("FileProvider must give content URI","content",content.getScheme());
  long length=0;
  byte[] block=new byte[8192];
  try(InputStream input=context.getContentResolver().openInputStream(content)){
   assertNotNull("Provider URI must be reopenable",input);
   int n;
   while((n=input.read(block))!=-1){
    length+=n;
    assertTrue("Provider content unexpectedly oversized",length<40_000_000L);
   }
  }
  assertEquals("Shareable URI bytes must equal generated file",file.length(),length);
 }

 private static void expectStageFailure(File file,byte[] expected)throws Exception{
  boolean rejected=false;
  try{PremiumV27BackupStaging.verify(file,expected);}
  catch(IOException fail){rejected=true;}
  assertTrue("Corrupted encrypted staging bytes must be rejected",rejected);
 }

 @Test public void verifyReadOnlyArtifactsAfterSignedUpgrade()throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  LedgerDatabase db=Room.databaseBuilder(context,LedgerDatabase.class,DB)
   .allowMainThreadQueries().build();
  File receipt=null,voucher=null,stagedZip=null,finalZip=null,encryptedCopy=null;
  try{
   LedgerDao dao=db.dao();
   assertNotNull("Synthetic ledger seed must have survived signed upgrade",dao.meta());
   assertEquals("Synthetic roster must survive",15,dao.members().size());
   Member member=null;
   for(Member m:dao.members())if(MEMBER_ID.equals(m.id))member=m;
   assertNotNull("Synthetic receipt owner not present",member);
   Contribution payment=null;
   for(Contribution c:dao.contributions())if(PAYMENT_ID.equals(c.id))payment=c;
   assertNotNull("Original synthetic cash payment not found",payment);
   assertEquals("cash",payment.kind);
   assertEquals(1379L,payment.amount);
   assertEquals(PERIOD,payment.feeMonth);
   Expense expense=null;
   for(Expense e:dao.expenses())if(EXPENSE_ID.equals(e.id))expense=e;
   assertNotNull("Synthetic recorded shuttle purchase not found",expense);
   assertEquals(2067L,expense.amount);
   String original=MigrationCodec.exportVerified(db);
   String verifiedBefore=MigrationCodec.canonical(MigrationCodec.inspect(original).ledger);

   // v27.4: supplementary voucher is a portrait PDF, not a new expense.
   voucher=PremiumV27ExpenseVoucher.make(context,expense);
   assertValidPdf(voucher,595,842);
   assertFileProviderRead(context,voucher);
   System.out.println("V27_TEST_VOUCHER_PDF_FILEPROVIDER_PASS");

   // Native original receipt PDF stays shareable and has a single portrait page.
   receipt=HistoricalReceiptsV253.payment(context,member,payment,dao.meta());
   assertValidPdf(receipt,595,842);
   assertFileProviderRead(context,receipt);
   // Verify that a real Android FileProvider input stream reads back the exact
   // previously reviewed PDF. SAF picker itself still needs physical-phone QA.
   PremiumV2794PdfSaveGuard pdfSession=new PremiumV2794PdfSaveGuard();
   assertTrue("PDF Save should reserve first Documents picker",pdfSession.begin());
   assertFalse("Second simultaneous PDF picker must be blocked",pdfSession.begin());
   try{
    pdfSession.stage(receipt);
    Uri receiptUri=FileProvider.getUriForFile(
     context,context.getPackageName()+".files",receipt);
    try(InputStream readback=context.getContentResolver().openInputStream(receiptUri)){
     pdfSession.verify(receipt,readback);
    }
   }finally{pdfSession.finish();}
   assertFalse("PDF Save session must be released",pdfSession.active());
   System.out.println("V27_TEST_PDF_SAVE_SHA_READBACK_PASS");
   System.out.println("V27_TEST_RECEIPT_PDF_FILEPROVIDER_PASS");

   // v27.3: exercise actual ZIP staging, payload integrity and Android sharing.
   File dir=new File(context.getCacheDir(),"pdfs");
   assertTrue(dir.isDirectory()||dir.mkdirs());
   stagedZip=new File(dir,"ci-v27-receipt-"+UUID.randomUUID()+".partial");
   finalZip=new File(dir,"ci-v27-receipt-"+UUID.randomUUID()+".zip");
   try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(stagedZip));
       InputStream input=new FileInputStream(receipt)){
    out.putNextEntry(new ZipEntry("001_"+PERIOD+"_"+PAYMENT_ID+".pdf"));
    byte[] bytes=new byte[8192];int n;
    while((n=input.read(bytes))!=-1)out.write(bytes,0,n);
    out.closeEntry();
   }
   PremiumV27ReceiptArchive.promote(stagedZip,finalZip,1);
   assertFalse("Staging file must be removed after success",stagedZip.exists());
   PremiumV27ReceiptArchive.verify(finalZip,1);
   assertFileProviderRead(context,finalZip);
   System.out.println("V27_TEST_ARCHIVE_ZIP_FILEPROVIDER_PASS");

   // v27.8: use actual Android AES-GCM for a verified export, confirm staged
   // ciphertext bytes, and reject changed bytes without any Room modification.
   char[] password="ci-v27-only-strong-unique-passphrase".toCharArray();
   try{
    String envelope=PremiumBackupCrypto.encrypt(original,password);
    byte[] expected=envelope.getBytes(StandardCharsets.UTF_8);
    encryptedCopy=new File(context.getCacheDir(),"ci-v27-encrypted-"+UUID.randomUUID()+".bpe");
    try(FileOutputStream output=new FileOutputStream(encryptedCopy)){
     output.write(expected);output.flush();
    }
    PremiumV27BackupStaging.verify(encryptedCopy,expected);
    assertEquals("Android AES-GCM must decrypt exactly",original,
     PremiumBackupCrypto.decrypt(envelope,password));
    byte[] changed=expected.clone();
    changed[changed.length/2]^=1;
    expectStageFailure(encryptedCopy,changed);
    assertFileProviderReadOnlyNotRequiredForPrivateBackup(encryptedCopy);
    System.out.println("V27_TEST_ENCRYPTED_STAGING_PASS");
   }finally{Arrays.fill(password,(char)0);}

   // Read-only closing and export gates must reject stale or repeated input.
   String currentMeta=dao.meta().inventoryStockJson;
   String hash=PremiumV27CloseReview.fingerprint(currentMeta);
   PremiumV27CloseReview.requireUnchanged(hash,currentMeta);
   boolean rejected=false;
   try{PremiumV27CloseReview.requireUnchanged(hash,currentMeta+" ");}
   catch(IllegalStateException expected){rejected=true;}
   assertTrue("Changed month-end snapshot must not be committed",rejected);
   PremiumV27TrendAudit.requireOrdered(new String[]{"2026-01","2026-08"});
   assertTrue(PremiumV27StockVariance.describe(20,19).contains("SHORTFALL"));
   System.out.println("V27_TEST_FINANCE_STOCK_READ_ONLY_GUARDS_PASS");

   String verifiedAfter=MigrationCodec.canonical(
    MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
   assertEquals("All receipt/voucher/ZIP/backup checks must leave every ledger field unchanged",
    verifiedBefore,verifiedAfter);
   assertEquals("Member count unchanged",15,dao.members().size());
   assertEquals("No duplicate payments",2,dao.contributions().size());
   assertEquals("No duplicate expenses",1,dao.expenses().size());
   assertEquals("No new stock-usage events",1,dao.stockUsages().size());
   System.out.println("V27_ARTIFACTS_AFTER_SIGNED_UPGRADE_PASS: no original ledger mutation");
  }finally{
   if(receipt!=null)receipt.delete();
   if(voucher!=null)voucher.delete();
   if(stagedZip!=null)stagedZip.delete();
   if(finalZip!=null)finalZip.delete();
   if(encryptedCopy!=null)encryptedCopy.delete();
   db.close();
  }
 }

 private static void assertFileProviderReadOnlyNotRequiredForPrivateBackup(File file){
  assertTrue("Private cached encrypted backup must remain accessible",file.isFile()&&file.length()>40);
 }
}
