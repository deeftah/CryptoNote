package fr.cryptonote.test;

import fr.cryptonote.base.DocumentDescr;

public class Config {
	private static Config c = null;
	public static void startup(Object config) throws Exception {
		c = (Config)config;
		declareDocumentsAndOperations();
	}
	
	private String foo;
	
	public static String foo() { return c.foo; }
	
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
