package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RemoveChangeTest {

  @Test
  void json_doesNotCarryTheInheritedEmptyOperations() throws JsonProcessingException {
    var objectMapper = new ObjectMapper();
    var removeChange = LicencePositionChangeType.removeChange()
        .withChangeId("123")
        .build();

    var json = objectMapper.writeValueAsString(removeChange);

    assertThat(json).doesNotContain("operations");
    assertThat(objectMapper.readValue(json, LicencePositionChangeType.class)).isEqualTo(removeChange);
  }

  @Test
  void builder() {
    var removeChange = LicencePositionChangeType.removeChange()
        .withChangeId("123")
        .build();

    assertThat(removeChange.changeId()).isEqualTo("123");
    assertThat(removeChange.type()).isEqualTo(LicencePositionChangeType.REMOVE_CHANGE);
  }
}
