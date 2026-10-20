package crsm;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Small role pages shared by the application shell. */
final class CrsmPages {
    private CrsmPages() {}
    static DashboardPage dashboard(CrsmHost host) { return new DashboardPage(host); }
    static ReportsPage reports(CrsmHost host) { return new ReportsPage(host); }
    static UserAccountsPage accounts(CrsmHost host) { return new UserAccountsPage(host); }
    static ProfilePage profile(CrsmHost host) { return new ProfilePage(host); }
    static SettingsPage settings(CrsmHost host) { return new SettingsPage(host); }

    private static class MessagePage extends JPanel {
        MessagePage(String title, String detail) { super(new BorderLayout(8, 8)); add(new JLabel(title), BorderLayout.NORTH); add(new JLabel(detail), BorderLayout.CENTER); }
    }
    static final class DashboardPage extends MessagePage { DashboardPage(CrsmHost host) { super("Dashboard", "Welcome to the Computer Repair Service Management System."); } void reload() {} }
    static final class ReportsPage extends MessagePage { ReportsPage(CrsmHost host) { super("Reports", "Reports are available after the database schema is configured."); } }
    static final class UserAccountsPage extends MessagePage { UserAccountsPage(CrsmHost host) { super("User Accounts", "Account management is available to administrators."); } }
    static final class ProfilePage extends MessagePage { ProfilePage(CrsmHost host) { super("My Profile", "Manage your CRSM account profile."); } }
    static final class SettingsPage extends MessagePage { SettingsPage(CrsmHost host) { super("Settings", "Application settings."); } }
}
