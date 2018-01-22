package fr.cryptonote.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import fr.cryptonote.provider.DBProvider;

public class S2Cleanup extends Document {
	
	/*
	 * Le nettoyage d'un document est lancé par une tâche qui s'exécute dans les N heures 
	 * après une création / modification / suppression de pièces jointes sur le document. 
	 * Si rien ne bouge pendant un certain temps sur un document aucun nettoyage n'est lancé 
	 * et si des modifications frénétiques sont effectuées, 
	 * au plus un nettoyage intervient par groupe de nbh heures (1, 2, 3, 4, 6, 8 ou 12) configurable.  
	 */
	private static final int[] slices = {1,2,3,4,6,8,12,24};
	
	private static int nbh;
	private static int nbs; // nombre de tranches de nbh heures dans un jour

	static {
		nbh = BConfig.S2CLEANUPPERIODINHOURS();
		boolean found = false;
		for(int i = 0; i < slices.length; i++) if (slices[i] == nbh) { found = true; break; }
		if (!found) nbh = 24;
		nbs = 24 / nbh;
	}
	
	private static final HashMap<String,Integer> hours = new HashMap<String,Integer>();	
	
	// tire une heure au hasard dans la tranche de nbh heures débutant à shour
	private static long nextStart(Stamp shour) {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		int hh = tlr.nextInt(shour.HH(), shour.HH() + nbh);
		int mm = tlr.nextInt(0, 60);
		int ss = tlr.nextInt(0, 60);
		return Stamp.fromDetail(shour.yy(), shour.MM(), shour.dd(), hh, mm, ss, 0).stamp();
	}
	
	/*
	 * Lance la tâche de nettoyage pour le document identifié par clid (classe.docid)
	 * Si transaction est vrai cette exécution s'effectue dans le contexte de la transaction en cours du thread
	 */
	public static boolean startCleanup(String clid) throws AppException {
		Stamp now  = Stamp.fromNow(0);
		Stamp day = Stamp.fromDetail(now.yy(), now.MM(), now.dd(), 0, 0, 0, 0);
		Stamp day1 = Stamp.fromEpoch(day.epoch() + 86400000);
		int slice = now.HH() / nbh;
		Stamp shour = slice == nbs - 1 ? Stamp.fromDetail(day1.yy(), day1.MM(), day1.dd(), 0, 0, 0, 0)
				: Stamp.fromDetail(day.yy(), day.MM(), day.dd(), (slice + 1) * nbh, 0, 0, 0);
		int hour = (int)(shour.stamp() / 10000000);
		
		Integer h = hours.get(clid);
		// Une tâche a peut-être déjà été lancée pour cette tranche
		if (h != null && h >= hour) return false;
	
		ExecContext exec = ExecContext.current();
		int hs2 = exec.dbProvider().lastS2Cleanup(clid);
		if (hs2 != 0) {
			hours.put(clid,  hs2);
			if (hs2 >= hour) return false;
		}
		TaskInfo ti = new TaskInfo(exec.nsqm().code, DoS2Cleanup.class, new DoS2Cleanup.Param(clid), clid, null, nextStart(shour), 0);
		exec.dbProvider().setS2Cleanup(ti, clid);
		return true;
	}
	
	public static class DoS2Cleanup extends Operation {
		public static class Param {
			public String clid;
			public Param() {}
			public Param(String clid) { this.clid = clid; }
		}
		
		Param param;

		@Override public Result work() throws AppException {
			DBProvider dbProvider = ExecContext.current().dbProvider();
			HashSet<String> shas = dbProvider.shas(new Id(param.clid));
			boolean emptyBasket = dbProvider.blobProvider().cleanup(param.clid, shas);
			if (!emptyBasket)
				startCleanup(param.clid);
			return Result.taskComplete();
		}
	}
	
}
