package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContact;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class ReminderRecipientServiceTest {

  private static final Integer BP_ID = 181;
  private static final Integer SHELL_ID = 202;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceContactService licenceContactService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private ReminderRecipientService reminderRecipientService;

  private Licence licence;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
  }

  @Test
  void getRecipientsByLicenceId_whenNoLicences_thenNoRecipients() {
    assertThat(reminderRecipientService.getRecipientsByLicenceId(List.of())).isEmpty();

    verify(licenceResponsibleOrganisationService, never()).getAllByLicenceIn(anyList());
  }

  @Test
  void getRecipientsByLicenceId_whenTheLicenceHasNoLicensees_thenNoRecipients() {
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence))).thenReturn(List.of());

    assertThat(reminderRecipientService.getRecipientsByLicenceId(List.of(licence))).isEmpty();

    verify(licenceContactService, never()).getContactsForLicensees(anyList());
  }

  @Test
  void getRecipientsByLicenceId_whenALicenceHasTwoLicensees_thenBothAreRecipients() {
    var bp = licensee(BP_ID);
    var shell = licensee(SHELL_ID);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence)))
        .thenReturn(List.of(bp, shell));
    when(licenceContactService.getContactsForLicensees(List.of(bp, shell)))
        .thenReturn(List.of(contact(bp, "bp@example.com"), contact(shell, "shell@example.com")));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID, SHELL_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd", SHELL_ID, "Shell UK Ltd"));

    var recipients = reminderRecipientService.getRecipientsByLicenceId(List.of(licence));

    assertThat(recipients).containsOnlyKeys(licence.getId());
    assertThat(recipients.get(licence.getId())).containsExactly(
        new ReminderRecipient(licence.getId(), BP_ID, "BP Exploration Alpha Ltd", "bp@example.com"),
        new ReminderRecipient(licence.getId(), SHELL_ID, "Shell UK Ltd", "shell@example.com"));
  }

  @Test
  void getRecipientsByLicenceId_whenALicenseeHasNoContact_thenOnlyTheOtherIsARecipient() {
    var bp = licensee(BP_ID);
    var shell = licensee(SHELL_ID);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence)))
        .thenReturn(List.of(bp, shell));
    when(licenceContactService.getContactsForLicensees(List.of(bp, shell)))
        .thenReturn(List.of(contact(bp, "bp@example.com")));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    var recipients = reminderRecipientService.getRecipientsByLicenceId(List.of(licence));

    assertThat(recipients.get(licence.getId())).containsExactly(
        new ReminderRecipient(licence.getId(), BP_ID, "BP Exploration Alpha Ltd", "bp@example.com"));
  }

  @Test
  void getRecipientsByLicenceId_whenNoLicenseeHasAContact_thenNoRecipients() {
    var bp = licensee(BP_ID);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence))).thenReturn(List.of(bp));
    when(licenceContactService.getContactsForLicensees(List.of(bp))).thenReturn(List.of());

    assertThat(reminderRecipientService.getRecipientsByLicenceId(List.of(licence))).isEmpty();

    verify(organisationUnitQueryService, never()).getOrganisationUnitNamesByIds(anyList());
  }

  @Test
  void getRecipientsByLicenceId_whenTheEnergyPortalHasNoNameForALicensee_thenThatLicenseeIsNotARecipient() {
    var bp = licensee(BP_ID);
    var shell = licensee(SHELL_ID);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence)))
        .thenReturn(List.of(bp, shell));
    when(licenceContactService.getContactsForLicensees(List.of(bp, shell)))
        .thenReturn(List.of(contact(bp, "bp@example.com"), contact(shell, "shell@example.com")));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(BP_ID, SHELL_ID)))
        .thenReturn(Map.of(BP_ID, "BP Exploration Alpha Ltd"));

    var recipients = reminderRecipientService.getRecipientsByLicenceId(List.of(licence));

    assertThat(recipients.get(licence.getId())).containsExactly(
        new ReminderRecipient(licence.getId(), BP_ID, "BP Exploration Alpha Ltd", "bp@example.com"));
  }

  private LicenceResponsibleOrganisation licensee(Integer responsibleOrganisationId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(responsibleOrganisationId);
    return licensee;
  }

  private LicenceContact contact(LicenceResponsibleOrganisation licensee, String contactEmail) {
    var contact = new LicenceContact();
    contact.setLicensee(licensee);
    contact.setContactEmail(contactEmail);
    return contact;
  }
}
