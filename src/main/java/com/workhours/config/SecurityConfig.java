package com.workhours.config;

import com.workhours.entity.Employee;
import com.workhours.mapper.EmployeeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.Resource;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private EmployeeMapper employeeMapper;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return phone -> {
            Employee emp = employeeMapper.selectByPhone(phone);
            if (emp == null) {
                throw new UsernameNotFoundException("手机号不存在: " + phone);
            }
            return new User(
                    emp.getPhone(),
                    emp.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + emp.getRole()))
            );
        };
    }

    @Bean
    public CaptchaFilter captchaFilter() {
        return new CaptchaFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/captcha").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                .requestMatchers("/", "/index").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(captchaFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}