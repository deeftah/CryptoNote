class ProfilePanel extends Polymer.Element {
	static get is() { return "profile-panel"; }
  
	static get properties() { return {
    	  lang:{type:String, value:App.lang},
    	  isSudo : {type:Boolean, value:false},
    	  userName: {type:String, value:null}, 
    	  userPhoto: {type:String, value:null}, 
    	  typed: {type:String, value:"", notify:true, observer:"onTyped"},
    	  target: { type: Object },
      };
	}
	
	constructor() {
		super();
		App.setMsg("fr", "pf_titre", "Prénom Nom");		
  		App.setMsg("fr", "pf_auth", "S'authentifier comme administrateur");
  		App.setMsg("fr", "pf_authok", "Authentification reconnue");
  		App.setMsg("fr", "pf_authab", "Authentification abandonnée");
  		App.setMsg("fr", "pf_authko", "Authentification non reconnue");
  		App.setMsg("fr", "pf_authti", "Phrase d'habilitation à l'administration :");
	}
	
	ready() {
		super.ready();
  		this.target = this.$.inp;
	}

	lib(code, lang) { return App.lib(code); }
		
	show() {
		this.$.panel.open();
		this.$.photo.show();
	}
	
	close() { 
		this.$.panel.close(); 
	}
		
  	clearInput() {
  		this.typed = "";
  	}

	async onEnter() { 
		App.Custom.declareNom(this.typed);
	}
	
	onFileLoaded(e) {
		const f = e.detail;
		if (!f || !f.type.startsWith("image"))
			App.confirmBox(this.lib("pf_noph"), this.lib("lu"));
		else {
			this.$.photo.show(f.resized);
			App.Custom.declarePhoto(f.resized);
		}
	}
	
  	async getPhrase() {
  		let typed = await this.$.phrase.show(this.lib("pf_authti"), 4, 20);
  		if (typed) {
  			console.log("prB: " + typed.prB + "\nprBD : " + typed.prBD);
  			App.Custom.declareSudo(typed.prB);
  			try {
  				const ret = await new App.Req().setOp("sudo").setNoCatch("SADMINOP").go();
  				App.messageBox.show(App.lib("pf_authok"), 3000);
  				this.sudo = App.Custom.sudo;
  			} catch (err) {
				App.Custom.declareSudo(null);
				this.sudo = null;
  				if (err.code == "SADMINOP")
  					App.confirmBox.show(App.lib("pf_authko"), App.lib("lu"));
  			}
  		} else {
			App.Custom.declareSudo(null);
			this.sudo = null;
			App.messageBox.show(App.lib("pf_authab"), 3000, true);
  		}
  	}

}
customElements.define(ProfilePanel.is, ProfilePanel);
