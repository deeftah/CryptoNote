package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
	public static String randomB64(int n8) { return bytesToB64(SHA256(random(n8))); }
	
	public static byte[] random(int n8) {
		int n = n8 < 1 ? 1 : (n8 > 4 ? 4 : n8);
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		ByteBuffer bf = ByteBuffer.allocate(n * 8);
		try {
			for(int i = 0; i < n; i++)
				bf.putLong(tlr.nextLong(1, Long.MAX_VALUE));
			return bf.array();
		} catch (Exception e) { 
			return bf.array();
		}
	}
	
	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Retourne en String la représentation en hexa d'un byte[]
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		if (bytes == null || bytes.length == 0) return "";
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static byte[] hexToBytes(String s) {
		if (s == null || s.length() < 2) return new byte[0];
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	public static String bytesToB64P(byte[] b){
		return Base64.getEncoder().encodeToString(b);
	}

	public static String bytesToB64(byte[] b){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
	}

	public static byte[] b64ToBytes(String b64){
		return Base64.getUrlDecoder().decode(b64);
	}

	public static byte[] b64ToBytesX(String b64){
		return Base64.getDecoder().decode(b64);
	}

	public static String bytes2string(byte[] bytes){
		if (bytes == null || bytes.length == 0) return "";
		try { return new String(bytes, "UTF-8"); } catch (Exception e) { return "";}
	}

	public static byte[] bytesFromStream(InputStream is) {
		try {
			if (is == null) return null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int l = 0;
			while((l = is.read(buf)) > 0)
				bos.write(buf, 0, l);
			is.close();
			bos.flush();
			byte[] bytes = bos.toByteArray();
			bos.close();
			return bytes;
		} catch (Exception e) { return null; }
	}

	public static void bytesToStream(byte[] bytes, FileOutputStream os) throws IOException {
		os.write(bytes);
		os.close();
	}

	public static void stringToStream(String s, FileOutputStream os) throws UnsupportedEncodingException, IOException {
		os.write(s.getBytes("UTF-8"));
		os.close();
	}

	/********************************************************************************/
	
	private static KeyPairGenerator keyGen;
	private static KeyFactory keyFactory;
	private static MessageDigest digestSha1;
	private static MessageDigest digestSha256;
	private static SecureRandom random;
	
	static {
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
			random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			keyGen.initialize(2048, random);
//			sig = Signature.getInstance("SHA256withRSA");
			keyFactory = KeyFactory.getInstance("RSA");
			digestSha1 = MessageDigest.getInstance("SHA-1");
			digestSha256 = MessageDigest.getInstance("SHA-256");
		} catch (Exception e) {}
		// OAEPWithSHA1AndMGF1Padding : seul qui accepte de matcher avec Web Cryptography
		// Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding"); // pour tester l'exception
	}
	
//	private static Cipher cipher() throws Exception { 
//		return Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
//	}
	
	/**
	 * Digest SHA-1 d'un byte[] retourné en byte[]
	 * @param x
	 * @return
	 * @throws Exception
	 */
	public static byte[] SHA1(byte[] x) {
		if (x == null) return null;
		synchronized (digestSha1) {
		    digestSha1.reset();
		    digestSha1.update(x);
		    return digestSha1.digest();
		}
	}

	public static String SHA256b64(String x) {
		return bytesToB64(Crypto.SHA256(b64ToBytes(x)));
	}
	
	/**
	 * Digest SHA-256 d'un byte[] retourné en byte[]
	 * @param x
	 * @return
	 * @throws Exception
	 */
	public static byte[] SHA256(byte[] x) {
		if (x == null) return null;
		synchronized (digestSha256) {
		    digestSha256.reset();
		    digestSha256.update(x);
		    return digestSha256.digest();
		}
	}
	
	public static class AES {
		private static final byte[] ivb = {101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116};
        private static final IvParameterSpec iv = new IvParameterSpec(ivb, 0, 16);
        private Cipher cipher;
        private SecretKeySpec skeySpec;
        
		public AES(byte[] key) throws Exception{
			if (key.length != 32) key = SHA256(key);
            skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		}
		
		public AES(String key) throws Exception {
			this(b64ToBytes(key));
		}
		
		public byte[] encrypt(byte[] data) throws Exception {
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            return cipher.doFinal(data);
		}

		public byte[] decrypt(byte[] data) throws Exception {
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            return cipher.doFinal(data);
		}

	}
	
	public static class KP {
		public String pub; 		// PEM public
		public String priv; 	// PEM private
		
		public KP() {
			KeyPair pair = keyGen.generateKeyPair();
			pub = bytesToPem(pair.getPublic().getEncoded(), true);
			priv = bytesToPem(pair.getPrivate().getEncoded(), false);
		}

	}
	
	public static byte[] pemToBytes(String pem){
		String[] a = pem.split("\n");
		StringBuffer sb = new StringBuffer();
		for(String l : a){
			String s = l.trim();
			if (s.length() != 0 && !s.startsWith("---"))
				sb.append(s);
		}
		return b64ToBytesX(sb.toString());
	}
	
	public static String bytesToPem(byte[] bytes, boolean isPub) {
		String s = bytesToB64P(bytes);
		int i = 0;
		String x = isPub ? "PUBLIC" : "PRIVATE";
		StringBuffer a = new StringBuffer().append("-----BEGIN " + x + " KEY-----\n");
		int l = s.length();
		while (i < l) {
			a.append(i + 64 >= l ? s.substring(i) : s.substring(i, i + 64)).append("\n");
			i += 64;
		}
		a.append("-----END " + x + " KEY-----");
		return a.toString();
	}
		
	public static class Encrypter {
		private Cipher cipher;
		public Encrypter(String pem) throws Exception {
			cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
			byte[] spki = pemToBytes(pem);
			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(spki));
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		}
		
		public byte[] encrypt(byte[] texte) throws Exception {
			return cipher.doFinal(texte);
		}		
	}

	public static class Decrypter {
		private Cipher cipher;
		public Decrypter(String pem) throws Exception {
			cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
			byte[] pkcs8 = pemToBytes(pem);
			RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
		}
		
		public byte[] decrypt(byte[] encrypted) throws Exception {
			return cipher.doFinal(encrypted);
		}		
	}

	public static class Signer {
		private RSAPrivateKey privateKey;
		private Signature sig;
		public Signer(String pem) throws Exception {
			byte[] pkcs8 = pemToBytes(pem);
			sig = Signature.getInstance("SHA256withRSA");
			privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
		}
		
		public byte[] sign(byte[] texte) throws Exception {
			sig.initSign(privateKey);
			sig.update(texte);
			return sig.sign();		
		}
	}
	
	public static class Verifier {
		private RSAPublicKey publicKey;
		private Signature sig;
		public Verifier(String pem) throws Exception {
			byte[] spki = pemToBytes(pem);
			sig = Signature.getInstance("SHA256withRSA");
			publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(spki));
		}
		
		public boolean verify(byte[] signature, byte[] texte) throws Exception {
			sig.initVerify(publicKey);
			sig.update(texte);
			return sig.verify(signature);
		}
	}

	/**********************************************************/
	public static final String bytes2String(byte[] b){
		if (b == null || b.length == 0) return "[]";
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < b.length; i++)
			sb.append(i == 0 ? '[' : ',').append(b[i]);
		return sb.append(']').toString();
	}
		
	// https://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters
	// Security.setProperty("crypto.policy", "unlimited");
	// Requis pour java8 < 130
	/*
	public static void fixKeyLength() {
	    String errorString = "Failed manually overriding key-length permissions.";
	    int newMaxKeyLength;
	    try {
	        if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
	            Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
	            Constructor con = c.getDeclaredConstructor();
	            con.setAccessible(true);
	            Object allPermissionCollection = con.newInstance();
	            Field f = c.getDeclaredField("all_allowed");
	            f.setAccessible(true);
	            f.setBoolean(allPermissionCollection, true);

	            c = Class.forName("javax.crypto.CryptoPermissions");
	            con = c.getDeclaredConstructor();
	            con.setAccessible(true);
	            Object allPermissions = con.newInstance();
	            f = c.getDeclaredField("perms");
	            f.setAccessible(true);
	            ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

	            c = Class.forName("javax.crypto.JceSecurityManager");
	            f = c.getDeclaredField("defaultPolicy");
	            f.setAccessible(true);
	            Field mf = Field.class.getDeclaredField("modifiers");
	            mf.setAccessible(true);
	            mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
	            f.set(null, allPermissions);

	            newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(errorString, e);
	    }
	    if (newMaxKeyLength < 256)
	        throw new RuntimeException(errorString); // hack failed
	}
	*/
	
	public static void main(String[] args){
		try {
			// fixKeyLength();
			Security.setProperty("crypto.policy", "unlimited");
			
			byte[] crypted;
			byte[] decrypted;
			
			String s;
			String privkey;
			String pubkey;
			byte[] toto = "toto est beau".getBytes("UTF-8");
			byte[] titi = "titi est beau".getBytes("UTF-8");
			
			String aeskey = bytes2string(bytesFromStream(new FileInputStream ("data/aeskey.txt")));
			String aescrypted = bytes2string(bytesFromStream(new FileInputStream ("data/aescrypted.txt")));
			byte[] binkey = b64ToBytes(aeskey);
			byte[] bincrypted = b64ToBytesX(aescrypted);
			bytesToStream(binkey, new FileOutputStream("data/aeskey.bin"));
			bytesToStream(bincrypted, new FileOutputStream("data/aescrypted.bin"));
			System.out.println(binkey.length);
			AES aes = new AES(aeskey);
			crypted = aes.encrypt(toto);
			System.out.println(bytesToB64P(crypted));
			decrypted = aes.decrypt(crypted);
			System.out.println(new String(decrypted, "UTF-8"));
			
			decrypted = aes.decrypt(bincrypted);
			System.out.println(new String(decrypted, "UTF-8"));
			
			pubkey =  bytes2string(bytesFromStream(new FileInputStream ("data/public.pem")));
			privkey = bytes2string(bytesFromStream(new FileInputStream ("data/private.pem")));

			Encrypter ec = new Encrypter(pubkey);
			crypted = ec.encrypt(toto);
			System.out.println(bytesToB64(crypted));
			stringToStream(bytesToB64(crypted), new FileOutputStream("data/crypted2.txt"));

			Decrypter dc = new Decrypter(privkey);
			decrypted = dc.decrypt(crypted);
			System.out.println(new String(decrypted, "UTF-8"));

			s =  bytes2string(bytesFromStream(new FileInputStream ("data/crypted.txt")));
			byte[] crypted2 = b64ToBytes(s);
			bytesToStream(crypted2, new FileOutputStream("data/crypted2.bin"));
			decrypted = dc.decrypt(crypted2);
			System.out.println(new String(decrypted, "UTF-8"));
			
			pubkey =  bytes2string(bytesFromStream(new FileInputStream ("data/publics.pem")));
			privkey = bytes2string(bytesFromStream(new FileInputStream ("data/privates.pem")));
			s =  bytes2string(bytesFromStream(new FileInputStream ("data/sign.txt")));
			byte[] sign = b64ToBytes(s);
			// byte[] ssaSpki, byte[] signature, byte[] texte
			Verifier vf = new Verifier(pubkey);
			boolean v = vf.verify(sign, toto);
			System.out.println(v);
			v = vf.verify(sign, titi);
			System.out.println(v);

			crypted = new Signer(privkey).sign(toto);
			System.out.println(bytesToB64(crypted));
			stringToStream(bytesToB64(crypted), new FileOutputStream("data/sign2.txt"));

			KP kp = new KP();
			Encrypter ec2 = new Encrypter(kp.pub);
			crypted = ec2.encrypt(toto);
			System.out.println(bytesToB64(crypted));

			Decrypter dc2 = new Decrypter(kp.priv);
			decrypted = dc2.decrypt(crypted);
			System.out.println(new String(decrypted, "UTF-8"));

			crypted = new Signer(kp.priv).sign(toto);
			System.out.println(bytesToB64(crypted));
			vf = new Verifier(kp.pub);
			v = vf.verify(crypted, toto);
			System.out.println(v);
			v = vf.verify(sign, titi);
			System.out.println(v);
			
		} catch (Throwable t){
			t.printStackTrace();
		}
	}
	
}
