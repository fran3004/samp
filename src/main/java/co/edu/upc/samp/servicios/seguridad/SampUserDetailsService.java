package co.edu.upc.samp.servicios.seguridad;

import co.edu.upc.samp.entidades.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adaptador de {@link UserDetailsService} para conectar el control de acceso de SAMP con Spring Security.
 */
@Service
public class SampUserDetailsService implements UserDetailsService {

    private final AuthService authService;

    /**
     * Crea el servicio de detalles de usuario respaldado por {@link AuthService}.
     *
     * @param authService servicio de autenticación de SAMP
     */
    public SampUserDetailsService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = authService.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        var authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .toList();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(authorities)
                .build();
    }
}
