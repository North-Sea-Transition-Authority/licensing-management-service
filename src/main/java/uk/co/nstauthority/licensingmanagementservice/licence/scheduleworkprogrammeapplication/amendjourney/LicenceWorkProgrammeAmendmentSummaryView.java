package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

public record LicenceWorkProgrammeAmendmentSummaryView(String workProgrammeAmendmentLabel,
                                                       String workProgrammeChangeRequestedDisplay,
                                                       String workProgrammeAmendmentInformation,
                                                       String workProgrammeCompletionDateChangeRequestedDisplay,
                                                       String workProgrammeExtensionDuration,
                                                       LicenceWorkProgrammeAmendmentSummaryMode summaryMode,
                                                       String changeUrl,
                                                       String deleteUrl,
                                                       Boolean workProgrammeCompletionDateChangeRequested,
                                                       Boolean workProgrammeChangeRequested) {
}