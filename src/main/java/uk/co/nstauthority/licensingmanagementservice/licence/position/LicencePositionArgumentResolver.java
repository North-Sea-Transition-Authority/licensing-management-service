package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.argumentresolver.HandlerMethodEntityResolver;

@Component
public class LicencePositionArgumentResolver implements HandlerMethodEntityResolver<LicencePosition> {

  private final LicencePositionService licencePositionService;

  LicencePositionArgumentResolver(LicencePositionService licencePositionService) {
    this.licencePositionService = licencePositionService;
  }

  @Override
  public Class<LicencePosition> entityClass() {
    return LicencePosition.class;
  }

  @Override
  public Optional<LicencePosition> resolve(String id) {
    return licencePositionService.findById(UUID.fromString(id));
  }
}