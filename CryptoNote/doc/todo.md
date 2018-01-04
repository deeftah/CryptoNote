### js-sha256 Download
[Compress](https://raw.github.com/emn178/js-sha256/master/build/sha256.min.js)  
[Uncompress](https://raw.github.com/emn178/js-sha256/master/src/sha256.js)

### bcrypt.js Downloads
[Distributions](https://github.com/dcodeIO/bcrypt.js/tree/master/dist)  
[ZIP-Archive](https://github.com/dcodeIO/bcrypt.js/archive/master.zip)

### text-encoding 
https://github.com/inexorabletash/text-encoding  

### showdown.js
https://github.com/showdownjs/showdown
Enlever la dernière ligne qui référence la map.

import / export
2. From version 54: this feature is behind the dom.moduleScripts.enabled preference. To change preferences in Firefox, visit about:config.

Debug IOS/Safari
Installation de remotedebug :
https://github.com/RemoteDebug/remotedebug-ios-webkit-adapter

Dans un terminal : remotedebug_ios_webkit_adapter --port=9000
Autoriser le web inspector dans Safari
Dans Chrome : chrome://inspect/#devices
Network target : localhost:9000
ça devrait apparaître en dessous

Test crypto à faire sha256 OK.
reqerr-panel à compléter / tester

Doc serveur à ajuster
Test / exemple


styles/z/z/post-it.png

Une tâche a un résultat : c'est un petit objet *report* qui synthétise le niveau d'avancement de la tâche, soit à son dernier point de reprise, soit à sa fin.

La phase work() d'une tâche se finit sous deux options :
- fin de la tâche : 
    - l'objet résultat est le report synthtétique de la réalisation;
    - l'objet param est mis à null.
    - 1) la TaskInfo a une date-heure startAt à null et une date-heure purgeAt qui indique quand le TaskInfo devra être purgé.
    - 2) la TaskInfo est purgée immédiatement, son report n'est plus accessible.
- point de reprise :
    - l'objet résultat est un report synthétique sur le niveau d'avancement.
    - l'objet param est enregistré dans TaskInfo : il a été mis à jour par work() de manière à ne plus contenir 
    que les paramètres nécessaires pour l'étape suivante, le cas échant avec simplement une modification du nombre d'items restant à traiter.
    - la requête valide ses mises à jour et la mise à jour de param et report. Elle boucle sur une phase suivante work().
    - pour le Queue Manager c'est toujours la même tâche qui est en exécution.
    - en cas de sortie en exception, le param contient tout ce qu'il faut pour redémarrer le travail au dernier point de reprise.

En cas de sortie en exception :
- le code de l'exception exc est enregistré.
- son détail complet (et stack) est enregsitré en detail.
- son indice de retry est incrémenté. Cet indice est remis à 0 à chaque point de reprise sorti en succès, exc et detail étant remis à null.
