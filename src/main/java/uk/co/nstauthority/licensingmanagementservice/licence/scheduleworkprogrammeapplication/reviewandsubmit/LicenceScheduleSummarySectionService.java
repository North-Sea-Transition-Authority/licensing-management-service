package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class LicenceScheduleSummarySectionService {

  private final List<SummarySectionService<ScheduleWorkProgrammeApplicationDetail>> summarySectionsServices;

  LicenceScheduleSummarySectionService(
      List<SummarySectionService<ScheduleWorkProgrammeApplicationDetail>> summarySectionsServices
  ) {
    this.summarySectionsServices = summarySectionsServices;
  }

  public List<SummarySection> getSummarySections(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail serviceUserDetail
  ) {

    return summarySectionsServices
        .stream()
        .map(summarySectionsService -> summarySectionsService.getSummarySection(
            scheduleWorkProgrammeApplicationDetail,
            serviceUserDetail
        ))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(SummarySection::displayOrder))
        .toList();
  }
}