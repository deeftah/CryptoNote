package fr.cryptonote.base;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class TaskInfo implements Comparable<TaskInfo> {

	public String ns;
	public Document.Id id;
	public long nextStart;
	public int retry;
	public String info;
	public transient int queue;
	public transient int workerIndex;
	public transient String report;
	public transient String stampKey;
	
	public TaskInfo(String ns, Document.Id id, long nextStart, int retry, String info) {
		this.ns = ns;
		this.id = id;
		this.nextStart = nextStart;
		this.retry = retry;
		this.info = info;
		stampKey = nextStart().toString() + "/" + pk();
	}
	
	public String pk() { return ns + "/" + id.toString() + "/" ; }
	
	public String url(){
		try { return ns + "/od/" + URLEncoder.encode(id.docid(), "UTF-8") + "/" + id.docclass();
		} catch (UnsupportedEncodingException e) { return null; }
	}
		
	public Stamp nextStart(){ return Stamp.fromStamp(nextStart); }

	@Override
	public int compareTo(TaskInfo o) { return stampKey.compareTo(o.stampKey); }

}
