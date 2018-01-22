package fr.cryptonote.base;

public class TaskInfo {

	public String 	ns;
	public String 	taskid;
	public int 		step;
	public long 	toStartAt;
	public long 	toPurgeAt;
	public String	opName;
	public String 	param;
	public String 	cron;
	public String 	info;
	public int 		qn;
	public int 		retry;
	public String 	exc;
	public String 	detail;
	public long		startTime;
	
	public transient int taskType; // 4:task complete 5:task complete + param 6:next step même requête 7:next step nouvelle requête
	public transient String taskJsonParam;
	public transient long taskT; // toStartAt(7) ou toPurgeAt(5)

	public TaskInfo() { }
	
	public long retryAt() { return BConfig.RETRYSTARTAT(retry); }
	
	/*
	 * Si toStartAt est "petit" (nombre de secondes en un an) c'est un nombre de secondes par rapport à la date-heure actuelle.
	 */
	public TaskInfo(String ns, Class<?> op, Object param, String info, String cron, long toStartAt, int qn) {
		this.param = JSON.toJson(param);
		this.cron = cron;
		if (cron != null && cron.length() != 0 && toStartAt == 0) {
			this.toStartAt = new Cron(cron).nextStart().stamp();
		} else
			this.toStartAt =  toStartAt < 366 * 86400 ? Stamp.fromNow(toStartAt * 1000).stamp() : toStartAt;
		this.ns = ns;
		this.taskid = Crypto.randomB64(2);
		this.info = info;
		this.opName = op.getSimpleName();
		this.qn = qn;
		this.step = 1;
	}
	
	public TaskInfo cloneCron(){
		TaskInfo t = new TaskInfo();
		t.ns = ns;
		t.taskid = Crypto.randomB64(2);;
		t.step = 1;
		t.opName = opName;
		t.toStartAt = new Cron(cron).nextStart().stamp();
		t.cron = cron;
		t.info = info;
		t.qn = qn;
		t.retry = 0;
		return t;
	}

	public TaskInfo clone(){
		TaskInfo t = new TaskInfo();
		t.ns = ns;
		t.taskid = taskid;
		t.step = step;
		t.toStartAt = toStartAt;
		t.toPurgeAt = toPurgeAt;
		t.opName = opName;
		t.cron = cron;
		t.info = info;
		t.qn = qn;
		t.retry = retry;
		t.startTime = startTime;
		return t;
	}
		
	public static class TaskMin implements Comparable<TaskMin> {
		public String 	ns;
		public String 	taskid;
		public int 		step;
		public long 	toStartAt;
		public int		qn;
		public transient int workerIndex;
		public TaskMin() {}
		public TaskMin(TaskInfo ti) { ns = ti.ns; taskid = ti.taskid; step = ti.step; toStartAt = ti.toStartAt; qn = ti.qn; }
		public TaskMin(String ns, String taskid, int step, long startAt, int qn) { this.ns = ns; this.taskid = taskid; this.step = step; this.toStartAt = startAt; this.qn = qn; }
		public long startAtEpoch() { return Stamp.fromStamp(toStartAt).epoch(); }
		
		public String pk() { return ns + "." + taskid; }
		public String stampKey() { return "" + toStartAt + "." + pk(); }
		
		@Override
		public int compareTo(TaskMin o) { 
			return toStartAt > o.toStartAt ? 1 : (toStartAt < o.toStartAt ? -1 : taskid.compareTo(o.taskid)); 
		}

	}
	
}
