package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.HashMap;

import fr.cryptonote.base.Document.BItem;
import fr.cryptonote.base.Document.P;
import fr.cryptonote.base.DocumentDescr.ItemDescr;

public class CDoc implements Comparable<CDoc> {
	static final CDoc FAKE = new CDoc();
	
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

	public boolean estInexistant() { return this == FAKE; }
	
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
	boolean hasChanges() { return nbToSave + nbToDelete != 0 ; }

	public Status status() { return status; };

	public boolean toSave() { return nbToSave + nbToDelete != 0; }
	public int nbExisting() { return nbExisting; }
	public int nbToSave() { return nbToSave; }
	public int nbToDelete() { return nbToDelete; }
	public int nbTotal() { return nbTotal; }
	public long v1() { return v1; };
	public long v2() { return v2; };

	void recreate() { status = Status.recreated; clearAllItems(); }
	void delete() {	status = version() == 0 ? Status.shortlived : Status.deleted; clearAllItems(); }
	private void clearAllItems() {
		sings.clear();
		for(HashMap<String,CItem> cis : colls.values()) cis.clear();
	}

	/*
	 * N'est utilisé QUE pour cloner le CDoc en cache en une copie pour l'opération
	 */
	protected synchronized CDoc newCopy() throws AppException {
		summarize();
		CDoc c = new CDoc();
		c.id = id;
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
		for(ItemDescr itd : id().descr().itemDescrs())
			if (!itd.isSingleton()) c.colls.put(itd.name(), new HashMap<String,CItem>());
		for(String k : sings.keySet())
			c.sings.put(k, sings.get(k).copy(c));
		for(String k : colls.keySet()) {
			HashMap<String,CItem> cis = colls.get(k);
			HashMap<String,CItem> cis2 = c.colls.get(k);
			if (cis !=  null && cis2 != null) // ça devrait toujours être le cas
				for(String key : cis.keySet())
					cis2.put(key, cis.get(key).copy(c));
		}
		return c;
	}

	/**
	 * Créé un CDoc correctement identifié et pas d'items
	 * @param id
	 */
	static CDoc newEmptyCDoc(Document.Id id){ 
		CDoc cdoc = new CDoc();
		cdoc.id = id;
		cdoc.status = Status.created;
		for(ItemDescr itd : id.descr().itemDescrs())
			if (!itd.isSingleton()) cdoc.colls.put(itd.name(), new HashMap<String,CItem>());
		return cdoc;
	}

	/********************************************************************************/
	public static class Index {
		public String name;
		public Object obj;
		public Index(String n, Object o) {name = n; obj = o; }
	}
	
	/********************************************************************************/
	public static abstract class CItemFilter {
		public abstract boolean accept(CItem ci) throws AppException;
	}

	public static class CItemFilterToSave extends CItemFilter {
		public boolean accept(CItem ci) { return ci.toSave; }
	}

	public static class CItemFilterExisting extends CItemFilter {
		public boolean accept(CItem ci) { return !ci.deleted; }
	}

	public static final CItemFilter filterToSave = new CItemFilterToSave();
	public static final CItemFilter filterExisting = new CItemFilterExisting();
	public static final CItemFilter filterStats = new Stats();

	public static class Stats extends CItemFilter {
		private CDoc d;
		public boolean accept(CItem ci) {
			if (d == null) d = ci.cdoc;
			d.sizeInCache += ci.sizeInCache();
			d.v1 += ci.v1();
			d.v2 += ci.v2();
			d.nbTotal++;
			if (!ci.deleted) d.nbExisting++;
			if (!ci.toSave) d.nbToSave++;
			if (!ci.toDelete) d.nbToDelete++;
			return true;
		}
	}

	void summarize() throws AppException {
		v1 = 0;
		v2 = 0;
		nbExisting = 0;
		nbToSave = 0;
		nbToDelete = 0;
		nbTotal = 0;
		sizeInCache = 0;
		if (status != Status.deleted && status != Status.shortlived) {
			browseAllItems(filterStats);
			if (status == Status.unchanged && status != Status.modified)
				status = hasChanges() ? Status.modified : Status.unchanged;
		}
	}

	void compSizeInCache() {
		int l = 0;
		for(String k : sings.keySet()) l += sings.get(k).sizeInCache();
		for(String k : colls.keySet()) {
			HashMap<String,CItem> cis = colls.get(k);
			for(String key : cis.keySet()) l += cis.get(key).sizeInCache();
		}
		sizeInCache = l;
	}
	
	CItemFilter browseAllItems(CItemFilter f) throws AppException{
		for(String k : sings.keySet()) 
			f.accept(sings.get(k));
		for(String k : colls.keySet()) {
			HashMap<String,CItem> cis = colls.get(k);
			for(String key : cis.keySet())
				f.accept(cis.get(key));
		}
		return f;
	}
	
	/********************************************************************************/
	String[] itemids(Class<?> itemClass) { 
		if (itemClass == null) return new String[0];
		HashMap<String,CItem> cis = colls.get(itemClass.getSimpleName());
		if (cis == null || cis.size() == 0) return new String[0];
		return cis.keySet().toArray(new String[cis.size()]);
	}

	/********************************************************************************/
	ArrayList<CItem> listAllItems(CItemFilter f) throws AppException{
		ArrayList<CItem> items = new ArrayList<CItem>();
		for(String k : sings.keySet()) {
			CItem ci = sings.get(k);
			if (f != null && f.accept(ci)) items.add(ci);
		}
		for(String k : colls.keySet()) {
			HashMap<String,CItem> cis = colls.get(k);
			for(String key : cis.keySet()) {
				CItem ci = cis.get(key);
				if (f != null && f.accept(ci)) items.add(ci);
			}
		}
		return items;
	}

	/********************************************************************************/
	ArrayList<String> listAllClKeys(CItemFilter f) throws AppException{
		ArrayList<String> items = new ArrayList<String>();
		for(String k : sings.keySet()) {
			CItem ci = sings.get(k);
			if (f != null && f.accept(ci)) items.add(ci.clkey());
		}
		for(String k : colls.keySet()) {
			HashMap<String,CItem> cis = colls.get(k);
			for(String key : cis.keySet()) {
				CItem ci = cis.get(key);
				if (f != null && f.accept(ci)) items.add(ci.clkey());
			}
		}
		return items;
	}
	
	/********************************************************************************/

	public static class ImpExpData {
		public Document.Id id;
		public long version;
		public long ctime;
		public ArrayList<CItem> items = new ArrayList<CItem>();
	}

	/**
	 * N'est utilisé QUE par GDCache pour insérer dans son cache un document
	 * importé en totalité depuis la base
	 * @param data
	 * @return
	 */
	static CDoc newCDoc(ImpExpData data) {
		CDoc cdoc = new CDoc();
		for(ItemDescr itd : data.id.descr().itemDescrs())
			if (!itd.isSingleton()) cdoc.colls.put(itd.name(), new HashMap<String,CItem>());
		cdoc.importData(data);
		return cdoc;
	}

	/**
	 * N'est utilisée QUE par CGDcache pour mettre à jour SON exemplaire
	 * depuis des données incrémentales (ou complètes) de la base
	 * SI le ctime a changé, data contient tout et il faut effectuer un clean des données actuelles
	 * @param data n'est jamais null
	 * @return
	 */
	synchronized CDoc importData(ImpExpData data) {
		if (data.ctime != ctime) clearAllItems();
		id = data.id;
		version = data.version;
		ctime = data.ctime;
		
		/*
		 *  Le CDoc en cache par principe est existant mais
		 *  si c'est un nouveau CDoc son status par défaut est created
		 */
		status = Status.unchanged;
				
		// on remplace / insère ceux mis à jour
		for(CItem ci : data.items) {
			ci.cdoc = this;
			if (ci.descr.isSingleton())	sings.put(ci.descr.name(), ci);
			else {
				HashMap<String,CItem> cis = colls.get(ci.descr.name());
				if (cis != null) cis.put(ci.key, ci);
			}
		}
		return this;
	}

	/**
	 * Exporte le CDoc sous forme brute d'un id, header, state et d'une liste d'items
	 * @param detached si true ImpExpData n'a plus aucune référence sur d'autres objets externes à lui-même
	 * (sauf immutable) sinon ses items continuent de pointer sur des données vivantes
	 * @param f filtre facultatif pour ignorer certains items
	 * @throws AppException 
	 * @ return ImpExpData correspondant
	 */
	public ImpExpData export(boolean detached, CItemFilter f) throws AppException {
		ImpExpData d = new ImpExpData();
		d.version = version;
		d.ctime = ctime;
		d.id = id;
		d.items = listAllItems(f);
		return d;
	}

	/********************************************************************************/
	/**
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
		if (descr.isP() && value != null && value.length() != 0){
			try {
				P p = JSON.fromJson(value, P.class);
				ci.blobSize = p.size();
			} catch (AppException e) {	
				return null;
			}
		}
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
			ci.nblobSize = 0;
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
		if (descr.isP() || descr.isRaw()) throw new AppException("BITEMPRAW", n);
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
		private String key;		// key d'un item, null pour un singleton
		private String value; 	// contenu avant. null pour une trace de suppression ou un item nouvellement créé
		private String nvalue; 	// nouveau contenu. null si l'item était existant et inchangé ou une trace de suppression (sans recréation)
		private int blobSize;	// taille du blob
		private int nblobSize;	// taille du blob après changement
		private boolean deleted;	// item non existant
		private boolean toSave;		// item créé / modifié à sauver
		private boolean toDelete;	// item à supprimer
		
		public ItemDescr descr() { return descr; }
		public long version() { return version; }
		public String key() { return descr().isSingleton() ? "" : key; }
		public String cvalue() { return nvalue != null ? nvalue : (value != null ? value : null); }
		public String clkey() { return descr.name() + (descr.isSingleton() ? "" : "." + key); }
		public int sizeInCache() { return (value != null ? value.length() : 0) + (key != null ? key.length() : 0); }
		public boolean deleted() { return deleted; }	// item non existant
		public boolean toSave() { return toSave; } // item créé / modifié à sauver
		public boolean toDelete() { return toDelete; } // item à supprimer
		
		void setStatus() {
			deleted = value == null && nvalue == null;
			toDelete = value != null && nvalue == null;
			toSave = value != null && !value.equals(nvalue);
		}
				
		public boolean changedAfterV(long v) { return version > v && toSave;}

		public boolean deletedAfterV(long v) { return version > v && toDelete; }

		public int v1() { return deleted ? 0 : sizeInCache(); }

		public int v2() { return deleted ? 0 : (toSave ? nblobSize : blobSize); }

		public String toString() { return cdoc != null ? cdoc.id().toString() + "#" + clkey() : clkey(); }
		
		public ArrayList<Index> indexes() throws AppException {	return descr.indexes(cvalue()); }
		
		CItem copy(CDoc cd){
			CItem c = new CItem();
			c.cdoc = cd;
			c.descr = descr;
			c.version = version;
			c.key = key;
			c.value = value;
			c.nvalue = nvalue;
			c.blobSize = blobSize;
			c.nblobSize = nblobSize;
			c.deleted = deleted;
			c.toSave = toSave;
			c.toDelete = toDelete;
			return c;
		}
						
		void delete() throws AppException {	nvalue = null;	nblobSize = 0; setStatus(); }

		void commit(String json) throws AppException{
			if (json == null) json = "{}";
			nvalue = json.equals(value) ?  null : json;
			setStatus();
		}
		
		void commitRaw(String text) throws AppException{
			if (text == null) text = "";
			nvalue = text.equals(value) ? null : text;
			setStatus();
		}
		
		void commitP(P p){
			blobSize = p.size();
			String s = JSON.toJson(p);
			nvalue = s.equals(value) ? null : s;
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
		
}
