# Un réseau a-social

Les personnes ayant un **compte** dans une *instance* de cette application peuvent se réunir sur des **forum** afin de partager (ou non) des notes et avoir des conversations privées avec d'autres personnes en contrôlant avec qui et en étant assuré de la non accessibilité des informations intelligibles, même en cas d'écoute intempestive du réseau comme de détournement des données.

Une **note** est constituée,
- **d'un texte** de quelques à quelques milliers de caractères pouvant apparaître sous une présentation simple agréable (niveaux de titre, gras / italique, liste à puces ...).
- **d'une pièce jointe** facultative de n'importe quel type : photo, clip vidéo, etc.

Une  **conversation**  est une suite chronologique **d'échanges textuels** entre ses participants. Une conversation dans un forum peut aussi supporter un vote validant ou invalidant une décision à prendre collégialement.

**Le compte d'une personne** dispose principalement :
- **d'un ou plusieurs CV**, pouvant comporter une photo d'identité, de présentation pour les autres comptes.
- d'un **répertoire de contacts** avec d'autres comptes pouvant comporter un de leurs CV, avec qui une conversation privée peut avoir lieu et que le titulaire peut inviter sur ses forums.
- de **notes personnelles**.

Un **forum** comporte :
- un **répertoire de ses participants**, chacun y apparaissant avec un CV.
- des **conversations** entre les participants.
- des **notes** partagées par les participants.

*La gouvernance d'un forum* est régie par quelques paramètres explicitant quelles décisions (invitation, clôture, etc.) sont soumises à approbation collégiale et quelles sont les règles de décompte des votes qui indique aussi comment la gouvernance peut être changée. A titre d'exemple,
- un forum peut être à caractère *autocratique* : son créateur est seul modérateur et décide seul qui il invite, qui il exclut, comment il modère les contenus, créé et détruit les conversations et in fine le forum lui-même.
- un forum peut aussi être à caractère *démocratique* et exiger une majorité ou un consensus pour ses décisions de gestion.
- un forum peut être aussi à caractère *collégial*, toute décision importante devant être co-signée par un ou N modérateurs.

##### Instances de l'application
Toute organisation / association / société ... (sous condition d'en payer les frais d'hébergement / administration technique) peut déployer *sa propre instance* qui lui sera privée et aura ses propres règles de gouvernance et de déontologie. Une instance est identifiée par l'URL de son serveur central.  
Les instances déployées sont étanches les unes des autres : 
- il n'est pas possible de savoir combien ont été déployées et par qui elles sont utilisées. 
- les comptes des personnes et les forums sont propres à chaque instance.
- la suite du document ne considère qu'une instance.

##### Logiciels de l'application
L'application fonctionne par la collaboration de plusieurs logiciels :
- ***une application serveur*** s'exécute sur un pool de serveurs du *cloud* disposant d'un espace de stockage de données persistant et sécurisé (base de données ...) : l'URL de ce pool est l'identifiant de l'instance ;
- ***une application browser*** s'exécute sur tous les terminaux  (mobiles / tablettes / PC ...) supportant un browser compatible et pouvant y disposer, facultativement, d'un espace de stockage persistant réservé à cette application et privé pour chaque utilisateur du browser. Cette application peut avoir des sessions de travail :
    - ***en mode incognito*** : rien n'est mémorisé sur le terminal utilisé, les données proviennent du cloud et les mises à jour ne se font que sur le cloud ;
    - ***en mode synchronisé*** : l'espace local de stockage du browser est maintenu synchronisé avec le cloud pour le sous ensemble des données qui a été souhaité comme tel par l'utilisateur spécifiquement pour ce terminal ;
    - ***en mode avion*** : l'application s'exécute sans accès réseau sur les seules données stockées localement sur le terminal à l'occasion d'une session antérieure synchronisée.
- ***une application utilitaire*** peut s'exécuter sur un terminal sous Linux / Windows / MacOs afin d'effectuer des sorties des données, en clair ou crypté, sur un répertoire du système de fichiers local.

>Une personne disposant de plusieurs terminaux *peut* avoir localement sur chacun une copie partielle des données qui l'intéresse et utiliser le mécanisme des sessions en mode synchronisé pour les conserver au même niveau sur tous ses terminaux et pouvoir travailler le cas échéant en mode local sur chacun.

# Pourquoi "a-social" ?
Toutes les données humainement signifiantes sont ***cryptés*** sur les terminaux, transitent cryptées sur le réseau, sont stockées cryptées sur les serveurs du cloud comme dans les mémoires des terminaux (en modes synchronisé et avion). Elles ne peuvent être ***décryptées*** que dans l'application qui s'exécute sur les terminaux des titulaires des comptes.

>Le détournement de données, que ce soit la base de données du serveur comme celles locales des terminaux ou de celles transitant sur les réseaux, est inexploitable : les données y sont indéchiffrables.

>Aucune utilisation *commerciale* des données n'est possible puisqu'elles sont toutes indéchiffrables, même et surtout pour le prestataire de l'application et ses hébergeurs.

**Aucun répertoire central ne peut être constitué, ile ne contiendrait que des données inintelligible** : il est impossible de chercher un compte ou un forum dans une instance de l'application en fonction de critères lisibles comme les noms, intitulés des forums, centres d'intérêt, mots clés ...

> On ne s'y connaît que par cooptation, contact direct et rencontre sur un forum.

## Les Comptes
##### Phrase secrète d'un compte
La connexion à un compte ne requiert que d'en connaître ***la phrase secrète*** mémorisée nulle part dans l'application.

**L'oubli de la phrase secrète d'un compte le rend irrémédiablement indéchiffrable, inutilisable** : les clés de cryptage techniques permettant de crypter et décrypter les informations sur le terminal sont elles-mêmes, directement ou indirectement, cryptées par la phrase secrète du compte.  
Aucun compte privilégié n'a la possibilité technologique de  réinitialiser une phrase secrète perdue : les données restent cryptées par l'ancienne phrase.

La phrase secrète d'un compte peut être changée à condition de pouvoir fournir la phrase actuelle.

Tous les comptes ont des phrases secrètes différentes, un dispositif interdit des *présomptions de ressemblance* entre phrases secrètes.

##### Nom immuable d'un compte
**Un compte a un nom** que son titulaire lui a attribué à sa création : ce nom est **immuable**, ne peut plus être changé, est **unique** dans l'application et un mécanisme interdit des *présomptions de ressemblance* entre noms.

Au fil du temps, le nom d'un compte apparaît à d'autres comptes : après avoir accordé leur confiance à un nom, ils sont assurés qu'aucune usurpation d'identité ou réemploi de nom n'est possible.

>**Le numéro d'un compte** est obtenu en brouillant aléatoirement son nom par un procédé de calcul fastidieux tel qu'il est impossible en interrogeant l'application ou en détournant la base de données,
>- de connaître la liste des noms des comptes,
>- de tester l'existence d'un compte en essayant des noms tirés de listes de noms usuels ou par force brute (essai de toutes les combinaisons).

##### Certificat d'identité d'un compte
*Un compte a son identité certifiée par un ou plusieurs autres comptes* : en certifiant l'identité d'un compte A, un compte C ne fait qu'affirmer que le nom du compte A est bien représentatif de la personne qui en est titulaire. Ceci ne signifie rien en termes d'amitié ou de convergence de vue, seulement une affirmation sur l'adéquation entre le nom présenté et la personne physique correspondante.

>Le certificat d'identité d'un compte est la liste des couples (numéro de compte,  nom) des comptes ayant certifié son identité.

>Un compte A *actif* (ni en création, ni résilié) a toujours au moins un compte dans son certificat d'identité. Quand un compte certifiant est résilié il apparaît en tant que tel dans le certificat d'identité de A (ce qui réduit la crédibilité que les autres peuvent avoir dans le compte A).

Comme au départ il n'y a pas de comptes dans l'instance, il est possible de créer ou ou quelques comptes **auto-certifiés** (voir plus avant).

##### Phrase secrète d'administration
Cette phrase se transmet humainement hors de l'application : un brouillage complexe de cette phrase donne une clé de cryptage qui est mémorisée (elle-même brouillée) dans le configuration de l'instance : le serveur peut vérifier qu'un détenteur présumé de cette phrase l'est réellement sans que cette clé ne soit physiquement écrite quelque part.

##### Compte auto-certifié
Une personne connaissant la phrase secrète d'administration peut créer un compte **auto-certifié** en fournissant un brouillage de cette phrase après l'avoir saisie sur son terminal :
- la création du compte est immédiate .
- elle ne requiert pas qu'un autre compte certifie l'identité du compte créé.

Le premier compte créé dans une instance est par principe un compte auto-certifié, les suivants pouvant se faire certifier par le compte auto-certifié. Mis à part qu'ils se sont auto-certifiés à leur création, ces comptes sont comme les autres et peuvent d'ailleurs ultérieurement avoir leur identité certifiée par d'autres comptes.

#### Cercles des comptes connus d'un compte A
**Le premier cercle** est l'ensemble des comptes inscrits dans le répertoire personnel des contacts de A.  
**Le deuxième cercle** est l'ensemble des comptes des participants inscrits aux mêmes forum que A (et par convention qui ne sont pas dans le premier cercle).  
**Le troisième cercle**, plus indirect, est celui des comptes ayant certifié l'identité d'un compte d'un des deux premiers cercles.  

**Pour inscrire un contact dans son répertoire personnel, A doit l'avoir trouvé dans l'un des deuxième ou troisième cercles.**  
Comme au départ un nouveau compte ne connaît personne d'autre que son propre certificateur (seul présent dans son répertoire personnel), ses cercles de connaissances ne peuvent pas s'agrandir, du moins tant que son certificateur ne l'a pas invité au moins à participer à un forum où il pourra rencontrer d'autres comptes.

**Contacts en attente d'approbation**  
A peut enregistrer dans son répertoire un contact C en attente d'approbation : il faut pour cela que A ait rencontré dans la vraie vie la personne C ayant un compte dans l'application et lui ayant confié l'adresse d'une de ses (4 au plus) *boîtes postales*.
- A peut inscrire C en attente d'approbation en donnant cette adresse de boîte postale et un court message de courtoisie destiné à se faire reconnaître.
- C peut reconnaître A et accepter l'établissement de leur contact mutuel en inscrivant A dans son répertoire et en transmettant son nom à A.
- mais C peut aussi se raviser, ne pas reconnaître A, suspecter une usurpation d'identité, bref refuser le contact : C reste un contact anonyme dans le répertoire de A. Si C a répondu au message de courtoisie de A en motivant son refus, A pourra le lire, sinon A n'a plus qu'à détruire cette entrée en attente qui encombre son répertoire.

L'adresse de boîte postale peut aussi être employée pour inviter un compte C à un forum quand aucun participant du forum ne peut lancer une invitation nommée faute de disposer du nom de C dans l'un de ses trois cercles.

## Les Forum
Un compte peut créer un forum dès qu'il souhaite partager des notes et/ou ouvrir des conversations avec plusieurs autres comptes.
- le forum ainsi créé a un identifiant interne tiré au hasard à sa création.
- **le créateur devient le premier participant**, avec un statut de modérateur, du forum.
- **il peut inviter certains des comptes** de ses cercles de connaissance, ou connue par une adresse de boîte postale, à participer au forum.
- ultérieurement tout participant peut aussi lancer des invitations qui selon les règles de gouvernance du forum, seront ou non soumise à approbation des autres participants.

Partager des informations privées à plusieurs suppose d'avoir confiance en ceux avec qui ça sera fait. Cette acquisition de confiance joue autant pour les **invitants** (*puis-je avoir confiance dans cet invité ?*) que pour **l'invité** (*avec qui vais-je partager des informations ?*).

Une note ou une conversation du forum peut avoir un **degré de confidentialité** :
- lisible par les **invités**.
- privées pour les **participants confirmés**.

Un invité reçoit avec son invitation une clé de cryptage qui lui permet :
- **de lire la liste des participants**, leur CV (du moins celui choisi pour être visible par les participants et invités de ce forum) et leurs certificats d'identité (qui les certifie).
- **de lire la ou les notes et conversation déclarées lisibles aux invités** et qui peuvent présenter l'objet du forum et le cas échéant ses règles internes.

Ainsi un invité peut se forger une opinion sur le forum qui l'invite et il peut choisir d'accepter cette invitation ou de la décliner.

**S'il l'accepte**, il fournit un de ses CV et son certificat d'identité pour que les autres participants sachent si oui ou non il est souhaitable de valider cette participation.
- **si oui** l'invité devenu *participant confirmé* reçoit les clés de cryptage lui permettant d'accéder aux notes et conversations privées du forum.
- **si non**, l'invité est recalé. Il aura eu le temps de lire la liste des participants et la ou les quelques notes de présentation mais rien d'autre.

Dans les cas simples ou un invité est particulièrement et favorablement bien connu, *son invitation peut se faire avec approbation implicite* pour raccourcir le délai. Il reçoit dès l'invitation les clés permettant d'accéder à tout le forum, le serveur ne lui donnant effectivement accès qu'aux notes / conversations réservées aux invités tant que l'invitation n'est pas confirmée.

La vie du forum est pour l'essentiel marquée par :
- la lecture et l'écriture de notes et de leurs pièces jointes.
- l'ouverture de conversations et la poursuite de conversations en y ajoutant des échanges.

##### Statut d'un participant
Un participant peut avoir plusieurs niveaux de statut :
- **invité** en attente d'acceptation ou de refus de l'invité.
- **en attente d'approbation** par les participants.
- **observateur** : il peut lire mais pas écrire, ni note, ni échange de conversations. Il ne prend pas part aux votes.
- **régulier** : il peut lire et écrire des notes et participer aux conversations. Il prend part aux votes selon les règles de gouvernance du forum.
- **modérateur** : il peut de plus modérer des notes (et leurs pièces jointes) et échanges des conversation en les masquant (seuls les modérateurs peuvent les lire) ou les démasquant (lisibles par tous) ou détruisant le contenu. Les règles de gouvernance peuvent,
    - fixer un poids supérieur aux décisions des modérateurs dans le forum.
    - décider de ne pas avoir de modérateur.

##### Conversations
Une conversation est créée par un participant qui peut éventuellement restreindre la liste des participants pouvant la lire et y participer (listes qui peuvent être ultérieurement étendues mais pas restreintes). Par défaut c'est l'ensemble des participants.  
Chaque échange d'un participant est inscrit par ordre chronologique et ne peut plus être rectifié, avec les deux exceptions suivantes :
- son auteur a le droit d'effacement de ses propres échanges (oups !) mais pas de correction.
- un modérateur a le droit à le classer *modéré* (lisible par les seuls modérateurs) et/ou l'effacer mais jamais d'en changer le contenu.

**La plupart des conversations sont à objet libre**: la première ligne du premier échange en donne l'intitulé.  
Un sondage implicite est toujours ouvert mais dans bien des cas n'est pas utilisé.

**Certaines conversations ont pour objet la prise d'une décision formelle**, comme par exemple :
- l'invitation d'un nouveau participant et son approbation, le changement de son statut.
- l'exclusion d'un participant.
- la suppression de conversations / notes obsolètes et consommatrices d'espaces.
- la clôture du forum lui-même.

Dans ce cas le vote de la conversation a pour but de valider (ou d'invalider) une décision d'action, formelle et ayant des paramètres précis : 
- la conversation peut faire évoluer les paramètres de la décision soumise à discussion : cela annule les votes antérieurs. 
- un participant peut voter plusieurs fois, le vote le plus récent est seul pris en compte.
- les votes sont publics, vérifiables par chacun.
- la fin de la conversation a lieu :
    - soit parce que son initiateur y renonce : elle est annulée.
    - soit sur constat d'un vote négatif, la décision n'est pas approuvée.
    - soit par la réalisation de l'action décidée, qui peut se conclure normalement ou sur un échec.

##### Notes et pièces jointes
**Une note peut être créée par tout participant régulier** qui devient son *propriétaire*.
- elle comporte au moins une ligne de texte qui lui sert de titre / descriptif.
- elle *peut* avoir une pièce jointe.
- elle *peut* être marquée comme associée à une conversation précise ou au forum. Si elle est associée à une conversation elle disparaîtra quand celle-ci est détruite.
- la liste des participants autorisés à lire / écrire *peut* être restreinte au départ (par défaut tous les participants au forum sont autorisés), peut être augmentée mais pas réduite.
- elle peut être déclarée *archivée* par son propriétaire : elle ne pourra plus être modifiée par personne.
- son propriétaire peut en transmettre la propriété à un autre participant.
- un modérateur (s'il en existe sur le forum) peut modérer son contenu :
    - la note n'est plus lisible que par son propriétaire (désormais seul à pouvoir la modifier) et les autres modérateurs.
    - elle peut être détruite par un modérateur.
    - elle peut avoir sa modération levée par un modérateur (si elle n'a pas été détruite).
    - un modérateur ne peut pas en changer le contenu.

##### Clôture d'un forum
Elle se passe en deux temps :
- une annonce de clôture proche qui le place en état zombie laissant le temps de copier des contenus jugés utiles et surtout ouvrant un délai de remord.
- une destruction effective un certain temps après.

Il est possible de réactiver un forum zombie quand on a un remord ou de prolonger l'état zombie.

La destruction effective supprime physiquement les données et retourne le quota aux comptes détenteurs (voir ci-près).

>**Remarque** : dans le cas le plus simple un forum s'ouvre instantanément sur demande d'un compte qui y invite immédiatement les contacts qu'il souhaite avec approbation par défaut. Une gouvernance par défaut s'installe, et les participants peuvent sur l'instant accepter l'invitation et participer à une conversation et s'échanger des notes.

## Gouvernance et contrôle de l'espace : volumes, quotas
Les données stockées pour un compte ou un forum, surtout celles correspondant aux notes, peuvent occuper un volume significatif. Ce volume est divisé en deux parts :
- (1) l'une correspond à toutes les données sauf les pièces jointes attachées aux notes.
- (2) l'autre correspond aux pièces jointes attachées aux notes.

Selon les options techniques retenues pour chaque instance les coûts de stockage peuvent être significativement différents pour les deux types.

### Gouvernance de l'instance
Héberger un serveur et occuper de l'espace a un coût qui peut être couvert de diverses façons :
- un mécène payeur s'acquitte des frais sans obligation ni contrepartie pour les comptes hébergés.
- le payeur global refacture les frais de l'instance, typiquement par disque virtuel.
- les comptes acquittent par ailleurs, éventuellement, une cotisation qui peut être associée au disque virtuel qu'ils utilisent.

La gouvernance de l'instance fait face aux questions suivantes :
- **doit-on réguler le volume utilisé** et si oui comment ?
- **que fait-on des comptes, et indirectement des forums, inactifs** sur une longue durée et immobilisant en pure perte des ressources de stockage ?

L'instance a été créée par une personne physique ou une organisation ayant un but : une charte de déontologie peut être explicitement proposée aux comptes qui s'engagent à la respecter. Sans émettre aucun jugement une organisation suprémaciste n'a pas envie d'héberger des discours et des campagnes défendant l'idée qu'il n'y a pas de races humaines donc de suprématie de l'une sur les autres alors que la déontologie d'une association anti-raciste aura exactement les objectifs opposés.

Sachant que le contenu des notes et conversations d'un compte lui sont totalement personnelles et impossibles à décrypter, **aucun contrôle déontologique de peut s'effectuer sur les notes d'un compte**.  
C'est un peu différent pour une conversation entre deux comptes : elle est certes totalement privée (et techniquement indiscutablement privée) et aucun des deux interlocuteurs n'est obligé à la poursuivre, chacun peut l'interrompre à son gré. Mais certains pays ont des lois réprimant l'insulte *même non publique* (même si l'application en est peu fréquente). Un compte peut être amené à se plaindre d'un autre et à justifier cette plainte par un texte indiscutable (non répudiable par son auteur).    
De plus un compte représente une personne physique qui peut finalement être jugée *persona non grata* sur l'instance en raison de sa *notoriété* dans le vrai monde, voire des autorités judiciaires qui peuvent vouloir priver quelqu'un de toute capacité d'échange et d'enregistrement même personnel d'informations.

Sur un forum c'est un peu différent : plusieurs comptes ont accès aux contenus et l'un d'entre eux peut estimer qu'un ou plusieurs contenus ne respectent pas la charte déontologique de l'instance. Un forum est certes strictement privé et ne fait pas partie de l'espace public : 
- en toute rigueur la liberté d'expression *privée* n'est légalement pas réprimée (enfin en général).
- mais les insultes, mêmes privées, peuvent parfois légalement ne pas être admises.
- **enfin l'instance est sous gouvernance privée** : elle peut avoir les règles déontologiques de son choix.

Quelques grands paramètres de gouvernance l'instance sont fixés en paramètre :
- un quota attribué peut-il revu à la baisse ? 
- le droit de dissolution d'un compte pour non respect de son quota est-il possible ?
- des comptes inactifs peuvent-ils être supprimés ?
- des alertes de non respect de la déontologie sont-elles recevables auprès d'un compte modérateur général de l'instance ? 
    - Le lanceur d'une alerte peut-il être anonyme ? Toujours, jamais, à sa convenance ?
    - un compte peut-il être résilié par la modération générale pour cause de non respect de la charte déontologique ?

Ainsi une instance peut avoir une gouvernance qui peut osciller entre :
- ultra libertarienne et le cas échéant être dissoute par action violente hors de l'application. Fermeture du site par l'hébergeur, arrestations, etc.
- moyennement contrôlée en y acceptant que des gentlemen ou des ladies.
- structurée afin de protéger le plus grand nombre de dérives éventuelles de quelques uns.

### Techniques de contrôle du volume
#### Disques virtuels
L'espace de stockage global est constitué d'un ensemble de  **disques virtuels** chacun ayant :
- un code court qui est son identifiant immuable.
- un quota d'espace distingué en q1 / q2.
- une clé de cryptage spécifique mémorisée cryptée par la clé d'administration. Cette clé ne crypte que son libellé, pas les contenus qu y sont inscrits.
- un libellé crypté par cette clé.
- une restriction d'accès :
    - *aucune restriction, lecture seule, accès interdit*.
    - *un code d'alerte* : quelques codes sont prédéfinis par instance et qualifie à la fois le reproche fait aux utilisateurs du disque et un niveau de menace sur leurs comptes.
- un ou quelques **comptes contrôleurs**.

Un compte connaissant la phrase secrète d'administration peut :
- accéder à la liste des disques virtuels, en ajouter et sous certaines conditions en supprimer.
- modifier leurs quotas q1 / q2.
- changer leurs comptes contrôleurs des disque virtuel.
- en cas de changement de la phrase secrète d'administration mettre à jour le cryptage des clés des disques par la clé d'administration en fournissant l'ancienne et la nouvelle clé d'administration.

#### Contrôle de l'espace global
Globalement chaque disque virtuel doit respecter son quota.  
L'administration de l'instance peut, par un compte connaissant la phrase secrète d'administration, **restreindre voire supprimer l'accès à un disque virtuel**, et in fine le cas échéant le détruire.

#### Contrôle de l'espace sur un disque virtuel
Son ou ses comptes contrôleurs gèrent les attributions de quotas aux comptes utilisant ce disque. Ils peuvent aussi contraindre ceux-ci au respect de leurs obligations en usant à leur égard d'une possible restriction d'accès puis de blocage et in fine le cas échéant de la destruction d'un compte. Ils sont en particulier capables :
    - de révoquer les comptes obsolètes utilisant des quotas qui seraient utiles à d'autres.
    - de restreindre l'accès voire supprimer des comptes suite à décision de modération.

#### Quota initial d'un compte A
A la validation de sa création, lorsqu'un compte certificateur C a accepté de certifier l'identité de A, A reçoit de C un quota minimal du disque virtuel auquel C est associé.  
A cette occasion une **conversation** est ouverte (mais vide) entre A et le ou les comptes contrôleurs (passés, actuels ou futurs) de son disque virtuel.

##### Demandes d'augmentation de quota
Le compte A peut faire une demande d'augmentation de son quota : elle est enregistrée sur la **conversation** entre le compte et son disque virtuel.
- un exemplaire est enregistré dans le dossier du disque virtuel : les comptes contrôleurs y ont accès.
- l'autre exemplaire est enregistré dans le dossier du compte.

La demande peut ainsi être :
- satisfaite, complètement ou partiellement (mas pas plus que le niveau de la demande).
- refusée.
- conservée en attente, prioritaire ou non.
- archivée un certain temps.
- détruite.  

##### Transfert d'un disque à un autre
Une demande de quota peut aussi comporter le souhait du compte à être transféré sur un autre disque plus adapté à l'usage ou à l'organisation.  
Tout contrôleur du disque étant aussi contrôleur d'un autre disque peut effectuer ce transfert qui se limite à une opération sur les quotas et à fournir la clé de cryptage du nouveau disque.

#### Quota d'un forum
Un forum reçoit à sa création un quota prélevé sur le quota du compte créateur.
- il peut recevoir ensuite des quotas en provenance d'autres comptes participants.
- ces quotas restent **concédés** par les comptes participants au forum : son quota est virtuellement la somme des quotas concédés par les participants.
- quand un forum décide de remettre à disposition du quota inutile, celui-ci est rendu aux participants concessionnaires, la règle de proportionnalité pouvant être contrariée par un vote des participants. C'est la même situation quand le forum est clôturé.
- un forum ne fait pas de demande d’accroissement de quota, ce sont ses participants qui y contribue.

#### Restriction d'accès à un disque posée par un administrateur de l'instance
Elle peut prendre les valeurs suivantes :
- pas de restriction : lecture / écriture possible. Si l'écriture conduit à une *augmentation* de volume alors que le quota est déjà dépassé,  elle est refusée. 
- lecture seule : toute modification des conversations et notes est impossible.
- accès bloqué (ni lecture, ni écriture). 
 
La restriction est accompagnée d'un code explicitant la raison de la restriction ou de sa levée afin que les mesures correctives puissent être prises.

#### Restriction d'accès à un compte posée par un compte contrôleur
Une restriction similaire peut être apposée sur un compte par un des contrôleurs de son disque virtuel avec le même objectif. Le contrôleur peut motiver sa décision par un échange sur la conversation permanente ouverte avec le compte.  
La restriction effectivement applicable au compte est le plus restrictive des deux, mais les deux sont visibles pour un compte.

### Résiliations autoritaires de comptes
Un compte peut ne pas respecte la limitation de quota qui lui a été fixée. Comme il n'est pas possible de créer / modifier des contenus qui feraient passer au dessus du quota d'un compte ou d'un forum, cette situation n'apparaît qu'en cas de restriction a posteriori :
- un compte a eu son quota réduit autoritairement par un contrôleur de son disque : son volume utilisé courant peut excéder le quota restant ;
- un forum a eu un participant résilié ou qui a réduit son le quota qu'il avait concédé au forum. Le volume utilisé par le forum peut excéder son quota et par rebond le volume imputé aux autres comptes étant accru en proportion peut leur faire excéder leur propre quota.

Un forum, donc indirectement des comptes, ou un compte a été la cible d'un lanceur d'alerte à propos d'un contenu non conforme à la charte éthique de l'instance et une décision de modération générale a été prise de fermeture.

Un contrôleur de disque a la possibilité de lister les comptes morts, n'ayant eu aucune activité depuis un certain temps. Chaque compte participant d'un forum est aussi en mesure de détecter de son côté quels sont les forums morts et peuvent s'en exclure.

La résiliation autoritaire est voisine d'une auto-résiliation du compte (voir ci-après).

# Compte : ses contacts, sa création et sa résiliation

##### CV (curriculum vitae) d'une personne
Un compte peut se déclarer de 1 à 4 CV comportant :
- un court libellé plus explicite que le numéro de 1 à 4 du CV pour en choisir un à l'écran.
- **un texte de présentation** de son choix, plus ou moins détaillé selon l'audience à qui le CV s'adresse. Il peut y figurer des données de contact permettant d'être joint dans la vraie vie comme un numéro de téléphone Signal ou Telegram, d'où l'importance de savoir quel CV est montré à qui ou quel forum.
- **une photo d'identité** facultative de faible définition.

##### Adresses de boîte postale
Un compte peut se déclarer jusqu'à 4 adresses de boîte postale qui peuvent être modifiées / supprimées au gré du titulaire du compte. Chaque adresse est,
- *un texte libre* de 30 à 72 caractères.
- *garanti unique* dans l'instance et ne doit pas ressembler de trop près à une autre adresse déjà déclarée.

Une adresse de boîte postale d'un compte A permet à une personne C ayant rencontré A hors de l'application,
- d'établir avec elle une *conversation d'échange de contact*, 
- de l'inviter à participer à un forum.

## Répertoire des contacts personnels d'un compte A
Un compte A peut enregistrer spécifiquement dans *son répertoire des contacts personnels* un certain nombre de comptes qu'il a pu rencontrer au cours de ses sessions.  
Pour chaque compte C ainsi répertorié, il est noté :
- **le numéro de compte** de C.
- **son nom**, s'il le connaît, ce qui, au moins temporairement, n'est pas systématique. Par exemple après avoir contacté C par une adresse de boîte postale, le nom de C reste inconnu de A jusqu'à ce que C réponde en acceptant de lui fournir son nom (ce qu'il n'est pas obligé de faire). L'absence de nom qualifie une entrée *en attente d'approbation*.
- **un mémo** personnel facultatif à propos de C. La première ligne de ce mémo vient compléter le nom à l'affichage.
- **le numéro du CV** que A a décidé de monter à C ce qui exprime *la confiance que A porte à C*.
- si cette entrée de répertoire est *favorite, normale ou masquée* à l'affichage.
- **la date-heure d'inscription** de cette entrée dans le répertoire.
- **la date-heure de résiliation** si C est résilié. Le nom et le mémo restent mais pas le numéro du CV auquel C avait (éventuellement) accès.
- **la relation initiale entre A et C**, comment A a connu C :
    - par une adresse de boîte postale,
    - en tant que coparticipant à un forum,
    - en tant que certificateur d'un de ses contacts inscrits au répertoire,
    - en tant que certificateur d'un des coparticipants à un forum.
    - parce que C l'a contacté à l'un des titres ci-dessus.
- **la liste des relations qui unissent A à C**, à quels titres A connaît C, peut ensuite être étoffée / réduite par A de manière à ne conserver que les plus pertinentes.
- **l'indicateur de demande de certification** : si A a fait cette demande à C et qu'elle n'a pas (encore) été honorée.
- **liste noire**: A refuse que C ait des échanges avec lui. Seul A, s'il le souhaite, peut envoyer un échange à C (si A n'est pas en liste noire de C). Tout ce que C peut faire est de lever un drapeau demandant à A de bien vouloir le retirer de sa liste noire (A en faisant ce que bon lui chante).
- réciproquement, A connaît ces informations issues du répertoire de C :
    - C et A sont-ils contacts réciproques, C as-t-il une entrée de répertoire pour A.
    - si oui, C y connaît-il le nom de A.
    - si oui, quel CV C a-t-il décidé de montrer à A (C a-t-il confiance en A).
    - C a-t-il demandé à avoir son identité certifiée par A.
    - C a-t-il inscrit A en liste noire.

##### Contacts réciproques entre A et C
Quand le compte A a inscrit C dans son répertoire et que C a aussi inscrit A dans son répertoire, les contacts sont dits **réciproques** mais ne sont pour autant pas forcément de même niveau :
- A peut connaître le nom de C sans que C ne connaisse le nom de A.
- A peut accorder sa confiance à C en lui présentant un de ses CV sans que C n'accorde sa confiance à A (A ne voyant pas de CV de C).
- A peut être certifié par C sans que C ne soit certifié par A.

##### Opérations de gestion du répertoire
La création d'une entrée se fait :
- soit en désignant un des comptes coparticipant à un forum, soit d'un compte certificateur d'un de ses contacts existants ou d'un coparticipant à un forum.
- soit en donnant une adresse de boîte postale et en ouvrant une conversation avec le compte correspondant. Dans ce cas le nom est à ce stade inconnu, le contact est en attente d'approbation.

Les actions possibles sur un contact inscrit au répertoire sont les suivantes :
- enregistrement du nom du contact quand il était inconnu et vient d'être accessible,
- mise à disposition de son nom pour le contact qui ne le connaissait pas et souhaitait en disposer,
- déclaration / suppression de confiance (CV) au contact,
- demande de certification d'identité par le contact,
- annulation de cette demande,
- certification de l'identité du contact,
- suppression de cette certification,
- suppression de la certification accordée par le contact,
- mise à jour du mémo personnel à propos du contact,
- mise à jour du statut favori / normal / masqué de l'entrée de répertoire pour le contact,
- suppression de l'entrée dans le répertoire pour ce contact.
- déclaration d'une *relation* avec le contact (rencontre à un autre titre que ceux déjà identifiés), suppression d'une *relation* enregistrée, vérification de validité des relations enregistrées.
- mise en liste noire ou retrait de la liste noire.

##### Certification d'identité
A ne peut certifier l'identité de C qu'à condition,
- que C ait accordé sa confiance à A en lui montrant un de ses CV.
- que C le lui ait demandé explicitement.

**Retrait de certification :**
- A peut librement retirer sa certification de C comme C peut retirer librement sa certification de A.
- si C retire sa confiance à A, cela retire la certification (éventuelle) de C par A.
- tout compte (sauf auto-certifié) doit avoir au moins un compte certifiant son identité.

#### Fiche Information à propos d'un autre compte
A partir d'un numéro d'un compte C le serveur peut rechercher si ce compte C est pour un compte A *contact de confiance / membre d'un même groupe / participant à un même mur* et si c'est le cas en retourner une fiche information comportant :
- son **nom**,
- s'il est inscrit dans le répertoire de A, le **mémo** enregistré pour lui. 
- la liste de ses **CV* que A peut voir,
- son **certificat d'identité**,
- la liste des **forums auxquels A et C participent tous deux** avec pour chacun,
    - son *intitulé*,
    - les statuts de A et C (observateur / régulier / modérateur),
    - les *numéros* des CV rendus visibles respectivement par A et C.
 
#### Recherches possibles
A étant un compte authentifié, la session apportant la preuve de sa connaissance de sa phrase secrète de A, il est possible d'effectuer des recherches d'autres compte.

***Depuis le numéro de compte de A :***
- la liste des comptes dont A certifie l'identité,
- la liste des comptes qui certifient l'identité de A,

***Depuis un forum auquel A participe :***
- la liste des comptes des autres participants.

***Depuis un numéro de compte C*** :
- la liste des contacts, avec leur nom, communs aux répertoires de A et C.
- la liste des comptes, avec leur nom, inscrits dans le répertoire de A dont C est certificateur.
- la liste des comptes, avec leur nom, inscrits au répertoire de C dont A est certificateur.
- la liste des participants, avec leur nom, aux forum auxquels A est participant dont C est certificateur.
- la liste des participants, avec leur nom, aux forums de C dont A est certificateur.
- la liste des forums où A et C sont tous deux participants.

***Depuis deux numéros de comptes X et Y***
- la liste des forums où ils peuvent se rencontrer.
- leurs inscriptions dans leurs répertoires respectifs.

>**Lister *tous* les comptes de *tous* les forums auxquels A participe** n'a pas de sens en raison de la taille potentielle de la liste résultante : A peut participer à une centaine de forums ayant chacun des dizaines de participants.

## Processus de création d'un compte
Avant de pouvoir créer son compte, A doit préalablement :
- connaître une adresse de boîte postale d'un compte C ayant accepté de le parrainer en certifiant son nom.
- choisir un nom qui ne ressemble pas trop à un nom déjà enregistré.
- choisir une phrase secrète qui ne ressemble pas trop à une phrase déjà enregistrée.
- établir son premier CV à l'intention de C si possible avec une photo.
- écrire un mot de courtoisie pour C.

Une fois ces conditions remplies, l'opération de création du compte s'effectue :
- le compte de A est créé et est en état *en création*. Il a des possibilités limitées et un quota très réduit.
- le répertoire des contacts de A est créé avec une unique entrée, celle de C. Y figure : le numéro de CV #1 (A accorde sa confiance d'avance à C) et l'indication que A souhaite que C certifie son nom.
- une conversation est engagée entre A et C : C reçoit le mot de courtoisie de A et pourra y répondre.

**Cas 1 : C accepte de certifier le nom de A.**
- le nom de C est désormais inscrit dans le répertoire de A. C a même pu à cette occasion accorder sa confiance à A en lui indiquant auquel de ses CV il lui donne accès.
- A est inscrit réciproquement, avec son nom, dans le répertoire des contacts de C.
- C est inscrit dans le certificat d'identité de A.
- A reçoit pour quota celui que C a souhaité lui donner (avec un minimum), ce quota étant associé au même disque virtuel que celui supportant le quota de C.
- le compte de A passe à l'état *actif*.

**Cas 2 : C refuse de certifier le nom de A (ou ne répond pas).**
- le compte de A est toujours *en création* et tout ce que A peut faire est :
    - de modifier / créer des CV.
    - mettre à jour quelques courtes notes personnelles (mais son quota est minimal).
    - et surtout ouvrir un dialogue d'échange de contact avec un autre compte C2, dont il s'est procuré une adresse de boîte postale, avec demande de certification de son nom.
- au bout de quelque jours passés en état *en création* le compte de A est automatiquement résilié.

## Vie d'un compte actif
La vie d'un compte actif est ponctuée par une suite d'opérations :
- de mise à jour des notes personnelles, des CV, des adresses de boîte postale.
- de gestion de son quota : réduction de quota, ou *demande* d'augmentation de quota ou de  transfert vers un autre disque virtuel.
- de gestion de son répertoire de contacts.
- d'échange sur les conversations avec ses contacts personnels. Il existe potentiellement une conversation ouverte avec chaque contact du répertoire. Il est possible :
    - d'y ajouter un échange textuel,
    - de supprimer un échange précédent (oups !) des deux côtés,
    - de purger de son côté les échanges trop vieux ou sans intérêt.- 
- de participation aux forums qu'il a créé ou auxquels il a été convié.

## Résiliation d'un compte par lui-même
Elle s'effectue en deux temps :
- **Résiliation du compte**: le compte passe en état *zombie* pendant quelque jours (le temps d'un remord).
- **A la fin de ce délai le compte est physiquement détruit** ainsi que les forums dont il était l'unique participant.

**Le titulaire du compte peut revenir sur sa propre résiliation pendant quelques jours**. Il peut également prolonger l'état zombie.

#### Résiliation par un des comptes contrôleurs de son disque virtuel
La seule différence avec le résiliation par le compte lui-même est qu'il ne lui est pas possible d'arrêter par lui-même cette destruction, ni prolonger son état zombie, ni en sortir.  
En état zombie toutefois la conversation avec ses contrôleurs de son disque virtuel reste active.

#### Destruction physique d'un compte
Cette destruction s'effectue N jours après,
- soit sa création si le compte est toujours *en création*.
- soit sa résiliation l'ayant plongé en état *zombie* (par lui-même ou son compte premier).

Le compte est physiquement supprimé et certaines autres mises à jour interviennent également :
- *le compte est marqué résilié dans la liste des participants de tous les forums* auxquels il participait.
- *le compte est marqué résilié* dans tous les répertoires des autres comptes qui le référençait.
- si le compte avait alloué un quota au forum, ce quota s'annule ce qui peut mettre le forum est pénurie d'espace.

*Remarque :*
- si le compte était *en création* il ne subsiste rien du compte.
- si le compte était *actif*, il ne subsiste que le minimum d'information bloquant techniquement l'utilisation d'un nom trop ressemblant au compte résilié avec juste ses dates de création / début et fin d'activité.

# Vie d'un forum

#### Décisions collégiales de déclenchement d'une opération de gestion
Pour chaque opération de gestion, une règle de gouvernance fixe ses conditions d'approbation : 
- le nombre de vote *pour* doit être supérieur au nombre de votes *contre* et *veto*.
- selon l'option choisie, un *veto* bloque l'approbation ou est compté comme vote *contre*.
- un nombre et un pourcentage (par rapport aux participants réguliers ou aux seuls votants) de votes *pour* minimaux requis pour valoir approbation.
- le collège des votants : tous les participants réguliers au forum ou les seuls modérateurs.
- le nombre minima de N heures d'ouverture du vote : une approbation n'est pas valide avant.

A titre d'exemple, un vote *minimal de 1 et de 1% sans veto ouvert a minima 0h*, permet de lancer l'opération immédiatement si le demandeur vote *pour* l'ouverture de la conversation.

Tous les participants peuvent intervenir plusieurs fois dans la conversation :
- en donnant leurs commentaires ;
- en exprimant leur vote *abstention pour contre blanc veto*. Seul le vote le plus récent de chacun compte.
- en modifiant les paramètres de l'opération proposée ce qui invalide les votes antérieurs.

**L'opération est lancée automatiquement dès qu'un vote fait basculer la décision d'approbation** et que le délai minimal d'ouverture du vote est dépassé : la conversation est close avec pour statut final celui de l'opération.

Au delà du délai minimal requis et sur demande explicite d'un participant à la conversation,
- si l'approbation est acquise : l'opération est lancée. La conversation est close avec pour statut final celui de l'opération.
- si l'approbation n'est pas acquise : la conversation est close avec pour statut final *désapprouvée*.

Ceci laisse la latitude de prolonger la durée de vote en tentant de convaincre d'autres votants.

L'initiateur de la conversation peut à tout instant l'annuler : elle est close avec pour statut final *annulée*.

##### Processus de gestion du forum
Les processus courants sont :
- **Invitation d'un nouveau participant**.
    - Les paramètres sont :
        - son statut : observateur, régulier, modérateur.
        - approbation automatique ou non en cas d'acceptation de l'invité.
    - l'invité peut :
        - *accepter cette invitation* en fournissant un CV consultable par leurs autres membres. Si l'approbation était automatique, il devient participant avec le statut prévu.
        - *décliner l'invitation* au forum.
    - en cas d'acceptation le vote valide l'approbation ou la rejette. 
- **Changement de statut d'un participant** entre observateur / régulier / modérateur.
- **Révocation d'un participant**.
    - en cas de vote positif, le participant a un statut *révoqué*, ne peut plus accéder au forum.
    - La trace de son passage dans les notes et conversations perdure après révocation : son droit à l'oubli se heurte au droit des autres à ne pas oublier. Les notes où il figurait comme seul auteur lui resteront attribuées (sauf en cas d'effacement pour raison éthiques). Son nom reste lisible mais son CV est effacé.
- **Modération d'une note ou d'un échange** : le collège est celui des modérateurs. En général il est requis une voix, parfois N pour une approbation collégiale.
- **Restitution de quota**. Le vote porte sur le montant restitué et en particulier à quels participants en ayant concédé et combien.
- **Changement de gouvernance du forum**.
- **Dissolution du forum**. En cas de vote positif, le forum ne passe pas en en état zombie, aucun remord n'est possible le vote ayant déjà pris du temps de réflexion (la décision ne peut pas être un loupé ou une faute de frappe).

## Contenus des notes et échanges d'un conversation
Une conversation ou une note d'un forum peut être créée avec deux listes restrictives :
- celle des participants ayant droit de lecture,
- celle de ceux ayant droit d'écriture.

Par défaut ces listes sont vides (autorisation à tous).

Un échange d'une conversation n'a qu'un auteur.

Une note porte à la fois,
- l'identification de son propriétaire (le premier auteur sauf en cas de transmission de propriété par son auteur initial).
- la suite de ses auteurs successifs.

##### Cryptage d'un contenu
Un contenu est crypté :
- soit par la clé délivrée aux invités du forum : il peut être lu par n'importe quel invité, même si sa participation n'est finalement pas approuvée.
- soit par la clé réservée aux participants **en cours au moment de l'écriture**. Le contenu n'est lisible que par les participants approuvés.

Le contenu d'un échange ou le texte d'une note peut contenir des **hashtags** qui facilitent leur sélection en session. Ces hashtags sont cryptés par la clé d'invitation.

##### Pourquoi plusieurs clés de cryptage des contenus ?
Un participant régulier à un instant t1 :
- peut lire une note N, il dispose de la clé et peut techniquement la sortir de l'application et conserver sur son terminal, comme il peut conserver sur son terminal le texte en clair de la note.
- il peut se faire exclure du forum à t2.
- la note N peut être mise à jour à t3 par un autre compte.
- il ne peut plus lire la nouvelle version de la note N sur le serveur officiel.
- s'il a accès à une copie détournée de la base, il pourrait lire la nouvelle version de la note N puisqu'il a pu conserver la clé de cryptage ... mais ceci va échouer.
- après son exclusion une nouvelle clé de cryptage a été ajoutée : les nouveaux contenus, dont la note N à t3 sont cryptés avec cette nouvelle clé créée à t3 et qu'il n'a pas. Les seuls contenus qu'il peut obtenir de la base détournée sont ceux qu'il pouvait lire avant son exclusion (cryptés par des clés datant d'avant t2) et qui n'ont pas changé.

Chaque contenu dispose en conséquence de l'indice de la clé de cryptage qui le crypte et chaque participant dispose de toutes les clés de cryptage qui ont pu être utilisées dans le passé (il n'est pas concevable de ré-encrypter tous les contenus).

##### Digest d'un contenu
Tout contenu dispose du digest de son texte en clair :
- digest court pour les textes des notes et des échanges.
- digest long pour les pièces jointes.

Le serveur n'ayant jamais les contenus en clair est incapable de vérifier la validité de ce digest mais toute session lisant un contenu peut le faire et ainsi savoir,
- si l'auteur a utilisé une session licite pour l'écrire et le transmettre au serveur,
- ou s'il s'agit d'un contenu émis par une session opérée par un logiciel pirate (et maladroit).

Dans ce dernier cas le découvreur de ce problème peut lancer une alerte au modérateur général de l'instance en lui transmettant :
- le contenu en clair mais ré-encrypté pour le modérateur,
- la référence du contenu dont le numéro de compte de l'auteur.

Le modérateur peut vérifier :
- si cette référence est réelle.
- si le digest enregistré est bien celui qu'il obtient lui en le recalculant sur le texte en clair.
- en conséquence si l'alerte est valide et qu'effectivement ce compte est indélicat.
- le cas échéant le compte peut être résilié.

>L'objectif est surtout de faire apparaître aux pirates que leurs tentatives de boguer l'application sont certes possibles mais sont détectables et leurs comptes repérables.  
>Un pirate qui s'introduit sur un forum a toujours la possibilité d'effacer ou de caviarder les notes sur lesquelles il peut écrire, et ceci par les moyens techniques les plus licites.

# Traces d'activités sur les comptes et forum
Il n'est pas naturel de savoir si un compte ou un forum est *actif* : 
- les opérations de mise à jour modifient la date-heure du dossier correspondant. On peut savoir quand s'est opéré la dernière mise à jour technique. Toutefois un échange par exemple écrit par C pour A, ne signifie pas pour autant que A est actif / vivant et pourtant le dossier a eu sa date-heure modifiée.
- la pure consultation ne laisse aucune trace dans les dossiers, elle est pourtant la marque d'une indéniable activité.

Des opérations techniques arbitraires sont définies pour signer une activité :
- **ouverture d'une session sur un compte** : ça a un sens indéniable dans l'application terminale (c'est du superflu pour les dossiers sur le serveur). Cette consultation est transformée en opération rien que pour signer la *date-heure de dernière visite d'un compte* par son titulaire.
- **accès à un forum** : sur l'application terminale c'est plus ou moins le moment ou l'onglet / page du forum est accédée.

L'application terminale a toujours facilement un concept *d'ouverture de session*: il est rare de laisser une page ouverte sur une application des jours entiers, elle est en général ouverte, puis fermée au plus quelques heures après, et rien n'empêche la session terminale de se considérer elle-même comme ré-ouverte si elle dure plus d'un temps raisonnable.

### Date-heures de dernières visites
Dès lors l'application terminale est conviée à invoquer des opérations :
- première visite d'un compte dans la session,
- première visite d'un forum dans la session.

Ceci permet d'enregistrer la date-heure de la session la plus récente ayant *visité* un compte ou un forum.

Ceci fournit :
- à un compte les date-heure de **sa** dernière visite,
    - à son compte, 
    - à chacun des forums dont il est participant.
- à un compte contrôleur,
   - la date de dernière visite d'un compte.
   - la date de dernière visite d'un forum (encore que ce ne soit guère utile).

## Notifications
L'objectif est d'allumer pour une session des signaux en face des conversations, notes, forums ... sur lesquels il s'est passé quelque chose d'intéressant pour la session depuis sa dernière visite.

Il faut,
- caractériser les événements intéressants pour un compte,
- savoir identifier quels comptes sont intéressés pour un forum,
- savoir remonter cette donnée en synthèse sur le dossier du compte.

A suivre.

# Cryptographie

## Éléments simplifiés de cryptographie
#### Octet : nombre de 0 à 255
Un grand nombre (dépassant 255) peut être représenté par une suite d'octets.  
Une chaîne de caractères est représentée par une suite d'octets : le codage standard UTF-8 définit comment chaque caractère est codé en 1 2 ou 3 octets.

#### Codage en Base 64 d'une suite d'octets
La conversion d'une suite d'octets en base 64 donne une suite de lettres et chiffres (`A-Z a-z 0-9`), des signes `+` et `/` et complétée par `==` ou `=` pour obtenir un nombre de caractères multiple de 4.  
Il faut 4 caractères pour coder 3 octets.

**Option URL** : `-` remplace `+` et `_` remplace `/` et il n'y a pas de remplissage `=` ou `==` à la fin.  De tels strings peuvent être clé de documents ou d'items, noms de fichier et apparaître dans les URLs. 

Tous les identifiants et noms sont en base 64 URL.

#### Cryptage symétrique AES-256
Une ***clé symétrique*** (AES-256) est générée **depuis une suite de 32 octets**. Elle  permet de crypter une suite d'octets (donc un texte) de n'importe quelle longueur et de décrypter la suite résultante avec la même clé pour ré-obtenir la suite initiale. Le cryptage / décryptage symétrique est rapide.

#### Cryptage asymétrique RSA-2048
Un ***couple asymétrique de clés*** (RSA-2048) (*publique* de cryptage / *privée* de décryptage) utilise deux clés de 256 octets : 
- on **crypte** avec la clé **publique** un texte court de longueur inférieure à 256 octets.
- on **décrypte** avec la clé **privée** du couple.

Depuis une clé RSA générée aléatoirement on exporte une forme dite JWK textuelle des clés publique et privées : l'importation de ces clés permet de reconstruire la clé RSA initiale.
- l'export de la clé publique est comme son nom l'indique publiable *en clair* sans restriction afin de permettre à n'importe qui de crypter un texte à destination du détenteur de la clé privée correspondante.
- l'export de la clé privée du couple de clés est *encrypté* par son propriétaire par le moyen de son choix de manière à être le seul à pourvoir décrypter un texte crypté par la clé publique correspondante.

On se sert d'un couple de clés publique/privée typiquement pour encrypter une clé symétrique à destination d'un unique destinataire et servir de cette clé pour échanger des textes de longueur quelconque.  
**Le cryptage / décryptage asymétrique est lent** et le **texte crypté a une longueur fixe de 256 octets** même quand le texte à crypter est plus court.

#### Hachage Java d'un string (court)
Cette fonction retourne un entier depuis un string : cet entier est ensuite converti en base 64 URL. Le nombre de collisions est élevé, non admissible pour s'assurer qu'on est en présence du même texte quand on est en présence de deux hachages identiques.

#### Hachage SHA-256 (long)
Le SHA-256 d'une suite d'octets est une suite de 32 octets tel qu'il est *très coûteux* (mais concevable pour des textes relativement courts de moins d'une quinzaine de caractères) de retrouver la suite d'octets originale depuis son digest.  
Le nombre de collisions est négligeable : deux textes différents ont, en pratique, deux digests différents. Le calcul est rapide.

#### Hachage BCRYPT d'un string
L'objectif du hachage BRCYPT est de rendre impossible de retrouver un string origine depuis son hachage : le calcul est, volontairement, très coûteux et ne peut pas être accéléré par des processeurs ad hoc. 
- une recherche par *force brute* (essai de toutes les combinaisons) est sans espoir dès que la longueur du texte excède une douzaine de caractères. 
- une recherche depuis des *dictionnaires de textes fréquemment utilisés* (les prénoms, noms de fleurs, 1234 ...) est sans espoir également si le texte est long et est brouillé par des séparateurs exotiques.

Le hachage BCRYPT d'un string ne traite que les 72 premiers octets de sa conversion en UTF-8 et retourne un string de 60 caractères qui :
- commence par la constante `salt` : `$2a$10$` + 22 caractères en base 64 spécifique de BCRYPT contenant des caractères `.` et `/`
- se termine par le `hash` de 31 caractères en base 64 spécifique de BCRYPT.   

Pour éviter ces séparateurs et raccourcir le résultat,
- on enlève les 29 premiers caractères (le `salt` considéré comme une constante du code),
- on remplace dans le `hash` les `.` par `-` et les `/` par `_` pour utiliser le même jeu de caractères qu'un base 64 URL.

Sur le serveur on vérifie la validité d'un hachage BCRYPT en vérifiant sa longueur (31) et les caractères employés (base 64 URL).

Pour obtenir une clé AES d'un hachage BCRYPT un vecteur de 32 octets est créé avec les 31 du `hash` et un 0 binaire à la fin.

## Noms, phrases secrètes et identifiants
### Normalisation / réduction d'un texte
La **normalisation** d'un texte consiste à y remplacer par un seul espace les `white space` consécutifs et en ignorant ceux de tête et de queue.

> White space : space, tab, form feed, line feed and other Unicode spaces.   
[ \f\n\r\t\v\u00a0\u1680\u180e\u2000\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]

La **réduction** d'un texte consiste à ne garder que 2 caractères sur 3 : le texte réduit est toujours ensuite haché par BCRYPT.

### Nom, nom réduit, initiales, numéro d'un compte
Le **nom** est la suite **normalisée** de caractères saisie par le titulaire à la création du compte.  Sa réduction est employée pour s'assurer que deux noms ne sont pas trop proches.

Les **initiales** sont la *mise en majuscule* du premier caractère de chaque mot du nom (découpé par les espaces et tiret) quand il en existe un équivalent *non accentué* de A-Z ou a-z (les initiales de `Jean élan` sont `JE`, celles de `jean 23` sont `J`.).

Le **nom suffixé** est le nom suivi de @ et de 12 caractères aléatoires en base 64 : dans l'application partout où il est mentionné un *nom* il est toujours *suffixé*. A l'affichage une session cliente enlève le suffixe.

Le numéro d'un compte **nc** est le **BCRYPT** de son **nom suffixé**. Par construction les numéros de compte ne sont pas réutilisés.

>Du fait que chaque fois que le *nom* d'un compte est donné il s'agit de *son nom suffixé* et qu'il est toujours accompagné du *numéro de son compte*, une session peut vérifier qu'un faux nom n'a pas été donné à la place du vrai par une session pirate(du moins de détecter cette anomalie) en recalculant le BCRYPT du nom suffixé et en le comparant avec le numéro de compte.

### Phrase secrète d'un compte
La phrase secrète d'un compte permet d'identifier et d'authentifier un compte :
- **identifier** : un seul compte correspond à cette phrase secrète.
- **authentifier** : c'est bien le titulaire du compte qui désigne ce compte.

Une phrase secrète est une suite de mots constitués chacun d'un chiffre de 1 à 9 suivi de 1 à N lettres de a à z.
- chaque lettre d'un mot est codée en un caractère dont la valeur décimale est le numéro d'ordre de la lettre dans l'alphabet (0 à 25) multiplié par le chiffre 1 à 9 de tête du mot + 32.
- la suite des mots donne un string inintelligible utilisant des caractères bizarres.

Elle n'existe *en clair* (si on peut dire !) que le temps court de sa saisie à l'intérieur de la mémoire d'une session terminale et en sort sous trois formes :
- `clé S` : la clé AES de cryptage de la clé `0` du compte obtenue depuis le BCrypt du string de la phrase secrète (complétée par un O, un BCRYPT a une longueur de 31, une clé AES de 32). **Elle ne sort jamais de la session**.
- `prB` : le BCRYPT du string réduit de la phrase secrète. `prB` est transmis au serveur pour identifier un compte et à la création pour vérifier qu'un autre compte n'a pas une phrase secrète voisine. 
- `prBD` : le SHA-256 de `prB` qui est enregistré dans la dossier du compte et permet au serveur de vérifier que le `prB` fourni par le terminal correspond bien à un compte.

>La phrase réduite est garantie unique afin d'empêcher de tomber par hasard (ou usage d'un robot) sur une phrase secrète enregistrée ouvrant un accès à un compte.  Ainsi il n'existe pas deux comptes ayant déclaré des phrases secrètes *trop proches*.  
>En supposant que le logiciel du serveur ait été substitué par une version pirate indélicate qui enregistrerait les requêtes en clair, et donc `prB`, une session non officielle pourrait se faire reconnaître comme disposant d'un compte authentifié vis à vis du serveur sans disposer de la phrase secrète. Elle ne serait toutefois pas capable de décrypter la clé *mère* S obtenue depuis la phrase secrète originale connue du seul titulaire et serait incapable de déchiffre la moindre donnée du compte.

>SHA-256 est considéré comme fragile à une attaque par force brute : c'est vrai pour un mot de passe de faible longueur alors qu'ici il s'agit des 31 caractères sortis d'un BCRYPT. Il est impossible de retrouver `prB` depuis `prBD` par attaque de force brute.  
>L'attaque du BCRYPT sur un minimum de 20 caractères, sachant qu'en plus les mots correspondants sont préfixés d'un chiffre aléatoire vis à vis des dictionnaires, semble vouée à l'échec d'après la littérature spécialisée.

*Pratique interdite* : la clé S et `prB` pourraient être conservés en données persistantes dans le browser cryptée par un code PIN numérique de 4 caractères : un utilisateur tentant une connexion rapide a alors 3 essais sinon la mémorisation est détruite. Un simple passage du browser en debug exhibe cette valeur cryptée permet de la copier puis de tester les 10000 clés AES possibles (ce qui va être court).

### Phrase secrète d'administration
C'est une phrase secrète dont le `prBD` est enregistré dans la configuration de l'instance. Même le piratage de cette `prBD` (voire de `prB`) par un serveur indélicat ne sert à rien : c'est la clé S, qui n'est jamais sortie de la session et directement issue du string de la phrase secrète qui permet de décrypter les informations.

### Numéro long : forum
Un vecteur de 15 octets tirés au hasard est codé en base 64 URL de 20 caractères.

>Le numéro d'un compte se distingue de celui d'un forum par sa longueur (31 pour un numéro de compte, 20 pour un groupe).

### Numéro court : conversation / note
Un vecteur de 9 octets tirés au hasard est codé en base 64 URL de 12 caractères.

## Logique de cryptage
On distingue deux niveaux de protection par cryptage :
- le niveau 1 protège une information même au cas ou la base serait détournée.
- le niveau 2 protège l'information sur la base officielle mais si une clé a été conservée par une session qui y avait droit mais ne l'a plus et que la base est détournée, l'information peut être décryptée.

### Clé de cryptage d'un disque
Un disque virtuel a les clés suivantes générées à sa création :
- une clé symétrique SD du disque virtuel stockée cryptée aussi par la clé SA. Elle sert à crypter le commentaire associé au disque pour les comptes connaissant la phrase secrète d'administration.
- un couple de clé publique (stockée en clair) / privée stockée cryptée par la clé SD.

Lorsqu'un compte connaissant la phrase d'administration ajoute un compte contrôleur à un disque virtuel, il lui transmet la clé SD cryptée par sa clé publique : le contrôleur connaît ainsi la clé SD et la clé privée du disque.

A sa validation par la certification de son nom par un compte certificateur, ce dernier lui transmet à la fois l'identifiant du disque sur lequel il s'installe mais aussi la clé de cryptage de ce disque. Si le compte certificateur est contrôleur de plusieurs disques, il peut choisir sur lequel des disques qu'il contrôle le nouveau compte sera installé.

##### Changement de la phrase secrète d'administration
La session effectuant cette opération connaît l'ancienne et la nouvelle clé. Pour chaque disque DV, elle décrypte la clé SD et la ré-encrypte par la nouvelle.

##### Ouverture d'une conversation entre un contrôleur de disque et un compte
Sur initiative du contrôleur :
- génération de la clé symétrique dédiée à cette conversation.
- cryptée par SD pour l'exemplaire du disque.
- cryptée par la clé publique du compte (qui la ré-encryptera par sa clé 0).

Sur initiative du compte :
- génération de la clé symétrique dédiée à cette conversation.
- cryptée par la clé publique du disque pour l'exemplaire du disque. Elle sera ré-encryptée par la clé SD par le premier contrôleur traitant la conversation.
- cryptée par la clé 0 du compte.

Les échanges sont ensuite cryptés par la clé de la conversation.

>Tout compte ayant été un jour contrôleur d'un disque peut techniquement conserver sa clé SD et est potentiellement capable de décrypter les conversations associées à ce disque depuis une base détournée. Ce n'est pas assez critique au point de gérer des clés changeantes et historisées. La protection du commentaire à propos du disque et des conversations à propos de ce disque est de niveau 2.

### Clés d'un compte
**La clé 0** est la clé majeure de cryptage d'un compte : 
- elle est générée aléatoirement dans la session terminale à la création du compte.
- elle est immuable au cours de la vie du compte. 
- elle est stockée dans l'entête du dossier du compte cryptée par la clé S tirée de la phrase secrète. Changer de phrase secrète n'a pour seule conséquence que le ré-encryptage de la clé 0.

**La clé 1** sert à crypter les noms suffixés des certificateurs dans le certificat d'identité du compte et le nom du compte lui-même :
- elle est générée aléatoirement dans la session terminale à la création du compte.
- elle est immuable au cours de la vie du compte.
- le nom du compte est mémorisé crypté par cette clé 1 dans l'entête du compte.
- elle est stockée dans l'entête du compte cryptée par la clé 0 du compte.

**La clé D** est la clé de la conversation courante avec le / les contrôleurs du disque virtuel du compte : elle est inexistante s'il n'y a pas de conversation en cours. 

**Le couple de clés PUB/PRIV asymétriques** publique / privée :
- il est généré à la création du compte dans la session terminale.
- il est immuable au cours de la vie du compte. 
- il est stocké dans l'entête du dossier du compte cryptée par la clé 0 du compte pour le clé privée et en clair pour la clé publique.
- il permet à un autre compte de crypter une clé entre eux par la clé publique. Ultérieurement dès que possible une session terminale du compte trouvant un tel cryptage (il est long), ré-encrypte la clé par la clé 0 (le résultat étant plus court).

**Les clés des CV** : à chaque fois qu'un CV #n est modifié,
- une nouvelle clé de cryptage de ce CV #n est générée aléatoirement dans la session terminale.
- elle est stockée cryptée par la clé 0 du compte.
- pour chaque forum auquel le compte participe, elle est cryptée par la clé d'invitation du forum.
- pour chaque contact de confiance inscrit en répertoire, elle est cryptée par la clé mutuelle du compte avec son contact.
- la mise à jour d'un CV #n induit pour la session le ré-encryptage de sa (nouvelle) clé par la clé 0 du compte et un nombre plus ou moins important d'autres clés de forum et mutuelle de contact. En conséquence, même si une session avait gardé une clé de CV #n elle ne pourra en obtenir les mises à jour ultérieures que si elle en a toujours le droit.

### Clé mutuelle entre deux comptes A et C
Quand un compte A inscrit une nouvelle entrée de son répertoire pour un compte C,
- *soit le compte C a une entrée existante pour le compte A* : 
    - la clé mutuelle entre les deux comptes existe.
    - elle y est cryptée par la clé publique de A (ou sa clé 0 selon l'historique entre les deux comptes) et par la clé 0 de C.
    - l'entrée du compte C dans le répertoire de A enregistre deux fois cette clé : a) cryptée par la clé 0 de A, b) cryptée par la clé 0 de C.
- *soit le compte C n'a pas d'entrée existante pour le compte A* : la session terminale génère aléatoirement une nouvelle clé mutuelle qui est enregistrée dans l'entrée de C du répertoire de A : a) cryptée par la clé 0 de A, b) cryptée par la clé publique de C.

De ce fait cette clé mutuelle reste la même tant qu'au moins l'une des deux entrées (C dans le répertoire de A et A dans le répertoire de C) existe.  
Elle est utilisée pour crypter :
- dans les deux entrées, les noms de A et C.
- dans chaque entrée, la clé du CV #n proposé à l'autre en cas de confiance.
- dans l'entrée C du répertoire de A, la clé 1 de A quand A demande à C de lui certifier son nom. Elle est effacée si A renonce à cette demande de certification ou quand C a certifié le nom de A.
- pour crypter la conversation entre les comptes.

### Clés d'un forum
Un forum a :
- une clé dite I disponible aux invités au forum. Ceux-ci pouvant refuser ou être exclu, peuvent avoir conserver cette clé obtenu du temps où ils étaient invités. Les conversation ou notes cryptées par la clé I sont donc de protection de niveau 2 (décryptable depuis une base détournée par un compte ayant eu un jour un accès licite comme invité).
- une suite de clés Fi : une nouvelle clé d'indice i est ajoutée lorsqu'un participant a été exclu, cette clé étant communiquée cryptée par la clé P de chaque participant. Les notes et échanges de conversation mises à jour / écrits après une exclusion procède à cette génération. Les notes et conversations cryptées par Fi ont une protection de niveau 1.

# Purgatoire

## Modes synchronisé et local

**Le mode local permet d'exécuter sur un terminal l'application cliente sans connexion internet.**  
Il faut préalablement avoir utilisé sur le même terminal le mode synchronisé pour un compte donné : le mode local ne permet d'accéder qu'aux comptes ayant été préalablement synchronisés en mode synchronisé et aux seuls forums ayant explicitement été déclarés comme devant être synchronisés sur ce terminal (pour une question de volume).

Pour un forum synchronisé,
- toutes les notes et les fil de discussion le sont ;
- seuls les fichiers explicitement cités pour ce terminal le sont (toujours pour une question de volume).

### Actions possibles en mode synchronisé
Toutes les actions faisables en mode *cloud* sont possibles. Il est possible de plus de décider de ne plus synchroniser le compte sur lequel le login a été fait et de détruire la mémoire correspondante, voire toute la mémoire y compris celles des autres comptes synchronisés.  
Le comportement est celui de l'application en mode cloud pur, à la différence près d'une sauvegarde locale de données cryptées, non perceptible à l'utilisateur (tant qu'il y a assez de volume disponible).

### Actions possibles en mode local
**Les données sont restreintes à celles d'un compte et de ceux des forums qu'il peut accéder et qui ont été déclarés accessibles sur cet appareil.**  
*Les consultations des autres comptes sont impossibles* : ceci ne concerne en pratique que l'impossibilité de lire, la carte de visite, la photo, la liste des garants ou les forums en commun d'une personne dont on a le nom à l'écran.

En mode local il est possible d'auto détruire sa propre mémoire ainsi que celle des autres comptes mémorisés localement sur l'appareil. C'est normal, l'appareil appartient à celui qui l'utilise : si l'appareil est partagé, les propriétaires doivent être unis et cohérents entre eux comme s'ils n'en étaient qu'un (ou utiliser des sessions distinctes du browser).

>La mémoire *locale* est celle **d'un utilisateur du browser** pour **une session de l'OS hôte**. Si plusieurs personnes autonomes utilisent le même appareil, elles ont autant de login sur l'appareil hôte et à tout le moins autant de *profiles* différents au sens de Chrome ou de Firefox.

Les actions de mise à jour sont restreintes aux suivantes :
- **création d'une note ou d'un fichier** (de taille réduite) ;
- **modification / suppression d'une note ou fichier** ;
- **ajout d'items** sur un fil de conversation (y compris la création d'un nouveau fil de conversation).

Les modifications sont enregistrées localement ***sous réserve de validation à la prochaine synchronisation***.

#### Prochaine resynchronisation
Si le compte a été fermé, la mémoire locale reste intacte (au moins elle peut être sauvegardée) mais rien ne sera mis à jour sur le cloud.

***La resynchronisation consiste avant toute chose à mettre à jour la mémoire locale avec tout ce qui a pu changé sur le cloud***.  
Se pose ***ensuite*** la question de répercuter les mises à jour *en attente* en local sur le cloud.

En cas d'insuffisance de quotas n'importe laquelle des mises à jour peut être refusée.

**Les nouveaux items de conversation sont inscrits sur le cloud et les créations de note ou de fichier sont répercutées sur le cloud**, sauf cas bien improbable mais possible, où le statut du compte aurait été rétrogradé de *rédacteur* à *lecteur* depuis la synchronisation précédente pour le forum concerné.

***Les modifications de notes et fichiers posent problème en cas de collision, c'est à dire de mise à jour parallèle sur le serveur.***  
Prenons l'exemple d'une note :
- au moment de la dernière synchronisation elle affichait `t1` comme date-heure de dernière mise à jour ;
- au moment de la resynchronisation elle affiche `t3` (> `t1`).

*Que faut-il faire ?*  
La note a été resynchronisée en local avec la date-heure `t3` mais elle a été mise à jour (ou supprimée) en `t2` sur l'appareil en mode local.  
Manifestement seul l'auteur de cette mise à jour peut décider de ce qu'il faut faire :
- les deux versions `t3` (validée, celle du serveur) et `t2` (en attente, celle du mode local) sont visibles ;
- le titulaire du compte recopie dans la note validée (celle en `t3`), les parties modifiées dans `t2` et jugées encore pertinentes ;
- en validant la réconciliation, l'auteur de la mise à jour enregistre la nouvelle version à `t4` et détruit la version `t2` en attente locale ;
- *s'il s'agit d'une suppression* et non d'une mise à jour c'est plus simple : l'auteur doit valider si la suppression est toujours pertinente au vu d'une version dont il n'avait pas connaissance quand il a décidé cette suppression ;
- pour un fichier c'est un peu différent : fusionner deux textes n'a de sens que s'il s'agit de textes (et non de photos ou de clips ...) et de plus en format révisable. En gros l'auteur décide soit d'accepter la version `t3` actuelle, soit de l'écraser par sa version `t2` locale ou toute autre.
