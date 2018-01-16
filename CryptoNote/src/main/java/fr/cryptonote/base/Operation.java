package fr.cryptonote.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import fr.cryptonote.base.Servlet.Attachment;
import fr.cryptonote.base.Servlet.InputData;

public abstract class Operation {
	private static Hashtable<String, OperationDescriptor> operationClasses = new Hashtable<String, OperationDescriptor>();
	private static Hashtable<String, OperationDescriptor> paramClasses = new Hashtable<String, OperationDescriptor>();

	private static class OperationDescriptor {
		private String operationName;
		private Class<?> operationClass;
		private Class<?> paramClass;
		private Field paramField;
	}

	/**********************************************************************************/
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
	static Operation newOperation(String operationName, String json, Object param) throws AppException {
		if (operationName != null) operationName = operationName.toLowerCase();
		Operation op = null;
		try {
			OperationDescriptor opd = operationClasses.get(operationName);
			if (opd == null) throw new AppException("BBADOPERATION0", operationName);
			op = (Operation)opd.operationClass.newInstance();
			op.opd = opd;
			op.execContext = ExecContext.current();
			op.inputData = op.execContext.inputData();
			if (opd.paramField != null) {
				if (param == null) {
					try { param = JSON.fromJson(json == null ? "{}" : json, opd.paramClass);
					} catch (AppException e){ throw new AppException(e.cause(), "BBADOPERATION1", operationName); }
				}
				opd.paramField.set(op, param);
			}
		} catch (Exception e){
			throw new AppException(e, "BBADOPERATION0", operationName);			
		}
		return op;
	}

	/**********************************************************************************/
	private ExecContext execContext;
	private InputData inputData;
	private OperationDescriptor opd;
	
	public ExecContext execContext() { return execContext;}
	public InputData inputData() { return inputData;}
	public Object getParam() { try { return opd.paramField.get(this);} catch (Exception e) { return null; } }
	public TaskInfo taskInfo() { return inputData.taskInfo();}
	public boolean isTask() { return inputData.isTask(); }
	public boolean isReadOnly() { return false; }
	public Stamp startTime() { return inputData.isTask() ? Stamp.fromStamp(taskInfo().startTime) : execContext.startTime(); }
	public OperationDescriptor descr() { return opd; }
	public String name() { return opd.operationName; }
	public boolean isSudo() { return execContext.isSudo(); }
	
	public void addTask(Class<?> op, Object param, String info, long startAt, int qn) throws AppException { execContext().addTask(op, param, info, startAt, qn); }
	public void addTaskByCron(Class<?> op, Object param, String cron, int qn) throws AppException { execContext().addTask(op, param, cron, new Cron(cron).nextStart().stamp(), qn); }

	/**********************************************************************************/
	public String ungzip(Attachment a) throws AppException {
		try {
			if (a == null) return null;
			 GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(a.bytes));
			 ByteArrayOutputStream bos = new ByteArrayOutputStream();
			 byte[] b = new byte[8192];
			 int l; while((l = gzis.read(b)) >= 0) bos.write(b, 0, l);
			 gzis.close();
			 try { return new String(bos.toByteArray(), "UTF-8"); } catch (Exception e) {return null;}
		} catch (Exception e){
			throw new AppException(e, "XUNGZIPEXC", this.getClass().getSimpleName(), a.name);
		}
	}
	
	/**********************************************************************************/
	// A surcharger
	public Result work() throws AppException { return null; }
	public void afterWork() throws AppException { }

}
