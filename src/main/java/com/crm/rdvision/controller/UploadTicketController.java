package com.crm.rdvision.controller;


import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.dto.UploadedTicketDto;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.TicketUpdateHistoryRepo;
import com.crm.rdvision.repository.UploadTicketRepo;
import com.crm.rdvision.repository.UserRepo;
import com.crm.rdvision.service.CallServiceHoduCC;
import com.crm.rdvision.service.EmailService;
import com.crm.rdvision.service.ExcelService;
import com.crm.rdvision.utility.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/upload/")
public class UploadTicketController {
    @Autowired
    private UploadTicketRepo uploadTicketRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    UserRepo userRepo;
    @Autowired
    TicketUpdateHistoryRepo ticketUpdateHistoryRepo;
    @Autowired
    CallServiceHoduCC callServiceHoduCC;
    @Autowired
    EmailService emailService;
    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(UploadTicketController.class);
    @Autowired
    private ExcelService excelService;


    @PostMapping("/upload_tickets")
    public ResponseEntity<?> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty. Please upload a valid Excel file.");
        }

        try {
            // Call service method to process file
            List<String> errors = excelService.saveProductsFromExcel(file);

            if (errors.isEmpty()) {
                return ResponseEntity.ok("File processed successfully. All rows are valid.");
            } else {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(errors);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the file: " + e.getMessage());
        }
    }


    @PostMapping(EndPointReference.ASSIGN_TICKT_TO_USER)
    public Map<String, Object> assignTicketToUser(@PathVariable Integer userId, @RequestBody List<String> ticketIds) throws JsonProcessingException {
        logger.info("Assign ticket to User Called");
        Map<String, Object> map=new HashMap<>();
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid team id"));
        for (String tickets:ticketIds){
            UploadTicket ticket = uploadTicketRepo.findByUniqueQueryId(tickets);
            ticket.setAssigntouser(user.getUserId());
            ticket.setAssignDate(LocalDate.now());
            uploadTicketRepo.save(ticket);
        }
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }

    @GetMapping(EndPointReference.GET_TICKET)
    public Map<String, Object> getTicket(@PathVariable String ticketId)
            throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Ticket By ticket Id Called");
        Map<String, Object> map = new HashMap<>();
        try {
            logger.info("Mapping response ");
            map.put(Constants.DTO_LIST, uploadTicketRepo.findByUniqueQueryId(ticketId));
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }


    @PostMapping(EndPointReference.UPDATE_TICKET_RESPONSE)
    public Map<String, Object> updateTicketResponse(@PathVariable String ticketId,@RequestParam int userId,@RequestParam(required = false) String call_id, @RequestParam(required = false) String ticketStatus, @RequestParam(required = false) String comment, @RequestParam(required = false) LocalDateTime followUpDateTime) throws JsonProcessingException {
        logger.info("Update ticket Response Called");
        Map<String, Object> map=new HashMap<>();
        UploadTicket ticket = uploadTicketRepo.findByUniqueQueryId(ticketId);
        if(ticket.getAssigntouser()==null){
            ticket.setAssigntouser((userId));
            uploadTicketRepo.save((ticket));
        }
        ticket.setTicketstatus(ticketStatus);
        Optional<User> user=userRepo.findByUserId(userId);
        ticket.setFollowUpDateTime(followUpDateTime);
        if (null!=comment){
            ticket.setComment(comment);
        }
        uploadTicketRepo.save(ticket);

        if(Objects.equals(ticketStatus, "Interested")){
            emailService.sendThankYouEmail(ticket.getEmail(),ticket.getFirstName(),ticket.getQueryProductName());

        }else if(Objects.equals(ticketStatus, "Follow")){
            emailService.sendFollowUpEmail(ticket.getEmail(),ticket.getFirstName(),followUpDateTime);

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
            ticketStatusUpdateHistory.setUserName(user.get().getFirstName()+" "+user.get().getLastName());
            if (!call_id.equals("0")) {
                try {
                    String recordingString = getRecording(call_id);
                    // Assuming the recordingString format is something like "key:value:value}"
                    // and you want to extract the second and third values.
                    String[] parts = recordingString.split(":");
                    if (parts.length >= 3) {
                        String recordingFile = parts[1] + ":" + parts[2].replace("}", "");
                        ticketStatusUpdateHistory.setRecordingFile(recordingFile);
                    } else {
                        // Handle cases where the split parts are not as expected
                        System.out.println("Unexpected format in recordingString: " + recordingString);
                    }
                } catch (Exception e) {
                    // Handle any exception that might occur during the getRecording call or string manipulation
                    e.printStackTrace(); // Print the stack trace for debugging
                    // Optionally, you can log the error or handle it in a specific way
                    System.out.println("Error while fetching or processing recording for call_id: " + call_id);
                }
            }
            ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
            logger.info("Ticket history updated successfully for ticketId: {}", ticketId);
        } catch (Exception e) {
            logger.error("Error updating ticket history for ticketId: {}", ticketId, e);
            // Optionally, you can rethrow the exception or handle it based on your needs.
        }



        return map;
    }

    //get recording function
    public String getRecording(String call_id) {
        String url = "http://omnicx.hoducc.com/HoduCC_api/v1.4/getRecording";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("tenant_id", "1117");
        requestBody.put("call_id",call_id);
        requestBody.put("token", "WErnuEh6WeijTptw");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String recordingFile = response.getBody().formatted();
        return (recordingFile.split(",")[3]).replace("\\","");
    }

    @GetMapping("/filesByDate")
    public Map<String,Object> getAllUploadedFilesByDate(){
        Map<String, Object> map=new HashMap<>();
        map.put("response",uploadTicketRepo.findDateAndTicketCount());
        return map;
    }


    @GetMapping("/getByDate/{date}")
    public Map<String, Object> getAllTicketOfUser(@RequestParam(required = false) Integer userId,
                                                  @RequestParam(required = false) Integer teamId,
                                                  @RequestParam(required = false) String ticketStatus,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,@PathVariable LocalDate date) throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        Page<UploadTicket> ticketPage;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"uploadDate"));

        if (userId != null && ticketStatus != null) {
            ticketPage = uploadTicketRepo.findByUploadDateAndTicketstatusAndAssigntouser(date,ticketStatus,userId,pageable);
        } else if (teamId != null && ticketStatus != null) {
            ticketPage = uploadTicketRepo.findByUploadDateAndTicketstatusAndAssigntoteam(date,ticketStatus, teamId, pageable);
        } else if (userId != null) {
            ticketPage = uploadTicketRepo.findByUploadDateAndAssigntouser(date,userId, pageable);
        } else if (ticketStatus != null) {
            ticketPage = uploadTicketRepo.findByUploadDateAndTicketstatus(date,ticketStatus, pageable);
        } else {
            ticketPage = uploadTicketRepo.findByUploadDateAndAssigntouserIsNull(date,pageable);
        }


        map.put(Constants.DTO_LIST, mapTicketResponse(ticketPage.getContent()));
        map.put(Constants.TOTAL_ELEMENTS, ticketPage.getTotalElements());
        map.put(Constants.TOTAL_PAGES, ticketPage.getTotalPages());
        map.put(Constants.CURRENT_PAGE, page);
        map.put(Constants.PAGE_SIZE, size);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }


    @GetMapping("/getABCTicketsNotAssigned")
    public Map<String, Object> getUnAssinedABCTickets(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Page<UploadTicket> ticketPage;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"queryTime"));
        ticketPage =uploadTicketRepo.findByAssigntouserIsNull(pageable);
        Map<String,Object> map=new HashMap<>();
        map.put(Constants.DTO_LIST,mapTicketResponse(ticketPage.getContent()));
        return map;
    }

    @GetMapping("/getAssignedTickets")
    public Map<String, Object> getByAssignToUserId(@RequestParam int userId,@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Page<UploadTicket> ticketPage;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"queryTime"));
        ticketPage =uploadTicketRepo.findByAssigntouser(pageable,userId);
        Map<String,Object> map=new HashMap<>();
        map.put(Constants.DTO_LIST,mapTicketResponse(ticketPage.getContent()));
        map.put(Constants.TOTAL_PAGES,ticketPage.getTotalPages());
        map.put(Constants.CURRENT_PAGE,page);
        map.put(Constants.PAGE_SIZE,size);
        return map;
    }

    @GetMapping("/followUpByDate")
    public Map<String,Object> numberOfFollowUpTicketsByDate(){
        Map<String, Object> map=new HashMap<>();
        map.put("response",uploadTicketRepo.findDateAndTicketCountByStatusFollow());
        return map;
    }

    @GetMapping("/getRatioOfTickets")
    public Map<String,Double> getRationOfTicketsStatus(){
        List<UploadTicket> tickets =uploadTicketRepo.findAll();
        Map<String,Double> map=new HashMap<>();
        int totalTickets=tickets.size();
        int totalFollowTickets =0;
        int totalSaleTickets=0;
        for(int i =0;i<tickets.size();i++){
            if(tickets.get(i).getTicketstatus().contains("Follow")){
                totalFollowTickets++;
            } else if (tickets.get(i).getTicketstatus().contains("Sale")) {
                totalSaleTickets++;
            }
        }
        map.put("followVsAll",(double) totalFollowTickets / totalTickets);
        map.put("saleVsFollow",(double) totalSaleTickets/totalFollowTickets);
        map.put("saleVsAll",(double)totalSaleTickets/totalTickets);
        map.put("allTickets",(double)totalTickets);
        map.put("totalFollowUp",(double)totalFollowTickets);
        map.put("totalSaleTickets",(double)totalSaleTickets);
        return map;
    }
    @GetMapping("/followUpByDate/{userId}")
    public Map<String, Object> numberOfFollowUpTicketsByDate(@PathVariable int userId) {
        // Initialize the response list and map
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();

        // Retrieve the results from the repository
        List<Object[]> results = uploadTicketRepo.findDateAndTicketCountAndCommentsWithSenderNameByStatusFollowAndAssigntouser(userId);

        // Process the results to format them as needed
        for (Object[] result : results) {
            Map<String, Object> entry = new HashMap<>();
            String date = (String) result[0];
            Integer count = ((Number) result[1]).intValue(); // Convert the count to Integer
            String comments = (String) result[2];
            entry.put("date", date);
            entry.put("no of tickets", count);
            entry.put("comments", comments);
            responseList.add(entry);
        }

        // Add the response list to the map with the key "response"
        map.put("response", responseList);

        // Return the map as the response
        return map;
    }
//user report
    @PostMapping("/totalassigntickets")
    public List<Map<String,String>> totalAssignTicketsToUserInSelectedWeek(@RequestBody Map<String,String> object ){
        int userId = Integer.parseInt(object.get("userId"));
        int weeks =Integer.parseInt(object.get("weeks"));
        LocalDate fromDate =LocalDate.now();
        LocalDate toDate = fromDate.minusDays(weeks*7);
        return uploadTicketRepo.countByAssignDatesBetweenAndAssigntouser(toDate,fromDate,userId);
    }
    @PostMapping("/totalassignticketsmonthly")
    public List<Map<String,String>> totalAssignTicketsToUserInSelectedMonth(@RequestBody Map<String,String> object ){
        int userId = Integer.parseInt(object.get("userId"));
        int weeks =Integer.parseInt(object.get("weeks"));
        LocalDate fromDate =LocalDate.now();
        LocalDate toDate = fromDate.minusDays(weeks*365);
        return uploadTicketRepo.countByAssignMonthAndYear(toDate,fromDate,userId);
    }
    public List<UploadedTicketDto> mapTicketResponse(List<UploadTicket> ticket){
        List<UploadedTicketDto> ticketResponseDtos= new ArrayList<>();
        for (UploadTicket ticketEntity:ticket){
            ticketResponseDtos.add(modelMapper.map(ticketEntity,UploadedTicketDto.class));
        }
        return ticketResponseDtos;
    }


    @GetMapping("/clickToCall/{ticketId}")
    public ResponseEntity<String> clicktoCallByTicketid(@PathVariable String ticketId){
        ResponseEntity<String> response = callServiceHoduCC.clickToCallByTicketIdForUploaded(ticketId);
        String recordingFile =callServiceHoduCC.getRecording(extractCallId(response));
        UploadTicket ticket=uploadTicketRepo.findByUniqueQueryId(ticketId);
        ticket.setRecordingFile(recordingFile);
        ticket.setComment("Called");
        uploadTicketRepo.save(ticket);
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
}
