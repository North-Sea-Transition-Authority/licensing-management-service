<#include '../../fds/layout.ftl'>
<#import '_pageSizes.ftl' as PageSize>
<#import '_header.ftl' as pageHeader>
<#import '../macros/_multiLineText.ftl' as multiLineText>

<#-- @ftlvariable name="serviceBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.ServiceConfigurationProperties" -->
<#-- @ftlvariable name="customerBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties" -->
<#-- @ftlvariable name="serviceHomeUrl" type="String" -->
<#-- @ftlvariable name="loggedInUser" type="uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail" -->
<#-- @ftlvariable name="accessibilityStatementUrl" type="String" -->
<#-- @ftlvariable name="cookiePolicyUrl" type="String" -->
<#-- @ftlvariable name="privacyUrl" type="String" -->
<#-- @ftlvariable name="contactPageUrl" type="String" -->
<#-- @ftlvariable name="notificationBanner" type="uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner" -->

<#macro defaultPage
  htmlTitle
  pageHeading=htmlTitle
  phaseBanner=true
  pageSize=PageSize.TWO_THIRDS_COLUMN
  extendContainerWidth=false
  backLinkUrl=""
  backLinkEnabled=false
  breadcrumbs={}
  errorSummaryItems=[]
  showNavigationItems=true
>
  <#local serviceName = serviceBranding.name() />
  <#local customerMnemonic = customerBranding.mnemonic() />
  <#local serviceHomeUrl = springUrl(serviceHomeUrl) />

  <#assign fullPageWidth=false />
  <#assign fullWidthColumn=false />
  <#assign oneHalfColumn=false />
  <#assign oneThirdColumn=false />
  <#assign twoThirdsColumn=false />
  <#assign twoThirdsOneThirdColumn=false />
  <#assign oneQuarterColumn=false />

  <#if pageSize == PageSize.FULL_COLUMN>
    <#assign fullWidthColumn=true/>
  <#elseif pageSize == PageSize.ONE_HALF_COLUMN>
    <#assign oneHalfColumn=true/>
  <#elseif pageSize == PageSize.ONE_THIRD_COLUMN>
    <#assign oneThirdColumn=true/>
  <#elseif pageSize == PageSize.TWO_THIRDS_ONE_THIRD_COLUMN>
    <#assign twoThirdsOneThirdColumn=true/>
  <#elseif pageSize == PageSize.ONE_QUARTER_COLUMN>
    <#assign oneQuarterColumn=true/>
  <#else>
    <#assign twoThirdsColumn=true/>
  </#if>

  <#assign useBreadCrumbs=false>
  <#if breadcrumbs?has_content>
      <#assign useBreadCrumbs=true>
  </#if>

  <#assign backLink = false>
  <#if backLinkUrl?has_content || backLinkEnabled>
      <#assign backLink=true/>
  </#if>

  <#assign serviceHeader>
    <@pageHeader.header
      serviceName=serviceName
      customerMnemonic=customerMnemonic
      serviceHomeUrl=serviceHomeUrl
      signedInUserName=(loggedInUser?has_content)?then(loggedInUser.displayName(), "")
      signOutUrl=springUrl("/logout")
      pageSize=pageSize
      extendContainerWidth=extendContainerWidth
    />
  </#assign>

  <#assign footerContent>
    <#local footerMetaContent>
      <@fdsFooter.footerMeta footerMetaHiddenHeading="Support links">
        <@fdsFooter.footerMetaLink linkText="Accessibility statement" linkUrl=springUrl(accessibilityStatementUrl) />
        <@fdsFooter.footerMetaLink linkText="Contact" linkUrl=springUrl(contactPageUrl) />
        <@fdsFooter.footerMetaLink linkText="Privacy" linkUrl=privacyUrl openInNewTab=true/> <#-- Absolute url -->
        <@fdsFooter.footerMetaLink linkText="Cookies" linkUrl=springUrl(cookiePolicyUrl) />
      </@fdsFooter.footerMeta>
    </#local>
      <#if customerMnemonic="NSTA">
          <@fdsNstaFooter.nstaFooter metaLinks=true footerMetaContent=footerMetaContent wrapperWidth=extendContainerWidth/>
      <#else>
          <@fdsFooter.footer metaLinks=true footerMetaContent=footerMetaContent wrapperWidth=extendContainerWidth/>
      </#if>
  </#assign>

  <#assign notificationBannerContent>
    <@_notificationBannerContent />
  </#assign>

  <@fdsDefaultPageTemplate
    htmlTitle=htmlTitle
    serviceName=serviceName
    htmlAppTitle=serviceName
    headerContent=serviceHeader
    footerContent=footerContent
    pageHeading=pageHeading
    headerLogo="GOV_CREST"
    logoProductText=customerMnemonic
    phaseBanner=phaseBanner
    serviceUrl=serviceHomeUrl
    homePageUrl=serviceHomeUrl
    wrapperWidth=extendContainerWidth
    fullWidthColumn=fullWidthColumn
    oneHalfColumn=oneHalfColumn
    oneThirdColumn=oneThirdColumn
    twoThirdsColumn=twoThirdsColumn
    twoThirdsOneThirdColumn=twoThirdsOneThirdColumn
    oneQuarterColumn=oneQuarterColumn
    backLink=backLink
    backLinkUrl=backLinkUrl
    breadcrumbs=useBreadCrumbs
    breadcrumbsList=breadcrumbs
    cookieBannerMacro=_cookieBanner
    errorItems=errorSummaryItems
    notificationBannerContent=notificationBannerContent
    topNavigation=showNavigationItems
  >
    <#nested />
  </@fdsDefaultPageTemplate>
</#macro>

<#macro _cookieBanner>
  <@fdsCookieBanner.analyticsCookieBanner
    serviceName=serviceBranding.name()
    cookieSettingsUrl=springUrl(cookiePolicyUrl)
  />
</#macro>


<#macro _notificationBannerContent>
  <#if !notificationBanner?has_content>
    <#return/>
  </#if>

  <#local bannerContent>
    <#if notificationBanner.headingContent()?has_content>
      <#if notificationBanner.otherContent()?has_content>
        <@fdsNotificationBanner.notificationBannerContent
          headingText=notificationBanner.headingContent()
          moreContent=notificationBanner.otherContent()
        />
      <#else>
        <@fdsNotificationBanner.notificationBannerContent>
          ${notificationBanner.headingContent()}
        </@fdsNotificationBanner.notificationBannerContent>
      </#if>
    <#else>
      <p class="govuk-body">${notificationBanner.otherContent()}</p>
    </#if>
  </#local>

  <#local bannerTitleText=notificationBanner.title()/>

  <#switch notificationBanner.type()>
    <#case "INFO">
      <@fdsNotificationBanner.notificationBannerInfo bannerTitleText=bannerTitleText>
        ${bannerContent}
      </@fdsNotificationBanner.notificationBannerInfo>
      <#break>
    <#case "SUCCESS">
      <@fdsNotificationBanner.notificationBannerSuccess bannerTitleText=bannerTitleText>
        ${bannerContent}
      </@fdsNotificationBanner.notificationBannerSuccess>
      <#break>
  </#switch>
</#macro>
