<br>
<semui:filter>
    <g:form action="surveyParticipants" method="post" class="ui form"
            params="[id: surveyInfo.id, surveyConfigID: params.surveyConfigID, tab: params.tab]">
        <g:render template="/templates/filter/orgFilter"
                  model="[
                          tmplConfigShow      : [['name', 'libraryType'], ['federalState', 'libraryNetwork', 'property']],
                          tmplConfigFormFilter: true,
                          useNewLayouter      : true
                  ]"/>
    </g:form>
</semui:filter>

<g:form action="deleteSurveyParticipants" controller="survey" method="post" class="ui form"
        params="[id: surveyInfo.id, surveyConfigID: params.surveyConfigID, tab: params.tab]">

    <h3><g:message code="surveyParticipants.hasAccess"/></h3>

    <g:set var="surveyParticipantsHasAccess"
           value="${selectedParticipants?.findAll{ it?.hasAccessOrg() }?.sort{ it?.sortname }}"/>

    <div class="four wide column">
    <g:if test="${surveyParticipantsHasAccess}">
        <g:link data-orgIdList="${(surveyParticipantsHasAccess?.id)?.join(',')}"
                data-targetId="copyEmailaddresses_ajaxModal4"
                class="ui icon button right floated trigger-modal">
            <g:message code="survey.copyEmailaddresses.participantsHasAccess"/>
        </g:link>
    </g:if>
    </div>
    <br>
    <br>

    <g:render template="/templates/filter/orgFilterTable"
              model="[orgList         : surveyParticipantsHasAccess,
                      tmplShowCheckbox: editable,
                      tmplConfigShow  : ['lineNumber', 'sortname', 'name', 'libraryType']
              ]"/>


    <h3><g:message code="surveyParticipants.hasNotAccess"/></h3>

    <g:set var="surveyParticipantsHasNotAccess"
           value="${selectedParticipants.findAll{ !it?.hasAccessOrg() }.sort{ it?.sortname }}"/>

    <div class="four wide column">
    <g:if test="${surveyParticipantsHasNotAccess}">
        <g:link data-orgIdList="${(surveyParticipantsHasNotAccess?.id)?.join(',')}"
                data-targetId="copyEmailaddresses_ajaxModal5"
                class="ui icon button right floated trigger-modal">
            <g:message code="survey.copyEmailaddresses.participantsHasNoAccess"/>
        </g:link>
    </g:if>
    </div>

    <br>
    <br>

    <g:render template="/templates/filter/orgFilterTable"
              model="[orgList         : surveyParticipantsHasNotAccess,
                      tmplShowCheckbox: editable,
                      tmplConfigShow  : ['lineNumber', 'sortname', 'name', 'libraryType']
              ]"/>

    <br/>

    <g:if test="${selectedParticipants && editable}">
        <input type="submit" class="ui negative button"
               value="${message(code: 'default.button.delete.label')}"/>
    </g:if>

</g:form>