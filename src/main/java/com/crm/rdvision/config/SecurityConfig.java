package com.crm.rdvision.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String[] WHITE_LIST_URL = { "/api/v1/auth/**", "/v2/api-docs", "/v3/api-docs",
            "/v3/api-docs/**", "/swagger-resources", "/swagger-resources/**", "/configuration/ui",
            "/configuration/security", "/swagger-ui/**", "/webjars/**", "/swagger-ui.html", "/api/auth/**",
            "/api/test/**", "/authenticate", "/third_party_api/ticket/getTicket","/indiamart/**" };



    @Autowired
    private JwtAuthenticationEntryPoint point;
    @Autowired
    private JwtAuthenticationFilter filter;

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserDetailsService userDetailService;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .authorizeRequests()
                .requestMatchers("/test").authenticated()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/track17/**").permitAll()
                .requestMatchers("/invoice/create-checkout-session/**").permitAll()
                .requestMatchers("/address/getAddress/**").permitAll()
                .requestMatchers("/invoice/viewInvoice/**").permitAll()
                .requestMatchers("/stripe/**").permitAll()
                .requestMatchers("/product/getAllProducts").permitAll()
                .requestMatchers("/address/createAddress").permitAll()
                .requestMatchers("/order/getOrderDetails/**").permitAll()
                .requestMatchers("/auth/otpForPassword/**").permitAll()
                .requestMatchers("/auth/forgotPassword").permitAll()
                .requestMatchers(WHITE_LIST_URL).permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/department/**").authenticated()
                .requestMatchers("/invoice/**").authenticated()
                .requestMatchers("/order/**").authenticated()
                .requestMatchers("/product/**").authenticated()
                .requestMatchers("/role/**").authenticated()
                .requestMatchers("/team/**").authenticated()
                .requestMatchers("/user/**").authenticated()
                .requestMatchers("/address/**").authenticated()
                .requestMatchers("/third_party_api/ticket/**").authenticated()
                .anyRequest()
                .authenticated()
                .and().exceptionHandling(ex -> ex.authenticationEntryPoint(point))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;

    }
    @Bean
    public FilterRegistrationBean coresFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.addAllowedOriginPattern("*");
        corsConfiguration.addAllowedHeader("Authorization");
        corsConfiguration.addAllowedHeader("Content-Type");
        corsConfiguration.addAllowedHeader("Accept");
        corsConfiguration.addAllowedMethod("POST");
        corsConfiguration.addAllowedMethod("GET");
        corsConfiguration.addAllowedMethod("DELETE");
        corsConfiguration.addAllowedMethod("PUT");
        corsConfiguration.addAllowedMethod("OPTIONS");
        corsConfiguration.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", corsConfiguration);

        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));

        bean.setOrder(-110);

        return bean;
    }
}
