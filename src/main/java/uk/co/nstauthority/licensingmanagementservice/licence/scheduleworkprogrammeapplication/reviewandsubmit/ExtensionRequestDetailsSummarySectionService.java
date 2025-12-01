package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRequestView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class ExtensionRequestDetailsSummarySectionService
    implements SummarySectionService<ScheduleWorkProgrammeApplicationDetail> {

  public static final String LICENCE_SECTION_NAME = "Extension details";
  public static final String EXTENSION_DURATION = "%s extension duration";
  public static final int SECTION_DISPLAY_ORDER = 10;

  private final LicenceScheduleExtensionService licenceScheduleExtensionService;

  public ExtensionRequestDetailsSummarySectionService(LicenceScheduleExtensionService licenceScheduleExtensionService) {
    this.licenceScheduleExtensionService = licenceScheduleExtensionService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user
  ) {
    var specificSummaryItem = getLicenceSummaryItem(scheduleWorkProgrammeApplicationDetail, LICENCE_SECTION_NAME);
    var summarySection = new SummarySection(SECTION_DISPLAY_ORDER, List.of(specificSummaryItem));

    return Optional.of(summarySection);
  }

  public SummaryItem getLicenceSummaryItem(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      String sectionName
  ) {

    List<LicenceScheduleExtensionRequestView> extensionRequestViews = licenceScheduleExtensionService
        .getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail);

    var selectedItems = extensionRequestViews.stream().filter(LicenceScheduleExtensionRequestView::isRequested).toList();

    if (selectedItems.isEmpty()) {
      return SummaryItem.withCard(sectionName, SummaryCard.emptySummaryCard());
    }

    var summaryCard = buildSummaryCard(selectedItems);
    return SummaryItem.withCard(sectionName, summaryCard);
  }

  private SummaryCard buildSummaryCard(List<LicenceScheduleExtensionRequestView> licenceScheduleExtensionRequestViews) {
    var summaryDataViewBuilder = SummaryDataView.newBuilder();

    licenceScheduleExtensionRequestViews.forEach(licenceScheduleExtensionRequestView -> summaryDataViewBuilder.addStringValue(
        EXTENSION_DURATION.formatted(licenceScheduleExtensionRequestView.displayName()),
        ThreeFieldDurationDisplayUtil.convertToDisplayText(licenceScheduleExtensionRequestView.duration())
    ));

    return SummaryCard.simpleSummaryCard(summaryDataViewBuilder.build());
  }
}