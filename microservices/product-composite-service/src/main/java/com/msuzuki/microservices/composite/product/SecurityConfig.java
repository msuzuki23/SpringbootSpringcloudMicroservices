package com.msuzuki.microservices.composite.product;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.http.HttpMethod.*;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

//    @Bean
//    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        http
//                .csrf().disable()
//                .authorizeExchange()
//                .pathMatchers("/openapi/**").permitAll()
//                .pathMatchers("/webjars/**").permitAll()
//                .pathMatchers("/actuator/**").permitAll()
//                .pathMatchers(POST, "/product-composite/**").hasAuthority("SCOPE_product:write")
//                .pathMatchers(DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write")
//                .pathMatchers(GET, "/product-composite/**").hasAuthority("SCOPE_product:read")
//                .anyExchange().authenticated()
//                .and()
//                .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);
//        return http.build();
//    }
}
