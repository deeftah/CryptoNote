# Un réseau a-social


Une personne ayant un **compte** dans une *instance* de cette application la perçoit d'abord comme *un espace de stockage arborescente classique* où les feuilles sont des **notes** (au lieu d'être exactement des fichiers).  
Les *répertoires racines* qui lui apparaissent sont :
- son répertoire **home** : strictement personnel, il est totalement invisible et inaccessible aux autres comptes.
- des répertoires **forums** sur lesquels il partage des notes avec d'autres comptes.

##### Note
Le contenu d'une **note** est constitué,
- **d'un sujet / résumé** textuel court (sans formatage).
- **d'une liste de mots clés** symbolisant les sujets généraux auxquels la note se rapporte.
- **d'une vignette** facultative, aperçu d'une photo ou d'un clip joint, photo d'identité de l'auteur, icône...
- **d'un texte** facultatif pouvant comporter jusqu'à quelques milliers de caractères lisible sous une présentation simple agréable (niveaux de titre, gras / italique, liste à puces ...).
- **de pièces jointes** facultatives de n'importe quel type : photo, clip vidéo, etc. Chaque pièce y porte un nom court spécifique de la note et peut avoir une vignette.

Une note possède un *identifiant unique universel* et peut avoir selon l'option choisie à sa création, a) une version unique (comme un e-mail, un SMS ...), b) plusieurs versions successives (comme tout document révisable).

Chaque version (première et suivantes) a :
- sa **date-heure**, 
- son **auteur**,
- son **contenu** propre : sujet, mots clés, texte, vignette et liste de pièces jointes.
- sa **clé de cryptage** : sans cette clé le contenu n'est pas lisible.
- sa **signature par son auteur** (un compte) : un procédé cryptographique permet à un lecteur (ou qu'il soit dans le monde, même non connecté) de vérifier à partir de la signature :
    - que le contenu de la note a bien été signé par l'auteur déclaré.
    - quand il dispose de la clé de cryptage, que le contenu n'a pas été falsifié et est bien celui écrit par son auteur.

Plusieurs notes, et plusieurs versions d'une note, peuvent référencer des pièces jointes communes. Chaque pièce jointe est également signée par son auteur.

***Paths* d'une note**  
Une note est disponible dans l'arbre de stockage d'un répertoire **home** ou d'un *forum*, son path est le nom d'accès dans ce répertoire racine, par exemple :  
`/Banques/Relevés-2017/janvier`  
Une note donnée peut être rangée à plus d'un endroit différent, avoir plus d'un path, désignant toutefois la même note. La note ci-dessus pourrait avoir un second path :  
`/A vérifier/relevé banque-2018-01`  
Quand une note est révisable on ne garde généralement que la dernière version. Mais chaque version peut avoir *son* path et être le cas échéant rangée dans un répertoire différent des autres versions conservées : par exemple les versions antérieures peuvent être gardées dans un sous-répertoire `../Historique/...`.

>**Remarque** : les *répertoires* comme `/Banques/Relevés-2017 `n'ont pas vraiment d'existence. Ils ne sont que des éléments de regroupements graphiques et si aucune note n'a un path commençant par `/Banques/Relevés-2017` ce répertoire n'apparaît pas.

>Une note copiée d'un forum à un autre est stockée en deux exemplaires, occupe deux fois du volume, mais a le même identifiant universel et sa version de date-heure `d1` y est strictement identique dans les deux forums. Elle peut être détruite dans l'un sans impact sur l'autre.  
>Une version d'une note exportée à l'autre bout de la planète dans un fichier local d'un poste portable déconnecté, aura le même identifiant universel et le même contenu, sauf s'il a été falsifié volontairement, ce que la signature et la connaissance de sa clé permet de détecter.

#### Forum
Le répertoire **home** ne pose guère de problème de confidentialité : seul le compte y a accès, il est crypté et seul le compte titulaire en possède la clé.  
Le répertoire d'un **forum** pose d'avantage de questions : plusieurs comptes ont accès à ce répertoire :
- les notes stockées peuvent être lues par tous les comptes ayant accès au forum.
- ces comptes peuvent y écrire des notes, y copier / coller des notes de / vers leur *home* ou d'autres *forums* auxquels ils ont accès.

>Toutes ces notes sont cryptées et seuls les comptes ayant accès au forum ont accès à leurs clés respectives.

**Participants**  
A la racine de l'espace d'un forum, figure un répertoire **Participants** qui contient un *sous répertoire spécial pour chaque compte* ayant accès au forum ayant pour **nom** celui du compte et auquel est attachée une petite fiche d'information donnant :
- son numéro de compte.
- une photo d'identité (facultative) choisie par le titulaire du compte.
- **son certificat d'identité**, à savoir la liste des numéros des comptes *avec leur nom* ayant certifié qu'il connaissait le titulaire du compte et que ce nom est bien représentatif.

Dans le répertoire d'un participant on va en général trouver une courte *note d'auto-présentation* qu'il a eu la courtoisie de montrer lorsqu'il a été invité sur ce forum.  
D'autres notes peuvent aussi être rangées à cet endroit, typiquement celles directement relatives à ce compte dans ce forum : le choix du classement des notes est libre et dépend des conventions librement consenties dans le forum.

**Confiance entre les participants**  
Inviter un nouveau participant à partager l'espace d'un forum pose deux problèmes de confiance :
- *à l'invité* : quel est l'objet de ce forum, comment fonctionne-t-il et surtout qui va-t-il y rencontrer et peut-il avoir confiance dans les participants actuels ?
- *aux participants actuels* : quel est ce nouvel invité, qui le connaît, qui certifie son nom, peuvent-ils accepter de dévoiler leurs notes à ce nouvel arrivant ?

Pour tenter de résoudre ce double problème l'invitation d'un nouvel arrivant passe par deux étapes :
- **l'invitation** : l'invité reçoit une clé qui lui permet de lire les répertoires **Participants** et **A propos** et ainsi de prendre connaissance de ce qu'est le forum et qui sont ses participants. L'invité peut déposer dans son répertoire dans **Participants** une ou des notes d'auto-présentation (que nul autre ne peut ni altérer ni détruire). L'invité peut également renoncer et disparaître du forum.
- **la confirmation** par les autres participants : au vu de cette information apportée par l'invité, de son certificat d'identité et des avis des uns et des autres, le nouvel invité peut être confirmé (ou refusé et disparaître).

**Notes accessibles aux invités et aux participants**  
Par défaut les notes ne sont accessibles qu'aux participants confirmés qui seuls en ont la clé de cryptage.  
Mais certaines notes ont besoin d'être consultées par les invités non confirmés :
- les notes de présentation et d'auto-présentation des participants, justement pour que les invités se fassent une idée de avec qui ils vont partager des notes.
- les notes expliquant l'objet du forum, ses règles éthiques, ses conventions d'organisation du rangement des notes ...

Ces notes portent un indicateur particulier dans le forum qui précise qu'étant cryptées par la clé d'invitation du forum, elles sont lisibles par les invités non confirmés.

>*Les invités* ne peuvent écrire des notes que dans leur propre répertoire sous **Participants**.

Une note est :
- lisible par tous les participants.
- si elle est révisable, par défaut elle ne peut l'être que par son créateur. Celui-ci a toutefois la possibilité *d'ajouter* des participants à la liste des auteurs autorisés et qui dès lors ont droit de création d'une nouvelle version de la note.
- par défaut quand une note est révisable, sa nouvelle version se substitue à la précédente, mais des options sont possibles comme :
    - garder la version antérieure.
    - mettre à la poubelle la version antérieure (ce qui permet de l'en ressortir).
- une note peut être mise à la poubelle par son auteur, ou l'un de ses auteurs listés et peut en être ressortie pendant un certain temps. La poubelle est vidée périodiquement de ses éléments les plus anciens.

**Fils d'actualité**
Chaque forum a de un à 10 fils d'actualité :
- quand une note est mise à jour / créée / mise à la poubelle / ressorti de la poubelle elle peut être signalée dans un des fils (ce n'est pas obligatoire).
- chaque est simplement une liste de chronologique d'événements relatif à une note.
- un fil d'actualité ne conserve que des événements récents (1 mois) et au plus 50.
- vis à vis d'une même note, l'événement le plus récent est conservé dans le fil (plusieurs mises à jour se confondent en la plus récente).

**Décisions importantes dans un forum**
On peut classer les décisions importantes en plusieurs catégories :
- (p) à propos d'un participant : 
    - confirmation de son invitation.
    - confirmation de sa résiliation.
- (c) à propos de l'existence même du forum : décision de clore ou non le forum.
- (g) à propos de la gouvernance du forum : changer son mode de gouvernance.
- (q) à propos de gestion quotidienne :
    - acquisition d'un crédit / transfert de crédit vers un autre forum ou un compte.
    - mise à la corbeille de notes obsolètes dont le ou les auteurs sont résiliés ou inactifs.
- (n) approbation ou non d'une note (sans opération associée).

Le chapitre **Gouvernance des forums** traite de comment ces décisions sont prises selon le profil de gouvernance en cours :
- **autoritaire unique ou multiple** : chaque membre du bureau a pouvoir de décision.
- **démocratique par délégation** : les décisions de gestion sont prises par consentement (absence d'opposition formelle) ou vote majoritaire des membres du bureau.
- **démocratique directe** : les décisions de gestion sont prises par consentement (absence d'opposition formelle) ou vote majoritaire des participants.

**Sondages**  
Il peut être intéressant de procéder à un sondage sur une décision à prendre ou le contenu d'une note. Selon les règles de gouvernance en cours, certaines opérations sont soumises à l'obligation de suivre l'avis d'un sondage à propos de la décision.  
Les sondages sont listés pour un forum.
- un sondage peut passer par une phase d'ajustement de son objet. Par exemple un sondage de modification de la gouvernance peut nécessiter d'ajuster certains paramètres, la composition du bureau proposé, etc.
- le sondage est ouvert.
- le sondage est clos, soit parce qu'une décision s'est dégagée avant sa limite, soit parce que son initiateur y renonce, soit parce qu'il a dépassé sa limite. Son résultat est proclamé.

Le passage d'un sondage a l'une de ces phases à une autre est un événement qui est notifié dans un des fils d'actualité au choix de l'initiateur du sondage avec toutefois des contraintes :
- les sondages à propos de la gouvernance, des invitations / résiliations des participants et de la clôture du forum sont toujours du fil principal (0).
- les autres sont laissés ouverts.

#### Compte
Chaque compte organise son espace de stockage de notes comme il l'entend. Un seul répertoire racine spécifique y apparaît toujours : **Contacts**.

Le répertoire racine **Contacts** a un sous répertoire pour chacun des autres comptes avec lequel le compte est, ou a été, en relation plus ou moins proche.
- **le nom de ce répertoire est celui du compte** quand il est connu : il peut temporairement ne pas l'être encore.
- une petite fiche d'information y est associée avec :
    - son numéro de compte.
    - sa photo d'identité (facultative).
    - son certificat d'identité quand il est connu.
    - quelques autres informations facultatives qui seront détaillées plus loin.

Le répertoire d'un contact contient habituellement une note d'auto-présentation communiquée par le contact mais aussi d'autres notes rangées par le titulaire du compte à propos de ce contact (c'est libre).

**Échanges**  
Le sous répertoire d'un contact contient toujours un sous-répertoire particulier **Échanges** qui représente le fil historique des échanges de notes entre le titulaire du compte et son contact. Les notes qui y apparaissent ici ne sont toutefois pas complètes mais ne sont que *l'aperçu d'une note complète* indiquant :
- si elle est *reçue* (c'est à dire a été présentée par le contact) ou *émise* (c'est à dire proposée par le titulaire à son contact).
- sa *date-heure*.
- son *sujet*. 
- quelques indicateurs sur la présence et la taille des autres éléments de contenu.

>Si la note est très courte et n'a qu'un sujet ... l'aperçu est la note elle-même.

En présence de l'aperçu d'une note proposée par un de ses contacts, le titulaire du compte :
- **obtenir une copie de la note complète** correspondante en remplacement de l'aperçu (optionnellement avec sa pièce jointe) et si souhaité lui donner un second path pour la voir rangée aussi à l'endroit approprié.
- **effacer l'aperçu** qui ne l'intéresse pas ou plus.
- ne rien faire : l'aperçu reste en attente.

>Cet espace d'échanges ne sera ainsi pas saturé par des volumes importants de notes pas toujours souhaitées, l'espace d'un compte reste maîtrisable par son titulaire.

L'aperçu d'une note proposée par A à C est double : chez A il figure comme une note proposée à C et chez C celui d'une note proposée par A. Les deux sont strictement synchronisés à la création, mais plus tard,
- A a pu détruire la note proposée à C : si C ne l'a pas obtenu avant, il ne l'obtiendra plus. Une date-heure minimale de conservation est spécifiée pour palier, en général, à cette difficulté.
- A comme C peuvent effacer de leur côté l'aperçu qui les encombre.

## L'application : instances et logiciels
Toute organisation / association / société ... (sous condition d'en payer les frais d'hébergement / administration technique) peut déployer *sa propre instance* qui lui sera privée et aura ses propres règles de gouvernance et de déontologie. Une instance est identifiée par l'URL de son serveur central.  
Les instances déployées sont étanches les unes des autres : 
- il n'est pas possible de savoir combien ont été déployées et par qui elles sont utilisées. 
- les comptes des personnes et les forums sont propres à chaque instance.
- la suite du document ne considère qu'une instance.

L'application fonctionne par la collaboration de plusieurs logiciels :
- ***une application serveur*** s'exécute sur un pool de serveurs du *cloud* disposant d'un espace de stockage de données persistant et sécurisé (base de données ...) : l'URL de ce pool est l'identifiant de l'instance ;
- ***une application browser*** s'exécute sur tous les terminaux  (mobiles / tablettes / PC ...) supportant un browser compatible et pouvant y disposer, facultativement, d'un espace de stockage persistant réservé à cette application et privé pour chaque utilisateur du browser. Cette application peut avoir des sessions de travail :
    - ***en mode incognito*** : rien n'est mémorisé sur le terminal utilisé, les données proviennent du cloud et les mises à jour ne se font que sur le cloud ;
    - ***en mode synchronisé*** : l'espace local de stockage du browser est maintenu synchronisé avec le cloud pour le sous ensemble des données qui a été souhaité comme tel par l'utilisateur spécifiquement pour ce terminal ;
    - ***en mode avion*** : l'application s'exécute sans accès réseau sur les seules données stockées localement sur le terminal à l'occasion d'une session antérieure synchronisée.
- ***une application utilitaire*** peut s'exécuter sur un terminal sous Linux / Windows / MacOs afin d'effectuer des sorties des données, en clair ou crypté, sur un répertoire du système de fichiers local.

>Une personne disposant de plusieurs terminaux *peut* avoir localement sur chacun une copie partielle des données qui l'intéresse et utiliser le mécanisme des sessions en mode synchronisé pour les conserver au même niveau sur tous ses terminaux et pouvoir travailler le cas échéant en mode local sur chacun.

# Un réseau a-social et crypté
Les personnes ayant un **compte** dans une *instance* de cette application ont certes des notes strictement personnelles mais l'effet de réseau leur permet aussi d'en partager avec d'autres comptes,
- **en contrôlant avec qui**,
- **en étant assuré de la non accessibilité des informations intelligibles**, même en cas d'écoute intempestive du réseau comme de détournement des données stockées dans le cloud ce que, par construction même, aucun réseau social ne peut proposer.

Ce dernier point induit des contraintes de cryptages sur les terminaux. Toutes les données humainement signifiantes :
- sont ***cryptés*** sur les terminaux, 
- transitent ***cryptées sur le réseau***, 
- sont ***stockées cryptées*** sur les serveurs du cloud comme dans les mémoires des terminaux (en modes synchronisé et avion). 
- ne peuvent être ***décryptées*** que dans l'application qui s'exécute sur les terminaux des titulaires des comptes.

>Le détournement de données, que ce soit la base de données du serveur comme celles locales des terminaux ou de celles transitant sur les réseaux, est inexploitable : les données y sont indéchiffrables.

>Aucune utilisation *commerciale* des données n'est possible puisque celles signifiantes sont toutes indéchiffrables, même **et surtout** pour le prestataire de l'application et ses hébergeurs.  
>Le *business model* d'un réseau a-social peut être basé sur une acquisition par des organisations, le sponsoring d'entreprises désintéressées, mais jamais par la perspective de publicité ciblée (à la limite de publicité totalement aveugle qui n'est prisée par aucun annonceur).

**Aucun répertoire central ne peut être constitué, il ne contiendrait que des données inintelligibles** : il est impossible de chercher un compte ou un forum dans une instance de l'application en fonction de critères comme les noms, intitulés des forums, centres d'intérêt, mots clés ...

>On ne s'y connaît que par cooptation, contacts directs et rencontres sur des forum.

## Compte
A sa création un compte reçoit une identification universelle unique et un enregistrement dit *Ticket public* qui contient les éléments cryptographiques nécessaires. Ce *Ticket public* est comme son, nom l'indique, public, peut être copié sans restriction et est immuable pour toute la vie du compte, et même après pour authentifier, après sa résiliation de son instance, les notes signées par un compte.

**Un compte est identifié par un numéro de compte** : c'est un *identifiant universel unique* est calculable depuis son Ticket public. Ce dernier est généré hors de toute instance.

>Cette universalité rend *possible* l'exportation du **home** d'un compte d'une instance (sans ses **Contacts**) et son importation dans une autre instance, pour autant que celle-ci n'ait pas de collision sur le nom avec un compte y étant déjà enregistré. D'où l'intérêt de se choisir un nom long (*`Edward Kennedy Ellington dit Duke, Washington D.C. USA`* plutôt que *`The Duke`*).

#### Phrase secrète d'un compte
Le ticket public d'un compte contient en particulier des clés cryptographiques privées, cryptées, qui ne peuvent être décryptées que par son titulaire en utilisant sa **phrase secrète**.  
La connexion à un compte ne requiert que d'en connaître ***sa phrase secrète*** mémorisée nulle part dans l'application.

**L'oubli de la phrase secrète d'un compte le rend irrémédiablement indéchiffrable, inutilisable** : les clés de cryptage techniques permettant de crypter et décrypter les informations sur le terminal sont elles-mêmes, directement ou indirectement, cryptées par la phrase secrète du compte.  
Aucun compte privilégié n'a la possibilité technologique de réinitialiser une phrase secrète perdue : les données restent cryptées par l'ancienne phrase.

La phrase secrète d'un compte peut être changée à condition de pouvoir fournir la phrase actuelle.

Dans une instance, les comptes ont des phrases secrètes différentes, un dispositif interdit des *présomptions de ressemblance* entre phrases secrètes.

##### Nom immuable d'un compte
**Un compte a un nom** que son titulaire lui a attribué à sa création : ce nom est **immuable**, ne peut plus être changé, est **unique** dans l'instance de l'application où il a été créé et un mécanisme interdit des *présomptions de ressemblance* entre noms (dans l'instance).

Au fil du temps, le nom d'un compte apparaît à d'autres comptes : après avoir accordé leur confiance à un nom, ils sont assurés qu'aucune usurpation d'identité ou réemploi de nom n'est possible.

>Il est impossible de deviner la liste des noms des comptes connus dans une instance.

>La résiliation d'un compte dans une instance laisse une trace indélébile qui bloque l'utilisation d'un nom proche : la ré-importation du **home** exporté d'un compte résilié exigerait qu'elle se fasse avec exactement le même nom (crypté dans le Ticket public par une clé impossible à deviner).

##### Certificat d'identité d'un compte
*Un compte a son identité certifiée par un ou plusieurs autres comptes* : en certifiant l'identité d'un compte A, un compte C ne fait qu'affirmer que le nom du compte A est bien représentatif de la personne qui en est titulaire.  
Ceci ne signifie rien en termes d'amitié ou de convergence de vue, seulement une affirmation sur l'adéquation entre le nom présenté et la personne physique correspondante.

>**Le certificat d'identité d'un compte** est la liste des comptes ayant certifié son identité avec pour chacun :
>- *son numéro de compte*.
>- *son nom* (si la certification n'a pas été résiliée).
>- la date-heure de début certification (et celle de fin si elle a été résiliée).
>- le certificat d'identité des comptes certificateurs n'y figurent pas : accéder au certificat d'identité d'un compte donne la liste des noms des certificateurs mais pas ceux des certificateurs des certificateurs.

#### Processus de création d'un compte
Un compte se crée par auto-déclaration par son titulaire :
- **il doit avoir trouvé un *parrain*** ayant un compte dans l'instance qui accepte :
    - de certifier son nom.
    - de lui communiquer une *phrase de contact* pour être reconnu en adressant sa demande de certification.
    - de lui prêter / donner un crédit minimal pour démarrer avant de pouvoir alimenter son compte.
- **il choisit un nom** : le serveur vérifie que s'il ne ressemble pas à un nom déjà déclaré dans l'instance.
- **il choisit une phrase secrète** : le serveur vérifie qu'elle ne ressemble pas à une phrase déjà déclarée dans l'instance.
- **il rédige une note d'auto présentation** pour son parrain et pour lui demander de bien vouloir certifier son nom.
- **il donne la *phrase de contact*** que lui a confié son parrain hors de l'application.
- le compte parrain voit dans ses **Échanges** l'aperçu de cette note d'auto présentation qui exprime le souhait du nouveau compte d'être certifié.
- le compte est en état *en création* et peut rester dans cet état au plus quelques jours en disposant d'un crédit de ressources très limité.

**Si le compte parrain accepte de certifier le nouveau compte** :
- les deux comptes figurent dans leurs **Contacts** respectifs comme **contacts de confiance**, c'est à dire que chacun peut connaître de l'autre son *nom* et son *certificat d'identité* et peuvent partager des notes.
- le nouveau compte devient *actif* et son crédit peut être rechargé (en général le parrain fait un cadeau de bienvenue ou au moins une avance remboursable).
- le compte parrain a un compte supplémentaire dans *sa liste des comptes certifiés* de son profil et le nouveau compte a le compte parrain dans sa liste *des comptes certificateurs* de son profil. 

**Si le compte parrain refuse de certifier le nouveau compte** :
- le nouveau compte reste en création : il n'a toujours pas de compte certificateur dans son certificat d'identité (toujours vide).
- il peut toutefois lire le contenu de la note explicative du refus écrite par celui qu'il espérait être son son parrain.
- son parrain espéré figure dans **Contacts** mais pas comme contact de confiance (et même peut-être sans nom).
- le nouveau compte doit chercher, rapidement, un autre parrain potentiel, obtenir de lui une phrase de contact et partager avec lui sa note de présentation demandant à être certifié par lui.
- au bout de quelques jours le compte en création est détruit automatiquement.

##### Profil d'un compte
C'est un enregistrement accessible au titulaire du compte qui donne :
- son numéro de compte.
- son nom.
- d'une ou de quelques photos d'identité préférées qu'il aura l'occasion d'afficher à ses contacts ou sur les forums auxquels il participe.
- la liste des certifications dont il a fait l'objet.
- la liste des comptes qu'il a certifiés.
- quelques phrases de contact. Une **phrases de contact** est garantie unique dans l'instance durant sa période de validité fixée par le compte a sa déclaration. Le compte peut ajouter des phrases, détruire ses phrases ou les prolonger.

##### Certificats d'administration et comptes auto-certifiés
Comme au départ il n'y a pas de compte dans l'instance, le premier compte à s'inscrire ne peut pas passer par la procédure normale puisque personne ne peut certifier son identité.  
Il est possible de créer ou ou quelques comptes **auto-certifiés** à condition de disposer dans son navigateur d'un certificat dont le DN est l'un de ceux enregistrés dans la configuration de l'instance. En mode auto-certifié,
- la création du compte est immédiate.
- elle ne requiert pas qu'un autre compte certifie l'identité du compte créé.
- un crédit fixé dans la configuration est donné *gratuitement*.

Le premier compte créé dans une instance est par principe un compte auto-certifié, les suivants pouvant se faire certifier par le compte auto-certifié. Mis à part qu'ils se sont auto-certifiés à leur création, ces comptes sont comme les autres et peuvent d'ailleurs ultérieurement avoir leur identité certifiée par d'autres comptes.

#### Contacts connus d'un compte A
Un compte A peut avoir dans ses **Contacts** un compte C connu :
- par son seul numéro de compte.
- par son numéro de compte et son nom.
- par son numéro de compte, son nom et accès à son certificat d'identité. 

Un compte peut inscrire comme contact un coparticipant à un forum et ceci lui permet d'enregistrer son nom et son certificat d'identité.

Un compte peut déclarer sa confiance à un autre : si ce dernier n'a ni son nom, ni accès à son certificat d'identité, cette déclaration lui donne ces informations. La confiance n'est pas forcément réciproque.

Quand deux comptes sont **contacts de confiance réciproques** :
- l'un peut certifier l'identité de l'autre à condition qu'il le lui ait demandé.
- les deux peuvent se certifier leur identité mutuellement.
- l'un comme l'autre peut, quand il veut, cesser sa certification ou supprimer la certification que l'autre lui a accordé.

#### Partage d'une note
Un compte A peut partager une note N avec un autre compte C (voir les restrictions ci-après).
- C voit apparaître dans sa liste des notes un aperçu de la note nouvellement partagée :
    - qui la partage,
    - l'identifiant de la note,
    - son sujet et ses mots clés,
    - quelques informations comme la date-heure et si la note contient un texte (et de quelle taille) et une pièce jointe (quel type / quel taille).
- C peut décider :
    - de jeter cette note.
    - de la conserver en attente.
    - de l'obtenir et de la copier dans son espace : C a accès au contenu complet de la note.
    - l'obtention de la pièce jointe est une opération distincte qui ne peut se faire (et c'est facultatif) qu'après avoir obtenu la note.

**Oups !**  
A peut avoir un remord d'avoir partagé une note avec C :
- il peut tenter d'effacer l'aperçu reçu de C : mais ceci est sans effet si C a déjà obtenu la note (l'a déjà copiée dans son compte).
- il peut effacer la note dans son propre compte : c'est aussi sans effet si C a déjà obtenu la note, mais ceci supprime aussi ses pièces jointes et C ne les a peut-être pas encore obtenues.

#### Restriction au partage de notes
N'importe qui ne peut pas partager une note avec qui il veut, ni inviter n'importe qui à un forum en partageant une note d'invitation.  
Le réseau est a-social : un compte A ne peut partager une note avec un compte C ou inviter C à un forum que :
- soit **A et C participent à un même forum**.
- soit **C a accordé sa confiance à A** : à noter que si la réciproque n'est pas vrai, A peut partager une note avec C mais C ne peut pas déclarer un partage de note avec A.
- soit **A connaît une phrase de contact de C** en cours de validité,
    - soit confiée par C hors de l'application après un contact direct.
    - soit transmise par un compte tiers, de confiance de A et de C, ayant accepté de relayer la phrase de contact que C lui a fourni pour A.

#### Auto résiliation d'un compte
Elle se passe en deux temps :
- le compte est se met d'abord en état *zombie*.
- après un certain temps en état zombie, la destruction physique du compte intervient automatiquement. Les comptes et forums ayant un contact avec ce compte en sont informés.

Ceci laisse un délai de remord permettant au compte :
- de prolonger l'état zombie.
- de revenir à l'état actif normal.

## Les Forum
Un compte peut créer un forum dès qu'il souhaite partager des notes avec plusieurs autres comptes sur un espace commun.
- le forum ainsi créé a un identifiant universel unique tiré au hasard à sa création.
- **le créateur devient le premier participant** et membre du bureau du forum. Le type de gouvernance peut reconnaître un rôle de gestion privilégié aux membres du bureau ou rien du tout.
- **il peut y inviter des comptes** : tous ceux avec qui il peut partager une note.

Ultérieurement tout participant peut aussi lancer des invitations qui selon les règles de gouvernance du forum, seront ou non soumises à sondage / approbation des autres participants.

Partager des informations privées à plusieurs suppose d'avoir confiance en ceux avec qui ce partage s'établit. Cette acquisition de confiance joue autant pour les **invitants** (*puis-je avoir confiance dans cet invité ?*) que pour **l'invité** (*avec qui vais-je partager des informations ?*).

A sa création un forum a généré aléatoirement, a) une clé dite *d'invitation*, b) une première **clé F** :
- la **clé d'invitation** est transmise aux invités avant qu'ils ne soient acceptés en tant que participants confirmés.
- la **clé F** est transmise à un invité lors de sa confirmation comme participant.

Une opération peu usuelle permet de générer une nouvelle clé F qui va être utilisée pour crypter toutes les versions de notes déclarées postérieurement à cet ajout. Les anciennes ne sont pas ré-encryptées et sont lisibles en utilisant la clé F qui était en vigueur lors de leur création.

>Il peut arriver qu'un participant soit résilié pour être suspecté de divulgation inopportune de notes et/ou de nuisance sur les notes auxquels il a droit d'écriture. Si cet exclu a les moyens techniques de détourner la base centrale et si la clé F ne changeait jamais, il pourrait continuer de lire les contenus écrits postérieurement à son exclusion. L'ajout d'une nouvelle clé annihile cette possibilité.

#### Espace hiérarchique de stockage des notes
Une note d'un forum est inscrite dans son répertoire en un ou plusieurs endroits selon son / ses *paths*. Sa clé de cryptage y est cryptée,
- soit par la clé d'invitation : elle est lisible par les **invités** et les **participants confirmés**.
- soit par la clé F du forum : elle n'est lisible que par les **participants confirmés**.

#### Participants
Ce répertoire liste les invités / participants avec un sous répertoire pour chacun :
- son **nom**, son **certificat d'identité**, sa photo d'identité éventuelle, cryptés par la clé d'invitation du forum.
- son **statut** :
    - *invité en attente d'acceptation* : son certificat d'identité et son nom peuvent ne pas figurer encore s'il a été invité par une note accompagnée d'une phrase de contact.
    - *invité ayant accepté mais n'ayant pas encore été confirmé*. L'invité peut écrire des notes avec la seule clé transmise aux invités et c'est par ce moyen qu'il s'est auto-présenté aux autres participants.
    - *participant confirmé*.
    - *résiliation en discussion* : il retrouve les droits d'écriture restreint d'un invité mais continue d'avoir droit de lecture.
    - *résiliation*.

Le participant invitant du forum a enregistré l'invité comme participant avec la clé d'invitation qui lui permet :
- **de lire la liste des participants** avec leurs certificats d'identité (qui les certifient).
- **de lire la ou les notes lisibles aux invités** qui présentent l'objet du forum, ses règles internes : certaines de ces notes sont associées aux participants et présentent qui ils sont.

**L'invité découvre son invitation :**
- parce qu'à l'écran il apparaît un forum qu'il n'avait pas avant avec une icône de statut d'invité.
- éventuellement parce que l'invitant a partagé avec lui une note de bienvenue.

Ainsi un invité peut se forger une opinion sur le forum sur lequel il est invité et il peut choisir d'accepter cette invitation ou de la décliner.
- **s'il refuse l'invitation** il est marqué comme résilié.
- **S'il l'accepte**, son acceptation enregistre son nom et son certificat d'identité (s'ils n'étaient pas déjà connus) pour que les autres participants sachent si oui ou non il est souhaitable de confirmer cette participation.
    - **si oui** l'invité devenu *participant confirmé* dispose des clés F de cryptage lui permettant de lire aussi les notes cryptées pour les participants.
    - **si non**, l'invité est recalé. Il aura eu le temps de lire la liste des participants et la ou les quelques notes de présentation mais rien d'autre.

Dans les cas simples ou un invité est particulièrement et favorablement bien connu, *son invitation peut se faire avec approbation implicite* pour raccourcir le délai. Il reçoit dès l'invitation la clé d'invitation et les clés F permettant d'accéder à tout le forum, le serveur ne lui donnant effectivement accès qu'aux notes réservées aux invités tant que l'invitation n'a pas été acceptée par l'invité.

#### Restrictions d'accès d'une note d'un forum
Toute note a par défaut pour seul auteur, son créateur. Un auteur peut ajouter d'autres participants à la liste des auteurs, tous de même rang.  
Une note ne peut être détruite que par l'un de ses auteurs enregistrés et selon la gouvernance du forum par décision de gestion.

#### Vie d'un forum
La vie du forum est pour l'essentiel marquée par :
- **la lecture et l'écriture de notes** et de leurs pièces jointes.
- **des actes de gestion** :
    - invitation / confirmation / résiliation d'un participant.
    - mise à la poubelle de notes obsolètes en l'absence d'action de ses auteurs (qui ont pu quitter le forum).
    - transfert de crédits du forum.
    - clôture du forum.
- **des sondages organisés** :
    - à propos de ces actes de gestion.
    - à propos du changement de mode de gouvernance.
    - à propos d'une note (sans conséquence directe d'une exécution d'opération).

##### Clôture d'un forum
Elle se passe en deux temps :
- une annonce de clôture proche qui le place en état *zombie* laissant le temps aux participants de copier des contenus jugés utiles et ouvrant un délai de remord.
- une destruction effective un certain temps après.

Il est possible de réactiver un forum zombie quand les participants ont un remord ou qu'ils souhaitent prolonger l'état zombie.

La destruction effective supprime physiquement les données.

>**Remarque** : dans le cas le plus simple un forum s'ouvre instantanément sur demande d'un compte qui y invite immédiatement les contacts qu'il souhaite avec approbation par défaut. Une gouvernance par défaut s'installe, et les participants peuvent sur l'instant accepter l'invitation et participer à une conversation et s'échanger des notes.

### Schémas de gouvernance d'un forum
On distingue deux **options de gouvernance** :
- il existe **un bureau de N membres**, certaines décisions ne relevant que du bureau.
- il n'existe pas de bureau : N = 0, toutes les décisions dépendantes de l'ensemble des participants.

On définit **quatre groupes de décisions** :
- (p) : gestion des participants invitation / confirmation / résiliation.
- (c) : clôture du forum.
- (a) : autres opérations de gestion : distribution de crédit excédentaire (ou restant à la clôture), mise à la corbeille de notes abandonnées.
- (g) : changement de gouvernance. Le changement de gouvernance est à la fois :
    - *constituant* : proposition de nouvelles règles.
    - *électif* : nomination d'un bureau (s'il y en un) avec une liste de membres.
    - il est bien entendu possible de proposer un changement de gouvernance de schéma identique au précédent et dont la liste des membres du bureau n'a qu'un membre en plus ou en moins.

**Le schéma de gouvernance consiste à définir les règles de décomptes des votes pour chacun des 4 groupes de décisions.**
- les règles de décompte sont codifiées comme décrit ci-après.
- pour chaque instance la configuration de l'instance déclare une liste restreinte de règles autorisées et les groupes de décisions auxquels elles peuvent s'appliquer.

Une instance peut ainsi spécifier par exemple que le changement de gouvernance d'un forum ne peut relever que d'un *scrutin universel majoritaire des votants avec quorum de moitié des inscrits au moins*. 
- un forum n'aurait ainsi pas la possibilité de se définir des conditions dictatoriales de changement de gouvernance spécifiant qu'il n'est possible que sur décision du membre unique du bureau.
- à la limite une instance peut figer dans le marbre un choix unique pour chaque groupe de décision : un forum n'aurait plus que le loisir d'élire son bureau (si au moins un schéma de vote en prévoit un).

**Décomptes des voix :**
- `B U` : B des seuls membres du bureau, U universel (tous participants).
- `Q Q%` : quorum requis, Q (nombre absolu), Q% (pourcentage du nombre d'inscrits). Par défaut 0.
- `N I% V%`: nombre de voix pour requises, N (nombre absolu), N%I (pourcentage des inscrits), N%V (pourcentage des votants). Par défaut 50%V.
- `N1,N2` : la durée du scrutin de N1 heures, prolongeable une fois N2 heures. Par défaut 24,0.
- `C` : mode consensuel. Un seul vote *contre* vaut opposition à la décision. Par défaut non.
- `A` : clôture anticipée possible dès qu'un résultat de vote positif s'est dégagé sans attendre la fin du scrutin. Comme chacun peut changer son vote jusqu'au dernier moment, un scrutin acquis 5 minutes avant la clôture ne présume rien du résultat final. Avec l'option A, la décision favorable constatée avant la fin du scrutin clôt le scrutin : ceci permet des votes simples et rapides du genre il suffit que 2 membres du bureau soient d'accord. Par défaut non.

Exemples :
- *dictature* : bureau d'un membre et schéma de vote pour toutes les décisions : `B,1,1, 0,0,,A`. Un seul votant, décision acquise quand il voté *pour*.
- *deux membres du bureau (sauf avis contraire)*: applicables typiquement pour les opérations de gestion courantes (a) : `B,2,2,24,24,C,A`.
- *majoritaire classique* : applicables par exemple pour l'invitation de nouveaux participants (p): `U,25%,50%V,48,0`
- *majoritaire lourde* : applicables par exemple à la clôture d'un forum (c): `U,50%,66%V,96,0`

Le schéma de gouvernance est en conséquence constitué de :
- N : le nombre de membres du bureau.
- 4 schémas de décomptes des votes, applicables aux décision de type `p c a g`.

>Un forum est quand-même un lieu de partage / construction / confrontation d'idées  *fair* entre *gentlemen* (au féminin ça se dit comment *gentlewomen* ?) et toutes les décisions n'y ont pas la gravité de celles d'une cour d'assise.

# Notifications d'événements sur un compte ou des forum
#### Sur son home
Quand un compte ouvre son répertoire de **Contacts**, il voit des fanions sur tous les contacts qui lui ont proposé de partager des notes qu'il n'a pas marquées comme *vues*.  
#### Sur chacun des forums auxquels il participe
En ouvrant un des forums auquel il a accès, le compte peut lire les **fils d'actualité** avec un fanion sur tous les événements survenus après son dernier effacement des fanions.
- événement survenu sur une note,
- sondage passant d'une phase à l'autre.

Après avoir regardé ceux-ci, le compte peut **effacer les fanions** sur le forum. D'autres réapparaîtront au fil des événements censés leur donner naissance.

#### Rappel synthétique central
Cette vue sur les fanions sur les sondages et les notes nécessite d'ouvrir le forum : si le compte accède à quelques dizaines de forum, c'est une tâche vite fastidieuse.

Pour éviter cette visite systématique de forums dont le compte découvrirai qu'ils n'ont pas de fanions levés, chaque compte voit au niveau racine de son arborescence (pour chaque forum) un fanion de synthèse pour chaque fil d'actualité du forum auquel il s'est abonné.

# Comptabilisation des consommations de ressources
Les données stockées pour un compte ou un forum, surtout celles correspondant aux notes, peuvent occuper un volume significatif. Ce volume est divisé en deux parts :
- (1) l'une correspond à toutes les données sauf les pièces jointes attachées aux notes.
- (2) l'autre correspond aux pièces jointes attachées aux notes.

Selon les options techniques retenues pour chaque instance les coûts de stockage peuvent être significativement différents pour les deux types.

Les opérations lancées ont aussi un coût de calcul et d'échange sur le réseau.

Un barème fixe les coûts unitaires suivants :
- coût de stockage d'un Mo sur un jour pour les données sauf les pièces jointes.
- coût de stockage d'un Mo sur un jour pour les pièces jointes.
- coût fixe d'une opération simple (certaines sont gratuites) : les opérations complexes sont décomptées comme les N opérations simples qui les composent.
- coût du volume échangé à chaque opération en entrée / sortie sur le réseau au delà d'un certain seuil.

##### Crédit d'un compte
Un compte dispose d'un **crédit** qui est amputé :
- du coût de stockage journalier des données de son propre compte.
- du coût des opérations menées par le compte, sur lui-même ou les forums auxquels il participe.

Le détenteur d'un compte peut transférer à tout instant une part de son crédit :
- sur un autre compte.
- sur un forum.

**Quand son crédit est épuisé, le compte est gelé** : seule une opération de rechargement du crédit peut le remettre en état normal.   
*Au bout d'un certain temps en état gelé, le compte est détruit.*

##### Crédit d'un forum
Un forum dispose d'un crédit qui est amputé du coût de stockage journalier de ses données.

Son crédit peut être augmenté par les comptes participants.

Sur décision, en général collégiale, un forum peut transférer une part de son crédit :
- sur un compte.
- sur un autre forum.

**Quand son crédit est épuisé, le forum passe en lecture seule** : seule une opération de rechargement du crédit par un participant peut le remettre en état normal. A noter que dans cet état ses participants peut encore lire le forum et en particulier en copier les informations puisque le coût des opérations est imputé aux comptes accédants, pas au forum.  
*Au bout d'un certain temps en état lecture seule, le forum est détruit*.

##### Seuil d'alerte
Avant de parvenir au niveau critique gelé / lecture seule, un compte ou un forum passe un seuil d'alerte qui est signalé sur les écrans et permet de remédier à la situation avant d'atteindre la zone rouge.

### Lignes de crédit
Le réseau étant a-social il s'interdit d'établir une corrélation entre des paiements reçus, d'une manière ou d'une autre nominatifs, et les comptes / forum qui vont en bénéficier.

Une personne physique ou morale qui souhaite approvisionner un compte ou un forum en crédit procède ainsi :
- il choisit une phrase secrète qu'il ne communique à personne si le paiement concerne son compte ou la communique au compte qui va en bénéficier.
- il effectue un paiement (par exemple un virement) d'un montant de son choix en lui associant le brouillage cryptographique de cette phrase.
- le comptable de l'instance, après avoir observé la réalité du crédit, enregistre cette **ligne de crédit** identifiée par la phrase brouillée, son montant et une date-heure limite d'utilisation.
- le compte bénéficiaire fournit la phrase secrète, la ligne de crédit associée est retrouvée et le montant est crédité à son compte, la ligne de crédit correspondante étant effacée.

De cette manière :
- seul le détenteur de la phrase secrète peut imputer la ligne de crédit à son compte.
- à aucun moment il n'est établi de lien entre la personne physique ou morale ayant payé et le compte ou forum bénéficiaire.

>**Remarque** : un compte prend un risque mesuré en payant d'avance et en faisant le pari qu'une ligne de crédit correspondante sera bien inscrite. Il peut vérifier que ceci est bien le cas sur une petite somme, peut faire des paiements modestes et successifs ...  

>Il est aussi concevable que le comptable qui introduit les lignes de crédit dans l'instance soit une organisation indépendante de l'instance, un tiers de confiance dont la réputation est basée sur le fait qu'il s'engage à n'opérer que sur des instances qui acceptent l'enregistrement de lignes de crédit sans exiger de données nominatives associées et n'enregistre pas quel compte a tiré sur quelle ligne.

>Dans le cas d'une instance gérée par une organisation au profit de ses membres, il n'intervient pas à proprement parler de mouvement monétaire : un adhérent à l'organisation peut recevoir périodiquement une phrase correspondant à une ligne de crédit déposée par l'organisation (quand c'est elle qui paye) ou à l'inverse l'organisation peut recevoir une phrase brouillée générée par l'adhérent et jointe au renouvellement de son adhésion.

### Synthèse
Lignes de crédit monétaires, cadeaux, adhésions ou droits d'usage émis par une organisation à ses membres, mécénat sans contrepartie ... toutes ces possibilités sont ouvertes et externes au fonctionnement de l'instance : au bout de cette chaîne **une ligne de crédit anonyme est disponible dans le serveur** (avec une période de validité maximale) jusqu'à sa capture par un compte qui seul en connaît la phrase secrète et peut s'en faire verser le montant.

Une fois leurs crédits expirés, les comptes / forums se bloquent puis s'autodétruisent.

## Modération d'une instance
La modération d'une instance a deux composantes :
- la possibilité pour un **compte ayant un privilège d'administrateur** :
    - *de restreindre l'accès à un compte ou un forum*, même disposant d'un crédit.
    - *de détruire effectivement un compte ou un forum*.
- la possibilité que des **lanceurs d'alertes signalent des contenus incompatibles avec l'éthique de l'instance**, avec pour objectif de faire cesser la production de ces contenus quitte à faire procéder à la restriction / destruction des comptes et forums responsables (ce qui rejoint le point 1).

>**Remarque** : en l'absence d'une possibilité technique de modération centrale un compte est seul juge de l'opportunité de sa résiliation. Tant qu'il dispose de crédit (et il peut financer une ligne de crédit anonyme sans aucune corrélation avec lui-même), il n'y a aucun moyen pour faire clore un compte qui ne le souhaite pas ... sauf à détruire toute l'instance.

>La suite de ce développement ne fait appel à aucun concept de se que pourrait / devrait être une *bonne* modération ni ce que devrait ou pourrait être une *bonne* éthique pour une instance. Il est clair que pour une organisation suprémaciste un contenu conforme à son éthique serait exactement considéré comme anti-éthique pour une organisation anti raciste. 

#### Instance sans modération
C'est un simple paramètre de configuration de l'instance : 
- une restriction d'accès ne se prononce que sur seul critère de disponibilité de crédit.
- aucun compte ne peut ni restreindre l'accès d'un autre ni procéder à sa résiliation.
- aucune alerte sur les contenus ne peut être soumise.

L'instance est du type *libertaire*, ses contenus sont intégralement *libres et privés* : aucune organisation ne peut y intervenir ni s'appuyer sur aucune information pour déclarer ses contenus illégaux / illicites etc. Seule la force physique peut techniquement empêcher le fonctionnement technique de l'ensemble de l'instance.

>Remarque : rien n'empêche un participant régulier R à publier hors de l'application une note en clair copie d'une note auquel il a accès régulièrement. Mieux, ce participant peut exhiber la signature de la note par l'auteur A, signature vérifiable et qui ne peut pas être techniquement contestée, et même publier le nom de A (et de ceux ayant certifié son identité) puisque lui R y a eu accès, ce qui peut aussi être vérifié techniquement :
>- et alors ? A est très vilain mais nul ne peut contraindre A à résilier son compte.
>- le nom de A est peut-être un pseudo : c'est dans l'application que son nom est certifié ... au dehors c'est une autre affaire.
>- prouver qu'un texte a été techniquement écrit par quelqu'un ayant eu connaissance de la phrase secrète de A est une chose quand à prouver que A n'a pas cédé à une contrainte insurmontable ou n'a pas été simplement coupable d'une imprudence dans la conservation du secret de cette phrase secrète ...

#### Instance avec droit de dissolution administrative
Si ce droit est ouvert dans la configuration de l'instance, un compte ayant un privilège d'administration peut sur injonction externe agir sur le droit d'accès d'un compte ou d'un forum en en connaissant l'identifiant :
- mettre un compte ou un forum en lecture seule ou le bloquer complètement.
- le détruire après un certain délai de remord après blocage, juste le temps en fait de s'assurer qu'une erreur n'a pas été commise en fournissant son identifiant.

Certaines instances peuvent préférer accepter l'arbitraire / contrainte d'une pression externe, judiciaire ou extra judiciaire, exercée à l'égard d'un compte précisément cité plutôt que de faire face à une fermeture imposée physiquement de l'ensemble de l'instance.  
D'autres instances peuvent faire le choix inverse : tout dépend de l'engagement moral pris avec les comptes participants.

>***En aucun cas le privilège d'administration ne permet d'accéder aux contenus, c'est techniquement impossible.***

#### Instance avec lanceurs d'alertes
Un lanceur d'alerte est une personne qui a eu accès normalement à des notes du fait de sa participation régulière à un forum et qui considère leurs contenus comme incompatible avec la charte éthique de l'instance.
- son objectif est de porter à la connaissance de personnes externes le ou les notes qu'elle juge non conforme.
- le lanceur d'alerte constitue et enregistre un dossier :
    - avec les signatures du / des notes en cause.
    - avec les textes de ces notes décryptés et ré-encryptés pour n'être lisibles que par un compte ayant privilège d'administration.
    - en fournissant ou non son nom / numéro de compte, selon que c'est déclaré dans la configuration de l'instance comme *interdit / obligatoire / au choix du lanceur d'alerte*.

Un dossier d'alerte peut être expertisé par un compte ayant privilège d'administration :
- il peut recalculer le hachage des contenus (qu'il a reçus cryptés pour lui seul) et les confronter avec la signature de l'auteur, bref s'assurer que ce n'est pas un fake.
- apprécier du contenu et des mesures à prendre.

>Il est clair qu'autoriser le lancement d'alertes sans possibilité de coercition éventuel peut manquer d'intérêt ... quoi que ...

Différences entre le lancement d'alertes dans une instance *modérée* et pour une instance *libertaire* :
- dans l'instance *libertaire* l'alerte ne peut être que publiée hors de l'application auprès d'un public qui certes peut vérifier que l'alerte n'est pas un fake ... mais peut ne pas le faire. L'alerte est publiée dans le vaste internet anonyme.
- dans l'instance *modérée*, certes la publication grand public ne peut pas être interdite mais il existe au moins une instance discrète interne.
- dans l'instance *libertaire* le soi disant coupable ... fait ce qu'il veut, y compris continuer. Pour le faire taire il faut ... tuer l'instance.
- dans l'instance *modérée* le compte incriminé peut être bloqué sans perturber les autres.
- le pouvoir de justice dans une instance modérée est celle de l'organisation qui la finance : est-ce mieux ou non que le pouvoir de justice d'un état ? Ce n'est pas une question technique et ça dépend autant de l'état que de l'organisation qui supporte l'instance.

>**Remarque** : dans un cas comme dans l'autre la notion d'appréciation ne peut pas être technique mais uniquement humaine et partiale. La preuve technique n'est qu'une preuve de publication de contenu sous contrôle d'une phrase secrète, réputée théoriquement devoir le rester sauf pour une personne : c'est certes un élément troublant mais l'ADN de l'auteur n'est pas capté par le système (et d'ailleurs le serait-il que c'est seulement le fichier le contenant qui le serait).

# Confiance dans l'application
Ces données passent par deux applications (une terminale, l'autre dans le serveur), circulent sur internet et sont stockées dans une base de données. Tous ces niveaux techniques sont susceptibles d'être attaqués et déroutés.  
Les techniques de cryptographie employées offrent certaines garanties en elles-mêmes, mais pas toutes les garanties et le niveau de confiance à accorder à l'application n'est pas un sujet trivial et s'étudie en fonction des niveaux de confiance qu'on peut accorder à chacun de ces éléments.

Il est impossible de garantir que toutes les données seront toujours présentes et inviolées mais il est possible de garantir que pour certaines données :
- si elles ont été trouvées elles sont fiables et inviolées, c'est à dire effectivement produites par qui prétend l'avoir fait et que le contenu n'en a pas été altéré.
- plus exactement il est possible de détecter si une telle donnée a été corrompue, donc si elle est fiable ou non. En règle générale les hackers ne se fatiguent pas à corrompre des données dont la corruption est détectable : ça ne fait que rendre l'application inutilisable (au même titre que des suppressions aveugles).

>**Se focaliser principalement sur la confiance envers l'environnement technique n'est pas une bonne idée. Un bon agent infiltré au bon endroit peut être beaucoup plus efficace.**

#### Confiance dans l'application terminale et le réseau Internet
C'est l'application terminale qui crypte, signe, décrypte et vérifie les signatures. Si l'application terminale, dans son ensemble, n'est pas de confiance, aucune confiance ne peut être accordée à l'application.
- son logiciel est open source et lisible en clair dans n'importe quel terminal. Il est vérifiable par tout lecteur ayant le minimum requis de compétence technique.
- il peut être rendu disponible depuis des serveurs divers, et en particulier des serveurs de confiance distincts des serveurs d'instance qui hébergent les données et l'application centrale.
- l'application s'exécute dans un environnement technique de l'appareil dans lequel, usuellement et à juste titre, on a confiance : *navigateur, système d'exploitation, matériel*. On a toutefois relevé par exemple :
    - un fournisseur de matériel de haute renommée qui détournait les frappes au clavier de l'utilisateur.
    - des CPU de haute renommée également mais pas complètement étanches aux inspections indésirables des autres programmes.
    - des navigateurs dont l'éditeur offre des primes à ceux qui y trouvent une faille de sécurité, preuve qu'il y en a.

**La transmission avec le serveur utilise le protocole chiffré d'internet HTTPS**, dont pour l'instant il n'existe pas de preuve de fragilité.

>Ce niveau technique est supposé de confiance, sinon l'application n'est pas pertinente (ni la quasi totalité des autres d'ailleurs).

Dans ce contexte :
- les clés de cryptographie requises sont générées aléatoirement à la création du compte dans l'application terminale :
    - le nom du compte est crypté et les diverses clés (sauf la clé 0) sont scellés dans ***un ticket public immuable et inviolable portant le numéro du compte***.
    - la clé 0 *mère* est enregistrée cryptée par la phrase secrète utilisée à la création.
- aucun autre compte ne peut être créé (dans aucune instance d'ailleurs) et présenter le même numéro de compte avec un ticket public valide différent (un *fake*): c'est détectable.
- si l'application mémorise pour chaque numéro de compte son **ticket public immuable et scellé**, n'importe quel terminal de la planète peut aussi en conserver des copies sur n'importe quels supports. Depuis ce ticket n'importe quel terminal, avec ou sans le logiciel de l'application :
    - **peut obtenir le nom du compte** à condition de disposer de la clé 1 qui le crypte. Une tentative de recherche du nom scellé par force brute en essayant toutes les combinaisons est vouée à l'échec.
    - **peut s'adresser au compte en cryptant des données de telle sorte que seul le détenteur du compte puisse les décrypter**.
    - **principe d'inviolabilité et de non répudiation** : il peut vérifier qu'une note qui prétend avoir été horodatée et signée par un compte, l'a véritablement été en utilisant les clés ad hoc que nul ne peut avoir inventé. S'il possède la clé de cryptage d'une note, la signature lui indiquera si le contenu est un faux ou non.

A la création du compte, la clé 0 mère est cryptée par la première phrase secrète.  
Ultérieurement, à condition de disposer pour un compte donné de la clé 0 cryptée par l'ancienne phrase secrète, une session peut ré-encrypter la clé 0 par une nouvelle phrase secrète.

>**Remarque** : il existe des moyens non techniques pour demander, et souvent finir par obtenir de son titulaire, la phrase secrète d'un compte. La violence certes, mais aussi plus fréquemment la persuasion couplée à l'imprudence du titulaire, sont les moyens les plus usuels.

>**Synthèse** : le système cryptographique garantit, même à un terminal indépendant de l'application, que muni du ticket public d'un compte identifié par son numéro :
>- il peut vérifier techniquement que le ticket n'est pas un fake,
>- que tout message envoyé à ce compte en utilisant la clé qui s'y trouve ne pourra être décrypté que par une application détenant au moins un ticket privé du compte et la phrase secrète associé.
>- que toute note prétendue horodatée et signée par ce ticket l'a vraiment été.
>- que la note n'a pas été altérée (après l'avoir décryptée par sa clé de cryptage).

### Confiance dans le serveur et la base de données
Les tickets et contenus sont générés, cryptés et scellés dans les applications terminales.  
Le serveur ne reçoit jamais aucun élément qui lui permette de décrypter ou crypter ces contenus : au plus peut-il vérifier que les signatures des contenus signent bien ce qu'elles prétendent signer, mais ce contrôle est juste destiné à décourager un éventuel hacker maladroit qui tenterait de pervertir les données à distance : la vraie et seule vérification crédible est celle refaite dans la session terminale lectrice des notes.

Une note stockée dans la base de données ne peut pas être décryptée sans disposer de sa clé de cryptage. Le ticket public de son auteur est nécessaire pour vérifier la signature.  
Le détournement / copie de la base de données ne peut fournir aucune note sans disposer de leurs clés, c'est à dire **sans la complicité d'un compte disposant d'un accès régulier**.  
Détourner une base ne servirait qu'à en obtenir les notes que le complice peut avoir régulièrement.

#### Destruction d'informations
Techniquement une *application serveur pirate* peut intercepter les opérations demandées au serveur régulier et effectuer son propre traitement :
- son action ne peut être que destructrice : suppression de comptes, de notes, de participants à des forums, de contacts dans les répertoires des comptes.
- il ne peut pas créer des notes fake, ni de faux certificats d'identité, ni de faux participants à un forum : tout cela est détecté à la lecture dans les applications terminales.

De même une intervention directe sur la base de données ne que miter les données mais pas pervertir silencieusement celles restantes.

>**Synthèse** : rendre une application inutilisable et non crédible en raison des *attaques destructives de ses données* n'est pas un aspect secondaire.
>- en revanche tout ce qui s'y trouve peut être considéré comme crédible, non altéré.
>- il est possible de détecter que des données ont été détruites *quand leur liste peut être scellée et signée* (par exemple, un certificat d'identité, la liste des participants d'un forum ... ) mais pas dans le cas général (la liste des notes d'un forum n'est pas scellée : un hacker pourrait parvenir à en détruire silencieusement).

### URLs d'une note et de sa pièce jointe
Un peu paradoxalement un compte peut obtenir pour toute note une URL qui comporte l'identification de la note et sa clé de cryptage (à condition qu'il l'ait) et une URL pour sa pièce jointe.  
Il peut transmettre cette URL à n'importe qui, la publier dans un blog ...

**Choquant pour une application a-sociale cryptée ?**
En fait non, c'est normal. Si un compte a un accès *licite et authentifié* à une note, il peut bien la lire en clair. L'objectif de l'application est de partager des notes avec qui on veut et si on a décidé que C pouvait lire une note, c'est bien que C en verra le contenu en clair, pièce jointe comprise. Dès lors cette note en clair peut se retrouver sur un fichier et il peut (*can* en anglais) bien la diffuser où il veut : c'est le risque pour toute information confidentielle. Dès lors qu'elle est connue de quelqu'un, il faut avoir confiance dans cette personne pour qu'elle reste non publiée, si tel était l'intention de son auteur.

>Pierre Dac avait soulevé il y a longtemps ce problème en publiant une petite annonce : *recherche personne ayant une belle écriture et ne sachant pas lire pour recopier des documents secrets*.

Bref rien de nouveau sous le soleil : la seule information qui ne pourra jamais être publiée hors de la volonté de son auteur est celle qu'il n'a pas écrite ou du moins n'a jamais fait lire à personne. C'est exactement la cas d'une note du **home** d'un compte qui n'a jamais été partagée. Seul son auteur la connaît et il ne faut pas qu'il l'imprime (ou qu'il broie le papier sans l'avoir perdu de vue).

Dès lors fournir une URL contenant la clé de cryptage d'une note ne pose pas plus de problème que de transmettre une copie par fichier de la note ou d'avoir laissé traîner une impression sur une imprimante.

Il est aussi possible d'obtenir l'URL d'une note sans sa clé de cryptage et de l'envoyer par e-mail : elle ne sera lisible que quand le destinataire aura reçu, peut-être par un autre canal (un SMS par exemple), la clé de cryptage.

### De l'usage des vrais noms ou des pseudos
Qu'est ce qu'un vrai nom ? En droit français un *nom / prénoms date et lieu de naissance* vérifiable dans un registre d'état civil. L'absence d'un tel registre informatisé et les approximations orthographiques que contiennent les registres rend ce concept techniquement malaisé à utiliser.  
Comment vérifier qu'une personne physique derrière un terminal distant n'usurpe pas l'état civil d'une autre ? Les données biométriques ne sont, à ce jour, que partiellement fiables et non vérifiables facilement à la création d'un compte.

L’État français ne signe pas de certificat d'état civil.

Les autorités de certifications reconnues en signent à toute personne qui peut payer (et même gratuitement parfois).

**Les requêtes de création de comptes auto-certifiées soit faites en utilisant un certificat** signé par une autorité de certification enregistrée à la configuration du serveur frontal HTTPS et ayant pour DN l'un de ceux cités comme d'administration pour l'instance. Le nom du compte créé n'est toutefois pas issu du DN.  
Les autres requêtes n'utilisent que l'authentification par phrase secrète, largement aussi fiable que le mot de passe demandé pour obtenir son certificat sur son poste. 

Il est techniquement simple de configurer une instance pour qu'elle exige un certificat reconnu par l'autorité de certification de l'organisation pour s'y connecter. Il est simple également à la création du compte d'utiliser son DN (ou une partie du DN) comme nom.  
- encore faut-il avoir confiance dans la distribution de ces certificats qui n'est guère envisageable que dans une organisation ayant intégré cet objectif.
- la liste des noms autorisés est dans ce cas connu de l'organisme de certification, ce qui peut être souhaitable ou non.
- se passer de phrase secrète suppose d'utiliser le DN comme base de génération de la clé mère : à tout le moins il faudrait a minima exiger un code supplémentaire ajouté au DN pour générer cette clé puisque le browser n'en réclame pas quand on y désigne un certificat.
- installer un certificat sur un poste *incognito*, est-ce une bonne idée ? (et ajouter le certificat du CA comme CA de confiance, et ne pas oublier de les enlever en partant).

>Mais la question centrale est : le *vrai* nom est-il souhaitable ?
>- cela dépend de chaque instance et de son objet.
>- des organisations de résistance, certains services secrets ... peuvent définitivement répondre NON.
>- le plus simple est d'utiliser ... la convention prévue dans la charte éthique de l'instance utilisée et accessible sur la page d'accueil.

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
Une ***clé symétrique*** (AES-256) est générée **depuis une suite de 32 octets**.  
Un ***vecteur d'initialisation*** `iv` est **une suite de 16 octets**.  
Le texte crypté d'un texte source dépend de ces deux suites.
- la clé est détenue par son propriétaire.
- la suite `iv` peut être :
    - constante dans l'ensemble de l'application.
    - être générée aléatoirement et être jointe en texte du résultat crypté de manière à ce que le décryptage lise cette suite comme `iv`.

Le choix a été fait d'une suite de 4 octets, aléatoire au cryptage et placée en tête du texte crypté. `iv` est cette suite répétée 4 fois. De ce fait un même contenu cryptée deux fois par la même clé a deux valeurs encryptées différentes : sans la clé il est impossible de savoir si deux textes encryptés différents sont le résultat de l'encryptage de deux contenus différents ou du même contenu. Ceci évite, par exemple sur une base détournée, de pouvoir déterminer si des données cryptées sont identiques ou si des clés cryptées sont identiques : l'interprétation même des métadonnées obscures devient impossible.

AES  permet de crypter une suite d'octets (donc un texte) de n'importe quelle longueur et de décrypter la suite résultante avec la même clé pour ré-obtenir la suite initiale. Le cryptage / décryptage symétrique est rapide.

#### Cryptage asymétrique RSA-2048
##### Encrypter / décrypter
Un ***couple asymétrique de clés*** (RSA-2048) (*publique* de cryptage / *privée* de décryptage) utilise deux clés de 256 octets : 
- on **crypte** avec la clé **publique** un texte court de longueur au plus de 256 octets.
- on **décrypte** avec la clé **privée** du couple.

Depuis une clé RSA générée aléatoirement on exporte une forme dite PEM textuelle des clés publique et privées : l'importation de ces clés permet de reconstruire la clé RSA initiale.
- l'export de la clé publique est comme son nom l'indique publiable *en clair* sans restriction afin de permettre à n'importe qui de crypter un texte à destination du détenteur de la clé privée correspondante.
- l'export de la clé privée du couple de clés est *encrypté* par son propriétaire par le moyen de son choix de manière à être le seul à pourvoir décrypter un texte crypté par la clé publique correspondante.

On se sert d'un couple de clés publique/privée typiquement pour encrypter une clé symétrique à destination d'un unique destinataire et servir de cette clé pour échanger des textes de longueur quelconque.  
**Le cryptage / décryptage asymétrique est lent** et le **texte crypté a une longueur fixe de 256 octets** même quand le texte à crypter est plus court.

##### Signer / vérifier
Un autre couple de clés asymétrique permet :
- **de signer par la clé privée** un texte de longueur d'au plus à 256 octets. Le résultat est la signature.
- en fournissant le texte initial et sa signature de **vérifier par la clé publique** que la signature est bien celle du texte par le détenteur de la moitié privée de la clé.

#### Hachage Java d'un string (court)
Cette fonction retourne un entier depuis un string : cet entier est ensuite converti en base 64 URL. Le nombre de collisions est élevé, non admissible pour s'assurer qu'on est en présence du même texte quand on est en présence de deux hachages identiques.

#### Hachage SHA-256 (long)
Le SHA-256 d'une suite d'octets est une suite de 32 octets tel qu'il est *très coûteux* (mais concevable pour des textes relativement courts de moins d'une quinzaine de caractères) de retrouver la suite d'octets originale depuis son digest.  
Le nombre de collisions est négligeable : deux textes différents ont, en pratique, deux digests différents. Le calcul est rapide.

#### Hachage BCRYPT d'un string
L'objectif du hachage BRCYPT est de rendre impossible de retrouver un string origine depuis son hachage : le calcul est, volontairement, très coûteux et ne peut pas être accéléré par des processeurs ad hoc. 
- une recherche par *force brute* (essai de toutes les combinaisons) est sans espoir dès que la longueur du texte excède une douzaine de caractères. 
- une recherche depuis des *dictionnaires de textes fréquemment utilisés* (les prénoms, noms de fleurs, 1234 ...) est sans espoir également si le texte est long et utilise des jeux de caractères changeants.

Le hachage BCRYPT d'un string ne traite que les 72 premiers octets de sa conversion en UTF-8 et retourne un string de 60 caractères qui :
- commence par la constante `salt` : `$2a$10$` + 22 caractères en base 64 spécifique de BCRYPT contenant des caractères `.` et `/`
- se termine par le `hash` de 31 caractères en base 64 spécifique de BCRYPT. 

Pour éviter ces séparateurs et raccourcir le résultat, on enlève les 29 premiers caractères (le `salt` considéré comme une constante du code).

Le hachage BCRYPT a l'inconvénient de se terminer par des 0 binaires sur un texte d'entrée pas trop long : toutefois ne sont stockés sur base ou fichier que les SHA-256 des BCRYPT ce qui empêche de savoir si le texte initial avait tendance à être court.

## Phrases secrètes, noms et identifiants

### Phrases secrètes
La sécurité repose sur la longueur de la phrase : ennuyer l'utilisateur avec des caractères spéciaux est inefficace. Une phrase secrète est constituée de N mots successifs dont :
- le premier signe est un chiffre de 1 à 9. Si le premier chiffre du premier mot est omis il est considéré comme étant 1.
- les suivants sont des lettres l de a à z.

*Exemple* : **9les8sanglots7longs6des5violons**

Une phrase doit avoir un nombre minimum de mots et un nombre minimum total de lettres. 

La **phrase complète** est obtenue en calculant un caractère `32 + (26 * c) + l` successivement pour chaque lettre `l` de chaque mot commençant par le chiffre `c`.  
D'où de facto l'utilisation d'un alphabet exotique et l'impossibilité d'utiliser des répertoires de mots, les chiffres correspondant à des aléas de l'utilisateur.

La **phrase réduite** est le texte résultant de la phrase complète en ne prenant que les deux premiers caractères de chaque tranche de trois caractères.

En session, de la phrase saisie par l'utilisateur on obtient :
- `cleS` : SHA-256 du BCRYPT de la phase complète. **Cette forme ne sort jamais de la session**.
- `psrB` : BCRYPT de la phrase réduite. **Cette forme s'échange sur le réseau** (cachée par le HTTPS).
- `psrBD` : SHA-256 de `psrB`. **Cette forme est stockée en base**.

##### Phrase secrète d'administration
C'est une phrase secrète dont le `psrD` est enregistré dans la configuration de l'instance. Même le piratage de cette `psrB` par un serveur indélicat ne sert à rien : c'est la clé S, qui n'est jamais sortie de la session et directement issue de la phrase secrète qui permet de décrypter les informations.

>La phrase réduite est garantie unique afin d'empêcher de tomber par hasard (ou usage d'un robot) sur une phrase secrète enregistrée ouvrant un accès à un compte.  Ainsi il n'existe pas deux comptes ayant déclaré des phrases secrètes *trop proches* (ou des phrases de contact).  
>En supposant que le logiciel du serveur ait été substitué par une version pirate indélicate qui enregistrerait les requêtes en clair, et donc `psr`, une session non officielle pourrait se faire reconnaître comme disposant d'un compte authentifié vis à vis du serveur sans disposer de la phrase secrète. Elle ne serait toutefois pas capable de décrypter la clé *mère* 0 obtenue depuis le BCRYPT de la phrase complète connue du seul titulaire et serait incapable de déchiffrer la moindre donnée du compte.

>SHA-256 est considéré comme fragile à une attaque par force brute : c'est vrai pour un mot de passe de faible longueur alors qu'ici il s'agit des 31 caractères sortis d'un BCRYPT. Il est impossible de retrouver `cleS` ou `psrD` par attaque de force brute.  
>L'attaque du BCRYPT sur un minimum de 20 caractères, sachant qu'en plus les mots correspondants sont préfixés d'un chiffre aléatoire vis à vis des dictionnaires, semble vouée à l'échec d'après la littérature spécialisée.

### Nom : normalisation / réduction et initiales
La **normalisation** d'un nom consiste dans le texte saisi par l'utilisateur à y remplacer par un seul espace les `white space` consécutifs et en ignorant ceux de tête et de queue.

> White space : space, tab, form feed, line feed and other Unicode spaces.   
[ \f\n\r\t\v\u00a0\u1680\u180e\u2000\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]

La **réduction** d'un `nom` consiste à ne garder que 2 caractères sur 3 du nom normalisé : **le nom réduit `nomrB` est toujours ensuite haché par BCRYPT**.

Les **initiales** sont la *mise en majuscule* du premier caractère de chaque mot du nom normalisé (découpé par les espaces et tiret) quand il en existe un équivalent *non accentué* de A-Z ou a-z (les initiales de `Jean élan` sont `JE`, celles de `jean 23` sont `J`.).

### Numéro de forum
C'est un vecteur de 15 octets tirés au hasard et codé en base 64 URL de 20 caractères.

>Le numéro d'un compte se distingue de celui d'un forum par sa longueur (31 pour un numéro de compte, 20 pour un groupe).

### Identifiant d'une note
Il est constitué de 3 parties :
- 0 ou 1. O:note immuable, 1:note versionnée.
- 8 caractères en base 64 URL représentant 6 octets dont la valeur binaire forme, en lecture décimale, la date-heure de création `AAMMJJhhmmssSSS` (180224225959001).
- 12 caractères en base 64 URL représentant un vecteur de 9 octets tirés au hasard pour éviter les collisions dans la même milliseconde de création.

Cet identifiant a l'avantage de pouvoir être considéré comme universel (toutes instances confondues).

### `TPU` : ticket public d'un compte
Ce ticket a été généré par la session créatrice du compte et comporte les propriétés suivantes :
- `dh` : date-heure de génération.
- **alias** : `nomrBD` : SHA-256 du BCRYPT du nom réduit utilisé pour détecter et interdire l'usage de noms trop proches.
- `c1O` : clé 1 du compte cryptée par la clé 0. Elle crypte le nom du compte `nom1` ci-dessous et ceux des comptes certifiant son identité.
- `nom1` : nom crypté par sa clé 1.
- `pub` : clé RSA publique de cryptage.
- `verif` : clé RSA publique de vérification.
- `priv0` : clé privée RSA de décryptage cryptée par la clé 0.
- `sign0` : clé privée RSA de signature cryptée par la clé 0.

**La clé de la constante** est le SHA-256 de `nom1 pub verif` et est le numéro de compte.

>Remarque : les trois identifiants majeurs, numéro de compte, numéro de forum, numéro d'une note sont des identifiants universels qui n'ont pas de possibilité réelle de collision même entre instances différentes. Ils ont l'avantage d'avoir des longueurs fixes et différentes (32, 21 et 20 caractères).
>Seul l'identifiant d'une note est porteur de deux informations (immuable / versionnée et date-heure de création).

## Logique de cryptage
On distingue deux niveaux de protection par cryptage :
- **le niveau 1** protège une information même au cas ou la base serait détournée.
- **le niveau 2** protège l'information sur la base officielle mais si une clé a été conservée par une session qui y avait droit mais ne l'a plus et que la base est détournée, l'information peut être décryptée.


### Clés d'un compte
**La clé 0** est la clé *mère* de cryptage d'un compte : 
- elle est générée aléatoirement dans la session terminale à la création du compte.
- elle est immuable au cours de la vie du compte. 
- elle est stockée dans l'entête du dossier du compte cryptée par la clé S tirée de la phrase secrète. Changer de phrase secrète n'a pour seule conséquence que le ré-encryptage de la clé 0.

**La clé 1** sert à crypter les noms des certificateurs dans le certificat d'identité du compte et le nom du compte lui-même dans son Ticket public :
- elle est générée aléatoirement dans la session terminale à la création du compte.
- elle est immuable au cours de la vie du compte.
- elle est stockée dans le ticket public du compte cryptée par la clé 0 du compte.

**Les deux couples de clés PUB/PRIV asymétriques** publique / privée :
- sont générés à la création du compte dans la session terminale.
- sont immuables au cours de la vie du compte. 
- sont stockés dans le ticket public du compte, cryptée par la clé 0 du compte pour le clé privée et en clair pour la clé publique.
- le premier couple permet à un autre compte de **crypter** une clé entre eux par la clé publique. Ultérieurement dès que possible une session terminale du compte trouvant un tel cryptage (il est long), ré-encrypte la clé par la clé 0 (le résultat étant plus court).
- le second couple permet une signature / vérification.

### Clés d'une note
Chaque version d'une note a sa clé tirée au hasard à sa constitution. Un compte qui aurait eu accès à une note d'une version 1 et aurait été résilié, ne pourrait pas lire les versions postérieures à sa résiliation même sur une base détournée.

### Clés d'un forum
La **clé complète** d'un forum est générée aléatoirement à sa création.  
La **clé réduite** est la clé complète où un octet sur 2 a été remplacé par `0`.

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

