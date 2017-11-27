Task : groupId ? J'utilisais comme ID de la task le couple groupId / version : certes il n'y avait qu'une task avec une version donnée dans un groupe.
Vérifier la logique d'ensemble et le fait qu'une task n'est plus identifiée par sa version.

	public static boolean startCleanup(String clid, boolean transaction) throws AppException {
Vérifier usage de transaction

Gestion de cache / providers / execCollect ... 