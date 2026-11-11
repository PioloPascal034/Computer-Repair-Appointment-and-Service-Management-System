package crsm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Text counterpart of the on-screen receipt; each receipt represents one received payment. */
public final class PaymentReceipt {
    private PaymentReceipt() {}
    public static String text(String customerName,Map<String,Object> bill,Map<String,Object> payment){
        BigDecimal gross=amount(payment.get("amount_paid")),refunded=amount(payment.get("refunded_amount"));
        return "CRSM | PAYMENT RECEIPT\n"+
                "Receipt: "+value(payment.get("receipt_number"))+"\n"+
                "Repair: #"+value(bill.get("repair_request_id"))+"\n"+
                "Customer: "+customerName+"\n"+
                "Payment date: "+value(payment.get("payment_date"))+"\n"+
                "Payment method: "+value(payment.get("payment_method"))+"\n\n"+
                "Amount received: PHP "+money(gross)+"\n"+
                "Refunded from this payment: PHP "+money(refunded)+"\n"+
                "Net payment: PHP "+money(gross.subtract(refunded))+"\n\n"+
                "Current bill summary (at time of viewing)\n"+
                "Final repair cost: "+(bill.get("final_cost")==null?"Not finalized":"PHP "+money(amount(bill.get("final_cost"))))+"\n"+
                "Total net paid: PHP "+money(amount(bill.get("net_paid")))+"\n"+
                "Balance due: "+(bill.get("balance_due")==null?"Not finalized":"PHP "+money(amount(bill.get("balance_due"))))+"\n"+
                "Repair status: "+value(bill.get("repair_status"))+"\n\n"+
                "This receipt records one payment. It does not confirm full settlement unless the balance is zero.\n";
    }
    private static BigDecimal amount(Object value){return value==null?BigDecimal.ZERO:new BigDecimal(value.toString());}
    private static String money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP).toPlainString();}
    private static String value(Object value){return value==null?"Not available":value.toString();}
}
