package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class WorkProgrammeAmendmentSummarySectionService
    implements SummarySectionService<ScheduleWorkProgrammeApplicationDetail> {

  public static final String LICENCE_SECTION_NAME = "Work programme amendments";
  public static final String COMPLETION_DATE_CHANGE_REQUESTED = "Completion date change requested";
  public static final String REQUESTED_EXTENSION_TO_COMPLETION_DATE = "Requested extension to completion date";
  public static final String WORK_PROGRAMME_CONTENT_CHANGE_REQUESTED = "Work programme content change requested";
  public static final String REQUESTED_CHANGE_TO_CONTENT = "Requested change to content";
  public static final int SECTION_DISPLAY_ORDER = 30;

  private final LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;
  private final SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  WorkProgrammeAmendmentSummarySectionService(
      LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService,
      SwpApplicationRequestPurposeService swpApplicationRequestPurposeService
  ) {
    this.licenceWorkProgrammeAmendmentSummaryService = licenceWorkProgrammeAmendmentSummaryService;
    this.swpApplicationRequestPurposeService = swpApplicationRequestPurposeService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    var requestPurpose = swpApplicationRequestPurposeService
        .getRequestPurpose(scheduleWorkProgrammeApplicationDetail);

    if (requestPurpose.isEmpty() || !requestPurpose.get().getAmendWorkProgramme()) {
      return Optional.empty();
    }

    if (!swpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities(scheduleWorkProgrammeApplicationDetail)) {
      return Optional.empty();
    }

    var specificSummaryItem = getLicenceSummaryItem(scheduleWorkProgrammeApplicationDetail, LICENCE_SECTION_NAME);
    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(specificSummaryItem));

    return Optional.of(summarySection);
  }

  public SummaryItem getLicenceSummaryItem(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      String sectionName
  ) {
    var amendmentViews = licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail);

    var summaryCards = getWorkProgrammeAmendmentSummaryCards(amendmentViews);

    return SummaryItem.withCards(sectionName, summaryCards);
  }

  private List<SummaryCard> getWorkProgrammeAmendmentSummaryCards(
      List<LicenceWorkProgrammeAmendmentSummaryView> amendmentViews
  ) {
    return amendmentViews.stream()
                         .map(this::buildSummaryCardFromAmendmentView)
                         .toList();
  }

  private SummaryCard buildSummaryCardFromAmendmentView(LicenceWorkProgrammeAmendmentSummaryView view) {
    var builder = SummaryDataView.newBuilder();

    if (!view.workProgrammeCompletionDateChangeRequestedDisplay().isEmpty()) {
      builder.addStringValue(COMPLETION_DATE_CHANGE_REQUESTED, view.workProgrammeCompletionDateChangeRequestedDisplay());
    }

    if (BooleanUtils.isTrue(view.workProgrammeCompletionDateChangeRequested())) {
      builder.addStringValue(REQUESTED_EXTENSION_TO_COMPLETION_DATE, view.workProgrammeExtensionDuration());
    }

    builder.addStringValue(WORK_PROGRAMME_CONTENT_CHANGE_REQUESTED, view.workProgrammeChangeRequestedDisplay());

    if (BooleanUtils.isTrue(view.workProgrammeChangeRequested())) {
      builder.addStringValue(REQUESTED_CHANGE_TO_CONTENT, view.workProgrammeAmendmentInformation());
    }

    return SummaryCard.simpleSummaryCardWithHeading(
        view.workProgrammeAmendmentLabel(),
        builder.build()
    );
  }
}