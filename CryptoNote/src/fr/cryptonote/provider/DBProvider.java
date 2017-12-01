package fr.cryptonote.provider;

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
import fr.cryptonote.base.TaskUpdDiff.Upd;

public interface DBProvider {
	public String ns();
	public String operationName();
	public void operationName(String operationName);
	
	public void rollbackTransaction();
	public void closeConnection();
	
	public Object connection() throws AppException;
	
	public String dbInfo(String info) throws AppException ;
	
	public BlobProvider blobProvider();
	
	public void shutdwon();
		
	/***********************************************************************************************************/
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
	 *  cas = 0 : cache à niveau (v == vdb) cas = 0 
	 *  
	 *  Cache retardée (v < vdb)
	 *  
	 *  cas = 1 : cache vide
	 *  cas = 2 : cache ayant une vie antérieure (c < cdb)
	 *  C'est une copie complète simple qui est retournée dispatchée entre :
	 *  items : tous les items existants
	 *       +  tous les items détruits
	 *  
	 *  Vie courante retardée
	 *  
	 *  cas 3 : v >= dtb. Le cache ne contient pas d'items détruits dont la suppression serait inconnue de la base
	 *  items : tous les items existants modifiés après v
	 *        + tous les items detruits après v
	 *  
	 *  cas 4 : v < dtb. Le cache peut contenir des items détruits dont la destruction est inconnue de la base
	 *  items : tous les items existants modifiés après v
	 *        + tous les items detruits après dtb (on en a pas avant)
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
	public DeltaDocument getDocument(Document.Id id, long ctime, long version, long dtime);
	
	/*
	 * Mise à jour en une transaction de tous les items de upd dans le document d'id donnée.
	 * Pour chaque item, upsert seulement si le champ cas (s'il existe) de l'item en base 
	 * est inférieur au cas donné dans l'item à insérer / mettre à jour;
	 */
	public void rawStore(Document.Id id, Upd upd)  throws AppException;
	
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
	public Collection<Document.Id> searchDocIdsByIndexes(String docClass, String itemClass,	Cond<?>... ffield) throws AppException;

	/**
	 * Recherche d'une liste d'items ayant un champ indexé repondant au filtre.
	 * @param docClass : obligatoire, classe des documents ayant les items filtrés
	 * @param itemClass : obligatoire, classe des items filtrés
	 * @param filter : un objet de filtre qui décide si un XItem fait ou non partie du résultat
	 * @param ffield : au moins une condition obligatoire. Condition portant sur les valeurs d'un des champs indexés
	 * @return Collection des Item trouvés
	 * @throws AppException
	 */
	public Collection<XItem> searchItemsByIndexes(String docClass, String itemClass, XItemFilter filter, Cond<?>... ffield) throws AppException;

	/** Tasks (sauf Datastore) ************************************************/
	/**
	 * Création d'une task par l'administrateur ou par validation ou par s2cleanup
	 * @param ti
	 * @throws AppException
	 */
	public void insertTask(TaskInfo ti) throws AppException ;

	/**
	 * Mise à jour de nexstart / retry / report d'une task
	 * Fin d'exécution en erreur (qm) ou relance (qm-admin)
	 * @param ti
	 * @throws AppException
	 */
	public void updateNRRTask(Document.Id id, long nextStart, int retry, String report) throws AppException ;

	/**
	 * Suppression d'une task
	 * Fin d'exécution OK (qm) ou renoncement à une tâche (qm-admin / app)
	 * @param ti
	 * @throws AppException
	 */
	public void removeTask(Document.Id id) throws AppException ;

	/**
	 * Liste les tasks en attente filtré le cas échéant par les paramètres de ti
	 * @param ti
	 * ti.id.docclass : restreint aux tasks de cette classe
	 * ti.id.docid : restreint à celles commençant par ce docid
	 * ti.nexstart : restreint à celles à lancer avant et à cette stamp
	 * ti.cron : restreint à celles de ce cron
	 * @return
	 * @throws AppException
	 */
	public Collection<TaskInfo> listTask(TaskInfo ti) throws AppException ;
		
	/**
	 * Retourne le report d'une task 
	 * @param ti
	 * @return
	 * @throws AppException
	 */
	public String taskReport(Document.Id id) throws AppException;

	/*************************************************************************************/
	/**
	 * Retourne la liste des sha des pièces jointes utiles (référencées) dans un document.
	 * @param clid
	 * @return set des shas des pièces jointes.
	 * @throws AppException
	 */
	public HashSet<String> shas(String clid) throws AppException;

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
	 * @param ti. docid=hour du cleanup (AAMMJJhh du nextStart); docclass=s2cleanup
	 * @param transaction si true, transaction indépendante
	 * @throws AppException
	 */
	public void setS2Cleanup(TaskInfo ti, boolean transaction) throws AppException ;

}
