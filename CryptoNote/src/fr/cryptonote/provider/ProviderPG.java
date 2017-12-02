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
import fr.cryptonote.base.Cond;
import fr.cryptonote.base.Document.Id;
import fr.cryptonote.base.Document.XItem;
import fr.cryptonote.base.Document.XItemFilter;
import fr.cryptonote.base.ExecContext.ExecCollect;
import fr.cryptonote.base.JSON;
import fr.cryptonote.base.TaskInfo;
import fr.cryptonote.base.TaskUpdDiff.Upd;
import fr.sportes.base.DocumentId;
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
	
	private BlobProvider blobProvider;
	
	@Override public void shutdwon() { }
	
	@Override public BlobProvider blobProvider() { return blobProvider; }

	public ProviderPG(String ns) throws AppException {
		if (ns == null || ns.length() == 0) ns = "z";
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
					InitialContext ic = new InitialContext();
					ds = (BaseDataSource)ic.lookup("java:comp/env/jdbc/cn" + ns);
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

	protected void err(PreparedStatement preparedStatement, ResultSet rs) {
		if (preparedStatement != null)
			try { preparedStatement.close(); } catch (SQLException e1) {}
		if (rs != null)
			try { rs.close(); } catch (SQLException e1) {}
		closeConnection();
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
		String sql = INSDBINFO; 
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
			err(preparedStatement, rs);
			throw (e instanceof AppException) ? (AppException)e : 
				new AppException(e, "XSQL0", operationName, ns, sql);
		}
	}

	/***********************************************************************************************/
	@Override
	public DeltaDocument getDocument(Id id, long ctime, long version, long dtime) {
		// TODO Auto-generated method stub
		return null;
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
	public Collection<Id> searchDocIdsByIndexes(String docClass, String itemClass, Cond<?>... ffield)
			throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<XItem> searchItemsByIndexes(String docClass, String itemClass, XItemFilter filter,
			Cond<?>... ffield) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	/***********************************************************************************************/
	private static final String UPSERTTASK = 
			"insert into taskqueue (clid, nextstart, retry, info) values (?,?,?,?);";
		
	@Override 
	public void insertTask(TaskInfo ti) throws AppException{
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn().prepareStatement(UPSERTTASK);
			int j = 1;
			preparedStatement.setString(j++, ti.id.toString());
			preparedStatement.setLong(j++, ti.nextStart);
			preparedStatement.setInt(j++, 0);
			preparedStatement.setString(j++, ti.info);
			preparedStatement.setString(j++, null);
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			err(preparedStatement, null);
			throw (e instanceof AppException) ? (AppException)e : new AppException(e, "XSQL0", "upsertTaskInfo", ti.ns, UPSERTTASK);
		}
	}

	private static final String UPDTASK = 
			"update taskqueue set nextstart = ?, retry = ?, report = ? where clid = ?;";

	@Override
	public void updateNRRTask(Id id, long nextStart, int retry, String report) throws AppException {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn().prepareStatement(UPDTASK);
			int j = 1;
			preparedStatement.setLong(j++, nextStart);
			preparedStatement.setInt(j++, retry);
			preparedStatement.setString(j++, report);
			preparedStatement.setString(j++, id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			err(preparedStatement, null);
			throw (e instanceof AppException) ? (AppException)e : new AppException(e, "XSQL0", "updateNRRTaskInfo", ns(), UPDTASK);
		}
	}

	private static final String DELTASK = "delete from taskqueue where clid = ?;";

	@Override
	public void removeTask(Id id) throws AppException {
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn().prepareStatement(DELTASK);
			preparedStatement.setString(1, id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch(Exception e){
			err(preparedStatement, null);
			throw (e instanceof AppException) ? (AppException)e : new AppException(e, "XSQL0", "removeTask", ns(), DELTASK);
		}
	}

	private static final String SELTI1 = "select clid, nextstart, retry, info from taskqueue ";

	@Override
	public Collection<TaskInfo> listTask(String BEGINclid, long AFTERnextstart, int MINretry, String CONTAINSinfo) throws AppException {
		ArrayList<TaskInfo> tiList = new ArrayList<TaskInfo>();
		StringBuffer sb = new StringBuffer();
		sb.append(SELTI1);
		int j = 1;
		if (ti.id != null) sb.append("where clid = ?");
		if (ti.nextStart != 0) {sb.append(j++ == 1 ? "where " : " and ").append("nextstart <= ?");}
		if (ti.retry != 0) {sb.append(j++ == 1 ? "where " : " and ").append("retry >= ?");}
		if (ti.info != null) {sb.append(j++ == 1 ? "where " : " and ").append("info = ?");}
		String sql = sb.append(";").toString();
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		try {
			preparedStatement = conn().prepareStatement(sql);
			j = 1;
			if (ti.id != null) preparedStatement.setString(j++, ti.id.toString());
			if (ti.nextStart != 0) preparedStatement.setLong(j++, ti.nextStart);
			if (ti.retry != 0) preparedStatement.setInt(j++, ti.retry);
			if (ti.info != null) preparedStatement.setString(j++, ti.info);
			rs = preparedStatement.executeQuery();
			while (rs.next()){
				long nextStart = rs.getLong("nextstart");
				Id id = new Id(rs.getString("clid"));
				int retry = rs.getInt("retry");
				String info = rs.getString("info");
				tiList.add(new TaskInfo(ti.ns, id, nextStart, retry, info));
			}
			rs.close();
			preparedStatement.close();
			return tiList;
		} catch(Exception e){
			err(preparedStatement, rs);
			throw (e instanceof AppException) ? (AppException)e : 
				new AppException(e, "XSQL0", "listTask", "", sql);
		}
	}

	@Override
	public String taskReport(Id id) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet<String> shas(String clid) throws AppException {
		// TODO Auto-generated method stub
		return null;
	}

	/***********************************************************************************************/
	private static final String SELS2 = "select hour from s2cleanup where clid = ?;";

	@Override
	public int lastS2Cleanup(String clid) throws AppException {
		PreparedStatement preparedStatement = null;
		ResultSet rs = null;
		int hour = 0;
		try {
			preparedStatement = conn().prepareStatement(SELS2);
			preparedStatement.setString(1, clid);
			rs = preparedStatement.executeQuery();
			if (rs.next())
				hour = rs.getInt("hour");
			rs.close();
			preparedStatement.close();
			return hour;
		} catch(Exception e){
			err(preparedStatement, rs);
			throw (e instanceof AppException) ? (AppException)e : new AppException(e, "XSQL0", "lastS2Cleanup", ns(), SELS2);
		}
	}

	private static final String UPDS2 = "update s2cleanup set hour = ? where clid = ?;";

	@Override
	public void setS2Cleanup(TaskInfo ti) throws AppException {
		PreparedStatement preparedStatement = null;
		int hour = (int)(ti.nextStart / 10000000L);
		try {
			beginTransaction();
			preparedStatement = conn().prepareStatement(UPDS2);
			int j = 1;
			preparedStatement.setInt(j++, hour);
			preparedStatement.setString(j++, ti.id.toString());
			preparedStatement.executeUpdate();
			preparedStatement.close();
			insertTask(ti);
			commitTransaction();
		} catch(Exception e){
			rollbackTransaction();
			err(preparedStatement, null);
			throw (e instanceof AppException) ? (AppException)e : new AppException(e, "XSQL0", "upsertTaskInfo", ns(), UPDS2);
		}
	}
	
}
