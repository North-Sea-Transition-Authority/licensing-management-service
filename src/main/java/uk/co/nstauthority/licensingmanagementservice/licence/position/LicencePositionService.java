package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.UndoLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea.LicenceCorrectionController;
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

    return LicencePositionPageView.fromExecutedPosition(
        getCorrectionTimelineView(chronologicalLicencePositions, licenceCorrection),
        licencePosition.getFormattedPositionDate(),
        licence.getLicenceReference(),
        licencePositionChangeViewService.getChangeViews(licencePosition, chronologicalLicencePositions, licencePositionChanges),
        licencePositionStateViewService.getStateView(licencePosition, chronologicalLicencePositions, licencePositionChanges),
        licencePosition.getId()
    );
  }

  public LicencePositionPageView getCorrectionAddedPositionPageView(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    var chronologicalLicencePositions = getChronologicalLicencePositions(licenceCorrection.getLicence());

    return LicencePositionPageView.fromNonExecutedPosition(
        getCorrectionTimelineView(chronologicalLicencePositions, licenceCorrection),
        DateUtil.formatLongDate(payload.effectiveDate()),
        payload.correctionReference(),
        UUID.fromString(payload.licencePositionId())
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
      LicenceCorrection licenceCorrection
  ) {
    var livePositions = licencePositions.stream()
        .filter(LicencePosition::isExecuted)
        .map(licencePosition -> new TimelineEntry(
            licencePosition.getPositionDate(),
            licencePosition.getPositionDateOrder(),
            baseTimelineViewBuilder(licencePosition, getCorrectionPositionUrl(licenceCorrection, licencePosition)).build()
        ));

    var addedPositions = getAddedPositionEntries(licenceCorrection);

    return Stream.concat(livePositions, addedPositions.stream())
        .sorted(TIMELINE_ORDER_COMPARATOR)
        .map(TimelineEntry::view)
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

  private LicencePositionTimelineView.Builder baseTimelineViewBuilder(LicencePosition position, String url) {
    return LicencePositionTimelineView.builder()
        .withPositionId(position.getId())
        .withUrl(url)
        .withRegulatorReference(position.getLicenceTransaction().getRegulatorReference())
        .withFormattedPositionDate(position.getFormattedPositionDate());
  }
}