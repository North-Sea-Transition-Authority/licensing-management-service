package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionSubareaChangeStartController.class)
class LicencePositionSubareaChangeStartControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(116)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final String VIEW_NAME = "lms/licence/correction/change/subarea/startSubareaChange";

  private static final Feature BLOCK_30_1 = FeatureTestUtil.builder().withFeatureName("30/1").build();
  private static final Feature BLOCK_30_2 = FeatureTestUtil.builder().withFeatureName("30/2").build();
  private static final List<Feature> BLOCK_FEATURES = List.of(BLOCK_30_1, BLOCK_30_2);
  private static final Map<String, String> BLOCK_OPTIONS = Map.of(
      BLOCK_30_1.getId().toString(), "Block 30/1",
      BLOCK_30_2.getId().toString(), "Block 30/2");

  @MockitoBean
  private SubareaChangeService subareaChangeService;

  @MockitoBean
  private SubareaChangeStartFormValidator subareaChangeStartFormValidator;

  @Captor
  private ArgumentCaptor<SubareaOperation> subareaOperationCaptor;


  @Test
  void renderForExecutedPosition_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenLicenceIsNotProduction_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(carbonStorageLicence)
            .build()));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_rendersFormWithBlockOptions() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(Optional.empty());
    when(licencePositionCorrectionService.getCommittedChangeOfType(null, SubareaOperation.class))
        .thenReturn(Optional.empty());
    when(licencePositionService.getBlockFeatures(licencePosition)).thenReturn(BLOCK_FEATURES);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", "Subarea change"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("blockOptions", BLOCK_OPTIONS),
            model().attribute("backLinkUrl", addChangeUrlForExecutedPosition()));
  }

  @Test
  void renderForExecutedPosition_whenChangeAlreadyStaged_prefillsSelectedBlock() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder().build();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionCorrectionService.findUpdatePositionCorrection(correction, licencePosition))
        .thenReturn(Optional.of(positionCorrection));
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, SubareaOperation.class))
        .thenReturn(Optional.of(new SubareaOperation(BLOCK_30_1.getId())));
    when(licencePositionService.getBlockFeatures(licencePosition)).thenReturn(BLOCK_FEATURES);

    var result = mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andReturn();

    var form = (SubareaChangeStartForm) result.getModelAndView().getModel().get("form");
    assertThat(form.getFeatureId()).isEqualTo(BLOCK_30_1.getId().toString());
  }

  @Test
  void submitForExecutedPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.getBlockFeatures(licencePosition)).thenReturn(BLOCK_FEATURES);
    when(subareaChangeStartFormValidator.hasErrors(
        any(SubareaChangeStartForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockOptions"),
            model().attribute("backLinkUrl", addChangeUrlForExecutedPosition()));

    verify(subareaChangeService, never()).commitSubareaChangeForExecutedPosition(any(), any(), any());
  }

  @Test
  void submitForExecutedPosition_whenValid_commitsAndRedirectsToPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = executedPosition();
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(licencePosition);
    when(licencePositionService.getBlockFeatures(licencePosition)).thenReturn(BLOCK_FEATURES);
    when(subareaChangeStartFormValidator.hasErrors(
        any(SubareaChangeStartForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(false);

    var form = new SubareaChangeStartForm();
    form.setFeatureId(BLOCK_30_1.getId().toString());

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .submitForExecutedPosition(CORRECTION_ID, POSITION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(CORRECTION_ID, POSITION_ID, null))));

    verify(subareaChangeService)
        .commitSubareaChangeForExecutedPosition(eq(correction), eq(licencePosition), subareaOperationCaptor.capture());
    assertThat(subareaOperationCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(new SubareaOperation(BLOCK_30_1.getId()));
  }

  @Test
  void renderForAddedPosition_rendersFormWithBlockOptions() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(licencePositionCorrectionService.getCommittedChangeOfType(positionCorrection, SubareaOperation.class))
        .thenReturn(Optional.empty());
    when(licencePositionService.getBlockFeaturesForCorrection(positionCorrection)).thenReturn(BLOCK_FEATURES);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("blockOptions", BLOCK_OPTIONS),
            model().attribute("backLinkUrl", addChangeUrlForAddedPosition()));
  }

  @Test
  void submitForAddedPosition_whenInvalid_rendersFormAndCommitsNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(licencePositionService.getBlockFeaturesForCorrection(positionCorrection)).thenReturn(BLOCK_FEATURES);
    when(subareaChangeStartFormValidator.hasErrors(
        any(SubareaChangeStartForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockOptions"),
            model().attribute("backLinkUrl", addChangeUrlForAddedPosition()));

    verify(subareaChangeService, never()).commitSubareaChange(any(), any());
  }

  @Test
  void submitForAddedPosition_whenValid_commitsAndRedirectsToAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = addedPositionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(licencePositionService.getBlockFeaturesForCorrection(positionCorrection)).thenReturn(BLOCK_FEATURES);
    when(subareaChangeStartFormValidator.hasErrors(
        any(SubareaChangeStartForm.class), any(BindingResult.class), eq(BLOCK_FEATURES)))
        .thenReturn(false);

    var form = new SubareaChangeStartForm();
    form.setFeatureId(BLOCK_30_2.getId().toString());

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionSubareaChangeStartController.class)
            .submitForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null))));

    verify(subareaChangeService).commitSubareaChange(eq(positionCorrection), subareaOperationCaptor.capture());
    assertThat(subareaOperationCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(new SubareaOperation(BLOCK_30_2.getId()));
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePosition executedPosition() {
    return LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .build();
  }

  private LicencePositionCorrection addedPositionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .build();
  }

  private static String addChangeUrlForExecutedPosition() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(CORRECTION_ID, POSITION_ID, null));
  }

  private static String addChangeUrlForAddedPosition() {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(CORRECTION_ID, POSITION_CORRECTION_ID, null));
  }
}
