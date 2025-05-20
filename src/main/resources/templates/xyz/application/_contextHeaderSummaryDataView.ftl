<#include '../layout/layout.ftl'>
<#import '../summary/summaryValue/_fileSummaryValue.ftl' as fileSummaryValue>

<#macro summaryDataView summaryDataView>
    <#list summaryDataView as dataItemRow>
        <@fdsResultList.resultListDataItem>
            <#list dataItemRow.keyValues() as keyValue>
                <#assign value>
                  <ol class="govuk-list">
                      <#if keyValue.summaryValueType() == "STRING_VALUE">
                          <#list keyValue.summaryValueData() as summaryValue>
                            <li class="govuk-body">
                                <@multiLineText.multiLineText contentText=summaryValue!""/>
                            </li>
                          </#list>
                      <#elseif keyValue.summaryValueType() == "FILE_VALUE">
                          <@fileSummaryValue.fileValueDisplay keyValue.summaryValueData()/>
                      </#if>
                  </ol>
                </#assign>
                <@fdsDataItems.dataValues key=keyValue.key() value=value!""/>
            </#list>
        </@fdsResultList.resultListDataItem>
    </#list>
</#macro>
