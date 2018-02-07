# Un réseau a-social


Une personne ayant un **compte** dans une *instance* de cette application la perçoit d'abord comme *un espace de stockage hiérarchique classique* où les feuilles sont des **notes** (et non exactement des fichiers).  
En tête de ce stockage apparaissent des *répertoires racines* :
- **home** : le répertoire strictement personnel du compte est totalement fermé et invisible aux autres comptes, seul le compte titulaire y a accès.
- **forums** : les autres répertoires racines sont partagés avec d'autres comptes et sont dénommés *forum*, justement parce que plusieurs comptes s'y retrouvent pour partager des notes.

##### Note
Le contenu d'une **note** est constitué,
- **d'un sujet / résumé** textuel court (sans formatage), contenant éventuellement des hashtags permettant de retrouver les notes par centres d'intérêt.
- **d'une vignette** facultative, aperçu d'une photo / clip joint, photo d'identité de l'auteur, icône...
- **d'un texte** facultatif pouvant comporter jusqu'à quelques milliers de caractères lisible sous une présentation simple agréable (niveaux de titre, gras / italique, liste à puces ...).
- **d'une pièce jointe** facultative de n'importe quel type : photo, clip vidéo, etc. et qui peut être le ZIP de plusieurs fichiers.

Une note possède un *identifiant unique universel* et peut avoir selon l'option choisie à sa création, a) une version unique (comme un e-mail, un SMS ...), b) plusieurs versions successives (comme tout document révisable).

Chaque version (première et suivantes) a 
- sa **date-heure**, 
- son **auteur**,
- son **contenu** (sujet / texte / vignette / pièce jointe) propre.
- sa **clé de cryptage** propre : sans cette clé le contenu n'est pas lisible.
- sa **signature par son auteur** (un compte) : un procédé cryptographique permet à un lecteur (ou qu'il soit dans le monde, même non connecté) de vérifier à partir de la signature :
    - que le contenu de la note a bien été signé par l'auteur déclaré (non répudiation). - quand il dispose de la clé de cryptage, que le contenu n'a pas été falsifié depuis sa création par son auteur.

#### Forum
Le répertoire **home** ne pose guère de problème de confidentialité : seul le compte y a accès, il est crypté et seul le compte titulaire en possède la clé.  
Le répertoire d'un **forum** pose d'avantage de questions : plusieurs comptes ont accès à ce répertoire :
- les notes stockées peuvent être lues par tous les comptes ayant accès au forum.
- ces comptes peuvent y écrire des notes, y copier / coller des notes de / vers leur *home* ou d'autres *forums* auxquels ils ont accès.

>Toutes ces notes sont cryptées et seuls les comptes ayant accès au forum ont accès à leurs clés respectives.

**Participants**  
A la racine de l'espace d'un forum, figure un répertoire **Participants** qui contient un *sous répertoire spécial pour chaque compte* ayant accès au forum, ayant pour **nom** celui du compte avec une petite fiche d'information donnant :
- son numéro de compte.
- une photo d'identité (facultative) choisie par le titulaire du compte.
- **son certificat d'identité**, à savoir la liste des comptes avec leur nom ayant certifié qu'il connaissait le titulaire du compte et que ce nom est bien représentatif.

Dans ce sous-répertoire on va en général trouver une courte *note d'auto-présentation* par le titulaire qu'il a eu la courtoisie de montrer lorsqu'il a été invité à partager ce forum.  
On peut le cas échéant trouver d'autres notes rangées à cet endroit, typiquement celles directement relatives à ce compte dans ce forum (mais le choix du classement des notes est libre et dépend des conventions librement consenties dans le forum).

**A propos**  
Toujours à la racine de l'espace d'un forum, un autre répertoire standard **A propos** est réservé à héberger des notes expliquant l'objet du forum, ses règles éthiques, ses conventions d'organisation du rangement des notes ...

**Confiance entre les participants**  
Inviter un nouveau participant à partager l'espace d'un forum pose deux problèmes de confiance :
- *à l'invité* : quel est l'objet de ce forum, comment fonctionne-t-il et surtout qui vais-je y rencontrer et puis-je avoir confiance dans les participants actuels ?
- *aux participants actuels* : quel est ce nouvel invité, qui le connaît, qui certifie son nom, pouvons accepter de dévoiler nos notes à ce nouvel arrivant ?

Pour tenter de résoudre ce double problème l'invitation d'un nouvel arrivant passe par deux étapes :
- **l'invitation** : l'invité reçoit une clé qui lui permet de lire les répertoires **Participants** et **A propos** et ainsi de prendre connaissance de ce qu'est le forum et qui sont ses participants. L'invité peut déposer dans son sous répertoire du répertoire **Participants** une ou des notes d'auto-présentation (que nul autre ne peut ni altérer ni détruire). L'invité peut également renoncer et disparaître du forum.
- **la confirmation** par les autres participants : au vu de cette information apportée par l'invité, de son certificat d'identité et des avis des uns et des autres, le nouvel invité peut être confirmé (ou refusé et disparaître).

Un chapitre particulier **Gouvernance des forums** traite des questions sous-jacentes à ce bref aperçu :
- comment se prennent les décisions dans le forum à propos des invitations / exclusions, etc.
- certains participants peuvent-ils avoir des rôles délégués particulier dans la gestion quotidienne : suppression des notes obsolètes, attribution / réduction de crédit de fonctionnement ?
- comment se décide la clôture et la destruction du forum.

***La gouvernance d'un forum*** peut être de l'une catégories suivantes :
- **autoritaire unique ou multiple** : chaque participant dirigeant a tout pouvoir de gestion (invitations / résiliations, clôture du forum, etc.), y compris celui de muter la gouvernance du forum dans l'une des catégories ci-après.
- **démocratique par délégation** : les décisions de gestion sont prises par consentement (absence d'opposition formelle) ou vote majoritaire des participants ayant un statut d'élu. Des scrutins peuvent être organiser avec l'ensemble des participants pour :
    - changer les élus.
    - changer le mode de gouvernance.
- **démocratique directe** : toutes les décisions de gestion sont prises par consentement (absence d'opposition formelle) ou vote majoritaire des participants, y compris le changement de mode de gouvernance.

#### Compte
Chaque compte organise son espace de stockage de notes comme il l'entend. Un seul répertoire racine spécifique y apparaît toujours : Contacts.

Le répertoire racine Contacts a un sous répertoire pour chacun des autres comptes avec lequel le compte est (ou a été) en relation plus ou moins proche.
- le nom de ce répertoire est celui du compte (il peut temporairement ne pas l'être encore).
- une petite fiche d'information y est associée avec :
    - son numéro de compte.
    - sa photo d'identité (facultative).
    - son certificat d'identité quand il est connu.
    - quelques autres informations facultatives qui seront détaillées plus loin.

Le sous répertoire d'un contact peut contenir une note d'auto-présentation communiquée par le contact mais aussi d'autres notes rangées par le titulaire du compte à propos de ce contact (c'est libre).

**Échanges**  
Le sous répertoire d'un contact contient toujours un sous-répertoire particulier Échanges qui représente le fil historique des échanges de notes entre le titulaire du compte et son contact. Les notes qui y apparaissent ici ne sont toutefois pas complètes mais ne sont que l'aperçu d'une note complète :
- si elle est reçue (a été présentée par le contact) ou émise (proposée par le titulaire à son contact).
- sa date-heure.
- son sujet. 
- quelques indicateurs sur la présence et la taille des autres éléments de contenu.

>Si la note est très courte et n'a qu'un sujet ... l'aperçu est la note elle-même.

En présence de l'aperçu d'une note proposée par un de ses contacts, le titulaire du compte :
- obtenir la note complète correspondante et la ranger où il veut (voir même ici en remplacement de l'aperçu).
- effacer l'aperçu qui ne l'intéresse pas ou plus.
- ne rien faire : l'aperçu reste.

>L'objectif est que cet espace d'échanges ne soit pas saturé par des volumes importants de notes pas toujours souhaitées : ainsi l'espace d'un compte reste maîtrisable par son titulaire.
>L'aperçu d'une note proposée par A à C est double : chez A il figure comme une note proposée à C et chez C celui d'une note proposée par A. Les deux sont strictement synchronisés à la création, mais plus tard,
>- A a pu détruire la note proposée à C : si C ne l'a pas obtenu avant, il ne l'obtiendra plus (une date-heure minimale de conservation est spécifiée pour palier, en général, à cette difficulté).
>- A comme C peuvent effacer de leur côté l'aperçu qui les encombre.

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
- **en étant assuré de la non accessibilité des informations intelligibles**, même en cas d'écoute intempestive du réseau comme de détournement des données stockées dans le cloud.

Ce dernier point induit des contraintes de cryptages sur les terminaux. Toutes les données humainement signifiantes :
- sont ***cryptés*** sur les terminaux, 
- transitent cryptées sur le réseau, 
- sont stockées cryptées sur les serveurs du cloud comme dans les mémoires des terminaux (en modes synchronisé et avion). 
- ne peuvent être ***décryptées*** que dans l'application qui s'exécute sur les terminaux des titulaires des comptes.

>Le détournement de données, que ce soit la base de données du serveur comme celles locales des terminaux ou de celles transitant sur les réseaux, est inexploitable : les données y sont indéchiffrables.

>Aucune utilisation *commerciale* des données n'est possible puisque celles signifiantes sont toutes indéchiffrables, même **et surtout** pour le prestataire de l'application et ses hébergeurs.

**Aucun répertoire central ne peut être constitué, il ne contiendrait que des données inintelligibles** : il est impossible de chercher un compte ou un forum dans une instance de l'application en fonction de critères comme les noms, intitulés des forums, centres d'intérêt, mots clés ...

>On ne s'y connaît que par cooptation, contacts directs et rencontres sur des forum.

## Compte
A sa création un compte reçoit une identification universelle unique et un enregistrement dit *Ticket public du compte* qui contient les éléments cryptographiques nécessaires. Ce *Ticket public* est comme son, nom l'indique, public, peut être copié sans restriction et est immuable pour toute la vie du compte, et même après pour authentifier les notes signées par un compte même après sa résiliation.

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

>Il est impossible de deviner la liste des noms des comptes connus dans une instance : toutefois connaissant un nom il est possible de savoir s'il correspond à un compte de l'instance. ?????????????????????????

##### Certificat d'identité d'un compte
*Un compte a son identité certifiée par un ou plusieurs autres comptes* : en certifiant l'identité d'un compte A, un compte C ne fait qu'affirmer que le nom du compte A est bien représentatif de la personne qui en est titulaire. Ceci ne signifie rien en termes d'amitié ou de convergence de vue, seulement une affirmation sur l'adéquation entre le nom présenté et la personne physique correspondante.

>**Le certificat d'identité d'un compte** est la liste des comptes ayant certifié son identité avec pour chacun :
>- *son numéro de compte*.
>- *son nom* (si la certification n'a pas été résiliée).
>- la date-heure de début certification (et celle de fin si elle a été résiliée).
>- le certificat d'identité de ce compte n'y figure pas : quand on accède au certificat d'identité d'un compte on obtient la liste des noms des certificateurs mais pas ceux des certificateurs des certificateurs.

#### Processus de création d'un compte
Un compte se crée par auto-déclaration par son titulaire :
- il choisit un nom qui ne sera accepté que s'il ne ressemble pas à un nom déjà déclaré dans l'instance.
- il choisit une phrase secrète dont il sera vérifiée qu'elle ne ressemble pas à une phrase déjà déclarée dans l'instance.
- il rédige une courte note d'auto présentation 
- il donne la *phrase de contact* que lui a confié un compte parrain déjà enregistré et s'étant engagé, hors de l'application, à certifier le nom du nouveau compte.
- le compte parrain voit dans ses **Échanges** l'aperçu de cette note d'auto présentation qui exprime le souhait du nouveau compte d'être certifié.
- le compte est en état en création et peut rester dans cet état au plus quelques jours en disposant d'un crédit de ressources très limité.

Si le compte parrain accepte de certifier le nouveau compte :
- les deux comptes figurent dans leurs **Contacts** respectifs comme **contacts de confiance**, c'est à dire que chacun peut connaître de l'autre son nom et son certificat d'identité et peuvent partager des notes.
- le nouveau compte devient actif et son crédit peut être rechargé (en général le parrain fait un cadeau de bienvenue ou au moins une avance remboursable).
- le compte parrain a un compte supplémentaire dans *sa liste des comptes certifiés* de son profil et le nouveau compte a le compte parrain dans sa liste *des comptes certificateurs* de son profil. 

Si le compte parrain refuse de certifier le nouveau compte :
- le nouveau compte reste en création : il n'a toujours pas de comptes certificateurs dans son certificat d'identité (toujours vide).
- il peut lire le contenu de la note explicative du refus écrite par son parrain (espéré).
- son parrain espéré figure dans **Contacts** mais pas comme contact de confiance (et même peut-être sans nom).
- le nouveau compte doit chercher, rapidement, un autre parrain potentiel, obtenir de lui une phrase de contact et partager avec lui sa note de présentation demandant à être certifié par lui.
- au bout de quelques jours le compte en création est détruit automatiquement.

##### Phrases de contact
Un compte peut déclarer dans son profil quelques **phrases de contact**, garanties uniques dans l'instance. Chaque phrase a une date-heure limite fixée par le compte a sa déclaration et le compte peut détruire une de ses phrases ou la prolonger.  
Un compte A ne peut partager une note avec un compte C que :
- soit si A connaît le certificat d'identité de C (donc son nom) : 
    - soit A et C se sont réciproquement acceptés comme contact de confiance.
    - soit A et C participe à un même forum.
- soit si A connaît une phrase de contact en cours de validité de C que C lui a confiée,
    - soit hors de l'application après un contact direct.
    - soit par l'entremise d'un compte tiers, de confiance de A et de C, ayant accepté de relayer la phrase de contact que C lui a fourni pour A.

##### Phrase secrète d'administration et comptes auto-certifiés
Comme au départ il n'y a pas de comptes dans l'instance, le premier compte à s'inscrire ne peut pas passer par la procédure normale puisque personne ne peut certifier son identité.  
Il est possible de créer ou ou quelques comptes **auto-certifiés** qui connaissent la **phrase secrète d'administration**.

Cette phrase se transmet humainement hors de l'application : un brouillage complexe de cette phrase donne une clé de cryptage qui est mémorisée (elle-même brouillée) dans le configuration de l'instance : le serveur peut vérifier qu'un détenteur présumé de cette phrase l'est réellement sans que cette clé ne soit physiquement écrite quelque part.

Une personne connaissant la phrase secrète d'administration peut créer un compte **auto-certifié** en fournissant un brouillage de cette phrase après l'avoir saisie sur son terminal :
- la création du compte est immédiate.
- elle ne requiert pas qu'un autre compte certifie l'identité du compte créé.

Le premier compte créé dans une instance est par principe un compte auto-certifié, les suivants pouvant se faire certifier par le compte auto-certifié. Mis à part qu'ils se sont auto-certifiés à leur création, ces comptes sont comme les autres et peuvent d'ailleurs ultérieurement avoir leur identité certifiée par d'autres comptes.

#### Contacts connus d'un compte A
Un compte A peut avoir dans ses **Contacts** un compte C connu :
- par son seul numéro de compte.
- par son numéro de compte et son nom.
- par son numéro de compte, son nom et avoir accès à son certificat d'identité. C'est le cas pour :
    - tous les comptes inscrits sur au moins un même forum. Cette situation n'est pas pérenne si le compte quitte le forum ou que le forum est dissous.
    - les **contacts de confiance** : la confiance est mutuelle et cesse dès lors que l'un des deux décide d'y mettre fin.

Quand deux comptes sont **contacts de confiance**, de plus,
- l'un peut certifier l'identité de l'autre à condition qu'il le lui ait demandé.
- les deux peuvent se certifier leur identité mutuellement.
- l'un comme l'autre peut, quand il veut, cesser sa certification ou supprimer la certification que l'autre lui a accordé.

#### Partage d'une note
Un compte A peut partager une note N avec un autre compte C (voir les restrictions ci-après).
- C voit apparaître dans sa liste des notes un aperçu de la note nouvellement partagée :
    - qui la partage,
    - l'identifiant de la note,
    - son sujet (donc ses hashtags),
    - quelques informations comme la date-heure et si la note contient un texte (et de quelle taille) et une pièce jointe (quel type / quel taille).
- C peut décider :
    - de jeter cette note.
    - de la conserver en attente.
    - de l'obtenir et de la copier dans son espace : C a accès au contenu complet de la note.
    - l'obtention de la pièce jointe est une opération distincte qui ne peut se faire (et c'est facultatif) qu'après avoir obtenu la note.

**Oups !**  
A peut avoir un remord d'avoir partagé une note avec C :
- il peut tenter d'effacer l'aperçu reçu de C : mais ceci est sans effet si C a déjà obtenu la note (copié dans son compte).
- il peut effacer la note dans son compte : c'est aussi sans effet si C a déjà obtenu la note, mais ceci supprime aussi la pièce jointe et C ne l'a peut-être pas encore obtenue.

#### Restriction au partage de notes
N'importe qui ne peut pas partager une note avec qui il veut, ni inviter n'importe qui à un forum en partageant une note d'invitation.  
Le réseau est a-social : un compte ne peut partager des notes qu'avec les comptes pour lesquels une acceptation réciproque explicite est établie :
- tout compte avec lequel les certificats d'identité sont mutuellement connus :
    - **participant à un forum commun**.
    - **contact de confiance** dans leurs **Contacts** respectifs.
- tout compte pour lequel une *phrase de contact* est connue, le partage de note ayant pour objectif l'inscription à un forum ou l'établissement d'un contact de confiance.

#### Clôture d'un compte
Elle se passe en deux temps :
- le compte est d'abord mis en état zombie.
- après un certain temps en état zombie, destruction physique du compte. Les comptes et forums ayant un contact avec ce compte en sont informés.

Ceci laisse un délai de remord permettant :
- de prolonger l'état zombie.
- de revenir à l'état actif normal.

## Les Forum
Un compte peut créer un forum dès qu'il souhaite partager des notes avec plusieurs autres comptes sur un espace commun.
- le forum ainsi créé a un identifiant interne tiré au hasard à sa création.
- **le créateur devient le premier participant**, avec selon le type de gouvernance un statut de dirigeant / élu (ou simple membre dans une gouvernance démocratique directe).
- **il peut inviter certains des comptes**, en fait tous ceux avec qui il peut partager une note (d'invitation).

Ultérieurement tout participant peut aussi lancer des invitations qui selon les règles de gouvernance du forum, seront ou non soumises à approbation des autres participants.

Partager des informations privées à plusieurs suppose d'avoir confiance en ceux avec qui ce partage s'établit. Cette acquisition de confiance joue autant pour les **invitants** (*puis-je avoir confiance dans cet invité ?*) que pour **l'invité** (*avec qui vais-je partager des informations ?*).

A sa création un forum dispose de deux clés :
- une clé transmise aux invités avant qu'ils soient acceptés en tant que participants confirmés.
- une clé transmise à un invité lors de sa confirmation comme participant.

Une note d'un forum est inscrite à son répertoire avec l'indication de sa clé de cryptage :
- elle peut ainsi être lisible par les **invités**.
- elle peut n'est lisible que par les **participants confirmés**.

Le répertoire des participants liste les invités / participants avec pour chacun :
- son **nom** et son **certificat d'identité** cryptés par la clé transmise aux invités.
- son **statut** :
    - *invité en attente d'acceptation* : son certificat d'identité et son nom peuvent ne pas figurer encore s'il a été invité par une note accompagnée d'une phrase de contact.
    - *invité ayant accepté mais n'ayant pas encore été confirmé*. L'invité peut écrire des notes avec la seule clé transmise aux invités et c'est par ce moyen qu'il peut s'auto-présenter aux autres participants.
    - *participant confirmé* :
        - *simple*.
        - *dirigeant / élu* selon le modèle de gouvernance retenu. 
    - *résilié* :
        - *avant confirmation*.
        - *après confirmation*. 

Un invité qui est notifié d'une note d'invitation dispose donc de la clé de cryptage qui lui permet :
- **de lire la liste des participants**, leurs certificats d'identité (qui les certifient).
- **de lire la ou les notes et conversations déclarées lisibles aux invités** et qui peuvent présenter l'objet du forum, ses règles internes. Beaucoup de ces notes sont associées aux participants et présentent qui ils sont.

Ainsi un invité peut se forger une opinion sur le forum sur lequel il est invité et il peut choisir d'accepter cette invitation ou de la décliner.
- **s'il refuse l'invitation** il est marqué comme résilié avant confirmation.
- **S'il l'accepte**, sa note d'acceptation fait enregistrer son nom et son certificat d'identité (s'ils n'étaient pas déjà connus) pour que les autres participants sachent si oui ou non il est souhaitable de confirmer cette participation.
    - **si oui** l'invité devenu *participant confirmé* dispose dès lors de la clés de cryptage lui permet de lire aussi les notes cryptées pour les participants.
    - **si non**, l'invité est recalé. Il aura eu le temps de lire la liste des participants et la ou les quelques notes de présentation mais rien d'autre.

Dans les cas simples ou un invité est particulièrement et favorablement bien connu, *son invitation peut se faire avec approbation implicite* pour raccourcir le délai. Il reçoit dès l'invitation les clés permettant d'accéder à tout le forum, le serveur ne lui donnant effectivement accès qu'aux notes réservées aux invités tant que l'invitation n'a pas été accepté par l'invité.

#### Restrictions d'accès d'une note d'un forum
Toute note a pour propriétaire son créateur. Lui seul peut ensuite en transmettre la propriété à un autre participant. Le propriétaire d'une note a certains droits sur cette note :
- la détruire.
- en restreindre la lecture à certains participants.
- en restreindre l'écriture (la production d'une nouvelle version pour une note versionnée) à certains participants.

#### Vie d'un forum
La vie du forum est pour l'essentiel marquée par :
- **la lecture et l'écriture de notes** et de leurs pièces jointes.
- **des actes de gestion** :
    - invitation / confirmation / résiliation d'un participant et changement de statut entre *participant simple* et *dirigeant / élu*.
    - suppression de notes obsolètes, transfert de propriété d'une ou de plusieurs note.
    - débit / crédit du forum.
    - clôture du forum.
- **des votes sur les scrutins organisés** :
    - à propos de ces actes de gestion.
    - à propos du changement de mode de gouvernance et d'élection des élus.

##### Clôture d'un forum
Elle se passe en deux temps :
- une annonce de clôture proche qui le place en état zombie laissant le temps aux participants de copier des contenus jugés utiles et surtout ouvrant un délai de remord.
- une destruction effective un certain temps après.

Il est possible de réactiver un forum zombie quand on a un remord ou de prolonger l'état zombie.

La destruction effective supprime physiquement les données.

>**Remarque** : dans le cas le plus simple un forum s'ouvre instantanément sur demande d'un compte qui y invite immédiatement les contacts qu'il souhaite avec approbation par défaut. Une gouvernance par défaut s'installe, et les participants peuvent sur l'instant accepter l'invitation et participer à une conversation et s'échanger des notes.

# Confiance dans l'application
Ces données passent par deux applications, une terminale, l'autre dans le serveur, circulent sur internet et sont stockées dans une base de données. Tous ces niveaux techniques sont susceptibles d'être attaqués et déroutés.  
Les techniques de cryptographie employées offrent certaines garanties en elles-mêmes, mais pas toutes les garanties et le niveau de confiance à accorder à l'application n'est pas un sujet trivial et s'étudie en fonction des niveaux de confiance qu'on peut accorder à chacun de ces éléments.

Il est impossible de garantir que toutes les données seront toujours présentes et inviolées mais il est possible de garantir que pour certaines données :
- si elles ont été trouvées elles sont fiables et inviolées, c'est à dire effectivement produites par qui de droit.
- plus exactement il est possible de détecter si une telle donnée a été corrompue, donc si elle est fiable ou non. En règle générale les hackers ne se fatiguent pas à corrompre des données dont la corruption serait détectable, sauf seulement à rendre l'application inutilisable.

#### Confiance dans l'application terminale et le réseau Internet
C'est elle qui crypte, signe, décrypte et vérifie les signatures. Si l'application terminale, dans son ensemble, n'est pas de confiance, aucune confiance ne peut être accordée à l'application.
- son logiciel est open source et lisible en clair dans n'importe quel terminal. Il est vérifiable par tout lecteur ayant le minimum requis de compétence technique.
- il peut être disponible depuis des sources diverses, et en particulier des sources de confiance.
- l'application s'exécute dans un environnement technique de l'appareil dans lequel, usuellement et à juste titre, on a confiance : navigateur, système d'exploitation, matériel. On a toutefois relevé par exemple :
    - un fournisseur de haute renommée qui détournait les frappes au clavier de l'utilisateur.
    - des CPU de haute renommée également mais pas complètement étanches aux inspections indésirables des autres programmes.

La transmission avec le serveur utilise le protocole chiffré d'internet, dont pour l'instant il n'existe pas de preuve de fragilité.

>Ce niveau technique est supposé de confiance, sinon l'application n'est pas pertinente (ni la quasi totalité des autres d'ailleurs).

Dans ce contexte :
- les clés de cryptographie requises sont générées aléatoirement à la création du compte dans l'application terminale :
    - le nom du compte est crypté et les diverses clés (sauf la clé 0) sont scellés dans ***un ticket public immuable et inviolable portant le numéro du compte***.
    - la clé 0 *mère* est enregistrée cryptée par la phrase secrète utilisée à la création.
- aucun autre compte ne peut être créé (dans aucune instance d'ailleurs) et présenter le même numéro de compte avec un ticket public valide différent (un *fake*): c'est détectable.
- si l'application mémorise pour chaque numéro de compte son **ticket public immuable et scellé**, n'importe quel terminal de la planète peut en conserver des copies sur n'importe quels supports. Depuis ce ticket n'importe quel terminal, avec ou sans le logiciel de l'application :
    - **peut obtenir le nom du compte** à condition de disposer de plus de la clé 1 qui en autorise la lecture (le décryptage).
    - **s'il connaît ou croit connaître le nom du compte**, peut vérifier si c'est ou non le bon. Une tentative de recherche du nom scellé par force brute en essayant toutes les combinaisons est vouée à l'échec.
    - **peut s'adresser au compte en cryptant des données de telle sorte que seul le détenteur du compte puisse les décrypter**.
    - **principe d'inviolabilité et de non répudiation** : il peut vérifier qu'une note qui prétend avoir été horodatée et signée par un compte, l'a véritablement été en utilisant les clés ad hoc que nul ne peut avoir inventé. S'il possède la clé de cryptage d'une note, la signature lui indiquera si le contenu a été violé (n'en rien croire c'est un faux) ou non.

A la création du compte, la clé 0 mère est cryptée par la première phrase secrète.  
Ultérieurement, à condition de disposer pour un compte donné de la clé 0 cryptée par l'ancienne phrase secrète, une session peut ré-encrypter la clé 0 par une nouvelle phrase secrète.

>**Remarque** : il existe des moyens non techniques pour demander, et souvent finir par obtenir de son titulaire, la phrase secrète d'un compte. La violence certes, mais aussi plus fréquemment la persuasion couplée à l'imprudence du titulaire, sont les moyens les plus usuels.

>**Synthèse** : le système cryptographique garantit, même à un terminal indépendant de l'application, que muni du ticket public d'un compte identifié par son numéro :
>- il peut vérifier techniquement que le ticket n'est pas un fake,
>- que tout message envoyé à ce compte en utilisant la clé qui s'y trouve ne pourra être décrypté que par une application détenant au moins un ticket privé du compte et la phrase secrète associé.
>- que toute note prétendue horodatée et signée par ce ticket l'a vraiment été.
>- que la note n'a pas été altérée (après l'avoir décryptée par sa clé de cryptage).

### Confiance dans le serveur et la base de données
Les tickets et contenus sont générés, cryptés et scellés dans les applications terminales. Le serveur ne reçoit jamais aucun élément qui lui permette de décrypter ou crypter ces contenus : au plus peut-il vérifier que les signatures des contenus signent bien ce qu'elles prétendent signer, mais ce contrôle est juste destiné à décourager un éventuel hacker maladroit qui tenterait de pervertir les données à distance : la vraie et seule vérification crédible est celle refaite dans la session terminale lectrice des notes.

Une note stockée dans la base de données ne peut pas être décrypter sans disposer du ticket public de son auteur et de sa clé de cryptage.  
Le détournement / copie de la base de données ne peut fournir aucune note sans disposer de leurs clés, c'est à dire sans la complicité d'un compte disposant d'un accès régulier.  
Détourner une base ne servirait qu'à en obtenir les notes que le complice utilisateur peut avoir régulièrement.

#### Destruction d'informations
Techniquement une application serveur pirate peut intercepter les opérations demandées au serveur :
- son action ne peut être que destructrice : suppression de comptes, de notes, de participants à des forums, de contacts dans les répertoires des comptes.
- il ne peut pas créer des notes fake, ni de faux certificats d'identité, ni de faux participants à un forum : tout cela est détecté à la lecture dans les applications terminales.

De même une intervention directe sur la base de données ne que miter les données mais pas pervertir celles restantes.

>**Synthèse** : rendre une application inutilisable ou non crédible en raison des attaques destructives de ses données n'est pas un aspect secondaire.
>- en revanche tout ce qui s'y trouve peut être considéré comme crédible, non altéré.
>- il est parfois, mais rarement, possible de détecter que des données ont été détruites.

### URLs d'une note et de sa pièce jointe
Un peu paradoxalement un compte peut obtenir pour toute note qu'il peut décrypter une URL qui comporte l'identification de la note, sa clé de cryptage et une URL pour sa pièce jointe.  
Il peut transmettre cette URL à n'importe qui, la publier dans un blog ...

**Choquant pour une application a-sociale ?**
En fait non, c'est normal. Si un compte a normalement accès à une note, il peut bien la lire en clair. L'objectif de l'application est de partager des notes avec qui on veut et si on a décidé que C pouvait lire une note, c'est bien que C en verra le contenu en clair, pièce jointe comprise. Dès lors cette note en clair peut se retrouver sur un fichier et il peut bien la diffuser où il veut : c'est le risque pour toute information confidentielle. Dès lors qu'elle est connue de quelqu'un, il faut avoir confiance dans cette personne pour qu'elle reste non publiée, si tel était l'intention de son auteur.

Bref rien de nouveau sous le soleil : la seule information qui ne pourra jamais être publiée hors de la volonté de son auteur est celle qu'il n'a pas écrite ou du moins n'a jamais fait lire à personne. C'est exactement la cas d'une note d'un compte qui n'a jamais été partagée. Seul son auteur la connaît et il ne faut qu'il l'imprime (ou qu'il broie le papier sans l'avoir perdu de vue).

Dès lors fournir une URL contenant la clé de cryptage d'une note ne pose pas plus de problème que de transmettre une copie par fichier de la note.

Il est aussi possible d'obtenir l'URL d'une note sans sa clé de cryptage et de l'envoyer par e-mail : elle ne sera lisible que quand le destinataire aura reçu, peut-être par un autre canal (un SMS par exemple), la clé de cryptage.


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

# Schémas de gouvernance d'un forum
La gouvernance d'un forum concerne :
- **comment certains actes de gestion peuvent être décidés** :
    - invitation / confirmation / résiliation d'un participant et changement de statut entre *participant simple* et *dirigeant / élu*.
    - suppression de notes obsolètes, transfert de propriété d'une ou de plusieurs note.
    - débit / crédit du forum.
    - clôture du forum.
- **comment changer de mode de gouvernance** et élire des élus / dirigeants.

### Les trois modes de gouvernance
Tous les scrutins sont publics, le vote de chacun est public / connu / vérifiable.

##### *Autoritaire*
Tout dirigeant a tout pouvoir de gestion, seul, même s'il y a plusieurs dirigeants.

Le changement de mode de gouvernance ne peut venir que des dirigeants :
- sur décision de l'un d'entre eux.
- quand il n'a plus de dirigeant, a) soit que le dernier se soit auto exclu, b) soit qu'il ait lui-même décidé de renoncer à son statut de dirigeant et qu'il était le dernier.

le forum bascule en **démocratie directe** avec **scrutin constituant ouvert**.

##### Démocratie *représentative*
Un collège de délégués élus assument les décisions de gestion :
- sur un mode consensuel :
    - un délai maximal est défini pour prendre une décision.
    - si au bout de ce délai personne n'a voté *contre* la décision est actée.
    - si avant ce délai tout le monde a voté *pour* ou *blanc*, la décision est actée.
- sur vote majoritaire :
    - un délai maximal est défini pour prendre une décision.
    - un quorum est exprimé en absolu ou pourcentage.
    - le vote est acquis s'il y a plus de *pour* que la somme des votes *blanc* et *contre*, a) soit dès que le quorum est atteint, b) soit au bout du délai maximal. A noter qu'avec un quorum de 2, si les deux premiers votes sont *pour*, la décision est actée : c'est un moyen pour rendre les décisions *un peu* collégiales.

##### Démocratie *directe*
Même procédé qu'avec la démocratie représentative, tous les participants étant délégués.

#### Changement de mode de gouvernance 
Un scrutin peut être ouvert par n'importe qui n'importe quand (sauf en mode autoritaire) en présentant une nouvelle gouvernance fixant :
- le mode choisi.
- dans le cas des démocraties :
    - si la gestion en consensuelle ou vote majoritaire (et dans ce cas quel est le quorum -absolu ou en pourcentage du nombre de participants).
    - le nombre d'élus dans le cas de démocratie représentative.
    - la durée des scrutins.

Le scrutin par défaut pour changer de gouvernance est :
- un durée de scrutin de 5 jours.
- pas de quorum.
- majorité simple.

##### Election des N délégués
Chaque participant a 10 bulletins et les affectent sur les participants au forum (le cas échéant plusieurs pour un participant).  
A l'issue du scrutin les N participants ayant reçu le plus de votes sont élus délégués.


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
Une ***clé symétrique*** (AES-256) est générée **depuis une suite de 32 octets**.  
Un ***vecteur d'initialisation*** `iv` est **une suite de 16 octets**.  
Le texte crypté d'un texte source dépend de ces deux suites.
- la clé est détenue par son propriétaire.
- la suite `iv` peut être :
    - constante dans l'ensemble de l'application.
    - être générée aléatoirement et être jointe en texte du résultat crypté de manière à ce que le décryptage lise cette suite comme `iv`.

Le choix a été fait d'une suite de 4 octets, aléatoire au cryptage et placée en tête du texte crypté. `iv` est cette suite répétée 4 fois. De ce fait un même contenu cryptée deux fois par la même clé a deux valeurs encryptées différentes : sans la clé il est impossible de savoir si deux textes encryptés différents sont le résultat de l'encryptage de deux contenus différents ou du même contenu. Ceci évite, par exemple sur une base détournée, de pouvoir déterminer si des données cryptées sont identiques ou si des clés cryptées sont identiques : l'interprétation même des metadonnées obscures devient impossible.

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
- 0 ou 1. O:note immutable, 1:note versionnée.
- 8 caractères en base 64 URL représentant 6 octets dont la valeur binaire forme, en lecture décimale, la date-heure de création AAMMJJhhmmssSSS (180224225959001).
- 12 caractères en base 64 URL représentant un vecteur de 9 octets tirés au hasard pour éviter les collisions dans la même milliseconde de création.

Cet identifiant a l'avantage de pouvoir être considéré comme universel (toutes instances confondues).

### `TPC` : ticket public d'un compte
Ce ticket a été généré par la session créatrice du compte et comporte les propriétés suivantes :
- `dh` : date-heure de génération.
- **alias** : `nomrBD` : SHA-256 du BCRYPT du nom réduit utilisé pour détecter et interdire l'usage de noms trop proches.
- `c1O` : clé 1 du compte cryptée par la clé 0. Elle crypte le nom du compte `nom1` ci-dessous et ceux des comptes certifiant son identité.
- `nomBD` : SHA-256 du BCRYPT du nom.
- `nom1` : nom crypté par sa clé 1.
- `pub` : clé RSA publique de cryptage.
- `verif` : clé RSA publique de vérification.
- `priv0` : clé privée RSA de décryptage cryptée par la clé 0.
- `sign0` : clé privée RSA de signature cryptée par la clé 0.

**La clé de la constante** est le SHA-256 de `pub` et est le numéro de compte.

>Remarque : les trois identifiants majeurs, numéro de compte, numéro de forum, numéro d'une note sont des identifiants universels qui n'ont pas de possibilité relle de collision même entre instances différentes. Ils ont l'avantage d'avoir des longueurs fixes et différentes (32, 21 et 20 caractères).
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
Chaque version d'une note a sa clé tirée au hasard à sa constitution. Un compte qui aurait eu accès à une note d'une version 1 et aurait été résiliée, ne pourrait pas lire les versions postérieures à sa résiliation même sur une base détournée.

### Clés d'un forum
Un forum a :
- une clé dite I disponible aux invités au forum. Ceux-ci pouvant refuser ou être exclu, peuvent avoir conserver cette clé obtenu du temps où ils étaient invités. Les notes cryptées par la clé I sont donc de protection de niveau 2 (décryptable depuis une base détournée par un compte ayant eu un jour un accès licite comme invité).
- une clé dite F disponible pour les participants au forum. Les notes du forum sont cryptées par cette clé (ou la clé I). C'est une protection de niveau 2. (à étudier : changement de clé F à chaque exclusion ? Il faut réencrypter la nouvelle par toutes les clés publiques de tous les participants ce qui est faisable si elles sont stockées dans le Forum et non dans le compte). Les notes nouvelles utilisent la nouvelle clé F (pour la I c'est moins grave mais ça pourrait se faire aussi).

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

