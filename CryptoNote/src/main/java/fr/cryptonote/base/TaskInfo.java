package fr.cryptonote.base;

public class TaskInfo {

	public String 	ns;
	public String 	taskid;
	public long 	startAt;
	public String	opName;
	public String 	param;
	public String 	info;
	public int 		qn;
	public int 		retry;
	public String 	exc;
	public String 	report;
	public long		startTime;
	
	public TaskInfo() { }
	
	/*
	 * Si startAt est "petit" (nombre de secondes en un an) c'est un nombre de secondes par rapport à la date-heure actuelle.
	 */
	public TaskInfo(String ns, Class<?> op, Object param, String info, long startAt, int qn) {
		this.param = JSON.toJson(param);
		this.startAt =  startAt < 366 * 86400 ? Stamp.fromNow(startAt * 1000).stamp() : startAt;
		this.ns = ns;
		this.taskid = Crypto.randomB64(2);
		this.info = info;
		this.opName = op.getSimpleName();
		this.qn = qn;
	}
		
	public static class TaskMin implements Comparable<TaskMin> {
		public String 	ns;
		public String 	taskid;
		public long 	startAt;
		public int		qn;
		public transient int workerIndex;
		public TaskMin() {}
		public TaskMin(TaskInfo ti) { ns = ti.ns; taskid = ti.taskid; startAt = ti.startAt; qn = ti.qn; }
		public TaskMin(String ns, String taskid, long startAt, int qn) { this.ns = ns; this.taskid = taskid; this.startAt = startAt; this.qn = qn; }
		public long startAtEpoch() { return Stamp.fromStamp(startAt).epoch(); }
		
		public String pk() { return ns + "." + taskid; }
		public String stampKey() { return "" + startAt + "." + pk(); }
		
		@Override
		public int compareTo(TaskMin o) { 
			return startAt > o.startAt ? 1 : (startAt < o.startAt ? -1 : taskid.compareTo(o.taskid)); 
		}

	}
	
}
