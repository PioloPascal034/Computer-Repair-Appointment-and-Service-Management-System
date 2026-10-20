package crsm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/** JWT-backed staff work, repair completion, payment, and pickup operations. */
final class RepairOperationsPage extends JPanel {
    private final UserSession session;
    private final CustomerWorkflowService service=new CustomerWorkflowService();
    private final JComboBox<RepairOption> repairs=new JComboBox<>();
    private final List<JButton> repairActions=new ArrayList<>();

    RepairOperationsPage(UserSession session) {
        this.session=session; setLayout(new BorderLayout(16,16)); setBackground(CrsmTheme.CANVAS); setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
        JButton refresh=CrsmUi.ghost("Refresh"); refresh.addActionListener(e->reload());
        add(CrsmUi.pageHead("Repair workflow","Manage technician assignment, diagnosis, estimates, payments, and pickup.",refresh),BorderLayout.NORTH);
        JPanel requestPicker=new JPanel(new BorderLayout(10,0)); requestPicker.setOpaque(false); CrsmUi.styleCombo(repairs);
        requestPicker.add(new JLabel("Repair request"),BorderLayout.WEST); requestPicker.add(repairs,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(0,2,12,12)); actions.setOpaque(false);
        JButton work=action("Record diagnosis, work and parts",e->recordWork());
        JButton assign=action("Assign technician",e->assign());
        JButton estimate=action("Prepare estimate and options",e->estimate());
        JButton finish=action("Set final cost and ready state",e->finish());
        JButton payment=action("Record payment and receipt",e->payment());
        JButton refund=action("Refund payment (Admin)",e->refund()); refund.setVisible("ADMIN".equals(session.role()));
        JButton pickup=action("Confirm device pickup",e->pickup());
        JButton photos=action("View repair photos",e->viewPhotos());
        JButton reviews=action("Estimate review queue (Admin)",e->reviewQueue()); reviews.setVisible("ADMIN".equals(session.role()));
        actions.add(assign); actions.add(estimate); actions.add(work); actions.add(finish); actions.add(payment); actions.add(refund); actions.add(photos); actions.add(reviews); actions.add(pickup);
        repairs.addActionListener(e->updateActionAvailability());
        JPanel workflow=new JPanel(new BorderLayout(0,16)); workflow.setOpaque(false);
        workflow.add(requestPicker,BorderLayout.NORTH); workflow.add(actions,BorderLayout.CENTER);
        add(CrsmUi.section("Select a request and choose an action","Available actions follow the selected repair and your account permissions.",workflow),BorderLayout.CENTER);
        reload();
    }

    private void reload() {
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return service.repairs(session); }
            protected void done() { try { repairs.removeAllItems(); for(Map<String,Object> row:get()) { int id=Integer.parseInt(String.valueOf(row.get("repair_request_id"))); Object related=row.get("computers"); Map<?,?> device=related instanceof Map<?,?> map?map:Map.of(); repairs.addItem(new RepairOption(id,str(row.get("repair_status"))+" · "+str(device.get("brand"))+" "+str(device.get("model")))); } updateActionAvailability(); } catch(Exception ex) { updateActionAvailability(); error(ex); } }
        }.execute();
    }

    private JButton action(String label, java.awt.event.ActionListener listener) {
        JButton button=CrsmUi.ghost(label);
        button.addActionListener(listener);
        repairActions.add(button);
        return button;
    }

    private void updateActionAvailability() {
        boolean selected=repairs.getSelectedItem()!=null;
        for(JButton button:repairActions) {
            boolean adminOnly = button.getText().startsWith("Refund") || button.getText().startsWith("Estimate review");
            boolean roleAllowed = !adminOnly || "ADMIN".equals(session.role());
            button.setEnabled(selected && roleAllowed);
            button.setToolTipText(!selected ? "Refresh repairs and select a request first."
                    : !roleAllowed ? "This action requires an Admin account." : null);
        }
    }

    private int selected() { RepairOption item=(RepairOption)repairs.getSelectedItem(); if(item==null) throw new IllegalStateException("Select a repair request first."); return item.id(); }

    private void recordWork() {
        JTextField diagnosis=new JTextField(), verified=new JTextField(), description=new JTextField(), parts=new JTextField(), cost=new JTextField("0.00");
        JPanel form=form("Verified hardware specifications",verified,"Diagnosis",diagnosis,"Work performed",description,"Parts used",parts,"Work/parts cost",cost);
        if(JOptionPane.showConfirmDialog(this,form,"Record work",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        int id=selected();
        run(()->{ service.verifyDeviceSpecification(session,id,verified.getText()); service.recordWork(session,id,diagnosis.getText(),description.getText(),parts.getText(),cost.getText()); },"Verified device details and work history saved.");
    }

    private void assign() {
        String input=JOptionPane.showInputDialog(this,"Technician ID (see Technicians list):","Assign technician",JOptionPane.QUESTION_MESSAGE);
        if(input==null)return;
        try { int tech=Integer.parseInt(input.trim()); int id=selected(); run(()->service.assignTechnician(session,id,tech),"Technician assigned."); }
        catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Enter a numeric technician ID.");}
    }

    private void estimate() {
        JTextField base=new JTextField(); JTextArea options=new JTextArea(6,32);
        JPanel form=new JPanel(new BorderLayout(8,8));
        JPanel top=new JPanel(new BorderLayout(8,8));top.add(new JLabel("Base repair amount"),BorderLayout.WEST);top.add(base,BorderLayout.CENTER);
        form.add(top,BorderLayout.NORTH);form.add(new JLabel("Optional items, one per line: name|price|description"),BorderLayout.CENTER);form.add(new javax.swing.JScrollPane(options),BorderLayout.SOUTH);
        if(JOptionPane.showConfirmDialog(this,form,"Prepare customer estimate",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        List<Map<String,Object>> items=new java.util.ArrayList<>();
        for(String line:options.getText().split("\\R")) {
            if(line.isBlank())continue; String[] parts=line.split("\\|",3);
            if(parts.length<2){JOptionPane.showMessageDialog(this,"Use name|price|description for each option.");return;}
            try { Map<String,Object> item=new java.util.LinkedHashMap<>();item.put("name",parts[0].trim());item.put("price",new java.math.BigDecimal(parts[1].trim()));item.put("description",parts.length>2?parts[2].trim():"");items.add(item); }
            catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Option prices must be numbers.");return;}
        }
        int id=selected();
        run(()->service.saveEstimate(session,id,base.getText(),items),"Estimate sent to the Customer for a decision.");
    }

    private void finish() {
        JTextField amount=new JTextField();
        if(JOptionPane.showConfirmDialog(this,form("Final total cost",amount),"Finish repair",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        int id=selected();
        run(()->service.finishRepair(session,id,amount.getText()),"Final cost saved. The repair is ready for pickup when the balance is paid.");
    }

    private void payment() {
        JTextField amount=new JTextField(); JTextField method=new JTextField("Cash");
        if(JOptionPane.showConfirmDialog(this,form("Amount",amount,"Method",method),"Record payment",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        int id=selected();
        new SwingWorker<String,Void>() { protected String doInBackground() throws Exception { return service.recordPayment(session,id,amount.getText(),method.getText()); }
            protected void done() { try { JOptionPane.showMessageDialog(RepairOperationsPage.this,"Payment saved. Receipt: "+get(),"Receipt",JOptionPane.INFORMATION_MESSAGE); reload(); } catch(Exception ex){error(ex);} }
        }.execute();
    }

    private void refund() {
        int repairId=selected();
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return service.payments(session,repairId); }
            protected void done() {
                try {
                    List<Map<String,Object>> rows=get();
                    if(rows.isEmpty()){JOptionPane.showMessageDialog(RepairOperationsPage.this,"This repair has no payments.");return;}
                    JComboBox<PaymentChoice> options=new JComboBox<>();
                    for(Map<String,Object> row:rows) options.addItem(new PaymentChoice(Integer.parseInt(String.valueOf(row.get("payment_id"))),"#"+row.get("payment_id")+" · "+row.get("payment_date")+" · "+row.get("amount_paid")+" paid, "+row.get("refunded_amount")+" refunded"));
                    if(JOptionPane.showConfirmDialog(RepairOperationsPage.this,options,"Choose payment to refund",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
                    PaymentChoice choice=(PaymentChoice)options.getSelectedItem(); if(choice==null)return;
                    JTextField amount=new JTextField(), reason=new JTextField();
                    if(JOptionPane.showConfirmDialog(RepairOperationsPage.this,form("Refund amount",amount,"Reason",reason),"Refund payment",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
                    new SwingWorker<Void,Void>() { protected Void doInBackground() throws Exception { service.refundPayment(session,choice.id(),amount.getText(),reason.getText());return null; }
                        protected void done(){try{get();JOptionPane.showMessageDialog(RepairOperationsPage.this,"Refund recorded.");reload();}catch(Exception ex){error(ex);}}
                    }.execute();
                } catch(Exception ex) { error(ex); }
            }
        }.execute();
    }

    private void pickup() { int id=selected(); run(()->service.confirmPickup(session,id),"Pickup confirmation saved."); }

    private void viewPhotos() {
        int repairId=selected();
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return service.repairPhotos(session,repairId); }
            protected void done() {
                try {
                    List<Map<String,Object>> photos=get();
                    if(photos.isEmpty()){JOptionPane.showMessageDialog(RepairOperationsPage.this,"No photos are attached to this repair.");return;}
                    JComboBox<String> paths=new JComboBox<>(); for(Map<String,Object> photo:photos)paths.addItem(String.valueOf(photo.get("object_path")));
                    if(JOptionPane.showConfirmDialog(RepairOperationsPage.this,paths,"Choose repair photo",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
                    String path=String.valueOf(paths.getSelectedItem());
                    new SwingWorker<byte[],Void>() { protected byte[] doInBackground() throws Exception { return new SupabaseStorageService().download(session,"crsm-repair-photos",path); }
                        protected void done(){try{byte[] data=get();java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));if(image==null)throw new IllegalStateException("Image data is invalid.");JLabel view=new JLabel(new javax.swing.ImageIcon(image.getScaledInstance(720,-1,java.awt.Image.SCALE_SMOOTH)));JOptionPane.showMessageDialog(RepairOperationsPage.this,new javax.swing.JScrollPane(view),"Repair photo",JOptionPane.PLAIN_MESSAGE);}catch(Exception ex){error(ex);}}
                    }.execute();
                } catch(Exception ex){error(ex);}
            }
        }.execute();
    }

    private void reviewQueue() {
        new SwingWorker<List<Map<String,Object>>,Void>() {
            protected List<Map<String,Object>> doInBackground() throws Exception { return service.pendingEstimateReviews(session); }
            protected void done() {
                try {
                    List<Map<String,Object>> pending=get(); if(pending.isEmpty()){JOptionPane.showMessageDialog(RepairOperationsPage.this,"No estimate reviews are pending.");return;}
                    JComboBox<ReviewChoice> choices=new JComboBox<>();
                    for(Map<String,Object> row:pending) choices.addItem(new ReviewChoice(Long.parseLong(String.valueOf(row.get("review_id"))),"#"+row.get("review_id")+" · repair #"+row.get("repair_request_id")+" · "+row.get("customer_note")));
                    if(JOptionPane.showConfirmDialog(RepairOperationsPage.this,choices,"Pending estimate reviews",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
                    ReviewChoice choice=(ReviewChoice)choices.getSelectedItem(); if(choice==null)return;
                    String[] outcomes={"REQUOTE","NO_CHANGE","CLOSE"}; String outcome=(String)JOptionPane.showInputDialog(RepairOperationsPage.this,"Choose a review outcome:","Resolve estimate review",JOptionPane.QUESTION_MESSAGE,null,outcomes,outcomes[0]); if(outcome==null)return;
                    String note=JOptionPane.showInputDialog(RepairOperationsPage.this,"Admin decision and rationale:","Review note",JOptionPane.QUESTION_MESSAGE); if(note==null||note.isBlank())return;
                    RepairOperationsPage.this.run(()->service.resolveEstimateReview(session,choice.id(),outcome,note.trim()),"Estimate review resolved.");
                } catch(Exception ex){error(ex);}
            }
        }.execute();
    }
    private void run(Work task,String success) { new SwingWorker<Void,Void>() { protected Void doInBackground() throws Exception { task.run(); return null; } protected void done(){try{get();JOptionPane.showMessageDialog(RepairOperationsPage.this,success);reload();}catch(Exception ex){error(ex);}}}.execute(); }
    private JPanel form(Object... pairs) { JPanel p=new JPanel(new GridLayout(0,2,8,8)); for(int i=0;i<pairs.length;i+=2){p.add(new JLabel((String)pairs[i]));p.add((JTextField)pairs[i+1]);} return p; }
    private void error(Exception ex) { Throwable t=ex.getCause()==null?ex:ex.getCause(); JOptionPane.showMessageDialog(this,t.getMessage(),"Repair operation failed",JOptionPane.ERROR_MESSAGE); }
    private static String str(Object x){return x==null?"—":x.toString();}
    private record RepairOption(int id,String label){@Override public String toString(){return "#"+id+" · "+label;}}
    private record PaymentChoice(int id,String label){@Override public String toString(){return label;}}
    private record ReviewChoice(long id,String label){@Override public String toString(){return label;}}
    @FunctionalInterface private interface Work { void run() throws Exception; }
}
