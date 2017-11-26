package fr.cryptonote.base;

public class Cron {
	
	private static final String periodes = "YMWDH";
	private static final int[] lperiodes = {9, 7, 6, 5, 3};

	private String periode;
	private int type;
	public String periode() { return periode; };
	public int type() { return type; };

	private int mo = 1;
	private int jo = 1;
	private int js = 1;
	private int hh = 0;
	private int mm = 0;
		
	public Cron(String periode){
		String postfix;
		this.periode = periode;
		if (periode == null || periode.length() < 1)
			throw new IllegalArgumentException("Période vide");
		char p = periode.charAt(0);
		type = periodes.indexOf(p);
		if (type == -1) 
			throw new IllegalArgumentException("Période [" + p + "] : doit être Y M W D ou H");
		int lg = lperiodes[type];
		if (lg != periode.length())
			throw new IllegalArgumentException("Période [" + p + "] : doit avoir " + lg + " caractères");
		postfix = periode.substring(1);
		try {
			switch (type){
			case 0 : { // Y
				mo = Integer.parseInt(postfix.substring(0,2));
				jo = Integer.parseInt(postfix.substring(2,4));
				hh = Integer.parseInt(postfix.substring(4,6));
				mm = Integer.parseInt(postfix.substring(6,8));
				break;
			}
			case 1 : { // M 
				jo = Integer.parseInt(postfix.substring(0,2));
				hh = Integer.parseInt(postfix.substring(2,4));
				mm = Integer.parseInt(postfix.substring(4,6));
				break;
			}
			case 2 : { // W 
				js = Integer.parseInt(postfix.substring(0,1));
				hh = Integer.parseInt(postfix.substring(1,3));
				mm = Integer.parseInt(postfix.substring(3,5));
				break;
			}
			case 3 : { // D 
				hh = Integer.parseInt(postfix.substring(0,2));
				mm = Integer.parseInt(postfix.substring(2,4));				
				break;
			}
			case 4 : { // H 
				mm = Integer.parseInt(postfix.substring(0,2));				
				break;
			}
			}
		} catch (Exception x) {
			throw new IllegalArgumentException("Période [" + periode + "] : valeur non numérique de mois, jour, jour de semaine, heure ou minute");
		}
		if (mo < 1 || mo > 12)
			throw new IllegalArgumentException("Période [" + periode + "] : valeur du mois incorrecte");
		if (jo < 1 || jo > Stamp.nbj(1, mo))
			throw new IllegalArgumentException("Période [" + periode + "] : valeur du jour incorrecte");
		if (js < 1 || js > 7)
			throw new IllegalArgumentException("Période [" + periode + "] : valeur du jour de semaine incorrecte");
		if (hh < 0 || hh > 23)
			throw new IllegalArgumentException("Période [" + periode + "] : valeur des heures incorrecte");
		if (mm < 0 || mm > 59)
			throw new IllegalArgumentException("Période [" + periode + "] : valeur des minutes incorrecte");
	}
		
	public Stamp nextStart() {
		Stamp s = Stamp.fromNow(0);
		switch (type){
		case 0 : { // Y 
			// essai même année, si échec année suivante
			Stamp s2 = Stamp.fromDetail(s.yy(), mo, jo, hh, mm, 0, 0);
			return s2.compareTo(s) > 0 ? s2 : Stamp.fromDetail(s.yy() + 1, mo, jo, hh, mm, 0, 0);
		}
		case 1 : { // M
			// essai même mois
			int yx = s.yy(), mx = s.MM();
			Stamp s2 = Stamp.fromDetail(yx, mx, Stamp.truncJJ(yx, mx, jo), hh, mm, 0, 0);
			if (s2.compareTo(s) > 0) return s2 ;
			mx++; // mois suivant
			if (mx > 12) { yx++; mx = 1; }
			return Stamp.fromDetail(yx, mx, Stamp.truncJJ(yx, mx, jo), hh, mm, 0, 0);
		}
		case 2 : { // W 
			int yx = s.yy(), mx = s.MM(), wc = s.wd(), hc = s.HH(), mc = s.mm();
			int d = 0;
			if (wc == js){
				if (hc > hh || (hc == hh && mc > mm))
					d = 7; // semaine prochaine
			} else if (wc > js)
				d = js + 7 - wc;
			else if (wc < js)
				d = js - wc;
			int jx = s.dd() + d;
			
			int nj = Stamp.nbj(yx, mx);
			if (jx > nj) { mx++; jx = jx - nj; }
			if (mx > 12) { yx++; mx = 1; }
			return Stamp.fromDetail(yx, mx, jx, hh, mm, 0, 0);			
		}
		case 3 : { // D
			int yx = s.yy(), mx = s.MM(), jx = s.dd() + 1;
			int nj = Stamp.nbj(yx, mx);
			if (jx > nj) { mx++; jx = 1; }
			if (mx > 12) { yx++; mx = 1; }
			return Stamp.fromDetail(yx, mx, jx, hh, mm, 0, 0);			
		}
		case 4 : { // H
			int yx = s.yy(), mx = s.MM(), jx = s.dd(), hx = s.HH() + 1;
			if (hx > 23) {
				hx = 0;
				jx++;
				int nj = Stamp.nbj(yx, mx);
				if (jx > nj) { mx++; jx = 1; }
				if (mx > 12) { yx++; mx = 1; }				
			}
			return Stamp.fromDetail(yx, mx, jx, hx, mm, 0, 0);			
		}
		}
		return Stamp.maxStamp;
	}
	
	public static void t(String p) {
		try {
			Cron c = new Cron(p);
			System.out.println(p + " " + c.nextStart());
		} catch (Exception e){
			System.out.println(p + " " + e.getMessage());
		}
	}

	public static void main(String[] args){
		Stamp n1 = Stamp.fromNow(0);
		Stamp n2 = Stamp.fromEpoch(n1.epoch() + 86400000);
		System.out.println(n1 + " " + n2);
		
		t(null);
		t("");
		t("Y");
		t("Y1234567");
		t("M12345");
		t("W1234");
		t("D123");
		t("X");

		t("Y13312359");
		t("Y02302359");
		t("Y02282459");
		t("Y02282360");
		t("Y02282359");

		t("M292359");

		t("W12359");
		t("W22359");
		t("W32359");
		t("W42359");
		t("W52359");
		t("W62359");
		t("W72359");

		t("D0859");

		t("H35");

	}

}
