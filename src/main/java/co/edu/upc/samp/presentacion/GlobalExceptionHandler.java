package co.edu.upc.samp.presentacion;

import co.edu.upc.samp.entidades.excepciones.EntityNotFoundException;
import co.edu.upc.samp.entidades.excepciones.SampException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Manejador global de excepciones de la capa de presentación.
 * Traduce errores específicos de negocio a mensajes claros en español con sugerencia de acción.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Atiende errores de recurso no encontrado mostrando una vista clara con salida.
     *
     * @param ex    excepción de entidad no encontrada
     * @param model modelo de vista
     * @return plantilla de estados/error
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorTitle", ex.getMessage());
        model.addAttribute("errorHint", ex.getUserHint());
        return "estados";
    }

    /**
     * Atiende excepciones controladas del dominio SAMP.
     *
     * @param ex    excepción de negocio
     * @param model modelo de vista
     * @return plantilla de estados/error
     */
    @ExceptionHandler(SampException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleDomainError(SampException ex, Model model) {
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorTitle", ex.getMessage());
        model.addAttribute("errorHint", ex.getUserHint());
        return "estados";
    }
}
