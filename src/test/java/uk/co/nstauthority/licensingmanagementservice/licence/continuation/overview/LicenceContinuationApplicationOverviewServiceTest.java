package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewService.SUBMITTED_BY_USER_PURPOSE;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryKeyValue;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationOverviewServiceTest {

  private static final Long SUBMITTED_BY_WUA_ID = 100L;

  @Mock
  private LicenceService licenceService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private LicenceContinuationApplicationOverviewService overviewService;

  @Test
  void getApplicationContext_returnsContextWithCorrectReference() {
    var licence = createLicence();
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(Instant.parse("2024-03-15T10:30:00Z"))
        .withSubmittedByWuaId(SUBMITTED_BY_WUA_ID)
        .withApplicationReference("LMS/CONT/2024/1")
        .build();

    var submittedByUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(SUBMITTED_BY_WUA_ID)
        .withForename("John")
        .withSurname("Smith")
        .buildJson();

    when(energyPortalUserService.getByWuaId(
        WebUserAccountId.from(SUBMITTED_BY_WUA_ID), SUBMITTED_BY_USER_PURPOSE))
        .thenReturn(submittedByUser);
    when(licenceService.getLicencePageCaption(licence))
        .thenReturn("Production licence - P1");

    var context = overviewService.getApplicationContext(applicationDetail, licence);

    assertThat(context.reference()).isEqualTo("LMS/CONT/2024/1");
  }

  @Test
  void getApplicationContext_returnsContextWithCorrectType() {
    var licence = createLicence();
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(Instant.parse("2024-03-15T10:30:00Z"))
        .withSubmittedByWuaId(SUBMITTED_BY_WUA_ID)
        .withApplicationReference("LMS/CONT/2024/1")
        .build();

    var submittedByUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(SUBMITTED_BY_WUA_ID)
        .withForename("John")
        .withSurname("Smith")
        .buildJson();

    when(energyPortalUserService.getByWuaId(
        WebUserAccountId.from(SUBMITTED_BY_WUA_ID), SUBMITTED_BY_USER_PURPOSE))
        .thenReturn(submittedByUser);
    when(licenceService.getLicencePageCaption(licence))
        .thenReturn("Production licence - P1");

    var context = overviewService.getApplicationContext(applicationDetail, licence);

    assertThat(context.type()).isEqualTo("Production licence - P1");
  }

  @Test
  void getApplicationContext_returnsSummaryDataViewWithExpectedKeys() {
    var licence = createLicence();
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(Instant.parse("2024-03-15T10:30:00Z"))
        .withSubmittedByWuaId(SUBMITTED_BY_WUA_ID)
        .withApplicationReference("LMS/CONT/2024/1")
        .build();

    var submittedByUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(SUBMITTED_BY_WUA_ID)
        .withForename("John")
        .withSurname("Smith")
        .buildJson();

    when(energyPortalUserService.getByWuaId(
        WebUserAccountId.from(SUBMITTED_BY_WUA_ID), SUBMITTED_BY_USER_PURPOSE))
        .thenReturn(submittedByUser);
    when(licenceService.getLicencePageCaption(licence))
        .thenReturn("Production licence - P1");

    var context = overviewService.getApplicationContext(applicationDetail, licence);

    assertThat(context.summaryDataView()).hasSize(1);
    var keys = context.summaryDataView().get(0).keyValues().stream()
        .map(SummaryKeyValue::key)
        .toList();
    assertThat(keys).containsExactly("Submitted by", "Submission date");
  }

  @Test
  void getApplicationContext_returnsSummaryDataViewWithCorrectValues() {
    var licence = createLicence();
    var applicationDetail = LicenceContinuationApplicationTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(LicenceContinuationApplicationStatus.SUBMITTED)
        .withSubmittedDatetime(Instant.parse("2024-03-15T10:30:00Z"))
        .withSubmittedByWuaId(SUBMITTED_BY_WUA_ID)
        .withApplicationReference("LMS/CONT/2024/1")
        .build();

    var submittedByUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(SUBMITTED_BY_WUA_ID)
        .withForename("John")
        .withSurname("Smith")
        .buildJson();

    when(energyPortalUserService.getByWuaId(
        WebUserAccountId.from(SUBMITTED_BY_WUA_ID), SUBMITTED_BY_USER_PURPOSE))
        .thenReturn(submittedByUser);
    when(licenceService.getLicencePageCaption(licence))
        .thenReturn("Production licence - P1");

    var context = overviewService.getApplicationContext(applicationDetail, licence);

    var keyValues = context.summaryDataView().get(0).keyValues();
    assertThat(getSummaryValueData(keyValues.get(0))).isEqualTo("John Smith");
    assertThat(getSummaryValueData(keyValues.get(1))).isNotBlank();
  }

  private static String getSummaryValueData(SummaryKeyValue summaryKeyValue) {
    return (String) summaryKeyValue.summaryValueData().iterator().next();
  }

  private Licence createLicence() {
    return LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS1")
        .build();
  }
}