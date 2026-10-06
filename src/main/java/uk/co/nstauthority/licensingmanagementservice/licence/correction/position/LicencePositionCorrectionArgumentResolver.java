package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.argumentresolver.HandlerMethodEntityResolver;

@Component
public class LicencePositionCorrectionArgumentResolver implements HandlerMethodEntityResolver<LicencePositionCorrection> {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  LicencePositionCorrectionArgumentResolver(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @Override
  public Class<LicencePositionCorrection> entityClass() {
    return LicencePositionCorrection.class;
  }

  @Override
  public Optional<LicencePositionCorrection> resolve(String id) {
    return licencePositionCorrectionService.findById(UUID.fromString(id));
  }
}