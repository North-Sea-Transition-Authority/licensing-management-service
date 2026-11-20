package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

import java.util.List;
import java.util.UUID;

public record DspCorrectedPayload(List<UUID> replacedFileIds, List<UUID> correctedFileIds) implements CaseEventPayload {

  public DspCorrectedPayload {
    replacedFileIds = replacedFileIds == null ? List.of() : replacedFileIds;
    correctedFileIds = correctedFileIds == null ? List.of() : correctedFileIds;
  }
}
