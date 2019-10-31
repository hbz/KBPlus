<%@ page import="com.k_int.kbplus.ApiSource; com.k_int.kbplus.Platform" %>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <title>${message(code: 'laser', default: 'LAS:eR')} : <g:message code="platform.nav.platformTipps"/></title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="platform" action="index" message="platform.show.all"/>
    <semui:crumb class="active" id="${platformInstance.id}" text="${platformInstance.name}"/>
</semui:breadcrumbs>

<semui:modeSwitch controller="platform" action="show" params="${params}"/>

<h1 class="ui left aligned icon header"><semui:headerIcon/>

    <g:if test="${editable}"><span id="platformNameEdit"
                                   class="xEditableValue"
                                   data-type="textarea"
                                   data-pk="${platformInstance.class.name}:${platformInstance.id}"
                                   data-name="name"
                                   data-url='<g:createLink controller="ajax"
                                                           action="editableSetValue"/>'>${platformInstance.name}</span>
    </g:if>
    <g:else>${platformInstance.name}</g:else>
</h1>

<g:render template="nav"/>

<semui:messages data="${flash}"/>

<g:render template="/package/filter" model="${[params:params]}"/>


<h3 class="ui left aligned icon header">${message(code: 'platform.show.availability', default: 'Availability of titles in this platform by package')}
<semui:totalNumber total="${tipps.size()}"/>
</h3>

<g:render template="/templates/tipps/table"
          model="[tipps: tipps, showPackage: true, showPlattform: false, showBulkFlag: false]"/>

<g:if test="${tipps}" >
    <semui:paginate action="current" controller="package" params="${params}" next="${message(code:'default.paginate.next')}" prev="${message(code:'default.paginate.prev')}" maxsteps="${max}" total="${num_tipp_rows}" />
</g:if>

</body>
</html>