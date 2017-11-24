package fr.cryptonote.provider;

import fr.cryptonote.base.AppException;

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

}
