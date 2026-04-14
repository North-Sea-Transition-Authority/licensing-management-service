package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Audited
@Entity
@Table(name = "licence_schedule_details")
public class LicenceScheduleDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_schedule_id")
  private LicenceSchedule licenceSchedule;

  @Enumerated(value = EnumType.STRING)
  private LicenceScheduleDetailStatus status;

  private Instant createdInstant;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LicenceSchedule getLicenceSchedule() {
    return licenceSchedule;
  }

  public void setLicenceSchedule(LicenceSchedule licenceSchedule) {
    this.licenceSchedule = licenceSchedule;
  }

  public LicenceScheduleDetailStatus getStatus() {
    return status;
  }

  public void setStatus(LicenceScheduleDetailStatus status) {
    this.status = status;
  }

  public Instant getCreatedInstant() {
    return createdInstant;
  }

  public void setCreatedInstant(Instant createdInstant) {
    this.createdInstant = createdInstant;
  }

  public String getScheduleTimelineRouteUrl() {
    return ReverseRouter.route(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(this.id, null, null, null));
  }

  public ModelAndView getScheduleTimelineRedirectUrl() {
    return ReverseRouter.redirect(on(LicenceScheduleTimelineController.class)
        .renderLicenceScheduleTimeline(this.id, null, null, null));
  }
}
