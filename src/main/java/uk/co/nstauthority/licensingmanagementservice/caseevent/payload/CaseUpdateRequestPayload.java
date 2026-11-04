package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

import java.time.LocalDate;

public record CaseUpdateRequestPayload(LocalDate updateDeadlineDate, String updateReason) implements CaseEventPayload {
}
