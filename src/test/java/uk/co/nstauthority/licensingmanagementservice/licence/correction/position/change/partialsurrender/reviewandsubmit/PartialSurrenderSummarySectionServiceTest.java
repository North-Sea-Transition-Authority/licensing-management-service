package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderSummarySectionServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();
  private static final PartialSurrenderSummaryContext CONTEXT =
      new PartialSurrenderSummaryContext(LicencePositionCorrectionTestUtil.newBuilder().build());
  private static final SummarySection SURRENDER_DETAILS_SECTION = new SummarySection(10,
      List.of(SummaryItem.withCard("Surrender details", SummaryCard.simpleSummaryCard(
          SummaryDataView.newBuilder()
              .addStringValue("Date of surrender", DateUtil.formatLongDate(LocalDate.of(2026, Month.AUGUST, 12)))
              .addStringValue("Blocks surrendered", List.of("Block 21/30"))
              .build()
          ))
      )
  );
  private static final SummarySection OTHER_SECTION = new SummarySection(20,
      List.of(SummaryItem.withCard("Other", SummaryCard.simpleSummaryCard(
              SummaryDataView.newBuilder()
                  .addStringValue("Temp field", "Value")
                  .build()
          ))
      )
  );

  @Mock
  private PartialSurrenderDetailsSummarySectionService partialSurrenderDetailsSummarySectionService;

  @Mock
  private SummarySectionService<PartialSurrenderSummaryContext> otherSummarySectionService;

  private PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;

  @BeforeEach
  void setUp() {
    List<SummarySectionService<PartialSurrenderSummaryContext>> summarySectionServices = List.of(
        partialSurrenderDetailsSummarySectionService,
        otherSummarySectionService
    );
    partialSurrenderSummarySectionService = new PartialSurrenderSummarySectionService(summarySectionServices);
  }

  @Test
  void getSummarySections_whenSectionsReturnedOutOfOrder_thenSortedByDisplayOrder() {
    when(partialSurrenderDetailsSummarySectionService.getSummarySection(CONTEXT, USER))
        .thenReturn(Optional.of(SURRENDER_DETAILS_SECTION));
    when(otherSummarySectionService.getSummarySection(CONTEXT, USER))
        .thenReturn(Optional.of(OTHER_SECTION));

    var sections = partialSurrenderSummarySectionService.getSummarySections(CONTEXT, USER);

    assertThat(sections).containsExactly(SURRENDER_DETAILS_SECTION, OTHER_SECTION);
  }

  @Test
  void getSummarySections_whenSectionServiceHasNoSection_thenSectionOmitted() {
    when(partialSurrenderDetailsSummarySectionService.getSummarySection(CONTEXT, USER))
        .thenReturn(Optional.empty());
    when(otherSummarySectionService.getSummarySection(CONTEXT, USER))
        .thenReturn(Optional.of(SURRENDER_DETAILS_SECTION));

    var sections = partialSurrenderSummarySectionService.getSummarySections(CONTEXT, USER);

    assertThat(sections).containsExactly(SURRENDER_DETAILS_SECTION);
  }
}
