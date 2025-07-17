<#import "../../fds/components/header/energyPortalHeader.ftl" as fdsEnergyPortalHeader>
<#import '_pageSizes.ftl' as PageSize>


<#-- @ftlvariable name="serviceName" type="String" -->
<#-- @ftlvariable name="customerMnemonic" type="String" -->
<#-- @ftlvariable name="serviceHomeUrl" type="String" -->
<#-- @ftlvariable name="signOutUrl" type="String" -->
<#-- @ftlvariable name="signedInUser" type="String" -->
<#-- @ftlvariable name="signOutButtonText" type="String" -->

<#macro header
  signOutUrl
  signedInUserName=""
  extendContainerWidth=false
  >
  <@fdsEnergyPortalHeader.energyPortalHeader headerLogo="NSTA" userDisplayName=signedInUserName signOutUrl=signOutUrl wrapperWidth=extendContainerWidth/>
</#macro>
