# Document DB - API

# Requêtes HTTP et Contextes d'exécution
Les sessions externes émettent des requêtes HTTPS qui peuvent être des GET ou des POST.  
***Raccourcis d'URLs***  
La configuration permet de définir des URLs raccourcies correspondant aux chargements des pages : ceci permet de fournir aux utilisateurs une URL très simple et en particulier de sauter le namespace. In fine dès le début du GET la substitution intervient et c'est comme si l'utilisateur avait saisi l'URL complète.

Les URLs comportent en tête un context path `cp` qui selon les options de déploiement peut ne pas exister et,
- soit un code de namespace `ns` obligatoire (le code `qm` étant interdit): 
    - `htpps://monserveur.mondomaine.fr/cp/ns` 
    - `htpps://monserveur.mondomaine.fr/ns`
- soit un code Queue Manager :
    - `htpps://monserveur.mondomaine.fr/cp/qm1` 
    - `htpps://monserveur.mondomaine.fr/qm1`

Ceci permet au frontal HTTP de router les requêtes sur les serveurs appropriés de la ferme et en particulier d'adresser les requêtes techniques pour les Queue Managers sur les instances qui les gèrent effectivement.  
Plusieurs codes `qmXXX` de Queue Manager peuvent être gérés par une même instance de serveur.

### Invocation d'opérations
URLs : `cp/ns/op/p1/p2 ...`  
Les autres arguments sont passés soit en `application/x-www-form-urlencoded` (sur un GET) ou en `multipart/form-data` (sur un POST) :
- `op/` :  obligatoire, signale un appel d'opération. Ce code est `od/` pour une opération différée *émise* par le Queue Manager et `qm/` pour une opération *gérée* par le Queue Manager ;
- `p1 p2 ...` sont facultatifs. Ils peuvent être obtenus par `inputData.uri()` qui retourne un `String[]`;
- les ***arguments*** sont récupérables par `inputData.args().get("nom argument")`:
    - l'argument de nom **`op`** donne le nom de l'opération et permet de trouver la classe de l'opération à instancier.
    - l'argument de nom **`key`** est en général utilisé pour contenir un jeton d'accès ou le digest de la clé de l'administrateur. D'autres arguments spécifiques sont autorisés.
    - l'argument de nom **`param`** est traité spécifiquement : c'est un JSON et il est dé-sérialisé dans le champ de nom `param` de l'opération.
    - ***dans le cas d'un POST des pièces jointes peuvent être transmises*** : elles sont récupérables par `inputData.attachments().get("name")` qui retourne un objet (ou `null`) portant les informations `name, fileName, contentType, bytes`.

### URLs spéciales de GET

**URL : `cp/build`** et **`cp/ns/build`**   
Elle retourne le numéro de build : cette URL fait office de ping et permet de tester la disponibilité effective du serveur (en particulier pour gérer le load balancing sur une ferme de serveurs).  
Chaque namespace a en plus de la build standard du logiciel un numéro complémentaire de sa propre build : on l'obtient par l'URL `cp/ns/build`.

**URL : `cp/ns.appcache` et `cp/ns.swjs`**  
Elles retournent la ressource manifest de l'application cache ou le script du service worker qui permettent de fonctionner offline. Ces ressources ont un texte généré depuis les fichiers trouvés dans le `war`.

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
Retourne true si l'argument de la requête `key` en base 64, représente un `byte[]` dont le SHA-256 en base 64 est égal au paramètre de configuration `secretKey`.

    public boolean hasQmKey() 
Retourne true si l'argument de la requête `key` en base 64, représente un `byte[]` dont le SHA-256 en base 64 est égal au paramètre de configuration `qmSecretKey`.

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

Dans le seul cas d'une opération différée il est possible d'accumuler des traces d'exécution textuelles simples et de les retrouver en cas de sortie en exception dans le rapport d'erreur stocké par le Queue Manager. C'est sans objet sur une requête émise par une requête interactive.

    public DBProvider dbProvider()

Le DBProvider est l'objet en charge d'accéder directement au stockage de données sans passer par l'intermédiaire normal des documents. Son emploi est obligatoire pour effectuer les recherches au travers des index.

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

Un résultat de synchronisation (c'est un String en JSON mis dans la propriété `syncs`) n'est possible que quand le résultat est dans l'option (1) (`Object` à sérialiser e JSON).

Il n'est pas obligatoire de déclarer un résultat : dans ce cas un `Object` vide est considéré et retourné à la session appelante.

#### Gestion des opérations différées

    public void setTask(DocumentId id, long nextStart, String info)

Cette méthode déclare une opération différée identifiée par le `DocumentId` fourni et sera lancée au plus tôt à la date-heure `nextStart` donnée.  
Un String d'information facultatif peut être donné et apparaîtra dans les pages de gestion du Queue Manager (c'est son seul rôle).

      public void setTaskByCron(DocumentId id, String cron)
Même fonctionnalité mais la `nextStart` est calculée selon le `String cron` passé en argument (qui sera mis en info).

    public void removeTask(DocumentId id)

Supprime cette tâche, si elle est enregistrée et que c'est encore possible (qu'elle n'est pas en exécution ou terminée). En GAE Datastore ce n'est jamais possible.

    public TaskInfo taskInfo(DocumentId id)

Retourne les données relatives à une opération différée en cours de gestion par le Queue Manager (pas en GAE Datastore).

    public Collection<TaskInfo> listTask(TaskInfo ti)

Liste toutes les tâches gérées par le Queue Manager à cet instant et correspondant au filtre `TaskInfo` fourni (pas en GAE Datastore).

Un objet `TaskInfo` contient principalement :
- `String ns` : le namespace sur lequel l'opération porte.
- `DocumentId id` : son identité.
- `long version` : date-heure de validation de l'opération ayant inscrit la tâche.
- `long nexstart` : sa date-heure de lancement au plus tôt.
- `int retry` : le numéro de l'essai d'exécution (usuellement 0).
- `String info` : le commentaire informatif donné à la création.

# Sérialisation JSON d'un document
La forme générale est la suivante:

    {
    "docclass":"C",
    "docid":"abcd",
    "version":1712... ,
    "ctime":1712... ,
    "dtime":1712... ,
    "items": [
        {"c":"S4", "v":1712...},
        {"c":"S5", "k":"def", "v":1712...},
        {"c":"S2", "v":1712... "s":"texte d'un raw"},
        {"c":"S1", "v":1712... "j":{ l'item en JSON }},
        {"c":"R1", "k":"def", "v":1712... "s":"texte d'un raw"},
        {"c":"I1", "k":"abc", "v":1712... "j":{ l'item en JSON }},
    ]
    "clkeys":["S1", "I1.abc", "I1.def", "R1.def"... ]
    }

`keys` n'est présent que dans le cas 2 où la `version` détenue par le cache à remettre à niveau est antérieure à la `dtime` de la source.  
Les items détruits :
- n'ont pas de valeur `s` ou `j`;
- sont à inclure si leur `version` est postérieure à,
    - cas 1 : la plus récente des deux `dtime` de la source et du cache.
    - cas 2 : la `dtime` de la source.

Les items créés, recréés, modifiés postérieurement à la version du cache sont à inclure.  
Dans le cas 2, la liste `clkeys` ne contient pas les clés des items créés /recréés / modifiés qui figurent déjà dans `items`.

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

Les propriétés intéressantes du `DState` d'un document sont lisibles directement sur un document :
- `DocumentId id()` : identifiant du document.
- `long ctime()` : date-heure de création, telle qu'elle était connue au début de l'opération.
- `long version()` : date-heure de création ou dernière modification, telle qu'elle était connue au début de l'opération.
- (*) `long v1() / long v2()` : volumes primaire et secondaire.
- (*) `Status status()` : état du document.
- `isReadOnly()` : `true` si le document est en lecture seule.

Les propriétés marquées (*) n'ont de valeurs significatives que juste après l'appel de la méthode **`summarize()`** qui scanne tous les items pour calculer les volumes et déterminer l'état du document.

Un document destiné à la lecture mais devant bloquer son groupe est marqué par l'appel de la méthode **`setExclusive()`**. Cet état est testable par `isExclusive()`.

### Obtention d'une instance de document
      public static Document get(DocumentId id, int maxDelayInSeconds) 
      throws AppException
      public static Document getOrNew(DocumentId id) throws AppException
      public static Document newDoc(DocumentId id) throws AppException
La première méthode retourne le document dont l'identification est donnée et `null` s'il n'existe pas. La contrainte de fraîcheur peut être spécifiée afin de tolérer un document trouvé en mémoire cache et d'âge compatible avec les exigences métier. Si cette valeur n'est pas 0 (par convention la version la plus récente possible) le document est en lecture seule.

La seconde méthode retourne un nouveau document s'il n'en existe pas un.

La troisième méthode force la création du document : s'il existait il est recréé, son contenu mis à vide et sa `ctime` change, une nouvelle vie du document commence.

>Dans une opération pour un document d'identification donnée toutes les variables de retour d'un `get()` pointe vers le même objet : un document donné n'existe qu'en un exemplaire unique dans l'espace d'une opération.

Usuellement chaque classe de document écrit ses propres méthodes `get()` / `getOrNew()`, ayant le cas échant des paramètres métiers pertinents et en castant le résultat dans la classe du document cherché / créé.

### Suppression et recréation d'un document
    void delete()
    void recreate()
    public static void delete(DocumentId id) throws AppException
Un document peut être supprimé depuis son instance elle-même ou en static depuis la classe elle-même.  
La recréation d'un document le met à vide et sa `ctime` est réinitialisé : une nouvelle vie du document commence.

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

Pour un `RawItem` ou un `RawSingleton`,
- sa valeur est obtenue par la méthode `String rawText()`.
- le changement de sa valeur est enregistré par `void commit(String text)`.

Les items / singletons se suppriment par : `void delete()`   

La clé d'un item (non singleton) peut être changée par : `void key(String key)`. Un `commit()` n'est pas nécessaire.  
Elle peut être lue par `String key()`

Le `Status` d'un item s'obtient par : `Status status()`
- `unchanged` : l'item existait avant le début de l'opération, existe toujours et n'a pas changé.
- `modified` : l'item existait avant le début de l'opération, existe toujours mais a changé..
- `created` : l'item n'existait pas (même en trace de suppression) et a été créé par un `getOrNew()`.
- `recreated` : l'item n'existait pas mais avait une trace de suppression et a été recréé par un `getOrNew()`. 
- `deleted` : l'item existait mais a été supprimé par l'opération.
- `shortlived` : l'item n'existait pas, a été créé par un `getOrNew()` et a été supprimé ensuite au cours de l'opération.
- `oldtrace` : l'item était supprimé avant l'opération (avait une trace de suppression) et n'a pas été recréé.

La version (connue au début de l'opération) d'un item s'obtient par : `long version()`

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
    String mime()
    String sha()
    int size()
    byte[] blobGet()
    
    // Méthodes standard d'un Item
    Status status()
    long version()
    String key()
    void key(String key)
    void delete()

Une seule méthode sur un document permet de créer / modifier un item P en donnant son contenu binaire: 

    P blobStore(String key, String mime, byte[] bytes)
La méthode retourne l'item créé ou modifié.

### Browse items : `CItemFilter browseAllItems(CItemFilter f)`
Un `CItem` est l'image NON dé-sérialisée d'un item. On peut obtenir des informations depuis un `CItem` sans dé-sérialiser son contenu:

    public ItemDescr descr() 
    public long version() 
    public String key() 
    public String itn() 
    public String cvalue() // son JSON
    public Status status() 
    public int v1()
    public int v2()

Dans un document on peut obtenir l'item lui-même, ce qui provoque la dé-sérialisation de son CItem associé, par la méthode :

    public BItem item(CItem ci)

Il faut caster le résultat dans la classe effective de l'item souhaité.

Il est possible définir une classe de filtre opérant sur un `CItem` et d'appeler ensuite la méthode  `browseAllItems(CItemFilter f)` qui va faire opérer ce filtre sur tous les items du document.

***Exemple :*** récupération de tous les items `Env` et `Rec` de date-heure créés depuis 24h :

    public static class MonFiltreEnvRec extends CItemFilter {
    ArrayList<Env> envs = new ArrayList<Env>();
    ArrayList<Rec> recs = new ArrayList<Rec>();
    Document d;
    Stamp limite;
    
    public MonFiltreEnvRec(Document d, Stamp limite) {
      this.d = d; this.limite = limite; 
    } 
    
    @Override public boolean accept(CItem ci) throws AppException {
      Status st = ci.status();
      if (st == Status.deleted || st == Status.zombie) return false;
      if (ci.descr().clazz() == Env.class) {
        Stamp s = Stamp.fromString(ci.key()
          .substring(0, ci.key().indexOf('.')));
        if (s.compareTo(limite) < 0) return false;
        envs.add((Env)d.item(ci));
        return true;
      }
      if (ci.descr().clazz() == Env.class) {
        Stamp s = Stamp.fromString(ci.key()
          .substring(0, ci.key().indexOf('.')));
        if (s.compareTo(limite) < 0) return false;
        envs.add((Env)d.item(ci));
        return true;
      }
      return false;
    }
    }
    
    public void testFiltre() throws AppException {
      MonFiltreEnvRec f = (MonFiltreEnvRec) browseAllItems(
        new MonFiltreEnvRec(this, Stamp.fromNow(-86400000)));
      for(Env e : f.envs){
        // ...
      }
    }


## Contrôle des synchronisations
Une synchronisation est une requête dont l'objectif est de récupérer les items modifiés de documents afin de remettre à jour ceux-ci dont la session requérante détient, ou non, une copie possiblement retardée.  
Une synchronisation peut être une requête spécifique ou intervenir après la bonne exécution d'une opération, en particulier de mise à jour.

>Pour chaque document concerné, selon sa classe, son identifiant et pour chaque item du document, sa classe, son identifiant sa clé puis en fin son contenu, il peut être décidé d'accepter ou non l'élément et finalement le cas échéant avec seulement certaines des propriétés.

Une synchronisation est définie par une liste de document à synchroniser. Cette liste parvient en JSON dans le paramètre `syncs` de la requête. Chaque item de cette liste `[{"groupid": ...}, {...}, ...]` correspond à un objet de la classe `Sync` et décrit un document à synchroniser :

    private String groupid;
    private String docid;
    private String docclass;
    private long version;
    private long ctime;
    private String filter;

Le `String filter` est lui-même le JSON d'un objet qui sera instancié selon la classe interne `static Filter` de la classe du document correspondant.

`ctime` est la date-heure de création du document détenu par la session. Si le document actuel n'a pas la même `ctime`, c'est que le document a été recréé : tous ses items seront délivrés puisqu'il s'agit d'une autre vie du document.  
Si la `ctime` est toujours la même, seuls les items modifiés après la version indiquée sont transmis, ainsi que le `header` (s'il a changé postérieurement).

Le retour d'une synchronisation est un vecteur similaire où chaque élément correspondant à un document *peut* avoir changé :
- `ctime` et `version` correspondent au nouvel état ;
- `header` : si le `header` a une version supérieure à celle détenue par la session, il est transmis avec :
    - `{version:..., nid:..., ids:"aa bb "}`
    - `ids` donne la nouvelle liste des identifiants des items existants ce qui permet à la session qui se synchronise de supprimer les items non cités.
- `items` : c'est un vecteur des items `[{...}, {... } ...]` ayant changé avec pour chacun :
    - `{version:1702..., itn:"aF", cl:"Rec", key:"truc.machin", val:{...}}`
    - `val` : contient l'item lui-même avec toutes ses propriétés et est un `String` pour un item opaque.
    - `key` n'est pas présent pour un singleton.

#### Classes héritant de `Document.ISyncFilter`
Chaque classe de Document *peut* avoir une classe `static` `Filter` héritant de `Document.ISyncFilter`.  
Si cette classe existe pour une classe de document, lors de chaque synchronisation relative à un document de cette classe un objet filtre est créé :
- soit depuis le constructeur par défaut ;
- soit depuis l'objet reçu en JSON comme paramètre de filtre passé par la requête de synchronisation.

Les propriétés de cet objet sont les paramètres de filtrage spécifiques au document à synchroniser et en particulier les paramètres éventuels d'authentification : *en fonction de ces paramètres, le document est-il à synchroniser et si oui quels items en particulier*.

`Document` a plusieurs méthodes surchargeables par héritage :  
***Filtre sur l'identifiant du document et sa classe selon le `Credential`***

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

    public String toJson(ISyncFilter sf, long version) throws AppException

Cette méthode retourne la sérilisation JSON du document et y applique les éléments de filtre décrits ci-dessus.  
Les paramètres ne sont pas optionnels.

# Accès aux groupes et documents par `GDCache`
Chaque instance de serveur dispose d'une mémoire cache gérée par la classe `GDCache` où sont stockés :
- l'état des derniers groupes accédés ;
- les derniers documents accédés.

Tout accès à un document passe toujours par la mémoire cache et les documents y sont en lecture seule.

La mémoire cache est séparée par `namespace` : il est en conséquence possible d'accéder à des documents d'un autre namespace, du moins en lecture. Acquérir d'abord la mémoire cache, soit de son namespace courant soit d'un autre :

    public static GDCache current()
    public static GDCache cacheOf(String namespace)

### Etat d'un groupe et de ses documents

    public CGroup cgroup(String groupid, Stamp minTime)
Depuis le `CGroup` retourné il est posible d'obtenir la `version` du groupe, et la liste des états des documents du groupe et pour chacun sa `ctime` et `version` (ou un document précis par son `docid` ou son `dn`).

### Document en lecture seule

    public Document documentRO(DocumentId id, Stamp minTime, long versionActuelle)
Le document est retourné de la mémoire cache (après y avoir été mis s'il n'y était pas). Il est en lecture seule, afin de ne pas perturber l'état de la mémoire cache lisible par les autres threads.

# Sélection des groupes et documents par `DBProvider`
Il existe autant de classes `DBProvider` que de type de bases de données implémentées.  
Un provider d'accès aux données est un objet représentant "la" base de données "d'un" namespace.  
Dans une opération son provider est accessible par `execContext().dbProvider()`  et n'importe où dans le code par :
`ExecContext.current().dbProvider()`

**Pour obtenir des sélections de groupes ou de documents** il faut passer par son provider et invoquer l'une des méthodes de sélection.

#### Recherche générique des noms de groupes

    public HashMap<String,Long> listGroups(Cond<String> fgroupid, 
    long version) throws AppException;

Une condition est fournie sur les noms de groupes (sauf `EQ` bien entendu).   
Seuls les groupes ayant une version postérieure à celle passée en paramètre sont cités.

Le résultat est une map donnant pour chaque groupe sa version.

#### Recherche générique des identifiants de documents

    public Collection<DocumentId> documentIds(String docclass, 
    String groupid, long version, Cond<String> fdocid) 
    throws AppException;

Sont fournis :
- `docclass` : la classe des documents cherchés ;
- `groupid` : recherche restreinte à l'intérieur de ce groupe ;
- `version` : recherche des seuls documents ayant une version postérieure à celle-ci.
- `fdocid` : filtre portant sur le `docid`. Si `null` tous documents ;

Le résultat est la collection des `DocumentId` des documents répondant au filtre.

En GAE Datastore la recherche n'a pas la même structure selon que la condition sur le `docid` est donnée ou non. Cette dernière est prioritaire mais si elle est absente la recherche se fait sur l'index de version.

#### Recherche générique sur les valeurs des propriétés d'un item
L'objectif est de pouvoir rechercher une liste de documents dont l'un des items a une valeur de propriété indexée recherchée (par exemple un code postal donné et les premières lettres d'un nom, ou une adresse e-mail ...).
 
Ce genre de recherche n'est fait que pour retourner un nombre réduit de documents.

    public Collection<DocumentId> searchByIndexes(Cond<String> fgroupid, 
    String docClass, String itemClass, Cond<?>... ffield)
    throws AppException;

- `fgroupid` ou `null` : recherche restreinte au(x) groupe(s) dont le nom respecte cette condition ;
- `docClass itemClass` : recherche sur les seuls items de cette classe pour cette classe de document ;
- *une ou plusieurs filtres portant sur les propriétés indexées* de l'item. Chaque filtre doit avoir pour attribut `name` le nom de la propriété filtrée. Seule la première condition peut être **une non `EQ`**.

Le résultat est une `Collection` de `DocumentId`.

En GAE Datastore : 
- si le `groupid` est fixé, la recherche s'effectue en cohérence forte sur le groupe : les dernières modifications sont prises en compte. Si `groupid` n'est pas fixé, l'index utilisé est retardé par rapport aux dernières modifications : une recherche quelques secondes après une mise à jour n'intègre généralement pas l'effet de celle-ci ;
- la recherche est prioritairement sur les conditions de filtre des propriétés indexées et d'abord sur la première d'entre elle seule à pouvoir être non `EQ` (conditions d'inégalité). L'autre condition d'inégalité `fgroupid` est appliquée sur le résultat retourné par le Datastore qu'il réduit.


# L'espace secondaire de stockage des fichiers attachés aux documents
Selon les providers ce stockage est implémenté sur un service Cloud ou un File-System local.  
Une classe abstraite `BlobProvider` donne les services minimum à fournir et chaque provider a son implémentation spécifique.

    public void blobDeleteAll()
Nettoyage général.

    public void blobStore(String groupid, String sha, byte[] bytes) 
    throws AppException
Le contenu est stocké relativement au `groupid` cité selon son `sha` en tant que nom et ceci avant même que le document ait enregistrer / valider l'existence de ce contenu.  
Clairement ce principe accumule les fichiers, même ceux qui ne sont plus référencés dans le groupe. Ceci laisse du temps pour synchroniser dans une session distante une image cohérente d'un document et de ses pièces jointes assurées de pouvoir être obtenues conformément à l'état du document synchronisé dans une version donnée.

    public byte[] blobGet(String groupid, String sha) 
    throws AppException
Retourne un contenu stocké dans un groupe depuis son sha.

    public boolean cleanup(String groupid, HashSet<String> shas)
Le nettoyage d'un groupe est lancé par une tâche qui s'exécute dans les N heures après une création / modification / suppression de pièces jointes sur un des documents du groupe.  
Si rien ne bouge pendant un certain temps sur un groupe aucun nettoyage n'est lancé et si des modifications frénétiques sont effectuées, au plus un nettoyage intervient par groupe de N heures (1, 2, 3, 4, 6, 8 ou 12) configurable.  

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
- `theme` :  lettre identifiant le thème visuel utilisé la charte graphique de base externe ;
- `label` : un intitulé plus long pour l'espace de noms ;
- `qm` : identifiant du Queue Manager gérant le namespace ;
- `nsbuild` : numéro d'ordre de la dernière modification du thème, du label, du queue manager, ou de l'une des ressources.

Quelques fichiers de ressources peuvent être définis, typiquement :
- `custom.css` : directives CSS intégrées à la suite du thème associé à l'espace ;
- `custom.js` : un script javascript exécuté après les autres juste avant le démarrage effectif de l'application dans le browser ;
- `logo.png` : un fichier image représentant un logo pour la page d'accueil de l'application graphique.

Toutes ces ressources ont obligatoirement une valeur par défaut (figurant dans `/var/z/...`) : en cas d'absence de valeur spécifique à l'espace de noms c'est la valeur par défaut de "z" qui est utilisée.  
L'espace `z` n'est pas configurable dynamiquement, une erreur risquant de rendre tous les domaines inaccessibles : il ne peut l'être que dans la livraison de l'application (qui, elle, est testée avant mise en ligne).

Une session externe standard ne peut accéder qu'à un seul namespace fixé dans son URL d'appel (et reconduite dans toutes les URLs d'opération).

Une session externe d'administration des namespaces peut accéder à la liste des namespaces afin de les initialiser, les mettre on/off ou les configurer.

La configuration d'un namespace est contenue dans un document de classe `NsDoc` ayant pour `docid` le nom de l'espace, pour `groupid` une constante donnée à la configuration (typiquement `namespace`) et pour namespace en général `z` ou tout autre donné à la configuration.

# Stockage en R-DBMS
Les colonnes `contentt` et `contentb` vont de pair :
- si le texte (typiquement un JSON) est court il est stocké dans `contentt`;
- sinon il est gzippé et stocké en `contentb`. L'une des deux colonnes est toujours `null`.

### Table `dbinfo`
    CREATE TABLE dbinfo (
    info text NOT NULL
    );
Cette table contient une constante : elle sert principalement au serveur à vérifier qu'il peut accéder à la base de données au lancement.

### Table `gstate`
    CREATE TABLE gstate (
    groupid varchar(255) NOT NULL,
    version bigint NOT NULL,
    ndn int NOT NULL,
    v1 bigint NOT NULL,
    v2 bigint NOT NULL,
    contentt text,
    contentb bytea,
    PRIMARY KEY (groupid, version)
    );
Cette table donne l'état d'un groupe : 
- sa version, 
- le numéro du prochain document à créer,
- les volumes v1 et v2 utilisés,
- la liste des identifiants des documents existants.

Un row y est créé dès la première demande d'accès à un groupe, même si aucun document n'y est ensuite inscrit. 
>Un batch de nettoyage est envisageable pour supprimer tous les `gstate` dont le `contentt` serait `null` ou `""` et le `contentb` `null` et la version 0 ou inférieure à un seuil donné.

### Table `s2cleanup`
    CREATE TABLE s2cleanup (
    groupid varchar(255) NOT NULL,
    hour int,
    CONSTRAINT s2cleanup_pk PRIMARY KEY (groupid, hour)
    );
Cette table indique pour chaque groupe SA prochaine heure de cleanup plannifiée. Contraitement aux apparences sa clé primaire logique est `groupid` : si `hour` y figure c'est pour bénéficier de l'index de clé primaire sans en créer un second.  Un row est inséré en même temps que le row correspondant dans gstate à la première sollicitation en lecture d'un `groupid`.

### Table `dstate`
    CREATE TABLE dstate (
    groupid varchar(255) NOT NULL,
    version bigint NOT NULL,
    dn varchar(8) NOT NULL,
    v1 bigint NOT NULL,
    v2 bigint NOT NULL,
    docid varchar(255) NOT NULL,
    docclass varchar(16) NOT NULL,
    ctime bigint NOT NULL,
    CONSTRAINT dstate_pk PRIMARY KEY (groupid, version, dn)
    );
Cette table donne l'état courant d'un document existant. Les documents sont  logiquement identifiés par (`groupid, dn`) : la clé primaire comporte la `version` au milieu en raison de l'accès systématique par filtrage sur la version pour un groupid fixé.  
Ce row est mis à jour pour un document à chaque création ou mise à jour et est détruit quand le document l'est.

### Table `item`
    CREATE TABLE item (
    groupid varchar(255) NOT NULL,
    docid varchar(255) NOT NULL,
    version bigint NOT NULL,
    itn varchar(8) NOT NULL,
    clkey varchar(255) NOT NULL,
    sha varchar(255),
    contentt text,
    contentb bytea
    );
    CREATE INDEX item_v on item (groupid, docid, version);
    CREATE INDEX item_sha on item (groupid, sha) where sha is NOT NULL;

Cette table est abstraite : pour chaque classe de document il est créé une table qui hérite de celle-ci. Par exemple la table `compte` :

    CREATE TABLE compte () INHERITS (item);
Cette table héberge tous les items n'ayant pas de propriétés indexées et l'item Header du document :
- `itn` vaut `"0"` pour le header et sinon l'identifiant de l'item ;
- `clkey` vaut :
    - `null` pour le header
    - `cl` pour les singletons où `cl` est la classe du singleton ;
    - `cl/key...` pour les items collections avec la classe de l'item suivi de sa clé.
- `sha` : n'a de valeur que pour un item P. Un index permet de récupérer tous les sha existant sur un groupe.
- `contentt / contentb` contient :
    - pour un header : la liste des identifiants des items existants, séparés par un espace ;
    - pour un item JSON : son texte en JSON;
    - pour un item opaque sa valeur String.

De plus pour chaque item comportant une ou des propriétés indexées il est déclarée une table spécifique qui hérite de la table des items du document :

    CREATE TABLE compte_ent (
    dhl varchar(255),
    nomr varchar(255)
    ) INHERITS (compte);
    CREATE INDEX compte_ent_dhl ON compte_ent (dhl, groupid, docid);
    CREATE INDEX compte_ent_nomr ON compte_ent (nomr, groupid, docid);
Le nom de la table est suivie de `_` et du nom de la classe d'item.  
Il est définie une colonne pour chaque valeur indexée.  
Il est défini un index reprenant cette colonne en tête et suivie de l'identifiant du document `groupid / docid`.

### Table `taskqueue`
    CREATE TABLE taskqueue (
    groupid varchar(255) NOT NULL,
    docid varchar(255) NOT NULL,
    docclass varchar(16) NOT NULL,
    version bigint,
    nextstart bigint, 
    retry int,
    info varchar(255),
    report text,
    CONSTRAINT taskqueue_pk PRIMARY KEY (groupid, docid, docclass)
    );
    CREATE INDEX taskqueue_nexstart on taskqueue (nextstart, groupid, docid, docclass, version, retry);

Cette table est celle des tâches différées en attente de traitement ou en cours.   Le Queue Manager est certes averti de chaque modification par une requête HTTP mais périodiquement il scanne la table par les `nextStart` pour récupérer les pertes éventuelles et au lancement également.

# Stockage en GAE Datastore
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

# Mémoires cache des groupes et documents
Il existe trois niveaux de mémoires caches :
- **memcached** : c'est une mémoire partagée par toutes les instances et qui ne garde que l'état des groupes et ce pour tous les namespaces. Cette mémoire est facultative et peut être configurée pour ne pas être disponible. Si la base technique reste `memcached`, GAE propose sa mise en œuvre intégrée alors que pour les R-DBMS un client Java (`spymemcached`) assure la liaison avec le serveur `memcached` officiel et distinct. Cette mémoire se réinitialise quand le serveur `memcached` est relancé et il ne conserve les objets qu'une heure après leur dernière sollicitation.
- **mémoire cache d'instance** : elle gère dans une instance des images de groupes et de documents. Elle ne se réinitialise qu'au lancement de l'instance. Son volume est fixé par configuration ainsi que sa durée maximale de rétention par objet. Elle se nettoie automatiquement des objets selon leur ordre d’ancienneté de sollicitation.
- **mémoire cache d'opération** : elle est propre à chaque cycle `work/validation/afterwork` d'une opération à laquelle elle appartient en exclusivité. Elle contient tous les documents ayant été accédé et leurs groupes.

## Cinématique entre les niveaux de cache
#### Objet Groupe
Cet objet correspond à une photographie d'une version donnée d'un groupe et de ses documents. Cet objet présente donc un état cohérent du groupe :
- **au niveau du groupe** : 
    - sa version, 
    - ses volumes v1 et v2 totaux occupés par les documents,
    - le numéro du prochain document à créer,
    - la liste des identifiants courts des documents existants dans le groupe à cette version.
- **pour chaque document existant** dans le groupe à cette version du groupe :
    - son `DocumentId` (classe / `groupid` / `docid`),
    - son identifiants court dans son groupe,
    - sa `ctime` (date-heure de création,
    - sa version,
    - ses volumes v1 et v2.

#### Objet Document
Il est le reflet cohérent à une version donnée d'un document:
- son `DocumentId`,
- sa `ctime`,
- sa version,
- son identifiant court dans son groupe;
- le numéro du prochain item à créer,
- la liste des identifiants courts des items existants dans cette version.
- **Pour chaque item** :
    - sa classe,
    - son identifiant court,
    - sa version,
    - sa clé fonctionnelle,
    - sa valeur JSON ou String pour un opaque.

**`memcached` ne retourne que des objets Groupe complets.**

**La base de données peut retourner des images partielles**, incrémentales par rapport à une version donnée:
- **des objets Groupe** ne comportant que les descriptifs des documents ayant une version postérieure à celle passée en argument, voire rien si rien n'a changé depuis cette version.
- **des objets Document ** ne comportant que les items ayant une version postérieure à celle passée en argument. La liste des identifiants d'items existants n'est communiquée que si elle a changé postérieurement à la version passée en argument.

**La mémoire cache locale d'une opération contient des images complètes des objets Groupe et Document** : ces objets sont en cours de mise à jour par l'opération.

>Un objet Groupe détient les versions de tous les documents qui en font partie. Pour savoir si un objet Document est encore valide, on le demande à son objet Groupe.

### Scénario général
Une opération débute et cherche un document en ayant son `DocumentId`.

L'opération demande à la mémoire cache d'instance,
- **une copie (un clone) de l'objet Groupe du groupe du document** et la conserve tout au long de sa vie. Si un autre document du même groupe est demandé plus tard, l'opération utilise la copie du Groupe déjà détenue. Cette copie de Groupe a une version : il sera vérifié en début de validation de l'opération que cette version est bien toujours la dernière disponible.
- **une copie (un clone) du document**. Cette copie est exclusivement réservée à l'opération. Si le même document est demandé plusieurs fois *dans la même opération* c'est la même copie qui est retournée.

L'opération modifie à volonté les copies des documents qu'elle détient.

L'étape de validation comporte plusieurs phases :
- **la détermination des documents créés / modifiés / supprimés** et des groupes correspondants.
- **la détermination de la version de l'opération** supérieure d'au moins une milliseconde aux versions des groupes modifiés ou en lecture exclusive.
- **la vérification par verrouillage au près de la base de données que tous ces groupes ont exactement la version qu'ils avaient dans la mémoire cache de l'opération**, bref que l'opération a bien travaillé sur un état stable. Sinon l'opération est détruite et relancée un certain temps (aléatoire) plus tard, elle n'a pas eu de chance.
- **la mise à jour effective en base de données** des items, documents et groupes.
- **le commit de l'opération**.

Juste après cette phase de validation, la mémoire cache de l'opération contient par principe les versions les récentes des groupes et documents modifiés.

**La mémoire cache d'instance se met à jour pour chacun de ces objet Groupe et Document** : du fait que les opérations s'exécutent en parallèle il peut arriver qu'une demande de mise à niveau de la mémoire cache d'instance soit refusée si celle-ci dispose d'une version plus récente apportée par une fin d'opération plus rapide (ou une relecture de base survenue dans ce court laps de temps après le commit de l'opération et avant la remise à niveau de la mémoire cache d'instance.

La mémoire cache d'instance en profite pour faire mettre à jour la mémoire `memcached` partagée avec les nouvelles versions des objets Groupe.  
Juste avant d'insérer en `memcached` on relit la version détenue en `memcached`, ce qui au passage ramène la `CAS Value` (objet identifiable en GAE). Si la version relue est postérieure à celle qu'on s'apprêtait à stocker, le stockage n'est pas effectué. Sinon on range la nouvelle version en utilisant la `CAS Value` ce qui va nous assurer que durant la fraction de milliseconde entre les deux instructions aucune valeur différente ne s'est glissée.  
`memcached` ne reçoit en conséquence que des versions croissantes.

#### Scénario vu de la mémoire cache d'instance
**Quand une opération lui demande un objet Groupe** ayant une date-heure minimale donnée,
- elle en a peut-être une version : si elle a au moins la fraîcheur requise (la date-heure de début de l'opération) celle-ci est retournée ;
- sinon elle demande à `memcached` sa version : si elle a la fraîcheur requise, la mémoire cache d'instance en profite pour la garder à le place de celle qu'elle avait (plus ancienne) et la communique à l'opération;
- sinon elle demande à la base de données la dernière version, le plus souvent par delta par rapport à celle déjà détenue. Dans ce cas elle en profite pour mettre à jour la mémoire `memcached` partagée.

Quand une opération demande un document, son groupe est d'abord demandé : aisni on connaît la version requise pour le document.
- s'il est présent en mémoire avec la fraîcheur suffisante il est retourné à l'opération.
- sinon il est demandé à la base de données, et remet à jour la copie détenue en mémoire puis est cloné et retourné à l'opération.

A la validation d'une opération elle va mettre à jour son propre état depuis les groupes et documents mis à jour par l'opération, toujours à condition que cet état soit plus récent que celui qu'elle détient. Elle en profite aussi pour mettre à jour la mémoire `memcached` partagée.

#### Risques de désynchronisation
Toute désynchronisation, c'est à dire la détention d'un état en cache moins récent que le dernier mis à jour par cette instance (c'est rare) ou une autre (c'est plausible) va toujours se terminer,
- par un échec à la phase de contrôle des versions des groupes au début de la validation d'un opération,
- par l'obligation que cet échec provoque de relire la base de données à la prochaine sollicitation (l'indicateur `mustRead` de l'objet Groupe est mis à `true`),
- in fine par l'acquisition du dernier état depuis la base de données (ce qui remet `mustRead` à `false`).

**La cohérence temporelle reste assurée par le mécanisme transactionnel de la base de données** : la politique optimiste n'ayant mené en l'occurrence qu'au recyclage d'opération(s) en échec lors de la vérification de cette cohérence temporelle.

#### Optimisation pour une instance unique
En GAE c'est non maîtrisable : le nombre d'instances peut toujours être plus de un.

En R-DBMS on peut configurer et contraindre à l'existence d'un seul serveur :
- `memcached` est inutile.
- la mémoire cache d'instance étant avertie de toutes les fins d'opération a, en principe, toujours le dernier état et applique ce principe pour éviter de relire la base de données.
- toutefois quand l'indicateur `mustRead` a été mis sur un objet Groupe, c'est qu'un croisement d'opération s'est effectué entre le commit des transactions et la mise à niveau de la mémoire cache d'instance : la relecture de la base de données sera forcée à la prochaine demande.

# Queue Manager 
En GAE Datastore le Queue Manager est assuré par le Datastore.

La queue des tâches en attente d'exécution est dans la table `taskqueue` :

    CREATE TABLE taskqueue (
    groupid varchar(255) NOT NULL,
    version bigint NOT NULL,
    docid varchar(255) NOT NULL,
    docclass varchar(16) NOT NULL,
    nextstart bigint, 
    retry int,
    info varchar(255),
    report text,
    CONSTRAINT taskqueue_pk PRIMARY KEY (groupid, version)
    );
    CREATE INDEX taskqueue_nexstart on taskqueue (nextstart, groupid, version);

L'inscription d'une tâche nécessitant le verrouillage du groupe de son ID (groupid / docid / docclass), la version de l'opération est suffisante avec le groupid pour former une clé primaire.  

Chaque serveur du pool peut initialiser un **Queue Manager**, une classe définissant l'exécution de plusieurs threads.  
Un Queue Manager d'un serveur du pool à une liste de codes QM : il est en charge de gérer tous les namespaces ayant pour paramètre de configuration `qm` l'un des codes de cette liste. Les threads sont les suivants  :
- un thread `QueueManager` chargé de distribuer les tâches à exécuter à un des threads **workers** ;
- un pool de threads **workers**. Chaque worker est en attente d'une tâche à exécuter et invoque la requête HTTP-POST correspondante pour faire exécuter cette tâche par un des serveurs du pool. En cas de succès (retour 200) le worker reveille le thread `Queue Manager` qui recherche une nouvelle tâche à exécuter. 
En cas d'échec un worker,
- enregistre cet échec dans le row de la tâche dans `taskqueue` ;
- le compteur `retry` est incrémenté ;
- l'estampille `nextStart` est fixée à une date-heure plus ou moins proche selon la valeur du `retry`. Mais si le nombre de `retry` excède le maximum prévu, cette estampille est mise à la fin du siècle : l'administrateur agira en conséquence;
- le retour de la requête de la tâche est aussi enregistrée comme report de la tâche : sa lecture permet le cas échéant à l'administrateur d'enquêter sur les causes de l'échec.

Ce mécanisme garantit en conséquence une reprise automatique en cas d'échec, du moins un certain nombre de fois.

Le thread `QueueManager` est reveillé par plusieurs événements :

- ***une validation d'opération a des tâches à lancer*** : celles-ci sont transmises au thread QueueManager, soit directement s'il est hébergé dans le même serveur, soit par une requête HTTP. Une validation permet d'enchaîner sur une exécution de tâche différée quasi immédiate si nécessaire ;
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
C'est une méthode de `Config` qui détermine le numéro de queue à affecter en fonction de l'information sur la tâche ('namespace', `groupid` ... et surtout `docclass`).  
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
- création d'une tâche : ceci est utile pour initialiser les tâches qui s'auto renouvellent ensuite par un cron.
 
Une sélection des tâches inscrites peut aussi être faite en fixant facultativement certains des éléments suivants :

 - son groupe de documents,
 - sa classe de document,
 - sa date-heure de relance (toutes celles prévues avant cette heure),
 - son occurrence d'échec (première ou plus, seconde ou plus ...),
 - son texte d'information qui en général contient le code cron quand il s'agit d'une tâche périodique lancée par ce procédé..

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
Quelques méthodes donnent également :
- la création d'un nouveau `Credential` (étend `ACredential`) ;
- la liste des classes de document et des opérations de l'application ;
- quelques libellés d'exceptions générales de l'application.