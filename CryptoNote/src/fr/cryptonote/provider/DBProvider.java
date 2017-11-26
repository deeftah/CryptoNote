package fr.cryptonote.provider;

import java.util.Collection;
import java.util.HashSet;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.Cond;
import fr.cryptonote.base.Document;
import fr.cryptonote.base.Document.XItem;
import fr.cryptonote.base.Document.XItemFilter;
import fr.cryptonote.base.TaskInfo;

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
	
	public boolean hasMC();

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
