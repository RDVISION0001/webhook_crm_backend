package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Address;
import com.crm.rdvision.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepo extends JpaRepository<Address,Integer> {
    Optional<Address> findByTicketId(String ticketId);


}
