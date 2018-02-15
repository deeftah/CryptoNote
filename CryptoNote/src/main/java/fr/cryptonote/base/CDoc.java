package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.HashMap;

import fr.cryptonote.base.Document.BItem;
import fr.cryptonote.base.Document.ExportedFields;
import fr.cryptonote.base.Document.Id;
import fr.cryptonote.base.Document.ItemId;
import fr.cryptonote.base.DocumentDescr.ItemDescr;
import fr.cryptonote.provider.DBProvider.DeltaDocument;

public class CDoc implements Comparable<CDoc> {
	
	/*
	 *- `unchanged` : aucun item n'a changé sur le document qui existait avant le début de l'opération.
	 *- `modified` : le document existait avant le début de l'opération et un ou des items ont été créés / modifiés / supprimés.
	 *- `created` : le document n'existait pas et il a été créé par un `getOrNew()`. Il n'a pas forcément d'items.
	 *- `recreated` : le document existait et a été recréé par la méthode `recreate()`. 
	 *   Sa date-heure de création n'est pas la même qu'au début de l'opération et son contenu a complètement été purgé par la suppression survenue avant sa recréation.
	 *- `deleted` : le document existait mais a été supprimé par l'opération.
	 *- `shortlived` : le document n'existait pas, a été créé par un `getOrNew()` et a été supprimé ensuite au cours de l'opération.
	 */
	
	public enum Status {unchanged, modified, created, recreated, deleted, shortlived};	

	/********************************************************************************/
	@Override public int compareTo(CDoc c) { return lastTouch < c.lastTouch ? -1 : (lastTouch > c.lastTouch ? 1 : 0); }
	
	private Document.Id id;
	private long version;
	private long ctime;
	private long dtime;
	private Status status;
	
	private int nbExisting = 0;
	private int nbToSave = 0;
	private int nbToDelete = 0;
	private int nbTotal = 0;
	private long v1;
	private long v2;
	
	private HashMap<String,CItem> sings = new HashMap<String,CItem>();
	private HashMap<String,HashMap<String,CItem>> colls = new HashMap<String,HashMap<String,CItem>>();

	// Données de gestion de cache
	int sizeInCache;
	long lastTouch;
	long lastCheckDB;
	Cache cache;

	Document.Id id() { return id; }
	public long version() { return version; }
	public long ctime() { return ctime; }
	public long dtime() { return dtime; }

	public Status status() { return status; };

	public int nbExisting() { return nbExisting; }
	public int nbToSave() { return nbToSave; }
	public int nbToDelete() { return nbToDelete; }
	public int nbTotal() { return nbTotal; }
	public long v1() { return v1; };
	public long v2() { return v2; };

	void recreate() { status = version() == 0 ? Status.created : Status.recreated; clearAllItems(); }
	void delete() {	status = version() == 0 ? Status.shortlived : Status.deleted; clearAllItems(); }
	private void clearAllItems() {
		sings.clear();
		for(HashMap<String,CItem> cis : colls.values()) cis.clear();
	}

	CDoc initStruct() {
		for(ItemDescr itd : id.descr().itemDescrs())
			if (!itd.isSingleton()) colls.put(itd.name(), new HashMap<String,CItem>());
		return this;
	}
	
	/********************************************************************************/
	String[] itemids(Class<?> itemClass) { 
		if (itemClass == null) return new String[0];
		HashMap<String,CItem> cis = colls.get(itemClass.getSimpleName());
		if (cis == null || cis.size() == 0) return new String[0];
		return cis.keySet().toArray(new String[cis.size()]);
	}

	/********************************************************************************/
	public static class Index {
		public String name;
		public Object obj;
		public Index(String n, Object o) {name = n; obj = o; }
	}
	
	/********************************************************************************/
	@FunctionalInterface public interface CIAction {
	    public boolean action(CItem ci);
	}

	@FunctionalInterface interface CICounter {
	    int count(CItem ci);
	}

	void browse(CIAction a) {
		for(CItem ci : sings.values()) a.action(ci);
		for(HashMap<String,CItem> cis : colls.values()) for(CItem ci : cis.values()) a.action(ci);
	}

	int count(CICounter c) {
		int n = 0;
		for(CItem ci : sings.values()) n += c.count(ci);
		for(HashMap<String,CItem> cis : colls.values()) for(CItem ci : cis.values()) n += c.count(ci);
		return n;
	}

	ArrayList<CItem> filter(CIAction a) {
		ArrayList<CItem> items = new ArrayList<CItem>();
		for(CItem ci : sings.values()) if (a.action(ci)) items.add(ci);
		for(HashMap<String,CItem> cis : colls.values()) for(CItem ci : cis.values()) if (a.action(ci)) items.add(ci);
		return items;
	}

	ArrayList<String> listKeys(CIAction a) {
		ArrayList<String> items = new ArrayList<String>();
		for(CItem ci : sings.values()) if (a.action(ci)) items.add(ci.clkey());
		for(HashMap<String,CItem> cis : colls.values()) for(CItem ci : cis.values()) if (a.action(ci)) items.add(ci.clkey());
		return items;
	}

	void compSizeInCache() { sizeInCache = count((ci) -> ci.sizeInCache()); }
//	ArrayList<CItem> listItemsToSave() { return filter((ci) -> ci.toSave); }
//	ArrayList<CItem> listItemsToDelete() { return filter((ci) -> ci.toDelete); }
//	ArrayList<CItem> listExistingItems() { return filter((ci) -> !ci.deleted); }
	ArrayList<CItem> listAllItems() { return filter((ci) -> true); }
	
	void summarize() throws AppException {
		// unchanged, modified, created, recreated, deleted, shortlived
		v1 = 0;	v2 = 0;	nbExisting = 0;	nbToSave = 0; nbToDelete = 0; nbTotal = 0; sizeInCache = 0;
		if (status != Status.deleted && status != Status.shortlived) {
			// unchanged, modified, created, recreated
			browse(ci -> {
				sizeInCache += ci.sizeInCache();
				v1 += ci.v1();
				v2 += ci.v2();
				nbTotal++;
				if (!ci.deleted) nbExisting++;
				if (!ci.toSave) nbToSave++;
				if (!ci.toDelete) nbToDelete++;
				return true;		
			});
			if (status != Status.created && status == Status.recreated)
				// unchanged, modified
				status = nbToSave + nbToDelete != 0 ? Status.modified : Status.unchanged;
		}
	}

	/************************************************************************************/
	/*
	 * Construit un CItem autonome depuis des données de DB ou d'importation
	 * Les CItem de classe P sont désérialisés immédiatement
	 * @param id du document
	 * @param clazz de l'item sous forme de String
	 * @param version de l'item
	 * @param key de l'item
	 * @param value json ou texte raw selon le cas
	 * @return null si la classe est inconnue, la value null, la clé null 
	 * (selon que l'item est raw ou non) ou qu'un item P est mal formé
	 */
	public static CItem newCItem(Document.Id id, String clazz, long version, String key, String value) {
		if (value == null) return null;
		ItemDescr descr = id.descr().itemDescr(clazz);
		if (descr == null || (descr.isSingleton() && key != null) || (!descr.isSingleton() && key == null)) return null;
		CItem ci = new CItem();
		ci.descr = descr;
		ci.key = key;
		ci.value = value;
		ci.version = version;
		return ci;
	}
		
	CItem citem(Class<?> clazz, boolean newIfNotExisting, String key) throws AppException{
		if (clazz == null) throw new AppException("BITEMCLASS", "?");
		ItemDescr descr = id().descr().itemDescr(clazz.getSimpleName());
		if (descr == null) throw new AppException("BITEMCLASS", clazz.getSimpleName());
		if (key == null) key = "";
		CItem ci;
		if (descr.isSingleton()) {
			ci = sings.get(descr.name());
			if (ci == null) {
				if (!newIfNotExisting) return null;
				ci = new CItem();
				ci.descr = descr;
				ci.cdoc = this;
				ci.setStatus();
				sings.put(descr.name(), ci);
				return ci;
			}
		} else {
			HashMap<String,CItem> cis = colls.get(descr.name());
			if (cis == null) throw new AppException("BITEMCLASSCOL", clazz.getSimpleName());
			ci = cis.get(key);
			if (ci == null) {
				if (!newIfNotExisting) return null;
				ci = new CItem();
				ci.descr = descr;
				ci.cdoc = this;
				ci.key = key;
				ci.setStatus();
				cis.put(key, ci);
				return ci;
			}
		}
		// ci existait mais peut-être détruit
		ci.setStatus();
		if (newIfNotExisting && ci.deleted) {
			// il est recréé (vide)
			ci.deleted = false;
			ci.toSave = true;
			ci.toDelete = false;
			ci.nvalue = ci.descr.isRaw() ? "{}" : "";
			ci.v2 = 0;
		}
		return ci.deleted ? null : ci;
	}

	/*
	 * Stockage d'un BItem remplaçant le cas échéant un existant.
	 * Ne s'applique ni à un P, ni à un raw
	 */
	CItem citem(BItem bitem, String key) throws AppException{
		String n = bitem.getClass().getSimpleName();
		ItemDescr descr = id().descr().itemDescr(n);
		if (descr == null)	throw new AppException("BITEMCLASS", n);
		if (key == null) key = "";
		CItem ci;
		if (descr.isSingleton()) {
			ci = sings.get(descr.name());
			if (ci == null) {
				ci = new CItem();
				ci.descr = descr;
				ci.cdoc = this;
				sings.put(descr.name(), ci);
			}
		} else {
			HashMap<String,CItem> cis = colls.get(descr.name());
			if (cis == null) throw new AppException("BITEMCLASSCOL", n);
			ci = cis.get(key);
			if (ci == null) {
				ci = new CItem();
				ci.descr = descr;
				ci.cdoc = this;
				ci.key = key;
				cis.put(key, ci);
			}
		}
		if (ci.bitem != null) ci.bitem.detach();
		ci.bitem = bitem;
		ci.nvalue = JSON.toJson(bitem);
		ci.setStatus();
		return ci;
	}

	/********************************************************************************/
	public static class CItem {
		private CDoc cdoc;
		private ItemDescr descr;
		transient BItem bitem;
		
		private long version;	// version d'un item qui existait avant (value != null) ou une trace de suppression (value == null)
		private long vop;		// dans le cas d'une mise à jour différée, version de l'opération ayant déclaré cette valeur
		private String key;		// key d'un item, null pour un singleton
		private String value; 	// contenu avant. null pour une trace de suppression ou un item nouvellement créé
		private String nvalue; 	// nouveau contenu. null si l'item était existant et inchangé ou une trace de suppression (sans recréation)
		private int v2;			// taille du blob
		private int nv2;		// taille du blob après changement
		private ExportedFields exportedFields; // valeurs des champs exportés (extraits de BItem au commit())
		private boolean deleted;	// item non existant
		private boolean toSave;		// item créé / modifié à sauver
		private boolean toDelete;	// item à supprimer
		
		public CItem() {}
		
		public CItem(ItemId i, long version, long vop, int v2, String value) {
			this.descr = i.descr(); this.key = i.key(); this.version = version; this.vop = vop; this.v2 = v2;
			if (value == null) deleted = true; else this.value = value;
		}
		
		public ItemDescr descr() { return descr; }
		public Id id() { return cdoc.id; }
		public long version() { return version; }
		public long vop() { return vop; }
		public String key() { return descr().isSingleton() ? "" : key; }
		public String cvalue() { return nvalue != null ? nvalue : (value != null ? value : null); }
		public int nv2() { return nv2; }
		public String clkey() { return descr.name() + (descr.isSingleton() ? "" : "." + key); }
		int sizeInCache() { return (value != null ? value.length() : 0) + (key != null ? key.length() : 0); }
		public boolean deleted() { return deleted; }				// item non existant
		public boolean toSave() { return toSave; } 					// item créé / modifié à sauver
		public boolean toDelete() { return toDelete; } 				// item à supprimer
		public boolean created() { return toSave && version == 0; } // item créé
		
		void setStatus() {
			deleted = value == null && nvalue == null;
			toDelete = value != null && nvalue == null;
			toSave = value != null && !value.equals(nvalue);
		}
				
		public boolean changedAfterV(long v) { return version > v && toSave;}

		public boolean deletedAfterV(long v) { return version > v && toDelete; }

		public int v1() { return deleted ? 0 : sizeInCache(); }

		public int v2() { return deleted ? 0 : (toSave ? nv2 : v2); }

		public String toString() { return cdoc != null ? cdoc.id().toString() + "#" + clkey() : clkey(); }
		
		public ExportedFields exportedFields() { return exportedFields; }
		
		private void purge() {
			if (descr.isSingleton()) 
				cdoc.sings.remove(descr.name());
			else
				cdoc.colls.get(descr.name()).remove(key);
			cdoc = null;
			descr = null;
			bitem = null;
		}
		
		CItem copy(CDoc cd){
			CItem c = new CItem();
			c.cdoc = cd;
			c.descr = descr;
			c.version = version;
			c.vop = vop;
			c.key = key;
			c.value = value;
			c.nvalue = nvalue;
			c.v2 = v2;
			c.nv2 = nv2;
			c.deleted = deleted;
			c.toSave = toSave;
			c.toDelete = toDelete;
			return c;
		}
						
		void delete() throws AppException {	nvalue = null;	nv2 = 0; setStatus(); }

		void commit(String json, int v2, ExportedFields exportedFields) throws AppException{
			if (json == null) json = "{}";
			this.exportedFields = exportedFields;
			nvalue = json.equals(value) ?  null : json;
			nv2 = v2;
			setStatus();
		}
		
		void commitRaw(String text, ExportedFields exportedFields) throws AppException{
			if (text == null) text = "";
			this.exportedFields = exportedFields;
			nvalue = text.equals(value) ? null : text;
			setStatus();
		}
		
		void jsonDel(StringBuffer sb) { 
			sb.append("\n{\"v\":").append(version).append(", \"c\":\"").append(descr.name()).append("\"");
			if (!descr.isSingleton()) sb.append(", \"k\":").append(JSON.json(key));
			sb.append("}");
		}

		void jsonExist(StringBuffer sb, String val) { 
			sb.append("\n{\"v\":").append(version).append(", \"c\":\"").append(descr.name()).append("\"");
			if (!descr.isSingleton()) sb.append(", \"k\":").append(JSON.json(key));
			if (descr.isRaw()) sb.append(", \"s\":"); else sb.append(", \"s\":");
			sb.append(val).append("}");
		}

	}
	
	/********************************************************************************/
	/*
	 * N'est utilisée QUE par Cache pour mettre à jour SON exemplaire
	 * depuis des données incrémentales (ou complètes) de la base
	 * data n'est jamais null
	 * 
	 * 	Cache retardée (v < vdb)
	 *  
	 *  cas = 1 : cache vide
	 *  cas = 2 : cache ayant une vie antérieure (c < cdb) : détruire tous les items actuels
	 *  C'est une copie complète simple qui est retournée dispatchée entre :
	 *  items : tous les items existants
	 *       +  tous les items détruits
	 *  
	 *  Vie courante retardée
	 *  
	 *  cas 3 : v >= dtb. Le cache ne contient pas d'items détruits dont la suppression serait inconnue de la base
	 *  items : tous les items existants modifiés après v
	 *        + tous les items detruits après v
	 *  
	 *  cas 4 : v < dtb. Le cache peut contenir des items détruits dont la destruction est inconnue de la base
	 *  items : tous les items existants modifiés après v
	 *        + tous les items detruits après dtb (on en a pas avant)
	 *  clkeys : clés des items existants qui ne figure pas dans items.

	 */
	
	void storeItem(CItem ci) {
		String n = ci.descr.name();
		if (ci.descr.isSingleton())	sings.put(n, ci); else colls.get(n).put(ci.key, ci);
	}
	
	synchronized void importData(DeltaDocument data) {
		if (data.ctime != ctime) clearAllItems();
		id = data.id;
		version = data.version;
		ctime = data.ctime;
		dtime = data.dtime;
		status = Status.unchanged;
				
		if (data.cas == 2)	clearAllItems();
		for(CItem ci : data.items.values()) storeItem(ci);		
		if (data.cas != 4) return;
		
		// suppression de tous les items qui ne figurent ni dans items, ni dans clkeys
		String[] keys = sings.keySet().toArray(new String[sings.size()]);
		for(String k : keys)
			if (data.items.get(k) == null && !data.clkeys.contains(k)) sings.remove(k);
		for(HashMap<String,CItem> cis : colls.values()) {
			keys = cis.keySet().toArray(new String[cis.size()]);
			for(String k : keys)
				if (data.items.get(k) == null && !data.clkeys.contains(k)) cis.remove(k);
		}
	}

	/*
	 * N'est utilisé QUE pour cloner le CDoc du cache en une copie pour l'opération
	 */
	protected synchronized CDoc newCopy() throws AppException {
		summarize();
		CDoc c = new CDoc().initStruct();
		c.id = id;
		c.lastCheckDB = lastCheckDB;
		c.version = version;
		c.ctime = ctime;
		c.dtime = dtime;
		c.status = Status.unchanged;
		c.nbExisting = nbExisting;
		c.nbToSave = 0;
		c.nbToDelete = 0;
		c.nbTotal = nbTotal;
		c.v1 = v1;
		c.v2 = v2;
		for(CItem ci : sings.values()) c.storeItem(ci.copy(c));
		for(HashMap<String,CItem> cis : colls.values())
			for(CItem ci : cis.values()) c.storeItem(ci.copy(c));
		return c;
	}

	/**
	 * Créé un CDoc correctement identifié, neuf et sans items
	 * @param id
	 */
	static CDoc newEmptyCDoc(Document.Id id){ 
		CDoc cdoc = new CDoc();
		cdoc.id = id;
		cdoc.status = Status.created;
		return cdoc.initStruct();
	}
	
	/******************************************************************************/
		
	/**
	 * Méthode basculant un CDoc local à un ExecContect après commit() d'une opération
	 * en un CDoc acceptable pour être mis en cache.
	 * Comme dtime / ctime / version changent, les items se réinitialisent
	 * @param version
	 * @throws AppException 
	 */
	void afterCommit(long version, long dtime) throws AppException {
		if (status == Status.created || status == Status.recreated) ctime = version;
		this.version = version;
		this.dtime = dtime;
		status = Status.unchanged;
		ArrayList<CItem> toPurge = new ArrayList<CItem>();
		browse(ci -> {
			if (ci.toSave) {
				ci.version = version;
				ci.vop = version;
				ci.value = ci.nvalue;
				ci.nvalue = null;
				ci.v2 = ci.nv2;
				ci.nv2 = 0;
				ci.toSave = false;
			} else if (ci.toDelete){
				ci.version = version;
				ci.vop = 0;
				ci.value = null;
				ci.nvalue = null;
				ci.v2 = 0;
				ci.nv2 = 0;
				ci.toSave = false;
				ci.toDelete = false;
				ci.deleted = true;	
			} else if (ci.deleted && ci.version < dtime) {
				toPurge.add(ci);
			}
			return true;
		});	
		for(CItem ci : toPurge) ci.purge();
	}
}
