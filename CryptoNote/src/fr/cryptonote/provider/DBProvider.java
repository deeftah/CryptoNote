package fr.cryptonote.provider;

import fr.cryptonote.base.AppException;
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
	 * Retourne la dernière heure identifiant la prochaine task de clean up planifiée 
	 * (ou la dernière exécutée) du stockage S2.
	 * @return
	 * @throws AppException
	 */
	public int lastS2Cleanup() throws AppException;

	/**
	 * Planifie le prochain cleanup à exécuter. C'est une transaction
	 * indépendante ou incluse dans celle de validation selon le cas.
	 * @param ti. docid=hour du cleanup (AAMMJJhh du nextStart); docclass=s2cleanup
	 * @param transaction si true, transaction indépendante
	 * @throws AppException
	 */
	public void setS2Cleanup(TaskInfo ti, boolean transaction) throws AppException ;

}
