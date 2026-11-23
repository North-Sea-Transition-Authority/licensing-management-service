package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.explorationoperator.ExplorationOperatorChangeContext;

@ExtendWith(MockitoExtension.class)
class ExplorationOperatorChangeFormHandlerTest {

  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .build();
  private static final LicencePositionChange LIVE_CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.randomUUID())
      .withLicencePosition(POSITION)
      .build();

  @Mock
  private OrganisationChangeService organisationChangeService;

  @Mock
  private OrganisationChangeFormValidator organisationChangeFormValidator;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private ExplorationOperatorChangeFormHandler explorationOperatorChangeFormHandler;

  @Test
  void formPage() {
    var target = new ChangeFormTarget.ExecutedPosition(CORRECTION, POSITION);
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION_ID, ExplorationOperatorOperation.class))
        .thenReturn(new ExplorationOperatorChangeContext(123, 456, "Current operator", "Previous operator"));
    when(organisationChangeService.populateExistingOrganisationForm(
        eq(123),
        eq(CORRECTION),
        eq(POSITION),
        isA(ExplorationOperatorChangeForm.class)
    ))
        .thenReturn(new ExplorationOperatorChangeForm("123"));
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("123")).thenReturn(Map.of("123", "Current operator"));

    var result = explorationOperatorChangeFormHandler.formPage(target);

    var expected = new ModelAndView("lms/licence/correction/change/addOrganisationChange")
        .addObject("pageTitle", "Change exploration operator")
        .addObject("operatorType", "exploration operator")
        .addObject("previousOperatorName", "Previous operator")
        .addObject("form", new ExplorationOperatorChangeForm("123"))
        .addObject("preselectedOperator", Map.of("123", "Current operator"))
        .addObject(
            "organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))
        );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void save_whenExecutedPosition_thenAddsExplorationOperatorChange() {
    explorationOperatorChangeFormHandler.save(
        new ChangeFormTarget.ExecutedPosition(CORRECTION, POSITION),
        new ExplorationOperatorChangeForm("789")
    );

    verify(organisationChangeService).addOrganisationChangeForExistingLicencePosition(
        POSITION,
        CORRECTION,
        ExplorationOperatorOperation.class,
        LicenceOperation.newExplorationOperatorOperation().withOperator(789).build()
    );
  }

  @Test
  void save_whenLiveChange_thenCorrectsChange() {
    explorationOperatorChangeFormHandler.save(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE),
        new ExplorationOperatorChangeForm("789")
    );

    verify(organisationChangeService).correctOrganisationChange(
        POSITION,
        CORRECTION,
        ExplorationOperatorOperation.class,
        LIVE_CHANGE.getId().toString(),
        LicenceOperation.newExplorationOperatorOperation().withOperator(789).build()
    );
  }
}
