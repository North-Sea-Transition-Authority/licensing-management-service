package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;

class SubareaChangeTypeTest {

  private static final SubareaDetails A = subarea("A");
  private static final SubareaDetails B = subarea("B");
  private static final SubareaDetails A1 = subarea("A1");
  private static final SubareaDetails A2 = subarea("A2");

  @Test
  void toTag() {
    var tag = SubareaChangeType.SPLIT.toTag();

    assertThat(tag).isEqualTo(new Tag("Split", TagColour.TURQUOISE));
  }

  @ParameterizedTest
  @MethodSource("stagedChangeTypeParams")
  void getStagedChangeType(List<SubareaTransfer> transfers, SubareaChangeType expectedChangeType) {
    var transfer = transfers.getFirst();

    var changeType = SubareaChangeType.getStagedChangeType(transfer, transfers);

    assertThat(changeType).isEqualTo(expectedChangeType);
  }

  @ParameterizedTest
  @MethodSource("liveChangeTypeParams")
  void getLiveChangeType(SubareaTransfer transfer, SubareaChangeType expectedChangeType) {
    var changeType = SubareaChangeType.getLiveChangeType(transfer);

    assertThat(changeType).isEqualTo(expectedChangeType);
  }

  private static Stream<Arguments> stagedChangeTypeParams() {
    return Stream.of(
        Arguments.of(
            List.of(new SubareaTransfer(List.of(), List.of(A1))),
            SubareaChangeType.SET
        ),
        Arguments.of(
            List.of(new SubareaTransfer(List.of(A, B), List.of(A1))),
            SubareaChangeType.MERGE
        ),
        Arguments.of(
            List.of(new SubareaTransfer(List.of(A), List.of(A1, A2))),
            SubareaChangeType.SPLIT
        ),
        Arguments.of(
            List.of(
                new SubareaTransfer(List.of(A), List.of(A1)),
                new SubareaTransfer(List.of(A), List.of(A2))
            ),
            SubareaChangeType.SPLIT
        ),
        Arguments.of(
            List.of(
                new SubareaTransfer(List.of(A), List.of(A1)),
                new SubareaTransfer(List.of(A), List.of(A1))
            ),
            SubareaChangeType.SPLIT
        ),
        Arguments.of(
            List.of(
                new SubareaTransfer(List.of(A), List.of(A1)),
                new SubareaTransfer(List.of(B), List.of(A2))
            ),
            SubareaChangeType.RENAME
        )
    );
  }

  private static Stream<Arguments> liveChangeTypeParams() {
    return Stream.of(
        Arguments.of(
            new SubareaTransfer(List.of(), List.of(A1)),
            SubareaChangeType.SET
        ),
        Arguments.of(
            new SubareaTransfer(List.of(A), List.of()),
            SubareaChangeType.REMOVE
        ),
        Arguments.of(
            new SubareaTransfer(List.of(A, B), List.of(A, B)),
            SubareaChangeType.UNCHANGED
        ),
        Arguments.of(
            new SubareaTransfer(List.of(A), List.of(A1, A2)),
            SubareaChangeType.TRANSFER
        )
    );
  }

  private static SubareaDetails subarea(String shortName) {
    return new SubareaDetails(UUID.randomUUID(), shortName, shortName);
  }
}
