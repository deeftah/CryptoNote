class PhraseBox extends Polymer.Element {
	static get is() { return "phrase-box"; }
	static get properties() { return { 
		lang : {type:String, value:"xx"},
		avis : {type:String, value:""},
		er: {type:Boolean, value:true},
		enclair: {type:Boolean, value:true, notify:true, observer:"oncb"},
		typed: {type:String, value:"", notify:true, observer:"onTyped"},
		type: {type:String, value:"text"},
		target: { type: Object },
		}
	}

	lib(code, lang) { return App.lib(code);}

  	constructor() { 
  		super();
  		App.setMsg("fr", "pb_ok", "OK");
  		App.setMsg("fr", "pb_ko", "Renoncer");
  		App.setMsg("fr", "pb_ex", "9les8sanglots7longs6des5violons");
  		App.setMsg("fr", "pb_ec", "Afficher la phrase en clair");
  		App.setMsg("fr", "pb_av", "Au moins {0} mots composés de lettres de \"a\" à \"z\" précédés d''un chiffre de 1 à 9, au moins {1} lettres au plus 60.");
  		App.setMsg("fr", "pb_di", "{0} mot(s), {1} lettre(s)");  		
  	}
  	
  	clearInput() {
  		this.typed = "";
  	}
  	
  	ready() {
  		super.ready();
  		this.target = this.$.inp;
  		this.$.panel.addEventListener("iron-overlay-closed", (e) => { this.closing(e); });		
	}

  	oncb() { this.type = this.enclair ? "text" : "password"; }
  	
	async closing(e){
		if (e.detail.confirmed)
			this.onEnter()
		else {
			this.typed = "";
			this.resolve(null);
		}
	}
	
	async onEnter() { 
		if (!this.er) {
			let res = await this.crypt();
			this.typed = "";
			this.$.panel.close();
			this.resolve(res);
		}
	}
	
	async crypt() {
		let x = "";
		let r = "";
		let n = 0;
		for(let i = 0, m = null; m = this.mots[i]; i++) {
			let c = m.charCodeAt(0) - 49; // "1"
			for(let j = 1; j < m.length; j++){
				let l = m.charCodeAt(j) - 97; // "a"
				let z = String.fromCharCode(32 + (26 * c) + l);
				x += z;
				if (n != 2) {
					r += z;
					n++;
				} else
					n = 0;
			}
		}
		let s = App.Util.bcrypt(x);
		let u = new Uint8Array(32);
		for(let i = 0; i < 32; i++) u[i] = s.charCodeAt(i);
		u[31] = 0;
		let cleS = await App.AES.newAES(u);
		let prB = App.Util.bcrypt(r);
		let prBD = App.B64.encode(await Util.sha256(prB));
		return {cleS:cleS, prB:prB, prBD:prBD};
	}
	
	async show(msg, nbmots, nbletters){
		return new Promise((resolve, reject) => {
			this.text = msg;
			this.$.panel.open();
			this.resolve = resolve;
			this.nbmots = nbmots;
			this.nbletters = nbletters;
			this.avis = App.format("pb_av", "" + nbmots, "" + nbletters);
			this.typed = "";
			this.onTyped(this.typed);
		});
	}
  	
	onTyped(nv, ov) {
		let s1 = nv.length ? nv.charAt(0) : "";
		let y = "";
		let nbl = 0;
		let mots = [];
		let mot = "1";
		for(let i = 0; i < nv.length && nbl < 60; i++) {
			let c = nv.charAt(i);
			if (c >= "1" && c <= "9") {
				// c'est un chiffre
				if (mot.length != 1) {
					mots.push(mot);
					y += mots.length == 1 && mot.charAt(0) != s1 ? mot.substring(1) : mot;
				}
				mot = c;
			} else {
				if (c >= "a" && c <= "z") {
					mot += c;
					nbl++;
				}
			}
		}
		if (!mots.length) {
			if (mot.charAt(0) == s1) 
				y = mot;
			else if (mot.length > 1)
				y = mot.substring(1);
			else
				y = "";
		} else
			y += mot;
		
		if (mot.length != 1)
			mots.push(mot);
		
		this.mots = mots;
		this.nm = this.mots.length;
		this.nl = nbl;
		this.diag = App.format("pb_di", "" + this.nm, "" + this.nl);
		this.er = (this.nm < this.nbmots) || (this.nl < this.nbletters);
		this.diagcl = this.er ? "cpt err" : "cpt ok";
		if (nv != y)
			this.typed = y;
	}
	
}
customElements.define(PhraseBox.is, PhraseBox);
