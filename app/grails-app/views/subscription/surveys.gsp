<%@ page import="com.k_int.kbplus.CostItem; com.k_int.kbplus.Person; de.laser.helper.RDStore; de.laser.interfaces.TemplateSupport" %>
<laser:serviceInjection />

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'subscription.details.surveys.label')}</title>
</head>
<body>

    <g:render template="breadcrumb" model="${[ params:params ]}"/>

    <semui:controlButtons>
        <g:render template="actions" />
    </semui:controlButtons>

    <h1 class="ui icon header"><semui:headerIcon />
        <semui:xEditable owner="${subscriptionInstance}" field="name" />
        <semui:totalNumber total="${surveys.size() ?: 0}"/>
    </h1>
    <semui:anualRings object="${subscriptionInstance}" controller="subscription" action="surveys" navNext="${navNextSubscription}" navPrev="${navPrevSubscription}"/>


    <g:render template="nav" />


    <semui:messages data="${flash}" />

    <g:if test="${surveys}">
        <table class="ui celled sortable table la-table">
            <thead>
            <tr>

                <th rowspan="2" class="center aligned">
                    ${message(code: 'sidewide.number')}
                </th>

                <g:sortableColumn params="${params}" property="surInfo.name"
                                  title="${message(code: 'surveyInfo.slash.name')}" rowspan="2" scope="col"/>

                <g:sortableColumn params="${params}" property="surInfo.startDate"
                                  title="${message(code: 'default.startDate.label', default: 'Start Date')}"/>
                <g:sortableColumn params="${params}" property="surInfo.endDate"
                                  title="${message(code: 'default.endDate.label', default: 'End Date')}"/>

                <th>${message(code: 'surveyInfo.finished')}</th>

            </tr>

            </thead>
            <g:each in="${surveys}" var="surveyConfig" status="i">

                <g:set var="surveyInfo"
                       value="${surveyConfig?.surveyInfo}"/>


                <g:set var="participantsFinish"
                       value="${com.k_int.kbplus.SurveyResult.findAllBySurveyConfigAndFinishDateIsNotNull(surveyConfig)}"/>

                <g:set var="participantsTotal"
                       value="${com.k_int.kbplus.SurveyResult.findAllBySurveyConfig(surveyConfig)}"/>

                <tr>
                    <td class="center aligned">
                        ${(params.int('offset') ?: 0) + i + 1}
                    </td>
                    <td><g:if test="${editable}">
                        <g:if test="${surveyConfig?.type == 'Subscription'}">
                            <i class="icon clipboard outline la-list-icon"></i>
                            <g:link controller="survey" action="surveyConfigsInfo" id="${surveyInfo?.id}"
                                    params="[surveyConfigID: surveyConfig?.id]" class="ui ">
                                ${surveyConfig?.subscription?.name}
                            </g:link>
                        </g:if>
                        <g:else>
                            <g:link controller="survey" action="show" id="${surveyInfo?.id}" class="ui ">
                                ${surveyConfig?.getConfigNameShort()}
                            </g:link>
                        </g:else>
                    </g:if>
                        <g:else>
                            <g:if test="${surveyConfig?.type == 'Subscription'}">
                                <i class="icon clipboard outline la-list-icon"></i>
                                ${surveyConfig?.subscription?.name}
                            </g:if>
                            <g:else>
                                <i class="icon chart bar la-list-icon"></i>
                                ${surveyConfig?.getConfigNameShort()}
                            </g:else>
                        </g:else>
                        <div class="la-flexbox">
                            <g:if test="${surveyConfig?.isSubscriptionSurveyFix}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${message(code: "surveyConfig.isSubscriptionSurveyFix.label.info2")}">
                                    <i class="yellow icon envelope large "></i>
                                </span>
                            </g:if>

                            <i class="icon chart bar la-list-icon"></i>
                            <g:link controller="survey" action="show" id="${surveyInfo?.id}" class="ui ">
                                ${surveyInfo?.name}
                            </g:link>
                        </div>
                    </td>
                    <td>
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${surveyInfo?.startDate}"/>

                    </td>
                    <td>

                        <g:formatDate formatName="default.date.format.notime"
                                      date="${surveyInfo?.endDate}"/>
                    </td>

                    <td class="center aligned">
                        <g:if test="${surveyConfig}">
                            <g:link controller="survey" action="evaluationConfigsInfo" id="${surveyInfo?.id}"
                                    params="[surveyConfigID: surveyConfig?.id]"
                                    class="ui icon">
                                <div class="ui circular ${(participantsFinish.size() == participantsTotal.size()) ? "green" : (participantsFinish.size() > 0) ? "yellow" :""} label">
                                    <g:if
                                            test="${participantsFinish && participantsTotal}">
                                        <g:formatNumber number="${(participantsFinish.size() / participantsTotal.size()) * 100}" minFractionDigits="2"
                                                        maxFractionDigits="2"/>%
                                    </g:if>
                                    <g:else>
                                        0%
                                    </g:else>
                                </div>
                            </g:link>
                        </g:if>
                    </td>
                </tr>

            </g:each>
        </table>
    </g:if>
        </body>
        </html>

