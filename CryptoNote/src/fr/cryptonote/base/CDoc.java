package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.HashMap;

import fr.cryptonote.base.Document.BItem;
import fr.cryptonote.base.Document.P;
import fr.cryptonote.base.DocumentDescr.ItemDescr;

public class CDoc {
	
	// zombie : document créé et détruit dans la même opération
	/*
	 *- `unchanged` : aucun item n'a changé sur le document qui existait avant le début de l'opération.
	 *- `modified` : le document existait avant le début de l'opération et un ou des items ont été créés / modifiés / supprimés.
	 *- `created` : le document n'existait pas et il a été créé par un `getOrNew()`. Il n'a pas forcément d'items.
	 *- `recreated` : le document existait et a été recréé par la méthode `recreate()`. 
	 * Sa date-heure de création n'est pas la même qu'au début de l'opération et son contenu a complètement été purgé par la suppression survenue avant sa recréation.
	 *- `deleted` : le document existait mais a été supprimé par l'opération.
	 *- `shortlived` : le document n'existait pas, a été créé par un `getOrNew()` et a été supprimé ensuite au cours de l'opération.
	 *- `oldtrace` : pour un item seulement : était supprimé avant l'opération et n'a pas été recréé.
	 */
	
	public enum Status {unchanged, modified, created, recreated, deleted, shortlived, oldtrace};	
	public static final int nbStatus = Status.values().length;

	/********************************************************************************/
	private Document.Id id;
	private long version;
	private long ctime;
	private long dtime;

	Document.Id id() { return id; }
	public long version() { return version; }
	public long ctime() { return ctime; }
	public long dtime() { return dtime; }

	private Status status;
	public Status status() { return status; };

	private HashMap<String,CItem> sings = new HashMap<String,CItem>();
	private HashMap<String,HashMap<String,CItem>> colls = new HashMap<String,HashMap<String,CItem>>();
	
	private long v1;
	private long v2;
	private long v1Before;
	private long v2Before;
	long v1Delta;
	long v2Delta;

	public boolean toSave() { return status == Status.created || status == Status.recreated || status == Status.modified; }
	void recreate() { status = Status.recreated; clearAllItems(); }
	void delete() {	status = version() == 0 ? Status.shortlived : Status.deleted; clearAllItems(); }
	private void clearAllItems() {
		sings.clear();
		for(HashMap<String,CItem> cis : colls.values()) cis.clear();
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
		public boolean accept(CItem ci) {
			Status st = ci.status();
			return st == Status.created || st == Status.recreated || st == Status.deleted || st == Status.modified;
		}
	}

	public static class CItemFilterExisting extends CItemFilter {
		public boolean accept(CItem ci) {
			Status st = ci.status();
			return st != Status.deleted && st != Status.shortlived && st != Status.oldtrace;
		}
	}

	public static final CItemFilter filterToSave = new CItemFilterToSave();
	public static final CItemFilter filterExisting = new CItemFilterExisting();

	public static class Stats extends CItemFilter {
		int[] byStatus = new int[nbStatus];
		int v1 = 0;
		long v2 = 0;
		
		public boolean accept(CItem ci) {
			Status st = ci.status();
			byStatus[st.ordinal()]++;
			v1 += ci.v1();
			v2 += ci.v2();
			return true;
		}
		
		int nbChanges() { return byStatus[Status.created.ordinal()] + byStatus[Status.modified.ordinal()]	+ byStatus[Status.deleted.ordinal()]; }
		
	}

	void summarize() throws AppException {
		if (status != Status.deleted && status != Status.shortlived) {
			Stats stats = new Stats();
			browseAllItems(stats);
			if (status != Status.created && status != Status.recreated)
				status = stats.nbChanges() != 0 ? Status.modified : Status.unchanged;
			v1 = stats.v1;
			v2 = stats.v2;
		} else {
			v1 = 0;
			v2 = 0;
		}
		v1Delta = v1 - v1Before;
		v2Delta = v2 - v2Before;
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
	/*
	 * N'est utilisé QUE pour cloner le CDoc en cache en une copie pour l'opération
	 * Ce dernier est cohérent (summarize() inutile)
	 */
	protected synchronized CDoc clone() {
		CDoc c = new CDoc();
		c.status = status;
		c.version = version;
		c.id = id;
		c.v1Before = v1;
		c.v2Before = v2;
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
	 * Créé un CDoc correctement identifié, avec header / state vides et pas d'items
	 * @param id
	 */
	static CDoc newEmptyCDoc(Document.Id id){ 
		CDoc cdoc = new CDoc();
		for(ItemDescr itd : id.descr().itemDescrs())
			if (!itd.isSingleton()) cdoc.colls.put(itd.name(), new HashMap<String,CItem>());
		cdoc.status = Status.created;
		return cdoc;
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
				ci.sha = p.sha();
				ci.nsha = null;
				ci.size = p.size();
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
				cis.put(key, ci);
				return ci;
			}
		}
		// ci existait mais il n'a peut-être pas de contenu réel (deleted oldtrace shortlived).
		Status st = status();
		if (st == Status.deleted || st == Status.oldtrace || st == Status.deleted) {
			if (newIfNotExisting) {
				ci.deleted = false;
				return ci;
			} else
				return null;
		}
		return ci;
	}

	CItem citem(BItem bitem, String key) throws AppException{
		Class<?> clazz = bitem.getClass();
		ItemDescr descr = id().descr().itemDescr(clazz.getSimpleName());
		if (descr == null)
			throw new AppException("BITEMCLASS", clazz.getSimpleName());
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
			if (ci.bitem != null) ci.bitem.detach();
			ci.bitem = bitem;
			ci.nvalue = JSON.toJson(bitem);
		} else {
			HashMap<String,CItem> cis = colls.get(descr.name());
			if (cis == null)
				throw new AppException("BITEMCLASSCOL", clazz.getSimpleName());
			ci = cis.get(key);
			if (ci == null) {
				ci = new CItem();
				ci.descr = descr;
				ci.cdoc = this;
				ci.key = key;
				cis.put(key, ci);
			}
			if (ci.bitem != null) ci.bitem.detach();
			ci.bitem = bitem;
			ci.nvalue = JSON.toJson(bitem);
		}
		return ci;
	}

	/********************************************************************************/
	public static class CItem {
		private transient CDoc cdoc;
		private transient ItemDescr descr;
		transient BItem bitem;
		
		private long version;	// version d'un item qui existait avant (value != null) ou une trace de suppression (value == null)
		private String key;		// key d'un item, null pour un singleton
		private String value; 	// contenu avant. null pour une trace de suppression ou un item nouvellement créé
		private String nvalue; 	// nouveau contenu. null si l'item était existant et inchangé ou une trace de suppression (sans recréation)
		private int size;		// taille du blob
		private String sha;		// sha du blob
		private String nsha;	// sha du blob après changement
		private boolean deleted;// a subi une suppression.
		
		public ItemDescr descr() { return descr; }
		public long version() { return version; }
		public String key() { return descr().isSingleton() ? "" : key; }
		public String cvalue() { return nvalue != null ? nvalue : (value != null ? value : null); }
		public String sha() { return sha; }
		public String nsha() { return nsha; }
		public String clkey() { return descr.name() + (descr.isSingleton() ? "" : "." + key); }
				
		/*
		 *- `unchanged` : l'item existait avant le début de l'opération, existe toujours et n'a pas changé.
		 *- `modified` : l'item existait avant le début de l'opération, existe toujours mais a changé.
		 *- `created` : l'item n'existait pas (même en trace de suppression) et a été créé par un `getOrNew()`.
		 *- `recreated` : l'item n'existait pas mais avait une trace de suppression et a été recréé par un `getOrNew()`. 
		 *- `deleted` : l'item existait mais a été supprimé par l'opération.
		 *- `shortlived` : l'item n'existait pas, a été créé par un `getOrNew()` et a été supprimé ensuite au cours de l'opération.
		 *- `oldtrace` : l'item était supprimé avant l'opération (avait une trace de suppression) et n'a pas été recréé.
		 */

		public Status status(){
			if (version == 0) return nvalue == null ? Status.shortlived : Status.created;
			if (value == null) return nvalue == null ? Status.oldtrace : Status.recreated;
			return nvalue == null ? (deleted ? Status.deleted : Status.unchanged) : Status.modified;
		}
		
		public int v1() {
			Status st = status();
			if (st == Status.deleted || st == Status.shortlived || st == Status.oldtrace) return 0;
			return (nvalue != null ? nvalue.length() : (value != null ? value.length() : 0)) + (key != null ? key.length() : 0);
		}

		public int v2() {
			if (!descr.isP()) return 0;
			Status st = status();
			if (st == Status.deleted || st == Status.shortlived || st == Status.oldtrace) return 0;
			return size;
		}

		public boolean hasValue() throws AppException {
			Status s = status();
			return s == Status.created || s == Status.recreated || s == Status.unchanged || s == Status.modified;
		}

		public String toString() { 
			return descr().docDescr().name() + "." + (cdoc != null ? cdoc.id().docid() : "") + "#" + descr.name() + (descr.isSingleton() || key == null ? "" :  "." + key); 
		}
		
		public ArrayList<Index> indexes() throws AppException {	return descr.indexes(cvalue()); }
		
		CItem copy(CDoc cd){
			CItem c = new CItem();
			c.cdoc = cd;
			c.descr = descr;
			c.version = version;
			c.key = key;
			c.value = value;
			c.nvalue = nvalue;
			c.sha = sha;
			c.size = size;
			c.nsha = nsha;
			c.deleted = deleted;
			return c;
		}
		
		void check(boolean singl, boolean raw) throws AppException {
			Status st = status();
			if (st == Status.deleted || st == Status.shortlived || st == Status.oldtrace) throw new AppException("BITEMSTATUSDEL", toString());
			if (singl && descr.isSingleton()) throw new AppException("BITEMSINGL", toString());
			if (raw && descr.isRaw()) throw new AppException("BITEMSRAW", toString());			
		}
				
		void delete() throws AppException {	check(false, false); value = null;	deleted = true; }

		void deleteBlob() throws AppException {
			check(true, true);
			value = null;
			nsha = null;
			size = 0;
		}

		void commit(String json) throws AppException{
			if (json == null) json = "{}";
			nvalue = json.equals(value) ?  null : json;
			check(false, true);
		}
		
		void commitRaw(String text) throws AppException{
			if (text == null) text = "";
			if (!descr.isRaw())	throw new AppException("BITEMNRAW", toString());			
			nvalue = text.equals(value) ? null : text;
			check(false, false);
		}
		
		void commitP(P p){
			this.nsha = p.sha();
			this.size = p.size();
			String s = JSON.toJson(p);
			nvalue = s.equals(value) ? null : s;
		}

		void json(StringBuffer sb) { 
			sb.append("\n{\"v\":").append(version).append(", \"c\":\"").append(descr.name()).append("\"");
			if (!descr.isSingleton()) sb.append(", \"k\":").append(JSON.json(key));
			sb.append("}");
		}

		void json(StringBuffer sb, String val) { 
			sb.append("\n{\"v\":").append(version).append(", \"c\":\"").append(descr.name()).append("\"");
			if (!descr.isSingleton()) sb.append(", \"k\":").append(JSON.json(key));
			if (descr.isRaw()) sb.append(", \"s\":"); else sb.append(", \"s\":");
			sb.append(val).append("}");
		}

	}
		
}
