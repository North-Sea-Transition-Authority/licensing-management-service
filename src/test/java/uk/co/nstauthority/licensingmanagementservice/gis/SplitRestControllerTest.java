package uk.co.nstauthority.licensingmanagementservice.gis;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.operator.JsonSplitHistoryStatus;
import uk.co.fivium.gisframework.operator.JsonSplitResponse;
import uk.co.fivium.gisframework.operator.OperatorCommandReceiver;
import uk.co.fivium.gisframework.operator.SplitFromMapRequest;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ContextConfiguration(classes = SplitRestController.class)
class SplitRestControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OperatorCommandReceiver operatorCommandReceiver;

  @MockitoBean
  private CommandJourneyService commandJourneyService;

  @MockitoBean
  private OperatorCommandService operatorCommandService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void split_whenSplitOccurs_assertOutputFeatureIds() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var outputFeature1 = FeatureTestUtil.builder().build();


    var requestBody = """
        {
          "cutterLineOriginalSrsCoordinates": [[[1.1, 2.2], [3.3, 4.4]]],
          "commandJourneyId": "%s"
        }
        """.formatted(commandJourneyId);

    when(operatorCommandReceiver.executeSplit(new SplitFromMapRequest(
        List.of(List.of(List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(2.2)), List.of(BigDecimal.valueOf(3.3), BigDecimal.valueOf(4.4)))),
        commandJourneyId
    ))).thenReturn(List.of(outputFeature1));

    var expected = new JsonSplitResponse(List.of(outputFeature1.getId().toString()));

    mockMvc.perform(post("/api/gis-framework/split")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(expected), JsonCompareMode.STRICT));
  }

  @Test
  void split_whenNoCutterLineCoordinates_assertEmptyOutputFeatureIds() throws Exception {
    var commandJourneyId = UUID.randomUUID();

    var requestBody = """
        {
          "cutterLineOriginalSrsCoordinates": [],
          "commandJourneyId": "%s"
        }
        """.formatted(commandJourneyId);

    when(operatorCommandReceiver.executeSplit(new SplitFromMapRequest(List.of(), commandJourneyId)))
        .thenReturn(List.of());

    mockMvc.perform(post("/api/gis-framework/split")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(new JsonSplitResponse(List.of())), JsonCompareMode.STRICT));
  }

  @Test
  void getSplitHistory_whenCanUndo_assertCanUndoTrue() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var commandJourney = new CommandJourney();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(operatorCommandService.canUndo(commandJourney)).thenReturn(true);

    mockMvc.perform(get("/api/gis-framework/split-history/{commandJourneyId}", commandJourneyId)
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(new JsonSplitHistoryStatus(true)), JsonCompareMode.STRICT));
  }

  @Test
  void getSplitHistory_whenCannotUndo_assertCanUndoFalse() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var commandJourney = new CommandJourney();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(operatorCommandService.canUndo(commandJourney)).thenReturn(false);

    mockMvc.perform(get("/api/gis-framework/split-history/{commandJourneyId}", commandJourneyId)
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(new JsonSplitHistoryStatus(false)), JsonCompareMode.STRICT));
  }

  @Test
  void undo_whenActiveCommandExists_assertReactivatedFeatureIds() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var commandJourney = new CommandJourney();
    var reactivatedFeature = FeatureTestUtil.builder().build();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(operatorCommandReceiver.undo(commandJourney)).thenReturn(List.of(reactivatedFeature));

    mockMvc.perform(post("/api/gis-framework/undo/{commandJourneyId}", commandJourneyId)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(
            objectMapper.writeValueAsString(new JsonSplitResponse(List.of(reactivatedFeature.getId().toString()))),
            JsonCompareMode.STRICT));
  }

  @Test
  void undo_whenNoActiveCommand_assertEmptyOutputFeatureIds() throws Exception {
    var commandJourneyId = UUID.randomUUID();
    var commandJourney = new CommandJourney();

    when(commandJourneyService.getCommandJourneyOrThrow(commandJourneyId)).thenReturn(commandJourney);
    when(operatorCommandReceiver.undo(commandJourney)).thenReturn(List.of());

    mockMvc.perform(post("/api/gis-framework/undo/{commandJourneyId}", commandJourneyId)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(status().isOk())
        .andExpect(content().json(objectMapper.writeValueAsString(new JsonSplitResponse(List.of())), JsonCompareMode.STRICT));
  }
}
