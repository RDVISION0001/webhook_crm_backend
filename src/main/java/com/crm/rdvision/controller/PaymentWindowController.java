package com.crm.rdvision.controller;

import com.crm.rdvision.entity.PaymentWindow;
import com.crm.rdvision.repository.PaymentWindowRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paymentwindow")
public class PaymentWindowController {

    @Autowired
    PaymentWindowRepo paymentWindowRepo;


    @PostMapping("/add")
    public ResponseEntity<PaymentWindow> addNewPaymentWindow(@RequestBody PaymentWindow paymentWindow){
        return new ResponseEntity<>(paymentWindowRepo.save(paymentWindow), HttpStatus.CREATED);
    }
    @GetMapping("/getAll")
    public List<PaymentWindow> getAllPaymentWindows(){
        return paymentWindowRepo.findAll();
    }

    @DeleteMapping("/delete/{id}")
    public void deletePaymentWindow(@PathVariable int id){
        paymentWindowRepo.deleteById(id);
    }
}
