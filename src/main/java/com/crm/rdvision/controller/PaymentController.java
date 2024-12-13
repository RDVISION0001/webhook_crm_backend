package com.crm.rdvision.controller;

import com.crm.rdvision.entity.Payment;
import com.crm.rdvision.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    PaymentRepository paymentRepository;




    @PutMapping("/update_payment")
    public Payment updateAndReturnPayment(@RequestBody Payment payment){
        Payment payment1=paymentRepository.findById(payment.getId()).get();
        payment1.setAmount(payment.getAmount());
        payment1.setCurrency(payment.getCurrency());
        payment1.setPaymentWindow(payment.getPaymentWindow());
        payment1.setPaymentIntentId(payment.getPaymentIntentId());
        payment1.setLastUpdateDateTime(LocalDateTime.now());
        return paymentRepository.save(payment1);
    }
}
