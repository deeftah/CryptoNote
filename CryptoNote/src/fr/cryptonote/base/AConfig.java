package fr.cryptonote.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.servlet.ServletException;

import fr.cryptonote.base.Document.Id;
import fr.cryptonote.provider.DBProvider;

public class AConfig {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/*******************************************************************************/
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
		mfDic0.put("XSERVLETCONFIG", new MessageFormat("La classe ConfigClass n'est pas donnée en paramètre dans web.xml"));
		mfDic0.put("XSERVLETCONFIGCLASS", new MessageFormat("La classe [{0}] donnée dans web.xml n'a pas de méthode static startup()"));
		mfDic0.put("XSERVLETCONFIGBUILD", new MessageFormat("Ressource /WEB-INF/config.json : build absente"));
		mfDic0.put("XSERVLETURL", new MessageFormat("URL mal formée (espace de noms [{0}] non reconnu)"));
		mfDic0.put("XSERVLETBASE", new MessageFormat("Base temporairement indisponible"));
		mfDic0.put("XSERVLETNS", new MessageFormat("URL mal formée (espace de noms [{0}] non reconnu)"));
		mfDic0.put("XSERVLETCLASS", new MessageFormat("La classe [{0}] doit exister et étendre (ou être) la classe [{1}]"));
		
		mfDic0.put("XRESSOURCEABSENTE", new MessageFormat("Ressource [{0}] : non trouvée"));
		mfDic0.put("XRESSOURCEJSON", new MessageFormat("Ressource [{0}] : erreur de parse json [{1}]"));
		mfDic0.put("XRESSOURCEPARSE", new MessageFormat("Ressource [{0}] : erreur de parse [{1}]"));
		mfDic0.put("XRESSOURCEMSGFMT", new MessageFormat("Ressource [{0}] : erreur MessageFormat [{1}]"));
		mfDic0.put("XRESSOURCECLASS", new MessageFormat("Classe [{0}] non trouvée ou non instantiable"));
		mfDic0.put("XAPPCONFIG", new MessageFormat("Classe [{0}] non trouvée ou non instantiable"));

		mfDic1.put("XRESSOURCEABSENTE", new MessageFormat("Resource [{0}] : not found"));
		mfDic1.put("XRESSOURCEJSON", new MessageFormat("Resource [{0}] : json parse error [{0}]"));
		mfDic0.put("XRESSOURCEPARSE", new MessageFormat("Resource [{0}] : parse error [{1}]"));
		mfDic1.put("XRESSOURCEMSGFMT", new MessageFormat("Resource [{0}] : MessageFormat error [{1}]"));
		mfDic0.put("XRESSOURCECLASS", new MessageFormat("Class [{0}] : not found / cannot instantiate"));
		mfDic0.put("XAPPCONFIG", new MessageFormat("Class [{0}] : not found / cannot instantiate"));
	}

	public static int _iLang(String lang) {
		if (lang != null && lang.length() != 0) {
			String[] lg = g.langs;
			for(int i = 0; i < lg.length; i++) if (lang.equals(lg[i])) return i;
		}
		return 0;
	}
		
	private static class DefMsg {
		@SuppressWarnings("unused") public String code;
		@SuppressWarnings("unused") public String[] args;
		public DefMsg(String code, String[] args) { this.code = code; this.args = args; }
	}

	public static final String _format(String code, String... args) { return _format(ExecContext.current().iLang(), code, args); }
	public static final String _formatLang(String lang, String code, String... args) { return _format(_iLang(lang), code, args); }
	public static final String _label(String code) { return _label(ExecContext.current().iLang(), code); }
	public static final String _label(String lang, String code) { return _label(_iLang(lang), code); }

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

	private static void setMF() throws Exception{
		mfDics = new MFDic[g.langs.length];
		mfDics[0] = mfDic0;
		if (g.langs.length > 1) mfDics[1] = mfDic1;
		for (int i = 2; i < g.langs.length; i++) mfDics[i] = new MFDic();
		for(String lang : g.langs) {
			setMF(lang, "/WEB-INF/app-msg-" + lang + ".json");
			setMF(lang, "/WEB-INF/base-msg-" + lang + ".json");
		}
	}
	
	private static void setMF(String lang, String n) throws Exception {
		Servlet.Resource r = Servlet.getResource(n);
		if (r != null) {
			try {
				TxtDic d = (TxtDic)JSON.fromJson(r.toString(), TxtDic.class);
				MFDic dic = mfDics[_iLang(lang)];
				for(String k : d.keySet())
					try { 
						if (!dic.containsKey(k))
							dic.put(k,  new MessageFormat(d.get(k)));
					} catch (Exception ex) { throw new Exception(_format(0, "XRESSOURCEMSGFMT", n, k)); }
			} catch (Exception ex) { throw new Exception(_format(0, "XRESSOURCEJSONPARSE", n, ex.getMessage())); }
		}
	}
	
	/*******************************************************************************/
	
	public static class QueueManager {
		public String url;
		public String base;
		public int[] threads;
		public int scanlapseinseconds;
		public int[] retriesInMin;
		public String pwd;
	}
	
	public static class Namespace {
		public String label;
		public String base;
		public String qm;
		public String url;
		public String pwd;
	}

	public static class S2Storage {
		public String blobsroot;
		public String bucket;
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
		public String pwd;
	}
	
	protected static class BaseConfig {
		private String build;
		private String instance = "?";
		private String zone = "Europe/Paris";
		private String[] langs = defaultLangs;
		private String dbProviderClass = "fr.cryptonote.provider.ProviderPG";
		private String appConfigClass = "fr.cryptonote.app.config";
		private boolean isDebug = false;
		private boolean isDistrib = true;
		private boolean isMonoServer = false;
		private String defaultUrl = "http://192.168.0.10:8090/cn";
		private String byeAndBack = "https://test.sportes.fr:8443/byeAndBack";
		
		private int TASKMAXTIMEINSECONDS = 1800;
		private int OPERATIONMAXTIMEINSECONDS = 120;
		private int CACHEMAXLIFEINMINUTES = 120;
		private int CACHEMAXSIZEINMO = 50;
		private int CACHECLEANUPLAPSEMINUTES = 5;
		private int NSSCANPERIODINSECONDS = 30;
		private int S2CLEANUPPERIODINHOURS = 4;
		private int DTIMEDELAYINHOURS = 24 * 8;
		
		private String[] ns;

		private HashMap<String,QueueManager> queueManagers;
		private HashMap<String,Namespace> namespaces;
		private HashMap<String,S2Storage> s2Storages;
		
		private String mailserver = "simu://alterconsos.fr/tech/server2.php";
		private String adminMails = "daniel@sportes.fr,domi.colin@laposte.net";
		private String[] emailfilter = {"sportes.fr"};
		private String[] offlinepages;
		private HashMap<String,String> shortcuts;
	}

	protected static class Passwords {
		private HashMap<String,String> passwords;
	}

	protected static AConfig config;
	public static AConfig config() { return config; }
		
	/** AConfig **********************************/
	private static BaseConfig g;
	private static Passwords p;

	private static String dbProviderName;
	private static String dbProviderConfig;
	private static Class<?> dbProviderClass;
	private static Constructor<?> dbProviderConstructor;

	private static Object appConfig;

	private ResFilter resFilter;
	
	
	private static Boolean ready = false;
	public static void startup(Class<?> genClass, Class<?> secretClass) throws Exception {	
		synchronized (ready) {
			if (ready) return;
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
				String s = rb.toString();
				int i = s.indexOf("\"");
				if (i == -1) throw exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "1"));
				int j = s.lastIndexOf("\"");
				if (j == -1) throw exc(null, _format(0, "XRESSOURCEPARSE", BUILDJS, "2"));
				g.build = s.substring(i+ 1, j);
			} catch (Exception e) {
				throw exc(e, _format(0, "XRESSOURCEABSENTE", BUILDJS));
			}

			g.instance = System.getProperty(DVARINSTANCE);
			
			r = Servlet.getResource(PASSWORDS);
			if (r != null) {
				try {
					p = (Passwords)JSON.fromJson(Util.fromUTF8(r.bytes), Passwords.class);
				} catch (Exception ex) {
					throw exc(ex, _format(0, "XRESSOURCEJSON", PASSWORDS));
				}
			} else 
				throw exc(null, _format(0, "XRESSOURCEABSENTE", PASSWORDS));
			
			try {
				String n = g.dbProviderClass;
				int i = n.lastIndexOf(".");
				dbProviderName = n.substring(i + 1);
				dbProviderClass = Class.forName(n);
				dbProviderConstructor = config.dbProviderClass.getConstructor(String.class);
			} catch (Exception e) {
				throw exc(e, _format(0, "XRESSOURCECLASS", g.dbProviderClass));
			}

			Class<?> appCfg = null;
			try {
				appCfg = Class.forName(g.appConfigClass);
				r = Servlet.getResource(APPCONFIG);
				if (r != null) {
					try {
						appConfig = JSON.fromJson(r.toString(), appCfg);
					} catch (Exception ex) {
						throw exc(ex, _format(0, "XRESSOURCEJSON", APPCONFIG));
					}
				} else
					throw exc(null, _format(0, "XRESSOURCEABSENTE", APPCONFIG));
			} catch (Exception e) {
				throw exc(e, _format(0, "XRESSOURCECLASS", g.appConfigClass));
			}

			// TODO : Complier tout
			
			declareDocumentsAndOperations();
			Method startup = null;
			try {
				startup = appCfg.getMethod("startup");
			} catch (Exception e) { err(new ServletException(AConfig._label("XSERVLETCONFIGCLASS", configClass.getSimpleName()))); }
			if (!Modifier.isStatic(startup.getModifiers())) 
				err(new ServletException(AConfig._label("XSERVLETCONFIGCLASS", configClass.getSimpleName())));
			try { 
				startup.invoke(null); 
			} catch (Exception e) { err(e); }

			try {
				appCfg.newInstance();
			} catch (Exception e) {
				throw exc(e, _format(0, "XAPPCONFIG", g.appConfigClass));				
			}
			ready = true;
		}
	}
	
	private static Exception exc(Exception e, String msg) {
		if (e != null) msg += " - " + e.getMessage();
		Util.log.log(Level.SEVERE, msg);
		return new Exception(msg);
	}
	
	public final int iLang(String lang) { return _iLang(lang); }
	public final String lang(int iLang) { return iLang < 0 || iLang >= gen.langs.length ? gen.langs[0] : gen.langs[iLang]; }
	public String byeAndBack() { return gen.byeAndBack; }
	public String build() { return gen.build; }
	public boolean distrib() { return gen.distrib;}
	public TimeZone timeZone() { return TimeZone.getTimeZone(gen.zone); }
	public String zone() { return gen.zone; }
	public String url() { return gen.url; }
	public String dbProviderName() { return dbProviderName; }
	public String dbProviderConfig() { return dbProviderConfig; }
	public String[] ns() { return gen.ns ;}
	public String nsz() { return gen.ns[0] ;}

	public static class DefaultNSSrvCfg {
		public HashMap<String,String> args;
	}

	public Object newNSSrvCfg() { return new DefaultNSSrvCfg(); }
	
	public String[] offlinepages() { return gen.offlinepages == null ? new String[0] : gen.offlinepages ;}
	public boolean MONOSERVER() { return gen.MONOSERVER;}
	public int TASKMAXTIMEINSECONDS() { return gen.TASKMAXTIMEINSECONDS;}
	public int OPERATIONMAXTIMEINSECONDS() { return gen.OPERATIONMAXTIMEINSECONDS;}
	public int CACHEMAXLIFEINMINUTES() { return gen.CACHEMAXLIFEINMINUTES;}
	public int CACHEMAXSIZEINMO() { return gen.CACHEMAXSIZEINMO;} 
	public int CACHECLEANUPLAPSEMINUTES() { return gen.CACHECLEANUPLAPSEMINUTES;}
	public int NSSCANPERIODINSECONDS() { return gen.NSSCANPERIODINSECONDS; }
	public int S2CLEANUPPERIODINHOURS() { return gen.S2CLEANUPPERIODINHOURS; }
	public int DTIMEDELAYINHOURS() { return gen.DTIMEDELAYINHOURS; }
	public String[] langs() { return config.gen.langs; }
	public String lang() { return config.gen.langs[0]; }
	public boolean isDebug() { return gen.isDebug;}
	
	public Instance instance() { 
		Instance x = gen.instances.get(config.gen.instance);
		if (x == null)
			x = gen.instances.get("default");
		return x;
	}
		
	public String shortcut(String s){
		if (s == null || s.length() == 0) s = "(empty)";
		return gen.shortcuts != null ? gen.shortcuts.get(s) : null;
	}
	
	public ResFilter resFilter(){
		if (resFilter == null)
			resFilter = new ResFilter();
		return resFilter;
	}
		
	public int queueNumber(Id id) {
		return  id != null && id.docclass().charAt(0) == 'B' ? 1 : 0;
	}
	
	/* Secret ************************************************/
	
	public String username() { return config.secret.username; }
	public String password() { return config.secret.password; }
	public String secretKey() { return config.secret.secretKey; }
	public String qmSecretKey() { return config.secret.qmSecretKey; }
	public boolean isSecretKey(String key) { return key != null && key.equals(config.secret.secretKey); }
	public boolean isQmSecretKey(String key) { return key != null && key.equals(config.secret.qmSecretKey); }
	public HashMap<String,String> mailers() { return config.secret.mailers; }

	/****************************************************/
	public final DBProvider newDBProvider(String ns) throws AppException {
		try {
			return (DBProvider) dbProviderConstructor.newInstance(ns);
		} catch (Exception e) {
			throw new AppException(e, "XDBPROVIDERFAILURE");
		}
	}

	/****************************************************/

	private static void declareDocumentsAndOperations() throws Exception{	
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
