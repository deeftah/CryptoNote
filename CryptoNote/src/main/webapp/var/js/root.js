class App {
	static setup() {
		this.build = 1000;
		this.home = "index";
		this.mode = 1;
		this.buildAtPageGeneration = 0;
		this.srvbuild = 0;
		this.contextpath = ""; 
		this.namespace = ""; 
		this.mode = 0; 
		this.modeMax = 0; 
		this.ready1 = false;
		this.ready2 = false;
		this.debug = false;
		this.isSW = true;
		this.langs = ["fr","en"]; 
		this.lang = "fr"; 
		this.zone = "Europe/Paris";
		this.theme = "a";
		this.themes = ["a"];
		this.customThemes = {};
		this.zDics = {};
	}
	
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
			d[code] = msg;
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
		for(let code in arg) t[code] = arg[code];
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

	static ctxNsSlash() { return (!this.contextpath ? "/" : "/" + this.contextpath + "/") + (this.namespace ? this.namespace + "/" : "");}
		
	static baseUrl(lvl) { // 0:/cp/ns/ 1:/cp/ns/var9999/
		return window.location.origin 
		+ (window.location.origin.endsWith("/") ? "" : "/") 
		+ (!this.contextpath ? "" : this.contextpath + "/")
		+ (!this.namespace ? "" : this.namespace + "/") 
		+ (!lvl ? "" : "var" + this.build + "/");
	}

	static homeUrl() { 
		return (!this.contextpath ? "" : this.contextpath + "/")
		+ (!this.namespace ? "" : this.namespace + (this.mode ? "/" : "$/")) 
		+ this.home;
	}
	
	static reloadUrl(reload) { 
		return window.location.origin 
		+ (window.location.origin.endsWith("/") ? "" : "/")
		+ (!this.contextpath ? "" : this.contextpath + "/")
		+ this.namespace + (reload ? "$/var" : "/var") + this.build + "/";
	}

};
/*****************************************************/
App.setup();

Date.prototype.format = function(format) {
	let fullYear = this.getYear();
	if (fullYear < 1000)
		fullYear = fullYear + 1900;
	const hour =this.getHours(); 
	const day = this.getDate();
	const month = this.getMonth() + 1;
	const minute = this.getMinutes();
	const seconde = this.getSeconds();
	const ms = this.getMilliseconds();
	const reg = new RegExp('(d|m|Y|H|i|s|S)', 'g');
	const replacement = new Array();
	replacement['d'] = day < 10 ? '0' + day : day;
	replacement['m'] = month < 10 ? '0' + month : month;
	replacement['S'] = ms < 10 ? '00' + ms : (ms < 100 ? '0' + ms : ms);
	replacement['Y'] = fullYear;
	replacement['H'] = hour < 10 ? '0' + hour : hour;
	replacement['i'] = minute < 10 ? '0' + minute : minute;
	replacement['s'] = seconde < 10 ? '0' + seconde : seconde;
	return format.replace(reg, function($0) {
		return ($0 in replacement) ? replacement[$0] : $0.slice(1, $0.length - 1);
	});
};

Date.prototype.compact = function(now) {
	if (!now) now = new Date();
	let a1 = now.getYear();
	if (a1 < 1000) a1 += 1900;
	const m1 = now.getMonth() + 1;
	const j1 = now.getDate();
	let a2 = this.getYear();
	if (a2 < 1000) a2 += 1900;
	const m2 = this.getMonth() + 1;
	const j2 = this.getDate();
	const h = this.getHours();
	const m = this.getMinutes();
	let res = "";
	if (a1 != a2) {
		res = "" + a2 + "-" + (m2 < 10 ? '0' + m2 : m2) + "-" + (j2 < 10 ? '0' + j2 : j2) + " ";
	} else if (m1 != m2) {
		res = "" + (m2 < 10 ? '0' + m2 : m2) + "-" + (j2 < 10 ? '0' + j2 : j2) + " ";
	} else if (j1 != j2) {
		res = "" + (j2 < 10 ? '0' + j2 : j2) + " ";
	}
	res += "" + (h < 10 ? '0' + h : h) + ":" + (m < 10 ? '0' + m : m);
	return res;
}
/*****************************************************/
class StringBuffer {
	constructor() { this.buffer = []; }
	append(string) { this.buffer.push(string); return this; }
	toString() { return this.buffer.join(""); }
	join(arg) { return this.buffer.join(arg); }
	isEmpty() { return this.buffer.length != 0; }
	clear() { this.buffer = []; }
}

/*****************************************************/
