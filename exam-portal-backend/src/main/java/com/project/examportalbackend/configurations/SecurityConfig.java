package com.project.examportalbackend.configurations;

import com.project.examportalbackend.services.implementation.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SUPER_ADMIN inherits everything ADMIN can do; ADMIN and USER stay distinct
     * (admins are not students and vice versa).
     */
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

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.cors();
        http.csrf().disable()
                .authorizeRequests()
                .expressionHandler(webExpressionHandler())

                // Health probe for orchestrators/load balancers — public, no DB/auth.
                .antMatchers(HttpMethod.GET, "/health").permitAll()
                .antMatchers("/api/register/school").permitAll()
                .antMatchers("/api/login").permitAll()
                .antMatchers(HttpMethod.GET, "/api/teachers").permitAll()
                // Platform branding: logo is public (header on every page);
                // only a SUPER_ADMIN may change it.
                .antMatchers(HttpMethod.GET, "/api/platform/**").permitAll()
                .antMatchers("/api/platform/**").hasAuthority("SUPER_ADMIN")

                // Any authenticated user may manage their own profile.
                .antMatchers("/api/profile/**").authenticated()

                // Super Admin management surface.
                .antMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")

                // Reporting is admin-only (super admin inherits via role hierarchy).
                .antMatchers("/api/reports/**").hasAuthority("ADMIN")

                // Teachers manage their own students.
                .antMatchers("/api/students/**").hasAuthority("ADMIN")

                // Content (classes/quizzes/questions) is created and managed ONLY by
                // SUPER_ADMIN. Schools (ADMIN) get read access to assign classes; students
                // read to browse/take. hasAuthority("SUPER_ADMIN") excludes plain ADMIN.
                .antMatchers(HttpMethod.POST, "/api/category/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.GET, "/api/category/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/category/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/category/**").hasAuthority("SUPER_ADMIN")

                // Student exam delivery (answers stripped) lives under /api/quiz/**.
                .antMatchers(HttpMethod.POST, "/api/quiz/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.GET, "/api/quiz/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/quiz/**").hasAuthority("SUPER_ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/quiz/**").hasAuthority("SUPER_ADMIN")

                // Question CRUD (answers included): SUPER_ADMIN only. Reads allow ADMIN
                // (super admin via hierarchy) so content can be viewed; students never call it.
                .antMatchers(HttpMethod.GET, "/api/question/**").hasAuthority("ADMIN")
                .antMatchers("/api/question/**").hasAuthority("SUPER_ADMIN")

                .antMatchers(HttpMethod.POST, "/api/quizResult/**").hasAuthority("USER")
                .antMatchers(HttpMethod.GET, "/api/quizResult/all/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/quizResult/by-class/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/quizResult/**").hasAnyAuthority("USER", "ADMIN")

                // Psychometric report: students read their own, teachers their
                // students' (per-row scoping in the service). POST = generate AI summary.
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
