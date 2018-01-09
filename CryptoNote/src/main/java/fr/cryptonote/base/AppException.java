package fr.cryptonote.base;

import java.util.logging.Level;

public class AppException extends Exception {

	private static final long serialVersionUID = 1L;
	
	/*
	 * Premier caractère de code
	 * N : notfound : 404 : not found. Document ou attachment non trouvé
	 * A : app : 400 : erreur applicative autre que N. Données incorrectes
	 * B : bug : 400 : bug, situation des données illogique
	 * X : unexpected : 400 : incident inattendu, matériel ou logiciel
	 * D : delayed build : 400 : recharger l'application cliente
	 * C : contention : 400 : trop de contention sur la base de données
	 * O : espace off  : 400 : espace en maintenance
	 * T : timeout : 400
	 * S : session non autorisée : 400 : ou résiliée ou trop longue ...
	 */

	private static class Error {
		@SuppressWarnings("unused")
		public int phase; // 0:initiale 1:operation 2:validation 3:afterwork 4:sync 5:finale
		@SuppressWarnings("unused")
		public String code; // code symbolique principal
		@SuppressWarnings("unused")
		public String[] detail; // arguments détaillés de l'erreur
		@SuppressWarnings("unused")
		public String message; // message en clair
		private Error(AppException e) {code = e.code; phase = e.phase; message = e.message; detail = e.detail;}
		public String toString() { return JSON.toJson(this); }
	}
		
	public int phase; // 0:initiale 1:operation 2:validation 3:afterwork 4:sync 5:finale
	public String code; // code symbolique principal
	public String[] detail; // arguments détaillés de l'erreur
	public String message; // message en clair

	public int httpStatus() { return code.charAt(0) == 'N' ? 404 : 400; }
	
	private Throwable cause;
	public Throwable cause() { return cause; }
			
	public AppException(String code, String... args){ this(null, code, args); }

	public String toString() { return message + (cause != null ? "\n" + cause.getMessage() : ""); }
	
	public String toJson() { return new Error(this).toString(); }
	
	public boolean toRetry(){ char c = code.charAt(0); return c == 'B' || c == 'X' || c == 'C';	}
	
	public AppException(Throwable t, String code, String... args){
		super();
		cause = t;
		ExecContext ec = ExecContext.current();
		phase = ec == null ? 0 : ec.phase();
		this.code = code == null || code.length() == 0 ? "X0" : code;
		char c = this.code.charAt(0);
		if ("NABXDCOTS".indexOf(c) == -1) this.code = "X0";
		message = BConfig.format(code,  args);
		if (t == null)
			detail = args;
		else {
			detail = new String[args == null ? 3 : args.length + 3];
			int i = 0;
			for (String x : args) detail[i++] = x;
			detail[i++] = "Cause : ";
			detail[i++] = t.getMessage();
			detail[i++] = Util.stack(t);
		}
		Util.log.log("BXCT".indexOf(c) != -1 ? Level.SEVERE : Level.FINE, toString());
	}
	
}
