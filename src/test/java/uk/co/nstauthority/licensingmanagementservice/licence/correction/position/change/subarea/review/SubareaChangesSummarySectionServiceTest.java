package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Layer;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.SubareaSetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@ExtendWith(MockitoExtension.class)
class SubareaChangesSummarySectionServiceTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final Feature BLOCK = FeatureTestUtil.blockFeature(BLOCK_FEATURE_ID, "110", 8);
  private static final String CHANGE_SUBAREAS_URL = "/change-subareas";
  private static final int ORGANISATION_ONE = 1;
  private static final int ORGANISATION_TWO = 2;

  @Mock
  private SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;

  @Mock
  private FeatureService featureService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @InjectMocks
  private SubareaChangesSummarySectionService subareaChangesSummarySectionService;

  @Test
  void getSummarySection_whenStagedSubareasAreSplitMergedAndRenamed_thenEachNetChangeIsSortedBySubareaName() {
    var a = subareaFeature("A", 10_000_000);
    var b = subareaFeature("B", 20_000_000);
    var c = subareaFeature("C", 30_000_000);
    var d = subareaFeature("D", 40_000_000);
    var legacySubarea = new SubareaDetails(null, null, "E");
    var a1 = subareaFeature("A1", 5_000_000);
    var aAndC = subareaFeature("F", 15_000_000);
    var d1 = subareaFeature("D1", 15_000_000);
    var d2 = subareaFeature("D2", 25_000_000);
    var b2 = subareaFeature("B2", 20_000_000);
    var stateBefore = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(a), details(b), details(c), details(d), legacySubarea))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_ONE, BigDecimal.valueOf(50))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_TWO, BigDecimal.valueOf(50))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(b), ORGANISATION_TWO, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(c), ORGANISATION_ONE, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(d), ORGANISATION_ONE, BigDecimal.valueOf(100));
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(details(a), details(b), details(c), details(d), legacySubarea),
        List.of(
            transfer(List.of(d), List.of(d1, d2)),
            transfer(List.of(a, c), List.of(aAndC)),
            transfer(List.of(b), List.of(b2)),
            transfer(List.of(a), List.of(a1))
        ),
        stateBefore,
        CHANGE_SUBAREAS_URL
    );
    givenLineageLost(stateBefore, List.of(d), false);
    givenLineageLost(stateBefore, List.of(a, c), true);
    givenLineageLost(stateBefore, List.of(b), false);
    givenLineageLost(stateBefore, List.of(a), false);
    givenFeatures(a, b, c, d, a1, aAndC, d1, d2, b2);
    givenOrganisationNames(Set.of(ORGANISATION_ONE, ORGANISATION_TWO));

    var section = subareaChangesSummarySectionService.getSummarySection(context, null);

    var aCard = card("Subarea A", "10.00 km²", "Organisation one (50%)", "Organisation two (50%)");
    var legacyCard = SummaryCard.simpleSummaryCardWithHeading("Subarea E", SummaryDataView.newBuilder().build());
    assertThat(section).usingRecursiveComparison().isEqualTo(changes(
        section(
            "Subarea A",
            List.of(new Tag("Split", TagColour.TURQUOISE)),
            before(List.of(a), aCard),
            after(List.of(a1), card("Subarea A1", "5.00 km²", "Organisation one (50%)", "Organisation two (50%)"))
        ),
        section(
            "Subareas A and C",
            List.of(new Tag("Merge", TagColour.BLUE), new Tag("Lineage lost", TagColour.RED)),
            before(List.of(a, c), aCard, card("Subarea C", "30.00 km²", "Organisation one (100%)")),
            after(List.of(aAndC), notAssignedCard("Subarea F", "15.00 km²"))
        ),
        section(
            "Subarea B",
            List.of(new Tag("Rename", TagColour.LIGHT_BLUE)),
            before(List.of(b), card("Subarea B", "20.00 km²", "Organisation two (100%)")),
            after(List.of(b2), card("Subarea B2", "20.00 km²", "Organisation two (100%)"))
        ),
        section(
            "Subarea D",
            List.of(new Tag("Split", TagColour.TURQUOISE)),
            before(List.of(d), card("Subarea D", "40.00 km²", "Organisation one (100%)")),
            after(
                List.of(d1, d2),
                card("Subarea D1", "15.00 km²", "Organisation one (100%)"),
                card("Subarea D2", "25.00 km²", "Organisation one (100%)")
            )
        ),
        section(
            "Subarea E",
            List.of(new Tag("Unchanged", TagColour.GREY)),
            before(List.of(), legacyCard),
            after(List.of(), legacyCard)
        )
    ));
  }

  @Test
  void getSummarySection_whenStagedMergeSharesBeneficialInterests_thenLineageIsKept() {
    var a = subareaFeature("A", 10_000_000);
    var c = subareaFeature("C", 30_000_000);
    var aAndC = subareaFeature("F", 40_000_000);
    var stateBefore = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(a), details(c)))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_ONE, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(c), ORGANISATION_ONE, BigDecimal.valueOf(100));
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(details(a), details(c)),
        List.of(transfer(List.of(a, c), List.of(aAndC))),
        stateBefore,
        CHANGE_SUBAREAS_URL
    );
    givenLineageLost(stateBefore, List.of(a, c), false);
    givenFeatures(a, c, aAndC);
    givenOrganisationNames(Set.of(ORGANISATION_ONE));

    var section = subareaChangesSummarySectionService.getSummarySection(context, null);

    assertThat(section).usingRecursiveComparison().isEqualTo(changes(
        section(
            "Subareas A and C",
            List.of(new Tag("Merge", TagColour.BLUE)),
            before(
                List.of(a, c),
                card("Subarea A", "10.00 km²", "Organisation one (100%)"),
                card("Subarea C", "30.00 km²", "Organisation one (100%)")
            ),
            after(List.of(aAndC), card("Subarea F", "40.00 km²", "Organisation one (100%)"))
        )
    ));
  }

  @Test
  void getSummarySection_whenStagedOutputHasBeneficialInterests_thenShowsThemInsteadOfInherited() {
    var a = subareaFeature("A", 10_000_000);
    var a1 = subareaFeature("A1", 10_000_000);
    var stateBefore = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(a)))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_ONE, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a1), ORGANISATION_TWO, BigDecimal.valueOf(100));
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(details(a)),
        List.of(transfer(List.of(a), List.of(a1))),
        stateBefore,
        CHANGE_SUBAREAS_URL
    );
    givenLineageLost(stateBefore, List.of(a), false);
    givenFeatures(a, a1);
    givenOrganisationNames(Set.of(ORGANISATION_ONE, ORGANISATION_TWO));

    var section = subareaChangesSummarySectionService.getSummarySection(context, null);

    assertThat(section).usingRecursiveComparison().isEqualTo(changes(
        section(
            "Subarea A",
            List.of(new Tag("Rename", TagColour.LIGHT_BLUE)),
            before(List.of(a), card("Subarea A", "10.00 km²", "Organisation one (100%)")),
            after(List.of(a1), card("Subarea A1", "10.00 km²", "Organisation two (100%)"))
        )
    ));
  }

  @Test
  void getSummarySection_whenLineageLostMergeHasStagedBeneficialInterests_thenShowsThemWithoutNotAssigned() {
    var a = subareaFeature("A", 10_000_000);
    var c = subareaFeature("C", 30_000_000);
    var aAndC = subareaFeature("F", 40_000_000);
    var stateBefore = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(a), details(c)))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_ONE, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(c), ORGANISATION_TWO, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(aAndC), ORGANISATION_TWO, BigDecimal.valueOf(100));
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(details(a), details(c)),
        List.of(transfer(List.of(a, c), List.of(aAndC))),
        stateBefore,
        CHANGE_SUBAREAS_URL
    );
    givenLineageLost(stateBefore, List.of(a, c), true);
    givenFeatures(a, c, aAndC);
    givenOrganisationNames(Set.of(ORGANISATION_ONE, ORGANISATION_TWO));

    var section = subareaChangesSummarySectionService.getSummarySection(context, null);

    assertThat(section).usingRecursiveComparison().isEqualTo(changes(
        section(
            "Subareas A and C",
            List.of(new Tag("Merge", TagColour.BLUE), new Tag("Lineage lost", TagColour.RED)),
            before(
                List.of(a, c),
                card("Subarea A", "10.00 km²", "Organisation one (100%)"),
                card("Subarea C", "30.00 km²", "Organisation two (100%)")
            ),
            after(List.of(aAndC), card("Subarea F", "40.00 km²", "Organisation two (100%)"))
        )
    ));
  }

  @Test
  void getSummarySection_whenStagedTransferReplacesNoSubarea_thenSetsSubareaWithNoBeneficialInterests() {
    var a = subareaFeature("A", 10_000_000);
    var g = subareaFeature("G", 70_000_000);
    var stateBefore = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(a)))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_ONE, BigDecimal.valueOf(100));
    var context = new SubareaChangeSummaryContext.Staged(
        BLOCK_FEATURE_ID,
        List.of(details(a)),
        List.of(transfer(List.of(), List.of(g))),
        stateBefore,
        CHANGE_SUBAREAS_URL
    );
    givenLineageLost(stateBefore, List.of(), true);
    givenFeatures(a, g);
    givenOrganisationNames(Set.of(ORGANISATION_ONE));

    var section = subareaChangesSummarySectionService.getSummarySection(context, null);

    var aCard = card("Subarea A", "10.00 km²", "Organisation one (100%)");
    assertThat(section).usingRecursiveComparison().isEqualTo(changes(
        section(
            "Subarea A",
            List.of(new Tag("Unchanged", TagColour.GREY)),
            before(List.of(a), aCard),
            after(List.of(a), aCard)
        ),
        section(
            "Subarea G",
            List.of(new Tag("Set", TagColour.GREEN)),
            new SummaryColumn("Before", null, List.of(), "This change did not replace any subareas."),
            after(List.of(g), card("Subarea G", "70.00 km²"))
        )
    ));
  }

  @Test
  void getSummarySection_whenLive_thenOutputsShowBeneficialInterestsAtEndOfPosition() {
    var a = subareaFeature("A", 10_000_000);
    var b = subareaFeature("B", 20_000_000);
    var c = subareaFeature("C", 30_000_000);
    var d = subareaFeature("D", 40_000_000);
    var e = subareaFeature("E", 50_000_000);
    var f = subareaFeature("F", 60_000_000);
    var a2 = subareaFeature("A2", 10_000_000);
    var g = subareaFeature("G", 70_000_000);
    var h = subareaFeature("H", 120_000_000);
    var stateBefore = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(a), details(b), details(c), details(d), details(e), details(f)))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a), ORGANISATION_ONE, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(b), ORGANISATION_TWO, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(f), ORGANISATION_ONE, BigDecimal.valueOf(100));
    var stateAtEnd = LicencePositionState.EMPTY
        .withBlock(BLOCK_FEATURE_ID, List.of(details(f), details(a2), details(g), details(h)))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(a2), ORGANISATION_TWO, BigDecimal.valueOf(100))
        .withSubareaEquity(BLOCK_FEATURE_ID, details(g), ORGANISATION_ONE, BigDecimal.valueOf(100));
    var context = new SubareaChangeSummaryContext.LiveChange(
        BLOCK_FEATURE_ID,
        List.of(details(a), details(b), details(c), details(d), details(e), details(f)),
        List.of(
            transfer(List.of(a), List.of(a2)),
            transfer(List.of(), List.of(g)),
            transfer(List.of(b), List.of()),
            transfer(List.of(e, c, d), List.of(h))
        ),
        stateBefore,
        stateAtEnd,
        CHANGE_SUBAREAS_URL
    );
    givenFeatures(a, b, c, d, e, f, a2, g, h);
    givenOrganisationNames(Set.of(ORGANISATION_ONE, ORGANISATION_TWO));

    var section = subareaChangesSummarySectionService.getSummarySection(context, null);

    var fCard = card("Subarea F", "60.00 km²", "Organisation one (100%)");
    assertThat(section).usingRecursiveComparison().isEqualTo(changes(
        section(
            "Subarea A",
            List.of(new Tag("Transfer", TagColour.BLUE)),
            before(List.of(a), card("Subarea A", "10.00 km²", "Organisation one (100%)")),
            after(List.of(a2), card("Subarea A2", "10.00 km²", "Organisation two (100%)"))
        ),
        section(
            "Subarea B",
            List.of(new Tag("Remove", TagColour.RED)),
            before(List.of(b), card("Subarea B", "20.00 km²", "Organisation two (100%)")),
            new SummaryColumn("After", null, List.of(), "This change removed these subareas.")
        ),
        section(
            "Subareas C, D and E",
            List.of(new Tag("Transfer", TagColour.BLUE)),
            before(
                List.of(c, d, e),
                card("Subarea C", "30.00 km²"),
                card("Subarea D", "40.00 km²"),
                card("Subarea E", "50.00 km²")
            ),
            after(List.of(h), card("Subarea H", "120.00 km²"))
        ),
        section(
            "Subarea F",
            List.of(new Tag("Unchanged", TagColour.GREY)),
            before(List.of(f), fCard),
            after(List.of(f), fCard)
        ),
        section(
            "Subarea G",
            List.of(new Tag("Set", TagColour.GREEN)),
            new SummaryColumn("Before", null, List.of(), "This change did not replace any subareas."),
            after(List.of(g), card("Subarea G", "70.00 km²", "Organisation one (100%)"))
        )
    ));
  }

  private void givenLineageLost(
      LicencePositionState stateBefore,
      List<Feature> replacedSubareas,
      boolean isLineageLost
  ) {
    when(subareaSetEquityCorrectionService.isLineageLost(
        stateBefore,
        BLOCK_FEATURE_ID,
        replacedSubareas.stream().map(SubareaChangesSummarySectionServiceTest::details).toList()
    )).thenReturn(isLineageLost);
  }

  private void givenFeatures(Feature... features) {
    when(featureService.getFeatureOrThrow(BLOCK_FEATURE_ID)).thenReturn(BLOCK);
    var featureIds = Stream.of(features).map(Feature::getId).collect(Collectors.toSet());
    when(featureService.getFeaturesByIds(featureIds)).thenReturn(List.of(features));
  }

  private void givenOrganisationNames(Set<Integer> organisationIds) {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationIds))
        .thenReturn(Map.of(ORGANISATION_ONE, "Organisation one", ORGANISATION_TWO, "Organisation two"));
  }

  private static Feature subareaFeature(
      String name,
      long squareMetres
  ) {
    return FeatureTestUtil.builder()
        .withFeatureName(name)
        .withAttributes(Map.of("LAYER", Layer.SUBAREAS.name(), "NAME", name))
        .withFeatureArea(BigDecimal.valueOf(squareMetres))
        .build();
  }

  private static SubareaDetails details(Feature feature) {
    return SubareaDetails.from(feature);
  }

  private static SubareaTransfer transfer(
      List<Feature> replacedSubareas,
      List<Feature> outputSubareas
  ) {
    return new SubareaTransfer(
        replacedSubareas.stream().map(SubareaChangesSummarySectionServiceTest::details).toList(),
        outputSubareas.stream().map(SubareaChangesSummarySectionServiceTest::details).toList()
    );
  }

  private static SummaryCard card(
      String heading,
      String area,
      String... beneficialInterests
  ) {
    var details = SummaryDataView.newBuilder().addStringValue("Area", area);
    if (beneficialInterests.length > 0) {
      details.addStringValue("Beneficial interests", List.of(beneficialInterests));
    }
    return SummaryCard.simpleSummaryCardWithHeading(heading, details.build());
  }

  private static SummaryCard notAssignedCard(
      String heading,
      String area
  ) {
    var details = SummaryDataView.newBuilder()
        .addStringValue("Area", area)
        .addStringValue("Beneficial interests", "Not assigned");
    return SummaryCard.simpleSummaryCardWithHeading(heading, details.build());
  }

  private static Optional<SummarySection> changes(SummaryItem... sections) {
    return Optional.of(new SummarySection(SubareaChangesSummarySectionService.SECTION_ORDER, List.of(sections)));
  }

  private static SummaryItem section(
      String heading,
      List<Tag> tags,
      SummaryColumn before,
      SummaryColumn after
  ) {
    return SummaryItem.withRows(heading, List.of(new SummaryRow(List.of(before, after)))).withTags(tags);
  }

  private static SummaryColumn before(
      List<Feature> subareas,
      SummaryCard... cards
  ) {
    return new SummaryColumn("Before", map(subareas), List.of(cards), null);
  }

  private static SummaryColumn after(
      List<Feature> subareas,
      SummaryCard... cards
  ) {
    var cardsWithChangeAction = Stream.of(cards)
        .map(card -> card.withAction(SummaryCardAction.post("Change", CHANGE_SUBAREAS_URL)))
        .toList();
    return new SummaryColumn("After", map(subareas), cardsWithChangeAction, null);
  }

  private static SummaryMapView map(List<Feature> subareas) {
    if (subareas.isEmpty()) {
      return null;
    }
    return new SummaryMapView(subareas.stream().map(Feature::getId).toList(), 4230);
  }
}
