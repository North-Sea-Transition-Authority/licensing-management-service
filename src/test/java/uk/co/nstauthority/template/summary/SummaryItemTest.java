package uk.co.nstauthority.template.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.template.summary.SummaryValueType.STRING_VALUE;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryItemTest {

  private static final String ITEM_DISPLAY_NAME = "Item display name";

  private static final List<SummaryKeyValue> summaryKeyValues = List.of(
      new SummaryKeyValue("key1", STRING_VALUE, List.of("value1")),
      new SummaryKeyValue("key2", STRING_VALUE, List.of("value2"))
  );

  private static final SummaryDataView summaryDataView = new SummaryDataView(summaryKeyValues);


  @Test
  void withCard_notEmpty() {
    assertThat(SummaryItem.withCard(ITEM_DISPLAY_NAME, SummaryCard.simpleSummaryCard(summaryDataView)))
        .isEqualTo(new SummaryItem(ITEM_DISPLAY_NAME, List.of(SummaryCard.simpleSummaryCard(summaryDataView))));
  }

  @Test
  void withCard_emptyNullInput() {
    assertThat(SummaryItem.withCard(ITEM_DISPLAY_NAME, null))
        .isEqualTo(new SummaryItem(ITEM_DISPLAY_NAME, SummaryCard.emptySummaryCardList()));
  }

  @Test
  void withCards_notEmpty() {
    List<SummaryCard> summaryCards =
        List.of(
            SummaryCard.simpleSummaryCard(summaryDataView),
            SummaryCard.simpleSummaryCard(summaryDataView)
        );
    assertThat(SummaryItem.withCards(ITEM_DISPLAY_NAME, summaryCards))
        .isEqualTo(new SummaryItem(ITEM_DISPLAY_NAME, summaryCards));
  }

  @Test
  void withCards_empty() {
    assertThat(SummaryItem.withCards(ITEM_DISPLAY_NAME, Collections.emptyList()))
        .isEqualTo(new SummaryItem(ITEM_DISPLAY_NAME, SummaryCard.emptySummaryCardList()));
  }

  @Test
  void withCards_emptyNullInput() {
    assertThat(SummaryItem.withCards(ITEM_DISPLAY_NAME, null))
        .isEqualTo(new SummaryItem(ITEM_DISPLAY_NAME, SummaryCard.emptySummaryCardList()));
  }
}
