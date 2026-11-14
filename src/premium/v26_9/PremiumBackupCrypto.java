package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * v26.8 optional, manual encrypted migration export. No cloud upload and no key in repo.
 * AES-256-GCM, fresh random 12-byte nonce/16-byte salt, PBKDF2-HMAC-SHA256.
 * Only the outer manifest is cleartext; verified ledger stays inside authenticated ciphertext.
 */
final class PremiumBackupCrypto {
 static final String FORMAT="BADMINTON_PREMIUM_ENCRYPTED_BACKUP";
 static final int VERSION=1, ITERATIONS=240000, MAX_INPUT=12_000_000;
 static final int MAX_ENVELOPE=19_000_000;
 private static final SecureRandom RANDOM=new SecureRandom();
 private PremiumBackupCrypto(){}

 private static byte[] sha(byte[] bytes) throws Exception {
  return MessageDigest.getInstance("SHA-256").digest(bytes);
 }
 private static String hash(byte[] bytes)throws Exception {
  StringBuilder text=new StringBuilder();
  for(byte x:sha(bytes))text.append(String.format(Locale.ROOT,"%02x",x&0xff));
  return text.toString();
 }
 private static byte[] key(char[] password,byte[] salt,int iteration)throws Exception {
  if(password==null||password.length<12||password.length>256)
   throw new IllegalArgumentException("Choose a password of at least 12 characters");
  if(iteration<200000||iteration>1000000)throw new IllegalArgumentException("Invalid backup key-derivation cost");
  PBEKeySpec spec=new PBEKeySpec(password,salt,iteration,256);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
  finally{spec.clearPassword();}
 }
 static String encrypt(String verifiedJson,char[] password)throws Exception {
  if(verifiedJson==null)throw new IllegalArgumentException("Verified ledger required");
  byte[] plaintext=verifiedJson.getBytes(StandardCharsets.UTF_8);
  if(plaintext.length==0||plaintext.length>MAX_INPUT)
   throw new IllegalArgumentException("Verified backup must fit in the 12 MB safety limit");
  MigrationCodec.inspect(verifiedJson); // Do not encrypt invalid/unverified ledger data.
  byte[] salt=new byte[16],nonce=new byte[12];
  RANDOM.nextBytes(salt);RANDOM.nextBytes(nonce);
  byte[] bytes=key(password,salt,ITERATIONS);
  try{
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(bytes,"AES"),new GCMParameterSpec(128,nonce));
   cipher.updateAAD((FORMAT+"/"+VERSION).getBytes(StandardCharsets.UTF_8));
   byte[] sealed=cipher.doFinal(plaintext);
   JSONObject envelope=new JSONObject();
   envelope.put("format",FORMAT);envelope.put("version",VERSION);
   envelope.put("kdf","PBKDF2WithHmacSHA256");envelope.put("iterations",ITERATIONS);
   envelope.put("cipher","AES-256-GCM");
   envelope.put("salt",Base64.getEncoder().encodeToString(salt));
   envelope.put("nonce",Base64.getEncoder().encodeToString(nonce));
   envelope.put("sha256",hash(plaintext));
   envelope.put("plainBytes",plaintext.length);
   envelope.put("payload",Base64.getEncoder().encodeToString(sealed));
   String output=envelope.toString();
   if(output.getBytes(StandardCharsets.UTF_8).length>MAX_ENVELOPE)
    throw new IllegalArgumentException("Encrypted export exceeds safety limit");
   return output;
  }finally{Arrays.fill(bytes,(byte)0);Arrays.fill(plaintext,(byte)0);}
 }
 static String decrypt(String envelope,char[] password)throws Exception {
  if(envelope==null||envelope.getBytes(StandardCharsets.UTF_8).length>MAX_ENVELOPE)
   throw new IllegalArgumentException("Encrypted backup missing or too large");
  JSONObject data=new JSONObject(envelope);
  if(!FORMAT.equals(data.optString("format"))||data.getInt("version")!=VERSION||
     !"AES-256-GCM".equals(data.optString("cipher"))||
     !"PBKDF2WithHmacSHA256".equals(data.optString("kdf")))
   throw new IllegalArgumentException("Unsupported encrypted backup format");
  int count=data.getInt("plainBytes"),iterations=data.getInt("iterations");
  if(count<1||count>MAX_INPUT)
   throw new IllegalArgumentException("Backup declares unsafe data size");
  byte[] salt=Base64.getDecoder().decode(data.getString("salt"));
  byte[] nonce=Base64.getDecoder().decode(data.getString("nonce"));
  byte[] payload=Base64.getDecoder().decode(data.getString("payload"));
  if(salt.length!=16||nonce.length!=12||payload.length!=count+16)
   throw new IllegalArgumentException("Encrypted backup metadata is inconsistent");
  byte[] bytes=key(password,salt,iterations);
  try{
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(bytes,"AES"),new GCMParameterSpec(128,nonce));
   cipher.updateAAD((FORMAT+"/"+VERSION).getBytes(StandardCharsets.UTF_8));
   byte[] clear;
   try{clear=cipher.doFinal(payload);}
   catch(AEADBadTagException e){throw new IllegalArgumentException("Incorrect password or modified encrypted backup",e);}
   try{
    if(clear.length!=count||!MessageDigest.isEqual(
       data.getString("sha256").getBytes(StandardCharsets.US_ASCII),
       hash(clear).getBytes(StandardCharsets.US_ASCII)))
     throw new IllegalArgumentException("Backup integrity check failed");
    String json=new String(clear,StandardCharsets.UTF_8);
    MigrationCodec.inspect(json);
    return json;
   }finally{Arrays.fill(clear,(byte)0);}
  }finally{Arrays.fill(bytes,(byte)0);}
 }
}
