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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicencePositionLicenseeChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.LicencePositionPartialSurrenderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.SingleBlockSurrender;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.LicencePositionSubareaChangeStartController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionAddChangeController.class)
@ActiveProfiles("test")
class LicencePositionAddChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private AddPositionChangeFormValidator addPositionChangeFormValidator;

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceReference("P/1")
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .build();
  private static final Licence CARBON_STORAGE_LICENCE = LicenceTestUtil.builder()
      .withId(2)
      .withLicenceReference("CS/1")
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .build();
  private static final Licence EXPLORATION_LICENCE = LicenceTestUtil.builder()
      .withId(3)
      .withLicenceReference("E/1")
      .withLicenceType(LicenceType.SEAWARD_EXPLORATION)
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final String VIEW_NAME = "lms/licence/correction/change/addChange";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .build();

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    return givenCorrectionAllocatedToUser(LICENCE);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser(Licence licence) {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(licence).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePosition givenExecutedPosition() {
    return givenExecutedPosition(LICENCE);
  }

  private LicencePosition givenExecutedPosition(Licence licence) {
    var licencePosition = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(licence).build();
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(licencePosition));
    return licencePosition;
  }

  private LicencePositionCorrection givenAddedPositionCorrection(LicenceCorrection correction) {
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .build();
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(positionCorrection));
    return positionCorrection;
  }

  @Test
  void renderForExecutedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(addPositionChangeFormValidator);
  }

  @Test
  void renderForExecutedPosition_whenCarbonStorage_offersSetEquityTransferEquityAndPartialSurrender()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(CARBON_STORAGE_LICENCE);
    var licencePosition = givenExecutedPosition(CARBON_STORAGE_LICENCE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(correction, licencePosition)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form"),
            model().attribute("pageTitle", "Add change"),
            model().attribute("pageCaption", CARBON_STORAGE_LICENCE.getLicenceReference()),
            model().attribute("changeTypeOptions", Map.of(
                "SET_EQUITY", "Set equity",
                "TRANSFER_EQUITY", "Transfer equity",
                "PARTIAL_SURRENDER", "Partial surrender",
                "LICENSEE", "Licensee change")),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction, licencePosition))));
  }

  @Test
  void renderForExecutedPosition_whenProduction_offersAdministratorPartialSurrenderAndSubarea() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(correction, licencePosition)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of(
                "ADMINISTRATOR", "Administrator change",
                "PARTIAL_SURRENDER", "Partial surrender",
                "SUBAREA", "Subarea change",
                "LICENSEE", "Licensee change")));
  }

  @Test
  void renderForExecutedPosition_whenExploration_offersNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser(EXPLORATION_LICENCE);
    var licencePosition = givenExecutedPosition(EXPLORATION_LICENCE);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForExecutedPosition(correction, licencePosition)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of()));
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "changeTypeOptions", "backLinkUrl"));
  }

  @Test
  void submitForExecutedPosition_whenLicenseeChange_redirectsToLicenseeChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.LICENSEE.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(correction, licencePosition))));
  }

  @Test
  void submitForExecutedPosition_whenAdministratorChange_redirectsToAdministratorChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(correction, licencePosition))));
  }

  @Test
  void submitForExecutedPosition_whenSetEquity_redirectsToSetEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
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
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForExecutedPosition(correction, licencePosition))));
  }

  @Test
  void submitForExecutedPosition_whenTransferEquity_redirectsToTransferEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.TRANSFER_EQUITY.name());

    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
            .renderForExecutedPosition(correction, licencePosition))));
  }

  @Test
  void render_whenNotAllocatedToUser_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser)).thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(addPositionChangeFormValidator);
  }

  @Test
  void renderForAddedPosition_whenCarbonStorage_offersSetEquityTransferEquityAndPartialSurrender()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser(CARBON_STORAGE_LICENCE);
    var positionCorrection = givenAddedPositionCorrection(correction);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(correction, positionCorrection)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form"),
            model().attribute("pageTitle", "Add change"),
            model().attribute("pageCaption", CARBON_STORAGE_LICENCE.getLicenceReference()),
            model().attribute("changeTypeOptions", Map.of(
                "SET_EQUITY", "Set equity",
                "TRANSFER_EQUITY", "Transfer equity",
                "PARTIAL_SURRENDER", "Partial surrender",
                "LICENSEE", "Licensee change")),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(correction, positionCorrection))));
  }

  @Test
  void renderForAddedPosition_whenProduction_offersAdministratorPartialSurrenderAndSubarea() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(correction, positionCorrection)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of(
                "ADMINISTRATOR", "Administrator change",
                "PARTIAL_SURRENDER", "Partial surrender",
                "SUBAREA", "Subarea change",
                "LICENSEE", "Licensee change")));
  }

  @Test
  void renderForAddedPosition_whenExploration_offersNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser(EXPLORATION_LICENCE);
    var positionCorrection = givenAddedPositionCorrection(correction);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .renderForAddedPosition(correction, positionCorrection)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("changeTypeOptions", Map.of()));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    var form = new AddPositionChangeForm();
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "changeTypeOptions", "backLinkUrl"));
  }

  @Test
  void submitForAddedPosition_whenLicenseeChange_redirectsToLicenseeChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.LICENSEE.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(correction, positionCorrection))));
  }

  @Test
  void submitForAddedPosition_whenAdministratorChange_redirectsToAdministratorChangeForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.ADMINISTRATOR.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(correction, positionCorrection))));
  }

  @Test
  void submitForAddedPosition_whenSetEquity_redirectsToSetEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.SET_EQUITY.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSetEquityController.class)
            .renderForAddedPosition(correction, positionCorrection))));
  }

  @Test
  void submitForExecutedPosition_whenPartialSurrenderOfMoreThanOneBlock_redirectsToSurrenderDetailsForm()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.stageSingleBlockSurrenderForExecutedPosition(correction, licencePosition))
        .thenReturn(Optional.empty());

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForExecutedPosition(correction, licencePosition))));
  }

  @Test
  void submitForExecutedPosition_whenPartialSurrenderOfASingleBlock_redirectsToDefineAreaForTheStagedSurrender()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    var stagedPositionCorrection = LicencePositionCorrectionTestUtil.newBuilder().withId(POSITION_CORRECTION_ID).build();
    var block = FeatureTestUtil.builder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.stageSingleBlockSurrenderForExecutedPosition(correction, licencePosition))
        .thenReturn(Optional.of(new SingleBlockSurrender(stagedPositionCorrection, block)));

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderDefineArea(CORRECTION, POSITION_CORRECTION, block.getId()))));
  }

  @Test
  void submitForExecutedPosition_whenSubarea_redirectsToSubareaChangeStartForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = givenExecutedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(positionCorrection);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.SUBAREA.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForExecutedPosition(correction, licencePosition, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForExecutedPosition(correction, licencePosition))));
  }

  @Test
  void submitForAddedPosition_whenPartialSurrenderOfMoreThanOneBlock_redirectsToSurrenderDetailsForm()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);
    when(partialSurrenderCorrectionService.stageSingleBlockSurrenderForAddedPosition(positionCorrection))
        .thenReturn(Optional.empty());

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .renderForAddedPosition(correction, positionCorrection))));
  }

  @Test
  void submitForAddedPosition_whenPartialSurrenderOfASingleBlock_redirectsToDefineAreaForTheStagedSurrender()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);
    var block = FeatureTestUtil.builder().build();
    when(partialSurrenderCorrectionService.stageSingleBlockSurrenderForAddedPosition(positionCorrection))
        .thenReturn(Optional.of(new SingleBlockSurrender(positionCorrection, block)));

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.PARTIAL_SURRENDER.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderDefineArea(CORRECTION, POSITION_CORRECTION, block.getId()))));
  }

  @Test
  void submitForAddedPosition_whenSubarea_redirectsToSubareaChangeStartForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.SUBAREA.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForAddedPosition(correction, positionCorrection))));
  }

  @Test
  void submitForAddedPosition_whenTransferEquity_redirectsToTransferEquityForm() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenAddedPositionCorrection(correction);

    var form = new AddPositionChangeForm();
    form.setChangeType(AddPositionChangeType.TRANSFER_EQUITY.name());
    when(addPositionChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(correction), eq(positionCorrection)))
        .thenReturn(false);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAddChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicencePositionTransferEquityController.class)
            .renderForAddedPosition(correction, positionCorrection))));
  }
}
