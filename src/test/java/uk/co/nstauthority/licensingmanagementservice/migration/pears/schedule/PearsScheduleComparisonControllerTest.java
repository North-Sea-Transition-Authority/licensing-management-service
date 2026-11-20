package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
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
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ActiveProfiles("migration")
@TestPropertySource(properties = {
    "pears.datasource.url=not-used-by-controller-tests",
    "pears.datasource.username=pears",
    "pears.datasource.password=pears"
})
@ContextConfiguration(classes = PearsScheduleComparisonController.class)
class PearsScheduleComparisonControllerTest extends AbstractControllerTest {

  private static final String ROUTE =
      ReverseRouter.route(on(PearsScheduleComparisonController.class).compare(null, null, null, null, null));

  private static final PearsScheduleComparison NOT_IN_LMS =
      PearsScheduleComparison.problem("P1556", "Not a licence in LMS");

  private static final PearsScheduleComparison NO_ISSUES = new PearsScheduleComparison("P1728", null, null, List.of(
      new PearsScheduleComparison.Version(1L, null, null, null, null, null, null, List.of(
          new PearsScheduleComparison.Row("Start", "Schedule start", null, null, null, null, null, null, null,
              PearsScheduleComparison.Result.MATCH, null)))));

  private static final PearsScheduleComparison NOT_MIGRATED = new PearsScheduleComparison("P2000", null, null, List.of(
      new PearsScheduleComparison.Version(1L, null, null, null, null, null, null, List.of(
          new PearsScheduleComparison.Row("Rate", "Rate", null, null, null, null, null, null, null,
              PearsScheduleComparison.Result.NOT_MIGRATED, "After the last term")))));

  @MockitoBean
  private PearsScheduleComparisonService comparisonService;

  @Test
  void compare_whenGet_showsThePage() throws Exception {
    givenTheComparisons(PearsScheduleComparison.problem("P1556", "Not a licence in LMS"));

    mockMvc.perform(get(ROUTE).param("licences", "P1556").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(content().string(Matchers.containsString("<h2>P1556</h2>")));

    verify(comparisonService).compare(List.of(new PearsLicenceReference("P", 1556)), false);
  }

  @Test
  void compare_withAllLicences_comparesEveryMigratedLicence() throws Exception {
    var migrated = List.of(new PearsLicenceReference("P", 1556), new PearsLicenceReference("P", 1728));
    when(comparisonService.migratedLicences()).thenReturn(migrated);
    givenTheComparisons(NOT_IN_LMS, NO_ISSUES);

    mockMvc.perform(get(ROUTE).param("licences", "all").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("<p>2 licences compared</p>")))
        .andExpect(content().string(Matchers.containsString("<h2>P1728</h2>")));

    verify(comparisonService).compare(migrated, false);
  }

  @Test
  void compare_withOnlyIssues_showsJustTheLicencesWithIssues() throws Exception {
    givenTheComparisons(NOT_IN_LMS, NO_ISSUES);

    mockMvc.perform(get(ROUTE).param("licences", "P1556,P1728").param("issues", "only").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<p>2 licences compared, 1 with issues to look into: only those are shown</p>")))
        .andExpect(content().string(Matchers.containsString("<h2>P1556</h2>")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<h2>P1728</h2>"))));
  }

  @Test
  void compare_withOnlyIssuesAsCsv_writesJustTheLicencesWithIssues() throws Exception {
    givenTheComparisons(NOT_IN_LMS, NO_ISSUES);

    mockMvc.perform(get(ROUTE).param("licences", "P1556,P1728").param("issues", "only").param("format", "csv")
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("P1556,,,,Not a licence in LMS")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("P1728"))));
  }

  @Test
  void compare_withOnlyNotMigrated_showsJustTheLicencesWithEventsNotMigrated() throws Exception {
    givenTheComparisons(NOT_IN_LMS, NO_ISSUES, NOT_MIGRATED);

    mockMvc.perform(get(ROUTE).param("licences", "P1556,P1728,P2000").param("notMigrated", "only")
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<p>3 licences compared, 1 with events not migrated: only those are shown</p>")))
        .andExpect(content().string(Matchers.containsString("<h2>P2000</h2>")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<h2>P1556</h2>"))))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<h2>P1728</h2>"))));
  }

  @Test
  void compare_withOnlyIssuesAndOnlyNotMigrated_showsTheLicencesWithEither() throws Exception {
    givenTheComparisons(NOT_IN_LMS, NO_ISSUES, NOT_MIGRATED);

    mockMvc.perform(get(ROUTE).param("licences", "P1556,P1728,P2000").param("issues", "only")
            .param("notMigrated", "only").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<p>3 licences compared, 2 with issues to look into or events not migrated: only those are shown</p>")))
        .andExpect(content().string(Matchers.containsString("<h2>P1556</h2>")))
        .andExpect(content().string(Matchers.containsString("<h2>P2000</h2>")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<h2>P1728</h2>"))));
  }

  @Test
  void compare_withANotMigratedOptionTheReportDoesNotHave_isABadRequest() throws Exception {
    mockMvc.perform(get(ROUTE).param("licences", "P1556").param("notMigrated", "all").with(user(regulatorUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("all is not an option of the comparison report"));

    verifyNoInteractions(comparisonService);
  }

  @Test
  void compare_withAnIssuesOptionTheReportDoesNotHave_isABadRequest() throws Exception {
    mockMvc.perform(get(ROUTE).param("licences", "P1556").param("issues", "some").with(user(regulatorUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("some is not an option of the comparison report"));

    verifyNoInteractions(comparisonService);
  }

  @Test
  void compare_whenPost_readsEachLicenceGivenOnce() throws Exception {
    givenTheComparisons();

    mockMvc.perform(post(ROUTE).param("licences", " p1556, PEDL236 ,,P1556").with(user(regulatorUser)).with(csrf()))
        .andExpect(status().isOk());

    verify(comparisonService).compare(
        List.of(new PearsLicenceReference("P", 1556), new PearsLicenceReference("PEDL", 236)), false);
  }

  @Test
  void compare_withEveryVersion_comparesEveryVersion() throws Exception {
    givenTheComparisons();

    mockMvc.perform(get(ROUTE).param("licences", "P1556").param("versions", "all").with(user(regulatorUser)))
        .andExpect(status().isOk());

    verify(comparisonService).compare(List.of(new PearsLicenceReference("P", 1556)), true);
  }

  @Test
  void compare_asCsv_downloadsAFile() throws Exception {
    givenTheComparisons(PearsScheduleComparison.problem("P1556", "Not a licence in LMS"));

    mockMvc.perform(get(ROUTE).param("licences", "P1556").param("format", "csv").with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"pears-schedule-comparison.csv\""))
        .andExpect(content().string(Matchers.containsString("P1556,,,,Not a licence in LMS")));
  }

  @Test
  void compare_withSomethingThatIsNotALicenceReference_isABadRequest() throws Exception {
    mockMvc.perform(get(ROUTE).param("licences", "P1556,1556").with(user(regulatorUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("1556 is not a PEARS licence reference"));

    verifyNoInteractions(comparisonService);
  }

  @Test
  void compare_withNoLicences_isABadRequest() throws Exception {
    mockMvc.perform(get(ROUTE).param("licences", " , ").with(user(regulatorUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("No licences given to compare"));
  }

  @Test
  void compare_withAFormatTheReportDoesNotHave_isABadRequest() throws Exception {
    mockMvc.perform(get(ROUTE).param("licences", "P1556").param("format", "pdf").with(user(regulatorUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("pdf is not an option of the comparison report"));

    verifyNoInteractions(comparisonService);
  }

  private void givenTheComparisons(PearsScheduleComparison... comparisons) {
    when(comparisonService.compare(anyList(), anyBoolean())).thenReturn(List.of(comparisons));
  }
}
