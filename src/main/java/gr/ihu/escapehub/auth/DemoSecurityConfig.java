package gr.ihu.escapehub.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Local QA only. Replace with the team's authentication in the production profile. */
@Configuration
@Profile("demo")
public class DemoSecurityConfig {
    @Bean
    UserDetailsService demoUsers() {
        var users = new InMemoryUserDetailsManager();
        for (int i = 1; i <= 4; i++) {
            users.createUser(User.withUsername("player" + i)
                    .password("{noop}demo-pass").roles("PLAYER").build());
        }
        return users;
    }
    @Bean
    SecurityFilterChain demoSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/api/csrf", "/actuator/health").permitAll()
                .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login.html").loginProcessingUrl("/login")
                        .defaultSuccessUrl("/profile.html", true).permitAll())
                .build();
    }
}
