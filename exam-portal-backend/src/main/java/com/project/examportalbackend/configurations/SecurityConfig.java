package com.project.examportalbackend.configurations;

import com.project.examportalbackend.services.implementation.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("SUPER_ADMIN > ADMIN");
        return roleHierarchy;
    }

    private DefaultWebSecurityExpressionHandler webExpressionHandler() {
        DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy());
        return handler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.cors().configurationSource(corsConfigurationSource());

        // CSRF with cookie-based token: JS reads XSRF-TOKEN cookie, sends it as
        // X-XSRF-TOKEN header. Login/register/refresh are exempt (no session yet).
        http.csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers("/api/login", "/api/register", "/api/register/school", "/api/refresh", "/api/logout");

        // Security headers.
        http.headers()
                .frameOptions().deny()
                .contentTypeOptions().and()
                .xssProtection().block(true).and()
                .httpStrictTransportSecurity()
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    .and()
                .referrerPolicy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .contentSecurityPolicy("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; " +
                        "frame-ancestors 'none'; base-uri 'self'");

        http.authorizeRequests()
                .expressionHandler(webExpressionHandler())

                // Health probe for orchestrators/load balancers — public, no DB/auth.
                .antMatchers(HttpMethod.GET, "/health").permitAll()
                .antMatchers("/api/register/school").permitAll()
                .antMatchers("/api/login").permitAll()
                .antMatchers("/api/logout").permitAll()
                .antMatchers("/api/refresh").permitAll()
                .antMatchers("/api/me").authenticated()
                .antMatchers(HttpMethod.GET, "/api/teachers").permitAll()

                .antMatchers(HttpMethod.GET, "/api/platform/**").permitAll()
                .antMatchers("/api/platform/**").hasAuthority("SUPER_ADMIN")

                .antMatchers("/api/profile/**").authenticated()

                .antMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")

                .antMatchers("/api/reports/**").hasAuthority("ADMIN")

                .antMatchers("/api/students/**").hasAuthority("ADMIN")

                .antMatchers(HttpMethod.POST, "/api/category/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.GET, "/api/category/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/category/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/category/**").hasAuthority("SUPER_ADMIN")

                .antMatchers(HttpMethod.POST, "/api/quiz/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.GET, "/api/quiz/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/quiz/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/quiz/**").hasAuthority("SUPER_ADMIN")

                .antMatchers(HttpMethod.GET, "/api/question/**").hasAuthority("ADMIN")
                .antMatchers("/api/question/**").hasAuthority("SUPER_ADMIN")

                .antMatchers(HttpMethod.POST, "/api/quizResult/**").hasAuthority("USER")
                .antMatchers(HttpMethod.GET, "/api/quizResult/all/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/quizResult/by-class/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/quizResult/**").hasAnyAuthority("USER", "ADMIN")

                .antMatchers(HttpMethod.GET, "/api/psychometric-report/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/psychometric-report/**").hasAnyAuthority("USER", "ADMIN")

                // The Mentalist PDF report: same ownership scoping as psychometric-report.
                .antMatchers(HttpMethod.GET, "/api/mentalist-report/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/mentalist-report/**").hasAnyAuthority("USER", "ADMIN")

                .anyRequest().denyAll()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ;

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.userDetailsService(userDetailsServiceImpl).passwordEncoder(passwordEncoder());
    }
}
