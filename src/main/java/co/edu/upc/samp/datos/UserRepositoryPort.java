package co.edu.upc.samp.datos;

import co.edu.upc.samp.entidades.User;

import java.util.Optional;

/**
 * Puerto de persistencia para usuarios y control de acceso.
 */
public interface UserRepositoryPort extends CrudRepositoryPort<User, Long> {

    /**
     * Busca un usuario registrado por su correo electrónico.
     *
     * @param email correo electrónico normalizado
     * @return usuario encontrado o vacío
     */
    Optional<User> findByEmail(String email);
}
