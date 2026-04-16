package uk.co.nstauthority.licensingmanagementservice.duplication;

public interface DuplicableEntity<T> {

  T getParent();

  void setParent(T parent);
}
