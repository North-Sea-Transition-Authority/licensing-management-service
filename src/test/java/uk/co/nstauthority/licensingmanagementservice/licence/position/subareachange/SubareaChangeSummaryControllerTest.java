package uk.co.nstauthority.licensingmanagementservice.licence.position.subareachange;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ContextConfiguration(classes = SubareaChangeSummaryController.class)
@ActiveProfiles("test")
class SubareaChangeSummaryControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final Feature BLOCK_FEATURE = FeatureTestUtil.builder()
      .withFeatureName("30/1")
      .build();
  private static final UUID BLOCK_FEATURE_ID = BLOCK_FEATURE.getId();
  private static final SubareaDetails REPLACED_SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
  private static final SubareaOperation OPERATION = new SubareaOperation(
      UUID.randomUUID(),
      BLOCK_FEATURE_ID,
      List.of(new SubareaTransfer(
          List.of(REPLACED_SUBAREA),
          List.of(new SubareaDetails(UUID.randomUUID(), "Subarea A2", "A2"))
      )),
      null
  );
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePositionChange CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(CHANGE_ID))
      .withLicencePosition(POSITION)
      .withOperations(List.of(OPERATION))
      .build();

  @MockitoBean
  private SubareaChangeService subareaChangeService;

  @MockitoBean
  private LicencePositionSpatialService licencePositionSpatialService;

  @MockitoBean
  private SubareaChangeSummarySectionService subareaChangeSummarySectionService;

  @MockitoBean
  private FeatureService featureService;

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE.getId())).thenReturn(LICENCE);
    when(licencePositionChangeService.findById(CHANGE.getId())).thenReturn(Optional.of(CHANGE));
  }

  @Test
  void renderSubareaChangeSummary_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(summaryUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderSubareaChangeSummary() throws Exception {
    var stateBefore = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(REPLACED_SUBAREA));
    var stateAtEnd = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, OPERATION.outputSubareas());
    var summarySections = List.of(new SummarySection(10, List.of(SummaryItem.withCards("Subarea A", List.of()))));

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(subareaChangeService.getLiveSubareaOperationOrThrow(CHANGE)).thenReturn(OPERATION);
    when(licencePositionSpatialService.getExecutedStateGoingIntoChange(POSITION, CHANGE_ID)).thenReturn(stateBefore);
    when(licencePositionSpatialService.getExecutedStateGoingIntoChange(POSITION, null)).thenReturn(stateAtEnd);
    when(subareaChangeSummarySectionService.getSummarySections(
        new SubareaChangeSummaryContext.LiveChange(
            BLOCK_FEATURE_ID,
            List.of(REPLACED_SUBAREA),
            OPERATION.transfers(),
            stateBefore,
            stateAtEnd,
            null
        ),
        regulatorUser
    )).thenReturn(summarySections);
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK_FEATURE);

    mockMvc.perform(get(summaryUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/subarea/subareaChangeReview"),
            model().attribute("readOnly", true),
            model().attribute("pageTitle", "Subarea change for Block 30/1"),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("accordionId", CHANGE_ID),
            model().attribute("summarySections", summarySections),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionController.class)
                .renderLicencePosition(LICENCE, POSITION, null, null))),
            content().string(not(containsString("value=\"Submit\"")))
        );
  }

  @Test
  void renderSubareaChangeSummary_whenPositionIsOnAnotherLicence_thenNotFound() throws Exception {
    var otherLicence = LicenceTestUtil.builder().withId(2).build();
    var positionOnOtherLicence = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(otherLicence)
        .build();

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(positionOnOtherLicence));

    mockMvc.perform(get(summaryUrl()).with(user(regulatorUser)))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @EnumSource(value = LicencePositionStatus.class, names = "EXECUTED", mode = EnumSource.Mode.EXCLUDE)
  void renderSubareaChangeSummary_whenPositionIsNotExecuted_thenNotFound(LicencePositionStatus status) throws Exception {
    var position = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withStatus(status)
        .build();

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));

    mockMvc.perform(get(summaryUrl()).with(user(regulatorUser)))
        .andExpect(status().isNotFound());

    verifyNoInteractions(licencePositionSpatialService, subareaChangeSummarySectionService);
  }

  private static String summaryUrl() {
    return ReverseRouter.route(on(SubareaChangeSummaryController.class)
        .renderSubareaChangeSummary(LICENCE, POSITION, CHANGE, null));
  }
}
