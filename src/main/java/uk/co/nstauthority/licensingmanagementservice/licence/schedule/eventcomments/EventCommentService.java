package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class EventCommentService {

  static final String COMMENT_AUTHOR_PURPOSE = "Fetch author names for licence schedule timeline comments";

  private final EventCommentRepository eventCommentRepository;
  private final EnergyPortalUserService energyPortalUserService;
  private final Clock clock;

  public EventCommentService(
      EventCommentRepository eventCommentRepository,
      EnergyPortalUserService energyPortalUserService,
      Clock clock
  ) {
    this.eventCommentRepository = eventCommentRepository;
    this.energyPortalUserService = energyPortalUserService;
    this.clock = clock;
  }

  @Transactional
  public void addNewComment(
      EventCommentForm form,
      EventReference eventReference,
      ServiceUserDetail author
  ) {
    var eventComment = new EventComment();
    eventComment.setEventReference(eventReference);
    eventComment.setComment(form.getComment());
    eventComment.setStatus(EventCommentStatus.PUBLISHED);
    eventComment.setTimestamp(clock.instant());
    eventComment.setAuthorWuaId(author.wuaId());
    eventCommentRepository.save(eventComment);
  }

  public Map<UUID, List<EventCommentView>> getEventCommentViewsForSchedule(LicenceSchedule licenceSchedule) {
    var comments = eventCommentRepository.getAllByEventReference_LicenceSchedule(licenceSchedule);

    if (comments.isEmpty()) {
      return Map.of();
    }

    var authorWuaIds = comments.stream()
        .map(EventComment::getAuthorWuaId)
        .distinct()
        .map(WebUserAccountId::from)
        .toList();

    var wuaIdNameMap = energyPortalUserService.findByWuaIds(authorWuaIds, COMMENT_AUTHOR_PURPOSE).stream()
        .collect(StreamUtil.toLinkedHashMap(
            EnergyPortalUserJson::webUserAccountId,
            EnergyPortalUserJson::displayName
            )
        );

    return comments.stream()
        .sorted(Comparator.comparing(EventComment::getTimestamp))
        .collect(Collectors.groupingBy(
            comment -> comment.getEventReference().getId(),
            Collectors.mapping(eventComment -> createViewFrom(eventComment, wuaIdNameMap), Collectors.toList())
        ));
  }

  private EventCommentView createViewFrom(
      EventComment eventComment,
      Map<Long, String> wuaIdNameMap
  ) {
    return new EventCommentView(
        eventComment.getComment(),
        wuaIdNameMap.get(eventComment.getAuthorWuaId()),
        DateFormatUtil.convertToDisplayTextWithTime(eventComment.getTimestamp())
    );
  }
}
