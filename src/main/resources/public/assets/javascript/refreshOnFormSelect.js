$(function () {
  $("#type").on('change', function () {
    $('#filterForm form:first').submit();
  });
});