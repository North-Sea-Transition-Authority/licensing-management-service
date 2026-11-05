<#include '../layout/layout.ftl'>
<#import './serviceSupport.ftl' as serviceSupportMacro>

<#-- @ftlvariable name="stackTrace" type="String" -->
<#-- @ftlvariable name="errorRef" type="String" -->

<#assign pageTitle = "Sorry, there is a problem with the service" />
<#assign hasStackTrace = stackTrace?has_content />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
phaseBanner=false
pageSize=hasStackTrace?then(PageSize.FULL_COLUMN, PageSize.TWO_THIRDS_COLUMN)
extendContainerWidth=hasStackTrace
>
  <#if hasStackTrace>
    <@grid.gridRow>
      <@grid.twoThirdsColumn>
        <@_supportDetails/>
      </@grid.twoThirdsColumn>
    </@grid.gridRow>
    <@_stackTrace/>
  <#else>
    <@_supportDetails/>
  </#if>
</@defaultPage>

<#macro _supportDetails>
  <p class="govuk-body">
    If you continue to experience this problem, contact the service desk using the details below.
    Include <#if errorRef?has_content>the error reference, </#if>a description of what you were trying to do
    and, if relevant, the reference number for the activity or information you were working on.
  </p>
  <@serviceSupportMacro.contactDetails
  includeHeading=false
  errorReference=errorRef!""
  emailSubject=errorRef?has_content?then("Error reference - ${errorRef}", "")
  />
</#macro>

<#macro _stackTrace>
  <div class="lms-stack-trace__header">
    <h2 class="govuk-heading-m govuk-!-margin-bottom-0">Stacktrace</h2>
    <button
      type="button"
      class="govuk-link lms-stack-trace__copy-link"
      data-copy-target="stack-trace"
      data-copy-status="stack-trace-copy-status"
      hidden
    >Copy to clipboard</button>
  </div>
  <span id="stack-trace-copy-status" class="govuk-visually-hidden" aria-live="polite"></span>
  <pre id="stack-trace" class="lms-stack-trace" tabindex="0" aria-label="Stacktrace"><code>${stackTrace}</code></pre>
  <script src="<@spring.url '/assets/javascript/copyToClipboard.js'/>"></script>
</#macro>
