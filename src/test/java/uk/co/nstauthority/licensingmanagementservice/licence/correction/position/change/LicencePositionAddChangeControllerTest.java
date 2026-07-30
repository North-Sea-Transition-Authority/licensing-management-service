package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionAddChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class LicencePositionAddChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private AddPositionChangeFormValidator addPositionChangeFormValidator;

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceReference("CS/1").build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final String VIEW_NAME = "lms/licence/correction/change/addChange";

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionCorrection givenPositionCorrection(LicenceCorrection correction) {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    return positionCorrection;
  }

  @Test
  void render_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void render_whenCarbonStorageLicence_offersAdministratorAndSetEquity() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licenceService.isCarbonStorageLicence(LICENCE)).thenReturn(true);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "backLinkUrl"),
            model().attribute("pageTitle", "Add change"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("changeTypeOptions", Map.of(
                "ADMINISTRATOR_CHANGE", "Administrator change",
                "SET_EQUITY", "Set equity")));
  }

  @Test
  void render_whenNotCarbonStorageLicence_offersAdministratorOnly() throws Exception {
    givenCorrectionAllocatedToUser();
    when(licenceService.isCarbonStorageLicence(LICENCE)).thenReturn(false);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of(
                "ADMINISTRATOR_CHANGE", "Administrator change")));
  }

  @Test
  void submit_whenInvalid_rendersForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenPositionCorrection(correction);
    var form = new AddPositionChangeForm();

    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().isOk())
        .andExpect(view().name(VIEW_NAME));
  }

  @Test
  void submit_whenAdministratorChange_redirectsToAdministratorChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenPositionCorrection(correction);
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
  void submit_whenSetEquity_redirectsToSetEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenPositionCorrection(correction);
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
            .showLicencePositionSetEquity(CORRECTION_ID, POSITION_CORRECTION_ID, null))));
  }
}