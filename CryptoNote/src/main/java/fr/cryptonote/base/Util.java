package fr.cryptonote.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import fr.cryptonote.base.BConfig.Nsqm;

public class Util {
	public static final Logger log = Logger.getLogger("fr.cryptonote");

	/** Digest ****************************************************************/
	private static MessageDigest MD256;
	private static MessageDigest MD1;
	
	static {
		try { MD256 = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException e) { }
		try { MD1 = MessageDigest.getInstance("SHA-1"); } catch (NoSuchAlgorithmException e) { }
	}
	
	/**
	 * Digest SHA-1 d'un byte[] retourné en byte[]
	 * @param x
	 * @return
	 * @throws Exception
	 */
	public static byte[] SHA1(byte[] x) {
		if (x == null) return null;
		synchronized (MD1) {
			MD1.reset();
			MD1.update(x);
		    return MD1.digest();
		}
	}
	
	/**
	 * Digest SHA-256 d'un byte[] retourné en byte[]
	 * @param x
	 * @return
	 * @throws Exception
	 */
	public static byte[] SHA256(byte[] x) {
		if (x == null) return null;
		synchronized (MD256) {
			MD256.reset();
			MD256.update(x);
		    return MD256.digest();
		}
	}
	
	public static String randomB64(int n8) { return bytesToB64u(SHA256(random(n8))); }
	
	/**
	 * Return un byte[] random de 4 * n4 bytes.
	 * @param n4 entre 1 et 8.
	 * @return
	 */
	public static byte[] random(int n4) {
		int n = n4 < 1 ? 1 : (n4 > 8 ? 8 : n4);
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		ByteBuffer bf = ByteBuffer.allocate(n * 4);
		try { for(int i = 0; i < n; i++)	bf.putInt(tlr.nextInt(1, Integer.MAX_VALUE)); } catch (Exception e) { }
		return bf.array();
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

	public static final String bytes2JSON(byte[] b){
		if (b == null || b.length == 0) return "[]";
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < b.length; i++)
			sb.append(i == 0 ? '[' : ',').append(b[i]);
		return sb.append(']').toString();
	}

	public static String bytesToB64(byte[] b){ return Base64.getEncoder().encodeToString(b);}
	
	public static byte[] b64ToBytes(String b64){ return Base64.getDecoder().decode(b64); }

	public static String bytesToB64u(byte[] b){ return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }

	public static byte[] b64uToBytes(String b64){ return Base64.getUrlDecoder().decode(b64); }

	/******************************************************************************/
	public static byte[] toUTF8(String s){
		try { return s == null ? null : s.getBytes("UTF-8"); } catch (UnsupportedEncodingException e) { return null; }	
	}

	public static String fromUTF8(byte[] s){
		try { return s == null ? null : new String(s, "UTF-8"); } catch (UnsupportedEncodingException e) { return null; }	
	}

	/** Reflect ****************************************************************/
	public static Class<?> hasClass(String name, Class<?> type) {
		try {
			Class<?> c = Class.forName(name);
			if (c != type && !type.isAssignableFrom(c)) return null;
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static HashMap<String,Field> getAllField(Class<?> clazz, Class<?> rootClazz, Class annot) {
		HashMap<String,Field> af = new HashMap<String,Field>();
		Class<?> c = clazz;
		while (true) {
			if (c == Object.class) break;
			Field[] fs = c.getDeclaredFields();
			if (fs != null)
				for (Field f : fs) {
					if (f.getAnnotation(annot) != null) {
						String n = f.getName();
						if (af.get(n) == null)
							af.put(n, f);
					}
				}
			if (c == rootClazz) break;
			c = (Class<?>) c.getGenericSuperclass();
			if (c == null) break;
		}
		return af;
	}

	public static Field getField(Class<?> clazz, Class<?> rootClazz, String name) {
		Class<?> c = clazz;
		while (true) {
			if (c == Object.class) break;
			Field f = null;
			try {
				f = c.getDeclaredField(name);
			} catch(Exception e){
				e.getMessage();
			}
			if (f != null) return f;
			if (c == rootClazz) break;
			c = (Class<?>) c.getGenericSuperclass();
			if (c == null) break;
		}
		return null;
	}
	
	/******************************************************************************/
	public static byte[] bytesFromStream(InputStream is) {
		try {
			if (is == null) return null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int l = 0;
			while((l = is.read(buf)) > 0) bos.write(buf, 0, l);
			is.close();
			bos.flush();
			byte[] bytes = bos.toByteArray();
			bos.close();
			return bytes;
		} catch (Exception e) { return null; }
	}

	public static void bytesToStream(byte[] bytes, OutputStream os) throws IOException {
		int i = 0;
		int l = bytes.length;
		while (i < l) {
			int lx = i + 4096 < l ? 4096 : l - i;
			os.write(bytes, i, lx);
			i += lx;
		}
		os.close();
	}

	public static void stringToStream(String s, OutputStream os) throws IOException { bytesToStream(toUTF8(s), os);}

	/******************************************************************************/
	public static byte[] gzip(String text) { return gzip(toUTF8(text)); }

	public static byte[] gzip(byte[] bytes){
		if (bytes == null) return null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			bytesToStream(bytes, new GZIPOutputStream(bos));
			return bos.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	public static String ungzipText(byte[] bytes){
		if (bytes == null || bytes.length == 0) return null;
		try {
			return new String(ungzip(bytes), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static byte[] ungzip(byte[] bytes){
		if (bytes == null) return null;
		try {
			GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int l;
			while((l = gzis.read(b)) >= 0) bos.write(b, 0, l);
			gzis.close();
			return bos.toByteArray();
		} catch (IOException e) {
			return bytes;
		}
	}

	/******************************************************************************/	
	public static boolean equals(HashSet<String> h1, HashSet<String> h2) {
		for (String s : h1) if (!h2.contains(s)) return false;
		for (String s : h2) if (!h1.contains(s)) return false;
		return true;
	}
	
	public static String[] as(String... res){
		if (res == null || res.length == 0) return new String[0];
		String[] r = new String[res.length];
		for(int i = 0; i < res.length; i++) r[i] = res[i];
		return r;
	}

	public static boolean eq(String a, String b) {	return a != null ? a.equals(b) : b == null;}

	public static String min(String... s) {
		if (s == null || s.length == 0) return null;		
		String x = null;
		for(String y : s)
			if (y != null && (x == null || y.compareTo(x) > 1)) x = y;
		return x;
	}

	
	/******************************************************************************/
	public static String stack(Throwable t) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(bos));
		return bos.toString();
	}
	
	/******************************************************************************/
	public static String urlEnc(String s){
		if (s == null || s.length() == 0) return "";
		try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return ""; }
	}

	public static class PostResponse {
		public int status;
		public String text;
		public PostResponse() {}
		public PostResponse(int status, String text) {this.status = status; this.text = text;}
	}
	
	/**
	 * Poste une requête au serveur indiqué par ns avec les arguments cités et ne retourne qu'un statut (pas de résultat).
	 * @param ns désigne soit un namespace soit un queue manager
	 * @param endUrl dans l'URL ce qui suit http://.../cp/ns/ (ou http://.../ns/). Pas de / en tête
	 * @param args les arguments String à passer qui seront URL encodés
	 * @return 0:succès (200) 1:réponse négative (pas 200) 2:exception technique, donc inconnu sur la réception / le traitement
	 */
	public static PostResponse postSrv(String ns, String endUrl, HashMap<String,String> args) {
		PostResponse pr = new PostResponse();
		try {
			StringBuffer sb = new StringBuffer();
			Nsqm nsqm = BConfig.nsqm(ns, false);
			sb.append("key=").append(urlEnc(nsqm.pwd()));
			if (args != null) 
				for(String k : args.keySet()) 
					sb.append("&").append(k).append("=").append(urlEnc(args.get(k)));
			String u = nsqm.url() + (endUrl != null ? urlEnc(endUrl) : "");
		    URL url = new URL(u);
		    String query = sb.toString();
		    HttpURLConnection con = url.getProtocol().equals("https") ?
		    		(HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
		    con.setRequestMethod("POST");
		    con.setRequestProperty("Content-length", String.valueOf(query.length())); 
		    con.setRequestProperty("Content-Type","application/x-www-form-urlencoded"); 
		    con.setDoOutput(true); 
		    DataOutputStream output = new DataOutputStream(con.getOutputStream()); 
		    output.writeBytes(query);
		    output.close();    
		    pr.status = con.getResponseCode();
	    	InputStream response = null;
	    	if (pr.status != 200)
	    		response = con.getErrorStream();
		    if (response == null)
		    	response = con.getInputStream();
		    if (response != null)
		    	pr.text = fromUTF8(bytesFromStream(response));
		} catch (Exception e) {
			pr.status = 999;
			pr.text = e.getMessage();
		}
		return pr;
	}

}
