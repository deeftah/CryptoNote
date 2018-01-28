package fr.cryptonote.provider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InitialContext;

import org.postgresql.ds.common.BaseDataSource;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.Cond;
import fr.cryptonote.base.Document.Id;
import fr.cryptonote.base.Document.ItemId;
import fr.cryptonote.base.Document.XItem;
import fr.cryptonote.base.Document.XItemFilter;
import fr.cryptonote.base.DocumentDescr;
import fr.cryptonote.base.QueueManager;
import fr.cryptonote.base.DocumentDescr.ItemDescr;
import fr.cryptonote.base.ExecContext.ExecCollect;
import fr.cryptonote.base.ExecContext.ItemIUD;
import fr.cryptonote.base.ExecContext.IuCDoc;
import fr.cryptonote.base.S2Cleanup;
import fr.cryptonote.base.Stamp;
import fr.cryptonote.base.TaskInfo;
import fr.cryptonote.base.TaskInfo.TaskMin;
import fr.cryptonote.base.UpdDiff.ByTargetClkey;
import fr.cryptonote.base.Util;

public class ProviderPG implements DBProvider {
	private static final int GZIPSIZE = 2000;
			
	private static HashMap<String,BaseDataSource> dataSources = new HashMap<String,BaseDataSource>();
			
	BaseDataSource dataSource;
	private String operationName = "?";
	private Connection conn;
	protected String ns;
	protected String dbname;
	private boolean inTransaction = false;
	private String sql;
	private BlobProvider blobProvider;
	
	@Override public void shutdwon() { }
	
	@Override public BlobProvider blobProvider() throws AppException { 
		if (blobProvider == null)
			blobProvider = new BlobProviderPG(ns);
		return blobProvider;
	}

	public static DBProvider getProvider(String dbname, String pwd) throws AppException {
		BaseDataSource ds = dataSources.get(dbname);
		if (ds == null) {
			try {
				ds = (BaseDataSource)new InitialContext().lookup("java:comp/env/jdbc/" + dbname);
				ds.setPassword(pwd);
				dataSources.put(dbname, ds);
			} catch (Exception e){
				throw new AppException("XSQLDS", dbname, e.getMessage());
			}
		}
		ProviderPG p = new ProviderPG();
		p.dataSource = ds;
		p.dbname = dbname;
		p.ns = dbname;
		return p;
	}
			
	@Override public String ns(){ return this.ns; }

	@Override public DBProvider ns(String ns){ this.ns = ns; return this; }

	@Override public String operationName(){ return this.operationName; }

	@Override public DBProvider operationName(String operationName){ this.operationName = operationName; return this; }

	/*************************************************************************************/
	@Override public Object connection() throws AppException { return conn(); }
	
	// "XSQL0":"Echec SQL - opération:[{0}] méthode:[{1}] namespace:[{2}]\nsql:[{3}]\nmessage:[{4}]",

	protected AppException err(PreparedStatement preparedStatement, ResultSet rs, Exception e, String meth) {
		String sqlx = sql;
		if (preparedStatement != null)
			try { preparedStatement.close(); } catch (SQLException e1) {}
		if (rs != null)
			try { rs.close(); } catch (SQLException e1) {}
		closeConnection();
		return (e instanceof AppException) ? (AppException)e : new AppException("XSQL0", operationName, meth, ns(), sqlx, e.getMessage());
	}

	protected Connection conn() throws AppException{ 
		if (conn == null)
			try {
				conn = (Connection)dataSource.getConnection();
			} catch (SQLException e){
				sql = "";
				throw err(null, null, e, "conn");
			}
		return conn; 
	}

	private void beginTransaction() throws AppException {
		if (inTransaction)
			return;
		try {
			conn().setAutoCommit(false);
			inTransaction = true;
		} catch (SQLException e) {
			sql = "";
			throw err(null, null, e, "beginTransaction");
		}
	}

	private void commitTransaction() throws AppException {
		if (inTransaction && conn != null) {
			try {
				conn.commit();
			} catch (SQLException e) {
				sql = "";
				throw err(null, null, e, "commitTransaction");
			}
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) { }
			inTransaction = false;
		}
	}

	@Override public void rollbackTransaction() {
		if (inTransaction && conn != null) {
			try {
				conn.rollback();
			} catch (SQLException e) { }
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) { }
		}
	}
	
	@Override public void closeConnection() {
		if (conn != null)
			try {
				rollbackTransaction();
				conn.close();
				conn = null;
			} catch (SQLException e) {
				conn = null;
			}		
	}

	/*************************************************************************************/

	private static int setContent(PreparedStatement preparedStatement, String text, int j) throws SQLException{
		byte[] bytes = (text != null && text.length() > GZIPSIZE) ? Util.gzip(text) : null;
		if (bytes != null && bytes.length > text.length() * 0.6)
			bytes = null;
		preparedStatement.setString(j++, bytes == null ? text : null);
		preparedStatement.setBytes(j++, bytes);
		return j;
	}

	private String getContent(ResultSet rs)  throws SQLException{
		String content = rs.getString("contentt");
		if (content != null) return content;
		byte[] bytes = rs.getBytes("contentb");
		return bytes != null ? Util.ungzipText(bytes) : null;
	}

	private static int setPS(Cond<?> c, PreparedStatement preparedStatement, int j) throws SQLException{
		if (c == null) return j;
		// String.class, Integer.class, Long.class, Double.class
		switch (c.classIndex()){
		case 0 : { preparedStatement.setString(j++, (String)c.v1()); break; }
		case 1 : { preparedStatement.setInt(j++, (Integer)c.v1()); break; }
		case 2 : { preparedStatement.setLong(j++, (Long)c.v1()); break; }
		case 3 : { preparedStatement.setDouble(j++, (Double)c.v1()); break; }
		}
		if (c.has2Args())
			switch (c.classIndex()){
			case 0 : { preparedStatement.setString(j++, (String)c.v2()); break; }
			case 1 : { preparedStatement.setInt(j++, (Integer)c.v2()); break; }
			case 2 : { preparedStatement.setLong(j++, (Long)c.v2()); break; }
			case 3 : { preparedStatement.setDouble(j++, (Double)c.v2()); break; }
			}
		return j;
	}

	/*************************************************************************************/
	private static final String SELDBINFO = "select info from dbinfo;" ;
	
	private static final String INSDBINFO = "insert into dbinfo values (?);"; 

	@Override public String dbInfo(String info) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		String dbinfo = null;
		sql = INSDBINFO; 
		try {
			if (info != null) {
				preparedStatement = conn().prepareStatement(sql);
				preparedStatement.setString(1, info);
				preparedStatement.executeUpdate();
				preparedStatement.close();
			}
			sql = SELDBINFO;
			preparedStatement = conn().prepareStatement(sql);
			rs = preparedStatement.executeQuery();
			if (rs.next())
				dbinfo = rs.getString(1);
			rs.close();
			preparedStatement.close();
			return dbinfo;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "dbInfo");
		}
	}

	/***********************************************************************************************/
	private static final String SELONOFF = "select ns, onoff from onoff;";
	
	private static final String UPSERTONOFF = "insert into onoff (ns, onoff) values (?,?) on conflict (onoff_pk) do update set onoff = ?;";
	
	public void setOnOff(String ns, int onoff) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPSERTONOFF; 
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, ns);
			preparedStatement.setInt(2, onoff);
			preparedStatement.setInt(3, onoff);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "setOnOff");
		}

	};

	public HashMap<String,Integer> getOnOff() throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		HashMap<String,Integer> res = new HashMap<String,Integer>();
		sql = SELONOFF; 
		try {
			preparedStatement = conn().prepareStatement(sql);
			rs = preparedStatement.executeQuery();
			while (rs.next())
				res.put(rs.getString("ns"), rs.getInt("onoff"));
			rs.close();
			preparedStatement.close();
			return res;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "getOnOff");
		}
		
	}

	/***********************************************************************************************/
	/*
	DROP TABLE IF EXISTS stats;
	CREATE TABLE stats (
		hour int NOT NULL,
	  	ns varchar(16) NOT NULL,
	  	stat text,
		CONSTRAINT stats_pk PRIMARY KEY (hour, ns)
	);
	 */
	
	private static final String INSSTATS = "insert into stats (hour, ns, stat) values (?,?,?);";
	
	public void recordHourStats(int hour, String ns, String json) throws AppException {
		PreparedStatement preparedStatement = null;
		String sql = INSSTATS;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setInt(j++, hour);
			preparedStatement.setString(j++, ns);
			preparedStatement.setString(j++, json);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "insertDoc");
		}		
		
	}
	
	/***********************************************************************************************/
	private static final String SELDOCS = "select clid, version, ctime, dtime from doc_";

	@Override public Collection<DeltaDocument> listDoc(String BEGINclid, long AFTERversion) throws AppException {
		ArrayList<DeltaDocument> lst = new ArrayList<DeltaDocument>();
		StringBuffer sb = new StringBuffer();
		sb.append(SELDOCS).append(ns).append(" ");
		boolean p = true;
		if (BEGINclid != null) { p = false; sb.append("where clid >= ? and clid < ?"); }
		if (AFTERversion != 0) {sb.append(p ? "where " : " and ").append("version > ?"); p = false;}
		sql = sb.append(";").toString();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			if (BEGINclid != null) {
				preparedStatement.setString(j++, BEGINclid);
				preparedStatement.setString(j++, BEGINclid + '\u1FFF');
			}
			if (AFTERversion != 0) preparedStatement.setLong(j++, AFTERversion);
			rs = preparedStatement.executeQuery();
			while (rs.next()){
				DeltaDocument dd = new DeltaDocument();
				dd.id = new Id(rs.getString("clid"));
				dd.version = rs.getLong("version");
				dd.ctime = rs.getLong("ctime");
				dd.dtime = rs.getLong("dtime");
				lst.add(dd);
			}
			rs.close();
			preparedStatement.close();
			return lst;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "listDoc");
		}
	}
	
	/***********************************************************************************************/
	private static final String SELDOC1 = "select version, ctime, dtime from doc_";
	private static final String SELDOC2 = " where clid = ?;";
	
	private DeltaDocument getDoc(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELDOC1 + ns + SELDOC2; 
		DeltaDocument dd = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, id.toString());
			rs = preparedStatement.executeQuery();
			if (rs.next()) {
				dd = new DeltaDocument();
				dd.id = id;
				dd.version = rs.getLong("version");
				dd.ctime = rs.getLong("ctime");
				dd.dtime = rs.getLong("dtime");
			}
			rs.close();
			preparedStatement.close();
			return dd;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "getDoc");
		}
	}
	
	private static final String SELITEMS1 = "select clkey, version, vop, sha, contentt, contentb from item_";
	private static final String SELITEMS2 = "where docid = ? ";
	
	private void addCItem(ResultSet rs, DeltaDocument dd) throws SQLException {
		ItemId i = new ItemId(dd.id.descr(), rs.getString("clkey"));
		if (i.descr() == null) return;
		long _version = rs.getLong("version");
		long _vop = rs.getLong("vop");
		String _sha = rs.getString("sha");
		String _t = getContent(rs);
		dd.items.put(i.toString(), new CItem(i, _version, _vop, _sha, _t));		
	}
	
	// copie complète simple - items : tous les items existants  +  tous les items détruits
	private DeltaDocument cas12(DeltaDocument dd) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELITEMS1 + ns + "_" + dd.id.docclass() + SELITEMS2 + ";"; 
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, dd.id.docid());
			rs = preparedStatement.executeQuery();
			while (rs.next()) addCItem(rs, dd);
			rs.close();
			preparedStatement.close();
			commitTransaction();
			return dd;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "cas12");
		}
	}

	// delta - items : tous les items existants modifiés après v + tous les items detruits après v
	private DeltaDocument cas3(DeltaDocument dd, long version, boolean commit) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELITEMS1 + ns + "_" + dd.id.docclass() + SELITEMS2 + " and version > ?;"; 
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, dd.id.docid());
			preparedStatement.setLong(2, version);
			rs = preparedStatement.executeQuery();
			while (rs.next()) addCItem(rs, dd);
			rs.close();
			preparedStatement.close();
			if (!commit) commitTransaction();
			return dd;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "cas12");
		}
	}

	private static final String SELITEMS3 = "select clkey from item_";
	private static final String SELITEMS4 = "where docid = ?;";

	
	/* delta + keys existantes
	 * items : tous les items existants modifiés après v + tous les items detruits après dtb (on en a pas avant, donc après v c'est pareil)
	 * clkeys : clés des items existants qui ne figurent pas dans items.
	 */
	private DeltaDocument cas4(DeltaDocument dd, long version) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELITEMS3 + ns + "_" + dd.id.docclass() + SELITEMS4; 
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, dd.id.docid());
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				String clkey = rs.getString(1);
				// si l'item figure dans items, c'est soit qu'il est existant, soit qu'il est détruit.
				// dans les deux cas il n'est pas à lister
				if (dd.items.get(clkey) == null)
					dd.clkeys.add(clkey);
			}
			rs.close();
			preparedStatement.close();
			commitTransaction();
			return dd;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "cas12");
		}
	}

	/***********************************************************************************************/
	@Override
	public DeltaDocument getDocument(Id id, long ctime, long version, long dtime) throws AppException {
		beginTransaction();
		DeltaDocument dd = getDoc(id);
		if (dd == null) return null;
		
		if (version == dd.version) { // cas = 0 : cache à niveau (v == vdb) cas = 0
			dd.cas = 0;
			return dd; // rien à remplir
		}
		
		// Cache ayant une version retardée (v < vdb)
		if (version == 0) { // cas = 1 : cache vide
			dd.cas = 1;
			return cas12(dd); // copie complète simple - items : tous les items existants  +  tous les items détruits
		}
		
		if (ctime < dd.ctime) { // cas = 2 : cache ayant une version d'une vie antérieure (c < cdb)
			dd.cas = 2;
			return cas12(dd); // copie complète simple - items : tous les items existants  +  tous les items détruits
		} 
		
		// Cache ayant une version retardée de la même vie
		if (version >= dd.dtime) { // Le cache ne contient pas d'items détruits dont la suppression serait inconnue de la base
			dd.cas = 3;
			return cas3(dd, version, true); // delta - items : tous les items existants modifiés après v + tous les items detruits après v
		} 
		
		// Le cache peut contenir des items détruits dont la destruction est inconnue de la base
		dd.cas = 4;
		/* delta + keys existantes
		 * items : tous les items existants modifiés après v + tous les items detruits après ddb (on en a pas avant, donc après v c'est pareil)
		 * clkeys : clés des items existants qui ne figurent pas dans items.
		 */
		cas3(dd, version, false);
		return cas4(dd, version);
	}
	
	/***********************************************************************************************/
//	CREATE TABLE doc (
//			clid varchar(255) NOT NULL,
//			version bigint NOT NULL,
//			ctime bigint NOT NULL,
//			dtime bigint NOT NULL,
//			CONSTRAINT doc_pk PRIMARY KEY (clid)
//		);

	private Stamp checkVersion(String info, String clid, long vx) throws AppException{
		Stamp st = Stamp.fromStamp(vx);
		if (st == null)	throw new AppException("XSQL0", operationName, ns, info + "corrompue [" + clid != null ? clid : "" + "] - [" + vx + "]");
		return st;
	}

	private static final String LOCKDOC1 = "select clid, version from doc_";
	private static final String LOCKDOC2 = " where clid in (";
	private static final String LOCKDOC3 = ") for update nowait;";
		
	/*
	 * Retourne null si tout est OK.
	 * Le retour peut être vide et non null si il y aune exception d'accès à la base
	 */
	private HashMap<String,Long> checkAndLock(HashMap<String,Long> docs) throws AppException {
		StringBuffer sb = new StringBuffer();
		HashMap<String,Long> badDocs = new HashMap<String,Long>();
		sb.append(LOCKDOC1).append(ns).append(LOCKDOC2);
		for(int i = 0; i < docs.size(); i++) sb.append(i == 0 ? "?" : ",?");
		String sql = sb.append(LOCKDOC3).toString();
		
		// tri des groupes pour limiter les apparitions de deadlocks
		String[] clids = docs.keySet().toArray(new String[docs.size()]);
		Arrays.sort(clids);
		
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		long _version;
		String _clid;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			for(String clid : clids)
				preparedStatement.setString(j++, clid);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				_clid = rs.getString("clid");
				_version = rs.getLong("version");
				checkVersion("table doc " + _clid, null, _version);
				Long v = docs.get(_clid);
				if (v != _version) badDocs.put(_clid, _version);
			}
			rs.close();
			preparedStatement.close();
			return badDocs.size() != 0 ? badDocs : null;
		} catch(Exception e){
			if (preparedStatement != null) try { preparedStatement.close(); } catch (SQLException e1) {}
			if (rs != null)	try { rs.close(); } catch (SQLException e1) {}
			return badDocs;
		}
	}

	/***********************************************************************************************/
	private static final String DELITEMS = " where docid = ?;";
	private static final String DELDOC = " where clid = ?;";

	private void purge(String clid) throws AppException {
		Id id = new Id(clid);
		PreparedStatement preparedStatement = null;
		String sql = "delete from item_" + ns + "_" + id.docclass() + DELITEMS;
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(2, id.docid());
			preparedStatement.executeUpdate();
			preparedStatement.close();
			sql = "delete from doc_" + ns + DELDOC;
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "purge");
		}
	
	}

	/***********************************************************************************************/
	private static final String INSDOC = " (clid, version, ctime, dtime) values (?,?,?,?);";

	private void insertDoc(IuCDoc x, long version) throws AppException {
		PreparedStatement preparedStatement = null;
		String sql = "insert into doc_" + ns + INSDOC;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, x.clid);
			preparedStatement.setLong(j++, version);
			preparedStatement.setLong(j++, version);
			preparedStatement.setLong(j++, version);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "insertDoc");
		}		

	}
	/***********************************************************************************************/
	private static final String UPDDOC = " set version = ?, ctime = ?, dtime = ? where clid = ?;";

	private void updateDoc(String clid, long version, long ctime, long dtime) throws AppException {
		PreparedStatement preparedStatement = null;
		String sql = "update doc_" + ns + UPDDOC;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setLong(j++, version);
			preparedStatement.setLong(j++, ctime);
			preparedStatement.setLong(j++, dtime);
			preparedStatement.setString(j++, clid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "insertDoc");
		}
	}

	/***********************************************************************************************/
//	CREATE TABLE item (
//		  	docid varchar(255) NOT NULL,
//		  	clkey varchar(255) NOT NULL,
//		  	version bigint NOT NULL,
//		  	vop bigint NOT NULL,
//		  	sha varchar(255),
//		  	contentt text,
//			contentb bytea,
//			CONSTRAINT item_pk PRIMARY KEY (docid, clkey)
//		);

	private static final String INSITEM1 = " (docid, clkey, version, vop, sha, contentt, contentb";
	private static final String INSITEM2 = ") values (?,?,?,?,?,?,?";

	private void ins(ItemIUD x, long version) throws AppException {
		String[] fields = null;
		if (x.exportedFields != null) {
			Set<String> ks = x.exportedFields.keySet();
			fields = ks.toArray(new String[ks.size()]);
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("insert into item_" + ns + "_" + x.id.docclass());
		if (fields != null)	sb.append("_" + x.name);
		sb.append(INSITEM1);
		if (fields != null)
			for(String n : fields)
				sb.append(", ").append(n);
		sb.append(INSITEM2);
		if (fields != null)
			for(int i = 0; i < fields.length; i++) sb.append(",?");
		String sql = sb.append(");").toString();

		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, x.id.docid());
			preparedStatement.setString(j++, x.clkey);
			preparedStatement.setLong(j++, version);
			preparedStatement.setLong(j++, version);
			preparedStatement.setString(j++, x.sha);
			j = setContent(preparedStatement, x.cvalue, j);
			if (fields != null) for(String n : fields) 
				preparedStatement.setObject(j++, x.exportedFields.get(n));
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "ins");
		}
	}

	/***********************************************************************************************/
	private static final String UPDITEM1 = " set version = ?, vop = ?, sha = ?, contentt = ?, contentb = ? ";
	private static final String UPDITEM2 = " where docid = ? and clkey = ?";
	private static final String UPDITEM3 = " and vop < ?;";

	private void upd(ItemIUD x, long version, boolean hasContent, long vopFromRawDup) throws AppException {
		String[] fields = null;
		if (x.exportedFields != null) {
			Set<String> ks = x.exportedFields.keySet();
			fields = ks.toArray(new String[ks.size()]);
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("update item_ " + ns + "_" + x.id.docclass());
		if (fields != null)	sb.append("_" + x.name);
		sb.append(UPDITEM1);
		if (fields != null)
			for(String n : fields)
				sb.append(", ").append(n).append(" = ?");
		sb.append(UPDITEM2);
		String sql = sb.append(vopFromRawDup == 0 ? ";" : UPDITEM3).toString();

		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setLong(j++, version);
			preparedStatement.setLong(j++, hasContent ? version : 0);
			if (hasContent) {
				preparedStatement.setString(j++, x.sha);
				j = setContent(preparedStatement, x.cvalue, j);
			} else {
				preparedStatement.setString(j++, null);
				preparedStatement.setString(j++, null);
				preparedStatement.setString(j++, null);				
			}
			if (fields != null) for(String n : fields) 
				preparedStatement.setObject(j++, x.exportedFields.get(n));
			preparedStatement.setString(j++, x.id.docid());
			preparedStatement.setString(j++, x.clkey);
			if (vopFromRawDup != 0)
				preparedStatement.setLong(j++, vopFromRawDup);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "insertDoc");
		}
	}

	/***********************************************************************************************/
	private static final String DELITEM = " where docid = ? and clkey = ?;";

	private void del(ItemIUD x) throws AppException {
		PreparedStatement preparedStatement = null;
		String sql = "delete from item_" + ns + "_" + x.id.docclass() + DELITEM;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, x.id.docid());
			preparedStatement.setString(j++, x.clkey);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "del");
		}
	}

	/***********************************************************************************************/
	@Override
	public HashMap<String,Long> validateDocument(ExecCollect collect) throws AppException {
//		// documents à supprimer
//		public HashSet<String> docsToDelForced;
//
//		// documents à sauver
//		public ArrayList<IuCDoc> docsToSave = new ArrayList<IuCDoc>();
//		
//		// documents à verrouiller
//		public HashMap<String,Long> versionsToCheckAndLock = new HashMap<String,Long>();
//
//		// documents avec S2 à nettoyer
//		public HashSet<String> s2Cleanup = new HashSet<String>();
//
//		// documents avec S2 à purger
//		public HashSet<String> s2Purge = new HashSet<String>();
//
//		// items à insérer / mettre à jour / supprimer
//		public ArrayList<ItemIUD> itemsIUD = new ArrayList<ItemIUD>();
//
//		// Tasks
//		public HashMap<String,TaskInfo> tq;
//
//		public TaskInfo taskInfo; // tâche actuelle
		
		try {			
			beginTransaction();
			HashMap<String,Long> badDocs = checkAndLock(collect.versionsToCheckAndLock);
			if (badDocs != null) { rollbackTransaction(); return badDocs; }
			
			if (collect.taskInfo != null){
				TaskInfo ti = collect.taskInfo;
				switch (ti.taskType){
				case 4 : { // fin tâche sans sauvegarde de param
					removeTask(ti.ns, ti.taskid);
					break;
				}
				case 5 : { // fin tâche avec sauvegarde de param
					finalTask(ti, ti.taskT, ti.taskJsonParam);
					break;
				}
				case 6 : { // nextStep interne
					stepTaskSR(ti, ti.taskJsonParam, Stamp.fromNow(0).stamp());
					break;
				}
				case 7 : { // nextStep nouvelle requête
					stepTaskNR(ti, ti.taskJsonParam, ti.taskT);
					break;
				}
				}
			}
			
			if (collect.docsToDelForced != null) for(String clid : collect.docsToDelForced) purge(clid);
			
			for (IuCDoc x : collect.docsToSave) {
				switch (x.iu) {
				case 1 : {insertDoc(x, collect.version); continue; } // insert
				case 2 : {updateDoc(x.clid, collect.version, x.oldctime, collect.dtime); continue; } // update 
				case 3 : {purge(x.clid); insertDoc(x, collect.version); continue; } // clear et update
				}
			}

			for(ItemIUD x : collect.itemsIUD){
				switch (x.iud) {
				case 1 : {ins(x, collect.version); continue; } // insertion
				case 2 : {upd(x, collect.version, true, 0); continue; } // update contenu
				case 3 : {upd(x, collect.version, false, 0); continue; } // suppression logique (contenu)
				case 4 : { del(x); continue; } // purge/delete (trop ancien)
				}
			}

			if (collect.s2Purge != null) for(String clid : collect.s2Purge) blobProvider().blobDeleteAll(clid);
			if (collect.tq != null) for(TaskInfo ti : collect.tq) newTask(ti);
			if (collect.s2Cleanup != null) for(String clid : collect.s2Cleanup) S2Cleanup.startCleanup(clid);
			
			commitTransaction();
			
			collect.afterCommit();
			
			if (collect.tq != null && collect.tq.size() != 0)
				for(TaskInfo ti : collect.tq) QueueManager.enqueue(new TaskMin(ti));
			if (collect.taskInfo != null && collect.taskInfo.taskType == 7)
				QueueManager.enqueue(new TaskMin(collect.taskInfo));

			return null;
		} catch (Throwable t){
			rollbackTransaction();
			if (t instanceof AppException) throw (AppException)t; else throw new AppException(t, "X0");
		}

	}

	/***********************************************************************************************/
	
	@Override
	public void rawDuplicate(long vop, HashMap<String,ArrayList<ByTargetClkey>> byDoc)  throws AppException {
		try {
			for(String clid : byDoc.keySet()){
				Id id = new Id(clid);
				ArrayList<ByTargetClkey> lst = byDoc.get(clid);
				
				beginTransaction();
				DeltaDocument doc = getDoc(id);
				if (doc == null) {
					commitTransaction();
					continue;
				}
				long version = System.currentTimeMillis();
				long v = doc.version;
				if (v <= version) version = Stamp.fromStamp(v, 1).stamp();
				updateDoc(clid, version, doc.ctime, doc.dtime);
				for(ByTargetClkey t : lst) {
					ItemIUD iud = new ItemIUD();
					iud.id = id;
					iud.cvalue = t.content;
					iud.exportedFields = t.exportedFields;
					iud.name = t.itd.name();
					iud.clkey = t.clkey;
					upd(iud, version, true, vop);
				}
				commitTransaction();
				
			}
		} catch(Exception e){
			throw err(null, null, e, "rawDuplicate");
		}
	}
	
	/***********************************************************************************************/
	private static final String SQLSEARCH1 = "select distinct docid from item_";
	private static final String SQLSEARCH2 = "select docid, version, clkey, contentt, contentb from item_";

	@Override
	public Collection<Id> searchDocIdsByIndexes(Class<?> docClass, Class<?> itemClass, Cond<?>... ffield) throws AppException {
		ArrayList<Id> res = new ArrayList<Id>();
		DocumentDescr dd = DocumentDescr.get(docClass);
		if (dd == null) return res;
		ItemDescr itd = dd.itemDescr(itemClass);
		if (itd == null) return res;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		
		StringBuffer sb = new StringBuffer();
		sb.append(SQLSEARCH1).append(ns).append("_").append(docClass.getSimpleName()).append("_").append(itemClass.getSimpleName()).append(" where ");
		boolean pf = true;
		for(Cond<?> c : ffield) {
			if (pf) pf = false; else sb.append(" and ");
			sb.append(c.toSql());
		}
		String sql = sb.toString();
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			for(Cond<?> c : ffield)
				j = setPS(c, preparedStatement, j);
			rs = preparedStatement.executeQuery();
			while (rs.next())
				res.add(new Id(docClass, rs.getString(1)));
			rs.close();
			preparedStatement.close();
			return res;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "searchDocIdsByIndexes");
		}
	}

	@Override
	public Collection<XItem> searchItemsByIndexes(Class<?> docClass, Class<?> itemClass, XItemFilter filter, Cond<?>... ffield) throws AppException {
		ArrayList<XItem> res = new ArrayList<XItem>();
		DocumentDescr dd = DocumentDescr.get(docClass);
		if (dd == null) return res;
		ItemDescr itd = dd.itemDescr(itemClass);
		if (itd == null) return res;
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		
		StringBuffer sb = new StringBuffer();
		sb.append(SQLSEARCH2).append(ns).append("_").append(docClass.getSimpleName()).append("_").append(itemClass.getSimpleName()).append(" where ");
		boolean pf = true;
		for(Cond<?> c : ffield) {
			if (pf) pf = false; else sb.append(" and ");
			sb.append(c.toSql());
		}
		String sql = sb.toString();
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			for(Cond<?> c : ffield)
				j = setPS(c, preparedStatement, j);
			rs = preparedStatement.executeQuery();
			while (rs.next()) {
				// public XItem(ItemDescr descr, String docid, String clkey, long version, String content) throws AppException{ 
				XItem xi = new XItem(itd, rs.getString("docid"), rs.getString("clkey"), rs.getLong("version"), getContent(rs));
				if (filter == null || filter.filter(xi))
					res.add(xi);
			}
			rs.close();
			preparedStatement.close();
			return res;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "searchDocIdsByIndexes");
		}
	}

	/***********************************************************************************************/
	/*
	DROP TABLE IF EXISTS taskqueue;
	CREATE TABLE taskqueue (
	  	ns varchar(16) NOT NULL,
	  	taskid varchar(255) NOT NULL,
		step int NOT NULL,
		tostartat bigint,
		topurgeat bigint, 
	  	opname varchar(16) NOT NULL,
	  	param text,
	  	cron varchar(16),
	  	info varchar(255),
		qn int NOT NULL,
		retry int NOT NULL,
	  	exc text,
		report text,
		starttime bigint, 
		CONSTRAINT taskqueue_pk PRIMARY KEY (ns, taskid)
	);
	ALTER TABLE taskqueue OWNER TO "docdb";
	DROP INDEX IF EXISTS taskqueue_startat;
	CREATE INDEX taskqueue_startat on taskqueue (startat, ns, taskid) where startat is not null;
	DROP INDEX IF EXISTS taskqueue_starttime;
	CREATE INDEX taskqueue_starttime on taskqueue (starttime, ns, taskid) where starttime is not null;
	*/
	
	private static final String INSERTTASK = 
		"insert into taskqueue (ns, taskid, step, tostartat, opname, cron, info, qn, retry, param) values (?,?,?,1,?,?,?,?,0,?);";
		
	@Override 
	public void newTask(TaskInfo ti) throws AppException{
		PreparedStatement preparedStatement = null;
		sql = INSERTTASK;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, ti.ns);
			preparedStatement.setString(j++, ti.taskid);
			preparedStatement.setLong(j++, ti.toStartAt);
			preparedStatement.setString(j++, ti.opName);
			preparedStatement.setString(j++, ti.cron);
			preparedStatement.setString(j++, ti.info == null ? "" : ti.info);
			preparedStatement.setInt(j++, ti.qn);
			preparedStatement.setInt(j++, 0);
			preparedStatement.setString(j++, ti.param);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "insertTask");
		}
	}

	private static final String UPDTASK1 = "update taskqueue set exc = null, detail = null, tostartat = null, starttime = ?, retry = ? where ns = ? and taskid = ?;";

	@Override public TaskInfo startTask(String ns, String taskid, int step, long startTime) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPDTASK1;
		try {
			beginTransaction();
			TaskInfo ti = taskInfo(ns, taskid);
			if (ti == null || ti.step == 0 || step != ti.step) {
				commitTransaction();
				return null;
			}
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setLong(j++, startTime);
			if (ti.startTime == 0) 	ti.retry++;
			preparedStatement.setInt(j++, ti.retry);
			preparedStatement.setString(j++, ns);
			preparedStatement.setString(j++, taskid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			commitTransaction();
			ti.exc = null;
			ti.detail = null;
			ti.toStartAt = 0;
			ti.startTime = startTime;
			return ti;
		} catch(Exception e){
			throw err(preparedStatement, null, e, "startTask");
		}
	}

	private static final String UPDTASK2b = "update taskqueue set exc = 'LOST', detail = null, tostartat = ?, retry = retry + 1, starttime = null "
			+ "where starttime IS NOT NULL and starttime  <= ?;";

	public void setLostTask(long minStartTime, long toStartAt) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPDTASK2b;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setLong(j++, toStartAt);
			preparedStatement.setLong(j++, minStartTime);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			commitTransaction();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "setLostTask");
		}
		
	}

	private static final String UPDTASK2 = "update taskqueue set exc = ?, detail = ?, tostartat = ?, retry = ?, starttime = null where ns = ? and taskid = ?;";

	@Override
	public boolean excTask(TaskInfo ti, AppException exc) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPDTASK2;
		try {
			beginTransaction();
			TaskInfo tidb = taskInfo(ti.ns, ti.taskid);
			if (tidb == null || tidb.startTime != ti.startTime) {
				// ignore cette fin de tak, c'était une exécution parasite (ou une trace de task).
				commitTransaction();
				return false;
			}
			ti.retry++;
			ti.toStartAt = ti.retryAt();
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, exc.code());
			preparedStatement.setString(j++, exc.toJson());
			preparedStatement.setLong(j++, ti.toStartAt);
			preparedStatement.setInt(j++, ti.retry);
			preparedStatement.setString(j++, ti.ns);
			preparedStatement.setString(j++, ti.taskid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			commitTransaction();
			return true;
		} catch(Exception e){
			throw err(preparedStatement, null, e, "excTask");
		}
	}

	private static final String UPDTASK3 = "update taskqueue set tostartat = ?, param = ?, exc = null, detail = null, starttime = null, retry = 0, step = step + 1 where ns = ? and taskid = ?;";

	// Fin d'une étape d'une tâche avec lancement d'une nouvelle requête pour continuation à l'étape suivante.
	@Override
	public boolean stepTaskNR(TaskInfo ti, String param, long toStartAt) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPDTASK3;
		try {
			TaskInfo tidb = taskInfo(ti.ns, ti.taskid);
			if (tidb == null || tidb.startTime != ti.startTime) {
				// ignore cette fin de tak, c'était une exécution parasite (ou une trace de task).
				return false;
			}
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setLong(j++, toStartAt);
			preparedStatement.setString(j++, param);
			preparedStatement.setString(j++, ti.ns);
			preparedStatement.setString(j++, ti.taskid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			return true;
		} catch(Exception e){
			throw err(preparedStatement, null, e, "stepTaskNR");
		}
	}

	private static final String UPDTASK4 = "update taskqueue set param = ?, starttime = ?, tostartat = null, exc = null, detail = null, retry = 0, step = step + 1 where ns = ? and taskid = ?;";

	// Fin d'une étape d'une tâche et son étape suivante se poursuit dans la même requête.
	@Override
	public boolean stepTaskSR(TaskInfo ti, String param, long startTime) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPDTASK4;
		try {
			TaskInfo tidb = taskInfo(ti.ns, ti.taskid);
			if (tidb == null || tidb.startTime != ti.startTime) {
				// ignore cette fin de tak, c'était une exécution parasite (ou une trace de task).
				return false;
			}
			preparedStatement = conn().prepareStatement(sql);
			ti.step++;
			ti.retry = 0;
			ti.exc = null;
			ti.toStartAt = 0;
			ti.startTime = startTime;
			int j = 1;
			preparedStatement.setString(j++, param);
			preparedStatement.setLong(j++, ti.startTime);
			preparedStatement.setString(j++, ti.ns);
			preparedStatement.setString(j++, ti.taskid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			return true;
		} catch(Exception e){
			throw err(preparedStatement, null, e, "stepTaskSR");
		}
	}

	private static final String UPDTASK5 = "update taskqueue set topurgeat = ?, param = ?, tostartat = null, exc = null, starttime = null, retry = 0, step = 0 where ns = ? and taskid = ?;";

	// Fin de la dernière étape avec ou sans conservation du résultat pendant un certain temps
	@Override
	public boolean finalTask(TaskInfo ti, long toPurgeAt, String param) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = param == null ? DELTASK : UPDTASK5;
		try {
			TaskInfo tidb = taskInfo(ti.ns, ti.taskid);
			if (tidb == null || tidb.startTime != ti.startTime) {
				// ignore cette fin de tak, c'était une exécution parasite (ou une trace de task).
				return false;
			}
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			if (param != null) {
				preparedStatement.setLong(j++, toPurgeAt);
				preparedStatement.setString(j++, param);
			}
			preparedStatement.setString(j++, ti.ns);
			preparedStatement.setString(j++, ti.taskid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			return true;
		} catch(Exception e){
			throw err(preparedStatement, null, e, "excTask");
		}
	}

	private static final String DELTASK = "delete from taskqueue where ns = ? and taskid = ?;";

	@Override
	public void removeTask(String ns, String taskid) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = DELTASK;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, ns);
			preparedStatement.setString(j++, taskid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "removeTask");
		}
	}

	private static final String SELTI3a = "select step, tostartat, topurgeat, opname, cron, info, qn, retry, exc, startTime";
	private static final String SELTI3b = ", param, detail ";
	private static final String SELTI3c = " from taskqueue where ";
	
	@Override
	public TaskInfo taskInfo(String ns, String taskid) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		TaskInfo ti = null;
		sql = SELTI3a + SELTI3b + SELTI3c + " ns = ? and taskid = ?;";
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, ns);
			preparedStatement.setString(j++, taskid);
			rs = preparedStatement.executeQuery();
			if (rs.next())
				ti = ti(rs, false);
			rs.close();
			preparedStatement.close();
			return ti;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "taskReport");
		}
	}
	
	private TaskInfo ti(ResultSet rs, boolean court) throws SQLException {
		TaskInfo ti = new TaskInfo();
		ti.ns = rs.getString("ns");;
		ti.taskid = rs.getString("taskid");;;
		ti.step = rs.getInt("step");
		Long l = rs.getLong("tostartAt");
		ti.toStartAt = l == null ? 0 : l;
		l = rs.getLong("topurgeat");
		ti.toPurgeAt = l == null ? 0 : l;
		ti.opName = rs.getString("opname");
		ti.cron = rs.getString("cron");
		if (!court) ti.param = rs.getString("param");
		ti.info = rs.getString("info");
		ti.qn = rs.getInt("qn");
		ti.retry = rs.getInt("retry");
		ti.exc = rs.getString("exc");
		if (!court) ti.detail = rs.getString("detail");
		l = rs.getLong("startTime");
		ti.startTime = l == null ? 0 : l;
		return ti;
	}
	
	//	public static class TaskMin implements Comparable<TaskMin> {
	//	public String 	ns;
	//	public String 	taskid;
	//  public int		step;
	//	public long 	startAt;
	//	public int		qn;
	
	private static final String SELTI2 = "select ns, taskid, step, tostartat, qn from taskqueue where tostartat is not null and tostartat <= ?";
	
	@Override
	public Collection<TaskMin> candidateTasks(String ns, long before) throws AppException {
		ArrayList<TaskMin> lst = new ArrayList<TaskMin>();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELTI2 + (ns == null ? ";" : " and ns = ?");
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			if (ns != null) preparedStatement.setString(j++, ns);
			preparedStatement.setLong(j++, before);
			rs = preparedStatement.executeQuery();
			while (rs.next())
				lst.add(new TaskMin(rs.getString("ns"), rs.getString("taskid"), rs.getInt("step"), rs.getLong("startAt"), rs.getInt("qn")));
			rs.close();
			preparedStatement.close();
			return lst;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "taskReport");
		}
	}
	
	@Override
	public Collection<TaskInfo> errTasks(String ns, long toStartAtMin, long toStartAtMax, String exc) throws AppException {
		ArrayList<TaskInfo> tiList = new ArrayList<TaskInfo>();
		StringBuffer sb = new StringBuffer();
		sb.append(SELTI3a).append(SELTI3c).append(" toStartAt NOT NULL");
		if (toStartAtMin != 0) sb.append(" and tostartat >= ?");
		if (toStartAtMax != 0) sb.append(" and tostartat >= ?");
		if (ns != null) sb.append(" and ns = ?");
		if (exc != null) sb.append(" and exc = ?");
		sb.setLength(sb.length() - 1);
		sql = sb.append(";").toString();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			if (toStartAtMin != 0) preparedStatement.setLong(j++, toStartAtMin);
			if (toStartAtMax != 0) preparedStatement.setLong(j++, toStartAtMax);
			if (ns != null) preparedStatement.setString(j++, ns);
			if (exc != null) preparedStatement.setString(j++, exc);
			rs = preparedStatement.executeQuery();
			while (rs.next())
				tiList.add(ti(rs, true));
			rs.close();
			preparedStatement.close();
			return tiList;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "listTask");
		}
	}

	@Override
	public Collection<TaskInfo> traceTasks(String ns, long toPurgeAtMin, long toPurgeAtMax, String opname) throws AppException {
		ArrayList<TaskInfo> tiList = new ArrayList<TaskInfo>();
		StringBuffer sb = new StringBuffer();
		sb.append(SELTI3a).append(SELTI3c).append(" toStartAt IS NULL and startTime is NULL and step = 0 ");
		if (toPurgeAtMin != 0) sb.append(" and topurgeat >= ?");
		if (toPurgeAtMax != 0) sb.append(" and topurgeat >= ?");
		if (ns != null) sb.append(" and ns = ?");
		if (opname != null) sb.append(" and opname = ?");
		sb.setLength(sb.length() - 1);
		sql = sb.append(";").toString();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			if (toPurgeAtMin != 0) preparedStatement.setLong(j++, toPurgeAtMin);
			if (toPurgeAtMax != 0) preparedStatement.setLong(j++, toPurgeAtMax);
			if (ns != null) preparedStatement.setString(j++, ns);
			if (opname != null) preparedStatement.setString(j++, opname);
			rs = preparedStatement.executeQuery();
			while (rs.next())
				tiList.add(ti(rs, true));
			rs.close();
			preparedStatement.close();
			return tiList;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "listTask");
		}
	}

	private static String SELTIPARAM = "select param from task where ns = ? and taskid = ? ;";

	@Override
	public String taskParam(String ns, String taskid) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		String report = "";
		sql = SELTIPARAM;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, ns);
			preparedStatement.setString(j++, taskid);
			rs = preparedStatement.executeQuery();
			if (rs.next())
				report = rs.getNString("param");
			rs.close();
			preparedStatement.close();
			return report;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "taskReport");
		}
	}

	private static String SELTIDETAIL = "select detail from task where ns = ? and taskid = ? ;";

	@Override
	public String taskDetail(String ns, String taskid) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		String report = "";
		sql = SELTIDETAIL;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, ns);
			preparedStatement.setString(j++, taskid);
			rs = preparedStatement.executeQuery();
			if (rs.next())
				report = rs.getNString("detail");
			rs.close();
			preparedStatement.close();
			return report;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "taskReport");
		}
	}

	/****************************************************************************************/
	
	private static final String SELPITEM1 = "select distinct sha from item_";
	private static final String SELPITEM2 = " where sha is not null and docid = ? ;";

	@Override
	public HashSet<String> shas(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		HashSet<String> res = new HashSet<String>();
		sql = SELPITEM1 + ns + "_" + id.docclass() + SELPITEM2;
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, id.docid());
			rs = preparedStatement.executeQuery();
			while (rs.next())
				res.add(rs.getString("sha"));
			rs.close();
			preparedStatement.close();
			return res;
		} catch(Exception e) { 
			throw err(preparedStatement, rs, e, "shas"); 
		}
	}

	/***********************************************************************************************/
	private static final String SELS21 = "select hour from s2cleanup_";
	private static final String SELS22 = " where clid = ?;";

	@Override
	public int lastS2Cleanup(String clid) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		int hour = 0;
		sql = SELS21 + ns + SELS22;
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, clid);
			rs = preparedStatement.executeQuery();
			if (rs.next())
				hour = rs.getInt("hour");
			rs.close();
			preparedStatement.close();
			return hour;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "lastS2Cleanup");
		}
	}

	private static final String UPDS2 = " set hour = ? where clid = ?;";
	
	@Override
	public void setS2Cleanup(TaskInfo ti, String clid) throws AppException {
		PreparedStatement preparedStatement = null;
		int hour = (int)(ti.toStartAt / 10000000L);
		sql = "update s2cleanup_" + ns + UPDS2;
		boolean transaction = inTransaction;
		try {
			if (!transaction) beginTransaction();
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setInt(j++, hour);
			preparedStatement.setString(j++, clid);
			preparedStatement.executeUpdate();
			preparedStatement.close();
			newTask(ti);
			if (!transaction) commitTransaction();
		} catch(Exception e){ 
			throw err(preparedStatement, null, e, "setS2Cleanup");
		}
	}
	
}
