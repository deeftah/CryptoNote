package fr.cryptonote.base;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.logging.Level;

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

}
