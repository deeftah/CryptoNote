### js-sha256 Download
[Compress](https://raw.github.com/emn178/js-sha256/master/build/sha256.min.js)  
[Uncompress](https://raw.github.com/emn178/js-sha256/master/src/sha256.js)

### bcrypt.js Downloads
[Distributions](https://github.com/dcodeIO/bcrypt.js/tree/master/dist)  
[ZIP-Archive](https://github.com/dcodeIO/bcrypt.js/archive/master.zip)

### text-encoding (pour Edge)
https://github.com/inexorabletash/text-encoding  

### showdown.js
https://github.com/showdownjs/showdown
Enlever la dernière ligne qui référence la map.

import / export
2. From version 54: this feature is behind the dom.moduleScripts.enabled preference. To change preferences in Firefox, visit about:config.

Debug IOS/Safari
Installation de remotedebug :
https://github.com/RemoteDebug/remotedebug-ios-webkit-adapter

Debug service workers sous Firefox about:debugging

Dans un terminal : remotedebug_ios_webkit_adapter --port=9000
Autoriser le web inspector dans Safari
Dans Chrome : chrome://inspect/#devices
Network target : localhost:9000
ça devrait apparaître en dessous

TaskQueue - opérations
- création task
- modif toStartAt / cron ...
- revisiter cron avec distinction date-heure réelle et date-heure fonctionnelle

Document DB - API : à revoir à partir de ExecContext
Suppression des items P, mais v2 continue d'exister. Gestion de corbeille pour les blobs à mentionner (+ API blob)

Opération POST : sans URL[]
Opération GET (sans auth) avec URL[] : TPU, URL avec clé de crypt etc.
Notion d'URL avec compte d'imputation ?
