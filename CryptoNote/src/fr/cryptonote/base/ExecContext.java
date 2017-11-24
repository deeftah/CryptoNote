package fr.cryptonote.base;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class ExecContext {
	private static TimeZone timezone;
	private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.FRANCE);
	
	public static ExecContext current() { return ExecContextTL.get(); }
	private static final ThreadLocal<ExecContext> ExecContextTL = new ThreadLocal<ExecContext>();

	
	private int iLang;
	public int iLang() { return iLang; }
	
	private int phase;
	public int phase() {return phase; }
}
