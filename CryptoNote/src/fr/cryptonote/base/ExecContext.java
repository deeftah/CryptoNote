package fr.cryptonote.base;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import fr.cryptonote.base.BConfig.Nsqm;
import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.CDoc.Status;
import fr.cryptonote.base.Cache.XCDoc;
import fr.cryptonote.base.Document.ExportedFields;
import fr.cryptonote.base.Document.Id;
import fr.cryptonote.base.Document.Sync;
import fr.cryptonote.base.Servlet.InputData;
import fr.cryptonote.base.TaskUpdDiff.ItemToCopy;
import fr.cryptonote.provider.DBProvider;

public class ExecContext {	
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
	
	private boolean isDebug;
	private int iLang;
	private Nsqm nsqm;
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
	
	ExecContext() {
		ExecContextTL.set(this);
		startTime = Stamp.fromNow(0);
		chrono = new Chrono(startTime.epoch());
		isDebug = BConfig.isDebug();		
	}

	ExecContext setLang(String lang) { iLang = BConfig.lang(lang); return this; }
	
	ExecContext setNsqm(Nsqm nsqm) { this.nsqm = nsqm; return this; }
	
	public int iLang() { return iLang; }
	public String lang() { return BConfig.lang(iLang); }
	public int phase() {return phase; }	
	public String operationName() { return inputData.operationName(); }
	public Operation operation() { return operation; }
	public Nsqm nsqm() { return nsqm; }
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
	
	public DBProvider dbProvider() throws AppException{	if (dbProvider == null)	dbProvider = BConfig.getDBProvider(nsqm.base()); return dbProvider; }

	public void maxTime() throws AppException { if (!isDebug && startTime.lapseInMs() > maxTime) throw new AppException("XMAXTIME", operationName()); }

	void closeAll() { if (dbProvider != null) dbProvider.closeConnection(); }
		
	private int xAdmin = 0;
	public boolean isSudo() {
		if (xAdmin == 0) {
			String sudo = inputData.args().get("sudo");
			if (sudo == null) 
				xAdmin = -1;
			else 
				try {
					if (isDebug && sudo.equals("nonuke"))
						xAdmin = 1;
					else {
						String x = Crypto.bytesToBase64(Crypto.SHA256(Crypto.base64ToBytes(sudo)), true);
						xAdmin = nsqm.pwd().equals(x) ? 1 : -1;
					}
				} catch (Exception e) {	xAdmin = -1; }
		}
		return xAdmin > 0;
	}
	
	public boolean isQM() { return nsqm.isQM; }

	Stamp startTime2(){ if (startTime2 == null)	startTime2 = Stamp.fromNow(0); return startTime2; }

	/*******************************************************************************************/
	// tâches à inscrire au QM
	HashMap<String,TaskInfo> tasks = new HashMap<String,TaskInfo>();
	HashMap<String,Document> docs = new HashMap<String,Document>();
	HashMap<String,Document> deletedDocuments = new HashMap<String,Document>();
	HashSet<String> forcedDeletedDocuments = new HashSet<String>();
	HashSet<String> emptyDocs = new HashSet<String>();
	
	private void clearCaches(){ tasks.clear(); docs.clear(); deletedDocuments.clear(); forcedDeletedDocuments.clear();}

	public void newTask(Document.Id id, long nextStart, String info) throws AppException{
		tasks.put(id.toString(), new TaskInfo(nsqm.code, id, nextStart, 0, info));
	}

	void forcedDeleteDoc(Id id) throws AppException{
		if (id != null){
			String k = id.toString();
			Document d = docs.get(k);
			if (d != null)
				deleteDoc(d);
			else {
				forcedDeletedDocuments.add(k);
				emptyDocs.add(k);
			}
		}
	}
	
	void deleteDoc(Document d) throws AppException {
		String k = d.id().toString();
		if (d.isRO()) throw new AppException("BDOCUMENTRO", "delete", "Document", k);
		d.cdoc().delete();
		deletedDocuments.put(k, d);
		emptyDocs.add(k);
		docs.remove(k);
	}

	/*
	 * Recherche du document dans le contexte, en cache/base s'il n'y est pas et mémorisation éventuelle
	 */
	Document getDoc(Document.Id id, int maxDelayInSeconds) throws AppException {
		String n = id.toString();
		if (emptyDocs.contains(n)) return null; // on avait déjà cherché en base et il n'y en avait pas
		boolean ro = maxDelayInSeconds != 0;
		Stamp minTime = ro ? Stamp.fromEpoch(startTime2().epoch() - maxDelayInSeconds * 1000) : startTime2();
		Document d = deletedDocuments.get(n);
		if (d != null) return d;
		d = docs.get(n);
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
		
		if (!xcdoc.existing) { // n'existe pas en base/cache
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
		Document d = deletedDocuments.get(n);
		if (d != null) {
			d.cdoc().recreate();
			deletedDocuments.remove(n);
			emptyDocs.remove(n);
			docs.put(n, d);
			return d;			
		}
		if (emptyDocs.contains(n)) {
			// on avait déjà cherché en base et il n'y en avait pas
			d = Document.newDocument(CDoc.newEmptyCDoc(id));
			docs.put(n, d);
			emptyDocs.remove(n);
			return d;
		}
		d = getDoc(id, 0);
		if (d != null) return d;
		d = Document.newDocument(CDoc.newEmptyCDoc(id));
		docs.put(n, d);
		return d;
	}
		
	/*********************************************************************************************/
	Result go(InputData inp) throws AppException {
		Result result = null;
		inputData = inp;
		maxTime = isTask() ? BConfig.TASKMAXTIMEINSECONDS() : BConfig.OPERATIONMAXTIMEINSECONDS();
		
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
							result.text = isSudo() || isDebug ? ExecContext.getStatsOp() : "{}";
							return result;
						}
						if ("statsD".equals(operationName())){
							result = new Result();
							result.mime = "application/json";
							result.text = isSudo() || isDebug ? ExecContext.getStatsD() : "{}";
							return result;
						}
						operation = Operation.CreateOperation(operationName(), taskCheckpoint);

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
							HashMap<String,Long> badDocs = dbProvider().validateDocument(collect);
							lapseVal = System.currentTimeMillis() - startVal;
							if (badDocs != null) {
								String lst = Cache.current().refreshCache(badDocs.keySet());
								throw new AppException("CONTENTION1", this.operationName(), lst);
							}
						}
						
						phase = 3;	
						operation.afterWork();
						result = operation.isTask() ? new Result() : operation.result();
						
						int[] st = new int[8];
						st[6] = (int)lapseOp;
						st[7] = (int)lapseVal;
						st[retry] = 1;
						stats(nsqm.code, operationName(), st);
						break; // OK : break retry, vers next step
					} catch (Throwable t){
						AppException ex = t instanceof AppException ? (AppException)t : new AppException(t, "X0");
						if (dbProvider != null)	dbProvider.rollbackTransaction();
						if (retry == 2 || isDebug || !ex.toRetry()) {
							stats(nsqm.code, operationName(), 0,0,0,0,0,1,0,0);
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
		Cache cache = Cache.current();
		Stamp minTime = Stamp.fromEpoch(startTime2().epoch());
		StringBuffer sb = new StringBuffer().append("[");
		for(Sync sync : syncs){
			Document.Id id = sync.id();
			XCDoc xc = cache.cdoc(id, minTime, 0L);
			String content = !xc.existing ? id.toJson() :  Document.newDocument(xc.cdoc).toJson(sync.filter, sync.ct, sync.v, sync.dt, true);
			if (sb.length() > 1) sb.append(", "); 
			sb.append(content);
		}
		return sb.append("]").toString();
	}

	/*********************************************************************************************/
	public static class ItemIUD {
		public int iud; // 1:insertion 2:update 3:suppression logique 4:purge/delete (trop ancien)
		public Id id;
		public String cvalue;
		public ExportedFields exportedFields;
		public String sha;
		public String name;
		public String clkey;
		public ItemIUD() {}
		public ItemIUD(CItem ci, long dtime, HashSet<String> s2Cleanup) {
			this.id = ci.id(); 
			name = ci.descr().name();
			clkey = ci.clkey();
			if (ci.deleted() && ci.version() < dtime)
				iud = 4;
			else
				iud = ci.created() ? 1 : (ci.toDelete() ? 3 : (ci.toSave() ? 2 : 0));
			if (ci.descr().isP()) {
				if (iud == 2)
					sha = ci.nsha();
				if (iud == 2 || iud == 3)
					s2Cleanup.add(ci.id().toString());
			}
			if (iud == 1 || iud == 2){
				cvalue = ci.cvalue();
				exportedFields = ci.exportedFields();
			}
		}
	}
	
	/*********************************************************************************************/
	public class IuCDoc {
		public int iu; // 1:insert 2:update 3:clear et insert
		public long oldVersion;
		public String clid;
		public long oldctime;
		public long olddtime;
		public CDoc cdoc;			// pour mise à jour de cache afterCommit
		public IuCDoc(Document doc) { 
			Status dst = doc.status();
			iu = dst == Status.created ? 1 : ( dst == Status.recreated ? 3 : 2);
			oldVersion = doc.version();
			clid = doc.id().toString();
			oldctime = doc.ctime();
			olddtime = doc.dtime();
			cdoc = doc.cdoc();
		}
	}


	public class ExecCollect {
//		HashMap<String,TaskInfo> tasks = new HashMap<String,TaskInfo>();
//		HashMap<String,Document> docs = new HashMap<String,Document>();
//		HashMap<String,Document> deletedDocuments = new HashMap<String,Document>();
//		HashSet<String> forcedDeletedDocuments = new HashSet<String>();
		
		public boolean nothingToCommit() {
			return versionsToCheckAndLock.size() == 0 && docsToDelForced.size() == 0 && (tq == null || tq.size() == 0); 
		}
						
		public long version; 	// future version des documenrs mis à jour / crés ou des items détruits
		public long dtime;		// future dtime des documenrs mis à jour
		
		public void afterCommit(){
			Cache.current().afterValidateCommit(version, docsToSave, docsToDelForced);
			QueueManager.insertAllInQueue(tq == null ? null : tq.values());
		}

		private void checkVersion(Document doc){
			long v = doc.version();
			if (v <= version) version = Stamp.fromStamp(v, 1).stamp();
			versionsToCheckAndLock.put(doc.id().toString(), v);
		}
		
		// documents à supprimer
		public HashSet<String> docsToDelForced;

		// documents à sauver
		public ArrayList<IuCDoc> docsToSave = new ArrayList<IuCDoc>();
		
		// documents à verrouiller
		public HashMap<String,Long> versionsToCheckAndLock = new HashMap<String,Long>();

		// documents avec S2 à nettoyer
		public HashSet<String> s2Cleanup = new HashSet<String>();

		// documents avec S2 à purger
		public HashSet<String> s2Purge = new HashSet<String>();

		// items à insérer / mettre à jour / supprimer
		public ArrayList<ItemIUD> itemsIUD = new ArrayList<ItemIUD>();

		// Tasks
		public HashMap<String,TaskInfo> tq;

		private TaskUpdDiff updDiff;
		private void updDiff(CItem ci){
			if (ci.descr().hasDifferedCopy())
				try {  
					if (updDiff == null)
						updDiff = (TaskUpdDiff)Document.newDocument(CDoc.newEmptyCDoc(new Document.Id(TaskUpdDiff.class, Crypto.randomB64(2))));
					updDiff.add(new ItemToCopy(ci.id().toString(), ci.clkey(), ci.cvalue()));
				} catch (AppException e) {	}
		}
		
		ExecCollect() throws AppException {
			tq = tasks;
			docsToDelForced = forcedDeletedDocuments;
			version = Stamp.fromNow(0).stamp();
			dtime = Stamp.fromNow(- (BConfig.DTIMEDELAYINHOURS() * 3600000)).stamp();
			for(Document doc : docs.values()) {
				if (doc.isRO()) continue;
				doc.summarize();
				// status : unchanged, modified, created, recreated, deleted, shortlived
				// les deleted / shortlived ne sont pas dans docs (mais dans docsToDel)
				if (doc.status() != Status.unchanged) {
					// pas de purge des vieux deleted si document non modifié
					docsToSave.add(new IuCDoc(doc));
					doc.cdoc().browse(ci -> {
						if (ci.toSave() || ci.deleted() || ci.toDelete()) {
							itemsIUD.add(new ItemIUD(ci, dtime, s2Cleanup)); 
							updDiff(ci);
							return true;
						}
						return false;
					});
				}
				checkVersion(doc);
			}
			if (updDiff != null) {
				CItem hdr = updDiff.commit();
				itemsIUD.add(new ItemIUD(hdr, dtime, s2Cleanup)); 
				updDiff(hdr);
				updDiff.summarize();
				docsToSave.add(new IuCDoc(updDiff));
				// 	public TaskInfo(String ns, Document.Id id, long nextStart, int retry, String info) {
				tq.put(updDiff.id().toString(), new TaskInfo(nsqm.code, updDiff.id(), version, 0, null));
			}
			for(Document doc : deletedDocuments.values()) {
				checkVersion(doc);
				docsToDelForced.add(doc.id().toString());
			}
			for(String clid : docsToDelForced) 
				s2Purge.add(clid);

			/*
			 * Maintenant on connaît la version et on peut basculer les docs et items dans leur état 
			 * committés prêts à être sauvés en base et à remettre en cache
			 * les vieux deleted peuvent être purgés, ils sont dans la liste des items à supprimer de la base
			 */
			for(Document doc : docs.values())
				if (!doc.isRO() && doc.status() == Status.unchanged) 
					doc.cdoc().afterCommit(version, dtime);
		}
		
	}
}
