package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;

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
	
	public static String bytesToB64P(byte[] b){
		return Base64.getUrlEncoder().encodeToString(b);
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
	
//	private static KeyPairGenerator keyGen;
	private static KeyFactory keyFactory;
	private static Signature sig;
	private static MessageDigest digestSha1;
	private static MessageDigest digestSha256;
	
	static void startup() throws Exception {
//		keyGen = KeyPairGenerator.getInstance("RSA");
//		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
//		keyGen.initialize(2048, random);
		sig = Signature.getInstance("SHA256withRSA");
		keyFactory = KeyFactory.getInstance("RSA");
		digestSha1 = MessageDigest.getInstance("SHA-1");
		digestSha256 = MessageDigest.getInstance("SHA-256");
		// OAEPWithSHA1AndMGF1Padding : seul qui accepte de matcher avec Web Cryptography
		// Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding"); // pour tester l'exception
	}
	
	private static Cipher cipher() throws Exception { 
		return Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
	}
	
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
	
//	public static byte[][] generate() {
//		KeyPair pair = keyGen.generateKeyPair();
//		byte[][] res = new byte[2][];
//		res[0] = pair.getPrivate().getEncoded();
//		res[1] = pair.getPublic().getEncoded();
//		return res;
//	}
	
	public static byte[] sign(byte[] ssaPkcs8, byte[] texte) throws Exception {
		synchronized (sig) {
			RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(ssaPkcs8));
			sig.initSign(privateKey);
			sig.update(texte);
			return sig.sign();
		}
	}
	
	public static boolean verify(byte[] ssaSpki, byte[] signature, byte[] texte) throws Exception {
		synchronized (sig) {
			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(ssaSpki));
			sig.initVerify(publicKey);
			sig.update(texte);
			return sig.verify(signature);
		}
	}
	
	public static byte[] pemToSpki(String b64){
		String s = b64.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\n", "").replaceAll("\r", "");
		return b64ToBytesX(s);
	}
	
	public static byte[] encrypt(byte[] spki, byte[] texte) throws Exception {
		Cipher cipher = cipher();
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(spki));
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(texte);
	}

	public static byte[] pemToPkcs8(String b64){
		String s = b64.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\n", "").replaceAll("\r", "");
		return b64ToBytesX(s);
	}

	public static byte[] decrypt(byte[] pkcs8, byte[] encrypted) throws Exception {
		Cipher cipher = cipher();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(encrypted);
	}

	/**********************************************************/
	public static final String bytes2String(byte[] b){
		if (b == null || b.length == 0) return "[]";
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < b.length; i++)
			sb.append(i == 0 ? '[' : ',').append(b[i]);
		return sb.append(']').toString();
	}
		
	public static void test1() throws Exception {
		byte[] u8 = {0,2,-1};
//		System.out.println(bytesToBase64Y(u8,false));
		String s1 = bytesToB64(u8);
		System.out.println(s1);
		System.out.println(bytes2String(b64ToBytes(s1)));
		String s2 = bytesToB64P(u8);
		System.out.println(s2);
		System.out.println(bytes2String(b64ToBytes(s2)));

		byte[] u8b = {0,2,-1,32};
//		System.out.println(bytesToBase64Y(u8b,false));
		String s1b = bytesToB64(u8b);
		System.out.println(s1b);
		System.out.println(bytes2String(b64ToBytes(s1b)));
		String s2b = bytesToB64(u8b);
		System.out.println(s2b);
		System.out.println(bytes2String(b64ToBytes(s2b)));

		byte[] u8c = {0,2,-1,32,48};
//		System.out.println(bytesToBase64Y(u8c,false));
		String s1c = bytesToB64(u8c);
		System.out.println(s1c);
		System.out.println(bytes2String(b64ToBytes(s1c)));
		String s2c = bytesToB64(u8c);
		System.out.println(s2c);
		System.out.println(bytes2String(b64ToBytes(s2c)));
		
		byte[] u8d = new byte[10000];
		for(int i = 0; i < u8d.length; i++) u8d[i] = (byte)(i % 92);
		long t0 = System.currentTimeMillis();
//		for(int i = 0; i < 10000; i++)
//			bytesToBase64Y(u8c,false);
		long t1 =  System.currentTimeMillis();
		System.out.println((t1 - t0) + "ms");
		for(int i = 0; i < 10000; i++)
			bytesToB64(u8c);
		long t2 =  System.currentTimeMillis();
		System.out.println((t2 - t1) + "ms");

		Properties props = new Properties();
		props.load(new InputStreamReader(new FileInputStream("data/test1.properties"), "UTF-8"));
		
		byte[] texte2 = props.getProperty("obj").getBytes("UTF-8");
//		byte[] texte = base64ToBytes(props.getProperty("texte"));
		
		String sign1 = props.getProperty("sign1");
//		byte[] signature1 = base64ToBytes(props.getProperty("sign1"));
		System.out.println("sign1=" + sign1);

		byte[] encrypted1 = b64ToBytes(props.getProperty("encrypted"));
			
//		byte[] ssaPkcs8 = base64ToBytes(props.getProperty("ssaPkcs8"));
//		byte[] signature2 = sign(ssaPkcs8, texte);
//		String sign2 = bytesToBase64(signature2);
//		System.out.println("sign2=" + sign2);
//		
//		byte[] ssaSpki = base64ToBytes(props.getProperty("ssaSpki"));
//		boolean b = verify(ssaSpki, texte, signature1);
//		System.out.println(b);
					
		byte[] spki = b64ToBytes(props.getProperty("spki"));
		byte[] encrypted = encrypt(spki, texte2);
		String encrypted2 = bytesToB64(encrypted);
		System.out.println("encrypted=" + encrypted2);

		String pkcs8s = props.getProperty("pkcs8");
		byte[] pkcs8 = b64ToBytes(pkcs8s);
		byte[] decrypted = decrypt(pkcs8, encrypted1);
		String decrypted2 = new String(decrypted, "UTF-8");
		
		System.out.println("decrypted=" + decrypted2);
		
	}
	
	public static void main(String[] args){
		try {
			startup();
			// test1();
			
			byte[] crypted;
			byte[] decrypted;
			
			String s;
			byte[] privkey;
			byte[] pubkey;
			byte[] toto = "toto est beau".getBytes("UTF-8");
			byte[] titi = "titi est beau".getBytes("UTF-8");
			
			pubkey =  pemToSpki(bytes2string(bytesFromStream(new FileInputStream ("data/public.pem"))));
			crypted = encrypt(pubkey, toto);
			System.out.println(bytesToB64(crypted));
			stringToStream(bytesToB64(crypted), new FileOutputStream("data/crypted2.txt"));

			privkey = pemToPkcs8(bytes2string(bytesFromStream(new FileInputStream ("data/private.pem"))));
			decrypted = decrypt(privkey, crypted);
			System.out.println(new String(decrypted, "UTF-8"));

			s =  bytes2string(bytesFromStream(new FileInputStream ("data/crypted.txt")));
			byte[] crypted2 = b64ToBytes(s);
			bytesToStream(crypted2, new FileOutputStream("data/crypted2.bin"));
			decrypted = decrypt(privkey, crypted2);
			System.out.println(new String(decrypted, "UTF-8"));
			
			pubkey =  pemToSpki(bytes2string(bytesFromStream(new FileInputStream ("data/publics.pem"))));
			privkey = pemToPkcs8(bytes2string(bytesFromStream(new FileInputStream ("data/privates.pem"))));
			s =  bytes2string(bytesFromStream(new FileInputStream ("data/sign.txt")));
			byte[] sign = b64ToBytes(s);
			// byte[] ssaSpki, byte[] signature, byte[] texte
			boolean v = verify(pubkey, sign, toto);
			System.out.println(v);
			v = verify(pubkey, sign, titi);
			System.out.println(v);

			crypted = sign(privkey, toto);
			System.out.println(bytesToB64(crypted));
			stringToStream(bytesToB64(crypted), new FileOutputStream("data/sign2.txt"));

		} catch (Throwable t){
			t.printStackTrace();
		}
	}
	
}
