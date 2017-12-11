package fr.cryptonote.app;

import java.util.HashMap;

import fr.cryptonote.base.Stamp;

public class Config {
	private static Config g;
	
	private int NBHEURESENCREATION;
	private int NBHEURESENZOMBIE;
	private int REMORDAVANTCLOTUREMINUTES;
	private int Q1CPARDEFAUT;
	private int Q1FPARDEFAUT;
	private int Q2FPARDEFAUT;
	private long RV1;
	private long RV2;
	private int RV1PC;
	private int RV2PC;
	private int SESSIONAGEMAXENSECONDES;
	private int MAXNBVISITES;
	private int NTFNBJ;
	private Gouvernances gouvernances;
	
	public int SESSIONAGEMAXENSECONDES() { return g.SESSIONAGEMAXENSECONDES; }
	public int NBHEURESENCREATION() { return g.NBHEURESENCREATION; }
	public int NBHEURESENZOMBIE() { return g.NBHEURESENZOMBIE; }
	public int REMORDAVANTCLOTUREMINUTES() { return g.REMORDAVANTCLOTUREMINUTES; }
	public int MAXNBVISITES() { return g.MAXNBVISITES; }
	public int NTFNBJ() { return g.NTFNBJ; }
	public int Q1CPARDEFAUT() { return g.Q1CPARDEFAUT; }
	public int Q1FPARDEFAUT() { return g.Q1FPARDEFAUT; }
	public int Q2FPARDEFAUT() { return g.Q2FPARDEFAUT; }
	public long RV1() { return g.RV1; }
	public long RV2() { return g.RV2; }
	public int RV1PC() { return g.RV1PC; }
	public int RV2PC() { return g.RV2PC; }

	public static void startup(Object config) throws Exception {
		g = (Config)config;
		
		if (g.NBHEURESENCREATION <= 0) g.NBHEURESENCREATION = 72 ;
		if (g.NBHEURESENZOMBIE <= 0) g.NBHEURESENZOMBIE = 240 ;
		if (g.MAXNBVISITES <= 0) g.MAXNBVISITES = 10 ;
		if (g.NTFNBJ <= 0) g.NTFNBJ = 30 ;
		if (g.Q1CPARDEFAUT <= 0) g.Q1CPARDEFAUT = 1;
		if (g.Q1FPARDEFAUT <= 0) g.Q1FPARDEFAUT = 5;
		if (g.Q2FPARDEFAUT <= 0) g.Q2FPARDEFAUT = 20;
		if (g.RV1 <= 0) g.RV1 = 1000000;
		if (g.RV2 <= 0) g.RV2 = 10000000;
		if (g.RV1PC <= 0) g.RV1PC = 10;
		if (g.RV2PC <= 0) g.RV2PC = 10;
		if (g.SESSIONAGEMAXENSECONDES <= 0) g.SESSIONAGEMAXENSECONDES = 43200;

		declareDocumentsAndOperations();
	}

	/*************************************************************************/
	private static final SchVote defScheVote = new SchVote(50,2,false,false,0);

	public static class GouvernanceExc extends HashMap<String,SchVote> {
		private static final long serialVersionUID = 1L;
		private SchVote schema(String type) { return get(type); }
	}

	public static class Gouvernances {
		private SchVote[] schemas;
		private HashMap<String,Gouvernance> liste;
	}

	public static class Gouvernance extends HashMap<String,Integer> {
		private static final long serialVersionUID = 1L;
		
		private static Gouvernances GV = null;
		
		private static final Gouvernance gouvernanceVide = new Gouvernance();
		
		public static Gouvernance gouvernance(String nom) { 
			if (GV == null) GV = g.gouvernances;
			Gouvernance g = GV.liste.get(nom); 
			return g != null ? g : gouvernanceVide;
		}

		public SchVote schVote(GouvernanceExc exgv, String type) { 
			SchVote s = null;
			if (exgv != null) {
				s = exgv.schema(type);
				if (s != null) return s;
			}
			if (GV == null) GV = g.gouvernances;
			Integer ns = get(type);
			if (ns == null || ns < 0 || ns > GV.schemas.length) return defScheVote;
			s = GV.schemas[ns];
			return s != null ? s : defScheVote;
		}
	}

	/*************************************************************************/
	public static class SchVote {
//		public static SchVote get(String json) throws AppException {
//			try {
//				return (SchVote) JSON.fromJson(json, SchVote.class);
//			} catch (AppException e){
//				Compte.AppExc("BPARAM", "schVote", json);
//				return null;
//			}
//		}
		
		public SchVote() {}
		
		public SchVote(int p, int m, boolean v, boolean i, int h) {
			this.p = p; this.v = v; this.m = m; this.i = i; this.h = h;
		}
		
		private int p = 30;	// pourcentage requis;
		private int m = 1;	// minimum de votes pour requis
		private boolean v; 	// vrai si veto pris en compte
		private boolean i;	// vrai si décompte basé sur les inscrits et non les votants
		private int h = 0;	// nombre d'heures minimale d'ouverture 

		public int p() { return p; }
		public int m() { return m; }
		public boolean v() { return v; }
		public boolean i() { return i; }
		public int h() { return h; }

		public boolean dp(int[] votes, int nbInscrits) {
			if (v && votes[4] != 0) return false; // un veto -> désapprobation
			int c = votes[2] + votes[4];	// un veto compte contre
			if (votes[1] <= c) return false; // plus de contre que de pour ou égalité -> désapprobation
			int nb = nbInscrits; // nombre d'inscrits ou de votes exprimés (blanc inclus comme exprimés mais pas absention)
			if (!i) {
				nb = 0;
				for(int x = 1; x < 4; x++) nb += votes[x];
			}
			int n1 = nb * p;
			int nm = n1 / 100;
			if (n1 % 100 != 0) nm++; // si le nombre min requis est x,... c'est x + 1
			if (nm < m) nm = m; // si le min requis par pourcentage est inférieur au min requis absolu, c'est le min requis absolu qui compte
			return votes[1] >= m; // s'il y a au moins autant de pour que le min requis, -> approbation.
		}
		public boolean dphl(String dhdv) {
			return dhdv == null ? true : Stamp.fromStamp(Stamp.fromString(dhdv).stamp(), h * 3600000).epoch() < System.currentTimeMillis();
		}
	}
	
	/****************************************************/

	private static void declareDocumentsAndOperations() throws Exception{
//		DocumentDescr.register(Compte.class);
//		DocumentDescr.register(Mur.class);
//		DocumentDescr.register(Groupe.class);
//		DocumentDescr.register(Jeton.class);
	}
}
