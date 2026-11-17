package com.vinayak.badminton.nativepreview;
import java.io.IOException;

/**
 * Strict Classic Master paper contract. Android print sheets can be changed
 * from ISO A4 landscape to Letter landscape after the app creates the job.
 * The Classic 842x595 PDF may never be silently scaled to another paper size.
 */
final class PremiumV27100A4PrintGuard {
 private PremiumV27100A4PrintGuard(){}

 static void requireExact(String chosenId,int width,int height,
                          String requiredId,int expectedWidth,int expectedHeight)throws IOException {
  if(chosenId==null||requiredId==null||!requiredId.equals(chosenId)||
     width!=expectedWidth||height!=expectedHeight||
     expectedWidth<=expectedHeight||expectedHeight<=0)
   throw new IOException("Select exactly A4 landscape in Android Print Settings. The original Classic Master must not be scaled to another paper size.");
 }
}
