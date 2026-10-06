<#include '../layout/layout.ftl'>
<#import '../search/search.ftl' as search>
<#import '../macros/dataItems/filters.ftl' as dataItemFilter>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
  <@fdsNotificationBanner.notificationBannerInfo fullWidth=true bannerTitleText="Licence management service - Contacts database">
    <p class="govuk-body">
      The NSTA has started developing a more extensive and integrated replacement for PEARS which will be the conduit
      for the management of all licences associated with petroleum, carbon capture, and gas storage activities.
      The first stage is this launch of the LMS Contacts Database, which you are now required to populate.
    </p>
  </@fdsNotificationBanner.notificationBannerInfo>
  <#if canStartApplication>
      <@fdsAction.link linkText="Start application" linkUrl=springUrl(startApplicationUrl) linkClass="govuk-button"/>
  </#if>
  <@search.standardSearch
  clearFilterUrl=clearFilterUrl
  searchResults=workAreaItems
  hasSearchBeenInvoked=workAreaItems?has_content
  isFilterPrimaryButton=false
  >
    <@dataItemFilter.referenceFilter
    form=form
    />
    <@dataItemFilter.licenceTypeFilter
    form=form
    licenceTypes=licenceTypes
    />
    <@dataItemFilter.licenseeOrgUnitFilter
    form=form
    licenseeOrgUnitUrl=licenseeOrgUnitUrl
    preSelectedLicenseeOrgUnit=preSelectedLicenseeOrgUnit
    />
    <#if isRegulatorUser>
        <@dataItemFilter.licenseeGroupFilter
        form=form
        licenseeGroupOrgUnitUrl=licenseeGroupOrgUnitUrl
        preSelectedLicenseeGroup=preSelectedLicenseeGroupOrgUnit
        />
    </#if>
    <@dataItemFilter.applicationReferenceFilter
    form=form
    />
    <@dataItemFilter.applicationTypeFilter
    form=form
    applicationTypes=applicationTypes
    />
    <@dataItemFilter.applicationStatusFilter
    form=form
    applicationStatuses=applicationStatuses
    />
  </@search.standardSearch>
</@defaultPage>
