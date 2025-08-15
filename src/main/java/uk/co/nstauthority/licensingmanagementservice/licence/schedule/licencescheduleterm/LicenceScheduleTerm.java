package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Audited
@Entity(name = "licence_schedule_terms")
public class LicenceScheduleTerm {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  private LicenceScheduleDetail licenceScheduleDetail;

  @Enumerated(EnumType.STRING)
  private TermType termType;

  @Embedded
  @AttributeOverride(name = "days", column = @Column(name = "term_duration_days"))
  @AttributeOverride(name = "months", column = @Column(name = "term_duration_months"))
  @AttributeOverride(name = "years", column = @Column(name = "term_duration_years"))
  private ThreeFieldDuration termDuration;

  private LocalDate startDate;

  private LocalDate endDate;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceScheduleDetail getLicenceScheduleDetail() {
    return licenceScheduleDetail;
  }

  public void setLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    this.licenceScheduleDetail = licenceScheduleDetail;
  }

  public TermType getTermType() {
    return termType;
  }

  public void setTermType(TermType termType) {
    this.termType = termType;
  }

  public ThreeFieldDuration getTermDuration() {
    return termDuration;
  }

  public void setTermDuration(ThreeFieldDuration termDuration) {
    this.termDuration = termDuration;
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
}

