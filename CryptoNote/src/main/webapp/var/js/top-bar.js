class TopBar extends Polymer.Element {
	static get is() { return "top-bar"; }
	static get properties() { return { 
		lang : {type:String, value:"xx"},
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
		this.mode = App.mode;
		this.modeMax = App.modeMax;
		this.sm = App.superman;
		this.inc = App.incognito;
	} 
	
	ready() {
		super.ready();
		this.resetSync(true);
	}
	
	backCl(pp) {
		return pp ? "back" : "backn";
	}
	
	isMode(mode, v){
		return mode == v;
	}
			
	futurMode(actuel, futur) {
		return actuel == futur || futur > this.modeMax;
	}
	
	reloadMode0() {
		
	}

	reloadMode1() {
		
	}

	reloadMode2() {
		
	}

	setPreviousPage(p) { 
		this.previousPage = p;
		this.libPreviousPage = p && p.title ? p.title() : "";
	}
	
	back() { App.appHomes.back(); }
	
	tbStyle(mode) {
		return "background-color:var(--tb-bg" + mode + ");";
	}
	
	showMenu() {
		
	}
	
	clickMode() {
		this.$.menuMode.open();
	}
	
	closeMenuMode() {
		this.$.menuMode.close();
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
