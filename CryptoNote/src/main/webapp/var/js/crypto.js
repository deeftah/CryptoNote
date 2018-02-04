/*****************************************************/
class AES {
	constructor(webkey, key){
		this.webkey = webkey;
		this.key = key;
		this.ivbuf = new ArrayBuffer(16);
		this.iv = new Uint8Array(this.ivbuf);
	}
	
	static iv() { 
		if (!this.defaultVector)
			this.defaultVector = new Uint8Array([101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116]); 
		return this.defaultVector;
	}
	
	static async newAES(passphraseKey) {
		const uint8 = typeof passphraseKey === "string" ? Util.toUTF8(passphraseKey) : passphraseKey;
		const b = uint8.length == 32 ? uint8 : Util.sha256(uint8);
		let webkey = await crypto.subtle.importKey('raw', b, {name: "AES-CBC"}, false, ["encrypt", "decrypt"]);
		return new AES(webkey, b);
	}

	async encrypt(data, gzip, iv){
		if (data == null) return null;
		let uint8 = typeof data === "string" ? Util.toUTF8(data) : data;
		if (!uint8) uint8 = new Uint8Array(0);
		const deflated = gzip ? pako.deflate(uint8) : uint8;
		let rnd;
		if (!iv) {
			rnd = Util.random(1);
			for(let i = 0; i < 4; i++) this.iv.set(rnd, i * 4);
		} else 
			this.iv = iv;
		let result = await crypto.subtle.encrypt({name: "AES-CBC", iv:this.iv}, this.webkey, deflated);
		const r8 = new Uint8Array(result);
		if (iv) return r8;
		const buf = new ArrayBuffer(r8.length + 4)
		let u8 = new Uint8Array(buf);
		u8.set(r8, 4);
		u8.set(rnd);
		return u8;
	}

	async decrypt(encoded, gzip, iv){
		if (encoded == null) return null;
		if (!iv)
			for(let i = 0; i < 16; i++) this.iv[i] = encoded[i % 4];
		else
			this.iv = iv;
		const cr = iv ? encoded : encoded.slice(4);
	    let result = await crypto.subtle.decrypt({name: "AES-CBC", iv:this.iv}, this.webkey, cr);
    	const bin = new Uint8Array(result);
        return gzip ? pako.inflate(bin) : bin;
	}

	/*
	 * photo est un base64 du cryptage d'une URL d'image par une clé AES
	 */
	async decodeImage(photoB64) {
		let photob = await this.decrypt(B64.decode(photoB64));
		const ph = Util.fromUTF8(photob);
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
	static async encrypter(pem){
		const key = this.pemToUint8(pem);
		const rsa = new RSA();
		rsa.e = await crypto.subtle.importKey("spki", key, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["encrypt"]);
		return rsa;
	}

	static async decrypter(pem){
		const key = this.pemToUint8(pem);
		const rsa = new RSA();
		rsa.d = await crypto.subtle.importKey("pkcs8", key, {name:"RSA-OAEP", hash:{name:"SHA-1"}}, true, ["decrypt"]);
		return rsa;
	}

	static async verifier(pem){
		const key = this.pemToUint8(pem);
		const rsa = new RSA();
		rsa.v = await crypto.subtle.importKey("spki", key, {name:"RSASSA-PKCS1-v1_5", hash:{name:"SHA-256"}}, true, ["verify"]);
		return rsa;
	}

	static async signer(pem){
		const key = this.pemToUint8(pem);
		const rsa = new RSA();
		rsa.s = await crypto.subtle.importKey("pkcs8", key, {name:"RSASSA-PKCS1-v1_5", hash:{name:"SHA-256"}}, true, ["sign"]);
		return rsa;
	}

	static pemToUint8(pem){
		const a = pem.split("\n");
		const r = [];
		for(let i = 0, l = null; l = a[i]; i++){
			let x = l.trim();
			if (x && !x.startsWith("---"))
				r.push(x);
		}
		return B64.decode(r.join(""));
	}
	
	static abToPem(ab, pub) { // ArrayBuffer
		const s = B64.encode(new Uint8Array(ab), true);
		let i = 0;
		let x = pub ? "PUBLIC" : "PRIVATE";
		let a = ["-----BEGIN " + x + " KEY-----"];
		while (i < s.length) {
			a.push(s.substring(i, i + 64));
			i += 64;
		}
		a.push("-----END " + x + " KEY-----");
		return a.join("\n");
	}

	static async newEDKeyPair() {
		let key = await crypto.subtle.generateKey(this.rsaObj, true, ["encrypt", "decrypt"]);
		let jpriv = await crypto.subtle.exportKey("pkcs8", key.privateKey);
		let jpub = await crypto.subtle.exportKey("spki", key.publicKey);
		return {priv:this.abToPem(jpriv, false), pub:this.abToPem(jpub, true)};
	}

	static async newSVKeyPair() {
		let key = await crypto.subtle.generateKey(this.rsassaObj, true, ["sign", "verify"]);
		let jpriv = await crypto.subtle.exportKey("pkcs8", key.privateKey);
		let jpub = await crypto.subtle.exportKey("spki", key.publicKey);
		return {priv:this.abToPem(jpriv, false), pub:this.abToPem(jpub, true)};
	}
	
	static get rsaObj() {
		// le hash DOIT être SHA-1 pour interaction avec java (le seul qu'il accepte d'échanger)
		return {name: "RSA-OAEP", modulusLength: 2048, publicExponent: new Uint8Array([0x01, 0x00, 0x01]), hash: {name: "SHA-1"}};
	}

	static get rsassaObj() { 
		return {name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: {name: "SHA-256"}};
	}
	
	// http://stackoverflow.com/questions/33043091/public-key-encryption-in-microsoft-edge
	// hash: { name: "SHA-1" } inutile mais fait marcher edge !!!
	async encrypt(data) {
		const uint8 = typeof data === "string" ? Util.toUTF8(data) : data;
		let result = await crypto.subtle.encrypt({name: "RSA-OAEP", hash: { name: "SHA-1" }}, this.e, uint8);
		return new Uint8Array(result);
	}
	
	async decrypt(data) {
		const uint8 = typeof data === "string" ? B64.decode(data) : data;
		let result = await crypto.subtle.decrypt({name: "RSA-OAEP", hash: { name: "SHA-1" }}, this.d, uint8);
	    return new Uint8Array(result);
	}
	
	async sign(data) {
		const uint8 = typeof data === "string" ? Util.toUTF8(data) : data;
		let result = await crypto.subtle.sign({name: "RSASSA-PKCS1-v1_5"}, this.s, uint8);
		return new Uint8Array(result);
	}
	
	async verify(signature, data) {
		const sig8 = typeof signature === "string" ? B64.decode(signature) : signature;
		const uint8 = typeof data === "string" ? Util.toUTF8(data) : data;
		let result = await crypto.subtle.verify({name: "RSASSA-PKCS1-v1_5"}, this.v, sig8, uint8);
	    return result;
	}

}

class TestCrypto {
	static async test() {
		const ivHex = App.Util.uint8ToHex(App.AES.iv());
		console.log("ivHex : " + ivHex);
		
		let x = await App.Util.sha256("toto");
		console.log(App.B64.encode(x, true));
		let y = App.Util.toUTF8("toto");
		x = await App.Util.sha256(y);
		console.log("SHA-256: " + App.B64.encode(x, true));
		
		x = App.Util.bcrypt("toto est beau");
		console.log("BCrypt: " + x + " / " + x.length);
		const aeskeyBin = App.Util.bcrypt2u32(x);
		let aeskey = App.B64.encode(aeskeyBin);
		
		console.log("AESKEY: " + aeskey);
		let aes1 = await App.AES.newAES(App.Util.bcrypt2u32(x));
		
		y = await aes1.encrypt("toto est beau");
		console.log("AES crypted: " + App.B64.encode(y, true)  + " / " + y.length);
		console.log("key : " + App.Util.uint8ToHex(aes1.key));
		console.log("ivHex : " + App.Util.uint8ToHex(aes1.iv));
		x = await aes1.decrypt(y);
		console.log("AES decrypted: " + App.Util.fromUTF8(x));
		console.log("key : " + App.Util.uint8ToHex(aes1.key));
		console.log("ivHex : " + App.Util.uint8ToHex(aes1.iv));
		
		let aes2 = await App.AES.newAES(B64.decode(aeskey));
		y = await aes2.encrypt("toto est beau", false, App.AES.iv());
		console.log("AES crypted: " + App.B64.encode(y, true)  + " / " + y.length);
		console.log("key : " + App.Util.uint8ToHex(aes2.key));
		console.log("ivHex : " + App.Util.uint8ToHex(aes2.iv));
		
		let z1 = JSON.stringify(App.zres);
		y = await aes1.encrypt(z1, true);
		console.log("AES crypted z: " + y.length + " / " + z1.length);
		x = await aes1.decrypt(y, true);
		let z2 = App.Util.fromUTF8(x);
		console.log("AES decrypted z: " + App.Util.bytesEqual(z1, z2));
		
		let kp = await App.RSA.newEDKeyPair();
		console.log(kp.pub);
		console.log(kp.priv);
		let pub = await App.RSA.encrypter(kp.pub);
		let priv = await App.RSA.decrypter(kp.priv);

		y = await pub.encrypt("toto est beau");
		console.log("RSA crypted: " + App.B64.encode(y)  + " / " + y.length);

		x = await priv.decrypt(y);
		console.log("RSA decrypted: " + App.Util.fromUTF8(x));

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
		let s = App.Util.fromUTF8(x);
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
	}

}
App.RSA = RSA;
App.AES = AES;
App.TestCrypto = TestCrypto;
