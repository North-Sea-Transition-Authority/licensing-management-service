package uk.co.nstauthority.licensingmanagementservice.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.servlet.HandlerMapping;

public class RequestUtil {

  private RequestUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  @SuppressWarnings("unchecked")
  public static Optional<UUID> getId(HttpServletRequest request, String pathVariableName) {
    return Optional.ofNullable(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .map(o -> (Map<String, String>) o)
        .map(map -> map.get(pathVariableName))
        .map(UUID::fromString);
  }
}
