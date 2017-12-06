package fr.cryptonote.test;

import java.util.HashMap;

import fr.cryptonote.base.AConfig;
import fr.cryptonote.base.DocumentDescr;

public class Config extends AConfig {
	protected static class Gen extends AGen {
		
	}
		
	protected static class Secret extends ASecret {
		int bar;
	}
	
	static {
		config = new Config();
	}

	public static class NSSrvCfg {
		public HashMap<String,String> args;
	}

	@Override public Object newNSSrvCfg() {	return new NSSrvCfg(); }

	public static Config config() { return (Config)config; }

	private static boolean ready = false;
	public static void startup() throws Exception {
		synchronized (config) {
			if (ready) return;
			AConfig.startup(Gen.class, Secret.class);
				
			@SuppressWarnings("unused")
			Gen g = (Gen)config.gen();
//			if (g.NBHEURESENCREATION <= 0) g.NBHEURESENCREATION = 72 ;

			declareDocumentsAndOperations();
			ready = true;
		}
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
