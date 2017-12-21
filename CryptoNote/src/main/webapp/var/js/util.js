/*****************************************************/
export class StringBuffer {
	constructor() { this.buffer = []; }
	append(string) { this.buffer.push(string); return this; }
	toString() { return this.buffer.join(""); }
	join(arg) { return this.buffer.join(arg); }
	isEmpty() { return this.buffer.length != 0; }
	clear() { this.buffer = []; }
}

/*****************************************************/
export class Err {
	static err(e, name, phase, message, detail){
		if (e && e instanceof Err) return e;
		if (!e) return new Err(name, phase, message, detail);
		const code = e.code ? e.ced : "?";
		const msg = e.message ? e.message : "?";
		if (!message) message = "";
		message += App.format("exc", code, msg);
		return new Err(name ? name : "script", phase ? phase : -1, message, detail, e.stack);
	}
	
	constructor(name, phase, message, detail, stack) {
		this.name = name ? "script" : name;
		this.phase = phase ? phase : -1;
		this.message = message ? message : this.name;
		this.detail = detail ? detail : [];
		this.stack = stack;
	}
	
	/*
	 * N : notfound -> 1 : 404 : not found. Document ou attachment non trouvé
	 * A : app -> 1 : 400 : erreur applicative autre que N. Données incorrectes
	 * B : bug -> 2 : 400 : bug, situation des données illogique
	 * X : unexpected -> 3 : 400 : incident inattendu, matériel ou logiciel
	 * D : delayed build -> 4 : 400 : recharger l'application cliente
	 * C : contention -> 5 : 400 : trop de contention sur la base de données
	 * O : espace off -> 6 : 400 : espace en maintenance
	 * S : session non autorisée -> 7 : 400 : ou résiliée ou trop longue ...
	 * s : erreur d'exécution d'un script
	 */
	major() {
		const m = this.code.charAt(0);
		return "NABXDCOS".indexOf(m) != -1 && this.phase >= 0 ? m : "s"
	}
		
	log(onPanel) { 
		const m = this.name + this.message ? " -" + this.message : "";
		if (onPanel && App.TracePanel) App.TracePanel.trace(m); 
		return new Date().format("Y-m-d H:i:s.S") + " - " + m + (this.stack ? "\n" + this.stack : "");
	}
	
}

/*****************************************************/
/*
 * Le Tracker est un objet qui :
 * - suit l'exécution de la requête et est notifié de ses départ, changements d'état, sortie en en succès / erreur.
 * - pendant que la requête tourne, peut l'interrompre par kill().
 * - quand la requête sort en succès : invoque le resolve mémorisé dans la requête pour que son demandeur obtienne le résultat.
 * - quand la requête est sortie en erreur, peut,
 * a) soit retourner l'erreur au demandeur initial en faisant invoquer son reject().
 * b) soit demander un retry().
 * Methodes d'un Tracker : onStart(req) onProgress(req) onSuccess(req, resp) onError(req, err)
 * Méthodes de Req qu'un Tracker peut appeler: kill() (ce qui en génral lui provoquera un appel onError)
 */
export class Req {
	constructor(isGet) {
		this.isGet = isGet ? true : false;
		this.TIME_OUT_MS = 300000;
		this.url = new StringBuffer().append(App.baseUrl(0));
		this.currentRetry = null;
		this.hasArgs = false;
		if (!this.isGet) this.formData = new FormData();
		this.cred = 0;
	}
	
	setTracker(tracker) { 
		if (tracker && tracker.onStart && tracker.onError && tracker.onSuccess && tracker.onProgress)
			this.tracker = tracker; 
		return this; 
	}
	
	// GET seulement (si ressource, pas op)
	setUrl(url) { this.url.append(url); return this; } // relative a /cp/ns

	// POST seulement
	setFormData() { this.formData = formData; return this; } // premier à citer
	setSyncs(syncs) { if (syncs) this.formData.append("syncs", JSON.stringify(syncs)); return this;}

	// GET et POST
	setTimeOut(timeOut) { this.TIME_OUT_MS = timeOut; return this; }
	setOp(op, param, url) { 
		this.op = op; 
		this.param = JSON.stringify(param); 
		this.url.append("op/").append(url ? url : "");
		if (this.isGet)
			this.url.append(this.hasArgs ? "?op=" : "&op=").append(this.op);
		else
			this.formData.append("op", this.op);
		this.hasArgs = true;
		return this;
	}
	setOp(op, param, url) { 
		this.op = op; 
		this.param = JSON.stringify(param); 
		this.url.append("od/").append(url ? url : "");
		if (this.isGet)
			this.url.append(this.hasArgs ? "?op=" : "&op=").append(this.op);
		else
			this.formData.append("op", this.op);
		this.hasArgs = true;
		return this;
	}
	setArgs(args) {
		if (!args) return;
		for(let a in args) {
			let v = args[a];
			if (this.isGet)
				this.url.append(this.hasArgs ? "?" : "&").append(a + "=").append(encodeURI(v));
			else
				this.formData.append(a,v);
			this.hasArgs = true;
		}
		return this;
	}
	
	/*
	 * !cred : pas de crédential
	 * cred = 1 crédential standard simple (propriétés account / key de App)
	 * cred = 2 crédential privilégié (account / key / sudo de App)
	 * cred = {c1:... c2:... } crédential spécifique
	 */
	setCred(cred) { 
		if (cred) {
			if (cred == 1)
				this.cred = {account:App.account, key:App.key}
			else if (cred == 2)
				this.cred = {account:App.account, key:App.key, sudo:App.sudo}
			else
				this.cred = cred;
		}
		return this;
	}
	
	setNoCatch(noCatch) { this.noCatch = noCatch; return this; }
	
	go(){
		return new Promise((resolve, reject) => {
			if (this.cred) {
				for(let a in cred) {
					let v = cred[a];
					if (this.isGet)
						this.url.append(this.hasArgs ? "?" : "&").append(a + "=").append(encodeURI(v));
					else
						this.formData.append(a,v);
					this.hasArgs = true;
				}
			}
			this.resolve = resolve;
			this.reject = reject;
			this.currentRetry = new Retry();
			this.currentRetry.req = this;
			if (this.tracker) 
				this.tracker.onStart(this);
			this.currentRetry.send().then(resp => {
				this.currentRetry.req = null;
				this.currentRetry= null;
				if (this.tracker == null)
					this.resolve(resp);
				else
					this.tracker.onSuccess(this, resp);
			}).catch(err => {
				this.currentRetry.req = null;
				this.currentRetry= null;
				if (this.tracker == null)
					this.reject(err);
				else
					this.tracker.onError(this, err);
			});
		});
	}
		
	kill() {
		if (this.currentRetry)
			this.currentRetry.kill();
	}
}

class Retry {
	kill() {
		if (this.done) return;
		this.killed = true;
		if (this.xhr) {
			this.xhr.abort();
			this.xhr.onreadystatechange();
		}
	}
	
	send() {
		const tracker = this.req.tracker;
		const url = this.req.url.toString();
		return Promise.race([
			new Promise((resolve, reject) => {
				this.tim = setTimeout(() => {
						reject(Err.err(null, "httpget", -1, App.format("httpgetto", url, Math.round(this.req.TIME_OUT_MS / 1000)))); 
					},	
					this.req.TIME_OUT_MS); 
			}),
			
			new Promise((resolve, reject) => {
				try {
					this.xhr = new XMLHttpRequest();
					this.xhr.open(this.req.isGet ? "GET" : "POST", url, true);
					this.xhr.responseType = "arraybuffer";
					this.xhr.onerror = (e) => {	
						if (this.tim) clearTimeout(this.tim);
						if (this.done) return;
						this.done = true;
						const er = this.killed ? Err.err(e, "interrupted", -1, App.format("interrupted", url))
								: Err.err(e, "httpget", -1, App.format("httpget", url)); 
						console.error(er.log()); 
						reject(er);
					}
					this.xhr.onerror = (e) => {	
						if (this.done || !tracker) return;
						tracker.onProgress(this.req, e.loaded, e.total);
					}
					this.xhr.onreadystatechange = () => {
						if (this.done) return;
						if (this.xhr.readyState != 4) return;
						if (this.tim) clearTimeout(this.tim);
						this.done = true;
						
						if (this.killed) {
							const er = Err.err(e, "interrupted", -1, App.format("interrupted", url)); 
							console.error(er.log()); 
							reject(er);
						};

						const ct = this.xhr.getResponseHeader("Content-Type");
						let contentType = ct;
						let charset = null;
						let i = ct ? ct.indexOf(";") : -1;
						if (i != -1) {
							contentType = ct.substring(0, i);
							i = ct.indexOf("charset=", i);
							if (i != -1)
								charset = ct.substring(i + 8);
						}
						const isJson = contentType && contentType == "application/json" ;
						const uint8 = this.xhr.response ? new Uint8Array(this.xhr.response) : null;
						let jsonObj = null;
						if (isJson) {
							try {
								const text = uint8 ? Util.toUtf8(uint8) : "{}";
								jsonObj = JSON.parse(text);
							} catch (e) {
								const er = Err.err(e, "jsonparseurl", -1, App.format("jsonparseurl", url)); 
								console.error(er.log()); 
								reject(er);
							}
						}
		
						if (this.xhr.status == 200) {					    
							resolve(isJson ? {json:jsonObj} : {uint8:uint8, charset:charset, contentType:contentType});
							return;
						} 
						
						const er = jsonObj ? error = new Err(jsonObj.name, jsonObj.phase, jsonObj.message, jsonObj.detail)
							: new Err("httpget", -1, App.format("httpget2", this.urlx, this.xhr.status, this.xhr.statusText));
						console.error(er.log());
						reject(er);
					}
					if (this.req.isGet)
						this.xhr.send();
					else
						this.xhr.send(this.req.formData);
				} catch(e) {
					if (this.tim) clearTimeout(this.tim);
					const er =  new APP_Error.err(e, "httpget", -1, APP.format("httpget", url)); 
					console.error(er.log()); 
					reject(er);
				}
		})
	  ]);
	}
}
App.Req = Req;
/*****************************************************/
export class Util {	
	static register() {
		// Pour Apple et Edge 
		if (!navigator.serviceWorker && APP.isSW) window.location = window.location.pathname + ".a" + window.location.search + window.location.hash;
		//Pour Safari / IOS !!!
		if (window.crypto && !window.crypto.subtle && window.crypto.webkitSubtle) window.crypto.subtle = window.crypto.webkitSubtle;
		// window.Polymer = { dom:'shadow'};
		if (App.mode){
			if (App.isSW)
				this.regSW(); 
			else 
				this.updCache();
		}
		if (this.mode != 2)
			this.checkSrvVersion();
	}

	static toUtf8(bytes) { return bytes ? this.decoder.decode(bytes) : ""; }

	static reload(b) {
		setTimeout(function() {
			const x = {lang:App.lang, build:b, ns:App.namespace, nslabel:App.nslabel, b:App.buildAtPageGeneration, home:App.homeUrl()}
			window.location = App.reloadUrl() + "reload.html?" + encodeURI(JSON.stringify(x));
		}, 3000);		
	}

	/*
	 * Avis de fin de rechargement de l'application cache : impose le rechargement de l'application
	 */
	static updCache(){
		window.applicationCache.addEventListener('updateready', function(e) {
		    if (window.applicationCache.status == window.applicationCache.UPDATEREADY) {
		    	location.reload(true);
		    }
		}, false);
	}

	// ping du namespace ou du serveur (notns est true)
	static ping(notns) {
		return new Promise((resolve, reject) => {
			new Req(true).setUrl(notns ? "../ping" : "ping").go()
			.then(r  => {
				App.offline = false;
				resolve(r);
			}).catch(e => {
				App.offline = true;
				resolve(null);
			});	
		});
	}
		
	static checkSrvVersion(){
		// alert("App.buildAtPageGeneration:" + App.buildAtPageGeneration + " App.build:" + App.build);
		if (App.buildAtPageGeneration != App.build)
			this.reload(App.build);
		this.ping()
		.then(r  => {
			if (r && r.json.b != App.build) {
				// alert("App.buildAtPageGeneration:" + App.buildAtPageGeneration + " App.build:" + App.build + " ping.b:" + r.json.b);
				this.reload(r.json.b);
			}
		});
	}
	
	/*
	 * Le service worker est une application INDEPENDANTE de la page.
	 * Le script sw.js qui l'anime est déclaré : ça lancera le service worker S'IL N'ETAIT PAS DEJA LANCE.
	 * S'il était lancé, il continue de vivre avec la version actuelle (donc d'une version potentiellement retardée).
	 * Si le sw.js a changé par rapport à celui en exécution, un noveau service est préparé et
	 * RESTE EN ATTENTE jusqu'à la fin de toutes les pages qui ont déclaré ce service.
	 * IL Y A UN SERVICE WORKER PAR "namespace" : /cp/nsB/sw.js et /cp/nsA/sw.js définissent DEUX services indépendants.
	 * Le "scope" de /cp/nsA/sw.js n'est PAS réduit : il interceptent TOUTES les URLs /cp/nsA/...
	 * MAIS en conséquence IGNORE les URLs de la navigation privée /cp/nsA$/...
	 * Une EXCEPTION "Request failed" signifie qu'une des URLs citée dans addAll() N'EST PAS ACCESSIBLE (404)
	 * MAIS one sait PAS laquelle (joie !)
	 */
	static regSW() {
		const js = App.ctxNsSlash() + "sw.js";
		navigator.serviceWorker.register(js)
		.then(reg => { 
			console.log(App.format("regok", js, reg.scope));
		}).catch(e => {
			const er = Err.err(e, "regko", js); console.error(er.log() + "\n" + e.stack); 
		});
	}

//	// ça eu servi un jour !!!	
//	static sendMessageToCache(message) {
//		return new Promise(function(resolve, reject) {
//			var messageChannel = new MessageChannel();
//			messageChannel.port1.onmessage = function(event) {
//				if (event.data.error) {
//					reject(event.data.error);
//				} else {
//					resolve(event.data);
//				}
//			};
//			const swc = navigator.serviceWorker.controller;
//			if (swc)
//				swc.postMessage(message, [messageChannel.port2]);
//		});
//	}
//
//	static getCacheVersion(){
//		return new Promise((resolve, reject) => {
//			if (navigator.serviceWorker && navigator.serviceWorker.controller) { // activated : répond aux messages
//				this.sendMessageToCache({command:"getBuild"})
//				.then(resp => { // Build: resp.build
//					console.log(APP.format("cachebuildok", 1, resp.build));
//					resolve(resp.build);
//				}).catch(e => { 
//					setTimeout(function() {
//						APP_Util.sendMessageToCache({command:"getBuild"})
//						.then(resp => {
//							console.log(APP.format("cachebuildok", 2, resp.build));
//							resolve(resp.build);
//						}).catch(e => {
//							const er = APP_Error.err(e, "cachebuildko1", -1, APP.format("cachebuildko1")); console.error(er.log()); reject(er);
//						});
//					}, 5000);
//				});
//			} else {
//				const er = new APP_Error("cachebuildko2", -1, APP.format("cachebuildko2")); console.error(er.log()); reject(er);
//			};
//		});
//	}

	static setup() {
		this.decoder = new TextDecoder("utf-8");

		this.defaultDRM = [
	    {'base':'A', 'letters':/[\u0041\u24B6\uFF21\u00C0\u00C1\u00C2\u1EA6\u1EA4\u1EAA\u1EA8\u00C3\u0100\u0102\u1EB0\u1EAE\u1EB4\u1EB2\u0226\u01E0\u00C4\u01DE\u1EA2\u00C5\u01FA\u01CD\u0200\u0202\u1EA0\u1EAC\u1EB6\u1E00\u0104\u023A\u2C6F]/g},
	    {'base':'AA','letters':/[\uA732]/g},
	    {'base':'AE','letters':/[\u00C6\u01FC\u01E2]/g},
	    {'base':'AO','letters':/[\uA734]/g},
	    {'base':'AU','letters':/[\uA736]/g},
	    {'base':'AV','letters':/[\uA738\uA73A]/g},
	    {'base':'AY','letters':/[\uA73C]/g},
	    {'base':'B', 'letters':/[\u0042\u24B7\uFF22\u1E02\u1E04\u1E06\u0243\u0182\u0181]/g},
	    {'base':'C', 'letters':/[\u0043\u24B8\uFF23\u0106\u0108\u010A\u010C\u00C7\u1E08\u0187\u023B\uA73E]/g},
	    {'base':'D', 'letters':/[\u0044\u24B9\uFF24\u1E0A\u010E\u1E0C\u1E10\u1E12\u1E0E\u0110\u018B\u018A\u0189\uA779]/g},
	    {'base':'DZ','letters':/[\u01F1\u01C4]/g},
	    {'base':'Dz','letters':/[\u01F2\u01C5]/g},
	    {'base':'E', 'letters':/[\u0045\u24BA\uFF25\u00C8\u00C9\u00CA\u1EC0\u1EBE\u1EC4\u1EC2\u1EBC\u0112\u1E14\u1E16\u0114\u0116\u00CB\u1EBA\u011A\u0204\u0206\u1EB8\u1EC6\u0228\u1E1C\u0118\u1E18\u1E1A\u0190\u018E]/g},
	    {'base':'F', 'letters':/[\u0046\u24BB\uFF26\u1E1E\u0191\uA77B]/g},
	    {'base':'G', 'letters':/[\u0047\u24BC\uFF27\u01F4\u011C\u1E20\u011E\u0120\u01E6\u0122\u01E4\u0193\uA7A0\uA77D\uA77E]/g},
	    {'base':'H', 'letters':/[\u0048\u24BD\uFF28\u0124\u1E22\u1E26\u021E\u1E24\u1E28\u1E2A\u0126\u2C67\u2C75\uA78D]/g},
	    {'base':'I', 'letters':/[\u0049\u24BE\uFF29\u00CC\u00CD\u00CE\u0128\u012A\u012C\u0130\u00CF\u1E2E\u1EC8\u01CF\u0208\u020A\u1ECA\u012E\u1E2C\u0197]/g},
	    {'base':'J', 'letters':/[\u004A\u24BF\uFF2A\u0134\u0248]/g},
	    {'base':'K', 'letters':/[\u004B\u24C0\uFF2B\u1E30\u01E8\u1E32\u0136\u1E34\u0198\u2C69\uA740\uA742\uA744\uA7A2]/g},
	    {'base':'L', 'letters':/[\u004C\u24C1\uFF2C\u013F\u0139\u013D\u1E36\u1E38\u013B\u1E3C\u1E3A\u0141\u023D\u2C62\u2C60\uA748\uA746\uA780]/g},
	    {'base':'LJ','letters':/[\u01C7]/g},
	    {'base':'Lj','letters':/[\u01C8]/g},
	    {'base':'M', 'letters':/[\u004D\u24C2\uFF2D\u1E3E\u1E40\u1E42\u2C6E\u019C]/g},
	    {'base':'N', 'letters':/[\u004E\u24C3\uFF2E\u01F8\u0143\u00D1\u1E44\u0147\u1E46\u0145\u1E4A\u1E48\u0220\u019D\uA790\uA7A4]/g},
	    {'base':'NJ','letters':/[\u01CA]/g},
	    {'base':'Nj','letters':/[\u01CB]/g},
	    {'base':'O', 'letters':/[\u004F\u24C4\uFF2F\u00D2\u00D3\u00D4\u1ED2\u1ED0\u1ED6\u1ED4\u00D5\u1E4C\u022C\u1E4E\u014C\u1E50\u1E52\u014E\u022E\u0230\u00D6\u022A\u1ECE\u0150\u01D1\u020C\u020E\u01A0\u1EDC\u1EDA\u1EE0\u1EDE\u1EE2\u1ECC\u1ED8\u01EA\u01EC\u00D8\u01FE\u0186\u019F\uA74A\uA74C]/g},
	    {'base':'OI','letters':/[\u01A2]/g},
	    {'base':'OO','letters':/[\uA74E]/g},
	    {'base':'OU','letters':/[\u0222]/g},
	    {'base':'P', 'letters':/[\u0050\u24C5\uFF30\u1E54\u1E56\u01A4\u2C63\uA750\uA752\uA754]/g},
	    {'base':'Q', 'letters':/[\u0051\u24C6\uFF31\uA756\uA758\u024A]/g},
	    {'base':'R', 'letters':/[\u0052\u24C7\uFF32\u0154\u1E58\u0158\u0210\u0212\u1E5A\u1E5C\u0156\u1E5E\u024C\u2C64\uA75A\uA7A6\uA782]/g},
	    {'base':'S', 'letters':/[\u0053\u24C8\uFF33\u1E9E\u015A\u1E64\u015C\u1E60\u0160\u1E66\u1E62\u1E68\u0218\u015E\u2C7E\uA7A8\uA784]/g},
	    {'base':'T', 'letters':/[\u0054\u24C9\uFF34\u1E6A\u0164\u1E6C\u021A\u0162\u1E70\u1E6E\u0166\u01AC\u01AE\u023E\uA786]/g},
	    {'base':'TZ','letters':/[\uA728]/g},
	    {'base':'U', 'letters':/[\u0055\u24CA\uFF35\u00D9\u00DA\u00DB\u0168\u1E78\u016A\u1E7A\u016C\u00DC\u01DB\u01D7\u01D5\u01D9\u1EE6\u016E\u0170\u01D3\u0214\u0216\u01AF\u1EEA\u1EE8\u1EEE\u1EEC\u1EF0\u1EE4\u1E72\u0172\u1E76\u1E74\u0244]/g},
	    {'base':'V', 'letters':/[\u0056\u24CB\uFF36\u1E7C\u1E7E\u01B2\uA75E\u0245]/g},
	    {'base':'VY','letters':/[\uA760]/g},
	    {'base':'W', 'letters':/[\u0057\u24CC\uFF37\u1E80\u1E82\u0174\u1E86\u1E84\u1E88\u2C72]/g},
	    {'base':'X', 'letters':/[\u0058\u24CD\uFF38\u1E8A\u1E8C]/g},
	    {'base':'Y', 'letters':/[\u0059\u24CE\uFF39\u1EF2\u00DD\u0176\u1EF8\u0232\u1E8E\u0178\u1EF6\u1EF4\u01B3\u024E\u1EFE]/g},
	    {'base':'Z', 'letters':/[\u005A\u24CF\uFF3A\u0179\u1E90\u017B\u017D\u1E92\u1E94\u01B5\u0224\u2C7F\u2C6B\uA762]/g},
	    {'base':'a', 'letters':/[\u0061\u24D0\uFF41\u1E9A\u00E0\u00E1\u00E2\u1EA7\u1EA5\u1EAB\u1EA9\u00E3\u0101\u0103\u1EB1\u1EAF\u1EB5\u1EB3\u0227\u01E1\u00E4\u01DF\u1EA3\u00E5\u01FB\u01CE\u0201\u0203\u1EA1\u1EAD\u1EB7\u1E01\u0105\u2C65\u0250]/g},
	    {'base':'aa','letters':/[\uA733]/g},
	    {'base':'ae','letters':/[\u00E6\u01FD\u01E3]/g},
	    {'base':'ao','letters':/[\uA735]/g},
	    {'base':'au','letters':/[\uA737]/g},
	    {'base':'av','letters':/[\uA739\uA73B]/g},
	    {'base':'ay','letters':/[\uA73D]/g},
	    {'base':'b', 'letters':/[\u0062\u24D1\uFF42\u1E03\u1E05\u1E07\u0180\u0183\u0253]/g},
	    {'base':'c', 'letters':/[\u0063\u24D2\uFF43\u0107\u0109\u010B\u010D\u00E7\u1E09\u0188\u023C\uA73F\u2184]/g},
	    {'base':'d', 'letters':/[\u0064\u24D3\uFF44\u1E0B\u010F\u1E0D\u1E11\u1E13\u1E0F\u0111\u018C\u0256\u0257\uA77A]/g},
	    {'base':'dz','letters':/[\u01F3\u01C6]/g},
	    {'base':'e', 'letters':/[\u0065\u24D4\uFF45\u00E8\u00E9\u00EA\u1EC1\u1EBF\u1EC5\u1EC3\u1EBD\u0113\u1E15\u1E17\u0115\u0117\u00EB\u1EBB\u011B\u0205\u0207\u1EB9\u1EC7\u0229\u1E1D\u0119\u1E19\u1E1B\u0247\u025B\u01DD]/g},
	    {'base':'f', 'letters':/[\u0066\u24D5\uFF46\u1E1F\u0192\uA77C]/g},
	    {'base':'g', 'letters':/[\u0067\u24D6\uFF47\u01F5\u011D\u1E21\u011F\u0121\u01E7\u0123\u01E5\u0260\uA7A1\u1D79\uA77F]/g},
	    {'base':'h', 'letters':/[\u0068\u24D7\uFF48\u0125\u1E23\u1E27\u021F\u1E25\u1E29\u1E2B\u1E96\u0127\u2C68\u2C76\u0265]/g},
	    {'base':'hv','letters':/[\u0195]/g},
	    {'base':'i', 'letters':/[\u0069\u24D8\uFF49\u00EC\u00ED\u00EE\u0129\u012B\u012D\u00EF\u1E2F\u1EC9\u01D0\u0209\u020B\u1ECB\u012F\u1E2D\u0268\u0131]/g},
	    {'base':'j', 'letters':/[\u006A\u24D9\uFF4A\u0135\u01F0\u0249]/g},
	    {'base':'k', 'letters':/[\u006B\u24DA\uFF4B\u1E31\u01E9\u1E33\u0137\u1E35\u0199\u2C6A\uA741\uA743\uA745\uA7A3]/g},
	    {'base':'l', 'letters':/[\u006C\u24DB\uFF4C\u0140\u013A\u013E\u1E37\u1E39\u013C\u1E3D\u1E3B\u017F\u0142\u019A\u026B\u2C61\uA749\uA781\uA747]/g},
	    {'base':'lj','letters':/[\u01C9]/g},
	    {'base':'m', 'letters':/[\u006D\u24DC\uFF4D\u1E3F\u1E41\u1E43\u0271\u026F]/g},
	    {'base':'n', 'letters':/[\u006E\u24DD\uFF4E\u01F9\u0144\u00F1\u1E45\u0148\u1E47\u0146\u1E4B\u1E49\u019E\u0272\u0149\uA791\uA7A5]/g},
	    {'base':'nj','letters':/[\u01CC]/g},
	    {'base':'o', 'letters':/[\u006F\u24DE\uFF4F\u00F2\u00F3\u00F4\u1ED3\u1ED1\u1ED7\u1ED5\u00F5\u1E4D\u022D\u1E4F\u014D\u1E51\u1E53\u014F\u022F\u0231\u00F6\u022B\u1ECF\u0151\u01D2\u020D\u020F\u01A1\u1EDD\u1EDB\u1EE1\u1EDF\u1EE3\u1ECD\u1ED9\u01EB\u01ED\u00F8\u01FF\u0254\uA74B\uA74D\u0275]/g},
	    {'base':'oi','letters':/[\u01A3]/g},
	    {'base':'ou','letters':/[\u0223]/g},
	    {'base':'oo','letters':/[\uA74F]/g},
	    {'base':'p','letters':/[\u0070\u24DF\uFF50\u1E55\u1E57\u01A5\u1D7D\uA751\uA753\uA755]/g},
	    {'base':'q','letters':/[\u0071\u24E0\uFF51\u024B\uA757\uA759]/g},
	    {'base':'r','letters':/[\u0072\u24E1\uFF52\u0155\u1E59\u0159\u0211\u0213\u1E5B\u1E5D\u0157\u1E5F\u024D\u027D\uA75B\uA7A7\uA783]/g},
	    {'base':'s','letters':/[\u0073\u24E2\uFF53\u00DF\u015B\u1E65\u015D\u1E61\u0161\u1E67\u1E63\u1E69\u0219\u015F\u023F\uA7A9\uA785\u1E9B]/g},
	    {'base':'t','letters':/[\u0074\u24E3\uFF54\u1E6B\u1E97\u0165\u1E6D\u021B\u0163\u1E71\u1E6F\u0167\u01AD\u0288\u2C66\uA787]/g},
	    {'base':'tz','letters':/[\uA729]/g},
	    {'base':'u','letters':/[\u0075\u24E4\uFF55\u00F9\u00FA\u00FB\u0169\u1E79\u016B\u1E7B\u016D\u00FC\u01DC\u01D8\u01D6\u01DA\u1EE7\u016F\u0171\u01D4\u0215\u0217\u01B0\u1EEB\u1EE9\u1EEF\u1EED\u1EF1\u1EE5\u1E73\u0173\u1E77\u1E75\u0289]/g},
	    {'base':'v','letters':/[\u0076\u24E5\uFF56\u1E7D\u1E7F\u028B\uA75F\u028C]/g},
	    {'base':'vy','letters':/[\uA761]/g},
	    {'base':'w','letters':/[\u0077\u24E6\uFF57\u1E81\u1E83\u0175\u1E87\u1E85\u1E98\u1E89\u2C73]/g},
	    {'base':'x','letters':/[\u0078\u24E7\uFF58\u1E8B\u1E8D]/g},
	    {'base':'y','letters':/[\u0079\u24E8\uFF59\u1EF3\u00FD\u0177\u1EF9\u0233\u1E8F\u00FF\u1EF7\u1E99\u1EF5\u01B4\u024F\u1EFF]/g},
	    {'base':'z','letters':/[\u007A\u24E9\uFF5A\u017A\u1E91\u017C\u017E\u1E93\u1E95\u01B6\u0225\u0240\u2C6C\uA763]/g}
	    ];
	}
	
	static removeDiacritics(str) {
		if (!this.defaultDRM) this.init();
		for(var i=0; i<this.defaultDRM.length; i++)
			str = str.replace(this.defaultDRM[i].letters, this.defaultDRM[i].base);
		return str;
	}

}
Util.setup();
App.Util = Util;
/*****************************************************/
export class B64 {
	static get chars() { return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"; }
	static get chars2() { return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"; }
	static get egal() { return "=".charCodeAt(0); }

	static init() {
		if (!this.lk) {
			this.lk = new Uint8Array(64);
			this.lk2 = new Uint8Array(64);
			for(let i = 0; i < 64; i++){
				this.lk[i] = this.chars.charCodeAt(i);
				this.lk2[i] = this.chars2.charCodeAt(i);
			}
		}
	}
	
	static isBase64NN(b64){
		if (!b64) return false;
		return this.isBase64(b64);
	}
	
	static isBase64(b64){
		if (!b64) return true;
		let len = b64.length;

		if (b64.charAt(len - 1) == '=') {
			len--;
			if (b64.charAt(len - 1) == '=') {
				len--;
			}
		}
		if (len % 4 == 1) return false;
		for(let i = 0; i < len; i++){
			let c = b64.charAt(i);
			if ((c == '+' || c == '-' || c == '/' || c == '_') 
				|| (c >= '0' && c <= '9') 
				|| (c >= 'a' && c <= 'z') 
				|| (c >= 'A' && c <= 'Z')) continue;		
			return false;
		}
		return true;
	}
	
	static intToBase64(intv) {
		this.init();
	    let b = [0, 0, 0, 0];
	    for (let i = 0; i < 4; i++) {
	        var byte = intv & 0xff;
	        b[i] = byte;
	        intv = (intv - byte) / 256 ;
	    }

		const cx = this.lk2;
		let out = String.fromCharCode(cx[b[0] >> 2]);
		out += String.fromCharCode(cx[((b[0] & 3) << 4) | (b[1] >> 4)]);
		out += String.fromCharCode(cx[((b[1] & 15) << 2) | (b[2] >> 6)]);
		out += String.fromCharCode(cx[b[2] & 63]);
		out += String.fromCharCode(cx[b[3] >> 2]);
		out += String.fromCharCode(cx[((b[3] & 3) << 4)]);
		return out;
	}

	static encode(bytes, special) {
		if (bytes == null) return null;
		this.init();
		const len = bytes.length;
		let len2 = Math.ceil(len / 3) * 4;
		if (special){
			if ((len % 3) === 2) {
				len2--;
			} else if (len % 3 === 1) {
				len2 -= 2;
			}
		}
		
		const cx = special ? this.lk2 : this.lk;
		const u8 = new Uint8Array(len2);

		for (let i = 0, j = 0; i < len; i+=3) {
			u8[j++] = cx[bytes[i] >> 2];
			u8[j++] = cx[((bytes[i] & 3) << 4) | (bytes[i + 1] >> 4)];
			u8[j++] = cx[((bytes[i + 1] & 15) << 2) | (bytes[i + 2] >> 6)];
			u8[j++] = cx[bytes[i + 2] & 63];
		}

		if (!special) {
			if ((len % 3) === 2) {
				u8[len2 - 1] = this.egal;
			} else if (len % 3 === 1) {
				u8[len2 - 1] = this.egal;
				u8[len2 - 2] = this.egal;
			}
		}

		return Util.toUtf8(u8);
	}
	
	static decode(strBase64) {
		if (strBase64 == null) return null;
		const base64 = strBase64.replace(/-/g, '+').replace(/_/g, '/');
		//let bufferLength = Math.round(base64.length * 0.75);
		let bufferLength = Math.floor((base64.length * 3) / 4);
		let len = base64.length;
		let p = 0;
		let encoded1, encoded2, encoded3, encoded4;

		if (base64[base64.length - 1] === "=") {
			bufferLength--;
			if (base64[base64.length - 2] === "=") {
				bufferLength--;
			}
		}

		const bytes = new Uint8Array(bufferLength);
		if (!this.lookup) {
			  // Use a lookup table to find the index.
			  this.lookup = new Uint8Array(256);
			  for (let i = 0; i < this.chars.length; i++) {
				  this.lookup[this.chars.charCodeAt(i)] = i;
			  }			
		}
		for (let i = 0; i < len; i+=4) {
			encoded1 = this.lookup[base64.charCodeAt(i)];
			encoded2 = this.lookup[base64.charCodeAt(i+1)];
			encoded3 = this.lookup[base64.charCodeAt(i+2)];
			encoded4 = this.lookup[base64.charCodeAt(i+3)];

			bytes[p++] = (encoded1 << 2) | (encoded2 >> 4);
			bytes[p++] = ((encoded2 & 15) << 4) | (encoded3 >> 2);
			bytes[p++] = ((encoded3 & 3) << 6) | (encoded4 & 63);
		}

		return bytes;
	}
	
	static src(image){
		return "data:" + image.contentType + ";base64," + this.encode(image.uint8);
	}
	
	static test(special) {
		const t1 = new Date().getTime();
		for(let len = 1; len < 1024; len++){
			for(let j = 0; j < 10; j++){
				const a = GEN.Crypt.randomNUint8(len);
				const b64 = this.encode(a, special);
				const b = this.decode(b64);
				if (!GEN.Crypt.uint8Equal(a, b)){
					console.error("Base64 - " + len + " / " + b64.length + "[" + a + "] " + b64);
				}
			}
		}
		const t2 = new Date().getTime();
		console.log((t2 - t1) + "ms");
	}
}

/*****************************************************/
