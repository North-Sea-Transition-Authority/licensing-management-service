package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleDetailServiceTest {

  @Mock
  private LicenceScheduleDetailRepository licenceScheduleDetailRepository;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private UserDetailService userDetailService;

  private static final Instant NOW = Instant.parse("2026-11-23T10:15:00Z");

  private static final Long USER_WUA_ID = 123L;

  @Spy
  private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @InjectMocks
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Captor
  private ArgumentCaptor<LicenceScheduleDetail> licenceScheduleDetailArgumentCaptor;

  @Test
  void getByIdOrThrow() {
    var licenceScheduleDetailId = UUID.randomUUID();

    var licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceScheduleDetailRepository.findById(licenceScheduleDetailId)).thenReturn(Optional.of(licenceScheduleDetail));

    assertThat(licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetailId)).isEqualTo(licenceScheduleDetail);
  }

  @Test
  void getByIdOrThrow_licenceScheduleDetailNotFound() {
    var licenceScheduleDetailId = UUID.randomUUID();

    when(licenceScheduleDetailRepository.findById(licenceScheduleDetailId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceScheduleDetailService.getByIdOrThrow(licenceScheduleDetailId)).isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getAllScheduleDetailsByLicence() {
    var licence = new Licence();

    licenceScheduleDetailService.getAllScheduleDetailsByLicence(licence);

    verify(licenceScheduleDetailRepository).findAllByLicenceSchedule_Licence(licence);
  }

  @Test
  void getScheduleDetailByLicenceAndStatus() {
    var licence = new Licence();

    licenceScheduleDetailService.getScheduleDetailByLicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE);

    verify(licenceScheduleDetailRepository).findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE);
  }

  @Test
  void getAllDraftLicenceScheduleDetailsForUser() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    var roleSet = Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR);

    when(teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), roleSet)).thenReturn(true);

    licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(user);

    verify(licenceScheduleDetailRepository).findAllByStatus(LicenceScheduleDetailStatus.DRAFT);
  }

  @Test
  void getAllDraftLicenceScheduleDetailsForUser_noPermissions() {
    var user = ServiceUserDetailTestUtil.newBuilder().build();

    var roleSet = Set.of(Role.SCHEDULE_ADMINISTRATOR, Role.WORK_PROGRAMME_ADMINISTRATOR);

    when(teamQueryService.userHasAtLeastOneRoleIn(user.wuaId(), roleSet)).thenReturn(false);

    licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(user);

    verify(licenceScheduleDetailRepository, never()).findAllByStatus(LicenceScheduleDetailStatus.DRAFT);
  }

  @Test
  void createNewLicenceScheduleEntitiesForLicence_noExistingEntities() {
    var licence = new Licence();
    var licenceSchedule = new LicenceSchedule();

    when(licenceScheduleService.getOrCreateNewLicenceScheduleForLicence(licence)).thenReturn(licenceSchedule);
    when(userDetailService.getUserDetail())
        .thenReturn(ServiceUserDetailTestUtil.newBuilder().withWuaId(USER_WUA_ID).build());

    licenceScheduleDetailService.createNewLicenceScheduleEntitiesForLicence(licence);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue().getLicenceSchedule()).isEqualTo(licenceSchedule);
  }

  @Test
  void createNewDraftLicenceScheduleDetail() {
    var licenceSchedule = new LicenceSchedule();
    when(userDetailService.getUserDetail())
        .thenReturn(ServiceUserDetailTestUtil.newBuilder().withWuaId(USER_WUA_ID).build());

    licenceScheduleDetailService.createNewDraftLicenceScheduleDetail(licenceSchedule);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue()).extracting(
        LicenceScheduleDetail::getLicenceSchedule,
        LicenceScheduleDetail::getStatus,
        LicenceScheduleDetail::getCreatedByWuaId,
        LicenceScheduleDetail::getAppliedInstant,
        LicenceScheduleDetail::getAppliedByWuaId
    ).containsExactly(
        licenceSchedule,
        LicenceScheduleDetailStatus.DRAFT,
        USER_WUA_ID,
        null,
        null
    );
  }

  @Test
  void nonDeletedScheduleExistsForLicence() {
    var licence = new Licence();

    when(licenceScheduleDetailRepository.existsByLicenceSchedule_LicenceAndStatusIn(
        licence,
        List.of(
            LicenceScheduleDetailStatus.ACTIVE,
            LicenceScheduleDetailStatus.DRAFT,
            LicenceScheduleDetailStatus.REPLACED
        )
    ))
        .thenReturn(true);

    assertThat(licenceScheduleDetailService.nonDeletedScheduleExistsForLicence(licence)).isTrue();
  }

  @Test
  void searchByLicenceReferenceLicenceTypeAndStatus() {
    var searchTerm = "searchTerm";
    var expectedResults = List.of(new LicenceScheduleDetail());

    when(licenceScheduleDetailRepository.searchByLicenceReferenceLicenceTypesAndStatus(
        searchTerm, List.of(LicenceType.CARBON_STORAGE), LicenceScheduleDetailStatus.ACTIVE
    )).thenReturn(expectedResults);

    var result = licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        List.of(LicenceType.CARBON_STORAGE),
        LicenceScheduleDetailStatus.ACTIVE
    );

    assertThat(result).isEqualTo(expectedResults);
  }

  @Test
  void applyAndReplaceActiveScheduleDetail() {
    var licence = LicenceTestUtil.builder().build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    var currentDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withId(UUID.randomUUID())
        .withStatus(LicenceScheduleDetailStatus.DRAFT)
        .build();

    var previousAppliedInstant = Instant.parse("2025-01-01T09:00:00Z");
    var previousAppliedByWuaId = 999L;

    var previousDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withId(UUID.randomUUID())
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .withAppliedInstant(previousAppliedInstant)
        .withAppliedByWuaId(previousAppliedByWuaId)
        .build();

    when(licenceScheduleDetailRepository.findByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(Optional.of(previousDetail));
    when(userDetailService.getUserDetail())
        .thenReturn(ServiceUserDetailTestUtil.newBuilder().withWuaId(USER_WUA_ID).build());

    licenceScheduleDetailService.applyAndReplaceActiveScheduleDetail(currentDetail);

    verify(licenceScheduleDetailRepository, times(2)).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getAllValues().getFirst()).extracting(
        LicenceScheduleDetail::getId,
        LicenceScheduleDetail::getStatus,
        LicenceScheduleDetail::getAppliedInstant,
        LicenceScheduleDetail::getAppliedByWuaId
    ).containsExactly(
        previousDetail.getId(),
        LicenceScheduleDetailStatus.REPLACED,
        previousAppliedInstant,
        previousAppliedByWuaId
    );

    assertThat(licenceScheduleDetailArgumentCaptor.getAllValues().get(1)).extracting(
        LicenceScheduleDetail::getId,
        LicenceScheduleDetail::getStatus,
        LicenceScheduleDetail::getAppliedInstant,
        LicenceScheduleDetail::getAppliedByWuaId
    ).containsExactly(
        currentDetail.getId(),
        LicenceScheduleDetailStatus.ACTIVE,
        NOW,
        USER_WUA_ID
    );
  }

  @Test
  void deleteDraftScheduleDetail() {
    var detail = new LicenceScheduleDetail();
    detail.setStatus(LicenceScheduleDetailStatus.DRAFT);

    licenceScheduleDetailService.deleteDraftScheduleDetail(detail);

    verify(licenceScheduleDetailRepository).save(licenceScheduleDetailArgumentCaptor.capture());

    assertThat(licenceScheduleDetailArgumentCaptor.getValue().getStatus()).isEqualTo(LicenceScheduleDetailStatus.DELETED);
  }

  @Test
  void deleteDraftScheduleDetail_detailIsNotADraft() {
    var detail = new LicenceScheduleDetail();
    detail.setStatus(LicenceScheduleDetailStatus.ACTIVE);

    assertThatThrownBy(() -> licenceScheduleDetailService.deleteDraftScheduleDetail(detail)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void getScheduleDetailHistoryOptions() {
    var licence = LicenceTestUtil.builder().build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);

    // Created dates run the opposite way to applied dates, so ordering and labels must come from the applied date
    var olderDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withId(UUID.randomUUID())
        .withStatus(LicenceScheduleDetailStatus.REPLACED)
        .withCreatedInstant(Instant.parse("2024-05-01T09:00:00Z"))
        .withAppliedInstant(Instant.parse("2024-05-02T09:00:00Z"))
        .build();

    var newerDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withId(UUID.randomUUID())
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .withCreatedInstant(Instant.parse("2024-04-01T09:00:00Z"))
        .withAppliedInstant(Instant.parse("2024-06-01T09:00:00Z"))
        .build();

    when(licenceScheduleDetailRepository.findAllByLicenceSchedule_LicenceAndStatusIn(
        licence,
        List.of(LicenceScheduleDetailStatus.ACTIVE, LicenceScheduleDetailStatus.REPLACED)
    ))
        .thenReturn(List.of(olderDetail, newerDetail));

    var result = licenceScheduleDetailService.getScheduleDetailHistoryOptions(licence);

    assertThat(result).containsExactly(
        Map.entry(
            newerDetail.getId().toString(),
            "Applied date: %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(newerDetail.getAppliedInstant()))
        ),
        Map.entry(
            olderDetail.getId().toString(),
            "Applied date: %s".formatted(DateFormatUtil.convertToDisplayTextWithTime(olderDetail.getAppliedInstant()))
        )
    );
  }

  @Test
  void getScheduleDetailHistoryOptions_noScheduleDetails() {
    var licence = LicenceTestUtil.builder().build();

    when(licenceScheduleDetailRepository.findAllByLicenceSchedule_LicenceAndStatusIn(
        licence,
        List.of(LicenceScheduleDetailStatus.ACTIVE, LicenceScheduleDetailStatus.REPLACED)
    ))
        .thenReturn(List.of());

    var result = licenceScheduleDetailService.getScheduleDetailHistoryOptions(licence);

    assertThat(result).isEmpty();
  }
}