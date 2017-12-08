# Document DB - API

# Requêtes HTTP et Contextes d'exécution
Les sessions externes émettent des requêtes HTTPS qui peuvent être des GET ou des POST.

***Raccourcis d'URLs***  
La configuration permet de définir des URLs raccourcies correspondant aux chargements des pages : ceci permet de fournir aux utilisateurs une URL plus simple et en particulier de sauter le *namespace*. In fine dès le début du GET la substitution intervient et c'est comme si l'utilisateur avait saisi l'URL complète.

Les URLs comportent en tête un *context path* `cp` qui selon les options de déploiement peut ne pas exister et,
- soit un code de namespace `ns` obligatoire (le code `qm` étant interdit): 
    - `htpps://monserveur.mondomaine.fr/cp/ns` 
    - `htpps://monserveur.mondomaine.fr/ns`
- soit un code Queue Manager :
    - `htpps://monserveur.mondomaine.fr/cp/qm1` 
    - `htpps://monserveur.mondomaine.fr/qm1`

Ceci permet au frontal HTTP de router les requêtes sur les serveurs appropriés du pool et en particulier d'adresser les requêtes techniques pour les Queue Managers sur les instances qui les gèrent effectivement.  
Plusieurs codes `qmXXX` de Queue Manager peuvent être gérés par une même instance de serveur.

### Invocation d'opérations
URLs : `cp/ns/op/p1/p2 ...`  
Les autres arguments sont passés soit en `application/x-www-form-urlencoded` (sur un GET) ou en `multipart/form-data` (sur un POST) :
- `op/` :  obligatoire, signale un appel d'opération. Ce code est `od/` pour une opération différée *émise* par le Queue Manager et `qm/` pour une opération *gérée* par le Queue Manager ;
- `p1 p2 ...` sont facultatifs. Ils peuvent être obtenus par `inputData.uri()` qui retourne un `String[]`;
- les ***arguments*** sont récupérables par `inputData.args().get("nom argument")`:
    - l'argument de nom **`op`** donne le nom de l'opération et permet de trouver la classe de l'opération à instancier.
    - les arguments de nom **`account key admin`** sont en général utilisés pour gérer les habilitations à effectuer l'opération.
    - l'argument de nom **`param`** est traité spécifiquement : c'est un JSON et il est dé-sérialisé dans le champ de nom `param` de l'opération.
    - ***dans le cas d'un POST des pièces jointes peuvent être transmises*** : elles sont récupérables par `inputData.attachments().get("name")` qui retourne un objet (ou `null`) portant les informations `name, fileName, contentType, bytes`.

### URLs spéciales de GET

**URL : `cp/build`** et **`cp/ns/build`**   
Elle retourne le numéro de build : cette URL fait office de `ping` et permet de tester la disponibilité effective du serveur (en particulier pour gérer le load balancing sur une ferme de serveurs).  
Chaque namespace a en plus de la build standard du logiciel un numéro complémentaire de sa propre build : on l'obtient par l'URL `cp/ns/build`.

**URL : `cp/ns.appcache` et `cp/ns.swjs`**  
Elles retournent la ressource `manifest` de l'application cache ou le script du *service worker* qui permettent de fonctionner offline. Ces ressources ont un texte généré depuis les fichiers trouvés dans le `war`.

**URL : `cp/ns/page.app` ou `.cloud` ou `.local` ou `.local2` ou `.sync` ou `.sync2`**  
Elles retournent le texte de la page `page.html` de l'application transformée afin d'être utilisable *online* (`app cloud`), en mode *avion* (`local local2`) ou en mode *synchronisé* (`sync sync2`).  
- `local` et `sync` correspondent à la gestion par *service worker*.
- `local2` et `sync2` correspondent à la gestion par *appcache*.

**URL : `cp/ns/var/...` ou `cp/ns/_build_nsVersion_/...`**  
Elles retournent les ressources de l'application.  
La seconde URL permet d'éviter les problèmes de rafraîchissement de cache des browsers en changeant d'URL à chaque évolution de build ou de version de configuration du namespace.  
Dans ces ressources celles qui commencent par `z/` (comme `z/custom.css`) indique d'aller chercher le texte de la ressource en base en tant que pièce jointe du document de configuration du namespace, puis seulement si elle n'existe pas de prendre la ressource de la build `z/..`.

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

# Queue Manager 
En GAE Datastore le Queue Manager est assuré par le Datastore.

La queue des tâches en attente d'exécution est dans la table `taskqueue` :

Chaque serveur du pool peut initialiser un **Queue Manager**, une classe définissant l'exécution de plusieurs threads.  
Un Queue Manager d'un serveur du pool à une liste de codes QM : il est en charge de gérer tous les namespaces ayant pour paramètre de configuration `qm` l'un des codes de cette liste. Les threads sont les suivants  :
- un thread `QueueManager` chargé de distribuer les tâches à exécuter à un des threads **workers** ;
- un pool de threads **workers**. Chaque worker est en attente d'une tâche à exécuter et invoque la requête HTTP-POST correspondante pour faire exécuter cette tâche par un des serveurs du pool. En cas de succès (retour 200) le worker réveille le thread `Queue Manager` qui recherche une nouvelle tâche à exécuter. 
En cas d'échec un worker,
- enregistre cet échec dans le row de la tâche dans `taskqueue` ;
- le compteur `retry` est incrémenté ;
- l'estampille `nextStart` est fixée à une date-heure plus ou moins proche selon la valeur du `retry`. Mais si le nombre de `retry` excède le maximum prévu, cette estampille est mise à la fin du siècle : l'administrateur agira en conséquence;
- le retour de la requête de la tâche est aussi enregistrée comme report de la tâche : sa lecture permet le cas échéant à l'administrateur d'enquêter sur les causes de l'échec.

Ce mécanisme garantit en conséquence une reprise automatique en cas d'échec, du moins un certain nombre de fois.

Le thread `QueueManager` est reveillé par plusieurs événements :

- ***une validation d'opération a des tâches à lancer*** : celles-ci sont transmises au thread `QueueManager`, soit directement s'il est hébergé dans le même serveur, soit par une requête HTTP. Une validation permet d'enchaîner sur une exécution de tâche différée quasi immédiate si nécessaire ;
- ***la fin de traitement d'un worker*** désormais disponible pour lancer et suivre l'exécution d'une nouvelle tâche ;
- ***périodiquement***, typiquement une fois par minute, pour exécuter un "full scan". Le full scan consiste à lire dans les bases de données de tous les namespaces gérés par le serveur du pool, la liste des tâches à exécuter dans la minute qui suit. Ceci garantit que même en cas d'interruption du `QueueManager` aucune tâche ne sera oubliée à la relance.

#### Options du QueueManager
Elles ont données dans le fichier `config.json`.

  "instances":{
    "default":{
      "hostedQM": ["qm1", "qm2"],
      "threads": [2,1],
      "scanlapseinseconds":60,
      "retriesInMin":[1, 10, 60, 180]
    },
    "A1":{
      "hostedQM": ["qm3"],
      "threads": [2,1],
      "scanlapseinseconds":60,
      "retriesInMin":[1, 10, 60, 180]
    }
  },

Chaque serveur du pool est identifié par un code d'instance transmis sur la ligne de commande de son lanceur (`default` si absent, sinon un code réel).
- `hostedQM` : si l'array est non vide, un Queue Manager est lancé pour cette instance et traitera tous les namespaces dont le code `qm` vaut `qm3` dans l'exemple de l'instance A1 ci-dessus.
- `threads` donne le nombre de workers pour chaque "queue". Dans l'exemple ci-dessus il y a deux queues ayant respectivement 2 workers pour la queue 0 et 1 worker pour la queue 1.
- `scanlapseinseconds` indique la fréquence d'exécution des "full scan" qui réinitialisent la liste des tâches à exécuter depuis la base de données pour faire face aux défaillances éventuelles de notifications en fin d'opération ou d'absence d'écoute à ces moments là.
- `retriesInMin` indique combien de minutes il faut attendre après un premier, second, ... n-ième échec avant de relancer la tâche.
 
#### Affectation d'un numéro de queue à une tâche
C'est une méthode de `Config` qui détermine le numéro de queue à affecter en fonction de l'information sur la tâche ('namespace' et surtout `docclass`).  
Typiquement il existe une queue `0` pour les tâches pseudo temps réel de front et une queue `1` pour les tâches de fond de faible priorité.

### Actions d'administration
Elles ne concernent pas le GAE Datastore ayant sa propre console d'administration du Queue Manager.

Les quelques actions possibles sont les suivantes :

- `stop` : arrêt du Queue Manager ;
- `start` : relance du Queue Manager ;
- `list` : liste des tâches à lancer et en cours à brève échéance (jusqu'au prochain "full scan") ;
- `report` : obtention du report de la dernière exécution en erreur d'une tâche ;
- changement d'heure de lancement d'une tâche (et du compteur de `retry` le cas échéant).
- suppression d'une tâche.
- création d'une tâche : ceci est utile pour initialiser les tâches qui s'auto renouvellent ensuite par un *cron*.
 
Une sélection des tâches inscrites peut aussi être faite en fixant facultativement certains des éléments suivants :
- sa classe de document,
- sa date-heure de relance (toutes celles prévues avant cette heure),
- son occurrence d'échec (première ou plus, seconde ou plus ...),
- son texte d'information qui en général contient le code *cron* quand il s'agit d'une tâche périodique lancée par ce procédé..

# Congiguration
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
