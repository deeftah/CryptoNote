class ReqerrPanel extends Polymer.Element {
	static get is() { return "reqerr-panel"; }
  
	static get properties() { return {
    	  lang:{type:String, value:App.lang},
    	  err:{type:Object, value:{code:"LX"}},
    	  topInfo:{type:String, value:"?"},
      };
	}
	
	constructor() {
		super();
		App.setMsg("fr", "er_retry", "Ré-essayer");
		App.setMsg("fr", "er_resume", "Renoncer");
		App.setMsg("fr", "er_reload", "Recharger l'application");
		App.setMsg("fr", "er_quit", "Quitter l'application");
		App.setMsg("fr", "er_bugl", "Bug dans un script de l''application locale");
		App.setMsg("fr", "er_app", "Données soumises au serveur qui les a détectées incompatibles avec celles enrégistrées");
		App.setMsg("fr", "er_bugs", "Bug de l''application du serveur");
		App.setMsg("fr", "er_tech", "Problème technique inattendu rencontré sur le serveur ou le réseau");
		App.setMsg("fr", "er_cont", "Surcharge d''accès aux données de l''application du serveur");
		App.setMsg("fr", "er_maint", "L''application du serveur est temporairement indisponible pour maintenance");
		App.setMsg("fr", "er_build", "La version de l''application locale est incompatible avec la version de l''application du serveur. Elle doit être rechargée");
		App.setMsg("fr", "er_aut", "Autorisation insuffisante pour effectuer cette opération");
		App.setMsg("fr", "er_to", "Dépassement du délai maximum d''exécution pour cette opération");
		App.setMsg("fr", "er_int", "Interruption de l''attente par clic.");
		App.setMsg("fr", "er_phs", "Erreur détectée par l''application du serveur : ");
		App.setMsg("fr", "er_phl", "Erreur détectée par l'application locale : ");
		App.setMsg("fr", "er_ph0", "avant d'avoir commencé l''exécution de l''opération.");
		App.setMsg("fr", "er_ph1", "pendant l''exécution de l''opération.");
		App.setMsg("fr", "er_ph2", "pendant la validation des mises à jour effectuées par l''opération.");
		App.setMsg("fr", "er_ph3", "après la validation des mises à jour (éventuelles) effectuées par l''opération.");
		App.setMsg("fr", "er_ph4", "après la validation des mises à jour (éventuelles) effectuées par l''opération, au cours de la collecte des données pour synchroniser les données locales avec celles du serveur.");
		App.setMsg("fr", "er_ph5", "après la fin complète du traitement pendant l''envoi de la réponse.");
		App.setMsg("fr", "er_ph6", "à un momment non déterminable entre l''envoi de la requête, son traitement sur le serveur et le retour de sa réponse (réseau ? serveur ?).");
		App.setMsg("fr", "er_ph8", "avant l'envoi au serveur.");
		App.setMsg("fr", "er_ph9", "au cours du traitement de la réponse.");
		App.setMsg("fr", "er_opr", "Opération de consultation sans mises à jour de données");
		App.setMsg("fr", "er_opw", "Opération pouvant comporter des mises à jour de données");
		App.setMsg("fr", "er_code", "Code de l''erreur : {0}");
		
		App.setMsg("fr", "er_whereq", "Où a été détecté l''erreur, à quelle phase de traitement ?");
		App.setMsg("fr", "er_upddoneq", "Les données ont-elles été mises à jour sur le serveur ?");
		App.setMsg("fr", "er_detailq", "Informations plus détaillées à propos de l''erreur");
		App.setMsg("fr", "er_retryq", "Faut-il ré-essayer ?");
		App.setMsg("fr", "er_resumeq", "Faut-il renoncer ?");
		App.setMsg("fr", "er_reloadq", "Faut-il recharger l'application ?");

		App.setMsg("fr", "op_opr", "opération de test des erreurs");
		App.setMsg("fr", "op_opw", "*opération de test des erreurs");

	}

	lib(code, lang) { 
		return App.lib(code);
	}
	
	/*
	 * Phase 
	 * 0 : avant opération dans le serveur
	 * 1 : dans l'opération (work)
	 * 2 : au cours de la validation
	 * 3 : après validation (afterwork)
	 * 4 : au cours de la synchronisation
	 * 5 : lors de l'envoi de la réponse
	 * 6 : inconnu entre 0 et 5, et / ou réseau
	 * 8 : dans le script d'envoi au serveur
	 * 9 : dans le script d'interprétation de la réponse
	 */
	
	/*
	 * N : notfound -> 1 : 404 : not found. Document ou attachment non trouvé
	 * A : app -> 1 : 400 : erreur applicative autre que N. Données incorrectes
	 * B : bug -> 2 : 400 : bug, situation des données illogique
	 * X : unexpected -> 3 : 400 : incident inattendu, matériel ou logiciel
	 * D : delayed build -> 4 : 400 : recharger l'application cliente
	 * C : contention -> 5 : 400 : trop de contention sur la base de données
	 * O : espace off -> 6 : 400 : espace en maintenance
	 * S : session non autorisée -> 7 : 400 : ou résiliée ou trop longue ...
	 * T : timeout -> 8 (phase -1 ou 6)
	 * I : interrupted -> 9
	 * L : erreur d'exécution d'un script local (avant ou après) -> 0 (phase 8 ou 9)
	 */

	open(request, err) {
		this.request = request;
		this.err = err;
		this.$.panel.open();
		const major = err.major();
		this.where = err.phase;
		this.hiddenbtns = false;
		this.oplib = this.lib("op_" + this.err.op);
		this.isUpdate = this.oplib.startsWith("*");
		if (this.isUpdate) this.oplib = this.oplib.substring(1);
		this.optype = this.lib("er_op" + (this.isUpdate ? "w" : "r"));
		this.lines = this.err.detail.join("\n-----------------------\n");
		this.where = App.format("er_ph" + (this.err.isSrv() ? "s" : "l")) + App.format("er_ph" + this.err.phase);
		this.detaillabel = App.format("er_code", this.err.code);
		
		switch (major) { 
		case 0 : { this.info = this.lib("er_bugl"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 1 : { this.info = this.lib("er_app"); this.more = err.message; this.topdet = "";
			break;
		}
		case 2 : { this.info = this.lib("er_bugs"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 3 : { this.info = this.lib("er_tech"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 4 : { this.info = this.lib("er_build"); this.more = err.message; this.topdet = ""; this.hiddenbtns = false;
			break;
		}
		case 5 : { this.info = this.lib("er_cont"); this.more = err.message; this.topdet = "";
			break;
		}
		case 6 : { this.info = this.lib("er_maint"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 7 : { this.info = this.lib("er_aut"); this.more = err.message; this.topdet = "";
			break;
		}
		case 8 : { this.info = this.lib("er_to"); this.more = err.message; this.topdet = "";
			break;
		}
		case 9 : { this.info = this.lib("er_int"); this.more = ""; this.topdet = "";
			break;
		}
		}
	}
	
	onRetry() {
		this.close();
		if (this.request && this.request.onRetry) this.request.onRetry();
	}
	
	onReturnToApp() {
		this.close();
		if (this.request && this.request.onReturnToApp) this.request.onReturnToApp(this.err);		
	}
	
	onBye() {
		this.close();
		App.bye();
	}

	onReload() {
		this.close();
		App.reload();
	}

	close() { this.$.panel.close(); }
	
}
customElements.define(ReqerrPanel.is, ReqerrPanel);
