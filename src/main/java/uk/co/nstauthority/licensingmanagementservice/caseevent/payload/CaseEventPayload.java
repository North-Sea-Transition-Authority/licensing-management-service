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
    @JsonSubTypes.Type(value = CaseAllocationPayload.class, name = "case-allocation"),
    @JsonSubTypes.Type(value = CaseUpdateRequestPayload.class, name = "case-update-request"),
    @JsonSubTypes.Type(value = CaseUpdateSubmittedPayload.class, name = "case-update-submitted"),
    @JsonSubTypes.Type(value = DspUploadedPayload.class, name = "dsp-uploaded"),
    @JsonSubTypes.Type(value = DspCorrectedPayload.class, name = "dsp-corrected"),
    @JsonSubTypes.Type(value = DspAddendumUploadedPayload.class, name = "dsp-addendum-uploaded")
})
public sealed interface CaseEventPayload permits
    CaseNotePayload,
    CaseAllocationPayload,
    CaseUpdateRequestPayload,
    CaseUpdateSubmittedPayload,
    DspUploadedPayload,
    DspCorrectedPayload,
    DspAddendumUploadedPayload {
}
