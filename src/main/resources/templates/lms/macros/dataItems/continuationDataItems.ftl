<#include '../../layout/layout.ftl'>

<#macro continuationDataItem currentTerm="" currentPhase="" nextTerm="" nextPhase="">
    <@fdsDataItems.dataItem>
        <#if currentTerm?has_content>
            <@fdsDataItems.dataValues key="Current term" value=currentTerm/>
        </#if>

        <#if currentPhase?has_content>
            <@fdsDataItems.dataValues key="Current phase" value=currentPhase/>
        </#if>

        <#if nextTerm?has_content>
            <@fdsDataItems.dataValues key="Next term" value=nextTerm/>
        </#if>

        <#if nextPhase?has_content>
            <@fdsDataItems.dataValues key="Next phase" value=nextPhase/>
        </#if>
    </@fdsDataItems.dataItem>
</#macro>