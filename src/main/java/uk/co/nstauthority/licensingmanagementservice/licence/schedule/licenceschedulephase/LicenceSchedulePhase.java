package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.duplication.LinkedToDuplicationParent;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.ScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;

@Audited
@Entity
@Table(name = "licence_schedule_phases")
@DiscriminatorValue("PHASE")
public class LicenceSchedulePhase extends ScheduleEvent implements LinkedToDuplicationParent<LicenceScheduleDetail> {

  @ManyToOne
  @JoinColumn(name = "licence_schedule_term_id")
  private LicenceScheduleTerm licenceScheduleTerm;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_detail_id")
  private LicenceScheduleDetail licenceScheduleDetail;

  @Enumerated(EnumType.STRING)
  private PhaseType phaseType;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "phase_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "phase_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "phase_duration_years"))
  private ThreeFieldDuration phaseDuration;

  private LocalDate startDate;

  private LocalDate endDate;

  public LicenceScheduleDetail getLicenceScheduleDetail() {
    return licenceScheduleDetail;
  }

  public void setLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    this.licenceScheduleDetail = licenceScheduleDetail;
  }

  @Override
  public void setDuplicationParent(LicenceScheduleDetail licenceScheduleDetail) {
    setLicenceScheduleDetail(licenceScheduleDetail);
  }

  public PhaseType getPhaseType() {
    return phaseType;
  }

  public void setPhaseType(PhaseType phaseType) {
    this.phaseType = phaseType;
  }

  public ThreeFieldDuration getPhaseDuration() {
    return phaseDuration;
  }

  public void setPhaseDuration(ThreeFieldDuration phaseDuration) {
    this.phaseDuration = phaseDuration;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public LicenceScheduleTerm getLicenceScheduleTerm() {
    return licenceScheduleTerm;
  }

  public void setLicenceScheduleTerm(LicenceScheduleTerm licenceScheduleTerm) {
    this.licenceScheduleTerm = licenceScheduleTerm;
  }

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.PHASE;
  }

  @Override
  public String getEventCaption() {
    return phaseType.getDisplayName();
  }
}
