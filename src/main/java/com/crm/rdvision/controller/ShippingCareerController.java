package com.crm.rdvision.controller;

import com.crm.rdvision.entity.ShippingCareer;
import com.crm.rdvision.repository.ShippingCareerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/career")
public class ShippingCareerController {

    @Autowired
    ShippingCareerRepo shippingCareerRepo;



    @PostMapping("/add")
    public ResponseEntity<ShippingCareer> addShippingCareer(@RequestBody ShippingCareer shippingCareer){
        return new ResponseEntity<>(shippingCareerRepo.save(shippingCareer), HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{careerId}")
    public void deleteById(@PathVariable int careerId){
        shippingCareerRepo.deleteById(careerId);
    }

    @GetMapping("/gelAll")
    public List<ShippingCareer> getAllShippingCareers(){
        return shippingCareerRepo.findAll();
    }
}
