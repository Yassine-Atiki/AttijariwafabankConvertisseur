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

/**
 * Configuration Spring Security.
 * - Déclare un filtre de sécurité HTTP.
 * - Définit les utilisateurs en mémoire (démo / POC).
 * - Gère les accès publics vs ressources protégées.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Chaîne de filtres de sécurité.
     * @param http objet de configuration
     * @return SecurityFilterChain configurée
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
                // Ressources statiques libres
                .requestMatchers("/css/**", "/images/**", "/js/**", "/static/**").permitAll()
                // API REST ouvertes (attention: exposées sans authentification)
                .requestMatchers("/api/**").permitAll()
                // Page de login accessible
                .requestMatchers("/login").permitAll()
                // Toute autre requête nécessite authentification
                .anyRequest().authenticated()
            )
            .csrf((csrf) -> csrf
                // Désactivation CSRF pour endpoints utilisés via AJAX / appels programme
                .ignoringRequestMatchers("/api/**", "/convert", "/validate")
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/dashboard", true) // Redirection forcée après succès
            )
            .logout((logout) -> logout
                .permitAll()
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }

    /**
     * Déclaration des utilisateurs en mémoire.
     * (À remplacer par un UserDetailsService persistant en production.)
     */
    @Bean
    public UserDetailsService userDetailsService() {
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

    /**
     * Encodeur BCrypt (fortement recommandé pour les mots de passe).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
