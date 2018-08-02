<%@ page import="com.k_int.kbplus.Subscription" %>
<!doctype html>
<html>
  <head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'subscription.label', default:'Subscription')}</title>
  </head>
    <body>
        <semui:breadcrumbs>
            <semui:crumb controller="myInstitution" action="currentSubscriptions" text="${message(code:'myinst.currentSubscriptions.label', default:'Current Subscriptions')}" />
            <semui:crumb controller="subscriptionDetails" action="index" id="${subscriptionInstance.id}"  text="${subscriptionInstance.name}" />
            <semui:crumb class="active" text="${message(code:'subscription.details.addEntitlements.label', default:'Add Entitlements')}" />
        </semui:breadcrumbs>
        <semui:controlButtons>
            <g:render template="actions" />
        </semui:controlButtons>
        <h1 class="ui header"><semui:headerIcon />

            <g:inPlaceEdit domain="Subscription" pk="${subscriptionInstance.id}" field="name" id="name" class="newipe">${subscriptionInstance?.name}</g:inPlaceEdit>
        </h1>

        <g:render template="nav" contextPath="." />

        <g:set var="counter" value="${offset+1}" />

      <semui:filter>
        ${message(code:'subscription.details.availableTitles', default:'Available Titles')} ( ${message(code:'default.paginate.offset', args:[(offset+1),(offset+(tipps?.size())),num_tipp_rows])} )
          <g:form class="ui form" action="addEntitlements" params="${params}" method="get">
            <input type="hidden" name="sort" value="${params.sort}">
            <input type="hidden" name="order" value="${params.order}">

              <div class="fields two">
                  <div class="field">
                      <label>${message(code:'subscription.compare.filter.title', default:'Filters - Title')}</label>
                      <input name="filter" value="${params.filter}"/>
                  </div>
                  <div class="field">
                      <label>${message(code:'subscription.details.from_pkg', default:'From Package')}</label>
                      <select name="pkgfilter">
                          <option value="">${message(code:'subscription.details.from_pkg.all', default:'All')}</option>
                          <g:each in="${subscriptionInstance.packages}" var="sp">
                              <option value="${sp.pkg.id}" ${sp.pkg.id.toString()==params.pkgfilter?'selected=true':''}>${sp.pkg.name}</option>
                          </g:each>
                    </select>
                  </div>
              </div>

              <div class="fields">
                  <semui:datepicker label="default.startsBefore.label" name="startsBefore" value="${params.startsBefore}" />
                  <semui:datepicker label="default.endsAfter.label" name="endsAfter" value="${params.endsAfter}" />
                  <div class="field">
                      <label>&nbsp;</label>
                      <input type="submit" class="ui secondary button" value="${message(code:'default.button.filter.label', default:'Filter')}">
                  </div>
              </div>

          </g:form>
      </semui:filter>

          <g:form action="processAddEntitlements">
            <input type="hidden" name="siid" value="${subscriptionInstance.id}"/>
              <div class="paginateButtons" style="text-align:center">
                  <input type="submit" value="${message(code:'subscription.details.addEntitlements.add_selected', default:'Add Selected Entitlements')}" class="ui button"/>
              </div>
            <table class="ui celled stripped table">
              <thead>
                <tr>
                  <th style="vertical-align:middle;">
                    <g:if test="${editable}"><input id="select-all" type="checkbox" name="chkall" onClick="javascript:selectAll();"/></g:if>
                  </th>
                    <th>${message(code:'sidewide.number')}</th>
                  <g:sortableColumn params="${params}" property="tipp.title.sortTitle" title="${message(code:'title.label', default:'Title')}" />
                  <th>ISSN</th>
                  <th>eISSN</th>
                  <g:sortableColumn params="${params}" property="startDate" title="${message(code:'default.startDate.label', default:'Start Date')}" />
                  <g:sortableColumn params="${params}" property="endDate" title="${message(code:'default.endDate.label', default:'End Date')}" />
                  <th>${message(code:'tipp.embargo', default:'Embargo')}</th>
                  <th>${message(code:'tipp.coverageDepth', default:'Coverage Depth')}</th>
                  <th>${message(code:'tipp.coverageNote', default:'Coverage Note')}</th>
                </tr>
              </thead>
              <tbody>
                <g:each in="${tipps}" var="tipp">
                  <tr>
                    <td><input type="checkbox" name="_bulkflag.${tipp.id}" class="bulkcheck"/></td>
                    <td>${counter++}</td>
                    <td>
                      <g:link controller="tipp" id="${tipp.id}" action="show">${tipp.title.title}</g:link>
                      <br/>
                      <span class="pull-right">
                        <g:if test="${tipp?.hostPlatformURL}"><a href="${tipp?.hostPlatformURL.contains('http') ?:'http://'+tipp?.hostPlatformURL}" TITLE="${tipp?.hostPlatformURL}">${message(code:'tipp.hostPlatformURL', default:'Host Link')}</a>
                            <a href="${tipp?.hostPlatformURL.contains('http') ?:'http://'+tipp?.hostPlatformURL}" TITLE="${tipp?.hostPlatformURL} (In new window)" target="_blank"><i class="icon-share-alt"></i></a> &nbsp;| &nbsp;</g:if>
                            <g:link action="processAddEntitlements" 
                                    params="${[siid:subscriptionInstance.id,('_bulkflag.'+tipp.id):'Y']}"
                                    class="pull-right">${message(code:'subscription.details.addEntitlements.add_now', default:'Add now')}</g:link>
                      </span>
                    </td>
                    <td style="white-space: nowrap;">${tipp?.title?.getIdentifierValue('ISSN')}</td>
                    <td style="white-space: nowrap;">${tipp?.title?.getIdentifierValue('eISSN')}</td>
                    <td style="white-space: nowrap;"><g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.startDate}"/></td>
                    <td style="white-space: nowrap;"><g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.endDate}"/></td>
                    <td>${tipp.embargo}</td>
                    <td>${tipp.coverageDepth}</td>
                    <td>${tipp.coverageNote}</td>
                  </tr>
                </g:each>
              </tbody>
            </table>

            <div class="paginateButtons" style="text-align:center">
              <input type="submit" value="${message(code:'subscription.details.addEntitlements.add_selected', default:'Add Selected Entitlements')}" class="ui button"/>
            </div>


              <g:if test="${tipps}" >
                <semui:paginate controller="subscriptionDetails"
                                  action="addEntitlements" 
                                  params="${params}" next="${message(code:'default.paginate.next', default:'Next')}" prev="${message(code:'default.paginate.prev', default:'Prev')}"
                                  max="${max}" 
                                  total="${num_tipp_rows}" />
              </g:if>

          </g:form>

    <r:script language="JavaScript">
      $(document).ready(function() {
        $('span.newipe').editable('<g:createLink controller="ajax" action="genericSetValue" />', {
          type      : 'textarea',
          cancel    : '${message(code:'default.button.cancel.label', default:'Cancel')}',
          submit    : '${message(code:'default.button.ok.label', default:'OK')}',
          id        : 'elementid',
          rows      : 3,
          tooltip   : '${message(code:'default.click_to_edit', default:'Click to edit...')}'
        });
      });

      function selectAll() {
        $('#select-all').is( ":checked")? $('.bulkcheck').prop('checked', true) : $('.bulkcheck').prop('checked', false);
      }

      $("simpleHiddenRefdata").editable({
          url: function(params) {
            var hidden_field_id = $(this).data('hidden-id');
            $("#"+hidden_field_id).val(params.value);
            // Element has a data-hidden-id which is the hidden form property that should be set to the appropriate value
          }
        });
    </r:script>

  </body>
</html>
