package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

public record CaseAllocationPayload(Long allocatedToWuaId) implements CaseEventPayload {
}
