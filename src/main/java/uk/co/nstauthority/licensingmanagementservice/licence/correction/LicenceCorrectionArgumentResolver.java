package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.argumentresolver.HandlerMethodEntityResolver;

@Component
public class LicenceCorrectionArgumentResolver implements HandlerMethodEntityResolver<LicenceCorrection> {

  private final LicenceCorrectionService licenceCorrectionService;

  LicenceCorrectionArgumentResolver(LicenceCorrectionService licenceCorrectionService) {
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public Class<LicenceCorrection> entityClass() {
    return LicenceCorrection.class;
  }

  @Override
  public Optional<LicenceCorrection> resolve(String id) {
    return licenceCorrectionService.findById(UUID.fromString(id));
  }
}
