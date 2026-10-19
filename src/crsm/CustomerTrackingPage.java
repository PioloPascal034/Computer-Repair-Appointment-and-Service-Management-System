package crsm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

/** Customer-owned repair tracking and estimate response controls. */
final class CustomerTrackingPage extends JPanel {
    private final UserSession session;
    private final CustomerWorkflowService service=new CustomerWorkflowService();
    private final JPanel rows=new JPanel();

    CustomerTrackingPage(UserSession session) {
        this.session=session; setLayout(new BorderLayout(8,8));
        rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        JButton refresh=new JButton("Refresh repairs"); refresh.addActionListener(e->reload());
        add(refresh,BorderLayout.NORTH); add(new JScrollPane(rows),BorderLayout.CENTER); reload();
    }

    private void reload() {
        rows.removeAll(); rows.add(new JLabel("Loading your repair requests…")); rows.revalidate(); rows.repaint();
        new SwingWorker<List<Repair>,Void>() {
            protected List<Repair> doInBackground() throws Exception {
                List<Repair> result=new ArrayList<>();
                for (Map<String,Object> row:service.repairs(session)) {
                    int id=Integer.parseInt(String.valueOf(row.get("repair_request_id")));
                    List<Map<String,Object>> options=service.estimateOptions(session,id);
                    List<Map<String,Object>> payments=service.payments(session,id);
                    result.add(new Repair(row,options,payments));
                }
                return result;
            }
            protected void done() {
                rows.removeAll();
                try { List<Repair> repairs=get();
                    if (repairs.isEmpty()) rows.add(new JLabel("No repair requests yet. Submit a new request to get started."));
                    for (Repair repair:repairs) rows.add(card(repair));
                } catch(Exception ex) { rows.add(new JLabel("Unable to load repair requests: "+message(ex))); }
                rows.revalidate(); rows.repaint();
            }
        }.execute();
    }

    private JPanel card(Repair repair) {
        Map<String,Object> row=repair.row(); int id=Integer.parseInt(String.valueOf(row.get("repair_request_id")));
        Map<?,?> device=(Map<?,?>)row.get("computers");
        String deviceName=String.join(" ",str(device.get("brand")),str(device.get("model"))).trim();
        JPanel card=new JPanel(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(12,14,12,14)));
        JLabel title=new JLabel("Request #"+id+" · "+deviceName+" · "+str(row.get("repair_status")));
        title.setFont(title.getFont().deriveFont(Font.BOLD)); card.add(title);
        card.add(new JLabel("Problem: "+str(row.get("reported_problem"))));
        card.add(new JLabel("Requested work: "+str(row.get("requested_work"))));
        card.add(new JLabel("Estimate: "+str(row.get("estimated_cost"))+"   Final cost: "+str(row.get("final_cost"))));
        for(Map<String,Object> payment:repair.payments()) card.add(new JLabel("Payment "+str(payment.get("payment_date"))+" · "+str(payment.get("amount_paid"))+" · "+str(payment.get("payment_method"))+" · Receipt "+str(payment.get("receipt_number"))));
        if (row.get("estimated_cost")!=null && row.get("customer_decision")==null) {
            List<JCheckBox> choices=new ArrayList<>();
            if(!repair.options().isEmpty()) card.add(new JLabel("Optional estimate items (the base repair is included):"));
            for (Map<String,Object> option:repair.options()) {
                long optionId=Long.parseLong(String.valueOf(option.get("option_id")));
                JCheckBox choice=new JCheckBox(str(option.get("option_name"))+" — "+str(option.get("price"))+"  "+str(option.get("description")));
                choice.putClientProperty("option_id",optionId); choices.add(choice); card.add(choice);
            }
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton approve=new JButton("Approve base + selected items"), decline=new JButton("Decline estimate");
            approve.addActionListener(e->decide(id,"APPROVE",choices)); decline.addActionListener(e->decide(id,"DECLINE",List.of()));
            actions.add(approve); actions.add(decline); card.add(actions);
        }
        JButton photo=new JButton("Attach damage photo");
        photo.addActionListener(e->choosePhoto(id,photo)); card.add(photo);
        if("DECLINED".equals(String.valueOf(row.get("customer_decision")))) {
            JButton review=new JButton("Request Admin review"); review.addActionListener(e->requestReview(id)); card.add(review);
        }
        card.add(Box.createVerticalStrut(8)); return card;
    }

    private void decide(int id,String decision,List<JCheckBox> choices) {
        List<Long> selected=choices.stream().filter(JCheckBox::isSelected).map(c->(Long)c.getClientProperty("option_id")).toList();
        new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception { service.decideEstimate(session,id,decision,selected); return null; }
            protected void done() { try { get(); JOptionPane.showMessageDialog(CustomerTrackingPage.this,"Your estimate decision was saved."); reload(); } catch(Exception ex) { JOptionPane.showMessageDialog(CustomerTrackingPage.this,message(ex),"Unable to save decision",JOptionPane.ERROR_MESSAGE); } }
        }.execute();
    }

    private void choosePhoto(int id,JButton button) {
        JFileChooser chooser=new JFileChooser();
        if (chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        Path path=chooser.getSelectedFile().toPath(); button.setEnabled(false);
        new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception {
                String objectPath=new SupabaseStorageService().uploadRepairPhoto(session,id,path);
                service.registerPhoto(session,id,objectPath); return null;
            }
            protected void done() { button.setEnabled(true); try { get(); JOptionPane.showMessageDialog(CustomerTrackingPage.this,"Photo attached to request #"+id+"."); } catch(Exception ex) { JOptionPane.showMessageDialog(CustomerTrackingPage.this,message(ex),"Photo upload failed",JOptionPane.ERROR_MESSAGE); } }
        }.execute();
    }

    private void requestReview(int repairId) {
        String note=JOptionPane.showInputDialog(this,"Describe the estimate or upgrade concern:","Request estimate review",JOptionPane.QUESTION_MESSAGE);
        if(note==null||note.isBlank())return;
        new SwingWorker<Long,Void>() { protected Long doInBackground() throws Exception { return service.requestEstimateReview(session,repairId,note.trim()); }
            protected void done(){try{long id=get();JOptionPane.showMessageDialog(CustomerTrackingPage.this,"Review #"+id+" was sent to an administrator.");reload();}catch(Exception ex){JOptionPane.showMessageDialog(CustomerTrackingPage.this,message(ex),"Unable to request review",JOptionPane.ERROR_MESSAGE);}}
        }.execute();
    }

    private static String message(Exception ex) { Throwable t=ex.getCause()==null?ex:ex.getCause(); return t.getMessage()==null?"Request failed.":t.getMessage(); }
    private static String str(Object o) { return o==null?"—":o.toString(); }
    private record Repair(Map<String,Object> row,List<Map<String,Object>> options,List<Map<String,Object>> payments) {}
}
