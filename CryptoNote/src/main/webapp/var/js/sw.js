/*
const CONTEXTPATH = "cp"; // ou ""
const BUILD = "6000"; 
const NS = "test"; 
const RESSOURCES = ["/cp/test/var6000/...", "/cp/test/home1.sync", "/cp/test/home1.local"];
*/

const PREFIX = CONTEXTPATH + "_" + NS + "_";
const CACHENAME =  PREFIX + BUILD;
const TRACEON = true;	// trace sur erreurs
const TRACEON2 = false; // trace sur actions normales
const NOTFOUND = "Page non trouvée";

this.addEventListener('install', function(event) {
	if (TRACEON) console.log("Install " + CACHENAME);
	event.waitUntil(
		caches.open(CACHENAME)
		.then(cache => {
			if (TRACEON2) console.log("Install addAll demandé ... " + CACHENAME);
			const res = cache.addAll(RESSOURCES);
			if (TRACEON2) console.log("Install addAll OK " + CACHENAME);
			return res;
		}).catch(error => {
			// Une des URLs citées est NOT FOUND, on ne sait pas laquelle
			if (TRACEON) console.log("Install addAll KO " + CACHENAME + " - " + error.message);
		})
	);
});

/*
 * Objectif : supprimer les caches de versions plus anciennes. 
 * Attention, il se peut qu'une encore plus récente puisse, éventuellement être présente
 */
this.addEventListener('activate', function(event) {
	const b1 = parseInt(BUILD, 10);
	event.waitUntil(
		caches.keys()
		.then(cacheNames => {
				if (TRACEON) console.log("Activate / cleaning " + CACHENAME);
				return Promise.all(
					cacheNames.map(cacheName => {
						if (cacheName.startsWith(PREFIX) && cacheName != CACHENAME) {
							const b = parseInt(cacheName.substring(PREFIX.length), 10);
							if (b < b1) {
								if (TRACEON) console.log("Activate delete " + cacheName + "/" + CACHENAME);
								return caches.delete(cacheName);
							}
						}
					})
				);
		}).catch(error => {
			if (TRACEON) console.log("Activate KO " + CACHENAME + " - " + error.message);
		})
	);
});

const fetchWithTimeout = function(req, TIME_OUT_MS) {
	let tim;
	return Promise.race([
		new Promise((resolve, reject) => {
			fetch(req)
			.then(response => {
				if (tim) clearTimeout(tim);
				resolve(response);
			}).catch(e => {
				if (tim) clearTimeout(tim);
				reject(e);
			})
		}),
		new Promise((resolve, reject) => {
			tim = setTimeout(() => {
				if (TRACEON) console.log("FETCH TIMEOUT - " + req.url);
				let myBlob = new Blob();
				let init = { "status" : 500 , "statusText" : "TIMEOUT " + TIME_OUT_MS + "ms" };
				let myResponse = new Response(myBlob,init);
				reject(myResponse);
			}, TIME_OUT_MS);
		})	
	]);
}

/*
 * Une home page .../home1.a (locale) a le même texte que son homologue normale .../home
 * DOIT laisser passer les opérations passées par GET : cp/ns/od/... cp/ns/op/... cp/ping cp/ns/ping    ns/od/.. ns/op/..  ping  ns/ping
 */
this.addEventListener('fetch', event => {
	let url = event.request.url;
	let j = url.lastIndexOf("?");
	let hasHash = false;
	if (j != -1) {
		url = url.substring(0, j);
		hasHash = true;
	}
	if (url.endsWith(".a")) url = url.substring(0, url.length - 2); // page "locale" (même texte que "normale")
	
	const i = url.indexOf("/", 10); // laisse passer https://...
	const x = !CONTEXTPATH ? url.substring(i + 1) : url.substring(i + 2 + CONTEXTPATH.length);
	const ping = x == "ping" || x ==  NS + "/ping";
	const op =  hasHash || ping || x.startsWith(NS + "/od/") || x.startsWith(NS + "/op/");
	
	const ch = event.request.headers.get("X-Custom-Header");
	let myOptions = {};
	if (ch)
		try { myOptions = JSON.parse(ch); } catch(err) { }
	const timeout = myOptions.timeout ? myOptions.timeout : 0;
		
	if (op && timeout) {
		event.respondWith(
			fetchWithTimeout(event.request, timeout)
			.then(response => {
				return response;
			}).catch(e => {
				return null;
			})
		);
	} else if (op) {
		event.respondWith(
			fetch(event.request)
		);
	} else {
		event.respondWith(
			caches.match(url)
			.then(response => {
				if (response && TRACEON2) console.log("fetch OK trouvée dans " + CACHENAME + " - " + url);
				if (!response && TRACEON) console.log("fetch KO non trouvée dans " + CACHENAME + " - " + url);
				return response;
			})
		);
	}
});

/*
 * ça eu servi !
 */
this.addEventListener('message', function(event) {
	let cmd = event.data.command;
	if (cmd == "getBuild") {
		if (TRACEON2) console.log('(message ' + BUILD + ') cmd:' + cmd );
	    event.ports[0].postMessage({ error: null, build: BUILD});
	}
});
