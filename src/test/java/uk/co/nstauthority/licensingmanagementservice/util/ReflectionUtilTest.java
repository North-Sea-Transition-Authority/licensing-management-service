package uk.co.nstauthority.licensingmanagementservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class ReflectionUtilTest {

  @Test
  void getAllMethods() {

    var methods = ReflectionUtil.getAllMethods(ReflectionUtilExampleClass.class);

    var methodNames = methods.stream().map(Method::getName).toList();

    assertThat(methodNames)
        .contains(
            "getId", "setId", "getDescription", "setDescription",
            "getSuperId", "setSuperId", "getSuperDescription", "setSuperDescription"
        );
  }

  @Test
  void getAllMethods_whenObjectClass_thenEmpty() {

    var methods = ReflectionUtil.getAllMethods(Object.class);

    assertThat(methods).isEmpty();
  }

  @Test
  void getAllFields() {

    var fields = ReflectionUtil.getAllFields(ReflectionUtilExampleClass.class);

    var fieldNames = fields.stream().map(Field::getName).toList();

    assertThat(fieldNames)
        .contains(
            "id", "description",
            "superId", "superDescription"
        );
  }

  @Test
  void getAllFields_whenObjectClass_thenEmpty() {

    var fields = ReflectionUtil.getAllFields(Object.class);

    assertThat(fields).isEmpty();
  }

  private static class ReflectionUtilExampleClass extends ReflectionUtilExampleSuperClass {
    private Integer id;
    private String description;
    public ReflectionUtilExampleClass(Integer id, String description,
                                      Integer superId, String superDescription) {
      super(superId, superDescription);
      this.id = id;
      this.description = description;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  private static class ReflectionUtilExampleSuperClass {
    private Integer superId;
    private String superDescription;
    public ReflectionUtilExampleSuperClass(Integer superId, String superDescription) {
      this.superId = superId;
      this.superDescription = superDescription;
    }

    public Integer getSuperId() {
      return superId;
    }

    public void setSuperId(Integer superId) {
      this.superId = superId;
    }

    public String getSuperDescription() {
      return superDescription;
    }

    public void setSuperDescription(String superDescription) {
      this.superDescription = superDescription;
    }
  }
}
