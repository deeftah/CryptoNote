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

Le nombre d'espaces peut être de ***quelques dizaines au plus*** (limite raisonnable des R-DBMS), l'administration pouvant s'avérer délicate au delà.  
C'est un moyen important pour augmenter le débit global et permettre des migrations d'espaces d'un hébergeur vers un autre.

# Documents et items
Le stockage de données apparaît comme un ensemble de ***documents***,
- ayant une **classe de document** qui en Java étend la classe `Document`.
- identifiés par un `String` nommé par la suite `docid`.
- un document a une **version**, estampille en millisecondes UTC, sous forme d'un `long` lisible (171128094052123) et croissante dans le temps : la version est la date-heure de l'opération qui a mis à jour le document.

### Items
Un document est un ***ensemble d'items***,
- ayant une **classe** d'item qui en Java correspond à une sous classe statique de la classe du document et qui étend la classe `Document.BItem` et plus spécifiquement `Item` `RawItem` `Singleton` `RawSingleton` qui étendent `BItem`.
- un item `Singleton` existe en une occurrence au plus dans son document.
- un item `Item` existe en 0 à N occurrences identifiées par un `String` nommé par la suite `key` avec la seule contrainte d'une longueur maximale de 255.
- un item a une **version** (date-heure de l'opération qui a mis à jour le document).
- *un item peut avoir une marque `D`* indiquant qu'il est détruit, sa version indiquant quelle opération l'a détruit. Un item détruit reste marqué D pendant un certain temps afin de permettre la synchronisation rapide du contenu du document avec un contenu distant, puis est finalement physiquement purgé.
- **la valeur d'un item** est un String qui peut être,
    - un JSON (`Singleton` et `Item`),
    - un `String` opaque (`RawSingleton` et `RawItem`), typiquement l'encodage en base 64 d'un vecteur d'octets.

