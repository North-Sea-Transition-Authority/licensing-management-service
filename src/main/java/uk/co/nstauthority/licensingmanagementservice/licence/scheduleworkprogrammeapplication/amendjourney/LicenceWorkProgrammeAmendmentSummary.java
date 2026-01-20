package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity
@Table(name = "licence_work_programme_amendment_summary")
public class LicenceWorkProgrammeAmendmentSummary {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_details_id")
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetails;

  @Enumerated(EnumType.STRING)
  private LicenceWorkProgrammeAmendmentSummaryOptions licenceWorkProgrammeAmendmentSummaryOptions;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetails() {
    return scheduleWorkProgrammeApplicationDetails;
  }

  public void setScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    this.scheduleWorkProgrammeApplicationDetails = scheduleWorkProgrammeApplicationDetail;
  }

  public LicenceWorkProgrammeAmendmentSummaryOptions getLicenceWorkProgrammeAmendmentSummaryOptions() {
    return licenceWorkProgrammeAmendmentSummaryOptions;
  }

  public void setLicenceWorkProgrammeAmendmentSummaryOptions(
      LicenceWorkProgrammeAmendmentSummaryOptions licenceWorkProgrammeAmendmentSummaryOptions) {
    this.licenceWorkProgrammeAmendmentSummaryOptions = licenceWorkProgrammeAmendmentSummaryOptions;
  }
}