package fr.cryptonote.base;

import java.util.HashMap;

import javax.servlet.ServletException;

public class MimeType {
	public static final String defaultMime = "application/octet-stream";
	
	@SuppressWarnings("serial")
	private static class Extensions extends HashMap<String,HashMap<String,String>>{};
	
	private static class Ext {
		String ext;
		String mime;
		boolean isText;
		private Ext(String ext, String mime, boolean isText){
			this.ext = ext; this.mime = mime; this.isText = isText;
		}
	}
	
	private static HashMap<String,Ext> byExt = new HashMap<String,Ext>();
	private static HashMap<String,Ext> byMime = new HashMap<String,Ext>();
		
	static void init() throws ServletException {
		byte[] r = Servlet.getRawResource(BConfig.MT);
		if (r != null)
			try {
				Extensions extensions = JSON.fromJson(Util.fromUTF8(r), Extensions.class);
				HashMap<String,String> t = extensions.get("text");
				if (t != null){
					for(String ext : t.keySet()){
						String m = t.get(ext);
						Ext e = new Ext(ext, m, true);
						byExt.put(ext, e);
						Ext e2 = byMime.get(m);
						if (e2 == null) byMime.put(m, e);
					}
				}
				t = extensions.get("bin");
				if (t != null){
					for(String ext : t.keySet()){
						String m = t.get(ext);
						Ext e = new Ext(ext, m, false);
						byExt.put(ext, e);
						Ext e2 = byMime.get(m);
						if (e2 == null) byMime.put(m, e);
					}
				}
			} catch (Exception ex){
				throw BConfig.exc(ex, BConfig._format(0, "XRESSOURCEJSON", BConfig.MT, ex.getMessage()));
			}
		else
			throw BConfig.exc(null, BConfig._format(0, "XRESSOURCEABSENTE", BConfig.MT));
	}
	
	public static String extOf(String mime) {
		Ext x = null;
		if (mime != null && mime.length() != 0) x = byMime.get(mime);
		return x == null ? null : x.ext;
	}

	public static boolean isText(String mime) {
		Ext x = null;
		if (mime != null && mime.length() != 0) x = byMime.get(mime);
		return x == null ? false : x.isText;
	}

	public static String mimeOf(String ext) {
		if (ext == null || ext.length() == 0) return null;
		int i = ext.lastIndexOf('.');
		if (i != -1 && i < ext.length() - 1) 
			ext = ext.substring(i + 1);
		if (ext == null || ext.length() == 0) return null;
		Ext x = byExt.get(ext);
		return x == null ? null : x.mime;
	}

}
