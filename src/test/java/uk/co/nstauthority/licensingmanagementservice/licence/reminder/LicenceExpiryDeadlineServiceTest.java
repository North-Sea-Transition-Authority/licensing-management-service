package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiry;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryRepository;

@ExtendWith(MockitoExtension.class)
class LicenceExpiryDeadlineServiceTest {

  private static final LocalDate EXPIRY_DATE = LocalDate.of(2027, Month.MARCH, 31);
  private static final LocalDate NOTICE_DATE = LocalDate.of(2026, Month.OCTOBER, 1);
  private static final LocalDate WINDOW_END = LocalDate.of(2027, Month.APRIL, 1);

  @Mock
  private LicenceScheduleExpiryRepository licenceScheduleExpiryRepository;

  @Mock
  private Clock clock;

  @InjectMocks
  private LicenceExpiryDeadlineService licenceExpiryDeadlineService;

  private Licence licence;
  private LicenceScheduleDetail scheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setId(UUID.randomUUID());
    licenceSchedule.setLicence(licence);

    scheduleDetail = new LicenceScheduleDetail();
    scheduleDetail.setId(UUID.randomUUID());
    scheduleDetail.setLicenceSchedule(licenceSchedule);
    scheduleDetail.setStatus(LicenceScheduleDetailStatus.ACTIVE);
  }

  @Test
  void getDeadlinesDueReminder_whenNothingIsInTheNoticeWindow_thenNothingIsDue() {
    mockWindow(NOTICE_DATE, WINDOW_END, List.of());

    assertThat(licenceExpiryDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenAScheduleExpiryIsOnItsNoticeDate_thenItIsDueWithTheLicenceAsItsIdentity() {
    var scheduleExpiry = scheduleExpiry(EXPIRY_DATE);
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(scheduleExpiry));

    var deadlines = licenceExpiryDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(new ReminderDeadline(
        scheduleExpiry,
        null,
        licence,
        EXPIRY_DATE,
        LicenceExpiryDeadlineService.DISPLAY_NAME,
        ReminderType.LICENCE_EXPIRY));
  }

  @Test
  void getDeadlinesDueReminder_thenTheWindowRunsFromTodayToTheLatestDeadlineDate() {
    mockWindow(NOTICE_DATE, WINDOW_END, List.of());

    licenceExpiryDeadlineService.getDeadlinesDueReminder();

    verify(licenceScheduleExpiryRepository).findAllByExpiryDateBetweenAndLicenceScheduleDetail_Status(
        NOTICE_DATE, WINDOW_END, LicenceScheduleDetailStatus.ACTIVE);
  }

  private void mockWindow(LocalDate today, LocalDate latestExpiryDate, List<LicenceScheduleExpiry> scheduleExpiries) {
    when(clock.instant()).thenReturn(today.atStartOfDay().toInstant(ZoneOffset.UTC));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(licenceScheduleExpiryRepository.findAllByExpiryDateBetweenAndLicenceScheduleDetail_Status(
        today, latestExpiryDate, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(scheduleExpiries);
  }

  private LicenceScheduleExpiry scheduleExpiry(LocalDate expiryDate) {
    var scheduleExpiry = new LicenceScheduleExpiry();
    scheduleExpiry.setId(UUID.randomUUID());
    scheduleExpiry.setOriginalEventId(UUID.randomUUID());
    scheduleExpiry.setLicenceSchedule(scheduleDetail.getLicenceSchedule());
    scheduleExpiry.setLicenceScheduleDetail(scheduleDetail);
    scheduleExpiry.setExpiryDate(expiryDate);
    return scheduleExpiry;
  }
}
