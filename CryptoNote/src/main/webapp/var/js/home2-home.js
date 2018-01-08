class Home2Home extends Polymer.Element {
	static get is() { return "home2-home"; }
      
	static get properties() {
		return {
			lang:{type:String, value:App.lang},
            owner: {type:String, value:'Daniel' },
        	build: {type:Number, value:App.build},
        	lkbower: {type:String, value:App.basevar + "bower.json"}
		};
	}
	
    constructor() {
        super();
        this.bonjour = App.bonjour ? App.bonjour : "Salut" ;
	}

	lib(code, lang) { return App.lib(code);}

	confirm2() {
		App.confirmBox.show("Ceci est un beau message.", "OK", "Bullshit")
		.then(() => {
			console.log("Confirm OK");
		}).catch(() => {
			console.log("Confirm KO");    		  
		})
	}
	
	confirm1() {
		App.confirmBox.show("Ceci est un TRES beau message.", "Lu")
		.then(() => {
			console.log("Confirm Lu");
		}).catch(() => {
			console.log("Confirm KO2");    		  
		})
	}
	
	changeThemeA() { App.appHomes.setTheme("a"); }
    changeThemeB() { App.appHomes.setTheme("b"); }
	changeLangFR() { App.appHomes.setLang("fr"); }
    changeLangEN() { App.appHomes.setLang("en"); }

    async bower() {
		const spin = this.$.spin;
		spin.start(this, "bower.json");
		setTimeout(() => {
			spin.progress("Traitement du serveur");
			setTimeout(() => {
				this.loadBower();
			}, 2000);    		  
		}, 2000);
	}
    
    async loadBower() {
		try {
			let r = await fetch(App.basevar + "bower.json");
			if (r.ok) {
				let t = await r.text();
				this.pingres = t.substring(0, 30);
			}
	    	this.$.spin.stop();
		} catch(e) {
	    	this.$.spin.stop();
	    	console.error(App.Util.error(e.message, 5000));
		}   	
    }
    
    kill() {
    	console.error(App.Util.error("Kill bower.json", 3000));
    }
    
	async ping(spin) {
		try {
			const r = await new Req().setOp("ping").setSpinner(spin).setTimeOut(6000).go();
			this.pingres = !r ? "json mal formé ?" : JSON.stringify(r);					
		} catch(err) {
			console.error(App.Util.error("PING KO - " + err.message, 3000));
		}
	}

	ping1() { this.ping(this.$.spin); }
	ping2() { this.ping(App.globalSpinner); }
	ping3() { this.ping(App.defaultSpinner); }

	checkSrvVersion() {
		App.Util.checkSrvVersion();
	}
	
	async crypto() {
		try{
			let x = await App.Util.sha256("toto");
			console.log(App.B64.encode(x, true));
			let y = App.Util.string2bytes("toto");
			x = await App.Util.sha256(y);
			console.log("SHA-256: " + App.B64.encode(x, true));
			
			x = App.Util.bcrypt("toto est beau");
			console.log("BCrypt: " + x + " / " + x.length);
			let aes1 = await App.AES.newAES(App.Util.bcrypt2u32(x));
			
			y = await aes1.encode("toto est beau");
			console.log("AES crypted: " + App.B64.encode(y, true)  + " / " + y.length);
			x = await aes1.decode(y);
			console.log("AES decrypted: " + App.Util.bytes2string(x));
			let z1 = JSON.stringify(App.zres);
			y = await aes1.encode(z1, true);
			console.log("AES crypted z: " + y.length + " / " + z1.length);
			x = await aes1.decode(y, true);
			let z2 = App.Util.bytes2string(x);
			console.log("AES decrypted z: " + App.Util.bytesEqual(z1, z2));
			
			let rsa = await App.RSA.newRSAGen();
			let pub = await App.RSA.newRSAPub(rsa.jwkpub);
			let priv = await App.RSA.newRSAPriv(rsa.jwkpriv);
			
			y = await pub.encode("toto est beau");
			console.log("RSA crypted: " + App.B64.encode(y, true)  + " / " + y.length);

			x = await priv.decode(y);
			console.log("RSA decrypted: " + App.Util.bytes2string(x));

			
		} catch(err) {
			console.log(err.message + "/n" + err.stack);
		}
	}
	
}
customElements.define(Home2Home.is, Home2Home);
