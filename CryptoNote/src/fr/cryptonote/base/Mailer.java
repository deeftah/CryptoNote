package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mailer {

	private static HashMap<String,String> mailers;
	
	private static class MailInfo {
	 	String name;
	 	String host;
	 	int port;
	 	String username;
	 	String from;
	  	boolean starttls = false;
	  	boolean auth = false;
	 	String password;
	 	boolean isDefault;
	 	transient Session session = null;
	 	
	 	private Session session(){
	 		if (session == null){
	 			Properties props = new Properties();
	 			if (auth) props.put("mail.smtp.auth", "true");
	 			if (starttls) props.put("mail.smtp.starttls.enable", "true");
	 			props.put("mail.smtp.host", host);
	 			props.put("mail.smtp.port", "" + port);
	 			session = Session.getDefaultInstance(props,
 					new javax.mail.Authenticator() {
 						protected PasswordAuthentication getPasswordAuthentication() {
 							return new PasswordAuthentication(username,password);
 						}
 					});
	 		}
	 		return session;
	 	}
	}
	
	private static class MailConfig {
		HashMap<String,MailInfo> mailInfos;
		String mailserver;
		String adminMails;
		String[] emailfilter;
	}
	private static MailInfo defMI;
	private static MailConfig mailConfig;
	private static String subjectPrefix;
	private static String httpServerPWD;
	
	static void initMailers() {
		mailers = BConfig.mailers();
		Servlet.Resource r = Servlet.getResource("/WEB-INF/mail.json");
		if (r != null)
			try {
			mailConfig = JSON.fromJson(Util.fromUTF8(r.bytes), MailConfig.class);
			} catch (Exception ex){
				Util.log.severe("Configuration mailer.jon mal formée : " + ex.getMessage());
				return;				
			}
		else {
			Util.log.severe("Configuration mailers absente");
			return;
		}
		for(String key : mailConfig.mailInfos.keySet()){
			MailInfo mi = mailConfig.mailInfos.get(key);
			String pwd = mailers.get(key);
			if (pwd != null)
				mi.password = pwd;
			if (mi.isDefault)
				defMI = mi;
		}
		if (defMI == null)
			Util.log.severe("Configuration mailers : pas de mailer marqué isDefault");
		subjectPrefix = mailConfig.emailfilter != null && mailConfig.emailfilter.length != 0 ? "[IGNORER (TEST)] " : "";
		httpServerPWD = mailers.get("httpserver");
	}
	
	static {
		initMailers();
	}
	
	private static MailInfo mailInfo(String code){
		MailInfo mi = mailConfig.mailInfos.get(code);
		return mi == null ? defMI : mi;
	}
	
	private static String[] normEMails(String lst, boolean filtered){
		if (lst == null || lst.length() == 0)
			return new String[0];
		if ("ADMIN".equals(lst)) lst = mailConfig.adminMails;
		ArrayList<String> l = new ArrayList<String>();
		String[] emf = mailConfig.emailfilter;
		if (filtered && (emf == null || emf.length == 0)) filtered = false;
		String[] mails = lst.split("\\s|,|;");
		for(String s : mails){
			String s1 = s.trim();
			if (s1 != null && s1.length() != 0 && s1.indexOf('@') != -1) {
				if (filtered) {
					boolean discard = true;
					for(String f : emf) if (s1.endsWith(f)) discard = false;
					if (discard) continue;
				}
				l.add(s1);
			}
		}
		return l.toArray(new String[l.size()]);
	}

	public static String pingMail() throws AppException {
		return sendMail(null, null, null, null);
	}

	public static String sendMail(String mailer, String to, String subject, String text) 
			throws AppException {
		boolean ping = mailer == null;
		String[] toc = normEMails(to, false);
		String mx = "Send mail : [" + (mailer != null && mailer.length() != 0 ? mailer : "?") 
				+ "] to:[" + (to != null && to.length() != 0 ? to : "?")
				+ "] subject:[" + (subject != null && subject.length() != 0 ? subject : "?") 
				+ "] text:" + (text != null ? text.length() : "0") + "c"
				+ "\n";
		if (!ping && (mailer == null || mailer.length() == 0 || toc.length == 0 || subject == null ))
			throw new AppException("BMAILPARAM", mx);
		MailInfo mi = mailInfo(mailer);
		if (mi == null)
			throw new AppException("BMAILPARAM", mx);

		String base = mailConfig.mailserver;
		
		if (base.startsWith("simu")) {
			try { Thread.sleep(500);} catch (InterruptedException e) {}
			if (ping)
				Util.log.info("Send mail : [PING]");
			else {
				String txt = text != null ? (text.length() > 1500 ? text.substring(0,1500) : text) : "";
				Util.log.warning(mx + txt);
			}
			return "OK1: envoi simulé";
		}
		
		String[] tox = normEMails(to, true);
		if (tox.length == 0) return "OK2: destinataires tous filtrés";
		if (base.startsWith("http")) {
			try {		
				StringBuffer stox = new StringBuffer();
				for(int i = 0; i < tox.length; i++)
					stox.append(i == 0 ? "": ",").append(tox[i]);
				byte[] body = null;
				if (!ping) {
					StringBuffer sb = new StringBuffer();
					sb.append("mailer=");
					sb.append(URLEncoder.encode(mailer, "UTF-8"));
					sb.append("&to=");
					sb.append(URLEncoder.encode(stox.toString(), "UTF-8"));
					sb.append("&subject=");
					sb.append(URLEncoder.encode(subjectPrefix + subject, "UTF-8"));
					sb.append("&text=");
					sb.append(URLEncoder.encode(text, "UTF-8"));
					sb.append("&cle=");
					sb.append(URLEncoder.encode(httpServerPWD, "UTF-8"));
					body = sb.toString().getBytes("UTF-8");
				}
				int retry = 0;
				while (retry < 3) {
					HttpURLConnection connection = (HttpURLConnection) new URL(base).openConnection();
					connection.setReadTimeout(120*1000);
					if (ping)
						connection.setRequestMethod("GET");
					else {
						connection.setDoOutput(true);
						connection.setRequestMethod("POST");
						connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
						connection.setRequestProperty("charset", "utf-8");
						connection.setRequestProperty("Content-Length", "" + Integer.toString(body.length));
						connection.setUseCaches(false);
						OutputStream os = connection.getOutputStream();
						os.write(body);
						os.close();
					}
					InputStream is = connection.getInputStream();
					byte[] buf = new byte[4096];
					ByteArrayOutputStream os2 = new ByteArrayOutputStream(16192);
					int l = 0;
					while ((l = is.read(buf)) > 0)
						os2.write(buf, 0, l);
					is.close();
					byte[] res = os2.toByteArray();
					os2.close();
					String ret = new String(res, "UTF-8");
					if (ret.startsWith("KO : Could not instantiate mail function")){
						retry++;
						try { Thread.sleep(5000);} catch (InterruptedException e) {}
					} else {
						return ret;
					}
				}
				throw new AppException("XMAILHTTP", mx);
			} catch (Exception e){
				throw new AppException(e, "XMAILHTTP", mx);
			}
		}
		if (ping) return "OK : mais mailer ignore les pings";
		if (base.startsWith("java")){
			return mailTo(mi, tox, subject, text, mx);
		} 
		throw new AppException("BMAILTYPE", base);
	}	

	private static String mailTo(MailInfo mi, String[] tox, String subject, String text, String mx) 
			throws AppException {
		try {
		    Message msg = new MimeMessage(mi.session());
		    msg.setFrom(new InternetAddress(mi.from, mi.name));
		    for (String s : tox)
		    	msg.addRecipient(Message.RecipientType.TO, new InternetAddress(s));
		    msg.setSubject(subjectPrefix + subject);
		    if (text == null) text = "";
		    if (text.startsWith("<!DOCTYPE html>"))
		    	msg.setContent(text, "text/html; charset=utf-8");
		    else
		    	msg.setText(text);
		    Transport.send(msg);
		    return "OK";
		} catch (Exception e) {
			throw new AppException(e, "XMAILJAVAMAIL", mx);
		} 
	}

	public static class SendMail extends Operation {
		public static class Param {
			String to;
			String subject;
			String mailer;
			String text;
			boolean isPing = false;
		}
		Param param;
		@Override public void work() throws AppException {
			if (param.isPing)
				result().text = pingMail();
			else
				result().text = sendMail(param.mailer, param.to, param.subject, param.text);
		}
	}
}
