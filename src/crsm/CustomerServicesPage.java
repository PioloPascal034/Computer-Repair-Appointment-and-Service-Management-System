package crsm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Searchable, database-backed service cards for Customers. */
final class CustomerServicesPage extends JPanel {
    private final UserSession session;
    private final JPanel cards = new JPanel(new java.awt.GridLayout(0, 3, 14, 14));
    private final JTextField search = new JTextField();
    private final JLabel status = new JLabel(" ");
    private final Consumer<Map<String,Object>> onSelect;
    private List<Map<String,Object>> allServices = List.of();
    private String selectedName = "";

    CustomerServicesPage(UserSession session, Consumer<Map<String,Object>> onSelect) {
        this.session=session; this.onSelect=onSelect;
        setLayout(new BorderLayout(12,12)); setBackground(Color.WHITE);
        JPanel header = new JPanel(new BorderLayout(10,10)); header.setOpaque(false);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy,BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Repair services"); title.setFont(title.getFont().deriveFont(Font.BOLD,22f));
        copy.add(title); copy.add(Box.createVerticalStrut(5)); copy.add(new JLabel("Choose a service to start a request. Final pricing is confirmed after device inspection."));
        JPanel searchBar = new JPanel(new BorderLayout(8,0)); searchBar.setOpaque(false); search.putClientProperty("JTextField.placeholderText","Search service or repair type");
        searchBar.add(search,BorderLayout.CENTER); JButton refresh = new JButton("Refresh"); refresh.addActionListener(e->reload()); searchBar.add(refresh,BorderLayout.EAST);
        header.add(copy,BorderLayout.NORTH); header.add(searchBar,BorderLayout.SOUTH); add(header,BorderLayout.NORTH);
        cards.setOpaque(false); cards.setBorder(BorderFactory.createEmptyBorder(4,2,8,2));
        JScrollPane scroll=new JScrollPane(cards); scroll.setBorder(null); scroll.getViewport().setOpaque(false); add(scroll,BorderLayout.CENTER); add(status,BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){render();}public void removeUpdate(DocumentEvent e){render();}public void changedUpdate(DocumentEvent e){render();}});
        reload();
    }

    void applySearch(String query) { search.setText(query == null ? "" : query); }

    private void reload() {
        status.setText("Loading current services…"); cards.removeAll(); cards.revalidate(); cards.repaint();
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return new CustomerWorkflowService().services(session); }
            protected void done() { try { allServices=get(); status.setText(allServices.size()+" services available · prices shown in the catalog"); render(); }
                catch(Exception ex){Throwable t=ex.getCause()==null?ex:ex.getCause();status.setText("Could not load the service catalog: "+t.getMessage());cards.removeAll();cards.revalidate();cards.repaint();} }
        }.execute();
    }

    private void render() {
        String q=search.getText().trim().toLowerCase(); cards.removeAll(); int count=0;
        for(Map<String,Object> service:allServices){
            String name=String.valueOf(service.getOrDefault("service_name",""));
            String desc=String.valueOf(service.getOrDefault("description",""));
            if(!q.isBlank()&&!name.toLowerCase().contains(q)&&!desc.toLowerCase().contains(q))continue;
            cards.add(card(service,name,desc)); count++;
        }
        if(count==0){JPanel empty=new JPanel(new BorderLayout());empty.setOpaque(false);empty.add(new JLabel(allServices.isEmpty()?"No services are listed yet.":"No services match your search. Try a different word."),BorderLayout.NORTH);cards.add(empty);}
        cards.revalidate(); cards.repaint();
    }

    private JPanel card(Map<String,Object> service,String name,String desc) {
        JPanel card=new JPanel(new BorderLayout(0,8)); card.setBackground(Color.WHITE); card.setPreferredSize(new Dimension(235,190));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(name.equals(selectedName)?new Color(103,111,157):new Color(220,222,232),name.equals(selectedName)?2:1),BorderFactory.createEmptyBorder(0,0,12,0)));
        JLabel image=new JLabel(iconFor(name),JLabel.CENTER); image.setOpaque(true); image.setBackground(new Color(238,239,247)); image.setForeground(new Color(45,50,80)); image.setFont(image.getFont().deriveFont(Font.BOLD,34f)); image.setPreferredSize(new Dimension(220,72)); card.add(image,BorderLayout.NORTH);
        JPanel text=new JPanel();text.setOpaque(false);text.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));text.setLayout(new BoxLayout(text,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("<html><b>"+escape(name)+"</b></html>");title.setFont(title.getFont().deriveFont(Font.BOLD,15f)); text.add(title);text.add(Box.createVerticalStrut(5));
        JLabel description=new JLabel("<html><div style='width:190px'>"+escape(desc)+"</div></html>");description.setForeground(new Color(90,94,110));text.add(description);text.add(Box.createVerticalStrut(8));
        Object price=service.get("service_price"); JLabel cost=new JLabel("Starting at ₱"+price);cost.setForeground(new Color(45,50,80));cost.setFont(cost.getFont().deriveFont(Font.BOLD,14f));text.add(cost);card.add(text,BorderLayout.CENTER);
        JButton choose=new JButton(name.equals(selectedName)?"Selected · Request service":"Choose service");choose.setFocusPainted(false);choose.addActionListener(e->{selectedName=name;render();onSelect.accept(service);});
        JPanel foot=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));foot.setOpaque(false);foot.add(choose);card.add(foot,BorderLayout.SOUTH);
        card.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){selectedName=name;render();}});
        return card;
    }
    private static String iconFor(String name){String n=name.toLowerCase();if(n.contains("battery"))return "▰  BATTERY";if(n.contains("screen"))return "▣  DISPLAY";if(n.contains("keyboard"))return "⌨  KEYBOARD";if(n.contains("virus")||n.contains("software"))return "◉  SOFTWARE";if(n.contains("clean"))return "✦  CLEANING";if(n.contains("system")||n.contains("diagnostic"))return "⚙  DIAGNOSTICS";return "⌁  REPAIR SERVICE";}
    private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
