package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionWorkProgramme;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionWorkProgrammeLicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionWorkProgrammeRepository;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class WorkProgrammeDecisionSummarySectionService
    implements SummarySectionService<RecordOfDecisionSummaryContext> {

  public static final String SECTION_NAME = "Work programme amendment details";
  public static final String DECISION = "Decision";
  public static final String AMENDED_DURATION = "Amended duration";
  public static final String AMENDED_TEXT = "Amended text";
  public static final String TARGET_LICENCES = "Licences";
  public static final int SECTION_DISPLAY_ORDER = 40;

  private final RecordOfDecisionService recordOfDecisionService;
  private final RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository;
  private final RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository;

  public WorkProgrammeDecisionSummarySectionService(
      RecordOfDecisionService recordOfDecisionService,
      RecordOfDecisionWorkProgrammeRepository recordOfDecisionWorkProgrammeRepository,
      RecordOfDecisionWorkProgrammeLicenceRepository recordOfDecisionWorkProgrammeLicenceRepository
  ) {
    this.recordOfDecisionService = recordOfDecisionService;
    this.recordOfDecisionWorkProgrammeRepository = recordOfDecisionWorkProgrammeRepository;
    this.recordOfDecisionWorkProgrammeLicenceRepository = recordOfDecisionWorkProgrammeLicenceRepository;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      RecordOfDecisionSummaryContext context,
      ServiceUserDetail user
  ) {
    var applicationDetail = context.applicationDetail();

    if (!recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)) {
      return Optional.empty();
    }

    var workProgrammes = recordOfDecisionWorkProgrammeRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    if (workProgrammes.isEmpty()) {
      return Optional.of(new SummarySection(
          SECTION_DISPLAY_ORDER,
          List.of(SummaryItem.withCard(SECTION_NAME, SummaryCard.emptySummaryCard()))));
    }

    var cards = workProgrammes.stream().map(this::toCard).toList();

    return Optional.of(new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(SummaryItem.withCards(SECTION_NAME, cards))));
  }

  private SummaryCard toCard(RecordOfDecisionWorkProgramme workProgramme) {
    var summaryDataViewBuilder = SummaryDataView.newBuilder()
        .addStringValue(DECISION, workProgramme.getDecision().getDisplayName());

    if (Boolean.TRUE.equals(workProgramme.getAmendDuration())) {
      summaryDataViewBuilder.addStringValue(
          AMENDED_DURATION,
          ThreeFieldDurationDisplayUtil.convertToDisplayText(workProgramme.getAmendedDuration()));
    }

    if (Boolean.TRUE.equals(workProgramme.getAmendText())) {
      summaryDataViewBuilder.addStringValue(AMENDED_TEXT, workProgramme.getAmendedText());
    }

    var targetLicences = recordOfDecisionWorkProgrammeLicenceRepository
        .findAllByRecordOfDecisionWorkProgramme(workProgramme)
        .stream()
        .map(workProgrammeLicence -> workProgrammeLicence.getLicence().getLicenceReference())
        .toList();

    if (!targetLicences.isEmpty()) {
      summaryDataViewBuilder.addStringValue(TARGET_LICENCES, String.join(", ", targetLicences));
    }

    return SummaryCard.simpleSummaryCardWithHeading(
        workProgramme.getWorkProgrammeActivity().getDescription(),
        summaryDataViewBuilder.build());
  }
}
