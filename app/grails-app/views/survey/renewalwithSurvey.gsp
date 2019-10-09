<%@ page import="de.laser.helper.RDStore; com.k_int.kbplus.SurveyProperty;com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.RefdataValue;com.k_int.kbplus.Org;com.k_int.kbplus.SurveyOrg" %>
<laser:serviceInjection/>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'surveyInfo.renewal')}</title>

</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="survey" action="currentSurveysConsortia" text="${message(code: 'menu.my.surveys')}"/>

    <g:if test="${surveyInfo}">
        <semui:crumb controller="survey" action="show" id="${surveyInfo.id}" text="${surveyInfo.name}"/>
    </g:if>
    <semui:crumb message="surveyInfo.renewal" class="active"/>
</semui:breadcrumbs>


<semui:controlButtons>
    <semui:exportDropdown>
        <semui:exportDropdownItem>
            <g:link class="item" action="renewalwithSurvey" id="${surveyInfo.id}"
                    params="[surveyConfigID: surveyConfig.id, exportXLS: true]">${message(code: 'renewalwithSurvey.exportRenewal')}</g:link>
        </semui:exportDropdownItem>
    </semui:exportDropdown>
</semui:controlButtons>

<h1 class="ui icon header"><semui:headerTitleIcon type="Survey"/>
${surveyInfo?.name}
<semui:surveyStatus object="${surveyInfo}"/>
</h1>

<semui:objectStatus object="${surveyInfo}" status="${surveyInfo.status}"/>

<semui:messages data="${flash}"/>

<h2>
    ${message(code: 'surveyInfo.renewal')}
</h2>


<semui:form>

    <g:form action="proccessRenewalwithSurvey" controller="survey" id="${surveyInfo?.id}"
            params="[surveyConfigID: surveyConfig?.id]"
            method="post" class="ui form newLicence">

        <h3>
        <g:message code="renewalwithSurvey.parentSubscription"/>:
        <g:if test="${parentSubscription}">
            <g:link controller="subscription" action="show"
                    id="${parentSubscription?.id}">${parentSubscription?.dropdownNamingConvention()}</g:link>
        </g:if>

        <br>
        <br>
        <g:message code="renewalwithSurvey.parentSuccessorSubscription"/>:
        <g:if test="${parentSuccessorSubscription}">
            <g:link controller="subscription" action="show"
                    id="${parentSuccessorSubscription?.id}">${parentSuccessorSubscription?.dropdownNamingConvention()}</g:link>

            <g:link controller="survey" action="copyElementsIntoRenewalSubscription" id="${parentSubscription?.id}"
                    params="[sourceSubscriptionId: parentSubscription?.id, targetSubscriptionId: parentSuccessorSubscription?.id, isRenewSub: true, isCopyAuditOn: true]"
                    class="ui button ">
                <g:message code="renewalwithSurvey.newSub.change"/>
            </g:link>

        </g:if>
        <g:else>
            <g:message code="renewalwithSurvey.noParentSuccessorSubscription"/>
            <g:link controller="survey" action="renewSubscriptionConsortiaWithSurvey" id="${surveyInfo?.id}"
                    params="[surveyConfig: surveyConfig?.id, parentSub: parentSubscription?.id]"
                    class="ui button ">
                <g:message code="renewalwithSurvey.newSub"/>
            </g:link>
        </g:else>
        </br>
        </h3>

        <br>

        <g:set var="consortiaSubscriptions" value="${com.k_int.kbplus.Subscription.findAllByInstanceOf(parentSubscription)?.size()}"/>
        <g:set var="surveyParticipants" value="${surveyConfig?.orgs?.size()}"/>
        <g:set var="totalOrgs" value="${(orgsContinuetoSubscription?.size()?:0)+(newOrgsContinuetoSubscription?.size()?:0)+(orgsWithMultiYearTermSub?.size()?:0)+(orgsLateCommers?.size()?:0)+(orgsWithTermination?.size()?:0)+(orgsWithoutResult?.size()?:0)+(orgsWithParticipationInParentSuccessor?.size()?:0)}"/>


        <h3 class="ui left aligned icon header">
            <g:link action="evaluationConfigsInfo" id="${surveyInfo?.id}" params="[surveyConfigID: surveyConfig?.id]" >${message(code: 'survey.label')} ${message(code: 'surveyParticipants.label')}</g:link>
            <semui:totalNumber total="${surveyParticipants}"/>
            <br>
            <g:link controller="subscription" action="members" id="${parentSubscription?.id}" >${message(code: 'renewalwithSurvey.orgsInSub')}</g:link>
            <semui:totalNumber class="${surveyParticipants != consortiaSubscriptions ? 'red': ''}" total="${consortiaSubscriptions}"/>
            <br>
            ${message(code: 'renewalwithSurvey.orgsTotalInRenewalProcess')}
            <semui:totalNumber class="${totalOrgs != consortiaSubscriptions ? 'red': ''}" total="${totalOrgs}"/>
        </h3>

        <br>
        <br>

        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.continuetoSubscription.label')} <semui:totalNumber
                total="${orgsContinuetoSubscription?.size()?:0}"/></h4>

        <table class="ui celled sortable table la-table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'org.name.label')}</th>

                <th>
                    ${participationProperty?.getI10n('name')}

                    <g:if test="${participationProperty?.getI10n('explain')}">
                        <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="bottom center"
                              data-content="${participationProperty?.getI10n('explain')}">
                            <i class="question circle icon"></i>
                        </span>
                    </g:if>
                </th>
                <g:if test="${multiYearTermTwoSurvey || multiYearTermThreeSurvey}">
                    <th>
                        <g:message code="renewalwithSurvey.period"/>
                    </th>
                </g:if>


                <g:each in="${properties}" var="surveyProperty">
                    <th>
                        ${surveyProperty?.getI10n('name')}

                        <g:if test="${surveyProperty?.getI10n('explain')}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${surveyProperty?.getI10n('explain')}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </th>
                </g:each>
                <th>${message(code: 'renewalwithSurvey.costItem.label')}</th>
                <th>${message(code: 'default.actions')}</th>
            </tr>
            </thead>
            <g:each in="${orgsContinuetoSubscription}" var="participantResult" status="i">

                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <td>
                        <g:link controller="myInstitution" action="manageParticipantSurveys" id="${participantResult?.participant.id}">
                            ${participantResult?.participant?.sortname}
                        </g:link>
                        <br>
                        <g:link controller="organisation" action="show"
                                id="${participantResult?.participant.id}">(${fieldValue(bean: participantResult?.participant, field: "name")})</g:link>

                        <div class="ui grid">
                            <div class="right aligned wide column">
                                <g:if test="${!surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(participantResult?.participant)}" >
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.newOrg')}">
                                        <i class="star black large  icon"></i>
                                    </span>
                                </g:if>

                                <g:if test="${surveyConfig?.checkResultsEditByOrg(participantResult?.participant) == com.k_int.kbplus.SurveyConfig.ALL_RESULTS_PROCESSED_BY_ORG}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.processedOrg')}">
                                        <i class="edit green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notprocessedOrg')}">
                                        <i class="edit red icon"></i>
                                    </span>
                                </g:else>

                                <g:if test="${surveyConfig?.isResultsSetFinishByOrg(participantResult?.participant)}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.finishOrg')}">
                                        <i class="check green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notfinishOrg')}">
                                        <i class="x red icon"></i>
                                    </span>
                                </g:else>
                            </div>
                        </div>
                    </td>
                    <td>
                        ${participantResult?.resultOfParticipation?.getResult()}

                        <g:if test="${participantResult?.resultOfParticipation?.comment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.comment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>

                        <g:if test="${participantResult?.resultOfParticipation?.ownerComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.ownerComment}">
                                <i class="info circle icon"></i>
                            </span>
                        </g:if>

                    </td>

                    <g:if test="${multiYearTermTwoSurvey || multiYearTermThreeSurvey}">
                        <td>
                    </g:if>

                    <g:if test="${multiYearTermTwoSurvey}">
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodTwoStartDate}"/>
                        <br>
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodTwoEndDate}"/>

                        <g:if test="${participantResult?.participantPropertyTwoComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.participantPropertyTwoComment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>

                    </g:if>
                    <g:if test="${multiYearTermThreeSurvey}">
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodThreeStartDate}"/>
                        <br>
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodThreeEndDate}"/>

                        <g:if test="${participantResult?.participantPropertyThreeComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.participantPropertyThreeComment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </g:if>

                    <g:if test="${multiYearTermTwoSurvey || multiYearTermThreeSurvey}">
                        </td>
                    </g:if>

                    <g:each in="${participantResult?.properties.sort { it?.type?.name }}"
                            var="participantResultProperty">
                        <td>
                            ${participantResultProperty?.getResult()}

                            <g:if test="${participantResultProperty?.comment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.comment}">
                                    <i class="question circle icon"></i>
                                </span>
                            </g:if>

                            <g:if test="${participantResultProperty?.ownerComment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.ownerComment}">
                                    <i class="info circle icon"></i>
                                </span>
                            </g:if>

                        </td>
                    </g:each>

                    <td>

                        <g:set var="costItem" value="${participantResult?.resultOfParticipation?.getCostItem()}"/>

                        <g:if test="${costItem}">
                            <b><g:formatNumber number="${costItem?.costInBillingCurrencyAfterTax}" minFractionDigits="2"
                                               maxFractionDigits="2" type="number"/></b>

                            (<g:formatNumber number="${costItem?.costInBillingCurrency}" minFractionDigits="2"
                                             maxFractionDigits="2" type="number"/>)

                            ${(costItem?.billingCurrency?.getI10n('value')?.split('-'))?.first()}
                        </g:if>
                    </td>
                    <td>

                        <g:link controller="myInstitution" action="surveyParticipantConsortiaNew" id="${participantResult?.participant?.id}"
                                params="[surveyConfig: surveyConfig?.id]" class="ui button icon"><i class="icon chart pie"></i></g:link>

                        <g:if test="${participantResult?.sub}">
                            <br>
                            <g:link controller="subscription" action="show" id="${participantResult?.sub?.id}"
                                    class="ui button icon"><i class="icon clipboard"></i></g:link>
                        </g:if>
                    </td>

                </tr>
            </g:each>
        </table>


        <br>
        <br>

        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.newOrgstoSubscription.label')} <semui:totalNumber
                total="${newOrgsContinuetoSubscription?.size()?:0}"/></h4>

        <table class="ui celled sortable table la-table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'org.name.label')}</th>

                <th>
                    ${participationProperty?.getI10n('name')}

                    <g:if test="${participationProperty?.getI10n('explain')}">
                        <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="bottom center"
                              data-content="${participationProperty?.getI10n('explain')}">
                            <i class="question circle icon"></i>
                        </span>
                    </g:if>
                </th>

                <g:if test="${multiYearTermTwoSurvey || multiYearTermThreeSurvey}">
                    <th>
                        <g:message code="renewalwithSurvey.period"/>
                    </th>
                </g:if>

                <g:each in="${properties}" var="surveyProperty">
                    <th>
                        ${surveyProperty?.getI10n('name')}

                        <g:if test="${surveyProperty?.getI10n('explain')}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="bottom center"
                                  data-content="${surveyProperty?.getI10n('explain')}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </th>
                </g:each>
                <th>${message(code: 'renewalwithSurvey.costItem.label')}</th>
                <th>${message(code: 'default.actions')}</th>
            </tr>
            </thead>
            <g:each in="${newOrgsContinuetoSubscription}" var="participantResult" status="i">

                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <td>
                        <g:link controller="myInstitution" action="manageParticipantSurveys" id="${participantResult?.participant.id}">
                            ${participantResult?.participant?.sortname}
                        </g:link>
                        <br>
                        <g:link controller="organisation" action="show"
                                id="${participantResult?.participant.id}">(${fieldValue(bean: participantResult?.participant, field: "name")})</g:link>

                        <div class="ui grid">
                            <div class="right aligned wide column">
                                <g:if test="${!surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(participantResult?.participant)}" >
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.newOrg')}">
                                        <i class="star black large  icon"></i>
                                    </span>
                                </g:if>

                                <g:if test="${surveyConfig?.checkResultsEditByOrg(participantResult?.participant) == com.k_int.kbplus.SurveyConfig.ALL_RESULTS_PROCESSED_BY_ORG}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.processedOrg')}">
                                        <i class="edit green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notprocessedOrg')}">
                                        <i class="edit red icon"></i>
                                    </span>
                                </g:else>

                                <g:if test="${surveyConfig?.isResultsSetFinishByOrg(participantResult?.participant)}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.finishOrg')}">
                                        <i class="check green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notfinishOrg')}">
                                        <i class="x red icon"></i>
                                    </span>
                                </g:else>
                            </div>
                        </div>
                    </td>

                    <td>
                        ${participantResult?.resultOfParticipation?.getResult()}

                        <g:if test="${participantResult?.resultOfParticipation?.comment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.comment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>

                        <g:if test="${participantResult?.resultOfParticipation?.ownerComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.ownerComment}">
                                <i class="info circle icon"></i>
                            </span>
                        </g:if>

                    </td>

                    <g:if test="${multiYearTermTwoSurvey || multiYearTermThreeSurvey}">
                        <td>
                    </g:if>

                    <g:if test="${multiYearTermTwoSurvey}">
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodTwoStartDate}"/>
                        <br>
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodTwoEndDate}"/>

                        <g:if test="${participantResult?.participantPropertyTwoComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.participantPropertyTwoComment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </g:if>
                    <g:if test="${multiYearTermThreeSurvey}">
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodThreeStartDate}"/>
                        <br>
                        <g:formatDate formatName="default.date.format.notime"
                                      date="${participantResult?.newSubPeriodThreeEndDate}"/>

                        <g:if test="${participantResult?.participantPropertyThreeComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.participantPropertyThreeComment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </g:if>

                    <g:if test="${multiYearTermTwoSurvey || multiYearTermThreeSurvey}">
                        </td>
                    </g:if>



                    <g:each in="${participantResult?.properties.sort { it?.type?.name }}"
                            var="participantResultProperty">
                        <td>
                            ${participantResultProperty?.getResult()}

                            <g:if test="${participantResultProperty?.comment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.comment}">
                                    <i class="question circle icon"></i>
                                </span>
                            </g:if>

                            <g:if test="${participantResultProperty?.ownerComment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.ownerComment}">
                                    <i class="info circle icon"></i>
                                </span>
                            </g:if>

                        </td>
                    </g:each>

                    <td>

                        <g:set var="costItem" value="${participantResult?.resultOfParticipation?.getCostItem()}"/>

                        <g:if test="${costItem}">
                            <b><g:formatNumber number="${costItem?.costInBillingCurrencyAfterTax}" minFractionDigits="2"
                                               maxFractionDigits="2" type="number"/></b>

                            (<g:formatNumber number="${costItem?.costInBillingCurrency}" minFractionDigits="2"
                                             maxFractionDigits="2" type="number"/>)

                            ${(costItem?.billingCurrency?.getI10n('value')?.split('-'))?.first()}
                        </g:if>
                    </td>
                    <td>

                        <g:link controller="myInstitution" action="surveyParticipantConsortiaNew" id="${participantResult?.participant?.id}"
                                params="[surveyConfig: surveyConfig?.id]" class="ui button icon"><i class="icon chart pie"></i></g:link>

                        <g:if test="${participantResult?.sub}">
                            <br>
                            <g:link controller="subscription" action="show" id="${participantResult?.sub?.id}"
                                    class="ui button icon"><i class="icon clipboard"></i></g:link>
                        </g:if>
                    </td>

                </tr>
            </g:each>
        </table>


        <br>
        <br>

        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.withMultiYearTermSub.label')} <semui:totalNumber
                total="${orgsWithMultiYearTermSub?.size()?:0}"/></h4>

        <table class="ui celled la-table table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'default.sortname.label')}</th>
                <th>${message(code: 'default.startDate.label')}</th>
                <th>${message(code: 'default.endDate.label')}</th>
                <th>${message(code: 'subscription.details.status')}</th>
                <th>${message(code: 'default.actions')}</th>

            </tr>
            </thead>
            <tbody>
            <g:each in="${orgsWithMultiYearTermSub}" var="sub" status="i">
                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <g:each in="${sub.getAllSubscribers()}" var="subscriberOrg">
                        <td>
                            ${subscriberOrg?.sortname}
                            <br>

                            <g:link controller="organisation" action="show"
                                    id="${subscriberOrg.id}">(${fieldValue(bean: subscriberOrg, field: "name")})</g:link>
                        </td>
                        <td><g:formatDate formatName="default.date.format.notime" date="${sub.startDate}"/></td>
                        <td><g:formatDate formatName="default.date.format.notime" date="${sub.endDate}"/></td>
                        <td>${sub.status.getI10n('value')}</td>
                        <td>
                            <g:if test="${sub}">
                                <g:link controller="subscription" action="show" id="${sub?.id}"
                                        class="ui button icon"><i class="icon clipboard"></i></g:link>
                            </g:if>
                            <g:if test="${sub?.getCalculatedSuccessor()}">
                                <br>
                                <g:link controller="subscription" action="show" id="${sub?.getCalculatedSuccessor()?.id}"
                                        class="ui button icon"><i class="icon yellow clipboard"></i></g:link>
                            </g:if>
                        </td>
                    </g:each>
                </tr>
            </g:each>
            </tbody>
        </table>

        <br>
        <br>

        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.orgsWithParticipationInParentSuccessor.label')} <semui:totalNumber
                total="${orgsWithParticipationInParentSuccessor?.size()?:0}"/></h4>

        <table class="ui celled la-table table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'default.sortname.label')}</th>
                <th>${message(code: 'default.startDate.label')}</th>
                <th>${message(code: 'default.endDate.label')}</th>
                <th>${message(code: 'subscription.details.status')}</th>
                <th>${message(code: 'default.actions')}</th>

            </tr>
            </thead>
            <tbody>
            <g:each in="${orgsWithParticipationInParentSuccessor}" var="sub" status="i">
                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <g:each in="${sub.getAllSubscribers()}" var="subscriberOrg">
                        <td>
                            ${subscriberOrg?.sortname}
                            <br>
                            <g:link controller="organisation" action="show"
                                    id="${subscriberOrg.id}">(${fieldValue(bean: subscriberOrg, field: "name")})</g:link>
                        </td>
                        <td><g:formatDate formatName="default.date.format.notime" date="${sub.startDate}"/></td>
                        <td><g:formatDate formatName="default.date.format.notime" date="${sub.endDate}"/></td>
                        <td>${sub.status.getI10n('value')}</td>
                        <td>
                            <g:if test="${sub}">
                                <g:link controller="subscription" action="show" id="${sub?.id}"
                                        class="ui button icon"><i class="icon clipboard"></i></g:link>
                            </g:if>
                            <g:if test="${sub?.getCalculatedSuccessor()}">
                                <br>
                                <g:link controller="subscription" action="show" id="${sub?.getCalculatedSuccessor()?.id}"
                                        class="ui button icon"><i class="icon yellow clipboard"></i></g:link>
                            </g:if>
                        </td>
                    </g:each>
                </tr>
            </g:each>
            </tbody>
        </table>

        <br>
        <br>


        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.orgsLateCommers.label')} <semui:totalNumber
                total="${orgsLateCommers?.size()?:0}"/></h4>

        <table class="ui celled la-table table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'default.sortname.label')}</th>
                <th>${message(code: 'default.startDate.label')}</th>
                <th>${message(code: 'default.endDate.label')}</th>
                <th>${message(code: 'subscription.details.status')}</th>
                <th>${message(code: 'default.actions')}</th>

            </tr>
            </thead>
            <tbody>
            <g:each in="${orgsLateCommers}" var="sub" status="i">
                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <g:each in="${sub.getAllSubscribers()}" var="subscriberOrg">
                        <td>
                            ${subscriberOrg?.sortname}
                            <br>
                            <g:link controller="organisation" action="show"
                                    id="${subscriberOrg.id}">(${fieldValue(bean: subscriberOrg, field: "name")})</g:link>
                        </td>
                        <td><g:formatDate formatName="default.date.format.notime" date="${sub.startDate}"/></td>
                        <td><g:formatDate formatName="default.date.format.notime" date="${sub.endDate}"/></td>
                        <td>${sub.status.getI10n('value')}</td>
                        <td>
                            <g:if test="${sub}">
                                <g:link controller="subscription" action="show" id="${sub?.id}"
                                        class="ui button icon"><i class="icon clipboard"></i></g:link>
                            </g:if>
                            <g:if test="${sub?.getCalculatedSuccessor()}">
                                <br>
                                <g:link controller="subscription" action="show" id="${sub?.getCalculatedSuccessor()?.id}"
                                        class="ui button icon"><i class="icon yellow clipboard"></i></g:link>
                            </g:if>
                        </td>
                    </g:each>
                </tr>
            </g:each>
            </tbody>
        </table>

        <br>
        <br>

        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.withTermination.label')} <semui:totalNumber
                total="${orgsWithTermination?.size()?:0}"/></h4>

        <table class="ui celled sortable table la-table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'org.name.label')}</th>

                <th>
                    ${participationProperty?.getI10n('name')}

                    <g:if test="${participationProperty?.getI10n('explain')}">
                        <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="bottom center"
                              data-content="${participationProperty?.getI10n('explain')}">
                            <i class="question circle icon"></i>
                        </span>
                    </g:if>
                </th>

                <g:each in="${properties}" var="surveyProperty">
                    <th>
                        ${surveyProperty?.getI10n('name')}

                        <g:if test="${surveyProperty?.getI10n('explain')}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${surveyProperty?.getI10n('explain')}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </th>
                </g:each>
                <th>${message(code: 'renewalwithSurvey.costItem.label')}</th>
                <th>${message(code: 'default.actions')}</th>
            </tr>
            </thead>
            <g:each in="${orgsWithTermination}" var="participantResult" status="i">

                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <td>
                        <g:link controller="myInstitution" action="manageParticipantSurveys" id="${participantResult?.participant.id}">
                            ${participantResult?.participant?.sortname}
                        </g:link>
                        <br>
                        <g:link controller="organisation" action="show"
                                id="${participantResult?.participant.id}">(${fieldValue(bean: participantResult?.participant, field: "name")})</g:link>

                        <div class="ui grid">
                            <div class="right aligned wide column">
                                <g:if test="${!surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(participantResult?.participant)}" >
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.newOrg')}">
                                        <i class="star black large  icon"></i>
                                    </span>
                                </g:if>

                                <g:if test="${surveyConfig?.checkResultsEditByOrg(participantResult?.participant) == com.k_int.kbplus.SurveyConfig.ALL_RESULTS_PROCESSED_BY_ORG}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.processedOrg')}">
                                        <i class="edit green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notprocessedOrg')}">
                                        <i class="edit red icon"></i>
                                    </span>
                                </g:else>

                                <g:if test="${surveyConfig?.isResultsSetFinishByOrg(participantResult?.participant)}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.finishOrg')}">
                                        <i class="check green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notfinishOrg')}">
                                        <i class="x red icon"></i>
                                    </span>
                                </g:else>
                            </div>
                        </div>

                    </td>
                    <td>
                        ${participantResult?.resultOfParticipation?.getResult()}

                        <g:if test="${participantResult?.resultOfParticipation?.comment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.comment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>

                        <g:if test="${participantResult?.resultOfParticipation?.ownerComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.ownerComment}">
                                <i class="info circle icon"></i>
                            </span>
                        </g:if>

                    </td>

                    <g:each in="${participantResult?.properties.sort { it?.type?.name }}"
                            var="participantResultProperty">
                        <td>
                            ${participantResultProperty?.getResult()}

                            <g:if test="${participantResultProperty?.comment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.comment}">
                                    <i class="question circle icon"></i>
                                </span>
                            </g:if>

                            <g:if test="${participantResultProperty?.ownerComment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.ownerComment}">
                                    <i class="info circle icon"></i>
                                </span>
                            </g:if>

                        </td>
                    </g:each>
                    <td>

                        <g:set var="costItem" value="${participantResult?.resultOfParticipation?.getCostItem()}"/>
                        <g:if test="${costItem}">
                            <b><g:formatNumber number="${costItem?.costInBillingCurrencyAfterTax}" minFractionDigits="2"
                                               maxFractionDigits="2" type="number"/></b>

                            (<g:formatNumber number="${costItem?.costInBillingCurrency}" minFractionDigits="2"
                                             maxFractionDigits="2" type="number"/>)

                            ${(costItem?.billingCurrency?.getI10n('value')?.split('-')).first()}
                        </g:if>
                    </td>
                    <td>

                        <g:link controller="myInstitution" action="surveyParticipantConsortiaNew" id="${participantResult?.participant?.id}"
                                params="[surveyConfig: surveyConfig?.id]" class="ui button icon"><i class="icon chart pie"></i></g:link>

                        <g:if test="${participantResult?.sub}">
                            <br>
                            <g:link controller="subscription" action="show" id="${participantResult?.sub?.id}"
                                    class="ui button icon"><i class="icon clipboard"></i></g:link>
                        </g:if>
                    </td>
                </tr>
            </g:each>
        </table>

        <br>
        <br>

        <h4 class="ui left aligned icon header">${message(code: 'renewalwithSurvey.orgsWithoutResult.label')} <semui:totalNumber
                total="${orgsWithoutResult?.size()?:0}"/></h4>

        <table class="ui celled sortable table la-table">
            <thead>
            <tr>
                <th class="center aligned">${message(code: 'sidewide.number')}</th>
                <th>${message(code: 'org.name.label')}</th>

                <th>
                    ${participationProperty?.getI10n('name')}

                    <g:if test="${participationProperty?.getI10n('explain')}">
                        <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="bottom center"
                              data-content="${participationProperty?.getI10n('explain')}">
                            <i class="question circle icon"></i>
                        </span>
                    </g:if>
                </th>

                <g:each in="${properties}" var="surveyProperty">
                    <th>
                        ${surveyProperty?.getI10n('name')}

                        <g:if test="${surveyProperty?.getI10n('explain')}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${surveyProperty?.getI10n('explain')}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>
                    </th>
                </g:each>
                <th>${message(code: 'renewalwithSurvey.costItem.label')}</th>
                <th>${message(code: 'default.actions')}</th>
            </tr>
            </thead>
            <g:each in="${orgsWithoutResult}" var="participantResult" status="i">

                <tr>
                    <td class="center aligned">
                        ${i + 1}
                    </td>
                    <td>

                        <g:link controller="myInstitution" action="manageParticipantSurveys" id="${participantResult?.participant.id}">
                        ${participantResult?.participant?.sortname}
                        </g:link>

                        <br>

                        <g:link controller="organisation" action="show"
                                id="${participantResult?.participant.id}">(${fieldValue(bean: participantResult?.participant, field: "name")})</g:link>

                        <div class="ui grid">
                            <div class="right aligned wide column">
                                <g:if test="${!surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(participantResult?.participant)}" >
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.newOrg')}">
                                        <i class="star black large  icon"></i>
                                    </span>
                                </g:if>

                                <g:if test="${surveyConfig?.checkResultsEditByOrg(participantResult?.participant) == com.k_int.kbplus.SurveyConfig.ALL_RESULTS_PROCESSED_BY_ORG}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.processedOrg')}">
                                        <i class="edit green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notprocessedOrg')}">
                                        <i class="edit red icon"></i>
                                    </span>
                                </g:else>

                                <g:if test="${surveyConfig?.isResultsSetFinishByOrg(participantResult?.participant)}">
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.finishOrg')}">
                                        <i class="check green icon"></i>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span data-position="top right"  class="la-popup-tooltip la-delay" data-content="${message(code:'surveyResult.notfinishOrg')}">
                                        <i class="x red icon"></i>
                                    </span>
                                </g:else>
                            </div>
                        </div>
                    </td>
                    <td>
                        ${participantResult?.resultOfParticipation?.getResult()}

                        <g:if test="${participantResult?.resultOfParticipation?.comment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.comment}">
                                <i class="question circle icon"></i>
                            </span>
                        </g:if>

                        <g:if test="${participantResult?.resultOfParticipation?.ownerComment}">
                            <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                  data-content="${participantResult?.resultOfParticipation?.ownerComment}">
                                <i class="info circle icon"></i>
                            </span>
                        </g:if>

                    </td>

                    <g:each in="${participantResult?.properties.sort { it?.type?.name }}"
                            var="participantResultProperty">
                        <td>
                            ${participantResultProperty?.getResult()}

                            <g:if test="${participantResultProperty?.comment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.comment}">
                                    <i class="question circle icon"></i>
                                </span>
                            </g:if>

                            <g:if test="${participantResultProperty?.ownerComment}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="right center"
                                      data-content="${participantResultProperty?.ownerComment}">
                                    <i class="info circle icon"></i>
                                </span>
                            </g:if>

                        </td>
                    </g:each>
                    <td>

                        <g:set var="costItem" value="${participantResult?.resultOfParticipation?.getCostItem()}"/>
                        <g:if test="${costItem}">
                            <b><g:formatNumber number="${costItem?.costInBillingCurrencyAfterTax}" minFractionDigits="2"
                                               maxFractionDigits="2" type="number"/></b>

                            (<g:formatNumber number="${costItem?.costInBillingCurrency}" minFractionDigits="2"
                                             maxFractionDigits="2" type="number"/>)

                            ${(costItem?.billingCurrency?.getI10n('value')?.split('-')).first()}
                        </g:if>
                    </td>
                    <td>

                        <g:link controller="myInstitution" action="surveyParticipantConsortiaNew" id="${participantResult?.participant?.id}"
                                params="[surveyConfig: surveyConfig?.id]" class="ui button icon"><i class="icon chart pie"></i></g:link>

                        <g:if test="${participantResult?.sub}">
                            <br>
                            <g:link controller="subscription" action="show" id="${participantResult?.sub?.id}"
                                    class="ui button icon"><i class="icon clipboard"></i></g:link>
                        </g:if>
                    </td>
                </tr>
            </g:each>
        </table>

    </g:form>
</semui:form>

</body>
</html>
