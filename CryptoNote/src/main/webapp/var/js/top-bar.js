class TopBar extends Polymer.Element {
	static get is() { return "top-bar"; }
	static get properties() { return { 
		lang : {type:String, value:App.lang},
		theme : {type:String, value:App.theme},
		themes : {type:Array, value:[]},
		isSudo : {type:Boolean, value:false},
		previousPage : {type:Object, value:null},
		libPreviousPage : {type:String, value:""},
		mode : {type:Number, value:0},
		modeMax : {type:Number, value:0},
		lapse : {type:String, value:"0s"}
		}
	}

	lib(code, lang) { return App.lib(code);}

	constructor() {
		super();
		App.setMsg("fr", "tb_menu", "Menu");
		App.setMsg("fr", "tb_resync", "Synchroniser l'état du cloud en local");
		App.setMsg("fr", "tb_ac0", "Mode actuel : incognito");
		App.setMsg("fr", "tb_ac1", "Mode actuel : synchronisé");
		App.setMsg("fr", "tb_ac2", "Mode actuel : avion");
		App.setMsg("fr", "tb_futur0", "Recharger l'application en mode incognito");
		App.setMsg("fr", "tb_futur1", "Recharger l'application en mode synchronisé");
		App.setMsg("fr", "tb_futur2", "Recharger l'application en mode avion");
		App.setMsg("fr", "tb_build", "Build : {0}");
		App.setMsg("fr", "tb_themes", "Thèmes : ");
		App.setMsg("fr", "tb_aidegen", "Accueil Aide");
		App.setMsg("fr", "tb_reload", "Recharger l''aplication");
		App.setMsg("fr", "tb_quit", "Quitter l''application");
		this.mode = App.mode;
		this.modeMax = App.modeMax;
		this.sm = App.superman;
		this.inc = App.incognito;
		this.libBuild = App.format("tb_build", App.build);
		this.langs = App.langs;
		this.themes = [];
		for(let i = 0, t = ""; t = App.themes[i]; i++)
			if (t.length == 1 && t != "z") this.themes.push(t);
	} 
	
	ready() {
		super.ready();
		this.resetSync(true);
	}
	
	setPreviousPage(p) { 
		this.previousPage = p;
		this.libPreviousPage = p && p.title ? p.title() : "";
	}

	clBtn(mode) { return "ic" + mode; }

	clSep(mode) { return "sep" + mode; }

	clSync(mode) { return "sync sync" + mode; }

	clBack(mode,pp) { return pp ? "back" + mode : "backn"; }
	
	clTop(mode) { return "top" + mode + " layout vertical flex-start"; }
	
	clLang(lang) { return "lang" + (lang == this.lang ? "" : "p") + " layout vertical center"; }

	clTheme(theme) { return "theme" + (theme == this.theme ? "" : "p"); }

	lblTheme(theme) { return this.lib("theme_" + theme);}
	
	flag(lang) { return "z/z/flag-" + lang + ".png"; }
	
	changeLang(e) { 
		let lg = e.model.item;
		if (lg != this.lang)
			App.appHomes.setLang(lg);
		this.closeMenuGen();
	}

	changeTheme(e) { 
		let th = e.model.item;
		if (th != this.theme)
			App.appHomes.setTheme(th);
		this.closeMenuGen();
	}

	isMode(mode, v){ return mode == v; }
			
	futurMode(actuel, futur) { return actuel == futur || futur > this.modeMax; }
	
	showMenuGen() { this.$.menuGen.open();	}

	closeMenuGen() { this.$.menuGen.close(); }

	showMenuMode() { this.$.menuMode.open(); }
	
	closeMenuMode() { this.$.menuMode.close(); }

	goHelp() { App.help("home"); }
	
	reloadMode0() {
		
	}

	reloadMode1() {
		
	}

	reloadMode2() {
		
	}
	
	back() { App.appHomes.back(); }
	
	quitApp() {
		
	}
	
	reloadApp() {
		
	}
	
	tapSync() {
		this.resetSync();
	}
	
	resetSync(b) {
		if (this.timer) {
			clearTimeout(this.timer);
			this.timer = null;
		}
		let age = "0s";
		const t = new Date().getTime();
		if (!b){
			this.lastsync = t;
		} else {
			if (!this.lastsync)
				this.lastsync = t;
			else {
				let s = Math.floor((t - this.lastsync) / 1000);
				if (s < 60){
					age = "" + s + "s";
				} else {
					const m = Math.floor(s / 60);
					s = Math.floor(s % 60);
					age = "" + m + (s < 10 ? "m0" : "m") + s ;
				}
			}
		}
		this.lapse = age;
		this.timer = setTimeout(() => {this.resetSync(true);}, 5000);
	}

}
customElements.define(TopBar.is, TopBar);
