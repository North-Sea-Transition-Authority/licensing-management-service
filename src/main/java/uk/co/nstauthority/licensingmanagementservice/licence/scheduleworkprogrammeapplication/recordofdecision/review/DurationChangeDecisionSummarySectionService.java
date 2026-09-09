package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationDisplayUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionReduction;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionReductionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.RecordOfDecisionService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class DurationChangeDecisionSummarySectionService
    implements SummarySectionService<RecordOfDecisionSummaryContext> {

  public static final String SECTION_NAME = "Term and phase durations";
  public static final String EXTENSIONS_CARD_NAME = "Term and phase extensions";
  public static final String REDUCTIONS_CARD_NAME = "Term and phase reductions";
  public static final String EXTENSION_DURATION = "%s extension duration";
  public static final String REDUCTION_DURATION = "%s reduction duration";
  public static final int SECTION_DISPLAY_ORDER = 20;

  private final RecordOfDecisionService recordOfDecisionService;
  private final RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository;
  private final RecordOfDecisionReductionRepository recordOfDecisionReductionRepository;

  public DurationChangeDecisionSummarySectionService(
      RecordOfDecisionService recordOfDecisionService,
      RecordOfDecisionExtensionRepository recordOfDecisionExtensionRepository,
      RecordOfDecisionReductionRepository recordOfDecisionReductionRepository
  ) {
    this.recordOfDecisionService = recordOfDecisionService;
    this.recordOfDecisionExtensionRepository = recordOfDecisionExtensionRepository;
    this.recordOfDecisionReductionRepository = recordOfDecisionReductionRepository;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      RecordOfDecisionSummaryContext context,
      ServiceUserDetail user
  ) {
    var applicationDetail = context.applicationDetail();

    if (!recordOfDecisionService.isExtensionApproved(applicationDetail)) {
      return Optional.empty();
    }

    var extensions = recordOfDecisionExtensionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    var reductions = recordOfDecisionReductionRepository
        .findAllByScheduleWorkProgrammeApplicationDetail(applicationDetail);

    var cards = new ArrayList<SummaryCard>();

    if (!extensions.isEmpty()) {
      var extensionData = SummaryDataView.newBuilder();
      extensions.forEach(extension -> extensionData.addStringValue(
          EXTENSION_DURATION.formatted(displayName(extension)),
          ThreeFieldDurationDisplayUtil.convertToDisplayText(extension.getExtensionDuration())));
      cards.add(SummaryCard.simpleSummaryCardWithHeading(EXTENSIONS_CARD_NAME, extensionData.build()));
    }

    if (!reductions.isEmpty()) {
      var reductionData = SummaryDataView.newBuilder();
      reductions.forEach(reduction -> reductionData.addStringValue(
          REDUCTION_DURATION.formatted(displayName(reduction)),
          ThreeFieldDurationDisplayUtil.convertToDisplayText(reduction.getReductionDuration())));
      cards.add(SummaryCard.simpleSummaryCardWithHeading(REDUCTIONS_CARD_NAME, reductionData.build()));
    }

    if (cards.isEmpty()) {
      return Optional.of(new SummarySection(
          SECTION_DISPLAY_ORDER,
          List.of(SummaryItem.withCard(SECTION_NAME, SummaryCard.emptySummaryCard()))));
    }

    return Optional.of(new SummarySection(
        SECTION_DISPLAY_ORDER,
        List.of(SummaryItem.withCards(SECTION_NAME, cards))));
  }

  private String displayName(RecordOfDecisionExtension extension) {
    return extension.getLicenceSchedulePhase() != null
        ? extension.getLicenceSchedulePhase().getPhaseType().getDisplayName()
        : extension.getLicenceScheduleTerm().getTermType().getDisplayName();
  }

  private String displayName(RecordOfDecisionReduction reduction) {
    return reduction.getLicenceSchedulePhase() != null
        ? reduction.getLicenceSchedulePhase().getPhaseType().getDisplayName()
        : reduction.getLicenceScheduleTerm().getTermType().getDisplayName();
  }
}
