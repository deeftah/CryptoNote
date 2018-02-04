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
- `clé S` : clé AES de cryptage de la clé `0` du compte. BCrypt du string de la phrase secrète (complétée par un O, un BCRYPT a une longueur de 31, une clé AES de 32). **Ne sort jamais de la session**.
- `prB` : le BCRYPT du string réduit de la phrase secrète. `prB` est transmis au serveur pour identifier un compte et à la création pour vérifier qu'un autre compte n'a pas une phrase secrète voisine. 
- `prBD` : le SHA-256 de `prB` est enregistré dans la dossier du compte et permet au serveur de vérifier que le `prB` fourni par le terminal correspond bien à un compte.

La requête **non authentifiée** `Xauth` demande au serveur l'entête `EnC` et `c0S` (la clé 0 du compte cryptée par la clé S) du compte ayant pour propriétés `prBD` le SHA-256 de l'argument de la requête `prB` : 
- si le serveur trouve ce compte, cela prouve que le titulaire a bien saisi la bonne phrase secrète.
- si la session peut décrypter `c0S` par la clé S détenue dans sa mémoire :
    - elle conserve la clé 0 qui permet de décrypter toutes les autres et les données du compte (dont le nom suffixé du compte et le numéro du compte `nc`).
    - elle pourra effectuer des requêtes authentifiées sur ce compte en transmettant sur chacune (`nc->account` `prB->key`).
- si la session ne peut pas décrypter `c0S` par sa clé S, 
    - soit le compte a été créé par une session de logiciel non officiel.
    - soit la phrase secrète du compte a été changée par une session de logiciel non officiel.
    - soit elle-même s'exécute avec un logiciel non officiel.
    - *si le problème persiste*, le compte est définitivement corrompu et ses données inutilisables. Le titulaire du compte sera invité à se résilier lui-même pour cause de compte corrompu.

Cette requête sert également à enregistrer, en cas de succès, la date-heure de dernière visite du compte.

### Opérations sur un compte authentifié
Une requête peut être invoquée en spécifiant un compte *authentifié* en fournissant la preuve que la session terminale est bien contrôlée par le titulaire de ce compte en ayant fourni la phrase secrète.
- pour une opération de mise à jour ceci permet de se protéger contre des requêtes de pure nuisance qui transmettraient des données fausses ou mal cryptées en paramètre.
- pour une consultation / synchronisation, des données peuvent être retournées ou non selon l'état du compte correspondant vis à vis des données souhaitées.

La requête porte deux paramètres identifiant et authentifiant le compte de la requête :
- `account` : le numéro du compte.
- `key` : le `prB` (BCRYPT de la phrase secrète réduite du compte).

Dans le serveur le dossier du compte est obtenu depuis `account` : dans son singleton `EnD` la propriété `prBD` est censée être le SHA-256 de `prB` passé en `key` sur la requête (ce que l'opération vérifie).

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

L'opération de crédit verse tout le crédit d'une ligne authentifiée sur le cr du compte et détruit cette ligne.  
Le crédit est réduit :
- à chaque fin d'opération :
    - le coût de l'occupation du volume depuis le dernier calcul est calculé.
    - le nouveau niveau d'occupation du volume est calculé.
    - le crédit est réduit de ce  montant et de celui de l'opération (dont le volume d'entrée / sortie).
- périodiquement, par exemple toutes les semaines, pour calculer les coûts d'occupation d'espace en l'absence d'opérations. 

# Constantes
Ces données ont :
- une clé primaire.
- `dh` : une date-heure de création de la constante.
- `alias` : un alias éventuel (unique).
- `value` : une valeur qui reste constante depuis la création de la constante.

Une constante est créée, peut être purgée mais jamais mise à jour.

## Constante : `TPC` : ticket public d'un compte
Ce ticket a été généré par la session créatrice du compte et comporte les propriétés suivantes :
- `dh` : date-heure de génération.
- **alias** : `nomrBD` : SHA-256 du BCRYPT du nom réduit utilisé pour détecter et interdire l'usage de noms trop proches.
- `c1O` : clé 1 du compte cryptée par la clé 0. Elle crypte le nom du compte dans son ticket public et ceux des comptes certifiant son identité.
- `nom1` : nom crypté par sa clé 1.
- `pub` : clé RSA publique de cryptage.
- `verif` : clé RSA publique de vérification.
- `priv0` : clé privée RSA de décryptage cryptée par la clé 0.
- `sign0` : clé privée RSA de signature cryptée par la clé 0.

**La clé de la constante** est le SHA-256 des champs `nom1 pub verif` séparés par un espace et est le numéro de compte.  

Toute session disposant du numéro de compte peut en obtenir le ticket public qui permet :
- d'obtenir les clés de cryptage et de vérification de signature,
- de s'assurer de sa validité en recalculant le SHA-256 des champs `nom1 pub verif` et en le comparant au numéro de compte.
- s'il pense connaître le nom du compte, en obtenir confirmation (ou infirmation).
- d'obtenir le nom du compte s'il en possède la clé 1 du compte.

Après destruction du compte, la constante est inutile et détruite.

#### Clés privées d'accès d'un compte
Un compte `nc` donné peut avoir, successivement dans l'application, plusieurs clés privées, autant que de phrases secrètes successivement choisies par son titulaire.  
Cette clé est cryptée par la phrase secrète du compte : c'est elle qui permet au titulaire d'accéder aux clé privées, clé 1 (donc nom) du `TPC`.

## Constante : `TCD` : trace d'un compte détruit
La clé est le numéro de compte et n'a que trois propriétés destinées à éviter le réemploi dans l'instance d'un nom proche à celui d'un compte ayant existé dans l'instance.
- `dh` : date-heure de destruction.
- `dhc` : date-heure de création.
- **alias** : `nomrBD` : BCRYPT du nom réduit.

# Dossier `Compte`
La clé d'accès `docid` est le numéro de compte nc.

## Singleton : *Entête du compte* `EnC`
Ce singleton est déclaré à la création du compte et n'est modifié qu'à quelques rares occasions :
- mise à jour de la phrase secrète du compte : son ticket privé valide change.
- lors des changements d'état : *début d'activité, passages zombie / retour à l'état actif, destruction physique*.

**Propriétés :**
- `prBD` : propriété indexée. SHA-256 du BCRYPT de la phrase secrète réduite. Permet de ne pas tolérer que des phrases secrètes soient trop ressemblantes dans l'instance.
- `c0S` : clé 0 du compte cryptée par le SHA-256 du BCRYPT de la phrase secrète. Cette cl ouvre toutes les autres clé mères du compte présentes dans son `TPC`.
- `dha` : date-heure de début d'activité.
- `dhzm` : date-heure de passage en zombie posée par la modération de l'instance.
- `dhzc` : date-heure de passage en zombie posée par le compte lui-même.
- `dhx` : *propriété indexée* date-heure de destruction prévue (s = 2) : la plus proche résultante de `dhzc` `dhza` et `d0` quand le compte est en création (s == 0 / `d1` absente), NC jours ou NZ jours après ces dates.

Le statut courant s est calculé depuis les dates `dha dhzm dhzc`.
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

Etant signées dans leur globalité, les certificats ou liste de certifications sont garanties complètes ou complètement détruites par piratage.

La création d'un compte a signé des `Cid` `Crt` vides (mais bel et bien signées par A).

### Items *phrases de contact* `Phc`
Un compte A peut déclarer à un instant donné jusqu'à 4 phrases de contact permettant d'être contacté par un compte C n'ayant pas A dans ses cercles de connaissance. Chaque phrase est garantie unique.

> Une phrase réduite est elle-même garantie unique afin d'éviter de tomber par hasard (ou en utilisant un robot) lors de la déclaration d'une phrase sur une déjà enregistrée et d'en déduire qu'un compte peut être contacté par cette phrase.

**Clé** : index de 1 à 4.

**Propriétés :**
- `pcrBD` : *propriété indexée unique*. SHA-256 du BCRYPT de la phrase réduite pour bloquer les phrases trop proches d'une phrase déjà déclarée.
- `pc0` : phrase de contact cryptée par la clé 0 du compte.

### Items *contact du répertoire* `Rep`
A peut avoir C comme contact : `Rep(A)` a une entrée C : `cDansA`.  
C peut avoir A comme contact : `Rep(C)` a une entrée A.  `aDansC`.

Chaque entrée est créée et peut disparaître n'importe quand, l'autre continue sa vie.  
Les informations suivantes sont maintenues redondées (réciproquement) **uniquement quand les deux entrées existent** :
- `cDansA.moi` est égal à `aDansC.lui` et réciproquement.
- `aDansC.dhr` est la date-heure de résiliation de C. 

**Clé** : numéro de compte du contact C.

**Propriétés** :
- `dh` : date heure de la dernière opération.
- `cm0P` : clé M cryptée, soit par la clé 0 du compte A, soit par sa clé publique (initiée par C).
- `nomcM` : nom du contact C crypté par la clé M. Si absent **son nom n'est pas encore connu de A**.
- `c1CM` : clé 1 du compte C cryptée par la clé M. **Si absent son nom (donc son CI) n'est pas encore connu de A**.
- `dhi` : date-heure d'inscription du contact dans le répertoire.
- `dhr` : date-heure de résiliation du contact.
- `moi` : état synthétique du contact : `ncd`.
    - n : nom : 1:anonyme, 2:nommé, 3:C est certifié par le compte.
    - c : confiance : 0:pas confiance 1:confiance (une note est disponible).
    - d : 1:le compte demande à C de le certifier. Remis à 0 quand le contact certifie le compte, ou que le compte renonce à sa demande.
- `lui` : réplication de `aDansC.moi` (0 quand `aDansC` n'existe pas).
- `fav` : 1:contact favori (affiché en tête), 0:normal, 2:caché.
- `cvA` : identifiant de la note que A a communiqué à C pour se présenter (absent si A n'a pas confiance en C).
- `cvC ccvCM` : identifiant de la note (et sa clé de cryptage) de C pour se présenter à A (absent si C n'a pas confiance en A).

#### Gestion de la clé mutuelle M
**Une nouvelle clé mutuelle M n'est générée par A que quand aucune des deux entrées `aDansC` et `cDansA` n'existent** ce dont la session va s'assurer par l'opération `Cinfo` quand `cDansA` n'existe pas avant de proposer une nouvelle clé. 
- la clé M ne disparaît de facto que quand les deux entrées sont supprimées.
- tant que l'une des entrées existe la clé M reste la même.
- dans les notifications cette clé est toujours donnée puisque au cours du temps elle pourrait changer pour un couple A / C donné (cas d'une notification archivée longtemps).
- la clé M est stockée :
    - dans `cDansA` cryptée par la clé 0 de A dans `cmma0` et cryptée par la clé publique de C dans `cmsa0P`. Toutefois celle-ci va être remplacée à la première occasion par son cryptage par la clé 0 de C (plus courte et plus rapide).
    - dans `aDansC` cryptée par la clé 0 de C dans `cmma0` et cryptée par la clé publique de A dans `cmsa0P`. Toutefois celle-ci va être remplacée à la première occasion par son cryptage par la clé 0 de A (plus courte et plus rapide).
    - en régime établi `aDansC.cmma0` == `cDansA.cmsa0P` et `cDansA.cmma0` == `aDansC.cmsa0P`.

**A l'invocation d'une opération `Contact` (inscription ou mise à jour)**,
- *quand `cDansA` existe* la clé M y figure déjà : ni `cmma0` ni `cmsa0P` ne sont transmises puisque déjà présentes. Si `aDansC` existe, `cDansA.cmma0` est copiée dans `aDansC.cmsa0P` si celle-ci était plus longue (elle était encore cryptée par la clé publique).
- *quand `cDansA` n'existe pas mais que `aDansC` existe*, la clé M y figure, 
     - la session récupère la clé M par `Cinfo` dans `aDansC.cmsa0P` de l'autre entrée. Si elle est longue (encryptée par la clé publique de A), elle est décryptée et encryptée par la clé 0 de A.
     - la clé M étant désormais toujours disponible cryptée par la clé 0 de A elle est copiée dans `cDansA.cmma0` et `aDansC.cmsa0P`.
     - la clé M cryptée par la clé 0 de C dans `aDansC.cmma0` est copiée dans `cDansA.cmsa0P`.
- *quand aucune des entrées n'existent* la session génère une clé M et l'encrypte en `cmma0` (par sa clé 0) et en `cmsa0P` (par la clé publique de C). Le serveur vérifie qu'effectivement aucune des deux entrées n'existent. Les clés sont stockées dans `aDansC` et `cDansA` (`cmma0 / cmsa0P`). 

**La clé M est utilisée comme clé dans les notifications / conversations entre A et C** où elles sont explicitement stockées afin qu'une vieille notification soit toujours lisible. 

#### Dialogues `contact` associés
La gestion d'une entrée répertoire de A vis à vis de C et réciproquement peut concerner l'autre : toutes ces actions s'inscrivent dans un dialogue qui permet ainsi de suivre une succession d'échanges entre A et C (ou C et A selon qui a été initiateur de la conversation).  
Les opérations `Contact` et `ContactS` donne en paramètre un numéro de dialogue,
- soit *existant* où elles ajouteront un échange supplémentaire,
- soit *à créer* avec un premier échange.

Pour qu'un échange soit ajouté (et le cas échéant son dialogue support créé) il faut que,
- l'opération ait eu une action réelle intéressant l'autre,
- et/ou qu'elle ait un texte de message à communiquer.  

C'est la session qui détermine d'après son contexte si l'opération invoquée l'est dans le cadre d'un dialogue en cours ou à l'opposé à créer parce qu'elle concerne un nouveau sujet.

#### Relations de A vers C autorisées : comment A connaît C
- `bp` : (*) A connaît une adresse de boîte postale que C lui a communiquée dans la vraie vie.
- `cr` : A a inscrit C dans son répertoire et réciproquement C a inscrit A dans son répertoire.
- `mg` : A et C sont membres d'un même groupe G.
- `mm` : A et C participent au même mur M.N.
- `acpc` : A est le compte premier de C.
- `ccpa` : A a C comme compte premier.
- `acnc` : A a certifié le nom de C.
- `ccna` : A a son nom certifié par C.
- `ccpga` : C est le compte premier du groupe G dont A est membre.
- `acpgc` : (*) A est le compte premier d'un groupe G dont C est membre.
- `acnxcfc` : (*) A certifie le nom de X contact de confiance de C.
- `ccnxcfa` : C certifie le nom de X contact de confiance de A.
- `acnxmgc` : (*) A certifie le nom de X membre du même groupe G que C.
- `ccnxmga` : A est membre du même groupe G que X dont C certifie le nom.
- `acnxmmc` : (*) A certifie le nom de X participant au même mur M.N que C.
- `ccnxmma` : A participe au même mur M.N que X dont C certifie le nom.


### Items *CV personnel* `Cvp`
**Clé** : index de 1 à 4.

**Propriétés :**
- `l0` : libellé crypté par la clé 0 du compte.
- `c0` : clé de cryptage C de la version courante cryptée par la clé 0 du compte.
- `tC` : texte du CV crypté par la clé C.
- `pC` : texte de la photo cryptée par la clé C.

Quand une mise à jour intervient sur CV d'index `x` (texte et/ou photo), la session génère une nouvelle clé courante de cryptage C pour ce code. Elle recherche dans les `Fac Rep` les entrées référençant cet index `x` et y crypte C par la clé (F / M) de cette entrée dans `cvcM`.  
Elle crypte dans `tC/pC` la nouvelle version du CV / photo par la clé elle-même cryptée par la clé 0 du compte.

**Justification**  
Le regroupement dans `Fac Rep` permet de les remettre à jour en une transaction et au même niveau pour tous les forums et entrées du répertoire.  
De cette manière le forum quitté, ou le contact révoqué n'est plus en mesure (même sur une copie de base) de lire les *nouvelles versions* du CV auquel il avait accès antérieurement (faute d'avoir la clé de cryptage de la nouvelle version).

### Items *Forum accessibles* `Fac`
**Clé** : `nf`. Identifiant du forum.

**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `cf0P` : clé M du forum crypté par la clé 0/P du participant.
- `p` : participation du compte au mur.
- `tiM` : titre du mur crypté par sa clé M. Propagée depuis `Compte(nc).Mdc(im).tiM`.
- `stdcm` : statut daté du compte du mur. Propagé depuis `Compte(nc).EnC.std`.
- `stdm` : statut daté du mur lui-même. Propagé depuis `Compte(nc).Mdc(im).std`.
- `rpmu` : restriction d'accès au mur du compte. Redondance avec `Mur(nc,im).EnM.compte(c).rpmu`.
- `ra` : restriction d'accès calculée. `max(EnC.std.r, stdcm.r, stdm.r, rpmu.r)`.
- `nompM` : nom suffixé de propriétaire du mur crypté par la clé M. Redondance avec `Mur(nc,im).nompM`. 
- `memo0` : mémo spécifique du participant crypté par sa clé 0.
- `fav` : 0:normal, 1:favori, 2:caché 
- `cvn` : numéro du CV présenté aux autres participants de ce mur.
- `cvcM` : clé du CV `cvn` crypté par la clé M.
- `c1M` : clé 1 du compte cryptée par la clé M.

### Items *groupe dont le compte est membre* `Grp`
**Clé** : `ng` numéro du groupe.

**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `p` : participation du compte au groupe.
- `cg0P` : clé G du groupe crypté par la clé 0/P du participant.
- `c1g0P` : clé 1 du groupe crypté par la clé 0/P du participant.
- `ti1` : titre du groupe crypté par sa clé 1. Propagée depuis `Groupe(ng).ti1`.
- `stdg` : statut daté du groupe. Propagée depuis `Groupe(ng).EnG.std`.
- `rpg` : restriction de participation du compte `c` au groupe. Redondance avec `Groupe(ng).Cpt(c).rpg`.
- `ra` : restriction d'accès calculée. `max(EnC.std.r, stdg.r, rpg.r)`.
- `memo0` : mémo spécifique du membre crypté par sa clé 0.
- `fav` : 0:normal, 1:favori, 2:caché 
- `cvn` : numéro du CV présenté aux autres membres de ce groupe.
- `cvcG` : clé du CV `cvn` cryptée par la clé G.
- `c1G` : clé 1 du compte cryptée par la clé G.
- `bur` : 1: membre du bureau.
- `murs` : map des murs du groupe accessibles. Clé : `im` (indice du mur)
    - `p` : participation du compte au mur. 
    - `tiG` : titre du mur crypté par la clé G du groupe. Propagée depuis `Groupe(ng).Mdg(im).tiG`.
    - `stdm` : statut daté du mur. Propagée depuis `Groupe(ng).Mdg(im).std`.
    - `rpmu` : restriction de participation du compte c au mur du groupe. Redondance avec `Groupe(ng).Mdg(im).comptes(c).rpmu`.
    - `ra` : restriction d'accès calculée. `max(EnC.std.r, stdg.r, rpg.r, stdm.r, rpmu.r)`.

### Items *exemplaire d'une notification* `Ntf`
Clé des items : `type` + "." + `ntf` numéro de notification.

**Propriétés** :
- `lnc` : liste des numéros des comptes. Pour les exemplaires des comptes 1 à N d'un avis, liste réduite au premier terme.
- `rel` : relation entre le compte 0 et, a) le compte 1, b) tous les comptes destinataires.
- `mnomM` : map (clé: `idx`) des noms cryptés par la clé M de la notification.
- `mdhx` : map (clé: `idx`) des date-heures de raccrochage (dialogue seulement).
- `dhop` : date-heure de dernière opération.
- `dhd` : date-heure de début.
- `dhf` : date-heure de fin.
- `st` : 
    - 0 : en cours (jamais pour un avis).
    - négatif : terminé, statut du traitement (OK).
    - 99 : pour un dialogue : l'avant dernier interlocuteur a raccroché.
    - positif : terminé, statut du traitement (KO).
    - -99 : pour un dialogue : annulation par le demandeur.
    - -98 : pour un dialogue avec vote : annulation par défaut de consensus.
- `props` : map des propriétés globales nommées.
- `ng` : numéro du groupe cible.
- `nc` : numéro d'un compte cible.
- `im` : index du mur.
- `note` : identifiant de la note.
- `lech` : liste des échanges (pour un avis, il n'y a toujours qu'un terme) :
    - `idx` : index de l'auteur.
    - `dh` : date-heure de l'échange.
    - `tM` : texte crypté par la clé mutuelle.
    - `args` : *négociations / dialogues seulement* : map des propriétés nommées de l'échange.
    - `vote` : 0:abstention 1:pour 2:contre 3:blanc 4:veto (négociation seulement).
- **Pour un dialogue avec votes seulement** :
    - `sv` : schéma de vote.
    - `p0nb` : vrai si participant #0 n'est pas membre du bureau
    - `p1pr` : vrai si vote du participant #1 *pour* requis     
    - `p1nv` : vrai si participant #1 est non votant
    - `p2nv` : vrai si participant #2 est non votant
    - `dhdv` : date-heure de début de vote (dernier changement des paramètres).
    - `votes` : Nombres de votes *abstention pour contre blanc veto*.
    - `pour1` : vrai si #1 a voté pour (et que p1v == 2).
    - `dhdp` : date-heure de dernier dépouillement.
    - `dp` : résultat du dernier dépouillement : vrai si approbation.
    - `dphl` : vrai si le dernier dépouillement a été calculé après l'heure limite (était déjà valide)
- ***spécifique à chaque exemplaire :***
    - `idx` : indice de l'exemplaire.
    - `cm0P` : clé M cryptée par la clé 0 ou publique du compte.
    - `npd` : vrai *si ne pas détruire automatiquement*.
    - `dhl` : date-heure de lecture.

**Négociation**
- la clôture du vote avec action est automatique après tout échange (initial ou ajout) dont le dépouillement est positif et le délai de vote respecté.
- sinon la clôture de la négociation doit être demandée explicitement : 
  - si le vote est négatif : st = 98 et pas d'action.
  - si le vote est positif : action.
- l'annulation peut être déclenchée par le #0 (tant que st == 0).

**Avis**
- st est le statut d'exécution de l'envoi.

**Dialogue**
- st = 1 (clôture) résulte de l'action de raccrocher de l'avant dernier interlocuteur.


## Cycle de vie d'un compte
Il passe par les états principaux suivants :
- 0 : **en création**. 
- 1 : **actif**.
- 2 : **zombie**, destruction imminente (implique l'effet d'un r à 1).
- 3 : **destruction lancée / résilié** : considéré dès lors comme irrémédiablement **résilié**, 
- 4 : **destruction terminée / résilié**.

Les opérations sont autorisées selon,
- `std.s` : l'état principal entre une valeur minimale et maximale.
- `std.r` : la restriction d'accès : 0 1 2 3 (valeur maximale).

#### Alertes
Les alertes sont des messages courts qui peuvent être posés :
- **sur un compte** :
    - par l'administrateur de l'instance au compte premier d'un compte standard ou au compte lui-même s'il est premier. Il s'agit d'une lettre a-z correspondant à un des messages standard de l'instance.
    - pour un compte standard par son compte premier : le texte est crypté par la clé 1 du compte.
    - par le compte lui-même pour ceux qui accèdent à ses murs ou le voient comme co-participant d'un mur ou d'un groupe. Le texte est crypté par la clé 1 du compte.
- **sur un groupe** :
    - par l'administrateur de l'instance au compte premier du groupe. Il s'agit d'une lettre a-z correspondant à un des messages standard de l'instance.
    - par son compte premier : le texte est crypté par la clé 1 du groupe.
    - par un compte du bureau du groupe pour ceux qui accèdent à ses murs ou le voient comme co-participant au groupe. 
- **sur un mur** : par le compte propriétaire / du bureau du groupe du mur. Le texte est crypté par la clé 1 du groupe.

Une alerte est très utile :
- en cas de restriction d'activité pour en donner la justification.
- en cas de demande de destruction.

Les alertes se manifestent dans les écrans, a minima par une icône qui permet d'en avoir ensuite le ou les textes.

#### Création d'un compte premier  
Il faut obtenir préalablement de l'administrateur un **jeton** de création représenté par son numéro : celui-ci correspond à un quota défini par ses soins.
- la création demande un nom et une phrase secrète.
- la certification par un autre compte n'est pas requise mais la fourniture du **numéro de jeton** l'est.
- le compte est créé *actif* immédiatement et ne passe pas en état *en création*.

#### Création d'un compte standard
Il faut d'abord obtenir **l'adresse de boîte postale d'un compte certificateur** (premier ou non) qui distraira une part de son propre quota.
- la création demande un nom et une phrase secrète.
- un CV.
- l'adresse de boîte postale du compte certificateur pressenti.
- un message de courtoisie à son intention.
- le compte passe en état *en création* jusqu'à acceptation de la certification.

>**Comment bloquer les demandes de créations intempestives ?**  
>Pour effectuer une création il faut donner l'adresse de la boîte postale d'un compte certificateur pressenti : 
>- un robot pourrait créer des comptes en rafale vis à vis d'une adresse de boîte postale d'un certificateur obtenue on ne sait comment. Certes les comptes ne passeraient jamais actifs mais en attendant ceci bloquerait des noms et des phrases secrètes.
>- on pourrait limiter le nombre de premières demandes de certification en cours sur une même boîte postale avec N jetons (des codes PIN) : chaque demande de première certification devant fournir un code PIN (ou se faire rejeter).

>La première certification du nom d'un compte réduit le quota du certificateur pour en attribuer une partie au compte devenu ainsi actif ce qui limite une éventuelle tendance à certifier n'importe qui.

La création d'un compte à une date-heure `dhc` met ce dernier en état *en création* et sa `dhx` est fixée à `dhc`  + un délai de quelques jours. Sa destruction interviendra à `dhx`.
- **si le compte est certifié par un premier certificateur**, il devient *actif* :
    - `dha` est présente, `dhx` est effacée.
    - `cp` est le compte premier du certificateur et devient celui du compte. 
    - un quota est prélevé du quota du compte certificateur (qui l'a fixé) et attribué au compte certifié.
- **tant que le certificateur pressenti ne répond pas ou s'il refuse la certification,** le compte reste *en création*. Ses opérations autorisées sont limitées à :
    - mettre à jour son mémo, ajouter / modifier des CV et des adresses de boîte postales.
    - établir un contact avec un autre compte pressenti comme certificateur.
    - *se résilier lui-même*, ce qui est immédiat :
        - `dhd` `est marquée à la date-heure actuelle : plus aucune opération ne peut modifier le compte, même par effet de bord, à l'exception des seules actions de nettoyage de la tâche de clôture.
        - la tâche de clôture est lancée.

**Le nom du compte n'est pas bloqué par une clôture en état en création**.  
Le numéro de compte (BCRYPT de par exemple `John Doe@1234...`) a une large composante aléatoire qui empêche de facto d'être réutilisé, mais le nom (`John Doe`) ou les noms proches sont ré-employables (`nomRB` ayant disparu).   
Un autre compte A sollicité comme certificateur et ayant refusé, affichant dans une conversation un nom `John Doe` (acquis durant la brève vie du premier), verrait s'afficher en zoomant sur ce nom qu'il correspond à un compte *inconnu (résilié en phase de création)*.   
A pourrait aussi voir s'afficher une autre conversation avec le nom `John Doe` mais correspondant à un autre compte créé ultérieurement `John Doe@abcd...` et ayant dépassé l'étape *en création* (ou non d'ailleurs).

>**Est-ce un problème ?**  
>Est-il préférable de bloquer le nom `John Doe` (et les noms approchants) pour toujours alors que le compte n'a jamais été activé, le nom n'ayant été utilisé que très peu de temps et n'ayant pu être diffusé que dans quelques notifications aux seuls parrains sollicités et ayant refusé ?  
>Le risque inverse serait de voir des créations en rafale de comptes qui s’autodétruiraient très vite mais bloqueraient des noms utiles.  
>Les noms fantômes ne peuvent apparaître qu'à titre informatif dans quelques historiques de notifications et sur demande d'information supplémentaire seraient indiqués comme *inconnu (résilié en phase de création)*.

>Une fois passé la phase *en création* d'un compte, son nom ne peut pas être réemployé (ni un nom approchant) même après résiliation.

#### État *actif* d'un compte
Dès cette première certification acquise, le compte passe en état **actif** :
- `dha` est présente.
- son certificat d'identité contient une entrée : celle de son certificateur.
- le compte connaît son compte premier.
- les opérations sont par défaut toutes possibles, sauf dans le cas de *restriction d'accès*.
- **une demande de résiliation** peut être faite et met le compte en état **zombie**,
    - par le compte lui-même : `dhzcg` est fixée et si `dhx` ne l'est pas, elle y est fixée à `dhzcg` + le délai de remord.
    - par le compte premier pour un compte standard : `dhzcp` est fixée et si `dhx` ne l'est pas, elle y est fixée à `dhzcp` + le délai de remord.
    - par l'administrateur pour un compte premier : `dhzad` est fixée et si `dhx` ne l'est pas, elle y est fixée à `dhzad` + le délai de remord.

#### Restriction d'accès en état *en création* ou *actif*.  
Elles ne s'appliquent qu'à certaines (la plupart) des opérations authentifiées sur le compte.
- **Pour un compte premier** la restriction d'accès effective est la plus restrictive de celle de l'administrateur `EnC.rad` et de celle posée par le compte `EnC.r` à lui-même : `max(rad, r)`
- **Pour un compte standard** la restriction d'accès effective est la plus restrictive de celle de l'administrateur `EnC.rad`, de celle posée par le compte premier `EnC.rcp` et de celle posée par le compte `EnC.r` à lui-même : `max(rad, rcp, r)`

#### État *zombie* d'un compte
Opérations possibles : 
- **avant `dhx`** :
    - la restriction d'accès applicable est a minima 1 (lecture et destructions seulement), voire pire selon ra.
    - le compte et/ou le compte premier / administrateur peut exprimer un remord et annuler *sa* résiliation. `dhzad dhzcp dhzcg` sont mises à `null`, `dhx` étant remis à la plus récente de celles non `null`. Retour à l'état **actif** si `dhx` est `null`.
- **après `dhx`**, le lancement de la tâche de destruction peut intervenir à n'importe quel instant et dès lancement effectif de la tâche le compte a une `dhd` *date-heure de début de destruction* (la destruction est irréversible). Le compte passe en état **début de fermeture**

#### État *destruction lancée / résilié*
Aucune opération n'est possible, le compte n'est plus lisible (sauf son entête `EnC`). 
- `dhd` donne sa date-heure de passage à l'état *résilié*, c'est celle de début de la tâche de clôture.
- seule la tâche de nettoyage du compte peut exécuter des opérations : envoi de notifications, suppressions de participations aux murs et groupes, suppression de murs, etc.
- la fin de la tâche le fait passer en état *destruction terminée / résilié*.

#### État *destruction terminée / résilié*
Aucune opération n'est possible, le compte n'est plus lisible (sauf son entête `EnC`). 
- `dhf` donne sa date-heure de fin de la tâche de clôture.
- ses données sont limitées à son singleton `EnC` avec certaines propriétés effacées. Certaines permettent encore à un compte premier d'accéder pendant un certain temps au nom du compte résilié afin que ses avis / dialogues en archive reste lisibles avec un nom pendant quelque temps.
- ultérieurement les autres propriétés sont finalement effacées, sauf :
    - `nomRB` : nom réduit brouillé afin de bloquer la création ultérieur de comptes ayant un nom proche de celui qu'avait le compte résilié. Toutefois si le compte était en création `nomRB` est effacé.
    - `dhc dhd` : ses date-heure de création et de résiliation.

## Opérations

### Opérations de l'administrateur de l'instance
Ces opérations ne sont pas authentifiées au sens où un compte authentifié n'est pas cité mais requièrent que le paramètre `key` de la requête contiennent un mot de passe brouillé par BCRYPT dont le SHA-256 est enregistré dans la configuration de l'instance (dans `secret.json` sous la propriété `secretKey`).

#### `Ajeton` : création / mise à jour / suppression d'un jeton
Les critères selon lesquels les jetons sont distribués dépendent de chaque organisation. Un jeton existant est mis à jour.

Paramètres :
- `jeton` : numéro de jeton à usage unique communiqué au *compte premier* pour qu'il puisse créer son compte.
- `categ` : code de catégorie (code court textuel libre) au compte à créer, code qui permettra d'effectuer des filtrages sur des listes longues.
- `q1 q2` : quota attribué. si `q1` est 0, c'est une suppression du jeton.
- `val` : date de validité du quota (seulement indicative).
- `dhd` : date-heure de destruction / expiration du jeton.

#### `Alstj` :  liste des jetons en cours
Retourne la liste des items Jet existant.

#### `Alstcp` :  liste filtrée des comptes premiers
Retourne une liste d'items étendus de `Pre` répondant au critère passé en paramètre.
- en majeur par index une sélection basée sur la catégorie qui permet à l'administrateur de filtrer les populations de comptes premiers par un critère arborescent de classement ("/pays/région" par exemple).
- en mineur, sans index, une sélection selon la restriction d'accès afin de ne filtrer que les comptes premiers ayant une restriction donnée par l'administrateur.
- enfin un **ET** de 3 sélections sur quota / volume / pourcentage du volume par rapport au quota.
    - chaque sélection comporte 4 compteurs `v1min v1max v2min v2max` ;
    - la sélection répond vrai si par exemple le quota `q1` est compris entre `v1min` et `v1max` **OU** `q2` compris entre `v2min` et `v2max`.
    - ceci permet de filtrer par exemple,
       - les gros quotas très remplis,
       - les gros quotas peu remplis,
       - les gros volumes.

Paramètres:
- `categ` : tous les items dont le code de catégorie commence par ce paramètre (tous si absent ou vide).
- `radmin` : tous les items dont la restriction d'accès est a minima celle citée (0 1 2 3). Par défaut 0.
- `fq fv fp` : les trois filtres par quota / volume / pourcentage avec pour chacun quatre compteurs représentant `v1min v1max v2min v2max`.

**Pour les volumes comme pour les quotas l'unité est le Mo (1024*1024 octets)**.

#### `Amajcp` : mise à jour d'un compte premier (dont résiliation / réactivation)
Mise à jour pour un compte premier du quota, de sa validité indicative d'expiration, de sa catégorie et de sa restriction d'accès.
- résiliation : positionne la date-heure à laquelle la tâche de clôture du compte premier sera lancée. Le compte premier passe en *zombie* pour laisser le temps d'un remord. La tâche de résiliation d'un compte premier résilie aussi tous les comptes et dissout tous les groupes l'ayant pour compte premier.
- réactivation : le compte premier repasse de *zombie* en *actif*. Quand la tâche de clôture déjà planifiée se lancera, elle s'interrompra immédiatement au vu de l'état actif du compte.

Paramètres :
- `dhop` : date-heure de l'opération.
- `ncp` : numéro du compte premier.
- `q1 q2` : nouvelle valeur du quota. Si 0, inchangée.
- `categ` : catégorie. Si absente, inchangée.
- `val` : validité indicative. Si absente, inchangée.
- `r` : restriction d'accès de 0 à 3. Si -1, inchangée.
- `alad` : code d'alerte posé par l'administrateur au compte premier. `null`:inchangé, "":mettre à `null`.
- `z` : 0:inchangé 2:passer en zombie 1:redevenir actif

*Retour en cas de dissolution* : liste des numéros des comptes et des groupes qui seront résiliés / dissous, le numéro du compte premier figurant par convention en tête.

La mise à jour du quota n'a pas d'effet tangible immédiat, sauf de faire apparaître un changement visuel de sa valeur et des pourcentages des volumes par rapport au quota.  
La mise à jour de la restriction d'accès, de l'alerte et du statut zombie entraîne des propagations désynchronisées :
- Mise à jour de `dhzad rad alad`. 
- Recalcul de `std` et si `std` a changé **propagation** C(ncp).
- Si `dhzad rad alad` a changé **propagation** CG(ncp)


#### `Arescp` : résiliation d'un compte premier
Positionne la date-heure à laquelle la tâche de clôture du compte premier sera lancée. Le compte premier passe en *zombie* pour laisser le temps d'un remord.

Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : numéro du compte premier.

Retour : liste des numéros des comptes et des groupes qui seront résiliés / dissous, le numéro du compte premier figurant par convention en tête.

La tâche de résiliation d'un compte premier résilie aussi tous les comptes et dissout tous les groupes l'ayant pour compte premier.

#### `Aanrcp` : annulation de résiliation d'un compte premier
Le compte premier repasse de *zombie* en *actif*. La tâche de clôture ne s'effectuera pas (même si elle se lance, elle s'interrompra immédiatement).

Paramètres :
- `nc` : numéro du compte premier.

### Opérations non authentifiées
#### `Xetat` : Opération *état d'un compte*
*Consultation* permettant d'obtenir pour n'importe quel compte `nc` passé en paramètre, les propriétés `EnC`. Si aucun compte n'a été trouvé pour ce `nc` retourne `null`.

Paramètre :
- `nc` : numéro du compte.

Retourne : `EnC` du compte.

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

Avis `canc` (compte standard seulement) : clé mutuelle Q.
- compte 0 : compte C. Clé cryptée par la clé 0 de C.
- compte 1 : compte premier P du compte C. Clé cryptée par la clé publique de P.

### Opérations authentifiées
#### `Cfc` : Opération *fiche d'un compte*
*Consultation* la fiche d'un compte `nc` passé en paramètre. L'opération peut être demandée :
- par l'administrateur de l'instance.
- par un compte C authentifié.

Paramètre :
- `nc` : numéro du compte (qui ne doit être le compte authentifié lui-même).

**`FicCpt` : fiche d'un compte :**
- `enc` : singleton `EnC` du compte `nc`.
- `lstCV` : liste de cartes de visite de `nc` accessible à C. Map de clé #cvn :
    - `tC` : texte du CV crypté par la clé C.
    - `pC` : texte de la photo cryptée par la clé C.
    - `c1C` : clé 1 du compte cryptée par la clé C.
- `conf` : présent seulement si C est un contact de confiance de `nc` :
    - `cvn` : numéro de CV présenté par `nc` à C.
    - `cvcM` : clé du CV crypté par la clé M mutuelle C/`nc`.
    - `c1M` : clé 1 du compte cryptée par la clé M.
- `murs` : Map des murs partagés avec `nc`. Clé: `ncg` + "." + `im`. Valeur : 
    - `idx` : index de la participation au mur (mur de compte seulement).
    - `etc` : à détailler (std...)
    - `dhdv` : date-heure de dernière visite (date-heure du début de session la plus récente).
    - `cvn` : numéro de CV présenté aux participants au mur (mur de compte seulement).
    - `cvcM` : clé de ce CV crypté par la clé M du mur (mur de compte seulement).
    - `c1M` : clé 1 du compte `ncg` cryptée par la clé M (mur de compte seulement).
- `groupes` : Map des comptes partagés avec `nc`. Clé: `ng`. Valeur : 
    - `idx` : index d'enregistrement de la participation au groupe.
    - `etc` : à détailler (std...)
    - `bur` : 1:membre du bureau.
    - `dhdv` : date-heure de dernière visite (date-heure du début de session la plus récente).
    - `cvn` : numéro de CV présenté aux membres du groupe.
    - `cvcG` : clé de ce CV crypté par la clé G du groupe.
    - `c1G` : clé 1 du groupe `ncg` cryptée par la clé G du groupe.

#### `Cmemo` Opération de *mise à jour du mémo d'un compte*
Invoquée par le titulaire du compte.

Paramètres :
- `dhop` : date-heure de l'opération.
- `memo0` : mémo crypté par la clé 0 du compte.

Retour : 0 (mise à jour faite), 1 (déjà faite).

#### `Cps` : Opération *changement de phrase secrète*
Invoquée par le titulaire du compte.

Paramètres :
- `psRB` : BCRYPT de la nouvelle phrase secrète réduite.
- `psB` : BCRYPT de la nouvelle phrase secrète.
- `c0S` : clé 0 ré-encryptée par le SHA-256 de la nouvelle phrase secrète.

Retour : 0 (mise à jour faite), 1 (déjà faite).

#### `Cclo` : Opération *demande de clôture du compte*
Invoquée par le titulaire du compte. Le compte n'est plus opérable après cette opération.

Paramètres :
- `dhop` : date-heure de l'opération.

Retour : 0 (clôture demandée), 1 (clôture déjà demandée).

Avis `cclo` (compte standard seulement) : clé mutuelle Q.
- compte 0 : compte C. Clé cryptée par la clé 0 de C.
- compte 1 : compte premier P du compte C. Clé cryptée par la clé 0/P de GQ.

#### `Cdtc` : Opération *demande de transfert d'un compte à un GQ pressenti*
Cette opération ouvre une conversation avec le compte et le futur GQ pressenti.

Paramètres :
- `dhop` : date-heure de l'opération.
- `fgq` : numéro de compte du gestionnaire de quota pressenti FGQ.
- `rel` : relation de GQ à C.
- `abp` : adresse de boîte postale de FGQ si `rel` l'exige.
- `ntf` : numéro de négociation.
- `txtQ` : texte de courtoisie pour GQ et FGQ crypté par la clé Q.
- `cqP` : clé Q cryptée par la clé publique de FGQ.

Retour : 0 (demande faite), 1 (demande déjà faite).

Conversation `cdtc` : clé mutuelle Q.
- compte 0 : compte C (avec son nom). Clé Q cryptée par la clé 0 de C (`EnD.cq0`).
- compte 1 : compte FGQ (avec ou sans son nom). Clé Q cryptée par la clé P de FGQ (`cqP`).
- compte 2 : gestionnaire de quota actuel GQ du compte C. Clé cryptée par 0 du GQ (`cq0`).

Le numéro de FGQ est enregistré dans le numéro du gestionnaire de quota pressenti ainsi que le numéro de cette conversation.

##### `Cdtc` : Opération *annulation de demande de transfert d'un compte à un GQ pressenti*
Cette opération poursuit une conversation avec le compte et le futur GQ pressenti.

Paramètres :
- `dhop` : date-heure de l'opération.
- `ntf` : numéro de conversation.
- `txtQ` : texte de courtoisie pour GQ et FGQ crypté par la clé Q.

Le numéro de FGQ est effacé dans le numéro du gestionnaire de quota pressenti ainsi que le numéro de cette conversation.

##### `Cdaqc` : Opération *demande d'augmentation du quota d'un compte*
Cette opération ouvre une conversation avec le gestionnaire de quota et enregistre comme propriétés de celle-ci le quota `q1 q2` souhaité.

Paramètres :
- `dhop` : date-heure de l'opération.
- `q1 q2` : quota souhaité.
- `ntf` : numéro de conversation.
- `txtQ` : texte de courtoisie pour C crypté par la clé Q.
- `cq0` : clé Q cryptée par la clé 0 de GQ.

Retour : 0 (mise à jour faite), 1 (déjà faite).

Négociation `cdaqc` : clé mutuelle Q.
- compte 0 : gestionnaire de quota du compte C. Clé Q cryptée par 0/P du GQ.
- compte 1 : compte C (avec son nom). Clé Q cryptée par la clé 0 de C (`EnD.cq0`).
- propriétés : `q1` et `q2`.

##### `Crqc` : Opération *réduction du quota d'un compte*
Cette opération réduit le quota du compte et en avise le gestionnaire de quota en enregistrant comme propriétés de celui-ci le quota `q1 q2` réduit.

Paramètres :
- `dhop` : date-heure de l'opération.
- `q1 q2` : quota réduit.
- `ntf` : numéro de l'avis.
- `txtQ` : texte de courtoisie pour C crypté par la clé Q.
- `cq0` : clé Q cryptée par la clé 0 de GQ.

Retour : 0 (mise à jour faite), 1 (déjà faite).

Avis `crqc` : clé mutuelle Q.
- compte 0 : gestionnaire de quota du compte C. Clé Q cryptée par 0/P du GQ.
- compte 1 : compte C (avec son nom). Clé Q cryptée par la clé 0 de C (`EnD.cq0`).
- propriétés : `q1` et `q2`.

# Dossier `Mur`
La clé `groupid / docid` d'un dossier `Mur`  :
- `groupid` : hachage Java du numéro de compte `nc` ou du groupe `ng`.
- `docid` : numéro du compte ou du groupe `ncg` + "." + `im` index de création du mur dans le compte ou groupe.

### Singleton *entête du mur de compte* `EmM` étend `ECGM`
**N'existe QUE pour un mur de compte seulement** : pour un mur de groupe ces données sont dans son groupe dans `Mdg(im)`.

**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- **Propriétés redondées avec `Compte(ncg).Mdc(im)`**
- `v1 v2` : volumes du mur.
- `tiM` : titre du mur crypté par sa clé M. C'est la clé M et non la clé 1 du compte, le compte premier n'a pas à lire les noms des murs des comptes qui l'ont pour compte premier.
- `memoM` : mémo du mur crypté par sa clé M. La première ligne donne `tiM`.
- `nompM` : nom suffixé de propriétaire du mur crypté par la clé M.
- `stdcm` : statut daté du *compte* du mur. Propagée depuis `Compte(ncg).EnC.std`.
- `dv` : liste des N dernières visites (tous participants confondus).
    - i : index du participant.
    - t : date-heure.
- `comptes` : map des participants au mur. Clé : `nc` :
    - `nomM` : nom du participant crypté par la clé M.
    - `p` : participation du compte au mur. 
    - `stdc` : statut daté du compte. Propagée depuis `Compte(nc).EnC.std`.
    - `rpmu` : restriction d'accès au mur du compte. Redondance avec `Compte(nc).Mca(ncg,im).rpmu`.
    - `ra` : restriction d'accès applicable calculée. `max(stdc.r, stdcm.r + std.r + rpmu.r)`

### Item *note* `Not`
**Clé** : identifiant tiré au hasard à la création de la note.

**Propriétés :**
- `lhtM` : liste des hashtags (cryptés par la clé M/G du mur / groupe) contenus dans le texte de la note.
- `red` : liste (pile chronologique) des index des rédacteurs successifs.
- `ecr` : liste des index des rédacteurs autorisés.
- `dhc` : date heure de création.
- `prop` : index du propriétaire.
- `dhm` : date heure de dernière modification.
- `t1` : taille du texte de la note.
- `t2` : taille de la pièce jointe.
- `mime` : type mime de la pièce jointe (redondance avec l'item P correspondant).
- `votes` : liste de 4 compteurs des votes *pour contre blanc veto*.
- `dho` : date-heure d'ouverture du sondage.
- `txtM` : texte de la note crypté par la clé M/G du mur / groupe.

### Item *image d'une note* `Ino`
**Clé** : identifiant la note.

L'item est la miniature de la pièce jointe de la note quand c'est une image (ou un clip ou toute autre image définie spécifiquement) cryptée par la clé M/G du mur / groupe.

### Item *commentaire d'une note* `Cno`
**Clé** : identifiant la note + "." + index de l'auteur.

**Propriétés :**
- `dh` : date-heure de dernière modification.
- `txtM` : texte du commentaire crypté par la clé M/G du mur / groupe.
- `vote` : 0:abstention 1:pour 2:contre 3:blanc 4:veto.

# Dossier `Groupe`
La clé `groupid / docid` d'un dossier `Groupe`  :
- `groupid` : hachage Java du numéro de groupe `ng`.
- `docid` : numéro de groupe `ng`.

### Singleton *entête du groupe* `EnG` étend `ECG`
**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `memoG` : mémo du groupe crypté par sa clé G. La première ligne cryptée par la clé 1 donne `ti1`.
- `ra` : restriction d'accès effective  : `std.r`.
- `nbp` : nombre de participants déjà déclarés.
- `nbs` : nombre de scrutins de changement de gouvernance déjà organisés.
- `nbm` : nombre de murs du compte déjà créés.
- `dv` : liste des N dernières visites (tous participants confondus).
    - i : index du participant.
    - t : date-heure.
- `ncpntf` : numéro de la négociation d'invitation du futur compte premier pressenti (pour qu'il n'y en ait pas plus d'une en cours). Remis à `null` après acceptation, refus ou abandon.
- `aqntf` : numéro de la négociation de demande d'augmentation de quota en cours. (pour qu'il n'y en ait pas plus d'une en cours). Remis à `null` après acceptation, refus ou abandon.
- `basegv` : nom du schéma de gouvernance de base.
- `exgv` : map des types de dialogue dérogeant au schéma de vote donné par la gouvernance par défaut. Valeur : schéma de vote.

**Schéma de vote** :
- `p` : pourcentage requis (défaut 30).
- `m` : minimum de votes pour requis (défaut 1).
- `v` : vrai si veto pris en compte (défaut faux).
- `i` : vrai si décompte basé sur les inscrits et non les votants (défaut faux).
- `h` : nombre d'heures minimal d'ouverture (défaut 0).


### Singleton *quota et volume du groupe* `Qvl` 
Identique à `Qvl` de Compte, ce singleton totalise le volume consommé et le quota du groupe ainsi que le volume qui a été remonté au compte premier.

### Items *mur du groupe* `Mdg` étend `ECGM`
Un item par mur existant.

**Clé** : `im` index du mur dans le groupe.

**Propriétés :**
- `dhop` : date-heure de la dernière opération.
- `v1 v2` : volumes du mur.
- `tiG` : titre du mur crypté par la clé G du groupe. C'est la clé G et non la clé 1 du groupe, le compte premier n'a pas à lire les noms des murs des groupes qui l'ont pour compte premier.
- `memoG` : mémo du mur crypté par la clé G du groupe.
- `ra` : restriction d'accès effective calculée. `max(EnG.std.r, std.r)`.
- `dv` : liste des N dernières visites (tous participants confondus).
    - i : index du participant.
    - t : date-heure.
- `comptes` : map des participants au mur. Clé : `nc` :
    - `p` : participation au mur.
    - `rpmu` : restriction de participation au mur du groupe. Redondance avec `Compte(nc).Grp(g).Mur(im).rpmu`.
    - `ra` : restriction d'accès effective calculée. `max(Cpt(nc).stdc.r, EnG.std.r, Cpt(nc).rpg.r, std.r, rpmu.r)`.
    - `dhdv` : date-heure de dernière visite à ce mur du groupe.

Un mur de groupe peut être clos et conservé, temporairement ou non, en historique.

>La liste des participants à un mur de groupe est dans le dossier du groupe et non dans le mur, tout accès à un mur du groupe requérant d'abord d'avoir accédé au groupe.

### Items *compte membre du groupe* `Cpt`
**Clé** : `nc` numéro du compte.

**Propriétés :**
- `p` : participation du compte au groupe.
- `nomG` : nom du participant crypté par la clé G.
- `stdc` : statut daté du *compte*. Propagée depuis `Compte(nc).EnC.std`).
- `rpg` : restriction d'accès au groupe spécifique à ce participant. Redondance avec `Compte(nc).Grp(g).rpg`.
- `ra` : restriction d'accès effective calculée. `max(stdc.r, EnG.std.r, rpg.r)`.
- `bur` : 1:membre du bureau.
- `dv` : dernières visites au groupe.

### Items *scrutin de gouvernance du groupe* `Sgg`
**Clé** : numéro du scrutin.

**Propriétés :**
- `dem` : index du demandeur.
- `txtG` : texte d'argumentaire crypté par la clé G.
- `dho` : date-heure d'ouverture du scrutin.
- `dhc` : date-heure de clôture du scrutin (absente s'il est ouvert).
- `bureau` : liste des index des membres du nouveau bureau proposé (absent si inchangé).
- `votants` : liste des index des votants, membres du groupe à l'ouverture du scrutin.
- `basegv` : nom du schéma de gouvernance de base. `null` si inchangé.
- `exgv` : map des types de dialogue dérogeant au schéma de vote donné par la gouvernance par défaut. Valeur : schéma de vote.
- `sv` : schéma de vote applicable.
- `st` : état du scrutin :
    - 0 : en cours.
    - 1 : clos par renoncement du demandeur.
    - 2 : clos sur approbation.
    - 3 : clos sur rejet.
- `votes` : liste des compteurs des votes *pour contre blanc veto*.
- `dhdp` : date-heure du dernier dépouillement
- `dp` : résultat du dernier dépouillement
- `dphl` : vrai si le dernier dépouillement a été calculé après l'heure limite (est valide)

Une gouvernance est une map avec :
- pour clé : le code d'une action qui ne suit pas le schéma de vote par défaut,
- pour valeur : le schéma de vote applicable.

### Item *Vote sur un scrutin* `Vot`
**Clé** : numéro du scrutin + "." + index de l'auteur.

**Propriétés :**
- `dh` : date-heure du vote.
- `txtG` : texte du commentaire crypté par la clé G du groupe.
- `vote` : 0:abstention 1:pour 2:contre 3:blanc 4:veto.

### Opérations spécifiques aux comptes premiers

##### `CPqi` : Opération *mise à jour du quota global distribuable*
Paramètres :
- `qd1 qd2` : nouveau quota distribuable indicatif.

Retour : 0 (mise à jour faite), 1 (mise à jour déjà faite).

##### `CPrac` : Opération *restriction d'accès à un compte*
Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : numéro du compte C.
- `st` : 1:lecture / écriture 2:lecture seule 3:bloqué
- `txtN` : texte de courtoisie pour C crypté par la clé N.
- `cn0` : clé N cryptée par la clé 0.
- `cnP` : clé N crypté par la clé publique de C.

Retour : 0 (mise à jour faite), 1 (déjà faite).

Avis `cprac` :
- compte 0 : compte premier du compte C. Clé N cryptée par la clé 0.
- compte 1 : compte C. Clé N cryptée par la clé publique de C.
- `prop st` : valeur de `st`.

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

##### `CPcqc` : Opération *attribution / refus du quota d'un compte*
Cette opération peut avoir lieu dans le cadre d'une négociation `cdaqc` ouverte par le compte demandant une augmentation de quota : cette négociation est utilisée pour notifier du quota effectivement attribué (propriétés `q1 q2`) ou du refus.

Mais l'opération peut aussi avoir été lancée sur initiative du compte premier : un avis `cpcqc` d'opération est envoyé pour notifier du quota effectivement attribué (propriétés `q1 q2`).

Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : numéro du compte.
- `q1 q2` : quota alloué. (-1 si refus de changement dans le cas d'une négociation).
- `ntf` : numéro d'avis / négociation.
- `txtN` : texte de courtoisie pour C crypté par la clé N.
- `cn0` : clé N cryptée par la clé 0.
- `cnP` : clé N crypté par la clé publique de C.

Retour : 0 (mise à jour faite), 1 (déjà faite), -3 (refus).

Avis `cpcqc` :
- compte 0 : compte premier du compte C. Clé N cryptée par sa clé 0.
- compte 1 : compte C (avec son nom). Clé N cryptée par la clé publique de C.
- propriétés `q1 q2`

Négociation `cdaqc` :
- compte 1 : compte premier du compte C. Clé N cryptée par sa clé 0.
- st peut être 0 (OK) ou -3 (refus en phase 2).

##### `CPdtc` : Opération *demande de transfert d'un compte à un autre compte premier pressenti*
Cette opération ouvre une négociation avec le compte C et le futur compte premier pressenti FCP.

Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : numéro du compte C.
- `fcp` : numéro de compte du gestionnaire de quota pressenti FGQ.
- `rel` : relation du compte à FCP.
- `abp` : adresse de boîte postale de FCP si `rel` l'exige.
- `ntf` : numéro de négociation.
- `txtN` : texte de courtoisie pour C crypté par la clé N.
- `cn0` : clé N cryptée par la clé 0.
- `cncP` : clé N crypté par la clé publique de C.
- `cnfcpP` : clé N crypté par la clé publique de FCP.

Retour : 0 (demande faite), 1 (demande déjà faite).

Négociation `cpdtc` :
- compte 0 : compte premier du compte C. Clé N cryptée par sa clé 0.
- compte 1 : compte FCP (avec ou sans son nom). Clé N cryptée par la clé publique de FCP. FCP devra donner son nom s'il n'y est pas déjà.
- compte 2 : compte C (avec son nom). Clé N cryptée par la clé publique de C.

Le numéro de compte FCP est enregistré (dans C) dans le numéro du futur compte premier ainsi que le numéro de cette négociation.

##### `cpadtc` : Opération *annulation de la demande de transfert d'un compte à un autre compte premier pressenti*
Cette opération poursuit une négociation `cpdtc` avec le compte et le futur compte premier pressenti.

Paramètres :
- `dhop` : date-heure de l'opération.
- `ntf` : numéro de négociation.
- `txtQ` : texte de courtoisie pour C et FGQ crypté par la clé Q.

Retour : 0 (demande faite), 1 (demande déjà faite).

La négociation `cpdtc` est poursuivie. `st` est à -3 (annulation en phase 2).

Le numéro de FCP est effacé (dans C) dans le numéro du futur compte premier pressenti ainsi que le numéro de cette négociation (`null` -> `Gvc(C).fcp et fcpntf`.

##### `GQatc` : Opération *acceptation / refus de transfert d'un compte par le futur compte premier*
Cette opération invoquée par FCP poursuit une conversation  `cpdtc` ou `cdtc` avec le compte et le compte premier pressenti qui a été ouverte, soit par l'actuel compte premier, soit par le compte C.

Paramètres :
- `dhop` : date-heure de l'opération.
- `nomN` : nom de FCP crypté par la clé N.
- `ar` : 0:acceptation 1:refus.
- `ntf` : numéro de négociation.
- `txtN` : texte de courtoisie pour C crypté par la clé N.
- `cn0` : clé N cryptée par la clé 0 de FCP.
- `nomN` : nom de FCP crypté par la clé N.
- `nom0PCP` : nom du compte crypté par la clé 0 de FCP.
- `nomcp0PC` : nom de FCP crypté par la clé publique de C.

Retour : 0 (acceptation/refus fait), 1 (acceptation/refus déjà fait).

Négociation poursuivie `gqdtc` ou `cdtc` :
- pour le compte 1 (FCP), le nom est donné (`nomN`) et la clé N est redonnée cryptée par la clé 0 de FCP (`cn0`).
- `st` est à 0 (acceptation) ou -2 (refus).

En cas d'acceptation,
- `nom0PCP` -> `Gvc(C).nom0PCP` : nom du compte crypté par la clé 0 (ou publique) du compte premier, qui pourra y lire le nom de C crypté par sa clé 0.
- `nomcp0PC` -> `Gvc(C).nomcp0PC` : nom du compte premier crypté par la clé (0 ou) publique du compte.
- numéro du compte FCP -> `Gvc(C).cp` et `null` ->`fcp` et `fcpntf`.
- le quota et volume de C sont retranchés de l'actuel CP et ajouté dans le nouveau FCP.

##### `GQrag` : Opération *restriction d'accès à un groupe*
Invoquée par le gestionnaire de quota GQ.  

Paramètres :
- `dhop` : date-heure de l'opération.
- `ng` : numéro du groupe G.
- `st` : 0:lecture / écriture 1:lecture seule 2:bloqué
- `txtQ` : texte de courtoisie pour les membres du bureau de G crypté par la clé Q.
- `cq0` : clé Q cryptée par la clé 0 de GQ.

Retour : 0 (mise à jour faite), 1 (déjà faite).

Avis `crac` : clé mutuelle Q.
- compte 0 : gestionnaire de quota du groupe G. Clé cryptée par la clé 0 de GQ (`cq0`).
- compte 1-N : membres du bureau de G. Clé cryptée par la clé G du groupe (`EnG.cq0`).
- `prop st` : valeur de `st`.

##### `GQmg` : Opération *mise à jour des mémos du GQ relatifs à un groupe*
Invoquée par le gestionnaire de quota GQ.  

Paramètres :
- `dhop` : date-heure de l'opération.
- `ng` : numéro du groupe.
- `memo0` : mémo du GQ pour lui-même crypté par la clé 0 du GQ. `null`:inchangé "":effacé.
- `memogQ` : mémo du GQ pour le groupe crypté par la clé G mutuelle GQ / groupe.  `null`:inchangé "":effacé.
- `txtQ` : texte de courtoisie pour les membres du bureau de G crypté par la clé Q.
- `cq0` : clé Q cryptée par la clé 0 de GQ.

Retour : 0 (mise à jour faite), 1 (déjà faite).

Avis `crac` : clé mutuelle Q. L'avis n'est envoyé que si `memogQ` change.
- compte 0 : gestionnaire de quota du compte C. Clé crypté par la clé 0 de GQ (`cq0`). 
- compte 1-N : compte des membres du bureau du groupe. Clé cryptée par la clé G du groupe (`EnG.cq0`).

##### `GQclg` : Opération *demande de clôture d'un groupe*

##### `GQacg` : Opération *annulation de demande de clôture d'un groupe*

##### `GQqg` : Opération *mise à jour du quota d'un groupe*
Cette opération peut avoir lieu dans le cadre d'une conversation ouverte par le groupe demandant une augmentation de quota : dans ce cas cette conversation est utilisée pour notifier du quota effectivement attribué.

Mais l'opération peut aussi avoir été lancée sur initiative du GQ : dans ce cas c'est un avis d'opération qui est envoyé.

Paramètres :
- `dhop` : date-heure de l'opération.
- `ng` : numéro du groupe.
- `q1 q2` : quota alloué.
- `ntf` : numéro d'avis / conversation.
- `txtQ` : texte de courtoisie pour les membres du bureau de G crypté par la clé Q.
- `cq0` : clé Q cryptée par la clé 0 de GQ.

Retour : 0 (mise à jour faite), 1 (déjà faite).

Avis `chqg` : clé mutuelle Q.
- compte 0 : gestionnaire de quota du compte C. Clé crypté par la clé 0 de GQ (`cq0`). 
- compte 1-N : membres du bureau du groupe. Clé cryptée par la clé G du groupe (`EnG.cq0`).

##### `GQdtg` : Opération *demande de transfert d'un groupe à un GQ pressenti*

##### `GQatg` : Opération *acceptation / refus de transfert d'un groupe par le futur gestionnaire*


## Propagations des statuts datés et noms / titres

**(C) Propagation de `std` d'un compte `c` :**
- Sur tous les murs im de Compte(c).Mdc(im) : (murs du compte)
    - dans Mur(c,im).stdc
    - Sur tous les comptes nc de Mur(c,im).comptes(nc) : (comptes participants à ces murs)
        - dans Compte(nc).Mca(c,im).stdcm
- Sur tous les groupes g de Compte(c).Grp(g) : (groupe dont c est membre)
    - dans Groupe(g).Cpt(c).std 

**(CG) Propagation de `dhzad` `rad` `alad` d'un compte `cp` :**
-Pour tous les comptes `c` dont `cp` est premier : `Compte(c).EnC`
    - Mise à jour de `dhzad rad alad`
    - Recalcul  de `std` et si `std` a changé **propagation** C(c).
- Pour tous les groupes `g` dont `cp` est premier : `Groupe(g).EnG`
    - Mise à jour de `dhzad rad alad`
    - Recalcul de `std` et si `std` a changé **propagation** G(g).

**(G) Propagation de `std` d'un groupe `g` :**
- Sur tous les comptes c de Groupe(g).Cpt(c) :
    - dans Compte(c).Grp(g).stdg

**(MC) Propagation de `std` d'un mur `c,im` :**
- Sur tous les comptes `nc` accédant au mur : `Mur(c,im).EmM.comptes(nc)`
    - dans Compte(nc).Mca(c,im).stdm

**(MG) Propagation de `std` d'un mur `g,im` :**
- Sur tous les comptes `nc` accédant au mur : `Groupe(g).Mdg(im).comptes(nc)`
    - dans Compte(nc).Mca(c,im).stdm

**Amajcp : Mise à jour d'un compte premier `cp` par l'administrateur**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `ncp` : le compte premier cible
- `r` : restriction d'accès éventuelle (0-3)
- `z` : 0:inchangé 1:passer en zombie -1:redevenir actif
- `a` : code d'alerte prédéfini éventuel  ("" : aucune alerte)

Actions: 
- `Compte(cp).EnC` 
    - Mise à jour de `dhzad rad alad`. 
    - Recalcul de `std` et si `std` a changé **propagation** C(ncp).
    - Si `dhzad rad alad` a changé **propagation** CG(ncp)

**Mise à jour d'un compte premier `cp` par lui-même**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `r` : restriction d'accès éventuelle (0-3)
- `z` : vrai si destruction imminente
- `a` : message d'alerte éventuel crypté par sa clé 1

Actions: 
- `Compte(cp).EnC` 
    - Mise à jour de `dhz rcg al`. 
    - Recalcul de `std` et **propagation** de C(cp).
- Pour tous les comptes `c` dont `cp` est premier : dans `Compte(c).EnC` :
    - Mise à jour de `dhzcp rcp alcp1`
    - Recalcul de `std` et **propagation** C(c).

**Mise à jour par un compte premier `cp` d'un compte `nc` ayant `cp` pour compte premier**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : compte à mettre à jour.
- `r` : restriction d'accès éventuelle (0-3)
- `z` : vrai si destruction imminente
- `a` : message d'alerte éventuel crypté par la clé 1 de `nc`

Actions: 
- `Compte(nc).EnC` :
    - Mise à jour de `dhzcp rcp alcp1`
    - Recalcul de `std` et **propagation** C(nc).

**Mise à jour d'un compte `c` par lui-même**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `r` : restriction d'accès éventuelle (0-3)
- `z` : vrai si destruction imminente
- `a` : message d'alerte éventuel crypté par sa clé 1

Actions: 
- `Compte(c).EnC` :
    - Mise à jour de `dhzcg r al`
    - Recalcul de `std` et **propagation** C(c).

**Mise à jour par un compte premier `cp` d'un groupe `g` ayant `cp` pour compte premier**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `g` : groupe ayant cp pour compte premier.
- `r` : restriction d'accès éventuelle (0-3).
- `z` : vrai si destruction imminente.
- `a` : message d'alerte éventuel crypté par la clé 1 de g.

Actions: 
- `Groupe(g).EnG` :
    - Mise à jour de `dhzcp rcp alcp1`
    - Recalcul de `std` et **propagation** G(g).

**Mise à jour d'un groupe `g` par lui-même**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `r` : restriction d'accès éventuelle (0-3).
- `z` : vrai si destruction imminente.
- `a` : message d'alerte éventuel crypté par sa clé 1.

Actions: 
- `Groupe(g).EnG` :
    - Mise à jour de `dhzcg r al`
    - Recalcul de `std` et **propagation** G(g).

**Mise à jour d'un mur `im` par son compte `c`**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `im` : index du mur dans le compte `c`.
- `r` : restriction d'accès éventuelle (0-3).
- `z` : vrai si destruction imminente.
- `a` : message d'alerte éventuel crypté par la clé M du mur.

Actions: 
- `Compte(c).Mdc(im)` :
    - Mise à jour de `dhz r al`
    - Recalcul de `std` et **propagation** MC(c,im).

**Mise à jour d'un mur `im` par son compte `g`**  
Paramètres :
- `dhop` : date-heure de l'opération.
- `im` : index du mur dans le compte `c`.
- `r` : restriction d'accès éventuelle (0-3).
- `z` : vrai si destruction imminente.
- `a` : message d'alerte éventuel crypté par la clé G du groupe.

Actions: 
- `Groupe(g).Mdc(im)` :
    - Mise à jour de `dhz r al`
    - Recalcul de `std` et **propagation** MG(g,im).

**Restriction d'accès à un groupe `g` pour le compte `c`**
Paramètres :
- `dhop` : date-heure de l'opération.
- `g` : groupe.
- `r` : restriction d'accès éventuelle (0-3).

Actions : dans Groupe(g)
- r -> Groupe(g).Cpt(c).rpg
- r -> Compte(c).Grp(g).rpg

**Restriction d'accès à un mur `im` d'un groupe `g` pour le compte `c`**
Paramètres :
- `dhop` : date-heure de l'opération.
- `g` : groupe.
- `im` : index du mur dans le groupe `g`.
- `r` : restriction d'accès éventuelle (0-3).

Actions : dans Groupe(g)
- r -> Groupe(g).Mur(im).comptes(c).rpmu
- r -> Compte(c).Grp(g).Mur(im).rpmu

**Restriction d'accès à un mur `im` d'un compte `ncg` pour le compte `c`**
Paramètres :
- `dhop` : date-heure de l'opération.
- `nc` : groupe.
- `im` : index du mur dans le groupe `g`.
- `r` : restriction d'accès éventuelle (0-3).

Actions : dans Groupe(g)
- r -> Mur(ncg,im).comptes(c).rpmu
- r -> Compte(c).Mca(ncg,im).rpmu

**Mise à jour du titre d'un mur `im` du compte `c`** 
Paramètres :
- `dhop` : date-heure de l'opération.
- `im` : index du mur dans le compte `c`.
- `t` : titre du mur crypté par sa clé M.

Actions :
- t -> Compte(c).Mdc(im).tiM
- t -> Mur(c,im).Emc.tiM
- **Propagation** sur tous les comptes nc participants au mur Mur(c,im).Emc.comptes(nc)
    - t  -> Compte(nc).Mca(c,im).tiM

**Mise à jour du titre d'un mur `im` du groupe `g`** 
Paramètres :
- `dhop` : date-heure de l'opération.
- `g` : groupe.
- `im` : index du mur dans le groupe `g`.
- `t` : titre du mur crypté par la clé G du groupe.

Actions :
- t -> Groupe(g).Mur(im).tiG
- **Propagation** sur tous les comptes nc participants au mur Groupe(g).Mur(im).comptes(nc)
    - t  -> Compte(nc).Mca(g,im).tiG

**Mise à jour du titre d'un groupe `g`** 
Paramètres :
- `dhop` : date-heure de l'opération.
- `g` : groupe.
- `t` : titre du groupe crypté par la clé 1 du groupe.

Actions :
- t -> Groupe(g).ti1
- **Propagation** sur tous les comptes nc du groupe Groupe(g).Cpt(nc)
    - t  -> Compte(nc).Grp(g).ti1

#### Destructions effectives mur / groupe / compte
En plus de la propagation de std, propager pour un compte le statut *résilié* sur :
- les entrées de répertoire (réciproques seulement, ou mettre un index ?),

Purges des entêtes : bien plus tard (une fois par an ...) ou jamais !

#### Récapitulatif des statuts datés, restriction d'accès, titres**

    Compte(c)
      Pre (compte premier seulement)
         rad (admin) <- Enc.rad
      Qvc (compte premier)
         cp : null
         rcp : null
      Qvc (compte standard)
         cp : compte premier
         rcp : <- EnC.rcp
      EnC
         std 
      Mdc(im)
         std : std du mur lui-même = Mur(c,im).stdm
         tiM : titre du mur = Mur(c,im).tiM
      Mca(nc,im)
         tiM <- Compte(nc).Mdc(im).tiM
         stdcm <- Compte(nc).EnC.std (std du compte du mur)
         stdm <- Compte(nc).Mdc(im).std (std du mur lui-même)
         rpmu : restriction d'accès au mur du compte = Mur(nc,im).EnM.compte(c).rpmu
         (ra = EnC.std + stdcm + stdm + rpmu)
      Grp(ng)
         ti1 : titre du groupe. <- Groupe(ng).ti1
         stdg <- Groupe(ng).EnG.std
         rpg : restriction de participation au groupe = Groupe(ng).Cpt(c).rpg
         (ra = EnC.std + stdg + rpg)
         murs(im)
            tiG titre du mur. <- Groupe(ng).Mdg(im).tiG
            stdm <- Groupe(ng).Mdg(im).std
            rpmu : restriction de participation au mur du groupe = Groupe(ng).Mdg(im).comptes(c).rpmu
            (ra = EnC.std + stdg + rpg + stdm + rpmu)
            
    Groupe(g)
      Qvg
         cp : compte premier
         rcp <- EnG.rcp
      EnG
         rad
         rcp
         std
         ti1 : titre du groupe
      Cpt(nc)
         stdc <- Compte(nc).std (std du compte membre)
         rpg : restriction de participation au groupe = Compte(nc).Grp(g).rpg
         (ra = ppc + EnG.pp + rpg)
      Mdg(im) (EnM)
         std
         tiG : titre du mur
         comptes(nc)
            rpmu : restriction de participation au mur du groupe = Compte(nc).Grp(g).Mur(im).rpmu
            (ra = Cpt(nc).stdc + EnG.std + rpg + std + rpmu)
    
    Mur(ncg,im) : (mur de compte seulement, ncg est un numéro de compte)
      EmM
         stdcm <- Compte(ncg).EnC.std (std du compte du mur)
         std : std du mur lui-même = Compte(ncg).Mdc(im).std
         tiM : titre du mur = Compte(ncg).Mdc(im).tiM
         comptes(nc)
            stdc <- Compte(nc).EnC.std (std du compte)
            rpmu : restriction d'accès au mur du compte = Compte(nc).Mca(ncg,im).rpmu
            (ra = stdc + stdcm + std + rpmu)
         

## Notifications
**Remarques:**
- (1) : Pas de nom : le compte premier ne laisse pas son nom à ses comptes standard ni aux groupes
- (2) : le compte premier garde en archive la trace des changements sur un de ses comptes standard / groupe avec leur nom / titre même au delà de leur disparition effective
- (3) : utilisation de la clé du mur. L'avis ne sera plus lisible si purge Mdc/Mca.
- (4) : utilisation de la clé du groupe. L'avis ne sera plus lisible si purge Grp.

**Avis d'un compte premier (et admin)**
- mise à jour du quota et/ou la restriction d'accès d'un compte premier par l'administrateur
    - clé M cryptée par : clé pub de #1
    - (#0 administrateur)
    - #1 compte premier.
- rendu de quota à l'administrateur de l'instance par un compte premier (trace historique pour lui-même)
    - #0 compte premier. clé 0.
- mise à jour du quota d'un compte standard par son compte premier
    - clé M cryptée par : clé 0 de #0. clé 1 de #1. 
    - #0 compte premier. (1)
    - #1 compte standard. NOM (2)
- mise à jour du quota d'un groupe par son compte premier
    - clé M cryptée par : clé 0 de #0. clé 1 du groupe.
    - #0 compte premier.
    - #i comptes du bureau.
    
**Avis d'un compte standard**
- mise à jour du mémo d'un mur de compte
- nouveau participant à un mur de compte
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #i participants au mur
- modification de la restriction d'accès d'un participant à un mur
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #1 participant au mur
- changement de propriétaire d'une note
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #1 ancien propriétaire
    - #2 nouveau propriétaire
- changement important d'une note
    - clé du mur. (3)
    - #0 compte auteur.
    - #i comptes participants au mur du compte (sauf auteur).
- suppression d'un commentaire contraire à l'éthique
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #i participants au mur
- suppression imminente d'un mur
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #i participants au mur
- réactivation d'un mur
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #i participants au mur
- révocation d'un participant à un mur de compte
    - clé du mur. (3)
    - #0 propriétaire du mur
    - #1 compte révoqué.
- transfert de quota pour un autre compte C
    - clé M cryptée par : clé 0 de #0, clé pub de #1.
    - #0 compte A.
    - #1 compte C.
- transfert de quota d'un membre du groupe pour un groupe
    - clé G.
    - #0 compte A.
    - #i comptes du bureau.
- rendu de quota à son compte premier
    - clé M cryptée par : clé 0 de #0, clé pub du compte CP.
    - #0 compte A. NOM (2)
    - #1 compte CP.

**Avis d'un membre du bureau d'un groupe**
- avis de création / mise à jour d'un mur d'un groupe
- avis de changement de propriété forcée d'une note
- avis de suppression forcée d'un commentaire d'une note
    - clé G du groupe. (4)
    - #0 compte initiateur.
    - #i comptes participants au mur sauf membres du bureau du groupe.
- avis de changement important d'une note d'un mur de groupe
    - clé G du groupe. (4)
    - #0 compte auteur.
    - #i comptes participants au mur du groupe (sauf auteur).

**Textos (avis textuel pur sans dialogue)**
- textos à 1 à N autres comptes Ci (option : les contacts de mon répertoire)
    - clé M cryptée par : clé 0 de #0. clé pub de #i ...
    - relation 0-i
    - #0 compte A. NOM.
    - #i compte Ci.
- textos aux participants au même mur de compte (voire réduit aux membres du bureau)
    - clé du mur
    - #0 compte A
    - #i compte Ci
- textos aux autres comptes Ci membres du même groupe (voire réduit aux membres du bureau)
    - clé du groupe
    - #0 compte A
    - #i compte Ci

**Dialogues**
- changement de compte premier d'un compte. VOTE
    - clé M cryptée par : clé 0 de #0. clé pub de #1. clé pub de #2.
    - relation 0-1
    - #0 compte demandeur. NOM.
    - #1 nouveau compte premier NCP (votant impératif).
    - #2 ancien compte premier ACP.
- changement de compte premier d'un groupe. VOTE
    - clé M cryptée par : clé du groupe. clé pub de #1. clé pub de #2.
    - relation 0-1
    - #0 compte demandeur du bureau (votant si bureau).
    - #1 nouveau compte premier NCP (votant impératif).
    - #2 ancien compte premier ACP.
    - #i autres comptes du bureau sauf #0 (votant).
- contact
    - clé A-C.
    - relation 0-1 (si C n'était pas déjà contact de A)
    - #0 compte A. (NOM si A a donné son nom à C).
    - #1 compte C.
- invitation d'un compte à participer à un mur de compte. VOTE
    - clé du mur. (3)
    - #0 compte propriétaire du mur.
    - #1 compte invité (votant impératif).
- invitation d'un nouveau membre d'un groupe. VOTE
    - clé du groupe.
    - relation 0-1
    - #0 compte invitant (votant si bureau).
    - #1 compte invité (votant impératif).
    - #i comptes du bureau du groupe sauf #0 (votant).
- révocation d'un membre d'un groupe. VOTE
    - clé du groupe.
    - #0 compte invitant (votant si bureau).
    - #1 compte révoqué sauf si bureau (non votant).
    - #i comptes du bureau du groupe sauf #0 (votant).
- création / mise à jour d'un mur d'un groupe
- changement de propriété forcée d'une note
- suppression forcée d'un commentaire d'une note. VOTE
    - clé du groupe.
    - #0 compte initiateur (votant si bureau).
    - #i comptes du bureau du groupe sauf #0 (votant).
    
**Chats : dialogues purement textuels**
- avec de 1 à 10 autres comptes Ci
    - clé M cryptée par : clé 0 de #0. clé pub de #i ...
    - relation 0-i
    - #0 compte A. NOM.
    - #i compte Ci. NOM si Ci le souhaite.
- avec de 1 à 10 autres comptes Ci participants au même mur de compte
    - clé du mur
    - #0 compte A
    - #i compte C
- avec de 1 à 10 autres comptes Ci membres du même groupe
    - clé du groupe
    - #0 compte A
    - #i compte Ci
- signalement à un compte premier du propriétaire d'un mur
    - clé M cryptée par : clé 0 de #0. clé pub de #1
    - #0 compte A. NOM.
    - #i compte P. NOM si P le souhaite et l'a mis.
- signalement à un compte premier d'un groupe
    - clé M cryptée par : clé 0 de #0. clé pub de #1
    - #0 compte A. NOM.
    - #i compte P. NOM si P le souhaite et l'a mis.



