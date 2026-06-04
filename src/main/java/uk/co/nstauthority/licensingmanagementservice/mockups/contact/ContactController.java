package uk.co.nstauthority.licensingmanagementservice.mockups.contact;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/mockups/contacts")
@Profile("mockups")
public class ContactController {

  public static final String BP_EXPLORATION = "BP Exploration Limited";
  public static final String BRITOIL_LIMITED = "Britoil Limited";
  public static final String SHELL_U_K = "Shell U.K. Limited";
  public static final String SHELL_U_K_PLC = "Shell U.K. Plc";
  public static final String SHELL_U_K_NORTH_ATLANTIC = "Shell U.K. North Atlantic Limited";
  public static final String HARBOUR_ENERGY = "Harbour Energy plc";
  public static final String TOTAL_ENERGIES_E_P_UK = "TotalEnergies E&P UK Limited";

  public static final String LICENSING_UK_BP_COM = "licensing@bp.com";
  public static final String UK_LICENSING_SHELL_COM = "uk-licensing@shell.com";
  public static final String ASSETS_NORTHSEA_HARBOURENERGY_COM = "assets.northsea@harbourenergy.com";

  public static final List<ContactRow> CONTACT_LIST = List.of(
      new ContactRow("101", "P1918", BP_EXPLORATION, BP_EXPLORATION, "p1918-" + LICENSING_UK_BP_COM, "Licence Administrator"),
      new ContactRow("102", "P2000", BP_EXPLORATION, BP_EXPLORATION, "p2000-" + LICENSING_UK_BP_COM, null),
      new ContactRow("103", "P2100", BP_EXPLORATION, BP_EXPLORATION, "p2100-" + LICENSING_UK_BP_COM, null),
      new ContactRow("104", "P2400", BP_EXPLORATION, BRITOIL_LIMITED, LICENSING_UK_BP_COM, null),
      new ContactRow("105", "P3000", BP_EXPLORATION, BP_EXPLORATION, "", null),
      new ContactRow("106", "P3200", BP_EXPLORATION, BRITOIL_LIMITED, "", null),
      new ContactRow("107", "CS100", BP_EXPLORATION, BRITOIL_LIMITED, "", "Exploration Operator"),
      new ContactRow("201", "P1011", SHELL_U_K_PLC, SHELL_U_K, UK_LICENSING_SHELL_COM, null),
      new ContactRow("202", "P1022", SHELL_U_K_PLC, SHELL_U_K, UK_LICENSING_SHELL_COM, null),
      new ContactRow("203", "P1033", SHELL_U_K_PLC, SHELL_U_K, UK_LICENSING_SHELL_COM, null),
      new ContactRow("204", "P2500", SHELL_U_K_PLC, SHELL_U_K_NORTH_ATLANTIC, "sarah.jones@shell.com", null),
      new ContactRow("205", "P3400", SHELL_U_K_PLC, SHELL_U_K, "", null),
      new ContactRow("206", "P3405", SHELL_U_K_PLC, SHELL_U_K, "", null),
      new ContactRow("301", "P2500", HARBOUR_ENERGY, HARBOUR_ENERGY, ASSETS_NORTHSEA_HARBOURENERGY_COM, null),
      new ContactRow("302", "P2501", HARBOUR_ENERGY, HARBOUR_ENERGY, ASSETS_NORTHSEA_HARBOURENERGY_COM, null),
      new ContactRow("303", "P2600", HARBOUR_ENERGY, HARBOUR_ENERGY, "j.bloggs@harbourenergy.com", null),
      new ContactRow("401", "P2601", TOTAL_ENERGIES_E_P_UK, TOTAL_ENERGIES_E_P_UK, "ep.uk.licensing@totalenergies.com",
          null),
      new ContactRow("402", "P2602", TOTAL_ENERGIES_E_P_UK, TOTAL_ENERGIES_E_P_UK, "", null)
  );
  public static final List<ContactRow> CONTACT_INDUSTRY_LIST = CONTACT_LIST.stream()
      .filter(contactRow -> contactRow.licenseeGroup().equals(BP_EXPLORATION))
      .toList();

  @GetMapping("/manage-contacts-industry")
  ModelAndView showManageContactsIndustryMockup() {
    var tableViewBuilder = getSortableTableView()
        .withActionHeading("Action");

    for (var row : CONTACT_INDUSTRY_LIST) {
      var emailValue = getEmailValue(row);

      var actionPrompt = row.email().isEmpty() ? "Add contact email" : "Update contact email";
      tableViewBuilder.addRow(
          getRowBuilder(row, emailValue)
              .withAction(actionPrompt, "bulk-update-contacts/" + row.licence(), "for " + row.licence())
              .build()
      );
    }

    return new ModelAndView("lms/mockups/manageContactsDesign")
        .addObject("contactsTableJson", tableViewBuilder.build().toString())
        .addObject("contactCount", CONTACT_INDUSTRY_LIST.size());
  }

  @GetMapping("/manage-contacts-regulator")
  ModelAndView showManageContactsRegulatorMockup() {
    var tableViewBuilder = getSortableTableView();

    for (var row : CONTACT_LIST) {
      var emailValue = getEmailValue(row);

      tableViewBuilder.addRow(
          getRowBuilder(row, emailValue)
              .build()
      );
    }

    return new ModelAndView("lms/mockups/manageContactsDesign")
        .addObject("contactsTableJson", tableViewBuilder.build().toString())
        .addObject("contactCount", CONTACT_LIST.size());
  }

  @GetMapping("/bulk-update-contacts/{licenceRef}")
  ModelAndView showBulkUpdateContactsMockup(@PathVariable String licenceRef) {
    var selectedRow = CONTACT_INDUSTRY_LIST.stream()
        .filter(cr -> cr.licence().equals(licenceRef))
        .findFirst()
        .orElseThrow();
    var targetLicences = CONTACT_INDUSTRY_LIST.stream()
        .filter(cr -> !cr.licence().equals(licenceRef))
        .map(cr -> new BulkUpdateContactRow(cr.id(), cr.licence(), cr.licensee(), cr.email()))
        .toList();
    return new ModelAndView("lms/mockups/bulkUpdateContactsDesign")
        .addObject("licenceRef", selectedRow.licence())
        .addObject("licensee", selectedRow.licensee())
        .addObject("currentEmail", selectedRow.email())
        .addObject("targetLicences", targetLicences);
  }

  private SortableTableRow.Builder getRowBuilder(ContactRow row, SortableTableValue emailValue) {
    var builder = SortableTableRow.builder()
        .withValue(row.licence());

    if (row.specialRole() != null) {
      builder.withValue(row.licensee(), new Tag(row.specialRole(), TagColour.YELLOW));
    } else {
      builder.withValue(row.licensee());
    }

    return builder.withValue(emailValue);
  }

  private SortableTableView.SortableTableViewBuilder getSortableTableView() {
    return SortableTableView.sortableTableBuilder()
        .newWithHeadings("Licence", "Licensee", "Contact Email")
        .withHeadingStyle(TableHeadingStyle.COLUMN)
        .withDefaultSortIndex(0)
        .withDefaultSortDirection(SortableTableSortDirection.ASCENDING);
  }

  private SortableTableValue getEmailValue(ContactRow row) {
    return row.email().isEmpty()
        ? new SortableTableValue("", null, null, List.of(new Tag("Not assigned", TagColour.GREY)))
        : new SortableTableValue(row.email());
  }
}
