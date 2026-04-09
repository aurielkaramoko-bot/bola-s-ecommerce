package com.bolas.ecommerce.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final BolasAuthenticationSuccessHandler successHandler;
    private final BolasAuthenticationFailureHandler failureHandler;
    private final RateLimitingFilter rateLimitingFilter;
    private final SensitiveCacheControlFilter sensitiveCacheControlFilter;
    private final AdminUserDetailsService adminUserDetailsService;
    private final Environment environment;

    // On a enlevé temporairement GoogleOAuth2SuccessHandler et BolaOAuth2UserService

    public SecurityConfig(BolasAuthenticationSuccessHandler successHandler,
                          BolasAuthenticationFailureHandler failureHandler,
                          RateLimitingFilter rateLimitingFilter,
                          SensitiveCacheControlFilter sensitiveCacheControlFilter,
                          AdminUserDetailsService adminUserDetailsService,
                          Environment environment) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.rateLimitingFilter = rateLimitingFilter;
        this.sensitiveCacheControlFilter = sensitiveCacheControlFilter;
        this.adminUserDetailsService = adminUserDetailsService;
        this.environment = environment;
    }

    @Bean
    public AuthenticationManager adminAuthenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    public DaoAuthenticationProvider adminAuthProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(adminUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
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
                                    + "media-src 'self' https://res.cloudinary.com; "
                                    + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://maps.googleapis.com; "
                                    + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                                    + "font-src 'self' https://cdn.jsdelivr.net https://fonts.gstatic.com; "
                                    + "connect-src 'self' https://maps.googleapis.com; "
                                    + "frame-ancestors 'none'; "
                                    + "object-src 'none';"));
                    headers.referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter(
                            "Permissions-Policy", "microphone=(), camera=(), payment=(), usb=()"));
                    headers.cacheControl(cache -> {});
                    if (isProdProfile()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true));
                    }
                });

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/products", "/products/**", "/categories", "/categories/**",
                        "/tracking", "/tracking/**", "/contact", "/cart", "/cart/**",
                        "/api/public/**", "/api/livreur/**", "/livreur/**",
                        "/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**",
                        "/admin/login", "/admin/login-process", "/admin/login-error",
                        "/customer/login", "/customer/signup", "/customer/logout",
                        "/vendor/login", "/vendor/login-process", "/vendor/register",
                        "/boutiques", "/boutiques/**", "/error"
                ).permitAll()
                .requestMatchers("/h2-console/**").access((authentication, context) ->
                        new org.springframework.security.authorization.AuthorizationDecision(!isProdProfile()))
                .requestMatchers("/vendor", "/vendor/**", "/chat/**", "/report").permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
        );

        http.formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login-process")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
        );

        http.authenticationProvider(adminAuthProvider(passwordEncoder()));

        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout", "POST"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
        );

        // OAuth2 Google désactivé temporairement pour permettre le démarrage
        // http.oauth2Login(...) 

        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(sensitiveCacheControlFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}