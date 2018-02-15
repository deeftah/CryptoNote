package fr.cryptonote.provider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

	@Override public void blobDeleteAll(String docid) throws AppException { deleteDirectory(path(docid, null).toFile()); }

	public void blobDelete(String docid, String sha) throws AppException {
		File file = path(docid, sha).toFile();
		if (file.exists() && file.isFile()) file.delete();
	}
		
	@Override public void blobStore(String docid, String sha, byte[] bytes)  throws AppException {
		try {
			Path path = path(docid, sha);
			Path tmp = path(docid, sha + ".tmp");
			Files.createDirectories(path(docid, null));
			File f = path.toFile();
			if (f.exists()) 
				f.delete();
			File t = tmp.toFile();
			if (t.exists()) 
				t.delete();
			Files.write(tmp, bytes);
			Files.move(tmp,  path, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new AppException(e, "XFILE0", docid + "/" + sha);
		}
	}

	@Override public byte[] blobGet(String docid, String sha) throws AppException {
		Path path = path(docid, sha);
		File file = path.toFile();
		if (!file.exists() || !file.isFile()) return null;
		try { return Files.readAllBytes(path); } catch (IOException e) { throw new AppException(e, "XFILE0", path.toString()); }
	}
				
	@Override public void cleanup(String docid, HashSet<String> shas) throws AppException { 
		File[] files = path(docid, null).toFile().listFiles();
        for(File f : files)
        	if (f.isFile() && !shas.contains(f.getName())) f.delete();
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
			
			bp.cleanup("g1", h);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}
