'use strict';

$(function () {
  $('.lms-select-all-table').each(function () {
    const $wrapper = $(this);
    const $selectAll = $wrapper.find('.lms-select-all-table__select-all');
    const $rows = $wrapper.find("input[type='checkbox']").not($selectAll);

    function syncSelectAll() {
      const total = $rows.length;
      const checked = $rows.filter(':checked').length;
      $selectAll.prop('checked', total > 0 && checked === total);
      $selectAll.prop('indeterminate', checked > 0 && checked < total);
    }

    $selectAll.on('change', function () {
      $rows.prop('checked', $selectAll.prop('checked'));
    });
    $rows.on('change', syncSelectAll);
    syncSelectAll();
  });
});
