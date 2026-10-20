package crsm;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Minimal database list placeholder retained for the management navigation. */
final class CrsmListing extends JPanel {
    CrsmListing(CrsmHost host, RepairDataModel.Entity entity) { super(new BorderLayout()); add(new JLabel(entity.label + " management"), BorderLayout.NORTH); }
    void reload() {}
    void applySearch(String query) {}
}
