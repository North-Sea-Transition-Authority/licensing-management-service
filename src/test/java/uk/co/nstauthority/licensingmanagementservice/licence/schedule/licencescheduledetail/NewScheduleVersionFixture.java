package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

/**
 * Makes a new version of a schedule as LMS does when a user edits it: copies the active detail to a draft, then applies
 * the draft. Import it into an integration test with {@code @Import}; the test stubs {@code UserDetailService}, as both
 * steps record the user.
 */
public class NewScheduleVersionFixture {

  private final LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;

  NewScheduleVersionFixture(
      LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService,
      LicenceScheduleDetailService licenceScheduleDetailService
  ) {
    this.licenceScheduleDetailDuplicationService = licenceScheduleDetailDuplicationService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  /**
   * The new active detail, a copy of the one given, which becomes the replaced one.
   */
  public LicenceScheduleDetail applyNewVersion(LicenceScheduleDetail activeDetail) {
    var draft = licenceScheduleDetailDuplicationService.createNewDraftLicenceScheduleDetailVersion(activeDetail);
    licenceScheduleDetailService.applyAndReplaceActiveScheduleDetail(draft);
    return draft;
  }
}
