package fr.cryptonote.base;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.servlet.ServletException;

import fr.cryptonote.provider.DBProvider;

public class BConfig {
	private static final String[] defaultLangs = {"fr", "en"};
	private static final String DVARINSTANCE = "fr.cryptonote.instance";
	private static final String BASECONFIG = "/WEB-INF/base_config.json";
	private static final String APPCONFIG = "/WEB-INF/app_config.json";
	private static final String PASSWORDS = "/WEB-INF/passwords.json";
	private static final String BUILDJS = "/var/build.js";

	private static class TxtDic extends HashMap<String,String> { 
		private static final long serialVersionUID = 1L;			
	}
	
	private static class MFDic extends HashMap<String,MessageFormat> {
		private static final long serialVersionUID = 1L;	
	}
	
	private static MFDic mfDic0 = new MFDic();
	private static MFDic mfDic1 = new MFDic();
	private static MFDic[] mfDics = {mfDic0, mfDic1};

	static {
		mfDic0.put("XSERVLETCONFIG", new MessageFormat("Classe de configuration d'a n'est pas donnée en paramètre dans web.xml"));
		mfDic0.put("XSERVLETCONFIGCLASS", new MessageFormat("La classe [{0}] donnée dans web.xml n'a pas de méthode static startup()"));

		mfDic0.put("XSERVLETURL", new MessageFormat("URL mal formée (espace de noms [{0}] non reconnu)"));
		mfDic0.put("XSERVLETBASE", new MessageFormat("Base temporairement indisponible"));
		mfDic0.put("XSERVLETNS", new MessageFormat("URL mal formée (espace de noms [{0}] non reconnu)"));
		mfDic0.put("XSERVLETCLASS", new MessageFormat("La classe [{0}] doit exister et étendre (ou être) la classe [{1}]"));
		
		mfDic0.put("BDBNAME", new MessageFormat("Database inconnue [{0}] ou sans password déclaré"));
		mfDic0.put("BS2NAME", new MessageFormat("Stockage S2 inconnu [{0}]"));
		mfDic0.put("BCRYPTOSTARTUP", new MessageFormat("Erreur d''initialisation de la classe Crypto"));
		mfDic0.put("XRESSOURCEABSENTE", new MessageFormat("Ressource [{0}] : non trouvée"));
		mfDic0.put("XRESSOURCEJSON", new MessageFormat("Ressource [{0}] : erreur de parse json [{1}]"));
		mfDic0.put("XRESSOURCEPARSE", new MessageFormat("Ressource [{0}] : erreur de parse [{1}]"));
		mfDic0.put("XRESSOURCEMSGFMT", new MessageFormat("Ressource [{0}] : erreur MessageFormat [{1}]"));
		mfDic0.put("XRESSOURCECLASS", new MessageFormat("Classe [{0}] non trouvée ou non instantiable"));
		mfDic0.put("XAPPCONFIG", new MessageFormat("Classe [{0}] de configuration application : non trouvée ou non instantiable ou sans méthode static startup(Object)"));
		mfDic0.put("XDBPROVIDERFACTORY", new MessageFormat("Classe [{0}] DBProvider : non trouvée ou sans méthode static getProvider(String)"));
				
		mfDic1.put("BDBNAME", new MessageFormat("Unknown database [{0}] or without declared password"));
		mfDic1.put("BS2NAME", new MessageFormat("Unknown S2 storage [{0}]"));
		mfDic1.put("BCRYPTOSTARTUP", new MessageFormat("Initialization error of Crypto class"));
		mfDic1.put("XRESSOURCEABSENTE", new MessageFormat("Resource [{0}] : not found"));
		mfDic1.put("XRESSOURCEJSON", new MessageFormat("Resource [{0}] : json parse error [{0}]"));
		mfDic1.put("XRESSOURCEPARSE", new MessageFormat("Resource [{0}] : parse error [{1}]"));
		mfDic1.put("XRESSOURCEMSGFMT", new MessageFormat("Resource [{0}] : MessageFormat error [{1}]"));
		mfDic1.put("XRESSOURCECLASS", new MessageFormat("Class [{0}] : not found / cannot instantiate"));
		mfDic1.put("XAPPCONFIG", new MessageFormat("Class [{0}] of application configuration : not found / cannot instantiate / not having a static startup(Object) method"));
		mfDic1.put("XDBPROVIDERFACTORY", new MessageFormat("Class [{0}] DBProvider : not found or not having a static getProvider(String) method"));
	}
		
	public static final String format(String code, String... args) { return _format(ExecContext.current().iLang(), code, args); }
	public static final String formatLang(String lang, String code, String... args) { return _format(lang(lang), code, args); }
	public static final String label(String code) { return _label(ExecContext.current().iLang(), code); }
	public static final String label(String lang, String code) { return _label(lang(lang), code); }

	private static class DefMsg {
		@SuppressWarnings("unused") public String code;
		@SuppressWarnings("unused") public String[] args;
		public DefMsg(String code, String[] args) { this.code = code; this.args = args; }
	}

	private static final String _format(int il, String code, String... args) {
		MessageFormat mf = mfDics[il].get(code);
		if (mf == null && il != 0) mf = mfDics[0].get(code);
		return mf == null ? JSON.toJson(new DefMsg(code, args)) : mf.format(args);
	}

	private static final String _label(int il, String code) {
		MessageFormat mf = mfDics[il].get(code);
		if (mf == null && il != 0) mf = mfDics[0].get(code);
		return mf == null ? JSON.toJson(new DefMsg(code, null)) : mf.toPattern();
	}

	private static void setMF() throws ServletException{
		mfDics = new MFDic[g.langs.length];
		mfDics[0] = mfDic0;
		if (g.langs.length > 1) mfDics[1] = mfDic1;
		for (int i = 2; i < g.langs.length; i++) mfDics[i] = new MFDic();
		for(String lang : g.langs) {
			setMF(lang, "/WEB-INF/app-msg-" + lang + ".json");
			setMF(lang, "/WEB-INF/base-msg-" + lang + ".json");
		}
	}
	
	private static void setMF(String lang, String n) throws ServletException {
		Servlet.Resource r = Servlet.getResource(n);
		if (r != null) {
			try {
				TxtDic d = (TxtDic)JSON.fromJson(r.toString(), TxtDic.class);
				MFDic dic = mfDics[lang(lang)];
				for(String k : d.keySet())
					try { if (!dic.containsKey(k)) dic.put(k,  new MessageFormat(d.get(k)));
					} catch (Exception ex) { throw exc(ex, _format(0, "XRESSOURCEMSGFMT", n, k)); }
			} catch (Exception ex) { throw exc(ex, _format(0, "XRESSOURCEJSONPARSE", n, ex.getMessage())); }
		}
	}
	
	/*******************************************************************************/
	public static class Nsqm {
		String code;
		boolean isQM;
		String label;
		String url;
		String base;
		private String pwd;
		
		String qm;
		String theme;
		HashMap<String,String> options;
		int build;
		int off;
		
		int[] threads;
		int scanlapseinseconds;
		int[] retriesInMin;
		
		public String label() { return label != null && label.length() != 0 ? label : code; }
		public boolean isQM() { return isQM; }
		public String url() { return url != null && url.length() != 0 ? url : g.defaultUrl; }
		public String base() { return base != null && base.length() != 0 ? base : g.defaultBase; }
		public String pwd() {
			if (pwd == null || pwd.length() == 0) return "";
			String px = p == null ? null : p.get(pwd);
			return px != null ? px : pwd;
		}
		
		public String qm() { return qm; }
		public String theme() { return theme != null || theme.length() != 0 ? theme : "a"; }
		public int build() { return build; }
		public int off() { return off; }
		public String[] options() { return options == null || options.size() == 0 ? new String[0] : options.keySet().toArray(new String[options.size()]); }
		public String option(String opt) { return options == null ? null : options.get(opt); }
		
		public int[] threads() { return threads; }
		public int scanlapseinseconds() { return scanlapseinseconds; }
		public int[] retriesInMin() { return retriesInMin; }
		
	}

	public static class S2Storage {
		private String blobsroot;
		private String bucket;
		public String blobsroot() { return blobsroot; }
		public String bucket() { return bucket; }
		public S2Storage() {}
		public S2Storage(String blobsroot, String bucket) { this.blobsroot = blobsroot; this.bucket = bucket; }
	}
	
	public static class Mailer {
		public String name;
		public String host;
		public int port;
		public String username;
		public String from;
		public boolean starttls;
		public boolean auth;
		public boolean isDefault;
		private String pwd;
		public String pwd() {
			if (pwd == null || pwd.length() == 0) return "";
			String px = p == null ? null : p.get(pwd);
			return px != null ? px : pwd;
		}
	}
	
//	protected static class Passwords {
//		private HashMap<String,String> passwords;
//	}

	private static class BaseConfig {
		private int 		build;
		private String 		instance = "?";
		private String 		zone = "Europe/Paris";
		private String[] 	langs = defaultLangs;
		private String 		dbProviderClass = "fr.cryptonote.provider.ProviderPG";
		private String 		defaultBase = "defaultBase";
		private String 		appConfigClass = "fr.cryptonote.app.config";
		private boolean 	isDebug = false;
		private boolean 	isDistrib = true;
		private boolean 	isMonoServer = false;
		private String 		defaultUrl = "http://localhost:8080/";
		private String 		byeAndBack = "https://test.sportes.fr:8443/byeAndBack";
		
		private int 		TASKMAXTIMEINSECONDS = 1800;
		private int 		OPERATIONMAXTIMEINSECONDS = 120;
		private int 		CACHEMAXLIFEINMINUTES = 120;
		private int 		CACHEMAXSIZEINMO = 50;
		private int 		CACHECLEANUPLAPSEMINUTES = 5;
		private int 		NSSCANPERIODINSECONDS = 30;
		private int 		S2CLEANUPPERIODINHOURS = 4;
		private int 		DTIMEDELAYINHOURS = 24 * 8;
		
		private HashMap<String,Nsqm> 		queueManagers;
		private HashMap<String,Nsqm> 		namespaces;
		private HashMap<String,S2Storage> 	s2Storages;
		private HashMap<String,Mailer> 		mailers;
		
		private String 		mailServer = "simu://alterconsos.fr/tech/server2.php";
		private String 		adminMails = "daniel@sportes.fr,domi.colin@laposte.net";
		private String[] 	emailFilter = {"sportes.fr"};
		
		private String[] 	offlinepages;
		private HashMap<String,String> 		shortcuts;
	}

	public static int 			build() { return g.build; }
	public static String 		instance() { return g.instance; }
	public static String 		zone() { return g.zone; }
	public static TimeZone 		timeZone() { return TimeZone.getTimeZone(g.zone); }
	public static String[] 		langs() { return g.langs; }
	public static int 			lang(String lang) {	for(int i = 0; i < g.langs.length; i++) if (g.langs[i].equals(lang)) return i;	return 0; }
	public static String 		lang(int iLang) { return iLang < 0 || iLang >= g.langs.length ? g.langs[0] : g.langs[iLang]; }
	public static String 		defaultLang() { return g.langs[0]; }
	public static boolean 		isDistrib() { return g.isDistrib;}
	public static boolean 		isDebug() { return g.isDebug;}
	public static boolean 		isMonoServer() { return g.isMonoServer;}
	public static String 		defaultUrl() { return g.defaultUrl; }
	public static String 		byeAndBack() { return g.byeAndBack; }
	public static String 		defaultBase() { return g.defaultBase; }

	public static int 			TASKMAXTIMEINSECONDS() { return g.TASKMAXTIMEINSECONDS;}
	public static int 			OPERATIONMAXTIMEINSECONDS() { return g.OPERATIONMAXTIMEINSECONDS;}
	public static int 			CACHEMAXLIFEINMINUTES() { return g.CACHEMAXLIFEINMINUTES;}
	public static int 			CACHEMAXSIZEINMO() { return g.CACHEMAXSIZEINMO;} 
	public static int 			CACHECLEANUPLAPSEMINUTES() { return g.CACHECLEANUPLAPSEMINUTES;}
	public static int 			NSSCANPERIODINSECONDS() { return g.NSSCANPERIODINSECONDS; }
	public static int 			S2CLEANUPPERIODINHOURS() { return g.S2CLEANUPPERIODINHOURS; }
	public static int 			DTIMEDELAYINHOURS() { return g.DTIMEDELAYINHOURS; }

	public static String 		mailServer() { return g.mailServer; }
	public static String 		adminMails() { return g.adminMails; }
	public static String[] 		emailFilter() { return g.emailFilter == null ? new String[0] : g.emailFilter; };
	public static Mailer		mailer(String code) { return g.mailers == null ? null : g.mailers.get(code); }
	
	public static String[] offlinepages() { return g.offlinepages == null ? new String[0] : g.offlinepages ;}
	public static String shortcut(String s){ return g.shortcuts != null ? g.shortcuts.get((s == null || s.length() == 0) ? "(empty)" : s) : null; }
		
	public static String[]		namespaces() { return g.namespaces.keySet().toArray(new String[g.namespaces.size()]);}
	public static String[]		queueManagers() { return g.queueManagers == null ? new String[0] : g.queueManagers.keySet().toArray(new String[g.queueManagers.size()]);}
	
	public static Nsqm namespace(String ns, boolean fresh) { 
		Nsqm x = g.namespaces.get(ns);
		return x != null && fresh ? NS.fresh(x) : x;
	}
	public static Nsqm queueManager(String ns, boolean fresh) { 
		Nsqm x = g.queueManagers == null ? null : g.queueManagers.get(ns); 
		return x != null && fresh ? NS.fresh(x) : x;
	}
	public static Nsqm nsqm(String nsqm, boolean fresh) { 
		Nsqm x = namespace(nsqm, fresh); 
		return x != null ? x : queueManager(nsqm, fresh);
	}
	
	/***************************************************************************************/
	private static BaseConfig g;
	private static HashMap<String,String> p;
	private static HashMap<String,ArrayList<String>> namespacesByDB = new HashMap<String,ArrayList<String>>();
	private static HashSet<String> databases = new HashSet<String>();
	private static String dbProviderName;
	private static Method dbProviderFactory;
	
	static ServletException exc(Exception e, String msg) {
		if (e != null) msg += "\n" + e.getMessage() + "\n" + Util.stack(e);
		Util.log.log(Level.SEVERE, msg);
		return new ServletException(msg);
	}
	
	private static boolean ready = false;
	@SuppressWarnings("unchecked")
	static void startup() throws ServletException {	
		if (ready) return;
		try { Crypto.startup(); } catch (Exception e) { throw exc(null, _format(0, "BCRYPTOSTARTUP")); }
		MimeType.init();
		
		Servlet.Resource r = Servlet.getResource(BASECONFIG);
		if (r != null) {
			try {
				g = (BaseConfig)JSON.fromJson(r.toString(), BaseConfig.class);
			} catch (Exception ex) {
				throw exc(ex, _format(0, "XRESSOURCEJSON", BASECONFIG));
			}
			if (g.langs == null || g.langs.length == 0) g.langs = defaultLangs;
			setMF();
		} else
			throw exc(null, _format(0, "XRESSOURCEABSENTE", BASECONFIG));

		try {
			Servlet.Resource rb = Servlet.getResource(BUILDJS);
				if (rb != null) {
				String s = rb.toString();
				int i = s.indexOf("=");
				if (i == -1 || i == s.length() - 1) throw exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "1"));
				try { g.build = Integer.parseInt(s.substring(i+ 1)); } catch (Exception e) { throw exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "2"));}
			}
			else 
				throw exc(null, _format(0, "XRESSOURCEABSENTE", BUILDJS));
		} catch (Exception e) {
			throw exc(e, _format(0, "XRESSOURCEABSENTE", BUILDJS));
		}

		checkNsqm();
		
		g.instance = System.getProperty(DVARINSTANCE);
		
		r = Servlet.getResource(PASSWORDS);
		if (r != null) {
			try {
				p = (HashMap<String,String>)JSON.fromJson(Util.fromUTF8(r.bytes), HashMap.class);
			} catch (Exception ex) {
				throw exc(ex, _format(0, "XRESSOURCEJSON", PASSWORDS));
			}
		} else 
			throw exc(null, _format(0, "XRESSOURCEABSENTE", PASSWORDS));
		
		try {
			int i = g.dbProviderClass.lastIndexOf(".");
			dbProviderName = g.dbProviderClass.substring(i + 1);
			Class<?> c = Class.forName(g.dbProviderClass);
			try {
				Method dbProviderFactory = c.getMethod("getProvider", String.class, String.class);
				if (!Modifier.isStatic(dbProviderFactory.getModifiers())) 
					throw exc(null, _format(0, "XDBPROVIDERFACTORY", g.dbProviderClass));
			} catch (Exception e) {
				throw exc(e, _format(0, "XDBPROVIDERFACTORY", g.dbProviderClass));				
			}
		} catch (Exception e) {
			throw exc(e, _format(0, "XRESSOURCECLASS", g.dbProviderClass));
		}
		
		compile();

		try {
			Class<?> appCfg = Class.forName(g.appConfigClass);
			r = Servlet.getResource(APPCONFIG);
			if (r != null) {
				Object appConfig;
				try {
					appConfig = JSON.fromJson(r.toString(), appCfg);
				} catch (Exception ex) {
					throw exc(ex, _format(0, "XRESSOURCEJSON", APPCONFIG));
				}
				try {
					Method startup = appCfg.getMethod("startup", Object.class);
					if (!Modifier.isStatic(startup.getModifiers())) 
						throw exc(null, _format(0, "XAPPCONFIG", g.appConfigClass));
					startup.invoke(appConfig); 
				} catch (Exception e) {
					throw exc(e, _format(0, "XAPPCONFIG", g.appConfigClass));				
				}
			} else
				throw exc(null, _format(0, "XRESSOURCEABSENTE", APPCONFIG));
		} catch (Exception e) {
			throw exc(e, _format(0, "XRESSOURCECLASS", g.appConfigClass));
		}
		ready = true;	
	}
			
	/****************************************************/
	private static void checkNsqm() throws ServletException {
		if (g.namespaces == null || g.namespaces.size() == 0)
			throw exc(null, _format(0, "XNAMESPACENO"));
		for(String ns : g.namespaces.keySet()) {
			Nsqm x = g.namespaces.get(ns);
			x.isQM = false;
			x.code = ns;
			if (x.pwd == null || x.pwd.length() == 0) throw exc(null, _format(0, "XNAMESPACEPWD", ns));
			if (x.base != null && x.base.length() != 0) {
				databases.add(x.base);
				ArrayList<String> al = namespacesByDB.get(x.base);
				if (al == null) {
					al = new ArrayList<String>();
					namespacesByDB.put(x.base, al);
				}
				al.add(ns);
			}
		}
		for(String qm : g.queueManagers.keySet()) {
			Nsqm x = g.queueManagers.get(qm);
			x.isQM = true;
			x.code = qm;
			if (x.pwd == null || x.pwd.length() == 0) throw exc(null, _format(0, "XQMPWD", qm));			
			if (x.base != null && x.base.length() != 0) databases.add(x.base);
		}
	}
	
	/****************************************************/
	public static ArrayList<String> namespacesByDB(String db) { return namespacesByDB.get(db); }
	public static String dbProviderName() { return dbProviderName; }
	public static String[] databases() { return databases.toArray(new String[databases.size()]); }
	public static boolean hasDatabase(String n) { return databases.contains(n); }

	public static final DBProvider getDBProvider(String db) throws AppException {
		try {
			if (!databases.contains(db)) throw new AppException("BDBNAME");
			String pwd = p.get(db);
			if (pwd == null) throw new AppException("BDBNAME");
			return (DBProvider) dbProviderFactory.invoke(db, pwd);
		} catch (Exception e) {
			throw new AppException(e, "XDBPROVIDERFACTORY", g.dbProviderClass);
		}
	}

	public static final S2Storage s2Storage(String db) throws AppException {
		S2Storage s = g.s2Storages.get(db);
		if (s == null) throw new AppException("BS2NAME", db);
		return s;
	}
	
	/****************************************************/
	private static final String[] ignore = {"bower.json", ".gitignore", ".yml", ".md", ".log", "Makefile", "index.html", ".js.map", "LICENSE.txt"};
	public static class ResFilter {

		public boolean filterDir(String dir, String name) {
			if (!g.isDistrib)
				return !(dir.startsWith("/var/bower_components/iron-flex-layout/classes/") ||
						(dir.startsWith("/var/bower_components/") &&
							(name.equals("test/") || name.equals("demo/") || name.equals("/.github/"))));
			else
				return !(dir.startsWith("/var/bower_components/") ||
						dir.startsWith("/var/app_components/") ||
						dir.startsWith("/var/base_components/"));
		}

		public boolean filterFile(String fullpath) {
			for(String x : ignore)
				if (fullpath.endsWith(x)) 
					return false;
			return true;
		}

	}
	
	/****************************************************/

	private static void compile() throws ServletException {	
		// TODO : Compiler tout 

		
		// déclarer Documents et opérations
//		DocumentDescr.register(NS.NsDoc.class);
//		Operation.register(NS.NSCfg.class);
//		Operation.register(NS.NSRes.class);
//		Operation.register(NS.NSExport.class);
//		DocumentDescr.register(S2Cleanup.class);
//				
//		Operation.register(Document.ListGroups.class);
//		// Operation.register(Document.ReschedTask.class);
//		// Operation.register(Document.DocsInfo.class);
//		// Operation.register(Document.ExportDoc.class);
//		// Operation.register(Document.ImportDoc.class);
//		Operation.register(Document.ImportPart.class);
//		
//		Operation.register(Mailer.SendMail.class);
		

	}

}
