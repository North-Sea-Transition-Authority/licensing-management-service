package uk.co.nstauthority.licensingmanagementservice.exception;

import jakarta.persistence.EntityNotFoundException;
import java.io.Serial;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The entity could not be found")
public class LmsEntityNotFoundException extends EntityNotFoundException {

  @Serial
  private static final long serialVersionUID = 4443343368848518168L;

  public LmsEntityNotFoundException(String message) {
    super(message);
  }

  public LmsEntityNotFoundException(String entityName, String id) {
    this("Could not find %s with id: %s".formatted(entityName, id));
  }

  public LmsEntityNotFoundException(String entityName, Integer id) {
    this(entityName, String.valueOf(id));
  }

  public LmsEntityNotFoundException(String entityName, UUID uuid) {
    this(entityName, uuid.toString());
  }
}
