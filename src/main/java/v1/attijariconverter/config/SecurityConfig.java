package v1.attijariconverter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
                .requestMatchers("/css/**", "/images/**", "/js/**", "/static/**").permitAll()
                .requestMatchers("/api/**").permitAll() // Autoriser l'accès aux API REST
                .requestMatchers("/login").permitAll()
                .anyRequest().authenticated()
            )
            .csrf((csrf) -> csrf
                .ignoringRequestMatchers("/api/**", "/convert", "/validate") // Désactiver CSRF pour les API REST et conversion
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/dashboard", true)
            )
            .logout((logout) -> logout
                .permitAll()
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Créer plusieurs utilisateurs avec des mots de passe personnalisés
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("attijari2025"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.builder()
                .username("yassineatiki")
                .password(passwordEncoder().encode("attijari2025"))
                .roles("USER")
                .build();

        UserDetails user2 = User.builder()
                .username("aymanhilal")
                .password(passwordEncoder().encode("attijari2025"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user, user2);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
