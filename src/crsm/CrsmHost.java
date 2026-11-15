package crsm;

interface CrsmHost {
    UserSession session();
    void replaceSession(UserSession session);
    String roleName();
    RepairRepository repository();
    int pageSize();
    boolean customerRecordsBlocked();
    boolean canMutate(RepairDataModel.Entity entity);
    void applySearch(String query);
    void refreshHeader();
    void showDatabaseError(Exception exception);
    void signOut();
    void showPage(String name);
}
