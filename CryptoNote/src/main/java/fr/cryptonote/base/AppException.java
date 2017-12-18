package fr.cryptonote.base;

import java.util.logging.Level;

public class AppException extends Exception {

	private static final long serialVersionUID = 1L;
	
	/*
	 * Premier caractère de code
	 * N : notfound -> 1 : 404 : not found. Document ou attachment non trouvé
	 * A : app -> 1 : 400 : erreur applicative autre que N. Données incorrectes
	 * B : bug -> 2 : 400 : bug, situation des données illogique
	 * X : unexpected -> 3 : 400 : incident inattendu, matériel ou logiciel
	 * D : delayed build -> 4 : 400 : recharger l'application cliente
	 * C : contention -> 5 : 400 : trop de contention sur la base de données
	 * O : espace off -> 6 : 400 : espace en maintenance
	 * S : session non autorisée -> 7 : 400 : ou résiliée ou trop longue ...
	 */

	public static class Error {
		public int major;  // 1 à 7 selon la première lettre du minor
		public int phase; // 0:initiale 1:operation 2:validation 3:afterwork 4:sync 5:finale
		public String minor; // code symbolique principal
		public String[] args; // arguments détaillés de l'erreur
		public String[] detail = new String[2]; // [0] : message en clair, [1]exception cause de l'erreur et son stack
		public int httpStatus; // 400 404
		public void addDetail(String s) {
			if (s == null || s.length() == 0) return;
			if (detail[1].length() == 0)
				detail[1] = s;
			else
				detail[1] += "\n\n" + s;
		}
		public String getMessage() {
			StringBuffer sb = new StringBuffer();
			sb.append("AppException: ").append(httpStatus);
			String mc = BConfig.label("MC" + major);
			sb.append(" Nature:").append(mc).append(" Code:").append(minor);
			sb.append("\nDetail: ").append(detail[0]);
			return sb.toString();
		}
		public String toString() {
			return getMessage() + "\n" + detail[1];
		}
		public String toJSON() {
			return JSON.toJson(this);
		}
	}
	
	private Error error = new Error();
	public Error error() { return error; }
	
	private Throwable cause;
	public Throwable cause() { return cause; }
			
	public AppException(String code, String... args){ this(null, code, args); }

	public boolean toRetry(){ return error.major == 2 || error.major == 3 || error.major == 5;	}
	
	public AppException(Throwable t, String code, String... args){
		super();
		ExecContext ec = ExecContext.current();
		error.phase = ec == null ? 0 : ec.phase();
		if (code == null || code.length() == 0)
			code = "X";
		char c = code.charAt(0);
		error.httpStatus = c == 'N' ? 404 : 400;
		switch (c) {
			case 'N' :
			case 'A' : {error.major = 1; break; }
			case 'B' : {error.major = 2; break; }
			case 'X' : {error.major = 3; break; }
			case 'D' : {error.major = 4; break; }
			case 'C' : {error.major = 5; error.phase = 1; break; }
			case 'O' : {error.major = 6; error.phase = 0; break; }
			case 'S' : {error.major = 7; break; }
			default : error.major = 3;
		}
		error.minor = code;
		
		error.detail[0] = BConfig.format(code,  args);
		
		error.detail[1] = "";
		if (t != null) {
			String x = t.getMessage();
			error.detail[1] = "Cause : " + (x == null || "null".equals(x) ? "" : x) + "\n" + Util.stack(t);
		}
		
		error.args = args != null ? args : new String[0];
		Util.log.log(error.major < 2 ? Level.FINE : Level.SEVERE, error.toString());
	}
	
	public String getMessage() {
		return error.getMessage();
	}

	public String toString() {
		return error.toString();
	}
	
}
