class ReqerrPanel extends Polymer.Element {
	static get is() { return "reqerr-panel"; }
  
	static get properties() { return {
    	  lang:{type:String, value:App.lang},
    	  err:{type:Object, value:{code:"LX"}},
    	  cref : {type:String, value:"0"}
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
		App.setMsg("fr", "er_ph10", "impossible de savoir où en était le traitement sur le serveur.");
		App.setMsg("fr", "er_opr", "Opération de consultation sans mises à jour de données");
		App.setMsg("fr", "er_opw", "Opération pouvant comporter des mises à jour de données");
		App.setMsg("fr", "er_code", "Code de l''erreur : {0}");
		
		App.setMsg("fr", "er_whereq", "Où a été détecté l''erreur, à quelle phase de traitement ?");
		App.setMsg("fr", "er_upddoneq", "Les données ont-elles été mises à jour sur le serveur ?");
		App.setMsg("fr", "er_detailq", "Informations plus détaillées à propos de l''erreur");
		App.setMsg("fr", "er_retryq", "Faut-il ré-essayer ?");
		App.setMsg("fr", "er_resumeq", "Faut-il renoncer ?");
		App.setMsg("fr", "er_reloadq", "Faut-il recharger l''application ?");

		App.setMsg("fr", "er_retryr1", "Le problème est que les données envoyées ne sont pas acceptables au vu de celles enregistrées. Ré-essayer produira a priori les mêmes effets, la cause étant inchangée. Toutefois si une autre session est intervenue depuis et a modifié les données enregistrées, il est posible que désormais les données envoyées soient jugées acceptables.");
		App.setMsg("fr", "er_retryr2", "Le bug provient d'une situation des données que le développeur n''avait pas imaginé qu'elle pouvait se produire. Si le bug était fugitif, résultait d''une situation temporelle rare, ré-essayer permet parfois de ne pas rencontrer la même situation. Si la séquence boguée retrouve la même situation, elle retombera dans le même bug. Toutefois depuis les données enregistrées pouvant avoir changé, peut-être que la situation imprévue ne se produira pas.");
		App.setMsg("fr", "er_retryr3", "L'incident technique peut résulter de n''importe quel problème d''accès aux données, sur le serveur ou le réseau. Si la situation critique est dépassée, le réseau à nouveau joignable, le serveur à nouveau joignable, la base de données également, le problème était fugitif et ré-essayer sera un succès. Si le problème persiste le même échec se produira. Voir déjà si la cause n''est pas locale (internet est-il joignable ?)");
		App.setMsg("fr", "er_retryr5", "Par principe la contention sur les données n''est pas durable : à un momment ou un autre la charge sur le serveur va se réduire et ré-essayer sera un succès. Le serveur peut toutefois être la cible d''une attaque par saturation d''envoi de requêtes et le problème n''est pas facile à contourner.");
		App.setMsg("fr", "er_retryr6", "L''application sera finalement ré-ouverte au service, une fois la maintenance terminée. Quand à savoir quand ... Ré-essayer à intervalles réguliers est une bonne option.");
		App.setMsg("fr", "er_retryr7", "L''authentification de la session a échoué. Il y a peu de chances qu'elle réussisse en ré-essayant, sauf si une autre session a fait en sorte que ce soit possible depuis.");
		App.setMsg("fr", "er_retryr8", "Si le dépassement de délai est lié à une surcharge temporaire, ré-essayer donne des chances de tomber après le pic de charge. Si ce dépassement est lié à une boucle de traitement infinie (c''est un bug), ré-essayer retombera dans la même situation, sauf si les données ayant changé cette situation imprévue et mal gérée par le développeur ne se reproduit pas.");
		App.setMsg("fr", "er_retryr9", "Ré-essayer et ne pas interrompre est une bonne option.");
		App.setMsg("fr", "er_retryr0", "L''incident a été détecté par l''application locale : té-essayer retransmet la requête au serveur qui en général va répondre à peu près la même chose et reprovoquer le même incident ... sauf si les conditions ont évolué, typiquement si l''inciden était lié à un problème réseau.");
		
		App.setMsg("fr", "er_upddoner0", "La requête n''est PAS parvenue au serveur. Aucune mise à jour n''a pu être calculée.");
		App.setMsg("fr", "er_upddoner1", "Le problème se situe AVANT la phase de validation des données, du moins avant sa fin effective. Aucune mise à jour n''a pu être validée.");
		App.setMsg("fr", "er_upddoner2", "Le problème se situe APRES la phase de validation des données : calcul des données à synchroniser dans la session, envoi de la réponse. S'il y avait des mises à jour, elles ont été validées. Normalement l''application est prémunie contre les mises à jour en double, ré-essayer ne devrait pas produire d''effet indésirable");
		App.setMsg("fr", "er_upddoner3", "Il est impoosible de déterminer techniquement si les mises à jour ont été validées ou non. Normalement l''application est prémunie contre les mises à jour en double, ré-essayer ne devrait pas produire d''effet indésirable. Il est aussi possible de renoncer à l''opération et de regarder si les effets des mises à jour s'y retrouvent.");
		
		App.setMsg("fr", "er_resumer0", "Renoncer à l''opération consiste à consentir à son échec : l''application locale agira en conséquence, permettra de modifier la saisie avant une nouvelle tentative ou actera de la non disponibilité du résultat attendu. Toutefois en cas de mises à jour, il n''est pas impossible qu'eles aient été validées (voir ci-dessus). Appuyer un peu plus tard sur le bouton \"Synchroniser\" est une bonne idée.");
		App.setMsg("fr", "er_resumer1", "Renoncer à l''opération consiste à consentir à son échec : l''application locale agira en conséquence, permettra de modifier la saisie avant une nouvelle tentative ou actera de la non disponibilité du résultat attendu. Appuyer un peu plus tard sur le bouton \"Synchroniser\" est une bonne idée.");

		App.setMsg("fr", "er_reloadr0", "Le problème ayant été détecté par le serveur, recharger l''application locale semble inutile. Toutefois c''est peut-être parce que celle-ci est corrompue que les données envoyées sont incohérentes. Recharger l''application est une option irrationnelle et pourtant souvent efficace.");
		App.setMsg("fr", "er_reloadr1", "Si le problème avait une cause technique externe ou résultait d''un incohérence applicative bien délimitée (une erreur de saisie...), recharger l''application ne résoudra rien. Toutefois il arrive que la session dans le navigateur soit corrompue, qu'elle soit à court de mémoire ... Dans ce cas recharger l''application remettra les choses d''aplomb.");
		App.setMsg("fr", "er_reloadr2", "Le serveur ne peut pas traiter des requêtes qui ont pu être faites depuis une application locale de version incompatible. Il n''y a que deux possibilités : a) recharger l''application maintenant, b) quitter l''application et elle sera rechargée à la prochaine sollicitation.");
		
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
	 * T : timeout -> 8 (phase 8 ou 6)
	 * I : interrupted -> 9
	 * L : erreur d'exécution d'un script local (avant ou après) -> 0 (phase 8 ou 9)
	 */

	open(request, err) {
		this.cref = "0";
		this.request = request;
		this.err = err;
		this.$.panel.open();

		const major = this.err.major();
		this.hiddenbtns = false;
		this.oplib = this.lib("op_" + this.err.op);
		this.isUpdate = this.oplib.startsWith("*");
		if (this.isUpdate) this.oplib = this.oplib.substring(1);
		this.optype = this.lib("er_op" + (this.isUpdate ? "w" : "r"));
		
		this.lines = this.err.detail.join("\n-----------------------\n");
		this.detaillabel = App.format("er_code", this.err.code);
		
		const ph = major == 9 ? 10 : this.err.phase;
		this.where = App.format("er_ph" + (this.err.isSrv() ? "s" : "l")) + App.format("er_ph" + ph);
		this.upddoner = this.lib("er_upddoner" + [1,1,1,2,2,2,3,0,0,2][ph]);

		this.resumer = this.lib("er_resumer" + (this.isUpdate ? "0" : "1"));

		this.reloadr = this.lib("er_reloadr" + (major == 4 ? "2" : (this.err.phase < 1 || this.err.phase > 4 ? "1" : "0")));

		this.retryr = this.lib("er_retryr" + major);
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
		case 4 : { this.info = this.lib("er_build"); this.more = err.message; this.topdet = ""; this.hiddenbtns = true;
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
