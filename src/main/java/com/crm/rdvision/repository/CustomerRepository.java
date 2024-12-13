package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Customers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customers,Long> {

    Optional<Customers> findByCustomerEmailOrCustomerMobile(String customerEmail, String customerMobile);

}
