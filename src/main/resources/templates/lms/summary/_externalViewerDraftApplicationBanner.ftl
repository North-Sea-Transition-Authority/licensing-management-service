<#include '../layout/layout.ftl'>

<#macro externalViewerDraftApplicationBanner hasIncompleteSections>
  <@fdsNotificationBanner.notificationBannerInfo fullWidth=true bannerTitleText="Draft application">
    <@fdsNotificationBanner.notificationBannerContent headingText="This is a draft application.">
      It has not been submitted to the NSTA<#if hasIncompleteSections> and some sections are incomplete</#if>.
    </@fdsNotificationBanner.notificationBannerContent>
  </@fdsNotificationBanner.notificationBannerInfo>
</#macro>