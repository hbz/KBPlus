<%@ page import="com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.SurveyProperty;com.k_int.kbplus.RefdataValue;" %>
<laser:serviceInjection/>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'subscriptionSurvey.label')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="dashboard" text="${institution?.getDesignation()}"/>
    <semui:crumb controller="myInstitution" action="currentSurveys" message="currentSurveys.label"/>
    <semui:crumb message="subscriptionSurvey.label" class="active"/>
</semui:breadcrumbs>


<semui:controlButtons>
    <semui:exportDropdown>
        <semui:exportDropdownItem>
            <g:link class="item" controller="myInstitution" action="surveyInfos"
                    params="${params + [exportXLS: true]}">${message(code: 'survey.exportSurvey')}</g:link>
        </semui:exportDropdownItem>
    </semui:exportDropdown>
</semui:controlButtons>



<h1 class="ui left aligned icon header"><semui:headerIcon/>
${message(code: 'subscriptionSurvey.label')} - ${surveyInfo.name}
<semui:surveyStatus object="${surveyInfo}"/>
%{--<g:if test="${surveyInfo.startDate || surveyInfo.endDate}">
(<g:formatDate formatName="default.date.format.notime"
               date="${surveyInfo.startDate}"/>
-
<g:formatDate formatName="default.date.format.notime"
              date="${surveyInfo.endDate}"/>)
</g:if>--}%
</h1>

<br>

<semui:messages data="${flash}"/>

<br>

<%--
<div class="ui icon info message">
    <i class="info icon"></i>

    <div class="content">
        <div class="header">${surveyInfo.status?.getI10n('value')}!</div>

        <p>
            <g:if test="${surveyInfo.status == com.k_int.kbplus.RefdataValue.getByValueAndCategory('Survey started', 'Survey Status')}">
                <g:message code="surveyInfo.status.surveyStarted"
                           args="[g.formatDate(formatName: 'default.date.format.notime', date: surveyInfo.startDate), (g.formatDate(formatName: 'default.date.format.notime', date: surveyInfo?.endDate) ?: '')]"/>
            </g:if>
            <g:if test="${surveyInfo.status == com.k_int.kbplus.RefdataValue.getByValueAndCategory('Survey completed', 'Survey Status')}">
                <g:message code="surveyInfo.status.surveyCompleted"
                           args="[g.formatDate(formatName: 'default.date.format.notime', date: surveyInfo.startDate)]"/>
            </g:if>
            <g:if test="${surveyInfo.status == com.k_int.kbplus.RefdataValue.getByValueAndCategory('In Evaluation', 'Survey Status')}">
                <g:message code="surveyInfo.status.inEvaluation"/>
            </g:if>
            <g:if test="${surveyInfo.status == com.k_int.kbplus.RefdataValue.getByValueAndCategory('Completed', 'Survey Status')}">
                <g:message code="surveyInfo.status.surveyCompleted"/>
            </g:if>
        </p>
    </div>
</div>
--%>

<g:if test="${!editable}">
    <div class="ui icon positive message">
        <i class="info icon"></i>

        <div class="content">
            <div class="header"></div>

            <p>
                <%-- <g:message code="surveyInfo.finishOrSurveyCompleted"/> --%>
                <g:message code="surveyResult.finish.info" />.
            </p>
        </div>
    </div>
</g:if>

<g:if test="${ownerId}">
    <g:set var="choosenOrg" value="${com.k_int.kbplus.Org.findById(ownerId)}"/>
    <g:set var="choosenOrgCPAs" value="${choosenOrg?.getGeneralContactPersons(false)}"/>

    <semui:form>
        <h3><g:message code="surveyInfo.owner.label"/>:</h3>

        <table class="ui table la-table la-table-small">
            <tbody>
            <tr>
                <td>
                    <p><strong>${choosenOrg?.name} (${choosenOrg?.shortname})</strong></p>

                    ${choosenOrg?.libraryType?.getI10n('value')}
                </td>
                <td>
                    <g:if test="${choosenOrgCPAs}">
                        <g:set var="oldEditable" value="${editable}"/>
                        <g:set var="editable" value="${false}" scope="request"/>
                        <g:each in="${choosenOrgCPAs}" var="gcp">
                            <g:render template="/templates/cpa/person_details"
                                      model="${[person: gcp, tmplHideLinkToAddressbook: true]}"/>
                        </g:each>
                        <g:set var="editable" value="${oldEditable ?: false}" scope="request"/>
                    </g:if>
                </td>
            </tr>
            </tbody>
        </table>
    </semui:form>
</g:if>

<br>

<h2 class="ui left aligned icon header">${message(code: 'surveyConfig.label')} <semui:totalNumber
        total="${surveyResults?.size()}"/></h2>
<br>

<semui:form>

    <h3 class="ui left aligned icon header">${message(code: 'subscription.plural')} <semui:totalNumber
            total="${com.k_int.kbplus.SurveyConfig.findAllByIdInListAndType(surveyResults.collect {
                it.key
            }, 'Subscription').size()}"/></h3>

    <table class="ui celled sortable table la-table">
        <thead>
        <tr>
            <th class="center aligned">
                ${message(code: 'sidewide.number')}
            </th>
            <th>${message(code: 'surveyProperty.subName')}</th>
            <th>${message(code: 'surveyProperty.subProviderAgency')}</th>
            <th>${message(code: 'surveyProperty.plural.label')}</th>
            <th>${message(code: 'surveyResult.finish')}</th>
            <th></th>

        </tr>

        </thead>

        <g:each in="${surveyResults}" var="config" status="i">

            <g:set var="surveyConfig" value="${com.k_int.kbplus.SurveyConfig.get(config.key)}"/>

            <g:if test="${surveyConfig?.type == 'Subscription'}">

                <g:set var="participantSubscription"
                       value="${surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(institution)}"/>
                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <td>
                        <g:if test="${participantSubscription}">
                            <i class="icon clipboard outline la-list-icon"></i>
                            <g:link action="surveyConfigsInfo" id="${surveyInfo.id}" params="[surveyConfigID: surveyConfig?.id]">
                                ${participantSubscription?.name}
                            </g:link>
                        </g:if>
                        <g:else>
                            <i class="icon clipboard outline la-list-icon"></i>
                            <g:link controller="public" action="gasco" params="[q: surveyConfig?.subscription?.name]">
                                ${surveyConfig?.subscription?.name}
                            </g:link>
                        </g:else>
                    </td>
                    <td>
                        <g:if test="${participantSubscription}">
                            <g:each in="${surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(institution)?.providers}"
                                    var="org">
                                <g:link controller="organisation" action="show" id="${org.id}">${org.name}</g:link><br/>
                            </g:each>
                            <g:each in="${surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(institution)?.agencies}"
                                    var="org">
                                <g:link controller="organisation" action="show"
                                        id="${org.id}">${org.name} (${message(code: 'default.agency.label', default: 'Agency')})</g:link><br/>
                            </g:each>
                        </g:if>
                        <g:else>
                            <g:each in="${surveyConfig?.subscription?.providers}"
                                    var="org">
                                <g:link controller="organisation" action="show" id="${org.id}">${org.name}</g:link><br/>
                            </g:each>
                            <g:each in="${surveyConfig?.subscription?.agencies}"
                                    var="org">
                                <g:link controller="organisation" action="show"
                                        id="${org.id}">${org.name} (${message(code: 'default.agency.label', default: 'Agency')})</g:link><br/>
                            </g:each>
                        </g:else>

                    </td>
                    <td class="center aligned">
                        <g:if test="${surveyConfig?.type == 'Subscription'}">
                            <g:link action="surveyConfigsInfo" id="${surveyInfo.id}"
                                    params="[surveyConfigID: surveyConfig?.id]" class="ui icon">
                                <div class="ui circular label">${surveyConfig?.surveyProperties?.size()}</div>
                            </g:link>
                        </g:if>
                    </td>

                    <td class="center aligned">
                        <g:set var="finish" value="${surveyConfig.checkResultsEditByOrg(institution)}"/>
                        <g:if test="${finish == com.k_int.kbplus.SurveyConfig.ALL_RESULTS_PROCESSED_BY_ORG}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${message(code: 'surveyConfig.allResultsFinishByOrg')}">
                                <i class="circle green icon"></i>
                            </span>
                        </g:if>
                        <g:elseif test="${finish == com.k_int.kbplus.SurveyConfig.ALL_RESULTS_HALF_PROCESSED_BY_ORG}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${message(code: 'surveyConfig.allResultsHalfFinishByOrg')}">
                                <i class="circle yellow icon"></i>
                            </span>
                        </g:elseif>
                        <g:else>
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${message(code: 'surveyConfig.allResultsNotFinishByOrg')}">
                                <i class="circle red icon"></i>
                            </span>
                        </g:else>
                    </td>

                    <td>
                        <span  class="la-popup-tooltip la-delay" data-content="${message(code: 'surveyConfig.editResult')}">
                            <g:link action="surveyConfigsInfo" id="${surveyInfo.id}" params="[surveyConfigID: surveyConfig?.id]" class="ui icon button">
                                <i class="pencil icon"></i>
                            </g:link>
                        </span>
                    </td>
                </tr>
            </g:if>
        </g:each>
    </table>
</semui:form>
<br>
<br>

<g:set var="surveyProperties" value="${com.k_int.kbplus.SurveyConfig.findAllByIdInListAndType(surveyResults.collect {
    it.key
}, 'SurveyProperty')}"/>

<g:if test="${surveyProperties.size() > 0}">

    <semui:form>
        <h3 class="ui left aligned icon header">${message(code: 'surveyConfigs.list.propertys')} <semui:totalNumber
                total="${surveyProperties.size()}"/></h3>
        <table class="ui celled sortable table la-table">
            <thead>
            <tr>
                <th class="center aligned">
                    ${message(code: 'sidewide.number')}
                </th>
                <th>${message(code: 'surveyProperty.name')}</th>
                <th>${message(code: 'surveyProperty.type.label')}</th>
                <th>${message(code: 'surveyResult.result')}</th>
                <th>${message(code: 'surveyResult.commentParticipant')}</th>

            </tr>

            </thead>

            <g:each in="${surveyResults}" var="config" status="j">

                <g:set var="surveyConfig" value="${com.k_int.kbplus.SurveyConfig.get(config?.key)}"/>

                <g:if test="${surveyConfig?.type == 'SurveyProperty'}">
                    <tr>
                        <td class="center aligned">
                            ${j + 1}
                        </td>
                        <td>
                            <g:if test="${surveyConfig?.type == 'SurveyProperty'}">
                                ${surveyConfig?.surveyProperty?.getI10n('name')}

                                <g:if test="${surveyConfig?.surveyProperty?.getI10n('explain')}">
                                    <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                          data-content="${surveyConfig?.surveyProperty?.getI10n('explain')}">
                                        <i class="question circle icon"></i>
                                    </span>
                                </g:if>

                            </g:if>

                        </td>
                        <td>
                            ${com.k_int.kbplus.SurveyConfig.getLocalizedValue(surveyConfig?.type)}

                            <g:if test="${surveyConfig?.surveyProperty}">
                                <br>
                                <b>${message(code: 'surveyProperty.type.label')}: ${surveyConfig?.surveyProperty?.getLocalizedType()}

                            </g:if>

                        </td>
                        <td>
                            <g:if test="${config.value[0]?.type?.type == Integer.toString()}">
                                <semui:xEditable owner="${config.value[0]}" type="text" field="intValue"/>
                            </g:if>
                            <g:elseif test="${config.value[0]?.type?.type == String.toString()}">
                                <semui:xEditable owner="${config.value[0]}" type="text" field="stringValue"/>
                            </g:elseif>
                            <g:elseif test="${config.value[0]?.type?.type == BigDecimal.toString()}">
                                <semui:xEditable owner="${config.value[0]}" type="text" field="decValue"/>
                            </g:elseif>
                            <g:elseif test="${config.value[0]?.type?.type == Date.toString()}">
                                <semui:xEditable owner="${config.value[0]}" type="date" field="dateValue"/>
                            </g:elseif>
                            <g:elseif test="${config.value[0]?.type?.type == URL.toString()}">
                                <semui:xEditable owner="${config.value[0]}" type="url" field="urlValue"
                                                 overwriteEditable="${overwriteEditable}"
                                                 class="la-overflow la-ellipsis"/>
                                <g:if test="${config.value[0]?.urlValue}">
                                    <semui:linkIcon/>
                                </g:if>
                            </g:elseif>
                            <g:elseif test="${config.value[0]?.type?.type == RefdataValue.toString()}">
                                <semui:xEditableRefData owner="${config.value[0]}" type="text" field="refValue"
                                                        config="${config.value[0].type?.refdataCategory}"/>
                            </g:elseif>
                        </td>
                        <td>
                            ${config.value[0]?.comment}
                        </td>
                    </tr>
                </g:if>
            </g:each>
        </table>
    </semui:form>

</g:if>
<g:if test="${editable}">
    <g:link class="ui button green js-open-confirm-modal"
            data-confirm-tokenMsg="${message(code: "confirm.dialog.concludeBinding.survey")}"
            data-confirm-term-how="concludeBinding"
            controller="myInstitution"
            action="surveyInfoFinish"
            id="${surveyInfo.id}"
            params="[subscriptionSurvey: true]">
        <g:message code="surveyResult.finish.info2"/>
    </g:link>
</g:if>
<br>
<br>
</body>
</html>
