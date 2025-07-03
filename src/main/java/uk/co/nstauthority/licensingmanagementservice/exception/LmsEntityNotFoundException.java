package uk.co.nstauthority.licensingmanagementservice.exception;

import jakarta.persistence.EntityNotFoundException;
import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The entity could not be found")
public class LmsEntityNotFoundException extends EntityNotFoundException {

  @Serial
  private static final long serialVersionUID = 4443343368848518168L;

  public LmsEntityNotFoundException(String message) {
    super(message);
  }

}
