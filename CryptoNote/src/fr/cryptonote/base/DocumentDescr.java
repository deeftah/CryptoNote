package fr.cryptonote.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

import fr.cryptonote.base.Document.BItem;

public class DocumentDescr {
	public enum FieldType {String, Long, Int, Double, StringArray, LongArray, IntArray, DoubleArray };
	
	public static final DocumentDescr FAKE = new DocumentDescr();
	
	private static final Class<?>[] indexClasses = { String.class, long.class, int.class, double.class,
			String[].class, long[].class, int[].class, double[].class};
	
	public static boolean isArray(FieldType type) {
		return type == null ? false : type.ordinal() >= FieldType.StringArray.ordinal();
	}
	
	public static Class<?> classOfValues(FieldType type) {
		if (type == null)
			return null;
		return indexClasses[type.ordinal()];
	}
	
	public static FieldType idxTypeOfClass(Class<?> clazz){
		for(int i = 0; i < indexClasses.length; i++)
			if (indexClasses[i] == clazz) return FieldType.values()[i];
		return null;
	}

	private static ItemDescr pDescr;
	private static ItemDescr qDescr;

	static {
		pDescr = new ItemDescr();
		pDescr.clazz = Document.P.class;
		pDescr.name = "P";
	}
	
	private String name;
	private Class<?> clazz;
	private Class<?> taskClass;
	private Class<?> filterClass;
	private Constructor<?> constructor;
	private Hashtable<Class<?>, ItemDescr> itemDescrs1 = new Hashtable<Class<?>, ItemDescr>();
	private Hashtable<String, ItemDescr> itemDescrs2 = new Hashtable<String, ItemDescr>();

	public String name() { return name; }
	public Class<?> clazz() { return clazz; }
	public Class<?> taskClass() { return taskClass; }	
	public boolean isTaskDocument() { return taskClass != null; }	
	public Class<?> filterClass() { return filterClass; }
	public Collection<ItemDescr> itemDescrs() { return itemDescrs2.values(); }
	public ItemDescr itemDescr(String name) { return itemDescrs2.get(name); }
	public ItemDescr itemDescr(Class<?> itemClass) { return itemDescrs1.get(itemClass); }
	
	public ISyncFilter defaultFilter() throws AppException{
		try { return (ISyncFilter)filterClass.newInstance();
		} catch (Exception e){	throw new AppException(e, "BDOCUMENTCLASS7", name); }
	}
	
	public Document newDocument() throws AppException{
		try { return (Document)constructor.newInstance();
		} catch (Exception e){ throw new AppException(e, "BDOCUMENTCLASS6", name); }
	}
	
	public static class ItemDescr {
		private DocumentDescr docDescr;
		private String name;
		private Class<?> clazz;
		private ExportedField[] exportedFields;
		private Constructor<?> constructor;
		private boolean isRaw = false;
		private boolean isSingleton = false;
		private Class<?>[] docclasses;
		private char separator;
		private DocumentDescr[] copyToDocs;
		
		public DocumentDescr[] copyToDocs() {
			if (copyToDocs == null) {
				ArrayList<DocumentDescr> x = new ArrayList<DocumentDescr>();
				if (docclasses != null && docclasses.length != 0) for(Class<?> c : docclasses) {
					DocumentDescr d = documentDescrs1.get(c);
					if (d != null) x.add(d);
				}
				copyToDocs = x.toArray(new DocumentDescr[x.size()]);
			}
			return copyToDocs;
		};
		public char separator() { return separator; }
		public boolean hasDifferedCopy() { return copyToDocs().length != 0 ; }
		public ExportedField[] exportedFields() { return exportedFields; }
		
		public DocumentDescr docDescr() { return docDescr; };
		public String name() { return name; }
		public Class<?> clazz() { return clazz; }
		
		public boolean isP() { return this == pDescr; }
		public boolean isQ() { return this == qDescr; }
		public boolean isRaw() { return isRaw; }
		public boolean isSingleton() { return isSingleton; }
		
		public BItem newItem(String json, String info) throws AppException {
			if (isRaw() || json == null || json.length() == 0)
				try { 
					BItem bi = (BItem)constructor.newInstance();
					if (isRaw()) {
						if (isSingleton())
							((Document.RawSingleton)bi).value = json;
						else
							((Document.RawItem)bi).value = json;
					}
					return bi;
				} catch(Exception e) { throw new AppException(e, "BDOCUMENTITEM", info); }
			else
				try { return (BItem)JSON.fromJson(json, clazz);
				} catch (AppException e){ throw new AppException(e.cause(), "BDOCUMENTITEM", info); }
		}

	}
	
	public static class ExportedField {
		private String name;
		private FieldType type;
		private Field field;
		private ExportedField() {}
		
		public String name() { return name; }
		public FieldType type() { return type; }
		public Object value(Object obj){
			if (obj == null) return nullValue();
			try {
				return field.get(obj);
			} catch (Exception e) {
				return nullValue();
			}
		}

		private ExportedField(Class<?> clazz, String fieldName){
			try {
				field = clazz.getDeclaredField(fieldName);
				name = fieldName;
				type = idxTypeOfClass(field.getType());
				field.setAccessible(true);
			} catch (NoSuchFieldException | SecurityException e) {
			}
		}

		private ExportedField(Field f, String docName, String itemName) throws AppException{
			name = f.getName();
			type = idxTypeOfClass(f.getType());
			if (type == null)
				throw new AppException("BDOCUMENTCLASS4", docName, itemName, name);
			field = f;
			f.setAccessible(true);
		}

		private Object nullValue() {
			switch (type) {
			case Double : return (double)0; 
			case Long : return (long)0; 
			case Int : return (int)0;
			default: return null;
			}
		}
		
	}
	
	private static final Hashtable<Class<?>, DocumentDescr> documentDescrs1 = new Hashtable<Class<?>, DocumentDescr>();
	private static final Hashtable<String, DocumentDescr> documentDescrs2 = new Hashtable<String, DocumentDescr>();
	
	public static DocumentDescr get(String docClassName) {return documentDescrs2.get(docClassName); }
	public static DocumentDescr get(Class<?> docClass) {return documentDescrs1.get(docClass); }
		
	public static DocumentDescr register(Class<?> clazz)throws AppException {
		if (clazz == null) throw new AppException("BDOCUMENTCLASS0");
		DocumentDescr dd = documentDescrs1.get(clazz);
		if (dd != null) return dd;
		if (!Document.class.isAssignableFrom(clazz)) 
			throw new AppException("BDOCUMENTCLASS1", clazz.getName());
		dd = new DocumentDescr();
		dd.clazz = clazz;
		dd.name = clazz.getSimpleName();
		DocumentDescr dd2 = documentDescrs2.get(dd.name);
		if (dd2 != null)
			throw new AppException("BDOCUMENTCLASS5", clazz.getName(), dd2.clazz.getName());
		
		try {
			dd.constructor = clazz.getDeclaredConstructor();
			dd.constructor.setAccessible(true);
		} catch (Exception e) {	
			throw new AppException(e, "BDOCUMENTCLASS10", dd.name);
		}

		dd.itemDescrs1.put(pDescr.clazz, pDescr);
		dd.itemDescrs2.put(pDescr.name, pDescr);

		Class<?>[] classes = clazz.getDeclaredClasses();
		for(Class<?> cl : classes){
			if (ISyncFilter.class.isAssignableFrom(cl)) {
				dd.filterClass = cl;
				continue;
			};
			
			if (Operation.class.isAssignableFrom(cl)) {
				if (cl.getSimpleName().equals("Task")){
					dd.taskClass = cl;
					Operation.register(cl, dd.name);
				} else
					Operation.register(cl);
				continue;
			}
			
			if (!Document.BItem.class.isAssignableFrom(cl)) continue;

			String n = cl.getSimpleName();
			if (!Modifier.isStatic(cl.getModifiers()))
				throw new AppException("BDOCUMENTCLASS3", n, dd.name);
			ItemDescr itd = new ItemDescr();
			itd.docDescr = dd;
			
			if (Document.Singleton.class.isAssignableFrom(cl)) {
				itd.isSingleton = true;
				itd.isRaw = false;
			} else if (Document.RawSingleton.class.isAssignableFrom(cl)) {
				itd.isSingleton = true;
				itd.isRaw = true;
			} else if (Document.Item.class.isAssignableFrom(cl)) {
				itd.isSingleton = false;
				itd.isRaw = false;
			} else if (Document.RawItem.class.isAssignableFrom(cl)) {
				itd.isSingleton = false;
				itd.isRaw = true;
			} else
				continue;
			
			itd.name = n;
			itd.clazz = cl;
			
			try {
				itd.constructor = cl.getDeclaredConstructor();
				itd.constructor.setAccessible(true);
			} catch (Exception e) {	
				throw new AppException(e, "BDOCUMENTCLASS9", dd.name + "." + n);
			}
			
			ADifferedCopy dc = itd.clazz.getAnnotation(ADifferedCopy.class);
			if (dc != null) {
				itd.docclasses = dc.copyToDocs();
				itd.separator = dc.separator();
			}
			
			dd.itemDescrs1.put(cl, itd);
			dd.itemDescrs2.put(n, itd);
			
			ArrayList<ExportedField> lst = new ArrayList<ExportedField>();
			HashMap<String,Field> af = Util.getAllField(cl, null, ExportedField.class);
			for(Field f : af.values())
				lst.add(new ExportedField(f, dd.name, itd.name));
			if (lst.size() != 0) 
				itd.exportedFields = lst.toArray(new ExportedField[lst.size()]);
		}
		
		documentDescrs1.put(clazz, dd);
		documentDescrs2.put(dd.name, dd);
		
		return dd;
	}

	public static DocumentDescr documentDescrNoExc(String name) { return documentDescrs2.get(name);}

	public static DocumentDescr documentDescrNoExc(Class<?> clazz) { return documentDescrs1.get(clazz);}

	public static DocumentDescr documentDescr(String name) throws AppException {
		DocumentDescr dd = documentDescrs2.get(name);
		if (dd == null) throw new AppException("BDOCUMENTCLASS2", name);
		return dd;
	}

}
