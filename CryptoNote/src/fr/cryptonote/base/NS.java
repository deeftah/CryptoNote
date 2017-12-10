package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import fr.cryptonote.base.Document.P;
import fr.cryptonote.base.NS.Namespace.Cfg;
import fr.cryptonote.base.Servlet.Attachment;
import fr.cryptonote.base.Servlet.Resource;
import fr.cryptonote.provider.DBProvider;
import fr.cryptonote.provider.DBProvider.DeltaDocument;

public class NS {
	/*
	 * L'item Cfg est singleton
	 * les pièces jointes peuvent être au moins:
	 * config.js : script lancé après tous les autres d'une page
	 * custom.css : :root{ ... } inclu après le CSS du thème de base cité dans cfg
	 */
		
	private static String nsz;
	public static String nsz() { return nsz; }
	private static long nextReload = 0;
	private static int NSSCANPERIODINSECONDS;
	private static final Integer LOCK = new Integer(0);
	private static long lastScan = 0;
	private static String[] allns;
	private static boolean ready = false;
	private static HashMap<String,NS> namespaces = new HashMap<String,NS>();
	private static HashMap<String,ArrayList<String>> queueManagers = new HashMap<String,ArrayList<String>>();
	
	static void init() throws AppException{
		NSSCANPERIODINSECONDS = AConfig.config().NSSCANPERIODINSECONDS();
		allns = AConfig.config().ns();
		nsz = allns[0];
		for(String n : allns) namespaces.put(n, new NS());
		ready = true;
	}
	
	static void reload() throws AppException{
		if (!ready) init();
		Stamp now = Stamp.fromNow(0);
		if (now.epoch() < nextReload) return;
		DBProvider provider = ExecContext.dbProvider(nsz);
		synchronized(LOCK){
			Collection<DeltaDocument> lst = provider.listDoc("Namespace.", lastScan);
			if (lst.size() != 0) {
				for(DeltaDocument dd : lst) {
					NS ns = namespaces.get(dd.id.docid());
					if (ns != null) {
						Namespace n = (Namespace)Cache.cacheOf(nsz).document(dd.id, now, ns.version);
						if (n != null)
							ns.refresh(n);
					}
				}
				
				queueManagers.clear();
				for(String ns : allns){
					String qm = NS.get(ns).cfg.qm;
					if (qm != null){
						ArrayList<String> nsl = queueManagers.get(qm);
						if (nsl == null){
							nsl = new ArrayList<String>();
							queueManagers.put(qm, nsl);
						}
						nsl.add(ns);
					}
				}
			}
			nextReload = now.epoch() + (NSSCANPERIODINSECONDS * 1000);
		}
	}
	
	private void refresh(Namespace d) throws AppException {
		version = d.version();
		
		Namespace.Cfg cfg = d.cfg();
		cfg = (Namespace.Cfg)cfg.getCopy();
		HashSet<String> resok = new HashSet<String>();
		for(String name : d.getPKeys()){
			resok.add(name);
			P p = d.p(name);
			PX r = resources.get(name);
			if (r != null && r.version >= p.version()) continue;
			PX px = new PX(p);
			if (px.resource != null)
				resources.put(name, px);
			else
				resources.remove(name);
		}
		for(String n : resources.keySet())
			if (!resok.contains(n)) 
				resources.remove(n);
	}
		
	public static NS get(String ns){ return namespaces.get(ns); }

	public static ArrayList<String> nsOfQm(String[] qms) throws AppException{
		reload();
		ArrayList<String> res = new ArrayList<String>();
		if (qms != null && qms.length != 0)
			for(String qm : qms) {
				ArrayList<String> nsl = queueManagers.get(qm);
				if (nsl == null) continue;
				for(String ns : nsl){
					if (res.indexOf(ns) == -1)
						res.add(ns);
				}
			}
		return res;
	}

	public static int status(String ns) throws AppException{
		NS x = get(ns);
		return x == null ? 3 : x.cfg.status;
	}

	public static int build(String ns) throws AppException{
		NS x = get(ns);
		return x == null ? 0 : x.cfg.build;
	}

	public static String info(String ns) throws AppException{
		NS x = get(ns);
		return x == null ? "" : (x.cfg.info == null ? "" : x.cfg.info);
	}

	public static String theme(String ns) throws AppException{
		NS x = get(ns);
		return x == null ? "a" : (x.cfg.theme == null ? "a" : x.cfg.theme);
	}

	public static String label(String ns) throws AppException{
		NS x = get(ns);
		return x == null ? "" : (x.cfg.label == null ? ns : x.cfg.label);
	}

	public static String qm(String ns) throws AppException{
		NS x = get(ns);
		return x == null ? "qm1" : (x.cfg.qm == null ? "qm1" : x.cfg.qm);
	}

	public static Object srvcfg(String ns) throws AppException{
		reload();
		NS x = get(ns);
		Object nscfg = AConfig.config().newNSSrvCfg();
		return x == null ? nscfg : JSON.fromJson(x.cfg.srvcfg, nscfg.getClass());
	}

	public static Resource resource(String ns, String name) throws AppException{
		if (nsz.equals(ns)) return null;
		NS x = get(ns);
		if (x != null) {
			PX px = x.resources.get(name);
			if (px != null) 
				return px.resource;
		}
		return null;
	}

	private static Document.Id id(String ns) throws AppException { return new Document.Id(Namespace.class, ns); }
	
	public static class Namespace extends Document {
		private static Namespace document(String ns) throws AppException {
			return (Namespace)Document.getOrNew(NS.id(ns));
		}

		private static Namespace existingDocument(String ns) throws AppException {
			return (Namespace)Document.get(NS.id(ns), 0);
		}

		private Cfg cfg() throws AppException {	
			return (Cfg)singletonOrNew(Cfg.class); 
		}
		
		public static class Cfg extends Singleton {
			// thème applicable et surchargeable par custom.css
			String theme = "a";
			
			// build
			int build = 1;
			
			// 0:rw 1:ro 2:interdit 3:inexistant
			int status = 0;
			
			// info sur le status
			String info = null;
			
			String label = null;
			
			// Code du queue manager
			String qm = "qm1";
			
			/* Un JSON utilisable librement par le serveur pour y obtenir 
			 * des données de configuration spécifique à un namespace
			 */
			String srvcfg = null;
		}
	}
			
	/** Instance NS ******************************/
	private long version = 0;
	private Namespace.Cfg cfg = new Namespace.Cfg();
	
	private static class PX {
		long version;
		private Resource resource;
		private PX(P p) throws AppException{
			this.version = p.version();
			byte[] bytes = p.blobGet();
			if (bytes != null)	resource = new Resource(bytes, p.mime());
		}
	}
	
	private HashMap<String,PX> resources = new HashMap<String,PX>();

	/*****************************************************************/
	public static class NSRes extends Operation {
		public static class Param {
			String ns;
			String name;
		}
		private Param param;
		
		@Override public void work() throws AppException {
			if (!hasAdminKey())	throw new AppException("SADMINOP");
			
			String nsc = ExecContext.current().ns();
			if (!nsc.equals(nsz)) throw new AppException("ANSBADNS", nsz, nsc);
			
			Attachment a = inputData().attachments().get("resource");
			if (a == null || a.bytes == null || a.bytes.length == 0) throw new AppException("ANSEMPTY");
			
			Namespace d = Namespace.existingDocument(param.ns);
			if (d == null) throw new AppException("ANSUNKNOWN", param.ns);
			if (!Servlet.isZres(param.name)) throw new AppException("ANSZRES", param.name);;
			
			P p = d.blobStore(param.name, a.contentType, a.bytes);
			if (p.toSave()) {
				Cfg cfg = d.cfg();
				cfg.build++;
				cfg.commit();
			}
		}
		
		@Override public void afterWork() throws AppException {
			nextReload = 0;
			reload();
		}
	}

	/*****************************************************************/
	public static class NSCfg extends Operation {
		public static class Param {
			String ns;
			String qm;
			String theme;
			String info;
			String label;
			String srvcfg;
			int status;
		}
		private Param param;
		
		@Override public void work() throws AppException {
			if (!hasAdminKey()) throw new AppException("SADMINOP");
			
			String nsc = ExecContext.current().ns();
			if (!nsc.equals(nsz)) throw new AppException("ANSBADNS", nsz, nsc);
				
			Namespace d = Namespace.document(param.ns);
			Cfg cfg = d.cfg();
			if (param.theme != null && param.theme.length() != 0) cfg.theme = param.theme;
			if (param.label != null && param.label.length() != 0) cfg.label = param.label;
			if (param.qm != null && param.qm.length() != 0) cfg.qm = param.qm;
			if (param.info != null && param.info.length() != 0) cfg.info = param.info;
			if (param.status >= 0 && param.status <= 3) cfg.status = param.status;
			String x = param.srvcfg;
			if (x == null || x.length() == 0 || "{}".equals(x))
				cfg.srvcfg = null;
			else {
				Object nscfg = AConfig.config().newNSSrvCfg();
				try {
					JSON.fromJson(x, nscfg.getClass());
				} catch (Exception e) {
					throw new AppException(e, "ANSSRVCFG", x);
				}
				cfg.srvcfg = x;
			}
			cfg.commit();
			if (cfg.toSave()) {
				cfg.build++;
				cfg.commit();
			}
		}
		
		@Override public void afterWork() throws AppException {
			nextReload = 0;
			reload();
		}
	}

	/*****************************************************************/
	public static class NSExport extends Operation {
		public static class Param {
			String ns;
		}
		private Param param;
		
		@Override public void work() throws AppException {
			if (!hasAdminKey())	throw new AppException("SADMINOP");
			
			String nsc = ExecContext.current().ns();
			if (!nsc.equals(nsz)) throw new AppException("ANSBADNS", nsz, nsc);
			
			String json = "{}";
			if (param.ns == null)
				json = JSON.toJson(namespaces);
			else {
				NS x = namespaces.get(param.ns);
				if (x != null) json = JSON.toJson(x);
			}
			gzipResultat(json);
		}
		
	}

}
