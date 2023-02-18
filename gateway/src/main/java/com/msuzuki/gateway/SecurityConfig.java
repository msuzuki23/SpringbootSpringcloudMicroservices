package com.msuzuki.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Configuration
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeExchange()
                .pathMatchers("/headerrouting/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/eureka/**").permitAll()
                .pathMatchers("/oauth2/**").permitAll()
                .pathMatchers("/login/**").permitAll()
                .pathMatchers("/error/**").permitAll()
                .pathMatchers("/openapi/**").permitAll()
                .pathMatchers("/webjars/**").permitAll()
                .pathMatchers("/auth-server/**").permitAll()
                .pathMatchers("/config/**").permitAll()
                .anyExchange().authenticated()
                .and()
                .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);
        return http.build();
    }
}
