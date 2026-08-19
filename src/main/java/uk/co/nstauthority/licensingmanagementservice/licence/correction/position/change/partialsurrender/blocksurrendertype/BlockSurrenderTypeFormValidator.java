package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class BlockSurrenderTypeFormValidator {

  private static final String SURRENDER_TYPE_FIELD = "surrenderType";
  private static final String SURRENDER_TYPE_ERROR = "Select the type of surrender for this block";

  public boolean hasErrors(BlockSurrenderTypeForm form, Errors errors) {
    if (StringUtils.isBlank(form.getSurrenderType()) || !isValidType(form.getSurrenderType())) {
      errors.rejectValue(SURRENDER_TYPE_FIELD, "surrenderType.required", SURRENDER_TYPE_ERROR);
    }
    return errors.hasErrors();
  }

  private boolean isValidType(String value) {
    return Arrays.stream(BlockSurrenderType.values())
        .anyMatch(type -> type.name().equals(value));
  }
}
