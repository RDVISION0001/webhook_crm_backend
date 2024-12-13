package com.crm.rdvision.repository;

import com.crm.rdvision.entity.User;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepo extends JpaRepository<User,Integer> {
    Optional<User> findByUserId(Integer userId);
    List<User> findByUserStatus(Character userStatus);

    Optional<User> findByEmail(String email);
    List<User> findByRoleId(Integer roleId);
    @Query("SELECT u FROM User u WHERE u.roleId = :roleId AND u.userStatus != 'F'")
    List<User> findByRoleIdAndActive(@Param("roleId") int roleId);

    List<User> findByTeamIdAndOnBreakIsFalseAndRoleId(int teamId, int roleId);
    List<User> findByTeamIdAndRoleId(int teamId,int roleId);



}
