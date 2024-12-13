package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Department;
import com.crm.rdvision.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepo extends JpaRepository<Department,Integer> {

}
