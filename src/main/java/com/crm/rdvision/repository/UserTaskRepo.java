package com.crm.rdvision.repository;

import com.crm.rdvision.entity.UsersTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface UserTaskRepo extends JpaRepository<UsersTask,Long> {
    UsersTask findByAssignedToRoleIdAndAssignedDate(int id, LocalDate date);
}
