package fr.cryptonote.base;

import java.util.HashMap;

public abstract class Const {
	private static class HH {
		private HashMap<String,Const> byKey = new HashMap<String,Const>();
		private HashMap<String,Const> byAlias = new HashMap<String,Const>();
		private void put(Const c) {
			byKey.put(c.key, c);
			if (c.alias != null) byAlias.put(c.alias, c);
		}
	}
	
	public String key;
	public String alias;
	
	private static final HashMap<String, HH> allConsts = new HashMap<String, HH>();
	
	public void put() throws AppException {
		String cl = this.getClass().getSimpleName();
		HH hh = allConsts.get(cl);
		if (hh == null) {
			hh = new HH();
			allConsts.put(cl, hh);
		}
		ExecContext.current().dbProvider().setConst(cl + "/" + key, alias == null ? null : cl + "/" + alias, JSON.toJson(this));
		hh.put(this);
	}

	public static Const getByKey(Class<?> clazz, String key) throws AppException {
		String cl = clazz.getSimpleName();
		HH hh = allConsts.get(cl);
		if (hh == null) {
			hh = new HH();
			allConsts.put(cl, hh);
		}
		Const c = hh.byKey.get(key);
		if (c != null) return c;
		String json = ExecContext.current().dbProvider().getConstByKey(cl + "/" + key);
		c = json == null ? null : (Const)JSON.fromJson(json, clazz);
		if (c != null)
			hh.put(c);
		return c;
	}

	public static Const getByAlias(Class<?> clazz, String alias) throws AppException {
		String cl = clazz.getSimpleName();
		HH hh = allConsts.get(cl);
		if (hh == null) {
			hh = new HH();
			allConsts.put(cl, hh);
		}
		Const c = hh.byAlias.get(alias);
		if (c != null) return c;
		String json = ExecContext.current().dbProvider().getConstByAlias(cl + "/" + alias);
		c = json == null ? null : (Const)JSON.fromJson(json, clazz);
		if (c != null)
			hh.put(c);
		return c;
	}

}
