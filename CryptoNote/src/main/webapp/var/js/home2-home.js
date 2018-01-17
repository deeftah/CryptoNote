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
        this.errTest = new TypeError("(script) erreur de test");
	}

	lib(code, lang) { return App.lib(code);}

    show(arg, previousPage) {
  	  this.arg = arg;
  	  this.previousPage = previousPage;
    }
    
    title() {
    	return "La belle page Home2";
    }
    
    async mayHide() {
  	  return true;
    }
    
    back() {
  	  App.appHomes.back();
    }
    
    home1() {
  	  App.appHomes.forward("test-home1", {texte:"Bonjour home1"});
    }

    home1b() {
    	App.appHomes.setPage("test-home1", {texte:"Bonjour home1"});
	}

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
	
	// 	constructor(op, code, phase, message, detail) {
	erA() {
		App.globalReqErr.open(this, new App.ReqErr("opr", "A_T", 0, "message de A", [this.errTest.message, this.errTest.stack]));
	}
	erB() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "B_T", 1, "message de B", [this.errTest.message, this.errTest.stack]));
	}
	erX() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "X_T", 2, "message de X", [this.errTest.message, this.errTest.stack]));
	}
	erD() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "DBUILD", 0, "message de D", [this.errTest.message, this.errTest.stack]));
	}
	erC() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "CONT", 2, "message de C", [this.errTest.message, this.errTest.stack]));
	}
	erO() {
		App.globalReqErr.open(this, new App.ReqErr("opr", "OFF", 0, "message de off", [this.errTest.message, this.errTest.stack]));
	}
	erS() {
		App.globalReqErr.open(this, new App.ReqErr("opr", "SAUTH", 1, "pas autorisé", [this.errTest.message, this.errTest.stack]));
	}
	erT() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "TIMEOUT", 3, "timeout 32s", [this.errTest.message, this.errTest.stack]));
	}
	erI() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "INTERRUPT", 9, "interruption par clic", [this.errTest.message, this.errTest.stack]));
	}
	erL() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "LREC", 6, "parse json", [this.errTest.message, this.errTest.stack]));
	}
	erS0() {
		App.scriptErr(this.errTest);
	}
	erS1() {
		App.scriptErr(this.errTest, true);
	}
}
customElements.define(Home2Home.is, Home2Home);
