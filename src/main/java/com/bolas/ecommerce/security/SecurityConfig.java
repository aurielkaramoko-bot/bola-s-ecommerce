package com.bolas.ecommerce.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final BolasAuthenticationSuccessHandler successHandler;
    private final BolasAuthenticationFailureHandler failureHandler;
    private final RateLimitingFilter rateLimitingFilter;
    private final SensitiveCacheControlFilter sensitiveCacheControlFilter;
    private final Environment environment;

    public SecurityConfig(BolasAuthenticationSuccessHandler successHandler,
                          BolasAuthenticationFailureHandler failureHandler,
                          RateLimitingFilter rateLimitingFilter,
                          SensitiveCacheControlFilter sensitiveCacheControlFilter,
                          Environment environment) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.rateLimitingFilter = rateLimitingFilter;
        this.sensitiveCacheControlFilter = sensitiveCacheControlFilter;
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");
        RequestMatcher h2Matcher = new AntPathRequestMatcher("/h2-console/**");

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .ignoringRequestMatchers(request -> !isProdProfile() && h2Matcher.matches(request))
                )
                .headers(headers -> {
                    headers.contentTypeOptions(with -> {});
                    headers.frameOptions(frame -> frame.deny());
                    headers.xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; "
                                    + "img-src 'self' data: https:; "
                                    + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://maps.googleapis.com; "
                                    + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                                    + "font-src 'self' https://cdn.jsdelivr.net https://fonts.gstatic.com; "
                                    + "connect-src 'self' https://maps.googleapis.com; "
                                    + "frame-ancestors 'none'; "
                                    + "object-src 'none';"));
                    headers.referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter(
                            "Permissions-Policy", "microphone=(), camera=(), payment=(), usb=()")); // geolocation autorisée pour /livreur
                    headers.cacheControl(cache -> {});
                    if (isProdProfile()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true));
                    }
                });

        if (isProdProfile()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        new AntPathRequestMatcher("/"),
                        new AntPathRequestMatcher("/products"),
                        new AntPathRequestMatcher("/products/**"),
                        new AntPathRequestMatcher("/categories"),
                        new AntPathRequestMatcher("/categories/**"),
                        new AntPathRequestMatcher("/tracking"),
                        new AntPathRequestMatcher("/tracking/**"),
                        new AntPathRequestMatcher("/contact"),
                        new AntPathRequestMatcher("/cart"),
                        new AntPathRequestMatcher("/cart/**"),
                        new AntPathRequestMatcher("/api/public/**"),
                        new AntPathRequestMatcher("/api/livreur/**"),
                        new AntPathRequestMatcher("/livreur/**"),
                        new AntPathRequestMatcher("/css/**"),
                        new AntPathRequestMatcher("/js/**"),
                        new AntPathRequestMatcher("/images/**"),
                        new AntPathRequestMatcher("/uploads/**"),
                        new AntPathRequestMatcher("/webjars/**"),
                        new AntPathRequestMatcher("/admin/login"),
                        new AntPathRequestMatcher("/admin/login-error"),
                        new AntPathRequestMatcher("/error")
                ).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).access((authentication, context) ->
                        new org.springframework.security.authorization.AuthorizationDecision(!isProdProfile()))
                .requestMatchers(
                        new AntPathRequestMatcher("/admin"),
                        new AntPathRequestMatcher("/admin/**")
                ).hasRole("ADMIN")
                .anyRequest().permitAll()
        );

        http.formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout", "POST"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
        );

        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(sensitiveCacheControlFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
