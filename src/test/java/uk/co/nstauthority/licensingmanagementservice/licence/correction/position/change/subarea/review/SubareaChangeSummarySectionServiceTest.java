package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@ExtendWith(MockitoExtension.class)
class SubareaChangeSummarySectionServiceTest {

  private static final ServiceUserDetail USER = ServiceUserDetailTestUtil.newBuilder().build();
  private static final SubareaChangeSummaryContext CONTEXT = new SubareaChangeSummaryContext.LiveChange(
      UUID.randomUUID(),
      List.of(),
      List.of(),
      LicencePositionState.EMPTY,
      LicencePositionState.EMPTY,
      null
  );
  private static final SummarySection OVERVIEW_SECTION =
      new SummarySection(10, List.of(SummaryItem.withCards("Overview", List.of())));
  private static final SummarySection CHANGES_SECTION =
      new SummarySection(20, List.of(SummaryItem.withCards("Changes", List.of())));

  @Mock
  private SummarySectionService<SubareaChangeSummaryContext> changesSummarySectionService;

  @Mock
  private SummarySectionService<SubareaChangeSummaryContext> overviewSummarySectionService;

  private SubareaChangeSummarySectionService subareaChangeSummarySectionService;

  @BeforeEach
  void setUp() {
    subareaChangeSummarySectionService = new SubareaChangeSummarySectionService(
        List.of(changesSummarySectionService, overviewSummarySectionService)
    );
  }

  @Test
  void getSummarySections_whenSectionsReturnedOutOfOrder_thenSortedByDisplayOrder() {
    when(changesSummarySectionService.getSummarySection(CONTEXT, USER)).thenReturn(Optional.of(CHANGES_SECTION));
    when(overviewSummarySectionService.getSummarySection(CONTEXT, USER)).thenReturn(Optional.of(OVERVIEW_SECTION));

    var sections = subareaChangeSummarySectionService.getSummarySections(CONTEXT, USER);

    assertThat(sections).containsExactly(OVERVIEW_SECTION, CHANGES_SECTION);
  }

  @Test
  void getSummarySections_whenSectionServiceHasNoSection_thenSectionOmitted() {
    when(changesSummarySectionService.getSummarySection(CONTEXT, USER)).thenReturn(Optional.of(CHANGES_SECTION));
    when(overviewSummarySectionService.getSummarySection(CONTEXT, USER)).thenReturn(Optional.empty());

    var sections = subareaChangeSummarySectionService.getSummarySections(CONTEXT, USER);

    assertThat(sections).containsExactly(CHANGES_SECTION);
  }
}
