package fr.cryptonote.provider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.naming.InitialContext;

import org.postgresql.ds.common.BaseDataSource;

import fr.cryptonote.base.AConfig;
import fr.cryptonote.base.AppException;
import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.Cond;
import fr.cryptonote.base.Document.Id;
import fr.cryptonote.base.Document.ItemId;
import fr.cryptonote.base.Document.XItem;
import fr.cryptonote.base.Document.XItemFilter;
import fr.cryptonote.base.ExecContext.ExecCollect;
import fr.cryptonote.base.JSON;
import fr.cryptonote.base.TaskInfo;
import fr.cryptonote.base.TaskUpdDiff.Upd;
import fr.cryptonote.base.Util;

public class ProviderPG implements DBProvider {
	private static final int GZIPSIZE = 2000;
	
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
		
	private static HashMap<String,BaseDataSource> dataSources = new HashMap<String,BaseDataSource>();

	private static class ProviderConfig {
		private String blobsroot;
	}
	private static ProviderConfig providerConfig;
			
	BaseDataSource dataSource;
	private String operationName = "?";
	private Connection conn;
	protected String ns;
	private boolean inTransaction = false;
	private String sql;
	
	private BlobProvider blobProvider;
	
	@Override public void shutdwon() { }
	
	@Override public BlobProvider blobProvider() { return blobProvider; }

	public ProviderPG(String ns) throws AppException {
		if (ns == null || ns.length() == 0) throw new AppException("XSQLDSNS");
		this.ns = ns;
		if (providerConfig == null){
			try {
				providerConfig = JSON.fromJson(AConfig.config().dbProviderConfig(), ProviderConfig.class);
			} catch (Exception e){
				throw new AppException(e, "XSQLCFG");
			}
		}
		if (dataSource == null) {
			BaseDataSource ds = dataSources.get(ns);
			if (ds == null) {
				try {
					ds = (BaseDataSource)new InitialContext().lookup("java:comp/env/jdbc/cn" + ns);
					dataSources.put(ns, ds);
				} catch (Exception e){
					throw new AppException(e, "XSQLDS", ns);
				}
			}
			dataSource = ds;
			blobProvider = new BlobProviderPG(providerConfig.blobsroot, ns);
		}
	}
			
	@Override public String ns(){ return this.ns; }

	@Override public String operationName(){ return this.operationName; }

	@Override public void operationName(String operationName){ this.operationName = operationName; }

	/*************************************************************************************/
	@Override public Object connection() throws AppException { return conn(); }
	
	protected Connection conn() throws AppException{ 
		if (conn == null)
			try {
				conn = (Connection)dataSource.getConnection();
			} catch (SQLException e){
				throw new AppException(e, "XSQL0", operationName, ns, "Connexion");
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
			throw new AppException(e, "XSQL0", operationName, ns, "begin transaction");
		}
	}

	private void commitTransaction() throws AppException {
		if (inTransaction && conn != null) {
			try {
				conn.commit();
			} catch (SQLException e) {
				throw new AppException(e, "XSQL0", operationName, ns, "commit transaction");
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
	
	protected AppException err(PreparedStatement preparedStatement, ResultSet rs, Exception e, String meth) {
		if (preparedStatement != null)
			try { preparedStatement.close(); } catch (SQLException e1) {}
		if (rs != null)
			try { rs.close(); } catch (SQLException e1) {}
		closeConnection();
		return (e instanceof AppException) ? (AppException)e : new AppException(e, "XSQL0", operationName, meth, ns(), sql);
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
	private static final String SELDOCS = "select clid, version, ctime, dtime from namespace ";

	@Override public Collection<DeltaDocument> listDoc(String BEGINclid, long AFTERversion) throws AppException {
		ArrayList<DeltaDocument> lst = new ArrayList<DeltaDocument>();
		StringBuffer sb = new StringBuffer();
		sb.append(SELDOCS);
		int j = 1;
		if (BEGINclid != null) { j++; sb.append("where clid >= ? and clid < ?"); }
		if (AFTERversion != 0) {sb.append(j++ == 1 ? "where " : " and ").append("version > ?");}
		sql = sb.append(";").toString();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			j = 1;
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
	private static final String SELDOC = "select version, ctime, dtime from doc where clid = ?;";
	
	private DeltaDocument getDoc(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELDOC; 
		DeltaDocument dd = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, id.toString());
			rs = preparedStatement.executeQuery();
			if (rs.next()) {
				int j = 1;
				dd = new DeltaDocument();
				dd.id = id;
				dd.version = rs.getLong(j++);
				dd.ctime = rs.getLong(j++);
				dd.dtime = rs.getLong(j++);
			}
			rs.close();
			preparedStatement.close();
			return dd;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "getDoc");
		}
	}
	
	private static final String SELITEMS1 = "select clkey, version, vop, contentt, contentb from item_";
	private static final String SELITEMS2 = "where docid = ? ";
	
	private void addCItem(ResultSet rs, DeltaDocument dd) throws SQLException {
		int j = 1;
		ItemId i = new ItemId(dd.id.descr(), rs.getString(j++));
		if (i.descr() == null) return;
		long _version = rs.getLong(j++);
		long _vop = rs.getLong(j++);
		String _t = getContent(rs);
		dd.items.put(i.toString(), new CItem(i, _version, _vop, _t));		
	}
	
	// copie complète simple - items : tous les items existants  +  tous les items détruits
	private DeltaDocument cas12(DeltaDocument dd) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		sql = SELITEMS1 + dd.id.docclass() + SELITEMS2 + ";"; 
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
		sql = SELITEMS1 + dd.id.docclass() + SELITEMS2 + " and version > ?;"; 
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
		sql = SELITEMS3 + dd.id.docclass() + SELITEMS4; 
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
	@Override
	public HashMap<String, Long> validateDocument(ExecCollect collect) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rawStore(Id id, Upd upd, long vop) throws AppException {
		// TODO Auto-generated method stub
		
	}
	/***********************************************************************************************/

	@Override
	public Collection<Id> searchDocIdsByIndexes(Class<?> docClass, Class<?> itemClass, Cond<?>... ffield)
			throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<XItem> searchItemsByIndexes(Class<?> docClass, Class<?> itemClass, XItemFilter filter,
			Cond<?>... ffield) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	/***********************************************************************************************/
	private static final String INSERTTASK = "insert into taskqueue (clid, nextstart, retry, info) values (?,?,?,?);";
		
	@Override 
	public void insertTask(TaskInfo ti) throws AppException{
		PreparedStatement preparedStatement = null;
		sql = INSERTTASK;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setString(j++, ti.id.toString());
			preparedStatement.setLong(j++, ti.nextStart);
			preparedStatement.setInt(j++, 0);
			preparedStatement.setString(j++, ti.info);
			preparedStatement.setString(j++, null);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "insertTask");
		}
	}

	private static final String UPDTASK = "update taskqueue set nextstart = ?, retry = ?, report = ? where clid = ?;";

	@Override
	public void updateNRRTask(Id id, long nextStart, int retry, String report) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = UPDTASK;
		try {
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setLong(j++, nextStart);
			preparedStatement.setInt(j++, retry);
			preparedStatement.setString(j++, report);
			preparedStatement.setString(j++, id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "updateNRRTaskInfo");
		}
	}

	private static final String DELTASK = "delete from taskqueue where clid = ?;";

	@Override
	public void removeTask(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		sql = DELTASK;
		try {
			preparedStatement = conn().prepareStatement(sql);
			preparedStatement.setString(1, id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			throw err(preparedStatement, null, e, "removeTask");
		}
	}

	private static final String SELTI1 = "select clid, nextstart, retry, info from taskqueue ";

	@Override
	public Collection<TaskInfo> listTask(String BEGINclid, long AFTERnextstart, int MINretry, String CONTAINSinfo) throws AppException {
		ArrayList<TaskInfo> tiList = new ArrayList<TaskInfo>();
		StringBuffer sb = new StringBuffer();
		sb.append(SELTI1);
		int j = 1;
		if (BEGINclid != null) sb.append("where clid >= ? and clid < ?");
		if (AFTERnextstart != 0) {sb.append(j++ == 1 ? "where " : " and ").append("nextstart <= ?");}
		if (MINretry != 0) {sb.append(j++ == 1 ? "where " : " and ").append("retry >= ?");}
		if (CONTAINSinfo != null) {sb.append(j++ == 1 ? "where " : " and ").append("info CONTAINS ?");}
		sql = sb.append(";").toString();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			j = 1;
			if (BEGINclid != null) {
				preparedStatement.setString(j++, BEGINclid);
				preparedStatement.setString(j++, BEGINclid + '\u1FFF');
			}
			if (AFTERnextstart != 0) preparedStatement.setLong(j++, AFTERnextstart);
			if (MINretry != 0) preparedStatement.setInt(j++, MINretry);
			if (CONTAINSinfo != null) preparedStatement.setString(j++, CONTAINSinfo);
			rs = preparedStatement.executeQuery();
			while (rs.next()){
				long nextStart = rs.getLong("nextstart");
				Id id = new Id(rs.getString("clid"));
				int retry = rs.getInt("retry");
				String info = rs.getString("info");
				tiList.add(new TaskInfo(ns(), id, nextStart, retry, info));
			}
			rs.close();
			preparedStatement.close();
			return tiList;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "listTask");
		}
	}

	private static String SELTIREP = "select report from task where clid = ? ;";

	@Override
	public String taskReport(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		String report = "";
		try {
			beginTransaction();
			preparedStatement = conn().prepareStatement(SELTIREP);
			preparedStatement.setString(1, id.toString());
			rs = preparedStatement.executeQuery();
			if (rs.next())
				report = rs.getNString("report");
			rs.close();
			preparedStatement.close();
			return report;
		} catch(Exception e){
			throw err(preparedStatement, rs, e, "taskReport");
		}
	}

	private static final String SELPITEM1 = "select distinct sha from item_";
	private static final String SELPITEM2 = " where sha is not null and docid = ? ;";

	@Override
	public HashSet<String> shas(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		HashSet<String> res = new HashSet<String>();
		sql = SELPITEM1 + id.docclass() + SELPITEM2;
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
	private static final String SELS2 = "select hour from s2cleanup where clid = ?;";

	@Override
	public int lastS2Cleanup(String clid) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		int hour = 0;
		sql = SELS2;
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

	private static final String UPDS2 = "update s2cleanup set hour = ? where clid = ?;";

	@Override
	public void setS2Cleanup(TaskInfo ti) throws AppException {
		PreparedStatement preparedStatement = null;
		int hour = (int)(ti.nextStart / 10000000L);
		sql = UPDS2;
		try {
			beginTransaction();
			preparedStatement = conn().prepareStatement(sql);
			int j = 1;
			preparedStatement.setInt(j++, hour);
			preparedStatement.setString(j++, ti.id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
			insertTask(ti);
			commitTransaction();
		} catch(Exception e){ 
			throw err(preparedStatement, null, e, "setS2Cleanup");
		}
	}
	
}
