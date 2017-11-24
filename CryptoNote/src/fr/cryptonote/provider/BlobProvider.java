package fr.cryptonote.provider;

import java.util.HashSet;

import fr.cryptonote.base.AppException;


public class BlobProvider {
	public BlobProvider(String blobsroot, String ns){
		this.blobsroot = blobsroot;
		this.ns = ns;
	}
	
	String blobsroot;
	String ns;

	public void blobDeleteAll() throws AppException {}
	
	public void blobStore(String groupid, String sha, byte[] bytes) throws AppException {}

	public byte[] blobGet(String groupid, String sha) throws AppException { return new byte[0]; }

	/**
	 * Vide la corbeille, ne conserve que les uids utiles et met les autres
	 * à la corbeille. 
	 * @param uids liste des uid encore référencés dans le groupe
	 * @return true si la corbeille est vide après cleanup, sinon elle contient des fichiers à supprimer plus tard.
	 * @throws AppException
	 */
	public boolean cleanup(String groupid, HashSet<String> shas) throws AppException { return true; }
}
