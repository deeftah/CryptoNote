'use strict';

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
	
//static async ios() {
//	if (App.IOS == null) {
//		try {
//			let key = await crypto.subtle.generateKey(RSA.rsaObj, true, ["encrypt", "decrypt"]);
//			let jwk = await crypto.subtle.exportKey("jwk", key.privateKey);
//			const x = JSON.stringify(jwk);
//			const y = JSON.parse(x);
//			let result2 = await crypto.subtle.importKey("jwk", y, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["decrypt"]);
//			App.IOS = false;
//		} catch (err) {
//			App.IOS = true;
//		}
//	}
//	return App.IOS;
//}

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


GEN.Base64 = class Base64 {
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

		return GEN.Crypt.decoder.decode(u8);
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
	
	static urlFromUint8(uint8, contentType){
		return "data:" + contentType + ";base64," + this.encode(uint8);
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

};

GEN.AES = class AES {
	static newAES(passphraseKey) {
		return new Promise((resolve, reject) => {
			try {
			const uint8 = typeof passphraseKey === "string" ? GEN.Crypt.stringToUint8(passphraseKey) : passphraseKey;
			if (uint8.length == 32) {
				crypto.subtle.importKey('raw', uint8, {name: "AES-CBC"}, false, ["encrypt", "decrypt"])
				.then(webKey => {
					resolve(new GEN.AES(webKey, uint8));
				});			
			} else {
				let u32;
			    crypto.subtle.digest({name: "SHA-256"}, uint8)
			    .then(sha => {
			    	u32 = new Uint8Array(sha);
					return crypto.subtle.importKey('raw', u32, {name: "AES-CBC"}, false, ["encrypt", "decrypt"]);
			    }).then(webKey => {
					resolve(new GEN.AES(webKey, u32));
			    });
			}
			} catch (err) {
				reject(err);
			}
		});
	}

	constructor(key, uint8){
		this.key = key;
		this.uint8 = uint8;
	}
	
	encode(data, gzip){
		return new Promise((resolve, reject) => {
			try {
			if (data == null) resolve(null);
			let uint8 = typeof data === "string" ? 
				GEN.Crypt.stringToUint8(data ? data : "") : data;
			if (!uint8) 
				uint8 = new Uint8Array(0);
			const deflated = gzip ? pako.deflate(uint8) : uint8;
	   	    crypto.subtle.encrypt({name: "AES-CBC", iv:GEN.Crypt.defaultVector}, this.key, deflated)
			.then(result => {
				resolve(new Uint8Array(result));
			}).catch(e => {
		    	console.log(e.message);
		    	reject(e);
		    });
			} catch (err) {
				reject(err);
			}
		});		
	}
	
	decode(encoded, gzip){
		return new Promise((resolve, reject) => {
			try {
			if (encoded == null) resolve(null);
		    crypto.subtle.decrypt({name: "AES-CBC", iv:GEN.Crypt.defaultVector}, this.key, encoded)
	 	    .then(result => {
	 	    	const bin = new Uint8Array(result);
	            resolve(gzip ? pako.inflate(bin) : bin);
	 	    }).catch(e => {
		    	console.log(e.message);
		    	reject(e);
		    });
			} catch (err) {
				reject(err);
			}
		});
	}
	/*
	 * photo est un base64 du cryptage d'une URL d'image par une clé AES
	 */
	decodeImage(photoB64) {
		try {
			return new Promise((resolve, reject) => {
				this.decode(GEN.Base64.decode(photoB64))
				.then(photob =>{
					const ph = GEN.Crypt.uint8ToString(photob);
					if (!ph) {
						resolve(null);
						return;
					}
					if (!ph.startsWith("data:image/")) {
						resolve(null);
						return;
					}
					const i = ph.indexOf(";");
					if (i == -1) {
						resolve(null);
						return;
					}
					const j = ph.indexOf(",");
					if (j != i + 7) {
						resolve(null);
						return;
					}
					if (ph.substring(i + 1, j) != "base64") {
						resolve(null);
						return;
					}
					if (!GEN.Base64.isBase64NN(ph.substring(j + 1))) {
						resolve(null);
						return;
					}
					resolve(ph);
				}).catch(err => {
					this.valeur("photo", false);
					resolve();
				});
			});
		} catch (err) {
			resolve(null);
		}
	}

};
	
GEN.Crypt = class Crypt {	
	static get decoder() { return new TextDecoder("utf-8"); }
	
	static get encoder() { return new TextEncoder("utf-8"); }
	
	static uint8ToString(uint8) { return new TextDecoder("utf-8").decode(uint8);}

	static stringToUint8(myString) { return new TextEncoder("utf-8").encode(myString);}

	static asciiToUint8(ascii, len) { 
		if (!len || len < 0) len = 32;
		if (!ascii) ascii = "";
		const uint8 = new Uint8Array(len);
		for(let i = 0; i < ascii.length && i < len; i++)
			uint8[i] = ascii.charCodeAt(i);
		return uint8;
	}

	static uint8ToHex(uint8){
		var hex = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'];
		var s = '';
		for (var i = 0; i < uint8.length; i++) {
		  var code = uint8[i];
		  s += hex[code >>> 4];
		  s += hex[code & 0x0F];
		}
		return s;
	}
	
	static hexToUint8(hex) {
		var uint8 = new Uint8Array(hex.length / 2);
		for(var i = 0, j = 0; i < hex.length - 1; i += 2, j++)
			uint8[j] = parseInt(hex.substr(i, 2), 16);
		return uint8;
	}

	static get salt() { return "$2a$10$kBdas.cIGiNW/ziEj/pZD."; }

	static bcrypt(data) {
		return dcodeIO.bcrypt.hashSync(data, this.salt).substring(29).replace(/\./g, '-').replace(/\//g, '_');
	}

	static bcryptCompare(data, hash) {
		let h = hash.replace(/-/g, '.').replace(/_/g, '/');
		return dcodeIO.bcrypt.compareSync(data, this.salt + h);
	}

	static sha256(data) {
		const sha = sha256.create();
		return new Uint8Array(sha.update(data).array());
	}

	static get defaultVector() { 
		if (!this.defaultVectorK)
			this.defaultVectorK = new Uint8Array([101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116]); 
		return this.defaultVectorK;
	}

	static uint8Equal(a, b){
		if (!a && !b) return true;
		if ((a && !b) || (!a && b) || (a.length != b.length)) return false;
		for(let i = 0; i< a.length; i++)
			if (a[i] != b[i]) return false;
		return true;
	}
		
	static hashOf(s) { return GEN.Base64.intToBase64(this.checksum(s))};
	
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

	static UINT32ToUint8Array(u) {
	    const b = [0, 0, 0, 0];
	    b[0] = u._low & 0xff;
	    b[1] = u._low >> 8;
	    b[2] = u._high & 0xff;
	    b[3] = u._high >> 8;
	    return new Uint8Array(b);
	}

	static UINT64ToUint8Array(u) {
	    const b = [0, 0, 0, 0, 0, 0, 0, 0];
	    b[0] = u._a00 & 0xff;
	    b[1] = u._a00 >> 8;
	    b[2] = u._a16 & 0xff;
	    b[3] = u._a16 >> 8;
	    b[4] = u._a32 & 0xff;
	    b[5] = u._a32 >> 8;
	    b[6] = u._a48 & 0xff;
	    b[7] = u._a48 >> 8;
	    return new Uint8Array(b);
	}

	static randomNUint8(n){
		const array = new Uint8Array(n);
		window.crypto.getRandomValues(array);
		return array;
	}

	static randomShortId(){ // String de 8c en base64 special
		const array = new Uint8Array(6);
		window.crypto.getRandomValues(array);
		return GEN.Base64.encode(array, true);
	}

	static randomLongId(){ // String de 12c en base64 special
		const array = new Uint8Array(9);
		window.crypto.getRandomValues(array);
		return GEN.Base64.encode(array, true);
	}

//	static get defaultIterations() { return 500; }
//	static get defaultSalt() { 
//		if (!this.defaultSaltK)
//			this.defaultSaltK = this.numberTo16(1515178919181945); 
//		return this.defaultSaltK;
//	}	
//
//	static get defPBKDF2() { 
//		if (!this.defPBKF2K)
//			this.defPBKF2K = {"name": "PBKDF2", salt:this.defaultSalt, iterations:this.defaultIterations, hash:{name:"SHA-256"} };
//		return this.defPBKF2K;
//	}
//
//	static pbkdf2(passphraseKey, short) {
//		return new Promise((resolve, reject) => {
//			try {
//			const uint8 = typeof passphraseKey === "string" ? this.stringToUint8(passphraseKey) : passphraseKey;
//			if (GEN.Crypt.hasPBKDF2) {
//				try {
//					crypto.subtle.importKey('raw', uint8, {name: 'PBKDF2'}, false, ['deriveBits'])
//					.then(key => {
//						crypto.subtle.deriveBits(this.defPBKDF2, key, short ? 128 : 256)
//						.then(bits => {
//							const x1 = new Uint8Array(bits);
//						    resolve(x1);
//						}, err => {
//							// {let m; if (m = GEN.log("PBK1 " + err.message)) console.error(m);}
//							GEN.Crypt.hasPBKDF2 = false;
//							resolve(sha256.pbkdf2(uint8, this.defaultSalt, this.defaultIterations, short ? 16 : 32));
//						});
//					}, err => {
//						// {let m; if (m = GEN.log("PBK2 " + err.message)) console.error(m);}
//						GEN.Crypt.hasPBKDF2 = false;
//						resolve(sha256.pbkdf2(uint8, this.defaultSalt, this.defaultIterations, short ? 16 : 32));
//					});
//				} catch(err) { // webkit lève une exception hors du promise
//					// {let m; if (m = GEN.log("PBK3 " + err.message)) console.error(m);}
//					GEN.Crypt.hasPBKDF2 = false;
//					resolve(sha256.pbkdf2(uint8, this.defaultSalt, this.defaultIterations, short ? 16 : 32));					
//				}
//			} else {
//				resolve(sha256.pbkdf2(uint8, this.defaultSalt, this.defaultIterations, short ? 16 : 32));				
//			};
//			} catch (err) {
//				reject(err);
//			}
//		});
//	}
//	static sha256(data) {
//		return new Promise((resolve, reject) => {
//			try {
//			const uint8 = typeof data === "string" ? GEN.Crypt.stringToUint8(data) : data;
//		    return crypto.subtle.digest({name: "SHA-256"}, uint8)
//		    .then(result => {
//		    	resolve(new Uint8Array(result));
//		    });
//			} catch (err) {
//				reject(err);
//			}
//		});
//	}
//	
		
};

GEN.RSA = class RSA {
	constructor() {
	}
	
	//	rsassaObj : {name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: {name: "SHA-256"}},
	// http://stackoverflow.com/questions/33043091/public-key-encryption-in-microsoft-edge
	// hash: { name: "SHA-1" } inutile mais fait marcher edge !!!
	
	encode(data) {
		return new Promise((resolve, reject) => {
			try {
			var uint8 = typeof data === "string" ? GEN.Crypt.stringToUint8(data ? data : "") : data;
		    crypto.subtle.encrypt({name: "RSA-OAEP", hash: { name: "SHA-1" }}, this.pub, uint8)
		    .then((result) => {
		    	resolve(new Uint8Array(result));
		    }).catch((e) => {
		    	console.log(e.message);
		    	reject(e);
			});
			} catch (err) {
				reject(err);
			}
		});
	}
	
	decode(data) {
		return new Promise((resolve, reject) => {
			try {
			var uint8 = typeof data === "string" ? GEN.Base64.decode(data) : data;
		    crypto.subtle.decrypt({name: "RSA-OAEP", hash: { name: "SHA-1" }}, this.priv, uint8)
		    .then((result) => {
		    	resolve(new Uint8Array(result));
		    }).catch((e) => {
		    	console.log(e.message);
		    	reject(e);
			});
			} catch (err) {
				reject(err);
			}
		});
	}
	
	// le hash DOIT être SHA-1 pour interaction avec java (le seul qu'il accepte d'échanger)
	static get rsaObj() {
		return {name: "RSA-OAEP", modulusLength: 2048, publicExponent: new Uint8Array([0x01, 0x00, 0x01]), hash: {name: "SHA-1"}};
	}
	
	static newRSAGen() {
		return new Promise((resolve, reject) => {
			try {
				const rsa = new GEN.RSA();
				crypto.subtle.generateKey(this.rsaObj, true, ["encrypt", "decrypt"])
				.then(key => {
					rsa.priv = key.privateKey;
					rsa.pub = key.publicKey;
					try {
						crypto.subtle.exportKey("jwk", rsa.priv)
						.then(jpriv => {
							rsa.jwkpriv = JSON.stringify(jpriv);
							crypto.subtle.exportKey("jwk", rsa.pub)
							.then(jpub => {
								rsa.jwkpub = JSON.stringify(jpub);
								resolve(rsa);
							}, e => {
								{let m; if (m = GEN.error("RSA7 " + GEN.e2a.push(e).join("\n"))) console.error(m);}		
								reject(e);
							});
						}, e => {
							{let m; if (m = GEN.error("RSA6 " + GEN.e2a.push(e).join("\n"))) console.error(m);}		
							reject(e);
						});
					} catch(e) {
						{let m; if (m = GEN.error("RSA5 " + GEN.e2a.push(e).join("\n"))) console.error(m);}		
						reject(e);				
					}
				}, e => {
					{let m; if (m = GEN.error("RSA4 " + GEN.e2a.push(e).join("\n"))) console.error(m);}		
					reject(e);
				});
			} catch(e) {
				{let m; if (m = GEN.error("RSA3 " + GEN.e2a.push(e).join("\n"))) console.error(m);}		
				reject(e);				
			}
		});
	}
	
	static compareRSAPub(p1, p2, ch){ // p2 PEUT être null
		return new Promise((resolve, reject) => {
			try {
			let chec64;
			p1.encode(ch)
			.then(x => {
				chec64 = GEN.Base64.encode(x, true);
				if (!p2) {
					resolve(chec64);
					return;
				}
				return p2.encode(ch);
			}).then(y => {
				y64 = GEN.Base64.encode(y, true);
				if (y64 == chec64)
					resolve(chec64);
				else
					reject("pub1 != pub2");
			}).catch(err => {
				reject("pub erreur encode");
			});
			} catch (err) {
				reject(err);
			}
		});		
	}

	static compareRSAPriv(p1, p2, chec64, ch64){ // p2 PEUT être null
		return new Promise((resolve, reject) => {
			try {
			let chdc64;
			const chec = GEN.Base64.decode(chec64);
			p1.decode(chec)
			.then(x => {
				chdc64 = GEN.Base64.encode(x, true);
				if (chdc64 != ch64) {
					reject("paire dissociée pub1 priv1");
					return;
				}
				if (!p2) {
					resolve();
					return;
				}
				return p2.decode(chec);
			}).then(y => {
				const y64 = GEN.Base64.encode(y, true);
				if (y64 == chdc64)
					resolve();
				else
					reject("priv1 != priv2");
			}).catch(err => {
				reject("priv erreur decode");
			});
			} catch (err) {
				reject(err);
			}
		});		
	}

	static compareRSA(pub1, pub2, priv1, priv2, ch) {
		return new Promise((resolve, reject) => {
			try {
			this.compareRSAPub(pub1, pub2, ch)
			.then(chec64 => {
				const ch64 = GEN.Base64.encode(ch, true);
				return this.compareRSAPriv(priv1, priv2, chec64, ch64);
			}).then(()=> {
				resolve();
			}).catch(err => {
				reject(err);
			})
			} catch (err) {
				reject(err);
			}
		});		
	}
		
	static newRSAPriv(jwkJson) {
		return new Promise((resolve, reject) => {
			try {
				const key = GEN.Crypt.ios ? GEN.Crypt.stringToUint8(jwkJson) : JSON.parse(jwkJson);
				const rsa = new GEN.RSA();
				crypto.subtle.importKey("jwk", key, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["decrypt"])
				.then(result2 => {
					rsa.priv = result2;
					rsa.jwkpriv = jwkJson;
					resolve(rsa);
				}).catch(e => {
					{let m; if (m = GEN.log("RSA1 " + e.message)) console.error(m);}
					reject(e);
				});
			} catch(e) {
				{let m; if (m = GEN.log("RSA3 " + e.message)) console.error(m);}
				reject(e);				
			}
		});
	}

	static newRSAPub(jwkJson) {
		return new Promise((resolve, reject) => {
			try {
				const key = GEN.Crypt.ios ? GEN.Crypt.stringToUint8(jwkJson) : JSON.parse(jwkJson);
				const rsa = new GEN.RSA();
				crypto.subtle.importKey("jwk", key, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["encrypt"])
				.then((result2) => {
					rsa.pub = result2;
					rsa.jwkpub = jwkJson;
					resolve(rsa);
				}).catch((e) => {
					{let m; if (m = GEN.log("RSA2 " + e.message)) console.error(m);}
					reject(e);
				});
			} catch(e) {
				{let m; if (m = GEN.log("RSA3 " + e.message)) console.error(m);}
				reject(e);				
			}
		});
	}

}
