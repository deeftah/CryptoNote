package fr.cryptonote.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.Cond;
import fr.cryptonote.base.Document;
import fr.cryptonote.base.Document.XItem;
import fr.cryptonote.base.Document.XItemFilter;
import fr.cryptonote.base.ExecContext.ExecCollect;
import fr.cryptonote.base.TaskInfo;
import fr.cryptonote.base.TaskInfo.TaskMin;
import fr.cryptonote.base.UpdDiff.ByTargetClkey;

public interface DBProvider {
	public String ns();
	public DBProvider ns(String ns);
	public String operationName();
	public DBProvider operationName(String operationName);
	
	public void rollbackTransaction();
	public void closeConnection();
	
	public Object connection() throws AppException;
	
	public String dbInfo(String info) throws AppException ;
	
	public void recordHourStats(int hour, String ns, String json) throws AppException;

	public void setOnOff(String ns, int onoff) throws AppException;

	public HashMap<String,Integer> getOnOff() throws AppException;

	public BlobProvider blobProvider() throws AppException;
	
	public void shutdwon();
		
	/***********************************************************************************************************/
	public Collection<DeltaDocument> listDoc(String BEGINclid, long AFTERversion) throws AppException;

	public static class DeltaDocument {
		public Document.Id id;
		public long version;
		public long ctime;
		public long dtime;
		public int cas;
		public HashMap<String, CItem> items = new HashMap<String, CItem>();
		public HashSet<String> clkeys = new HashSet<String>();
	}
	
	/*
	 * Exemplaire en cache : version:v   ctime:c   dtime:d
	 * Exemplaire en base  : version:vdb ctime:cdb dtime:ddb
	 * L'exemplaire en cache est, soit vide (v == 0), soit à niveau (v == vdb), soit retardé (v < vdb).
	 * Un exemplaire en cache est TOUJOURS une image qui existe ou a existé en base.
	 * 
	 *  Base vide : retourne null
	 *  
	 *  cas = 0 : cache à niveau (v == vdb) cas = 0 . rien à remplir
	 *  
	 *  Cache ayant une version retardée (v < vdb)
	 *  
	 *  cas = 1 : cache vide (v == 0
	 *  cas = 2 : cache ayant une vie antérieure (c < cdb)
	 *  Remplit une copie complète simple :  items : tous les items existants  +  tous les items détruits
	 *  
	 *  Cache ayant une version retardée de la même vie
	 *  
	 *  cas 3 : v >= dtb. Le cache ne contient pas d'items détruits dont la suppression serait inconnue de la base
	 *  delta - items : tous les items existants modifiés après v + tous les items detruits après v
	 *  
	 *  cas 4 : v < dtb. Le cache peut contenir des items détruits dont la destruction est inconnue de la base
	 *  items : tous les items existants modifiés après v + tous les items detruits après dtb (on en a pas avant, donc après v c'est pareil)
	 *  clkeys : clés des items existants qui ne figure pas dans items.
	 *  
	 *  C'est une transaction : il y a de 1 à 3 requêtes qui doivent être consistantes entre elles.
	 */
	
	/**
	 * Retourne le delta entre le document en cache et celui en base.
	 * @param id
	 * @param ctime
	 * @param version
	 * @param dtime
	 * @return null si le document n'existe pas.
	 */
	public DeltaDocument getDocument(Document.Id id, long ctime, long version, long dtime) throws AppException ;
	
	/*
	 * Mise à jour par document cible des items dupliqués.
	 * Pour chaque item, update seulement si vop > version de l'item en base;
	 */
	public void rawDuplicate(long vop, HashMap<String,ArrayList<ByTargetClkey>> byDoc)  throws AppException;
	
	/**
	 * Fin de transaction de lecture ou de mise à jour
	 * @param collect documents et parts à mettre jour, groups à checker
	 * @throws AppException
	 */
	public HashMap<String,Long> validateDocument(ExecCollect collect) throws AppException;

	/***********************************************************************************************************/
	
	/**
	 * Recherche d'une liste de documents dont un au moins des items a un champ indexé
	 * repondant au filtre.
	 * @param docClass : obligatoire, classe des documents ayant les items filtrés
	 * @param itemClass : obligatoire, classe des items filtrés
	 * @param ffield : au moins une condition obligatoire. Condition portant sur les valeurs d'un des champs indexés
	 * @return Collection des identifiants de documents où un item a été trouvé
	 * @throws AppException
	 */
	public Collection<Document.Id> searchDocIdsByIndexes(Class<?> docClass, Class<?> itemClass,	Cond<?>... ffield) throws AppException;

	/**
	 * Recherche d'une liste d'items ayant un champ indexé repondant au filtre.
	 * @param docClass : obligatoire, classe des documents ayant les items filtrés
	 * @param itemClass : obligatoire, classe des items filtrés
	 * @param filter : un objet de filtre qui décide si un XItem fait ou non partie du résultat
	 * @param ffield : au moins une condition obligatoire. Condition portant sur les valeurs d'un des champs indexés
	 * @return Collection des Item trouvés
	 * @throws AppException
	 */
	public Collection<XItem> searchItemsByIndexes(Class<?> docClass, Class<?> itemClass, XItemFilter filter, Cond<?>... ffield) throws AppException;

	/** Tasks (sauf Datastore) ************************************************/
	/**
	 * Création d'une task par l'administrateur ou par validation ou par s2cleanup
	 * @param ti
	 * @throws AppException
	 */
	public void insertTask(TaskInfo ti) throws AppException ;

	public TaskInfo taskInfo(String ns, String taskid) throws AppException;

	/**
	 * Lancement d'une task, fixe startTime et incrémente retry. startAt et exc sont mis à null.<br>
	 * @param ns
	 * @param taskid
	 * @param startTime
	 * @return TaskInfo mis à jour. null si la tâche n'existe plus.
	 * @throws AppException
	 */
	public TaskInfo startTask(String ns, String taskid, long startTime) throws AppException ;

	/**
	 * Sortie d'une task en exception. Il faut que que startTime et step soient égaux en ti et dans TaskQueue
	 * sinon c'esst une exception portant sur une exécution parasite à ignorer.
	 * @param ti
	 * @param exc AppException
	 * @throws AppException
	 */
	public void excTask(TaskInfo ti, AppException exc) throws AppException ;

	
	/**
	 * Fin d'une étape d'une tâche avec relance d'une nouvelle requête pour continuation.
	 * @param ns
	 * @param taskid
	 * @param param paramètre de la prochaine étape
	 * @param toStartAt date-heure de lancement de la prochaine étape
	 * @return false si la task était parasite
	 * @throws AppException
	 */
	public boolean stepTask(TaskInfo ti, String param, long toStartAt) throws AppException ;
	
	/**
	 * Fin d'une étape d'une tâche et son étape suivante se poursuit dans la même requête.
	 * @param taskInfo
	 * @param param paramètre de la prochaine étape
	 * @return false si la task était parasite
	 * @throws AppException
	 */
	public boolean stepTask(TaskInfo ti, String param) throws AppException ;

	/**
	 * Fin de la dernière étape avec ou sans conservation du résultat pendant un certain temps
	 * @param ns
	 * @param taskInfo
	 * @param toPurgeAt
	 * @param param report synthétique de la task : si null pas de conservation et la task est purgée
	 * @return false si la task était parasite
	 * @throws AppException
	 */
	public boolean finalTask(TaskInfo ti, long toPurgeAt, String param) throws AppException;
	
	/**
	 * Suppression d'une task sur demande de l'administrateur
	 * @param taskInfo
	 * @throws AppException
	 */
	public void removeTask(String ns, String taskid) throws AppException ;

//	/**
//	 * Liste les tasks en attente filtré le cas échéant par les paramètres de ti
//	 * @param ti
//	 * ti.id.docclass : restreint aux tasks de cette classe
//	 * ti.id.docid : restreint à celles commençant par ce docid
//	 * ti.nexstart : restreint à celles à lancer avant et à cette stamp
//	 * ti.cron : restreint à celles de ce cron
//	 * @return
//	 * @throws AppException
//	 */
//	public Collection<TaskInfo> listTask(String BEGINclid, long AFTERnextstart, int MINretry, String CONTAINSinfo) throws AppException ;

	public Collection<TaskMin> candidateTasks(long before, Collection<String> listNs) throws AppException ;

	/**
	 * Retourne le report d'une task 
	 * @param ns
	 * @param taskid
	 * @return
	 * @throws AppException
	 */
	public String taskReport(String ns, String taskid) throws AppException;

	/*************************************************************************************/
	/**
	 * Retourne la liste des sha des pièces jointes utiles (référencées) dans un document.
	 * @param Id du document
	 * @return set des shas des pièces jointes.
	 * @throws AppException
	 */
	public HashSet<String> shas(Document.Id id) throws AppException;

	/**
	 * Retourne la dernière heure identifiant la prochaine task de clean up planifiée 
	 * (ou la dernière exécutée) du stockage S2 pour le document clid.
	 * @param clid : identifiant du document
	 * @return l'heure de la prochaine task.
	 * @throws AppException
	 */
	public int lastS2Cleanup(String clid) throws AppException;

	/**
	 * Planifie le prochain cleanup à exécuter. C'est une transaction
	 * indépendante ou incluse dans celle de validation selon le cas.
	 * Le startAt de la task donne le hour du cleanup
	 * @param ti TaskInfo de la tâche à insérer
	 * @param clid id du document à nettoyer
	 * @throws AppException
	 */
	public void setS2Cleanup(TaskInfo ti, String clid) throws AppException ;

}
