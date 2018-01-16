package fr.cryptonote.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

public final class Result {
	public String out;
	public String syncs;
	public transient String text;
	public transient String encoding;
	public transient byte[] bytes;
	public transient String mime;
	public transient Stamp t;
	public transient int type;
	
	void setSyncs(String syncs) {
		if (type <= 1){
			this.syncs = syncs == null ? "{}" : syncs;
			type = 1;
		}
	}
		
	byte[] bytes() {
		if (type <= 1) {
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			if (out != null) {
				sb.append("\"out\":").append(out);
				if (syncs != null) sb.append(", ");
			}
			if (syncs != null) sb.append("\"syncs\":").append(syncs);
			sb.append("}");
			return Util.toUTF8(sb.toString());
		}
		if (type == 2) return bytes;
		if (type == 3)
			try { return text.getBytes(encoding); } catch (Exception e) { return new byte[0]; }
		return new byte[0]; 
	}
	
	String encoding() {
		return encoding == null ? "UTF-8" : encoding;
	}
	
	String mime() {
		if (type <= 1) return "application/json";
		if (type == 2) return mime == null ? "text/plain" : mime;
		return mime == null ? "application/octet-stream" : mime;
	}
	
	private Result() {}
	
	public boolean isEmpty() {
		return type == 0;
	}
	
	public static Result empty(){
		return new Result();
	}
	
	public static Result json(Object out) {
		Result r = new Result();
		r.out = out == null ? "{}" : JSON.toJson(out);
		r.type = 1;
		return r;
	}

	public static Result binary(byte[] bytes, String mime) {
		Result r = new Result();
		r.type = 2;
		r.bytes = bytes;
		r.mime = mime;
		return r;
	}

	public static Result gzip(String res) {
		Result r = new Result();
		r.type = 2;
		byte[] bytes = null;
		if (res == null || res.length() == 0)
			bytes = new byte[0];
		else
			try { bytes = res.getBytes("UTF-8"); } catch (UnsupportedEncodingException e) {	}
		r.mime = "application/x-gzip";
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			GZIPOutputStream zos = new GZIPOutputStream(bos);
			zos.write(bytes);
			zos.close();
			r.bytes = bos.toByteArray();
		} catch (IOException e) {
			r.bytes = new byte[0];
		}
		return r;
	}

	public static Result text(String text, String encoding, String mime) {
		Result r = new Result();
		r.mime = mime;
		r.text = text;
		r.encoding = encoding == null ? "UTF-8" : encoding;
		if (!"UTF-8".equals(r.encoding))
			try { "a".getBytes(r.encoding);	} catch (Exception x) {	r.encoding = "UTF-8"; }
		r.type = 2;
		return r;
	}

	/*
	 * Aucun report à conserver
	 */
	public static Result taskComplete() {
		Result r = new Result();
		r.type = 3;
		return r;
	}

	/*
	 * Le report est dans param
	 */
	public static Result taskComplete(Stamp toPurgeAt) {
		Result r = new Result();
		r.type = 4;
		r.t = toPurgeAt;
		return r;
	}

	/*
	 * param contient le paramètre de la prochaine étape
	 * Exécution dans la même requête
	 */
	public static Result nextStep() {
		Result r = new Result();
		r.type = 5;
		return r;
	}

	/*
	 * param contient le paramètre de la prochaine étape
	 * Exécution dans une requête ultérieure
	 */
	public static Result nextStep(Stamp toStartAt) {
		Result r = new Result();
		r.type = 6;
		r.t = toStartAt;
		return r;
	}

}
