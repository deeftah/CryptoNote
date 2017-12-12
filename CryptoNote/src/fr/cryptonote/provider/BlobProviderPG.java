package fr.cryptonote.provider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.BConfig.S2Storage;

public class BlobProviderPG extends BlobProvider {
	public static final String basketPath = "__basket__";
		
	public BlobProviderPG(String ns) throws AppException{
		super(ns);
	}
	
	public BlobProviderPG(S2Storage s2, String ns) throws AppException {super(s2, ns);}
	
	/** Blob store **************************************************/	
	private Path path(String clid, String sha){
		String root = s2Storage.blobsroot() + "/" + ns ;
		int f = clid.hashCode() % 100;
		String g = urlencode(clid);
		return sha == null ? Paths.get(root + "/" + f, g) : Paths.get(root, g, sha);
	}

	private static void deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
	        for(File f : files)
	        	if (f.isDirectory()) deleteDirectory(f); else f.delete();
			path.delete();
		}
	}

	@Override public void blobDeleteAll(String clid) throws AppException { deleteDirectory(path(clid, null).toFile()); }

	public void blobDelete(String clid, String sha) throws AppException {
		File file = path(clid, sha).toFile();
		if (file.exists() && file.isFile()) file.delete();
	}
		
	@Override public void blobStore(String clid, String sha, byte[] bytes)  throws AppException {
		// TODO
		/*
		 * Pour les fichiers au-dessus de 4K, créer un temp et le renommer
		 * Dans tous les cas tester l'existence du fichier avant de le réécrire
		 */
		Path path = path(clid, sha);
		try {
			Files.createDirectories(path(clid, null));
			Files.write(path, bytes);
		} catch (IOException e) {
			throw new AppException(e, "XFILE0", path.toString());
		}
	}

	@Override public byte[] blobGet(String clid, String sha) throws AppException {
		Path path = path(clid, sha);
		File file = path.toFile();
		if (!file.exists() || !file.isFile()) return null;
		try { return Files.readAllBytes(path); } catch (IOException e) { throw new AppException(e, "XFILE0", path.toString()); }
	}
	
	private String[] linesGet(String clid) throws AppException {
		try {
			byte[] x = blobGet(clid, basketPath);
			return x == null || x.length == 0 ? null : new String(x, "UTF-8").split("\n");
		} catch (UnsupportedEncodingException e) { return null; }
	}
	
	/*
	 * Supprime physiquement tous les fichiers dont le nom ne figure pas dans la liste des fichiers utiles.
	 */
	private void emptyBasket(String clid, HashSet<String> shas) throws AppException {
		String[] lst = linesGet(clid);
		if (lst != null) {
			for(String p : lst) if (shas == null || !shas.contains(p)) blobDelete(clid, p);
			blobDelete(clid, basketPath);
		}
	}
	
	/* Retourne la liste des shas dans le directory clid, en excluant ceux utiles (shas) et la basket elle-même.
	 * Bref liste les shas inutiles.
	 */
	private byte[] blobToBasket(String clid, HashSet<String> shas) throws AppException {
		String n = "";
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    try {
			File[] files = path(clid, null).toFile().listFiles();
	        for(File f : files)
	        	if (f.isFile()) {
	        		n = f.getName();
	        		if (!basketPath.equals(n) && !shas.contains(n)) bos.write((n + "\n").getBytes(UTF8));
	        	}
	    	byte[] bytes = bos.toByteArray();
	    	bos.close();
	    	return bytes;
		} catch (IOException e) { throw new AppException(e, "XFILE0", clid + "/" + n); }
	}
	
	/*
	 * A chaque tour de cleanup, 
	 * a) la basket est vidée : les fichiers dont les noms y figuraient sont détruits et le fichier basket lui-même l'est.
	 * b) la basket contient la liste des fichiers inutiles qu'il faudra purger au tour cleanup suivant (purgatoire)
	 */
	@Override public boolean cleanup(String clid, HashSet<String> shas) throws AppException { 
		// TODO Nettoyer aussi les temp
		emptyBasket(clid, shas);		
		byte[] bk = blobToBasket(clid, shas);
		if (bk.length == 0)	return false;
		blobStore(clid, basketPath, bk); // stocke basket (non vide)
		return true;
	}
	
	public static void main(String[] args) {
		try {
			S2Storage s2 = new S2Storage("/tmp/blobsroot", "bk1");
			BlobProviderPG bp = new BlobProviderPG(s2, "cna");
			byte[] t = "toto".getBytes(UTF8);
			bp.blobStore("g1", "abc", t);
			bp.blobStore("g1", "abcd", t);
			bp.blobStore("g1", "abcde", t);
			
			HashSet<String> h = new HashSet<String>();
			h.add("abc");
			h.add("abcde");
			
			boolean basket = bp.cleanup("g1", h);
			System.out.println("basket: " + basket);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}
