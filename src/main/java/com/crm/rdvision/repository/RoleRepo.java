package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Department;
import com.crm.rdvision.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepo extends JpaRepository<Role,Integer> {

}
