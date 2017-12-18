package fr.cryptonote.base;

public interface ISyncFilter {
	public void init(ExecContext ec, Document d) throws AppException;
}
