package fr.cryptonote.base;

public class TaskInfo {

	public String 	ns;
	public String 	taskid;
	public int 		step;
	public long 	toStartAt;
	public long 	toPurgeAt;
	public String	opName;
	public String 	param;
	public String 	info;
	public int 		qn;
	public int 		retry;
	public String 	exc;
	public String 	detail;
	public long		startTime;
	
	public TaskInfo() { }
	
	public long retryAt() { return BConfig.RETRYSTARTAT(retry); }
	
	/*
	 * Si toStartAt est "petit" (nombre de secondes en un an) c'est un nombre de secondes par rapport à la date-heure actuelle.
	 */
	public TaskInfo(String ns, Class<?> op, Object param, String info, long toStartAt, int qn) {
		this.param = JSON.toJson(param);
		this.toStartAt =  toStartAt < 366 * 86400 ? Stamp.fromNow(toStartAt * 1000).stamp() : toStartAt;
		this.ns = ns;
		this.taskid = Crypto.randomB64(2);
		this.info = info;
		this.opName = op.getSimpleName();
		this.qn = qn;
		this.step = 1;
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
