package com.fooddelivery.fooddeliverybackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
            		
                // ✅ ALLOW IMAGES
                .requestMatchers("/images/**").permitAll()
                

                // ✅ ALLOW AUTH APIs
                .requestMatchers("/api/users/**").permitAll()

                // ✅ ALLOW FOOD API
                .requestMatchers("/api/foods/**").permitAll()
                
                .requestMatchers("/api/recommendation/**").permitAll()
                
                .requestMatchers("/api/chatbot/**").permitAll()


                // 🔒 everything else secured
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
