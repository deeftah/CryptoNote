package fr.cryptonote.base;

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
import fr.cryptonote.base.TaskInfo.TaskMin;
import fr.cryptonote.provider.DBProvider;

public class ExecContext {	
	/* stats d'une heure */
	private static HashMap<String,HashMap<String,int[]>> stats = new HashMap<String,HashMap<String,int[]>>();
	private static int hourToSave;
	
	/*
	 * 0 : nb ok sans retry
	 * 1 : nb ok avec retry
	 * 2 : nb ko
	 * 3 : nb exc CONTENTION
	 * 4 : lapse total
	 * 5 : lapse work
	 * 6 : lapse validation
	 * 7 : lapse sync
	 */
	
	private static void stats(String ns, String op, int[] s){
		if (op == null || op.length() == 0 || s == null || ns == null) return;
		HashMap<String,String> jsonByNs = null;
		long now = System.currentTimeMillis();
		int hour = (int)(Stamp.fromEpoch(now).stamp() / 10000000);
		int hourToBD = 0;
		if (hourToSave == 0) hourToSave = hour;
		synchronized (stats){
			HashMap<String,int[]> sop = stats.get(ns);
			if (sop == null) {
				sop = new HashMap<String,int[]>();
				stats.put(ns, sop);
			}
			int[] a = sop.get(op);
			if (a == null) {
				a = new int[8];
				sop.put(op, a);
			}
			for(int i = 0; i < s.length && i < 8; i++) a[i] += s[i];
			
			if (hour == hourToSave) return;
			hourToBD = hourToSave;
			hourToSave = (int)(Stamp.fromEpoch(now + 3600000).stamp() / 10000000);
			jsonByNs = new HashMap<String,String>();
			for(String n : stats.keySet())
				jsonByNs.put(n, JSON.toJson(stats.get(n)));
			stats.clear();
		}
		// sauver jsonByNs par ns sous la clé hourToDB / ns
		for(String n : jsonByNs.keySet()) {
			try {
				BConfig.getDBProvider(BConfig.namespace(n, false).base).recordHourStats(hourToBD, n, jsonByNs.get(n));
			} catch (AppException e) {	}
		}
	}
	
	/*******************************************************************************************/

	public static ExecContext current() { return ExecContextTL.get(); }
	private static final ThreadLocal<ExecContext> ExecContextTL = new ThreadLocal<ExecContext>();
	
	private boolean isDebug;
	private int iLang;
	private Nsqm nsqm;
	private Stamp startTime;
	private long maxTime;
	private InputData inputData;
	private Operation operation;
	protected int phase = 0; // 0:initiale 1:operation 2:validation 3:afterwork 4:sync 5:finale
	private DBProvider dbProvider;
	private HashMap<String,Object> appData = new HashMap<String,Object>();
	private long ti = 0L;

	ExecContext() {
		ExecContextTL.set(this);
		isDebug = BConfig.isDebug();
		ti = System.currentTimeMillis();
	}

	ExecContext setLang(String lang) { iLang = BConfig.lang(lang); return this; }
	
	ExecContext setNsqm(Nsqm nsqm) { this.nsqm = nsqm; return this; }
	
	public int iLang() { return iLang; }
	public String lang() { return BConfig.lang(iLang); }
	public int phase() {return phase; }	
	public String operationName() { return inputData.operationName(); }
	public Operation operation() { return operation; }
	public Nsqm nsqm() { return nsqm; }
	public boolean isTask() { return inputData.isTask(); }
	public InputData inputData() { return inputData; }
	
	public Object getAppData(String name) {	return appData.get(name); }
	public void setAppData(String name, Object obj) { appData.put(name, obj); }
	public Set<String> getAppDataNames() { return appData.keySet(); }
	
	public DBProvider dbProvider() throws AppException{	if (dbProvider == null)	dbProvider = BConfig.getDBProvider(nsqm.base()); return dbProvider; }

	public void maxTime() throws AppException { 
		if (!isDebug && (System.currentTimeMillis() - ti) > maxTime) 
			throw new AppException("TMAXTIME", operationName(), "" + maxTime); 
	}

	void closeAll() { if (dbProvider != null) dbProvider.closeConnection(); }
		
	private int xAdmin = 0;
	public boolean isSudo() {
		if (xAdmin == 0) {
			String sudo = inputData.args().get("sudo");
			xAdmin = sudo != null && (nsqm.isPwd(sudo) || (isDebug && sudo.equals("nonuke"))) ? 1 : -1;
		}
		return xAdmin > 0;
	}
	
	Stamp startTime(){ if (startTime == null)	startTime = Stamp.fromNow(0); return startTime; }

	/*******************************************************************************************/
	// tâches à inscrire au QM
	ArrayList<TaskInfo> tasks = new ArrayList<TaskInfo>();
	HashMap<String,Document> docs = new HashMap<String,Document>();
	HashMap<String,Document> deletedDocuments = new HashMap<String,Document>();
	HashSet<String> forcedDeletedDocuments = new HashSet<String>();
	HashSet<String> emptyDocs = new HashSet<String>();
	
	private void clearCaches(){ tasks.clear(); docs.clear(); deletedDocuments.clear(); forcedDeletedDocuments.clear();}

	/*
	 * Si startAt est "petit" (nombre de secondes en un an) c'est un nombre de secondes par rapport à la date-heure actuelle.
	 */
	public void addTask(Class<?> op, Object param, String info, long startAt, int qn) {
		tasks.add(new TaskInfo(nsqm.code, op, param, info, startAt, qn));
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
		Stamp minTime = ro ? Stamp.fromEpoch(startTime().epoch() - maxDelayInSeconds * 1000) : startTime();
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
		int[] cs = new int[8];
		/*
		 * 0 : nb ok 
		 * 1 : nb ko
		 * 2 : nb retries
		 * 3 : nb exc CONTENTION
		 * 4 : lapse total
		 * 5 : lapse work
		 * 6 : lapse validation
		 * 7 : lapse sync
		 */

		if (!"sync".equalsIgnoreCase(operationName())) {
			Object taskCheckpoint = null;
			
			do {
				
				for(int retry = 0;;retry++) {
					if (retry != 0) cs[2]++;
					startTime = null;
					maxTime();
					try {
						long t0 = System.currentTimeMillis();
						clearCaches();
						operation = Operation.CreateOperation(operationName(), taskCheckpoint);
						phase = 1;
						operation.work();
						long t1 = System.currentTimeMillis();
						cs[5] += (int)(t1 - t0);
						taskCheckpoint = operation.taskCheckpoint();
						ExecCollect collect = new ExecCollect();
						
						if (!collect.nothingToCommit()) {
							phase = 2;
							HashMap<String,Long> badDocs = dbProvider().validateDocument(collect);
							cs[6] += (int)(System.currentTimeMillis() - t1);
							if (badDocs != null) {
								cs[3]++;
								String lst = Cache.current().refreshCache(badDocs.keySet());
								throw new AppException("CONTENTION1", this.operationName(), lst);
							}
						}
						
						phase = 3;	
						operation.afterWork();
						break; // OK : break retry, vers next step
					} catch (Throwable t){
						AppException ex = t instanceof AppException ? (AppException)t : new AppException(t, "X0");
						if (dbProvider != null)	dbProvider.rollbackTransaction();
						if (retry == 2 || isDebug || !ex.toRetry()) {
							cs[2] = 1;
							cs[4] += (int)(System.currentTimeMillis() - ti);		
							stats(nsqm.code, operationName(), cs);
							throw ex; // sortie des retries (éventuellement avant 3)
						}
						phase = 0;
						int rnd = new Random().nextInt(1000);
						try { Thread.sleep((rnd + 1000) * retry); } 
						catch (InterruptedException e) { } // retry
					}
				}
				
			} while(taskCheckpoint != null);
			cs[0] = 1;
		}

		if (!isTask()) {
			String json = inputData.args().get("syncs");
			if (json != null && json.length() != 0) {
				phase = 4;
				Sync[] syncs;
				try {
					long t2 = System.currentTimeMillis();
					syncs = JSON.fromJson(json,  Sync[].class);
					cs[7] += (int)(System.currentTimeMillis() - t2);
				} catch (AppException e){
					throw new AppException(e.cause(), "BEXECSYNCSPARSE", operationName());
				}
				if (result == null)	result = new Result();
				result.syncs = sync(syncs);
			}
		}
		
		phase = 5;
		cs[4] += (int)(System.currentTimeMillis() - ti);		
		stats(nsqm.code, operationName(), cs);
		return result;
	}
		
	/*********************************************************************************************/
	private String sync(Sync[] syncs) throws AppException {
		Cache cache = Cache.current();
		Stamp minTime = Stamp.fromEpoch(startTime().epoch());
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
			if (tq != null && tq.size() != 0)
				for(TaskInfo ti : tq) QueueManager.toQueue(new TaskMin(ti));
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
		public ArrayList<TaskInfo> tq;

		private UpdDiff.Param updDiff;
		
		private void updDiff(CItem ci){
			if (ci.descr().hasDifferedCopy()) {
				if (updDiff == null) updDiff = new UpdDiff.Param();
				updDiff.addCItem(ci);
			}
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
				updDiff.vop = version;
				tq.add(new TaskInfo(nsqm.code, UpdDiff.class, updDiff, null, 0L, 0));
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
