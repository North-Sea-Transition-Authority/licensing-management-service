package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = RemovePartialSurrenderChangeController.class)
@ActiveProfiles({"test", "enable-lms2"})
class RemovePartialSurrenderChangeControllerTest extends AbstractControllerTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicenceReference("P/1")
      .build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final String CHANGE_ID = UUID.randomUUID().toString();
  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this partial surrender?";
  private static final String VIEW_NAME =
      "lms/licence/correction/change/partialSurrender/removePartialSurrenderChange";
  private static final List<PartialSurrenderChangeView.BlockRow> BLOCK_ROWS =
      List.of(new PartialSurrenderChangeView.BlockRow("30/1", "Full surrender"));
  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JANUARY, 1);

  @MockitoBean
  private PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  private final String positionUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION_ID, POSITION_ID, null));

  @Test
  void renderRemoveExecutedPartialSurrender_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .renderRemoveExecutedPartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderRemoveExecutedPartialSurrender_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .renderRemoveExecutedPartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderRemoveExecutedPartialSurrender_whenTheSurrenderHasItsOwnDate_thenThatDateIsShown() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = positionWithId();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    givenExecutedSurrender(SURRENDER_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .renderRemoveExecutedPartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", REMOVE_PAGE_TITLE),
            model().attribute("primaryButtonText", "Remove partial surrender"),
            model().attribute("surrenderDate", "5 June 2026"),
            model().attribute("blockRows", BLOCK_ROWS),
            model().attribute("cancelUrl", positionUrl)
        );

    verify(licencePositionCorrectionService, never()).getEffectivePositionDate(correction, position);
  }

  @Test
  void renderRemoveExecutedPartialSurrender_whenTheSurrenderHasNoDate_thenTheEffectivePositionDateIsShown()
      throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = positionWithId();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    givenExecutedSurrender(null);
    when(licencePositionCorrectionService.getEffectivePositionDate(correction, position))
        .thenReturn(POSITION_DATE);

    mockMvc.perform(get(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .renderRemoveExecutedPartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            model().attribute("surrenderDate", "1 January 2026"),
            model().attribute("blockRows", BLOCK_ROWS)
        );
  }

  @Test
  void removePartialSurrender_whenNotLoggedIn() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .removePartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void removePartialSurrender_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(post(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .removePartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden());

    verifyNoInteractions(partialSurrenderCorrectionService);
  }

  @Test
  void removePartialSurrender_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = positionWithId();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);

    mockMvc.perform(post(ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
            .removePartialSurrender(CORRECTION_ID, POSITION_ID, CHANGE_ID, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(positionUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Partial surrender removed")
                .build())
        );

    verify(partialSurrenderCorrectionService).removeExistingPartialSurrender(position, correction, CHANGE_ID);
  }

  private void givenExecutedSurrender(@Nullable LocalDate surrenderDate) {
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderDate(surrenderDate)
        .withFeatureIds(List.of(UUID.randomUUID()))
        .build();

    when(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(CHANGE_ID)).thenReturn(surrender);
    when(partialSurrenderCorrectionService.getBlockRows(surrender)).thenReturn(BLOCK_ROWS);
  }

  private LicencePosition positionWithId() {
    return LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    return correction;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}
