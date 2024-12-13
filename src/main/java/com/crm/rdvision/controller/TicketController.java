package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.*;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.feignClient.TichetFiegnClient;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.*;
import com.crm.rdvision.utility.Constants;
import com.crm.rdvision.utility.RateLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@CrossOrigin
@RequestMapping("/third_party_api/ticket/")
public class TicketController {
    @Autowired
    TichetFiegnClient tichetFiegnClient;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    TeamRepo teamRepo;
    @Autowired
    UserRepo userRepo;
    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    private CallServiceHoduCC callServiceHoduCC;
    @Autowired
    private TicketUpdateHistoryRepo ticketUpdateHistoryRepo;
    @Autowired
    private UploadTicketRepo uploadTicketRepo;
    @Autowired
    private CallRecordService callRecordService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private InvoiceRepo invoiceRepo;
    @Autowired OrderRepo orderRepo;
    private ProductOrderRepo productOrderRepo;
    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketTrackHistoryService ticketTrackHistoryService;


    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    private final RateLimiter rateLimiter = new RateLimiter(5, 20000); // Allow up to 5 concurrent requests with a
                                                                       // 20-second timeout

    /*
     * @Scheduled(fixedRate = 500000) // Run auto every 5 minutes
     * 
     * @GetMapping("/getTicket")
     * 
     * @Transactional
     * public Map<String, Object> getTickets() throws JsonProcessingException,
     * InterruptedException {
     * Map<String, Object> responseMap = new HashMap<>();
     * 
     * try {
     * boolean acquired = rateLimiter.tryAcquire();
     * if (!acquired) {
     * responseMap.put("error", "Rate limit exceeded. Please try again later.");
     * return responseMap;
     * }
     * 
     * String response = tichetFiegnClient.getTickets();
     * processTickets(response);
     * responseMap.put("ticket", response);
     * } catch (FeignException e) {
     * if (e.status() == 429) {
     * retryGetTickets(responseMap);
     * } else {
     * System.err.println("Error fetching tickets: " + e.getMessage());
     * responseMap.put("error", "Error fetching tickets: " + e.getMessage());
     * }
     * } catch (Exception e) {
     * System.err.println("Unexpected error: " + e.getMessage());
     * responseMap.put("error", "Unexpected error: " + e.getMessage());
     * } finally {
     * rateLimiter.release();
     * }
     * 
     * return responseMap;
     * }
     */

    private void retryGetTickets(Map<String, Object> responseMap) throws InterruptedException {
        int retries = 5;
        long delay = 1000;

        for (int i = 0; i < retries; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
                String response = tichetFiegnClient.getTickets();
                processTickets(response);
                responseMap.put("ticket", response);
                return;
            } catch (FeignException e) {
                if (e.status() != 429 || i == retries - 1) {
                    System.err.println("Retry failed: " + e.getMessage());
                    responseMap.put("error", "Retry failed: " + e.getMessage());
                    return;
                }
                delay *= 2; // Exponential backoff
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processTickets(String response) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TicketResponse ticketResponse = objectMapper.readValue(response, TicketResponse.class);

        for (Response ticket : ticketResponse.getResponse()) {
            modelMapper.getConfiguration()
                    .setMatchingStrategy(MatchingStrategies.STRICT) // Ensure strict field name matching
                    .setFieldMatchingEnabled(true)
                    .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
            TicketEntity ticketEntity = modelMapper.map(ticket, TicketEntity.class);
            ticketEntity.setTicketstatus("New");
            ticketRepo.save(ticketEntity);
        }

    }

    @PostMapping(EndPointReference.ASSIGN_TICKT_TO_USER)
    public Map<String, Object> assignTicketToUser(@PathVariable Integer userId, @RequestBody List<String> ticketIds)
            throws JsonProcessingException {
        logger.info("Assign ticket to User Called");
        Map<String, Object> map = new HashMap<>();
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid team id"));
        for (String tickets : ticketIds) {
            TicketEntity ticket = ticketRepo.findByUniqueQueryId(tickets);
            ticket.setAssigntouser(user.getUserId());
            ticket.setAssignDate(LocalDate.now());
            ticketRepo.save(ticket);
        }
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;

    }

    @PostMapping(EndPointReference.ASSIGN_TICKT_TO_TAEM)
    public Map<String, Object> assignTicketToTeam(@PathVariable Integer teamId, @RequestBody List<String> ticketIds)
            throws JsonProcessingException {
        logger.info("Assign Ticket to team Called");
        Map<String, Object> map = new HashMap<>();
        Team team = teamRepo.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Invalid team id"));
        for (String tickets : ticketIds) {
            TicketEntity ticket = ticketRepo.findByUniqueQueryId(tickets);
            ticket.setAssigntoteam(team.getTeamId());
            ticketRepo.save(ticket);
        }

        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;

    }

    // @GetMapping(EndPointReference.GET_ALL_TICKET_BY_TEAM)
    // public Map<String, Object> getAllTicketOfTeam(@PathVariable Integer
    // teamId,@PathVariable String ticketStatus) throws JsonProcessingException {
    // Map<String, Object> map=new HashMap<>();
    // List<TicketEntity> ticket =
    // ticketRepo.findByTicketstatusAndAssigntoteam(ticketStatus,teamId);
    // map.put(Constants.DTO_LIST, mapTicketResponse(ticket));
    // map.put(Constants.ERROR, null);
    // map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
    // return map;
    //
    // }
    
    //get by ticket status
    // public Map<String, Object> getAllTicketOfUser(@RequestParam(required = false)
    // Integer userId,@RequestParam(required = false) Integer
    // teamId,@RequestParam(required = false) String ticketStatus) throws
    // JsonProcessingException {
    // logger.info("Get All tickets By Stats called");
    // Map<String, Object> map=new HashMap<>();
    // List<TicketEntity> ticket=new ArrayList<>();
    // if (null!=userId && null!=ticketStatus ){
    // ticket = ticketRepo.findByTicketstatusAndAssigntouser(ticketStatus,userId);
    // } else if (null!=teamId && null!=ticketStatus) {
    // ticket = ticketRepo.findByTicketstatusAndAssigntoteam(ticketStatus,teamId);
    // } else if (null!=userId) {
    // ticket = ticketRepo.findByAssigntouser(userId);
    // } else if (null!=ticketStatus) {
    // ticket = ticketRepo.findByTicketstatus(ticketStatus);
    // }else {
    // ticket= ticketRepo.findByAssigntouserIsNull();
    // }
    // Collections.sort(ticket,
    // Comparator.comparing(TicketEntity::getQueryTime).reversed());
    // map.put(Constants.DTO_LIST, mapTicketResponse(ticket));
    // map.put(Constants.ERROR, null);
    // map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
    // return map;
    //
    // }
//    public Map<String, Object> getAllTicketOfUser(@RequestParam(required = false) Integer userId,
//            @RequestParam(required = false) Integer teamId,
//            @RequestParam(required = false) String ticketStatus,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) throws JsonProcessingException {
//        Map<String, Object> map = new HashMap<>();
//        Page<TicketEntity> ticketPage;
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "queryTime"));
//
//        if (userId != null && ticketStatus != null) {
//            if(!ticketStatus.equals("Follow")){
//                ticketPage = ticketRepo.findByTicketstatusAndAssigntouserOrAssigntouserIsNull(ticketStatus, userId, pageable);
//            }else{
//                List<String> list=new ArrayList<>();
//                list.add("New");
//                list.add("Sale");
//                ticketPage=ticketRepo.findByTicketstatusNotInAndAssigntouserOrAssigntouserIsNull(list,userId,pageable);
//            }
//            logger.info("Getting all ticket by Pagination, ticket status, userId ");
//
//        } else if (teamId != null && ticketStatus != null) {
//            logger.info("Getting tickets by ticket status, team id, pagination");
//            ticketPage = ticketRepo.findByTicketstatusAndAssigntoteam(ticketStatus, teamId, pageable);
//        } else if (userId != null) {
//            logger.info("Getting tickets by user id and pagination");
//            ticketPage = ticketRepo.findByAssigntouser(userId, pageable);
//        } else if (ticketStatus != null) {
//            logger.info("Getting tickets by ticket status and pagination");
//            ticketPage = ticketRepo.findByTicketstatus(ticketStatus, pageable);
//        } else {
//            logger.info("Getting all tickets bye only pagination ");
//            ticketPage = ticketRepo.findByAssigntouserIsNull(pageable);
//        }
//        logger.info("Mapping response");
//        map.put(Constants.DTO_LIST, mapTicketResponse(ticketPage.getContent()));
//        map.put(Constants.TOTAL_ELEMENTS, ticketPage.getTotalElements());
//        map.put(Constants.TOTAL_PAGES, ticketPage.getTotalPages());
//        map.put(Constants.CURRENT_PAGE, page);
//        map.put(Constants.PAGE_SIZE, size);
//        map.put(Constants.ERROR, null);
//        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
//        return map;
//    }
    @GetMapping(EndPointReference.GET_ALL_TICKET_BY_STATUS)
    public Map<String, Object> getAllTicketOfUser(@RequestParam(required = false) Integer userId,
                                                  @RequestParam(required = false) Integer teamId,
                                                  @RequestParam(required = false) String ticketStatus,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        Page<TicketEntity> ticketPage = null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "queryTime"));

        if (userId != null && ticketStatus != null) {
            logger.info("Getting all ticket by Pagination, ticket status, userId ");
            ticketPage = ticketRepo.findByTicketstatusAndAssigntouserOrNull(ticketStatus, userId, pageable);
         }
        else if (teamId != null && ticketStatus != null) {
            logger.info("Getting tickets by ticket status, team id, pagination");
            ticketPage = ticketRepo.findByTicketstatusAndAssigntoteam(ticketStatus, teamId, pageable);
        } else if (userId != null) {
            logger.info("Getting tickets by user id and pagination");
            ticketPage = ticketRepo.findByAssigntouser(userId, pageable);
        } else if (ticketStatus != null) {
            logger.info("Getting tickets by ticket status and pagination");
            ticketPage = ticketRepo.findByTicketstatus(ticketStatus, pageable);
        } else {
            logger.info("Getting all tickets bye only pagination ");
            ticketPage = ticketRepo.findByAssigntouserIsNull(pageable);
        }
        logger.info("Mapping response");
        map.put(Constants.DTO_LIST, mapTicketResponse(ticketPage.getContent()));
        map.put(Constants.TOTAL_ELEMENTS, ticketPage.getTotalElements());
        map.put(Constants.TOTAL_PAGES, ticketPage.getTotalPages());
        map.put(Constants.CURRENT_PAGE, page);
        map.put(Constants.PAGE_SIZE, size);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }

    @GetMapping({"/getAllNewTickets", "/getAllNewTickets/{userId}"})
    public List<TicketEntity> getAllNewTickets(@PathVariable(required = false) String userId){
        if(userId!=null){
            return ticketRepo.findAllByTicketstatusAndAssigntouserOrderByQueryTimeDesc("New",Integer.parseInt(userId));
        }else{
            return ticketRepo.findAllByTicketstatusOrderByQueryTimeDesc("New");
        }
    }

    // Get all tickets by User
    @GetMapping("getAllByUser/{userId}")
    public List<TicketEntity> GetAllTicketsAssignedToUser(@PathVariable int userId) {
        return ticketRepo.findByAssigntouser(userId);
    }

    @GetMapping(EndPointReference.GET_TICKET)
    public Map<String, Object> getTicket(@PathVariable String ticketId)
            throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Ticket By ticket Id Called");
        Map<String, Object> map = new HashMap<>();
        try {
            logger.info("Mapping response ");
            map.put(Constants.DTO_LIST, ticketRepo.findByUniqueQueryId(ticketId));
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }

    @PostMapping(EndPointReference.UPDATE_TICKET_RESPONSE)
    public Map<String, Object> updateTicketResponse(@PathVariable String ticketId, @RequestParam int userId,
            @RequestParam(required = false) String call_id, @RequestParam(required = false) String ticketStatus,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) LocalDateTime followUpDateTime) throws JsonProcessingException {
        logger.info("Update ticket Response Called");
        Map<String, Object> map = new HashMap<>();
        TicketEntity ticket = ticketRepo.findByUniqueQueryId(ticketId);
        if(ticket.getAssigntouser()==null){
            ticket.setAssigntouser((userId));
            ticket.setAssignDate(LocalDate.now());
            ticketRepo.save((ticket));
        }
        Optional<User> user = userRepo.findByUserId(userId);
        ticket.setTicketstatus(ticketStatus);
        ticket.setFollowUpDateTime(followUpDateTime);
        if (null != comment) {
            ticket.setComment(comment);
        }
        ticketRepo.save(ticket);
        if(Objects.equals(ticketStatus, "Interested")){
            emailService.sendThankYouEmail(ticket.getSenderEmail(),ticket.getSenderName(),ticket.getQueryProductName());

        }else if(Objects.equals(ticketStatus, "Follow")){
            emailService.sendFollowUpEmail(ticket.getSenderEmail(),ticket.getSenderName(),followUpDateTime);

       }
//        else if (Objects.equals(ticketStatus, "Sale")) {
//            Invoice invoice=invoiceRepo.findByTicketId(ticketId);
//            OrderItem orderItem=orderRepo.findByTicketId(ticketId);
//            List<ProductOrder> productOrders=productOrderRepo.findAllByOrderId(orderItem.getOrderId());
//            emailService.sendPurchaseThankYouEmail(ticket.getSenderEmail(),ticket.getSenderName(),"Testing product",39090.00,orderItem.getTicketId());
//        }
        map.put("status", ticketStatus);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        try {
            TicketStatusUpdateHistory ticketStatusUpdateHistory = new TicketStatusUpdateHistory();
            ticketStatusUpdateHistory.setUpdatedBy(userId);
            ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
            ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
            ticketStatusUpdateHistory.setStatus(ticketStatus);
            ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketId);
            ticketStatusUpdateHistory.setComment(comment);
            ticketStatusUpdateHistory.setUserName(user.get().getFirstName() + " " + user.get().getLastName());
            if (!call_id.equals("0")) {
                try {
                    String recordingString = callServiceHoduCC.getRecording(call_id);
                    String[] parts = recordingString.split(":");
                    if (parts.length >= 3) {
                        String recordingFile = parts[1] + ":" + parts[2].replace("}", "");
                        ticketStatusUpdateHistory.setRecordingFile(recordingFile);
                    } else {
                        System.out.println("Unexpected format in recordingString: " + recordingString);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error while fetching or processing recording for call_id: " + call_id);
                }
            }


            ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
            logger.info("Ticket history updated successfully for ticketId: {}", ticketId);
        } catch (Exception e) {
            logger.error("Error updating ticket history for ticketId: {}", ticketId, e);
            // Optionally, you can rethrow the exception or handle it based on your needs.
        }
        ticketTrackHistoryService.addTicketTrackHistory(ticketId,ticket.getSenderName(),ticketStatus,ticket.getQueryTime(),userId,"Status Update");
        return map;

    }


    @GetMapping("/followUpByDate/{userId}")
    public Map<String, Object> numberOfFollowUpTicketsByDate(@PathVariable int userId) {
        // Initialize the response list and map
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();

        // Retrieve the results from the repository
        List<Object[]> results = ticketRepo.findDateAndTicketCountAndCommentsWithSenderNameByStatusFollowAndAssigntouser(userId);

        // Process the results to format them as needed
        for (Object[] result : results) {
            Map<String, Object> entry = new HashMap<>();
            String date = (String) result[0];
            String comments =(String) result[2];
            Integer count = ((Number) result[1]).intValue(); // Convert the count to Integer
            entry.put("date", date);
            entry.put("no of tickets", count);
            entry.put("comments",comments);
            responseList.add(entry);
        }

        // Add the response list to the map with the key "response"
        map.put("response", responseList);

        // Return the map as the response
        return map;
    }

    @GetMapping("/getRatioOfTickets")
    public Map<String, Double> getRationOfTicketsStatus() {
        List<TicketEntity> tickets = ticketRepo.findAll();
        Map<String, Double> map = new HashMap<>();
        int totalTickets = tickets.size();
        int totalFollowTickets = 0;
        int totalSaleTickets = 0;
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).getTicketstatus().contains("Follow")) {
                totalFollowTickets++;
            } else if (tickets.get(i).getTicketstatus().contains("Sale")) {
                totalSaleTickets++;
            }
        }
        map.put("followVsAll", (double) totalFollowTickets / totalTickets);
        map.put("saleVsFollow", (double) totalSaleTickets / totalFollowTickets);
        map.put("saleVsAll", (double) totalSaleTickets / totalTickets);
        map.put("allTickets", (double) totalTickets);
        map.put("totalFollowUp", (double) totalFollowTickets);
        map.put("totalSaleTickets", (double) totalSaleTickets);
        return map;
    }

    // Click to call api
    @PostMapping("/clickToCall")
    public ResponseEntity<String> clickToCallForTicket(@RequestBody Map<String, String> numberDetails) {
        TicketEntity ticket1=ticketRepo.findByUniqueQueryId(numberDetails.get("ticketId"));
        try{
            TicketTrackHistory ticketTrackHistory=new TicketTrackHistory();
            ticketTrackHistoryService.addTicketTrackHistory(ticket1.getUniqueQueryId(),ticket1.getSenderName(),ticket1.getTicketstatus(),ticket1.getQueryTime(),Integer.parseInt(numberDetails.get("userId")),"Clicked To Call");
        }catch (Exception e){

        }
        callRecordService.addCallRecord(numberDetails);
        TicketEntity ticket =new TicketEntity();
        return callServiceHoduCC.clickToCall(numberDetails,ticket);
    }
    @PostMapping("/totalassigntickets")
    public List<Map<String,String>> totalAssignTicketsToUserInSelectedWeek(@RequestBody Map<String,String> object ){
        int userId = Integer.parseInt(object.get("userId"));
        int weeks =Integer.parseInt(object.get("weeks"));
        LocalDate fromDate =LocalDate.now();
        LocalDate toDate = fromDate.minusDays(weeks*7);
        return ticketRepo.countByAssignDatesBetweenAndAssigntouser(toDate,fromDate,userId);
    }
    @PostMapping("/totalassignticketsmonthly")
    public List<Map<String,String>> totalAssignTicketsToUserInSelectedMonth(@RequestBody Map<String,String> object ){
        int userId = Integer.parseInt(object.get("userId"));
        int weeks =Integer.parseInt(object.get("weeks"));
        LocalDate fromDate =LocalDate.now();
        LocalDate toDate = fromDate.minusDays(weeks*365);
        return ticketRepo.countByAssignMonthAndYear(toDate,fromDate,userId);
    }
   @GetMapping("/todayfollowup/{userId}")
    public int totalNumberOfFollowupByUserByToday(@PathVariable int userId){
       return uploadTicketRepo.findByAssigntouserAndTicketstatusInAndFollowUpDate(userId,LocalDate.now()).size()+ticketRepo.findByAssigntouserAndTicketstatusInAndFollowUpDate(userId,LocalDate.now()).size();
     }
    public List<TicketResponseDto> mapTicketResponse(List<TicketEntity> ticket) {
        List<TicketResponseDto> ticketResponseDtos = new ArrayList<>();
        for (TicketEntity ticketEntity : ticket) {
            ticketResponseDtos.add(modelMapper.map(ticketEntity, TicketResponseDto.class));
        }
        return ticketResponseDtos;
    }

    @PostMapping("/negotiationstagebased")
    public List<NegotiationDto> ticketstesting(@RequestBody Map<String, String> map) {
        int userId = Integer.parseInt(map.get("user"));
        int stage = Integer.parseInt(map.get("stage"));

        List<String> list = new ArrayList<>();

        // Modify the list based on the stage
        switch (stage) {
            case 1:
                list.add("Not_Pickup");
                list.add("Wrong_Number");
                list.add("Not_Interested");
                list.add("Not_Connected");
                list.add("hang_up");
                break;
            case 2:
                list.add("Follow");
                list.add("Interested");
                list.add("Place_with_other");
                break;
            case 3:
                list.add("Sale");
                break;
            default:
                break;
        }
       if(userId==0){
           // Fetch tickets and uploaded tickets based on the modified list
           List<TicketEntity> tickets = ticketRepo.findByTicketstatusIn(list);
           List<UploadTicket> uploadedTickets = uploadTicketRepo.findByTicketstatusIn(list);

           List<NegotiationDto> ticketDtos = new ArrayList<>();
           for(int i=0;i<tickets.size();i++){
               TicketEntity ticket =tickets.get(i);
               NegotiationDto negotiationDto=new NegotiationDto();
               negotiationDto.setComment(ticket.getComment());
               negotiationDto.setEmail(ticket.getSenderEmail());
               negotiationDto.setSenderName(ticket.getSenderName());
               negotiationDto.setMobileNumber(ticket.getSenderMobile());
               negotiationDto.setFollowUpDateTime(ticket.getFollowUpDateTime());
               negotiationDto.setTicketstatus(ticket.getTicketstatus());
               negotiationDto.setQueryTime(ticket.getQueryTime());
               negotiationDto.setQueryProductName(ticket.getQueryProductName());
               negotiationDto.setUniqueQueryId(ticket.getUniqueQueryId());
               negotiationDto.setSenderCountryIso(ticket.getSenderCountryIso());
               negotiationDto.setLastActionDate(ticket.getLastActionDate());
               negotiationDto.setDeliveryStatus(ticket.getDeliveryStatus());
               negotiationDto.setRecordingFile(ticket.getRecordingFile());
               negotiationDto.setTrackingNumber(ticket.getTrackingNumber());
               ticketDtos.add(negotiationDto);
           }
           List<NegotiationDto> uploadTicketDtos = uploadedTickets.stream()
                   .map(uploadTicket -> modelMapper.map(uploadTicket, NegotiationDto.class))
                   .collect(Collectors.toList());

           // Combine the lists
           List<NegotiationDto> combinedList = Stream.concat(ticketDtos.stream(), uploadTicketDtos.stream())
                   .collect(Collectors.toList());

           combinedList.sort((dto1, dto2) -> {
               LocalDateTime date1 = getDateTimeFromDto(dto1);
               LocalDateTime date2 = getDateTimeFromDto(dto2);
               return date2.compareTo(date1);
           });
           return combinedList;
       }else{
           List<TicketEntity> tickets = ticketRepo.findByAssigntouserAndTicketstatusIn(userId, list);
           List<UploadTicket> uploadedTickets = uploadTicketRepo.findByAssigntouserAndTicketstatusIn(userId, list);
           List<NegotiationDto> ticketDtos = new ArrayList<>();
           for(int i=0;i<tickets.size();i++){
               TicketEntity ticket =tickets.get(i);
               NegotiationDto negotiationDto=new NegotiationDto();
               negotiationDto.setComment(ticket.getComment());
               negotiationDto.setEmail(ticket.getSenderEmail());
               negotiationDto.setSenderName(ticket.getSenderName());
               negotiationDto.setMobileNumber(ticket.getSenderMobile());
               negotiationDto.setFollowUpDateTime(ticket.getFollowUpDateTime());
               negotiationDto.setTicketstatus(ticket.getTicketstatus());
               negotiationDto.setQueryTime(ticket.getQueryTime());
               negotiationDto.setQueryProductName(ticket.getQueryProductName());
               negotiationDto.setUniqueQueryId(ticket.getUniqueQueryId());
               negotiationDto.setLastActionDate(ticket.getLastActionDate());
               negotiationDto.setDeliveryStatus(ticket.getDeliveryStatus());
               negotiationDto.setRecordingFile(ticket.getRecordingFile());
               negotiationDto.setTrackingNumber(ticket.getTrackingNumber());
               ticketDtos.add(negotiationDto);
           }
           List<NegotiationDto> uploadTicketDtos = uploadedTickets.stream()
                   .map(uploadTicket -> modelMapper.map(uploadTicket, NegotiationDto.class))
                   .collect(Collectors.toList());

           // Combine the lists
           List<NegotiationDto> combinedList = Stream.concat(ticketDtos.stream(), uploadTicketDtos.stream())
                   .collect(Collectors.toList());

           // Sort the combined list based on queryTime and uploadDate
           combinedList.sort((dto1, dto2) -> {
               LocalDateTime date1 = getDateTimeFromDto(dto1);
               LocalDateTime date2 = getDateTimeFromDto(dto2);

               // Sort by descending date (newest first)
               return date2.compareTo(date1);
           });

           return combinedList;
       }
    }


    @GetMapping("/clickToCall/{ticketId}")
    public ResponseEntity<String> clicktoCallByTicketid(@PathVariable String ticketId){
        ResponseEntity<String> response = callServiceHoduCC.clickToCallByTicketId(ticketId);
        String recordingFile =callServiceHoduCC.getRecording(extractCallId(response));
        TicketEntity ticket=ticketRepo.findByUniqueQueryId(ticketId);
        ticket.setRecordingFile(recordingFile);
        ticket.setComment("Called");
        ticketRepo.save(ticket);
        return response;
    }

    public String extractCallId(ResponseEntity<String> response) {
        String responseBody = response.getBody();
        if (responseBody != null) {
            int jsonStartIndex = responseBody.indexOf('{');
            if (jsonStartIndex != -1) {
                String jsonString = responseBody.substring(jsonStartIndex);
                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.has("call_id")) {
                    return jsonObject.getString("call_id");
                }
            }
        }
        return null; // return null if not found
    }
    @GetMapping({"/ticketswithstatus","/ticketswithstatus/{startdate}"})
    public Map<String, Object> getTicketStatistics(@PathVariable(required = false)  String startdate) {
      if(startdate==null){
          List<Object[]> results = ticketRepo.countTicketsByStatus();
          List<Map<String, Long>> ticketCounts = new ArrayList<>();
          long totalTickets = ticketRepo.countTotalTickets();
          // Add totalTickets as the first entry
          Map<String, Long> totalTicketsMap = new HashMap<>();
          totalTicketsMap.put("totalTickets", totalTickets);
          ticketCounts.add(totalTicketsMap);
          // Process results and add to ticketCounts
          ticketCounts.addAll(results.stream()
                  .map(result -> {
                      Map<String, Long> map = new HashMap<>();
                      map.put((String) result[0], ((Number) result[1]).longValue());
                      return map;
                  })
                  .sorted((a, b) -> Long.compare(b.values().iterator().next(), a.values().iterator().next())) // Sort by count descending
                  .toList());

          // Prepare the final result map
          Map<String, Object> result = new HashMap<>();
          result.put("ticketCounts", ticketCounts);
          return result;
      }else{
          LocalDate startDate  =LocalDate.parse(startdate);
          LocalDate enddate =LocalDate.now();
          List<Object[]> results = ticketRepo.countTicketsByStatusWithinDateRange(startDate,enddate);
          List<Map<String, Long>> ticketCounts = new ArrayList<>();
          long totalTickets = ticketRepo.countTotalTicketsWithinDateRange(startDate,enddate);

          // Add totalTickets as the first entry
          Map<String, Long> totalTicketsMap = new HashMap<>();
          totalTicketsMap.put("totalTickets", totalTickets);
          ticketCounts.add(totalTicketsMap);

          // Process results and add to ticketCounts
          ticketCounts.addAll(results.stream()
                  .map(result -> {
                      Map<String, Long> map = new HashMap<>();
                      map.put((String) result[0], ((Number) result[1]).longValue());
                      return map;
                  })
                  .sorted((a, b) -> Long.compare(b.values().iterator().next(), a.values().iterator().next())) // Sort by count descending
                  .toList());

          // Prepare the final result map
          Map<String, Object> result = new HashMap<>();
          result.put("ticketCounts", ticketCounts);

          return result;
      }
    }

    @GetMapping({"/ticketsCountByCountry", "/ticketsCountByCountry/{startdate}"})
    public List<Object[]> getTicketsCountByStatus(@PathVariable(required = false) LocalDate startdate) {
        if (startdate == null) {
            return ticketRepo.countTicketsByCountry();
        } else {
            LocalDate endDate = LocalDate.now();
            return ticketRepo.countTicketsByCountryWithinDateRange(startdate, endDate);
        }
    }


@GetMapping({"/salesByCountry","/salesByCountry/{startdate}"})
public List<Object[]> getSalesCountByCountry(@PathVariable(required = false)  String startdate){
  if(startdate==null){
      return ticketRepo.countTicketsByCountryWithSaleStatus();
  }else{
      LocalDate endDate =LocalDate.now();
      LocalDate startDate  =LocalDate.parse(startdate);
      return ticketRepo.countTicketsByCountryWithSaleStatusWithinDateRange(startDate,endDate);
  }
}

@GetMapping("/getcountoftcikets/{userId}")
public List<Map<String, Integer>> getTicketCounts(@PathVariable int userId) {
    List<Object[]> results = ticketRepo.countTicketsByUserGroupByStatus(userId);
    List<Object[]> res = uploadTicketRepo.countTicketsByUserGroupByStatus(userId);


    Map<String, Integer> statusCountMap = new HashMap<>();

    // Process the first result set
    for (Object[] result : results) {
        String status = (String) result[0];
        Long count = (Long) result[1];
        statusCountMap.put(status, statusCountMap.getOrDefault(status, 0) + count.intValue());
    }

    for (Object[] result : res) {
        String status = (String) result[0];    // Ticket status
        Long count = (Long) result[1];
        statusCountMap.put(status, statusCountMap.getOrDefault(status, 0) + count.intValue());
    }
    List<Map<String, Integer>> statusCountList = new ArrayList<>();

    for (Map.Entry<String, Integer> entry : statusCountMap.entrySet()) {
        Map<String, Integer> mapEntry = new HashMap<>();
        mapEntry.put(entry.getKey(), entry.getValue());
        statusCountList.add(mapEntry);
    }

    return statusCountList;
}

    // Helper method to extract the LocalDateTime from either uploadDate or queryTime
    private LocalDateTime getDateTimeFromDto(NegotiationDto dto) {
        if (dto.getUploadDate() != null) {
            // Convert uploadDate (which is likely a list of integers [year, month, day]) to LocalDateTime
            List<Integer> uploadDate = convertLocalDateToList(dto.getUploadDate());
            return LocalDateTime.of(uploadDate.get(0), uploadDate.get(1), uploadDate.get(2), 0, 0, 0);
        } else if (dto.getQueryTime() != null) {
            String queryTime = dto.getQueryTime();
            try {
                // Handle the case where the queryTime is in ISO 8601 format (e.g., 2024-08-30T15:30:00Z)
                Instant instant = Instant.parse(queryTime);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                // Fallback: If the queryTime doesn't match the ISO format, try another pattern
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(queryTime, formatter);
            }
        }
        // If both are null, return the current time (or handle this case appropriately)
        return LocalDateTime.now();
    }

    // Convert LocalDate to a list of integers
    public static List<Integer> convertLocalDateToList(LocalDate date) {
        return Arrays.asList(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    //tickets one by one
    @PostMapping("/next")
    public ActionTicketDto getNextTicket( @RequestBody Map<String,String> map) {
        return ticketService.getNextTicket(map.get("status"), map.get("userId").toString());
    }

    @PostMapping("/previous")
    public ActionTicketDto getPreviousTicket( @RequestBody Map<String,String> map) {
        return ticketService.getPreviousTicket(map.get("status"),map.get("userId").toString());
    }
    @PostMapping("/getFirstTicket")
    public ActionTicketDto getFirstTicket( @RequestBody Map<String,String> map) {
        return ticketService.getFirstTicket(map.get("status"), map.get("userId").toString());
    }
    @PostMapping("/getCurrentWorkingTicket")
    public ActionTicketDto getCurrentWorking( @RequestBody Map<String,String> map) {
        return ticketService.getFirstTicket(map.get("status"), map.get("userId").toString());
    }
    @PostMapping("/getSpecificTicketByNumber")
    public ActionTicketDto getSpecificTicketByTicketnumber( @RequestBody Map<String,String> map) {
        return ticketService.getSpecificTicketByNumber(map.get("status"), map.get("userId"),Integer.parseInt(map.get("number")));
    }
    @PostMapping("/getBySearchQuery")
    public ActionTicketDto getBySearchQuery( @RequestBody Map<String,String> map) {
        return ticketService.getSpecificTicketByDetails(map.get("status"), map.get("userId"),map.get("searchQuery"));
    }

    @GetMapping("/getticketCounts")
    public List<Map<String, Object>> getTicketsCounts() {
        List<Object[]> objects = ticketRepo.getTicketsCountByUser();

        return objects.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();

            // Extract values from each row
            String userId = row[0] != null ? row[0].toString() : "NotAssignedTicket";
            Long ticketCount = (Long) row[1];  // Cast to Long instead of Integer

            // Optionally, if you need to convert the Long to Integer (e.g., for UI), use intValue()
            Integer ticketCountInt = ticketCount.intValue();  // Converting Long to Integer

            // Fetch userName from UserRepository for each userId
            String userName = "Unassigned";
            if (!"NotAssignedTicket".equals(userId)) {
                userName = userRepo.findByUserId(Integer.parseInt(userId))
                        .map(user -> user.getFirstName())
                        .orElse("Unknown User");
            }

            // Add data to the map
            map.put("userId", userId);
            map.put("userName", userName);
            map.put("ticketCount", ticketCountInt);

            return map;
        }).collect(Collectors.toList());
    }



    @GetMapping("/numberOfNotassignedTickets")
    public Map<String,Integer> getNumberOfNotAssignedTicketsInBoth(){
        Map<String,Integer> number =new HashMap<>();
        number.put("liveCount",ticketRepo.countByAssigntouserIsNull());
        number.put("uploadedCount",uploadTicketRepo.countByAssigntouserIsNull());
        return number;
    }

    @PostMapping("/getStatusReport")
    public Map<String, Map<String, Object>> getStatusReports(@RequestBody Map<String,LocalDate> map) {
        LocalDate endDate = map.get("endDate");
        LocalDate startDate = map.get("startDate");

        // Fetch raw data
        List<Object[]> rawResults = ticketRepo.findStatusCountsByUserAndDateRange(startDate, endDate);
        List<Object[]> uploadedResults = uploadTicketRepo.findStatusCountsByUserAndDateRange(startDate, endDate);

        // Process results
        Map<String, Map<String, Integer>> liveResult = mapResults(rawResults);
        Map<String, Map<String, Integer>> abcResult = mapResults(uploadedResults);

        // Merge data into the desired format
        Map<String, Map<String, Object>> finalResult = new HashMap<>();

        // Combine Live and Abc data under each user
        mergeResults(finalResult, liveResult, "Live");
        mergeResults(finalResult, abcResult, "Abc");

        return finalResult;
    }

    private Map<String, Map<String, Integer>> mapResults(List<Object[]> results) {
        Map<String, Map<String, Integer>> result = new HashMap<>();

        results.forEach(record -> {
            String userId = String.valueOf(record[0]);
            if (!Objects.equals(userId, "null")) {
                User user = userRepo.findByUserId(Integer.parseInt(userId)).orElse(null);
                if (user != null) {
                    String status = (String) record[1];  // Status
                    Long count = (Long) record[2];       // Count

                    result.computeIfAbsent(user.getFirstName() + " " + user.getLastName(),
                                    k -> new HashMap<>())
                            .put(status, count.intValue());
                }
            }
        });

        return result;
    }

    private void mergeResults(Map<String, Map<String, Object>> finalResult,
                              Map<String, Map<String, Integer>> data,
                              String category) {
        data.forEach((userName, statusCounts) -> {
            finalResult
                    .computeIfAbsent(userName, k -> new HashMap<>())
                    .put(category, statusCounts);
        });
    }

    //Today assigned Report

    @GetMapping("/getTodayStatusReport")
    public Map<String, Object> getTodayStatusReport() {
        return ticketService.getTodayStatusReport();
    }

    @GetMapping("/getTiccketCountforBar/{userId}")
    public List<Map<String, Object>> getNumberOfTickets(@PathVariable int userId) {
        List<Map<String, Object>> combinedResults = new ArrayList<>();

        // Fetch live tickets and transform
        List<Map<String, Long>> live = ticketRepo.getStatusCountByUserId(userId);
        live.forEach(map -> {
            Map<String, Object> mutableMap = new HashMap<>(map); // Create a mutable copy
            mutableMap.put("type", 1L);
            combinedResults.add(mutableMap);
        });

        // Fetch upload tickets and transform
        List<Map<String, Long>> abc = uploadTicketRepo.getStatusCountByUserId(userId);
        abc.forEach(map -> {
            Map<String, Object> mutableMap = new HashMap<>(map); // Create a mutable copy
            mutableMap.put("type", 2L);
            combinedResults.add(mutableMap);
        });

        return combinedResults;
    }


    @GetMapping("/getDistinctCountries")
    public List<String> getAllDistinctCountries(){
        return ticketRepo.findAllDistinctCountryIso();
    }


}