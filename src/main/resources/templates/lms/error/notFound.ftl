<#include '../layout/layout.ftl'>
<#import './serviceSupport.ftl' as serviceSupportMacro>

<#assign pageTitle = "Page not found" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
phaseBanner=false
>
  <div class="govuk-body">
    <ul class="govuk-list">
      <li>If you typed the web address, check it is correct.</li>
      <li>If you pasted the web address, check you copied the entire address.</li>
    </ul>
    <p>If the web address is correct, or you selected a link or button, contact the service desk using the details
      below:</p>
      <@serviceSupportMacro.contactDetails
      emailSubject="Page not found"
      includeHeading=false
      />
  </div>
</@defaultPage>