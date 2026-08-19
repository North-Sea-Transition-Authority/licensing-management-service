package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import static org.mockito.ArgumentMatchers.any;
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

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = BlockSurrenderTypeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class BlockSurrenderTypeControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final Feature BLOCK = FeatureTestUtil.builder().withFeatureName("30/1a").build();
  private static final UUID FEATURE_ID = BLOCK.getId();
  private static final String VIEW_NAME = "lms/licence/correction/change/partialSurrender/partialSurrenderType";

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;

  @Test
  void renderSurrenderTypeForm_whenNotAllocated_forbidden() throws Exception {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSurrenderTypeForm_whenLicenceIsNotProduction_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(LicenceCorrectionTestUtil.newBuilder()
            .withId(CORRECTION_ID)
            .withLicence(carbonStorageLicence)
            .build()));

    mockMvc.perform(get(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderSurrenderTypeForm_rendersFormWithBlockNameAndSurrenderTypeOptions() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("blockName", "Block 30/1a"),
            model().attribute("surrenderTypeOptions", BlockSurrenderType.getOptions()),
            model().attribute("backLinkUrl", taskListUrl()));
  }

  @Test
  void submitSurrenderTypeForm_whenInvalid_rendersFormAndSavesNothing() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .submitSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attributeExists("form", "blockName", "surrenderTypeOptions"),
            model().attribute("backLinkUrl", taskListUrl()));

    verify(partialSurrenderCorrectionService, never()).setBlockSurrenderType(any(), any(), any());
  }

  @Test
  void submitSurrenderTypeForm_whenValid_savesTypeAndRedirectsToTaskList() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = positionCorrection();
    when(licencePositionCorrectionService.getPositionCorrectionForCorrection(POSITION_CORRECTION_ID, correction))
        .thenReturn(positionCorrection);
    when(partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, FEATURE_ID))
        .thenReturn(BLOCK);
    when(blockSurrenderTypeFormValidator.hasErrors(any(BlockSurrenderTypeForm.class), any(BindingResult.class)))
        .thenReturn(false);

    var form = new BlockSurrenderTypeForm();
    form.setSurrenderType(BlockSurrenderType.FULL_SURRENDER.name());

    mockMvc.perform(post(ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .submitSurrenderTypeForm(CORRECTION_ID, POSITION_CORRECTION_ID, FEATURE_ID, null, null, null, null)))
            .with(user(regulatorUser)).with(csrf())
            .flashAttr("form", form))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(taskListUrl()));

    verify(partialSurrenderCorrectionService)
        .setBlockSurrenderType(positionCorrection, FEATURE_ID, BlockSurrenderType.FULL_SURRENDER);
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).withLicence(LICENCE).build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private LicencePositionCorrection positionCorrection() {
    return LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .build();
  }

  private static String taskListUrl() {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(CORRECTION_ID, POSITION_CORRECTION_ID, null, null));
  }
}
