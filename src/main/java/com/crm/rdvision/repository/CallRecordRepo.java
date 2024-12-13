package com.crm.rdvision.repository;

import com.crm.rdvision.entity.CallRecords;
import com.crm.rdvision.entity.User;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CallRecordRepo extends JpaRepository<CallRecords,Long> {

    int countByUserAndCallDate(User user, LocalDate callDate);

    @Query("SELECT CAST(c.callDate AS LocalDate), COUNT(c) " +
            "FROM CallRecords c WHERE c.user = :user GROUP BY CAST(c.callDate AS LocalDate)")
    List<Object[]> countCallsByUserGroupedByDate(@Param("user") User user);


    @Query("SELECT u.firstName, COUNT(c) " +
            "FROM CallRecords c JOIN c.user u " +
            "GROUP BY u.firstName")
    List<Object[]> countAllCallsGroupedByUserFirstName();

    @Query("SELECT u.firstName, COUNT(c) " +
            "FROM CallRecords c JOIN c.user u " +
            "WHERE c.callDate = :callDate " +
            "GROUP BY u.firstName")
    List<Object[]> countAllCallsGroupedByUserFirstName(@Param("callDate") LocalDate callDate);

    @Query("SELECT u.firstName, " +
            "CASE FUNCTION('MONTH', c.callDate) " +
            "WHEN 1 THEN 'January' " +
            "WHEN 2 THEN 'February' " +
            "WHEN 3 THEN 'March' " +
            "WHEN 4 THEN 'April' " +
            "WHEN 5 THEN 'May' " +
            "WHEN 6 THEN 'June' " +
            "WHEN 7 THEN 'July' " +
            "WHEN 8 THEN 'August' " +
            "WHEN 9 THEN 'September' " +
            "WHEN 10 THEN 'October' " +
            "WHEN 11 THEN 'November' " +
            "WHEN 12 THEN 'December' " +
            "END AS monthName, " +
            "COUNT(c) " +
            "FROM CallRecords c JOIN c.user u " +
            "GROUP BY u.firstName, FUNCTION('MONTH', c.callDate)")
    List<Object[]> countAllCallsGroupedByUserFirstNameAndMonth();


    @Modifying
    @Transactional
    @Query("DELETE FROM CallRecords c WHERE c.user.userId = :userId")
    void deleteAllByUser(int userId);

}
