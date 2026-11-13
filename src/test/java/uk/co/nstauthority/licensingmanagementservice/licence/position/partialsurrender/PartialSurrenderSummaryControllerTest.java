package uk.co.nstauthority.licensingmanagementservice.licence.position.partialsurrender;

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
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummaryContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderSummarySectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ContextConfiguration(classes = PartialSurrenderSummaryController.class)
@ActiveProfiles("test")
class PartialSurrenderSummaryControllerTest extends AbstractControllerTest {

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
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final LicencePositionChange CHANGE = LicencePositionChangeTestUtil.newBuilder()
      .withId(UUID.fromString(CHANGE_ID))
      .withLicencePosition(POSITION)
      .build();
  private static final PartialSurrenderOperation SURRENDER = LicenceOperation.newPartialSurrenderOperation()
      .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
      .build();

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  @MockitoBean
  private LicencePositionSpatialService licencePositionSpatialService;

  @MockitoBean
  private PartialSurrenderSummarySectionService partialSurrenderSummarySectionService;

  @BeforeEach
  void setUp() {
    when(licenceService.findLicenceByIdOrThrow(LICENCE.getId())).thenReturn(LICENCE);
    when(licencePositionChangeService.findById(CHANGE.getId())).thenReturn(Optional.of(CHANGE));
  }

  @Test
  void renderPartialSurrenderSummary_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(summaryUrl()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderPartialSurrenderSummary() throws Exception {
    var state = LicencePositionState.EMPTY.withBlock(UUID.randomUUID(), List.of());
    var summarySections = List.of(new SummarySection(10, List.of(SummaryItem.withCards("Surrender details", List.of()))));

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(POSITION));
    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(CHANGE_ID)).thenReturn(SURRENDER);
    when(licencePositionSpatialService.getExecutedStateGoingIntoChange(POSITION, CHANGE_ID)).thenReturn(state);
    when(partialSurrenderSummarySectionService.getSummarySections(
        new PartialSurrenderSummaryContext.LiveChange(null, POSITION, CHANGE_ID, SURRENDER, state),
        regulatorUser
    )).thenReturn(summarySections);

    mockMvc.perform(get(summaryUrl()).with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/change/partialSurrender/partialSurrenderReviewAndSubmit"),
            model().attribute("readOnly", true),
            model().attribute("pageTitle", PartialSurrenderSummaryController.PAGE_TITLE),
            model().attribute("pageCaption", LICENCE.getLicenceReference()),
            model().attribute("accordionId", CHANGE_ID),
            model().attribute("summarySections", summarySections),
            model().attribute("backLinkUrl", ReverseRouter.route(on(LicencePositionController.class)
                .renderLicencePosition(LICENCE, POSITION, null, null))),
            content().string(not(containsString("value=\"Submit\"")))
        );
  }

  @Test
  void renderPartialSurrenderSummary_whenPositionIsOnAnotherLicence_thenNotFound() throws Exception {
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
  void renderPartialSurrenderSummary_whenPositionIsNotExecuted_thenNotFound(LicencePositionStatus status)
      throws Exception {
    var position = LicencePositionTestUtil.newBuilder()
        .withId(POSITION_ID)
        .withLicence(LICENCE)
        .withStatus(status)
        .build();

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));

    mockMvc.perform(get(summaryUrl()).with(user(regulatorUser)))
        .andExpect(status().isNotFound());

    verifyNoInteractions(licencePositionSpatialService, partialSurrenderSummarySectionService);
  }

  private static String summaryUrl() {
    return ReverseRouter.route(on(PartialSurrenderSummaryController.class)
        .renderPartialSurrenderSummary(LICENCE, POSITION, CHANGE, null));
  }
}
