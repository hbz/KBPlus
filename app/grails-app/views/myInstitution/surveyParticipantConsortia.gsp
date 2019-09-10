<%@ page import="de.laser.helper.RDStore; com.k_int.kbplus.SurveyProperty;com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.RefdataValue;" %>
<laser:serviceInjection/>
<!doctype html>

<r:require module="annotations"/>

<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'surveyEvaluation.label', default: 'Survey Evaluation')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="survey" action="currentSurveysConsortia" text="${message(code: 'menu.my.surveys')}"/>

    <g:if test="${surveyInfo}">
        <semui:crumb controller="survey" action="show" id="${surveyInfo.id}" text="${surveyInfo.name}"/>
    </g:if>
    <semui:crumb message="surveyEvaluation.label" class="active"/>
</semui:breadcrumbs>


<br>

<h1 class="ui icon header"><semui:headerTitleIcon type="Survey"/>
${surveyInfo.name}
</h1>


<semui:messages data="${flash}"/>


<g:if test="${participant}">
    <g:set var="choosenOrg" value="${com.k_int.kbplus.Org.findById(participant.id)}" />
    <g:set var="choosenOrgCPAs" value="${choosenOrg?.getGeneralContactPersons(false)}" />

    <table class="ui table la-table la-table-small">
        <tbody>
        <tr>
            <td>
                <p><strong>${choosenOrg?.name} (${choosenOrg?.shortname})</strong></p>

                ${choosenOrg?.libraryType?.getI10n('value')}
            </td>
            <td>
                <g:if test="${choosenOrgCPAs}">
                    <g:set var="oldEditable" value="${editable}" />
                    <g:set var="editable" value="${false}" scope="request"/>
                    <g:each in="${choosenOrgCPAs}" var="gcp">
                        <g:render template="/templates/cpa/person_details" model="${[person: gcp, tmplHideLinkToAddressbook: true]}" />
                    </g:each>
                    <g:set var="editable" value="${oldEditable ?: false}" scope="request"/>
                </g:if>
            </td>
        </tr>
        </tbody>
    </table>
</g:if>

<semui:form>

    <g:each in="${surveyResult}" var="config" >

        <g:set var="surveyConfig" value="${com.k_int.kbplus.SurveyConfig.get(config.key)}"/>

        <h4 class="ui left aligned icon header">${surveyConfig.getConfigName()} <semui:totalNumber
                total="${config?.value?.size()}"/></h4>

        <table class="ui celled sortable table la-table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'surveyProperty.label')}</th>
                <th>${message(code: 'surveyProperty.type.label')}</th>
                <th>${message(code: 'surveyResult.result')}</th>
                <th>${message(code: 'surveyResult.participantComment')}</th>
                <th>${message(code: 'surveyResult.commentOnlyForOwner')}</th>
            </tr>
            </thead>
            <g:each in="${config.value}" var="result" status="i">

                <g:set var="surveyResult" value="${com.k_int.kbplus.SurveyResult.get(result?.id)}"/>
                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <td>
                        ${surveyResult?.type?.getI10n('name')}

                        <g:if test="${surveyResult?.type?.getI10n('explain')}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center" data-content="${surveyResult?.type?.getI10n('explain')}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>

                    </td>
                    <td>
                        ${surveyResult?.type?.getLocalizedType()}
                    </td>


                    <g:set var="surveyOrg" value="${com.k_int.kbplus.SurveyOrg.findBySurveyConfigAndOrg(surveyResult?.surveyConfig, surveyResult?.participant)}"/>

                    <g:if test="${!surveyOrg?.existsMultiYearTerm()}">

                        <td>
                            <g:if test="${surveyResult?.type?.type == Integer.toString()}">
                                <semui:xEditable owner="${surveyResult}" type="text" field="intValue"/>
                            </g:if>
                            <g:elseif test="${surveyResult?.type?.type == String.toString()}">
                                <semui:xEditable owner="${surveyResult}" type="text" field="stringValue"/>
                            </g:elseif>
                            <g:elseif test="${surveyResult?.type?.type == BigDecimal.toString()}">
                                <semui:xEditable owner="${surveyResult}" type="text" field="decValue"/>
                            </g:elseif>
                            <g:elseif test="${surveyResult?.type?.type == Date.toString()}">
                                <semui:xEditable owner="${surveyResult}" type="date" field="dateValue"/>
                            </g:elseif>
                            <g:elseif test="${surveyResult?.type?.type == URL.toString()}">
                                <semui:xEditable owner="${surveyResult}" type="url" field="urlValue"
                                                 overwriteEditable="${overwriteEditable}"
                                                 class="la-overflow la-ellipsis"/>
                                <g:if test="${surveyResult.value}">
                                    <semui:linkIcon/>
                                </g:if>
                            </g:elseif>
                            <g:elseif test="${surveyResult?.type?.type == RefdataValue.toString()}">
                                <semui:xEditableRefData owner="${surveyResult}" type="text" field="refValue"
                                                        config="${surveyResult.type?.refdataCategory}"/>
                            </g:elseif>
                        </td>
                        <td>
                            ${surveyResult?.comment}
                        </td>
                    </g:if>
                    <g:else>
                        <td>
                            <g:message code="surveyOrg.perennialTerm.available"/>
                        </td>
                        <td>

                        </td>
                    </g:else>
                    <td>
                        <semui:xEditable owner="${surveyResult}" type="textarea" field="ownerComment"/>
                    </td>

                </tr>
            </g:each>
        </table>

    </g:each>

</semui:form>

</body>
</html>
