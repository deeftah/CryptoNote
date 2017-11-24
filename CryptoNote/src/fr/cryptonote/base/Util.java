package fr.cryptonote.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;

public class Util {
	public static final Logger log = Logger.getLogger("fr.cryptonote");

	private static final boolean[] reserved = new boolean[123];
	
	static {
		for(int i = 0; i < reserved.length; i++) reserved[i] = true;
		reserved[37] = false; // %
		reserved[42] = false; // *
		reserved[43] = false; // +
		reserved[45] = false; // -
		reserved[46] = false; // .
		reserved[95] = false; // _
		for(int i = 48; i <= 57; i++) reserved[i] = false; // 0-9
		for(int i = 65; i <= 90; i++) reserved[i] = false; // A-Z
		for(int i = 97; i <= 122; i++) reserved[i] = false; // a-z
	}
	
    public static String[] as(String... res){
    	if (res == null || res.length == 0) return new String[0];
    	String[] r = new String[res.length];
    	for(int i = 0; i < res.length; i++) r[i] = res[i];
    	return r;
    }
	
	public static boolean eq(String a, String b) {
		return a != null ? a.equals(b) : b == null;
	}
	
	public static String min(String... s) {
		if (s == null || s.length == 0) return null;		
		String x = null;
		for(String y : s)
			if (y != null && (x == null || y.compareTo(x) > 1)) x = y;
		return x;
	}
	
	/**
	 * Indique si un caractère fait partie du jeu "unreserved" pouvant figurer dans une URL 
	 * sans encocage ou contient + ou % correspondant à une uRL encodée
	 * @param c
	 * @return
	 */
	public static boolean isUnreserved(Character c){ return c >= 37 && c <= 122 && !reserved[c]; }
	
	public static boolean isValidId(String s, int len){
		if (s == null || s.length() > len)
			return false;
		for(int i = 0; i < s.length(); i++)
			if (!isUnreserved(s.charAt(i))) return false;
		return true;
	}

	public static String urlEnc(String s){
		if (s == null || s.length() == 0) return s;
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			return s;
		}
	}
	
	public static String urlDec(String s){
		if (s == null || s.length() == 0) return s;
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (Exception e) {
			return s;
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

	public static byte[] gzip(String text){
		if (text == null || text.length() == 0) return null;
		try {
			return gzip(text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return new byte[0];
		}
	}

	
	public static byte[] gzip(byte[] bytes){
		if (bytes == null || bytes.length == 0) return null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			GZIPOutputStream zos = new GZIPOutputStream(bos);
			zos.write(bytes);
			zos.close();
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
		if (bytes == null || bytes.length == 0) return null;
		try {
			GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int l;
			while((l = gzis.read(b)) >= 0)
				bos.write(b, 0, l);
			gzis.close();
			return bos.toByteArray();
		} catch (IOException e) {
			return bytes;
		}
	}

	public static String[] b64Array(String s){
		if (s == null) return null;
		String[] sx = s.split(" ");
		for(String x : sx)
			if (!Crypto.isBase64NN(x)) return null;
		return sx;
	}
	
	public static boolean equals(HashSet<String> h1, HashSet<String> h2) {
		for (String s : h1) if (!h2.contains(s)) return false;
		for (String s : h2) if (!h1.contains(s)) return false;
		return true;
	}
	
	public static void streamBytes(OutputStream os, byte[] bytes) throws IOException{
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		byte[] buf = new byte[4096];
		int l = 0;
		while((l = bis.read(buf)) > 0)
			os.write(buf, 0, l);
		os.flush();
		bis.close();
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

	public static Class<?> hasClass(String name, Class<?> type) throws ServletException{
		try {
			Class<?> c = Class.forName(name);
			if (c != type && !type.isAssignableFrom(c)) 
				throw new Exception("x");
			return c;
		} catch (Exception e) {
			String msg1 = AConfig._format("XSERVLETCLASS", name, type.getCanonicalName());
			log.log(Level.SEVERE, msg1);
			throw new ServletException(msg1, e);
		}
	}

	public static byte[] toUTF8(String s){
		if (s == null || s.length() == 0) return null;
		try {
			return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}	
	}

	public static String fromUTF8(byte[] s){
		if (s == null || s.length == 0) return null;
		try {
			return new String(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}	
	}

	public static String stack(Throwable t) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(bos));
		return bos.toString();
	}

	public static void main(String[] args){
		String s = "toto titi tutu";
		String[] sx = Util.b64Array(s);
		System.out.println(sx.length);	
	}

}
