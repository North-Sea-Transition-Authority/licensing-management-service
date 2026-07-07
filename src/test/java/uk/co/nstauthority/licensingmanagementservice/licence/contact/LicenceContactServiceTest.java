package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class LicenceContactServiceTest {

  private static final Integer LICENCE_ID = 1;
  private static final Integer ORG_ID = 10;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceReference("P 123")
      .build();
  public static final String SHELL_U_K_LIMITED = "Shell U.K. Limited";
  private static final OrganisationUnitJson ORG_UNIT = new OrganisationUnitJson(ORG_ID, SHELL_U_K_LIMITED);

  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private LicenceContactRepository licenceContactRepository;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceOrganisationService licenceOrganisationService;

  @InjectMocks
  private LicenceContactService licenceContactService;

  @Captor
  private ArgumentCaptor<LicenceContact> contactCaptor;

  @Test
  void getContactTableForUser_whenUserHasNoOrgUnits_returnsEmptyTableAndDoesNotQuery() {
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of());

    var tableJson = licenceContactService.getContactTableForUser(user);

    assertThat(tableJson).doesNotContain("licence-contacts/");
    verifyNoInteractions(licenceResponsibleOrganisationService, licenceContactRepository);
  }

  @Test
  void getContactTableForUser_whenLicenseeHasNoContact_showsNotAssigned() {
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licensee()));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of());

    var tableJson = licenceContactService.getContactTableForUser(user);

    assertThat(tableJson)
        .contains("P 123")
        .contains(SHELL_U_K_LIMITED)
        .contains("Not assigned")
        .contains("\"licence-contacts/licence/%d/responsible-organisation/%d\"".formatted(LICENCE_ID, ORG_ID));
  }

  @Test
  void getContactTableForUser_whenLicenseeHasContact_showsEmail() {
    var licensee = licensee();
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licensee));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(contact(licensee, "licensing@example.com")));

    var tableJson = licenceContactService.getContactTableForUser(user);

    assertThat(tableJson)
        .contains("licensing@example.com")
        .doesNotContain("Not assigned");
  }

  @Test
  void getLicenceContactFormView_whenOrganisationNotInUsersGroups_throwsForbidden() {
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, 999))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> licenceContactService.getLicenceContactFormView(user, LICENCE_ID, 999))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    verifyNoInteractions(licenceResponsibleOrganisationService, licenceContactRepository);
  }

  @Test
  void getLicenceContactFormView_whenLicenseeNotFound_throwsNotFound() {
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenThrow(new LmsEntityNotFoundException("licence responsible organisation", "1/10"));

    assertThatThrownBy(() -> licenceContactService.getLicenceContactFormView(user, LICENCE_ID, ORG_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getLicenceContactFormViewWithCurrentEmail() {
    var licensee = licensee();
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenReturn(licensee);
    when(licenceContactRepository.findByLicensee(licensee))
        .thenReturn(Optional.of(contact(licensee, "licensing@example.com")));

    var context = licenceContactService.getLicenceContactFormView(user, LICENCE_ID, ORG_ID);

    assertThat(context.licenceReference()).isEqualTo("P 123");
    assertThat(context.currentEmail()).isEqualTo("licensing@example.com");
    assertThat(context.isUpdate()).isTrue();
  }

  @Test
  void saveContact_whenExistingContact_updatesEmail() {
    var licensee = licensee();
    var existing = contact(licensee, "old@example.com");
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenReturn(licensee);
    when(licenceContactRepository.findByLicensee(licensee)).thenReturn(Optional.of(existing));

    licenceContactService.saveContact(user, LICENCE_ID, ORG_ID, "new@example.com");

    verify(licenceContactRepository).save(existing);
    assertThat(existing.getContactEmail()).isEqualTo("new@example.com");
  }

  @Test
  void saveContact_whenOrganisationNotInUsersGroups_throwsForbiddenAndDoesNotSave() {
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, 999))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> licenceContactService.saveContact(user, LICENCE_ID, 999, "x@example.com"))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(licenceResponsibleOrganisationService, licenceContactRepository);
  }

  private static LicenceResponsibleOrganisation licensee() {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(LICENCE);
    licensee.setResponsibleOrganisationId(ORG_ID);
    licensee.setManagedByLms(false);
    return licensee;
  }

  private static LicenceContact contact(LicenceResponsibleOrganisation licensee, String email) {
    var contact = new LicenceContact();
    contact.setLicensee(licensee);
    contact.setContactEmail(email);
    return contact;
  }
}
