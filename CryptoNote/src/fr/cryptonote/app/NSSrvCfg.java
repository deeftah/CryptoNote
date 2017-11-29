package fr.cryptonote.app;

import java.util.ArrayList;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.NS;
import fr.cryptonote.base.Operation;

public class NSSrvCfg {
	public ArrayList<String> premiers;
	
	public static boolean estPrimitif (Operation op, String nc) {
		NSSrvCfg cfg = null;
		try {
			cfg = (NSSrvCfg)NS.srvcfg(op.execContext().ns());
		} catch (AppException e) {
			return false;
		}
		return cfg != null && cfg.premiers != null && cfg.premiers.contains(nc);
	}
}
