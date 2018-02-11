# Notes Secrètes - API-REST

L'API est de type REST : chaque requête est indépendante des précédentes et peut être traitée par n'importe quel serveur du pool. 

## Opérations
L'exécution d'une requête correspond à une opération :
- **non authentifiée**, associée à aucun compte.
- **authentifiée** associée à un compte : la session terminale ayant émis la requête  fournit la preuve que le titulaire du compte en a délivré la phrase secrète.

### Opérations NON authentifiées
N'importe quelle application n'importe où dans le monde peut lancer une telle opération.  
Une opération de simple consultation n'a, en général, pas strictement besoin d'être authentifiée : les données retournées sont cryptées et illisibles par la session cliente si elle ne dispose pas des clés issues de la phrase secrète.  
L'opération de synchronisation est authentifiée afin de se faire délivrer des contenus qui, même cryptés, ne sont retournés qu'à des comptes cités dans les dossiers.

### Session *cliente* associée à un compte
Une **session cliente associée à un compte** se déclare dans un terminal en demandant la frappe de la phrase secrète du compte. Cette saisie enregistre dans la session cliente,
- `cleS` : SHA-256 du BCRYPT de la phase complète. **Cette forme ne sort jamais de la session**.
- `psrB` : BCRYPT de la phrase réduite. **Cette forme s'échange sur le réseau** (cachée par le HTTPS).
- `psrBD` : SHA-256 de `psr`. **Cette forme est stockée en base**.

La requête **non authentifiée** `Xauth` demande au serveur l'entête `EnC` (dont `c0S` la clé 0 du compte cryptée par la clé S) du compte ayant pour propriétés `psrBD` le SHA-256 de l'argument de la requête `psrB` et la constante TPU correspondant à ce numéro de compte: 
- si le serveur trouve ce compte, cela prouve que le titulaire a bien saisi la bonne phrase secrète.
- si la session peut décrypter `c0S` par la clé S détenue dans sa mémoire :
    - elle conserve la clé 0 qui permet de décrypter toutes les autres et les données du compte.
    - elle pourra effectuer des requêtes authentifiées sur ce compte en transmettant sur chacune (`nc->account` `psrB->key`).
- si la session ne peut pas décrypter `c0S` par sa clé S, 
    - soit le compte a été créé par une session de logiciel non officiel.
    - soit la phrase secrète du compte a été changée par une session de logiciel non officiel.
    - soit elle-même s'exécute avec un logiciel non officiel.
    - *si le problème persiste*, le compte est définitivement corrompu et ses données inutilisables. Le titulaire du compte sera invité à se résilier lui-même pour cause de compte corrompu.

### Opérations sur un compte authentifié
Une requête peut être invoquée en spécifiant un compte *authentifié* en fournissant la preuve que la session terminale est bien contrôlée par le titulaire de ce compte en ayant fourni la phrase secrète.
- pour une opération de mise à jour ceci permet de se protéger contre des requêtes de pure nuisance qui transmettraient des données fausses ou mal cryptées en paramètre.
- pour une consultation / synchronisation, des données peuvent être retournées ou non selon l'état du compte correspondant vis à vis des données souhaitées.

La requête porte deux paramètres identifiant et authentifiant le compte de la requête :
- `account` : le numéro du compte.
- `key` : le `psrB` (BCRYPT de la phrase secrète réduite du compte).

Dans le serveur le dossier du compte est obtenu depuis `account` : dans son singleton `EnC` la propriété `psrBD` est censée être le SHA-256 de `psrB` passé en `key` sur la requête (ce que l'opération vérifie).

### Opérations sous privilège d'administrateur
L'argument `sudo` de la requête contient le BCRYPT de la phrase secrète réduite d'administration dont le SHA-256 est enregistré en configuration des serveurs. Ce privilège peut être :
- celui toutes instances confondues qui permet de les mettre on / off et d'inspecter les queues / traces des tâches pour des besoins techniques.
- celui de chaque instance déclaré à la configuration pour chaque instance séparément.

La clé AES tirée de la phrase secrète complète crypte certaines données (des disques virtuels par exemple).

# Comptabilité
La comptabilité est gérée par deux tables indépendantes des dossiers.

### Table `LCR` : ligne de crédit.
Chaque item est une ligne de crédit : 
- `psBD` : SHA-256 de la phrase secrète brouillée d'inscription de la ligne de crédit.  
- `psrB` : propriété indexée. Phase secrète réduite brouillée afin d'éviter que la même phrase secrète ne soit utilisée par deux lignes de crédit.
- `cr` : montant du crédit porté par la ligne.
- `dh` : date-heure à laquelle la ligne a été enregistrée.

L'opération de transfert d'une ligne de crédit sur un compte ou un forum requiert que la session fournisse le BCRYPT de la phrase secrète donnée par le payeur (dont le SHA-256 est le `docid` de la ligne de crédit).

### Table `CRE` : crédit courant
**Propriétés :**
- `id` : numéro de compte ou de forum.
- `cr` : montant du crédit courant en unités de compte. Il peut être négatif.
- `v1 v2` : volumes occupés lors du dernier calcul du crédit restant.
- `dh` : date-heure de dernier calcul du crédit restant.
- `dho` : date-heure de signalement du franchissement de la zone orange.
- `dhr` : date-heure de signalement du franchissement de la zone rouge : blocage des opérations (sauf crédit).
- `dhz` : date-heure de passage en zombie.

L'opération de crédit verse tout le crédit d'une ligne authentifiée sur le `CRE` du compte et détruit cette ligne.

Le crédit est réduit :
- *à chaque fin d'opération* :
    - le coût `cv` de l'occupation du volume depuis le dernier calcul est calculé.
    - le nouveau volume est mémorisé.
    - le crédit est réduit de `cv` et de celui de l'opération (dont le volume d'entrée / sortie).
- *périodiquement*, par exemple toutes les semaines, pour calculer les coûts d'occupation d'espace en l'absence d'opérations. 

# Constantes
Ces données ont :
- une clé primaire.
- `dh` : une date-heure de création de la constante.
- `alias` : un alias éventuel (unique).
- `value` : une valeur qui reste constante depuis la création de la constante.

Une constante est créée, peut être purgée mais jamais mise à jour.

## Constante : `TPU` : ticket public d'un compte
Ce ticket a été généré par la session créatrice du compte et comporte les propriétés suivantes :
- `dh` : date-heure de génération.
- **alias** : `nomrBD` : SHA-256 du BCRYPT du nom réduit utilisé pour détecter et interdire l'usage de noms trop proches.
- `c1O` : clé 1 du compte cryptée par la clé 0. Elle crypte le nom du compte dans son ticket public et ceux des comptes certifiant son identité.
- `nom1` : nom crypté par sa clé 1.
- `pub` : clé RSA publique de cryptage.
- `verif` : clé RSA publique de vérification.
- `priv0` : clé privée RSA de décryptage cryptée par la clé 0.
- `sign0` : clé privée RSA de signature cryptée par la clé 0.

**La clé de la constante** est le SHA-256 des champ `nom1 pub verif` et est le numéro de compte.  

Toute session disposant du numéro de compte peut en obtenir le ticket public qui permet :
- d'obtenir les clés de cryptage et de vérification de signature,
- de s'assurer de sa validité en recalculant le SHA-256 des champs `nom1 pub verif` et en le comparant au numéro de compte.
- s'il connaît la clé 1 -clé des noms- d'obtenir le nom.

Après destruction du compte, la constante est inutile et détruite.

#### Clés privées d'accès d'un compte
Un compte `nc` donné peut avoir, successivement dans l'application, plusieurs clés privées, autant que de phrases secrètes successivement choisies par son titulaire.  
Cette clé est cryptée par la phrase secrète du compte : c'est elle qui permet au titulaire d'accéder aux clé privées et clé 1 (donc nom) du `TPC`.

## Constante : `TCR` : trace d'un compte résilié
La clé est le numéro de compte et n'a que trois propriétés destinées à éviter le réemploi dans l'instance d'un nom proche à celui d'un compte ayant existé dans l'instance.
- `dh` : date-heure de destruction.
- `dhc` : date-heure de création.
- **alias** : `nomrBD` : BCRYPT du nom réduit.

# Dossier `Compte`
La clé d'accès `docid` est le numéro de compte `nc`.

**Singleton :** 
- `EnC` : entête du compte.
- `Cid` : certificat d'identité.
- `Crt` : comptes certifiés.

**Items :**
- `Phc` : phrases de contact.
- `Pho` : photos d'identité.
- `Rep` : entrée du répertoire de contacts.
- `Vno` : version d'une note.
- `Pth` : paths d'une note.
- `P` : pièce jointe (standard).
- `Flg` : flags d'un forum accédé.
- `Fac` : forum accédé.

Le *profil* d'un compte est constitué de : 
- la constante `TPU`,
- les singletons et items `EnC Cid Crt Phc Pho`.

## Singleton : *Entête du compte* `EnC`
Ce singleton est déclaré à la création du compte et n'est modifié qu'à quelques rares occasions :
- mise à jour de la phrase secrète du compte.
- lors des changements d'état : *début d'activité, passages zombie / retour à l'état actif, destruction physique*.

**Propriétés :**
- `psrBD` : *propriété indexée*. SHA-256 du BCRYPT de la phrase secrète réduite. Permet de ne pas tolérer que des phrases secrètes soient trop ressemblantes dans l'instance.
- `c0S` : clé 0 du compte cryptée par le SHA-256 du BCRYPT de la phrase secrète. Cette clé ouvre toutes les autres clé mères du compte présentes dans son `TPU`.
- `dha` : date-heure de début d'activité.
- `dhzm` : date-heure de passage en zombie posée par la modération de l'instance.
- `dhzc` : date-heure de passage en zombie en raison d'un défaut de crédit.
- `dhza` : date-heure de passage en zombie posée par le compte lui-même.
- `dhx` : *propriété indexée* date-heure de destruction prévue (s = 2) : la plus proche résultante des `dhz?` et `dha` quand le compte est en création (s == 0 / `dha` absente), NC jours ou NZ jours après ces dates.

Le statut courant s est calculé depuis les dates `dha dhzm dhzc dhza`.
- 0 : en création. 
- 1 : actif.
- 2 : zombie, destruction imminente.

### Singletons : *certificat d'identité* : `Cid` et *certifications* : `Crt`
Le certificat d'identité `Cid` comporte une liste et sa signature :
- `lst` : une liste de triplets `dh nc nomc1A` ou `dh nc dhf` ordonnés par `dh`, chacun correspond à la certification du nom de A par un compte C du numéro `nc` à la date-heure `dh` en fournissant `nomc1A` son nom crypté par la clé 1 du compte A. Si la certification est résiliée, `dhf` est la date-heure de fin de certification (précédée de $) et remplace `nomc1A`.
- `nc` : le numéro du dernier compte ayant mis à jour le certificat,
- `sign` : sa signature de la liste.
- `dh` : la date-heure de l'opération.

`Crt` la liste des comptes certifiés par A et comporte comporte une liste et sa signature :
- `lst` : une liste de triplets `dh nc nomc1C` ordonnés par `dh`, chacun correspond à la signature d'un compte C du numéro `nc` à la date-heure `dh` en fournissant `nomc0` le nom de C crypté par la clé 0 du compte A. `nomc1A` peut être remplacé par `dhf` (date-heure de fin précédée de $), signifiant l'effacement d'une certification.
- `nc` : le numéro du dernier compte ayant mis à jour la liste des certifications,
- `sign` sa signature de la liste.
- `dh` : la date-heure de l'opération.

L'opération de certification ou suppression de certification par A du compte C opère en symétrique sur le `Crt` de A et le `Cid` de C. Elle vérifie :
- que A a bien signé les deux listes.
- que A n'a pas altéré dans le `Cid` des triplets qui ne sont pas le sien : il peut mettre à jour le sien mais pas les autres et s'il ajoute le sien que c'est bien à la fin.
- que dans la liste `Crt` seul le triplet de C est ajouté / supprimé.

L'opération ne peut pas vérifier les noms ni leur cryptages dans `Cid` et `Crt`.

Étant signées dans leur globalité, les certificats ou liste de certifications sont garanties complètes ou complètement détruites par piratage.

La création d'un compte a signé des `Cid` `Crt` vides (mais bel et bien signées par A).

### Items *phrase de contact* `Phc`
Un compte A peut déclarer à un instant donné jusqu'à 4 phrases de contact permettant d'être contacté par un compte C n'ayant pas A dans ses cercles de connaissance. Chaque phrase est garantie unique.

> Une phrase réduite est elle-même garantie unique afin d'éviter de tomber par hasard (ou en utilisant un robot) lors de la déclaration d'une phrase sur une déjà enregistrée et d'en déduire qu'un compte peut être contacté par cette phrase.

**Clé** : index de 1 à 4.

**Propriétés :**
- `pcrBD` : *propriété indexée unique*. SHA-256 du BCRYPT de la phrase réduite pour bloquer les phrases trop proches d'une phrase déjà déclarée.
- `pc0` : phrase de contact cryptée par la clé 0 du compte.

### Items *contact du répertoire* `Rep`
A peut avoir C comme contact : `Rep(A)` a une entrée C : `cDansA`.  
C peut avoir A comme contact : `Rep(C)` a une entrée A.  `aDansC`.

**Clé** : numéro de compte du contact C.

**Propriétés** :
- `dh` : date heure de la dernière opération.
- `nom0` : nom du contact crypté par la clé 0 du compte.
- `c10` : clé 1 du contact cryptée par la clé 0 du compte.
- `nc1P` : couple du nom du compte et de sa clé 1 crypté par la clé publique du compte et déposé par le contact lorsqu'il les dévoile au compte. A la première occasion, cette propriétés est ré-encryptée en nom0 c10 par le compte (plus court et plus rapide à interpréter).
- `memo0` : un court texte de commentaire du compte à propos de son contact (crypté par la clé 0).
- `photo0` : une photo d'identité du contact qui a pu être récupérée dans une note reçue ou sur un forum (crypté par la clé 0).
- `dhi` : date-heure d'inscription du contact dans le répertoire.
- `dhr` : date-heure de résiliation du contact.
- `cf` : confiance mutuelle entre le compte et son contact :
    - 0 : aucune 
    - 1 : le compte a accordé sa confiance au contact mais l'inverse n'est pas vrai.
    - 2 : le contact a accordé sa confiance au compte mais l'inverse n'est pas vrai.
    - 1 : confiance mutuelle.
- `dhdc` : date-heure de demande au contact de certification émise par le compte. Effacée quand le contact a certifié l'identité du compte ou que le compte a renoncé à sa demande.
- `fav` : 1:contact favori (affiché en tête), 0:normal.

#### Opérations
##### Inscription d'un contact / mise à jour
Le compte A peut inscrire un contact C en fournissant a minima son numéro de compte.  
Il peut simultanément ou ultérieurement fournir :
- `memo0` : un court texte de commentaire personnel à propos du contact.
- `photo0` : une photo d'identité du contact qui a pu être récupérée dans une note reçue ou sur un forum
- `nom0` : le nom du contact s'il l'a obtenu par ailleurs dans l'application (coparticipant à un forum, certificateur d'un de ses contacts ou d'un coparticipant à un forum).
- `c10` : la clé 1 du contact s'il l'a obtenue par ailleurs dans l'application (coparticipant à un forum).
- `cf` : 0:inchangée -1:retrait de sa confiance 1:confiance accordée. Dans ce dernier cas il donne également le paramètre `nc1P` quand `nom0 c10` sont absents dans `ADansC`.
- demande de certification / renoncement à cette demande : cette demande suppose que le compte ait donné au contact sa clé 1 et son nom (`nc1P`).

##### Certification d'identité
Le compte peut certifier l'identité d'un contact à condition :
- que le contact l'ait demandé (`dhdc` présente dans son entrée `ADansC`).
- que le contact ait donné sa clé 1.

Les certifications d'identité (que A a accordé à C ou que C a accordé à A) peuvent être radiés par A.

##### Suppression d'un contact

## Items *version d'une note* `Vno`
La clé est le couple :
- `nn` : numéro de la note générée à la création de sa première version.
- `dh` : date-heure de sa version.

Le numéro de la note est :
- `m` : 0 pour une note immuable, 1 pour une note révisable.
- `dhc` : date-heure de création : 6 bytes, 8 base 64.
- `rnd` : random sur 9 bytes, 12 base 64.

La version d'une note est immuable.

***Propriétés***
- `au` : le numéro de compte de son auteur.
- `sj` : son sujet crypté : c'est un texte court sans formatage.
- `mc` : liste des mots clés cryptée.
- `tx` : son texte crypté : il peut être relativement long et son format MD autorise à la fois une lecture / écriture textuelle et un aspect plus plaisant (titres, liste à puces, gras / souligné ...). Au delà d'une certaine taille le texte est compressé.
- `gz` : indicateur de compression du texte.
- `ic` : une vignette / icône cryptée : elle est de faible définition (32x32).
- `hs` : SHA-256 du sujet en clair.
- `ht` : SHA-256 du texte en clair.
- `hi` : SHA-256 de l'icône / vignette en clair.
- `pj` : liste des références des pièces jointes : triplets :
    - SHA-256 du contenu en clair (clé de la pièce jointe).
    - nom local dans la note.
    - clé de cryptage cryptée par la clé de la note.
- `sg` : signature de `dh hs ht hi SHA des pj` séparés par un espace, l'absence d'un argument est noté par 0. Cette signature a été faite par la clé privée de signature de son auteur (voir son TPU).

En ne disposant que de l'enregistrement d'une note il est juste possible de vérifier qu'elle est correctement signée :
- obtention du ticket public du compte depuis le numéro de compte de l'auteur. La validité de ce ticket est vérifiable en elle-même. Le nom de l'auteur,
    - peut être obtenu du ticket si sa clé 1 est connue.
    - peut être confirmé s'il est supposé connu.
- en obtenant de ce ticket la clé publique de vérification de signature et en vérifiant que la signature par ce compte a bien signé `dh hs ht hi hp`.

En disposant de plus de la clé de cryptage de la note :
- les textes peuvent être obtenus en clair.
- il est possible de s'assurer que l'auteur n'a pas signé un faux en recalculant les (`hs ht hi sha des pj`).

## Items *pièce jointe* `P`
La clé d'une pièce jointe est le SHA-256 de son texte en clair.  
Une pièce jointe est immuable.

***Propriétés***  
- `dh` : date-heure de son enregistrement.
- `tm` : son type MIME précédé de # si le texte est gzippé.
- `lg` : taille du texte compressé et crypté (sur disque).
- `vg` : vignette 32px (facultative) en base 64 cryptée par la clé de cryptage de la pièce jointe.
- `au` : numéro de compte de l'auteur.
- `sg` : signature de `sha dh mt lg` par son auteur.

Le texte lui-même est enregistré sur le volume 2 : son texte est encrypté par sa clé et son identifiant est le `sha` de son texte en clair.

## Items *paths de note* `Pth`
Le compte mémorise l'ensemble des notes écrites par lui-même ou qu'il a obtenues.  
La clé d'accès est :
- `nn` : numéro de la note. 
- `dh` : version de la note.

***Propriétés :***
- `cn0` : clé de cryptage de la version de la note cryptée par la clé 0 du compte.
- `paths` : liste des paths de rangement de la note dans l'arborescence. La racine de chaque path est :
    - soit `home`.
    - soit le numéro du forum.
- `lp` : liste des participants auteurs autorisés (seulement pour `Pth` dans un Forum).
- `dht` : date-heure de mise à la corbeille.

#### Pièces jointes à la corbeille
La liste des pièces jointes mises en corbeille se calcule :
- la liste des pièces jointes référencées et par quelles versions de note s'obtient par browsing des items `Vno`.
- sont marquées à détruire toutes celles qui ne sont plus référencées ou par seulement des versions de notes obsolètes : ceci donne un volume v2 libérable.
- celles qui ne sont pas à détruire sont celles utiles.

On peut ainsi obtenir par calcul libérable en vidant la corbeille des notes jetées depuis au moins N jours :
- volume v1 des notes.
- volume v2 des pièces jointes.
- la liste des pièces jointes à conserver en v2, ce qui permettra de nettoyer les autres dont celles parasites créées dans une transaction et mal supprimées à la fin (le v2 n'étant pas strictement protégé par le commit du v1, il peut en avoir trop).

#### Opérations
##### Création / mise à jour d'une note

#### Mise à la corbeille d'une note

##### Sortie de corbeille d'une note

#### Vider la corbeille

## Items *partage de note* `Pno`
***Clé*** :
- `er` : 0:export 1:reçu
- `nc` : vers / de compte
- `nn.dh` : version de note
- `dhp` : date-heure de partage.

***Propriétés***  
- `cn0P` : (r) clé de la note cryptée par la clé 0 ou P.
- `sj` : (r) sujet de la note.
- `mc` : (r) mots clés de la note.
- 'forum' : (r) si la note est celle d'un forum et,
    - qu'elle est accessible aux invités et que le réceptionnaire est invité du forum. 
    - que le réceptionnaire est participant au forum.
    - dans ces cas `cn0P` est absente.
- Pour une note exportée c'est le statut de la réception de l'autre côté.
    - `dhv` : date-heure de vue et laissée en attente.
    - `dhc` : date-heure de copie locale.
    - `dhs` : date-heure de suppression (e) uniquement.

#### Opérations
##### Partage d'une note vers un autre compte
Ce compte doit être :
- contact de confiance.
- coparticipant à un même forum.

##### Vu une note proposée par un autre compte

##### Copie locale d'une note proposée par un autre compte

##### Suppression de l'avis de partage

## Items *Forum accessibles* `Fac`
**Clé** : `nf`. Identifiant du forum.

**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `memo0` : nom du forum sur la première ligne, commentaires ensuite (crypté par sa clé 0).
- date-heures du dernier cycle d'invitation (copie de `Paf`):
    - `dh1` : invitation.
    - `dh2` : acceptation de l'invitation. Début du vote sur la confirmation.
    - `dh3` : confirmation.
    - `dh4` : résiliation en discussion. Début du vote sur la résiliation.
    - `dh5` : résiliation effective.
- `bureau` : est membre du bureau.


## Items *flags du forum* `Flg`
**Clé** : `nf`. Identifiant du forum.

**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `flags` : duplication asynchrone de `Flg` dans `Forum`.

## Opérations non authentifiées
#### `Xauth` : Opération non authentifiée *authentification d'un compte*
Retourne `EnC` du compte ayant une phrase réduite brouillée `psRB`.  
Le serveur accède au compte dont `psRB` (clé indexée unique) est le `psRB` passé en argument et vérifie que `psBD` du compte est bien égal au SHA-256 de `psB` passé en argument.  
La session cliente dispose ainsi au retour *du numéro et du nom du compte* et des *clés de cryptage nécessaires* à la lecture des autres données du dossier compte, du moins si le SHA-256 de la phrase secrète décrypte bien la clé 0 (`c0S`) sinon c'est que le compte a été corrompu.  
La **date-heure de dernière visite** n'est enregistrée dans `EnD` que si le compte n'est pas *résilié* (ou tâche de clôture en cours), que la reconnaissance de `psB` a été un succès et que la date-heure passée en paramètre est supérieure à celle déjà enregistrée.

Paramètres :
- `dhop` : date-heure d'opération (devient la date-heure de dernière visite).
- `psRB` : phrase secrète réduite brouillée du compte.
- `psB` : phrase secrète brouillée du compte.

Retourne : `EnC`du compte.

#### `Xcre` : Opération non authentifiée *création d'un compte*
La création d'un nouveau compte retourne comme `Xauth` le singleton `EnC`du compte qui vient d'être créé. La session cliente dispose ainsi au retour du numéro et du nom du compte et des clés de cryptage nécessaires à la lecture des autres données du dossier compte.
- si un compte existe déjà avec la même clé secrète (SHA-256 de `psB`) et le même nom réduit (`nomRB`), on considère que le compte a déjà été créé et `EnC` est retourné comme si c'était une opération `Xauth`. 
- si ce n'est pas un compte premier, une conversation `contact` est ouverte avec demande de certification au compte pressenti comme parrain certificateur.
- le CV à destination du parrain est enregistrée.
- la date-heure de dernière visite est enregistrée dans `EnD`.

Paramètres :
- `nc` : numéro du compte.
- `psRB` : BCRYPT de la phrase secrète réduite.
- `psB` : BCRYPT de la phrase secrète.
- `c0S` : clé 0 générée cryptée par le SHA-256 de la phrase secrète.
- `c10` : clé 1 du certificat d'identité crypté par la clé 0.
- `pub` : partie publique en clair de la clé asymétrique.
- `priv0` : partie privée cryptée par la clé 0 de la clé asymétrique.
- `nom1` : nom suffixé du compte crypté par la clé 1.
- `nomRB` : BCRYPT du nom réduit.
- `ins` : inscription au répertoire du compte certificateur pressenti. (compte standard seulement)   
    - `cmma0` : clé M cryptée par la clé 0 du compte.
    - `cmsa0P` : clé M cryptée par la clé publique de C.
    - `ntf` : numéro du dialogue à utiliser.
    - `txtM` : message de courtoisie à transmettre à C crypté par la clé M.
    - `monnomM` : nom suffixé du compte crypté par la clé M.
    - `cvcM` : clé du CV #1 crypté par la clé M.
    - `c1M` : clé de certification 1 cryptée par la clé M.
    - `bpRB` : code de boîte postale réduite.
- `cv` : CV numéro 1. (compte standard seulement).
    - `c0` : clé C de cryptage du CV cryptée par la clé 0.
    - `l0` : court libellé identifiant le CV #1 crypté par la clé 0.
    - `tC` : texte du CV crypté par la clé générée.
    - `pC` : texte de la photo crypté par la clé générée.
    - `c1C` : clé de certification 1 cryptée par la clé C.
- `jeton` : jeton de création donné par l'administrateur. (compte premier seulement).

Retour :
- `EnC` comme pour `Xauth`.

#### `Xanc` : Opération *annulation de demande de clôture du compte*
Invoquée par le titulaire du compte. Comme une `Xauth` mais vérifiant que le compte est bien sous la menace d'une demande de clôture par le compte. Celle-ci est supprimée et de plus un avis est émis.

Paramètres : 
- `dhop` : date-heure d'opération (devient la date-heure de dernière visite).
- `psRB` : phrase secrète réduite brouillée du compte.
- `psB` : phrase secrète brouillée du compte.

Retour :
- `EnC` comme pour `Xauth`.


# Forum
Le `docid` est un identifiant universel `nf` tiré au hasard à la création du forum.

**Singleton :** 
- `EnF` : entête du forum.

**Items :**
- `Vno` : version d'une note.
- `Pth` : paths d'une note.
- `P` : pièce jointe (standard).
- `Flg` : flags d'un participant.
- `Paf` : participant au forum.
- `Fnw` : fil de nouvelles.
- `Snd` : sondage.

Il possède les items `Vno Pth P` comme Compte.  
`Pth` a deux propriétés supplémentaires : 
- liste des index des signataires.
- document accessible aux invités (la clé est fixe) ou réservé aux confirmés (la clé peut avoir plusieurs valeurs au cours du temps).

L'item `Flg` est le symétrique de `Flg` de Compte avec le numéro de compte comme clé.
- un vote de changement de gouvernance est en cours
- une élection de délégués est en cours.
- une invitation est en cours.
- une résiliation est en cours.
- une note ayant un des mots clés référencés par le compte a été écrite / mise à la corbeille.

### Singleton *entête du forum* `EnF`
**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `dhcc` : date-heure du dernier changement de clé.
- `nbp` : nombre de participants ayant été invités.
- `basegv` : schéma de gouvernance.
    - `b` : liste des membres du bureau.
    - `p c a g` : 4 chaînes donnant les schémas de vote applicables aux quatre groupes de décision (participants, clôture du forum, autres décisions, changement de gouvernance).

## Items *participant au forum* `Paf`
***Clé*** : numéro de compte.

***Propriétés***
- `idx` : index du participant dans le forum.
- `cif0P` : clé d'invitation du forum cryptée par la clé 0 ou P du participant.
- `hcf0P` : historique des clés du forum cryptée par la clé 0 ou P du participant.
- `nomI` : nom du participant crypté par la clé d'invitation du forum.
- `c1I` : clé 1 du participant crypté par la clé d'invitation du forum.
- `phI` : photo d'identité du participant crypté par la clé d'invitation du forum.
- `pdhc` : date-heure de début de la première phase confirmée.
- `ddhc` : date-heure de fin de la dernière phase confirmée (absente si encore confirmé).
- `acnf` : auto confirmé dès l'acceptation de l'invitation.
- date-heures du dernier cycle d'invitation :
    - `dh1` : invitation.
    - `dh2` : acceptation de l'invitation. Début du vote sur la confirmation.
    - `dh3` : confirmation.
    - `dh4` : résiliation en discussion. Début du vote sur la résiliation.
    - `dh5` : résiliation effective.
- `bureau` : est membre du bureau.
- `dhef` : date-heure du dernier effacement des fanions sur les fils d'actualité.
- `news` : liste des indices des news intéressantes pour le participant.

### Historique des clés du forum
La clé d'invitation n'est pas historisée.  
Les pièces jointes ont leurs clés transmises par la ou les notes qui y font référence. Elles sont immuables et ne sont pas directement liées aux clés du forum.  
Une note change de clé à chaque version : à l'écriture elle est cryptée par la dernière clé. Pour accéder à une version d'une note il faut regarder sa date-heure et prendre la clé de l'historique qui était valide dans le vecteur `[[dh1,c1], [dh2,c2] ...]`.

L'opération de changement de clés a quelques contraintes :
- la session doit encoder la nouvelle clé générée avec la clé P de chacun des participants, opération qui en soit peut prendre du temps en session.
- la mise à jour dans le serveur vérifie que tous les participants *confirmés* sont bien listés dans le paramètre de l'opération donnant la nouvelle clé.

### Items *fil de nouvelles* `Fnw`
***Clé :***
- numéro de fil.
- date-heure.
- numéro de note ou de sondage
- code de l'événement.
    - Pour une note :
        - c : création.
        - m : mise à jour.
        - p : mise à la poubelle.
        - s : sortie de poubelle.
    - Pour un sondage :
        - c : création (ouverture de discussion éventuelle à propos de ses paramètres).
        - o : ouverture du sondage.
        - 0-3 : fin du sondage (son résultat).

### Items *sondage* `Snd`
Un sondage / vote peut avoir selon les cas et les règles de gouvernance en cours :
- une valeur décisionnelle vis à vis d'une opération à réaliser qui ne peut être engagée que suite à un vote positif.
- une valeur indicative sans impact d'autorisation / blocage d'une quelconque opération.

**Clé** :
- `obj` : objet du sondage `c r x t p g n`:
    - (c) confirmation d'invitation de l'invité `nc`.
    - (r) confirmation de la résiliation de l'invité `nc`.
    - (x) clôture du forum.
    - (t) transfert de crédit `{c1:m1, c2:m2, f1:m3 ...}` à des comptes / forums.
    - (p) mise à la corbeille de notes obsolètes : `[n1, n2 ...]`
    - (g) changement de mode de gouvernance avec éventuellement liste du bureau : voir plus avant les paramètres.
    - (n) sondage sur la note elle-même.
- `dhc` : date-heure de création (ouverture de la discussion à propos de ses paramètres éventuels).

**Propriétés :**
- `idx` : index de l'initiateur du sondage.
- `dho` : date-heure d'ouverture du sondage.
- `dhf` : date-heure de fermeture.
- `note` : note d'explication du sondage.
- `sv` : schéma de vote applicable.
- `st` : état du scrutin :
    - 0 : en cours.
    - 1 : clos par renoncement du demandeur.
    - 2 : clos sur approbation.
    - 3 : clos sur rejet.
- `votes` : vote de chaque participant : *pour contre blanc veto*.
- `rsdv` : résultat après le dernier vote. En général comme les votes de chaque participant peuvent changer jusqu'à la clôture du scrutin, ce résultat n'a de valeur qu'une fois la date-heure de fermeture du scrutin passée. Mais certains schémas de vote peuvent spécifier pour gagner du temps que dès qu'un résultat positif a été acquis, le scrutin peut être clos et la décision applicable.
- `param` : paramètre du sondage :
    - (c r) `nc` du participant.
    - (t) transfert de crédit `{c1:m1, c2:m2, f1:m3 ...}` à des comptes / forums.
    - (p) mise à la corbeille de notes obsolètes : `[n1, n2 ...]`
    - (g) changement de mode de gouvernance avec éventuellement liste du bureau :
    `{b:[...] p:"B...", c:..., a:..., g:...}`

Les règles de gouvernance en cours fixent le *schéma de vote* en fonction de l'objet du sondage, sauf pour une note (n) où il est libre et peut changer avant ouverture du vote.  
L'objet du sondage est fixé à sa création mais ses paramètres peuvent changer avant ouverture du vote (pendant la *campagne*).


##### `CPclc` : Opération *demande de clôture d'un compte*
Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : numéro du compte C.

Retour : 0 (clôture demandée), 1 (clôture déjà demandée).

##### `CPacc` : Opération *annulation de demande de clôture d'un compte*
Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : numéro du compte C.
- `st` : 0:lecture / écriture 1:lecture seule 2:bloqué
- `txtN` : texte de courtoisie pour C crypté par la clé N.
- `cn0` : clé N cryptée par la clé 0.
- `cnP` : clé N crypté par la clé publique de C.

Retour : 0 (annulation  de la demande faite), 1 (pas de demande en cours).

Avis `cpacc` :
- compte 0 : compte premier du compte C. Clé N cryptée par la clé 0.
- compte 1 : compte C. Clé N cryptée par la clé publique de C.
- `prop st` : valeur de `st`.


# TODO
Rappel des zones orange / rouge / zombie dans les comptes et forums.
