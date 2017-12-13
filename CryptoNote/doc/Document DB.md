# Document DB

La base du stockage persistant est de considéré des documents ayant une identification et une version.  
La mise en œuvre est considérée sous deux environnements :
- DS/GAE : Google App Engine (servlets Java) et son datastore.
- PG/SQL : serveurs de servlets Java avec une base de données SQL, en l'occurrence PostgreSQL (PG).

# Espaces de noms (namespaces)
Pour une instance donnée par son URL, l'application apparaît comme N applications **de logiciel identique** travaillant chacune sur **son propre espace de données** indépendant des autres et identifié par un nom court.  
Un espace spécifique, par défaut `"z"` (mais la configuration peut lui donner un autre code) est dédié à la gestion de la configuration des espaces de données. Cette configuration consiste en : 
- quelques paramètres élémentaires techniques ;
- quelques fichiers de ressources utilisables en particulier depuis le Web.
 
Avec un déploiement sur R-DBMS chaque espace correspond à une base de données : l'espace `z` peut au choix être hébergé par un autre ou disposer de sa (très petite) base dédiée.  
Avec un déploiement GAE Datastore, chaque espace correspond à un `namespace`.

Chaque espace étant a priori considéré comme étanche aux autres, voire ignorant de leur existence pour les utilisateurs, tout ce qui suit se rapporte à un seul espace.  
Une section de l'API est dédiée à la description,
- des quelques actions spécifiques à l'espace **z**;
- des quelques possibilités offertes à un espace pour consulter les données des autres avec des restrictions significatives.

Le nombre d'espaces peut être de ***quelques dizaines au plus***, limite raisonnable d'administration de R-DBMS pour un serveur.  
C'est un moyen important pour augmenter le débit global et permettre des migrations d'espaces d'un hébergeur vers un autre.

# Documents et items
Le stockage de données apparaît comme un ensemble de ***documents*** :
- un document a une **classe** qui en Java étend la classe `Document`.
- un document est **identifié** par un `String` nommé par la suite `docid`.
- un document a une **version**, une date-heure en millisecondes UTC sous forme d'un `long` lisible (171128094052123) et croissante dans le temps : la version est la date-heure de la dernière opération ayant mis à jour le document.
- un document a une **date-heure de création `ctime`** : un document peut être détruit et recréé plus tard avec un même `docid` : `ctime` permet à une mémoire externe retardée de savoir si elle détient une version retardée de la *même vie* du document (dont le contenu peut être valable en partie) ou une version *d'une vie antérieure* (dont le contenu est totalement obsolète).
- un document à une **`dtime`** qui est la date-heure au delà de laquelle le document a gardé la trace des items détruits (voir la gestion des mémoires persistante et cache).

### Items
Un document est un ***ensemble d'items*** chacun ayant,
- une **classe** d'item qui en Java correspond à une sous classe statique de la classe du document et qui étend la classe `Document.BItem` et plus spécifiquement `Item` `RawItem` `Singleton` `RawSingleton` qui étendent `BItem`.
    - un `Singleton` existe en une occurrence au plus dans son document.
    - un `Item` existe en 0 à N occurrences identifiées par un `String` nommé par la suite `key` avec la seule contrainte d'une longueur maximale de 255.
- une **version** : date-heure de l'opération qui a *mis à jour le document*.
- une **vop** : date-heure de l'opération ayant mis à jour l'item. Pour une mise à jour non différée `vop` et `version` ont la même valeur. `vop` vaut 0 pour un item détruit.
- un item a un **contenu** `String` qui peut être,
    - un JSON (`Singleton` et `Item`),
    - un `String` opaque (`RawSingleton` et `RawItem`), typiquement l'encodage en base 64 d'un vecteur d'octets.
- un **contenu `null`** indique un item détruit (`vop` est 0), sa *version* indiquant quelle opération l'a détruit. Un item détruit peut rester connu en tant que tel pendant un certain temps afin de permettre la synchronisation rapide du contenu du document avec un contenu distant, puis être finalement physiquement purgé.

##### Items JSON
Leurs propriétés peuvent être : 
- d'un des types primitifs : `long, int, double, boolean` ; 
- d'une classe interne `?` ou des `Map<String,?>` ou des `Collection<?>` où les classes `?` elles-même peuvent avoir le même type de propriétés. 

> ***Remarque :*** les noms des classes de Document et d'Item dans un document doivent être le plus court possible afin de réduire la taille des index et clés en stockage.

**Sérialisation en JSON** : les propriétés `transient` ne sont pas sérialisées et les propriétés ayant une valeur `null` non plus (sauf en tant que valeurs à l'intérieur d'une `Map` ou `Collection`).

##### Pièces jointes attachés à un document
Il est possible d'attacher à un document des pièces jointes par des items de classe prédéfinie `Document.P` mémorisant pour chaque pièce jointe (une suite d'octets) :
- `key` : sa clé d'accès (comme pour tout item) ;
- `mime` : son type mime (qui n'est pas contrôlé) le cas échéant comportant une extension quand le fichier a été gzippé / crypté (`.g` `.c` `.g` `.gc` ou rien);
- `size` : sa taille en octets ;
- `sha` : son digest `SHA 256`.

Le stockage des pièces jointes bénéficie de la même cohérence temporelle que les items avec les détails suivants :
- le volume des fichiers eux-mêmes est décompté en tant que `v2` (secondaire) et non `v1`. Selon les fournisseurs d'infrastructure le coût du stockage peut être bien inférieur en espace secondaire qu'en espace primaire.;
- quand des documents ont des pièces jointes ayant le même contenu (même `sha`) elles ne sont stockées qu'une fois mais décomptés en v2 autant de fois que cités dans les items P;
- une pièce jointe ayant été modifié (donc ayant désormais un `sha` différent) reste accessible *un certain temps* sous son ancien `sha` (s'il est demandé par son `sha` et non sa clé).  Ceci facilite l'exportation cohérente à une version donnée d'un document et de ses pièces jointes, même quand elle est volumineuse en raison de la taille des pièces jointes. Des exportations incrémentales de documents peuvent être effectuées à coût réseau minimal en évitant le re-transfert de pièces jointes déjà détenues à distance.

### Mémoires persistante de référence et cache
Il n'existe qu'une **mémoire persistante de référence** (base de données / datastore) de documents contenant pour chacun son exemplaire de référence.

Il existe de nombreuses **mémoires caches** de documents, qui sont toujours potentiellement retardées (dispose d'une version antérieure) par rapport à la référence persistante :
- éventuellement dans le cas Datastore une en mémoire *memcached* *commune à tous les serveurs du pool* servant les opérations.
- une mémoire cache *volatile dans chaque serveur du pool* servant les opérations.
- dans les *sessions externes clientes*: 
    - une mémoire cache volatile dans chaque session active.
    - éventuellement une (ou des) mémoire cache persistante locale au terminal survivant aux interruptions de sessions.

L'objectif est de permettre de remettre à un niveau plus récent `vr` (*version de référence*, la plus récente pour simplifier) un exemplaire d'un document `d` datée `v` en minimisant le volume de données échangé et l'effort de calcul de remise à niveau.  
La mise à jour d'une mémoire cache depuis un exemplaire de référence d'un document `d` est *un document de mise à niveau* plus ou moins complet établi selon les principes suivants :
- si l'exemplaire en cache a une `ctime` inférieure à celle de référence, son contenu en cache est à remplacer en totalité totalement par le document de mise à niveau qui contient une copie complète de celui de référence (y compris des items détruits).
- sinon l'exemplaire en cache contient :
    - des items inchangés, toujours existants en référence avec la même version : ils ne figurent pas dans le document de remise à niveau.
    - des items qui ont changé, toujours existants en référence avec une version plus récente : ils figurent dans le document de remise à niveau avec leur nouveau contenu.
    - des items qui existent en mémoire cache mais plus en référence : ils devront être détruits ou conservés avec un contenu vide (marqués détruits) dans la mémoire cache. Voir les cas 1 et 2 ci-dessous.

Un exemplaire dans une mémoire cache contient,
- tous les items existants (ayant un contenu) avec leur version;
- tous les items détruits (sans contenu) avec leur version de destruction, *postérieurement à la date-heure `dtime` du document*.

La mémoire cache à remettre à niveau contient un exemplaire de `d` de version `v` et de `dtime` `dt`.  
La mémoire source de la remise à niveau contient un exemplaire de `d` de version `vr` et de `dtime` `dtr`.  
En plus des données `vr` et `dtr`, que contient le document de mise à niveau ?

##### Cas 1 : `v` est postérieure ou égale à `dtr`.
La référence contient tous les items détruits depuis `v` : 
- le document de mise à niveau inclus les items détruits postérieurement à `v` avec un contenu `null` et leurs version de destruction.
- les autres items sont inclus avec un contenu et une version si leur version est postérieure à `v` (ils ont changé).
- les items inchangés depuis v ne sont pas inclus.

Dans ce cas un item sans contenu est un item détruit et un item présent dans le cache et absent du document de remise à niveau est inchangé.

La mise à niveau de la mémoire cache par le document de mise à niveau transmis consiste à :
- inscrire `vr` comme nouvelle version du cache.
- inscrire la plus récente de la `dtime` actuelle `dt` et `dtr` comme nouvelle `dtime` du cache (les items détruits antérieurement n'étant pas connus).
- les items *sans* contenu du document de remise à niveau sont inscrits dans le cache avec un contenu vide et leur version.
- les items *avec* contenu du document de remise à niveau écrasent ceux du cache ou les insèrent quand ils n'y étaient pas.
- la nouvelle `dtime` du cache peut aussi être avancée entre sa nouvelle valeur et la nouvelle version pour éviter de garder trop d'items détruits : le cache purge tous les items sans contenu de version antérieure à la nouvelle `dtime`.

Le cache peut lui-même désormais servir de référence pour créer un document de remise à niveau d'un autre cache plus retardé.

##### Cas 2 : `v` est antérieure à `dtr`.
La référence ne contient pas tous les items détruits depuis `v` mais seulement depuis `dtr`. Si le cache détient un item `x` détruit entre `v` et `dtr`, le document de remise à niveau ne peut pas le savoir.  

Le document de remise à niveau contient en plus de `vr` et `dtr` une **liste des clés des items existants** (sauf toutefois celles de ceux listés parce qu'ayant été créés ou modifiés).
- le document de mise à niveau inclus les items détruits postérieurement à `dtr` avec un contenu `null` et leurs version de destruction.
- les autres items sont inclus avec un contenu et une version si leur version est postérieure à `v` (ils ont changé).
- les items inchangés depuis `v` ne sont pas inclus.

La nouvelle `dtime` du cache est `dtr`.  
Elle peut aussi être avancée entre sa nouvelle valeur et la nouvelle version `vr` pour éviter de garder trop d'items détruits : le cache purge tous les items sans contenu de version antérieure à la nouvelle `dtime`.

Dans ce cas un item sans contenu reste toujours un item détruit et un item absent est, 
- soit inchangé s'il figure dans la liste des existants, 
- soit détruit avant `dtr` dans le cas contraire et à purger du cache.

La mise à niveau s'effectue de manière identique au cas 1 mais comporte une phase supplémentaire de purge de tous les items existants en cache et absents de la liste des existants et de la liste des items détruits depuis `dtr`.

# Opérations
Une opération est une succession d'actions de lecture, d'écriture (création / modification / suppression) de documents et d'items de documents et de calculs sur ces données :
- **une opération est "atomique"** vis à vis de la mise à jour des documents, tous mis à jour et datés du même instant ou aucun.
- **une opération est "isolée"** et travaille sur tous les groupes de documents accédés comme si elle était seule le faire.
- **une opération peut retourner un résultat** ou aucun. Ce résultat peut être :
    - n'importe quel **contenu binaire** ayant un type mime.
    - le couple, a) d'un **résultat structuré** sous forme d'un objet JSON, b) d'une **liste de synchronisations** donnant tous les changements opérés depuis une version donnée sur les documents que le demandeur de l'opération avait cité à des fins de synchronisation.
- **une opération peut se terminer prématurément en exception**, technique ou métier, bug détecté ou situation inattendue ...
- Une opération a trois date-heures :
    - souvent mais pas toujours, une *date-heure d'opération* `dhop` qui est celle de la session ayant invoqué l'opération.
    - une *date heure de début*, horloge du début du traitement. Si `dhop` existe elle doit ne pas être trop inférieure à celle-ci mais jamais supérieure. Les horloges des sessions doivent être à peu synchronisées sur celle du serveur;
    - une **date-heure de validation**, non connue au cours du traitement, et qui marque la **version** de tous les documents et de leurs items ayant été créés, modifiés, ou détruits au cours de l'opération.

Une opération est initiée par l'arrivée d'une requête externe HTTPS le cas échéant émise par le *Queue Manager* gérant des requêtes différées mises en file d'attente.

Une opération est matérialisée par un objet `operation` d'une classe étendant `Operation` et ayant deux méthodes `work()` et `afterwork()`, l'étape *validation* intervenant entre elles:

- `work()` : cette méthode lit les arguments de la requête qui l'a initiée, effectue les traitements nécessaires et consigne le résultat éventuel dans la variable `result` de l'opération.
- à la fin de `work()` l'étape de **validation** vérifie la consistance temporelle des documents utilisés au cours du traitement et le cas échéant rend persistant les documents créés / modifiés / supprimés au cours de `work()` : la validation peut échouer en cas d'inconsistance temporelle des documents et/ou de problèmes de contention sur la base de données ;
- `afterWork()` : cette méthode est exécutée après la validation et peut le cas échéant modifier le résultat pour y intégrer la valeur de la version -la date-heure de validation de l'opération désormais connue. En général et par défaut elle ne fait rien.

En cas d'exception au cours du traitement, un nouvel objet `operation` est recréé et le cycle **`work()` -> validation->  `afterwork()`** est relancé (après un certain délai, au plus 3 fois et pas pour toutes les exceptions). 

##### Estampilles d'un document et de ses items : classe `Stamp`
Les estampilles sont des nombres de la forme `AAMMJJhhmmssmmm` soit la date en année mois jour heure minute seconde milliseconde en UTC (du XXIième siècle) :
- `Stamp.minStamp` : première milliseconde du siècle,
- `Stamp.maxStamp` : dernière milliseconde du siècle.
 
Par exemple : `160714223045697` (le 14 juillet 2016 à 22:30:45 et 697 ms).  
La classe `Stamp` dont les objets sont immuables permet de créer et manipuler ces estampilles et de les convertir en `epoch` nombre de millisecondes écoulées depuis le 1/1/1970 (`epoch`).

>Un document *existe* dès lorsqu'il a une date-heure de création et une version : il peut n'avoir aucun item. Les versions étant attribuées à la validation d'une opération, en cours d'opération une version à 0 traduit un document créé au cours de cette opération.

>Une opération peut créer / modifier / supprimer plusieurs documents : ces documents, et leurs items, portent comme version l'estampille de validation de l'opération ce qui permet de déterminer après coup si plusieurs modifications ont été portées exactement par la même opération.

>Vues d'un document toutes les estampilles de validation des opérations sont distinctes et croissantes dans le temps. Deux opérations strictement parallèles et n'ayant pas modifié les mêmes documents peuvent avoir une même estampille de validation : la chronologie réelle entre elles est indécidable mais ne peut pas avoir d'impact sur la logique métier.

# Cohérence temporelle entre documents
**Dans sa phase `work()`** une opération qui souhaite modifier un document, ou s'assurer qu'il ne changera pas au cours du traitement, le demande avec *une tolérance temporelle de 0 seconde* (l'état le plus récent possible) : 
- ceci autorise l'opération à modifier le document, ce qu'elle n'est *pas* obligé de faire.
- ceci garantit qu'au cours du traitement c'est exactement ce même objet document qui sera vu / modifié, même s'il est demandé plusieurs fois et que sa référence n'a pas été conservée dans des variables.

Si l'opération demande un document avec une tolérance temporelle supérieure à 0 (1 seconde, 30 secondes, etc.) elle récupère une version (disponible le cas échéant en cache) avec une fraîcheur compatible avec l'exigence : le document est en lecture seule dans l'opération.

**La validation d'une opération** s'opère selon les étapes suivantes :
- **elle s'assure** (en les verrouillant effectivement sur le stockage persistant) que tous les documents demandés avec une tolérance 0 (qu'ils aient été modifiés ou non) **n'ont pas changé de version** par rapport à celle de l'exemplaire utilisé en traitement ;
- **elle détermine sa propre version**: c'est la date-heure courante mais toujours garantie supérieure d'au moins une milliseconde à la plus tardive des versions des documents à tolérance 0 ;
- **elle valide définitivement le stockage des contenus** des documents créés / modifies / supprimes au cours du traitement et de leurs items.
- **elle libère les verrouillages** sur les documents qui étaient verrouillés par le `commit()` technique du stockage persistant.

>Si un document a évolué entre la phase de traitement et la validation, l'opération subit une exception : la validation s'interrompt, libère les verrous et l'opération est recyclée. Elle prendra alors la dernière version du / des documents ayant provoqué cette exception.
>Le nombre de recyclages est limité et un certain temps est laissé entre chaque cycle : in fine l'opération peut sortir définitivement en erreur du fait d'un excès de contention d'accès sur les documents persistants.

Ce mode de gestion *optimiste* table sur le fait que les opérations vont travailler sur peu de documents (il y a une limite autoritaire à 32). Elle optimise aussi la connaissance entre plusieurs instances de serveurs des dernières versions des documents et rend la gestion des caches locales à chaque instance plus efficientes.

##### Consistance structurelle entre plusieurs documents : documents verrous
Parfois un groupe de documents doit être considéré comme immobile dès lors qu'une opération travaille sur l'un deux :
- par exemple pour effectuer des arrêtés comptables exactement synchronisés sur les documents du groupe et enregistrer dans un document un agrégat de décomptes provenant de documents du groupe.
- dans ce cas il faut déterminer un document représentatif du groupe (le cas échéant avec un singleton quasi vide) et s'astreindre à le demander avec une tolérance 0 dans toutes les opérations portant sur un des documents du groupe.

### Opérations de mise à jour et synchronisations
Elles ont pour objectif de mettre à jour, ajouter ou supprimer des items dans un ou plusieurs documents du même groupe ou non. 

En cas de succès et si l'opération ne retourne pas de résultat ou retourne un résultat en JSON, elle est suivie d'une ***synchronisation*** qui liste (en JSON) toutes les modifications résultantes de l'opération sur tous les documents que la session ayant émis la requête a cité "à synchroniser" (avec la version et le `dtime` détenus pour chacun).  
Une session peut ainsi effectuer des actions de mises à jour et récupérer toutes les conséquences de ces actions sur les documents dont elle détient une copie (plus ou moins retardée).

**Une opération de mise à jour doit être idempotente** : l'exécuter plusieurs fois successivement doit laisser l'ensemble des documents dans le même état qu'une exécution unique ce qui exige le cas échéant de détecter que la mise à jour a déjà été faite ou qu'elle peut être faite plusieurs fois sans dommage pour la logique métier.  
L'utilisation de la date-heure d'opération `dhop` permet souvent de mémoriser qu'une opération de la session a déjà été effectuée et éviter de la refaire.

***Une opération de mise à jour peut retourner un résultat*** : du fait de l'idempotence le traitement doit retourner le même résultat qu'elle ait procédé effectivement à la mise à jour ou constaté qu'elle avait déjà été faite.

### Opérations de reporting
Elles ont pour objectif de retourner un résultat construit depuis des documents stockés.
 
Des exécutions successives donnent usuellement des résultats différents, l'état des documents stockés ayant évolué.  
Le résultat d'une opération de reporting peut avoir n'importe quelle forme : *JSON, image, html*, etc.

Si une opération de reporting retourne un résultat en JSON, elle *peut* être suivie d'une synchronisation.

# Exceptions
Opérations et synchronisations retournent une ***exception*** en cas d'échec aboutissant à un status HTTP 400 (404 pour les N). 

### Codes MAJOR / MINOR
La première lettre du code **MINOR** de l'exception définit son code **MAJOR** (1 à 7) :
- 1 : ***A - métier*** : les conditions fonctionnelles sont non satisfaisantes sur le contenu des documents vis à vis des paramètres de l'opération. Recommencer l'opération avec les mêmes paramètres a de fortes chances de provoquer la même exception ...  sauf si les données sur le serveur ont évolué favorablement depuis, typiquement du fait d'une requête émise par une autre session depuis la requête initiale.
- 1 : ***N - ressource inexistante*** : l'opération a demandé typiquement le contenu d'une pièce jointe inexistante (404).
- 2 : ***B - bug*** : l'application *a rencontré et bien détecté* une conjonction des valeurs de données qui n'auraient jamais dû exister (selon le développeur). Recommencer l'opération avec les mêmes paramètres a des chances de provoquer la même exception, sauf si les données sur le serveur ont évolué depuis de sorte que cette conjonction non prévue n'apparaisse plus (chance ... relative, c'est un incident non reproductible et peut-être difficile à corriger).
- 3 : ***X - inattendue*** : l'application *a rencontré une exception inattendue* (par exemple des valeurs incohérentes et non détectées en tant que telle) qui peut être la conséquence d'une indisponibilité ou du système ou du matériel (I/O, réseau, contention sur les données, famine de mémoire ...). Recommencer l'opération un peu plus tard avec les mêmes paramètres peut ne pas faire apparaître la même exception si elle résultait d'un problème ponctuel ou d'une surcharge système ayant disparu depuis. Mais ce peut être aussi un bug non détecté par l'application en tant que bug.
- 4 : ***D - version retardée*** : la session requérante peut (et sauf exception le fait) transmettre avec sa requête l'identification de sa build. Si le serveur a une build plus avancée pour éviter des incidents de gestion liés à cette discordance il émet une exception de ce type.
- 5 : ***C - contention sur les données*** : l'opération trop longue n'arrive pas à s'exécuter avec un ensemble de documents cohérents temporellement entre eux. Malgré plusieurs tentatives l'échec de la validation d'une opération réapparaît.
- 6 : ***O - domaine off*** : une opération de maintenance technique a fermé le service temporairement. Un message plus détaillé peut donner plus d'informations sur les raisons de cette fermeture et perspective de réouverture.
- 7 : ***S - non autorisée*** : la session a joint des identifiant / clé d'authentification / autorisation ... n'autorisant pas l'exécution de l'opération. Il s'agit soit d'une session dont l'autorisation a expiré ou  a été supprimée par l'administrateur ou d'une tentative d'intrusion.

### Phase de traitement
Une exception embarque la phase de ce traitement où elle a été levée et permet de déterminer si oui ou non le traitement a été exécuté et/ou validé :
- phase 0 : avant début de l'opération.
- phase 1 : au cours de l'opération (`work()`).
- phase 2 : durant la *validation* de l'opération.
- phase 3 : au cours de la phase `afterwork()` de l'opération.
- phase 4 : durant la synchronisation postérieure à l'opération.
- phase 5 : durant l'envoi du résultat.

Une requête HTTP peut également retourner des codes d'incident autres :
- avant envoi de la requête ;
- des codes HTTP 0 ou 500 émis par le frontal ou le conteneur de l'application, dépassement de temps etc.
- après retour de la requête sur l'interprétation du résultat retourné.

# Tâches : opérations différées
Certains traitements peuvent être constitués d'une séquence, possiblement longue, d'opérations. D'autres sont à lancer périodiquement.  
Comme une opération doit avoir un temps d'exécution réduit et surtout ne pas accéder à trop de documents en tolérance 0, ces traitements s'exécutent sous la forme d'une suite d'opérations distinctes et différées.  
Par ailleurs une opération *principale* peut avoir besoin d'être suivie après un délai plus ou moins court d'opérations `secondaires` différées par rapport à l'opération principale.

Le **Queue Manager** est un ensemble de threads chargés d'envoyer des requêtes HTTP correspondant aux opérations différées inscrites et d'en récupérer le status d'exécution. Le Queue Manager joue un rôle de session externe mais non humaine :
- elle n'a que faire d'un quelconque résultat.
- elle doit gérer les erreurs d'exécution des opérations,
    - en relançant les traitements tombés en exception : si la cause était fugitive, ça s'arrange tout seul. Typiquement un lapse de temps croissant est laissé entre les relances afin de contourner un éventuel problème de contention sur les données ou une indisponibilité technique un peu longue ;
    - en alertant un administrateur technique par e-mail si un traitement échoue à sa n-ième relance. Ce dernier peut,
        - soit remédier à la cause fonctionnelle du souci si possible, puis remettre la tâche en exécution.
        - soit renoncer définitivement à celle-ci en ayant apprécié les conséquences fonctionnelles de cet abandon.

### Création d'une tâche
Une tâche est inscrite dans une table `TaskQueue` (*entity* en Datastore) au commit de l'opération qui l'a créé avec les données suivantes :
- `ns` : son namespace.
- `taskid` : son identification aléatoire tirée à la création.
- `opName` : le nom de l'opération en charge du traitement.
- `param` : un JSON contenant les paramètres requis. Si ceux-ci sont très volumineux, on créé un document et `param` en contient juste l'identification.
- `info` : texte d'information indexé permettant des filtrages pour l'administrateur.
- `retry` : son numéro d'ordre d'exécution. 0 à la création il est incrémenté à chaque relance par le Queue Manager.
- `startAt` : la date-heure de son lancement / relance au plus tôt.
- `startTime` : la date-heure de sa demande d'exécution par le Queue Manager. `null` quand elle est en attente, sa présence indique qu'elle est en cours de traitement dans le serveur.
- `qn` : numéro de queue : le Queue Manager a plusieurs queues numérotées de 0 à N pour des usages différents, par exemple : 
    - 0:*rapide*, plusieurs threads pour des tâches courtes,
    - 1:*standard*, 2 threads pour des tâches plus longues,
    - 3:*background*, un seul thread pour des tâches peu fréquentes et peu prioritaires.
    - pour chaque code d'opération la configuration d'application définit le numéro de queue (quand il n'est pas 0).
- `exc` : code d'exception du dernier traitement en erreur. `null` au lancement / relance.
- `report` : texte d'information sur la dernière exception.

### Opération associée à une tâche
C'est une opération normale. L'objet `param` et le nom de l'opération `opName` ont été récupérés de `TaskQueue` avec les deux informations `startTime` et `retry` rendues disponibles dans `InputData.args`.
- elle ne retourne aucun résultat, n'effectue pas de calcul de synchronisation, et n'a pas de phase `afterwork()` : elle ne fait *que* des mises à jour de documents dans sa phase `work()`.
- elle peut inscrire des opérations différées.
- elle bénéficie d'un quota de temps plus long pour son exécution.
- elle peut itérer plusieurs cycles **`work()` / validation** au cours de la même requête HTTPS initiale (voir ci-dessous).

### Principe d'exécution nominal
Une opération inscrit une nouvelle tâche en ayant fourni `ns opName param info` : `taskid` est générée et `qn` obtenu de la configuration.  
Au commit :
- la tâche est inscrite dans la table `TaskQueue`.
- pour un Datastore, **avant le commit**, une tâche est mise en queue avec pour URL d'invocation `/ns/od/taskid?key=...`.
- pour une base de données, **après le commit**, si la `startAt` est proche (moins de X minutes : X est le scan lapse du Queue Manager), le Queue Manager associé au namespace (`qm7`) reçoit une requête HTTP `/qm7/op?key=...&op=inq&param={ns:..,taskid:..,startAt:..,qn:..}` pour inscription de la tâche à relancer sans attendre le prochain scan.

**Quand le Queue Manager peut / doit lancer la tâche**, il cherche un thread worker libre qui émet vers le serveur du namespace une requête HTTP avec `/ns/od/taskid?key=...` :
- `ns` : le namespace (comme pour toute requête),
- `od` au lieu de op pour identifier qu'il s'agit d'une opération différée,
- `taskid` dans l'URL.
- le paramètre `key` : mot de passe permettant de s'assurer que c'est bien le Queue Manager qui a émis la requête et non une session externe (en fait sur un POST, `key` n'apparaît pas dans l'URL comme dans celle ci-dessus qui est employée en test).

Quand le Datastore lance la tâche il émet une requête HTTP sur l'URL `/ns/od/taskid?key=...`.

La requête HTTP arrive dans le serveur et recherche dans `TaskQueue` le descriptif de la tâche :
- si il n'y est pas la requête se termine en 200. La tâche s'était bien terminée :
    - soit c'était une relance intempestive,
    - soit l'administrateur l'a supprimée. Dans le cas du Datastore c'est un moyen donné à l'administrateur de supprimer une tâche.
- si elle y est avec une `startAt` pas encore atteinte (avec un minimum de tolérance) c'est que l'administrateur l'a reculée :
    - en Datastore il a fait inscrire une nouvelle tâche avec la nouvelle valeur de `startAt`.
    - la requête se termine en 200.
    - en base de données, si la `nextStart` est proche le Queue Manager reçoit une notification.
- si elle y est avec une `startTime` (ça ne devrait pas arriver en Datastore) la requête se termine en 200 : la tâche est déjà en exécution ailleurs.
- sinon la tâche est à exécuter :
    - la requête récupère `param` et `retry`.
    - la requête inscrit dans `TaskQueue` une `startTime` qui marque le fait que la tâche est en exécution. 

L'opération souhaitée s'exécute ensuite :
- **traitement OK** :
    - la tâche `ns.taskid` est supprimée de la table `TaskQueue` au commit.
    - le retour est un status 200 ce qui libère le worker du Queue Manager pour prendre en charge une nouvelle tâche ou signale au Datastore que la tâche est finie.
- **traitement en Exception** :
    - la tâche `ns.taskid` est mise à jour dans `TaskQueue` :
        - `starTime` y est mise à `null`;
        - `retry` est incrémenté;
        - `exc` et `report` sont renseignés;
        - `startAt` est calculée à une valeur future d'autant plus lointaine que le numéro de `retry` est élevé, voire finalement infinie.
    - le retour est un status 500 ce qui libère le worker du Queue Manager pour prendre en charge une nouvelle tâche ou signale au Datastore qu'il faudra relancer.
    - sauf Datastore, si la `nexstart` est proche (moins de X minutes), le Queue Manager associé au namespace reçoit une requête HTTP pour inscription de la tâche à relancer sans attendre le prochain scan.

**Scan périodique des TaskQueue par le Queue Manager**
Un Queue Manager gère un ou plusieurs namespaces et fait donc face à une ou plusieurs base de données.  
Seule les tâches à échéance proche lui sont soumises par HTTP : un scan périodique lui permet de récupérer les autres. Ce scan, pour chaque base de données, filtre les tâches ayant :
- une `startAt` antérieure à la date-heure du scan suivant,
- ayant l'un des namespaces dont il est en charge,
- ayant une `startTime` `null` (tâche pas en cours).

Ce scan permet au Queue Manager de récupérer les tâches à relancer autant que celle à lancer une première fois.

### Situations anormales
##### Perte de contact par le worker du Queue Manager qui suit l'exécution d'une tâche
Sa requête HTTP sort prématurément en exception : la notification de fin ne lui parviendra pas.
- le worker considère la tâche comme terminée.
- le worker n'est en fait pas intéressé par le status de bonne ou mauvaise fin d'une tâche.

Si la tâche se termine bien, elle disparaîtra de `TaskQueue`.  
Si la tâche sort en exception elle sera modifiée en `TaskQueue` pour une future relance et si cette relance est proche le Queue Manager sera notifié par HTTP pour accélérer la relance.

##### Tâches perdues
Il s'agit de tâches qui ont été lancées (ou relancées), 
- ayant une `startTime` ancienne,
- dont le Queue Manager n'a pas de trace en exécution dans un worker.

La tâche est supposée s'être mal terminée sans que son exception n'ait pu être enregistrée dans `TaskQueue`.

Le Queue Manager au cours d'un scan va récupérer ces tâches supposée encore en exécution depuis longtemps et pour chacune :
- si un worker indique être encore à l'écoute, elle est ignorée : elle n'était pas perdue bien qu'elle aurait dû sortir en timeout.
- sinon la tâche est marquée en exception `LOST` et dans `TaskQueue` :
    - `startTime` est mis à `null`, `retry` est incrémenté, `nextStart` est calculée.
    - le cas échéant elle est réinscrite pour relance proche.

##### Tâche perdue retrouvée
Une tâche a été perdue, son worker a perdu le contact et le Queue Manager a inscrit un `retry`.  
Mais la tâche finalement se termine :
- bien : elle est supprimée de `TaskQueue`.
- mal : `TaskQueue` n'est pas mis à jour, elle y reste marquée `LOST`.

Le Queue Manager,
- soit a déjà relancé cette tâche après l'avoir détectée perdue : elle est à nouveau en cours d'exécution.
- soit ne l'a pas encore fait :
    - si elle était OK, elle a disparu de `TaskQueue`, le Queue Manager n'arrivera pas à la relancer : elle est effacée des tâches candidates.
    - si elle était KO, elle est déjà planifié en relance.

Une tâche courre le risque (rare) d'être lancée une seconde fois après une première exécution en succès, tout comme n'importe quelle opération peut être relancée par une session : la logique applicative doit s'en prémunir :
- pour une opération classique une date-heure d'opération donnée par la session cliente permet de détecter cette situation quand elle est stockée dans les documents.
- pour une opération différée la `startTime` peut être employée.

### L'objet `taskCheckPoint`
Une opération différée reçoit en entrée un `Object` dans sa propriété `taskCheckpoint` :
- au début cet objet est `null`. 
- après validation de l'opération
    - si cet objet est toujours `null`, l'opération se termine.
    - si cet objet n'est pas `null` : un nouveau cycle s'effectue. Un nouvel objet `Operation` est créé et sa méthode `work()` appelée. Les paramètres d'entrée de la requête sont identiques mais son objet `tasckCheckpoint` n'est pas `null` mais contient celui laissé lors de la validation précédente.

***Exemple d'utilisation***  
Un document contient une liste d'organisations à traiter.  
Chaque exécution de `work()` traite une organisation et l'enlève de la liste : 
- si la liste est vide le `taskCheckpoint` est mis à `null`.
- sinon le compteur d'organisations traitées est incrémenté de 1 dans le `taskCheckpoint`. Ainsi après validation de l'opération une nouvelle opération dans la même requête repart immédiatement en phase `work()`.
- si ce compteur atteint 100 par exemple, une nouvelle tâche différée est inscrite et ne comporte en `param` que la liste résiduelle des organisations restant à traiter : l'objet `taskCheckPoint` est mis à `null`.

Ceci est un moyen d'assurer une reprise de traitement organisation par organisation tout en évitant à la fois, a) des requêtes HTTP trop courtes, b) de trop mobiliser les ressources au profit d'une tâche de fond au détriment des tâches de front.

### La classe `Cron`
Une tâche peut être périodique : à la fin d'une exécution, une nouvelle tâche est inscrite pour exécution ultérieure.  
Par exemple une tâche mensuelle prévue le 2 du mois à 3h30, sera inscrite dès sa fin d'exécution pour le 2 du mois suivant à 3h30.  
Une tâche peut *calculer* cette date-heure de prochaine relance mais peut aussi utiliser la classe `Cron` qui depuis un court texte calcule la prochaine échéance en fonction de la date-heure courante, du moins dans les cas usuels.

`Cron` gère une période qui peut être : 
- Y : annuelle, 
- M : mensuelle, 
- W : hebdomadaire, 
- D : journalière, 
- H : horaire.

***Exemples :***
- `H25` : soit cette même heure à 25 minutes s'il est moins de 25, soit l'heure suivante à 25 minutes ;
- `D0425` : soit aujourd'hui à 4h25 s'il est moins de 4h25, soit le jour calendaire suivant à 4h25 ;
- `W30425` : soit aujourd'hui si c'est un mercredi et qu'il moins de 4h25, soit le mercredi qui suit à 4h25 ;
- `M100425` : soit le 10 de ce mois si on est avant le 10 à 4h25, soit le 10 du mois suivant à 4h25 ;
- `Y11100425` : soit le 10 novembre de cette année à 4h25 si on est avant cette date-heure, soit le 10 novembre de l'année prochaine à 4h25.

Normalement un traitement dont la `startAt` a été calculée depuis `Cron` avec le paramètre `D0425` n'est PAS lancé AVANT 4h25 : en conséquence à sa validation il sera plus de 4h25 et le traitement suivant sera inscrit pour le lendemain à 4h25. Si toutefois le traitement du jour normalement prévu pour le jour J a eu beaucoup de retard au point d'être lancé / terminé à J+1 3h10, le traitement suivant s'effectuera 1h15 plus tard ... sauf à ce que le traitement de la tâche contredise le calcul standard basé sur le `Cron`.

## Administration des tâches
Un certain nombre d'opérations permettent d'assurer les services sur le serveur et des pages spécifiques permettent d'en assurer l'interface : le namespace est le code du Queue Manager géré.   - ce namespace n'a pas de base données directement associée et agit avec toutes les bases de données de tous les namespaces qu'il gère.

La liste des tâches peut être consultée et filtrée depuis `TaskQueue`.
- tous namespaces confondus avec seulement des filtres sur `startTime retry opName`.
- par namespace avec un filtre plus fonctionnel sur `opName info` qui est un champ indexé.

Une tâche peut être supprimée :
- en Datastore elle sera toutefois lancée mais se terminera immédiatement.
- en base de données en général elle ne sera pas lancée, si le Queue Manager est notifié à temps.

Une tâche peut être avancée :
- en Datastore une autre tâche sera créée plus tôt, la seconde sera ignorée à l'exécution.
- en base de données le Queue Manager est notifié.

Une tâche peut être reculée :
- en Datastore une autre tâche sera créée plus tard, la première sera ignorée à l'exécution.
- en base de données le Queue Manager est notifié, en général à temps avant lancement.

En Datastore c'est aussi un moyen de faire une relance de tâche après erreur quand lui a renoncé de le faire.

Hors Datastore, l'administrateur peut,
- suspendre un Queue Manager,
- lever sa suspension, 
- interroger son *backlog* en mémoire (tâches à lancer très prochainement et en cours).

# Réplications différées d'items
Une opération ne doit travailler que sur peu de documents : en conséquence un document A1 ayant à répliquer son état synthétique sur des dizaines / centaines d'autres documents ne peut pas le faire dans le cadre d'une opération unique. L'usage d'une tâche différée permet d'y remédier.

Un cas particulièrement fréquent a toutefois fait l'objet d'un traitement générique. Exemple :
- chaque document d'une classe *Asso* représente une association identifiée par une identification (*idAsso*) :
    - un singleton `Sta` représente le statut général de l'association : son intitulé, son état (actif / résilié), voire quelques compteurs importants changeant peu souvent (nombre d'adhérents au premier janvier de l'année).
    - des items `Adh` avec pour clé le numéro de l'adhérent (`numAdh`) dans l'association représente les adhérents de l'association avec un résumé simple de données changeant peu souvent : nom, état d'activité, date d'adhésion / résiliation.
- chaque document d'une classe `GT` représente un groupe de travail réunissant quelques adhérents de multiples associations. On souhaite disposer dans un document G1 de `GT` lui-même,
    - `Adh` : du résumé relatif à chaque adhérent : c'est la copie (faiblement désynchronisée) de l'item `Adh` de l'adhérent dans son association.
    - `Sta` : du statut général de toutes les associations dont au moins un adhérent fait partie du groupe de travail : c'est la copie (faiblement désynchronisée) du singleton `Sta` de l'association.

Dans les deux classes `Asso` et `GT` les items `Sta` et `Adh` portent le même nom, mais :
- pour `Sta` :
    - dans `Asso` c'est un singleton ayant une annotation :
    `@ADifferedCopy (copyToDocs={GT.class}, separator='.')` . Il peut y avoir plusieurs classes de documents listées.
    - dans `GT` c'est un item ayant pour clé `idAsso`.
- pour `Adh` :
    - dans `Asso` c'est un item de clé `numAdh` ayant une annotation :
    `@ADifferedCopy (copyToDocs={GT.class}, separator='.')`.
    - dans `GT` c'est un item ayant pour clé `numAdh.idAsso` : le `.` qui sépare les deux parties de la clé est le signe déclaré dans le paramètre `separator` ci-dessus.

Deux index sont déclarés sur la colonne `clkey` pour les items `Sta` et `Adh` de `GT`.

**A la déclaration d'une nouvelle participation** d'un adhérent `N1` d'une association `A1` à un groupe de travail `G1`, l'opération effectue :
- la création d'un item `Adh` de clé `N1.A1` dans `G1` en y copiant le contenu courant de l'item `Adh` de clé `N1` représentant l'adhérent dans son `Asso` d'identifiant `A1`.
- si c'est le premier participant appartenant à l'association `A1`, un item `Sta` de clé `A1` est créé dans `G1` avec une copie du singleton `Sta` de `A1`.

**Au retrait de la participation** de l'adhérent `N1` de l'association `A1` du groupe de travail `G1`, l'opération effectue :
- la suppression de l'item `Adh` de clé `N1.A1` dans `G1`.
- si c'est le dernier participant appartenant à l'association `A1`, la suppression de l'item `Sta` de clé `A1` dans `G1`

Moyennant ces contraintes, toute mise à jour de `Sta` ou d'un `Adh` dans le document d'une association `Ai` par une opération de date-heure `dh1` provoquera une réplication faiblement différée automatique :
- dans tous les documents d'une des classes citées dans les annotations de `Sta / Adh`,
- ayant un item de même nom de classe `Sta / Adh` et de clé `A1` (pour `Sta`) ou `xxx.A1` pour `Adh`, d'où l'importance des index sur `clkey`.
- les items sont insérés en base *sans lecture des dossiers*.
- chaque dossier impliqué a désormais une version `dh2` supérieure à `dh1` (chacun des dossiers ayant leur propre date-heure de mise à jour).
- chaque item mis à jour a pour version `dh2` et pour `vop` `dh1` : il est ainsi possible de savoir de combien la réplication a été retardée.
- comme les réplications sont désynchronisées rien n'interdirait qu'une seconde opération de date-heure `dh3` ait sa réplication doublant celle de `dh1` pour un item donné : ce dernier n'est mis à jour que si sa `vop` mémorisée est inférieure à la `vop` proposée en mise à jour, bref un état retardé n'écrase pas un état plus récent.
- les mises à jours désynchronisées ne sont soumises à aucun contrôle fonctionnel.

# Identification / authentification
Ce service est géré au niveau de l'application : chaque requête est autonome des précédentes et il n'y a pas de concept de session dans le serveur.  
Chaque requête peut être porteuse de propriétés, ***par exemple*** `account` `key` `admin` où,
- `account` : identifie un compte,
- `key` : donne le mot de passe, tout autre élément d'authentification, un jeton de session etc.
- `admin` : donne pour certaines opérations une clé d'autorisation de privilège administrateur.

Des opérations de login *peuvent* être écrites pour identifier des sessions d'utilisation avec un utilisateur identifié par le login qui déclare (ou retrouve) une session puis des requêtes qui ne font que référencer des sessions déclarées.

# Indexation et recherche de documents
L'objectif de l'indexation est de pouvoir effectuer une recherche de tous les identifiants des documents d'une classe donnée,
- ***dont l'identifiant est éventuellement contraint***,
- ***dont au moins un item d'une classe donnée a une valeur de propriété satisfaisant au critère de sélection***. 

Un second type de recherche utilisant l'indexation consiste à obtenir, non plus seulement l'identifiant d'un document mais le ***contenu*** des items (dont leur `key` et l'identifiant de leur document).

Par exemple si un item `Adr` contient un `codePostal` et une *collection* d'adresses `email`, obtenir la liste des identifiants des documents ayant un item `Adr` dont le `codePostal` est compris entre `94000` et `94999` et ayant au moins une adresse `email` `toto@gmail.com`.

### Déclaration des propriétés indexées
Dans la classe d'un Item quelques propriétés peuvent avoir une annotation `@ExportedField`. Le type de ces propriétés est l'un des suivants `String long int double String[] long[] int[] double[]`.

#### Mise en œuvre en R-DBMS
Il faut déclarer une table portant le nom `Item_docClass_itemClass` héritant de la table `Item` et y déclarer les propriétés indexées sous le même nom que dans la classe Java et avec le même type. `docClass` et `itemClass` sont les noms des classes de document et de l'item dans le document.  
Il faut ensuite déclarer un (ou plusieurs) index sur ces propriétés, index contenant en tête la (ou les) propriétés indexées puis les colonnes identifiantes du document et de l'item.

#### Mise en œuvre en GAE Datastore
L'entité des items est `docClass` : il faut déclarer des index sur les noms des propriétés indexées ( `itemClass_propName`).

#### Conditions sur les valeurs des identifiants et propriétés
Une condition est déclarée par un objet immuable de classe `Filtre` dont les deux constructeurs permettent de spécifier une ou deux valeurs et la condition à respecter :
- `Filtre(type, valeur1)` :
    - `EQ` : égal à la valeur 1 ;
    - `SW` : commençant par la valeur 1 (String seulement) ;
    - `LT LE GE GT` : supérieur / inférieur strictement ou non à la valeur 1 ;
- `Filtre(type, valeur1, valeur2)`
    - `LTGE LTGT LEGE LEGT`  : compris entre valeur 1 et valeur 2 (bornes inférieure et supérieures incluses ou non).

#### Recherches spécifiques
Des recherches *spécifiques* demandent le développement d'une classe comportant une ou deux (voire plus) implémentations selon le type GAE Datastore et R-DBMS (le cas échéant plusieurs).

Détail des méthodes dans le document API.

----------

# Argumentaires
### Gestion pessimiste / optimiste du verrouillage des groupes
***En mode pessimiste*** le verrouillage ***a priori*** des groupes présente un risque accru de deadlock du fait de l'allongement de la durée de verrouillage durant les traitements. En contrepartie la lecture (avec verrouillage) du row du groupe n'intervient qu'une fois. 

***En mode optimiste***, le traitement est fait sans verrouillage : les verrouillages des documents n'interviennent qu'au moment de la *phase finale d'écriture des documents en base*. Cette fois-ci les verrouillages des documents permettent a minima de vérifier que les versions n'ont pas changé depuis le début de la transaction (une requête un peu complexe `select for update` mais n'attaquant que des index). Si ça n'est  pas le cas l'opération est reprise.

### Mono / multi instances de serveur
En Datastore il n'y a pas de moyen (semble-t-il) de garantir qu'une seule instance de serveur tourne, même si c'est celle-ci peut assurer le débit nécessaire. Le mode "multi" est implicite.

En R-DBMS c'est un choix de configuration qui fixe le nombre d'instances en exécution parallèle : il est donc possible de fixer le choix en "mono".

Le mode "mono" permet plusieurs optimisations de la gestion de la mémoire cache du serveur qui en baissant la consommation de ressources permet de retarder le basculement en multi-instances : ce basculement augmente le débit mais la surconsommation de ressources est plus que linéaire.

En mono instance il n'est pas nécessaire de vérifier qu'un document détenu en cache a bien la dernière version : il l'a, presque, toujours (ou pas du tout). La base peut cependant être committée et la mise à jour de la mémoire cache à l'instruction suivante une fraction de milliseconde plus tard échouer (ou son thread temporairement être bloqué par le scheduler) : la relecture dans ce cas s'impose.

Le code générique est unique avec quelques courtes variantes.

