class Util {			

	static editPC(n1, n2){
		if (n2 <= 0 || n1 >= n2) return "100%";
		if (!n1 || n1 < 0) return "0%";
		return "" + Math.round((n1 * 100) / n2) + "%";
	}
	
	static editVol(v) {
		if (!v || v <= 0) return "0o";
		v = Math.round(v);
		if (v < 1000) return "" + v + "o";
		const s = "" + v;
		if (v < 10000) return s.substring(0,1) + "," + s.substring(1,3) + "Ko";
		if (v < 100000) return s.substring(0,2) + "," + s.substring(2,3) + "Ko";
		if (v < 1100000) return s.substring(0,1) + "," + s.substring(1,3) + "Mo";
		if (v < 10000000) return s.substring(0,2) + "," + s.substring(2,3) + "Mo";
		return s.substring(0,3) + "Mo";
	}

	/*
	 * Réservé aux messages fonctionnels (qui apparaissent à l'écran (sauf si duration est 0)
	 * Usage : console.log(App.Util.log("Mon beau message", 5000)); (ou console.error(...))
	 */
	static log(message, duration) { 
		if (duration && message && App.messageBox) App.messageBox.show(message, duration);
		return new Date().format("Y-m-d H:i:s.S") + " - " + message;
	}

	/*
	 * Réservé aux messages fonctionnels (qui apparaissent à l'écran (sauf si duration est 0)
	 * Usage : console.error(App.Util.log("Mon beau message", 5000));
	 */
	static error(message, duration) { 
		if (duration && message && App.messageBox) App.messageBox.show(message, duration, true);
		return new Date().format("Y-m-d H:i:s.S") + " - " + message;
	}

	static bytes2string(bytes) { 
		if (!this.decoder) this.decoder = new TextDecoder("utf-8");
		return bytes ? this.decoder.decode(bytes) : ""; 
	}

	static string2bytes(string) { 
		if (!this.encoder) this.encoder = new TextEncoder("utf-8");
		return string ? this.encoder.encode(string) : new Uint8Array(0); 
	}
		
	static get defaultDRM() { return [
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
		const drm = this.defaultDRM();
		for(var i=0; i<drm.length; i++)
			str = str.replace(drm[i].letters, drm[i].base);
		return str;
	}

	static bytesEqual(a, b){
		if (!a && !b) return true;
		if ((a && !b) || (!a && b) || (a.length != b.length)) return false;
		for(let i = 0; i< a.length; i++)
			if (a[i] != b[i]) return false;
		return true;
	}

	/** BCrypt ***************************************************/
	static get salt() { return "$2a$10$kBdas.cIGiNW/ziEj/pZD."; }

	static bcrypt(data) {
		return dcodeIO.bcrypt.hashSync(data, this.salt).substring(29).replace(/\./g, '-').replace(/\//g, '_');
	}

	static bcryptCompare(data, hash) {
		let h = hash.replace(/-/g, '.').replace(/_/g, '/');
		return dcodeIO.bcrypt.compareSync(data, this.salt + h);
	}
	
	static bcrypt2u32(data) {
		const r = new Uint8Array(32).fill(0);
		r.set(B64.decode(data));
		return r;
	}
	
	/** SHA-256 ***************************************************/
	static async sha256(data) {
		const uint8 = typeof data === "string" ? Util.string2bytes(data) : data;
		let result = await crypto.subtle.digest({name: "SHA-256"}, uint8);
		return new Uint8Array(result);
	}

	/** Hash of String ***************************************************/
	static hashOf(s) { return B64.intToBase64(this.checksum(s))};
	
	static checksum(s) { // hash de Java String
		let hash = 0;
		let strlen = s ? s.length : 0;
		if (strlen === 0) return 0;
		for (let i = 0; i < strlen; i++) {
			let c = s.charCodeAt(i);
			hash = ((hash << 5) - hash) + c;
			hash = hash & hash; // Convert to 32bit integer
		}
		return hash;
	}

}

/*****************************************************/
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
class B64 {
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

	static encode(bytes, file) {
		if (bytes == null) return null;
		this.init();
		const len = bytes.length;
		let len2 = Math.ceil(len / 3) * 4;
		if (!file){
			if ((len % 3) === 2) {
				len2--;
			} else if (len % 3 === 1) {
				len2 -= 2;
			}
		}
		
		const cx = file ? this.lk : this.lk2;
		const u8 = new Uint8Array(len2);

		for (let i = 0, j = 0; i < len; i+=3) {
			u8[j++] = cx[bytes[i] >> 2];
			u8[j++] = cx[((bytes[i] & 3) << 4) | (bytes[i + 1] >> 4)];
			u8[j++] = cx[((bytes[i + 1] & 15) << 2) | (bytes[i + 2] >> 6)];
			u8[j++] = cx[bytes[i + 2] & 63];
		}

		if (file) {
			if ((len % 3) === 2) {
				u8[len2 - 1] = this.egal;
			} else if (len % 3 === 1) {
				u8[len2 - 1] = this.egal;
				u8[len2 - 2] = this.egal;
			}
		}

		return Util.bytes2string(u8);
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
	
}

/*****************************************************/
class AES {
	constructor(key, uint8){
		this.key = key;
		this.uint8 = uint8;
		if (!AES.defaultVector)
			AES.defaultVector = new Uint8Array([101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116]); 
	}
	
	static async newAES(passphraseKey) {
		const uint8 = typeof passphraseKey === "string" ? Util.string2bytes(passphraseKey) : passphraseKey;
		const b = uint8.length == 32 ? uint8 : Util.sha256(uint8);
		let webkey = await crypto.subtle.importKey('raw', b, {name: "AES-CBC"}, false, ["encrypt", "decrypt"]);
		return new AES(webkey, uint8);
	}

	async encode(data, gzip){
		if (data == null) return null;
		let uint8 = typeof data === "string" ? Util.string2bytes(data) : data;
		if (!uint8) uint8 = new Uint8Array(0);
		const deflated = gzip ? pako.deflate(uint8) : uint8;
		let result = await crypto.subtle.encrypt({name: "AES-CBC", iv:AES.defaultVector}, this.key, deflated);
		return new Uint8Array(result);
	}

	async decode(encoded, gzip){
		if (encoded == null) return null;
	    let result = await crypto.subtle.decrypt({name: "AES-CBC", iv:AES.defaultVector}, this.key, encoded);
    	const bin = new Uint8Array(result);
        return gzip ? pako.inflate(bin) : bin;
	}

	/*
	 * photo est un base64 du cryptage d'une URL d'image par une clé AES
	 */
	async decodeImage(photoB64) {
		let photob = await this.decode(B64.decode(photoB64));
		const ph = Util.bytes2string(photob);
		if (!ph || !ph.startsWith("data:image/")) return null;
		const i = ph.indexOf(";");
		if (i == -1) return null;
		const j = ph.indexOf(",");
		if (j != i + 7 || ph.substring(i + 1, j) != "base64" || !B64.isBase64NN(ph.substring(j + 1))) return null;
		return ph;
	}

}

/*****************************************************/
class RSA {
	constructor() {	
	}

	static get rsaObj() {
		// le hash DOIT être SHA-1 pour interaction avec java (le seul qu'il accepte d'échanger)
		return {name: "RSA-OAEP", modulusLength: 2048, publicExponent: new Uint8Array([0x01, 0x00, 0x01]), hash: {name: "SHA-1"}};
	}

	//	rsassaObj : {name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: {name: "SHA-256"}},
	// http://stackoverflow.com/questions/33043091/public-key-encryption-in-microsoft-edge
	// hash: { name: "SHA-1" } inutile mais fait marcher edge !!!
	
	async encode(data) {
		const uint8 = typeof data === "string" ? Util.string2bytes(data) : data;
		let result = await crypto.subtle.encrypt({name: "RSA-OAEP", hash: { name: "SHA-1" }}, this.pub, uint8);
		return new Uint8Array(result);
	}
	
	async decode(data) {
		const uint8 = typeof data === "string" ? B64.decode(data) : data;
		let result = await crypto.subtle.decrypt({name: "RSA-OAEP", hash: { name: "SHA-1" }}, this.priv, uint8);
	    return new Uint8Array(result);
	}
	
	static async newRSAGen() {
		const rsa = new RSA();
		const obj = this.rsaObj;
		let key = await crypto.subtle.generateKey(obj, true, ["encrypt", "decrypt"]);
		rsa.priv = key.privateKey;
		rsa.pub = key.publicKey;
		let jpriv = await crypto.subtle.exportKey("jwk", rsa.priv);
		rsa.jwkpriv = RSA.ios() ? Util.bytes2string(jpriv) : JSON.stringify(jpriv);
		let jpub = await crypto.subtle.exportKey("jwk", rsa.pub);
		rsa.jwkpub = RSA.ios() ? Util.bytes2string(jpub) : JSON.stringify(jpub);
		return rsa;
	}
	
	static async compareRSAPub(p1, p2, ch){ 
		if ((p1 && !p2) || (p2 && !p1) || !ch) return null;
		try {
			let x = await p1.encode(ch);
			let chec64 = B64.encode(x, true);
			let y = await p2.encode(ch);
			let y64 = B64.encode(y, true);
			return y64 == chec64 ? chec64 : null;
		} catch(err) {
			return null;
		}
	}

	static async compareRSAPriv(p1, p2, chec64, ch64){ 
		if ((p1 && !p2) || (p2 && !p1) || !chec64 || !ch64) return false;
		try {
			const chec = B64.decode(chec64);
			let x = await p1.decode(chec);
			let chdc64 = B64.encode(x, true);
			if (chdc64 != ch64) return false;
			let y = await p2.decode(chec);
			const y64 = B64.encode(y, true);
			return y64 == chdc64;
		} catch (err) {
			return false;
		}
	}

	static async compareRSA(pub1, pub2, priv1, priv2, ch) {
		let chec64 = await this.compareRSAPub(pub1, pub2, ch);
		if (!chec64) return false;
		const ch64 = B64.encode(ch, true);
		return await this.compareRSAPriv(priv1, priv2, chec64, ch64);
	}
		
//	static async ios() {
//		if (App.IOS == null) {
//			try {
//				let key = await crypto.subtle.generateKey(RSA.rsaObj, true, ["encrypt", "decrypt"]);
//				let jwk = await crypto.subtle.exportKey("jwk", key.privateKey);
//				const x = JSON.stringify(jwk);
//				const y = JSON.parse(x);
//				let result2 = await crypto.subtle.importKey("jwk", y, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["decrypt"]);
//				App.IOS = false;
//			} catch (err) {
//				App.IOS = true;
//			}
//		}
//		return App.IOS;
//	}
	
	static ios() { return App.IOS; }
	
	static async newRSAPriv(jwkJson) {
		const key = RSA.ios() ? Util.string2bytes(jwkJson) : JSON.parse(jwkJson);
		let result2 = await crypto.subtle.importKey("jwk", key, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["decrypt"]);
		const rsa = new RSA();
		rsa.priv = result2;
		rsa.jwkpriv = jwkJson;
		return rsa;
	}

	static async newRSAPub(jwkJson) {
		const key = RSA.ios() ? Util.string2bytes(jwkJson) : JSON.parse(jwkJson);
		let result2 = await crypto.subtle.importKey("jwk", key, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["encrypt"])
		const rsa = new RSA();
		rsa.pub = result2;
		rsa.jwkpub = jwkJson;
		return rsa;
	}

}

/*****************************************************/
class ReqErr {
	constructor(op, code, phase, message, detail) {
		this.op = op;
		this.code = code ? code : "BX";
		this.phase = phase && phase > 0 && phase < 10 ? phase : 0;
		this.message = message ? message : this.code;
		this.detail = detail ? detail : [];
	}
	
	/*
	 * Phase 
	 * 0 : avant opération dans le serveur
	 * 1 : dans l'opération (work)
	 * 2 : au cours de la validation
	 * 3 : après validation (afterwork)
	 * 4 : au cours de la synchronisation
	 * 5 : lors de l'envoi de la réponse
	 * 6 : inconnu entre 0 et 5, et / ou réseau
	 * 8 : dans le script d'envoi au serveur
	 * 9 : dans le script d'interprétation de la réponse
	 */
	
	/* m : major
	 * N : notfound -> 1 : 404 : not found. Document ou attachment non trouvé
	 * A : app -> 1 : 400 : erreur applicative autre que N. Données incorrectes
	 * B : bug -> 2 : 400 : bug, situation des données illogique
	 * X : unexpected -> 3 : 400 : incident inattendu, matériel ou logiciel
	 * D : delayed build -> 4 : 400 : recharger l'application cliente
	 * C : contention -> 5 : 400 : trop de contention sur la base de données
	 * O : espace off -> 6 : 400 : espace en maintenance
	 * S : session non autorisée -> 7 : 400 : ou résiliée ou trop longue ...
	 * T : timeout -> 8
	 * I : interrupted -> 9
	 * L : erreur d'exécution d'un script local (avant ou après) -> 0
	 */
	major() {
		const m = "NABXDCOSTI".indexOf(this.code.charAt(0));
		return m == -1 ? 0 : m;
	}
	
	isSrv() { return this.phase < 6; }
	
}

/*****************************************************/
class Req {
	constructor() {
		this.TIME_OUT_MS = 300000;
		this.url = new StringBuffer().append(App.base + "/");
		this.currentRetry = null;
		this.hasArgs = false;
		this.formData = new FormData();
		this.cred = 0;
		this.reqErr = App.globalReqErr;
		this.spinner = App.globalSpinner;
	}
	
	setReqErr(reqErr) {
		if (reqErr)
			this.reqErr = reqErr; 
		return this; 
	}

	setSpinner(spinner) {
		if (spinner)
			this.spinner = spinner; 
		return this; 
	}

	setTimeOut(timeOut) { this.TIME_OUT_MS = timeOut; return this; }

	setFormData() { this.formData = formData; return this; } // à citer AVANT ceux qui suivent

	setSyncs(syncs) { if (syncs) this.formData.append("syncs", JSON.stringify(syncs)); return this;}
	
	setOp(op, param, url) { 
		this.op = op; 
		if (op.endsWith("ping")) { // "ping" ou "../ping"
			this.url.append(op);
		} else {
			this.param = JSON.stringify(param); 
			this.url.append("op/").append(url ? url : "");
			this.formData.append("op", this.op);
			this.hasArgs = true;
		}
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
	
	setStartMsg(startMsg) {
		if (startMsg)
			this.startMsg = startMsg;
		return this;
	}
	
	async go(){
		return new Promise((resolve, reject) => {
			try{
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
			this.url = this.url.toString();
			this.resolve = resolve;
			this.reject = reject;
			this.currentRetry = new Retry();
			this.currentRetry.req = this;
			} catch(err) { App.errscript(err);}
			this.currentRetry.send();
		});
	}
	
	kill() {
		if (this.currentRetry)
			this.currentRetry.kill();
	}
	
	onProgress(loaded, total){
		try {
			if (this.spinner.progress)
				this.spinner.progress(App.format("reqRec", Util.editPC(loaded, total), Util.editVol(total)));
		} catch(e) {}
	}
	
	onSuccess(resp) {
		this.currentRetry = null;
		try { if (this.spinner.stop) this.spinner.stop(); } catch(e) {}
		this.resolve(resp);
	}
	
	onError(err) {
		this.currentRetry = null;
		try { if (this.spinner.stop) this.spinner.stop(); } catch(e) {}
		if (this.noCatch && this.noCatch.indexOf(err.code + " ") != -1) {
			this.reject(err);
			return;
		}
		if (this.reqErr && this.reqErr.open)
			this.reqErr.open(this, err);
		else
			this.reject(err);
	}
	
	onRetry() {
		this.currentRetry = new Retry();
		this.currentRetry.req = this;
		this.currentRetry.send();
	}
	
	onReturnToApp(err) {
		this.reject(err);		
	}

}

class Retry { // un objet par retry pour éviter les collisions sur le XHR
	kill() {
		if (this.done) return;
		this.done = true;
		if (this.tim) clearTimeout(this.tim);
		try {if (this.xhr) this.xhr.abort(); } catch(e) {}
		const d = new Date().getTime() - this.t0;
		this.req.onError(new ReqErr(this.req.op, "INTERRUPTED", 9, App.format("INTERRUPTED", this.req.url, d), [this.req.url, d]));
	}
	
	send() {
		let errSend;
		try {
			try { if (this.req.spinner.start)	this.req.spinner.start(this.req, this.req.startMsg ? this.req.startMsg : App.lib("reqStarted")); } catch(e) {}
			this.t0 = new Date().getTime();
			
			this.tim = setTimeout(() => {
				if (!this.done) {
					this.done = true;
					try { if (this.xhr) this.xhr.abort(); } catch(e) {}
					this.req.onError(new ReqErr(this.req.op, "TIMEOUT", 8, App.format("TIMEOUT", this.req.url, Math.round(this.req.TIME_OUT_MS)))); 
				}
			}, this.req.TIME_OUT_MS); 

			this.xhr = new XMLHttpRequest();
			this.xhr.open("POST", this.req.url, true);
			this.xhr.responseType = "arraybuffer";
			
			this.xhr.onerror = (e) => {	
				if (this.done) return;
				if (this.tim) clearTimeout(this.tim);
				this.done = true;
				this.req.onError(this.req.op, new ReqErr("X1", 9, App.format("X1", this.req.url, e.message), [this.req.url, e])); 
			}
			
			this.xhr.onprogress = (e) => {	
				if (this.done) return;
				try { this.req.onProgress(e.loaded, e.total); } catch(e) {}
			}
			
			this.xhr.onreadystatechange = () => {
				if (this.done || this.xhr.readyState != 4) return;
				let resp;
				let err;
				try {
					this.done = true;
					if (this.tim) clearTimeout(this.tim);
					
					const b = this.xhr.getResponseHeader("X-Custom-Header");
					if (b) {
						try {
							let j = JSON.parse(b);
							if (j.build) App.srvbuild = parseInt(j.build);
						} catch (e) {}
					}
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
					let text;
					if (isJson) {
						try {
							text = uint8 ? Util.bytes2string(uint8) : "{}";
							jsonObj = JSON.parse(text);
						} catch (e) {
							err = this.req.op, new ReqErr("BJSONRESP", 9, App.format("BJSONRESP", this.req.url, e.message), [this.req.url, text]); 
						}
					}
					if (!err) {
						if (this.xhr.status == 200) {					    
							resp = isJson ? {json:jsonObj} : {uint8:uint8, charset:charset, contentType:contentType};
						} else {
							err = jsonObj ? new ReqErr(this.req.op, jsonObj.code, jsonObj.phase, jsonObj.message, jsonObj.detail)
							: new ReqErr(this.req.op, "XHTTP", 6, App.format("XHTTP", this.req.url, this.xhr.status, this.xhr.statusText), [this.req.url, this.xhr.status, this.xhr.statusText]);
						}
					}
				} catch(e) { this.req.onError(new ReqErr(this.req.op, "LREC", 9, App.format("LREC", this.req.url, e.message), [this.req.url, e.stack])); }
				// Hors du catch de réception
				if (resp) 
					this.req.onSuccess(resp);
				else
					this.req.onError(err);
			}
			
		} catch(e) {
			this.done = true;
			if (this.tim) clearTimeout(this.tim);
			errSend = new ReqErr(this.req.op, "LSEND", 8, App.format("XSEND", this.req.url, e.message), [this.req.url, e.stack]); 
		}
		
		// Hors du catch d'émission
		if (!errSend) {
			try {
				this.xhr.send(this.req.formData);
			} catch(e) {
				this.done = true;
				if (this.tim) clearTimeout(this.tim);
				errSend = new ReqErr(this.req.op, "XCONN", 8, App.format("XCONN", this.req.url, e.message), [this.req.url, e.stack]); 
			}
		}
		if (errSend)
			this.req.onError(errSend);
	}
}

/*****************************************************/
class DefaultSpinner {
	start(request, info) {
		console.log("Spinner start : " + info);
	}
	progress(info) { 
		console.log("Spinner progress : " + info);
	}
	stop() {
		console.log("Spinner stop");
	}
}

/*****************************************************/
App.Req = Req;
App.ReqErr = ReqErr;
App.Util = Util;
App.B64 = B64;
App.AES = AES;
App.RSA = RSA;
App.defaultSpinner = new DefaultSpinner();
