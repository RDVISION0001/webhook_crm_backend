package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Department;
import com.crm.rdvision.entity.Team;
import com.crm.rdvision.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepo extends JpaRepository<Team,Integer> {

}
