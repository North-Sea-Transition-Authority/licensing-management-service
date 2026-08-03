package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionAddChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionAddChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private AddPositionChangeFormValidator addPositionChangeFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceReference("CS/1").build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final String VIEW_NAME = "lms/licence/correction/change/addChange";

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
  }


  @Test
  void renderForExecutedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(addPositionChangeFormValidator);
  }

  @Test
  void renderForExecutedPosition_whenCarbonStorage_offersAdministratorAndSetEquity() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licenceService.isCarbonStorageLicence(LICENCE)).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form"),
            model().attribute("pageTitle", "Add change"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("changeTypeOptions", Map.of(
                "ADMINISTRATOR_CHANGE", "Administrator change",
                "SET_EQUITY", "Set equity")),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void renderForExecutedPosition_whenNotCarbonStorage_offersAdministratorOnly() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licenceService.isCarbonStorageLicence(LICENCE)).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of("ADMINISTRATOR_CHANGE", "Administrator change")));
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "changeTypeOptions", "backLinkUrl"));
  }

  @Test
  void submitForExecutedPosition_whenAdministratorChange_redirectsToAdministratorChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR_CHANGE.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void submitForExecutedPosition_whenSetEquity_redirectsToSetEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null))));
  }

  @Test
  void renderForAddedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(addPositionChangeFormValidator);
  }

  @Test
  void renderForAddedPosition_whenCarbonStorage_offersAdministratorAndSetEquity() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licenceService.isCarbonStorageLicence(LICENCE)).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form"),
            model().attribute("pageTitle", "Add change"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("changeTypeOptions", Map.of(
                "ADMINISTRATOR_CHANGE", "Administrator change",
                "SET_EQUITY", "Set equity")),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }

  @Test
  void renderForAddedPosition_whenNotCarbonStorage_offersAdministratorOnly() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licenceService.isCarbonStorageLicence(LICENCE)).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of("ADMINISTRATOR_CHANGE", "Administrator change")));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "changeTypeOptions", "backLinkUrl"));
  }

  @Test
  void submitForAddedPosition_whenAdministratorChange_redirectsToAdministratorChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR_CHANGE.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }

  @Test
  void submitForAddedPosition_whenSetEquity_redirectsToSetEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }
}