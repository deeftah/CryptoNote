package fr.cryptonote.base;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
import java.security.MessageDigest;
//import java.security.SecureRandom;
//import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;

public class Crypto {
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
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static String intToBase64(int intv) {
	    int[] b = {0, 0, 0, 0};
	    for (int i = 0; i < 4; i++) {
	        int byt = intv & 0xff;
	        b[i] = byt;
	        intv = (intv - byt) / 256 ;
	    }

	    char[] cx = chars2;
		String out = "" + cx[b[0] >> 2];
		out += cx[((b[0] & 3) << 4) | (b[1] >> 4)];
		out += cx[((b[1] & 15) << 2) | (b[2] >> 6)];
		out += cx[b[2] & 63];
		out += cx[b[3] >> 2];
		out += cx[((b[3] & 3) << 4)];
		return out;
	}

	/*
	 * Retourne un String en base64 hash Java d'un string
	 */
	public static String hashOf(String s) {
		if (s == null) s = "";
		return intToBase64(s.hashCode());
	};
	
	public static String timeRandom() {
		long t = System.currentTimeMillis();
		int j = (int)(t / 86400000);
		int h = (int)(t % 86400000);
		return new StringBuffer().append(intToB64(j)).append(intToB64(h))
			.append(bytesToBase64(random(1), true)).toString();
	}
	
	public static String randomB64(int n8) { return bytesToBase64(random(n8), true); }
	
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

	private static final char[] chars = {
		'A','B','C','D','E','F','G','H',
		'I','J','K','L','M','N','O','P',
		'Q','R','S','T','U','V','W','X',
		'Y','Z','a','b','c','d','e','f',
		'g','h','i','j','k','l','m','n',
		'o','p','q','r','s','t','u','v',
		'w','x','y','z','0','1','2','3',
		'4','5','6','7','8','9','+','/',
	};

	private static final char[] chars2 = {
		'A','B','C','D','E','F','G','H',
		'I','J','K','L','M','N','O','P',
		'Q','R','S','T','U','V','W','X',
		'Y','Z','a','b','c','d','e','f',
		'g','h','i','j','k','l','m','n',
		'o','p','q','r','s','t','u','v',
		'w','x','y','z','0','1','2','3',
		'4','5','6','7','8','9','-','_',
	};

	public static boolean isBase64NN(String b64){
		if (b64 == null || b64.length() == 0) return false;
		return isBase64(b64);
	}
	
	public static boolean isBcrypt(String bcrypt){
		if (bcrypt == null || bcrypt.length() != 31) return false;
		for(int i = 0; i < 31; i++){
			char c = bcrypt.charAt(i);
			if ((c == '-' || c == '_') 
				|| (c >= '0' && c <= '9') 
				|| (c >= 'a' && c <= 'z') 
				|| (c >= 'A' && c <= 'Z')) continue;		
			return false;
		}
		return true;
	}

	public static boolean isBase64(String b64){
		if (b64 == null || b64.length() == 0) return true;
		int len = b64.length();

		if (b64.charAt(len - 1) == '=') {
			len--;
			if (b64.charAt(len - 1) == '=') {
				len--;
			}
		}
		if (len % 4 == 1) return false;
		for(int i = 0; i < len; i++){
			char c = b64.charAt(i);
			if ((c == '+' || c == '-' || c == '/' || c == '_') 
				|| (c >= '0' && c <= '9') 
				|| (c >= 'a' && c <= 'z') 
				|| (c >= 'A' && c <= 'Z')) continue;		
			return false;
		}
		return true;
	}
	
	private static byte[] lookup;
	
	public static final byte[] intToByteArray(int data) {
		int l = 4;
		byte b0 = (byte) ((data & 0xFF000000) >> 24);
		if (b0 == 0) l = 3;
		byte b1 = (byte) ((data & 0x00FF0000) >> 16);
		if (l == 3 && b1 == 0) l = 2;
		byte b2 = (byte) ((data & 0x0000FF00) >> 8);
		if (l == 2 && b2 == 0) l = 1;
		byte b3 = (byte) ((data & 0x000000FF) >> 0);
		if (l == 1) {
			byte[] r = {b3};
			return r;
		}
		if (l == 2) {
			byte[] r = {b2, b3};
			return r;
		}
		if (l == 3) {
			byte[] r = {b1, b2, b3};
			return r;
		}
		byte[] r = {b0, b1, b2, b3};
		return r;
	}
	
	public static final String intToB64(int data) {
		return bytesToBase64(intToByteArray(data), true);
	}
	
	public static final byte[] bytes0 = new byte[0];

	public static int base64Length(String base64) {
		if (base64 == null || base64.length() == 0) return 0;
		base64 = base64.replaceAll("\n", "");
		return base64LengthNNL(base64);
	}
	
	private static int base64LengthNNL(String base64) {
		if (base64 == null || base64.length() == 0) return 0;
		int len = base64.length();
		int bufferLength = (len * 3) / 4;
		if (base64.charAt(len - 1) == '=') {
			bufferLength--;
			len--;
			if (base64.charAt(len - 1) == '=') {
				bufferLength--;
				len--;
			}
		}
		return len % 4 == 1 ? 0 : bufferLength; // pas un base64
	}
	
	public static byte[] base64ToBytes(String base64) {
		if (lookup == null) {
			lookup = new byte[256];
			for (byte i = 0; i < chars.length; i++) {
				int c = chars[i];
				lookup[c] = i;
			}
		}
		base64 = base64.replaceAll("-", "+").replaceAll("_", "/").replaceAll("\n", "");
		int bufferLength = base64LengthNNL(base64);
		if (bufferLength == 0) return bytes0;
		int len = base64.length();
		int p = 0, encoded1, encoded2, encoded3, encoded4;
		byte[] bytes = new byte[bufferLength];

		for (int i = 0; i < len; i+=4) {
			char c1 = base64.charAt(i);
			char c2 = base64.charAt(i+1);
			if (c1 > 255 || c2 > 255) return bytes0; // pas un base64
			encoded1 = lookup[c1];
			encoded2 = lookup[c2];
			bytes[p++] = (byte)((encoded1 << 2) | (encoded2 >> 4));
			if (i + 2 != len) {
				char c3 = base64.charAt(i+2);
				if (c3 > 255) return bytes0; // pas un base64
				encoded3 = lookup[c3];
				bytes[p++] = (byte)(((encoded2 & 15) << 4) | (encoded3 >> 2));
				if (i + 3 != len) {
					char c4 = base64.charAt(i+3);
					if (c4 > 255) return bytes0; // pas un base64
					encoded4 = lookup[c4];
					bytes[p++] = (byte)(((encoded3 & 3) << 6) | (encoded4 & 63));
				}
			}
		}

		return bytes;
	}

	public static String bytesToBase64(byte[] bytes){
		return bytesToBase64(bytes, false);
	}
	
	public static String bytesToBase64(byte[] bytes, boolean special){
		if (bytes == null || bytes.length == 0) return "";
		int len = bytes.length;
		char[] cx = special ? chars2 : chars;
		StringBuffer sb = new StringBuffer();
		int j0, j1, j2;
		for (int i = 0; i < len; i+=3) {
			j0 = bytes[i]; if (j0 < 0) j0 += 256;
			sb.append(cx[j0 >> 2]);
			if (i + 1 != len){
				j1 = bytes[i+1]; if (j1 < 0) j1 += 256;
				sb.append(cx[((j0 << 4 ) & 0x30) | (j1 >> 4) ]);
				if (i + 2 != len){
					j2 = bytes[i+2]; if (j2 < 0) j2 += 256;
					sb.append(cx[((j1 << 2) & 0x3c) | (j2 >> 6)]);
					sb.append(cx[j2 & 0x3F]);
				} else {
					sb.append(cx[((j1 << 2) & 0x3c)]);
					if (!special) sb.append('=');
				}
			} else {
				sb.append(cx[((j0 << 4 ) & 0x30)]);
				if (!special) sb.append("==");
			}
		}
		return sb.toString();
	}
	
	
	/********************************************************************************/

	public static class JWK {
		String alg;
		String e;
		String[] ops;
		String kty;
		String n;
		
		BigInteger e() {
			return new BigInteger(1, Crypto.base64ToBytes(e));
		};
		BigInteger n() {
			return new BigInteger(1, Crypto.base64ToBytes(n));
		};
	}

//	private static KeyPairGenerator keyGen;
	private static KeyFactory keyFactory;
//	private static Signature sig;
	private static MessageDigest digestSha1;
	private static MessageDigest digestSha256;
	
	static void startup() throws Exception {
//		keyGen = KeyPairGenerator.getInstance("RSA");
//		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
//		keyGen.initialize(2048, random);
//		sig = Signature.getInstance("SHA256withRSA");
		keyFactory = KeyFactory.getInstance("RSA");
		digestSha1 = MessageDigest.getInstance("SHA-1");
		digestSha256 = MessageDigest.getInstance("SHA-256");
		// OAEPWithSHA1AndMGF1Padding : seul qui accepte de matcher avec Web Cryptography
		Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding"); // pour tester l'exception
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
		byte[] b1 = Crypto.base64ToBytes(x);
		byte[] b2 = Crypto.SHA256(b1);
		return Crypto.bytesToBase64(b2);
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
//	
//	public static byte[] sign(byte[] ssaPkcs8, byte[] texte) throws Exception {
//		synchronized (sig) {
//			RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(ssaPkcs8));
//			sig.initSign(privateKey);
//			sig.update(texte);
//			return sig.sign();
//		}
//	}
//	
//	public static boolean verify(byte[] ssaSpki, byte[] texte, byte[] signature) throws Exception {
//		synchronized (sig) {
//			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(ssaSpki));
//			sig.initVerify(publicKey);
//			sig.update(texte);
//			return sig.verify(signature);
//		}
//	}
	
	public static byte[] encrypt(byte[] spki, byte[] texte) throws Exception {
		Cipher cipher = cipher();
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(spki));
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(texte);
	}

	public static byte[] encrypt(String jwkJson, byte[] texte) throws Exception {
		Cipher cipher = cipher();
		JWK jwk = JSON.fromJson(jwkJson, JWK.class);
		RSAPublicKeySpec spec = new RSAPublicKeySpec(jwk.n(), jwk.e());
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(spec);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(texte);
	}

	public static byte[] decrypt(byte[] pkcs8, byte[] encrypted) throws Exception {
		Cipher cipher = cipher();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(encrypted);
	}

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
		String s1 = bytesToBase64(u8,false);
		System.out.println(s1);
		System.out.println(bytes2String(base64ToBytes(s1)));
		String s2 = bytesToBase64(u8,true);
		System.out.println(s2);
		System.out.println(bytes2String(base64ToBytes(s2)));

		byte[] u8b = {0,2,-1,32};
//		System.out.println(bytesToBase64Y(u8b,false));
		String s1b = bytesToBase64(u8b,false);
		System.out.println(s1b);
		System.out.println(bytes2String(base64ToBytes(s1b)));
		String s2b = bytesToBase64(u8b,true);
		System.out.println(s2b);
		System.out.println(bytes2String(base64ToBytes(s2b)));

		byte[] u8c = {0,2,-1,32,48};
//		System.out.println(bytesToBase64Y(u8c,false));
		String s1c = bytesToBase64(u8c,false);
		System.out.println(s1c);
		System.out.println(bytes2String(base64ToBytes(s1c)));
		String s2c = bytesToBase64(u8c,true);
		System.out.println(s2c);
		System.out.println(bytes2String(base64ToBytes(s2c)));
		
		byte[] u8d = new byte[10000];
		for(int i = 0; i < u8d.length; i++) u8d[i] = (byte)(i % 92);
		long t0 = System.currentTimeMillis();
//		for(int i = 0; i < 10000; i++)
//			bytesToBase64Y(u8c,false);
		long t1 =  System.currentTimeMillis();
		System.out.println((t1 - t0) + "ms");
		for(int i = 0; i < 10000; i++)
			bytesToBase64(u8c,false);
		long t2 =  System.currentTimeMillis();
		System.out.println((t2 - t1) + "ms");

		Properties props = new Properties();
		props.load(new InputStreamReader(new FileInputStream("doc/test1.properties"), "UTF-8"));
		
		byte[] texte2 = props.getProperty("obj").getBytes("UTF-8");
//		byte[] texte = base64ToBytes(props.getProperty("texte"));
		
		String sign1 = props.getProperty("sign1");
//		byte[] signature1 = base64ToBytes(props.getProperty("sign1"));
		System.out.println("sign1=" + sign1);

		byte[] encrypted1 = base64ToBytes(props.getProperty("encrypted"));
			
//		byte[] ssaPkcs8 = base64ToBytes(props.getProperty("ssaPkcs8"));
//		byte[] signature2 = sign(ssaPkcs8, texte);
//		String sign2 = bytesToBase64(signature2);
//		System.out.println("sign2=" + sign2);
//		
//		byte[] ssaSpki = base64ToBytes(props.getProperty("ssaSpki"));
//		boolean b = verify(ssaSpki, texte, signature1);
//		System.out.println(b);
					
		byte[] spki = base64ToBytes(props.getProperty("spki"));
		byte[] encrypted = encrypt(spki, texte2);
		String encrypted2 = bytesToBase64(encrypted);
		System.out.println("encrypted=" + encrypted2);

		String pkcs8s = props.getProperty("pkcs8");
		byte[] pkcs8 = base64ToBytes(pkcs8s);
		byte[] decrypted = decrypt(pkcs8, encrypted1);
		String decrypted2 = new String(decrypted, "UTF-8");
		
		System.out.println("decrypted=" + decrypted2);
		
		System.out.println("sha1  Hex=" + bytesToHex(SHA1(decrypted)));
		System.out.println("sha256Hex=" + bytesToHex(SHA256(decrypted)));		
	}
	
	public static void main(String[] args){
		try {
			startup();
			// test1();
			/*
			String json = Servlet.bytes2string(Servlet.bytesFromStream(new FileInputStream ("data/pub.jwk")));
			byte[] crypted = Crypto.encrypt(json, "toto est beau".getBytes("UTF-8"));
			*/
//			String s1 = Servlet.bytes2string(Servlet.bytesFromStream(new FileInputStream ("data/public.pem")));
//			String s2 = s1.replace("-----BEGIN PUBLIC KEY-----", "")
//					.replace("-----END PUBLIC KEY-----", "").replaceAll("\n", "");
//			byte[] b1 = Crypto.base64ToBytes(s2);
//			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(b1));
//			
//			String s3 = Servlet.bytes2string(Servlet.bytesFromStream(new FileInputStream ("data/pkcs8.pem")));
//			String s4 = s3.replace("-----BEGIN PRIVATE KEY-----", "")
//					.replace("-----END PRIVATE KEY-----", "").replaceAll("\n", "");
//			byte[] b2 = Crypto.base64ToBytes(s4);
			
//			cipher.init(Cipher.ENCRYPT_MODE, publicKey2);
//			byte[] crypted = cipher.doFinal("toto est beau".getBytes("UTF-8"));

			/*
			String res1 = bytesToBase64(crypted);
			FileOutputStream os = new FileOutputStream("data/crypted.txt");
			os.write(res1.getBytes("UTF-8"));
			os.close();
			*/
//			byte[] b3 = Servlet.bytesFromStream(new FileInputStream ("data/private.der"));
//			String s5 = Crypto.bytesToBase64(b3);
//			System.out.println(s5.equals(s4) ? "yes" : "no");
//			
//			RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(b2));
//			cipher.init(Cipher.DECRYPT_MODE, privateKey);
//			byte[] decrypted = cipher.doFinal(crypted);
//			String res2 = new String(decrypted, "UTF-8");
//			System.out.println(res2);
			int n = "tititatatoto".hashCode();
			System.out.println(n + " " + hashOf("tititatatoto"));
		} catch (Throwable t){
			t.printStackTrace();
		}
	}
	
}
