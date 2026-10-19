package crsm;

interface CrsmHost {
    UserSession session();
    void replaceSession(UserSession session);
    String roleName();
    RepairRepository repository();
    void showPage(String name);
    void showDatabaseError(Exception exception);
    void signOut();
    int pageSize();
    void refreshHeader();
}
