package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.CDoc.CItemFilter;
import fr.cryptonote.base.CDoc.Status;
import fr.cryptonote.base.DocumentDescr.ItemDescr;

public class Document {
	/********************************************************************************/
	public static class Id {
		private String docid = "";
		private DocumentDescr descr;
		
		public String docid() { return docid != null ? docid : ""; }
		public String docclass() { return descr != null ? descr.name() : ""; }
		public DocumentDescr descr() { return descr; }
		
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
		
		public Id(Class<?> docclass, String docid){
			descr = docclass != null ? DocumentDescr.get(docclass.getSimpleName()) : null;
			if (docid != null) this.docid = docid;
		}
		
		public String toString() { return docclass() + "." + docid; }
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
	public static Document newDoc(Document.Id id) throws AppException { return ExecContext.current().newDoc(id); }
	public static void delete(Document.Id id) throws AppException {	ExecContext.current().deleteDoc(id);;}

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
		return ExecContext.current().dbProvider().searchDocIdsByIndexes(docClass.getSimpleName(), itemClass.getSimpleName(), ffield);
	}
	
	public static Collection<XItem> searchItemsByIndexes(Class<?> docClass, Class<?> itemClass, XItemFilter filter, Cond<?>... ffield) throws AppException {
		return ExecContext.current().dbProvider().searchItemsByIndexes(docClass.getSimpleName(), itemClass.getSimpleName(), filter, ffield); 
	}
	/***************************************************************************************/

	static Document newDocument(CDoc cdoc) throws AppException { Document d = cdoc.id().descr().newDocument(); d.cdoc = cdoc; return d; }

	/********************************************************************************/
	private CDoc cdoc;
	private boolean isReadOnly = false;
	
	public CDoc cdoc() { return cdoc; }
	public Document.Id id() { return cdoc.id(); }
	public boolean isReadOnly() { return isReadOnly; }
	void setReadOnly() { isReadOnly = true; }
	
	public Status status() { return cdoc.status(); }
	public long ctime() { return cdoc.ctime(); }
	public long version() { return cdoc.version(); }
	public long dtime() { return cdoc.dtime(); }

	/********************************************************************************/
	public void delete() throws AppException {
		if (isReadOnly()) throw new AppException("BDOCUMENTRO", "delete", "Document", id().toString());
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

	public CItemFilter browseAllItems(CItemFilter f) throws AppException {	return cdoc.browseAllItems(f); }
	
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
	public static abstract class BItem {
		private transient CItem _citem;
		private transient Document _document;

		void _checkAttached() throws AppException {	if (_citem == null || _document == null) throw new AppException("BITEMDETACHED", getClass().getSimpleName()); }
		void _checkDetached() throws AppException {	if (_citem != null || _document != null) throw new AppException("BITEMATTACHED", getClass().getSimpleName()); }

		CItem _citem() throws AppException { _checkAttached(); return _citem; }
		public Document _document() throws AppException { _checkAttached(); return _document; }
		public int v1() throws AppException{ return _citem().v1(); }
		public int v2() throws AppException{ return _citem().v2(); }
		public boolean hasValue() throws AppException {	return _citem().hasValue(); }
		public ItemDescr descr() throws AppException { return _citem().descr(); }
		public Status status() throws AppException { return _citem().status();	}
		public long version() throws AppException {	return _citem().version();	}
		public String key() throws AppException { return _citem().key(); }
		public void delete() throws AppException{ _checkro("delete"); _citem().delete(); }

		public void _checkro(String method) throws AppException {
			if (_document == null) throw new AppException("BDOCUMENTRO", method, getClass().getSimpleName(), "?");
			if (_document.isReadOnly())	throw new AppException("BDOCUMENTRO", method, getClass().getSimpleName(), _document.cdoc().id().toString());
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
		private String sha;
				
		public String mime() { return mime; }
		public String sha() { return sha; }
		public int size() { return size; }
		public void delete() throws AppException{ _checkro("delete"); _citem().deleteBlob(); }
			
		public byte[] blobGet() throws AppException {
			_citem();
			return ExecContext.current().dbProvider().blobProvider().blobGet(_document().id().toString(), sha);
		}
			
	}
	
	/********************************************************************************/
	public static class Item  extends BItem {
		public fr.cryptonote.base.CDoc.Status status() throws AppException { return _citem().status();	}
		public void commit() throws AppException{ _checkro("commit"); _citem().commit(JSON.toJson(this)); }
		public BItem getCopy() throws AppException { return descr().newItem(_citem().cvalue(), _citem().toString()); }

		public void replaceIn(Document d, String key) throws AppException {
			_checkDetached();
			if (d == null)
				throw new AppException("BITEMATTACHED", getClass().getSimpleName());
			d.set(this, key);
		}
		
	}

	/********************************************************************************/
	public static class Singleton extends BItem {
		public void commit() throws AppException{ _checkro("commit"); _citem().commit(JSON.toJson(this)); }
		public BItem getCopy() throws AppException { return descr().newItem(_citem().cvalue(), _citem().toString()); }
		
		public void replaceIn(Document d) throws AppException {
			_checkDetached();
			if (d == null)
				throw new AppException("BITEMATTACHED", getClass().getSimpleName());
			d.set(this, "");
		}

	}

	/********************************************************************************/
	public static class RawItem  extends BItem {
		public String rawText() throws AppException{ return _citem().cvalue(); }
		public void commit(String text) throws AppException{ _checkro("commit"); _citem().commitRaw(text); }		
	}

	/********************************************************************************/
	public static class RawSingleton extends BItem {
		public String rawText() throws AppException{ return _citem().cvalue(); }
		public void commit(String text) throws AppException{ _checkro("commit"); _citem().commitRaw(text); }		
	}

	/********************************************************************************/
	public P blobStore(String key, String mime, byte[] bytes) throws AppException {
		if (key == null || key.length() == 0) throw new AppException("BKEYBLOB", id().toString());
		if (mime == null || mime.length() == 0) throw new AppException("BMIMEBLOB", id().toString(), key);
		if (bytes == null || bytes.length == 0) throw new AppException("BEMPTYBLOB", id().toString(), key);
		String sha = Crypto.bytesToBase64(Crypto.SHA256(bytes), true);
		P p = (P)bitem(P.class, true, key);
		if (p.sha != null && p.sha.equals(sha)) {
			p.mime = mime;
			p.size = bytes.length;
		} else {	
			String clid = id().toString();
			S2Cleanup.startCleanup(clid, true);
			ExecContext.current().dbProvider().blobProvider().blobStore(clid, sha, bytes);
			p.mime = mime;
			p.size = bytes.length;
			p.sha = sha;
		}
		p._citem().commitP(p);
		return p;
	}

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
	public String toString(){ return JSON.toJson(new Hdr(this, version())); }

	public String toStringOpen(long dt){ 
		String s = JSON.toJson(new Hdr(this, dt)); 
		return s.substring(0, s.lastIndexOf('}')); 
	}

	private static class Hdr {
		@SuppressWarnings("unused")	private String docclass;
		@SuppressWarnings("unused")	private String docid;
		@SuppressWarnings("unused")	private long version;
		@SuppressWarnings("unused")	private long ctime;
		@SuppressWarnings("unused")	private long dtime;
		Hdr(Document d, long dt) { docclass = d.id().docclass(); docid = d.id().docid(); version = d.version(); ctime = d.ctime(); dtime = dt; }		
	}
	
	public static class CItemFilterDeleted extends CItemFilter {
		private long dtime;
		public CItemFilterDeleted(long dtime) { this.dtime = dtime; }
		public boolean accept(CItem ci) {
			if (ci.version() <= dtime) return false;
			Status st = ci.status();
			return st == Status.deleted || st == Status.oldtrace;
		}
	}

	public static class CItemFilterModified extends CItemFilter {
		private long version;
		public CItemFilterModified(long version) { this.version = version; }
		public boolean accept(CItem ci) {
			if (ci.version() <= version) return false;
			Status st = ci.status();
			return st == Status.modified || st == Status.created || st == Status.recreated;
		}
	}

	public static class CItemFilterExistingBut extends CItemFilter {
		private HashSet<String> but;
		public CItemFilterExistingBut(HashSet<String> but) { this.but = but; }
		public boolean accept(CItem ci) {
			if (but != null && but.contains(ci.clkey())) return false;
			Status st = ci.status();
			return st != Status.deleted && st != Status.shortlived && st != Status.oldtrace;
		}
	}

	/* 
	 * La mémoire cache à remettre à niveau contient un exemplaire de `d` de version `v` et de `dtime` `dt`.  
	 * La mémoire source de la remise à niveau contient un exemplaire de `d` de version `vr` et de `dtime` `dtr`.  
	 */ 

	/* Forme générale de sortie
	    {
	    "docclass":"C",
	    "docid":"abcd",
	    "version":1712... ,
	    "ctime":1712... ,
	    "dtime":1712... ,
	    "items": [
	        {"c":"S2", "v":1712... "t":"texte d'un raw"},
	        {"c":"S1", "v":1712... "i":{ l'item en JSON }},
	        {"c":"R1", "k":"def", "v":1712... "s":"texte d'un raw"},
	        {"c":"I1", "k":"abc", "v":1712... "j":{ l'item en JSON }},
	    ],
	   	"clkeys":["S1", "I1.abc", "I1.def", "R1.def"... ]
	    }
		`keys` n'est présent que dans le cas 2 où la `version` détenue par le cache à remettre à niveau est antérieure à la `dtime` de la source.  
		Les items détruits :
		- n'ont pas de valeur `s` ou `j`;
		- sont à inclure si leur `version` est postérieure à,
		    - cas 1 : la plus récente des deux `dtime` de la source et du cache.
		    - cas 2 : la `dtime` de la source.
		
		Les items créés, recréés, modifiés postérieurement à la version du cache sont à inclure.
	 */
	
	public String toJson() throws AppException { return toJson(null, 0, 0); }

	public String toJson(ISyncFilter sf, long v, long dt) throws AppException{
		if (dt == 0) dt = version();
		long dtr = dtime();
		boolean cas2 = v < dtr;
		long dtx = cas2 ? dtr : (dtr < dt ? dt : dtr);

		StringBuffer sb = new StringBuffer();
		sb.append(toStringOpen(dtx));
		if (filter(sf) != FilterPolicy.Accept) return sb.append("}").toString();
		
		ArrayList<CItem> items = cdoc.listAllItems(new CItemFilterModified(v));
		
		HashSet<String> clex = new HashSet<String>();
		
		sb.append(",\n\"items\":[");
		boolean pf = true;
		
		ArrayList<CItem> dels = cdoc.listAllItems(new CItemFilterDeleted(dtx));
		for (CItem ci : dels) {
			FilterPolicy fpid = filter(sf, ci.descr(), ci.key());
			if (fpid == FilterPolicy.Exclude) continue;
			if (!pf) sb.append(",");
			ci.json(sb);
			pf = false;
		}

		for(CItem ci : items) {
			String val;
			ItemDescr descr = ci.descr();
			String key = ci.key();
			FilterPolicy fpid = filter(sf, descr, key);
			if (fpid == FilterPolicy.Exclude) continue;
			if (fpid == FilterPolicy.Accept)
				val = descr.isRaw() ? JSON.json(ci.cvalue()) : ci.cvalue();
			else {
				if (!descr.isRaw()) {
					BItem item;
					try { item = (BItem)JSON.fromJson(ci.cvalue(), descr.clazz()); } catch (Exception e) { continue; }
					FilterPolicy fpit = sf == null ? FilterPolicy.Accept : filter(sf, descr, key, item);
					if (fpit == FilterPolicy.Exclude) continue;
					val = fpit == FilterPolicy.Accept ? ci.cvalue() : JSON.toJson(item);
				} else {
					Text t = new Text(ci.cvalue());
					FilterPolicy fpit = sf == null ? FilterPolicy.Accept : filter(sf, descr, key, t);
					if (fpit == FilterPolicy.Exclude) continue;
					val = fpit == FilterPolicy.Accept ? JSON.json(ci.cvalue()) : JSON.json(t.filtered);
				}
			}
			if (!pf) sb.append(",");
			ci.json(sb, val);
			pf = false;
			clex.add(ci.clkey());
		}
		
		if (cas2) {
			ArrayList<String> clkeys = cdoc.listAllClKeys(new CItemFilterExistingBut(clex));
			String x = JSON.toJson(clkeys);
			sb.append(",\n\"clkeys\":").append(x);
		}
		
		sb.append("]");
		return sb.append("}").toString();
	}

}
