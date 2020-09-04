<%@ page import="de.laser.Person; de.laser.SubscriptionsQueryService; com.k_int.kbplus.Subscription; java.text.SimpleDateFormat; de.laser.helper.RDStore; de.laser.FormService;com.k_int.kbplus.GenericOIDService;" %>
<laser:serviceInjection/>

<g:set var="formService" bean="formService"/>

<semui:form>
    <g:render template="/templates/copyElements/selectSourceAndTargetObject" model="[
            sourceObject          : sourceObject,
            targetObject          : targetObject,
            allObjects_readRights : allObjects_readRights,
            allObjects_writeRights: allObjects_writeRights]"/>
    <g:form action="copyElementsIntoSubscription" controller="subscription"
            params="[workFlowPart: workFlowPart, sourceObjectId: GenericOIDService.getOID(sourceObject), targetObjectId: GenericOIDService.getOID(targetObject), isRenewSub: isRenewSub, fromSurvey: fromSurvey]"
            method="post" class="ui form newLicence">
        <input type="hidden" name="${FormService.FORM_SERVICE_TOKEN}" value="${formService.getNewToken()}"/>
        <g:if test="${com.k_int.kbplus.SurveyConfig.findAllBySubscriptionAndSubSurveyUseForTransfer(sourceObject, true)}">
            <semui:msg class="negative" message="copyElementsIntoObject.surveyExist"/>
        </g:if>
        <g:else>

            <table class="ui celled table">
                <tbody>
                <table>
                    <tr>
                        <td>
                            <table class="ui celled la-table table">
                                <thead>
                                <tr>
                                    <th colspan="5">
            <g:if test="${sourceObject}"><g:link controller="subscription"
                                                       action="show"
                                                       id="${sourceObject.id}">${sourceObject.dropdownNamingConvention()}</g:link></g:if>
            </th>
        </tr>
            <tr>
                <th>${message(code: 'default.sortname.label')}</th>
                <th>${message(code: 'default.startDate.label')}</th>
                <th>${message(code: 'default.endDate.label')}</th>
                <th>${message(code: 'default.status.label')}</th>
                <th>
                    <input type="checkbox" name="checkAllCopyCheckboxes" data-action="copy"
                           onClick="toggleAllCheckboxes(this)" checked/>
                </th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${validSourceSubChilds}" var="sub">
                <tr>
                    <g:each in="${sub.getAllSubscribers()}" var="subscriberOrg">
                        <td>
                            <g:link controller="subscription"
                                    action="show"
                                    id="${sub.id}">${subscriberOrg.sortname}</g:link>
                            <g:if test="${subscriberOrg.getCustomerType() in ['ORG_INST', 'ORG_INST_COLLECTIVE']}">
                                <span class="la-long-tooltip la-popup-tooltip la-delay" data-position="bottom center"
                                      data-content="${subscriberOrg.getCustomerTypeI10n()}">
                                    <i class="chess rook grey icon"></i>
                                </span>
                            </g:if>
                        </td>
                        <td><g:formatDate formatName="default.date.format.notime"
                                          date="${sub.startDate}"/></td>
                        <td><g:formatDate formatName="default.date.format.notime"
                                          date="${sub.endDate}"/></td>
                        <td>${sub.status.getI10n('value')}</td>
                        <td>
                            <div class="ui checkbox la-toggle-radio la-replace">
                                <g:checkBox name="copyObject.copySubscriber"
                                            value="${genericOIDService.getOID(sub)}"
                                            data-action="copy" checked="${true}"/>
                            </div>
                        </td>
                    </g:each>
                </tr>
            </g:each>
            </tbody>
            </table>
        </td>
            <td>
                <table class="ui celled la-table table">
                    <thead>
                    <tr>
                        <th colspan="4">
                            <g:if test="${targetObject}"><g:link controller="subscription"
                                                                       action="show"
                                                                       id="${targetObject.id}">${targetObject.dropdownNamingConvention()}</g:link></g:if>
                        </th>
                    </tr>
                    <tr>
                        <th>${message(code: 'default.sortname.label')}</th>
                        <th>${message(code: 'default.startDate.label')}</th>
                        <th>${message(code: 'default.endDate.label')}</th>
                        <th>${message(code: 'default.status.label')}</th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each in="${validTargetSubChilds}" var="sub">
                        <tr>
                            <g:each in="${sub.refresh().getAllSubscribers()}" var="subscriberOrg">
                                <td>
                                    <g:link controller="subscription"
                                            action="show"
                                            id="${sub.id}">${subscriberOrg.sortname}</g:link>
                                </td>
                                <td><g:formatDate formatName="default.date.format.notime"
                                                  date="${sub.startDate}"/></td>
                                <td><g:formatDate formatName="default.date.format.notime"
                                                  date="${sub.endDate}"/></td>
                                <td>${sub.status.getI10n('value')}</td>
                            </g:each>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </td>
            </table>
        </g:else>

        <g:set var="submitDisabled" value="${(sourceObject && targetObject) ? '' : 'disabled'}"/>
        <div class="sixteen wide field" style="text-align: right;">
            <input type="submit" class="ui button js-click-control"
                   value="${message(code: 'copyElementsIntoObject.copySubscriber.button')}" ${submitDisabled}/>
        </div>
        </tbody>
    </table>
    </g:form>
</semui:form>

<script language="JavaScript">
    $('#subListToggler').click(function () {
        if ($(this).prop('checked')) {
            $("tr[class!=disabled] input[name=selectedSubs]").prop('checked', true)
        } else {
            $("tr[class!=disabled] input[name=selectedSubs]").prop('checked', false)
        }
    })
</script>
<style>
table {
    table-layout: fixed;
    width: 100%;
}

table td {
    vertical-align: top;
}
</style>
