package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.argumentresolver.HandlerMethodEntityResolver;

@Component
public class LicencePositionChangeArgumentResolver implements HandlerMethodEntityResolver<LicencePositionChange> {

  private final LicencePositionChangeService licencePositionChangeService;

  LicencePositionChangeArgumentResolver(LicencePositionChangeService licencePositionChangeService) {
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Override
  public Class<LicencePositionChange> entityClass() {
    return LicencePositionChange.class;
  }

  @Override
  public Optional<LicencePositionChange> resolve(String id) {
    return licencePositionChangeService.findById(UUID.fromString(id));
  }
}