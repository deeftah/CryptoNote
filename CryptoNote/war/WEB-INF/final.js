// Appel juste en fin de <head> après customisation par z/custom.js
if (SRV.mode){
	if (navigator.serviceWorker) {
		GEN.regSW(); 
	} else 
		GEN.updCache()
} else
	GEN.reloadApp(SRV.build + "_" + SRV.nsbuild);
