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
	
	public TaskInfo(String ns, String opName, String param, String info, long startAt, int qn) {
		this.ns = ns;
		this.taskid = Crypto.randomB64(2);
		this.startAt = startAt;
		this.info = info;
		this.opName = opName;
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
