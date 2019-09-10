<%@ page import="com.k_int.kbplus.RefdataCategory;com.k_int.kbplus.SurveyProperty;com.k_int.kbplus.SurveyConfig" %>
<laser:serviceInjection/>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code: 'laser', default: 'LAS:eR')} : ${message(code: 'survey.label')}</title>
</head>

<body>

<semui:breadcrumbs>
    <semui:crumb controller="myInstitution" action="dashboard" text="${contextService.getOrg()?.getDesignation()}"/>
    <semui:crumb controller="survey" action="currentSurveysConsortia" text="${message(code: 'menu.my.surveys')}"/>
    <g:if test="${surveyInfo}">
        <semui:crumb controller="survey" action="show" id="${surveyInfo.id}" text="${surveyInfo.name}"/>
    </g:if>
    <g:if test="${surveyInfo}">
        <semui:crumb controller="survey" action="surveyConfigsInfo" id="${surveyInfo.id}"
                     params="[surveyConfigID: surveyConfig]" text="${surveyConfig?.getConfigNameShort()}"/>
    </g:if>
    <semui:crumb message="surveyCostItems.label" class="active"/>
</semui:breadcrumbs>

<semui:controlButtons>
    <semui:exportDropdown>
        <semui:exportDropdownItem>
            <g:link class="item" action="exportSurCostItems" id="${surveyInfo?.id}"
                    params="[exportXLS: true, surveyConfigID: surveyConfig?.id]">${message(code: 'survey.exportCostItems')}</g:link>
        </semui:exportDropdownItem>
    </semui:exportDropdown>
    <g:render template="actions"/>
</semui:controlButtons>

<h1 class="ui icon header"><semui:headerTitleIcon type="Survey"/>
<semui:xEditable owner="${surveyInfo}" field="name"/>
<semui:surveyStatus object="${surveyInfo}"/>
</h1>



<g:render template="nav"/>

<semui:objectStatus object="${surveyInfo}" status="${surveyInfo.status}"/>

<semui:messages data="${flash}"/>

<br>

<h2 class="ui left aligned icon header">
    <g:if test="${surveyConfig?.type == 'Subscription'}">
        <i class="icon clipboard outline la-list-icon"></i>
        <g:link controller="subscription" action="show" id="${surveyConfig?.subscription?.id}">
            ${surveyConfig?.subscription?.name}
        </g:link>

    </g:if>
    <g:else>
        ${surveyConfig?.getConfigNameShort()}
    </g:else>
    : ${message(code: 'surveyCostItems.label')}
</h2>

<br>

<g:if test="${surveyConfigs}">
    <div class="ui grid">
%{--<div class="four wide column">
    <div class="ui vertical fluid menu">
<g:each in="${surveyConfigs.sort { it.configOrder }}" var="config" status="i">

<g:link class="item ${params.surveyConfigID == config?.id.toString() ? 'active' : ''}"
        style="${config?.costItemsFinish ? 'background-color: Lime' : ''}"
        controller="survey" action="surveyCostItems"
        id="${config?.surveyInfo?.id}" params="[surveyConfigID: config?.id]">

    <h5 class="ui header">${config?.getConfigNameShort()}</h5>
    ${com.k_int.kbplus.SurveyConfig.getLocalizedValue(config?.type)}


    <div class="ui floating circular label">${config?.orgs?.size() ?: 0}</div>
</g:link>
</g:each>
</div>
</div>--}%

    <div class="sixteen wide stretched column">
        <div class="ui top attached tabular menu">
    <g:link class="item ${params.tab == 'selectedSubParticipants' ? 'active' : ''}"
            controller="survey" action="surveyCostItems"
            id="${surveyConfig?.surveyInfo?.id}"
            params="[surveyConfigID: surveyConfig?.id, tab: 'selectedSubParticipants']">
        ${message(code: 'surveyParticipants.selectedSubParticipants')}
        <div class="ui floating circular label">${selectedSubParticipants?.size() ?: 0}</div>
    </g:link>

    <g:link class="item ${params.tab == 'selectedParticipants' ? 'active' : ''}"
            controller="survey" action="surveyCostItems"
            id="${surveyConfig?.surveyInfo?.id}"
            params="[surveyConfigID: surveyConfig?.id, tab: 'selectedParticipants']">
        ${message(code: 'surveyParticipants.selectedParticipants')}
        <div class="ui floating circular label">${selectedParticipants?.size() ?: 0}</div>
    </g:link>

    </div>

    <g:if test="${params.tab == 'selectedSubParticipants'}">

        <div class="ui bottom attached tab segment active">

        <div class="four wide column">

    %{--<button type="button" class="ui icon button right floated" data-semui="modal"
            data-href="#modalCostItemAllSub"><i class="plus icon"></i></button>


    <g:render template="/survey/costItemModal"
              model="[modalID: 'modalCostItemAllSub', setting: 'bulkForAll']"/>--}%

        <g:link onclick="addForAllSurveyCostItem([${(selectedSubParticipants?.id)}])" class="ui icon button right floated trigger-modal">
            <i class="plus icon"></i>
        </g:link>
        </div>

        <br>
        <br>

        <semui:filter>
            <g:form action="surveyCostItems" method="post" class="ui form"
                    params="[id: surveyInfo.id, surveyConfigID: params.surveyConfigID, tab: 'selectedSubParticipants']">
                <g:render template="/templates/filter/orgFilter"
                          model="[
                                  tmplConfigShow      : [['name', 'libraryType'], ['federalState', 'libraryNetwork', 'property'], ['customerType']],
                                  tmplConfigFormFilter: true,
                                  useNewLayouter      : true
                          ]"/>
            </g:form>
        </semui:filter>


        <h3><g:message code="surveyParticipants.hasAccess"/></h3>

        <g:set var="surveyParticipantsHasAccess"
               value="${selectedSubParticipants?.findAll { it?.hasAccessOrg() }?.sort {
                   it?.sortname
               }}"/>

        <div class="four wide column">

        <g:link data-orgIdList="${(surveyParticipantsHasAccess.id).join(',')}"
                data-targetId="copyEmailaddresses_ajaxModal2"
                class="ui icon button right floated trigger-modal">
            <g:message code="survey.copyEmailaddresses.participantsHasAccess"/>
        </g:link>

        <br>
        <br>

        <g:render template="/templates/filter/orgFilterTable"
                  model="[orgList       : surveyParticipantsHasAccess,
                          tmplConfigShow: ['lineNumber', 'sortname', 'name', 'surveySubInfoStartEndDate', 'surveySubCostItem', 'surveyCostItem'],
                          tableID       : 'costTable'
                  ]"/>


        <h3><g:message code="surveyParticipants.hasNotAccess"/></h3>

        <g:set var="surveyParticipantsHasNotAccess"
               value="${selectedSubParticipants?.findAll { !it?.hasAccessOrg() }?.sort { it?.sortname }}"/>

        <div class="four wide column">

            <g:link data-orgIdList="${(surveyParticipantsHasNotAccess.id).join(',')}"
                    data-targetId="copyEmailaddresses_ajaxModal3"
                    class="ui icon button right floated trigger-modal">
                <g:message code="survey.copyEmailaddresses.participantsHasNoAccess"/>
            </g:link>

            <br>
            <br>

            <g:render template="/templates/filter/orgFilterTable"
                      model="[orgList       : surveyParticipantsHasNotAccess,
                              tmplConfigShow: ['lineNumber', 'sortname', 'name', 'surveySubInfoStartEndDate', 'surveySubCostItem', 'surveyCostItem'],
                              tableID       : 'costTable'
                      ]"/>

        </div>

    </g:if>


    <g:if test="${params.tab == 'selectedParticipants'}">

        <div class="ui bottom attached tab segment active">

            <div class="four wide column">

            %{--<button type="button" class="ui icon button right floated"
                    data-href="#modalCostItemAllSub"><i class="plus icon"></i></button>


            <g:render template="/survey/costItemModal"
                      model="[modalID: 'modalCostItemAllSub', setting: 'bulkForAll']"/>--}%

                <g:link onclick="addForAllSurveyCostItem([${(selectedParticipants?.id)?.join(',')}])" class="ui icon button right floated trigger-modal">
                    <i class="plus icon"></i>
                </g:link>
            </div>

            <br>
            <br>

            <semui:filter>
                <g:form action="surveyCostItems" method="post" class="ui form"
                        params="[id: surveyInfo.id, surveyConfigID: params.surveyConfigID, tab: 'selectedParticipants']">
                    <g:render template="/templates/filter/orgFilter"
                              model="[
                                      tmplConfigShow      : [['name', 'libraryType'], ['federalState', 'libraryNetwork', 'property'], ['customerType']],
                                      tmplConfigFormFilter: true,
                                      useNewLayouter      : true
                              ]"/>
                </g:form>
            </semui:filter>



            <h3><g:message code="surveyParticipants.hasAccess"/></h3>


            <g:set var="surveyParticipantsHasAccess"
                   value="${selectedParticipants?.findAll { it?.hasAccessOrg() }?.sort { it?.sortname }}"/>

            <div class="four wide column">

                <g:link data-orgIdList="${(surveyParticipantsHasAccess.id).join(',')}"
                        data-targetId="copyEmailaddresses_ajaxModal4"
                        class="ui icon button right floated trigger-modal">
                    <g:message code="survey.copyEmailaddresses.participantsHasAccess"/>
                </g:link>

            </div>


            <%--<g:link action="copyEmailaddresses" params='[orgList: surveyParticipantsHasAccess]'
                    data-targetId="copyEmailaddresses_ajaxModal5"
                    class="ui icon button trigger-modal">
                <i class="write icon"></i>
            </g:link>--%>

            <br>
            <br>



            <g:render template="/templates/filter/orgFilterTable"
                      model="[orgList       : surveyParticipantsHasAccess,
                              tmplConfigShow: ['lineNumber', 'sortname', 'name', 'surveyCostItem'],
                              tableID       : 'costTable'
                      ]"/>


            <h3><g:message code="surveyParticipants.hasNotAccess"/></h3>

            <g:set var="surveyParticipantsHasNotAccess"
                   value="${selectedParticipants?.findAll { !it?.hasAccessOrg() }?.sort { it?.sortname }}"/>

            <div class="four wide column">
                <g:link data-orgIdList="${(surveyParticipantsHasNotAccess.id)?.join(',')}"
                        data-targetId="copyEmailaddresses_ajaxModal6"
                        class="ui icon button right floated trigger-modal">
                    <g:message code="survey.copyEmailaddresses.participantsHasNoAccess"/>
                </g:link>
            </div>


            <br>
            <br>

            <g:render template="/templates/filter/orgFilterTable"
                      model="[orgList       : surveyParticipantsHasNotAccess,
                              tmplConfigShow: ['lineNumber', 'sortname', 'name', 'surveyCostItem'],
                              tableID       : 'costTable'
                      ]"/>

        </div>

    </g:if>

    <br>
    <br>

    <g:form action="surveyCostItemsFinish" method="post" class="ui form"
            params="[id: surveyInfo.id, surveyConfigID: params.surveyConfigID]">

        <div class="ui right floated compact segment">
            <div class="ui checkbox">
                <input type="checkbox" onchange="this.form.submit()"
                       name="costItemsFinish" ${surveyConfig?.costItemsFinish ? 'checked' : ''}>
                <label><g:message code="surveyConfig.costItemsFinish.label"/></label>
            </div>
        </div>

    </g:form>

    </div>
</div>
</g:if>
<g:else>
<p><b>${message(code: 'surveyConfigs.noConfigList')}</b></p>
</g:else>

<g:javascript>

var isClicked = false;

function addForAllSurveyCostItem(orgsIDs) {
                        event.preventDefault();

                        // prevent 2 Clicks open 2 Modals
                        if (!isClicked) {
                            isClicked = true;
                            $('.ui.dimmer.modals > #modalSurveyCostItem').remove();
                            $('#dynamicModalContainer').empty()

                           $.ajax({
                                url: "<g:createLink controller='survey' action='addForAllSurveyCostItem'/>",
                                traditional: true,
                                data: {
                                    id: "${params.id}",
                                    surveyConfigID: "${surveyConfig?.id}",
                                    orgsIDs: orgsIDs
                                }
                            }).done(function (data) {
                                $('#dynamicModalContainer').html(data);

                                $('#dynamicModalContainer .ui.modal').modal({
                                    onVisible: function () {
                                        r2d2.initDynamicSemuiStuff('#modalSurveyCostItem');
                                        r2d2.initDynamicXEditableStuff('#modalSurveyCostItem');

                                    },
                                    detachable: true,
                                    closable: false,
                                    transition: 'scale',
                                    onApprove: function () {
                                        $(this).find('.ui.form').submit();
                                        return false;
                                    }
                                }).modal('show');
                            })
                            setTimeout(function () {
                                isClicked = false;
                            }, 800);
                        }
                    }

</g:javascript>

</body>
</html>
