<#include '../layout/layout.ftl'>

<#import '../summary/_xyzApplicationSummary.ftl' as applicationSummary>
<#import '_xyzApplicationContext.ftl' as applicationContextInfo>

<#-- @ftlvariable name="application" type="uk.co.nstauthority.licensingmanagementservice.application.Application" -->

<#assign pageTitle = application.getReference()/>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=""
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkUrl=springUrl(cancelUrl)>

  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@applicationSummary.applicationSummary accordionId=accordionId summarySections=summarySections/>
</@defaultPage>
