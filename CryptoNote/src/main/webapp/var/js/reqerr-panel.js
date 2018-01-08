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
		App.setMsg("fr", "er_bugl", "Bug de l''application locale");
		App.setMsg("fr", "er_app", "Données incorrectes");
		App.setMsg("fr", "er_bugs", "Bug de l''application du serveur");
		App.setMsg("fr", "er_tech", "Incident technique inattendu du serveur / réseau");
		App.setMsg("fr", "er_cont", "Surcharge d''accès aux données de l''application du serveur");
		App.setMsg("fr", "er_maint", "L''application du serveur est temporairement indisponible pour maintenance");
		App.setMsg("fr", "er_aut", "Autorisation insuffisante pour effectuer cette opération");
		App.setMsg("fr", "er_to", "Dépassement du délai maximum d''exécution pour cette opération");
		App.setMsg("fr", "er_int", "Interruption de l''attente par clic.");
	}

	lib(code, lang) { 
		return App.lib(code);
	}
	
	/*
	 * Phase 
	 * -1 : connexion au serveur
	 * 0 : avant opération dans le serveur
	 * 1 : dans l'opération (work)
	 * 2 : au cours de la validation
	 * 3 : après validation (afterwork)
	 * 4 : au cours de la synchronisation
	 * 5 : lors de l'envoi de la réponse
	 * 6 : dans le script d'interprétation de la réponse
	 * 9 : inconnu entre 0 et 5
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
	 * T : timeout -> 8
	 * I : interrupted -> 9
	 * L : erreur d'exécution d'un script local (avant ou après) -> 0
	 */

	open(request, err) {
		this.request = request;
		this.err = err;
		this.$.panel.open();
		const major = err.major();
		this.where = err.phase;
		switch (major) {
		case 0 : { this.info = this.lib("er_bug1"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 1 : { this.info = this.lib("er_app"); this.more = err.message; this.topdet = "_";
			break;
		}
		case 2 : { this.info = this.lib("er_bugs"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 3 : { this.info = this.lib("er_tech"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 5 : { this.info = this.lib("er_tech"); this.more = ""; this.topdet = err.message;
			break;
		}
		case 6 : { this.info = this.lib("er_maint"); this.more = ""; this.topdet = "";
			break;
		}
		case 7 : { this.info = this.lib("er_aut"); this.more = err.message; this.topdet = err.message;
			break;
		}
		case 8 : { this.info = this.lib("er_to"); this.more = ""; this.topdet = "";
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
