package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.argumentresolver.HandlerMethodEntityResolver;

@Component
public class LicencePositionStagedPayloadArgumentResolver implements HandlerMethodEntityResolver<LicencePositionStagedPayload> {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  LicencePositionStagedPayloadArgumentResolver(LicencePositionStagedPayloadService licencePositionStagedPayloadService) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  @Override
  public Class<LicencePositionStagedPayload> entityClass() {
    return LicencePositionStagedPayload.class;
  }

  @Override
  public Optional<LicencePositionStagedPayload> resolve(String id) {
    return licencePositionStagedPayloadService.findById(UUID.fromString(id));
  }
}