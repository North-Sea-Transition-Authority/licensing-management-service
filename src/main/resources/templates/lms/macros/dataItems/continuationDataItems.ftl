<#include '../../layout/layout.ftl'>

<#macro continuationDataItem currentTermPhaseDisplay="" nextTermPhaseDisplay="">
    <@fdsDataItems.dataItem>

        <#if currentTermPhaseDisplay?has_content>
            <@fdsDataItems.dataValues key="Current Term/Phase" value=currentTermPhaseDisplay/>
        </#if>

        <#if nextTermPhaseDisplay?has_content>
            <@fdsDataItems.dataValues key="Next Term/Phase" value=nextTermPhaseDisplay/>
        </#if>

    </@fdsDataItems.dataItem>
</#macro>