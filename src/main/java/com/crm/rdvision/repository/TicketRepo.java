package com.crm.rdvision.repository;

import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.UploadTicket;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface TicketRepo extends JpaRepository<TicketEntity, Integer> {
    TicketEntity findByUniqueQueryId(String uniqueQueryId);

    @Query("SELECT t FROM TicketEntity t WHERE t.ticketstatus = :ticketstatus AND (t.assigntouser = :userId OR t.assigntouser IS NULL)")
    Page<TicketEntity> findByTicketstatusAndAssigntouserOrNull(
            @Param("ticketstatus") String ticketstatus,
            @Param("userId") Integer userId,
            Pageable pageable
    );
    @Query("SELECT t FROM TicketEntity t WHERE t.ticketstatus = :ticketStatus AND (t.assigntouser = :userId OR t.assigntouser IS NULL)")
    Page<TicketEntity> findByTicketstatusAndAssigntouserOrAssigntouserIsNull(String ticketStatus,Integer userId, Pageable pageable);
    Page<TicketEntity> findByTicketstatusNotInAndAssigntouserOrAssigntouserIsNull(List<String> ticketStatuses, Integer userId, Pageable pageable);
    Page<TicketEntity> findByTicketstatusAndAssigntoteam(String ticketStatus, Integer teamId, Pageable pageable);

    Page<TicketEntity> findByAssigntouser(Integer userId, Pageable pageable);

    @Query("SELECT t FROM TicketEntity t WHERE t.assigntouser = :userId")
    List<TicketEntity> findByUser(@Param("userId") int userId);


    List<TicketEntity> findByAssigntouser(int userId);

    Page<TicketEntity> findByTicketstatus(String ticketStatus, Pageable pageable);


    @Query(value = "SELECT DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM ticket_entity t " +
            "WHERE t.ticketstatus = 'Follow' AND t.assigntouser = :userId " +
            "GROUP BY DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d')",
            nativeQuery = true)
    List<Object[]> findDateAndTicketCountByStatusFollowAssigntouser(int userId);

    @Query(value = "SELECT DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS count, " +
            "GROUP_CONCAT(CONCAT(t.comment, ' (', t.sender_name, ')') SEPARATOR ', ') AS comments_with_names " +
            "FROM ticket_entity t " +
            "WHERE t.ticketstatus = 'Follow' AND t.assigntouser = :userId " +
            "GROUP BY DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d')",
            nativeQuery = true)
    List<Object[]> findDateAndTicketCountAndCommentsWithSenderNameByStatusFollowAndAssigntouser(int userId);


    Page<TicketEntity> findByAssigntouserIsNull(Pageable pageable);

    int countByAssignDateBetweenAndAssigntouser(LocalDate fromDate, LocalDate toDate, int userId);

    @Query("SELECT a.assignDate as date, " +
            "COUNT(a) as assigncount, " +
            "SUM(CASE WHEN a.ticketstatus = 'sale' THEN 1 ELSE 0 END) as assignedWithStatusSale, " +
            "SUM(CASE WHEN a.ticketstatus NOT IN ('New', 'Sale') THEN 1 ELSE 0 END) as assignedWithStatusNotIn " +
            "FROM TicketEntity a " +
            "WHERE a.assignDate BETWEEN :fromDate AND :toDate " +
            "AND a.assigntouser = :userId " +
            "GROUP BY a.assignDate")
    List<Map<String, String>> countByAssignDatesBetweenAndAssigntouser(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("userId") int userId);





    @Query("SELECT FUNCTION('MONTH', a.assignDate) as month, " +
            "FUNCTION('YEAR', a.assignDate) as year, " +
            "COUNT(a) as assigncount, " +
            "SUM(CASE WHEN a.ticketstatus = 'sale' THEN 1 ELSE 0 END) as assignedWithStatusSale, " +
            "SUM(CASE WHEN a.ticketstatus NOT IN ('New', 'Sale') THEN 1 ELSE 0 END) as assignedWithStatusNotIn " +
            "FROM TicketEntity a " +
            "WHERE a.assignDate BETWEEN :fromDate AND :toDate " +
            "AND a.assigntouser = :userId " +
            "GROUP BY FUNCTION('MONTH', a.assignDate), FUNCTION('YEAR', a.assignDate)")
    List<Map<String, String>> countByAssignMonthAndYear(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("userId") int userId);

    @Query("SELECT " +
            "COUNT(t) AS totalAssignTickets, " +
            "COUNT(CASE WHEN t.ticketstatus = 'New' THEN 1 END) AS totalNewTickets, " +
            "COUNT(CASE WHEN t.ticketstatus <> 'New' THEN 1 END) AS totalFollowupsTickets " +
            "FROM TicketEntity t " +
            "WHERE t.assigntouser = :userId")
    Map<String, Long> countTicketStatisticsByUserId(@Param("userId") int userId);


    @Query("SELECT t FROM TicketEntity t WHERE t.assigntouser = :userId AND t.ticketstatus IN ('Follow', 'Call_Back') AND FUNCTION('DATE', t.followUpDateTime) = :followupDate")
    List<TicketEntity> findByAssigntouserAndTicketstatusInAndFollowUpDate(
            @Param("userId") int userId,
            @Param("followupDate") LocalDate followupDate
    );

    List<TicketEntity> findByAssigntouserAndTicketstatusIn(int userId, List<String> statusList);
    List<TicketEntity> findByTicketstatusIn(List<String> statusList);

//    @Query("SELECT FUNCTION('YEAR', a.loginDate) as year, FUNCTION('MONTH', a.loginDate) as month, " +
//            "SUM(a.actualWorkingSeconds) as totalWorkTime, SUM(a.totalBreakInSec) as totalBreakTime " +
//            "FROM EmployeeAttendance a " +
//            "WHERE a.loginDate BETWEEN :fromDate AND :toDate " +
//            "AND a.employeeId = :userId " +
//            "GROUP BY FUNCTION('YEAR', a.loginDate), FUNCTION('MONTH', a.loginDate)")
//    List<Map<String,String>> findWorkAndBreakTimeByLoginDateMonthBetweenAndEmployeeId(
//            @Param("fromDate") LocalDate fromDate,
//            @Param("toDate") LocalDate toDate,
//            @Param("userId") int userId);


    int countByAssigntouserAndAssignDate(Integer assigntouser, LocalDate assignDate);

    @Query("SELECT t.ticketstatus, COUNT(t) FROM TicketEntity t GROUP BY t.ticketstatus")
    List<Object[]> countTicketsByStatus();

    // Method to count total tickets
    @Query("SELECT COUNT(t) FROM TicketEntity t")
    long countTotalTickets();

    // Method to count tickets grouped by country
    @Query("SELECT t.senderCountryIso, COUNT(t) FROM TicketEntity t GROUP BY t.senderCountryIso")
    List<Object[]> countTicketsByCountry();

    // Method to count tickets by country where ticketstatus is 'Sale'
    @Query("SELECT t.senderCountryIso, COUNT(t) FROM TicketEntity t WHERE t.ticketstatus = 'Sale' GROUP BY t.senderCountryIso")
    List<Object[]> countTicketsByCountryWithSaleStatus();


    @Query("SELECT t.ticketstatus, COUNT(t) FROM TicketEntity t WHERE DATE(t.queryTime) BETWEEN :startDate AND :endDate GROUP BY t.ticketstatus")
    List<Object[]> countTicketsByStatusWithinDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    // Method to count total tickets
    @Query("SELECT COUNT(t) FROM TicketEntity t WHERE DATE(t.queryTime) BETWEEN :startDate AND :endDate")
    long countTotalTicketsWithinDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    // Method to count tickets grouped by country
    @Query("SELECT t.senderCountryIso, COUNT(t) FROM TicketEntity t WHERE DATE(t.queryTime) BETWEEN :startDate AND :endDate GROUP BY t.senderCountryIso")
    List<Object[]> countTicketsByCountryWithinDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    // Method to count tickets by country where ticketstatus is 'Sale'
    @Query("SELECT t.senderCountryIso, COUNT(t) FROM TicketEntity t WHERE t.ticketstatus = 'Sale' AND DATE(t.queryTime) BETWEEN :startDate AND :endDate GROUP BY t.senderCountryIso")
    List<Object[]> countTicketsByCountryWithSaleStatusWithinDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    @Query("SELECT t.ticketstatus, COUNT(t) FROM TicketEntity t WHERE t.assigntouser = :userId GROUP BY t.ticketstatus")
    List<Object[]> countTicketsByUserGroupByStatus(@Param("userId") int userId);

    TicketEntity findByTrackingNumber(String trackingNumber);

    //tickets one by one
    @Query("SELECT t FROM TicketEntity t WHERE t.ticketstatus = :status ORDER BY CAST(t.queryTime AS java.time.LocalDateTime) DESC, t.id ASC")
    List<TicketEntity> findByStatusOrdered(String status);

    @Query("SELECT t FROM TicketEntity t WHERE t.ticketstatus NOT IN ('New', 'Sale') ORDER BY CAST(t.queryTime AS java.time.LocalDateTime) DESC, t.id ASC")
    List<TicketEntity> findAllByStatusExcludingNewAndSale();

    @Query("SELECT COALESCE(CAST(t.assigntouser AS string), 'NotAssignedTicket') AS assignedUser, COUNT(t) AS ticketCount " +
            "FROM TicketEntity t GROUP BY t.assigntouser")
    List<Object[]> getTicketsCountByUser();

    int countByAssigntouserIsNull();

    @Query("SELECT te.assigntouser as userId, te.ticketstatus as status, COUNT(te) as count " +
            "FROM TicketEntity te " +
            "WHERE te.assignDate BETWEEN :startDate AND :endDate " +
            "GROUP BY te.assigntouser, te.ticketstatus")
    List<Object[]> findStatusCountsByUserAndDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Query for overall status counts
    @Query("SELECT t.ticketstatus, COUNT(t) " +
            "FROM TicketEntity t " +
            "WHERE t.assignDate = :date " +
            "GROUP BY t.ticketstatus")
    List<Object[]> findOverallStatusCountsByDate(LocalDate date);

    // Query for user-specific status counts
    @Query("SELECT t.assigntouser, t.ticketstatus, COUNT(t) " +
            "FROM TicketEntity t " +
            "WHERE t.assignDate = :date " +
            "GROUP BY t.assigntouser, t.ticketstatus")
    List<Object[]> findUserSpecificStatusCountsByDate(LocalDate date);

    List<TicketEntity> findAllByTicketstatusOrderByQueryTimeDesc(String status);

    List<TicketEntity> findAllByTicketstatusAndAssigntouserOrderByQueryTimeDesc(String status, int assigntouser);


    @Query("SELECT t.ticketstatus AS status, COUNT(t) AS ticketCount " +
            "FROM TicketEntity t WHERE t.assigntouser = :userId " +
            "GROUP BY t.ticketstatus")
    List<Map<String, Long>> getStatusCountByUserId(Integer userId);

    // Custom query to get all distinct country ISO values
    @Query("SELECT DISTINCT t.senderCountryIso FROM TicketEntity t")
    List<String> findAllDistinctCountryIso();
}
