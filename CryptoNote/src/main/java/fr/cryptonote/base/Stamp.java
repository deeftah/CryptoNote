package fr.cryptonote.base;

public class Stamp implements Comparable<Stamp> {

	private static final int[][] nbj = {{0,31,28,31,30,31,30,31,31,30,31,30,31},
			{0,31,29,31,30,31,30,31,31,30,31,30,31}};
	
	private static final int[][] nbjc = new int[2][14];
	
	public static String edl(long stamp){
		String s = "000" + stamp;
		return  s.substring(s.length() - 15);
	}
	
	static {
		for (int i = 0; i < 2; i++)
			for(int m = 1; m < 14; m++)
				for(int k = 1; k < m; k++)
					nbjc[i][m] += nbj[i][k];
	}
	
	public static int nbj(int yy, int mm){
		return nbj[yy % 4 == 0 ? 1 : 0][mm];
	}

	public static int truncJJ(int yy, int mm, int jj){
		int x = nbj[yy % 4 == 0 ? 1 : 0][mm];
		return jj > x ? x : jj;
	}

	// période de 4 ans commençant par une bissectile
	private static final int qa = (365 * 4) + 1;
	private static final int[] nbjq = {0, 366, 366 + 365, 366 + 365 + 365, 366 + 365 + 365 + 365};

	// nb jours 2000-01-01 - 1970-01-01 - 30 années dont 7 bissextiles - C'était un Samedi
	private static final int nbj00 = (365 * 30) + 7;
	private static final int wd00 = 5;
	
	private static boolean skipN2 = true;
	
	public static final Stamp minStamp = Stamp.fromDetail(0,1,1,0,0,0,0);
	public static final Stamp maxStamp = Stamp.fromDetail(99,12,31,23,59,59,999);
	public static final String minStampS = "000101000000000";
	public static final String maxStampS = "991231235959999";
	
	static {
		skipN2 = false;
	}
	
	private int yy;
	private int MM;
	private int dd;
	private int HH;
	private int mm;
	private int ss;
	private int SSS;
	private int wd;
	private long epoch;
	private long epoch00;
	private int nbd00;
	private int q;
	private int nbms;
	private int date;
	private int time;
	private long stamp;
	
	public int yy() { return yy ; }
	public int MM() { return MM ; }
	public int dd() { return dd ; }
	public int HH() { return HH ; }
	public int mm() { return mm ; }
	public int ss() { return ss ; }
	public int SSS() { return SSS ; }
	public int wd() { return wd ; }
	public long epoch() { return epoch ; }
	public long epoch00() { return epoch00 ; }
	public int nbd00() { return nbd00 ; }
	public int q() { return q ; }
	public int nbms() { return nbms ; }
	public int date() { return date ; }
	public int time() { return time ; }
	public long stamp() { return stamp ; }

	@Override
	public int compareTo(Stamp o) {
		return stamp < o.stamp ? -1 : (stamp == o.stamp ? 0 : 1);
	}

	public boolean equals(Stamp o){
		return stamp == o.stamp;
	}
	
	public String toString() { return edl(stamp); }

	public long lapseInMs(){
		return System.currentTimeMillis() - epoch;
	}
	
	private Stamp normalize2(){
		if (!skipN2) {
			if (this.stamp == minStamp.stamp) return minStamp;
			if (this.stamp == maxStamp.stamp) return maxStamp;
		}
		return this;
	}
	
	private Stamp normalize(){
		if (yy < 0) yy = 0;
		if (yy > 99) yy = 99;
		if (MM < 1) MM = 1;
		if (MM > 12) MM = 12;
		dd = dd < 1 ? 1 : truncJJ(yy, MM, dd);
		if (HH < 0) HH = 0;
		if (mm < 0) mm = 0;
		if (ss < 0) ss = 0;
		if (SSS < 0) SSS = 0;
		if (HH > 23) HH = 23;
		if (mm > 59) mm = 59;
		if (ss > 59) ss = 59;
		if (SSS > 999) SSS = 999;
		time = SSS + (ss * 1000) + (mm * 100000) + (HH * 10000000);
		date = dd + (MM * 100) + (yy * 10000);
		stamp = (((long)date) * 1000000000) + time;
		q = nbjc[yy % 4 == 0 ? 1 : 0][MM] + dd;
		nbd00 = ((yy / 4) * qa) + nbjq[(yy % 4)] + q - 1;
		nbms = SSS + (ss * 1000) + (mm * 60000) + (HH * 3600000);
		epoch00 = ((long)(nbd00) * 86400000) + nbms;
		epoch = ((long)(nbd00 + nbj00) * 86400000) + nbms;
		wd = ((nbd00 + wd00) % 7) + 1;
		return this.normalize2();
	}
	
	public static Stamp fromString(String s) {
		if (s == null || s.length() == 0) return null;
		try {
			long l = Long.parseLong(s);
			if (l > maxStamp.stamp) return null;
			if (l < minStamp.stamp) return null;
			return fromStamp(l, 0);
		} catch (Exception e) {
			return null;
		}
	}

	public static Stamp fromStamp(long l){ return fromStamp(l, 0); }
	
	public static Stamp fromStamp(long l, int deltaInMs){
		if (l > maxStamp.stamp) return maxStamp;
		if (l < minStamp.stamp) return minStamp;
		Stamp s = new Stamp();
		s.yy = (int)(l / 10000000000000L);
		long x = l % 10000000000000L;
		s.MM = (int)(x / 100000000000L);
		x = l % 100000000000L;
		s.dd = (int)(x / 1000000000L);
		x = l % 1000000000L;
		s.HH = (int)(x / 10000000L);
		x = l % 10000000L;
		s.mm = (int)(x / 100000L);
		x = l % 100000L;
		s.ss = (int)(x / 1000L);
		s.SSS = (int)(l % 1000L);
		s.normalize();
		return deltaInMs == 0 ? s : fromEpoch(s.epoch + deltaInMs);
	}

	public static Stamp fromDetail(int yy, int MM, int dd, int HH, int mm, int ss, int SSS){
		Stamp s = new Stamp();
		s.yy = yy;
		s.MM = MM;
		s.dd = dd;
		s.HH = HH;
		s.mm = mm;
		s.ss = ss;
		s.SSS = SSS;
		return s.normalize();
	}
	
	public static Stamp fromNow(long deltaInMs){
		return fromEpoch(System.currentTimeMillis() + deltaInMs);
	}
	
	public static enum TimeUnit {yy, MM, dd, HH, mm, ss}
	
	public static Stamp trunc(Stamp src, TimeUnit tu){
		Stamp s = fromDetail(src.yy, src.MM, src.dd, src.HH, src.mm, src.ss, src.SSS);
		switch (tu){
		case yy : s.MM = 1;
		case MM : s.dd = 1;
		case dd : s.HH = 0;
		case HH : s.mm = 0;
		case mm : s.ss = 0;
		case ss : s.SSS = 0;
		}
		return s.normalize();
	}
	
	public static Stamp fromEpoch(long l){
		if (l > maxStamp.epoch) return maxStamp;
		if (l < minStamp.epoch) return minStamp;
		Stamp s = new Stamp();
		s.epoch = l;
		s.nbd00 = (int)((l / 86400000) - nbj00);
		s.wd = ((s.nbd00 + wd00) % 7) + 1;
		s.nbms = (int)(l % 86400000);
		s.epoch00 = (s.nbd00 * 86400000) + s.nbms;
		s.yy = (s.nbd00 / qa) * 4;
		int x1 = s.nbd00 % qa;
		for(int na = 0;;s.yy++, na++){
			int[] nbjcx = nbjc[s.yy % 4 == 0 ? 1 : 0];
			if (x1 < nbjq[na + 1]) {
				int nj = x1 - nbjq[na];
				for(s.MM = 1;; s.MM++) {
					if (nj < nbjcx[s.MM+1]){
						s.dd = nj - nbjcx[s.MM] + 1;
						break;
					}
				}
				break;
			}
		}
		s.date = s.dd + (s.MM * 100) + (s.yy * 10000);
		s.HH = s.nbms / 3600000;
		int x = s.nbms % 3600000;
		s.mm = x / 60000;
		x = s.nbms % 60000;
		s.ss = x / 1000;
		s.SSS = x % 1000;
		s.time = s.SSS + (s.ss * 1000) + (s.mm * 100000) + (s.HH * 10000000);
		s.stamp = (((long)s.date) * 1000000000) + s.time;
		return s.normalize2();
	}
	
	public static void main(String[] args){
		long l = System.currentTimeMillis();
		System.out.println(l);
		Stamp n1 = Stamp.fromNow(0);
		System.out.println(n1);
		System.out.println(n1.stamp);
		System.out.println(n1.epoch);
		Stamp n1b = Stamp.fromStamp(n1.stamp);
		System.out.println(n1b.stamp);
		System.out.println(n1b.epoch);
		Stamp n2 = Stamp.fromEpoch(n1.epoch() + 86400000);
		System.out.println(n2);
		n2 = Stamp.fromDetail(0, 0, 0, 0, 0, 0, 0);
		// Stamp n4 = minStamp;
		System.out.println(n2);
		n2 = Stamp.fromDetail(100, 13, 32, 24, 60, 60, 1000);
		System.out.println(n2);
		n2 = Stamp.fromDetail(17, 1, 1, 23, 59, 59, 999);
		System.out.println(n2);
		n2 = Stamp.fromEpoch(n2.epoch);
		System.out.println(n2);

	}
}
