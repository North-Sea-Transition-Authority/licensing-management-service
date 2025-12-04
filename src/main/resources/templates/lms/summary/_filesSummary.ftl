<#include '../layout/layout.ftl'>

<#-- @ftlvariable name="fileViews" type="java.util.List<uk.co.nstauthority.licensingmanagementservice.summary.SummaryFileView>" -->

<#macro summary heading fileViews>
  <@fdsSummaryList.summaryListCard
    headingText=heading
    headingSize="h3"
    summaryListId="files-summary-card-list">
    <#list fileViews as fileView>
        <#list fileView.uploadedFileViews() as uploadedFile>
      <@fdsSummaryList.summaryListRow
        keyText=uploadedFile.fileName()
        actionText="Download"
        actionUrl=springUrl(uploadedFile.downloadUrl())
        screenReaderActionText="Download ${uploadedFile.fileName()}">
        <p class="govuk-body">
          <@multiLineText.multiLineText contentText=uploadedFile.fileDescription()!/>
        </p>
      </@fdsSummaryList.summaryListRow>
    </#list>
    </#list>
  </@fdsSummaryList.summaryListCard>
</#macro>