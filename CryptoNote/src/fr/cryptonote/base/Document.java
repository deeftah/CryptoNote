package fr.cryptonote.base;

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
	/* Instance ****************************/
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
		ExecContext.current().deleteDoc(id());;		
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

		public BItem getCopy() throws AppException { return _citem().descr().newItem(_citem.cvalue(), _citem.toString()); }
	}
	
	/********************************************************************************/
	public static final class P extends BItem {
		private String mime;
		private int size;
		private String sha;
		
		public String mime() { return mime; }
		public String sha() { return sha; }
		public int size() { return size; }
					
		public byte[] blobGet() throws AppException {
			_citem();
			return ExecContext.current().dbProvider().blobProvider().blobGet(sha);
		}
			
		public void delete() throws AppException{ _checkro("delete"); _citem().deleteBlob(); }

	}
	
	/********************************************************************************/
	public static class Item  extends BItem {
		public fr.cryptonote.base.CDoc.Status status() throws AppException { return _citem().status();	}
		public void delete() throws AppException{ _checkro("delete"); _citem().delete(); }
		public void commit() throws AppException{ _checkro("commit"); _citem().commit(JSON.toJson(this)); }
		
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
			S2Cleanup.startCleanup(true);
			ExecContext.current().dbProvider().blobProvider().blobStore(sha, bytes);
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

}
