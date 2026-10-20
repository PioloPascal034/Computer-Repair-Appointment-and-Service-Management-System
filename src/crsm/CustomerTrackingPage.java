package crsm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
        this.session=session; setLayout(new BorderLayout(16,16)); setBackground(CrsmTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
        JButton refresh=CrsmUi.ghost("Refresh repairs"); refresh.addActionListener(e->reload());
        add(CrsmUi.pageHead("My repairs", "Track repair progress, review estimates, and manage your repair requests.", refresh),BorderLayout.NORTH);
        rows.setOpaque(false); rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        JScrollPane scroll=new JScrollPane(rows); scroll.setBorder(null); scroll.getViewport().setOpaque(false); scroll.getViewport().setBackground(CrsmTheme.CANVAS);
        add(scroll,BorderLayout.CENTER); reload();
    }

    private void reload() {
        rows.removeAll(); rows.add(CrsmUi.notice("Loading your repairs", "Your latest repair requests are being retrieved securely."));
        rows.add(Box.createVerticalStrut(12)); rows.revalidate(); rows.repaint();
        new SwingWorker<List<Repair>,Void>() {
            protected List<Repair> doInBackground() throws Exception {
                List<Repair> result=new ArrayList<>();
                for (Map<String,Object> row:service.repairs(session)) {
                    int id=CustomerWorkflowService.integerId(row.get("repair_request_id"), "repair_requests.repair_request_id");
                    List<Map<String,Object>> options=service.estimateOptions(session,id);
                    List<Map<String,Object>> payments=service.payments(session,id);
                    result.add(new Repair(row,options,payments));
                }
                return result;
            }
            protected void done() {
                rows.removeAll();
                try { List<Repair> repairs=get();
                    if (repairs.isEmpty()) rows.add(CrsmUi.notice("No repair requests yet", "Submit a new repair request and it will appear here with its current status and estimate."));
                    for (Repair repair:repairs) { rows.add(card(repair)); rows.add(Box.createVerticalStrut(12)); }
                } catch(Exception ex) { rows.add(CrsmUi.notice("Unable to load repairs", message(ex))); }
                rows.revalidate(); rows.repaint();
            }
        }.execute();
    }

    private JPanel card(Repair repair) {
        Map<String,Object> row=repair.row(); int id=CustomerWorkflowService.integerId(row.get("repair_request_id"), "repair_requests.repair_request_id");
        Map<?,?> device=(Map<?,?>)row.get("computers");
        Map<?,?> selected=(Map<?,?>)row.get("repair_services");
        String deviceName=String.join(" ",str(device.get("brand")),str(device.get("model"))).trim();
        JPanel card=CrsmUi.card(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        JPanel titleRow=new JPanel(new BorderLayout(12,0)); titleRow.setOpaque(false);
        JLabel title=CrsmUi.heading("Request #"+id+"  ·  "+deviceName,16); titleRow.add(title,BorderLayout.CENTER); titleRow.add(CrsmUi.badge(str(row.get("repair_status"))),BorderLayout.EAST); card.add(titleRow);
        card.add(Box.createVerticalStrut(10));
        card.add(detail("Selected service", selected == null ? "Staff recommendation after diagnosis" : str(selected.get("service_name"))));
        card.add(detail("Reported problem", str(row.get("reported_problem"))));
        card.add(detail("Requested work", str(row.get("requested_work"))));
        JPanel pricing=new JPanel(new GridLayout(1,2,12,0)); pricing.setOpaque(false);
        pricing.add(detail("Current estimate · not final",money(row.get("estimated_cost"))));
        pricing.add(detail("Final cost · after inspection",money(row.get("final_cost"))));
        card.add(Box.createVerticalStrut(6)); card.add(pricing);
        for(Map<String,Object> payment:repair.payments()) card.add(detail("Payment",str(payment.get("payment_date"))+" · "+str(payment.get("amount_paid"))+" · "+str(payment.get("payment_method"))+" · Receipt "+str(payment.get("receipt_number"))));
        card.add(Box.createVerticalStrut(8));
        if (row.get("estimated_cost")!=null && row.get("customer_decision")==null && "Under Diagnosis".equals(str(row.get("repair_status")))) {
            List<JCheckBox> choices=new ArrayList<>();
            if(!repair.options().isEmpty()) card.add(new JLabel("Optional estimate items (the base repair is included):"));
            for (Map<String,Object> option:repair.options()) {
                long optionId=Long.parseLong(String.valueOf(option.get("option_id")));
                JCheckBox choice=new JCheckBox(str(option.get("option_name"))+" — "+money(option.get("price"))+"  "+str(option.get("description")));
                choice.setOpaque(false);
                choice.putClientProperty("option_id",optionId); choices.add(choice); card.add(choice);
            }
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton approve=CrsmUi.primary("Approve base + selected items"), decline=CrsmUi.ghost("Decline estimate");
            approve.addActionListener(e->decide(id,"APPROVE",choices)); decline.addActionListener(e->decide(id,"DECLINE",List.of()));
            actions.add(approve); actions.add(decline); card.add(actions);
        }
        JButton photo=CrsmUi.ghost("Attach damage photo");
        photo.addActionListener(e->choosePhoto(id,photo)); card.add(photo);
        if("DECLINED".equals(String.valueOf(row.get("customer_decision")))) {
            JButton review=CrsmUi.ghost("Request Admin review"); review.addActionListener(e->requestReview(id)); card.add(review);
        }
        card.add(Box.createVerticalStrut(8)); return card;
    }

    private JPanel detail(String label,String value) {
        JPanel detail=new JPanel(); detail.setOpaque(false); detail.setLayout(new BoxLayout(detail,BoxLayout.Y_AXIS));
        JLabel caption=CrsmUi.muted(label); caption.setFont(CrsmTheme.font(Font.PLAIN,11));
        JLabel text=new JLabel(value); text.setForeground(CrsmTheme.TEXT); text.setFont(CrsmTheme.font(Font.PLAIN,13));
        detail.add(caption); detail.add(Box.createVerticalStrut(3)); detail.add(text); return detail;
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
    private static String money(Object value) { return value==null?"Not set":CrsmUi.money(value); }
    private record Repair(Map<String,Object> row,List<Map<String,Object>> options,List<Map<String,Object>> payments) {}
}