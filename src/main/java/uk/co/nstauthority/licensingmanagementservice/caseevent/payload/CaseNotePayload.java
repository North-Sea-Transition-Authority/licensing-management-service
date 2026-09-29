package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

import java.util.UUID;

public record CaseNotePayload(UUID caseNoteId) implements CaseEventPayload {
}
