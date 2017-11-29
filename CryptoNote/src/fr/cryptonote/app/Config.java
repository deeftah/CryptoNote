package fr.cryptonote.app;

import java.util.HashMap;

import fr.cryptonote.base.AConfig;
import fr.cryptonote.base.Stamp;

public class Config extends AConfig {
	protected static class Gen extends AGen {
		int NBHEURESENCREATION;
		int NBHEURESENZOMBIE;
		int REMORDAVANTCLOTUREMINUTES;
		int Q1CPARDEFAUT;
		int Q1FPARDEFAUT;
		int Q2FPARDEFAUT;
		long RV1;
		long RV2;
		int RV1PC;
		int RV2PC;
		int SESSIONAGEMAXENSECONDES;
		int MAXNBVISITES;
		int NTFNBJ;
		Gouvernances gouvernances;
	}

	public int SESSIONAGEMAXENSECONDES() { return ((Gen)config.gen()).SESSIONAGEMAXENSECONDES; }
	public int NBHEURESENCREATION() { return ((Gen)config.gen()).NBHEURESENCREATION; }
	public int NBHEURESENZOMBIE() { return ((Gen)config.gen()).NBHEURESENZOMBIE; }
	public int REMORDAVANTCLOTUREMINUTES() { return ((Gen)config.gen()).REMORDAVANTCLOTUREMINUTES; }
	public int MAXNBVISITES() { return ((Gen)config.gen()).MAXNBVISITES; }
	public int NTFNBJ() { return ((Gen)config.gen()).NTFNBJ; }
	public int Q1CPARDEFAUT() { return ((Gen)config.gen()).Q1CPARDEFAUT; }
	public int Q1FPARDEFAUT() { return ((Gen)config.gen()).Q1FPARDEFAUT; }
	public int Q2FPARDEFAUT() { return ((Gen)config.gen()).Q2FPARDEFAUT; }
	public long RV1() { return ((Gen)config.gen()).RV1; }
	public long RV2() { return ((Gen)config.gen()).RV2; }
	public int RV1PC() { return ((Gen)config.gen()).RV1PC; }
	public int RV2PC() { return ((Gen)config.gen()).RV2PC; }
	
	private static final SchVote defScheVote = new SchVote(50,2,false,false,0);
	
	protected static class Secret extends ASecret {
		int bar;
	}
	
	static {
		config = new Config();
	}

	public static Config config() { return (Config)config; }

	private static boolean ready = false;
	
	public static void startup() throws Exception {
		synchronized (config) {
			if (ready) return;
			AConfig.startup(Gen.class, Secret.class);
				
			Gen g = (Gen)config.gen();
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
			
			ready = true;
		}
	}

	/*************************************************************************/
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
			if (GV == null) GV = ((Gen)config.gen()).gouvernances;
			Gouvernance g = GV.liste.get(nom); 
			return g != null ? g : gouvernanceVide;
		}

		public SchVote schVote(GouvernanceExc exgv, String type) { 
			SchVote s = null;
			if (exgv != null) {
				s = exgv.schema(type);
				if (s != null) return s;
			}
			if (GV == null) GV = ((Gen)config.gen()).gouvernances;
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
//		DocumentDescr.register(LivrC3.class);
//		Operation.register(GetLivrC.class);
//		Operation.register(UpdLivrC.class);
//
//		DocumentDescr.register(Repertoire.class);
//		DocumentDescr.register(RepTask.class);
//
//		Operation.register(Default.class);
//		Operation.register(DBInfo.class);
//
//		DocumentDescr.register(Compte.class);
//		DocumentDescr.register(Mur.class);
//		DocumentDescr.register(Groupe.class);
//		DocumentDescr.register(Jeton.class);
	}
}
