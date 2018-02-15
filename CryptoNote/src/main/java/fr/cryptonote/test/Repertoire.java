package fr.cryptonote.test;

import fr.cryptonote.base.AppException;
import fr.cryptonote.base.Cond;
import fr.cryptonote.base.ADifferedCopy;
import fr.cryptonote.base.Document;
import fr.cryptonote.base.DocumentDescr.ItemDescr;
import fr.cryptonote.base.ExecContext;
import fr.cryptonote.base.ISyncFilter;
import fr.cryptonote.base.AExportedField;
import fr.cryptonote.base.Operation;
import fr.cryptonote.base.Result;

public class Repertoire extends Document {
	
	public static Repertoire get(String nom) throws AppException {
		return (Repertoire) Document.getOrNew(new Document.Id(Repertoire.class, nom ));
	}
	
	public Contact contact(String code) throws AppException{
		return (Contact) itemOrNew(Contact.class, code);
	}
	
	@ADifferedCopy (copyToDocs={Repertoire.class}, separator='/')
	public static class Contact extends Document.Item {
		String code;
		String nom;
		@AExportedField String codePostal;
	}
	
	public static class SyncFilter implements ISyncFilter {
		public int skip;
		public int mask;
		@Override
		public void init(ExecContext ec, Document d) throws AppException {
		}
	}

	@Override public FilterPolicy filter(ISyncFilter sf){
		return FilterPolicy.Accept;
	}
	
	@Override public FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String itemid){
		if (itemDescr.clazz() != Contact.class) return FilterPolicy.Exclude;
		SyncFilter f = (SyncFilter)sf;
		if (itemid.equals("c"+f.skip)) return FilterPolicy.Exclude;
		return FilterPolicy.Continue;
	}

	@Override public FilterPolicy filter(ISyncFilter sf, ItemDescr itemDescr, String itemid, BItem item){
		if (itemDescr.clazz() != Contact.class) return FilterPolicy.Accept;
		SyncFilter f = (SyncFilter)sf;
		Contact c = (Contact)item;
		if (c.nom.equals("Paulo-"+f.mask)) {
			c.nom = "***";
			return FilterPolicy.Continue;
		}
		return FilterPolicy.Accept;
	}

	public static class RepGen extends Operation {
		public static class Param {
			String rep;
			int n1;
			int n2;
		}
		
		Param param;
		
		@Override public Result work() throws AppException {
			Repertoire r = Repertoire.get(param.rep);
			for(int i=param.n1; i < param.n2; i++){
				Contact c = r.contact("c" + i);
				c.code = "" + i;
				c.nom = "Paulo-" + i;
				c.codePostal = "" + (94200 + i);
				c.commit(0);
			}
			return Result.empty();
		}
	}

	public static class RepUpd extends Operation {
		public static class Param {
			String rep;
			int n1;
			int n2;
			String nom;
		}
		
		Param param;
		
		@Override public Result work() throws AppException {
			Repertoire r = null;
			r = Repertoire.get(param.rep);
			for(int i = param.n1; i < param.n2; i++){
				Contact c = r.contact("c" + i);
				c.nom = param.nom + i;
				c.commit(0);
			}
			return Result.empty();
		}
	}

	public static class RepSearch extends Operation {
		public static class Param {
			String cp1;
			String cp2;
		}
		
		Param param;
		
		@Override public Result work() throws AppException {
			Cond<String> c = new Cond<String>(Cond.Type.gele, param.cp1, param.cp2).name("codepostal");
			return Result.json(searchDocIdsByIndexes(Repertoire.class, Contact.class, c));
		}
	}

}
