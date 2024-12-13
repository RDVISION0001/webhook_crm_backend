package com.crm.rdvision.repository;

import com.crm.rdvision.entity.EmployeeAttendance;
import com.crm.rdvision.entity.User;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeAttendanceRepo extends JpaRepository<EmployeeAttendance,Long> {
    EmployeeAttendance findByAttendanceId(long id);
    List<EmployeeAttendance> findByLoginDateAndEmployeeId(LocalDate loginDate,int employeeId);
    List<EmployeeAttendance> findByLoginDate(LocalDate loginDate);
    List<EmployeeAttendance> findByLoginDateBetweenAndEmployeeId(LocalDate fromDate, LocalDate toDate, int userId);

    @Query("SELECT a.loginDate as date, SUM(a.actualWorkingSeconds) as totalWorkTime, SUM(a.totalBreakInSec) as totalBreakTime " +
            "FROM EmployeeAttendance a " +
            "WHERE a.loginDate BETWEEN :fromDate AND :toDate " +
            "AND a.user = :user " +
            "GROUP BY a.loginDate")
    List<Map<String,String>> findWorkAndBreakTimeByLoginDateBetweenAndUser(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("user") User user);

    @Query("SELECT FUNCTION('YEAR', a.loginDate) as year, FUNCTION('MONTH', a.loginDate) as month, " +
            "SUM(a.actualWorkingSeconds) as totalWorkTime, SUM(a.totalBreakInSec) as totalBreakTime " +
            "FROM EmployeeAttendance a " +
            "WHERE a.loginDate BETWEEN :fromDate AND :toDate " +
            "AND a.user = :user " +
            "GROUP BY FUNCTION('YEAR', a.loginDate), FUNCTION('MONTH', a.loginDate)")
    List<Map<String,String>> findWorkAndBreakTimeByLoginDateMonthBetweenAndUser(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("userId") User user);

    @Query("SELECT CONCAT(u.firstName, ' ', u.lastName) as userName, FUNCTION('YEAR', a.loginDate) as year, FUNCTION('MONTH', a.loginDate) as month, " +
            "SUM(a.actualWorkingSeconds) as totalWorkTime, SUM(a.totalBreakInSec) as totalBreakTime " +
            "FROM EmployeeAttendance a " +
            "JOIN a.user u " +
            "WHERE a.loginDate BETWEEN :fromDate AND :toDate " +
            "AND u.roleId = 4 " +  // Filtering users with roleId = 4
            "GROUP BY u.firstName, u.lastName, FUNCTION('YEAR', a.loginDate), FUNCTION('MONTH', a.loginDate)")
    List<Map<String, String>> findWorkAndBreakTimeByLoginDateMonthBetweenWithUserAndRole(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    List<EmployeeAttendance> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeAttendance c WHERE c.user.userId = :userId")
    void deleteAllByUser(int userId);






}
