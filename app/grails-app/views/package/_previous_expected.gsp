<%@ page import="com.k_int.kbplus.Package" %>
<!doctype html>
<html>
  <head>
    <meta name="layout" content="semanticUI">
    <title>${message(code:'laser')} : ${message(code:'package.label')}</title>
  </head>
  <body>

      <semui:breadcrumbs>
          <semui:crumb controller="package" action="index" text="${message(code:'package.show.all')}" />
          <semui:crumb text="${packageInstance.name}" id="${packageInstance.id}" class="active"/>
      </semui:breadcrumbs>

      <semui:modeSwitch controller="package" action="${params.action}" params="${params}" />

      <semui:controlButtons>
          <semui:exportDropdown>
              <semui:exportDropdownItem>
                  <g:link class="item" action="show" params="${params+[format:'json']}">JSON</g:link>
              </semui:exportDropdownItem>
              <semui:exportDropdownItem>
                  <g:link class="item" action="show" params="${params+[format:'xml']}">XML</g:link>
              </semui:exportDropdownItem>
          </semui:exportDropdown>
          <g:render template="actions" />
      </semui:controlButtons>


          <h1 class="ui icon header la-clear-before la-noMargin-top"><semui:headerIcon />

              <g:if test="${editable}"><span id="packageNameEdit"
                        class="xEditableValue"
                        data-type="textarea"
                        data-pk="${packageInstance.class.name}:${packageInstance.id}"
                        data-name="name"
                        data-url='<g:createLink controller="ajax" action="editableSetValue"/>'>${packageInstance.name}</span></g:if>
              <g:else>${packageInstance.name}</g:else>
          </h1>

            <g:render template="nav" contextPath="." />

                <sec:ifAnyGranted roles="ROLE_ADMIN">
                    <g:link class="ui button" controller="announcement" action="index" params='[at:"Package Link: ${pkg_link_str}",as:"RE: Package ${packageInstance.name}"]'>${message(code:'package.show.announcement', default:'Mention this package in an announcement')}</g:link>
                </sec:ifAnyGranted>

            <g:if test="${forum_url != null}">
              <a href="${forum_url}">| Discuss this package in forums</a> <a href="${forum_url}" title="Discuss this package in forums (new Window)" target="_blank"><i class="icon-share-alt"></i></a>
            </g:if>

  <semui:messages data="${flash}" />

  <semui:errors bean="${packageInstance}" />

    <div>

        <dl>
          <dt>${message(code:'title.search.offset.text', args:[offset+1,lasttipp,num_tipp_rows])}

          </dt>

        %{--  <dd>

          <table class="ui celled la-table table">
            <g:form action="packageBatchUpdate" params="${[id:packageInstance?.id]}">
            <thead>
                <tr>
                    <th rowspan="2">&nbsp;</th>
                    <th rowspan="2">&nbsp;</th>
                    <g:sortableColumn rowspan="2" params="${params}" property="tipp.title.sortTitle" title="${message(code:'title.label', default:'Title')}" />
                    <th rowspan="2">${message(code:'tipp.platform', default:'Platform')}</th>
                    <th rowspan="2">${message(code:'identifier.plural', default:'Identifiers')}</th>
                    <th colspan="2">${message(code:'tipp.coverage')}</th>
                    <th colspan="2">${message(code:'tipp.access')}</th>
                    <th rowspan="2">${message(code:'tipp.coverageDepth', default:'Coverage Depth')}</th>
                </tr>
                <tr>
                    <th>${message(code:'default.from')}</th>
                    <th>${message(code:'default.to')}</th>
                    <th>${message(code:'default.from')}</th>
                    <th>${message(code:'default.to')}</th>
                </tr>
            </thead>
            <tbody>
            <g:set var="counter" value="${offset+1}" />
            <g:each in="${titlesList}" var="t">
              <g:set var="hasCoverageNote" value="${t.coverageNote?.length() > 0}" />
              <tr>
                <td ${hasCoverageNote==true?'rowspan="2"':''}><g:if test="${editable}"><input type="checkbox" name="_bulkflag.${t.id}" class="bulkcheck"/></g:if></td>
                <td ${hasCoverageNote==true?'rowspan="2"':''}>${counter++}</td>
                <td style="vertical-align:top;">
                   ${t.title.title} 
                   <g:link controller="title" action="show" id="${t.title.id}">(${message(code:'title.label', default:'Title')})</g:link>
                   <g:link controller="tipp" action="show" id="${t.id}">(${message(code:'tipp.label', default:'TIPP')})</g:link><br/>
                   <span title="${t.availabilityStatusExplanation}">${message(code:'default.access.label', default:'Access')}: ${t.availabilityStatus?.value}</span>
                    <span>${message(code:'title.type.label')}: ${t.title.medium.getI10n('value')}</span>
                </td>
                <td style="white-space: nowrap;vertical-align:top;">
                   <g:if test="${t.hostPlatformURL != null}">
                       <a href="${t.hostPlatformURL.contains('http') ?:'http://'+t.hostPlatformURL}" target="_blank">${t.platform?.name}</a>
                   </g:if>
                   <g:else>
                     ${t.platform?.name}
                   </g:else>
                </td>
                <td style="white-space: nowrap;vertical-align:top;">
                  <g:each in="${t.title.ids?.sort{it?.identifier?.ns?.ns}}" var="id">
                    ${id.identifier.ns.ns}: ${id.identifier.value}<br/>
                  </g:each>
                </td>

                <td>
                  ${message(code:'default.date.label')}: <semui:xEditable owner="${t}" type="date" field="startDate" /><br/>
                  ${message(code:'tipp.volume', default:'Volume')}: <semui:xEditable owner="${t}" field="startVolume" /><br/>
                  ${message(code:'tipp.issue', default:'Issue')}: <semui:xEditable owner="${t}" field="startIssue" />
                </td>
                <td>
                    ${message(code:'default.date.label')}: <semui:xEditable owner="${t}" type="date" field="endDate" /><br/>
                    ${message(code:'tipp.volume', default:'Volume')}: <semui:xEditable owner="${t}" field="endVolume" /><br/>
                    ${message(code:'tipp.issue', default:'Issue')}: <semui:xEditable owner="${t}" field="endIssue" /></td>
                <td>
                    ${message(code:'default.date.label')}: <semui:xEditable owner="${t}" type="date" field="accessStartDate" />
                </td>
                <td>
                    ${message(code:'default.date.label')}: <semui:xEditable owner="${t}" type="date" field="accessEndDate" />
                </td>
                <td>
                  <semui:xEditable owner="${t}" field="coverageDepth" />
                </td>
              </tr>

              <g:if test="${hasCoverageNote==true}">
                <tr>
                  <td colspan="6">${message(code:'tipp.coverageNote', default:'Coverage Note')}: ${t.coverageNote}</td>
                </tr>
              </g:if>

            </g:each>
            </tbody>
            </g:form>
          </table>
          </dd>--}%


        <g:render template="/templates/tipps/table" model="[tipps: titlesList, showPackage: false, showPlattform: true, showBulkFlag: false]"/>

        </dl>

          <g:if test="${titlesList}" >
            <semui:paginate  action="${params.action}" controller="package" params="${params}" next="${message(code:'default.paginate.next')}" prev="${message(code:'default.paginate.prev')}" maxsteps="${max}" total="${num_tipp_rows}" />
          </g:if>


        <%--
        <g:if test="${editable}">

            <semui:form>
                <g:form class="ui form" controller="ajax" action="addToCollection">

            <legend><h3 class="ui header">${message(code:'package.show.title.add', default:'Add A Title To This Package')}</h3></legend>
            <input type="hidden" name="__context" value="${packageInstance.class.name}:${packageInstance.id}"/>
            <input type="hidden" name="__newObjectClass" value="com.k_int.kbplus.TitleInstancePackagePlatform"/>
            <input type="hidden" name="__recip" value="pkg"/>

            <!-- N.B. this should really be looked up in the controller and set, not hard coded here -->
            <input type="hidden" name="status" value="com.k_int.kbplus.RefdataValue:29"/>

              <div class="two fluid fields">
                  <div class="field">
                    <label>${message(code:'package.show.title.add.title', default:'Title To Add')}</label>
                    <g:simpleReferenceTypedown class="input-xxlarge" style="width:350px;" name="title" baseClass="com.k_int.kbplus.TitleInstance"/><br/>
                    </div>
                  <div class="field">
                    <label>${message(code:'package.show.title.add.platform', default:'Platform For Added Title')}</label>
                    <g:simpleReferenceTypedown class="input-large" style="width:350px;" name="platform" baseClass="com.k_int.kbplus.Platform"/><br/>
                  </div>
              </div>
            <button type="submit" class="ui button">${message(code:'package.show.title.add.submit', default:'Add Title...')}</button>

                </g:form>
            </semui:form>


        </g:if>--%>

      </div>


    <%-- <g:render template="enhanced_select" contextPath="../templates" /> --%>
    <g:render template="orgLinksModal" 
              contextPath="../templates" 
              model="${[roleLinks:packageInstance?.orgs,parent:packageInstance.class.name+':'+packageInstance.id,property:'orgs',recip_prop:'pkg']}" />

    <r:script>
      $(function(){
        $.fn.editable.defaults.mode = 'inline';
        $('.xEditableValue').editable();
      });
      function selectAll() {
        $('#select-all').is( ":checked")? $('.bulkcheck').prop('checked', true) : $('.bulkcheck').prop('checked', false);
      }

      function confirmSubmit() {
        if ( $('#bulkOperationSelect').val() === 'remove' ) {
          var agree=confirm("${message(code:'default.continue.confirm', default:'Are you sure you wish to continue?')}");
          if (agree)
            return true ;
          else
            return false ;
        }
      }

    </r:script>

  </body>
</html>
