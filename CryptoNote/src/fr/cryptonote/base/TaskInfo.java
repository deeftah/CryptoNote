package fr.cryptonote.base;

public class TaskInfo {

	public String 	ns;
	public String 	taskid;
	public long 	nextStart;
	public String	opName;
	public String 	info;
	public int 		retry;
	public String 	exc;
	public String 	report;
	public long		starTime;
	
	public TaskInfo(String ns, String opName, String info, long nextStart) {
		this.ns = ns;
		this.taskid = Crypto.randomB64(2);
		this.nextStart = nextStart;
		this.info = info;
		this.opName = opName;
	}
		
	public static class TaskMin implements Comparable<TaskMin> {
		public String 	ns;
		public String 	taskid;
		public long 	startAt;
		public int		qn;
		public transient int workerIndex;
		public TaskMin() {}
		public TaskMin(TaskInfo ti) { ns = ti.ns; taskid = ti.taskid; startAt = ti.nextStart; qn = BConfig.queueIndexByOp(ti.opName); }
		
		public long startAtEpoch() { return Stamp.fromStamp(startAt).epoch(); }
		
		public String pk() { return ns + "." + taskid; }
		public String stampKey() { return "" + startAt + "." + pk(); }
		
		@Override
		public int compareTo(TaskMin o) { 
			return startAt > o.startAt ? 1 : (startAt < o.startAt ? -1 : taskid.compareTo(o.taskid)); 
		}

	}
	
}
