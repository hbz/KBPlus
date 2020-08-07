<%@ page import="com.k_int.kbplus.Subscription" %>
<g:if test="${! (sourceSubscription && targetSubscription)}">
    <% if (params){
        params.remove('sourceSubscriptionId')
        params.remove('targetSubscriptionId')
    } %>
    <g:form action="${actionName}" controller="${controllerName}" id="${params.id}"
            params="${params << [workFlowPart: workFlowPart]}"
            method="post" class="ui form newLicence"  onsubmit="enableSubmit();">
        <div class="fields" style="justify-content: flex-end;">
            <div class="six wide field">
                <label>${message(code: 'subscription.details.copyElementsIntoSubscription.sourceSubscription.name')}: </label>
                <g:select class="ui search selection dropdown"
                      name="sourceSubscriptionId"
                      from="${((List<Subscription>)allSubscriptions_readRights)?.sort {it.dropdownNamingConvention()}}"
                      optionValue="${{it?.dropdownNamingConvention()}}"
                      optionKey="id"
                      value="${sourceSubscription?.id}"
                      />
            </div>
            <div class="six wide field">
                <label>${message(code: 'subscription.details.copyElementsIntoSubscription.targetSubscription.name')}: </label>
                <g:select class="ui search selection dropdown"
                      name="targetSubscriptionId"
                      from="${((List<Subscription>)allSubscriptions_writeRights)?.sort {it.dropdownNamingConvention()}}"
                      optionValue="${{it?.dropdownNamingConvention()}}"
                      optionKey="id"
                      value="${targetSubscription?.id}"
                          noSelection="${['':message(code: 'default.select.choose.label')]}"
                />
            </div>
        </div>
        <div class="fields" style="justify-content: flex-end;">
            <div class="six wide field" style="text-align: right;">
                <input type="submit" class="ui wide button" value="Lizenzen auswählen"/>
            </div>
        </div>

    </g:form>
</g:if>