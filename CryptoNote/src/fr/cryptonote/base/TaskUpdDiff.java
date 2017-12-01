package fr.cryptonote.base;

import java.util.HashMap;

import fr.cryptonote.base.DocumentDescr.ItemDescr;
import fr.cryptonote.provider.DBProvider;

public class TaskUpdDiff extends Document {
	private static final int NBDOCSBYREQ = 50;
	
	void add(Document.Id id, String key, BItem item) throws AppException{
		if (item == null || id == null) throw new AppException("BTRIGGER1");
		String n = item.getClass().getSimpleName();
		ItemDescr descr = id.descr().itemDescr(n);
		if (descr == null) throw new AppException("BTRIGGER2", n);
		if (!descr.isSingleton() && (key == null || key.length() == 0)) throw new AppException("BTRIGGER3", n);
		((Upd)itemOrNew(Upd.class, id.toString())).put(descr.name() + (descr.isSingleton() ? "" : "." + key), item.serializedBItem());
	}

	public static class Upd extends Item {
		public HashMap<String,SerializedBItem> lst;
		public void put(String clkey, SerializedBItem sbi) throws AppException { 
			if (lst == null) lst = new HashMap<String,SerializedBItem>();
			lst.put(clkey, sbi); 
			commit();
		}
	}
	
	public static class Task extends Operation {
		@Override public void work() throws AppException {
			int ckp = taskCheckpoint() == null ? 0 : (int)taskCheckpoint();
			ckp++;
			TaskUpdDiff t = (TaskUpdDiff)Document.get(taskId(), 0);
			if (t == null) return;
			
			DBProvider provider = execContext().dbProvider();
			
			String[] docIds = t.getKeys(Upd.class);
			for(int i = 0; i < docIds.length; i++) {
				Document.Id id = new Document.Id(docIds[i]);
				Upd upd = (Upd)t.item(Upd.class, docIds[i]);
				if (id.descr() != null) 
					provider.rawStore(id, upd);
				upd.delete();
				if (id.descr() != null) break;
			}
			
			int nbx = t.getKeys(Upd.class).length;
			if (nbx == 0) {
				t.delete();
				taskCheckpoint(null);
			} else {
				if (ckp < NBDOCSBYREQ) // NBDOCSBYREQ dans la même requête
					taskCheckpoint(new Integer(ckp));
				else {
					setTask(taskId(), Stamp.fromNow(1000).stamp(), "" + nbx);
					taskCheckpoint(null);
				}
			}
			
		}
	}
}
