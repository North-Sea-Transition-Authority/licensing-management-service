package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderChangeActionHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/partialSurrender/removePartialSurrenderChange";
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().build();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(CHANGE_ID))
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder().build();
  private static final LocalDate SURRENDER_DATE = LocalDate.of(2024, Month.MARCH, 1);
  private static final LocalDate POSITION_DATE = LocalDate.of(2024, Month.JUNE, 1);
  private static final List<PartialSurrenderChangeView.BlockRow> BLOCK_ROWS =
      List.of(new PartialSurrenderChangeView.BlockRow("30/1", List.of("Full surrender")));

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @InjectMocks
  private PartialSurrenderChangeActionHandler partialSurrenderChangeActionHandler;

  @Test
  void executedChangeSummary_whenSurrenderHasItsOwnDate_thenShowsThatDate() {
    var surrender = surrenderDated(SURRENDER_DATE);
    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(CHANGE_ID)).thenReturn(surrender);
    when(partialSurrenderCorrectionService.getBlockRows(surrender)).thenReturn(BLOCK_ROWS);

    var result = partialSurrenderChangeActionHandler.executedChangeConfirmation(CORRECTION, POSITION, LIVE_CHANGE);

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation(SURRENDER_DATE));
  }

  @Test
  void executedChangeSummary_whenSurrenderHasNoDate_thenShowsTheEffectivePositionDate() {
    var surrender = surrenderDated(null);
    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(CHANGE_ID)).thenReturn(surrender);
    when(partialSurrenderCorrectionService.getBlockRows(surrender)).thenReturn(BLOCK_ROWS);
    when(licencePositionCorrectionService.getEffectivePositionDate(CORRECTION, POSITION)).thenReturn(POSITION_DATE);

    var result = partialSurrenderChangeActionHandler.executedChangeConfirmation(CORRECTION, POSITION, LIVE_CHANGE);

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation(POSITION_DATE));
  }

  @Test
  void stagedChangeSummary_whenSurrenderHasItsOwnDate_thenShowsThatDate() {
    var surrender = surrenderDated(SURRENDER_DATE);
    when(partialSurrenderCorrectionService.getStagedPartialSurrenderOrThrow(POSITION_CORRECTION, CHANGE_ID))
        .thenReturn(surrender);
    when(partialSurrenderCorrectionService.getBlockRows(surrender)).thenReturn(BLOCK_ROWS);

    var result = partialSurrenderChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_CORRECTION,
        CHANGE_ID,
        ChangeAction.UNDO
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation(SURRENDER_DATE));
  }

  @Test
  void stagedChangeSummary_whenSurrenderHasNoDate_thenShowsTheCorrectedPositionDate() {
    var surrender = surrenderDated(null);
    when(partialSurrenderCorrectionService.getStagedPartialSurrenderOrThrow(POSITION_CORRECTION, CHANGE_ID))
        .thenReturn(surrender);
    when(partialSurrenderCorrectionService.getBlockRows(surrender)).thenReturn(BLOCK_ROWS);
    when(licencePositionCorrectionService.resolveEffectiveDate(POSITION_CORRECTION)).thenReturn(POSITION_DATE);

    var result = partialSurrenderChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        POSITION_CORRECTION,
        CHANGE_ID,
        ChangeAction.UNDO
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(expectedConfirmation(POSITION_DATE));
  }

  @Test
  void beforeDroppingStagedChange() {
    var stagedChange = LicencePositionChangeType.removeChange().withChangeId(CHANGE_ID).build();

    partialSurrenderChangeActionHandler.beforeDroppingStagedChange(POSITION_CORRECTION, stagedChange);

    verify(partialSurrenderCorrectionService).deleteStagedSurrenderArtefacts(stagedChange);
  }

  private static PartialSurrenderOperation surrenderDated(LocalDate surrenderDate) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .withSurrenderDate(surrenderDate)
        .build();
  }

  private static ModelAndView expectedConfirmation(LocalDate surrenderDate) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("surrenderDate", DateUtil.formatLongDate(surrenderDate))
        .addObject("blockRows", BLOCK_ROWS);
  }
}
