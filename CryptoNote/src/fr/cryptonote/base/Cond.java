package fr.cryptonote.base;

public class Cond<T> {
	private static final Class<?>[] classIndexes = {String.class, Integer.class, Long.class, Double.class};
	
	public enum Type {eq, sw, le, lt, ge, gt, gele, gelt, gtle, gtlt};
	
	private int classIndex = 0;
	public int classIndex() { return classIndex; }
	
	private Class<?> clazz;
	public Class<?> clazz() { return clazz; }
	
	private T v1;
	public T v1() { return v1; };
	
	private T v2;
	@SuppressWarnings("unchecked")
	public T v2() { return type == Type.sw ? (T)(v1.toString() + '\u1FFF') : v2; };
	
	private Type type;
	public Type type() { return type; }
	
	private String name;
	public String name() { return name; }
	public Cond<T> name(String name) { this.name = name; return this; }
	
	public boolean has2Args(){
		return type == Type.sw || type.ordinal() > Type.gt.ordinal();
	}
	
	public String toString(){
		if (!has2Args())
			return "Filter sur (" + clazz.getSimpleName() + ") " + type.toString() + " v1=["
				+ v1.toString() + "]";
		return "Filter sur (" + clazz.getSimpleName() + ") " + type.toString() + " v1=["
			+ v1.toString() + "] v2=["+ v2.toString() + "]";
	}
	
	public String toSql(){
		switch (type) {
		case eq : return " " + this.name + " = ? ";
		case sw : return " " + this.name + " >= ? and " + this.name + " < ? " ;
		case le : return " " + this.name + " <= ? ";
		case lt : return " " + this.name + " < ? ";
		case ge : return " " + this.name + " >= ? " ;
		case gt : return " " + this.name + " > ? " ;
		case gtle : return " " + this.name + " > ? and " + this.name + " <= ? ";
		case gtlt : return " " + this.name + " > ? and " + this.name + " < ? " ;
		case gele : return " " + this.name + " >= ? and " + this.name + " <= ? " ;
		case gelt : return " " + this.name + " >= ? and " + this.name + " < ? " ;
		}
		return "";
	}
	
	@SuppressWarnings("unchecked")
	public Cond(String value) { this(Type.eq, (T)value); }
	@SuppressWarnings("unchecked")
	public Cond(Integer value) { this(Type.eq, (T)value); }
	@SuppressWarnings("unchecked")
	public Cond(Long value) { this(Type.eq, (T)value); }
	@SuppressWarnings("unchecked")
	public Cond(Double value) { this(Type.eq, (T)value); }

	public Cond(Type type, T value){
		if (type == null) 
			throw new IllegalArgumentException("Filter : type non null requis");
		if (value == null) 
			throw new IllegalArgumentException("Filter : valeur non null requise");
		this.type = type;
		if (has2Args()) 
			throw new IllegalArgumentException("Filter " + type + " : 2 valeurs requises");
		classIndex = 0;
		clazz = value.getClass();
		for(Class<?> c : classIndexes) if (clazz == c) break; else classIndex++;
		if (classIndex >= classIndexes.length)
			throw new IllegalArgumentException("Filter " + type + " : classes interdite (ni String int long double");
		if (type == Type.sw && classIndex != 0)
			throw new IllegalArgumentException("Filter " + type + " : 2 valeurs requises");
		this.v1 = value;
	}
	
	public Cond(Type type, T value1, T value2){
		if (type == null) 
			throw new IllegalArgumentException("Filter : type non null requis");
		if (value1 == null || value2 == null) 
			throw new IllegalArgumentException("Filter : 2 valeurs non null requises");
		this.type = type;
		if (!has2Args()) 
			throw new IllegalArgumentException("Filter " + type + " : 1 seule valeur requise");
		classIndex = 0;
		clazz = value1.getClass();
		for(Class<?> c : classIndexes) if (clazz == c) break; else classIndex++;
		if (classIndex >= classIndexes.length)
			throw new IllegalArgumentException("Filter " + type + " : classes interdite (ni String int long double");
		this.v1 = value1;
		this.v2 = value2;
	}

	@SuppressWarnings("unchecked")
	private T nullValue(Object value){
		switch (classIndex) {
		case 0: return (T)"";
		case 1: return (T)new Integer(0);
		case 2: return (T)new Long(0);
		case 3: return (T)new Double(0); 
		}
		return null;
	}

	public boolean match(Object value){
		if (value == null) value = nullValue(value);
		switch (classIndex) {
		case 0: {
			switch (type) {
			case eq : return ((String)value).equals((String)v1);
			case sw : return ((String)value).startsWith((String)v1) ;
			case le : return ((String)value).compareTo((String)v1) <= 0 ;
			case lt : return ((String)value).compareTo((String)v1) < 0 ;
			case ge : return ((String)value).compareTo((String)v1) >= 0 ;
			case gt : return ((String)value).compareTo((String)v1) > 0 ;
			case gtle : return ((String)value).compareTo((String)v1) > 0 
					&& ((String)value).compareTo((String)v2) <= 0 ;
			case gtlt : return ((String)value).compareTo((String)v1) > 0 
					&& ((String)value).compareTo((String)v2) < 0 ;
			case gele : return ((String)value).compareTo((String)v1) >= 0 
					&& ((String)value).compareTo((String)v2) <= 0 ;
			case gelt : return ((String)value).compareTo((String)v1) >= 0 
					&& ((String)value).compareTo((String)v2) < 0 ;
			}
		}
		case 1: {
			switch (type) {
			case eq : return ((Integer)value).equals((Integer)v1);
			case le : return ((Integer)value).compareTo((Integer)v1) <= 0 ;
			case lt : return ((Integer)value).compareTo((Integer)v1) < 0 ;
			case ge : return ((Integer)value).compareTo((Integer)v1) >= 0 ;
			case gt : return ((Integer)value).compareTo((Integer)v1) > 0 ;
			case gtle : return ((Integer)value).compareTo((Integer)v1) > 0 
					&& ((Integer)value).compareTo((Integer)v2) <= 0 ;
			case gtlt : return ((Integer)value).compareTo((Integer)v1) > 0 
					&& ((Integer)value).compareTo((Integer)v2) < 0 ;
			case gele : return ((Integer)value).compareTo((Integer)v1) >= 0 
					&& ((Integer)value).compareTo((Integer)v2) <= 0 ;
			case gelt : return ((Integer)value).compareTo((Integer)v1) >= 0 
					&& ((Integer)value).compareTo((Integer)v2) < 0 ;
			default:
				return false;
			}
		}
		case 2: {
			switch (type) {
			case eq : return ((Long)value).equals((Long)v1);
			case le : return ((Long)value).compareTo((Long)v1) <= 0 ;
			case lt : return ((Long)value).compareTo((Long)v1) < 0 ;
			case ge : return ((Long)value).compareTo((Long)v1) >= 0 ;
			case gt : return ((Long)value).compareTo((Long)v1) > 0 ;
			case gtle : return ((Long)value).compareTo((Long)v1) > 0 
					&& ((Long)value).compareTo((Long)v2) <= 0 ;
			case gtlt : return ((Long)value).compareTo((Long)v1) > 0 
					&& ((Long)value).compareTo((Long)v2) < 0 ;
			case gele : return ((Long)value).compareTo((Long)v1) >= 0 
					&& ((Long)value).compareTo((Long)v2) <= 0 ;
			case gelt : return ((Long)value).compareTo((Long)v1) >= 0 
					&& ((Long)value).compareTo((Long)v2) < 0 ;
			default:
				return false;
			}			
		}
		case 3: {
			switch (type) {
			case eq : return ((Double)value).equals((Double)v1);
			case le : return ((Double)value).compareTo((Double)v1) <= 0 ;
			case lt : return ((Double)value).compareTo((Double)v1) < 0 ;
			case ge : return ((Double)value).compareTo((Double)v1) >= 0 ;
			case gt : return ((Double)value).compareTo((Double)v1) > 0 ;
			case gtle : return ((Double)value).compareTo((Double)v1) > 0 
					&& ((Double)value).compareTo((Double)v2) <= 0 ;
			case gtlt : return ((Double)value).compareTo((Double)v1) > 0 
					&& ((Double)value).compareTo((Double)v2) < 0 ;
			case gele : return ((Double)value).compareTo((Double)v1) >= 0 
					&& ((Double)value).compareTo((Double)v2) <= 0 ;
			case gelt : return ((Double)value).compareTo((Double)v1) >= 0 
					&& ((Double)value).compareTo((Double)v2) < 0 ;
			default:
				return false;
			}			
		}
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public static Cond fromJson(String json){
		if (json == null || json.length() == 0)
			throw new IllegalArgumentException("Filter.fromJson : json vide");
		Json jx;
		try {
			jx = JSON.fromJson(json, Cond.Json.class);
		} catch (Exception e){
			throw new IllegalArgumentException("Filter.fromJson : syntaxe incorrecte", e);			
		}
		return jx.toFilter();
	}
	
	private static String nullValue(int classIndex, String value){
		if (value == null || value.length() == 0)
			switch (classIndex) {
			case 0: return "";
			case 1: return "0";
			case 2: return "0";
			case 3: return "0"; 
			}
		return value;
	}

	private static class Json {
		public String t;
		public String v1;
		public String v2;
		
		public Cond<?> toFilter(){
			if (t == null || t.length() < 3)
				throw new IllegalArgumentException("Filter.Json : classe/type incorrect");
			String x = t.toLowerCase();
			int i = "sild".indexOf(x.charAt(0));
			if (i == -1)
				throw new IllegalArgumentException("Filter.Json : classe [" +
						t.charAt(0) + " n'est ni S I L D s i l d");
			Type type;
			try {
				type = Type.valueOf(x.substring(1));
			} catch(Exception e){
				throw new IllegalArgumentException("Filter.Json : type [" +
						t.substring(1) + " n'est ni eq, sw, le, lt, ge, gt, gele, gelt, gtle, gtlt");				
			}
			
			v1 = nullValue(i, v1);
			if (type.ordinal() > 5)
				v2 = nullValue(i, v2);
			
			switch (i) {
			case 0: {
				if (type.ordinal() <= 5)
					return new Cond<String>(type, v1);
				return new Cond<String>(type, v1, v2);
			}
			case 1: {
				int x1;
				int x2;
				try {
					x1 = Integer.parseInt(v1);
				} catch (Exception e) {
					throw new IllegalArgumentException("Filter.Json : valeur [" + v1 + "] pas int");
				}
				if (type.ordinal() <= 5)
					return new Cond<Integer>(type, x1);
				try {
					x2 = Integer.parseInt(v2);
				} catch (Exception e) {
					throw new IllegalArgumentException("Filter.Json : valeur [" + v2 + "] pas int");
				}
				return new Cond<Integer>(type, x1, x2);
			}
			case 2: {
				long x1;
				long x2;
				try {
					x1 = Long.parseLong(v1);
				} catch (Exception e) {
					throw new IllegalArgumentException("Filter.Json : valeur [" + v1 + "] pas long");
				}
				if (type.ordinal() <= 5)
					return new Cond<Long>(type, x1);
				try {
					x2 = Long.parseLong(v2);
				} catch (Exception e) {
					throw new IllegalArgumentException("Filter.Json : valeur [" + v2 + "] pas long");
				}
				return new Cond<Long>(type, x1, x2);
			}
			case 3: {
				double x1;
				double x2;
				try {
					x1 = Double.parseDouble(v1);
				} catch (Exception e) {
					throw new IllegalArgumentException("Filter.Json : valeur [" + v1 + "] pas double");
				}
				if (type.ordinal() <= 5)
					return new Cond<Double>(type, x1);
				try {
					x2 = Double.parseDouble(v2);
				} catch (Exception e) {
					throw new IllegalArgumentException("Filter.Json : valeur [" + v2 + "] pas double");
				}
				return new Cond<Double>(type, x1, x2);				
			}
			}
			return null;
		}
	}
	
	public static void main(String[] args){
		try {
			Cond<Integer> f1 = new Cond<Integer>(Type.gele, 3, 7);
			System.out.println(f1.match(3));
			System.out.println(f1.match(4));
			System.out.println(f1.match(8));						
			
			Cond<?> f;
			
			f = Cond.fromJson("{\"t\":\"seq\", \"v1\":\"toto\"}");
			System.out.println(f.match("toto"));
			System.out.println(f.match("toti"));

			f = Cond.fromJson("{\"t\":\"ssw\", \"v1\":\"toto\"}");
			System.out.println(f.match("toto"));
			System.out.println(f.match("totototo"));
			System.out.println(f.match("totitoto"));

			f = Cond.fromJson("{\"t\":\"sle\", \"v1\":\"toto\"}");
			System.out.println(f.match("toto"));
			System.out.println(f.match("totz"));
			System.out.println(f.match("toti"));

			f = Cond.fromJson("{\"t\":\"sgelt\", \"v1\":\"totb\", \"v2\":\"toty\"}");
			System.out.println(f.match("toto"));
			System.out.println(f.match("totb"));
			System.out.println(f.match("toty"));
			
			f = Cond.fromJson("{\"t\":\"dgele\", \"v1\":\"3\", \"v2\":\"7\"}");
			System.out.println(f.match(3.0));
			System.out.println(f.match(4.0));
			System.out.println(f.match(8.0));

		} catch(Throwable t){
			t.printStackTrace();
		}
	}
}
