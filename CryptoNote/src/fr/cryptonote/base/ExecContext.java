package fr.cryptonote.base;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletException;

import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.CDoc.Status;
import fr.cryptonote.base.Cache.XCDoc;
import fr.cryptonote.base.Document.BItem;
import fr.cryptonote.base.Document.ExportedFields;
import fr.cryptonote.base.Document.SerializedBItem;
import fr.cryptonote.base.Document.Sync;
import fr.cryptonote.base.DocumentDescr.ItemDescr;
import fr.cryptonote.base.Servlet.InputData;
import fr.cryptonote.provider.DBProvider;

public class ExecContext {	
	private static boolean isDebug ;
		
	/*******************************************************************************************/
	private static HashMap<String,int[]> statsOp = new HashMap<String,int[]>();
	private static HashMap<String,int[]> statsD = new HashMap<String,int[]>();
	
	/*
	 * 0 : nb ok retry 0
	 * 1 : 1
	 * 2 : 2
	 * 
	 * 5 : nb errors
	 * 6 : lapse op
	 * 7 : lapse validation
	 */
	
	private static void stats(String ns, String op, int... s){
		if (op == null || op.length() == 0 || s == null) return;
		synchronized (statsOp){
			int[] a = statsOp.get(op);
			if (a == null) a = new int[8];
			for(int i = 0; i < s.length && i < 8; i++){
				int c = s[i];
				if (c > 0) a[i] += c;
			}
			statsOp.put(op,  a);
		}
		if (ns == null || ns.length() == 0) ns = "0";
		synchronized (statsD){
			int[] a = statsD.get(ns);
			if (a == null) a = new int[8];
			for(int i = 0; i < s.length && i < 8; i++){
				int c = s[i];
				if (c > 0) a[i] += c;
			}
			statsD.put(ns,  a);
		}
	}
	
	private static String getStatsOp() { synchronized (statsOp){ return JSON.toJson(statsOp); } }
	private static String getStatsD() {	synchronized (statsD){ return JSON.toJson(statsD); } }

	/*******************************************************************************************/
	public class Chrono {
		private long start;		
		public Chrono() { start = System.currentTimeMillis(); }	
		public Chrono(long start) {	this.start = start;}
		public int lapse(){	return (int)(System.currentTimeMillis() - start);}
		public String toString(){
			String l = "" + lapse();
			if (l.length() < 3) return l + "ms";
			return l.substring(0,  l.length() - 3) + "." + l.substring(l.length() - 3) + "s";
		}
	}

	/*******************************************************************************************/

	public static ExecContext current() { return ExecContextTL.get(); }
	private static final ThreadLocal<ExecContext> ExecContextTL = new ThreadLocal<ExecContext>();

	private int iLang = 0;
	private String ns;
	private Stamp startTime;
	private Stamp startTime2;
	private Chrono chrono;
	private long maxTime;
	private InputData inputData;
	private Operation operation;
	protected int phase = 0;
	private StringWriter sw;
	private DBProvider dbProvider;
	private HashMap<String,Object> appData = new HashMap<String,Object>();
	
	ExecContext() throws ServletException {
		ExecContextTL.set(this);
		startTime = Stamp.fromNow(0);
		chrono = new Chrono(startTime.epoch());
	}

	ExecContext setLang(String lang) { iLang = AConfig._iLang(lang); return this; }
	
	/*
	 * N'est invoqué que depuis Servlet init / get / post qui ne supporte que du ServletException
	 */
	ExecContext setNS(String namespace) throws ServletException {
		if (ns != null) return this;
		ns = namespace;
		try {
			NS.reload();
			return this;
		} catch (AppException e) {
			throw new ServletException(e);
		}
	}
	
	public int iLang() { return iLang; }
	public String lang() { return AConfig.config().lang(iLang); }
	public int phase() {return phase; }	
	public String operationName() { return inputData.operationName(); }
	public Operation operation() { return operation; }
	public String ns() { return ns == null ? AConfig.config().nsz() : ns; }
	public boolean isTask() { return inputData.taskId() != null; }
	public InputData inputData() { return inputData; }
	
	public Object getAppData(String name) {	return appData.get(name); }
	public void setAppData(String name, Object obj) { appData.put(name, obj); }
	public Set<String> getAppDataNames() { return appData.keySet(); }

	public final boolean hasTraces() { return sw == null; }
	public final String traces() { return sw == null ? "" : sw.toString();}
	public void trace(String msg){
		if (sw == null) { sw = new StringWriter(); sw.append(startTime.toString()).append(" - Start\n"); }
		sw.append(chrono.toString() + " - ").append(msg).append("\n");
	}
	
	public DBProvider dbProvider() throws AppException{	if (dbProvider == null)	dbProvider = AConfig.config().newDBProvider(ns()); 	return dbProvider; }

	public void maxTime() throws AppException { if (!isDebug && startTime.lapseInMs() > maxTime) throw new AppException("XMAXTIME", operationName()); }

	void closeAll() { if (dbProvider != null) dbProvider.closeConnection(); }
		
	private int xAdmin = 0;
	public boolean hasAdminKey() {
		if (xAdmin == 0) {
			String key = inputData.args().get("key");
			if (key == null) 
				xAdmin = -1;
			else 
				try {
					if (isDebug && key.equals("nonuke"))
						xAdmin = 1;
					else {
						String x = Crypto.bytesToBase64(Crypto.SHA256(Crypto.base64ToBytes(key)), true);
						xAdmin = AConfig.config().isSecretKey(x) ? 1 : -1;
					}
				} catch (Exception e) {	xAdmin = -1; }
		}
		return xAdmin > 0;
	}

	public boolean hasQmKey() {	return AConfig.config().isQmSecretKey(inputData.args().get("key")); }
	
	public boolean isQM() { return isTask() && hasQmKey(); }

	Stamp startTime2(){ if (startTime2 == null)	startTime2 = Stamp.fromNow(0); return startTime2; }

	/*******************************************************************************************/
	// tâches à inscrire au QM
	HashMap<String,TaskInfo> tasks = new HashMap<String,TaskInfo>();
	HashMap<String,Document> docs = new HashMap<String,Document>();
	HashSet<String> emptyDocs = new HashSet<String>();
	HashSet<String> dels = new HashSet<String>();
	TaskUpdDiff updDiff;
	
	private void clearCaches(){ tasks.clear(); docs.clear(); updDiff = null; dels.clear(); }

	public void newTask(Document.Id id, long nextStart, String info) throws AppException{
		tasks.put(id.toString(), new TaskInfo(ns(), id, nextStart, 0, info));
	}

	public void putUpdDiff(Document.Id id, String key, BItem item) throws AppException{
		if (updDiff == null)
			updDiff = (TaskUpdDiff)getOrNewDoc(new Document.Id(TaskUpdDiff.class, Crypto.randomB64(2)));
		updDiff.add(id, key, item);
	}

	void deleteDoc(Document.Id id) throws AppException {
		if (id == null) throw new AppException("BDELDOC");
		String k = id.toString();
		dels.add(k);
		Document d = docs.get(k);
		if (d != null) d.cdoc().delete();
	}

	/*
	 * Recherche du document dans le contexte, en cache/base s'il n'y est pas et mémorisation éventuelle
	 */
	Document getDoc(Document.Id id, int maxDelayInSeconds) throws AppException {
		String n = id.toString();
		if (emptyDocs.contains(n)) return null; // on avait déjà cherché en base et il n'y en avait pas
		boolean ro = maxDelayInSeconds != 0;
		Stamp minTime = ro ? Stamp.fromEpoch(startTime2().epoch() - maxDelayInSeconds * 1000) : startTime2();
		Document d = docs.get(n);
		long versionActuelle = 0;
		if (d != null) {
			if (!d.isReadOnly) return d; // on ne pourra jamais faire mieux
			if (d.cdoc().lastCheckDB >= minTime.epoch()) {
				// document assez frais. Peut-être en RO ?
				if (!ro) d.isReadOnly = false;
				return d;
			}
			versionActuelle = d.version();
		}
		
		// il faut en obtenir un exemplaire (voire plus frais que celui du contexte)
		XCDoc xcdoc = Cache.current().cdoc(id, minTime, versionActuelle);
		
		if (!xcdoc.existant) { // n'existe pas en base/cache
			if (versionActuelle != 0) 
				throw new AppException("CONTENTION2", this.operationName(), n); // Gros soucis : le document a été détruit depuis
			// c'était la première fois qu'on le cherchait
			emptyDocs.add(n);
			return null;
		}
		
		// il existe en base/cache
		if (xcdoc.cdoc == null) { // et n'a pas changé par rapport à celui du contexte
			d.cdoc().lastCheckDB = xcdoc.lastCheckDB;
			return d;
		}
		
		// il existe en base/cache et il y a un delta ou un premier état
		if (versionActuelle != 0) { // on en avait un dans le contexte
			if (!d.isReadOnly) // Gros soucis : le document a changé deuis le début de l'opération et on a fait des modifications
				throw new AppException("CONTENTION3", this.operationName(), n);
		}

		// il faut en faire un document et le garder dans le contexte
		// s'il était présent dans le contexte en lecture seule, il est remplacé par une version plus récente
		d = Document.newDocument(xcdoc.cdoc);
		if (ro) d.isReadOnly = true;
		docs.put(n, d);
		return d;
	}
		
	Document getOrNewDoc(Document.Id id) throws AppException {
		String n = id.toString();
		if (emptyDocs.contains(n)) {
			// on avait déjà cherché en base et il n'y en avait pas
			Document d = Document.newDocument(CDoc.newEmptyCDoc(id));
			docs.put(n, d);
			emptyDocs.remove(n);
			return d;
		}
		Document d = getDoc(id, 0);
		if (d != null) return d;
		d = Document.newDocument(CDoc.newEmptyCDoc(id));
		docs.put(n, d);
		return d;
	}
		
	/*********************************************************************************************/
	Result go(InputData inp) throws AppException {
		Result result = null;
		inputData = inp;
		iLang = AConfig._iLang(inp.args().get("lang"));
		isDebug = AConfig.config().isDebug();
		maxTime = isTask() ? AConfig.config().TASKMAXTIMEINSECONDS() : AConfig.config().OPERATIONMAXTIMEINSECONDS();
		int nsStatus = NS.status(ns); // 0:rw 1:ro 2:interdit 3:inexistant
		if (nsStatus > 2 && !hasAdminKey())
			throw new AppException("ODOMAINOFF", NS.info(ns));
		
		if (!"sync".equalsIgnoreCase(operationName())) {
			Object taskCheckpoint = null;
			do {
				phase = 0;
				for(int retry = 0;;retry++) {
					startTime2 = null;
					maxTime();
					try {
						clearCaches();

						if ("statsOp".equals(operationName())){
							result = new Result();
							result.mime = "application/json";
							result.text = hasAdminKey() || isDebug ? ExecContext.getStatsOp() : "{}";
							return result;
						}
						if ("statsD".equals(operationName())){
							result = new Result();
							result.mime = "application/json";
							result.text = hasAdminKey() || isDebug ? ExecContext.getStatsD() : "{}";
							return result;
						}
						operation = Operation.CreateOperation(operationName(), taskCheckpoint);
						if (nsStatus > 1 && !operation.isReadOnly())
							throw new AppException("ODOMAINOFF", NS.info(ns));

						phase = 1;
						long lapseOp = 0;
						long startop = System.currentTimeMillis();
						operation.work();
						taskCheckpoint = operation.taskCheckpoint();
						ExecCollect collect = new ExecCollect();
						lapseOp = System.currentTimeMillis() - startop;
						
						long lapseVal = 0;
						if (!collect.nothingToCommit()) {
							phase = 2;
							long startVal = System.currentTimeMillis();
							HashMap<String,Long> badGroups = dbProvider().validateDocument(collect);
							if (badGroups != null) {
								GDCache.current().refreshCache(badGroups);
								StringBuffer sb = new StringBuffer();
								for(String g : badGroups.keySet())
									sb.append("\n" + g + " / " + badGroups.get(g));
								throw new AppException("CONTENTION1", this.operationName(), sb.toString());
							}
							lapseVal = System.currentTimeMillis() - startVal;
						}
						
						phase = 3;	
						operation.afterWork();
						result = operation.isTask() ? new Result() : operation.result();
						
						int[] st = new int[8];
						st[6] = (int)lapseOp;
						st[7] = (int)lapseVal;
						st[retry] = 1;
						stats(ns, operationName(), st);
						break; // OK : break retry, vers next step
					} catch (Throwable t){
						AppException ex = t instanceof AppException ? (AppException)t : new AppException(t, "X0");
						if (dbProvider != null)	dbProvider.rollbackTransaction();
						if (retry == 2 || isDebug || !ex.toRetry()) {
							stats(ns, operationName(), 0,0,0,0,0,1,0,0);
							throw ex; // sortie des retries (éventuellement avant 3)
						}
						phase = 0;
						int rnd = new Random().nextInt(1000);
						try { Thread.sleep((rnd + 1000) * retry); } 
						catch (InterruptedException e) { } // retry
					}
				}
				
			} while(taskCheckpoint != null);
		}

		if (result == null)
			result = new Result();

		if (inputData.args().size() != 0) {
			String json = inputData.args().get("syncs");
			if (json != null && json.length() != 0) {
				Sync[] syncs;
				try {
					syncs = JSON.fromJson(json,  Sync[].class);
				} catch (AppException e){
					throw new AppException(e.cause(), "BEXECSYNCSPARSE", operationName());
				}
				phase = 4;
				result.syncs = sync(syncs);
			}
		}
		phase = 5;
		if (hasTraces()) trace("End");
		return result;
	}
		
	/*********************************************************************************************/
	private String sync(Sync[] syncs) throws AppException {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		GDCache cache = GDCache.current();
		Stamp minTime = Stamp.fromEpoch(startTime2().epoch());
		boolean pf = true;
		if (syncs.length != 0) 
			for(Sync sync : syncs){
				String content = null;
				Document.Id id = sync.id();
				Document d =  cache.documentRO(id, minTime, 0L);
				
				ISyncFilter sf = null;
				if (sync.filter != null)
					try {
						Class<?> cf = id.descr().filterClass();
						sf = (ISyncFilter)JSON.fromJson(sync.filter, cf);
					} catch (Exception e) {
						throw new AppException("BJSONFILTER", sync.filter, id.toString());
					}
				else
					sf = id.descr().defaultFilter();
				sf.init(this, d);

				content = d.toJson(sf, sync.ct, sync.v, sync.dt, true);
				
				if (content != null) {
					if (!pf) sb.append(", "); else pf = false;
					sb.append(content);
				}
			}
		sb.append("]");
		return sb.toString();
	}

	/*********************************************************************************************/
	public class ExecCollect {
		public boolean nothingToCommit() { return true;}
		
		ExecCollect() throws AppException {
			
		}
		
	}
}
