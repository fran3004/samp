package co.edu.upc.samp.servicios.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad web de SAMP con control de acceso por roles:
 * acceso público para búsqueda y comparación, perfil {@code REGISTERED} para cuenta/favoritos/alertas
 * y perfil {@code ADMIN} para administración de tiendas, conectores y usuarios.
 */
@Configuration
public class SecurityConfig {

    /**
     * Provee el codificador BCrypt compartido por la capa web y el modo consola.
     *
     * @return instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura las reglas HTTP para el perfil web (se omite en {@code consola} y {@code scraper}).
     *
     * @param http constructor de filtros de Spring Security
     * @return cadena de filtros configurada
     * @throws Exception si falla la construcción de seguridad
     */
    @Bean
    @Profile("!consola & !scraper")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index", "/resultados", "/producto/**", "/comparar",
                                "/asistente", "/armador-pc", "/escanear", "/estados", "/menus", "/sistema",
                                "/entrar", "/registro",
                                "/api/search/**", "/api/ai/**", "/api/health",
                                "/assets/**", "/css/**", "/js/**", "/images/**",
                                "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cuenta/**", "/favoritos/**", "/alertas/**").hasAnyRole("REGISTERED", "ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/entrar")
                        .loginProcessingUrl("/entrar")
                        .defaultSuccessUrl("/cuenta", false)
                        .failureUrl("/entrar?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/salir")
                        .logoutSuccessUrl("/?salir=true")
                        .permitAll()
                );
        return http.build();
    }
}
