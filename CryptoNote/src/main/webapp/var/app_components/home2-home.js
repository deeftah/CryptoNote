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
  	  this.$.photo.show(App.superman);
    }
    
    title() {
    	return "La belle page Home2, La belle page Home2, La belle page Home2";
    }
    
    async mayHide() {
  	  return true;
    }
    
    back() {
  	  App.appHomes.back();
    }
    
    home1() {
  	  App.appHomes.forward("z-home1", {texte:"Bonjour home1"});
    }

    home1b() {
    	App.appHomes.setPage("z-home1", {texte:"Bonjour home1"});
	}

	confirm2() {
		App.confirmBox.show("Ceci est un beau message.", "OK", "Bullshit")
		.then(() => {
			console.log("Confirm OK");
		}).catch(() => {
			console.log("Confirm KO");    		  
		})
	}
	
	async confirm1() {
		if (await App.confirmBox.show("Ceci est un TRES beau message.", App.lib("lu")))
			console.log("Confirm Lu");
		else
			console.log("Confirm KO2");
	}
	
	changeThemeA() { App.appHomes.setTheme("a"); }
    changeThemeB() { App.appHomes.setTheme("b"); }
	changeLangFR() { App.appHomes.setLang("fr"); }
    changeLangEN() { App.appHomes.setLang("en"); }

	onFileLoaded(e){
		console.log(e.detail.url);
		console.log(e.detail.url.length);
		if (e.detail.resized)
			console.log(e.detail.resized.length);
	}

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

	async dbInfo() {
		try {
			const r = await new Req().setOp("dbInfo").setTimeOut(6000).go();
			this.pingres = !r ? "json mal formé ?" : JSON.stringify(r);					
		} catch(err) {
			console.error(App.Util.error("DbInfo KO - " + err.message, 3000));
		}
	}
		
	async crypto() {
		try{
			const ivHex = App.Util.uint8ToHex(App.AES.iv());
			console.log("ivHex : " + ivHex);
			
			let x = await App.Util.sha256("toto");
			console.log(App.B64.encode(x, true));
			let y = App.Util.string2bytes("toto");
			x = await App.Util.sha256(y);
			console.log("SHA-256: " + App.B64.encode(x, true));
			
			x = App.Util.bcrypt("toto est beau");
			console.log("BCrypt: " + x + " / " + x.length);
			const aeskeyBin = App.Util.bcrypt2u32(x);
			const aeskeyHex = App.Util.uint8ToHex(aeskeyBin);
			console.log("aeskeyHex : " + aeskeyHex);
			let aeskey = App.B64.encode(aeskeyBin);
			
			console.log("AESKEY: " + aeskey);
			let aes1 = await App.AES.newAES(App.Util.bcrypt2u32(x));
			console.log("B: " + App.B64.encode(aes1.uint8));
			
			y = await aes1.encrypt("toto est beau");
			console.log("AES crypted: " + App.B64.encode(y, true)  + " / " + y.length);
			
			let aes2 = await App.AES.newAES(B64.decode(aeskey));
			y = await aes2.encrypt("toto est beau");
			console.log("AES crypted: " + App.B64.encode(y, true)  + " / " + y.length);
			
			x = await aes1.decrypt(y);
			console.log("AES decrypted: " + App.Util.bytes2string(x));
						
			let z1 = JSON.stringify(App.zres);
			y = await aes1.encrypt(z1, true);
			console.log("AES crypted z: " + y.length + " / " + z1.length);
			x = await aes1.decrypt(y, true);
			let z2 = App.Util.bytes2string(x);
			console.log("AES decrypted z: " + App.Util.bytesEqual(z1, z2));
			
			let kp = await App.RSA.newEDKeyPair();
			console.log(kp.pub);
			console.log(kp.priv);
			let pub = await App.RSA.encrypter(kp.pub);
			let priv = await App.RSA.decrypter(kp.priv);

			y = await pub.encrypt("toto est beau");
			console.log("RSA crypted: " + App.B64.encode(y)  + " / " + y.length);

			x = await priv.decrypt(y);
			console.log("RSA decrypted: " + App.Util.bytes2string(x));

			let pri;
			let cryp;
			let pu;
			let r = await fetch(App.basevar + "z/z/private.pem");
			if (r.ok) 
				pri = await r.text();
			r = await fetch(App.basevar + "z/z/public.pem");
			if (r.ok) 
				pu = await r.text();
			r = await fetch(App.basevar + "z/z/crypted.txt");
			if (r.ok) 
				cryp = await r.text();

			let pub2 = await App.RSA.encrypter(pu);
			let priv2 = await App.RSA.decrypter(pri);
			
			x = await priv2.decrypt(cryp);
			let s = App.Util.bytes2string(x);
			console.log("RSA decrypted2: " + s);

			x = await pub2.encrypt(s);
			console.log("RSA crypted2: " + App.B64.encode(x));
			
			kp = await App.RSA.newSVKeyPair();
			console.log(kp.pub);
			console.log(kp.priv);
			pub = await App.RSA.verifier(kp.pub);
			priv = await App.RSA.signer(kp.priv);

			y = await priv.sign("toto est beau");
			console.log("RSA sign: " + App.B64.encode(y)  + " / " + y.length);

			x = await pub.verify(y, "toto est beau");
			console.log("RSA verify: " + x);

			x = await pub.verify(y, "titi est beau");
			console.log("RSA verify: " + x);
		} catch(err) {
			App.scriptErr(err);
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
