package fr.cryptonote.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import fr.cryptonote.base.BConfig.Nsqm;
import fr.cryptonote.base.Document.P;
import fr.cryptonote.base.NS.Nsdoc.Cfg;
import fr.cryptonote.base.Servlet.Attachment;
import fr.cryptonote.base.Servlet.Resource;
import fr.cryptonote.provider.DBProvider;
import fr.cryptonote.provider.DBProvider.DeltaDocument;

public class NS {
	public static final String NAMESPACE = "namespace";
	/*
	 * L'item Cfg est singleton
	 * les pièces jointes peuvent être au moins:
	 * config.js : script lancé après tous les autres d'une page
	 * custom.css : inclu après le CSS du thème de base cité dans cfg ou la config générale
	 */
		
	private static Long nextReload = 0L;
	private static long lastScan = 0;
	private static boolean ready = false;
	private static HashMap<String,NS> namespaces = new HashMap<String,NS>();
		
	static void reload() {
		if (!ready) {
			for(String n : BConfig.namespaces()) namespaces.put(n, new NS());
			ready = true;
		}
		long now = System.currentTimeMillis();
		if (now > nextReload) 
			synchronized(nextReload){
				try {
					DBProvider provider = BConfig.getDBProvider(BConfig.defaultBase()).ns(NAMESPACE);
					Collection<DeltaDocument> lst = provider.listDoc(null, lastScan);
					Stamp st = Stamp.fromNow(0);
					lastScan = st.stamp();
					if (lst.size() != 0) {
						for(DeltaDocument dd : lst) {
							NS ns = namespaces.get(dd.id.docid());
							if (ns != null) {
								Nsdoc doc = (Nsdoc)Cache.cacheOf(NAMESPACE).document(dd.id, st, ns.version);
								if (doc != null)
									ns.refresh(doc);
							}
						}
					}
				} catch (Exception e) {}
				nextReload = now + (BConfig.NSSCANPERIODINSECONDS() * 1000);
		}
	}
	
	private void refresh(Nsdoc d) throws AppException {
		version = d.version();
		Nsdoc.Cfg cfg = d.cfg();
		cfg = (Nsdoc.Cfg)cfg.getCopy();
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
		
	/*********************************************************************/
	private long version = 0;
	private Cfg cfg = null;
	private HashMap<String,PX> resources = new HashMap<String,PX>();

	private static class PX {
		long version;
		private Resource resource;
		private PX(P p) throws AppException{
			this.version = p.version();
			byte[] bytes = p.blobGet();
			if (bytes != null)	resource = new Resource(bytes, p.mime());
		}
	}

	/*********************************************************************/
	public static Nsqm fresh(Nsqm nsqm) {
		reload();
		NS x = namespaces.get(nsqm.code);
		Cfg c = x == null ? null : x.cfg;
		if (c != null) {
			if (c.options != null) nsqm.options = c.options;
			nsqm.build = c.build;
			nsqm.off = c.off;
		}
		return nsqm;
	}

	public static Resource resource(String ns, String name) {
		reload();
		if (NAMESPACE.equals(ns)) return null;
		NS x = namespaces.get(ns);
		if (x != null) {
			PX px = x.resources.get(name);
			if (px != null) 
				return px.resource;
		}
		return null;
	}

	/*********************************************************************/
	private static Document.Id id(String ns) throws AppException { return new Document.Id(Nsdoc.class, ns); }
	
	public static class Nsdoc extends Document {
		private static Nsdoc document(String ns) throws AppException { return (Nsdoc)Document.getOrNew(NS.id(ns)); }
		private static Nsdoc existingDocument(String ns) throws AppException { return (Nsdoc)Document.get(NS.id(ns), 0); }
		private Cfg cfg() throws AppException {	return (Cfg)singletonOrNew(Cfg.class); }
		
		public static class Cfg extends Singleton {
			String theme = "a"; // thème applicable et surchargeable par custom.css			
			int build = 1; 		// build
			int off = 0;		// si non 0, CP : 1-9 cause d'arrêt, 0-9 durée off prévisible
			HashMap<String,String> options = null;
		}
	}
			
	/*****************************************************************/
	public static class NSRes extends Operation {
		public static class Param {
			String ns;
			String name;
		}
		private Param param;
		
		@Override public void work() throws AppException {
			if (!isSudo())	throw new AppException("SADMINOP");
			String nsc = ExecContext.current().nsqm().code;
			if (!nsc.equals(NAMESPACE)) throw new AppException("ANSBADNS", NAMESPACE, nsc);
			
			Attachment a = inputData().attachments().get("resource");
			if (a == null || a.bytes == null || a.bytes.length == 0) throw new AppException("ANSEMPTY");
			
			Nsdoc d = Nsdoc.existingDocument(param.ns);
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
			nextReload = 0L;
			reload();
		}
	}

	/*****************************************************************/
	public static class NSCfg extends Operation {
		public static class Param {
			String ns;
			String theme;
			HashMap<String,String> options;
			int off;
		}
		private Param param;
		private boolean changed = false;
		
		@Override public void work() throws AppException {
			if (!isSudo())	throw new AppException("SADMINOP");
			String nsc = ExecContext.current().nsqm().code;
			if (!nsc.equals(NAMESPACE)) throw new AppException("ANSBADNS", NAMESPACE, nsc);
				
			Nsdoc d = Nsdoc.document(param.ns);
			Cfg cfg = d.cfg();
			if (param.theme != null && param.theme.length() != 0) cfg.theme = param.theme;
			if (param.options != null) cfg.options = param.options;
			if (param.off >= 0 && param.off <= 99) cfg.off = param.off;
			cfg.commit();
			if (cfg.toSave()) {
				cfg.build++;
				cfg.commit();
				changed = true;
			}
		}
		
		@Override public void afterWork() throws AppException {
			if (changed) {
				nextReload = 0L;
				reload();
			}
		}
	}

	/*****************************************************************/
	public static class NSExport extends Operation {
		public static class Param {
			String ns;
		}
		private Param param;
		
		@Override public void work() throws AppException {
			if (!isSudo())	throw new AppException("SADMINOP");
			String nsc = ExecContext.current().nsqm().code;
			if (!nsc.equals(NAMESPACE)) throw new AppException("ANSBADNS", NAMESPACE, nsc);
			
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
