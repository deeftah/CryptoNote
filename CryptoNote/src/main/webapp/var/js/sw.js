
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
			if (TRACEON2) console.log("Install avant addAll " + CACHENAME);
			return cache.addAll(RESSOURCES);
		}).catch(error => {
			if (TRACEON) console.log("Install adAll KO " + CACHENAME + " - " + error.message);
		})
	);
});

this.addEventListener('activate', function(event) {
	event.waitUntil(
		caches.keys()
		.then(cacheNames => {
				if (TRACEON2) console.log("Activate / cleaning " + CACHENAME);
				return Promise.all(
					cacheNames.map(cacheName => {
						if (cacheName.startsWith(PREFIX) && cacheName != CACHENAME) {
							if (TRACEON) console.log("Activate delete " + cacheName + "/" + CACHENAME);
							return caches.delete(cacheName);
						}
					})
				);
		}).catch(error => {
			if (TRACEON) console.log("Activate KO " + CACHENAME + " - " + error.message);
		})
	);
});

this.addEventListener('fetch', event => {
	event.respondWith(
			caches.match(event.request.url)
			.then(response => {
				return response || fetch(event.request.url);
			})
		);
	});


this.addEventListener('message', function(event) {
	let cmd = event.data.command;
	if (cmd == "getBuild") {
		if (TRACEON2) console.log('(message ' + BUILD + ') cmd:' + cmd );
	    event.ports[0].postMessage({ error: null, build: BUILD});
	}
});
