package uk.co.nstauthority.licensingmanagementservice.phasedrelease;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Default-deny gate for the phased go-live. For each request it resolves the handling controller's phase via
 * {@link PhasedReleasePolicy} and returns a plain 404 if that phase is off (or the controller is not classified) —
 * hiding the feature entirely rather than advertising it with a 403.
 *
 * <p>See {@code documentation/adr/0008-phased-go-live-feature-flag.md}.</p>
 */
@Component
public class PhasedReleaseInterceptor implements HandlerInterceptor {

  private final FeatureFlagService featureFlagService;

  public PhasedReleaseInterceptor(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // Only controller handler methods are gated; static resources and other handlers pass through.
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    var phase = PhasedReleasePolicy.phaseFor(handlerMethod.getBeanType());
    if (phase.isPresent() && featureFlagService.isEnabled(phase.get())) {
      return true;
    }

    // Unmapped controller, or its phase is not switched on → hide it.
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
}
