package fr.cryptonote.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import fr.cryptonote.base.CDoc.CItem;
import fr.cryptonote.base.Cond.Type;
import fr.cryptonote.base.DocumentDescr.ItemDescr;

public class TaskUpdDiff extends Document {
	public static class ItemToCopy {
		public String clid;
		public String clkey;
		public String json;
		public ItemToCopy(String clid, String clkey, String json) { this.clid = clid; this.clkey = clkey; this.json = json; }
	}
	
	public static class ToDo extends HashMap<String,ByDocClass> {
		private static final long serialVersionUID = 1L;
	}
	
	public static class ByDocClass {
		public DocumentDescr dd;
		public HashMap<String,ByTargetClkey> targets = new HashMap<String,ByTargetClkey>();
		private ByDocClass(DocumentDescr descr) { dd = descr; }
	}

	public static class ByTargetClkey {
		public String clkey;
		public ItemDescr itd;
		public String content;
		public ExportedFields exportedFields;
		private ByTargetClkey(ItemDescr descr, String clk, String content) { 
			itd = descr; 
			clkey = clk;
			this.content = content;
			if (!descr.isRaw())
				try { exportedFields = ((ItemSingleton)descr.newItem(content, "")).exportFields(descr); } catch (AppException e) {	}
		}
	}

	public static class ListItemToCopy extends ArrayList<ItemToCopy>{
		private static final long serialVersionUID = 1L;
				
		public ToDo todo() {
			ToDo todo = new ToDo();
			for(ItemToCopy itc : this) {
				Id id = new Id(itc.clid);
				if (id.descr() == null) continue;
				ItemId itid1 = new ItemId(id.descr(), itc.clkey);
				if (itid1.descr() == null) continue;
				char sep = itid1.descr().separator();
				DocumentDescr[] docs = itid1.descr().copyToDocs();
				if (docs == null || docs.length == 0) continue;
				
				for(DocumentDescr dd : docs) {
					ByDocClass bdc = todo.get(dd.name());
					if (bdc == null) {
						bdc = new ByDocClass(dd);
						todo.put(id.docclass(), bdc);
					}
										
					ItemDescr itd2 = dd.itemDescr(itid1.descr().name());
					if (itd2 != null) {
						String clk = itc.clkey + sep + id.docid();
						ByTargetClkey target = bdc.targets.get(clk);
						if (target == null) {
							target = new ByTargetClkey(itd2, clk, itc.json);
							bdc.targets.put(itd2.name(), target);
						}
					}
				}
			}
			return todo;
		}
		
	}


	void add(ItemToCopy item) throws AppException{ if (item != null) hdr().items.add(item); }
	
	void setVop(long version) throws AppException { hdr().vop = version; }
	
	CItem commit() throws AppException { 
		Hdr hdr = hdr();
		hdr.commit(); 
		return hdr._citem();
	}

	public static class Hdr extends Singleton {
		public long vop;
		public ListItemToCopy items = new ListItemToCopy();
	}
	
	private Hdr hdr() throws AppException { return (Hdr)singletonOrNew(Hdr.class); }
	
	public static class Task extends Operation {
		@Override public void work() throws AppException {
			TaskUpdDiff t = (TaskUpdDiff)Document.get(taskId(), 0);
			if (t == null) return;
			Hdr hdr = t.hdr();
			ToDo todo = hdr.items.todo();
			HashMap<String,ArrayList<ByTargetClkey>> byDoc = new HashMap<String,ArrayList<ByTargetClkey>>();
			for(ByDocClass bdc : todo.values()) {
				for(ByTargetClkey target : bdc.targets.values()) {
					Collection<Id> ids = searchDocIdsByIndexes(bdc.dd.clazz(), target.itd.clazz(), new Cond<String>(Type.eq, target.clkey).name("clkey"));
					for(Id id : ids) {
						ArrayList<ByTargetClkey> lst = byDoc.get(id.toString());
						if (lst == null) {
							lst = new ArrayList<ByTargetClkey>();
							byDoc.put(id.toString(), lst);
						}
						lst.add(target);
					}
				}
			}
			ExecContext.current().dbProvider().rawDuplicate(hdr.vop, byDoc);
			t.delete();
		}
	}
}
