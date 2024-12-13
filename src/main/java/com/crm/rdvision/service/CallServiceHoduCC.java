package com.crm.rdvision.service;

import com.crm.rdvision.dto.JobRequestDto;
import com.crm.rdvision.entity.HoduCall;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.UploadTicket;
import com.crm.rdvision.repository.HoduCallRepo;
import com.crm.rdvision.repository.TicketRepo;
import com.crm.rdvision.repository.UploadTicketRepo;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

@Service
public class CallServiceHoduCC {
    private  RestTemplate restTemplate;

    public CallServiceHoduCC(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    HoduCallRepo hoduCallRepo;

    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    UploadTicketRepo uploadTicketRepo;
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(password.getBytes());

            // Convert byte array to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public void sendJobRequestDto(JobRequestDto jobRequestDto) {
        String apiUrl = "http://ccn.cloud-connect.in/HoduCC_api/v1.4/addJobNumber";
        List<HoduCall> hoduCall =hoduCallRepo.findAll();
        jobRequestDto.setTenant_id(hoduCall.get(0).getHoduTanentId());
        jobRequestDto.setToken(hoduCall.get(0).getHoduToken());
        jobRequestDto.setJob_id("67");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<JobRequestDto> requestEntity = new HttpEntity<>(jobRequestDto, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);
        System.out.println("Response Body of auto call: " + response.getBody());
    }

    public String getRecording(String call_id) {
        String url = "http://ccn.cloud-connect.in/HoduCC_api/v1.4/getRecording";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        Map<String, String> requestBody = new HashMap<>();
        List<HoduCall> hoduCall =hoduCallRepo.findAll();
        requestBody.put("tenant_id", hoduCall.get(0).getHoduTanentId());
        requestBody.put("call_id", call_id);
        requestBody.put("token", hoduCall.get(0).getHoduToken());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String recordingFile = response.getBody().formatted();
        return (recordingFile.split(",")[3]).replace("\\", "");
    }

    public ResponseEntity<String> clickToCall(Map<String, String> numberDetails, TicketEntity ticketEntity) {
        int userId =Integer.parseInt(numberDetails.get("userId"));
        HoduCall hoduCall =hoduCallRepo.findByUserId(userId);
        String url = "http://ccn.cloud-connect.in/HoduCC_api/v1.4/clickToCall";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        Object agentId = getAgentList(hoduCall.getHoduToken(),hoduCall.getHoduTanentId(),hoduCall.getAgentUserName());
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("agent_username", hoduCall.getAgentUserName());
        requestBody.put("agent_password", hashPassword(hoduCall.getAgentPassword()));
        requestBody.put("tenant_id", hoduCall.getHoduTanentId());
        requestBody.put("campaign_name", "USA_MANUAL");
        requestBody.put("action", "Call");
        requestBody.put("customer_phone", numberDetails.get("number"));
        requestBody.put("token", hoduCall.getHoduToken());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            autoCall(ticketEntity,agentId);
            String errorMessage = "Client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            return ResponseEntity.status(e.getStatusCode()).body(errorMessage);
        } catch (HttpServerErrorException e) {
            autoCall(ticketEntity,agentId);
            String errorMessage = "Server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
            return ResponseEntity.status(e.getStatusCode()).body(errorMessage);
        } catch (RestClientException e) {
            autoCall(ticketEntity,agentId);
            String errorMessage = "Error during call to HoduCC: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (Exception e) {
            autoCall(ticketEntity,agentId);
            String errorMessage = "Unexpected error: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

    public ResponseEntity<String> clickToCallByTicketId(String ticketId) {
        TicketEntity ticketEntity =ticketRepo.findByUniqueQueryId(ticketId);
        int userId =ticketEntity.getAssigntouser();
        HoduCall hoduCall =hoduCallRepo.findByUserId(userId);
        String url = "http://ccn.cloud-connect.in/HoduCC_api/v1.4/clickToCall";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        Object agentId = getAgentList(hoduCall.getHoduToken(),hoduCall.getHoduTanentId(),hoduCall.getAgentUserName());
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("agent_username", hoduCall.getAgentUserName());
        requestBody.put("agent_password", hashPassword(hoduCall.getAgentPassword()));
        requestBody.put("tenant_id", hoduCall.getHoduTanentId());
        requestBody.put("campaign_name", "USA_MANUAL");
        requestBody.put("action", "Call");
        requestBody.put("customer_phone", ticketEntity.getSenderMobile().replaceAll("[+-]", ""));
        requestBody.put("token", hoduCall.getHoduToken());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        }
    public ResponseEntity<String> clickToCallByTicketIdForUploaded(String ticketId) {
        UploadTicket ticketEntity =uploadTicketRepo.findByUniqueQueryId(ticketId);
        int userId =ticketEntity.getAssigntouser();
        HoduCall hoduCall =hoduCallRepo.findByUserId(userId);
        String url = "http://ccn.cloud-connect.in/HoduCC_api/v1.4/clickToCall";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        Object agentId = getAgentList(hoduCall.getHoduToken(),hoduCall.getHoduTanentId(),hoduCall.getAgentUserName());
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("agent_username", hoduCall.getAgentUserName());
        requestBody.put("agent_password", hashPassword(hoduCall.getAgentPassword()));
        requestBody.put("tenant_id", hoduCall.getHoduTanentId());
        requestBody.put("campaign_name", "USA_MANUAL");
        requestBody.put("action", "Call");
        requestBody.put("customer_phone", 1+ticketEntity.getMobileNumber());
        requestBody.put("token", hoduCall.getHoduToken());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public Object getAgentList(String token, String tenantId, String agentName) {
        String apiUrl = "http://ccn.cloud-connect.in/HoduCC_api/v1.4/getAgentList";
        System.out.println("Provided agents name is :"+agentName);
        try {
            // Prepare URL and connection
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Prepare the JSON body with token and tenant_id
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("token", token);
            jsonInput.put("tenant_id", tenantId);

            // Send the JSON input as the POST request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read the response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            // Assuming agents are in an array under the key "agents"
            if (jsonResponse.has("Agents")) {
                JSONArray agentsArray = jsonResponse.getJSONArray("Agents");
                // Loop through the agents array to find the matching agentName
                for (int i = 0; i < agentsArray.length(); i++) {
                    JSONObject agent = agentsArray.getJSONObject(i);

                    // Assuming the agent username is under the key "agt_uname"
                    if (agent.has("agt_uname") && agentName.equals(agent.getString("agt_uname"))) {
                        // Assuming the agent ID is under the key "agentId"
                        System.out.println(agent.get("agt_id"));
                        return agent.get("agt_id");
                    }
                }
            }

            // If no matching agent is found, return a message indicating so
            return "Agent not found";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    public void autoCall(TicketEntity ticketEntity,Object agentId){
        try {
            JobRequestDto jobRequestDto = new JobRequestDto();
            jobRequestDto.setAgent_id(agentId);
            jobRequestDto.setNumbers(new ArrayList<>());
            JobRequestDto.NumberDetails numberDetails = new JobRequestDto.NumberDetails();
            numberDetails.setJob_fname(ticketEntity.getSenderName());
            numberDetails.setNumber(ticketEntity.getSenderMobile().replaceAll("[+-]", ""));
            jobRequestDto.getNumbers().add(numberDetails);
            System.out.println(jobRequestDto.toString());
            sendJobRequestDto(jobRequestDto);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
