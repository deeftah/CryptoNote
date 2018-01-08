App.setAll("fr", {

"off-1":"interruption temporaire pour installation d''une nouvelle version",
"X1":"Exception inattendue dans le navigateur pour l''URL [{0}] - Cause : [{1}]",
"TIMEOUT":"Dépassement du temps maximum d'''attente dans le navigateur {1}ms pour l''URL {0}",
"INTERRUPTED":"Fin d''attente dans le navigateur par clic au bout de {1}s pour l'URL {0}",
"BJSONRESP":"Erreur de syntaxe dans la réponse du serveur pour l''URL [{0}] - Cause : [{1}] ",
"XHTTP":"Réponse d''erreur inattendue du serveur pour l''URL [{0}]. Status-HTTP:{1} Message:{2}",
"XSEND":"Exception inattendue lors de l''envoi de la requête au serveur pour l''URL [{0}] - Cause : [{1}]",
"DBUILD":"Version d''application {0} incompatible avec celle du serveur {1}",

"testsrv":"Test d''accès au serveur",
"srvko":"Serveur non joignable : {0}",
"srvok":"Serveur joignable : {0}",
"reqStarted":"Envoi au serveur",
"reqRec":"{0} reçus de {1}",
"sb_npa": "Ne plus attendre",
"sb_texte": "Interrompre correspond à renoncer à l'action engagée qui sera traitée comme un échec. L'autre option est d'avoir plus de patience.",
"sb_att": "Attendre encore",
"sb_int": "Interrompre",
"er_retry": "Ré-essayer",
"er_resume": "Renoncer à l'opération",
"er_reload": "Recharger l'application",
"er_quit": "Quitter l'application",
"start_mode0": "Mode incognito (rien en local, tout cloud)",
"start_mode1": "Mode synchronisé (local / cloud)",
"start_mode2": "Mode avion (tout local / aucun accès réseau)",
"start_mode0off": "Mode incognito (rien en local, tout cloud). Serveur NON joignable, actions très limitées",
"start_mode1off": "Mode synchronisé (local / cloud). Serveur NON joignable, actions très limitées",
"wait_home": "Chargement en cours ...",
"xping_1": "Le serveur n''est pas joignable (vérifier la connexion Internet).\nAprès appui sur OK un rechargement sera tenté.",
"xping_2": "Le serveur n''est pas joignable (vérifier la connexion Internet).\nOK : pour tenter un rechargement.\nAnnuler : pour passer en mode avion.",


"500URLMF1":"URL [{0}] mal formée : https://site.com/org/... attendue",
"500URLMF1cp":"URL [{0}] mal formée : https://site.com/app/org/... attendue",
"500URLMF2":"URL [{0}] mal formée : https://site.com/org/... attendue. [{1}] n''est ni une organisation ni un queue manager répertorié",
"500URLMF2cp":"URL [{0}] mal formée : https://site.com/app/org/... attendue [{1}] n''est ni une organisation ni un queue manager répertorié",
"500HQM":"Ce serveur n''héberge pas le Queue Manager [{0}]",
"404SWJS":"Ressource /var/sw.js non trouvée",
"404HOME3":"Cette page d''accueil [{0}] n''est pas utilisable offline (mode avion)",
"404HOME2":"Cette page d''accueil [{0}] n''est utilisable qu''en mode incognito (cloud seulement)",
"404HOME1":"L''application n''a pas de page d''accueil [{0}]",
"404IDX0":"Ressource index.html non trouvée",
"404IDX1":"Ressource index.html mal formée : ligne 1 du type <!DOCTYPE html><html><head><base href=''...'' /> requise",
"404IDX2":"Ressource index.html mal formée : une ligne commençant par </head> est requise pour isoler la section <head>",
"404IDX3":"Ressource index.html mal formée : une section <head> non vide est requise",
"404IDX4":"Ressource index.html mal formée : une section <body> et le tag de fin </html> sont requis",


"SADMINOP":"Opération requérant une clé d''administrateur",

"XRESSOURCECLASS":"Classe [{0}] non trouvée ou sans constructeur par défaut",

"BNAMESPACENO":"Aucune organisation (namespace) déclérée à la configuration",
"XMAILERCFG1":"Deux mailers déclarés ''par défaut''",
"XMAILERCFG2":"Aucun mailer déclaré ''par défaut''",
"XMAILERCFG3":"Password requis pour ''mailServer''",

"BMAILPARAM":"Paramètres du mail incorrects : [{0}]",
"BMAILNOMAILER":"Nom de mailer non déclaré : [{0}]",
"XMAILHTTP":"Echec d''envoi (HTTP) du mail : [{0}]",
"XMAILJAVAMAIL":"Echec d''envoi (JAVAMAIL) du mail : [{0}]",
"BMAILTYPE":"Paramètres du mail incorrects. url : [{0}]",

"XSQL0":"Echec SQL - opération:[{0}] méthode:[{1}] namespace:[{2}] sql:[{3}]\nmessage:[{4}]",
"XSQLDS":"Datasource pour la base [{0}] non initialisable.\nmessage:[{1}]",

"X0":"Exception inattendue sur le serveur [cause : {0}]",
"TMAXTIME":"Dépassement du temps maximum d'exécution sur le serveur {1}ms pour l''opération {0}",
"OFF":"Application hors service : {0}",
"BJSONFILTER":"Erreur de syntaxe sur le filtre json [{0}] du document [{1}]",

"CONTENTION1":"Opération [{0}] : contention sur la base de données. Validation impossible : locks:[{1}]. Réessayer plus tard",
"CONTENTION2":"Opération [{0}] : contention sur la base de données. Document [{1}] détruit depuis le début de l''opération. Réessayer plus tard",
"CONTENTION3":"Opération [{0}] : contention sur la base de données. Document [{1}] modifié en base depuis le début de l''opération:. Réessayer plus tard",

"ANSBADNS":"Opération uniquement accessible depuis l''espace de noms [{0}] (pas depuis [{1}])",
"ANSUNKNOWN":"Espace de noms [{0}] non reconnu",
"ANSZRES":"Ressource de nom [{0}] sans valeur par défaut dans l'application",
"ANSEMPTY":"Pas de fichier joint",

"SQMKEY":"Clé d''accès au Queue Manager non reconnue",
"STASKKEY":"Clé de lancement de tâche non reconnue",
"BQMJSONTI":"Erreur de syntaxe sur TaskInfo json :[{0}]",
"BQMOP":"Queue Manager : opération [{0}] inconnue",

"BDOCUMENTCACHE0":"Demande d''un document d''identifiant null",

"BEXECSYNCSPARSE":"Opération [{0}] : syncs non parsable",

"BBADOPERATION2":"Opération [{0}] : classe n''étendant pas Operation",
"BBADOPERATION0":"Opération [{0}] inconnue",
"BBADOPERATION1":"Opération [{0}] : param non parsable",
"XGZIPEXC":"Opération [{0}] : gzip du résultat en erreur",
"XUNGZIPEXC":"Opération [{0}] : ungzip de l''attachement [{0}] en erreur",

"XFILE0":"Erreur I/O sur [{0}]",

"BDOCUMENTCLASS0":"Tentative d''enrigistrement d''une classe de Document vide",
"BDOCUMENTCLASS1":"Tentative d''enrigistrement d''une classe de Document [{0}] n''étendant pas Document",
"BDOCUMENTCLASS2":"Classe de Document non enregistrée [{0}]",
"BDOCUMENTCLASS3":"Classe d''Item [{0}] non statique pour le document [{1}]",
"BDOCUMENTCLASS4":"Document [{0}], Item [{1}] : champ indexé [{2}] de classe non autorisée",
"BDOCUMENTCLASS5":"Tentative d''enrigistrement de deux classes de Document ayant même nom [{0}] [{1}]",
"BDOCUMENTCLASS6":"Classe de Document non instantiable [{0}]",
"BDOCUMENTCLASS7":"Classe de IsyncFilter de Document non instantiable [{0}]",
"BDOCUMENTCLASS9":"Classe de document [{0}] : constructeur non accessible",
"BDOCUMENTCLASS10":"Classe d''item [{0}] : constructeur non accessible",
"BDOCUMENTITEM":"Classe d''item [{0}] non instantiable",

"BITEMDETACHED":"Méthode non applicable à un item non attaché à un document (class {0})",
"BITEMATTACHED":"Méthode non applicable à un item attaché à un document (class {0})",
"BITEMCLASS":"Classe d''item [{0}] inconnue",
"BITEMPRAW":"Méthode ne s'appliquant ni aux items P ni aux raw : classe [{0}]",
"BITEMCLASSCOL":"Classe d''item collection [{0}] inconnue",
"BDOCUMENTRO":"Méthode [{1}].[{0}] interdite sur un document [{2}] en lecture seule",
"BDOCUMENTDEL":"Méthode [{1}].[{0}] interdite sur un document [{2}] détruit",
"BEMPTYBLOB":"BlobStore sans blob pour [{0}] [{1}]",
"BMIMEBLOB":"BlobStore sans mime pour [{0}] [{1}]",
"BKEYBLOB":"BlobStore sans nom pour [{0}]",

"RIEN":"rien"
});