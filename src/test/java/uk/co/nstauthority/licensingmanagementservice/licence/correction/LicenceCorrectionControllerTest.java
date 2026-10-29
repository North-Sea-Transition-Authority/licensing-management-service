package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController.FILTER_SESSION_ATTRIBUTE;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.ReviewCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.FilteredCorrectionTimeline;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTimelineView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilter;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterOptions;
import uk.co.nstauthority.licensingmanagementservice.licence.position.filter.LicenceTimelineFilterSession;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicenceCorrectionController.class)
@ActiveProfiles("test")
class LicenceCorrectionControllerTest extends AbstractControllerTest {

  @MockitoBean
  private CorrectionDetailsViewService correctionDetailsViewService;

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final int LICENCE_ID = 10;
  private static final String LICENCE_REFERENCE = "P1234";
  private static final String CORRECTION_REFERENCE = "COR-1";
  private static final String REASON = "Typo in executed position";
  private static final String PAGE_CAPTION = "Licence - P1234";
  private static final LicenceType LICENCE_TYPE = LicenceType.SEAWARD_PRODUCTION;
  private static final long ALLOCATED_TO_WUA_ID = 123L;
  private static final String PAGE_TITLE = "%s - licence correction".formatted(LICENCE_REFERENCE);
  private static final CorrectionDetailsView CORRECTION_DETAILS = new CorrectionDetailsView(
      CORRECTION_REFERENCE,
      REASON,
      "Jane Doe",
      LicenceCorrectionStatus.IN_PROGRESS.getDisplayName(),
      LICENCE_REFERENCE,
      "5 June 2026"
  );
  private static final Integer ORGANISATION_ID = 10;
  private static final LicenceTimelineFilterOptions FILTER_OPTIONS = new LicenceTimelineFilterOptions(
      Map.of(LicenceOperation.SET_EQUITY, "Set equity"),
      Map.of(String.valueOf(ORGANISATION_ID), "Shell plc")
  );
  private static final LicenceTimelineFilter SET_EQUITY_FILTER =
      new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of());

  @Test
  void renderCorrection_whenNotLoggedIn() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .build();

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderCorrection_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(CORRECTION_DETAILS);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correctionDetails", CORRECTION_DETAILS),
            model().attribute("addPositionUrl",
                ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                    .renderAddLicencePositionCorrection(correction))),
            model().attributeExists("licencePositionPageView"),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(correction))),
            model().attribute("reviewCorrectionUrl", ReverseRouter.route(on(ReviewCorrectionController.class)
                .renderReviewCorrection(correction))),
            content().string(containsString("Licence reference")),
            content().string(containsString(LICENCE_REFERENCE))
        );
  }

  @Test
  void renderCorrection_whenExecutedPositionsExist_redirectsToLatestExecutedPosition() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();

    var earlierExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withStatus(LicencePositionStatus.EXECUTED).build();
    var latestExecuted = LicencePositionTestUtil.newBuilder()
        .withPositionDate(LocalDate.of(2026, Month.JUNE, 1)).withStatus(LicencePositionStatus.EXECUTED).build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence))
        .thenReturn(List.of(earlierExecuted, latestExecuted));
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction, null)))
            .with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, latestExecuted, null))));
  }

  @Test
  void renderCorrection_whenOnlyAddedPositionsExist_redirectsToAddedPosition() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference(LICENCE_REFERENCE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .build();

    var addedPosition = LicencePositionCorrectionTestUtil.newBuilder().withId(POSITION_CORRECTION_ID).build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(List.of());
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction))
        .thenReturn(List.of(addedPosition));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction, null)))
            .with(user(regulatorUser)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(correction, addedPosition, null))));
  }

  @Test
  void renderLicencePosition_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
    var position = LicencePositionTestUtil.newBuilder().withLicence(licence).build();
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(),
        "1 Jan 2026",
        "REF-1",
        List.of(),
        null,
        position.getId(),
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of(),
        LicenceTimelineFilterOptions.none(),
        false
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.findById(position.getId())).thenReturn(Optional.of(position));
    when(licencePositionViewService.getCorrectionPositionPageView(correction, position, LicenceTimelineFilter.empty())).thenReturn(pageView);
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(CORRECTION_DETAILS);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, position, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correctionDetails", CORRECTION_DETAILS),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(correction)))
        );
  }

  @Test
  void renderLicencePosition_whenTheCorrectionIsComplete_thenItRedirectsToTheAppliedCorrection() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();
    var position = LicencePositionTestUtil.newBuilder().withLicence(licence).build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionService.findById(position.getId())).thenReturn(Optional.of(position));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, position, null)))
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(correction))));
  }

  @Test
  void renderLicencePosition_whenPartialSurrenderStaged_rendersThePartialSurrenderCard() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
    var position = LicencePositionTestUtil.newBuilder().withLicence(licence).build();
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(LicencePositionTimelineView.builder()
            .withPositionId(position.getId())
            .withUrl("/position")
            .withRegulatorReference("REF-1")
            .withFormattedPositionDate("1 Jan 2026")
            .build()),
        "1 Jan 2026",
        "REF-1",
        List.of(
            new PartialSurrenderChangeView("1 August 2026",
                List.of(
                    new PartialSurrenderChangeView.BlockRow("30/1a", "Full surrender"),
                    new PartialSurrenderChangeView.BlockRow("30/2", "Partial surrender")),
                LicencePositionChangeType.ADD_CHANGE,
                new ChangeViewUrls("/correct", null, "/undo", null, null, null))),
        new LicencePositionStateView(
            new AdministratorStateView("Operator Ltd"),
            new LicenseeStateView(List.of("Licensee")),
            "",
            List.of(),
            List.of(),
            List.of()
        ),
        position.getId(),
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of(),
        LicenceTimelineFilterOptions.none(),
        false
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionService.findById(position.getId())).thenReturn(Optional.of(position));
    when(licencePositionViewService.getCorrectionPositionPageView(correction, position, LicenceTimelineFilter.empty())).thenReturn(pageView);
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(CORRECTION_DETAILS);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, position, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correctionDetails", CORRECTION_DETAILS),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(correction))));
  }

  @Test
  void renderAddedPosition_whenAllocatedToUser() throws Exception {
    var licence = LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceReference(LICENCE_REFERENCE)
        .withLicenceType(LICENCE_TYPE)
        .build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(licence)
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .build();
    var pageView = LicencePositionPageView.fromAddedPosition(
        List.of(),
        "1 Jan 2026",
        "REF-1",
        List.of(),
        null,
        POSITION_CORRECTION_ID,
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of(),
        LicenceTimelineFilterOptions.none(),
        false
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID)).thenReturn(Optional.of(positionCorrection));
    when(licenceService.getLicencePageCaption(licence)).thenReturn(PAGE_CAPTION);
    when(licencePositionViewService.getCorrectionAddedPositionPageView(
        correction,
        positionCorrection,
        LicenceTimelineFilter.empty()
    )).thenReturn(pageView);
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(CORRECTION_DETAILS);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderAddedPosition(correction, positionCorrection, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("pageCaption", PAGE_CAPTION),
            model().attribute("correctionDetails", CORRECTION_DETAILS),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
                .renderCancelCorrection(correction)))
        );

  }

  @Test
  void renderCorrection_whenNotAllocatedToUser() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderCorrection_whenFilterInSession_thenRedirectsToLatestMatchingPosition() throws Exception {
    var correction = allocatedCorrection();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionViewService.getFilteredCorrectionTimeline(correction, SET_EQUITY_FILTER))
        .thenReturn(new FilteredCorrectionTimeline.LatestMatchingPosition("/matching-position"));

    mockMvc.perform(get(correctionUrl(correction))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(CORRECTION_ID, SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl("/matching-position"));
  }

  @Test
  void renderCorrection_whenFilterInSessionMatchesNothing_thenNoMatchingPositionsRendered() throws Exception {
    var correction = allocatedCorrection();
    var filter = new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of(ORGANISATION_ID));

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(correction.getLicence())).thenReturn(PAGE_CAPTION);
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(CORRECTION_DETAILS);
    when(licencePositionViewService.getFilteredCorrectionTimeline(correction, filter))
        .thenReturn(new FilteredCorrectionTimeline.NoMatchingPositions(
            LicencePositionPageView.noMatchingPositions(LICENCE_TYPE, FILTER_OPTIONS)
        ));

    mockMvc.perform(get(correctionUrl(correction))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(CORRECTION_ID, filter))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/correction/viewCorrection"),
            model().attribute("licencePositionPageView",
                LicencePositionPageView.noMatchingPositions(LICENCE_TYPE, FILTER_OPTIONS)),
            model().attribute("form", allOf(
                hasProperty("changeTypes", contains(LicenceOperation.SET_EQUITY)),
                hasProperty("organisationIds", contains(ORGANISATION_ID))
            )),
            model().attribute("filterUrl", filterUrl(correction)),
            model().attribute("clearFilterUrl", clearFiltersUrl(correction)),
            content().string(allOf(
                containsString("<option value=\"%s\" selected>Shell plc</option>".formatted(ORGANISATION_ID)),
                containsString("No positions match the selected filters.")
            ))
        );
  }

  @Test
  void renderCorrection_whenFilterIsForAnotherCorrection_thenRedirectsToLatestExecutedPosition() throws Exception {
    var correction = allocatedCorrection();
    var latestExecuted = LicencePositionTestUtil.newBuilder().withStatus(LicencePositionStatus.EXECUTED).build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionService.getExecutedChronologicalLicencePositions(correction.getLicence()))
        .thenReturn(List.of(latestExecuted));
    when(licencePositionCorrectionService.getAddedLicencePositionCorrections(correction)).thenReturn(List.of());

    mockMvc.perform(get(correctionUrl(correction))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(UUID.randomUUID(), SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, latestExecuted, null))));
  }

  @Test
  void renderLicencePosition_whenFilterInSession_thenFilteredPageViewIsRendered() throws Exception {
    var correction = allocatedCorrection();
    var position = LicencePositionTestUtil.newBuilder().withLicence(correction.getLicence()).build();
    var pageView = LicencePositionPageView.fromExecutedPosition(
        List.of(),
        "1 Jan 2026",
        "REF-1",
        List.of(),
        null,
        position.getId(),
        LicencePositionPageView.Actions.none(),
        LICENCE_TYPE,
        List.of(),
        FILTER_OPTIONS,
        true
    );

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licenceService.getLicencePageCaption(correction.getLicence())).thenReturn(PAGE_CAPTION);
    when(correctionDetailsViewService.getDetailsView(correction)).thenReturn(CORRECTION_DETAILS);
    when(licencePositionService.findById(position.getId())).thenReturn(Optional.of(position));
    when(licencePositionViewService.getCorrectionPositionPageView(correction, position, SET_EQUITY_FILTER))
        .thenReturn(pageView);

    mockMvc.perform(get(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, position, null)))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(CORRECTION_ID, SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("licencePositionPageView", pageView),
            model().attribute("form", allOf(
                hasProperty("changeTypes", contains(LicenceOperation.SET_EQUITY)),
                hasProperty("organisationIds", empty())
            )),
            model().attribute("filterUrl", filterUrl(correction)),
            model().attribute("clearFilterUrl", clearFiltersUrl(correction))
        );
  }

  @Test
  void filterCorrectionTimeline_whenNotLoggedIn() throws Exception {
    var correction = allocatedCorrection();

    mockMvc.perform(post(filterUrl(correction)).with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void filterCorrectionTimeline_whenFiltersSubmitted_thenOnlyThoseOnTheCorrectionAreHeldForTheCorrection()
      throws Exception {
    var correction = allocatedCorrection();
    var filterSession = new LicenceTimelineFilterSession();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(licencePositionViewService.getCorrectionFilterOptions(correction)).thenReturn(FILTER_OPTIONS);

    mockMvc.perform(post(filterUrl(correction))
            .param("changeTypes", LicenceOperation.SET_EQUITY, "not-a-change-type")
            .param("organisationIds", String.valueOf(ORGANISATION_ID), "999")
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl(correctionUrl(correction)));

    assertThat(filterSession.getFilter(CORRECTION_ID.toString()))
        .isEqualTo(new LicenceTimelineFilter(Set.of(LicenceOperation.SET_EQUITY), Set.of(ORGANISATION_ID)));
  }

  @Test
  void filterCorrectionTimeline_whenTheCorrectionIsComplete_thenItRedirectsToTheAppliedCorrection() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder().withId(LICENCE_ID).build())
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .build();
    var filterSession = new LicenceTimelineFilterSession();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(post(filterUrl(correction))
            .param("changeTypes", LicenceOperation.SET_EQUITY)
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(redirectedUrl(ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(correction))));

    assertThat(filterSession.isEmpty()).isTrue();
  }

  @Test
  void clearCorrectionTimelineFilters_whenOtherCorrectionsFiltered_thenOnlyThisCorrectionFilterIsCleared()
      throws Exception {
    var correction = allocatedCorrection();
    var otherCorrectionId = UUID.randomUUID();
    var otherCorrectionFilter = new LicenceTimelineFilter(Set.of(), Set.of(ORGANISATION_ID));
    var filterSession = filterSession(CORRECTION_ID, SET_EQUITY_FILTER);
    filterSession.update(otherCorrectionId.toString(), otherCorrectionFilter);

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(clearFiltersUrl(correction))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession)
            .with(user(regulatorUser)))
        .andExpectAll(
            redirectedUrl(correctionUrl(correction)),
            request().sessionAttribute(FILTER_SESSION_ATTRIBUTE, filterSession)
        );

    assertThat(filterSession.getFilter(CORRECTION_ID.toString())).isEqualTo(LicenceTimelineFilter.empty());
    assertThat(filterSession.getFilter(otherCorrectionId.toString())).isEqualTo(otherCorrectionFilter);
  }

  @Test
  void clearCorrectionTimelineFilters_whenNoOtherCorrectionsFiltered_thenFilterIsRemovedFromSession()
      throws Exception {
    var correction = allocatedCorrection();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(clearFiltersUrl(correction))
            .sessionAttr(FILTER_SESSION_ATTRIBUTE, filterSession(CORRECTION_ID, SET_EQUITY_FILTER))
            .with(user(regulatorUser)))
        .andExpectAll(
            redirectedUrl(correctionUrl(correction)),
            request().sessionAttributeDoesNotExist(FILTER_SESSION_ATTRIBUTE)
        );
  }

  @Test
  void clearFilter_whenOtherCorrectionsFiltered_thenOnlyThisCorrectionFilterIsCleared() {
    var correction = allocatedCorrection();
    var otherCorrectionId = UUID.randomUUID();
    var otherCorrectionFilter = new LicenceTimelineFilter(Set.of(), Set.of(ORGANISATION_ID));
    var filterSession = filterSession(CORRECTION_ID, SET_EQUITY_FILTER);
    filterSession.update(otherCorrectionId.toString(), otherCorrectionFilter);
    var session = new MockHttpSession();
    session.setAttribute(FILTER_SESSION_ATTRIBUTE, filterSession);

    LicenceCorrectionController.clearFilter(session, correction);

    var expectedFilterSession = filterSession(otherCorrectionId, otherCorrectionFilter);
    assertThat(session.getAttribute(FILTER_SESSION_ATTRIBUTE))
        .usingRecursiveComparison()
        .isEqualTo(expectedFilterSession);
  }

  @Test
  void clearFilter_whenNoOtherCorrectionsFiltered_thenFilterIsRemovedFromSession() {
    var session = new MockHttpSession();
    session.setAttribute(FILTER_SESSION_ATTRIBUTE, filterSession(CORRECTION_ID, SET_EQUITY_FILTER));

    LicenceCorrectionController.clearFilter(session, allocatedCorrection());

    assertThat(session.getAttribute(FILTER_SESSION_ATTRIBUTE)).isNull();
  }

  private static LicenceCorrection allocatedCorrection() {
    return LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LicenceTestUtil.builder()
            .withId(LICENCE_ID)
            .withLicenceReference(LICENCE_REFERENCE)
            .withLicenceType(LICENCE_TYPE)
            .build())
        .withCorrectionReference(CORRECTION_REFERENCE)
        .withReason(REASON)
        .withAllocatedToWuaId(ALLOCATED_TO_WUA_ID)
        .build();
  }

  private static String correctionUrl(LicenceCorrection correction) {
    return ReverseRouter.route(on(LicenceCorrectionController.class).renderCorrection(correction, null));
  }

  private static String filterUrl(LicenceCorrection correction) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .filterCorrectionTimeline(correction, null, null));
  }

  private static String clearFiltersUrl(LicenceCorrection correction) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .clearCorrectionTimelineFilters(correction, null, null));
  }

  private static LicenceTimelineFilterSession filterSession(UUID correctionId, LicenceTimelineFilter filter) {
    var filterSession = new LicenceTimelineFilterSession();
    filterSession.update(correctionId.toString(), filter);
    return filterSession;
  }
}
