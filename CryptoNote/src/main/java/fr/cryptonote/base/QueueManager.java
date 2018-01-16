package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import fr.cryptonote.base.BConfig.Nsqm;
import fr.cryptonote.base.Servlet.InputData;
import fr.cryptonote.base.TaskInfo.TaskMin;
import fr.cryptonote.provider.DBProvider;

public class QueueManager implements Runnable {
	
	private static boolean isDatastore;
	private static String myName;
	private static ArrayList<Nsqm> myNSs;
	private static ArrayList<String> myNSnames = new ArrayList<String>();
	private static HashSet<String> myDBs = new HashSet<String>();
	public static boolean hostsQM; 
	private static QueueManager qm;
	private static int nbQueues;
	private static Nsqm myNsqm;
		
	private BlockingQueue<Integer> qmQueue =  new ArrayBlockingQueue<Integer>(1000) ;
	private Worker[] workers;
	private boolean running = false;
	private boolean suspended = false;
	private Stamp nextFullScan = Stamp.minStamp;
	private TIStore tiStore = new TIStore();
		
	public static int normalizeQueueNumber(int n){
		if (n < 0) return 0;
		return n >= nbQueues ? nbQueues - 1 : n;
	}
	
	static void startQM() {
		isDatastore = (BConfig.dbProviderName()).endsWith("DS");
		myName = BConfig.QM();
		myNSs = BConfig.namespacesByQM(myName); 
		if (myNSs != null) for(Nsqm x : myNSs) { myDBs.add(x.base()); myNSnames.add(x.code); }
		hostsQM = myName != null && myNSs != null && !isDatastore && myNSs.size() != 0; 
		if (!hostsQM) return;
		myNsqm = BConfig.queueManager(myName, false);
		nbQueues = myNsqm.threads.length;
		qm = new QueueManager();
		Thread t = new Thread(qm);
		t.setName("TM-Main");
		qm.running = true;
		qm.startWorkers();
		t.start();
		qm.wakeup();
	}

	static void stopQM(){
		if (!hostsQM) return;
		qm.suspended = true;
		waitAllStopped();
	}

	static void suspendQM(){
		if (!hostsQM) return;
		qm.suspended = true;
		waitAllStopped();
	}

	static void restartQM(){
		if (!hostsQM) return;
		qm.suspended = false;
	}
	
	static Result doTmRequest(ExecContext exec, InputData inp) throws AppException {
		if (!hostsQM) return null;
		String op = inp.args().get("op");
		String key = inp.args().get("key");
		String jsonp = inp.args().get("param");

		if ("inq".equals(op)){
			if (key == null || !key.equals(myNsqm.pwd())) throw new AppException("SQMKEY");
			if (jsonp == null) return Result.empty();
			try { return JSON.fromJson(jsonp, Inq.class).doIt(); } catch (Exception e){ throw new AppException(e, "BQMJSONTI", jsonp); }
		}
		
		if (!myNsqm.isPwd(key)) throw new AppException("SQMKEY");
		if ("suspend".equals(op)){
			suspendQM();
			return Result.empty();
		}
		
		if ("restart".equals(op)){
			startQM();
			return Result.empty();
		}
		
		if ("list".equals(op)){
			ArrayList<TaskMin> res = new ArrayList<TaskMin>();
			if (qm != null)
				for(TaskMin tm : qm.tiStore.candidates(qm.nextFullScan)) res.add(tm);
			return Result.json(res);
		}
		
		if (jsonp == null) return Result.empty();
		
		if ("errTasks".equals(op))
			try { return JSON.fromJson(jsonp, ErrTasks.class).doIt(); } catch (Exception e){ throw new AppException(e, "BQMJSONTI", jsonp); }
		if ("traceTasks".equals(op))
			try { return JSON.fromJson(jsonp, TraceTasks.class).doIt(); } catch (Exception e){ throw new AppException(e, "BQMJSONTI", jsonp); }
		if ("candidateTasks".equals(op))
			try { return JSON.fromJson(jsonp, CandidateTasks.class).doIt(); } catch (Exception e){ throw new AppException(e, "BQMJSONTI", jsonp); }
		if ("taskDetail".equals(op))
			try { return JSON.fromJson(jsonp, TaskDetail.class).doIt(); } catch (Exception e){ throw new AppException(e, "BQMJSONTI", jsonp); }
		if ("taskParem".equals(op))
			try { return JSON.fromJson(jsonp, TaskParam.class).doIt(); } catch (Exception e){ throw new AppException(e, "BQMJSONTI", jsonp); }
		
		throw new AppException("BQMOP", op);
	}

	private static String hasRunningTasks(){
		StringBuffer sb = new StringBuffer();
		if (qm != null) {
			for(int i = 0; i < qm.workers.length; i++)
				if (qm.workers[i].tm != null) sb.append(" " + i);
			}
		return sb.toString();
	}

	private static void waitAllStopped() {
		while (true) {
			String s = hasRunningTasks();
			if (s.length() == 0) return;
			try { Thread.sleep(3000); } catch (InterruptedException e) {}
			Util.log.warning("Attente de fin des workers :" + s);
		}
	}

	public static void toQueue(Inq tm) {
		Nsqm ns = BConfig.namespace(tm.ns, false);
		if (ns == null) return;
		Nsqm nsqm = BConfig.queueManager(ns.qm, false);
		if (nsqm == null) return;
		if (tm.startAtEpoch() > System.currentTimeMillis() + (nsqm.scanlapseinseconds() * 1000)) return;
		if (nsqm == myNsqm) { // inscription locale sans passer par un POST
			qm.tiStore.put(tm);
			qm.wakeup();
		} else {
			HashMap<String,String> args = new HashMap<String,String>();
			args.put("param", JSON.toJson(tm));
			args.put("op", "inq");
			args.put("key", nsqm.pwd());
			Util.postSrv(nsqm.code, "op/", args);
		}
	}
	
	/** TiStore **********************************************/
	private class TIStore {
		private HashMap<String,TaskMin> byPK = new HashMap<String,TaskMin>();
		private TreeMap<String,TaskMin> byStamp = new TreeMap<String,TaskMin>();
		
		private synchronized void clear() {
			byPK.clear();
			byStamp.clear();
		}
		
		private void rawinsert(TaskMin tm){
			byPK.put(tm.pk(), tm);
			byStamp.put(tm.stampKey(), tm);
		}

		private synchronized void put(TaskMin tm){
			if (tm == null || tm.toStartAt > qm.nextFullScan.stamp()) return;
			tm.qn = QueueManager.normalizeQueueNumber(tm.qn);
			tm.workerIndex = 0;
			TaskMin tx = byPK.get(tm.pk());
			if (tx != null) {
				if (tx.step != tm.step) {
					if (tx.workerIndex != 0)
						qm.toIgnore(tx.workerIndex); 	// pour que le worker ne tente plus de retry éventuel de l'ancienne, voire ne la lance pas
				} else {								// cette step est déjà connue ici
					if (tx.workerIndex != 0) 
						return;							// déjà en cours d'exécution par un worker (rien de nouveau, il faut le laisser finir)	
				}
				byStamp.remove(tx.stampKey()); 			// on enlève l'ancienne de la queue (si elle l'y était)
			}
			rawinsert(tm);								// on insère la nouvelle étape (ce qui peut cacher l'ancienne)	
		}

		private synchronized void remove(Worker w){
			byStamp.remove(w.tm.stampKey());
			byPK.remove(w.tm.pk());
			w.tm = null;
		}

		private synchronized Collection<TaskMin> candidates(Stamp start){
			return byStamp.subMap("0", start.toString()).values();
		}
		
	}

	/** Instance *********************************************/
	private void wakeup(){ qmQueue.add(new Integer(0)); }
			
	private QueueManager() {
		int nb = 0;
		for(int i = 0; i < myNsqm.threads.length; i++) nb += myNsqm.threads[i];
		workers = new Worker[nb];
		int k = 0;
		for(int i = 0; i < myNsqm.threads.length; i++)
			for (int j = 0; j < myNsqm.threads[i]; j++, k++)
				workers[k] = new Worker(i, j+1);
	}

	private void toIgnore(int workerIndex) {
		if (workerIndex >= 0 && workerIndex < workers.length)
			workers[workerIndex].toIgnore = true;
	}
	
	private void startWorkers(){
		for(int i = 0; i < workers.length; i++){
			Worker w = workers[i];
			Thread tw = new Thread(w);
			tw.setName("TM-Worker-" + w.queue + "-" + w.index);
			tw.start();
		}
	}

	public void run() {
		while (running) {
			try {
				qmQueue.poll(myNsqm.scanlapseinseconds, TimeUnit.SECONDS);
				if (!running) break;
				if (suspended) {
					Thread.sleep(10000);
					if (!running) break;
				} else
					doTheJob();
			} catch (InterruptedException e) {}
		}
	}
	
	private Worker freeWorker(int q){
		for(int k = 0; k < workers.length; k++) {
			Worker w = workers[k];
			if (w.tm == null && w.queue == q) 
				return w;
		}
		return null;
	}
		
	private ArrayList<TaskMin> getAllFromDB() {
		ArrayList<TaskMin> tmp = new ArrayList<TaskMin>();
		nextFullScan = Stamp.fromNow(myNsqm.scanlapseinseconds * 1000);
		long minStartTime = Stamp.fromNow(BConfig.TASKLOSTINMINUTES() * 60000).stamp();
		long toStartAt = Stamp.fromNow(0).stamp();
		for(String db : myDBs) {
			DBProvider provider = null;
			try {
				provider = BConfig.getDBProvider(db);
				provider.setLostTask(minStartTime, toStartAt);
				Collection<TaskMin> tiList = provider.candidateTasks(null, nextFullScan.stamp());
				for(TaskMin ti : tiList) tmp.add(ti);
			} catch (AppException e){
				Util.log.log(Level.SEVERE, "Queue Scan [" + db + "]", e);
			}
			if (provider != null) provider.closeConnection();
		}
		return tmp;
	}
	
	private void doTheJob() {
		if (System.currentTimeMillis() > nextFullScan.epoch()) {
			// reload depuis DB
			ArrayList<TaskMin> tmp = getAllFromDB();
			// clean tiStore, reinsert ti en cours et ceux venant de DB pas déjà insrit
			synchronized (tiStore) {
				tiStore.clear();
				for(int i = 0; i < workers.length; i++) {
					TaskMin tm = workers[i].tm;
					if (tm != null) tiStore.rawinsert(tm);
				}
				for(TaskMin tm : tmp) tiStore.put(tm);
			}
		}
			
		int[] th = myNsqm.threads;
		int[] nbw = new int[th.length];
		int nbws = 0;
		int k = 0;
		for(int i = 0; i < th.length; i++)
			for(int j = 0; j < th[i]; j++, k++) {
				if (workers[k].tm == null) { 
					nbw[i]++; 
					nbws++;
				}
			}
		if (nbws > 0)
			for (TaskMin tm : tiStore.candidates(Stamp.fromNow(0))) {
				if (tm.workerIndex > 0 || nbw[tm.qn] == 0) continue;
				Worker w = freeWorker(tm.qn);
				if (w == null) continue;
				nbw[tm.qn]--;
				nbws--;
				w.tm = tm;
				tm.workerIndex = w.index;
				w.workerQueue.add(tm);
				if (nbws == 0) break;
			}
	}
		
	/** Worker *************************************************/
	private class Worker implements Runnable {
		private int 	queue;
		private int 	index;
		private boolean toIgnore = false;
		private TaskMin tm;
		private BlockingQueue<TaskMin> workerQueue =  new ArrayBlockingQueue<TaskMin>(10);
		
		private Worker(int queue, int index) { this.queue = queue; this.index = index; }
		
		public void run() {
			while (running) {
				try {
					if (suspended) {
						Thread.sleep(10000);
						if (!running) break;
					} else {
						toIgnore = false;
						tm = workerQueue.poll(10, TimeUnit.SECONDS);
						if (!running) break;
						if (tm != null) {
							for(int i = 0; i < 3; i++) {
								if (toIgnore) break;
								int st = Util.postSrv(tm.ns, tm.taskid + "/" + tm.step, null);
								if (!running) break;
								if (st != 2) break;
								// on tente quand même de surmonter un problème techniquue fugitif
								Thread.sleep(10000);
							}
							qm.tiStore.remove(this);
							if (!running) break;
							qm.wakeup();
						}
					}
				} catch (InterruptedException e) {}
			}
		}
		
	}
	
	/** QM operations ***************************************************/
	interface QMOP {
		Result doIt() throws AppException;
	}
	
	private static DBProvider dbProvider(String nsCode) throws AppException {
		Nsqm ns = BConfig.namespace(nsCode, false);
		if (ns == null) throw new AppException("ANSUNKNOWN", nsCode);
		return BConfig.getDBProvider(ns.base());
	}

	static class Inq extends TaskMin implements QMOP {
		public Inq(TaskInfo ti) {
			super(ti);
		}
		public Result doIt() throws AppException {
			qm.tiStore.put(this);
			qm.wakeup();	
			return Result.empty();
		}
	}

	static class TraceTasks implements QMOP {
		// Collection<TaskInfo> traceTasks(String ns, long toPurgeAtMin, long toPurgeAtMax, String opname)
		String ns;
		long toPurgeAtMin;
		long toPurgeAtMax;
		String opname;
		public Result doIt() throws AppException {
			if (ns != null)
				return Result.json(dbProvider(ns).traceTasks(ns, toPurgeAtMin, toPurgeAtMax, opname));
			ArrayList<TaskInfo> res = new ArrayList<TaskInfo>();
			for(String db : BConfig.bases())
				res.addAll(BConfig.getDBProvider(db).traceTasks(ns, toPurgeAtMin, toPurgeAtMax, opname));
			return Result.json(res);
		}
	}

	static class ErrTasks implements QMOP {
		// Collection<TaskInfo> errTasks(String ns, long toStartAtMin, long toStartAtMax, String exc)
		String ns;
		long toStartAtMin;
		long toStartAtMax;
		String exc;
		public Result doIt() throws AppException {
			if (ns != null)
				return Result.json(dbProvider(ns).errTasks(ns, toStartAtMin, toStartAtMax, exc));
			ArrayList<TaskInfo> res = new ArrayList<TaskInfo>();
			for(String db : BConfig.bases())
				res.addAll(BConfig.getDBProvider(db).errTasks(ns, toStartAtMin, toStartAtMax, exc));
			return Result.json(res);
		}
	}

	static class CandidateTasks implements QMOP {
		// Collection<TaskMin> candidateTasks(long before)
		String ns;
		long before;
		public Result doIt() throws AppException {
			if (ns != null)
				return Result.json(dbProvider(ns).candidateTasks(ns, before));
			ArrayList<TaskMin> res = new ArrayList<TaskMin>();
			for(String db : BConfig.bases())
				res.addAll(BConfig.getDBProvider(db).candidateTasks(ns, before));
			return Result.json(res);
		}
	}

	static class TaskDetail implements QMOP {
		// Collection<TaskMin> candidateTasks(long before)
		String ns;
		String taskid;
		public Result doIt() throws AppException {
			return Result.json(dbProvider(ns).taskDetail(ns, taskid));
		}
	}		

	static class TaskParam implements QMOP {
		// Collection<TaskMin> candidateTasks(long before)
		String ns;
		String taskid;
		public Result doIt() throws AppException {
			return Result.json(dbProvider(ns).taskParam(ns, taskid));
		}
	}		

}
