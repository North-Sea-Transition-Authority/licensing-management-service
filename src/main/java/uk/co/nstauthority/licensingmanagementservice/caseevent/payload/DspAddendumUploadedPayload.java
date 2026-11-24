package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

import java.util.List;
import java.util.UUID;

public record DspAddendumUploadedPayload(List<UUID> fileIds) implements CaseEventPayload {

  public DspAddendumUploadedPayload {
    fileIds = fileIds == null ? List.of() : fileIds;
  }
}
