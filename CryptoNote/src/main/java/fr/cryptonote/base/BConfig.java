package fr.cryptonote.base;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import javax.servlet.ServletException;

import fr.cryptonote.base.QueueManager.CandidateTasks;
import fr.cryptonote.base.QueueManager.ErrTasks;
import fr.cryptonote.base.QueueManager.DetailTask;
import fr.cryptonote.base.QueueManager.ParamTask;
import fr.cryptonote.base.QueueManager.TraceTasks;
import fr.cryptonote.provider.DBProvider;

public class BConfig {
	
	private static final String[] defaultLangs = {"fr", "en"};
	private static final String DVARQM = "fr.cryptonote.QM";
	private static final String BASECONFIG = "/WEB-INF/base-config.json";
	private static final String APPCONFIG = "/WEB-INF/app-config.json";
	private static final String PASSWORDS = "/WEB-INF/passwords.json";
	private static final String GENDICS = "/var/js/";
	private static final String APPDICS = "/app-msg.js";
	private static final String BASEDICS = "/base-msg.js";
	private static final String BUILDJS = "/var/js/build.js";
	static final String SWJS = "/var/js/sw.js";
	static final String INDEX = "/var/index.html";

	static class TxtDic extends HashMap<String,String> { 
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
		
	public static final String format(String code, String... args) { ExecContext ec = ExecContext.current(); return _format(ec.iLang(), ec.nsqm(), code, args); }
	public static final String formatLang(String lang, String code, String... args) { ExecContext ec = ExecContext.current(); return _format(lang(lang), ec.nsqm(), code, args); }
	public static final String label(String code) { ExecContext ec = ExecContext.current(); return _label(ec.iLang(), ec.nsqm(), code); }
	public static final String label(String lang, String code) { ExecContext ec = ExecContext.current(); return _label(lang(lang), ec.nsqm(), code); }

	private static class DefMsg {
		@SuppressWarnings("unused") public String code;
		@SuppressWarnings("unused") public String[] args;
		public DefMsg(String code, String[] args) { this.code = code; this.args = args; }
	}

	private static MessageFormat mf(int il, Nsqm nsqm, String code) {
		MessageFormat mf = null;
		if (nsqm != null) {
			MFDic d = nsqm.mfDics[il];
			mf = d == null ? null : d.get(code);
			if (mf != null) return mf;
			if (il != 0) {
				d = nsqm.mfDics[0];
				mf = d == null ? null : d.get(code);
				if (mf != null) return mf;
			}
		}
		mf = mfDics[il].get(code);
		return mf != null ? mf : mfDics[0].get(code);
	}
	
	static final String _format(int il, String code, String... args) { return _format(il, null, code, args);}
	private static final String _format(int il, Nsqm nsqm, String code, String... args) {
		MessageFormat mf = mf(il, nsqm, code);
		return mf == null ? JSON.toJson(new DefMsg(code, args)) : mf.format(args);
	}

	static final String _label(int il, String code, String... args) { return _label(il, null, code);}
	static final String _label(int il, Nsqm nsqm, String code) {
		MessageFormat mf = mf(il, nsqm, code);
		return mf == null ? JSON.toJson(new DefMsg(code, null)) : mf.toPattern();
	}

	private static void setMF() throws ServletException{
		mfDics = new MFDic[g.langs.length];
		mfDics[0] = mfDic0;
		if (g.langs.length > 1) mfDics[1] = mfDic1;
		for (int i = 2; i < g.langs.length; i++) mfDics[i] = new MFDic();
		for(String lang : g.langs) {
			setMF(mfDics, lang, GENDICS + lang + APPDICS);
			setMF(mfDics, lang, GENDICS + lang + BASEDICS);
		}
	}
	
	private static void setMF(MFDic[] mfDics, String lang, String n) throws ServletException {
		if (Servlet.hasVar(n)) {
			TxtDic d = (TxtDic)Servlet.script2json(n, TxtDic.class);
			MFDic dic = mfDics[lang(lang)];
			for(String k : d.keySet()) {
				String s = d.get(k);
				try { if (!dic.containsKey(k)) dic.put(k,  new MessageFormat(s));
				} catch (Exception ex) { throw Servlet.exc(ex, _format(0, "XRESSOURCEMSGFMT", n, k)); }
			}
		}
	}
	
	private static String normUrl(String ns){
		String s = g.url;
		if (!s.endsWith("/")) s += "/";
		String cp = Servlet.contextPath();
		if (cp != null && cp.length() != 0) {
			s += cp;
			if (!cp.endsWith("/")) s +=  "/";
		}
		return ns == null ? s : s + ns + "/";
	}
	
	public static class HelpPage {
		String p;		// précédente dans la section (null si première)
		String t;		// page tête de la section (null pour home)
		String s;		// page suivante dans la section (null si dernière)
		String[] refs; 	// pages référencées
	}
	
	
	/*******************************************************************************/
	public static class Nsqm {
		int 	onoff;

		String 	code;
		boolean isQM;
		String 	base;
		String 	pwd;
		
		String 	qm;
		String 	theme;
		String 	lang;
		HashMap<String,String> options;
		HashMap<String,Integer> homes;
		HashMap<String,HelpPage> help;
		MFDic[] mfDics;
		
		int[] 	threads;
		int 	scanlapseinseconds;
		
		public boolean isQM() { return isQM; }
		public String base() { return base != null && base.length() != 0 ? base : g.defaultBase; }
		public String lang() { return g.langs[BConfig.lang(lang)]; }
		public String url() { return normUrl(code); }
		public String pwd() {
			if (pwd == null || pwd.length() == 0) return "";
			String px = p == null ? null : p.get(pwd);
			return px != null ? px : pwd;
		}
		
		public String qm() { return qm; }
		public String theme() { return theme != null || theme.length() != 0 ? theme : "a"; }
		public int onoff() { return onoff; }
		public String[] options() { return options == null || options.size() == 0 ? new String[0] : options.keySet().toArray(new String[options.size()]); }
		public String option(String opt) { return options == null ? null : options.get(opt); }
		public int modeMax(String home) { Integer m = homes.get(home); return m == null ? -1 : m; }
		public ArrayList<String> homes(int min) { 
			ArrayList<String> res = new ArrayList<String>();
			for(String h : homes.keySet()) 
				if (homes.get(h) >= min) res.add(h);
			return res;
		}
		public HashMap<String,Integer> homes() { return homes;}
		
		public HashMap<String,HelpPage> help() { 
			return help == null ? g.help : help; 
		}
		
		public int[] threads() { return threads; }
		public int scanlapseinseconds() { return scanlapseinseconds; }
		
		public boolean isPwd(String key) {
			String sha = Servlet.SHA256b64(key, false);
			return sha != null && sha.equals(pwd());
		}
		
		private void compile() throws ServletException {
			if (help != null)
				for(String p : g.help.keySet())
					if (help.get(p) == null) help.put(p, g.help.get(p));
			mfDics = new MFDic[g.langs.length];
			for(int i = 0; i < g.langs.length; i++) {
				String lg = g.langs[i];
				setMF(mfDics, lg, "z/" + code + "/" + lg + "/" + BASEDICS);
				setMF(mfDics, lg, "z/" + code + "/" + lg + "/" + APPDICS);
			}
			if (base != null && base.length() != 0) g.bases.add(base);
		}
	}

	public static class S2Storage {
		private String blobsroot;
		private String bucket;
		public String blobsroot() { return blobsroot; }
		public String bucket() { return bucket; }
		public S2Storage() {}
		public S2Storage(String blobsroot, String bucket) { this.blobsroot = blobsroot; this.bucket = bucket; }
	}
	
	public static class MailerCfg {
		String name;
		String host;
		int port;
		String username;
		String from;
		boolean starttls;
		boolean auth;
		boolean isDefault;
		private String pwd;
		public String pwd() {
			if (pwd == null || pwd.length() == 0) return "";
			String px = p == null ? null : p.get(pwd);
			return px != null ? px : pwd;
		}
	}
	
	private static class BaseConfig {
		private int 		build;
		private String 		QM = null;
		private String 		dbProviderClass = "fr.cryptonote.provider.ProviderPG";
		private String 		appConfigClass = "fr.cryptonote.app.config";
		private String 		zone = "Europe/Paris";
		private String[] 	langs = defaultLangs;
		private String 		defaultBase = "defaultBase";
		private boolean 	isDebug = false;
		private boolean 	isDistrib = true;
		private boolean 	isMonoServer = false;
		private String 		url = "http://localhost:8080/";
		
		private int 		TASKMAXTIMEINSECONDS = 1800;
		private int			TASKLOSTINMINUTES = 30;
		private int 		OPERATIONMAXTIMEINSECONDS = 120;
		private int 		CACHEMAXLIFEINMINUTES = 120;
		private int 		CACHEMAXSIZEINMO = 50;
		private int 		CACHECLEANUPLAPSEMINUTES = 5;
		private int 		NSSCANPERIODINSECONDS = 30;
		private int 		S2CLEANUPPERIODINHOURS = 4;
		private int 		DTIMEDELAYINHOURS = 24 * 8;
		private int[] 		TASKRETRIESINMIN = {1, 10, 60, 180};
		
		private HashSet<String>				bases = new HashSet<String>();
		private HashMap<String,Nsqm> 		queueManagers;
		private HashMap<String,Nsqm> 		namespaces;
		private HashMap<String,S2Storage> 	s2Storages;
		private HashMap<String,MailerCfg> 	mailers;
		private HashMap<String,HelpPage>	help;
		private String[]					themes;
		
		private String 		mailServer = "simu://alterconsos.fr/tech/server2.php";
		private String 		adminMails = "daniel@sportes.fr,domi.colin@laposte.net";
		private String[] 	emailFilter = {"sportes.fr"};
		
		private HashMap<String,String> 		shortcuts;
	}

	public static int 			build() { return g.build; }
	public static String 		QM() { return g.QM; }
	public static String 		zone() { return g.zone; }
	public static TimeZone 		timeZone() { return TimeZone.getTimeZone(g.zone); }
	public static String[] 		langs() { return g.langs; }
	public static int 			lang(String lang) {	for(int i = 0; i < g.langs.length; i++) if (g.langs[i].equals(lang)) return i;	return 0; }
	public static String 		lang(int iLang) { return iLang < 0 || iLang >= g.langs.length ? g.langs[0] : g.langs[iLang]; }
	public static String 		defaultLang() { return g.langs[0]; }
	public static boolean 		isDistrib() { return g.isDistrib;}
	public static boolean 		isDebug() { return g.isDebug;}
	public static boolean 		isMonoServer() { return g.isMonoServer;}
	public static String 		url() { return normUrl(g.url); }
	public static String 		defaultBase() { return g.defaultBase; }

	public static int 			TASKMAXTIMEINSECONDS() { return g.TASKMAXTIMEINSECONDS;}
	public static int			TASKLOSTINMINUTES() { return g.TASKLOSTINMINUTES; }
	public static int 			OPERATIONMAXTIMEINSECONDS() { return g.OPERATIONMAXTIMEINSECONDS;}
	public static int 			CACHEMAXLIFEINMINUTES() { return g.CACHEMAXLIFEINMINUTES;}
	public static int 			CACHEMAXSIZEINMO() { return g.CACHEMAXSIZEINMO;} 
	public static int 			CACHECLEANUPLAPSEMINUTES() { return g.CACHECLEANUPLAPSEMINUTES;}
	public static int 			NSSCANPERIODINSECONDS() { return g.NSSCANPERIODINSECONDS; }
	public static int 			S2CLEANUPPERIODINHOURS() { return g.S2CLEANUPPERIODINHOURS; }
	public static int 			DTIMEDELAYINHOURS() { return g.DTIMEDELAYINHOURS; }
	public static long 			RETRYSTARTAT(int retry) { 
		return retry < 0 ? Stamp.minStamp.stamp() : (retry >= g.TASKRETRIESINMIN.length ? Stamp.minStamp.stamp() : Stamp.fromNow(g.TASKRETRIESINMIN[retry] * 60000).stamp());
	}

	public static String 		mailServer() { return g.mailServer; }
	public static String 		adminMails() { return g.adminMails; }
	public static String[] 		emailFilter() { return g.emailFilter == null ? new String[0] : g.emailFilter; };
	public static MailerCfg		mailer(String code) { return g.mailers == null ? null : g.mailers.get(code); }
	public static String[] 		mailers() { return g.mailers == null ? new String[0] : g.mailers.keySet().toArray(new String[g.mailers.size()]);}
	public static String 		shortcut(String s){ return g.shortcuts != null ? g.shortcuts.get((s == null || s.length() == 0) ? "(empty)" : s) : null; }		
	public static String[]		namespaces() { return g.namespaces.keySet().toArray(new String[g.namespaces.size()]);}
	public static String[]		queueManagers() { return g.queueManagers == null ? new String[0] : g.queueManagers.keySet().toArray(new String[g.queueManagers.size()]);}
	public static String		password(String code) { return p == null ? null : p.get(code); }
	public static int			queueIndexByOp(String op) { return appConfig.queueIndexByOp(op); }
	
	public static String[]		themes() { return g.themes; };
	
	private static String[] 	dbases;
	public static String[] 		bases() {
		if (dbases == null)
			dbases = g.bases.toArray(new String[g.bases.size()]);
		return dbases;
	}
	
	public static Nsqm namespace(String ns, boolean fresh) { 
		Nsqm x = g.namespaces.get(ns);
		if (fresh && x != null) x.onoff = OnOff.status(ns, false);
		return x;
	}
	public static Nsqm queueManager(String ns, boolean fresh) { 
		Nsqm x = g.queueManagers == null ? null : g.queueManagers.get(ns); 
		if (fresh && x != null) x.onoff = OnOff.status(ns, false);
		return x;
	}
	public static Nsqm nsqm(String nsqm, boolean fresh) { 
		Nsqm x = namespace(nsqm, fresh); 
		if (x == null) return null;
		if (fresh)
			x.onoff = OnOff.status(nsqm, false);
		return x;
	}
	
	/***************************************************************************************/
	private static BaseConfig g = new BaseConfig();
	private static HashMap<String,String> p;
	private static HashMap<String,ArrayList<Nsqm>> namespacesByDB = new HashMap<String,ArrayList<Nsqm>>();
	private static HashMap<String,ArrayList<Nsqm>> namespacesByQM = new HashMap<String,ArrayList<Nsqm>>();
	private static HashSet<String> databases = new HashSet<String>();
	private static String dbProviderName;
	private static Method dbProviderFactory;
	private static IConfig appConfig;
			
	@SuppressWarnings("unchecked")
	static void startup() throws ServletException {			
		g = (BaseConfig)Servlet.script2json(BASECONFIG, BaseConfig.class);
		if (g.langs == null || g.langs.length == 0) g.langs = defaultLangs;
		setMF();
		
		try {
			Servlet.Resource rb = Servlet.getResource(BUILDJS);
				if (rb != null) {
				String s = rb.toString();
				int i = s.indexOf("=");
				if (i == -1 || i == s.length() - 1) throw Servlet.exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "1"));
				int j = s.indexOf(";", i + 2);
				if (j == -1) throw Servlet.exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "2"));
				String x = s.substring(i + 1, j).trim();
				try { g.build = Integer.parseInt(x); } catch (Exception e) { throw Servlet.exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "3"));}
			}
			else 
				throw Servlet.exc(null, _format(0, "XRESSOURCEABSENTE", BUILDJS));
		} catch (Exception e) {
			throw Servlet.exc(e, _format(0, "XRESSOURCEABSENTE", BUILDJS));
		}

		checkNsqm();
		
		g.QM = System.getProperty(DVARQM);
		
		p = (HashMap<String,String>)Servlet.script2json(PASSWORDS, HashMap.class);
		
		try {
			int i = g.dbProviderClass.lastIndexOf(".");
			dbProviderName = g.dbProviderClass.substring(i + 1);
			Class<?> c = Class.forName(g.dbProviderClass);
			try {
				dbProviderFactory = c.getMethod("getProvider", String.class, String.class);
				if (!Modifier.isStatic(dbProviderFactory.getModifiers())) 
					throw Servlet.exc(null, _format(0, "XDBPROVIDERFACTORY", g.dbProviderClass));
			} catch (Exception e) {
				throw Servlet.exc(e, _format(0, "XDBPROVIDERFACTORY", g.dbProviderClass));				
			}
		} catch (Exception e) {
			throw Servlet.exc(e, _format(0, "XRESSOURCECLASS", g.dbProviderClass));
		}
		
		compile();

		Class<?> appCfg = Util.hasClass(g.appConfigClass, IConfig.class);
		if (appCfg == null) throw Servlet.exc(null, _format(0, "XRESSOURCECLASS", g.appConfigClass));
		appConfig = (IConfig)Servlet.script2json(APPCONFIG, appCfg);
		try {
			Method startup = appCfg.getMethod("startup", IConfig.class);
			if (!Modifier.isStatic(startup.getModifiers())) 
				throw Servlet.exc(null, _format(0, "XAPPCONFIG", g.appConfigClass));
			startup.invoke(null, appConfig); 
		} catch (Exception e) {
			throw Servlet.exc(e, _format(0, "XAPPCONFIG", g.appConfigClass));				
		}

		QueueManager.startQM();
	}
			
	/****************************************************/
	private static void checkNsqm() throws ServletException {
		if (g.namespaces == null || g.namespaces.size() == 0)
			throw Servlet.exc(null, _format(0, "XNAMESPACENO"));
		for(String ns : g.namespaces.keySet()) {
			Nsqm x = g.namespaces.get(ns);
			x.isQM = false;
			x.code = ns;
			if (x.pwd == null || x.pwd.length() == 0) throw Servlet.exc(null, _format(0, "XNAMESPACEPWD", ns));
			if (x.base != null && x.base.length() != 0) {
				databases.add(x.base);
				ArrayList<Nsqm> al = namespacesByDB.get(x.base);
				if (al == null) {
					al = new ArrayList<Nsqm>();
					namespacesByDB.put(x.base, al);
				}
				al.add(x);
			}
			ArrayList<Nsqm> al2 = namespacesByQM.get(x.qm);
			if (al2 == null) {
				al2 = new ArrayList<Nsqm>();
				namespacesByQM.put(x.qm, al2);
			}
			al2.add(x);
		}
		for(String qm : g.queueManagers.keySet()) {
			Nsqm x = g.queueManagers.get(qm);
			x.isQM = true;
			x.code = qm;
			if (x.pwd == null || x.pwd.length() == 0) throw Servlet.exc(null, _format(0, "XQMPWD", qm));			
			if (x.base != null && x.base.length() != 0) databases.add(x.base);
		}
	}
	
	/****************************************************/
	public static ArrayList<Nsqm> namespacesByQM(String qm) { return namespacesByQM.get(qm); }
	public static ArrayList<Nsqm> namespacesByDB(String db) { return namespacesByDB.get(db); }
	public static String dbProviderName() { return dbProviderName; }
	public static String[] databases() { return databases.toArray(new String[databases.size()]); }
	public static boolean hasDatabase(String n) { return databases.contains(n); }

	public static final DBProvider getDBProvider(String db) throws AppException {
		try {
			if (!databases.contains(db)) throw new AppException("BDBNAME");
			String pwd = p.get(db);
			if (pwd == null) throw new AppException("BDBNAME");
			return (DBProvider) dbProviderFactory.invoke(null, db, pwd);
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
//	private static final String[] ignore = {"bower.json", ".gitignore", ".yml", ".md", ".log", "Makefile", "index.html", ".js.map", "LICENSE.txt"};
//	public static class ResFilter {
//
//		public boolean filterDir(String dir, String name) {
//			if (!g.isDistrib)
//				return !(dir.startsWith("/var/bower_components/iron-flex-layout/classes/") ||
//						(dir.startsWith("/var/bower_components/") &&
//							(name.equals("test/") || name.equals("demo/") || name.equals("/.github/"))));
//			else
//				return !(dir.startsWith("/var/bower_components/") ||
//						dir.startsWith("/var/app_components/") ||
//						dir.startsWith("/var/base_components/"));
//			return true;
//		}
//
//		public boolean filterFile(String fullpath) {
//			for(String x : ignore)
//				if (fullpath.endsWith(x)) 
//					return false;
//			return true;
//		}
//
//	}
//	
	/****************************************************/

	private static void compile() throws ServletException {	
		for(Nsqm nsqm : g.namespaces.values()) nsqm.compile();		
		for(Nsqm nsqm : g.queueManagers.values()) nsqm.compile();
		
		boolean defMailer = false;
		for(MailerCfg m : g.mailers.values()) {
			if (m.isDefault) {
				if (defMailer) throw Servlet.exc(null, _format(0, "XMAILERCFG1"));
				defMailer = true;
			}		
		}
		if (!defMailer) throw Servlet.exc(null, _format(0, "XMAILERCFG2"));
		if (password("mailServer") == null) throw Servlet.exc(null, _format(0, "XMAILERCFG3"));
			
		try {
			// déclarer Documents et opérations
			Operation.register(OnOff.GetOnOff.class);
			Operation.register(OnOff.SetOnOff.class);
			Operation.register(OnOff.Sudo.class);
			Operation.register(OnOff.DbInfo.class);
			Operation.register(Mailer.SendMail.class);
			Operation.register(TraceTasks.class);
			Operation.register(ErrTasks.class);
			Operation.register(CandidateTasks.class);
			Operation.register(DetailTask.class);
			Operation.register(ParamTask.class);
		} catch (Exception e) {
			throw Servlet.exc(e, _format(0, "BCONFIGOP"));
		}
		

	}

}
