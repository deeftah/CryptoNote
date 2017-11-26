package fr.cryptonote.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashSet;

import fr.cryptonote.base.AppException;


public class BlobProvider {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public BlobProvider(String blobsroot, String ns){
		this.blobsroot = blobsroot;
		this.ns = ns;
	}
	
	public String urlencode(String s) {	try { return URLEncoder.encode(s, "UTF-8"); } catch (UnsupportedEncodingException e) { return ""; }	}
	
	String blobsroot;
	String ns;

	public void blobDeleteAll(String clid) throws AppException {}
	
	public void blobStore(String clid, String sha, byte[] bytes) throws AppException {}

	public byte[] blobGet(String clid, String sha) throws AppException { return new byte[0]; }

	/**
	 * Vide la corbeille, ne conserve que les shas utiles et met les autres à la corbeille. 
	 * @param clid : identifiant du documment (classe.docid)
	 * @param uids liste des uid encore référencés
	 * @return true si la corbeille est vide après cleanup, sinon elle contient des fichiers à supprimer plus tard.
	 * @throws AppException
	 */
	 public boolean cleanup(String clid, HashSet<String> shas) throws AppException { return true; }
}
