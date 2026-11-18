package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.util.RequestUtil;

/**
 * Resolves a {@link CorrectionPosition} from the {@value CORRECTION_ID} and {@value LICENCE_POSITION_ID} path
 * variables. The position is only found when it is on that correction's timeline, so a position from another licence
 * is not found.
 */
@Component
public class CorrectionPositionArgumentResolver implements HandlerMethodArgumentResolver {

  public static final String CORRECTION_ID = "correctionId";
  public static final String LICENCE_POSITION_ID = "licencePositionId";

  private final LicenceCorrectionService licenceCorrectionService;
  private final CorrectionPositionService correctionPositionService;

  CorrectionPositionArgumentResolver(
      LicenceCorrectionService licenceCorrectionService,
      CorrectionPositionService correctionPositionService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.correctionPositionService = correctionPositionService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(CorrectionPosition.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory
  ) {
    var request = ((ServletWebRequest) webRequest).getRequest();

    try {
      var correctionId = getRequiredId(request, CORRECTION_ID);
      var licencePositionId = getRequiredId(request, LICENCE_POSITION_ID);

      return licenceCorrectionService.findById(correctionId)
          .flatMap(correction -> correctionPositionService.findCorrectionPosition(correction, licencePositionId))
          .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.NOT_FOUND,
              "No position %s on correction %s".formatted(licencePositionId, correctionId)
          ));
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UUID parse error", e);
    }
  }

  private static UUID getRequiredId(HttpServletRequest request, String pathVariableName) {
    return RequestUtil.getId(request, pathVariableName)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Missing required %s param".formatted(pathVariableName)
        ));
  }
}
