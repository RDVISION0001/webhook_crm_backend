package com.crm.rdvision.repository;

import com.crm.rdvision.dto.TicketStatusHistoryDto;
import com.crm.rdvision.entity.TicketStatusUpdateHistory;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TicketUpdateHistoryRepo extends JpaRepository<TicketStatusUpdateHistory,Long> {
    @Query("SELECT new com.crm.rdvision.dto.TicketStatusHistoryDto(t.updatedBy, t.status, t.updateDate, " +
            "t.ticketIdWhichUpdating, t.comment, t.userName, t.recordingFile) " +
            "FROM TicketStatusUpdateHistory t WHERE t.ticketIdWhichUpdating = :ticketId")
    List<TicketStatusHistoryDto> findByTicketIdWhichUpdating(@Param("ticketId") String ticketId);

    List<TicketStatusUpdateHistory> findByUpdatedByAndUpdateDate(int id, LocalDate localDate);

    @Query("SELECT u.userName AS userName, COUNT(u.status) AS count " +
            "FROM TicketStatusUpdateHistory u " +
            "WHERE u.status = 'sale' AND u.updateDate BETWEEN :startDate AND :endDate " +
            "GROUP BY u.userName")
    List<Map<String, String>> findUserSaleCount(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
    SELECT 
        e.userName AS userName,
        CASE 
            WHEN e.status LIKE %:emailText% THEN 'TotalEmail'
            WHEN e.status LIKE %:saleText% THEN 'TotalSale'
            WHEN e.status LIKE %:quotationText% THEN 'TotalQuotation'
            WHEN e.status LIKE %:paylinkText% THEN 'TotalPaymentLink'
            ELSE 'Other'
        END AS statusType,
        COUNT(e) AS count
    FROM TicketStatusUpdateHistory e
    WHERE 
        (e.updateDate BETWEEN :startDate AND :endDate)
        AND (
            e.status LIKE %:emailText% 
            OR e.status LIKE %:saleText%
            OR e.status LIKE %:quotationText%
            OR e.status LIKE %:paylinkText%
        )
    GROUP BY e.userName, statusType
    """)
    List<Object[]> countByStatusTypesForAllUsers(
            @Param("emailText") String emailText,
            @Param("saleText") String saleText,
            @Param("quotationText") String quotationText,
            @Param("paylinkText") String paylinkText,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
