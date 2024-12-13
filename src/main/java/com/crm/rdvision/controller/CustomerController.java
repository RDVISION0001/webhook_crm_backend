package com.crm.rdvision.controller;

import com.crm.rdvision.entity.Customers;
import com.crm.rdvision.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    @Autowired
    CustomerRepository customerRepository;



    @GetMapping("/getAll")
    public List<Customers> getAllCustomers(){
        return customerRepository.findAll();
    }
}
