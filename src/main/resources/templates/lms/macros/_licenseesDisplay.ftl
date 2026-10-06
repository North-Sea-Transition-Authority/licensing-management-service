<#macro displayLicensees names>
  <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Licensees</h3>
  <ul class="govuk-list">
      <#list names as name>
        <li>${name}</li>
      </#list>
  </ul>
</#macro>