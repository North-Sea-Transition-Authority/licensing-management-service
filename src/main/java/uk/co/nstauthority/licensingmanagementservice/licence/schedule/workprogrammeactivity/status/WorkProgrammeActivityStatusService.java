package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;

@Service
public class WorkProgrammeActivityStatusService {

  private final Clock clock;
  private final WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;
  private final LicenceService licenceService;

  public WorkProgrammeActivityStatusService(
      Clock clock,
      WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository,
      LicenceService licenceService
  ) {
    this.clock = clock;
    this.workProgrammeActivityStatusRepository = workProgrammeActivityStatusRepository;
    this.licenceService = licenceService;
  }

  @Transactional
  public void createInitialStatusFor(WorkProgrammeActivity activity) {
    if (!workProgrammeActivityStatusRepository.findAllByActivityEventReference(activity.getEventReference()).isEmpty()) {
      return;
    }

    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setActivityEventReference(activity.getEventReference());
    activityStatus.setStatus(WorkProgrammeStatus.OPEN);
    activityStatus.setAppliedDatetime(Instant.now(clock));

    workProgrammeActivityStatusRepository.save(activityStatus);
  }

  @Transactional
  public void saveStatusFromForm(WorkProgrammeActivityStatusForm form, WorkProgrammeActivity activity) {
    var activityStatus = new WorkProgrammeActivityStatus();
    activityStatus.setActivityEventReference(activity.getEventReference());
    activityStatus.setStatus(form.getStatus());
    activityStatus.setAppliedDatetime(Instant.now(clock));

    if (form.getTransferredToLicenceId() != null) {
      var licence = licenceService.findLicenceByIdOrThrow(Integer.parseInt(form.getTransferredToLicenceId()));
      activityStatus.setLicenceTransferredTo(licence);
    } else {
      activityStatus.setLicenceTransferredTo(null);
    }

    workProgrammeActivityStatusRepository.save(activityStatus);
  }

  public WorkProgrammeActivityStatus getLatestStatusFor(WorkProgrammeActivity activity) {
    return workProgrammeActivityStatusRepository.findAllByActivityEventReference(activity.getEventReference()).stream()
        .max(Comparator.comparing(WorkProgrammeActivityStatus::getAppliedDatetime))
        .orElseThrow(() -> new LmsEntityNotFoundException(
                "No status found for WorkProgrammeActivity with event reference: %s".formatted(activity.getEventReference())
            )
        );
  }

  public WorkProgrammeActivityStatusForm getStatusForm(WorkProgrammeActivity activity) {
    var latestStatus = getLatestStatusFor(activity);
    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(latestStatus.getStatus());

    if (latestStatus.getLicenceTransferredTo() != null) {
      form.setTransferredToLicenceId(latestStatus.getLicenceTransferredTo().getId().toString());
    }

    return form;
  }
}
