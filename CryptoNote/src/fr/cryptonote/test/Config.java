package fr.cryptonote.test;

import java.util.HashMap;

import fr.cryptonote.base.DocumentDescr;
import fr.cryptonote.base.IConfig;

public class Config implements IConfig {
	private static Config c = null;
	public static void startup(IConfig config) throws Exception {
		c = (Config)config;
		declareDocumentsAndOperations();
	}
	
	private String foo;
	private HashMap<String,Integer>	queueIndexByOp;
	
	public static String foo() { return c.foo; }
	
	@Override public int queueIndexByOp(String op) { 
		Integer i = c.queueIndexByOp == null ? null : c.queueIndexByOp.get(op); 
		return i == null ? 0 : i; 
	}

	/****************************************************/

	private static void declareDocumentsAndOperations() throws Exception{
		DocumentDescr.register(Repertoire.class);
//		ItemDescr itd = DocumentDescr.get(Repertoire.class).itemDescr(Contact.class);
//		DocumentDescr[] dx = itd.copyToDocs();
//		char sep = itd.separator();
//		DocumentDescr.register(LivrC3.class);
//		Operation.register(GetLivrC.class);
//		Operation.register(UpdLivrC.class);
	}
}
