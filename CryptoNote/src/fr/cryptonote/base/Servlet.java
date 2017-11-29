package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
		
	private static final String ext1 = "_appcache_swjs_";
	private static final String ext2 = "_cloud_app_local_sync_local2_sync2_";	

	private static boolean done = false;
	private static String contextPath;
	private static ServletContext servletContext;
	private static String[] var;
	private static String[] zres;

	public static String contextPath() { return contextPath; }
	public static String[] var() { return var; }
	public static String[] zres() { return zres; }
	public static boolean isZres(String name) { for (String n : zres) if (n.equals(name)) return true; return false; }

	/********************************************************************************/
	private static Exception initException;
	private static String msg;

	private void err(Exception e) throws ServletException {
		msg = e.getMessage() + "\n" + Util.stack(e);
		Util.log.severe(msg);
		if (e instanceof ServletException) throw (ServletException)e;
		initException = e;
	}

	@Override public void init(ServletConfig servletConfig) throws ServletException {
		if (done) return;
		servletContext = servletConfig.getServletContext();
		String s  = servletContext.getContextPath();
		contextPath = s == null || s.length() <= 1 ? "" : s;
		// ExecContext exec = 
		new ExecContext();
		
		try { Crypto.startup(); } catch (Exception e) { err(e); }
		
		String c = servletContext.getInitParameter("ConfigClass");
		if (c == null)	throw new ServletException(AConfig._label("XSERVLETCONFIG"));
		Class<?> configClass = Util.hasClass(c, AConfig.class);
		Method startup = null;
		try {
			startup = configClass.getMethod("startup");
		} catch (Exception e) { err(new ServletException(AConfig._label("XSERVLETCONFIGCLASS", configClass.getSimpleName()))); }
		if (!Modifier.isStatic(startup.getModifiers())) 
			err(new ServletException(AConfig._label("XSERVLETCONFIGCLASS", configClass.getSimpleName())));
		try { 
			startup.invoke(null); 
		} catch (Exception e) { err(e); }
		
		if (AConfig.config().build() == null || AConfig.config().build().length() == 0)
			err(new ServletException(AConfig._label("XSERVLETCONFIGBUILD")));
		
		try {			
			ArrayList<String> varx = new ArrayList<String>();
			lpath(AConfig.config().resFilter(), varx, "/var/");
			var = varx.toArray(new String[varx.size()]);
			Arrays.sort(var);
			ArrayList<String> zresx = new ArrayList<String>();
			for(String x : var) if (x.startsWith("z/"))	zresx.add(x.substring(2));
			zres = zresx.toArray(new String[zresx.size()]);
			
			// NS.init();
			// ec.setNS(AConfig.config().nsz());
			// QueueManager.initQ();
			
			done = true;
		} catch (Exception e){
			err(e);
		}

	}
	
	@Override public void destroy() {
		try {
			// QueueManager.closeQ();
			// new ExecContext().setNS(AConfig.config().nsz()).dbProvider().shutdwon();
		} catch (Exception e) {	}
	}

	/********************************************************************************/
	private ExecContext init1(HttpServletRequest req) throws ServletException { 
		if (initException != null) throw new ServletException(msg, initException); 
		ExecContext exec = new ExecContext();
		exec.setLang(req.getHeader("lang"));
		return exec;
	}

	private String init2(ExecContext exec, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException { 
		String uri = req.getRequestURI().substring(contextPath.length() + 1);
		
		if ("build".equals(uri)) {
			String b = AConfig.config().build();
			sendText(b, resp, b);
			return null;
		}

		if ("ping".equals(uri)) {
			String b = AConfig.config().build();
			String dbInfo;
			try {
				dbInfo = exec.dbProvider().dbInfo(null);
			} catch (AppException e) {
				dbInfo = AConfig._label("XDBINFO") + "\n" + e.getMessage();			
			}
			sendText(b + " - " + dbInfo, resp, b);
			return null;
		}
		
		return uri;
	}
	
	/********************************************************************************/
	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ExecContext exec = init1(req);
		String uri = init2(exec, req, resp);
		if (uri == null) return;
		
		String shortcut = AConfig.config().shortcut(uri);
		if (shortcut != null) uri = shortcut;
		
		int x = uri.lastIndexOf('.');
		String extx = '_' + (x == -1 ? "" : uri.substring(x + 1)) + '_';
		if (ext1.indexOf(extx) != -1) {
			int i = uri.indexOf('.');
			if (i == -1) {
				resp.sendError(404);
				return;
			}
			String ns = uri.substring(0, i);
			String ext = uri.substring(i + 1);
			exec.setNS(ns);
			OfflineServlet.doGetOffline(ns, ext, req, resp);
			return;
		}
		if (ext2.indexOf(extx) != -1) {
			int i = uri.indexOf('/');
			if (i == -1) {
				resp.sendError(404);
				return;
			}
			String ns = uri.substring(0, i);
			int j = uri.lastIndexOf('.');
			String ext = uri.substring(j + 1);
			exec.setNS(ns);
			OfflineServlet.doGetPages(ns, ext, uri, req, resp);
			return;
		}
				
		int ix = uri.indexOf('/');
		if (ix == -1) {
			resp.sendError(404);
			return;
		}
		String ns = uri.substring(0, ix);
		int nsb = 0;
		if (!ns.startsWith("qm"))
			try {
				exec.setNS(ns);
				if (NS.status(ns) > 2) {
					resp.sendError(404, AConfig._format("XSERVLETURL", ns));
					return;
				};
				nsb = NS.build(ns);
			} catch (AppException e){
				resp.sendError(404, AConfig._label("XSERVLETBASE"));
				return;			
			}
		else {
			// par convention ns est égal au qm
			ns = ns.substring(2);
			exec.setNS(ns);
		}

		if (uri.equals(ns + "/build")){
			String b = AConfig.config().build() + "_" + nsb;
			sendText(b, resp, b);
			return;
		}
		
		uri = uri.substring(ix + 1);
		if (uri.startsWith("op/") || uri.startsWith("qm/") || uri.startsWith("od/")){
			doGetPost(true, uri, exec, req, resp);
			return;
		}
		
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

		try {
			Resource r = null;
			if (uri.startsWith("z/"))
				r = NS.resource(ns, uri.substring(2));
			if (r == null)
				r = getResource("/var/" + uri);
			sendRes(r, req, resp);
		} catch (AppException e){
			resp.sendError(404, AConfig._label("XSERVLETBASE"));
			return;			
		}

	}

	/********************************************************************************/
	@Override public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		ExecContext exec = init1(req);
		String uri = init2(exec, req, resp);
		if (uri == null) return;

		int i = uri.indexOf('/');
		if (i == -1) {
			resp.sendError(404);
			return;
		}
		String ns = uri.substring(0, i);
		int nsb = 0;
		
		if (!ns.startsWith("qm"))
			try {
				exec.setNS(ns);
				if (NS.status(ns) > 2) {
					resp.sendError(404, AConfig._format("XSERVLETNS", ns));
					return;
				};
				nsb = NS.build(ns);
			} catch (AppException e){
				resp.sendError(404, AConfig._label("XSERVLETBASE"));
				return;			
			}
		else {
			// par convention ns est le namespace des namespace ("z" en général)
			ns = NS.nsns();
			exec.setNS(ns);
		}
		
		if (uri.equals(ns + "/build")){
			String b = AConfig.config().build() + "_" + nsb;
			sendText(b, resp, b);
			return;
		}

		uri = uri.substring(i + 1);

		if (uri.startsWith("op/") || uri.startsWith("qm/") || uri.startsWith("od/")){
			doGetPost(false, uri, exec, req, resp);
			return;
		}

		resp.sendError(404);

	}

	/********************************************************************************/
	private void sendText(String text, HttpServletResponse resp, String build) throws IOException {
		if (text == null) text = "";
		resp.setStatus(200);
		resp.setContentType("text/plain");
		if (build != null)
			resp.addHeader("build", build);
		try {
			byte[] bytes = text.getBytes("UTF-8");
			resp.setContentLength(bytes.length);
			resp.getOutputStream().write(bytes);		
		} catch (UnsupportedEncodingException e) {
			resp.sendError(500);
		}
	}

	/********************************************************************************/
	private void doGetPost(boolean isGet, String uri, ExecContext exec, HttpServletRequest req, HttpServletResponse resp) 
			throws IOException, ServletException {
		// op/ od/ qm/
		InputData inp = null;
		Result result = null;
		
		try {
			String ct = req.getContentType();
			boolean mpfd = !isGet && ct != null && ct.toLowerCase().indexOf("multipart/form-data") > -1;
			inp = !mpfd ? getInputData(req) : postInputData(req);
			String b1 = inp.args.get("build");
			String b2 = AConfig.config().build();
			if (b1 != null && !b2.equals(b1))
				throw new AppException("DBUILD", b1, b2);

			if (uri.startsWith("qm")) {
				result = QueueManager.doTmRequest(exec, inp);
			} else {
				inp.uri = uri.substring(3);
				if (uri.startsWith("od/")){
					inp.taskId = Document.Id.fromURL(inp.uri);
					inp.operationName = inp.taskId.docclass();
				} else { // op/ 
					String op = inp.args.get("op");
					inp.operationName = op != null ? op  : "Default";
				}
				result = exec.go(inp);
			}
			exec.closeAll();
			writeResp(resp, 200, result);
		} catch (Throwable t){
			AppException ex;
			if (t instanceof AppException)
				ex = (AppException)t;
			else
				ex = new AppException(t, "X0");
			if (exec != null) {
				ex.error().addDetail(exec.traces());
				exec.closeAll();
			}
			result = new Result();
			result.text = inp.isGet ? ex.error().toString() : ex.error().toJSON();
			result.mime = inp.isGet ? "text/plain" : "application/json";
			writeResp(resp, ex.error().httpStatus, result);
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
	private void lpath(ResFilter rf, ArrayList<String> var, String root){
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
