package fr.cryptonote.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashSet;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.BConfig;
import fr.cryptonote.base.BConfig.Nsqm;
import fr.cryptonote.base.BConfig.S2Storage;


public class BlobProvider {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public BlobProvider(String ns) throws AppException{
		nsqm = BConfig.namespace(ns, false);
		if (nsqm == null) throw new AppException("BNAMESPACENO", ns);
		s2Storage = BConfig.s2Storage(nsqm.base());
		this.ns = ns;
	}

	public BlobProvider(S2Storage s2, String ns) throws AppException{
		s2Storage = s2;
		this.ns = ns;
	}
	
	public String urlencode(String s) {	try { return URLEncoder.encode(s, "UTF-8"); } catch (UnsupportedEncodingException e) { return ""; }	}
	
	S2Storage s2Storage;
	String ns;
	Nsqm nsqm;

	public void blobDeleteAll(String docid) throws AppException {}
	
	public void blobStore(String docid, String sha, byte[] bytes) throws AppException {}

	public byte[] blobGet(String docid, String sha) throws AppException { return new byte[0]; }

	/**
	 * Vide la corbeille, ne conserve que les shas utiles et met les autres à la corbeille. 
	 * @param docid : identifiant du documment (docid)
	 * @param shas liste des shas encore référencés
	 * @throws AppException
	 */
	 public void cleanup(String docid, HashSet<String> shas) throws AppException { return; }
}
