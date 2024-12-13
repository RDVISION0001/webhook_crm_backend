package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface InvoiceRepo extends JpaRepository<Invoice,Integer> {
    Invoice findByTicketId(String ticketId);


            @Query("SELECT new map("
            + "COUNT(i) as totalInvoices, "  // Total number of invoices
            + "SUM(CASE WHEN i.inviceStatus = 'Pending' THEN 1 ELSE 0 END) as totalPendingInvoices, "  // Total pending invoices
            + "SUM(CASE WHEN i.inviceStatus = 'Paid' THEN 1 ELSE 0 END) as totalPaidInvoices, "  // Total paid invoices
            + "SUM(CASE WHEN i.inviceStatus = 'Paid' AND i.trackingNumber IS NULL THEN 1 ELSE 0 END) as totalAwaitingPaidInvoices "  // Total awaiting paid invoices
            + ") "
            + "FROM Invoice i")
            Map<String, Long> findNumberOfInvoice();
            Invoice findByTrackingNumber(String trackingNumber);

            Invoice findByInviceId(int invoiceId);

           List<Invoice> findByPaymentStatusIgnoreCaseAndIsVerified(String paymentStatus, boolean isVerified);
           List<Invoice> findByCreatedByUserIdAndPaymentStatusIgnoreCaseAndIsVerified(int createdByUserId,String paymentStatus, boolean isVerified);
           List<Invoice> findByCreatedByUserId(int createdByUserId);

}
