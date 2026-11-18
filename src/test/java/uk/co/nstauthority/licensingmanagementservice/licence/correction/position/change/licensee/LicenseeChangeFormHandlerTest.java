package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceFormService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.licensee.LicenseeChangeContext;

@ExtendWith(MockitoExtension.class)
class LicenseeChangeFormHandlerTest {

  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID ADDED_POSITION_ID = UUID.randomUUID();
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
  private static final LicencePositionCorrection ADDED_POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withLicenceCorrection(CORRECTION)
      .withTargetLicencePosition(null)
      .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
          .withLicencePositionId(ADDED_POSITION_ID.toString())
          .build())
      .build();
  private static final LicenseeChangeContext CONTEXT_WITH_PREVIOUS_LICENSEES = new LicenseeChangeContext(
      List.of(2),
      List.of(1),
      List.of(1),
      List.of(2),
      Map.of(1, "Withdrawing licensee", 2, "Joining licensee")
  );

  @Mock
  private LicenceFormService licenceFormService;

  @Mock
  private LicenseeChangeFormValidator licenseeChangeFormValidator;

  @Mock
  private LicenseeChangeService licenseeChangeService;

  @InjectMocks
  private LicenseeChangeFormHandler licenseeChangeFormHandler;

  @Test
  void formPage_whenPositionHasPreviousLicensees_thenShowsCurrentChangeAndConstrainsSearches() {
    when(licenseeChangeService.getLicenseeChangeContext(CORRECTION, POSITION_ID))
        .thenReturn(CONTEXT_WITH_PREVIOUS_LICENSEES);
    when(licenceFormService.getPreselectedOrganisationUnits(List.of("2"))).thenReturn(List.of());
    when(licenceFormService.getPreselectedOrganisationUnits(List.of("1"))).thenReturn(List.of());

    var result = licenseeChangeFormHandler.formPage(new ChangeFormTarget.ExecutedPosition(CORRECTION, POSITION));

    var expectedForm = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("2"))
        .setWithdrawingOrganisationIds(List.of("1"));
    var previousNames = List.of("Withdrawing licensee");
    var expected = expectedFormPage(
        expectedForm,
        previousNames,
        SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
            .searchOrganisationUnitsWithConstraints(null, null, previousNames)),
        SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
            .searchOrganisationUnitsWithConstraints(null, previousNames, null))
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void formPage_whenPositionHasNoPreviousLicensees_thenSearchesAllOrganisationsAndOffersNoWithdrawal() {
    var context = new LicenseeChangeContext(null, null, List.of(), List.of(), Map.of());
    when(licenseeChangeService.getLicenseeChangeContext(CORRECTION, ADDED_POSITION_ID)).thenReturn(context);
    when(licenceFormService.getPreselectedOrganisationUnits(null)).thenReturn(List.of());

    var result = licenseeChangeFormHandler.formPage(
        new ChangeFormTarget.AddedPosition(CORRECTION, ADDED_POSITION_CORRECTION)
    );

    var expected = expectedFormPage(
        new LicenseeChangeForm(),
        List.of(),
        SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)),
        ""
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void formPageIfInvalid_whenWithdrawingLicenseesMissing_thenValidatesWithNoneWithdrawing() {
    var form = new LicenseeChangeForm().setJoiningOrganisationIds(List.of("2"));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    when(licenseeChangeService.getLicenseeChangeContext(CORRECTION, POSITION_ID))
        .thenReturn(CONTEXT_WITH_PREVIOUS_LICENSEES);
    when(licenseeChangeFormValidator.hasErrors(form, bindingResult, List.of(1))).thenReturn(false);

    var result = licenseeChangeFormHandler.formPageIfInvalid(
        new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE),
        form,
        bindingResult
    );

    assertThat(result).isEmpty();
    assertThat(form.getWithdrawingOrganisationIds()).isEmpty();
  }

  @Test
  void formPageIfInvalid_whenFormHasErrors_thenReturnsFormPage() {
    var form = new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("2"))
        .setWithdrawingOrganisationIds(List.of("1"));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    when(licenseeChangeService.getLicenseeChangeContext(CORRECTION, POSITION_ID))
        .thenReturn(CONTEXT_WITH_PREVIOUS_LICENSEES);
    when(licenseeChangeFormValidator.hasErrors(form, bindingResult, List.of(1))).thenReturn(true);
    when(licenceFormService.getPreselectedOrganisationUnits(List.of("2"))).thenReturn(List.of());
    when(licenceFormService.getPreselectedOrganisationUnits(List.of("1"))).thenReturn(List.of());

    var result = licenseeChangeFormHandler.formPageIfInvalid(
        new ChangeFormTarget.ExecutedPosition(CORRECTION, POSITION),
        form,
        bindingResult
    );

    var previousNames = List.of("Withdrawing licensee");
    var expected = expectedFormPage(
        form,
        previousNames,
        SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
            .searchOrganisationUnitsWithConstraints(null, null, previousNames)),
        SearchSelectorService.routeWithConstraints(on(OrganisationUnitRestController.class)
            .searchOrganisationUnitsWithConstraints(null, previousNames, null))
    );
    assertThat(result).get().usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void save_whenExecutedPosition_thenAddsChangeToPosition() {
    licenseeChangeFormHandler.save(new ChangeFormTarget.ExecutedPosition(CORRECTION, POSITION), validForm());

    verify(licenseeChangeService)
        .addLicenseeChangeForExistingLicencePosition(POSITION, CORRECTION, List.of(2), List.of(1));
  }

  @Test
  void save_whenAddedPosition_thenAddsChangeToAddedPosition() {
    licenseeChangeFormHandler.save(
        new ChangeFormTarget.AddedPosition(CORRECTION, ADDED_POSITION_CORRECTION),
        validForm()
    );

    verify(licenseeChangeService)
        .addLicenseeChangeForAddedLicencePosition(ADDED_POSITION_CORRECTION, List.of(2), List.of(1));
  }

  @Test
  void save_whenLiveChange_thenCorrectsChange() {
    licenseeChangeFormHandler.save(new ChangeFormTarget.LiveChange(CORRECTION, POSITION, LIVE_CHANGE), validForm());

    verify(licenseeChangeService).correctExistingLicenseeChange(
        POSITION,
        CORRECTION,
        List.of(2),
        List.of(1),
        LIVE_CHANGE.getId().toString()
    );
  }

  private static LicenseeChangeForm validForm() {
    return new LicenseeChangeForm()
        .setJoiningOrganisationIds(List.of("2"))
        .setWithdrawingOrganisationIds(List.of("1"));
  }

  private static ModelAndView expectedFormPage(
      LicenseeChangeForm form,
      List<String> previousLicenseeNames,
      String joiningSearchEndpoint,
      String withdrawingSearchEndpoint
  ) {
    return new ModelAndView(LicenseeChangeFormHandler.TEMPLATE_NAME)
        .addObject("previousLicenseeNames", previousLicenseeNames)
        .addObject("pageTitle", LicenseeChangeFormHandler.PAGE_TITLE)
        .addObject("form", form)
        .addObject("preselectedJoiningOrgUnits", List.of())
        .addObject("preselectedWithdrawingOrgUnits", List.of())
        .addObject("joiningOrganisationUnitSearchEndpoint", joiningSearchEndpoint)
        .addObject("withdrawingOrganisationUnitSearchEndpoint", withdrawingSearchEndpoint);
  }
}
