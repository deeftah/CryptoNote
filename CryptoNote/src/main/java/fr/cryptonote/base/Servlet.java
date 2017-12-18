package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
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
import fr.cryptonote.base.BConfig.TxtDic;
import fr.cryptonote.provider.DBProvider;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final HashMap<String,Integer> ext2 = new HashMap<String,Integer>();	
	
	static {
		ext2.put("net", 0); ext2.put("sync", 1); ext2.put("sync2", 1); ext2.put("local", 2); ext2.put("local2", 2); 
	}
	
	private static Boolean done = false;
	private static String contextPath;
	private static ServletContext servletContext;
	private static String[] var;
	private static String[] zres;

	public static String contextPath() { return contextPath; }
	public static String contextPathSlash() { return (contextPath.length() == 0) ? "/" : "/" + contextPath + "/"; }
	public static boolean hasCP() { return contextPath.length() != 0; }
	public static String[] var() { return var; }
	public static String[] zres() { return zres; }
	public static boolean isZres(String name) { for (String n : zres) if (n.equals(name)) return true; return false; }

	/********************************************************************************/
	private void lpath(ArrayList<String> var, String root){
		Set<String> paths = servletContext.getResourcePaths(root);
		if (paths != null)
			for(String s : paths)
				if (s.endsWith("/"))
					lpath(var, s);
				else if (MimeType.mimeOf(s) != null) 
					var.add(s);
	}

	/********************************************************************************/
	@Override public void init(ServletConfig servletConfig) throws ServletException {
		synchronized (done) {
			if (done) return;
			servletContext = servletConfig.getServletContext();
			String s  = servletContext.getContextPath();
			contextPath = s == null ? "" : s.startsWith("/") ? s.substring(1) : s;
			
			MimeType.init();

			ArrayList<String> varx = new ArrayList<String>();
			lpath(varx, "/var/");
			var = varx.toArray(new String[varx.size()]);
			Arrays.sort(var);
			ArrayList<String> zresx = new ArrayList<String>();
			for(String x : var) if (x.startsWith("z/"))	zresx.add(x.substring(2));
			zres = zresx.toArray(new String[zresx.size()]);
	
			BConfig.startup();			
			
			done = true;
		}

	}
	
	@Override public void destroy() {
		try {
			QueueManager.stopQM();
		} catch (Exception e) {	}
	}

	/********************************************************************************/	
	private final class ReqCtx {
		HttpServletRequest req;
		HttpServletResponse resp;
		ExecContext exec;
		String origUri;
		String uri;
		int build;
		Nsqm nsqm;
		boolean fini = false;
		boolean isGet;
		boolean isTask;
		String lang;
		
		ReqCtx(HttpServletRequest req, HttpServletResponse resp, boolean isGet) throws IOException { 
			String lg = req.getHeader("Accept-Language");
			if (lg != null && lg.length() != 0) {
				int i = lg.indexOf(',');
				if (i != -1) lg = lg.substring(0,i);
			}
			this.lang = BConfig.lang(BConfig.lang(lg));
			this.req = req; 
			this.resp = resp; 
			this.isGet = isGet;
			String s =  req.getRequestURI();
			origUri = s.substring(contextPath.length() + 2);
			build = BConfig.build();
			if ("ping".equals(origUri)) {
				String x = "{\"t\":" + Stamp.fromNow(0).stamp() + ", \"b\":" + build + "}";
				sendText(0, x, resp, "application/json");
				fini = true;
			}
			String shortcut = BConfig.shortcut(origUri);
			if (shortcut != null) origUri = shortcut;
			exec = new ExecContext().setLang(this.lang);
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
				build += nsqm.build;
			}
			exec.setNsqm(nsqm);
			
			uri = origUri.substring(i + 1);
			if ("ping".equals(uri)) {
				String dbInfo;
				try {
					dbInfo = exec.dbProvider().dbInfo(null);
				} catch (AppException e) {
					dbInfo = BConfig.label("XDBINFO");			
				}
				StringBuffer sb = new StringBuffer();
				sb.append("{\"t\":").append("" + Stamp.fromNow(0).stamp()).append(", \"b\":").append(build).append(", \"off\":").append(nsqm.off)
				.append(", \"db\":\"").append(dbInfo).append("\"}");
				sendText(0, sb.toString(), resp, "application/json");
				fini = true;
				return;
			}
		}
		
		private void appcache() throws IOException {
			String p = contextPath + "/" + nsqm.code + "/var" + build + "/";
			StringBuffer sb = new StringBuffer();
			sb.append("CACHE MANIFEST\n#").append(build).append("\nCACHE:\n");
			int n = "/var/".length();
			for(String s : var())
				sb.append(p + s.substring(n) + "\n");
			for(String x : BConfig.offlineHomes()) 
				sb.append(p).append(x).append("2\",\n");	
			sendRes(new Servlet.Resource(sb.toString().getBytes("UTF-8"), "text/cache-manifest"), req, resp);
			fini = true;
		}

		private void swjs() throws IOException {
			Resource c = getResource(BConfig.SWJS);
			if (c == null) {
				sendText(404, BConfig.format("404SWJS"), resp);
				fini = true;
				return;
			}
			String p1 = "\"/" + contextPath + "/" + nsqm.code + "/";
			String p2 = p1 + "var" + build + "/";

			StringBuffer sb = new StringBuffer();
			sb.append("'use strict';\n");
			sb.append("const CONTEXTPATH = \"").append(contextPath).append("\";\n");
			sb.append("const BUILD = \"").append(BConfig.build()).append("\";\n");
			sb.append("const NS = \"").append(nsqm.code).append("\";\n");
			sb.append("let RESSOURCES = [\n");
			for(String x : BConfig.offlineHomes()) 
				sb.append(p1).append(x).append("\",\n");	
			int n = "/var/".length();
			for(String s : var()) 
				sb.append(p2 + s.substring(n) + "\",\n");
			sb.setLength(sb.length() - 2);
			sb.append("];\n").append(c.toString());
			sendRes(new Servlet.Resource(sb.toString().getBytes("UTF-8"), " text/javascript"), req, resp);	
			fini = true;
		}
	
		void resource() throws IOException {
			int i = uri.indexOf("/");
			if (i == -1 || i >= uri.length() -1) {
				resp.sendError(404);
				return;
			}
			uri = uri.substring(i + 1);
			Resource res = null;
			if (uri.startsWith("z/"))
				res = NS.resource(nsqm.code, uri.substring(2));
			if (res == null)
				res = getResource("/var/" + uri);
			if (res == null)
				resp.sendError(404);
			sendRes(res, req, resp);
		}

		void page() throws IOException{
			int i = uri.indexOf('.');
			String home = null;
			String ext = null;
			if (i == -1) {
				ext = "net";
				home = uri;
			} else {
				ext = uri.substring(i + 1);
				home = uri.substring(0, i);
			}
			if (!BConfig.homes().contains(home)){
				sendText(404, BConfig.format("404HOME2", home), resp);
				fini = true;
				return;
			}
			int mx = BConfig.homeMode(home);
			Integer mode = ext2.get(ext);
			if (mode == null) {
				sendText(404, BConfig.format("404HOME1", ext), resp);
				fini = true;
				return;				
			}
			if (mode > mx) {
				sendText(404, BConfig.format("404HOME3", home), resp);
				fini = true;
				return;				
			}

			Resource r = getResource(BConfig.INDEX);
			if (r == null){
				sendText(404, BConfig.format("404IDX0"), resp);
				fini = true;
				return;
			}
			String html = r.toString();
			int x = html.indexOf('\n');
			if (x == -1){
				sendText(404, BConfig.format("404IDX1"), resp);
				fini = true;
				return;
			}
			html = html.substring(x);
			x = html.indexOf("</head>");
			if (x == -1){
				sendText(404, BConfig.format("404IDX2"), resp);
				fini = true;
				return;
			}
			String head = html.substring(0, x);
			if (head.length() == 0){
				sendText(404, BConfig.format("404IDX3"), resp);
				fini = true;
				return;
			}
			String body = html.substring(x);
			if (body.length() < 19){
				sendText(404, BConfig.format("404IDX4"), resp);
				fini = true;
				return;
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("<!DOCTYPE html>");
			
			if (ext.endsWith("2")) 
				sb.append("<html manifest=\"").append(contextPathSlash()).append(nsqm.code).append("/x.appcache\">");
			else
				sb.append("<html>");
			
			sb.append("<head><base href=\"").append(contextPathSlash()).append(nsqm.code).append("/var").append(build).append("/\">\n");
			sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n");
			sb.append("<script src=\"js/init.js\"></script>\n");
			sb.append("<script src=\"js/build.js\"></script>\n");

			sb.append("\n<script type=\"text/javascript\">\n");
			sb.append("'use strict';\n");
			sb.append("APP.home = \"").append(home).append("\";\n");
			sb.append("APP.homemode = ").append(mode).append(";\n");
			sb.append("APP.srvbuild = ").append(BConfig.build()).append(";\n");
			sb.append("APP.contextpath = \"").append(contextPath).append("\";\n");
			
			sb.append("APP.langs = [");
			String[] lx = BConfig.langs();
			for(int l = 0; l < lx.length; l++){
				if (l != 0) sb.append(",");
				sb.append("\"").append(lx[l]).append("\"");
			}
			sb.append("];\n");
			
			sb.append("APP.lang = \"").append(lang).append("\";\n");
			sb.append("APP.zone = \"").append(BConfig.zone()).append("\";\n");
			sb.append("APP.namespace = \"").append(nsqm.code).append("\";\n");
			sb.append("APP.nslabel = \"").append(nsqm.label).append("\";\n");
			
			for(int l = 0; l < lx.length; l++){
				TxtDic dic = BConfig.exportDic(lx[l]);
				for(String k : dic.keySet()){
					String m = dic.get(k);
					String s = m.replace("\"", "\\\"").replace("\n", "\\n");
					sb.append("APP.addMsg(\"").append(lx[l]).append("\", \"").append(k).append("\", \"").append(s).append("\");\n");
				}
			}
			
			String b = BConfig.byeAndBack();
			if (b != null)
				sb.append("APP.byeAndBack = \"").append(b).append("\";\n");
			sb.append("APP.init();\n");
			sb.append("</script>\n");

			sb.append(head);
			sb.append("<link rel='import' href='themes/" + nsqm.theme + "-theme.html'>\n");
			sb.append("<link rel='import' href='z/custom.html'>\n");
			sb.append("<script src='js/final.js'></script>\n");

			sb.append(body);
			
			sendRes(new Resource(sb.toString(), "text/html"), req, resp);
			fini = true;
		}

	}
	
	/********************************************************************************/
	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ReqCtx r = new ReqCtx(req, resp, true);
		if (r.fini) return;
		r.checkNsqm();
		if (r.fini) return;
		
		if (r.uri.endsWith("x.appcache")) { r.appcache(); return; }
		if (r.uri.endsWith("x.swjs")) { r.swjs();	return;	}
		
		r.isTask = r.uri.startsWith("od/");
		if (r.uri.startsWith("op/") || r.isTask){
			doGetPost(r);
			return;
		}

		if (r.uri.startsWith("var")) {
			r.resource();
			return;
		}

//		if (r.uri.endsWith(".sync") || r.uri.endsWith(".local"))
//			Util.log.info(r.uri);
		r.page();
	}

	/********************************************************************************/
	@Override public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ReqCtx r = new ReqCtx(req, resp, false);
		if (r.fini) return;
		r.checkNsqm();
		if (r.fini) return;
				
		r.isTask = r.uri.startsWith("od/");
		if (r.uri.startsWith("op/") || r.isTask){
			doGetPost(r);
			return;
		}

		resp.sendError(404);
	}

	/********************************************************************************/
	private void sendText(int code, String text, HttpServletResponse resp) throws IOException {sendText(code, text, resp, null);}
	private void sendText(int code, String text, HttpServletResponse resp, String contentType) throws IOException {
		if (text == null) text = "";
		resp.setContentType(contentType == null ? "text/plain" : contentType);
		resp.setStatus(200);
		resp.setCharacterEncoding("UTF-8");
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
	private void doGetPost(ReqCtx r) throws IOException, ServletException {
		// op/ od/
		InputData inp = null;
		Result result = null;
		
		try {
			String ct = r.req.getContentType();
			boolean mpfd = !r.isGet && ct != null && ct.toLowerCase().indexOf("multipart/form-data") > -1;
			inp = r.isTask ? taskInputData(r) : (!mpfd ? getInputData(r.req) : postInputData(r.req));
			inp.uri = r.uri.substring(3);
			String op = inp.args.get("op");
			inp.operationName = op != null ? op  : "Default";

			String b1 = r.req.getHeader("build");
			String b2 = "" + BConfig.build();
			if (b1 != null && !b2.equals(b1))
				throw new AppException("DBUILD", b1, b2);
			
			result = r.nsqm.isQM ? QueueManager.doTmRequest(r.exec, inp) :  r.exec.go(inp);
			r.exec.closeAll();
			if (r.isTask)
				respTask(r, inp.taskInfo, null);
			else 
				writeResp(r.resp, 200, result);
		} catch (Throwable t){
			AppException ex = t instanceof AppException ? (AppException)t : new AppException(t, "X0");
			if (r.isTask) ex.error().addDetail(r.exec.traces());
			r.exec.closeAll();
			result = new Result();
			if (r.isTask)
				respTask(r, inp.taskInfo, ex.error());
			else {
				result.text = inp.isGet ? ex.error().toString() : ex.error().toJSON();
				result.mime = inp.isGet ? "text/plain" : "application/json";
				writeResp(r.resp, ex.error().httpStatus, result);
			}
		}
	}

	/********************************************************************************/
	private void respTask(ReqCtx r, TaskInfo ti, AppException.Error err) {
		r.resp.setStatus(err == null ? 200 : err.httpStatus);
		try {
			DBProvider provider = BConfig.getDBProvider(r.nsqm.base).ns(r.nsqm.code);
			if (err == null)
				provider.removeTask(ti.ns, ti.taskid);
			else
				provider.excTask(ti.ns, ti.taskid, err.minor, err.toJSON(), Stamp.fromNow(BConfig.TASKRETRIESINMIN(ti.retry - 1) * 60000).stamp());
		} catch (Exception e) { }
	}

	/********************************************************************************/
	private void writeResp(HttpServletResponse resp, int status, Result r){
		resp.setStatus(status);
		if (r == null || r.isEmpty())
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

	/*********************************************************************************/
	private InputData taskInputData(ReqCtx r) throws AppException {
		String key = r.req.getParameter("key");
		if (!r.nsqm.pwd().equals(key)) throw new AppException("STASKKEY");
		InputData inp = new InputData();
		DBProvider provider = BConfig.getDBProvider(r.nsqm.base).ns(r.nsqm.code);
		inp.taskInfo = provider.taskInfo(r.nsqm.code, r.uri.substring(3));
		inp.args.put("op", inp.taskInfo.opName);
		inp.taskInfo.startTime = Stamp.fromNow(0).stamp();
		provider.startTask(inp.taskInfo.ns, inp.taskInfo.taskid, inp.taskInfo.startTime);
		return inp;
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
				resp.setHeader("build", "" + BConfig.build());
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
		private TaskInfo taskInfo;
		private boolean isGet;
		private boolean isTask;
		private String uri;
		private Hashtable<String, String> args = new Hashtable<String, String>();
		private Hashtable<String, Attachment> attachments = new Hashtable<String, Attachment>();		

		public boolean isGet() { return isGet; };	
		public boolean isTask() { return isTask; };	
		public String operationName() {return operationName; }	
		public TaskInfo taskInfo() { return taskInfo; }
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

	static byte[] getRawResource(String name) {
		return Util.bytesFromStream(servletContext.getResourceAsStream(name));
	}
	
	public static Resource getResource(String name){
		if (name == null || name.length() == 0) return null;
		Resource r = resources.get(name);
		if (r != null)
			return r == emptyResource ? null : r;
		String mime = MimeType.mimeOf(name);
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
