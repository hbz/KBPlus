<%@ page import="de.laser.helper.RDStore" %>
<laser:serviceInjection />

<%
    String title
    if(comboType.id == RDStore.COMBO_TYPE_CONSORTIUM.id) {
        title = message(code: 'menu.institutions.manage_consortia')
    }
    else if(comboType.id == RDStore.COMBO_TYPE_DEPARTMENT.id) {
        title = message(code: 'menu.institutions.manage_departments')
    }
%>
<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <g:set var="entityName" value="${message(code: 'org.label', default: 'Org')}"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${title}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb message="${title}" class="active"/>
</semui:breadcrumbs>

<semui:controlButtons>
    <semui:exportDropdown>
        <g:if test="${filterSet}">
            <semui:exportDropdownItem>
                <g:link class="item js-open-confirm-modal"
                        data-confirm-tokenMsg = "${message(code: 'confirmation.content.exportPartial')}"
                        data-confirm-term-how="ok" controller="myInstitution" action="manageMembers"
                        params="${params+[exportXLS:true]}">
                    ${message(code:'default.button.exports.xls')}
                </g:link>
            </semui:exportDropdownItem>
            <semui:exportDropdownItem>
                <g:link class="item js-open-confirm-modal"
                        data-confirm-tokenMsg = "${message(code: 'confirmation.content.exportPartial')}"
                        data-confirm-term-how="ok" controller="myInstitution" action="manageMembers"
                        params="${params+[format:'csv']}">
                    ${message(code:'default.button.exports.csv')}
                </g:link>
            </semui:exportDropdownItem>
        </g:if>
        <g:else>
            <semui:exportDropdownItem>
                <g:link class="item" action="manageMembers" params="${params+[exportXLS:true]}">${message(code:'default.button.exports.xls')}</g:link>
            </semui:exportDropdownItem>
            <semui:exportDropdownItem>
                <g:link class="item" action="manageMembers" params="${params+[format:'csv']}">${message(code:'default.button.exports.csv')}</g:link>
            </semui:exportDropdownItem>
        </g:else>
    </semui:exportDropdown>
    <%
        editable = (editable && accessService.checkPerm('ORG_INST_COLLECTIVE,ORG_CONSORTIUM')) || contextService.getUser()?.hasRole('ROLE_ADMIN,ROLE_ORG_EDITOR')
    %>
    <g:if test="${editable}">
        <g:render template="actions"/>
    </g:if>
</semui:controlButtons>

<h1 class="ui left aligned icon header"><semui:headerIcon />${title}
<semui:totalNumber total="${membersCount}"/>
</h1>

<semui:messages data="${flash}"/>
    <%
        List configShowFilter = []
        List configShowTable = []
        if(comboType.id == RDStore.COMBO_TYPE_CONSORTIUM.id) {
            configShowFilter = [['name', 'identifier', 'libraryType'], ['federalState', 'libraryNetwork','property']]
            configShowTable = ['sortname', 'name', 'mainContact', 'legalInformation', 'numberOfSubscriptions', 'numberOfSurveys', 'libraryType', 'hasInstAdmin']
        }
        else if(comboType.id == RDStore.COMBO_TYPE_DEPARTMENT.id) {
            configShowFilter = [['name', 'identifier'], ['property']]
            configShowTable = ['name', 'mainContact', 'legalInformation', 'numberOfSubscriptions']
        }
    %>
    <semui:filter>
        <g:form action="manageMembers" method="get" class="ui form">
            <g:render template="/templates/filter/orgFilter"
                      model="[
                              tmplConfigShow: configShowFilter,
                              tmplConfigFormFilter: true,
                              useNewLayouter: true
                      ]"/>
        </g:form>
    </semui:filter>

<g:if test="${members}">
<g:form action="manageMembers" controller="myInstitution" method="post" class="ui form">
        <g:render template="/templates/filter/orgFilterTable"
                  model="[orgList: members,
                          tmplShowCheckbox: editable,
                          comboType: comboType,
                          tmplConfigShow: configShowTable
                  ]"/>


        <g:if test="${members && editable}">
            <input type="submit" class="ui button"
                   data-confirm-tokenMsg="${message(code: "confirm.dialog.delete.function", args: [message(code:'members.confirmDelete')])}"
                   data-confirm-term-how="delete" value="${message(code: 'default.button.revoke.label')}"/>
        </g:if>
    </g:form>
</g:if>
<g:else>
    <g:if test="${filterSet}">
        <br><strong><g:message code="filter.result.empty.object" args="${[message(code:"myinst.consortiaSubscriptions.consortia")]}"/></strong>
    </g:if>
    <g:else>
        <br><strong><g:message code="result.empty.object" args="${[message(code:"myinst.consortiaSubscriptions.consortia")]}"/></strong>
    </g:else>
</g:else>

    <g:render template="../templates/copyEmailaddresses" model="[orgList: totalMembers]"/>
    <semui:paginate action="manageMembers" controller="myInstitution" params="${params}" next="${message(code:'default.paginate.next', default:'Next')}" prev="${message(code:'default.paginate.prev', default:'Prev')}" max="${max}" total="${membersCount}" />

</body>
</html>
