package fr.cryptonote.base;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;

import fr.cryptonote.base.BConfig.Instance;
import fr.cryptonote.base.Servlet.InputData;
import fr.cryptonote.provider.DBProvider;

public class QueueManager implements Runnable {
	private static boolean isDatastore = (BConfig.config().dbProviderName()).endsWith("DS");
	public static boolean hostsQM() { 
		return instance.hostedQM != null && instance.hostedQM.length != 0 && !isDatastore; 
	}
	
	private static ArrayList<String> myNs = new ArrayList<String>();
	
	private static ArrayList<String> myNs(boolean refresh){
		if (refresh) try { myNs = NS.nsOfQm(instance.hostedQM); } catch (AppException e) { }
		return myNs;
	}
	
	private static boolean hostNsQ(String ns){ return ns != null && myNs.indexOf(ns) != -1; }
	
	private static QueueManager qm;
	private static Instance instance;
	private static BConfig config;
	private static int nbQueues = 0;
	
	private BlockingQueue<Integer> qmQueue =  new ArrayBlockingQueue<Integer>(1000) ;
	private Worker[] workers;
	private boolean running = false;
	private Stamp nextFullScan = Stamp.minStamp;
	private TIStore tiStore = new TIStore();
	
	public static String postQM(TaskInfo ti, boolean toQM) throws AppException{
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("key=").append(URLEncoder.encode(config.qmSecretKey(), "UTF-8"));
			if (toQM)
				sb.append("&param=").append(URLEncoder.encode(JSON.toJson(ti), "UTF-8")).append("&op=inq");
			String u = config.url();
			if (toQM) 
				// notification d'une mise en queue d'une task
				u += NS.qm(ti.ns) + "/qm";
			else 
				// exécution d'une task
				u += "/" + ti.url();
		    URL url = new URL(u);
		    String query = sb.toString();
		    HttpURLConnection con = url.getProtocol().equals("https") ?
		    		(HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
		    con.setRequestMethod("POST");
		    con.setRequestProperty("Content-length", String.valueOf(query.length())); 
		    con.setRequestProperty("Content-Type","application/x-www-form-urlencoded"); 
		    con.setDoOutput(true); 
		    DataOutputStream output = new DataOutputStream(con.getOutputStream()); 
		    output.writeBytes(query);
		    output.close();
		    
		    int status = con.getResponseCode();
		    if (status == 200)
		    	return null;
		    
		    byte[] bytes = null;
		    try {
		    	InputStream response;
			    response = con.getErrorStream();
			    if (response == null)
			    	response = con.getInputStream();
			    if (response != null)
			    	bytes = Util.bytesFromStream(response);
		    } catch(Exception e) {}
		    return Util.bytes2string(bytes);
		} catch (Exception e) {
			String ac = toQM ? "notification-qm" : "task";
			Util.log.log(Level.SEVERE, "POST au site du QueueManager : " + ac, e);
			throw new AppException(e, "XQMPOST", ac);
		}
	}
	
	public static int normalizeQueueNumber(int n){
		if (n < 0) return 0;
		return n >= nbQueues ? nbQueues - 1 : n;
	}
	
	static void initQ() throws AppException{
		config = BConfig.config();
		instance = config.instance();
		nbQueues = instance.threads.length;
		if (!hostsQM()) return;
		
		myNs(true);
		
		qm = new QueueManager();
		Thread t = new Thread(qm);
		t.setName("TM-Main");
		qm.running = true;
		qm.startWorkers();
		t.start();
		qm.wakeup();
	}
	
	static void closeQ(){
		if (qm == null) return;
		qm.running = false;
		waitAllStopped();
	}
			
	private static class QV {
		String docclass;
		String docid;
		Document.Id id () { return new Document.Id(docclass, docid); }
	}
	
	public static Result doTmRequest(ExecContext exec, InputData inp) throws AppException {
		if (qm == null) return null;
		// String qmId = exec.ns(); // par convention le ns de lexecContext est l'identifiant du qm
		String op = inp.args().get("op");

		if ("inq".equals(op)){
			if (!exec.hasQmKey()) throw new AppException("SQMKEY");
			TaskInfo ti = null;
			String jsonp = inp.args().get("param");
			if (jsonp != null) {
				try {
					ti = JSON.fromJson(jsonp, TaskInfo.class);
				} catch (Exception e){
					throw new AppException(e, "BQMJSONTI", jsonp);
				}
			}
			if (ti != null) {
				ArrayList<TaskInfo> til = new ArrayList<TaskInfo>();
				til.add(ti);
				insertAllInQueue(til);
			}
			return null;
		}
		if (!exec.hasQmKey()) throw new AppException("SQMKEY");
		if ("stop".equals(op)){
			closeQ();
			return null;
		}
		if ("start".equals(op)){
			initQ();
			return null;
		}
		if ("list".equals(op)){
			Result r = new Result();
			r.out = listBackLog();
			return r;
		}
		if ("report".equals(op)){
			String jsonp = inp.args().get("param");
			QV qv = null;
			if (jsonp != null) {
				try {
					qv = JSON.fromJson(jsonp, QV.class);
				} catch (Exception e){
					throw new AppException(e, "BQMJSONTI", jsonp);
				}
			}
			Result r = new Result();
			r.out = exec.dbProvider().taskReport(qv.id());
			return r;
		}
		throw new AppException("BQMOP", op);
	}

	private static String hasRunningTasks(){
		StringBuffer sb = new StringBuffer();
		if (qm != null) {
			for(int i = 0; i < qm.workers.length; i++)
				if (qm.workers[i].ti != null) sb.append(" " + i);
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

	public static void insertAllInQueue(Collection<TaskInfo> tiList){
		if (qm == null) return;
		if (tiList == null || tiList.size() == 0) return;
		for(TaskInfo ti : tiList) {
			if (ti.nextStart > qm.nextFullScan.stamp()) continue;
			if (qm == null || !hostNsQ(ti.ns))
				try {
					 postQM(ti, true);
				} catch (Exception e) {}
			else {
				qm.tiStore.put(ti);
				qm.wakeup();
			}
		}
	}

	private static ArrayList<TaskInfo> listBackLog(){
		ArrayList<TaskInfo> res = new ArrayList<TaskInfo>();
		if (qm != null)
			for(TaskInfo ti : qm.tiStore.candidates(qm.nextFullScan)) res.add(ti);
		return res;
	}
	
	/** TiStore **********************************************/
	private class TIStore {
		private HashMap<String,TaskInfo> byPK = new HashMap<String,TaskInfo>();
		private TreeMap<String,TaskInfo> byStamp = new TreeMap<String,TaskInfo>();
		
		private synchronized void clear() {
			byPK.clear();
			byStamp.clear();
		}
		
		private void rawinsert(TaskInfo ti){
			byPK.put(ti.pk(), ti);
			byStamp.put(ti.stampKey, ti);
		}

		private synchronized void put(TaskInfo ti){
			if (ti == null || ti.nextStart > qm.nextFullScan.stamp()) return;
			TaskInfo tx = byPK.get(ti.pk());
			if (tx != null && tx.workerIndex != 0) return; // déjà en cours d'exécution par un worker
			ti.queue = QueueManager.normalizeQueueNumber(BConfig.config().queueNumber(ti.id));
			ti.workerIndex = 0;
			if (tx != null)
				byStamp.remove(tx.stampKey);
			rawinsert(ti);
		}

		private synchronized void remove(Worker w){
			if (w == null || w.ti == null) return;
			String k = w.ti.pk();
			TaskInfo t = byPK.get(k);
			if (t != null) {
				byStamp.remove(t.stampKey);
				byPK.remove(k);
				w.ti = null;
			}
		}

		private synchronized void reinsert(Worker w, long nextStart2, int retry2){
			if (w == null || w.ti == null) return;
			String k = w.ti.pk();
			TaskInfo t = byPK.get(k);
			if (t != null) {
				byStamp.remove(t.stampKey);
				if (nextStart2 > qm.nextFullScan.stamp())
					byPK.remove(k);
				else {
					t.nextStart = nextStart2;
					t.retry = retry2;
					t.workerIndex = 0;
					t.stampKey = t.nextStart().toString() + "/" + t.pk();
					byStamp.put(t.stampKey, t);
				}
				w.ti = null;
			}
		}

		private synchronized Collection<TaskInfo> candidates(Stamp start){
			return byStamp.subMap("0", start.toString()).values();
		}
		
	}

	/** Instance *********************************************/
	private void wakeup(){ qmQueue.add(new Integer(0)); }
			
	private QueueManager() throws AppException{
		int nb = 0;
		for(int i = 0; i < instance.threads.length; i++) nb += instance.threads[i];
		workers = new Worker[nb];
		int k = 0;
		for(int i = 0; i < instance.threads.length; i++)
			for (int j = 0; j < instance.threads[i]; j++, k++)
				workers[k] = new Worker(i, j+1);
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
				qmQueue.poll(instance.scanlapseinseconds, TimeUnit.SECONDS);
				if (!running)
					break;
				else {
					doTheJob();
				}
			} catch (InterruptedException e) {}
		}
	}
	
	private Worker freeWorker(int q){
		for(int k = 0; k < workers.length; k++) {
			Worker w = workers[k];
			if (w.ti == null && w.queue == q) 
				return w;
		}
		return null;
	}
		
	private ArrayList<TaskInfo> getAllFromDB() {
		ArrayList<TaskInfo> tmp = new ArrayList<TaskInfo>();
		nextFullScan = Stamp.fromNow(instance.scanlapseinseconds * 1000);
		for(String ns : myNs(true)) {
			DBProvider provider = null;
			try {
				if (NS.status(ns) != 0) continue;
				provider = BConfig.config().newDBProvider(ns);
				// 	public Collection<TaskInfo> listTask(String BEGINclid, long AFTERnextstart, int MINretry, String CONTAINSinfo) throws AppException ;
				Collection<TaskInfo> tiList = provider.listTask(null, nextFullScan.stamp(), 0, null);
				for(TaskInfo ti : tiList)
					tmp.add(ti);
			} catch (AppException e){
				if (provider != null)
					provider.closeConnection();
				Util.log.log(Level.SEVERE, "Queue Scan [" + ns + "]", e);
			}
			if (provider != null)
				provider.closeConnection();
		}
		return tmp;
	}
	
	private void doTheJob() {
		if (System.currentTimeMillis() > nextFullScan.epoch()) {
			// reload depuis DB
			ArrayList<TaskInfo> tmp = getAllFromDB();
			// clean tiStore, reinsert ti en cours et ceux venant de DB pas déjà insrit
			synchronized (tiStore) {
				tiStore.clear();
				for(int i = 0; i < workers.length; i++) {
					TaskInfo ti = workers[i].ti;
					if (ti != null) 
						tiStore.rawinsert(ti);
				}
				for(TaskInfo ti : tmp)
					tiStore.put(ti);
			}
		}
			
		int[] nbw = new int[instance.threads.length];
		int nbws = 0;
		int k = 0;
		for(int i = 0; i < instance.threads.length; i++)
			for(int j = 0; j < instance.threads[i]; j++, k++) {
				if (workers[k].ti == null) { 
					nbw[i]++; 
					nbws++;
				}
			}
		if (nbws > 0)
			for (TaskInfo ti : tiStore.candidates(Stamp.fromNow(0))) {
				if (ti.workerIndex > 0 || nbw[ti.queue] == 0) continue;
				Worker w = freeWorker(ti.queue);
				if (w == null) continue;
				nbw[ti.queue]--;
				nbws--;
				w.ti = ti;
				ti.workerIndex = w.index;
				w.workerQueue.add(ti);
				if (nbws == 0) break;
			}
	}
		
	/** Worker *************************************************/
	private class Worker implements Runnable {
		private int queue;
		private int index;
		private TaskInfo ti;
		private BlockingQueue<TaskInfo> workerQueue =  new ArrayBlockingQueue<TaskInfo>(10);
		
		private Worker(int queue, int index) throws AppException{
			this.queue = queue;
			this.index = index;
		}
		
		public void run() {
			while (running) {
				try {
					ti = workerQueue.poll(10, TimeUnit.SECONDS);
					if (!running) break;
					if (ti != null) {
						postTask(ti);
						if (running)
							qm.wakeup();
						else
							break;
					}
				} catch (InterruptedException e) {}
			}
		}
		
		private void postTask(TaskInfo ti) {
			String report = null;
			for(int i = 0; i < 3; i++)
				try {
					report = null;
					report = postQM(ti, false);
					if (report == null)
						break;
					else
						try { Thread.sleep(3000); } catch (InterruptedException e1) { }	
				} catch(AppException e){
					if (!running) return;
					if (i == 2) 
						report = e.toString();
					else
						try { Thread.sleep(3000); } catch (InterruptedException e1) { }				
				}
			
			long nextStart2 = 0;
			int retry2 = 0;
			
			if (report != null) {
				if (ti.retry >= instance.retriesInMin.length)
					nextStart2 = Stamp.maxStamp.stamp();
				else
					nextStart2 = Stamp.fromNow(instance.retriesInMin[ti.retry] * 60000).stamp();
				retry2 = ti.retry + 1;
			}
			
			DBProvider myProvider = null;
			for(int i = 0; i < 3; i++) {
				try {
					myProvider = BConfig.config().newDBProvider(ti.ns);
					if (report == null) {
						myProvider.removeTask(ti.id);
						qm.tiStore.remove(this);
					} else {
						myProvider.updateNRRTask(ti.id, nextStart2, retry2, report);
						qm.tiStore.reinsert(this, nextStart2, retry2);
					}
					if (myProvider != null)
						myProvider.closeConnection();
					return;
				} catch (AppException e) {
					Util.log.log(Level.SEVERE, "UpdateTaskInfo", e);
					if (!running) return;
					try { Thread.sleep(3000); } catch (InterruptedException e1) { }
				}
			}
			if (myProvider != null)
				myProvider.closeConnection();
			return;
		}

	}
	
}
