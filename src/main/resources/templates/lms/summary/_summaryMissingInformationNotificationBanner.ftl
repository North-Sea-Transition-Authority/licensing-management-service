<#include '../layout/layout.ftl'>

<#macro summaryMissingInformationNotificationBanner>
  <@fdsNotificationBanner.notificationBannerInfo fullWidth=true bannerTitleText="Missing information">
    <@fdsNotificationBanner.notificationBannerContent headingText="Not all sections shown on the task list have been completed.">
      You will not be allowed to submit without completing all sections on the task list.
    </@fdsNotificationBanner.notificationBannerContent>
  </@fdsNotificationBanner.notificationBannerInfo>
</#macro>