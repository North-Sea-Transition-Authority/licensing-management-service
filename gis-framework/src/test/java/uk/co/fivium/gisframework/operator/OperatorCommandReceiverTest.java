package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.OperatorCommand;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.command.TransformationType;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class OperatorCommandReceiverTest {

  @Mock
  private SplitOperatorService splitOperatorService;

  @Mock
  private FeatureService featureService;

  @Mock
  private OperatorCommandService operatorCommandService;

  @InjectMocks
  private OperatorCommandReceiver operatorCommandReceiver;

  @Test
  void executeSplit_whenFeatureIsSplit_deactivatesInputAndActivatesOutput() {
    var commandJourney = new CommandJourney();
    var inputFeature = FeatureTestUtil.newBuilder().build();
    inputFeature.setCommandJourney(commandJourney);

    var outputFeature1 = FeatureTestUtil.newBuilder().build();
    var outputFeature2 = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy cutter line esriJson";
    var command = new OperatorCommand();

    when(splitOperatorService.splitPolygon(inputFeature, cutterLineEsriJson))
        .thenReturn(List.of(outputFeature1, outputFeature2));
    when(operatorCommandService.createOperatorCommand(
        commandJourney, Set.of(inputFeature.getId()), TransformationType.SPLIT)).thenReturn(command);

    var result = operatorCommandReceiver.executeSplit(List.of(inputFeature), cutterLineEsriJson);

    assertThat(result).containsExactly(outputFeature1, outputFeature2);
    assertThat(inputFeature.isActive()).isFalse();
    assertThat(outputFeature1).extracting(
        Feature::getCommandJourney,
        Feature::isActive,
        Feature::getCreatedByCommand
    ).contains(commandJourney, true, command);
    assertThat(outputFeature2).extracting(
        Feature::getCommandJourney,
        Feature::isActive,
        Feature::getCreatedByCommand
    ).contains(commandJourney, true, command);

    verify(featureService).saveFeatures(List.of(inputFeature));
    verify(featureService).saveFeatures(List.of(outputFeature1, outputFeature2));
  }

  @Test
  void executeSplit_whenNoFeatureIsSplit_returnsEmptyListAndDoesNotRecordCommand() {
    var inputFeature = FeatureTestUtil.newBuilder().build();
    var cutterLineEsriJson = "dummy cutter line esriJson";

    when(splitOperatorService.splitPolygon(inputFeature, cutterLineEsriJson)).thenReturn(List.of());

    var result = operatorCommandReceiver.executeSplit(List.of(inputFeature), cutterLineEsriJson);

    assertThat(result).isEmpty();
    assertThat(inputFeature.isActive()).isTrue();
    verify(operatorCommandService, never()).createOperatorCommand(any(), any(), any());
    verify(featureService, never()).saveFeatures(any());
  }

  @Test
  void executeSplit_whenNoInputFeatures_returnsEmptyList() {
    assertThat(operatorCommandReceiver.executeSplit(List.of(), "dummy cutter line esriJson")).isEmpty();
    verify(splitOperatorService, never()).splitPolygon(any(), any());
  }
}
