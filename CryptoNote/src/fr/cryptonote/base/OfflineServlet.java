package fr.cryptonote.base;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.cryptonote.base.Servlet.Resource;

public class OfflineServlet {
	
	static void doGetOffline(String ns, String ext, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String nsbuild = null;
		try {
			if (NS.status(ns) != 0) {
				resp.sendError(404, BConfig.format("XNSNA", ns));
				return;
			}
			nsbuild = "" + NS.build(ns);
		} catch (AppException e){
			resp.sendError(404, BConfig.format("XRESSOURCEND", req.getRequestURI()));
			return;
		}
		if ("appcache".equals(ext)) {
			appcache(ns, nsbuild, req, resp);
			return;
		}
		if ("swjs".equals(ext)) {
			wsjs(ns, nsbuild, req, resp);
			return;
		}
		resp.sendError(404, BConfig.format("XRESSOURCEMF", req.getRequestURI()));
	}
	
	private static void appcache(String ns, String nsbuild, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String p1 = Servlet.contextPath() + "/" + ns;
		String v = "_" + BConfig.build() + "_" + nsbuild;
		String p2 = p1 + "/" + v + "/";
		StringBuffer sb = new StringBuffer();
		sb.append("CACHE MANIFEST\n#").append(v).append("\nCACHE:\n");
		for(String s : Servlet.var())
			sb.append(p2 + s + "\n");
		for(String s : BConfig.offlinepages())
			sb.append(p1).append('/').append(s).append(".local2\n")
			.append(p1).append('/').append(s).append(".sync2\n");	
		byte[] bytes = sb.toString().getBytes("UTF-8");
		Servlet.sendRes(new Servlet.Resource(bytes, "text/cache-manifest"), req, resp);
	}

	private static void wsjs(String ns, String nsbuild, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Servlet.Resource c = Servlet.getResource("/var/sw.js");
		if (c == null) { resp.sendError(404, BConfig.format("XRESSOURCEABSENTE", "/var/sw.js")); return; }
		
		String p1 = "\"" + Servlet.contextPath() + "/" + ns;
		String v = "_" + BConfig.build() + "_" + nsbuild;
		String p2 = p1 + "/" + v + "/";

		StringBuffer sb = new StringBuffer();
		sb.append("'use strict';\n");
		sb.append("const CONTEXTPATH = \"").append(Servlet.contextPath()).append("\";\n");
		sb.append("const BUILD = \"").append(BConfig.build()).append("\";\n");
		sb.append("const NS = \"").append(ns).append("\";\n");
		sb.append("const NSBUILD = \"").append(nsbuild).append("\";\n");
		sb.append("const RESSOURCES = [\n");
		for(String s : Servlet.var())
			sb.append(p2 + s + "\",\n");
		for(String s : BConfig.offlinepages())
			sb.append(p1).append('/').append(s).append(".local\",\n")
			.append(p1).append('/').append(s).append(".sync\",\n");	
		sb.append("];\n");
		sb.append(new String(c.bytes, "UTF-8"));
		byte[] bytes = sb.toString().getBytes("UTF-8");
		Servlet.sendRes(new Servlet.Resource(bytes, " text/javascript"), req, resp);		
	}
	
	static void doGetPages(String ns, String ext, String uri, HttpServletRequest req, HttpServletResponse resp) throws IOException {		
		Servlet.Resource res = null;
		int nsstatus = 0;
		String nsbuild = null;
		String nsinfo = null;
		String nslabel = null;
		String theme = null;
		try {
			res = NS.resource(ns, "custom.css");
			theme = NS.theme(ns);
			nsinfo = NS.info(ns);
			nslabel = NS.label(ns);
			nsstatus = NS.status(ns);
			nsbuild = "" + NS.build(ns);
		} catch (AppException e){
			resp.sendError(404, BConfig.format("XRESSOURCEND", uri));
			return;
		}
		
		int i = uri.lastIndexOf('.');
		int j = uri.lastIndexOf('/');
		if (j == -1) {
			resp.sendError(404);
			return;
		}
		int ix = uri.lastIndexOf('_', i);
		if (ix < j) ix = i;
		String page = uri.substring(j + 1, ix);
		
		Servlet.Resource r = Servlet.getResource("/var/" + page + ".html");
		if (r == null){
			resp.sendError(404, "page [" + page + ".html] non trouvée");
			return;
		}
		String html = r.toString();
		int x = html.indexOf('\n');
		if (x == -1){
			resp.sendError(404, "page [" + page + ".html] mal formée (pas de ligne 1)");
			return;
		}
		html = html.substring(x);
		x = html.indexOf("</head>");
		if (x == -1){
			resp.sendError(404, "page [" + page + ".html] mal formée (pas de </head>)");
			return;
		}
		String head = html.substring(0, x);
		if (head.length() == 0){
			resp.sendError(404, "page [" + page + ".html] mal formée (<head> vide)");
			return;
		}
		String body = html.substring(x);
		if (body.length() < 19){
			resp.sendError(404, "page [" + page + ".html] mal formée (pas de <body> ... </html>)");
			return;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html>");
		
		if (ext.endsWith("2"))
			sb.append("<html manifest=\"").append(Servlet.contextPath())
			.append("/").append(ns).append(".appcache\">");
		else
			sb.append("<html>");
		
		sb.append("<head><base href=\"").append(Servlet.contextPath()).append("/").append(ns);
		sb.append("/_").append(BConfig.build()).append("_").append(nsbuild);
		sb.append("/\">\n");
		
		sb.append(head);
		sb.append("<style is=\"custom-style\">\n");
		r = Servlet.getResource("/var/themes/default.css");
		if (r != null) sb.append(r.toString()).append("\n");
		r = Servlet.getResource("/var/themes/" + theme + ".css");
		if (r != null) sb.append(r.toString()).append("\n");
		
		if (res != null) 
			sb.append("\n\n/***** theme de l'espace de noms ")
			.append(ns)
			.append(" ************/\n")
			.append(res.toString()).append("\n");
		sb.append("</style>\n");

		sb.append("<script src=\"build.js\"></script>\n");

		sb.append("\n<script type=\"text/javascript\">\n");
		sb.append("'use strict';\n");
		sb.append("SRV.buildSrv = \"").append(BConfig.build()).append("\";\n");
		sb.append("SRV.contextpath = \"").append(Servlet.contextPath()).append("\";\n");
		
		sb.append("SRV.langs = [");
		String[] lx = BConfig.langs();
		for(int l = 0; l < lx.length; l++){
			if (l != 0) sb.append(",");
			sb.append("\"").append(lx[l]).append("\"");
		}
		sb.append("];\n");
		sb.append("SRV.defaultLang = \"").append(BConfig.defaultLang()).append("\";\n");
		// TODO
//		sb.append("APP.lang = \"").append(config.lang()).append("\";\n");
		sb.append("SRV.zone = \"").append(BConfig.zone()).append("\";\n");
		sb.append("SRV.ns = \"").append(ns).append("\";\n");
		sb.append("SRV.nsbuild = \"").append(nsbuild).append("\";\n");
		sb.append("SRV.nslabel = \"").append(nslabel).append("\";\n");
		sb.append("SRV.nsinfo = \"").append(nsinfo).append("\";\n");
		sb.append("SRV.nsstatus = ").append(nsstatus).append(";\n");
		String b = BConfig.byeAndBack();
		if (b != null)
			sb.append("SRV.byeAndBack = \"").append(b).append("\";\n");
		sb.append("</script>\n");
		
		sb.append("<script src=\"z/custom.js\"></script>\n");
		sb.append("<script src=\"final.js\"></script>\n");

		sb.append(body);
		
		Servlet.sendRes(new Resource(sb.toString(), "text/html"), req, resp);
	}

}
