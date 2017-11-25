package fr.cryptonote.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import fr.cryptonote.provider.DBProvider;

public class S2Cleanup extends Document {
	
	private static final int[] slices = {1,2,3,4,6,8,12,24};
	
	private static int nbh;
	private static int nbs;

	static {
		nbh = AConfig.config().S2CLEANUPPERIODINHOURS();
		boolean found = false;
		for(int i = 0; i < slices.length; i++) 
			if (slices[i] == nbh) { found = true; break; }
		if (!found) nbh = 24;
		nbs = 24 / nbh;
		
	}
	
	private static final HashMap<String,Integer> hours = new HashMap<String,Integer>();	
	
	private static long nextStart(Stamp shour) {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		int hh = tlr.nextInt(shour.HH(), shour.HH() + nbh);
		int mm = tlr.nextInt(0, 60);
		int ss = tlr.nextInt(0, 60);
		return Stamp.fromDetail(shour.yy(), shour.MM(), shour.dd(), hh, mm, ss, 0).stamp();
	}
	
	public static boolean startCleanup(boolean transaction) throws AppException {
		Stamp now  = Stamp.fromNow(0);
		Stamp day = Stamp.fromDetail(now.yy(), now.MM(), now.dd(), 0, 0, 0, 0);
		Stamp day1 = Stamp.fromEpoch(day.epoch() + 86400000);
		int slice = now.HH() / nbh;
		Stamp shour = slice == nbs - 1 ? Stamp.fromDetail(day1.yy(), day1.MM(), day1.dd(), 0, 0, 0, 0)
				: Stamp.fromDetail(day.yy(), day.MM(), day.dd(), (slice + 1) * nbh, 0, 0, 0);
		int hour = (int)(shour.stamp() / 10000000);
		
		Integer h = hours.get(groupid);
		if (h != null && h >= hour) return false;
	
		DBProvider dbProvider = ExecContext.current().dbProvider();
		int hs2 = dbProvider.lastS2Cleanup();
		if (hs2 != 0) {
			hours.put(groupid,  hs2);
			if (hs2 >= hour) return false;
		}
		TaskInfo ti = new TaskInfo(dbProvider.ns(), new Document.Id(S2Cleanup.class, "" + hour), nextStart(shour), 0, "", 0);
		if (transaction)
			ti.version = Stamp.fromNow(0).stamp();
		dbProvider.setS2Cleanup(ti, transaction);
		return true;
	}
	
	public static class Task extends Operation {		
		@Override public void work() throws AppException {
			String groupid = taskId().groupid();
			DBProvider dbProvider = ExecContext.current().dbProvider();
			HashSet<String> shas = dbProvider.shas(groupid);
			boolean emptyBasket = dbProvider.blobProvider().cleanup(groupid, shas);
			if (!emptyBasket)
				startCleanup(groupid, true);
		}
	}
	
}
