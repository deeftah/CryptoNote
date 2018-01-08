class App {
	static get HPC() { return "htm"}
	static get AVION() { return "a"}
	static get INCOGNITO() { return "$"}

	static async setup() {
		this.build = appbuild;
		this.isSW = navigator.serviceWorker ? true : false;
		
		this.origin = window.location.origin; 						// https://... :8443
		this.path = window.location.pathname; 						// /cn/test/home2.ahtm?...
		this.hash = window.location.hash;							// ?...
		this.page = this.path.substring(0, this.path.length - this.hash.length); // /cn/test/home2.ahtm
		
		let i = this.page.lastIndexOf("/");
		let x = this.page.substring(i + 1); 						// home2.ahtm
		let j = x.lastIndexOf(".");
		this.ext = j == -1 ? "" : x.substring(j + 1); 				// ahtm
		this.home = j == -1 ? x : x.substring(0, j);				// home2 OU home2_202_
		let wb = false;
		if (this.home.endsWith("_")){
			j = this.home.lastIndexOf("_", this.home.length - 2);
			this.home = this.home.substring(0, j);
			wb = true;
		}
		this.base = this.page.substring(0, i);						// /cn/test OU /test OU /cn/test$ OU /test$
		this.basevar = this.base + "/var" + App.build + "/";
		this.mode = this.base.endsWith(this.INCOGNITO) ? 0 : (this.ext.startsWith(this.AVION) ? 2 : 1);
		
		x = this.mode ?	this.base : this.base.substring(0, this.base.length - 1);
		i = x.lastIndexOf("/");
		if (i == 0) {
			// PAS de contextpath
			this.contextpath = "";
			this.namespace = x.substring(1);
		} else {
			this.contextpath = x.substring(1, i);
			this.namespace = x.substring(i + 1);
		}
		
		if (wb) {
			let nu = this.base + "/" + this.home + (this.ext ? "." + this.ext : "") + this.hash;
			history.replaceState({}, "", nu);
		}
		
		// Pour Apple et Edge (appcache)
		if (this.mode > 0 && !this.isSW && !this.ext.endsWith(this.HPC)) 
			window.location = this.origin + this.page + (this.ext ? this.HPC : "." + this.HPC) + this.hash; // rechargement avec htm dans l'extension
		
		//Pour Safari / IOS !!!
		if (window.crypto && !window.crypto.subtle && window.crypto.webkitSubtle) {
			window.crypto.subtle = window.crypto.webkitSubtle;
			this.IOS = true;
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
		if (this.isSW) {
			if (this.mode) {
				const swscope = (!this.contextpath ? "/" : "/" + this.contextpath + "/") + this.namespace + "/sw.js";
				navigator.serviceWorker.register(swscope)
				.then(reg => {
					console.log("Succès de l'enregistrement auprès du service worker. Scope:" + reg.scope);
				}).catch(err => {
					console.log("Echec de l'enregistrement auprès du service worker. Scope:" + swscope);					
				})
			}
		} else {
			console.log("Ecoute updateready ... ");
			window.applicationCache.addEventListener('updateready', e => {
				console.log("Evt. updateready reçu " + window.applicationCache.status);
			    if (window.applicationCache.status == window.applicationCache.UPDATEREADY) 
			    	this.reload();
			}, false);
		}

		if (App.mode == 2) 
			return this.rootready();
		
		// test buuild du serveur
		App.srvbuild = await this.pingFetch();
		if (App.mode == 0) 
			return !App.srvbuild || App.srvbuild != App.build ? this.reloadWB() : this.rootready();
		
		// mode 1
		if (!App.srvbuild) {
			if (App.modeMax < 2) 
				return this.reloadWB();
			if (confirm(App.lib("xping_2")))
				return this.reloadWB();					
			return this.reloadAvion();	
		}
		
		return App.srvbuild != App.build ? this.reloadWB() : this.rootready();
	}
	
	static rootready() {
		App.rootcomplete = true;
  		if (App.appHomes)
  			App.appHomes.starting();
	}
	
	static async pingFetch() {
		try {
			const myOptions = {timeout:6000}
			const myHeaders = new Headers();
			myHeaders.append("X-Custom-Header", JSON.stringify(myOptions));
			const resp = await fetch(this.base + "/ping", {headers: myHeaders});
			if (resp.ok) {
				const r = await resp.json();
				console.log("srvok:" + JSON.stringify(r ? r : {}));
				return  r && r.b ? r.b : 0;
			} else {
				console.error("srvko: fetch not ok");
				return 0;
			}
		} catch(err) {
			console.error("srvko: " + err.message);
			return 0;
		}
	}

	static reload() {
		window.location.reload(true);
	}

	static reloadWB() { // With Build
		// window.location = this.origin + this.base + "/" + this.home + "_" + this.srvbuild + "_" + (this.isSW ? "" : "." + this.HPC) + this.hash;
		const url = this.origin + this.base + "/" + this.home + "_" + this.srvbuild + "_" + (this.isSW ? "" : "." + this.HPC) + this.hash;
	 	const b2 = this.base.endsWith(this.INCOGNITO) ? this.base.substring(0, this.base.length - this.INCOGNITO.length) : this.base;
		const url2 = this.origin + b2 + "/var/reload2.html?";
	 	const x = {lang:App.lang, nslabel:App.nslabel(), applabel:App.applabel(), build:App.srvbuild, b:App.build, reload:url, reload2:url2}
	 	const b = this.base.endsWith(this.INCOGNITO) ? this.base : this.base + this.INCOGNITO;
//	 	const b = this.base;
		window.location = this.origin + b + "/var/reload.html?" + encodeURI(JSON.stringify(x));
	}

	static reloadAvion() { 
		window.location = this.mode == 2 ? (this.origin + this.path) : (this.origin + this.page + "." + this.AVION + (this.isSW ? "" : this.HPC) + this.hash);
	}

	static bye() {
		const x = {lang:App.lang, nslabel:App.nslabel(), applabel:App.applabel(), home:App.homeUrl()}
		window.location = this.origin + this.page + "/bye.html?" + encodeURI(JSON.stringify(x));
	}

//	static errorPage(msg) {
//		const x = {lang:App.lang, nslabel:App.nslabel(), applabel:App.applabel(), home:App.homeUrl(), msg:msg}
//		window.location = this.origin + this.page + "/error.html?" + encodeURI(JSON.stringify(x));
//	}

	static setAll(lang, arg) {
		if (this.langs.indexOf(lang) == -1 || !arg) return;
		let d = this.zDics[lang];
		if (!d) {
			d = {}
			this.zDics[lang] = d;
		}
		for(let code in arg) d[code] = arg[code].replace(/''/g, "'");
	}
	
	static setMsg(lang, code, msg) {
		let d = this.zDics[lang];
		if (d && !d[code])
			d[code] = msg.replace(/''/g, "'");
	}
	
	static lib(code) {
		if (!code) return "?";
		let d = this.zDics[this.lang];
		let x = !d ? null : d[code];
		const l = this.langs[0];
		if (!x && this.lang != l) {
			d = this.zDics[l];
			x = !d ? null : d[code];
		}
		return x ? x : code;
	}
	
	static setTheme(code, arg){
		if (!code || !arg || this.themes.indexOf(code) == -1) return;
		let t = this.customThemes[code];
		if (!t) {
			t = {};
			this.customThemes[code] = t;
		}
		for(let x in arg) t[x] = arg[x];
	}
	
	static format(code, args) { // 0 à N arguments après le code
		let x = this.lib(code);
		if (x == "?" || x == code) {
			if (arguments.length > 1)
				for(let i = 1; i < arguments.length; i++) {
					let t = arguments[i] ? arguments[i] : "";
					x += " " + i + ":[" + t + "]";
				}
			return x;
		}
		if (arguments.length == 1) return x;
		for(let i = 1; i < arguments.length; i++) {
			let t = typeof arguments[i] != 'undefined' ? arguments[i] : "";
			let y = "{" + (i - 1) + "}";
			let j = x.indexOf(y);
			if (j != -1)
				x = x.substring(0, j) + t + x.substring(j + y.length);
		}
		return x;
	}

	static nslabel() { return App.lib("ns_" + App.namespace);}

	static applabel() { return App.lib("application");}

}
