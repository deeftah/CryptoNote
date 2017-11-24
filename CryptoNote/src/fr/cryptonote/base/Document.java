package fr.cryptonote.base;

import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.CDoc.Status;

public class Document {
	/********************************************************************************/
	public static class Id {
		private String docid;
		private String docclass;
		private DocumentDescr descr;
		
		public String docid() { return docid != null ? docid : ""; }
		public String docclass() { return docclass != null ? docclass : ""; }
		public DocumentDescr descr() { return descr; }
		
		public Id(String s){
			if (s != null) {
				int i = s.indexOf('.');
				if (i != -1) {
					docclass = s.substring(0,  i);
					descr = DocumentDescr.get(docclass);
					if (i < s.length() - 1)
						docid = s.substring(i + 1);
				}
			}
		}
		
		public String toString() { return docclass + "." + docid; }
	}
	
	/********************************************************************************/
	/* Instance ****************************/
	private CDoc cdoc;
	private boolean isReadOnly = false;
	private boolean isExclusive = false;
	
	public CDoc cdoc() { return cdoc; }
	public boolean isReadOnly() { return isReadOnly; }
	void setReadOnly() { isReadOnly = true; }
	public boolean isExclusive() { return isExclusive; }
	
	public Status status() { return cdoc.status(); }
	public long ctime() { return cdoc.ctime(); }
	public long version() { return cdoc.version(); }
	
	/********************************************************************************/
	public static abstract class BItem {
		private transient CItem _citem;
		private transient Document _document;

		void _checkAttached() throws AppException {
			if (_citem == null || _document == null) throw new AppException("BITEMDETACHED", getClass().getSimpleName());
		}
		void _checkDetached() throws AppException {
			if (_citem != null || _document != null) throw new AppException("BITEMATTACHED", getClass().getSimpleName());
		}

		CItem _citem() throws AppException { _checkAttached(); return _citem; }
		public Document _document() throws AppException { _checkAttached(); return _document; }
		public int v1() throws AppException{ return _citem().v1(); }
		public int v2() throws AppException{ return _citem().v2(); }

		public BItem _checkro() throws AppException {
			if (_document == null || _document.isReadOnly())
				throw new AppException("BDOCUMENTRO", "key(val)", getClass().getSimpleName());
			return this;
		}
		
		
		void detach() {
			_document = null;
			if (_citem != null) {
				_citem.bitem = null;
				_citem = null;
			}
		}

		public boolean hasValue() throws AppException {
			Status s = _citem().status();
			if (s == Status.deleted || s == Status.shortlived) return false;
			if (s == Status.created || s == Status.recreated)
				return _citem.cvalue() != null;
			return true;
		}
				
		public BItem getCopy() throws AppException {
			return _citem().descr().newItem(_citem.cvalue(), _citem.toString());
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
					
//			public Status status() throws AppException {
//				return _citem().status();
//			}
//			public long version() throws AppException {
//				return _citem().version();			
//			}
//			public String key() throws AppException {
//				return _citem().key();						
//			}
//			public void key(String key) throws AppException{
//				_checkro()._citem().key(key);
//			}
//
//			public byte[] blobGet() throws AppException {
//				_citem();
//				return ExecContext.current().dbProvider().blobProvider().blobGet(_document().id().groupid(), sha);
//			}
//			
//			public void delete() throws AppException{
//				_checkro()._citem().deleteBlob();			
//			}

	}
	
	/********************************************************************************/
	public static class Item  extends BItem {
		public fr.cryptonote.base.CDoc.Status status() throws AppException { return _citem().status();	}
		public long version() throws AppException {
			return _citem().version();			
		}
		public String key() throws AppException {
			return _citem().key();						
		}
		public void key(String key) throws AppException{
			_checkro()._citem().key(key);
		}
		public void delete() throws AppException{
			_checkro()._citem().delete();			
		}
		public void commit() throws AppException{
			_checkro()._citem().commit(JSON.toJson(this));
		}
		
//		public void replaceIn(Document d, String key) throws AppException {
//			_checkDetached();
//			if (d == null)
//				throw new AppException("BITEMATTACHED", getClass().getSimpleName());
//			d.set(this, key);
//		}
		
	}

	/********************************************************************************/
	public static class Singleton extends BItem {
		public Status status() throws AppException {
			return _citem().status();
		}
		public long version() throws AppException {
			return _citem().version();			
		}
		public void delete() throws AppException{
			_checkro()._citem().delete();			
		}
		public void commit() throws AppException{
			_checkro()._citem().commit(JSON.toJson(this));
		}
		
//		public void replaceIn(Document d) throws AppException {
//			_checkDetached();
//			if (d == null)
//				throw new AppException("BITEMATTACHED", getClass().getSimpleName());
//			d.set(this, "");
//		}

	}

	/********************************************************************************/
	public static class RawItem  extends BItem {
		public Status status() throws AppException {
			return _citem().status();
		}
		public long version() throws AppException {
			return _citem().version();			
		}
		public String key() throws AppException {
			return _citem().key();						
		}
		public void key(String key) throws AppException{
			_checkro()._citem().key(key);
		}
		public void delete() throws AppException{
			_checkro()._citem().delete();			
		}
		public String rawText() throws AppException{
			return _citem().cvalue();
		}
		public void commit(String text) throws AppException{
			_checkro()._citem().commitRaw(text);
		}		
	}

	/********************************************************************************/
	public static class RawSingleton extends BItem {
		public Status status() throws AppException {
			return _citem().status();
		}
		public long version() throws AppException {
			return _citem().version();			
		}
		public void delete() throws AppException{
			_checkro()._citem().delete();			
		}
		public String rawText() throws AppException{
			return _citem().cvalue();
		}
		public void commit(String text) throws AppException{
			_checkro()._citem().commitRaw(text);
		}				
	}

	/********************************************************************************/


}
