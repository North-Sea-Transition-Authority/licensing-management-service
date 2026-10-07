package uk.co.nstauthority.licensingmanagementservice.caseevent.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CaseNotePayload.class, name = "case-note"),
    @JsonSubTypes.Type(value = CaseAllocationPayload.class, name = "case-allocation")
})
public sealed interface CaseEventPayload permits CaseNotePayload, CaseAllocationPayload {
}
