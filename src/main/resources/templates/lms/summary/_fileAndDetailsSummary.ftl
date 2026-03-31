<#include '../layout/layout.ftl'>
<#import 'summaryValue/_fileSummaryValue.ftl' as fileSummaryValue>
<#import 'summaryValue/_stringSummaryValue.ftl' as stringSummaryValue>

<#macro filesAndDetailsSummary mixedView heading>
    <@fdsSummaryList.summaryListCard
    headingText=heading
    headingSize="h3"
    summaryListId="mixed-summary-card-list">

        <#list mixedView.summaryData().keyValues() as keyValue>
            <@fdsSummaryList.summaryListRowNoAction keyText=keyValue.key()>
                <#if keyValue.summaryValueType() == "STRING_VALUE">
                    <@stringSummaryValue.stringValueDisplay keyValue.summaryValueData()/>
                <#elseif keyValue.summaryValueType() == "FILE_VALUE">
                    <@fileSummaryValue.fileValueDisplay keyValue.summaryValueData()/>
                </#if>
            </@fdsSummaryList.summaryListRowNoAction>
        </#list>

        <#list mixedView.fileViews() as fileView>
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