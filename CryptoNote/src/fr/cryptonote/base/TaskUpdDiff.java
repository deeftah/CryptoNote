package fr.cryptonote.base;

import java.util.ArrayList;

import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.provider.DBProvider.ItemToCopy;

public class TaskUpdDiff extends Document {
	
	void add(ItemToCopy item) throws AppException{ if (item != null) hdr().items.add(item); }
	
	void setVop(long version) throws AppException { hdr().vop = version; }
	
	CItem commit() throws AppException { 
		Hdr hdr = hdr();
		hdr.commit(); 
		return hdr._citem();
	}

	public static class Hdr extends Singleton {
		public long vop;
		public ArrayList<ItemToCopy> items = new ArrayList<ItemToCopy>();
	}
	
	private Hdr hdr() throws AppException { return (Hdr)singletonOrNew(Hdr.class); }
		
	public static class Task extends Operation {
		@Override public void work() throws AppException {
			TaskUpdDiff t = (TaskUpdDiff)Document.get(taskId(), 0);
			if (t == null) return;
			
			Hdr hdr = t.hdr();
			execContext().dbProvider().rawDuplicate(hdr.vop, hdr.items);
			
			t.delete();
		}
	}
}
