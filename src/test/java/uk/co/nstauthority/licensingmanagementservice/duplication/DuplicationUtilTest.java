package uk.co.nstauthority.licensingmanagementservice.duplication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

class DuplicationUtilTest {

  @Test
  void instantiateBlankInstance() {
    var blankInstance = DuplicationUtil.instantiateBlankInstance(LicenceScheduleDetail.class);
    assertThat(blankInstance).hasAllNullFieldsOrProperties();
  }

  @Test
  void copyAllFields() {
    var example = new CopyUtilExampleClass(
        123,
        "description",
        new CopyUtilExampleClass(
            456,
            "desc 2",
            null
        )
    );

    var copyInstance = BeanUtils.instantiateClass(CopyUtilExampleClass.class);
    DuplicationUtil.copyProperties(example, copyInstance);

    assertThat(copyInstance)
        .extracting(
            CopyUtilExampleClass::getId,
            CopyUtilExampleClass::getDescription,
            CopyUtilExampleClass::getNested
        )
        .containsExactly(
            example.getId(),
            example.getDescription(),
            example.getNested()
        );
  }

  @Test
  void copyFieldsExceptId() {
    var example = new CopyUtilExampleClass(
        123,
        "description",
        new CopyUtilExampleClass(
            456,
            "desc 2",
            null
        )
    );

    var copyInstance = BeanUtils.instantiateClass(CopyUtilExampleClass.class);
    DuplicationUtil.copyProperties(example, copyInstance, "id");

    assertThat(copyInstance)
        .extracting(
            CopyUtilExampleClass::getDescription,
            CopyUtilExampleClass::getNested
        )
        .containsExactly(
            example.getDescription(),
            example.getNested()
        );

    assertThat(copyInstance.getId()).isNull();
  }

  @Test
  void copyFieldsIgnoringFieldThatDoesNotExist() {
    var example = new CopyUtilExampleClass(
        123,
        "description",
        new CopyUtilExampleClass(
            456,
            "desc 2",
            null
        )
    );

    var copyInstance = BeanUtils.instantiateClass(CopyUtilExampleClass.class);
    DuplicationUtil.copyProperties(example, copyInstance, "fieldThatDoesNotExist");

    assertThat(copyInstance)
        .extracting(
            CopyUtilExampleClass::getId,
            CopyUtilExampleClass::getDescription,
            CopyUtilExampleClass::getNested
        )
        .containsExactly(
            example.getId(),
            example.getDescription(),
            example.getNested()
        );
  }

  private static class CopyUtilExampleClass {
    private Integer id;
    private String description;
    private CopyUtilExampleClass nested;

    // Blank constructor required for DuplicationUtil::instantiateBlankInstance
    public CopyUtilExampleClass() {
    }

    public CopyUtilExampleClass(Integer id, String description, CopyUtilExampleClass nested) {
      this.id = id;
      this.description = description;
      this.nested = nested;
    }

    public Integer getId() {
      return id;
    }

    public String getDescription() {
      return description;
    }

    public CopyUtilExampleClass getNested() {
      return nested;
    }
  }
}