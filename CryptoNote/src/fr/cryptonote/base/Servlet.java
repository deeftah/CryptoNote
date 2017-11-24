package fr.cryptonote.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
		
	private static boolean done = false;
	private static String contextPath;
	private static ServletContext servletContext;
	private static Servlet servlet;
	
	public static String contextPath() { return contextPath; }

	private static Exception initException;
	private static void check() throws ServletException {
		if (initException != null) {
			Util.log.severe(initException.getMessage());
			if (initException instanceof ServletException) throw (ServletException)initException;
			throw new ServletException(initException);
		}
	}

	@Override public void init(ServletConfig servletConfig) throws ServletException {
		if (done) return;
		servlet = this;
		servletContext = servletConfig.getServletContext();
		String s  = servletContext.getContextPath();
		contextPath = s == null || s.length() <= 1 ? "" : s;

	}
	
	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		PrintWriter out = resp.getWriter();
	    out.println("Hello, world!");
	    out.close();
	}

	@Override public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

	}

	/********************************************************************************/
	private static Resource emptyResource = new Resource((byte[])null, null);
	private static Hashtable<String, Resource> resources = new Hashtable<String, Resource>();

	public static class Resource {
		public byte[] bytes;
		public String mime;
		public String sha;
		
		public Resource(byte[] bytes, String mime) { this.bytes = bytes; this.mime = mime;  sha = Crypto.bytesToBase64(Crypto.SHA256(bytes)); }
		public Resource(String text, String mime){ this(Util.toUTF8(text), mime); }
		public String toString(){ String s = Util.fromUTF8(bytes); return s == null ? "" : s; }
	}

	public static Resource getResource(String name){
		if (name == null || name.length() == 0) return null;
		Resource r = resources.get(name);
		if (r != null)
			return r == emptyResource ? null : r;
		String mime = name.endsWith(".json") ? "application/json" : MimeType.mimeOf(name);
		if (mime == null) mime = MimeType.defaultMime;
		byte[] b = Util.bytesFromStream(servletContext.getResourceAsStream(name));
		if (b == null){
			resources.put(name, emptyResource);
			return null;
		}
		r = new Resource(b, mime);
		resources.put(name, r);
		return r;
	}

	/********************************************************************************/
}
