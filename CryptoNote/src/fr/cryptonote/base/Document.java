package fr.cryptonote.base;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.CDoc.Status;
import fr.cryptonote.base.DocumentDescr.ExportedField;
import fr.cryptonote.base.DocumentDescr.ItemDescr;

public class Document {
	/********************************************************************************/
	public static class Id {
		private String docid = "";
		private DocumentDescr descr;
		
		public String docid() { return docid != null ? docid : ""; }
		public String docclass() { return descr != null ? descr.name() : ""; }
		public DocumentDescr descr() { return descr; }
		public String toString() { return docclass() + "." + docid; }
		public String toJson() { return JSON.toJson(new Hdr(this)); }
		
		@SuppressWarnings("unused")
		private Id() {}
		
		public Id(String s){
			if (s != null) {
				int i = s.indexOf('.');
				if (i != -1) {
					descr = DocumentDescr.get(s.substring(0,  i));
					docid = i < s.length() - 1 ? s.substring(i + 1) : "";
				}
			}
		}

		public Id(String docclass, String docid){
			descr = DocumentDescr.get(docclass);
			this.docid = docid;
		}

		public Id(Class<?> docclass, String docid){
			descr = docclass != null ? DocumentDescr.get(docclass.getSimpleName()) : null;
			if (docid != null) this.docid = docid;
		}
		
		ISyncFilter syncFilter(String filter) throws AppException {
			if (filter != null)
				try {
					return (ISyncFilter)JSON.fromJson(filter, descr().filterClass());
				} catch (Exception e) {
					throw new AppException("BJSONFILTER", filter, toString());
				}
			else
				return descr().defaultFilter();
		}
		
		public static Document.Id fromURL(String url) throws AppException {
			try {
				Document.Id id = new Document.Id(URLDecoder.decode(url == null ? "" : url, "UTF-8"));
				if (id.descr == null) throw new AppException("BTASKDOCUMENTID", url);
				return id;
			} catch (UnsupportedEncodingException e) {
				throw new AppException("BTASKDOCUMENTID", url);
			}
		}

	}

	/********************************************************************************/
	public static class ItemId {
		private String key;
		private ItemDescr descr;
		
		public String key() { return key != null ? key : ""; }
		public String itemClass() { return descr != null ? descr.name() : ""; }
		public ItemDescr descr() { return descr; }
		public DocumentDescr docDescr() { return descr != null ? descr.docDescr() : null; }
		
		@SuppressWarnings("unused")
		private ItemId() {}
		
		public ItemId(DocumentDescr docDescr, String s){
			if (s != null) {
				int i = s.indexOf('.');
				if (i != -1) {
					descr = docDescr != null ? docDescr.itemDescr(s.substring(0,  i)) : null;
					key = i < s.length() - 1 ? s.substring(i + 1) : "";
				}
			}
		}
		
		public ItemId(Class<?> docclass, Class<?> itemClass, String key){
			DocumentDescr docDescr = docclass != null ? DocumentDescr.get(docclass.getSimpleName()) : null;
			descr = docDescr != null && itemClass != null ? docDescr.itemDescr(itemClass.getSimpleName()) : null;
			if (key != null) this.key = key;
		}
		
		public String toString() { return itemClass() + (key == null ? "" : "." + key); }
	}

	/********************************************************************************/
	public enum FilterPolicy {Accept, Exclude, Continue};
	
	/** Méthode statique publique **/
	public static Document get(Document.Id id, int maxDelayInSeconds) throws AppException { return ExecContext.current().getDoc(id, maxDelayInSeconds); }
	public static Document getOrNew(Document.Id id) throws AppException { return ExecContext.current().getOrNewDoc(id); }
	public static Document newDoc(Document.Id id) throws AppException { 
		Document d = getOrNew(id);
		d.cdoc.delete();
		return d;
	}
	public static void delete(Document.Id id) throws AppException {	ExecContext.current().deleteDoc(id); }

	/** Méthodes à surcharger **/
	/* Filtre sur le document lui-même */
	public FilterPolicy filter(ISyncFilter sf) throws AppException { return FilterPolicy.Accept;}
	
	/* Filtre sur la classe et la clé item */
	public FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String key) throws AppException { return FilterPolicy.Accept; }
	
	/* Filtre sur le contenu d'un item */
	public FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String key, BItem item) throws AppException { return FilterPolicy.Accept; }

	/* Filtre sur le contenu d'un item raw */
	public FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String key, Text text){	return FilterPolicy.Accept; }

	public static class Text {
		public String src;
		public String filtered;
		public Text(String src) { this.src = src; }
	}
	/********************************************************************************/
	public interface XItemFilter {
		public boolean filter(XItem xitem);
	}

	public static Collection<Document.Id> searchDocIdsByIndexes(Class<?> docClass, Class<?> itemClass, Cond<?>... ffield) throws AppException {
		return ExecContext.current().dbProvider().searchDocIdsByIndexes(docClass, itemClass, ffield);
	}
	
	public static Collection<XItem> searchItemsByIndexes(Class<?> docClass, Class<?> itemClass, XItemFilter filter, Cond<?>... ffield) throws AppException {
		return ExecContext.current().dbProvider().searchItemsByIndexes(docClass, itemClass, filter, ffield); 
	}
	/***************************************************************************************/

	static Document newDocument(CDoc cdoc) throws AppException { Document d = cdoc.id().descr().newDocument(); d.cdoc = cdoc; return d; }

	/********************************************************************************/
	private CDoc cdoc;
	boolean isReadOnly;
	boolean isRO() { return isReadOnly; }
	
	public CDoc cdoc() { return cdoc; }
	public Document.Id id() { return cdoc.id(); }
	
	public Status status() { return cdoc.status(); }
	public long ctime() { return cdoc.ctime(); }
	public long version() { return cdoc.version(); }
	public long dtime() { return cdoc.dtime(); }

	/********************************************************************************/
	public void delete() throws AppException {
		if (isRO()) throw new AppException("BDOCUMENTRO", "delete", "Document", id().toString());
		cdoc.delete();
		ExecContext.current().deleteDoc(id());	
	}

	public Document recreate(){	cdoc.recreate(); return this; }
	
	public BItem item(CItem ci) throws AppException{
		if (ci == null) return null;
		if (ci.bitem != null) return ci.bitem;
		BItem item = ci.descr().newItem(ci.cvalue(), ci.toString());
		item._citem = ci;
		item._document = this;
		ci.bitem = item;
		return item;
	}

	private BItem bitem(Class<?> itemClass, boolean newIfNotExisting, String key) throws AppException {
		CItem ci = this.cdoc.citem(itemClass, newIfNotExisting, key);
		return item(ci);
	}

//	public CItemFilter browseAllItems(CItemFilter f) throws AppException {	return cdoc.browseAllItems(f); }
	
	private void set(BItem bitem, String key) throws AppException {
		bitem._citem = cdoc().citem(bitem, key);
		bitem._document = this;
	}

	/********************************************************************************/
	public static class XItem {
		public String docId;
		public String key;
		public long version;
		public BItem item;
		public XItem(Document.Id id, CItem ci) throws AppException{ 
			docId = id.docid(); 
			item = ci.descr().newItem(ci.cvalue(), ci.toString());
			key = ci.key();
			version = ci.version();
		}
	}

	/********************************************************************************/
	public static class ExportedFields extends HashMap<String,Object> {
		private static final long serialVersionUID = 1L;
	}
	
	public static class SerializedBItem {
		private ExportedFields exportedFields;
		private String serializedValue;
		public ExportedFields ef() { return exportedFields; }
		public String serializedValue() { return serializedValue; }
		private SerializedBItem(BItem bi) { exportedFields = bi.exportFields(); serializedValue = bi.serializedValue();
		}
	}
	/********************************************************************************/
	public static abstract class BItem {
		private transient CItem _citem;
		private transient Document _document;

		void _checkAttached() throws AppException {	if (_citem == null || _document == null) throw new AppException("BITEMDETACHED", getClass().getSimpleName()); }
		void _checkDetached() throws AppException {	if (_citem != null || _document != null) throw new AppException("BITEMATTACHED", getClass().getSimpleName()); }

		CItem _citem() throws AppException { _checkAttached(); return _citem; }
		public Document _document() throws AppException { _checkAttached(); return _document; }
		public int v1() throws AppException{ return _citem().v1(); }
		public int v2() throws AppException{ return _citem().v2(); }
		public ItemDescr descr() throws AppException { return _citem().descr(); }
		public boolean toSave() throws AppException { return _citem().toSave(); }
		public long version() throws AppException {	return _citem().version();	}
		public long vop() throws AppException {	return _citem().vop();	}
		public String key() throws AppException { return _citem().key(); }
		public void delete() throws AppException{ _checkro("delete"); _citem().delete(); }
		public abstract String serializedValue();
		public abstract ExportedFields exportFields();
		public SerializedBItem serializedBItem() { return new SerializedBItem(this) ; }
		public void _checkro(String method) throws AppException {
			if (_document == null) throw new AppException("BDOCUMENTRO", method, getClass().getSimpleName(), "?");
			if (_document.isRO())	throw new AppException("BDOCUMENTRO", method, getClass().getSimpleName(), _document.cdoc().id().toString());
		}
		
		void detach() {
			_document = null;
			if (_citem != null) {
				_citem.bitem = null;
				_citem = null;
			}
		}

	}
	
	/********************************************************************************/
	public static final class P extends BItem {
		private String mime;
		private int size;
		@IndexedField private String sha;
				
		public String mime() { return mime; }
		public String sha() { return sha; }
		public int size() { return size; }
		public void delete() throws AppException{ _checkro("delete"); _citem().delete(); }
		public String serializedValue() { return JSON.toJson(this); }
		public ExportedFields exportFields() { return null; }
		
		public byte[] blobGet() throws AppException {
			_citem();
			return ExecContext.current().dbProvider().blobProvider().blobGet(_document().id().toString(), sha);
		}
	}
	
	public P blobStore(String key, String mime, byte[] bytes) throws AppException {
		if (key == null || key.length() == 0) throw new AppException("BKEYBLOB", id().toString());
		if (mime == null || mime.length() == 0) throw new AppException("BMIMEBLOB", id().toString(), key);
		if (bytes == null || bytes.length == 0) throw new AppException("BEMPTYBLOB", id().toString(), key);
		String sha = Crypto.bytesToBase64(Crypto.SHA256(bytes), true);
		P p = (P)bitem(P.class, true, key);
		p.mime = mime;
		p.size = bytes.length;
		if (sha.equals(p.sha)) {
			String clid = id().toString();
			S2Cleanup.startCleanup(clid, true);
			ExecContext.current().dbProvider().blobProvider().blobStore(clid, sha, bytes);
			p.sha = sha;
		}
		p._citem().commitP(p);
		return p;
	}

	/********************************************************************************/
	public static abstract class ItemSingleton  extends BItem {
		public String serializedValue() { return JSON.toJson(this); }
		public void commit() throws AppException{ _checkro("commit"); _citem().commit(serializedValue(), exportFields()); }
		public BItem getCopy() throws AppException { return descr().newItem(_citem().cvalue(), _citem().toString()); }
		
		public final ExportedFields exportFields() {
			ExportedField[] fields = null;
			try { fields = descr().indexedFields(); } catch (AppException e) {	}
			if (fields == null || fields.length == 0) return null;
			ExportedFields val = new ExportedFields();
			for(ExportedField f : fields) val.put(f.name(), f.value(this));
			return val;
		}

		public void replaceIn(Document d, String key) throws AppException {
			_checkDetached();
			if (d == null) throw new AppException("BITEMATTACHED", getClass().getSimpleName());
			d.set(this, key);
		}
	}

	/********************************************************************************/
	public static abstract class RawItemSingleton  extends BItem {
		public transient String value;
		public String serializedValue() { return value; }
		public void commit() throws AppException{ _checkro("commit"); _citem().commitRaw(serializedValue(), exportFields()); }		
		
		/*
		 * Peut être surchargée
		 */
		public ExportedFields exportFields() { return null; }
	}

	/********************************************************************************/
	public static abstract class Singleton extends ItemSingleton { }
	public static abstract class Item extends ItemSingleton { }
	public static abstract class RawSingleton extends RawItemSingleton { }
	public static abstract class RawItem extends RawItemSingleton {	}
	/********************************************************************************/

	public P p(String key) throws AppException { return (P)bitem(P.class, false, key); }

	public String[] getPKeys() { return cdoc.itemids(P.class); }

	public Singleton singleton(Class<?> itemClass) throws AppException { return (Singleton)bitem(itemClass, false, null); }

	public Singleton singletonOrNew(Class<?> itemClass) throws AppException { return (Singleton)bitem(itemClass, true, null); }

	public Item item(Class<?> itemClass, String key) throws AppException { return (Item)bitem(itemClass, false, key); }

	public Item itemOrNew(Class<?> itemClass, String key) throws AppException {	return (Item)bitem(itemClass, true, key); }

	public RawItem rawItem(Class<?> itemClass, String key) throws AppException { return (RawItem)bitem(itemClass, false, key); }

	public RawItem rawItemOrNew(Class<?> itemClass, String key) throws AppException { return (RawItem)bitem(itemClass, true, key); }

	public String[] getKeys(Class<?> itemClass) { return cdoc.itemids(itemClass);}

	/********************************************************************************/
	public String toString(){ return JSON.toJson(new Hdr(this, version(), dtime())); }
	
	public String toJson() throws AppException { return toJson(null, 0, 0, 0, true); }

	public String toStringOpen(long v, long dt){ 
		String s = JSON.toJson(new Hdr(this, v, dt)); 
		return s.substring(0, s.lastIndexOf('}')); 
	}

	public static class Hdr {
		public String c;
		public String id;
		public long v;
		public long ct;
		public long dt;
		public Hdr() {}
		Hdr(Document doc, long v, long dt) { c = doc.id().docclass(); id = doc.id().docid(); this.v = v; ct = doc.ctime(); this.dt = dt; }
		public Document.Id id() { return new Document.Id(c, id); }
		Hdr(Document.Id id) {c = id.docclass(); this.id = id.docid();}
	}
	public static class Sync extends Hdr {
		public String filter; // Objet de classe docclass.SyncFilter en Json
	}
	
	/*
	 * La mémoire cache à remettre à niveau contient un exemplaire de `d` de version `v` et de `dtime` `dt`. 
	 * La mémoire source de la remise à niveau contient un exemplaire de `d` de version `vr` et de `dtime` `dtr`.  
	 * `items` contient toujours les items créés / recréés / modifiés après `v`.
	 * {
	 * "c":"C",
	 * "id":"abcd",
	 * "v":1712... ,
	 * "ct":1712... ,
	 * "dt":1712... ,
	 * "dels": [
	 *      {"c":"S4", "v":1712...},
	 *      {"c":"S5", "k":"def", "v":1712...}
	 *      ],
	 * "items": [
	 *      {"c":"S1", "v":1712... "j":{ l'item en JSON }},
	 *      {"c":"R1", "k":"def", "v":1712... "s":"texte d'un raw"},
	 *      {"c":"I1", "k":"abc", "v":1712... "j":{ l'item en JSON }}
	 *      ],
	 *  "clkeys":["S1", "I1.abc", "I1.def", "R1.def"... ]
	 * }
	 *  
	 * Document supprimé :  {"c":"C", "id":"abcd", "v":0 , "ct":1712... }
	 * 
	 * 1A
	 * source ----------dtr-----------vr
	 * cache  ----------------dt---v
	 * maj1   ----------------dt------vr suppr depuis dt / clkeys vide
	 * maj2   ----------------dt------vr suppr depuis dt / clkeys vide
	 * 
	 * 1B
	 * source ----------dtr-----------vr
	 * cache  ------dt-------------v
	 * maj1   ----------dtr-----------vr suppr depuis dtr / clkeys vide
	 * maj2   ----------dtr-----------vr suppr depuis dtr / clkeys vide
	 *  
	 * 2A
	 * source ----------dtr-----------vr
	 * cache  --dt---v
	 * maj1   ----------dtr-----------vr suppr depuis dtr / clkeys
	 * maj2   ------------------------vr vr/vr suppr vide / clkeys vide
	 *  
	 * 2B
	 * source ----------dtr-----------vr
	 * cache  v
	 * maj1   ----------dtr-----------vr suppr depuis dtr / clkeys
	 * maj2   ------------------------vr vr/vr suppr vide / clkeys vide
	 * 
	 * Sous option `maj1` on garde dans le document de mise à jour l'historique des destructions le plus large possible.  
	 * Sous option `maj2` on limite au maximum dans le document de mise à jour l'historique des destructions.  
	 * Dans le cas 2B, le cache ne connaît rien (`v = 0`).  
	 * Lorsque les `ctime` diffèrent, on se ramène au cas 2B.  
	 * `clkeys` ne contient pas les clés des items créés /recréés / modifiés qui figurent déjà dans `items`.
	 * 
	 * Après analyse de la situation de départ, les options de calcul sont :
	 * - Cas 1 : `clkeys` vide. Option maj1/maj2 ignorée.
	 *     - suppressions postérieures à dtx = max(dt, dtr)
	 *     - en sortie : vr->version ctime->ctime dtx->dtime
	 * - Cas 2 : option maj1. clkeys NON vide.
	 *     - suppressions postérieures à dtr
	 *     - en sortie : vr->version ctime->ctime dtr->dtime
	 * - Cas 2 : option maj2. suppressions vide. clkeys vide.
	 *     - en sortie : vr->version ctime->ctime vr->dtime
	*/
	
	/**
	 * Sérialisation du document Json en vue de mise à jour d'un exemplaire de cache.
	 * @param sf filtre pour ignorer le document ou certains items
	 * @param ctime date-heure de création de l'exemplaire du cache. 0 : pas d'exemplaire en cache, contenu intégral.
	 * @param version version de l'exemplaire du cache. Toujours postérieure ou égale à ctime. 0 : pas d'exemplaire, contenu intégral.
	 * @param dtime date-heure à partir de laquelle le cache détient les suppressions d'items. 
	 * Si version n'est pas 0, toujours antérieure à version.
	 * @param court si true, réduire l'historique des destructions.
	 * @return
	 * @throws AppException
	 */
	public String toJson(String filter, long ctime, long version, long dtime, boolean court) throws AppException {
		ISyncFilter sf = id().syncFilter(filter);
		sf.init(ExecContext.current(), this);
		StringBuffer sb = new StringBuffer();
		Status st = status();
		if (st == Status.deleted || st == Status.shortlived)
			return sb.append(toStringOpen(0,0)).append("}").toString();
		
		if (ctime != ctime()) version = 0;
		long vr = version();
		long dtr = dtime();
		boolean cas2 = version == 0 || version < dtr;
		
		boolean clk = cas2 && !court;  		// vrai si on produit clkeys
		boolean dlx = !(cas2 && court);		// vrai si on produit dels
		long dtx = cas2 ? dtr : (dtr < dtime ? dtime : dtr);	// si on produits dels : date-heure de filtre

		sb.append(toStringOpen(vr, dlx ? dtx : 0));
		if (filter(sf) != FilterPolicy.Accept) return sb.append("}").toString();
		
		ArrayList<CItem> items = cdoc.listAllItems();
		ArrayList<String> clkeys = clk ? new ArrayList<String>() : null;
		ArrayList<CItem> dels = dlx ? new ArrayList<CItem>() : null;
		
		sb.append(",\n\"items\":[");
		boolean pf = true;
		
		for(CItem ci : items) {
			ItemDescr descr = ci.descr();
			String key = ci.key();
			FilterPolicy fpid = filter(sf, descr, key);
			if (fpid == FilterPolicy.Exclude) continue;
			
			if (ci.deletedAfterV(version)) {
				if (dlx) dels.add(ci);
			} else {
				String val = null;
				if (fpid == FilterPolicy.Accept) {
					if (ci.changedAfterV(version))
						val = descr.isRaw() ? JSON.json(ci.cvalue()) : ci.cvalue();
					else if (clk && !ci.deleted())
							clkeys.add(ci.clkey());
				} else {
					if (!descr.isRaw()) {
						BItem item;
						try { item = (BItem)JSON.fromJson(ci.cvalue(), descr.clazz()); } catch (Exception e) { continue; }
						FilterPolicy fpit = sf == null ? FilterPolicy.Accept : filter(sf, descr, key, item);
						if (fpit == FilterPolicy.Exclude) continue;
						if (ci.changedAfterV(version))
							val = fpit == FilterPolicy.Accept ? ci.cvalue() : JSON.toJson(item);
						else if (clk && !ci.deleted())
							clkeys.add(ci.clkey());
					} else {
						Text t = new Text(ci.cvalue());
						FilterPolicy fpit = sf == null ? FilterPolicy.Accept : filter(sf, descr, key, t);
						if (fpit == FilterPolicy.Exclude) continue;
						if (ci.changedAfterV(version))						
							val = fpit == FilterPolicy.Accept ? JSON.json(ci.cvalue()) : JSON.json(t.filtered);
						else if (clk && !ci.deleted())
							clkeys.add(ci.clkey());						
					}
				}
				if (val != null) {
					if (!pf) sb.append(",");
					ci.jsonExist(sb, val);
					pf = false;
				}
			}
		}
		sb.append("]");
		
		if (dlx) {
			sb.append(",\n\"dels\":[");
			pf = true;
			for (CItem ci : dels) {
				if (!pf) sb.append(",");
				ci.jsonDel(sb);
				pf = false;
			}
			sb.append("]");
		}

		if (clk) {
			String x = JSON.toJson(clkeys);
			sb.append(",\n\"clkeys\":").append(x);
		}

		return sb.append("}").toString();
	}

}
