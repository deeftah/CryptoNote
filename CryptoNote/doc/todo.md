reqerr-panel à compléter / tester

Doc serveur à ajuster
Test / exemple

Une tâche a un résultat : c'est un petit objet *report* qui synthétise le niveau d'avancement de la tâche, soit à son dernier point de reprise, soit à sa fin.

La phase worj() d'une tâche se finit sous deux options :
- fin de la tâche : 
    - l'objet résultat est le report synthtétique de la réalisation;
    - l'objet param est mis à null.
    - 1) la TaskInfo a une date-heure startAt à null et une date-heure purgeAt qui indique quand le TaskInfo devra être purgé.
    - 2) la TaskInfo est purgée immédiatement, son report n'est plus accessible.
- point de reprise :
    - l'objet résultat est un report synthétique sur le niveau d'avancement.
    - l'objet param est enregistré dans TaskInfo : il a été mis à jour par work() de manière à ne plus contenir 
    que les paramètres nécessaires pour l'étape suivante, le cas échant avec simplement une modification du nombre d'items restant à traiter.
    - la requête valide ses mises à jour et la mise à jour de param et report. Elle boucle sur une phase suivante work().
    - pour le Queue Manager c'est toujours la même tâche qui est en exécution.
    - en cas de sortie en exception, le param contient tout ce qu'il faut pour redémarrer le travail au dernier point de reprise.

En cas de sortie en exception :
- le code de l'exception exc est enregistré.
- son détail complet (et stack) est enregsitré en detail.
- son indice de retry est incrémenté. Cet indice est remis à 0 à chaque point de reprise sorti en succès, exc et detail étant remis à null.
