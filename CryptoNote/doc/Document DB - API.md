# Document DB - API

# Requêtes HTTP et Contextes d'exécution
Les sessions externes émettent des requêtes HTTPS qui peuvent être des GET ou des POST.

### Applications : `context-path`, ping, build
Le pool de serveurs peut servir plusieurs instances internes d'application, chacune relative à une organisation, l'URL étant différente selon qu'il y a ou un `context-path` `cp` :

    Sans context-path    : https://site.org/monOrg/xxx...
    Avec context-path cp : https://site.org/cp/monOrg/xxx...

A la place de `monApp` on peut avoir `admin` afin d'adresser l'application d'administration  globale des instances (on/off, gestion des tâches différées).  
Par la suite on ne considère les URLs qu'à partir de `monOrg/...`

##### GET `ping`

    (1) ping
    (2) monOrg/ping
    (3) admin/ping

Retourne un JSON avec :
- `t` : la date-heure sous la forme estampille : `180124175849876`
- `b `: le numéro de build : 264
- pour les formes 2 et 3 :
    - `off` : 0 ou 1 selon que l'instance est on ou off. L'opération `setOnOff` permet à l'administrateur de mettre on ou off une instance.
    - `db` : le message d'information stocké dans la table `dbinfo` de la base de l'instance. Si `?` elle n'est pas joignable.

**Queue Managers**
Certains hosts sont chargés de gérer la file des tâches différées pour une ou plusieurs instances : ils ont un code d'instance commençant par `qm` et le front end nginx se charge de router les quelques opérations spécifiques à un Queue Manager en détectant le nom d'instance.

## Ressources Web 
Les ressources web sont /var et /WEB-INF mais celle-ci n'étant pas accessible depuis un GET il ne reste que les ressources localisées sous `/var`.  
Une page d'accueil (home) pour l'instance `monOrg` ressemble à ça et a été générée en utilisant le template /var/index.html

    <!-- Partie générée sur le serveur en remplacement de la première ligne de index.html -->
    <!DOCTYPE html><html><head><base href="/cp/monOrg/var281/">
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>
    <script src='js/build.js'></script> <<<<<<<<<<<<<<<<(1)
    <script src='js/root.js'></script>  <<<<<<<<<<<<<<<<(2)
    
    <!-- Ce texte est inséré par le serveur en fonction de l'instance et de sa configuration ->
    <script type='text/javascript'>
    App.modeMax = 0;
    App.buildAtPageGeneration = 281;
    App.zone = "Europe/Paris";
    App.langs = ["fr","en"];
    App.lang = "fr";
    App.theme = "z";
    App.themes = JSON.parse('["z","a","b","en","fr"]');
    App.helpDic = JSON.parse('{"p12":{"p":"p11","t":"s1","refs":["p22"]},"p11":{"t":"s1","s":"p12","refs":["p22"]},"p22":{"p":"p21","t":"s1","refs":["p11","p12"]},"s1":{"t":"home","s":"s2","refs":["p11","p12"]},"p21":{"t":"s1","s":"p22","refs":[]},"home":{"refs":["s1","s2"]},"s2":{"p":"s1","t":"home","refs":["p21","p22"]}}');
    App.zDics = {};
    App.customThemes = {};
    App.zres = {
    };
    </script>
    <script src='js/fr/base-msg.js'></script>
    <script src='js/fr/app-msg.js'></script>
    <script src='js/fr/theme.js'></script>
    <script src='js/en/app-msg.js'></script>
    <script src='js/en/theme.js'></script>
    <script src='js/theme-z.js'></script>
    <script src='js/theme-a.js'></script>
    <script src='js/theme-b.js'></script>
    
    <!-- Le texte qui suit est directement celui du template index.html. ->
    <title>CN-test</title>
    <link rel="shortcut icon" type="image/png" href="z/z/icon.png">
    <script>App.setup();</script>                    <<<<<<<<<<<<<<<<<<<<<<<<(3)
    <script src='js/encoding.js'></script>
    <script src='js/pako.js'></script>
    <script src='js/bcrypt.js'></script>
    <script src='js/showdown.min.js'></script>
    <script src='js/util.js'></script>

    <script src="bower_components/webcomponentsjs/webcomponents-loader.js"></script>
    <link rel="import" href="bower_components/polymer/polymer-element.html">
    <link rel="import" href="base.html">
    <link rel="import" href="shared1.html">
    <link rel="import" href="shared2.html">
    <link rel="import" href="shared3.html">
    <link rel="stylesheet" type="text/css" href="fonts/roboto.css">
    <link rel="stylesheet" type="text/css" href="fonts/roboto-mono.css">
    <link rel="stylesheet" type="text/css" href="fonts/comfortaa.css">
    <link rel="import" href="app_components/app-homes.html">
    <script src='js/custom.js'></script>
    <script src='z/z/custom.js'></script>
    </head>
    <body>
    <app-homes></app-homes>
    </body>
    </html>

Dans ce texte générée par le servlet `281` est le numéro de build. Sa présence permet de mettre hors jeu les ressources gardées en cache d'une build antérieure : dans le répertoire déployé il n'apparaît que `../var/...`

#### Structure du /var

    app_components/
    bower_compoents/
    fonts/
    js/
    z/
    base.html
    bower.json
    bye.html
    index.html
    reload.html
    reload2.html
    reload3.html
    shared.html

**app_components/** : répertoire contenant les web components de l'application. Quand on souhaite ne pas laisser les scripts (sauf les très simples) dans le `.html` où ils peuvent poser problèmes de debug dans d'autres browser que Chrome, on les retrouve ici avec `.js`.

**bower_components/** : répertoire contenant les web components externes (Polymer etc.).

**fonts/** : répertoire contenant les fichiers `f1.css` des fontes f1 et `f1x.woff` référencés dans les `f1.css`.

**js/** : 
- un répertoire par langue contenant les fichiers **base-msg.js app-msg.js theme.js**
- les fichiers **.js** de l'application (en particulier ceux des web components) et les fichiers spécifiques suivants :
    - **build.js** : ce fichier est à changer à chaque build et son format est contraint.
    `appbuild = 281;`
    - **root.js** : premier script chargé, déclare la classe App et l'initialise, enregistre le service worker. Il est réellement exécuté à la ligne (3).
    - **util.js** : contient les classes utilitaires requises.
    - **sw.js** : template du script du service worker.
    - service worker : déclare les mime types acceptés des ressources et spécifie ceux qui peuvent subir une compression. **Peut être customisé**.
    - ***Scripts externes*** :
        - **encoding.js** : pallie pour Edge au manque de la classe `TextEncoder`.
        - **pako.js** : gzip en javascript.
        - **showdown.min.js** : conversion MD en HTML.
        - **bcrypt.js** : digest BCRYPT.
    - ***Scripts de définition des thèmes*** : **theme-z.js theme-a.js theme-b.js**

**z/** : 
- un sous répertoire pour chaque instance contenant ses surcharges spécifiques des valeurs par défaut qui sont dans `z/z/`. Par principe toutes les ressources requises se trouvent dans `z/z/` mais s'il en existe une de même nom pour le sous-répertoire d'une instance c'est celle-ci qui est prise quand cette instance s'exécute dans un browser.
- **custom.js** : classe d'interface entre la couche de base et l'application. La méthode `static ready()` est invoquée quand tout est chargé. Elle peut être différente d'une application à l'autre. Elle étend / surcharge `CustomTemplate` de `js/custom.js`.
- **shared1.html shared2-styles** : ils peuvent être inclus dans le `custom-style` dans les web components qui en ont besoin. Ces styles ne référencent pas d'attributs de présentation (couleur, taille, fonte) en dur mais uniquement des variables qu'on va trouver dans les thèmes.

**base.html** : c'est le `<custom-style>` applicable dans la page d'accueil et qui régit les dimensionnements des fontes selon la taille des pages afin que le reste de l'application s'exprime en `rem`.

**bower.json** : pour mettre à jour les web components externes. Pas d'intérêt pour l'application mais il doit être là.

**bye.html** : page d'adieu invoquée en sortie de l'application.

**index.html** : template de la page d'accueil générique.

**reload.html reload2.html reload3.html** : trois pages nécessaires pour arriver à faire recharger une nouvelle version dans le cas d'un service worker.

**WEB-INF**  
Il comporte les fichiers suivants :
- **web.xml** : contient principalement le nom de la classe d'application de configuration et toutes les liens vers les ressources jdbc requises (en gros la liste des bases).
- **appengine-web.xml queue.xml** : configuration pour GAE.
- **logging.properties** : configuration du logger.
- **base-config.json** : configuration de l'application et des instances.
- **app-config.json** : configuration complète spécifique de l'application.

## Pages d'accueil et URLs
Chaque instance peut avoir une ou plusieurs pages d'accueil :
- chaque page d'accueil a un nom `home admin index` ... et la configuration spécifie si la page peut être en mode `sync` ou `avion`.
- la page générique d'accueil `app-homes.html` reprend toutes les pages (d'accueil  ou non) possibles pour toutes les instances, sachant que plusieurs instances peuvent avoir une même page d'accueil.
- à chaque page d'accueil correspond une *top bar* qui ne changera pas alors que la navigation permet de passer à d'autres pages que celle d'accueil. Toutes les pages possibles figurent dans `app-homes`.

### URLs d'une instance

    monOrg/home2      >>> page d'accueil home2 de l'instance monOrg en incognito
    monOrg/s/home2    >>> page d'accueil home2 de l'instance monOrg en mode sync
    monOrg/s/home2.a  >>> page d'accueil home2 de l'instance monOrg en mode avion
    
    monOrg/var...     >>> ressources accessibles par GET en mode incognito
    monOrg/s/var...   >>> ressources accessibles par GET en mode sync et avion
    
    monOrg/x.appcache >>> en attente de passage en service worker
    
    monOrg/s/sw.js    >>> script du service worker spécifique à cette instance
    
    monOrg/ping       >>> ping en GET et POST
    monOrg/op/...     >>> appel d'une opération en POST 
    monOrg/od/...     >>> appel d'une opération différée en POST
    

***Raccourcis d'URLs*** pour les pages d'accueil 
La configuration permet de définir des URLs raccourcies correspondant aux chargements des pages d'accueil : ceci permet de fournir aux utilisateurs une URL plus simple et en particulier de sauter le *code de l'organisation*. In fine dès le début du GET la substitution intervient et c'est comme si l'utilisateur avait saisi l'URL complète.

    https://site.org/cp/ -> https://site.org/cp/monOrg/home2
    https://site.org/cp/ad -> https://site.org/cp/admin/admin

#### Ressources en /var/z
Une ressource `/var/z/z/toto.png` demandée par l'instance `monOrg` est obtenue depuis :
- `/var/z/monOrg/toto.png`,
- si elle n'a pas été trouvée elle est recherchée en `/var/z/z/toto.png`

Le répertoire `/var/z/z/...` contient toutes les ressources par défaut qui sont en priorité recherchées dans le répertoire spécifique de l'instance `monOrg` `/var/z/monOrg/...`

#### Traduction des messages
Un message a un **code** et un **texte** dans lequel les arguments `{0} {1} ...` sont remplacés par les paramètres positionnels passés lors de l'obtention du texte. Les simples `quotes` doivent être doublés, les `doubles quotes` précédés d'un `backslash`.  
Un fichier de traduction est un script `.js` qui respecte cette syntaxe :

    App.setAll("en", {
    "ns_test":"Test : workgroups and associations",
    "ns_admin":"Hosted Organizations Administration" 
    });

Tout ce qui est entre le premier `{` et le dernier `}` (inclus) respecte strictement une syntaxe JSON.

Les fichiers de traductions pour une langue donnée `fr` et une instance donnée `monOrg` sont chargés dans l'ordre suivant, le dernier trouvé étant retenu :

    js/fr/base-msg.js js/fs/app-msg.js z/monOrg/fr/base-msg.js z/monOrg/fr/app-msg.js 

#### Définition des thèmes graphiques
Les thèmes graphiques sont nommés `a b ...` Le thème `z` est le thème virtuel contenant les valeurs par défaut.  
- il existe de plus un correctif de thème par langue, pour les quelques données CSS pouvant dépendre de la langue (`after` / `before` et quelques images.
- un thème est défini par une liste de variables CSS sous la syntaxe ci-dessous.
- la liste des valeurs finalement applicables pour le thème `a` en langue `fr` résulte de l'application des scripts : `js/theme-z.js js/theme-a.js js/fr/theme.js`

*Syntaxe d'un fichier thème :*

    App.setTheme("z", {
    "--font-mono" : "'Roboto Mono'",
    "--font-std" : "'Roboto'",
    "--font-cf" : "'Comfortaa'",
    "--btnstd-color": "var(--paper-indigo-900)",
    ...
    });

## Invocation d'opérations
URLs : `/cp/monOrg/op/p1/p2 ...`  ou `cp/monOrg/od/taskid/step`

Les autres arguments sont passés soit en `application/x-www-form-urlencoded` (sur un GET) ou en `multipart/form-data` (sur un POST) :
- `op/` :  obligatoire, signale un appel d'opération. Ce code est `od/` pour une opération différée *émise* par le Queue Manager.
- `p1 p2 ...` sont facultatifs. Ils peuvent être obtenus par `inputData.uri()` qui retourne un `String[]`;
- les ***arguments*** sont récupérables par `inputData.args().get("nom argument")`:
    - l'argument de nom **`op`** donne le nom de l'opération et permet de trouver la classe de l'opération à instancier.
    - les arguments de nom **`account key sudo`** sont en général utilisés pour gérer les habilitations à effectuer l'opération.
    - l'argument de nom **`param`** est traité spécifiquement : c'est un JSON et il est dé-sérialisé dans le champ de nom `param` de l'opération.
    - ***dans le cas d'un POST des pièces jointes peuvent être transmises*** : elles sont récupérables par `inputData.attachments().get("name")` qui retourne un objet (ou `null`) portant les informations `name, fileName, contentType, bytes`.

## `ExecContext`
Un objet de la classe `ExecContext` est créé par l'arrivée d'une requête et suit son thread durant sa vie.  
Cet objet se récupère par `ExecContext.current()` à n'importe quel endroit du code.

Quelques informations disponibles sur `ExecContext` :

    public boolean hasAdminKey() 
Retourne true si l'argument de la requête `admin` en base 64, représente un `byte[]` dont le SHA-256 en base 64 est égal au paramètre de configuration `secretKey`.

    public boolean hasQmKey() 
Retourne true si l'argument de la requête `admin` en base 64, représente un `byte[]` dont le SHA-256 en base 64 est égal au paramètre de configuration `qmSecretKey`.

    public int phase() 
Retourne numéro de phase ou s'en trouve l'exécuion.

    public String operationName()
    public Operation operation()
Retournent le nom de l'opération en cours et son objet (du moins quand la requête en est à une phase où une opération est en exécuttion `work()` ou `afterwork()`).

    public String ns()
Retourne le code du namespace sur lequel la requête s'effectue.

    public boolean isTask()
Indique si l'opération est normale ou différée.

    public InputData inputData()
Retourne l'objet `InputData` contenant le sparamètres de la requête :
- `uri()` : partie utile de l'URL de la requête (après la partie `namespace` et `op`).
- `args()` : une map donnant les arguments passés sur le POST (habituellement sans utilité).
- `attachments()` : map des fichiers attachés, clé par leur nom d'attachement. Chaque `Attachment` contient :
    - `String name` : son nom d'attachement.
    - `String fileName` : son nom de fichier dans la session.
    - `String contentType` : son type MIME dans la session.
    - `byte[] bytes` : son contenu.

.

    public void trace(String msg)

**Dans le seul cas d'une opération différée** il est possible d'accumuler des traces d'exécution textuelles simples et de les retrouver en cas de sortie en exception dans le rapport d'erreur stocké par le Queue Manager. C'est sans objet sur une requête émise par une requête interactive.

    public DBProvider dbProvider()

Le DBProvider est l'objet en charge d'accéder directement au stockage de données sans passer par l'intermédiaire normal des documents. Son emploi n'est nécessaire que pour écrire des recherches spécifiques.

Le contexte d'exécution a pour fonction :
- de créer un objet `Operation` en initialisant sa propriété `param` avec le contenu dé-sérialisé de l'objet JSON reçu en argument `param` de la requête. L'opération trouve dans l'objet Java `param` les arguments (du moins la plupart) du travail à effectuer.
- de garder dans une mémoire cache de travail spécifique à ce contexte tous les objets `Document` des documents accédés / créés / modifiés / supprimés.
- de garder trace des demandes de création de tâches différées.
- d'appeler la méthode `work()` de l'objet `Operation`.
- d'effectuer la validation des documents créé / modifiés / supprimés et des tâches créées  / modifiées / annulées.
- d'appeler la méthode `afterwork()`.
- en cas d'exception (et pour certaines d'entre elles seulement) de recommencer le cycle création d'opération `work()` / validation / `afterwork()`.
- de calculer les synchronisations de documents requises dans la requête.
- de mettre en forme et retourner le résultat à retourner à la session appelante et de terminer la requête.

## Opération
L'objet `ExecContext` de la requête crée un objet `Operation` correspondant à l'opération souhaitée par la requête (paramètre `op` de la requête).  
Les classes de document (qui étendent `Document`) peuvent avoir des classes internes statiques qui étendent la classe `Operation` : celles-ci sont automatiquement enregistrées comme classe d'opération.  
Les opérations figurant dans une classe séparée doivent être déclarées à la configuration.

#### La propriété `param` d'un objet opération
La classe d'une opération (qui étend `Operation`) doit avoir :
- une classe interne statique (sérialisable en JSON, même contrainte qu'un Item) usuellement dénommée `Param`.
- une propriété d'instance nommée `Param param`.

Lorsqu'une opération est lancée, le paramètre de la requête HTTPS nommé `param` est supposé être un JSON qui va être dé-sérialisé selon la classe `Param` et stocké dans la propriété `param` de l'objet `Operation`: les paramètres de l'opération s'y trouvent, même si parfois d'autres paramètres peuvent plus exceptionnellement figurer dans l'URI de la requête et des pièces jointes de la requête.

#### Méthodes génériques de la classe `Operation`
L'objet `Operation` a en générique quelques services :

    public ExecContext execContext()
    public InputData inputData()
    
    // retourne et mémorise l'objet taskChekPoint (tâche seulement)
    public Object taskCheckpoint()
    public void taskCheckpoint(Object obj)
    
    // retourne le résultat à remplir par l'opération
    public Result result()
    
    // retourne le Document.Id identifiant une tâche
    // quand un document existe pour cette id, son contenu donne les paramètres de la tâche
    public Document.Id taskId()
    public boolean isTask()
    
    public Stamp startTime()
    
    // Descripteur technique de l'opéaration
    public OperationDescriptor descr()
    public String name()
    
    public String ungzip(Attachment a)
    public void gzipResultat(String res)

#### Résultat d'une opération
L'objet résultat peut être rempli selon l'une des trois options suivantes :
- (1) `Object out` : c'est un objet applicatif quelconque qui sera sérialisé en JSON.
- (2) `String text / String encoding` : le résultat est considéré comme un texte et son encoding peut être donné s'il n'est pas UTF-8.
- (3) `byte[] bytes / String mime` : le résultat est en binaire et son type MIME est donné.

Un résultat de synchronisation (c'est un String en JSON mis dans la propriété `syncs`) n'est possible que quand le résultat est dans l'option (1) (`Object` à sérialiser en JSON).

Il n'est pas obligatoire de déclarer un résultat : un `Object` laissé vide est retourné à la session appelante.

#### Gestion des opérations différées

    public void setTask(Document.Id id, long nextStart, String info)

Cette méthode déclare une opération différée identifiée par le `Document.Id` fourni et sera lancée au plus tôt à la date-heure `nextStart` donnée.  
Un String d'information facultatif peut être donné et apparaîtra dans les pages de gestion du Queue Manager (c'est son seul rôle).

      public void setTaskByCron(Document.Id id, String cron)
Même fonctionnalité mais la `nextStart` est calculée selon le `String cron` passé en argument (qui sera mis en info).

Un objet `TaskInfo` contient principalement :
- `String ns` : le namespace sur lequel l'opération porte.
- `Document.Id id` : son identité.
- `long nexstart` : sa date-heure de lancement au plus tôt.
- `int retry` : le numéro de l'essai d'exécution (usuellement 0).
- `String info` : le commentaire informatif donné à la création.

# Sérialisation JSON d'un document
La forme générale est la suivante:

    {
    "c":"C",
    "id":"abcd",
    "v":1712... ,
    "ct":1712... ,
    "dt":1712... ,
    "dels": [
        {"c":"S4", "v":1712...},
        {"c":"S5", "k":"def", "v":1712...}
        ],
    "items": [
        {"c":"S2", "v":1712... "s":"texte d'un raw"},
        {"c":"S1", "v":1712... "j":{ l'item en JSON }},
        {"c":"R1", "k":"def", "v":1712... "s":"texte d'un raw"},
        {"c":"I1", "k":"abc", "v":1712... "j":{ l'item en JSON }}
        ],
    "clkeys":["S1", "I1.abc", "I1.def", "R1.def"... ]
    }
    
    Document supprimé :  {"c":"C", "id":"abcd", "v":0 , "ct":1712... }
    
    1A
    source ----------dtr-----------vr
    cache  ----------------dt---v
    maj1   ----------------dt------vr suppr depuis dt / clkeys vide
    maj2   ----------------dt------vr suppr depuis dt / clkeys vide
    
    1B
    source ----------dtr-----------vr
    cache  ------dt-------------v
    maj1   ----------dtr-----------vr suppr depuis dtr / clkeys vide
    maj2   ----------dtr-----------vr suppr depuis dtr / clkeys vide
    
    2A
    source ----------dtr-----------vr
    cache  --dt---v
    maj1   ----------dtr-----------vr suppr depuis dtr / clkeys
    maj2   ------------------------vr vr/vr suppr vide / clkeys vide
    
    2B
    source ----------dtr-----------vr
    cache  v
    maj1   ----------dtr-----------vr suppr depuis dtr / clkeys
    maj2   ------------------------vr vr/vr suppr vide / clkeys vide
    
La mémoire cache à remettre à niveau contient un exemplaire de `d` de version `v` et de `dtime` `dt`. 
La mémoire source de la remise à niveau contient un exemplaire de `d` de version `vr` et de `dtime` `dtr`.  
`items` contient toujours les items créés / recréés / modifiés après `v`.
Sous option `maj1` on garde dans le document de mise à jour l'historique des destructions le plus large possible.  
Sous option `maj2` on limite au maximum dans le document de mise à jour l'historique des destructions.  
Dans le cas 2B, le cache ne connaît rien (`v = 0`).  
Lorsque les `ctime` diffèrent, on se ramène au cas 2B.  
`clkeys` ne contient pas les clés des items créés /recréés / modifiés qui figurent déjà dans `items`.

Après analyse de la situation de départ, les options de calcul sont :
- Cas 1 : `clkeys` vide. Option maj1/maj2 ignorée.
    - suppressions postérieures à dtx = max(dt, dtr)
    - en sortie : vr->version ctime->ctime dtx->dtime
- Cas 2 : option maj1. clkeys NON vide.
    - suppressions postérieures à dtr
    - en sortie : vr->version ctime->ctime dtr->dtime
- Cas 2 : option maj2. suppressions vide. clkeys vide.
    - en sortie : vr->version ctime->ctime vr->dtime


# Accès aux documents par la classe `Document`
Chaque classe de document a une classe qui hérite de `Document`.   
Chaque classe d'item est une classe statique interne portant le nom de la classe d'item (relative au document) héritant de :  
`Document.Item .RawItem .Singleton .RawSingleton` selon le cas.

### Méthodes générales applicables à un document
Le `Status` d'un document traduit l'état du document à l'instant t vis à vis de son cycle de vie et de ce qu'il convient de faire à la validation de l'opération si les choses en restaient là :
- `unchanged` : aucun item n'a changé sur le document qui existait avant le début de l'opération.
- `modified` : le document existait avant le début de l'opération et un ou des items ont été créés / modifiés / supprimés.
- `created` : le document n'existait pas et il a été créé par un `getOrNew()`. Il n'a pas forcément d'items.
- `recreated` : le document existait et a été recréé par la méthode `recreate()`. Sa date-heure de création n'est pas la même qu'au début de l'opération et son contenu a complètement été purgé par la suppression survenue avant sa recréation.
- `deleted` : le document existait mais a été supprimé par l'opération.
- `shortlived` : le document n'existait pas, a été créé par un `getOrNew()` et a été supprimé ensuite au cours de l'opération.

Les principales propriétés d'un document sont :
- `Document.Id id()` : identifiant du document.
- `long ctime()` : date-heure de création, telle qu'elle était connue au début de l'opération.
- `long dtime()` : **tous** les items supprimés après cette date-heure sont disponibles en cache (et marqués supprimés).
- `long version()` : date-heure de création ou dernière modification, telle qu'elle était connue au début de l'opération.
- `Status status()` : état du document.
- `isRO()` : `true` si le document est en lecture seule.

      public int nbExisting() : nombre d'items existants
      public int nbToSave() : nombre d'items qui seront sauvés à la validation
      public int nbToDelete() : nombre d'items qui seront détruits à la validation
      public int nbTotal() : nombre total d'items
      public long v1() : volume de class (1) occupé
      public long v2() : volume de classe (2) - pièces jointes - occupé.

Ces dernières méthodes n'ont de valeurs significatives que juste après l'appel de la méthode **`summarize()`** qui scanne tous les items pour calculer les volumes et les dénombrer.

### Obtention d'une instance de document
      public static Document get(Document.Id id, int maxDelayInSeconds) 
      throws AppException
      public static Document getOrNew(Document.Id id) throws AppException
      
La première méthode retourne le document dont l'identification est donnée et `null` s'il n'existe pas. La contrainte de fraîcheur peut être spécifiée afin de tolérer un document trouvé en mémoire cache et d'âge compatible avec les exigences métier. Si cette valeur n'est pas 0 (par convention la version la plus récente possible) le document est en lecture seule.

La seconde méthode retourne un nouveau document s'il n'en existe pas un.

>Dans une opération pour un document d'identification donnée toutes les variables de retour d'un `get()` pointe vers le même objet : un document donné n'existe qu'en un exemplaire unique dans l'espace d'une opération.

Usuellement chaque classe de document écrit ses propres méthodes `get()` / `getOrNew()`, ayant le cas échant des paramètres métiers pertinents et en castant le résultat dans la classe du document cherché / créé.

### Suppression et recréation d'un document
    public void delete() throws AppException
    public static void delete(Document.Id id) throws AppException
    public void recreate() throws AppException

Un document peut être supprimé,
- soit depuis son instance elle-même : c'est obligatoire quand les conditions de sa suppression sont liées à son contenu. Il est vérifié à la validation que la version du document n'a pas progressé depuis.*
- soit en `static` depuis la classe elle-même en donnant son Id quand la logique de suppression ne se base pas sur le contenu du document (éventuellement juste son Id).  

La recréation d'un document le met à vide et sa `ctime` est réinitialisée : une nouvelle vie du document commence.

### Accès aux items d'un document
Les méthodes ci-dessous sont génériques mais usuellement une classe de document écrit ses propres accesseurs, le cas échéant avec des paramètres métiers adéquats et en castant les résultats dans la classe de l'item souhaité.

    public Singleton singleton(Class<?> itemClass)
    public Singleton singletonOrNew(Class<?> itemClass)
    public Item item(Class<?> itemClass, String key)
    public Item itemOrNew(Class<?> itemClass, String key)

Les méthodes retournant des singletons n'ont pas de paramètre `key`.  
Toutes les méthodes prennent en premier paramètre la classe de l'item souhaité.
Les méthodes `...OrNew` créé un item s'il n'existait pas alors que les autres retournent `null` dans ce cas.

    public String[] getKeys(Class<?> itemClass)
Cette méthode retourne toutes les clés des items (non singleton) existant pour la classe citée.

L'accès aux propriétés d'un item JSON est celui standard d'accès aux propriétés en Java.   
**Après un ou plusieurs changements dans un item item il faut appeler la méthode `commit()` de l'item pour valider ces changements** : sinon les modifications de l'item seront perdues.

Pour un `RawItem` ou un `RawSingleton` sa valeur est la propriété unique `String value`.

Après une série de changement de valeurs des propriétés d'un item, un appel `void commit()` pour les valider. Plusieurs appels commit() peuvent être effectués.

    Document _document() : retourne le document auquel appartient de l'item
    public ItemDescr descr() : retourne le descripteur de l'item
    String key() : retourne la clé de l'item
    String clkey() : retourne sa classe pour un singleton ou sa classe "." sa clé pour un item.
    long version() : retourne sa version au début de l'opération (si 0, l'item à été créé)
    long vop() : retourne la date-heure de l'opération l'ayant mis à jour, inférieure à version dans le cas d'une réplication désynchronisée.
    int v1() : retourne le volume (1) occupé par l'item.
    int v2() : dans le cas d'un item P retourne son volume (2) (la pièce jointe)
    boolean toSave() : retourne true si l'item a été créé ou modifié
    String cvalue() : retourne sa valeur courante sérilisée
    boolean deleted() : si true l'item détruit.
    boolean toDelete() : si true l'item sera détruit à la prochaine validation.
    boolean created() : si true l'item vient d'être créé dans l'opération.
    
    void commit() : valide une ou plusieurs modification de l'item.
    void delete() : supprime l'item.


#### Items / singletons, détachés et rattachés
Quand on obtient un item par `get()` ou `getOrNew()` l'item est instancié et automatiquement *attaché* au document dont il fait partie.  
Si on créé ce même item par un `new MonItem()` standard Java, on obtient un objet de la classe `MonItem` mais détaché de tout document : typiquement ce peut être le cas au cours d'une procédure d'importation depuis une source externe.   
Cet item détaché peut venir remplacer l'item actuel par l'appel d'une de ces méthodes (Item ou Singleton) appliqués sur l'item créé détaché :

    void replaceIn(Document d, String key)
    void replaceIn(Document d)

par exemple :

    MonItem it = new MonItem();
    it.foo = "bar";
    it.replaceIn(monDoc, "clé2");

### Les items de classe prédéfinie P
Chaque instance de ces items représente une pièce jointe, un fichier attaché au document.  
Un item P a des méthodes spécifiques, son contenu n'étant pas directement lisible.  
Pour l'obtention dans un document :

    P p(String key)
    String[] getPKeys()

Pour la lecture des propriétés d'un item P :

    // Méthodes spécifiques d'un item P
    String mime() : retourne le type mime de la pièce jointe
    String sha() : retourne son sha.
    int size() : retourne sa taille en bytes.
    byte[] blobGet() : retourne le contenu de la pièce jointe.
    
Une seule méthode sur un document permet de créer / modifier un item P en donnant son contenu binaire: 

    P blobStore(String key, String mime, byte[] bytes)
La méthode retourne l'item créé ou modifié.

### Browse items
Ces méthodes permettent d'effectuer une action / dénombrer les items selon une action / filtre quelconque.

    @FunctionalInterface public interface CIAction {
      public boolean action(CItem ci);
    }
    @FunctionalInterface interface CICounter {
      int count(CItem ci);
    }
    void browse(CIAction a) : exécute l'action indiquée sur tous les items
    int count(CICounter c) : compte tous les items ayant répondu true

On peut obtenir l'item lui-même dans des méthodes CIAction / CICounter en provoquant la dé-sérialisation de son CItem associé, par la méthode :

    public BItem item(CItem ci)

Il faut caster le résultat dans la classe effective de l'item souhaité.

## Contrôle des synchronisations
Une synchronisation est une requête dont l'objectif est de récupérer les items modifiés de documents afin de remettre à jour ceux-ci dont la session requérante détient, ou non, une copie possiblement retardée.  
Une synchronisation peut être une requête spécifique ou intervenir après la bonne exécution d'une opération, en particulier de mise à jour.

>Pour chaque document concerné, selon sa classe, son identifiant et pour chaque item du document, sa classe, son identifiant sa clé puis enfin son contenu, il peut être décidé d'accepter ou non l'élément et finalement le cas échéant avec seulement certaines des propriétés.

Une synchronisation est définie par une liste de document à synchroniser. Cette liste parvient en JSON dans le paramètre `syncs` de la requête. Chaque item de cette liste `[{"c": ...}, {...}, ...]` correspond à un objet de la classe `Sync` et décrit un document à synchroniser :

    String c; // classe du document
    String id; // docid du document
    long v; // version du document détenue
    long ct; // date-heure de création
    long dt; // toutes les suppressions d'items après cette date-heure sont connues 
    String filter; // Objet de classe docclass.SyncFilter en Json

Le `String filter` est lui-même le JSON d'un objet qui sera instancié selon la classe interne `static Filter` de la classe du document correspondant.

    La mémoire cache à remettre à niveau contient un exemplaire de `d` de version `v` et de `dtime` `dt`. 
    La mémoire source de la remise à niveau contient un exemplaire de `d` de version `vr` et de `dtime` `dtr`.  
    `items` contient toujours les items créés / recréés / modifiés après `v`.
    {
    "c":"C",
    "id":"abcd",
    "v":1712... ,
    "ct":1712... ,
    "dt":1712... ,
    "dels": [
         {"c":"S4", "v":1712...},
         {"c":"S5", "k":"def", "v":1712...}
         ],
    "items": [
         {"c":"S1", "v":1712... "j":{ l'item en JSON }},
         {"c":"R1", "k":"def", "v":1712... "s":"texte d'un raw"},
         {"c":"I1", "k":"abc", "v":1712... "j":{ l'item en JSON }}
         ],
     "clkeys":["S1", "I1.abc", "I1.def", "R1.def"... ]
    }
     
    Document supprimé :  {"c":"C", "id":"abcd", "v":0 , "ct":1712... }
    
    1A
    source ----------dtr-----------vr
    cache  ----------------dt---v
    maj1   ----------------dt------vr suppr depuis dt / clkeys vide
    maj2   ----------------dt------vr suppr depuis dt / clkeys vide
    
    1B
    source ----------dtr-----------vr
    cache  ------dt-------------v
    maj1   ----------dtr-----------vr suppr depuis dtr / clkeys vide
    maj2   ----------dtr-----------vr suppr depuis dtr / clkeys vide
     
    2A
    source ----------dtr-----------vr
    cache  --dt---v
    maj1   ----------dtr-----------vr suppr depuis dtr / clkeys
    maj2   ------------------------vr vr/vr suppr vide / clkeys vide
     
    2B
    source ----------dtr-----------vr
    cache  v
    maj1   ----------dtr-----------vr suppr depuis dtr / clkeys
    maj2   ------------------------vr vr/vr suppr vide / clkeys vide
    
    Sous option `maj1` on garde dans le document de mise à jour l'historique des destructions le plus large possible.  
    Sous option `maj2` on limite au maximum dans le document de mise à jour l'historique des destructions.  
    Dans le cas 2B, le cache ne connaît rien (`v = 0`).  
    Lorsque les `ctime` diffèrent, on se ramène au cas 2B.  
    `clkeys` ne contient pas les clés des items créés /recréés / modifiés qui figurent déjà dans `items`.
    
    Après analyse de la situation de départ, les options de calcul sont :
    - Cas 1 : `clkeys` vide. Option maj1/maj2 ignorée.
        - suppressions postérieures à dtx = max(dt, dtr)
        - en sortie : vr->version ctime->ctime dtx->dtime
    - Cas 2 : option maj1. clkeys NON vide.
        - suppressions postérieures à dtr
        - en sortie : vr->version ctime->ctime dtr->dtime
    - Cas 2 : option maj2. suppressions vide. clkeys vide.
        - en sortie : vr->version ctime->ctime vr->dtime

Le retour d'une synchronisation est un vecteur similaire où chaque élément correspondant à un document *peut* avoir changé.

#### Classes héritant de `Document.ISyncFilter`
Chaque classe de Document *peut* avoir une classe `static` `Filter` héritant de `Document.ISyncFilter`.  
Si cette classe existe pour une classe de document, lors de chaque synchronisation relative à un document de cette classe un objet filtre est créé :
- soit depuis le constructeur par défaut ;
- soit depuis l'objet reçu en JSON comme paramètre de filtre passé par la requête de synchronisation.

Les propriétés de cet objet sont les paramètres de filtrage spécifiques au document à synchroniser et en particulier les paramètres éventuels d'authentification : *en fonction de ces paramètres, le document est-il à synchroniser et si oui quels items en particulier*.

`Document` a plusieurs méthodes surchargeables par héritage :  
***Filtre sur l'identifiant du document et sa classe selon l'environnement de la requête***

    FilterPolicy filter(ISyncFilter sf)
Cette méthode retourne la politique à mener pour le document lui-même en fonction de sa **classe** et son **identification** :
- `Accept` : accepter tous les items du document ;
- `Exclude` : exclure de la synchronisation ce document ;
- `Continue` : l'acceptation ou l'exclusion des items dépend de l'identifiant de chaque classe d'item.

A noter que cette méthode peut stocker dans l'objet de filtre des informations  utiles à la poursuite de la synchronisation pour ce document.

***Filtre sur les classes d'items et leur clé:***

    FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String key)

Cette méthode retourne la politique à mener pour les items du document en fonction de leur classe et de leur clé éventuelle :
- `Accept` : accepter l'item ;
- `Exclude` : exclure cet item de la synchronisation ;
- `Continue` : l'acceptation ou l'exclusion dépend du contenu de l'item.

***Filtre sur le contenu même de l'item***

    FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String key, Item item)
    FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String key, Text text)
    public static class Text {
        public String src;
        public String filtered;
      }

Cette méthode retourne,
- `Exclude` : l'item n'est pas synchronisé.
- `Accept` : l'item est synchronisé dans son intégralité et son état initial.
- `Continue` : la méthode a pu modifier l'item, masquer certains champs ou en calculer d'autres... c'est l'item modifié passé en argument qui est synchronisé.  
La seconde méthode s'applique aux items opaques : si la propriété `filtered` est non `null` c'est la valeur à retourner en synchronisation au lieu de la valeur `src` réelle du contenu de l'item.  

***Sérialisation d'un document***

    String toJson(String filter, long ctime, long version, long dtime, boolean shortest)
    String toJson()

Cette méthode retourne la sérialisation JSON du document de delta en supposant qu'un état antérieur était connu et y applique les éléments de filtre décrits ci-dessus.  
- `ctime` : date-heure de création de l'exemplaire détenu.
- `version` : version de l'exemplaire détenu.
- `dtime` : dans l'exemplaire détenu toutes les suppressions au delà de dtime sont connues.
- `shortest` : si vrai les items supprimés ne sont pas transmis (dtime = version). 

La seconde méthode suppose qu'aucun exemplaire n'est détenu et que les items supprimés ne sont pas demandés : bref c'est l'état courant complet du document.

# Accès documents du cache `Cache`
Chaque instance de serveur dispose d'une mémoire cache gérée par la classe `Cache` où sont stockés les derniers documents accédés.

Tout accès à un document passe toujours par la mémoire cache et les documents y sont en lecture seule.

La mémoire cache est séparée par `namespace` : il est en conséquence possible d'accéder à des documents d'un autre namespace, du moins en lecture. Acquérir d'abord la mémoire cache, soit de son namespace courant soit d'un autre :

    public static Cache current()
    public static Cache cacheOf(String namespace)

    public Document document(DocumentId id, Stamp minTime, long versionActuelle)
Le document est retourné de la mémoire cache (après y avoir été mis s'il n'y était pas). Il est en lecture seule. Le retour est null si la version obtenue ne peut pas être meilleure que celle détenue (`versionActuelle`).

# Sélection des identifiants de documents et des items
### Recherche générique sur les valeurs des propriétés d'un item
L'objectif est de pouvoir rechercher une liste de documents dont l'un des items a une valeur de propriété indexée recherchée (par exemple un code postal donné et les premières lettres d'un nom, ou une adresse e-mail ...).
 
Ce genre de recherche n'est fait que pour retourner un nombre réduit de documents.

    public static Collection<Document.Id> searchDocIdsByIndexes(Class<?> docClass, Class<?> itemClass, Cond<?>... ffield) throws AppException
    
    public static Collection<XItem> searchItemsByIndexes(Class<?> docClass, Class<?> itemClass, XItemFilter filter, Cond<?>... ffield) throws AppException 
    public static class XItem {
        public String docId;
        public String key;
        public long version;
        public BItem item;
    }


*Une ou plusieurs filtres portent sur les propriétés indexées* de l'item. Chaque filtre doit avoir pour attribut `name` le nom de la propriété filtrée. Seule la première condition peut être **une non `EQ`**.

Le résultat est une `Collection` de `Document.Id` dans le premier cas et une Collection de XItem (item + méta données).

En GAE Datastore la recherche est prioritairement sur les conditions de filtre des propriétés indexées et d'abord sur la première d'entre elle seule à pouvoir être non `EQ` (conditions d'inégalité). Les éventuelles autres conditions d'inégalité ne sont appliquées que sur le résultat retourné par le Datastore qu'il réduit.

# L'espace secondaire de stockage des fichiers attachés aux documents
Selon les providers ce stockage est implémenté sur un service Cloud ou un File-System local.  
Une classe abstraite `BlobProvider` donne les services minimum à fournir et chaque provider a son implémentation spécifique.

Le stockage des blobs s'effectue par document : chacun y est identifié par son sha-256.  
- Un blob est stocké au cours d'exécution de `work(`) dans une opération, même si l'opération n'est pas validée. 
- Un blob n'est pas détruit quand il est détruit au cours d'un `work()` (sa validation n'étant pas acquise).
- Ce principe prend le risque d'accumuler des fichiers qui ne sont pas / plus référencés dans le document.
- L'avantage est que ça laisse du temps pour synchroniser dans une session distante une image cohérente d'un document et de ses pièces jointes assurées de pouvoir être obtenues conformément à l'état du document synchronisé dans une version donnée. 
- En contrepartie ceci suppose des nettoyages périodiques.

.

    void blobDeleteAll(String clid) : suppression physique de tous les blobs du document
    void blobStore(String clid, String sha, byte[] bytes) : stocke un blob
    byte[] blobGet(String clid, String sha) : retourne un blob
    boolean cleanup(String clid, HashSet<String> shas)
    

Le nettoyage d'un document est lancé par une tâche qui s'exécute dans les N heures après une création / modification / suppression de ses pièces jointes.  
Si rien ne bouge pendant un certain temps sur un document aucun nettoyage n'est lancé et si des modifications frénétiques sont effectuées, au plus un nettoyage intervient par groupe de N heures (1, 2, 3, 4, 6, 8 ou 12) configurable.  

Le nettoyage,
- lit la liste des sha des fichiers qui avait été *mis à la corbeille* lors du cycle précédent et supprime les fichiers correspondants, sauf quand ils sont dans la liste des sha encore référencés ;
- lit tous les noms des fichiers stockés et les inscrit dans la corbeille quand ils ne sont plus référencés dans `shas` : ils sont simplement ainsi *candidats* à être purgés au tours suivant (sauf ceux qui d'ici là sont de nouveau référencés).

# Configuration des Namespaces
Chaque namespace a quelques attributs techniques que l'administrateur peut fixer :
- `status` : il peut prendre les valeurs suivantes :
  - `0` : espace accessible normalement (lecture et écriture) ;
  - `1` : espace accessible temporairement aux seules opération n'agissant qu'en lecture ;
  - `2` : espace temporairement non accessible, sauf aux requêtes d'administration ;
  - `3` : espace définitivement non accessible (en destruction) mais pouvant peut-être un jour être réactivé.
- `info`  un court texte à propos du status ;
- `theme` :  lettre identifiant le thème visuel utilisé ;
- `label` : un intitulé plus long pour l'espace de noms ;
- `qm` : identifiant du Queue Manager gérant le namespace ;
- `nsbuild` : numéro d'ordre de la dernière modification du thème, du label, du queue manager, ou de l'une des ressources.

Quelques fichiers de ressources peuvent être définis, typiquement :
- `custom.css` : directives CSS intégrées à la suite du thème associé à l'espace ;
- `custom.js` : un script *javascript* exécuté après les autres juste avant le démarrage effectif de l'application dans le browser ;
- `logo.png` : un fichier image représentant un logo pour la page d'accueil de l'application graphique.

Toutes ces ressources ont obligatoirement une valeur par défaut (figurant dans `/var/z/...`) : en cas d'absence de valeur spécifique à l'espace de noms c'est la valeur par défaut de "z" qui est utilisée.  
L'espace `z` n'est pas configurable dynamiquement, une erreur risquant de rendre tous les namespaces inaccessibles : il ne peut l'être que dans la livraison de l'application (qui, elle, est testée avant mise en ligne).

Une session externe standard ne peut accéder qu'à un seul namespace fixé dans son URL d'appel (et reconduite dans toutes les URLs d'opération).

Une session externe d'administration des namespaces peut accéder à la liste des namespaces afin de les initialiser, les mettre on/off ou les configurer.

La configuration d'un namespace est contenue dans un document de classe `Namespace` :
- ayant pour `docid` le code du namespace,
- mémorisé dans le namespace `z` (en réalité dans le premier cité dans la configuration de l'application).

# Stockage en R-DBMS
Les colonnes `contentt` et `contentb` vont de pair :
- si le texte (typiquement un JSON) est court il est stocké dans `contentt`;
- sinon il est gzippé et stocké en `contentb`. L'une des deux colonnes est toujours `null`.

### Table `dbinfo`
    CREATE TABLE dbinfo (
    info text NOT NULL
    );
Cette table contient une constante : elle sert principalement au serveur à vérifier qu'il peut accéder à la base de données au lancement.

### Table `s2cleanup`
    CREATE TABLE s2cleanup (
    clid varchar(255) NOT NULL,
    hour int,
    CONSTRAINT s2cleanup_pk PRIMARY KEY (clid, hour)
    );
Cette table indique pour chaque document SA prochaine heure de cleanup plannifiée. Contraitement aux apparences sa clé primaire logique est `clid` : si `hour` y figure c'est pour bénéficier de l'index de clé primaire sans en créer un second.

### Table `doc`
    CREATE TABLE doc (
    clid varchar(255) NOT NULL,
    version bigint NOT NULL,
    ctime bigint NOT NULL,
    dtime bigint NOT NULL,
    CONSTRAINT doc_pk PRIMARY KEY (clid)
    );

Cette table donne l'état courant d'un document existant.
Ce row est mis à jour pour un document à chaque création ou mise à jour et est détruit quand le document l'est.

### Table `item`
    CREATE TABLE item (
    docid varchar(255) NOT NULL,
    clkey varchar(255) NOT NULL,
    version bigint NOT NULL,
    vop bigint NOT NULL,
    sha varchar(255),
    contentt text,
    contentb bytea,
    CONSTRAINT item_pk PRIMARY KEY (docid, clkey)
    );
    CREATE INDEX item_v on item (docid, version, clkey);
    CREATE INDEX item_p on item (docid, sha) where sha is NOT NULL;

Cette table est *abstraite* : pour chaque classe de document il est créé une table qui hérite de celle-ci. Par exemple la table `compte` :

    CREATE TABLE compte () INHERITS (item);

Cette table héberge tous les items n'ayant pas de propriétés indexées.

Pour chaque item comportant une ou des propriétés indexées (du moins visible du R-DBMS) il est déclarée une table spécifique qui hérite de la table des items du document :

    CREATE TABLE compte_ent (
    dhl varchar(255),
    nomr varchar(255)
    ) INHERITS (compte);
    CREATE INDEX compte_ent_dhl ON compte_ent (dhl, docid);
    CREATE INDEX compte_ent_nomr ON compte_ent (nomr, docid);

Le nom de la table est suivie de `_` et du nom de la classe d'item.  
Il est définie une colonne pour chaque valeur exportée / indexée.  
Il est, en général, défini un index reprenant cette colonne en tête et suivie de l'identifiant du document `docid`.

### Table `taskqueue`
    CREATE TABLE taskqueue (
    clid varchar(255) NOT NULL,
    nextstart bigint NOT NULL, 
    retry int NOT NULL,
    info varchar(255),
    report text,
    CONSTRAINT taskqueue_pk PRIMARY KEY (docid)
    CREATE INDEX taskqueue_nexstart on taskqueue (nextstart, clid);

Cette table est celle des tâches différées en attente de traitement ou en cours.   Le Queue Manager est certes averti de chaque modification par une requête HTTP mais périodiquement il scanne la table par les `nextStart` pour récupérer les pertes éventuelles et au lancement également.

# Stockage en GAE Datastore (à revoir)
Le Datastore est sensible au namespace standard de GAE.  
Les entités ont deux propriétés non indexées qui hébergent le texte (JSON souvent, simple String parfois) de l'entité :
- `text` : type `Text` pour les contenus courts (inférieurs à 2K);
- `blob` : type `Blob` pour les contenus longs gzippés en binaire.

Une des deux propriétés est toujours `null`.

### Entité `Group`
C'est l'entité racine de chaque groupe et sa clé est `groupid`.  
Propriétés :
- `version` : version du groupe;
- `text/blob` : le contenu du `GState`.


### Entité S2
C'est l'entité enregistrant pour un groupe la dernière date-heure de cleanup de l'espace de stockage secondaire.  

Clé : 
- parente : clé du groupe, 
- nom : "0"

Propriété :
- `hour` : AAMMJJHH heure du dernier cleanup effectué / planifié.

### Entité `Document`
Cette entité représente un `DState`, l'état d'un document et traduit son existence.  

Clé :
- parente : clé du groupe,
- nom : `docclass/docid`

Propriété :
- `version` : version du document,
- `text/blob` : son `DState`.

### Entité des items : `docClass` du document
Les entités représentant les items d'un document portent comme classe celle du document.

Clé :
- parente : celle de l'entité Document du document,
- nom : 
    - `"0"` : pour le header du document.
    - `cl/itn` : classe de l'item et son identifiant relatif au document.

Propriétés :
- `version` : version de l'item.
- `key` : `null` pour les singletons.
- `text/blob` : le contenu de l'item (String pour les opaques ou JSON pour les autres).
- `P_sha` : pour les seuls items P.

Quand un item de classe `Cl` d'un document de classe `docClass` à des propriétés indexées p1, p2 l'entité `docClass` a les propriétés suivantes supplémentaires :
- `Cl_p1`
- `Cl_p2`

Des index sont déclarés sur ces propriétés.

#### Exemple du `datastore-indexes.xml`

    <datastore-indexes autoGenerate="false">
    <datastore-index kind="Group" ancestor="true" source="manual">
        <property name="version" direction="asc"/>
    </datastore-index>
    <datastore-index kind="Document" ancestor="true" source="manual">
        <property name="version" direction="asc"/>
    </datastore-index>
    <datastore-index kind="NsDoc" ancestor="true" source="manual">
        <property name="P_sha" direction="asc"/>
    </datastore-index>
    <datastore-index kind="NsDoc" ancestor="true" source="manual">
        <property name="version" direction="asc"/>
    </datastore-index>
    <datastore-index kind="Compte" ancestor="true" source="manual">
        <property name="version" direction="asc"/>
    </datastore-index>
    <datastore-index kind="Compte" ancestor="true" source="manual">
        <property name="Ent_dhl" direction="asc"/>
    </datastore-index>
    <datastore-index kind="Compte" ancestor="true" source="manual">
        <property name="Ent_nomR" direction="asc"/>
    </datastore-index>

Les index `P_sha` ne sont à déclarer que sur les documents ayant des pièces jointes.


# Tâches : opérations différées
#### Enregistrement du début d'exécution 
Il est effectué dans le serveur d'exécution.
- **enregistrement de `TaskQueue` non trouvé** : on ne fait rien et on retourne un status 200 pour ne pas provoquer de relance du QM Datastore. La tâche était terminée ou supprimée par l'administrateur.
- **step demandée inférieure à celle enregistrée (ou celle enregistrée est 0 - trace)** : on ne fait rien et on retourne un status 200 pour ne pas provoquer de relance du QM Datastore. L'étape était terminée, le QM Datastore n'en savait rien (et a même lancé l'étape suivante).
- **steps demandée et enregistrée identiques**.
    - l'état enregistré n'a pas de `startTime`. C'est le cas normal, du premier lancement ou d'une relance. On inscrit une `startTime` et le `retry` est incrémenté.
    - l'état enregistré a une `startTime`. On écrase la `startTime` (le `retry` est laissé tel quel) ce qui fera ignorer la fin de la précédente. La nouvelle exécution devient officielle (l'autre ayant de fortes chances de ne plus être suivie par le QM Datastore).
- **step demandée supérieure à celle enregistrée**. Situation a priori impossible : on ne fait rien et on retourne un status 200. 

#### Fin d'exécution
Elle est exécutée par l'opération, soit sur fin normale, soit en exception :
- toute exécution qui se termine alors que l'état enregistré ne correspond pas à sa signature `startTime` est ignorée, non enregistrée et la transaction non validée : c'est une exécution perdue qui a été relancée depuis. Son retour est un status 200 afin que le QM Datastore surtout ne relance rien (au cas improbable où il écouterait encore).
- la fin sur exception est enregistrée (transaction non validée) et retourne un 500 afin que le QM Datastore relance.
- la fin normale est enregistrée, la transaction est validée et retourne un status 200 afin que le QM Datastore ne relance pas. La fin normale correspond à,
    - soit la fin de la dernière étape avec trace (`step` est `null`, `toPurgetAt` est renseignée). 
    - soit la fin de la dernière étape sans trace (l'enregistrement est supprimé). 
    - soit la demande d'une nouvelle étape (`step` est incrémenté, `retry` à 0, `toStartAt` est renseignée). 

#### Fin d'étape d'une tâche
La fin d'une étape *intermédiaire* donne lieu :
- à l'enregistrement du `param` de l'étape suivante, objet pouvant comporter facultativement des données de compte rendu synthétique d'exécution des étapes précédentes;
- à l'incrémentation du compteur d'étapes `step`;
- à l'enregistrement d'une nouvelle date-heure de lancement pour l'étape suivante.

La fin de la *dernière* étape donne lieu :
- à l'enregistrement du `param` qui, s'il n'est pas `null`, ne contient plus que des données de compte rendu synthétique d'exécution;
- à l'enregistrement d'une date-heure de purge de l'enregistrement de suivi de la tâche.
- à la mise à `null` du numéro de la prochaine étape et de la date-heure de prochain lancement.

A noter que si la dernière étape ne veut pas conserver de trace de traitement, son enregistrement est purement et simplement détruit.

### Principe d'exécution nominal
Une opération inscrit une nouvelle tâche en ayant fourni `ns opName param info cron` : `taskid` est générée et `qn` obtenu de la configuration.  
Au commit :
- la tâche est inscrite dans la table `TaskQueue`.
- pour un Datastore, **avant le commit**, une tâche est mise en queue avec pour URL d'invocation `/ns/od/taskid/step?key=...`.
- pour une base de données, **après le commit**, si la `toStartAt` est proche (moins de X minutes : X est le scan lapse du Queue Manager), le Queue Manager associé à l'instance (`qm7` par exemple) reçoit une requête HTTP `/qm7/op?key=...&op=inq&param={ns:..,taskid:..,toStartAt:..,qn:..,step:N}` pour inscription de l'étape N de la tâche à relancer sans attendre le prochain scan.

**Quand le Queue Manager peut / doit lancer la tâche**, il cherche un thread worker libre qui émet vers le serveur du namespace une requête HTTP avec `/ns/od/taskid/step?key=...` :
- `ns` : le code de l'instance (comme pour toute requête),
- `od` au lieu de `op` pour identifier qu'il s'agit d'une opération différée,
- `taskid/step` dans l'URL.
- le paramètre `key` : mot de passe permettant de s'assurer que c'est bien le Queue Manager qui a émis la requête et non une session externe (en fait sur un POST, `key` n'apparaît pas dans l'URL comme dans celle ci-dessus qui est employée en test).

Quand le Datastore lance la tâche il émet une requête HTTP sur l'URL `/ns/od/taskid/step?key=...`.

L'opération souhaitée s'exécute ensuite :
- **traitement OK** :
    - la tâche `ns.taskid` est,
        - soit supprimée de la table `TaskQueue` au commit.
        - soit enregistrée pour une nouvelle étape.
    - le retour est un status 200 ce qui libère le worker du Queue Manager pour prendre en charge une nouvelle étape de tâche ou signale au Datastore que l'étape de la tâche est finie.
- **traitement en Exception** :
    - la tâche `ns.taskid` est mise à jour dans `TaskQueue` :
        - `starTime` y est mise à `null`;
        - `retry` est incrémenté;
        - `exc detail` sont renseignés;
        - `toStartAt` est calculée à une valeur future d'autant plus lointaine que le numéro de `retry` est élevé, voire finalement infinie.
    - le retour est un status 500 ce qui libère le worker du Queue Manager pour prendre en charge une nouvelle étape d'une tâche ou signale au Datastore qu'il faudra relancer.
    - sauf Datastore, si la `nexstart` est proche (moins de X minutes), le Queue Manager associé au namespace reçoit une requête HTTP pour inscription de la tâche à relancer sans attendre le prochain scan.

**Scan périodique des `TaskQueue` par le Queue Manager**
Un Queue Manager gère une ou plusieurs instances et fait donc face à une ou plusieurs base de données.  
Seules les étapes des tâches à échéance proche lui sont soumises par HTTP : un scan périodique lui permet de récupérer les autres. Ce scan, pour chaque base de données, filtre les tâches ayant :
- une `toStartAt` antérieure à la date-heure du scan suivant,
- ayant l'un des codes d'instance dont il est en charge,
- un numéro d'étape : si `null` la tâche est terminée et l'enregistrement n'est qu'une trace,
- ayant une `startTime` `null` (tâche pas en cours).

Ce scan permet au Queue Manager de récupérer les tâches à relancer autant que celle à lancer une première fois.

### Situations anormales
##### Perte de contact par le worker du Queue Manager qui suit l'exécution d'une étape d'une tâche
Sa requête HTTP sort prématurément en exception : la notification de fin ne lui parviendra pas.
- le worker considère l'étape comme terminée.
- le worker n'est en fait pas intéressé par le status de bonne ou mauvaise fin de celle-ci.

Si l'étape de la tâche se termine bien, elle disparaîtra de `TaskQueue` ou aura un numéro d'étape supérieur.  
Si la tâche sort en exception elle sera modifiée en `TaskQueue` pour une future relance de son étape et si cette relance est proche le Queue Manager sera notifié par HTTP pour accélérer la relance.

##### Tâches perdues
Il s'agit de tâches qui ont été lancées (ou relancées),
- ayant une `startTime` ancienne,
- dont le Queue Manager n'a pas de trace en exécution dans un worker.

On va considérer que l'étape de la tâche s'est mal terminée sans que son exception n'ait pu être enregistrée dans `TaskQueue` : elle est enregistrée avec comme exception `LOST` et marquée avec une `toStartAt` pour une relance ultérieure (comme une exception normale). 

Une tâche perdue est *supposée* être perdue mais en fait elle peut être cachée en exécution et dans ce cas il peut exister à un moment donné plus d'une exécution en cours, voire dans le pire des cas avec des numéros d'étapes différents.

# Configuration (à reprendre)
Chaque instance de serveur du pool a un code identifiant qui lui permet de retrouver dans le fichier de configuration les options qui lui sont spécifiques.
Ce code est donnée comme paramètre système :

    java -Dfr.sportes.base.INSTANCE=A1 ... 

#### `web.xml` 

    <?xml version="1.0" encoding="utf-8"?>
    <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns="http://java.sun.com/xml/ns/javaee"
    xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
    xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
    http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" version="2.5">
    
      <context-param>
        <param-name>ConfigClass</param-name>
        <param-value>fr.sportes.forums.Config</param-value>
      </context-param>
    
      <system-properties>
            <property name="java.util.logging.config.file" value="WEB-INF/logging.properties" />
        </system-properties>
    
      <servlet>
        <servlet-name>Generic</servlet-name>
        <servlet-class>fr.sportes.base.Servlet</servlet-class>
        <load-on-startup>1</load-on-startup>
      </servlet>
      <servlet-mapping>
        <servlet-name>Generic</servlet-name>
        <url-pattern>/*</url-pattern>
      </servlet-mapping>  
    </web-app>

Le paramètre `ConfigClass` donne le nom de la classe de configuration qui étend `AConfig`.

#### `config.json` 
Ce fichier donne les options de configuration générale et les options propres à chaque instance de serveur du pool.
#### `secret.json` 
Ce fichier donne des options de sécurité (mot de passe, etc.) requis pour accéder à la base de données, le SHA-256 de la clé d'administration, les paramètres d'accès au mailer  ..
#### Classe `Config`
Cette classe étend la classe `AConfig` et donne la description spécifique de `config.json` et `secret.json`.
