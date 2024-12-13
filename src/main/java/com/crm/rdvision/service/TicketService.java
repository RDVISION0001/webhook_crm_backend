package com.crm.rdvision.service;

import com.crm.rdvision.dto.ActionTicketDto;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.TicketRepo;
import com.crm.rdvision.repository.UploadTicketRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Service
public class TicketService {

    @Autowired
    private TicketRepo ticketRepository;
    @Autowired
    private UploadTicketRepo uploadTicketRepo;
    @Autowired
    private UserRepo userRepo;

    private ConcurrentHashMap<String, Integer> userTicketIndex = new ConcurrentHashMap<>();

    private List<TicketEntity> getTicketsBasedOnStatus(String status) {
        if ("new".equalsIgnoreCase(status)) {
            return ticketRepository.findByStatusOrdered(status);
        } else {
            return ticketRepository.findAllByStatusExcludingNewAndSale();
        }
    }

    public ActionTicketDto getFirstTicket(String status, String userId) {
        List<TicketEntity> tickets = getTicketsBasedOnStatus(status);
        ActionTicketDto actionTicketDto = new ActionTicketDto();

        if (tickets.isEmpty()) {
            actionTicketDto.setTotalTickets(0);
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
            return actionTicketDto;
        }

        userTicketIndex.put(userId, 0);
        actionTicketDto.setTotalTickets(tickets.size());
        actionTicketDto.setCurrentTicketNo(1);
        actionTicketDto.setTicket(tickets.get(0));
        return actionTicketDto;
    }

    public ActionTicketDto getNextTicket(String status, String userId) {
        List<TicketEntity> tickets = getTicketsBasedOnStatus(status);
        ActionTicketDto actionTicketDto = new ActionTicketDto();

        if (tickets.isEmpty()) {
            actionTicketDto.setTotalTickets(0);
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
            return actionTicketDto;
        }

        int currentIndex = userTicketIndex.getOrDefault(userId, -1);
        if (currentIndex < tickets.size() - 1) {
            currentIndex++;
            userTicketIndex.put(userId, currentIndex);
        }

        actionTicketDto.setTotalTickets(tickets.size());
        actionTicketDto.setCurrentTicketNo(currentIndex + 1);
        actionTicketDto.setTicket(tickets.get(currentIndex));
        return actionTicketDto;
    }

    public ActionTicketDto getPreviousTicket(String status, String userId) {
        List<TicketEntity> tickets = getTicketsBasedOnStatus(status);
        ActionTicketDto actionTicketDto = new ActionTicketDto();

        if (tickets.isEmpty()) {
            actionTicketDto.setTotalTickets(0);
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
            return actionTicketDto;
        }

        int currentIndex = userTicketIndex.getOrDefault(userId, 0);
        if (currentIndex > 0) {
            currentIndex--;
            userTicketIndex.put(userId, currentIndex);
        }

        actionTicketDto.setTotalTickets(tickets.size());
        actionTicketDto.setCurrentTicketNo(currentIndex + 1);
        actionTicketDto.setTicket(tickets.get(currentIndex));
        return actionTicketDto;
    }

    public ActionTicketDto getSpecificTicketByNumber(String status, String userId, int ticketNumber) {
        List<TicketEntity> tickets = getTicketsBasedOnStatus(status);
        ActionTicketDto actionTicketDto = new ActionTicketDto();

        if (tickets.isEmpty()) {
            actionTicketDto.setTotalTickets(0);
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
            return actionTicketDto;
        }

        actionTicketDto.setTotalTickets(tickets.size());

        if (ticketNumber >= 1 && ticketNumber <= tickets.size()) {
            int index = ticketNumber - 1;
            userTicketIndex.put(userId, index);

            actionTicketDto.setCurrentTicketNo(ticketNumber);
            actionTicketDto.setTicket(tickets.get(index));
        } else {
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
        }

        return actionTicketDto;
    }

    public ActionTicketDto getSpecificTicketByDetails(String status, String userId, String searchQuery) {
        List<TicketEntity> tickets = getTicketsBasedOnStatus(status);
        ActionTicketDto actionTicketDto = new ActionTicketDto();

        if (tickets.isEmpty()) {
            actionTicketDto.setTotalTickets(0);
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
            return actionTicketDto;
        }

        actionTicketDto.setTotalTickets(tickets.size());

        OptionalInt matchingIndexOpt = IntStream.range(0, tickets.size())
                .filter(i -> {
                    TicketEntity ticket = tickets.get(i);
                    return ticket.getSenderEmail().equalsIgnoreCase(searchQuery) ||
                            ticket.getSenderMobile().equalsIgnoreCase(searchQuery) ||
                            ticket.getSenderName().equalsIgnoreCase(searchQuery);
                })
                .findFirst();

        if (matchingIndexOpt.isPresent()) {
            int matchingIndex = matchingIndexOpt.getAsInt();
            userTicketIndex.put(userId, matchingIndex);

            actionTicketDto.setCurrentTicketNo(matchingIndex + 1);
            actionTicketDto.setTicket(tickets.get(matchingIndex));
        } else {
            actionTicketDto.setCurrentTicketNo(-1);
            actionTicketDto.setTicket(null);
        }

        return actionTicketDto;
    }



    public Map<String, Object> getTodayStatusReport() {
        LocalDate today = LocalDate.now();

        // Fetch overall and user-specific data for Live
        List<Object[]> overallStatsLive = ticketRepository.findOverallStatusCountsByDate(today);
        List<Object[]> userStatsLive = ticketRepository.findUserSpecificStatusCountsByDate(today);

        // Fetch overall and user-specific data for ABC
        List<Object[]> overallStatsAbc = uploadTicketRepo.findOverallStatusCountsByDate(today);
        List<Object[]> userStatsAbc = uploadTicketRepo.findUserSpecificStatusCountsByDate(today);

        // Aggregated overall counts
        Map<String, Integer> overallCounts = new HashMap<>();
        int totalAssignedToday = 0;

        // Process overall data from Live
        for (Object[] record : overallStatsLive) {
            String status = (String) record[0];
            Long count = (Long) record[1];
            overallCounts.merge(status, count.intValue(), Integer::sum);
            totalAssignedToday += count.intValue();
            System.out.println("Total assigned today (Live): " + totalAssignedToday);
        }

        // Process overall data from ABC
        for (Object[] record : overallStatsAbc) {
            String status = (String) record[0];
            Long count = (Long) record[1];
            overallCounts.merge(status, count.intValue(), Integer::sum);
            totalAssignedToday += count.intValue();
            System.out.println("Total assigned today (ABC): " + totalAssignedToday);
        }

        // Split user-specific report into Live and ABC
        Map<String, Map<String, Integer>> userReportLive = new HashMap<>();
        Map<String, Map<String, Integer>> userReportAbc = new HashMap<>();

        // Process user-specific data from Live
        for (Object[] record : userStatsLive) {
            String userId = String.valueOf(record[0]);
            String status = (String) record[1];
            Long count = (Long) record[2];

            // Fetch user by userId and create userName
            User user = userRepo.findByUserId(Integer.parseInt(userId)).get();
            String userName = user.getFirstName() + " " + user.getLastName();

            // Aggregate counts for Live data
            userReportLive.computeIfAbsent(userName, k -> new HashMap<>())
                    .merge(status, count.intValue(), Integer::sum);

            // Add total tickets assigned to the user in Live
            userReportLive.get(userName)
                    .merge("totalAssignedToThisUser", count.intValue(), Integer::sum);
        }

        // Process user-specific data from ABC
        for (Object[] record : userStatsAbc) {
            String userId = String.valueOf(record[0]);
            String status = (String) record[1];
            Long count = (Long) record[2];

            // Fetch user by userId and create userName
            User user = userRepo.findByUserId(Integer.parseInt(userId)).get();
            String userName = user.getFirstName() + " " + user.getLastName();

            // Aggregate counts for ABC data
            userReportAbc.computeIfAbsent(userName, k -> new HashMap<>())
                    .merge(status, count.intValue(), Integer::sum);

            // Add total tickets assigned to the user in ABC
            userReportAbc.get(userName)
                    .merge("totalAssignedToThisUser", count.intValue(), Integer::sum);
        }

        // Combine Live and ABC user reports
        Map<String, Object> combinedUserReport = new HashMap<>();
        combinedUserReport.put("Live", userReportLive);
        combinedUserReport.put("ABC", userReportAbc);

        // Prepare final result
        Map<String, Object> result = new HashMap<>();
        result.put("userReport", combinedUserReport);
        result.put("statusBreakdown", overallCounts);
        result.put("totalTodayAssigned", totalAssignedToday);

        return result;
    }



}
