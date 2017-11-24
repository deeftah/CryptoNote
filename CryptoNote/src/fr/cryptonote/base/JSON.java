package fr.cryptonote.base;

import java.util.Hashtable;

//import com.google.appengine.repackaged.com.google.gson.Gson;
//import com.google.appengine.repackaged.com.google.gson.GsonBuilder;
//import com.google.appengine.repackaged.com.google.gson.JsonObject;
//import com.google.appengine.repackaged.com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JSON {
	
	private static final Gson gson = new Gson();
	public static String json(String s) {
		synchronized (gson) {
			return gson.toJson(s);
		}
	}
	
	private static Hashtable<Class<?>, GsonCache<?>> gsonObjects = new Hashtable<Class<?>, GsonCache<?>>();
	private static class GsonCache<T> {
		private Class<?> clazz;
		private Gson std;
		private Gson stdp;
		@SuppressWarnings("unchecked")
		private synchronized T fromJson(String json) throws AppException {
			try {
				return std.fromJson(json, (Class<T>) clazz);
			} catch (Exception ex){
				throw new AppException(ex, "BJSONPARAMPARSE", clazz.getName());
			}
		}
		private synchronized String toJson(Object obj, boolean pretty){
			return pretty ? stdp.toJson(obj) : std.toJson(obj);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static GsonCache<?> getCache(Class<?> clazz) {
		GsonCache<?> gsonCache = gsonObjects.get(clazz);
		if (gsonCache == null) {
			gsonCache = new GsonCache();
			gsonCache.clazz = (Class<?>) clazz;
			gsonCache.std = new GsonBuilder().create();
			gsonCache.stdp = new GsonBuilder().setPrettyPrinting().create();
			gsonObjects.put((Class<?>) gsonCache.clazz, gsonCache);
		}
		return gsonCache;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T fromJson(String json, Class<T> clazz) throws AppException {
		if (json == null || json.length() == 0 || clazz == null) return null;
		return ((GsonCache<T>) getCache(clazz)).fromJson(json);
	}

	public static String toJson(Object obj, boolean pretty) {
		if (obj == null) return "null";
		return getCache(obj.getClass()).toJson(obj, pretty);
	}

	public static String toJson(Object obj) {
		if (obj == null) return "null";
		return getCache(obj.getClass()).toJson(obj, false);
	}

	public static JsonObject rawParse(String json){
		return new JsonParser().parse(json).getAsJsonObject();
	}

	public static void main(String[] args) {
		System.out.println("[" + json("toto") + "]");
		System.out.println("[" + json("to\"to") + "]");
		System.out.println("[" + json(null) + "]");
		System.out.println("[" + json("\\.\"") + "]");
	}
}
