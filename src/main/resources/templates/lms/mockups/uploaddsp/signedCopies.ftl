<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <p class="govuk-body">A licensee is asked to post their hardcopies as soon as their own signed deed has been accepted.</p>

  <@fdsSummaryList.summaryList>
    <#list rows as row>
      <@fdsSummaryList.summaryListRowNoAction keyText=row.licensee()>
        ${row.status()}
        <#if row.actionUrl()??>
          <br/><a class="govuk-link" href="${springUrl(row.actionUrl())}">${row.actionText()}</a>
        </#if>
      </@fdsSummaryList.summaryListRowNoAction>
    </#list>
  </@fdsSummaryList.summaryList>

  <@fdsAction.link linkText="Back to the application" linkUrl=springUrl(backUrl)/>
</@defaultPage>
