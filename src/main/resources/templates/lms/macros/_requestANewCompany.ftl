<#include '../layout/layout.ftl'/>

<#macro displayRequest displayName>
  <@fdsDetails.summaryDetails
  summaryTitle="The ${displayName} I want to select is not in the list"
  >
    <p class="govuk-body">
      If the ${displayName} you want to select is not shown in the list then you can <@requestNewCompany.requestCompanyLink/>
    </p>
  </@fdsDetails.summaryDetails>
</#macro>