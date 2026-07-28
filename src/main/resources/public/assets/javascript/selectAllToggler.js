'use strict';

document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.lms-select-all-table').forEach(function (wrapper) {
    const selectAll = wrapper.querySelector('.lms-select-all-table__select-all');
    const rows = Array.from(wrapper.querySelectorAll("input[type='checkbox']"))
                      .filter(cb => cb !== selectAll);

    function syncSelectAll() {
      const total = rows.length;
      const checked = rows.filter(row => row.checked).length;

      selectAll.checked = (total > 0 && checked === total);
      selectAll.indeterminate = (checked > 0 && checked < total);
    }

    selectAll.addEventListener('change', function () {
      rows.forEach(row => row.checked = selectAll.checked);
    });

    rows.forEach(row => row.addEventListener('change', syncSelectAll));

    syncSelectAll();
  });
});
