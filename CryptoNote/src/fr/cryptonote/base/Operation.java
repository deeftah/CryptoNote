package fr.cryptonote.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class Operation {
	private static Hashtable<String, OperationDescriptor> operationClasses = new Hashtable<String, OperationDescriptor>();
	private static Hashtable<String, OperationDescriptor> paramClasses = new Hashtable<String, OperationDescriptor>();

	private static class OperationDescriptor {
		private String operationName;
		private Class<?> operationClass;
		private Class<?> paramClass;
		private Field paramField;
	}

	public static void register(Class<?> c, String taskName) {
		if (c == null)
			return;
		OperationDescriptor opd = new OperationDescriptor();
		opd.operationName = taskName.toLowerCase();
		opd.operationClass = c;
		operationClasses.put(opd.operationName,  opd);
		if (opd.paramClass != null)
			paramClasses.put(opd.paramClass.getSimpleName(), opd);
	}

	public static void register(Class<?> c) throws AppException {
		if (c == null)
			return;
		OperationDescriptor opd = new OperationDescriptor();
		opd.operationName = c.getSimpleName().toLowerCase();
		opd.operationClass = c;
		if (!Operation.class.isAssignableFrom(c)) {
			String msg1 = "L'opération " + c.getName() + " doit étendre " + Operation.class.getCanonicalName();
			Util.log.log(Level.SEVERE, msg1);
			throw new AppException("BBADOPERATION2", opd.operationName);						
		}
				
		opd.paramField = Util.getField(c, null, "param");
		if (opd.paramField != null) {
			opd.paramField.setAccessible(true);
			opd.paramClass = (Class<?>)opd.paramField.getGenericType();
		}
		
		operationClasses.put(opd.operationName,  opd);
		if (opd.paramClass != null)
			paramClasses.put(opd.paramClass.getSimpleName(), opd);
		
	}

	/**********************************************************************************/
	
	private ExecContext execContext;
	private InputData inputData;
	private Object taskCheckpoint = null;
	private Result result = new Result();
	private Document.Id taskId;
	private OperationDescriptor opd;
	
	public ExecContext execContext() { return execContext;}
	public InputData inputData() { return inputData;}
	public Object taskCheckpoint() { return taskCheckpoint;}
	public void taskCheckpoint(Object obj) {taskCheckpoint = obj;}
	public Result result() { return result;}
	public Document.Id taskId() { return taskId;}
	public boolean isTask() { return taskId != null; }
	public boolean isReadOnly() { return false; }
	public Stamp startTime() { return execContext.startTime2(); }
	public OperationDescriptor descr() { return opd; }
	public String name() { return opd.operationName; }
	
	public boolean hasAdminKey() { return execContext().hasAdminKey(); }

	public boolean hasQmKey() { return execContext().hasQmKey(); }

	public boolean isQM() { return execContext().isQM(); }
	
	public void setTask(Document.Id id, long nextStart, String info) throws AppException{
		execContext().setTaskInfo(id, nextStart, info);
	}
	
	public void setTaskByCron(Document.Id id, String cron) throws AppException {
		long nextStart = new Cron(cron).nextStart().stamp();
		execContext().setTaskInfo(id, nextStart, cron);
	}
	
//	public TaskInfo taskInfo(DocumentId id) throws AppException{
//		return execContext().dbProvider().taskInfo(id);
//	}

	public Collection<TaskInfo> listTask(TaskInfo ti) throws AppException{
		return execContext().dbProvider().listTask(ti);
	}

	public String ungzip(Attachment a) throws AppException {
		try {
			if (a == null) return null;
			 GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(a.bytes));
			 ByteArrayOutputStream bos = new ByteArrayOutputStream();
			 byte[] b = new byte[8192];
			 int l;
			 while((l = gzis.read(b)) >= 0)
				bos.write(b, 0, l);
			 gzis.close();
			 try { return new String(bos.toByteArray(), "UTF-8"); } catch (Exception e) {return null;}
		} catch (Exception e){
			throw new AppException(e, "XUNGZIPEXC", this.getClass().getSimpleName(), a.name);
		}
	}
	
	public void gzipResultat(String res) throws AppException {
		byte[] bytes = null;
		if (res == null || res.length() == 0)
			bytes = new byte[0];
		else
			try { bytes = res.getBytes("UTF-8"); } catch (UnsupportedEncodingException e) {	}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			GZIPOutputStream zos = new GZIPOutputStream(bos);
			zos.write(bytes);
			zos.close();
			result.bytes = bos.toByteArray();
			result.mime = "application/x-gzip";
		} catch (IOException e) {
			throw new AppException(e, "XGZIPEXC", this.getClass().getSimpleName());
		}
	}

	// A surcharger
	public void work() throws AppException { }
	public void afterWork() throws AppException { }

}
