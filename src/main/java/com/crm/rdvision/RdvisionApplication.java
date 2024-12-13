package com.crm.rdvision;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.modelmapper.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;

@EnableWebMvc
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "RdVision CRM APIs",version = "1.0",description = "manages all upcomming leads by the users"),servers = @Server(url = "my-ce2-instance",description = "crm url"))
public class RdvisionApplication {
	private static  final Logger logger = LoggerFactory.getLogger(RdvisionApplication.class);
	public static void genkey(String[] args) {
		SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
		String base64Key = Encoders.BASE64.encode(key.getEncoded());
		System.out.println(base64Key);
	}
	public static void main(String[] args) {
		SpringApplication.run(RdvisionApplication.class, args);
          logger.info("Application Started Successfully at Port 8080");
	}
	@Bean
	ModelMapper modelMapper(){
		return new ModelMapper();
     }

}
