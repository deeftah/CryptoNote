package fr.cryptonote.base;

import java.util.HashMap;

import fr.cryptonote.provider.DBProvider;

public class OnOff {
	public static final String NAMESPACE = "admin";

	private static Long nextReload = 0L;
	private static HashMap<String,Integer> status = new HashMap<String,Integer>();
	
	public static HashMap<String,Integer> status() { 
		return status; 
	}
	
	public static int status(String ns, boolean local) {
		reload();
		Integer s1 = status.get(ns);
		if (s1 == null) s1 = 0;
		Integer s2 = status.get("z");
		if (s2 == null) s2 = 0;
		return local ? s1 : (s1 < s2 ? s2 : s1);
	}

	static void reload() {
		long now = System.currentTimeMillis();
		if (now > nextReload) 
			synchronized(nextReload){
				try {
					DBProvider provider = BConfig.getDBProvider(BConfig.defaultBase()).ns(NAMESPACE);
					status = provider.getOnOff();
				} catch (Exception e) {}
				nextReload = now + (BConfig.NSSCANPERIODINSECONDS() * 1000);
		}
	}
	
	/*****************************************************************/
	public static class Sudo extends Operation {
		
		@Override public Result work() throws AppException {
			if (!isSudo())	throw new AppException("SADMINOP");
			return Result.empty();
		}
		
	}
	
	/*****************************************************************/
	public static class SetOnOff extends Operation {
		public static class Param {
			String ns;
			int onoff;
		}
		private Param param;
		
		@Override public Result work() throws AppException {
			if (!isSudo())	throw new AppException("SADMINOP");
			if (param.onoff < 0) param.onoff = 0;
			String nsc = execContext().nsqm().code;
			if (!nsc.equals(NAMESPACE)) {
				param.ns = nsc;
				if (BConfig.nsqm(param.ns, false) == null) throw new AppException("ANSUNKNOWN", param.ns);
			} else {
				if (!"z".equals(param.ns) && BConfig.nsqm(param.ns, false) == null) throw new AppException("ANSUNKNOWN", param.ns);
			}
			execContext().dbProvider().setOnOff(param.ns, param.onoff);
			return Result.empty();
		}
		
		@Override public void afterWork() throws AppException {
			nextReload = 0L;
			reload();
		}
	}

	/*****************************************************************/
	public static class GetOnOff extends Operation {
		public static class Param {
		}
		@SuppressWarnings("unused")
		private Param param;
		
		@Override public Result work() throws AppException {
			String nsc = execContext().nsqm().code;
			if (!nsc.equals(NAMESPACE)) {
				HashMap<String,Integer> st = new HashMap<String,Integer>();
				st.put(nsc, status(nsc, false));
				st.put("z", status("z", false));
				return Result.json(st);
			} else {
				return Result.json(status);				
			}
		}
		
	}

}
