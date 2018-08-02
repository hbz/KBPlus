<!doctype html>
<html>
    <head>
        <meta name="layout" content="semanticUI"/>
        <title>${message(code:'laser', default:'LAS:eR')} : ${institution.name} - ${message(code:'myinst.tipview.label', default:'Edit Core Titles')}</title>
    </head>

    <body>

        <semui:breadcrumbs>
            <semui:crumb controller="myInstitution" action="dashboard" text="${institution?.getDesignation()}" />
            <semui:crumb text="(JUSP & KB+)" message="myinst.tipview.label" class="active" />
        </semui:breadcrumbs>

    <h1 class="ui header"><semui:headerIcon />${message(code:'menu.institutions.myCoreTitles')}</h1>

        <ul class="nav nav-pills">
            <g:set var="nparams" value="${params.clone()}"/>
            <g:set var="active_filter" value="${nparams.remove('filter')}"/>

            <li class="${(active_filter=='core' || active_filter == null)?'active':''}">
                <g:link action="tipview" params="${nparams + [filter:'core']}">${message(code:'subscription.details.core', default:'Core')}</g:link>
            </li>
            <li class="${active_filter=='not'?'active':''}"><g:link action="tipview" params="${nparams + [filter:'not']}">${message(code:'myinst.tipview.notCore', default:'Not Core')}</g:link></li>
            <li class="${active_filter=='all'?'active':''}"><g:link action="tipview" params="${nparams + [filter:'all']}">${message(code:'myinst.tipview.all', default:'All')}</g:link></li>
        </ul>

        <semui:messages data="${flash}" />

        <semui:filter>
            <g:form class="ui form" action="tipview" method="get">

                <div class="fields">
                    <div class="field">
                        <label>${message(code:'title.search', default:'Search For')}</label>
                        <select name="search_for">
                            <option ${params.search_for=='title' ? 'selected' : ''} value="title">${message(code:'title.label', default:'Title')}</option>
                            <option ${params.search_for=='provider' ? 'selected' : ''} value="provider">${message(code:'default.provider.label', default:'Provider')}</option>
                        </select>
                    </div>
                    <div class="field">
                        <label>${message(code:'default.name.label', default:'Name')}</label>
                        <input name="search_str" style="padding-left:8px" placeholder="${message(code:'myinst.tipview.search.ph', default:'Partial terms accepted')}" value="${params.search_str}"/>
                    </div>
                    <div class="field">
                        <label>${message(code:'default.sort.label', default:'Sort')}</label>
                        <select name="sort">
                            <option ${params.sort=='title-title' ? 'selected' : ''} value="title-title">${message(code:'title.label', default:'Title')}</option>
                            <option ${params.sort=='provider-name' ? 'selected' : ''} value="provider-name">${message(code:'default.provider.label', default:'Provider')}</option>
                        </select>
                    </div>
                    <div class="field">
                        <label>${message(code:'default.order.label', default:'Order')}</label>
                        <select name="order" value="${params.order}">
                            <option ${params.order=='asc' ? 'selected' : ''} value="asc">${message(code:'default.asc', default:'Ascending')}</option>
                            <option ${params.order=='desc' ? 'selected' : ''} value="desc">${message(code:'default.desc', default:'Descending')}</option>
                        </select>
                        <input type="hidden" name="filter" value="${params.filter}"/>
                    </div>
                    <div class="field">
                        <label>&nbsp;</label>
                        <button type="submit" class="ui secondary button" name="search">${message(code:'default.button.filter.label', default:'Filter')}</button>
                    </div>
                </div>

            </g:form>
        </semui:filter>

        <table class="ui celled la-table table">
          <thead>
            <tr>
              <th>${message(code:'myinst.tipview.tip_tid', default:'Title in Package; Title Details')}</th>
              <th>${message(code:'default.provider.label', default:'Provider')}</th>
              <th>${message(code:'default.status.label', default:'Status')}</th>
            </tr>
          </thead>
          <tbody>
          <g:each in="${tips}" var="tip">
            <tr>

              <td>
                  <semui:listIcon type="${tip?.title?.type.('value')}"/>
                  <strong><g:link controller="myInstitution" action="tip" id="${tip.id}">${tip?.title?.title} ${message(code:'default.via', default:'via')} ${tip?.provider?.name}</g:link></strong><br>
                  <g:link controller="titleDetails" action="show" id="${tip?.title?.id}">${message(code:'myinst.tipview.link_to_title', default:'Link to Title Details')}</g:link>
              </td>
              <td>
              <g:link controller="org" action="show" id="${tip?.provider?.id}">${tip?.provider?.name}</g:link>
              </td>
              <td class="link">

                <g:set var="coreStatus" value="${tip?.coreStatus(null)}"/>
                <a href="#" class="editable-click" onclick="showDetails(${tip.id});">${coreStatus?'True(Now)':coreStatus==null?'False(Never)':'False(Now)'}</a>
              </td>
            </tr>
          </g:each>
          </tbody>
        </table>

          <semui:paginate action="tipview" max="${user?.defaultPageSize?:10}" params="${[:]+params}" next="Next" prev="Prev" total="${tips.totalCount}" />

        <div id="magicArea">
        </div>


        <g:javascript>
        function showDetails(id){
          console.log(${editable});
          jQuery.ajax({type:'get', url:"${createLink(controller:'ajax', action:'getTipCoreDates')}?editable="+${editable}+"&tipID="+id,success:function(data,textStatus){jQuery('#magicArea').html(data);$('div[name=coreAssertionEdit]').modal("show")},error:function(XMLHttpRequest,textStatus,errorThrown){}
        });
        }

         function hideModal(){
          $("[name='coreAssertionEdit']").modal('hide');
         }

        function showCoreAssertionModal(){
          $("[name='coreAssertionEdit']").modal('show');
          $('.xEditableValue').editable();
        }
        </g:javascript>

  </body>
</html>
