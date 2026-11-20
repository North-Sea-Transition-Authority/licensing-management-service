package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

import java.util.List;
import java.util.UUID;

public record DspUploadedPayload(List<UUID> fileIds) implements CaseEventPayload {

  public DspUploadedPayload {
    fileIds = fileIds == null ? List.of() : fileIds;
  }
}
