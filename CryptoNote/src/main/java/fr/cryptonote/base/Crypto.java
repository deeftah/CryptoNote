package fr.cryptonote.base;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
	private static KeyPairGenerator keyGen;
	private static KeyFactory keyFactory;
	private static SecureRandom random;
	
	static {
		try {
			// fixKeyLength(); // java 8 < 160 (environ)
			Security.setProperty("crypto.policy", "unlimited"); // java > (environ)
			keyGen = KeyPairGenerator.getInstance("RSA");
			random = SecureRandom.getInstance("SHA1PRNG", "SUN");
			keyGen.initialize(2048, random);
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (Exception e) {}
	}	
	
	/*** AES *******************************************************************/ 
	public static class AES {
		private static final byte[] ivb = {101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116};
		
		public static byte[] iv() { return ivb; }
		
        private Cipher cipher;
        private SecretKeySpec skeySpec;
        byte[] iv;
        
		public AES(byte[] key) throws Exception{
			if (key.length != 32) key = Util.SHA256(key);
            skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		}
		
		public AES(String key) throws Exception { this(Util.b64uToBytes(key)); }
		
		public byte[] encrypt(byte[] data, byte[] ivSpec) throws Exception {
			if (ivSpec != null && ivSpec.length == 16)
				iv = ivSpec; 
			else {
				iv = new byte[16];
				byte[] rnd = Util.random(1);
				for(int i = 0; i < 16; i++) iv[i] = rnd[i % 4];
			}
	        IvParameterSpec ivx = new IvParameterSpec(iv, 0, 16);
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivx);
            byte[] res = cipher.doFinal(data);
            if (ivSpec != null) return res;
            byte[] r = new byte[res.length + 4];
            System.arraycopy(res, 0, r, 4, res.length);
            System.arraycopy(iv, 0, r, 0, 4);
            return r;
		}

		public byte[] decrypt(byte[] data, byte[] ivSpec) throws Exception {
			byte[] cr;
			if (ivSpec != null && ivSpec.length == 16) {
				iv = ivSpec;
				cr = data;
			} else {
				iv = new byte[16];
				for(int i = 0; i < 16; i++) iv[i] = data[i % 4];
				cr = new byte[data.length - 4];
				System.arraycopy(data, 4, cr, 0, data.length - 4);
			}
	        IvParameterSpec ivx = new IvParameterSpec(iv, 0, 16);
	        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivx);
            return cipher.doFinal(cr);
		}

	}
	
	/*** RSA KeyPair *******************************************************************/ 
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
		return Util.b64ToBytes(sb.toString());
	}
	
	public static String bytesToPem(byte[] bytes, boolean isPub) {
		String s = Util.bytesToB64(bytes);
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
		
	/*** RSA Encrypter *******************************************************************/ 
	public static class Encrypter {
		private Cipher cipher;
		// OAEPWithSHA1AndMGF1Padding : seul qui accepte de matcher avec Web Cryptography
		
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

	/*** RSA Decrypter *******************************************************************/ 
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

	/*** RSA Signer *******************************************************************/ 
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
	
	/*** RSA Verifier *******************************************************************/ 
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
			
			byte[] crypted;
			byte[] decrypted;
			
			String s;
			String privkey;
			String pubkey;
			byte[] toto = Util.toUTF8("toto est beau");
			byte[] titi = Util.toUTF8("titi est beau");
			
			String aeskey = Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/aeskey.txt")));
			String aescrypted = Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/aescrypted.txt")));
			byte[] binkey = Util.b64uToBytes(aeskey);
			byte[] bincrypted = Util.b64ToBytes(aescrypted);
			Util.bytesToStream(binkey, new FileOutputStream("data/aeskey.bin"));
			Util.bytesToStream(bincrypted, new FileOutputStream("data/aescrypted.bin"));
			System.out.println(binkey.length);
			
			AES aes = new AES(aeskey);
			crypted = aes.encrypt(toto, null);
			System.out.println(Util.bytesToB64(crypted));
			decrypted = aes.decrypt(crypted, null);
			System.out.println(new String(decrypted, "UTF-8"));
			
			decrypted = aes.decrypt(bincrypted, AES.iv());
			System.out.println(new String(decrypted, "UTF-8"));
			
			pubkey =  Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/public.pem")));
			privkey = Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/private.pem")));

			Encrypter ec = new Encrypter(pubkey);
			crypted = ec.encrypt(toto);
			System.out.println(Util.bytesToB64(crypted));
			Util.stringToStream(Util.bytesToB64(crypted), new FileOutputStream("data/crypted2.txt"));

			Decrypter dc = new Decrypter(privkey);
			decrypted = dc.decrypt(crypted);
			System.out.println(new String(decrypted, "UTF-8"));

			s =  Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/crypted.txt")));
			byte[] crypted2 = Util.b64uToBytes(s);
			Util.bytesToStream(crypted2, new FileOutputStream("data/crypted2.bin"));
			decrypted = dc.decrypt(crypted2);
			System.out.println(new String(decrypted, "UTF-8"));
			
			pubkey =  Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/publics.pem")));
			privkey = Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/privates.pem")));
			s =  Util.fromUTF8(Util.bytesFromStream(new FileInputStream ("data/sign.txt")));
			byte[] sign = Util.b64uToBytes(s);
			Verifier vf = new Verifier(pubkey);
			boolean v = vf.verify(sign, toto);
			System.out.println(v);
			v = vf.verify(sign, titi);
			System.out.println(v);

			crypted = new Signer(privkey).sign(toto);
			System.out.println(Util.bytesToB64u(crypted));
			Util.stringToStream(Util.bytesToB64u(crypted), new FileOutputStream("data/sign2.txt"));

			KP kp = new KP();
			Encrypter ec2 = new Encrypter(kp.pub);
			crypted = ec2.encrypt(toto);
			System.out.println(Util.bytesToB64(crypted));

			Decrypter dc2 = new Decrypter(kp.priv);
			decrypted = dc2.decrypt(crypted);
			System.out.println(new String(decrypted, "UTF-8"));

			crypted = new Signer(kp.priv).sign(toto);
			System.out.println(Util.bytesToB64(crypted));
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
