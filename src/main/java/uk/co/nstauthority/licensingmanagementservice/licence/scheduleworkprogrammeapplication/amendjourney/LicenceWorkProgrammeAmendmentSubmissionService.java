package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Service
public class LicenceWorkProgrammeAmendmentSubmissionService {

  private final LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService;
  private final LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  public LicenceWorkProgrammeAmendmentSubmissionService(
      LicenceWorkProgrammeAmendmentService licenceWorkProgrammeAmendmentService,
      LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService
  ) {
    this.licenceWorkProgrammeAmendmentService = licenceWorkProgrammeAmendmentService;
    this.licenceWorkProgrammeAmendmentSummaryService = licenceWorkProgrammeAmendmentSummaryService;
  }

  public boolean isAmendmentSectionSubmittable(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {

    List<LicenceWorkProgrammeAmendmentRequest> workProgrammeApplicationDetails = licenceWorkProgrammeAmendmentService
        .getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    return !workProgrammeApplicationDetails.isEmpty();
  }

  public boolean isAmendmentSectionComplete(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {

    var workProgrammeApplicationDetails = licenceWorkProgrammeAmendmentService
        .getAmendmentRequestsByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);

    var licenceWorkProgrammeAmendmentSummaryOption =
        licenceWorkProgrammeAmendmentSummaryService
        .getLicenceWorkProgrammeAmendmentSummaryByScheduleWorkProgrammeApplicationDetail(
            scheduleWorkProgrammeApplicationDetail
        );

    return licenceWorkProgrammeAmendmentSummaryOption
        .map(summaryOption -> summaryOption
                                  .getLicenceWorkProgrammeAmendmentSummaryOptions()
                                  .equals(LicenceWorkProgrammeAmendmentSummaryOptions.NO)
                              && licenceWorkProgrammeAmendmentService.validateAllWorkProgrammeAmendments(
                                  workProgrammeApplicationDetails))
        .orElse(false);
  }

}