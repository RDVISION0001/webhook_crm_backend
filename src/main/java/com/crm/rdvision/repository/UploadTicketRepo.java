package com.crm.rdvision.repository;

import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.UploadTicket;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface UploadTicketRepo extends JpaRepository<UploadTicket,Long> {
    UploadTicket findByUniqueQueryId(String uniqueQueryId);
    Page<UploadTicket> findByUploadDateAndTicketstatusAndAssigntouser(LocalDate date,String ticketStatus, Integer userId, Pageable pageable);
    Page<UploadTicket> findByUploadDateAndTicketstatusAndAssigntoteam(LocalDate date,String ticketStatus, Integer teamId, Pageable pageable);
    Page<UploadTicket> findByUploadDateAndAssigntouser(LocalDate date,Integer userId, Pageable pageable);
    Page<UploadTicket> findByUploadDateAndTicketstatus(LocalDate date,String ticketStatus, Pageable pageable);
    Page<UploadTicket> findByUploadDateAndAssigntouserIsNull(LocalDate date,Pageable pageable);

    @Query(value = "SELECT DATE_FORMAT(t.upload_date, '%Y-%m-%d'), COUNT(*) FROM upload_ticket t GROUP BY t.upload_date", nativeQuery = true)
    List<Object[]> findDateAndTicketCount();

    @Query(value = "SELECT DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d'), COUNT(*) FROM upload_ticket t WHERE t.ticketstatus = 'Follow' GROUP BY DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d')", nativeQuery = true)
    List<Object[]> findDateAndTicketCountByStatusFollow();

    List<UploadTicket> findByUploadDate(LocalDate date);


    @Query("SELECT " +
            "COUNT(t) AS totalAssignTickets, " +
            "COUNT(CASE WHEN t.ticketstatus = 'New' THEN 1 END) AS totalNewTickets, " +
            "COUNT(CASE WHEN t.ticketstatus <> 'New' THEN 1 END) AS totalFollowupsTickets  " +
            "FROM UploadTicket t " +
            "WHERE t.assigntouser = :userId")
    Map<String, Long> countTicketStatisticsByUserId(@Param("userId") int userId);

    @Query(value = "SELECT DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d') AS date, COUNT(*) AS count " +
            "FROM upload_ticket t " +
            "WHERE t.ticketstatus = 'Follow' AND t.assigntouser = :userId " +
            "GROUP BY DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d')",
            nativeQuery = true)
    List<Object[]> findDateAndTicketCountByStatusFollowAssigntouser(int userId);

    @Query(value = "SELECT DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS count, " +
            "GROUP_CONCAT(CONCAT(t.comment, ' (', t.first_name, ')') SEPARATOR ', ') AS comments_with_names " +
            "FROM upload_ticket t " +
            "WHERE t.ticketstatus = 'Follow' AND t.assigntouser = :userId " +
            "GROUP BY DATE_FORMAT(t.follow_up_date_time, '%Y-%m-%d')",
            nativeQuery = true)
    List<Object[]> findDateAndTicketCountAndCommentsWithSenderNameByStatusFollowAndAssigntouser(int userId);


    @Query("SELECT t FROM UploadTicket t WHERE t.assigntouser = :userId AND t.ticketstatus IN ('Follow', 'Call_Back') AND FUNCTION('DATE', t.followUpDateTime) = :followupDate")
    List<UploadTicket> findByAssigntouserAndTicketstatusInAndFollowUpDate(
            @Param("userId") int userId,
            @Param("followupDate") LocalDate followupDate
    );


    @Query("SELECT a.assignDate as date, " +
            "COUNT(a) as assigncount, " +
            "SUM(CASE WHEN a.ticketstatus = 'sale' THEN 1 ELSE 0 END) as assignedWithStatusSale, " +
            "SUM(CASE WHEN a.ticketstatus NOT IN ('New', 'Sale') THEN 1 ELSE 0 END) as assignedWithStatusNotIn " +
            "FROM UploadTicket a " +
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
            "FROM UploadTicket a " +
            "WHERE a.assignDate BETWEEN :fromDate AND :toDate " +
            "AND a.assigntouser = :userId " +
            "GROUP BY FUNCTION('MONTH', a.assignDate), FUNCTION('YEAR', a.assignDate)")
    List<Map<String, String>> countByAssignMonthAndYear(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("userId") int userId);

    List<UploadTicket> findByAssigntouserAndTicketstatusIn(int userId, List<String> statusList);
    List<UploadTicket> findByTicketstatusIn(List<String> statusList);

    int countByAssigntouserAndAssignDate(Integer assigntouser, LocalDate assignDate);

    Page<UploadTicket> findByAssigntouserIsNull(Pageable pageable);
    Page<UploadTicket> findByAssigntouser(Pageable pageable,int userId);

    @Query("SELECT t FROM UploadTicket t WHERE t.assigntouser = :userId")
    List<UploadTicket> findByUser(@Param("userId") int userId);


    @Query("SELECT t.ticketstatus, COUNT(t) FROM UploadTicket t WHERE t.assigntouser = :userId GROUP BY t.ticketstatus")
    List<Object[]> countTicketsByUserGroupByStatus(@Param("userId") int userId);

    @Query("SELECT t.ticketstatus, COUNT(t) FROM UploadTicket t GROUP BY t.ticketstatus")
    List<Object[]> countTicketsGroupByStatus();

    UploadTicket findByTrackingNumber(String trackingNumber);
    int countByAssigntouserIsNull();

    @Query("SELECT te.assigntouser as userId, te.ticketstatus as status, COUNT(te) as count " +
            "FROM UploadTicket te " +
            "WHERE te.assignDate BETWEEN :startDate AND :endDate " +
            "GROUP BY te.assigntouser, te.ticketstatus")
    List<Object[]> findStatusCountsByUserAndDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Query for overall status counts
    @Query("SELECT t.ticketstatus, COUNT(t) " +
            "FROM UploadTicket t " +
            "WHERE t.assignDate = :date " +
            "GROUP BY t.ticketstatus")
    List<Object[]> findOverallStatusCountsByDate(LocalDate date);

    // Query for user-specific status counts
    @Query("SELECT t.assigntouser, t.ticketstatus, COUNT(t) " +
            "FROM UploadTicket t " +
            "WHERE t.assignDate = :date " +
            "GROUP BY t.assigntouser, t.ticketstatus")
    List<Object[]> findUserSpecificStatusCountsByDate(LocalDate date);

    @Query("SELECT t.ticketstatus AS status, COUNT(t) AS ticketCount " +
            "FROM UploadTicket t WHERE t.assigntouser = :userId " +
            "GROUP BY t.ticketstatus")
    List<Map<String, Long>> getStatusCountByUserId(Integer userId);
}
