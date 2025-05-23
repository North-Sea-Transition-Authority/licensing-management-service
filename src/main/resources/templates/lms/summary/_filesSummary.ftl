<#include '../layout/layout.ftl'>

<#-- @ftlvariable name="fileViews" type="java.util.List<uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView>" -->

<#macro summary heading fileViews>
  <@fdsSummaryList.summaryListCard
    headingText=heading
    headingSize="h3"
    summaryListId="files-summary-card-list">
    <#list fileViews as fileView>
      <@fdsSummaryList.summaryListRow
        keyText=fileView.filename()
        actionText="Download"
        actionUrl=springUrl(fileView.downloadUrl())
        screenReaderActionText="Download ${fileView.filename()}">
        <p class="govuk-body">
          <@multiLineText.multiLineText contentText=fileView.description()/>
        </p>
      </@fdsSummaryList.summaryListRow>
    </#list>
  </@fdsSummaryList.summaryListCard>
</#macro>
