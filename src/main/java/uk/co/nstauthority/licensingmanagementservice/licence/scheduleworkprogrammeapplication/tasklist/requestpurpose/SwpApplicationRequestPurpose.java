package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Entity(name = "swp_application_request_purpose")
@Audited
public class SwpApplicationRequestPurpose {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "schedule_work_programme_application_detail_id")
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @Column
  private boolean extendPhaseOrTerm;

  @Column
  private boolean extendTerm;

  @Column
  private boolean amendWorkProgramme;

  public SwpApplicationRequestPurpose() {
  }

  @VisibleForTesting
  public SwpApplicationRequestPurpose(UUID id) {
    this.id = id;
  }

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

  public boolean getExtendPhaseOrTerm() {
    return extendPhaseOrTerm;
  }

  public void setExtendPhaseOrTerm(boolean extendPhaseOrTerm) {
    this.extendPhaseOrTerm = extendPhaseOrTerm;
  }

  public boolean getExtendTerm() {
    return extendTerm;
  }

  public void setExtendTerm(boolean extendTerm) {
    this.extendTerm = extendTerm;
  }

  public boolean getAmendWorkProgramme() {
    return amendWorkProgramme;
  }

  public void setAmendWorkProgramme(boolean amendWorkProgramme) {
    this.amendWorkProgramme = amendWorkProgramme;
  }
}
