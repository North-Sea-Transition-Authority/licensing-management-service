package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Audited
@Entity(name = "swp_record_of_decision")
public class RecordOfDecision {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_detail_id")
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @Enumerated(EnumType.STRING)
  @Column
  private RecordOfDecisionResponse extensionDecision;

  @Enumerated(EnumType.STRING)
  @Column
  private RecordOfDecisionResponse workProgrammeDecision;

  @Enumerated(EnumType.STRING)
  @Column
  private RecordWorkProgrammeAmendmentSummaryOptions workProgrammeSummaryOption;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ScheduleWorkProgrammeApplicationDetail getScheduleWorkProgrammeApplicationDetail() {
    return scheduleWorkProgrammeApplicationDetail;
  }

  public void setScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    this.scheduleWorkProgrammeApplicationDetail = scheduleWorkProgrammeApplicationDetail;
  }

  public RecordOfDecisionResponse getExtensionDecision() {
    return extensionDecision;
  }

  public void setExtensionDecision(RecordOfDecisionResponse extensionDecision) {
    this.extensionDecision = extensionDecision;
  }

  public RecordOfDecisionResponse getWorkProgrammeDecision() {
    return workProgrammeDecision;
  }

  public void setWorkProgrammeDecision(RecordOfDecisionResponse workProgrammeDecision) {
    this.workProgrammeDecision = workProgrammeDecision;
  }

  public RecordWorkProgrammeAmendmentSummaryOptions getWorkProgrammeSummaryOption() {
    return workProgrammeSummaryOption;
  }

  public void setWorkProgrammeSummaryOption(RecordWorkProgrammeAmendmentSummaryOptions workProgrammeSummaryOption) {
    this.workProgrammeSummaryOption = workProgrammeSummaryOption;
  }
}
