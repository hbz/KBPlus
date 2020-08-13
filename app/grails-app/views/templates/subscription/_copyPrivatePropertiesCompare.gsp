<%@ page import="com.k_int.kbplus.Subscription; com.k_int.kbplus.SubscriptionController" %>
<laser:serviceInjection/>
<!doctype html>
<html>
%{--<head>--}%
%{--<meta name="layout" content="semanticUI" />--}%
%{--<title>${message(code:'laser')} : ${message(code:'menu.my.comp_lic')}</title>--}%
%{--</head>--}%
<body>
<semui:form>
    <g:if test="${!fromSurvey && !isRenewSub}">
        <g:render template="/templates/copyElements/selectSourceAndTargetObject" model="[
                sourceObject          : sourceObject,
                targetObject          : targetObject,
                allObjects_readRights : allObjects_readRights,
                allObjects_writeRights: allObjects_writeRights]"/>
    </g:if>

    <g:form controller="${controllerName}" action="${actionName}" id="${params.id ?: params.sourceObjectId}"
            params="[workFlowPart: SubscriptionController.WORKFLOW_END, sourceObjectId: sourceObjectId, targetObjectId: targetObjectId, isRenewSub: isRenewSub, fromSurvey: fromSurvey]"
            method="post" class="ui form newLicence">

        <%
            List subscriptions = [Subscription.get(sourceObjectId)]
            if (targetObjectId) subscriptions.add(Subscription.get(targetObjectId))
        %>

        <g:set var="subscriptionsCount" value="${subscriptions?.size()}"/>

        <g:if test="${privateProperties?.size() > 0}">
            <table class="ui celled table la-table">
                <g:render template="/templates/subscription/propertyComparisonTableRow"
                          model="[group: privateProperties, key: message(code: 'subscription.properties.private') + ' ' + contextService.getOrg().name, subscriptions: subscriptions]"/>
            </table>
        </g:if>
        <g:set var="submitDisabled" value="${(sourceObject && targetObject) ? '' : 'disabled'}"/>

        <g:if test="${customProperties || privateProperties}">
            <g:set var="submitButtonText" value="${isRenewSub ?
                    message(code: 'subscription.renewSubscriptionConsortia.workFlowSteps.lastStep') :
                    message(code: 'subscription.details.copyElementsIntoSubscription.copyProperties.button')}"/>
            <div class="sixteen wide field" style="text-align: right;">
                <input type="submit" class="ui button js-click-control" value="${submitButtonText}"
                       onclick="return jsConfirmation()" ${submitDisabled}/>
            </div>
        </g:if>
        <g:else>
            ${message(code: 'subscription.details.copyElementsIntoSubscription.copyProperties.empty')}
            <br><br>

            <div class="sixteen wide field" style="text-align: right;">
                <g:set var="submitButtonText" value="${isRenewSub ?
                        message(code: 'subscription.renewSubscriptionConsortia.workFlowSteps.lastStepWithoutSaveDate') :
                        message(code: 'subscription.details.copyElementsIntoSubscription.lastStepWithoutSaveDate')}"/>
                <input type="submit" class="ui button js-click-control" value="${submitButtonText}"
                       onclick="return jsConfirmation()" ${submitDisabled}/>
            </div>
        </g:else>

    </g:form>
</semui:form>
</body>
</html>
