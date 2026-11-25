package uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class ScheduleCommentService {

  static final String COMMENT_AUTHOR_PURPOSE = "Fetch author names for licence schedule comments";

  private final ScheduleCommentRepository scheduleCommentRepository;
  private final EnergyPortalUserService energyPortalUserService;

  public ScheduleCommentService(
      ScheduleCommentRepository scheduleCommentRepository,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.scheduleCommentRepository = scheduleCommentRepository;
    this.energyPortalUserService = energyPortalUserService;
  }

  public List<ScheduleCommentView> getCommentViews(LicenceSchedule licenceSchedule) {
    return getCommentViews(licenceSchedule, comment -> "");
  }

  public List<ScheduleCommentView> getRemovableCommentViews(LicenceSchedule licenceSchedule) {
    return getCommentViews(
        licenceSchedule,
        comment -> ReverseRouter.route(on(ScheduleCommentRemovalController.class)
            .renderRemoveCommentPage(comment.getId()))
    );
  }

  ScheduleComment getCommentOrThrow(UUID scheduleCommentId) {
    return scheduleCommentRepository.findById(scheduleCommentId)
        .orElseThrow(() -> new LmsEntityNotFoundException("ScheduleComment", scheduleCommentId));
  }

  ScheduleCommentView getCommentView(ScheduleComment scheduleComment) {
    return createView(scheduleComment, authorNames(List.of(scheduleComment)), "");
  }

  @Transactional
  public void removeComment(ScheduleComment scheduleComment) {
    scheduleCommentRepository.delete(scheduleComment);
  }

  private List<ScheduleCommentView> getCommentViews(
      LicenceSchedule licenceSchedule,
      Function<ScheduleComment, String> removeUrl
  ) {
    var comments = scheduleCommentRepository.findAllByLicenceScheduleOrderByTimestamp(licenceSchedule);
    if (comments.isEmpty()) {
      return List.of();
    }

    var authorNames = authorNames(comments);
    return comments.stream()
        .map(comment -> createView(comment, authorNames, removeUrl.apply(comment)))
        .toList();
  }

  private Map<Long, String> authorNames(List<ScheduleComment> comments) {
    var authorWuaIds = comments.stream()
        .map(ScheduleComment::getAuthorWuaId)
        .filter(Objects::nonNull)
        .distinct()
        .map(WebUserAccountId::from)
        .toList();

    if (authorWuaIds.isEmpty()) {
      return Map.of();
    }

    return energyPortalUserService.findByWuaIds(authorWuaIds, COMMENT_AUTHOR_PURPOSE).stream()
        .collect(StreamUtil.toLinkedHashMap(
            EnergyPortalUserJson::webUserAccountId,
            EnergyPortalUserJson::displayNameAndEmail
        ));
  }

  private static ScheduleCommentView createView(
      ScheduleComment scheduleComment,
      Map<Long, String> authorNames,
      String removeCommentUrl
  ) {
    return new ScheduleCommentView(
        scheduleComment.getComment(),
        authorName(scheduleComment, authorNames),
        DateFormatUtil.convertToDisplayTextWithTime(scheduleComment.getTimestamp()),
        removeCommentUrl
    );
  }

  /**
   * A comment with no author, such as a general note migrated from PEARS with the author's name in its text, shows a
   * blank author. "Unknown" is kept for an author whose user can no longer be found.
   */
  private static String authorName(ScheduleComment scheduleComment, Map<Long, String> authorNames) {
    if (scheduleComment.getAuthorWuaId() == null) {
      return "";
    }
    return authorNames.getOrDefault(scheduleComment.getAuthorWuaId(), "Unknown");
  }
}
