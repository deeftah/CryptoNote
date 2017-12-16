'use strict';

if (!navigator.serviceWorker && (window.location.pathname.endsWith(".sync") || window.location.pathname.endsWith(".local")))
	window.location = window.location.pathname + "2" + window.location.search + window.location.hash;

//Pour Safari / IOS !!!
if (window.crypto && !window.crypto.subtle && window.crypto.webkitSubtle)
    window.crypto.subtle = window.crypto.webkitSubtle;

const GEN = { libs:{} }
const APP = { }
const SRV = { }

SRV.build = "0.0";
SRV.contextpath = "/";
SRV.pathname = window.location.pathname;
SRV.hash = window.location.hash;
SRV.search = window.location.search;
SRV.ext = SRV.pathname.substring(SRV.pathname.lastIndexOf(".") + 1);
SRV.mode = SRV.ext == "sync" || SRV.ext == "sync2" ? 1 : (SRV.ext == "local" || SRV.ext == "local2" ? 2 : 0);
SRV.origin = window.location.origin;

window.Polymer = { dom:'shadow'};
