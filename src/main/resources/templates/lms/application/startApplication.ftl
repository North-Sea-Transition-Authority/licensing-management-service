<#include '../layout/layout.ftl'>

<#assign pageTitle = "Create a new application" />
<#-- @ftlvariable name="backLinkUrl" type="String" -->
<#-- @ftlvariable name="actionUrl" type="String" -->

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
backLinkUrl=springUrl(backLinkUrl)>

    <@fdsStartPage.startPage startActionText="Start now" startActionUrl=actionUrl>
      <p class="govuk-body">
        Use this to create a new application
      </p>
      <p class="govuk-body">
        You will be required to provide information on the xyz application.
      </p>
    </@fdsStartPage.startPage>
</@defaultPage>