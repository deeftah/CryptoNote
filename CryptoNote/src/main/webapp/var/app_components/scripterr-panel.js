class ScripterrPanel extends Polymer.Element {
	static get is() { return "scripterr-panel"; }
  
	static get properties() { return {
    	  lang:{type:String, value:App.lang},
    	  err:{type:Object, value:{code:"LX"}},
    	  cref : {type:String, value:"0"}
      };
	}
	
	constructor() {
		super();
		App.setMsg("fr", "er_return", "Retour à l''application");
		App.setMsg("fr", "er_reload", "Recharger l'application");
		App.setMsg("fr", "er_quit", "Quitter l'application");
		
		App.setMsg("fr", "er_returnq", "Faut-il retourner à l''application ?");
		App.setMsg("fr", "er_reloadq", "Faut-il recharger l''application ?");

		App.setMsg("fr", "er_bugs1", "Bug sévère dans un script de l''application locale");		
		App.setMsg("fr", "er_bugs0", "Bug dans un script de l''application locale");
		
		App.setMsg("fr", "er_return1", "retour après erreur sévère");
		App.setMsg("fr", "er_return0", "retour après erreur");

		App.setMsg("fr", "er_reloadrs0", "recharger après erreur sévère");
		App.setMsg("fr", "er_reloadrs1", "recharger après erreur");
	}

	lib(code, lang) { 
		return App.lib(code);
	}
	
	 // info message name lines (depuis stack)

	open(err, severe) {
		this.cref = "0";
		this.err = err;
		const sv = severe ? 1 : 0;
		this.svcl = "info cl" + sv;
		
		this.info = this.lib("er_bugs" + sv);
		this.name = err.name;
		this.message = err.message;
		this.lines = err.stack;
		this.resumer = this.lib("er_return" + sv);
		this.reloadr = this.lib("er_reloadrs" + sv);		
		this.$.panel.open();
	}
	
	onReturnToApp() {
		this.close();
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
customElements.define(ScripterrPanel.is, ScripterrPanel);
