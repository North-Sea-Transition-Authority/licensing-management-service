package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ExtendWith(MockitoExtension.class)
class ScheduleCommentServiceTest {

  private static final Instant TIMESTAMP = Instant.parse("2025-01-01T10:00:00Z");

  @Mock
  private ScheduleCommentRepository scheduleCommentRepository;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private ScheduleCommentService scheduleCommentService;

  private LicenceSchedule licenceSchedule;

  @BeforeEach
  void setUp() {
    licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(LicenceTestUtil.builder().build());
  }

  @Test
  void getCommentViews_whenNoComments_isEmpty() {
    when(scheduleCommentRepository.findAllByLicenceScheduleOrderByTimestamp(licenceSchedule)).thenReturn(List.of());

    assertThat(scheduleCommentService.getCommentViews(licenceSchedule)).isEmpty();
    verify(energyPortalUserService, never()).findByWuaIds(any(), anyString());
  }

  @Test
  void getCommentViews_namesEachAuthorWithNoRemoveLink() {
    var named = comment("Named author", 42L);
    var missing = comment("Missing author", 43L);
    var noAuthor = comment("Migrated from PEARS", null);
    when(scheduleCommentRepository.findAllByLicenceScheduleOrderByTimestamp(licenceSchedule))
        .thenReturn(List.of(named, missing, noAuthor));
    var userJson = new EnergyPortalUserJson(42L, null, "Jane", "Smith", "jane.smith@example.com", null, true, null, false);
    when(energyPortalUserService.findByWuaIds(
        List.of(WebUserAccountId.from(42L), WebUserAccountId.from(43L)),
        ScheduleCommentService.COMMENT_AUTHOR_PURPOSE
    )).thenReturn(List.of(userJson));

    var postedAt = DateFormatUtil.convertToDisplayTextWithTime(TIMESTAMP);
    assertThat(scheduleCommentService.getCommentViews(licenceSchedule)).containsExactly(
        new ScheduleCommentView("Named author", userJson.displayNameAndEmail(), postedAt, ""),
        new ScheduleCommentView("Missing author", "Unknown", postedAt, ""),
        new ScheduleCommentView("Migrated from PEARS", "", postedAt, "")
    );
  }

  @Test
  void getCommentViews_whenNoCommentHasAnAuthor_doesNotLookUpUsers() {
    when(scheduleCommentRepository.findAllByLicenceScheduleOrderByTimestamp(licenceSchedule))
        .thenReturn(List.of(comment("Migrated from PEARS", null)));

    assertThat(scheduleCommentService.getCommentViews(licenceSchedule))
        .extracting(ScheduleCommentView::author)
        .containsExactly("");
    verify(energyPortalUserService, never()).findByWuaIds(any(), anyString());
  }

  @Test
  void getRemovableCommentViews_linksEachCommentToItsRemovePage() {
    var scheduleComment = comment("Migrated from PEARS", null);
    when(scheduleCommentRepository.findAllByLicenceScheduleOrderByTimestamp(licenceSchedule))
        .thenReturn(List.of(scheduleComment));

    assertThat(scheduleCommentService.getRemovableCommentViews(licenceSchedule))
        .extracting(ScheduleCommentView::removeCommentUrl)
        .containsExactly(ReverseRouter.route(on(ScheduleCommentRemovalController.class)
            .renderRemoveCommentPage(scheduleComment.getId())));
  }

  @Test
  void getCommentOrThrow_whenFound_returnsComment() {
    var scheduleComment = comment("Comment", null);
    when(scheduleCommentRepository.findById(scheduleComment.getId())).thenReturn(Optional.of(scheduleComment));

    assertThat(scheduleCommentService.getCommentOrThrow(scheduleComment.getId())).isSameAs(scheduleComment);
  }

  @Test
  void getCommentOrThrow_whenMissing_throwsNotFound() {
    var id = UUID.randomUUID();
    when(scheduleCommentRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> scheduleCommentService.getCommentOrThrow(id))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void removeComment() {
    var scheduleComment = comment("Comment", null);

    scheduleCommentService.removeComment(scheduleComment);

    verify(scheduleCommentRepository).delete(scheduleComment);
  }

  private ScheduleComment comment(String text, Long authorWuaId) {
    var scheduleComment = new ScheduleComment();
    scheduleComment.setId(UUID.randomUUID());
    scheduleComment.setLicenceSchedule(licenceSchedule);
    scheduleComment.setComment(text);
    scheduleComment.setTimestamp(TIMESTAMP);
    scheduleComment.setAuthorWuaId(authorWuaId);
    return scheduleComment;
  }
}
