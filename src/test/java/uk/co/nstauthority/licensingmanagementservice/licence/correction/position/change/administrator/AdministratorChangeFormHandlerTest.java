package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.AdministratorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;

@ExtendWith(MockitoExtension.class)
class AdministratorChangeFormHandlerTest {

  private static final String TEMPLATE_NAME = "lms/licence/correction/change/addOrganisationChange";
  private static final String OPERATOR_TYPE = "licence administrator";
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final Integer CURRENT_ADMINISTRATOR_ID = 123;
  private static final Integer PREVIOUS_ADMINISTRATOR_ID = 456;
  private static final String PREVIOUS_ADMINISTRATOR_NAME = "Previous administrator";
  private static final Map<String, String> PRESELECTED_ADMINISTRATOR = Map.of("123", "Current administrator");
  private static final AdministratorChangeContext CHANGE_CONTEXT = new AdministratorChangeContext(
      CURRENT_ADMINISTRATOR_ID,
      PREVIOUS_ADMINISTRATOR_ID,
      "Current administrator",
      PREVIOUS_ADMINISTRATOR_NAME
  );
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .build();
  private static final CorrectionPosition EXECUTED_POSITION = new CorrectionPosition.Executed(CORRECTION, POSITION);
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

  @Mock
  private AdministratorChangeService administratorChangeService;

  @InjectMocks
  private AdministratorChangeFormHandler administratorChangeFormHandler;

  @Test
  void formPage_whenNewChange_thenFormIsPopulatedForThePosition() {
    var populatedForm = formWithAdministrator("123");
    givenChangeContext(POSITION_ID);
    when(organisationChangeService.populateOrganisationForm(
        eq(CURRENT_ADMINISTRATOR_ID), eq(EXECUTED_POSITION), any(AdministratorChangeForm.class)))
        .thenReturn(populatedForm);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("123")).thenReturn(PRESELECTED_ADMINISTRATOR);

    var result = administratorChangeFormHandler.formPage(new ChangeFormTarget.NewChange(EXECUTED_POSITION));

    assertThat(result).usingRecursiveComparison()
        .isEqualTo(expectedFormPage(populatedForm, PRESELECTED_ADMINISTRATOR));
  }

  @Test
  void formPage_whenCorrectingLiveChange_thenShowsCurrentAdministrator() {
    givenChangeContext(POSITION_ID);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("123")).thenReturn(PRESELECTED_ADMINISTRATOR);

    var result = administratorChangeFormHandler.formPage(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE)
    );

    assertThat(result).usingRecursiveComparison()
        .isEqualTo(expectedFormPage(formWithAdministrator("123"), PRESELECTED_ADMINISTRATOR));
  }

  @Test
  void formPage_whenCorrectingLiveChangeWithNoCurrentAdministrator_thenLeavesFormEmpty() {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(null, PREVIOUS_ADMINISTRATOR_ID, "", PREVIOUS_ADMINISTRATOR_NAME));
    when(organisationUnitQueryService.getOrganisationUnitSelectOption(null)).thenReturn(Map.of());

    var result = administratorChangeFormHandler.formPage(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE)
    );

    assertThat(result).usingRecursiveComparison()
        .isEqualTo(expectedFormPage(new AdministratorChangeForm(), Map.of()));
  }

  @Test
  void formPageIfInvalid_whenFormHasErrors_thenReturnsFormPage() {
    var form = formWithAdministrator("789");
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    givenChangeContext(POSITION_ID);
    when(organisationChangeFormValidator.hasErrors(form, bindingResult, PREVIOUS_ADMINISTRATOR_ID, OPERATOR_TYPE))
        .thenReturn(true);
    when(organisationUnitQueryService.getOrganisationUnitSelectOption("789")).thenReturn(Map.of());

    var result = administratorChangeFormHandler.formPageIfInvalid(
        new ChangeFormTarget.NewChange(EXECUTED_POSITION),
        form,
        bindingResult
    );

    assertThat(result).get().usingRecursiveComparison().isEqualTo(expectedFormPage(form, Map.of()));
  }

  @Test
  void formPageIfInvalid_whenFormIsValid_thenReturnsEmpty() {
    var form = formWithAdministrator("789");
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    givenChangeContext(POSITION_ID);
    when(organisationChangeFormValidator.hasErrors(form, bindingResult, PREVIOUS_ADMINISTRATOR_ID, OPERATOR_TYPE))
        .thenReturn(false);

    var result = administratorChangeFormHandler.formPageIfInvalid(
        new ChangeFormTarget.NewChange(EXECUTED_POSITION),
        form,
        bindingResult
    );

    assertThat(result).isEmpty();
  }

  @Test
  void save_whenNewChange_thenAddsChangeToPosition() {
    administratorChangeFormHandler.save(new ChangeFormTarget.NewChange(EXECUTED_POSITION), formWithAdministrator("789"));

    verify(organisationChangeService).addOrganisationChange(
        EXECUTED_POSITION,
        AdministratorOperation.class,
        LicenceOperation.newAdministratorChange().withOperator(789).build()
    );
  }

  @Test
  void save_whenLiveChange_thenCorrectsChange() {
    administratorChangeFormHandler.save(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE),
        formWithAdministrator("789")
    );

    verify(administratorChangeService)
        .correctExistingAdministratorChange(POSITION, CORRECTION, LIVE_CHANGE.getId().toString(), 789);
  }

  private void givenChangeContext(UUID positionId) {
    when(organisationChangeService.getOrganisationChangeContext(CORRECTION, positionId, AdministratorOperation.class))
        .thenReturn(CHANGE_CONTEXT);
  }

  private static ModelAndView expectedFormPage(AdministratorChangeForm form, Map<String, String> preselected) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("pageTitle", "Change licence administrator")
        .addObject("operatorType", OPERATOR_TYPE)
        .addObject("previousOperatorName", PREVIOUS_ADMINISTRATOR_NAME)
        .addObject("form", form)
        .addObject("preselectedOperator", preselected)
        .addObject(
            "organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))
        );
  }

  private static AdministratorChangeForm formWithAdministrator(String administratorId) {
    return new AdministratorChangeForm(administratorId);
  }
}
