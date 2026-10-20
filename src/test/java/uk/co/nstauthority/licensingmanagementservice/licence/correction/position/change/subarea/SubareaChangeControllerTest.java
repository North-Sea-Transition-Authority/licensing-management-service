package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.assertj.core.api.Assertions.assertThat;
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
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = SubareaChangeController.class)
class SubareaChangeControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final int ED50_WKID = 4230;
  private static final String VIEW_NAME = "lms/licence/correction/change/subarea/subareaChange";
  private static final String NO_CHANGE_ERROR = "You must make a change to the subareas before completing";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .build();
  private static final Feature BLOCK_FEATURE = FeatureTestUtil.builder()
      .withFeatureName("30/1")
      .withCoordinateSystem(CoordinateSystem.ED50)
      .build();
  private static final UUID BLOCK_FEATURE_ID = BLOCK_FEATURE.getId();

  @MockitoBean
  private SubareaChangeService subareaChangeService;

  @MockitoBean
  private FeatureService featureService;

  @MockitoBean
  private SubareaChangeValidator subareaChangeValidator;

  @Test
  void renderSubareaChange_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(subareaChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSubareaChange_whenLicenceIsNotProduction_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    var notAllowedCorrection = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(carbonStorageLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(notAllowedCorrection));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(notAllowedCorrection));

    mockMvc.perform(get(subareaChangeUrl())
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSubareaChange_rendersMapWithCommandJourneyAndSrsWkid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenSubareaChange(addedPositionCorrection(correction));
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK_FEATURE);

    mockMvc.perform(get(subareaChangeUrl())
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("commandJourneyId", COMMAND_JOURNEY_ID),
            model().attribute("srsWkid", ED50_WKID),
            model().attribute("pageTitle", "Subarea change"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(correction, positionCorrection))));
  }

  @Test
  void complete_whenAddedPosition_redirectsToAddedPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = givenSubareaChange(addedPositionCorrection(correction));
    when(subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID)).thenReturn(false);

    mockMvc.perform(post(completeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderAddedPosition(correction, positionCorrection))),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change saved")
                .build()));
  }

  @Test
  void complete_whenExecutedPosition_redirectsToPosition() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var licencePosition = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .build();
    givenSubareaChange(updatePositionCorrection(correction, licencePosition));
    when(subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID)).thenReturn(false);

    mockMvc.perform(post(completeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction, licencePosition))),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Subarea change saved")
                .build()));
  }

  @Test
  void complete_whenNoChangeMade_rendersMapWithError() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    givenSubareaChange(addedPositionCorrection(correction));
    when(subareaChangeValidator.hasErrors(COMMAND_JOURNEY_ID)).thenReturn(true);
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK_FEATURE);

    var result = mockMvc.perform(post(completeUrl())
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("commandJourneyId", COMMAND_JOURNEY_ID),
            model().attribute("mapErrorMessage", NO_CHANGE_ERROR))
        .andReturn();

    assertThat(result.getModelAndView().getModel().get("errorSummaryItems"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(new ErrorSummaryItem(1, "subarea-change", NO_CHANGE_ERROR)));
  }

  private LicencePositionCorrection givenSubareaChange(LicencePositionCorrection positionCorrection) {
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(positionCorrection));
    when(subareaChangeService.getSubareaOperationOrThrow(positionCorrection, BLOCK_FEATURE_ID))
        .thenReturn(new SubareaOperation(UUID.randomUUID(), BLOCK_FEATURE_ID, List.of(), List.of(), COMMAND_JOURNEY_ID));
    return positionCorrection;
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionCorrection addedPositionCorrection(LicenceCorrection correction) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .build();
  }

  private LicencePositionCorrection updatePositionCorrection(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withChangeType(LicencePositionCorrectionChangeType.UPDATE_POSITION)
        .withTargetLicencePosition(licencePosition)
        .build();
  }

  private static String subareaChangeUrl() {
    return ReverseRouter.route(on(SubareaChangeController.class)
        .renderSubareaChange(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID));
  }

  private static String completeUrl() {
    return ReverseRouter.route(on(SubareaChangeController.class)
        .complete(CORRECTION, POSITION_CORRECTION, BLOCK_FEATURE_ID, null));
  }
}
