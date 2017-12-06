package fr.cryptonote.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import fr.cryptonote.base.ExecContext.IuCDoc;
import fr.cryptonote.provider.DBProvider.DeltaDocument;

public class Cache {
	private static final int MAXLIFE = AConfig.config().CACHEMAXLIFEINMINUTES() * 60 * 1000;
	private static final long MAXSIZE = AConfig.config().CACHEMAXSIZEINMO() * 1000000;
	private static final int CLEANUPLAPSEMINUTES = AConfig.config().CACHECLEANUPLAPSEMINUTES();
	private static Long NEXTCLEANUP = System.currentTimeMillis() + (CLEANUPLAPSEMINUTES * 60000);
	
	private static final Hashtable<String, Cache> caches = new Hashtable<String, Cache>();

	/**
	 * Retourne la cache du namespace courant de l'ExecContext et la créée vide
	 * si elle n'existait pas.
	 * @param namespace
	 * @return
	 */
	public static synchronized Cache cacheOf(String namespace) {
		Cache dc = caches.get(namespace);
		if (dc == null) {
			dc = new Cache();
			caches.put(namespace, dc);
		}
		return dc;
	}

	public static synchronized Cache current() { return cacheOf(ExecContext.current().ns()) ;}

	/*************************************************************/

	private static long cleanup(){
		long now = System.currentTimeMillis();
		if (NEXTCLEANUP < now)
			synchronized (NEXTCLEANUP) {
				docleanup();
				NEXTCLEANUP = now + (CLEANUPLAPSEMINUTES * 60000);
			}
		return now;
	}

	private static void docleanup(){
		long vol = 0;
		int nbd = 0;
		Collection<Cache> dcs = caches.values();
		for(Cache dc : dcs){
			vol += dc.vol();
			nbd += dc.nbd();
		}
		CDoc[] lst = new CDoc[nbd];
		int i = 0;
		for(Cache dc : dcs)	for(CDoc d : dc.documents.values()) lst[i++] = d;
		Arrays.sort(lst);
		
		long limit = System.currentTimeMillis() - MAXLIFE;
		for(CDoc e : lst) {
			if (e.lastTouch < limit || vol > MAXSIZE){
				e.cache.remove(e.id());
				vol -= e.sizeInCache;
			}
		}
	}
	
	/*************************************************************/
	private HashMap<String,CDoc> documents = new HashMap<String,CDoc>();

	private synchronized CDoc doc(Document.Id id){ return documents.get(id.toString()); }
	private synchronized void remove(Document.Id id){ documents.remove(id.toString()); }
	private long vol(){ long n = 0; for(CDoc d : documents.values()) n += d.sizeInCache; return n;	}
	private int nbd() { return documents.size(); }

	private synchronized CDoc doc(CDoc d){
		d.cache = this;
		documents.put(d.id().toString(), d);
		return d;
	}

	public static class XCDoc {
		public long lastCheckDB;
		public boolean existing; 
		public CDoc cdoc;
		public XCDoc(long lastCheckDB, boolean existant, CDoc cdoc) { this.lastCheckDB = lastCheckDB; this.existing = existant; this.cdoc = cdoc; }
	}
	
	/**
	 * Retourne CDoc trouvé en cache, le cas échéant l'ayant lu / remis à niveau si nécessaire.
	 * @param id
	 * @param minTime : contrainte de fraîcheur. En exigeant la startTime de l'ExecContexct fait lire la plus récente
	 * @param versionActuelle : si non 0, version actuellement détenue. Retourne null si pas mieux
	 * @return null: pas mieux que la version actuelle (non 0), XCdoc.existant indique si le document existe ou non.
	 * @throws AppException
	 */
	public XCDoc cdoc(Document.Id id, Stamp minTime, long versionActuelle) throws AppException {
		if (id == null) throw new AppException("BDOCUMENTCACHE0");
		long now = cleanup();
		long maxAge = minTime != null ? minTime.epoch() : now;
		
		CDoc d = doc(id);
		if (d == null) {
			d = doc(CDoc.newEmptyCDoc(id));
			d.lastTouch = now;
		} else {
			if (d.lastCheckDB >= maxAge) {
				// Présent en cache, assez frais : joie !
				d.lastTouch = now;
				if (d.version() == versionActuelle) return new XCDoc(d.lastCheckDB, true, null);  		// rien de nouveau
				if (d.version() > versionActuelle) return new XCDoc(d.lastCheckDB, true, d.newCopy());	// version plus récente
			}
		}

		// Il faut relire en base : l'exemplaire en cache est soit vide, soit pas assez récent, soit dans une version trop ancienne
		d.lastCheckDB = now;
		DeltaDocument delta = ExecContext.current().dbProvider().getDocument(id, d.ctime(), d.version(), d.dtime());
		if (delta == null) {
			// document inexistant en base
			remove(id); // il était peut-être en cache, le cas échant mis 5 lignes plus haut
			return new XCDoc(d.lastCheckDB, false, null);
		}
		
		if (delta.cas == 0) { // cache à niveau (v == vdb)
			if (d.version() == versionActuelle) return null;	
		} else // cache retardée ou vide
			d.importData(delta);
		return new XCDoc(d.lastCheckDB, true, d.newCopy());
	}

	/**
	 * Retourne le document trouvé en cache, le cas échéant l'ayant lu / remis à niveau si nécessaire.
	 * @param id
	 * @param minTime : contrainte de fraîcheur. En exigeant la startTime de l'ExecContexct fait lire la plus récente
	 * @param versionActuelle : si non 0, version actuellement détenue. Retourne null si pas mieux
	 * @return null: pas mieux que la version actuelle (non 0), XCdoc.existant indique si le document existe ou non.
	 * @throws AppException
	 */
	public Document document(Document.Id id, Stamp minTime, long versionActuelle) throws AppException {
		XCDoc xc = cdoc(id, minTime, versionActuelle);
		return Document.newDocument(xc.existing ? xc.cdoc : CDoc.newEmptyCDoc(id));
	}
	
	/*
	 * Invoquée dans la phase de validation d'une opération juste APRES le commit()
	 * pour répercuter dans la cache locale les cdocs commités et supprimés.
	 */
	public synchronized void afterValidateCommit(long version, Collection<IuCDoc> updated, Collection<Document> docsToDel, Collection<String> docsToDelForced) {
		if (updated != null) for(IuCDoc x : updated) {
			CDoc a = documents.get(x.cdoc.id().toString());
			if (a == null || a.version() < version) doc(x.cdoc);
		}
		if (docsToDel != null) for(Document x : docsToDel) documents.remove(x.id().toString());
		if (docsToDelForced != null) for(String clid : docsToDelForced) documents.remove(clid);
	}
	
	/*
	 * Validation échouée parce que ces documents sont obsolètes : rafraîchir
	 */
	public synchronized void refreshCache(HashSet<String> badDocs) {
		if (badDocs != null) for(String k : badDocs) {
			CDoc a = documents.get(k);
			if (a != null) a.lastCheckDB = 0;
		}
	}
}
