package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.io.Serial;
import java.io.Serializable;
import org.springframework.web.bind.annotation.SessionAttributes;

@SessionAttributes("workAreaSession")
public class WorkAreaSession implements Serializable {

  @Serial
  private static final long serialVersionUID = -5420071941113464786L;

  private WorkAreaFilterForm workAreaFilterForm;

  public WorkAreaSession(WorkAreaFilterForm workAreaFilterForm) {
    this.workAreaFilterForm = workAreaFilterForm;
  }

  public void clearSession() {
    workAreaFilterForm.clearFilter();
  }

  public void update(WorkAreaFilterForm workAreaFilterForm) {
    this.workAreaFilterForm = workAreaFilterForm;
  }

  public WorkAreaFilterForm getWorkAreaFilterForm() {
    return workAreaFilterForm;
  }
}
