package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;

@ActiveProfiles("migration")
@TestPropertySource(properties = {
    "pears.datasource.url=not-used-by-controller-tests",
    "pears.datasource.username=pears",
    "pears.datasource.password=pears"
})
@ContextConfiguration(classes = PearsWorkProgrammeComparisonController.class)
class PearsWorkProgrammeComparisonControllerTest extends AbstractControllerTest {

  private static final String COMPARE_URL = "/migration/pears-work-programmes/compare";

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);
  private static final PearsLicenceReference P1161 = new PearsLicenceReference("P", 1161);

  @MockitoBean
  private PearsWorkProgrammeComparisonService comparisonService;

  @Test
  void compare_givesAPageOfTheLicencesGiven() throws Exception {
    when(comparisonService.compareInParallel(List.of(P2244, P1161))).thenReturn(List.of(matching("P2244"), matching("P1161")));

    mockMvc.perform(get(COMPARE_URL).param("licences", "p2244, P1161").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("2 licences compared")));
  }

  @Test
  void compare_withAll_andIssuesOnly_keepsTheMigratedLicencesWithIssues() throws Exception {
    when(comparisonService.migratedLicences()).thenReturn(List.of(P2244, P1161));
    when(comparisonService.compareInParallel(List.of(P2244, P1161))).thenReturn(List.of(
        matching("P2244"), PearsWorkProgrammeComparison.problem("P1161", "Not a licence in LMS")));

    mockMvc.perform(get(COMPARE_URL).param("licences", "all").param("issues", "only").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "2 licences compared, 1 with issues to look into: only those are shown")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<h2>P2244</h2>"))));
  }

  @Test
  void compare_asCsv_isADownload() throws Exception {
    when(comparisonService.compareInParallel(List.of(P2244))).thenReturn(List.of(matching("P2244")));

    mockMvc.perform(get(COMPARE_URL).param("licences", "P2244").param("format", "csv").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition",
            "attachment; filename=\"pears-work-programme-comparison.csv\""));
    verify(comparisonService).compareInParallel(List.of(P2244));
  }

  @Test
  void compare_withAnOptionItDoesNotHave_isABadRequestThatDoesNotRepeatIt() throws Exception {
    mockMvc.perform(get(COMPARE_URL).param("licences", "P2244").param("format", "<script>").with(user(regulatorUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Not a licence reference or option of the comparison report"));
    verifyNoInteractions(comparisonService);
  }

  private static PearsWorkProgrammeComparison matching(String licenceReference) {
    return new PearsWorkProgrammeComparison(licenceReference, null, null, List.of(new Row("701", "OTHER", null,
        "Studies", "IN_PROGRESS", "Other activity", "Conditional", "Studies", "Within Initial Term", "In progress",
        "0", Result.MATCH, null)), List.of());
  }
}
