package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeAction;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;

@ExtendWith(MockitoExtension.class)
class EquityChangeActionHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/removeEquityChange";
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final EquityChangeContext CONTEXT = new EquityChangeContext(
      List.of(new SetEquityRow("Organisation", BigDecimal.TEN)),
      List.of(),
      "Subarea A"
  );
  private static final ModelAndView EXPECTED_CONFIRMATION = new ModelAndView(TEMPLATE_NAME)
      .addObject("subareaLabel", "Subarea A")
      .addObject("setEquityRows", CONTEXT.setEquityRows())
      .addObject("transferEquityRows", List.of());

  @Mock
  private EquityChangeService equityChangeService;

  @InjectMocks
  private EquityChangeActionHandler equityChangeActionHandler;

  @Test
  void executedChangeConfirmation() {
    var liveChange = LicencePositionChangeTestUtil.newBuilder().withId(UUID.fromString(CHANGE_ID)).build();
    when(equityChangeService.getExecutedEquityChangeContext(CHANGE_ID)).thenReturn(CONTEXT);

    var result = equityChangeActionHandler.executedChangeConfirmation(
        CORRECTION,
        LicencePositionTestUtil.newBuilder().build(),
        liveChange
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(EXPECTED_CONFIRMATION);
  }

  @Test
  void stagedChangeConfirmation() {
    when(equityChangeService.getEquityChangeContext(CORRECTION, CHANGE_ID)).thenReturn(CONTEXT);

    var result = equityChangeActionHandler.stagedChangeConfirmation(
        CORRECTION,
        LicencePositionStagedPayloadTestUtil.newBuilder().build(),
        CHANGE_ID,
        ChangeAction.UNDO
    );

    assertThat(result).usingRecursiveComparison().isEqualTo(EXPECTED_CONFIRMATION);
  }
}
