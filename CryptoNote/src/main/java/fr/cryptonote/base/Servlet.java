package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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
import fr.cryptonote.provider.DBProvider;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String HPC = "htm";
	public static final String AVION = "a";
	public static final String SYNC = "s";
	public static final String SYNC2 = SYNC + "/";
		
	private static Boolean done = false;
	private static String contextPath;
	private static ServletContext servletContext;
	private static String[] var;
	private static HashSet<String> varx = new HashSet<String>();
	private static HashMap<String,HashMap<String,String>> zres = new HashMap<String,HashMap<String,String>>();
	
	public static boolean hasVar(String name) { return varx.contains(name); }

	public static String contextPath() { return contextPath; }
	public static String contextPathSlash() { return (contextPath.length() == 0) ? "/" : "/" + contextPath + "/"; }
	public static boolean hasCP() { return contextPath.length() != 0; }
	public static String[] var() { return var; }
	public static HashMap<String,String> zres(String ns) { 
		HashMap<String,String> x = zres.get(ns); 
		if (x == null) {
			x = new HashMap<String,String>();
			zres.put(ns, x);
		}
		return x;
	}
	
	/********************************************************************************/
	private static MessageDigest digestSha256;
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
	
	public static String SHA256b64(String s) {
		return Base64.getUrlEncoder().encodeToString(SHA256(Base64.getUrlDecoder().decode(s == null ? "" : s)));
	}

	public static String SHA256b64(byte[] bytes) {
		return Base64.getUrlEncoder().encodeToString(SHA256(bytes == null ? new byte[0] : bytes));
	}

	/********************************************************************************/
	private void lpath(String root){
		Set<String> paths = servletContext.getResourcePaths(root);
		if (paths != null)
			for(String s : paths)
				if (s.endsWith("/"))
					lpath(s);
				else if (MimeType.mimeOf(s) != null) {
					if (!s.startsWith("/var/z/")) {
						varx.add(s);
					} else {
						int i = s.indexOf('/', 7);
						if (i != -1) {
							String ns = s.substring(7, i);
							String p1 = "/var/z/z" + s.substring(i);
							String p2 = "/var/z/" + ns + s.substring(i);
							HashMap<String,String> vz = zres(ns);
							vz.put(p1, p2);
						}
					}
				} else
					Util.log.info("Ressource ignorée : " + s);
		
	}

	/********************************************************************************/
	static ServletException exc(Exception e, String msg) {
		if (e != null) msg += "\n" + e.getMessage() + "\n" + Util.stack(e);
		return new ServletException(msg);
	}

	public static Object script2json(String resName, Class<?> cl) throws ServletException{
		Servlet.Resource r = Servlet.getResource(resName, resName.endsWith(".js") ? "text/javascript" : null);
		if (r != null) {
			try {
				String s = Util.fromUTF8(r.bytes);
				int i = s.indexOf("{");
				int j = s.lastIndexOf("}");
				String x = i != -1 && j != -1 && i < j ? s.substring(i, j+1) : "{}";
				return JSON.fromJson(x, cl);
			} catch (Exception ex) {
				throw exc(ex, "Ressource : " + resName + " - JSON mal formé");
			}
		} else 
			throw exc(null, "Ressource : " + resName + " - Absente");
	}

	/********************************************************************************/
	@Override public void init(ServletConfig servletConfig) throws ServletException {
		synchronized (done) {
			if (done) return;
			new ExecContext();
			try { digestSha256 = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException e) { throw new ServletException(e); }
			emptyResource = new Resource((byte[])null, null);
			servletContext = servletConfig.getServletContext();
			String s  = servletContext.getContextPath();
			contextPath = s == null ? "" : s.startsWith("/") ? s.substring(1) : s;
			
			MimeType.init();

			lpath("/var/");
			var = varx.toArray(new String[varx.size()]);
			Arrays.sort(var);
			HashMap<String,String> rz = zres("z");
			for(String ns : zres.keySet()) {
				HashMap<String,String> rns = zres(ns);
				for(String n : rz.keySet())
					if (rns.get(n) == null) rns.put(n, rz.get(n));
			}
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
		boolean isSW;
		boolean isIncognito;
		int mode; // navigation ... // 0:privée(incognito) 1:normale(cache+net) 2:locale(cache seulement)
		int modeMax;
		String lang;
		CustomHeader customHeader;
		
		ReqCtx(HttpServletRequest req, HttpServletResponse resp, boolean isGet) throws IOException { 
			exec = new ExecContext();
			String lg = req.getHeader("Accept-Language");
			if (lg != null && lg.length() != 0) {
				int i = lg.indexOf(',');
				if (i != -1) lg = lg.substring(0,i);
			}
			String b1 = req.getHeader("X-Custom-Header");
			if (b1 != null)
				try { customHeader = JSON.fromJson(b1, CustomHeader.class); } catch (Exception e) {}

			this.lang = BConfig.lang(BConfig.lang(customHeader != null && customHeader.lang != null ? customHeader.lang : lg));
			exec.setLang(this.lang);
			
			this.req = req; 
			this.resp = resp; 
			this.isGet = isGet;
			String s =  req.getRequestURI();
			// /contextPath/namespace/... /contextPath/namespace/s/...
			origUri = s.substring(contextPath.length() + 2);
			build = BConfig.build();
			if ("ping".equals(origUri)) {
				String x = "{\"t\":" + Stamp.fromNow(0).stamp() + ", \"b\":" + build + "}";
				sendText(200, x, resp, "application/json");
				fini = true;
			}
			String shortcut = BConfig.shortcut(origUri);
			if (shortcut != null) origUri = shortcut;
		}
				
		void checkNsqm() throws IOException {
			// namespace/... pu namespace/s/...
			int i = origUri.indexOf('/');
			if (i == -1 || i == origUri.length() - 1) {
				sendText(500, BConfig.format("500URLMF1" + (hasCP() ? "cp" : ""), origUri), resp);
				fini = true;
				return;
			}
			uri = origUri.substring(i + 1);
			String ns = origUri.substring(0, i);
			nsqm = BConfig.nsqm(ns, true);
			if (nsqm == null) {
				sendText(500, BConfig.format("500URLMF2" + (hasCP() ? "cp" : ""), origUri, ns), resp);
				return;			
			}

			// si namespace est suivi de $ : navigation privée/incognito
			isIncognito = !uri.startsWith(SYNC2);
			if (!isIncognito) {
				mode = 1;
				uri = uri.substring(SYNC2.length());
			}
			if (nsqm.isQM) {
				if (nsqm.code.equals(BConfig.QM())) {
					sendText(500, BConfig.format("500HQM", nsqm.code), resp);	
					fini = true;
					return;
				}	
			}
			exec.setNsqm(nsqm);
			
			if ("ping".equals(uri)) {
				String dbInfo;
				try {
//					try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException e) {}
					dbInfo = exec.dbProvider().dbInfo(null);
				} catch (AppException e) {
					dbInfo = "?";			
				}
				StringBuffer sb = new StringBuffer();
				sb.append("{\"t\":").append("" + Stamp.fromNow(0).stamp()).append(", \"b\":").append(build).append(", \"off\":").append(nsqm.onoff)
				.append(", \"db\":\"").append(dbInfo).append("\"}");
				sendText(200, sb.toString(), resp, "application/json");
				fini = true;
				Util.log.info("Ping");
				return;
			}
		}
		
		private void appcache() throws IOException {
			String p1 = "/" + contextPath + "/" + nsqm.code + "/";
			String p2 = p1 + "var" + build + "/";
			StringBuffer sb = new StringBuffer();
			sb.append("CACHE MANIFEST\n#").append(build).append("\n\nCACHE:\n\n");
			for(String x : nsqm.homes(1)) sb.append(p1).append(x).append("." + HPC + "\n");	
			for(String x : nsqm.homes(2)) sb.append(p1).append(x).append("." + AVION + HPC + "\n");	
			for(String s : var()) 
				if (!s.endsWith("/sw.js"))
					sb.append(p2 + s.substring(5) + "\n"); // 5 "/var/".length()
			HashMap<String,String> rns = zres(nsqm.code);
			for(String s : rns.keySet()) sb.append(p2 + s.substring(5) + "\n"); // 5 "/var/".length()
			sendRes2(sb.toString().getBytes("UTF-8"), resp, "text/cache-manifest");
			fini = true;
		}

		private void swjs() throws IOException {
			Resource c = getResource(BConfig.SWJS);
			if (c == null) {
				sendText(404, BConfig.format("404SWJS"), resp);
				fini = true;
				return;
			}
			String p1 = "\"/" + contextPath + "/" + nsqm.code + "/" + SYNC2;
			String p2 = p1 + "var" + build + "/";

			StringBuffer sb = new StringBuffer();
			sb.append("'use strict';\n");
			sb.append("const CONTEXTPATH = \"").append(contextPath).append("\";\n");
			sb.append("const BUILD = \"").append(BConfig.build()).append("\";\n");
			sb.append("const NS = \"").append(nsqm.code).append("\";\n");
			sb.append("const RESSOURCES = [\n");
			for(String x : nsqm.homes(1)) sb.append(p1).append(x).append("\",\n");	
			for(String x : nsqm.homes(1)) sb.append(p1).append(x).append('_').append(BConfig.build()).append("_\",\n");	
			for(String s : var()) 
				if (!s.endsWith("/sw.js"))
					sb.append(p2 + s.substring(5) + "\",\n"); // 5 "/var/".length()
			HashMap<String,String> rns = zres(nsqm.code);
			for(String s : rns.keySet()) sb.append(p2 + s.substring(5) + "\",\n"); // 5 "/var/".length()
			sb.setLength(sb.length() - 2);
			sb.append("];\n").append(c.toString());
			sendRes2(sb.toString().getBytes("UTF-8"), resp, "text/javascript");	
			fini = true;
		}
	
		void resource() throws IOException {
			String urix = uri;
			int i = uri.indexOf("/");
			if (i == -1) {
				resp.sendError(404);
				Util.log.severe("404 - " + urix);
				return;
			}
			if (i == uri.length() -1) {
				sendText(200, "", resp);
				return;
			}
			uri = uri.substring(i + 1);
			Resource res = null;
			HashMap<String,String> rns;
			if (uri.startsWith("z/")) {
				rns = zres(nsqm.code);
				String subst = rns.get("/var/" + uri);
				if (subst != null)
					res = getResource(subst);
			} else
				res = getResource("/var/" + uri);
			if (res == null) {
//				sendText(200, "", resp);
				resp.sendError(404);
				Util.log.info("404 - " + urix);
				return;
			}
			sendRes(res, req, resp);
		}

		void page() throws IOException{
			// Util.log.info("Demande de : " + origUri);
			boolean avion = false;
			isSW = true;
			
			if (uri.endsWith(HPC)) {
				isSW = false;
				uri = uri.substring(0, uri.length() - HPC.length());
				if (uri.endsWith("."))
					uri = uri.substring(0, uri.length() - 1);
			}
			
			if (uri.endsWith("." + AVION)){
				uri = uri.substring(0, uri.length() - AVION.length() - 1);
				avion = true;
			}
			
			if (uri.endsWith("_")) { // élimine la version home2_203_
				int j = uri.lastIndexOf('_', uri.length() - 2);
				if (j != -1) uri = uri.substring(0, j);
			}
			
			modeMax = nsqm.modeMax(uri);
			if (modeMax == -1){
				sendText(404, BConfig.format("404HOME1", uri), resp);
				fini = true;
				return;
			}
			if (avion && modeMax < 2){
				sendText(404, BConfig.format("404HOME3", uri), resp);
				fini = true;
				return;
			}
			if (avion)
				mode = 2;
			if (mode > 0 && modeMax == 0){
				sendText(404, BConfig.format("404HOME2", uri), resp);
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
			
			if (isSW || mode == 0)
				sb.append("<html>");
			else
				sb.append("<html manifest=\"").append(contextPathSlash()).append(nsqm.code).append("/x.appcache\">");
			
			sb.append("<head><base href=\"").append(contextPathSlash()).append(nsqm.code)
				.append(mode > 0 ? "/" + SYNC2 : "/").append("var").append(build).append("/\">\n");
			sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n");

			sb.append("<script src='js/build.js'></script>\n");
			sb.append("<script src='js/root.js'></script>\n");

			sb.append("\n<script type='text/javascript'>\n");
			sb.append("App.modeMax = ").append(modeMax).append(";\n");
			sb.append("App.buildAtPageGeneration = ").append(BConfig.build()).append(";\n");
			sb.append("App.zone = \"").append(BConfig.zone()).append("\";\n");
			sb.append("App.langs = ").append(JSON.toJson(BConfig.langs())).append(";\n");
			sb.append("App.lang = \"").append(lang).append("\";\n");
			sb.append("App.theme = \"").append(nsqm.theme).append("\";\n");
			sb.append("App.themes = JSON.parse('").append(JSON.toJson(BConfig.themes())).append("');\n");
			sb.append("App.helpDic = JSON.parse('").append(JSON.toJson(nsqm.help())).append("');\n");
			sb.append("App.zDics = {};\n");
			sb.append("App.customThemes = {};\n");
			
			HashMap<String,String> rns = zres(nsqm.code);
			sb.append("App.zres = {\n");
			int l = "/var/z/z/".length();
			for(String n : rns.keySet())
				sb.append("\"").append(n.substring(l)).append("\":true,\n");
			sb.setLength(sb.length() - 2);
			sb.append("};\n");
			sb.append("</script>\n");
			
			// dics et thèmes dépendant de lang
			for(String lang : BConfig.langs()){
				String p = "/var/js/" + lang + "/";
				if (varx.contains(p + "base-msg.js"))
					sb.append("<script src='js/" + lang + "/base-msg.js'></script>\n");
				if (varx.contains(p + "app-msg.js"))
					sb.append("<script src='js/" + lang + "/app-msg.js'></script>\n");
				p = "/var/z/" + nsqm.code + "/" + lang + "/";
				if (varx.contains(p + "app-msg.js"))
					sb.append("<script src='z/z/" + lang + "/app-msg.js'></script>\n");
				if (varx.contains(p + "base-msg.js"))
					sb.append("<script src='z/z/" + lang + "/base-msg.js'></script>\n");
				if (varx.contains("/var/js/" + lang + "/theme.js"));
					sb.append("<script src='js/" + lang + "/theme.js'></script>\n");				
			}
			
			for(String t : BConfig.themes())
				if (varx.contains("/var/js/theme-" + t + ".js"))
					sb.append("<script src='js/theme-" + t + ".js'></script>\n");
			
			sb.append(head);
			sb.append(body);
			sendRes2(sb.toString().getBytes("UTF-8"), resp, "text/html");
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
		if (r.uri.endsWith("sw.js")) { r.swjs(); return;	}
		
		r.isTask = r.uri.startsWith("od/");
		if (r.uri.startsWith("op/") || r.isTask){
			doGetPost(r);
			return;
		}

		if (r.uri.startsWith("var")) {
			r.resource();
			return;
		}

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
	private static class CustomHeader {
		int build;
		String lang;
		CustomHeader(int build) { this.build = build; }
		public String toString() { return JSON.toJson(this); }
	}
	
	static void setBuild(HttpServletResponse resp) {
		resp.addHeader("X-Custom-Header", new CustomHeader(BConfig.build()).toString());
	}
	
	/********************************************************************************/
	private void doGetPost(ReqCtx r) throws IOException, ServletException {
		// op/ od/
		InputData inp = null;
		Result result = null;
		
		try {
			if (r.isTask) {
				inp = taskInputData(r);
				if (inp == null) {
					sendText(200, null, r.resp);
					return;
				}
				if (r.nsqm.onoff != 0) 
					throw new AppException("OFF", BConfig.label("OFF"));
			} else {
				if (r.customHeader != null && r.customHeader.build != 0 && r.customHeader.build != BConfig.build())
					throw new AppException("DBUILD", "" + r.customHeader.build, "" + BConfig.build());
				String ct = r.req.getContentType();
				boolean mpfd = !r.isGet && ct != null && ct.toLowerCase().indexOf("multipart/form-data") > -1;
				inp = !mpfd ? getInputData(r.req) : postInputData(r.req);
			}
			
			inp.uri = r.uri.substring(3);
			String op = inp.args.get("op");
			inp.operationName = op != null ? op  : "Default";			
			if(inp.operationName.indexOf("OnOff") == -1 && r.nsqm.onoff != 0) throw new AppException("OFF", BConfig.label("OFF"));	
			
			result = r.nsqm.isQM ? QueueManager.doTmRequest(r.exec, inp) : r.exec.go(inp);
			r.exec.closeAll();
			if (r.isTask) {
				sendText(200, null, r.resp);
			} else 
				writeResp(r.resp, 200, result, r.build);
		} catch (Throwable t){
			AppException ex = t instanceof AppException ? (AppException)t : new AppException(t, "X0");
			r.exec.closeAll();
			if (r.isTask) {
				try {
					r.exec.dbProvider().excTask(inp.taskInfo(), ex);
				} catch (Exception e) {}
				sendText(200, null, r.resp);
			} else
				writeResp(r.resp, ex.httpStatus(), Result.text(ex.toJson(), null, "application/json"), r.build);
		}
	}

	/********************************************************************************/
	private void sendText(int code, String text, HttpServletResponse resp) throws IOException {sendText(code, text, resp, null);}
	private void sendText(int code, String text, HttpServletResponse resp, String contentType) throws IOException {
		if (text == null) text = "";
		resp.setContentType(contentType == null ? "text/plain" : contentType);
		resp.setCharacterEncoding("UTF-8");
		setBuild(resp);
		if (code == 0 || code == 200) {
			try {
				resp.setStatus(200);
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
	private void writeResp(HttpServletResponse resp, int status, Result r, int build){
		resp.setStatus(status);
		setBuild(resp);
		if (r == null) r = Result.empty();
		byte[] bytes = r.bytes();
		resp.setContentLength(bytes.length);
		resp.setContentType(r.mime);
		resp.setCharacterEncoding(r.encoding);
		try { Util.streamBytes(resp.getOutputStream(), bytes); } catch (IOException e) {	}
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
				setBuild(resp);
				resp.getOutputStream().write(r.bytes);
			} else {
				resp.setStatus(304);
			}
		} else {
			resp.sendError(404);
		}	
	}

	static void sendRes2(byte[] bytes, HttpServletResponse resp, String ct) throws IOException {
		if (bytes != null) {
			resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			resp.setHeader("Expires", "0"); // Proxies.
			resp.setStatus(200);
			resp.setContentType(ct);
			resp.setContentLength(bytes.length);
			setBuild(resp);
			resp.getOutputStream().write(bytes);
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
						try { bytes = Base64.getDecoder().decode(b64.substring(prefix.length(), b64.length())); } catch (Exception e) {	bytes = null; }
				}
				if (bytes == null) contentType = "text/plain";
			}
		}
	}

	/********************************************************************************/
	private static Resource emptyResource;
	private static Hashtable<String, Resource> resources;

	public static class Resource {
		public byte[] bytes;
		public String mime;
		public String sha;
		
		public Resource(byte[] bytes, String mime) { 
			this.bytes = bytes; 
			this.mime = mime;  
			sha = SHA256b64(bytes); 
		}
		public Resource(String text, String mime){ this(Util.toUTF8(text), mime); }
		public String toString(){ String s = Util.fromUTF8(bytes); return s == null ? "" : s; }
	}

	static byte[] getRawResource(String name) {
		return Util.bytesFromStream(servletContext.getResourceAsStream(name));
	}

	public static Resource getResource(String name){
		return getResource(name, null);
	}
	public static Resource getResource(String name, String mt){
		if (name == null || name.length() == 0) return null;
		if (resources == null)
			resources = new Hashtable<String, Resource>();
		Resource r = resources.get(name);
		if (r != null)
			return r == emptyResource ? null : r;
		String mime = mt != null ? mt : MimeType.mimeOf(name);
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
