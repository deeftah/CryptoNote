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
