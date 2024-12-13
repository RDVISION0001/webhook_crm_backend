package com.crm.rdvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

    @Configuration
    public class MailConfig {


        @Bean
        public JavaMailSender otpMailSender(
                @Value("${spring.mail.otp.host}") String host,
                @Value("${spring.mail.otp.port}") int port,
                @Value("${spring.mail.otp.username}") String username,
                @Value("${spring.mail.otp.password}") String password) {
            return configureMailSender(host, port, username, password);
        }

        @Bean
        public JavaMailSender enquiryMailSender(
                @Value("${spring.mail.enquiry.host}") String host,
                @Value("${spring.mail.enquiry.port}") int port,
                @Value("${spring.mail.enquiry.username}") String username,
                @Value("${spring.mail.enquiry.password}") String password) {
            return configureMailSender(host, port, username, password);
        }

        @Bean
        public JavaMailSender invoiceMailSender(
                @Value("${spring.mail.invoice.host}") String host,
                @Value("${spring.mail.invoice.port}") int port,
                @Value("${spring.mail.invoice.username}") String username,
                @Value("${spring.mail.invoice.password}") String password) {
            return configureMailSender(host, port, username, password);
        }

        @Bean
        public JavaMailSender trackingMailSender(
                @Value("${spring.mail.tracking.host}") String host,
                @Value("${spring.mail.tracking.port}") int port,
                @Value("${spring.mail.tracking.username}") String username,
                @Value("${spring.mail.tracking.password}") String password) {
            return configureMailSender(host, port, username, password);
        }

        private JavaMailSender configureMailSender(String host, int port, String username, String password) {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(port);
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));

            return mailSender;
        }
    }



