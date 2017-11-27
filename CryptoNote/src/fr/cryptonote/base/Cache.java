package fr.cryptonote.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

import fr.cryptonote.provider.DBProvider;
import fr.cryptonote.provider.DBProvider.ImpExpDocument;

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

	/**
	 * Retourne CDoc trouvé en cache, le cas échéant l'ayant lu / remis à niveau si nécessaire.
	 * @param id
	 * @param minTime : contrainte de fraîcheur. En exigeant la startTime de l'ExecContexct fait lire la plus récente
	 * @param versionActuelle : si non 0, version actuellement détenue. Retourne null si pas mieux
	 * @return null: pas mieux que la version actuelle (non 0), 
	 * CDoc.FAKE: ce document n'existe pas (plus), 
	 * autre le CDoc correspondant
	 * @throws AppException
	 */
	public CDoc cdoc(Document.Id id, Stamp minTime, long versionActuelle) throws AppException {
		if (id == null) throw new AppException("BDOCUMENTCACHE0");
		long now = cleanup();
		long maxAge = minTime != null ? minTime.epoch() : now;
		
		CDoc d = doc(id);
		if (d != null) {
			if (d.lastCheckDB >= maxAge) {
				// Présent en cache, assez frais : joie ! (mais peut-être rien de nouveau)
				d.lastTouch = now;
				if (d.version() == versionActuelle) return null;
				if (d.version() > versionActuelle) return d.clone();	
			}
		}
		
		// Il faut relire en base : il est soit absent du cache, soit pas assez récent, soit dans une version trop ancienne
		DBProvider provider = ExecContext.current().dbProvider();
		ImpExpDocument impExp = provider.getDocument(id, d != null ? d.ctime() : 0, d != null ? d.version() : 0, d != null ? d.dtime() : 0);
		if (impExp == null) {
			// document inexistant en base
			remove(id); // par sécurité s'il y était encore
			return CDoc.FAKE;
		}
		
		
		ImpExpData data = provider.exportDoc(id, d != null ? d.version() : 0);
		data.state = ds;

		if (data.header == null && data.items.size() == 0) {
			/*
			 * Rechargement incrémental mais rien de nouveau : comment est-ce possible ?
			 * Si on avait une version retardée par rapport au cache de groupe, on aurait du trouver des items.
			 * Si on n'avait aucune version mais que le cache de groupe disait qu'il y en avait
			 * on aurait trouver des items.
			 * Seule explication : le document a été détruit et le cache de groupe ne l'a pas encore su.
			 */
			remove(id); // par sécurité s'il y était encore
			return CDoc.FAKE;			
		}
				
		// on a une mise à jour (incrémentale OU totale selon le ctime) du document en cache
		if (d != null) {
			// c'était une incrémentale
			d.importData(data);
		} else {
			// c'est une nouvelle insertion en cache
			d = CDoc.newCDoc(data);
			doc(d);
		}
		return d.version() <= versionActuelle ? null : d;
	}

}
