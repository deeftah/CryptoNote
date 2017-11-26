package fr.cryptonote.base;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import fr.cryptonote.provider.DBProvider;

public class ExecContext {
	private static TimeZone timezone;
	private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.FRANCE);
	
	public static ExecContext current() { return ExecContextTL.get(); }
	private static final ThreadLocal<ExecContext> ExecContextTL = new ThreadLocal<ExecContext>();

	
	private int iLang;
	public int iLang() { return iLang; }
	
	private int phase;
	public int phase() {return phase; }
	
	public DBProvider dbProvider() { return null; }

	public Document getDoc(Document.Id id, int maxDelayInSeconds) {return null;}
	public Document getDoc(Document.Id id) {return null;}
	public Document newDoc(Document.Id id) {return null;}
	public Document getOrNewDoc(Document.Id id) {return null;}
	public void deleteDoc(Document.Id id) {}

}
