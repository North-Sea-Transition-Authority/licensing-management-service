<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  breadcrumbs=breadcrumbs
  pageSize=PageSize.FULL_WIDTH
  pageHeadingClass="govuk-heading-xl govuk-!-margin-bottom-2"
>
    <@fdsForm.htmlForm actionUrl=springUrl(approveUrl)>
      <@fdsAction.button buttonText="Approve and issue letter" buttonClass="govuk-button govuk-button--primary"/>
    </@fdsForm.htmlForm>

    <@fdsAction.link
      linkText="Preview document"
      linkUrl=springUrl("#")
      linkClass="govuk-button govuk-button--secondary"
      role=true
      openInNewTab=true
    />

    <@fdsAction.link
      linkText="Reload document"
      linkUrl=springUrl("#")
      linkClass="govuk-button govuk-button--secondary"
    />
</@defaultPage>
