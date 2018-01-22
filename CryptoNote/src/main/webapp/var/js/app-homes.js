class AppHomes extends Polymer.Element {
	static get is() { return "app-homes"; }
	
	static get properties() { return { 
		anpage: {type:String, value:"z-wait"}, 
		bar: {type:String, value:"z-wait-bar"}, 
		lang:{type:String, value:App.lang},
		history: {type:Array, value:[]}
		};
	}
	
  	constructor() { super(); }
  	
  	ready() {
  		super.ready();
  		App.confirmBox = this.$.confirm;
  		App.messageBox = this.$.message;
  		App.globalSpinner = this.$.spin;
  		App.globalReqErr = this.$.reqerr;
  		App.scriptErrPanel = this.$.scripterr;
  		App.helpPanel = this.$.help;
  		App.appHomes = this;
  		if (App.rootcomplete)
  			this.starting();
  	}
  	
  	async starting() {
  		this.setTheme(App.theme);
		if (App.Custom && App.Custom.ready)
			await App.Custom.ready();
		this.setHome(App.home);
  	}
  	
  	async setHome(home) {
  		this.home = home;
  		let b = App.namespace + "-" + home + "-bar";
  		App.topBar = this.$[b];
  		if (!App.topBar) {
  	  		b = "z-" + home + "-bar";
  	  		App.topBar = this.$[b];
  		}
  		this.bar = b;
  		let h = App.namespace + "-" + home;
  		if (!this.$[h])
  			h = "z-" + home;
  		this.setPage(h, {});
  	}
  	
  	setLang(lang) { 
  		App.lang = lang;
  		this.setTheme(App.theme);
  	}
  	
  	setTheme(theme) {
  		App.theme = theme;
  		const sty = {};
  		let t = App.customThemes["z"];
  		if (t) for(let k in t) sty[k] = t[k];
  		t = App.customThemes[theme];
  		if (t) for(let k in t) sty[k] = t[k];
  		t = App.customThemes[App.lang];
  		if (t) for(let k in t) sty[k] = t[k];
  		Polymer.updateStyles(sty);
  		this.lang = App.lang;
  	}
  	
  	async mayHideCurrent() {
  		if (!this.history.length) return true;
  		const pq = this.$[this.history[this.history.length - 1].page];
  		if (!pq.mayHide) return true;
  		return await pq.mayHide();
  	}
  	
  	anim(t) {
  		if (t == 1) {
			this.enan = "slide-from-left-animation";
			this.exan = "slide-right-animation";
		} else if (t == 2) {
			this.enan = "slide-from-right-animation";
			this.exan = "slide-left-animation";		
  		} else {
			this.enan = "fade-in-animation";
			this.exan = "fade-out-animation";		  			
  		}

  	}
  	
  	async setPage(page, arg) {
  		const may = await this.mayHideCurrent();
  		if (!may) return;
  		this.history = [{page:page, arg:arg}];
  		if (App.topBar.setPreviousPage) App.topBar.setPreviousPage(null);
  		this.anim(0);
  		this.page = page;
  		this.anpage = this.page + "-page";
  		if (this.$[this.page].show)
  			this.$[this.page].show(arg, null);
  	}
  	
  	async forward(page, arg) {
  		const may = await this.mayHideCurrent();
  		if (!may) return;
  		const pb = this.history[this.history.length - 1].page;
  		if (App.topBar.setPreviousPage) App.topBar.setPreviousPage(this.$[pb]);
  		this.history.push({page:page, arg:arg});
  		this.anim(1);
  		this.page = page;
  		this.anpage = this.page + "-page";
  		if (this.$[this.page].show)
  			this.$[this.page].show(arg, pb);
  	}

  	async back() {
  		if (this.history.length < 2) return;
  		const may = await this.mayHideCurrent();
  		if (!may) return;
  		this.history.pop();
  		const pb = this.history.length >= 2 ? this.history[this.history.length - 2].page : null;
  		if (App.topBar.setPreviousPage) App.topBar.setPreviousPage(pb ? this.$[pb] : null);
  		const top = this.history[this.history.length - 1];
  		this.anim(2);
  		this.page = top.page;
  		this.anpage = this.page + "-page";
  		if (this.$[this.page].show)
  			this.$[this.page].show(top.arg, pb);
  	}

}
customElements.define(AppHomes.is, AppHomes);
