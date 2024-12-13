package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByInvoiceId(String invoiceId);
}
