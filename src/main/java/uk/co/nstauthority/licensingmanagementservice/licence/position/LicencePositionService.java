package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.ReinstateLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.RemoveExecutedLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.UndoLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class LicencePositionService {

  private static final Comparator<TimelineEntry> TIMELINE_ORDER_COMPARATOR =
      Comparator.comparing(TimelineEntry::date).thenComparingInt(TimelineEntry::order).reversed();

  private final LicencePositionRepository licencePositionRepository;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicencePositionChangeViewService licencePositionChangeViewService;
  private final LicencePositionStateViewService licencePositionStateViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public LicencePositionService(
      LicencePositionRepository licencePositionRepository,
      LicencePositionChangeService licencePositionChangeService,
      LicencePositionChangeViewService licencePositionChangeViewService,
      LicencePositionStateViewService licencePositionStateViewService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licencePositionRepository = licencePositionRepository;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licencePositionChangeViewService = licencePositionChangeViewService;
    this.licencePositionStateViewService = licencePositionStateViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @Transactional
  public LicencePosition createLicencePosition(
      Licence licence,
      LicenceTransaction transaction,
      LocalDate positionDate
  ) {
    //TODO LMS2-63: Add a lock to licence to serialise concurrent position inserts
    var maxOrder = licencePositionRepository.findMaxPositionDateOrder(licence, positionDate);
    var positionDateOrder = (maxOrder == null) ? 1 : maxOrder + 1;

    LicencePosition licencePosition = new LicencePosition();
    licencePosition.setLicence(licence);
    licencePosition.setLicenceTransaction(transaction);
    licencePosition.setPositionDate(positionDate);
    licencePosition.setPositionDateOrder(positionDateOrder);
    licencePosition.setExecuted(true);

    return licencePositionRepository.save(licencePosition);
  }

  public LicencePosition getPositionForLicence(Licence licence, UUID licencePositionId) {
    return licencePositionRepository.findByIdAndLicence(licencePositionId, licence)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licencePosition", licencePositionId));
  }

  public Integer getCurrentAdministratorId(LicencePosition licencePosition) {
    var chronologicalLicencePositions = getChronologicalLicencePositions(licencePosition.getLicence());
    var licencePositionChanges = licencePositionChangeService.findByLicencePositionIn(chronologicalLicencePositions);
    return licencePositionStateViewService.resolveCurrentAdministratorId(
        licencePosition, chronologicalLicencePositions, licencePositionChanges);
  }

  public List<LicencePosition> getChronologicalLicencePositions(Licence licence) {
    return licencePositionRepository.findByLicence(licence)
        .stream()
        .sorted(Comparator.comparing(LicencePosition::getPositionDate).thenComparing(LicencePosition::getPositionDateOrder))
        .toList();
  }

  public LicencePositionPageView getPositionPageView(LicencePosition licencePosition) {
    var licence = licencePosition.getLicence();
    var chronologicalLicencePositions = getChronologicalLicencePositions(licence);
    var licencePositionChanges = licencePositionChangeService.findByLicencePositionIn(chronologicalLicencePositions);

    return LicencePositionPageView.readOnly(
        getReadOnlyTimelineView(chronologicalLicencePositions),
        licencePosition.getFormattedPositionDate(),
        licencePosition.getLicenceTransaction().getRegulatorReference(),
        licencePositionChangeViewService.getChangeViews(licencePosition, chronologicalLicencePositions, licencePositionChanges),
        licencePositionStateViewService.getStateView(licencePosition, chronologicalLicencePositions, licencePositionChanges),
        licencePosition.getId()
    );
  }

  public LicencePositionPageView getCorrectionPositionPageView(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    var licence = licencePosition.getLicence();
    var chronologicalLicencePositions = getChronologicalLicencePositions(licence);
    var licencePositionChanges = licencePositionChangeService.findByLicencePositionIn(chronologicalLicencePositions);
    var removedPositionIds = licencePositionCorrectionService.getRemovedLicencePositionIds(licenceCorrection);

    //todo kept in temporarily to fix an exception being thrown.
    // Checks licence postions and filters out the ones that are marked for removal.
    var timelineStatePositions = chronologicalLicencePositions.stream()
        .filter(position -> position.getId().equals(licencePosition.getId())
            || !removedPositionIds.contains(position.getId()))
        .toList();

    var actions = new LicencePositionPageView.Actions(
        ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(licenceCorrection.getId(), licencePosition.getId(), null)));

    return LicencePositionPageView.fromExecutedPosition(
        getCorrectionTimelineView(chronologicalLicencePositions, licenceCorrection, removedPositionIds),
        licencePosition.getFormattedPositionDate(),
        licence.getLicenceReference(),
        licencePositionChangeViewService.getChangeViews(licencePosition, timelineStatePositions, licencePositionChanges),
        licencePositionStateViewService.getStateView(licencePosition, timelineStatePositions, licencePositionChanges),
        licencePosition.getId(),
        actions
    );
  }

  public LicencePositionPageView getCorrectionAddedPositionPageView(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    var chronologicalLicencePositions = getChronologicalLicencePositions(licenceCorrection.getLicence());
    var removedPositionIds = licencePositionCorrectionService.getRemovedLicencePositionIds(licenceCorrection);

    var actions = new LicencePositionPageView.Actions(
        ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(licenceCorrection.getId(), positionCorrection.getId(), null)));

    return LicencePositionPageView.fromNonExecutedPosition(
        getCorrectionTimelineView(chronologicalLicencePositions, licenceCorrection, removedPositionIds),
        DateUtil.formatLongDate(payload.effectiveDate()),
        payload.correctionReference(),
        UUID.fromString(payload.licencePositionId()),
        actions
    );
  }

  public List<LicencePosition> getExecutedChronologicalLicencePositions(Licence licence) {
    return getChronologicalLicencePositions(licence)
        .stream()
        .filter(LicencePosition::isExecuted)
        .toList();
  }

  private List<LicencePositionTimelineView> getReadOnlyTimelineView(List<LicencePosition> licencePositions) {
    return licencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .map(licencePosition -> new TimelineEntry(
            licencePosition.getPositionDate(),
            licencePosition.getPositionDateOrder(),
            baseTimelineViewBuilder(licencePosition, getPositionUrl(licencePosition)).build()
        ))
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(TimelineEntry::view)
        .toList();
  }

  private List<LicencePositionTimelineView> getCorrectionTimelineView(
      List<LicencePosition> licencePositions,
      LicenceCorrection licenceCorrection,
      Set<UUID> removedPositionIds
  ) {
    var correctedPayloadsByPositionId =
        licencePositionCorrectionService.getUpdatedPositionPayloadsByTargetId(licenceCorrection);
    var livePositions =
        getLivePositionEntries(licencePositions, licenceCorrection, removedPositionIds, correctedPayloadsByPositionId);
    var addedPositions = getAddedPositionEntries(licenceCorrection);

    return Stream.concat(livePositions.stream(), addedPositions.stream())
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(TimelineEntry::view)
        .toList();
  }

  private List<TimelineEntry> getLivePositionEntries(
      List<LicencePosition> licencePositions,
      LicenceCorrection licenceCorrection,
      Set<UUID> removedPositionIds,
      Map<UUID, UpdateLicencePositionPayload> correctedPayloadsByPositionId
  ) {
    return licencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .map(licencePosition -> {
          var removed = removedPositionIds.contains(licencePosition.getId());
          var correctedPayload = correctedPayloadsByPositionId.get(licencePosition.getId());
          var dateCorrected = correctedPayload != null && correctedPayload.effectiveDate() != null;
          var effectiveDate = dateCorrected
              ? correctedPayload.effectiveDate() : licencePosition.getPositionDate();
          var effectiveDateOrder = correctedPayload != null && correctedPayload.effectiveDateOrder() != null
              ? correctedPayload.effectiveDateOrder() : licencePosition.getPositionDateOrder();

          var timelineViewBuilder = baseTimelineViewBuilder(
              licencePosition, getCorrectionPositionUrl(licenceCorrection, licencePosition))
              .withFormattedPositionDate(DateUtil.formatLongDate(effectiveDate))
              .withCorrectedInThisCorrection(correctedPayload != null)
              .withRemovedInThisCorrection(removed);

          if (correctedPayload != null) {
            timelineViewBuilder.withRegulatorReference(correctedPayload.correctionReference());
          }

          if (removed) {
            timelineViewBuilder.withReinstateUrl(getReinstatePositionUrl(licenceCorrection, licencePosition));
          } else {
            timelineViewBuilder.withRemoveUrl(getRemovePositionUrl(licenceCorrection, licencePosition));
            timelineViewBuilder.withCorrectDateUrl(getCorrectDatePositionUrl(licenceCorrection, licencePosition));
          }

          return new TimelineEntry(effectiveDate, effectiveDateOrder, timelineViewBuilder.build());
        })
        .toList();
  }

  private List<TimelineEntry> getAddedPositionEntries(LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionService.getAddedLicencePositionCorrections(licenceCorrection)
        .stream()
        .map(licencePositionCorrection -> {
          var payload = (CreateLicencePositionPayload) licencePositionCorrection.getPayload();
          var effectiveDate = payload.effectiveDate();
          return new TimelineEntry(
              effectiveDate,
              payload.effectiveDateOrder(),
              LicencePositionTimelineView.builder()
                  .withPositionId(UUID.fromString(payload.licencePositionId()))
                  .withUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
                      .renderAddedPosition(licenceCorrection.getId(), licencePositionCorrection.getId(), null)))
                  .withRegulatorReference(payload.correctionReference())
                  .withFormattedPositionDate(DateUtil.formatLongDate(effectiveDate))
                  .withAddedInThisCorrection(true)
                  .withUndoUrl(ReverseRouter.route(on(UndoLicencePositionCorrectionController.class)
                      .renderUndoPosition(licenceCorrection.getId(), licencePositionCorrection.getId(), null)))
                  .build()
          );
        })
        .toList();
  }

  private String getPositionUrl(LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicencePositionController.class)
        .renderLicencePosition(licencePosition.getLicence(), licencePosition.getId()));
  }

  private String getCorrectionPositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), position.getId(), correction));
  }

  private String getRemovePositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(RemoveExecutedLicencePositionCorrectionController.class)
        .renderRemovePosition(correction.getId(), position.getId(), null));
  }

  private String getReinstatePositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(ReinstateLicencePositionCorrectionController.class)
        .renderReinstatePosition(correction.getId(), position.getId(), null));
  }

  private String getCorrectDatePositionUrl(LicenceCorrection correction, LicencePosition position) {
    return ReverseRouter.route(on(CorrectPositionDateController.class)
        .renderCorrectLicencePositionCorrectionDate(correction.getId(), position.getId(), null));
  }

  private LicencePositionTimelineView.Builder baseTimelineViewBuilder(LicencePosition position, String url) {
    return LicencePositionTimelineView.builder()
        .withPositionId(position.getId())
        .withUrl(url)
        .withRegulatorReference(position.getLicenceTransaction().getRegulatorReference())
        .withFormattedPositionDate(position.getFormattedPositionDate());
  }
}