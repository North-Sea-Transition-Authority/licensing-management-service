<#include '../layout/layout.ftl'/>

<#-- This is resolved from EnergyPortalAccountsControllerAdvice in the EPAS starter-->
<#-- @ftlvariable name="requestCompanyUrl" type="java.lang.String" -->

<#macro requestCompanyLink>
  <@fdsAction.link
    linkText="request to add a new organisation (opens in a new tab)"
    linkUrl=requestCompanyUrl
    openInNewTab=true
  />
</#macro>