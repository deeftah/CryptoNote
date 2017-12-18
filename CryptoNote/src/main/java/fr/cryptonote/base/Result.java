package fr.cryptonote.base;

public final class Result {
	public Object out;
	public String syncs;
	public transient String text;
	public transient String encoding;
	public transient byte[] bytes;
	public transient String mime;
	
	public boolean isEmpty() {
		return out == null && syncs == null && text == null && bytes == null;
	}
}
