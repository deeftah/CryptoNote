package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import fr.cryptonote.base.BConfig.Nsqm;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
		
	private static final String[] ext2 = {".cloud", ".app", ".local", ".sync", ".local2"};	

	private static Boolean done = false;
	private static String contextPath;
	private static ServletContext servletContext;
	private static String[] var;
	private static String[] zres;

	public static String contextPath() { return contextPath; }
	public static boolean hasCP() { return contextPath.length() != 0; }
	public static String[] var() { return var; }
	public static String[] zres() { return zres; }
	public static boolean isZres(String name) { for (String n : zres) if (n.equals(name)) return true; return false; }

	/********************************************************************************/
	@Override public void init(ServletConfig servletConfig) throws ServletException {
		synchronized (done) {
			if (done) return;
			servletContext = servletConfig.getServletContext();
			String s  = servletContext.getContextPath();
			contextPath = s == null || s.length() <= 1 ? "" : s;
			
			BConfig.startup();			
			
			ArrayList<String> varx = new ArrayList<String>();
			lpath(new BConfig.ResFilter(), varx, "/var/");
			var = varx.toArray(new String[varx.size()]);
			Arrays.sort(var);
			ArrayList<String> zresx = new ArrayList<String>();
			for(String x : var) if (x.startsWith("z/"))	zresx.add(x.substring(2));
			zres = zresx.toArray(new String[zresx.size()]);
			
			done = true;
		}

	}
	
	@Override public void destroy() {
		try {
			QueueManager.stopQM();
			// TODO : fermer toutes les connexions / datasources ?;
		} catch (Exception e) {	}
	}

	/********************************************************************************/	
	private final class ReqCtx {
		HttpServletRequest req;
		HttpServletResponse resp;
		ExecContext exec;
		String origUri;
		String uri;
		String build;
		Nsqm nsqm;
		boolean fini = false;
		
		ReqCtx(HttpServletRequest req, HttpServletResponse resp) throws IOException { 
			this.req = req; 
			this.resp = resp; 
			origUri = req.getRequestURI().substring(contextPath.length() + 1);
			build = ""+ BConfig.build();
			if ("ping".equals(origUri)) {
				String x = "\"{t\":" + Stamp.fromNow(0).stamp() + ", \"b\":" + build + "}";
				sendText(0, x, resp);
				fini = true;
			}
			String shortcut = BConfig.shortcut(uri);
			if (shortcut != null) origUri = shortcut;
			exec = new ExecContext().setLang(req.getHeader("lang"));
		}
		
		void checkNsqm() throws IOException {
			int i = origUri.indexOf('/');
			if (i == -1 || i == origUri.length() - 1) {
				sendText(500, BConfig.format("500URLMF1" + (hasCP() ? "cp" : ""), origUri), resp);
				fini = true;
				return;
			}
			String x = origUri.substring(0, i);
			nsqm = BConfig.nsqm(x, true);
			if (nsqm == null) {
				sendText(500, BConfig.format("500URLMF2" + (hasCP() ? "cp" : ""), origUri, x), resp);
				return;			
			}
			if (nsqm.isQM) {
				if (nsqm.code.equals(BConfig.QM())) {
					sendText(500, BConfig.format("500HQM", nsqm.code), resp);	
					fini = true;
					return;
				}	
			} else {
				if (nsqm.off != 0) {
					sendText(500, BConfig.format("500OFF", ""+nsqm.off), resp);	
					fini = true;
					return;
				}
				build += "_" + nsqm.build;
			}
			exec.setNsqm(nsqm);
			
			uri = uri.substring(i + 1);
			if ("ping".equals(uri)) {
				String dbInfo;
				try {
					dbInfo = exec.dbProvider().dbInfo(null);
				} catch (AppException e) {
					dbInfo = BConfig.label("XDBINFO");			
				}
				StringBuffer sb = new StringBuffer();
				sb.append("\"{t\":").append("" + Stamp.fromNow(0).stamp()).append(", \"b\":").append(build).append(", \"off\":").append(nsqm.off)
				.append(", \"db\":").append(JSON.toJson(dbInfo)).append("}");
				sendText(0, sb.toString(), resp);
				fini = true;
				return;
			}
		}
		
		private void appcache() throws IOException {
			String p1 = contextPath + "/" + nsqm.code + "/";
			String p2 = p1 + "_" + build + "/";
			StringBuffer sb = new StringBuffer();
			sb.append("CACHE MANIFEST\n#").append(build).append("\nCACHE:\n");
			for(String s : var())
				sb.append(p2 + s + "\n");
			for(String s : BConfig.offlinepages())
				sb.append(p1).append(s).append(".local2\n").append(p1).append(s).append(".sync2\n");	
			sendRes(new Servlet.Resource(sb.toString().getBytes("UTF-8"), "text/cache-manifest"), req, resp);
			fini = true;
		}

		private void wsjs() throws IOException {
			Resource c = getResource("/var/sw.js");
			if (c == null) {
				sendText(404, BConfig.format("404SWJS"), resp);
				fini = true;
				return;
			}
			String p1 = "\"" + contextPath + "/" + nsqm.code + "/";
			String p2 = p1 + "_" + build + "/";

			StringBuffer sb = new StringBuffer();
			sb.append("'use strict';\n");
			sb.append("const CONTEXTPATH = \"").append(contextPath).append("\";\n");
			sb.append("const BUILD = \"").append(BConfig.build()).append("\";\n");
			sb.append("const NS = \"").append(nsqm.code).append("\";\n");
			sb.append("const NSBUILD = \"").append(nsqm.build).append("\";\n");
			sb.append("const RESSOURCES = [\n");
			for(String s : var())
				sb.append(p2 + s + "\",\n");
			for(String s : BConfig.offlinepages())
				sb.append(p1).append(s).append(".local\",\n").append(p1).append(s).append(".sync\",\n");	
			sb.append("];\n").append(c.toString());
			sendRes(new Servlet.Resource(sb.toString().getBytes("UTF-8"), " text/javascript"), req, resp);	
			fini = true;
		}
		
		void pages() throws IOException{
			String ext = null;	for(String s : ext2) if (uri.endsWith(s)) ext = s;
			if (ext == null) return;
			String page = uri.substring(0, uri.length() - ext.length());
			Resource res = NS.resource(nsqm.code, "custom.css");
			String rp = "/var/" + page + ".html";
			Resource r = getResource(rp);
			if (r == null){
				sendText(404, BConfig.format("404PAGE", rp), resp);
				fini = true;
				return;
			}
			String html = r.toString();
			int x = html.indexOf('\n');
			if (x == -1){
				sendText(404, BConfig.format("404PAGE1", rp), resp);
				fini = true;
				return;
			}
			html = html.substring(x);
			x = html.indexOf("</head>");
			if (x == -1){
				sendText(404, BConfig.format("404PAGE2", rp), resp);
				fini = true;
				return;
			}
			String head = html.substring(0, x);
			if (head.length() == 0){
				sendText(404, BConfig.format("404PAGE3", rp), resp);
				fini = true;
				return;
			}
			String body = html.substring(x);
			if (body.length() < 19){
				sendText(404, BConfig.format("404PAGE4", rp), resp);
				fini = true;
				return;
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("<!DOCTYPE html>");
			
			if (ext.endsWith("2")) 
				sb.append("<html manifest=\"").append(contextPath).append("/").append(nsqm.code).append("/x.appcache\">");
			else
				sb.append("<html>");
			
			sb.append("<head><base href=\"").append(contextPath).append("/").append(nsqm.code).append("/_").append(build).append("_").append(build).append("/\">\n");
			
			sb.append(head);
			sb.append("<style is=\"custom-style\">\n");
			r = getResource("/var/themes/default.css");
			if (r != null) sb.append(r.toString()).append("\n");
			r = getResource("/var/themes/" + nsqm.theme + ".css");
			if (r != null) sb.append(r.toString()).append("\n");
			
			if (res != null) 
				sb.append("\n\n/***** theme spécifique de ").append(nsqm.code).append(" ************/\n").append(res.toString()).append("\n");
			sb.append("</style>\n");

			sb.append("<script src=\"build.js\"></script>\n");

			sb.append("\n<script type=\"text/javascript\">\n");
			sb.append("'use strict';\n");
			sb.append("SRV.buildSrv = ").append(BConfig.build()).append(";\n");
			sb.append("SRV.contextpath = \"").append(contextPath).append("\";\n");
			
			sb.append("SRV.langs = [");
			String[] lx = BConfig.langs();
			for(int l = 0; l < lx.length; l++){
				if (l != 0) sb.append(",");
				sb.append("\"").append(lx[l]).append("\"");
			}
			sb.append("];\n");
			sb.append("APP.lang = \"").append(nsqm.lang()).append("\";\n");
			sb.append("SRV.zone = \"").append(BConfig.zone()).append("\";\n");
			sb.append("SRV.ns = \"").append(nsqm.code).append("\";\n");
			sb.append("SRV.nsbuild = ").append(nsqm.build).append(";\n");
			sb.append("SRV.nslabel = \"").append(nsqm.label).append("\";\n");
			String b = BConfig.byeAndBack();
			if (b != null)
				sb.append("SRV.byeAndBack = \"").append(b).append("\";\n");
			sb.append("</script>\n");
			
			sb.append("<script src=\"z/custom.js\"></script>\n");
			sb.append("<script src=\"final.js\"></script>\n");

			sb.append(body);
			
			sendRes(new Resource(sb.toString(), "text/html"), req, resp);
			fini = true;
		}

		void resource() throws IOException {
			if (uri.startsWith("var/"))
				uri = uri.substring(4);
			else {
				if (uri.charAt(0) == '_'){
					int i = uri.indexOf('/');
					if (i != -1)
						uri = uri.substring(i + 1);
					else {
						resp.sendError(404);
						return;
					}
				} else {
					resp.sendError(404);
					return;
				}
			}

			Resource res = null;
			if (uri.startsWith("z/"))
				res = NS.resource(nsqm.code, uri.substring(2));
			if (res == null)
				res = getResource("/var/" + uri);
			if (res == null)
				resp.sendError(404);
			sendRes(res, req, resp);
		}
	}
	
	/********************************************************************************/
	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ReqCtx r = new ReqCtx(req, resp);
		if (r.fini) return;
		r.checkNsqm();
		if (r.fini) return;
		
		if ("x.appache".equals(r.uri)) { r.appcache(); return; }
		if ("x.swjs".equals(r.uri)) { r.wsjs();	return;	}
		r.pages();
		if (r.fini) return;
		
		if (r.uri.startsWith("op/") || r.uri.startsWith("od/")){
			doGetPost(true, r);
			return;
		}
		
		r.resource();
	}

	/********************************************************************************/
	@Override public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ReqCtx r = new ReqCtx(req, resp);
		if (r.fini) return;
		r.checkNsqm();
		if (r.fini) return;
				
		if (r.uri.startsWith("op/") || r.uri.startsWith("od/")){
			doGetPost(true, r);
			return;
		}

		resp.sendError(404);
	}

	/********************************************************************************/
	private void sendText(int code, String text, HttpServletResponse resp) throws IOException {
		if (text == null) text = "";
		resp.setStatus(200);
		resp.setContentType("text/plain");
		resp.addHeader("build", "" + BConfig.build());
		if (code == 0 || code == 200) {
			try {
				byte[] bytes = text.getBytes("UTF-8");
				resp.setContentLength(bytes.length);
				resp.getOutputStream().write(bytes);		
			} catch (UnsupportedEncodingException e) {
				resp.sendError(500);
			}
		} else {
			resp.sendError(code, text);
		}
	}

	/********************************************************************************/
	private void doGetPost(boolean isGet, ReqCtx r) throws IOException, ServletException {
		// op/ od/
		InputData inp = null;
		Result result = null;
		
		try {
			String ct = r.req.getContentType();
			boolean mpfd = !isGet && ct != null && ct.toLowerCase().indexOf("multipart/form-data") > -1;
			inp = !mpfd ? getInputData(r.req) : postInputData(r.req);
			String b1 = r.req.getHeader("build");
			String b2 = "" + BConfig.build();
			if (b1 != null && !b2.equals(b1))
				throw new AppException("DBUILD", b1, b2);
			boolean isOP = r.uri.startsWith("op/");
			inp.uri = r.uri.substring(3);
			
			if (r.nsqm.isQM) {
				result = QueueManager.doTmRequest(r.exec, inp);
			} else {
				if (!isOP){ // od/
					inp.taskId = Document.Id.fromURL(inp.uri);
					inp.operationName = inp.taskId.docclass();
				} else { // op/ 
					String op = inp.args.get("op");
					inp.operationName = op != null ? op  : "Default";
				}
				result = r.exec.go(inp);
			}
			r.exec.closeAll();
			writeResp(r.resp, 200, result);
		} catch (Throwable t){
			AppException ex;
			if (t instanceof AppException)
				ex = (AppException)t;
			else
				ex = new AppException(t, "X0");
			if (r.exec != null) {
				ex.error().addDetail(r.exec.traces());
				r.exec.closeAll();
			}
			result = new Result();
			result.text = inp.isGet ? ex.error().toString() : ex.error().toJSON();
			result.mime = inp.isGet ? "text/plain" : "application/json";
			writeResp(r.resp, ex.error().httpStatus, result);
		}
	}
		
	/********************************************************************************/
	private void writeResp(HttpServletResponse resp, int status, Result r){
		resp.setStatus(status);
		if (r.isEmpty())
			r.out = new Object();
		if (r.out != null || r.syncs != null) {
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			if (r.out != null) {
				sb.append("\"out\":").append(JSON.toJson(r.out));
				if (r.syncs != null) sb.append(", ");
			}
			if (r.syncs != null) sb.append("\"syncs\":").append(r.syncs);
			sb.append("}");
			r.bytes = Util.toUTF8(sb.toString());
			r.encoding = "UTF-8";
			r.mime = "application/json";
		} else if (r.text != null)
			try {
				if (r.mime == null)
					r.mime = "text/plain";
				if (r.encoding == null)
					r.encoding = "UTF-8";
				if (!"UTF-8".equals(r.encoding))
					try { "a".getBytes(r.encoding);	} catch (Exception x) {	r.encoding = "UTF-8"; }
				r.bytes = r.text.getBytes(r.encoding); 				
			} catch (Exception e1) { 
				r.bytes = null; 
			}
		if (r.mime == null)
			r.mime = "application/octet-stream";
		if (r.bytes != null){
			resp.setContentType(r.mime);
			resp.setContentLength(r.bytes.length);
			if (r.encoding != null)
				resp.setCharacterEncoding(r.encoding);
			try { Util.streamBytes(resp.getOutputStream(), r.bytes); } catch (IOException e) {	}
		}
	}

	/********************************************************************************/
	private InputData getInputData(HttpServletRequest req) {
		InputData inp = new InputData();
		inp.isGet = true;
		Enumeration<String> e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String n = e.nextElement();
			String[] v = req.getParameterValues(n);
			if (v.length >= 1) {
				if (inp.args == null)
					inp.args = new Hashtable<String, String>();
				inp.args.put(n,  v[0]);
			}
		}
		return inp;
	}
	
	/********************************************************************************/
	private InputData postInputData(HttpServletRequest req) throws AppException {
		InputData inp = new InputData();
		inp.isGet = false;
		ServletFileUpload upload = new ServletFileUpload();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		FileItemIterator iterator;
		try {
			iterator = upload.getItemIterator(req);
			while (iterator.hasNext()) {
				FileItemStream item = iterator.next();
				InputStream stream = item.openStream();
				String fn = item.getFieldName();
				if (item.isFormField()){
					int len;
					bos.reset();
					while ((len = stream.read(buffer, 0, buffer.length)) != -1)
						bos.write(buffer, 0, len);
					if (inp.args == null)
						inp.args = new Hashtable<String, String>();
					inp.args.put(fn, new String(bos.toByteArray(), "UTF-8"));
				} else {
					Attachment a = new Attachment();
					FileItemHeaders fih = item.getHeaders();
					String h = fih.getHeader("Content-Transfer-Encoding");
					a.filename = item.getName();
					a.name = fn;
					a.contentType = item.getContentType();
					int len;
					bos.reset();
					while ((len = stream.read(buffer, 0, buffer.length)) != -1)
						bos.write(buffer, 0, len);
					byte[] b = bos.toByteArray();
					if ("base64".equals(h)){
						a.b64 = new String(b, "UTF-8");
						a.decodeB64();
					} else 
						a.bytes = b;
					if (inp.attachments == null)
						inp.attachments = new Hashtable<String, Attachment>();
					inp.attachments.put(a.name, a);
				}
			}
			bos.close();
			return inp;
		} catch (FileUploadException | IOException e) {
			throw new AppException(e, "XINPFAILURE");
		}
	}	

	/********************************************************************************/
	static void sendRes(Resource r, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (r != null) {
			String et = req.getHeader("If-None-Match");
			if (et != null && et.endsWith("gzip"))
				et = et.substring(0, et.indexOf('-'));
			if (et == null || !r.sha.equals(et)) {
				resp.setStatus(200);
				resp.setContentType(r.mime);
				resp.setContentLength(r.bytes.length);
				resp.setHeader("ETag", r.sha);
				resp.getOutputStream().write(r.bytes);
			} else {
				resp.setStatus(304);
			}
		} else {
			resp.sendError(404);
		}	
	}

	/********************************************************************************/
	public static class InputData {
		private String operationName;
		private Document.Id taskId;
		private boolean isGet;	
		private String uri;
		private Hashtable<String, String> args = new Hashtable<String, String>();
		private Hashtable<String, Attachment> attachments = new Hashtable<String, Attachment>();		

		public boolean isGet() { return isGet; };	
		public String operationName() {return operationName; }	
		public Document.Id taskId() { return taskId; }
		public String uri() { return uri;}
		public Hashtable<String, String> args() { return args; };
		public Hashtable<String, Attachment> attachments() { return attachments; };		
	}
	
	/********************************************************************************/
	public static class Attachment {
		public String name;
		public String filename;
		public String contentType;
		public String b64;
		public byte[] bytes;	
		
		public void decodeB64() {
			if (b64 != null){
				int i = b64.indexOf(':');
				int j = b64.indexOf(';');
				int k = b64.indexOf(',');
				if (i != -1 && j > i && k == j + 7) {
					contentType = b64.substring(i+1, j);
					String prefix = "data:" + contentType + ";base64,";
					if (b64.startsWith(prefix))
						try { bytes = Crypto.base64ToBytes(b64.substring(prefix.length(), b64.length())); } catch (Exception e) {	bytes = null; }
				}
				if (bytes == null) contentType = "text/plain";
			}
		}
	}

	/********************************************************************************/
	private static final int lvar = "/var/".length();
	private void lpath(BConfig.ResFilter rf, ArrayList<String> var, String root){
		Set<String> paths = servletContext.getResourcePaths(root);
		if (paths != null)
			for(String s : paths)
				if (rf.filterDir(s, s.substring(root.length()))) {
					if (s.endsWith("/"))
						lpath(rf, var, s);
					else if (MimeType.mimeOf(s) != null && rf.filterFile(s)) 
							var.add(s.substring(lvar));
				}
	}

	
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
	
	/**Test Blob ******************************************************************************/
//	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
//		try {
//			AConfig cfg = AConfig.config();
//			DBProvider provider = cfg.newDBProvider(cfg.ns()[0]);
//			BlobProvider bp = provider.blobProvider();
//		    String data = req.getParameter("d");
//		    if (data == null) data = "toto";
//			byte[] t = data.getBytes(BlobProvider.UTF8);
//			bp.blobStore("abc", t);
//			byte[] b = bp.blobGet("abc");
//		    resp.setContentType("text/plain");
//		    resp.setCharacterEncoding("UTF-8");
//		    resp.getWriter().print(new String(b, BlobProvider.UTF8));
//		} catch (AppException e) {
//			e.printStackTrace();
//		}
//	}

	/********************************************************************************/
//	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
//		PrintWriter out = resp.getWriter();
//	    out.println("Hello, world!");
//	    out.close();
//	}
	/********************************************************************************/
}
