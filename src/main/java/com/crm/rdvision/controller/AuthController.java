package com.crm.rdvision.controller;

import com.crm.rdvision.config.JwtHelper;
import com.crm.rdvision.dto.RoleDto;
import com.crm.rdvision.dto.TrackingDto;
import com.crm.rdvision.dto.UserDto;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.OrderRepo;
import com.crm.rdvision.repository.RoleRepo;
import com.crm.rdvision.repository.TicketRepo;
import com.crm.rdvision.repository.UserRepo;
import com.crm.rdvision.service.EmailService;
import com.crm.rdvision.service.EmployeeAttendanceService;
import com.crm.rdvision.service.StripeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private JwtHelper helper;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmployeeAttendanceService employeeAttendanceService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private TicketRepo ticketRepo;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtRequest request) {
        logger.info("Login API Called with email: {}", request.getEmail());
        Optional<User> user = userRepo.findByEmail(request.getEmail());
        if(user.isPresent()){
            if(user.get().getUserStatus() == 'F'){
                logger.info("User {} is Deactivated by Admin", request.getEmail());
                return ResponseEntity.badRequest().body("User Deactivated Please Contact to Administrator");

            } else {
                if(request.getLogInOtp() == user.get().getLogInOtp()){
                    Duration duration = Duration.between(user.get().getTimeWhenOtpGenerated(), LocalDateTime.now());
                    if(duration.getSeconds() > 120){
                        logger.info("OTP for user {} is expired", request.getEmail());
                        return ResponseEntity.badRequest().body("OTP Is Expired");
                    } else {
                        try {
                            doAuthenticate(request.getEmail(), request.getPassword());
                            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
                            String token = helper.generateToken(userDetails);
                            String refreshToken = helper.generateRefreshToken(userDetails);
                            UserDto userDto = modelMapper.map(userDetails, UserDto.class);
                            Optional<Role> roleOptional = roleRepo.findById(userDto.getRoleId());

                            if (roleOptional.isPresent()) {
                                RoleDto roleDto = modelMapper.map(roleOptional.get(), RoleDto.class);
                                userDto.setRoleDto(roleDto);
                                ResponseEntity<EmployeeAttendance> employeeAttendance = employeeAttendanceService.employeeLogin(user.get().getUserId());
                                JwtResponse response = JwtResponse.builder()
                                        .jwtToken(token)
                                        .refreshToken(refreshToken)
                                        .user(userDto).build();
                                response.setAttendanceId(Objects.requireNonNull(employeeAttendance.getBody()).getAttendanceId());

                                logger.info("User {} logged in successfully", request.getEmail());
                                return new ResponseEntity<>(response, HttpStatus.OK);

                            } else {
                                logger.error("Role not found for user {}", request.getEmail());
                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                            }
                        } catch (BadCredentialsException e) {
                            logger.error("Invalid credentials for user {}", request.getEmail());
                            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                        } catch (Exception e) {
                            logger.error("Internal server error for user {}", request.getEmail(), e);
                            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                } else {
                    logger.info("OTP does not match for user {}", request.getEmail());
                    return ResponseEntity.badRequest().body("OTP Does not match");
                }
            }
        } else {
            logger.info("Email {} is not registered", request.getEmail());
            return ResponseEntity.badRequest().body("Your email is not registered, contact to Administrator!");
        }
    }

    @PostMapping("/generateOtp")
    public ResponseEntity<?> generateOtp(@RequestBody JwtRequest jwtRequest){
        logger.info("Generate OTP Called by {}", jwtRequest.getEmail());
        Optional<User> user = userRepo.findByEmail(jwtRequest.getEmail());
        if(user.isPresent()){
            logger.info("User {} found", jwtRequest.getEmail());
            if(user.get().getUserStatus() != 'F'){
                logger.info("User {} is activated", jwtRequest.getEmail());
                doAuthenticate(jwtRequest.getEmail(), jwtRequest.password);
                Random random = new Random();
                int otp = random.nextInt(1000000);
                user.get().setLogInOtp(otp);
                System.out.println(otp);
                user.get().setTimeWhenOtpGenerated(LocalDateTime.now());
                user.get().setOnBreak(false);
                String message = "This is your verification email use to login on RD Vision, \n your OTP is: " + otp + "\n" + "Don't share this OTP with anyone\n" + "RDVISION";
                userRepo.save(modelMapper.map(user, User.class));
                logger.info("OTP generated and email sent to {}", jwtRequest.getEmail());
                String messagecc ="This is otp "+otp+ " for login of "+jwtRequest.getEmail()+ "user please use this to login this user only ";
                emailService.sendOtp("davishernandez728@gmail.com","OTP for login of "+jwtRequest.getEmail(),messagecc);
                return ResponseEntity.ok(emailService.sendOtp(jwtRequest.getEmail(), "Login OTP for RDVision", message));
            } else {
                logger.info("User {} is deactivated", jwtRequest.getEmail());
                return ResponseEntity.badRequest().body("This user is deactivated, please contact to administrator");
            }
        } else {
            logger.info("User {} not found", jwtRequest.getEmail());
            return ResponseEntity.badRequest().body("Your email is not registered, contact to Administrator!");
        }
    }

    @GetMapping("/otpForPassword/{email}")
    public ResponseEntity<?> sendOtpForForgotPassword(@PathVariable String email){
        logger.info("Generate OTP for Forgot Password by: {}", email);
        Optional<User> user = userRepo.findByEmail(email);
        if(user.isPresent()){
            Random random = new Random();
            int otp = 100000 + random.nextInt(900000);
            user.get().setLogInOtp(otp);
            user.get().setTimeWhenOtpGenerated(LocalDateTime.now());
            userRepo.save(modelMapper.map(user, User.class));
            String message = "Use this OTP to reset your password, \n your OTP is: " + otp + "\n" + "Don't share this OTP with anyone\n" + "RDVISION";
            logger.info("OTP for password reset generated and email sent to {}", email);
            return ResponseEntity.ok(emailService.sendOtp(email, "OTP for Reset Password: RDVISION", message));
        } else {
            logger.info("Email {} is not registered", email);
            return ResponseEntity.badRequest().body("Email is not registered");
        }
    }

    @PostMapping("/forgotPassword")
    public String resetPassword(@RequestBody Map<String, String> object){
        logger.info("Forgot password called by: {}", object.get("email"));
        Optional<User> user = userRepo.findByEmail(object.get("email"));

        if(user.isPresent()){
            Duration duration = Duration.between(user.get().getTimeWhenOtpGenerated(), LocalDateTime.now());
            if(user.get().getLogInOtp() == Integer.parseInt(object.get("otp"))){
                if(duration.getSeconds() > 120){
                    logger.info("OTP expired for user {}", object.get("email"));
                    return "OTP is Expired, Send again";
                } else {
                    user.get().setPassword(passwordEncoder.encode(object.get("newPassword")));
                    userRepo.save(modelMapper.map(user, User.class));
                    logger.info("Password reset successful for user {}", object.get("email"));
                    return "Password Changed, Go and Login";
                }
            } else {
                logger.info("Incorrect OTP for user {}", object.get("email"));
                return "OTP is incorrect";
            }
        } else {
            logger.info("User not found for email {}", object.get("email"));
            return "User Not Found";
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@RequestBody String refreshToken) {
        logger.info("Refresh API called");
        try {
            String username = helper.getUsernameFromToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (helper.validateToken(refreshToken, userDetails)) {
                String newToken = helper.generateToken(userDetails);
                UserDto userDto = modelMapper.map(userDetails, UserDto.class);
                Optional<Role> roleOptional = roleRepo.findById(userDto.getRoleId());

                if (roleOptional.isPresent()) {
                    RoleDto roleDto = modelMapper.map(roleOptional.get(), RoleDto.class);
                    userDto.setRoleDto(roleDto);

                    JwtResponse response = JwtResponse.builder()
                            .jwtToken(newToken)
                            .refreshToken(refreshToken)
                            .user(userDto).build();

                    logger.info("Token refreshed successfully for user {}", username);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    logger.error("Role not found for user {}", username);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } else {
                logger.info("Invalid refresh token for user {}", username);
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            logger.error("Error while refreshing token", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void doAuthenticate(String email, String password) {
        logger.info("Authenticating user: {}", email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, password);
        manager.authenticate(authentication);
        logger.info("User {} authenticated successfully", email);
    }

    @Autowired
    private RestTemplate restTemplate;

    private final String API_URL = "https://api.17track.net/track/v2.2/gettrackinfo";

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<?> trackPackage(@PathVariable String trackingNumber) {
        String apiKey = "F28E9A9EEE74E07F8959451F6C0E470D"; // Replace with your actual API token

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("17token", apiKey);

        // Create the request body as a list of TrackingDto
        List<TrackingDto> number = new ArrayList<>();
        TrackingDto trackingDto = new TrackingDto();
        trackingDto.setNumber(trackingNumber);
        number.add(trackingDto);

        // Convert the list to a JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(number);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid request body");
        }

        // Set up the request with the JSON body
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Call the Track17 API
        ResponseEntity<Object> response = restTemplate.exchange(
                API_URL,
                HttpMethod.POST,
                entity,
                Object.class
        );

        // Return the same data returned by the Track17 API
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    @GetMapping("/track-ip/{ticketId}")
    public String getClientIp(HttpServletRequest request,@PathVariable String ticketId) {
        String clientIp = extractClientIp(request);
        // You can log this IP, save it to a database, etc.
        return "Client IP address: " + clientIp;
    }

    private String extractClientIp(HttpServletRequest request) {
        String[] headerKeys = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerKeys) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                // In case of multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }
        }

        // Fallback to request.getRemoteAddr()
        String remoteAddr = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            // Optional: Replace IPv6 loopback with IPv4 loopback
            return "127.0.0.1";
        }
        return remoteAddr;
    }

}


