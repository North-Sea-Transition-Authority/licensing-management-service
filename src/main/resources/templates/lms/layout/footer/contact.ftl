<#include '../layout.ftl' >

<#-- @ftlvariable name="serviceBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.ServiceConfigurationProperties" -->
<#-- @ftlvariable name="customerBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties" -->

<#assign pageHeading = "Contact"/>

<@defaultPage
pageHeading=pageHeading
htmlTitle=pageHeading
pageSize=PageSize.FULL_COLUMN
>
  <div class="govuk-body">
    <h2 class="govuk-heading-m">Business support</h2>
    <p>For example, questions about filling in your application or the information you need to provide</p>
      <@fdsSummaryList.summaryListCard summaryListId="business-contact-details" headingText="${serviceBranding.mnemonic()} business support">
        [TODO XYZ business support contact]
      </@fdsSummaryList.summaryListCard>
  </div>
  <div class="govuk-body">
    <h2 class="govuk-heading-m">Technical support</h2>
    <p>For example, unexpected problems using the service or system errors being received</p>
      <@fdsSummaryList.summaryListCard summaryListId="technical-support-contact-details" headingText="${customerBranding.mnemonic()} service desk">
          <@fdsSummaryList.summaryListRowNoAction keyText="Telephone">
              ${serviceBranding.supportContact().phone()}
          </@fdsSummaryList.summaryListRowNoAction>
          <@fdsSummaryList.summaryListRowNoAction keyText="Email">
              <@fdsAction.link
              linkText=serviceBranding.supportContact().email()
              linkUrl="mailto:${serviceBranding.supportContact().email()}?subject=${serviceBranding.name()} - Support"
              />
          </@fdsSummaryList.summaryListRowNoAction>
      </@fdsSummaryList.summaryListCard>
  </div>
</@defaultPage>
