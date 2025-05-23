<#include '../layout/layout.ftl'>
<#import './serviceSupport.ftl' as serviceSupportMacro>

<#-- @ftlvariable name="stackTrace" type="String" -->
<#-- @ftlvariable name="errorRef" type="String" -->

<#assign pageTitle = "Sorry, there is a problem with the service" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
phaseBanner=false
pageSize=stackTrace?has_content?then(PageSize.FULL_COLUMN, PageSize.TWO_THIRDS_COLUMN)
>
  <p class="govuk-body">
    If you continue to experience this problem, contact the service desk using the
    details below. Be sure to include the error reference below in any correspondence,
    along with a description of what you were trying to do and, if relevant, the reference
    number for the activity or information you were working on.
  </p>
    <@_errorReference reference=errorRef!>
        <@serviceSupportMacro.contactDetails
        emailSubject="Error reference - ${errorRef}"
        includeHeading=false
        />
    </@_errorReference>
</@defaultPage>

<#macro _errorReference reference>
    <#if reference?has_content>
      <div class="govuk-body">
        <p>Error reference: <span class="govuk-!-font-weight-bold">${reference}</span></p>
          <#nested/>
      </div>
        <#if stackTrace?has_content>
          <h2 class="govuk-heading-l">Stacktrace</h2>
          <pre class="govuk-body">
            <code>${stackTrace}</code>
          </pre>
        </#if>
    </#if>
</#macro>
