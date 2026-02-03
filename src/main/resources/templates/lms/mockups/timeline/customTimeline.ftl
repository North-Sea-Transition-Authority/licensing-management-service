<#macro timelineTimeStampSmall
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span class="fds-timeline__node-number" style="width : 18px; height : 18px; left: -15px; top: 0px">${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>

<#macro timelineTimeStampComplete
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li style="border-color: #00703c" class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span class="fds-timeline__node-number" style="border-color: #00703c; background-color: #00703c">${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>

<#macro timelineTimeStampCompleteNoBorder
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span class="fds-timeline__node-number" style="border-color: #00703c; background-color: #00703c">${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>

<#macro timelineTimeStampSmallComplete
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li style="border-color: #00703c" class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span class="fds-timeline__node-number" style="width : 18px; height : 18px; left: -15px; top: 0px; border-color: #00703c; background-color: #00703c">${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>

<#macro timelineTimeStampSmallCompleteNoBorder
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span class="fds-timeline__node-number" style="width : 18px; height : 18px; left: -15px; top: 0px; border-color: #00703c; background-color: #00703c">${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>

<#macro timelineTimeStampNoDot
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span>${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>

<#macro timelineTimeStampNoDotComplete
timeStampHeading
timeStampHeadingSize="h3"
timeStampClass=""
timeStampHeadingHint=""
nodeNumber=""
timelineActionContent="">

<li style="border-color: #00703c" class="fds-timeline__time-stamp <#if nodeNumber?has_content> fds-timeline__time-stamp--${nodeNumber}</#if> ${timeStampClass}">
    <${timeStampHeadingSize} class="govuk-heading-m">${timeStampHeading}</${timeStampHeadingSize}>
    <span>${nodeNumber!}</span>
    <#if timeStampHeadingHint?has_content>
        <div class="govuk-hint">${timeStampHeadingHint}</div>
    </#if>
    <div class="fds-timeline__events">
        <#nested>
    </div>
    <#if timelineActionContent?has_content>
        <div class="fds-timeline__actions">
            ${timelineActionContent}
        </div>
    </#if>
    </li>
</#macro>