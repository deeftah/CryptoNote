class HelpPanel extends Polymer.Element {
	static get is() { return "help-panel"; }
  
	static get properties() { return {
    	  lang:{type:String, value:App.lang},
    	  page : {type:Object, value:{refs:[]}}, 	// descriptif de la page courante
    	  hist : {type:Array, value:[]},
    	  dic : {type:Object}
      };
	}
	
	/* page : 
	 	String p;		// précédente dans la section (null si première)
		String t;		// page tête de la section (null pour home)
		String s;		// page suivante dans la section (null si dernière)
		String[] refs; 	// pages référencées
	 */
	
	constructor() {
		super();
		App.setMsg("fr", "help__back", "Page précédemment affichée");
		App.setMsg("fr", "help__home", "Accueil de l''aide");
		App.setMsg("fr", "help__previous", "Page précédente dans la section de cette aide");	
		App.setMsg("fr", "help__top", "Page tête de cette section d''aide");
		App.setMsg("fr", "help__next", "Page suivante dans la section de cette aide");
		App.setMsg("fr", "help__close", "Fermeture de l''aide");	
		App.setMsg("fr", "help__nop", "***Rédaction en cours ...***");	
		
	}
	
	ready() {
		super.ready();
		this.dic = App.helpDic;
	}

	lib(code, lang) { 
		return App.lib(code);
	}
	
	isDisabled(idx, disabled) { return disabled[idx]; }

	open(page) {
		this.hist = [];
		this.show(page);
		this.$.panel.open();
	}
	
	show(page) {
		let p = this.dic[page];
		if (p) {
			p.page = page;
			this.setTitle(p, "p");
			this.setTitle(p, "t");
			this.setTitle(p, "n");
			if (p.refs) {
				p.refsl = new Array(p.refs.length);
				for(let i = 0; i < p.refs.length; i++) {
					let x = p.refs[i];
					if (x)
						p.refsl[i] = {ref:x, lib:this.lib("help_" + x)};
				}	
			} else
				p.refs = [];
		} else
			p = {page:page, refs:[]};
		this.set("page", p);
		this.push("hist", p.page);
		this.getContent();
	}
		
	setTitle(p, x) {
		if (p[x])
			p[x + "l"] = this.lib("help_" + p.page);
	}
	
	dp(page) { return page.p == null; }
	dt(page) { return page.t == null; }
	dn(page) { return page.n == null; }
	db(histlen) { return histlen <= 1 ; }
	isHome(page) { return page.page == "home"; }
	
	back() {
		if (this.hist.length > 1) {
			this.pop("hist");
			const p = this.pop("hist");
			this.show(p);
		}
	}
		
	home() {
		this.show("home");
	}
	
	previous() {
		if (this.page.p) this.show(this.page.p);
	}
	
	top() {
		if (this.page.t) this.show(this.page.t);
	}

	next() {
		if (this.page.t) this.show(this.page.t);
	}

	showref(e){
		this.show(e.model.item.ref);
	}
	
	close() { 
		this.$.panel.close(); 
		this.hist = [];
		this.page = {refs:[]};
	}
	
	async getContent() {
		let text = await App.getZRes("help-" + this.page.page + ".md");
		this.$.content.innerHTML = Util.md2html(text ? text : this.lib("help__nop"));
	}
	
}
customElements.define(HelpPanel.is, HelpPanel);
