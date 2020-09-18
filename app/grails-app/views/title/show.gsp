<%@ page import="de.laser.ApiSource; com.k_int.kbplus.Package; de.laser.RefdataCategory" %>

<!doctype html>
<html>
<head>
    <meta name="layout" content="semanticUI">
    <title>${message(code:'laser')} : ${message(code:'title.details')}</title>
</head>
  <body>

      <semui:breadcrumbs>
          <semui:crumb controller="title" action="list" message="menu.public.all_titles" />
          <semui:crumb class="active" text="${message(code:'title.title.label')}: ${ti.title}" />
      </semui:breadcrumbs>
  <br>
  <h1 class="ui icon header la-clear-before la-noMargin-top">
            <semui:headerTitleIcon type="${ti.printTitleType()}"/>

            <% /*
            <g:if test="${editable}"><span id="titleEdit"
                                     class="xEditableValue"
                                     data-type="textarea"
                                     data-pk="${ti.class.name}:${ti.id}"
                                     data-name="title"
                                     data-url='<g:createLink controller="ajax" action="editableSetValue"/>'
                                     data-original-title="${ti.title}">${ti.title}</span></g:if>
             <g:else>${ti.title}</g:else> */ %>
            ${ti.title}
            <g:if test="${ti.status?.value && ti.status.value != 'Current'}">
                <span class="badge badge-error" style="vertical-align:middle;">${ti.status.getI10n('value')}</span>
            </g:if>
        </h1>

        <g:render template="nav" />

        <g:render template="/templates/meta/identifier" model="${[object: ti, editable: editable]}" />

        <semui:messages data="${flash}" />

        <div class="ui grid">

            <div class="sixteen wide column">

                <semui:form>
                <div class="la-icon-list">
                    <div class="item">
                        <semui:listIcon type="${ti.printTitleType()}"/>
                        <div class="content">
                            ${ti.printTitleType()}
                        </div>
                    </div>
                    <g:if test="${ti instanceof com.k_int.kbplus.BookInstance && (ti.firstAuthor || ti.firstEditor)}">
                        <div class="item">
                            <i class="grey icon user circle la-popup-tooltip la-delay" data-content="${message(code: 'author.slash.editor')}"></i>
                            <div class="content">
                                ${ti.getEbookFirstAutorOrFirstEditor()}
                            </div>
                        </div>
                    </g:if>

                    <g:if test="${ti instanceof com.k_int.kbplus.BookInstance}">
                        <g:if test="${ti.volume}">
                        <div class="item">
                            <i class="grey icon la-books la-popup-tooltip la-delay" data-content="${message(code: 'tipp.volume')}"></i>
                            <div class="content">
                                 ${ti.volume})
                            </div>
                        </div>
                        </g:if>
                        <g:if test="${ti.editionStatement}">
                        <div class="item">
                            <i class="grey icon copy la-popup-tooltip la-delay" data-content="${message(code: 'title.editionStatement.label')}"></i>
                            <div class="content">
                                ${ti.editionStatement}
                            </div>
                        </div>
                        </g:if>
                        <g:if test="${ti.editionNumber}">
                        <div class="item">
                            <i class="grey icon copy outline la-popup-tooltip la-delay" data-content="${message(code: 'title.editionNumber.label')}"></i>
                            <div class="content">
                                ${ti.editionNumber}
                            </div>
                        </div>
                        </g:if>
                        <g:if test="${ti.summaryOfContent}">
                        <div class="item">
                            <i class="grey icon desktop la-popup-tooltip la-delay" data-content="${message(code: 'title.summaryOfContent.label')}"></i>
                            <div class="content">
                                ${ti.summaryOfContent}
                            </div>
                        </div>
                        </g:if>
                        <g:if test="${ti.seriesName}">
                            <div class="item">
                                <i class="grey icon list la-popup-tooltip la-delay" data-content="${message(code: 'title.seriesName.label')}"></i>
                                <div class="content">
                                    ${ti.seriesName}
                                </div>
                            </div>
                        </g:if>
                        <g:if test="${ti.subjectReference}">
                            <div class="item">
                                <i class="grey icon comment alternate la-popup-tooltip la-delay" data-content="${message(code: 'title.subjectReference.label')}"></i>
                                <div class="content">
                                    ${ti.subjectReference}
                                </div>
                            </div>
                        </g:if>

                        <div class="item">
                            <i class="grey fitted la-books icon la-popup-tooltip la-delay"
                                               data-content="${message(code: 'title.dateFirstInPrint.label')}"></i>
                        <div class="content">
                            <g:formatDate format="${message(code: 'default.date.format.notime')}"
                                  date="${ti.dateFirstInPrint}"/>
                        </div>
                        </div>
                        <div class="item">
                            <i class="grey fitted la-books icon la-popup-tooltip la-delay"
                                   data-content="${message(code: 'title.dateFirstOnline.label')}"></i>
                        <div class="content"><g:formatDate format="${message(code: 'default.date.format.notime')}"
                                              date="${ti.dateFirstOnline}"/>
                        </div>
                        </div>
                    </g:if>

                    <div class="item">
                        <i class="grey clipboard check clip icon la-popup-tooltip la-delay" data-content="${message(code: 'default.status.label')}"></i>
                        <div class="content">
                            ${ti.status.getI10n("value")}
                        </div>
                    </div>

                </div>
                </semui:form>
            </div>


            <div class="sixteen wide column">
                <semui:form>
                  <g:each in="${duplicates}" var="entry">

                          ${message(code:'title.edit.duplicate.warn', args:[entry.key])}:
                          <ul>
                              <g:each in ="${entry.value}" var="dup_title">
                                  <li><g:link controller='title' action='show' id="${dup_title.id}">${dup_title.title}</g:link></li>
                              </g:each>
                          </ul>

                  </g:each>
                </semui:form>
            </div>

        </div><!-- .grid -->

          <h3 class="ui header"><g:message code="title.edit.orglink"/></h3>

            <table class="ui celled la-table table ">
              <thead>
                <tr>
                  %{--<th><g:message code="title.edit.component_id.label"/></th>--}%
                  <th><g:message code="template.orgLinks.name"/></th>
                  <th><g:message code="template.orgLinks.role"/></th>
                  <th><g:message code="title.edit.orglink.from"/></th>
                  <th><g:message code="title.edit.orglink.to"/></th>
                </tr>
              </thead>
              <tbody>
                <g:each in="${ti.orgs}" var="org">
                  <tr>
                    %{--<td>${org.org.id}</td>--}%
                    <td><g:link controller="organisation" action="show" id="${org.org.id}">${org.org.name}</g:link></td>
                    <td>${org?.roleType?.getI10n("value")}</td>
                    <td>
                      <semui:xEditable owner="${org}" type="date" field="startDate"/>
                    </td>
                    <td>
                      <semui:xEditable owner="${org}" type="date" field="endDate"/>
                    </td>
                  </tr>
                </g:each>
              </tbody>
            </table>

        %{--
            <g:render template="orgLinks" contextPath="../templates" model="${[roleLinks:ti?.orgs,editmode:editable]}" />

            <g:render template="orgLinksModal"
                contextPath="../templates"
                model="${[linkType:ti?.class?.name,roleLinks:ti?.orgs,parent:ti.class.name+':'+ti.id,property:'orgLinks',recip_prop:'title']}" />
        --}%
            <h3 class="ui header">${message(code: 'title.show.history.label')}</h3>

            <table class="ui celled la-table table">
              <thead>
                <tr>
                  <th>${message(code: 'default.date.label')}</th>
                  <th>${message(code: 'title.show.history.from')}</th>
                  <th>${message(code: 'title.show.history.to')}</th>
                </tr>
              </thead>
              <tbody>
                <g:each in="${titleHistory}" var="th">
                  <tr>
                    <td><g:formatDate date="${th.eventDate}" formatName="default.date.format.notime"/></td>
                    <td>
                      <g:each in="${th.participants}" var="p">
                        <g:if test="${p.participantRole=='from'}">
                          <g:link controller="title" action="show" id="${p.participant.id}"><span style="<g:if test="${p.participant.id == ti.id}">font-weight:bold</g:if>">${p.participant.title}</span></g:link><br/>
                        </g:if>
                      </g:each>
                    </td>
                    <td>
                      <g:each in="${th.participants}" var="p">
                        <g:if test="${p.participantRole=='to'}">
                          <g:link controller="title" action="show" id="${p.participant.id}"><span style="<g:if test="${p.participant.id == ti.id}">font-weight:bold</g:if>">${p.participant.title}</span></g:link><br/>
                        </g:if>
                      </g:each>
                    </td>
                  </tr>
                </g:each>
              </tbody>
            </table>
            <g:if test="${ti.getIdentifierValue('originediturl') != null}">
              <span class="la-float-right">
                ${message(code: 'title.show.gokb')} <a href="${ti.getIdentifierValue('originediturl')}">GOKb</a>.
              </span>
            </g:if>

  <h3 class="ui icon header la-clear-before la-noMargin-top"><g:message code="title.edit.tipp"/>
  <semui:totalNumber total="${ti.tipps.size()}"/>
  </h3>
%{--<% /*
              <table class="ui celled la-table table">
                  <thead>
                  <tr>
                      <th><g:message code="tipp.startDate"/></th>
                      <th><g:message code="tipp.startVolume"/></th>
                      <th><g:message code="tipp.startIssue"/></th>
                      <th><g:message code="tipp.endDate"/></th>
                      <th><g:message code="tipp.endVolume"/></th>
                      <th><g:message code="tipp.endIssue"/></th>
                      <th><g:message code="tipp.coverageDepth"/></th>
                      <th><g:message code="tipp.platform"/></th>
                      <th><g:message code="package.label"/></th>
                      <th class="la-action-info"><g:message code="default.actions.label"/></th>
                  </tr>
                  </thead>
                  <tbody>
                  <g:each in="${ti.tipps}" var="t">
                      <tr>
                          <td><g:formatDate format="<g:message code="default.date.format.notime"/>" date="${t.startDate}"/></td>
                          <td>${t.startVolume}</td>
                          <td>${t.startIssue}</td>
                          <td><g:formatDate format="${message(code:'default.date.format.notime')}" date="${t.endDate}"/></td>
                          <td>${t.endVolume}</td>
                          <td>${t.endIssue}</td>
                          <td>${t.coverageDepth}</td>
                          <td><g:link controller="platform" action="show" id="${t.platform.id}">${t.platform.name}</g:link></td>
                          <td><g:link controller="package" action="show" id="${t.pkg.id}">${t.pkg.name}</g:link></td>
                          <td><g:link controller="tipp" action="show" id="${t.id}"><g:message code="title.edit.tipp.show"/></g:link></td>
                      </tr>
                  </g:each>
                  </tbody>
              </table>
*/ %>

            --}%%{--<g:form id="${params.id}" controller="title" action="batchUpdate" class="ui form"> BULK_REMOVE --}%%{--
              <table class="ui celled la-rowspan table">
                  <thead>
                    <tr>
                  --}%%{--<th rowspan="2"></th> BULK_REMOVE --}%%{--
                      <th><g:message code="tipp.platform"/></th>
                      <th><g:message code="package.label"/></th>
                      <th><g:message code="tipp.start"/></th>
                      <th><g:message code="tipp.end"/></th>
                      <th><g:message code="tipp.start"/></th>
                      <th><g:message code="tipp.end"/></th>
                      <th><g:message code="tipp.coverageDepth"/></th>
                      <th class="la-action-info"><g:message code="default.actions.label"/></th>
                    </tr>
                    <tr>
                      <th colspan="6"><g:message code="tipp.coverageNote"/></th>
                    </tr>
                  </thead>

                --}%%{-- BULK_REMOVE
                <g:if test="${editable}">
                  <tr>
                    <td rowspan="2"><input type="checkbox" name="checkall" onClick="javascript:$('.bulkcheck').attr('checked', true);"/></td>
                    <td colspan="2"><button class="ui button" type="submit" value="Go" name="BatchEdit"><g:message code="default.button.apply_batch.label"/></button></td>
                    <td>

                        <semui:datepicker label="default.date.label" id="bulk_start_date" name="bulk_start_date" value="${params.bulk_start_date}" />
                       - <input type="checkbox" id="clear_start_date" name="clear_start_date"/> (<g:message code="title.edit.tipp.clear"/>)

                        <div class="field">
                            <label><g:message code="tipp.volume"/></label>
                            <semui:simpleHiddenValue id="bulk_start_volume" name="bulk_start_volume"/>
                            - <input type="checkbox" name="clear_start_volume"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>

                        <div class="field">
                            <label><g:message code="tipp.issue"/></label>
                            <semui:simpleHiddenValue id="bulk_start_issue" name="bulk_start_issue"/>
                            - <input type="checkbox" name="clear_start_issue"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>
                    </td>
                    <td>

                        <semui:datepicker label="default.date.label" id="bulk_end_date" name="bulk_end_date" value="${params.bulk_end_date}" />
                       - <input type="checkbox" id="clear_end_date" name="clear_end_date"/> (<g:message code="title.edit.tipp.clear"/>)

                        <br/>

                        <div class="field">
                            <label><g:message code="tipp.volume"/></label>
                            <semui:simpleHiddenValue id="bulk_end_volume" name="bulk_end_volume"/>
                            - <input type="checkbox" name="clear_end_volume"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>

                        <div class="field">
                            <label><g:message code="tipp.issue"/></label>
                            <semui:simpleHiddenValue id="bulk_end_issue" name="bulk_end_issue"/>
                            - <input type="checkbox" name="clear_end_issue"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>

                    </td>
                    <td>
                        <div class="field">
                            <label>&nbsp;</label>
                            <semui:simpleHiddenValue id="bulk_coverage_depth" name="bulk_coverage_depth"/>
                            - <input type="checkbox" name="clear_coverage_depth"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>
                    </td>
                    <td/>
                  </tr>
                  <tr>
                    <td colspan="6">
                        <div class="field">
                            <label><g:message code="title.edit.tipp.bulk_notes_change"/></label>
                            <semui:simpleHiddenValue id="bulk_coverage_note" name="bulk_coverage_note"/>
                            - <input type="checkbox" name="clear_coverage_note"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>
                        <div class="field">
                            <label><g:message code="title.edit.tipp.bulk_platform_change"/></label>
                            <semui:simpleHiddenValue id="bulk_hostPlatformURL" name="bulk_hostPlatformURL"/>
                            - <input type="checkbox" name="clear_hostPlatformURL"/> (<g:message code="title.edit.tipp.clear"/>)
                        </div>
                    </td>
                  </tr>
                </g:if>
                --}%%{--

                <g:each in="${ti.tipps}" var="t">
                  <tr>
                    --}%%{--<td rowspan="2"><g:if test="${editable}"><input type="checkbox" name="_bulkflag.${t.id}" class="bulkcheck"/></g:if></td> BULK_REMOVE --}%%{--
                    <td><g:link controller="platform" action="show" id="${t.platform.id}">${t.platform.name}</g:link></td>
                    <td>
                        <div class="la-flexbox">
                            <i class="icon gift scale la-list-icon"></i>
                            <g:link controller="package" action="show" id="${t.pkg.id}">${t.pkg.name}</g:link>
                        </div>
                    </td>

                    <td><g:message code="default.date.label"/>: <g:formatDate format="${message(code:'default.date.format.notime')}" date="${t.startDate}"/><br/>
                    <g:message code="tipp.volume"/>: ${t.startVolume}<br/>
                    <g:message code="tipp.issue"/>: ${t.startIssue}</td>
                    <td><g:message code="default.date.label"/>: <g:formatDate format="${message(code:'default.date.format.notime')}" date="${t.endDate}"/><br/>
                    <g:message code="tipp.volume"/>: ${t.endVolume}<br/>
                    <g:message code="tipp.issue"/>: ${t.endIssue}</td>
                    <td>${t.coverageDepth}</td>
                    <td><g:link controller="tipp" action="show" id="${t.id}"><g:message code="title.edit.tipp.show"/></g:link></td>
                  </tr>
                  <tr>
                    <td colspan="6"><g:message code="tipp.coverageNote"/>: ${t.coverageNote?:"${message(code:'title.edit.tipp.no_note')}"}<br/>
                                    <g:message code="tipp.hostPlatformURL"/>:" ${t.hostPlatformURL?:"${message(code:'title.edit.tipp.no_url')"}</td>
                  </tr>
                </g:each>
              </table>
            --}%%{--</g:form> BULK_REMOVE--}%

            %{--NEW VIEW FOR TIPPS--}%

  <table class="ui sortable celled la-table table la-ignore-fixed la-bulk-header">
      <thead>
      <tr>
          <th></th>
          <th><g:message code="package"/></th>
          <th class="two wide">${message(code: 'tipp.coverage')}</th>
          <th class="two wide">${message(code: 'tipp.access')}</th>
      </tr>
      <tr>
          <th colspan="2" rowspan="2"></th>
          <th><g:message code="default.from"/></th>
          <th><g:message code="default.from"/></th>
      </tr>
      <tr>
          <th><g:message code="default.to"/></th>
          <th><g:message code="default.to"/></th>
      </tr>
      </thead>
      <tbody>

      <g:set var="counter" value="${1}" />
      <g:each in="${ti.tipps.sort{it?.pkg?.name}}" var="t">
          <tr>
              <td>${counter++}</td>
              <td>


                  <div class="la-inline-flexbox la-popup-tooltip la-delay">
                      <i class="icon gift scale la-list-icon"></i>
                      <g:link controller="package" action="show" id="${t?.pkg?.id}">${t?.pkg?.name}</g:link>
                  </div>
                  <g:if test="${t.hostPlatformURL}">
                      <a role="button" class="ui icon tiny blue button la-js-dont-hide-button la-popup-tooltip la-delay"
                         data-content="${t.platform.name}"
                         href="${t.hostPlatformURL.contains('http') ? t.hostPlatformURL :'http://'+t.hostPlatformURL}"
                         target="_blank">
                          <i class="cloud icon"></i>
                      </a><br>
                  </g:if>

                  <g:each in="${t?.title?.ids?.sort{it?.ns?.ns}}" var="id">
                      <span class="ui small blue image label">
                          ${id.ns.ns}: <div class="detail">${id.value}</div>
                      </span>
                  </g:each>
                  <div class="la-icon-list">
                     %{-- <g:if test="${t.availabilityStatus?.getI10n('value')}">
                          <div class="item">
                              <i class="grey key icon la-popup-tooltip la-delay" data-content="${message(code: 'default.access.label')}"></i>
                              <div class="content">
                                  ${t.availabilityStatus?.getI10n('value')}
                              </div>
                          </div>
                      </g:if>--}%

                      <div class="item">
                          <i class="grey clipboard check clip icon la-popup-tooltip la-delay" data-content="${message(code: 'default.status.label')}"></i>
                          <div class="content">
                              <semui:xEditableRefData owner="${t}" field="status" config="${de.laser.helper.RDConstants.TIPP_STATUS}"/>
                          </div>
                      </div>

                      <g:if test="${t?.platform.name}">
                          <div class="item">
                              <i class="grey icon cloud la-popup-tooltip la-delay" data-content="${message(code:'tipp.tooltip.changePlattform')}"></i>
                              <div class="content">
                                <g:if test="${t?.platform.name}">
                                    <g:link controller="platform" action="show"
                                          id="${t?.platform.id}">
                                      ${t?.platform.name}
                                    </g:link>
                                </g:if>
                                <g:else>
                                    ${message(code: 'default.unknown')}
                                </g:else>
                              </div>
                          </div>
                      </g:if>
                      <g:if test="${t?.id}">
                          <div class="la-title">${message(code: 'default.details.label')}</div>
                          <g:link class="ui icon tiny blue button la-js-dont-hide-button la-popup-tooltip la-delay"
                                  data-content="${message(code: 'laser')}"
                                  target="_blank"
                                  controller="tipp" action="show"
                                  id="${t.id}">
                              <i class="book icon"></i>
                          </g:link>
                      </g:if>

                      <g:each in="${ApiSource.findAllByTypAndActive(ApiSource.ApiTyp.GOKBAPI, true)}"
                              var="gokbAPI">
                          <g:if test="${t?.gokbId}">
                              <a role="button" class="ui icon tiny blue button la-js-dont-hide-button la-popup-tooltip la-delay"
                                 data-content="${message(code: 'gokb')}"
                                 href="${gokbAPI.editUrl ? gokbAPI.editUrl + '/gokb/resource/show/' + t?.gokbId : '#'}"
                                 target="_blank"><i class="la-gokb  icon"></i>
                              </a>
                          </g:if>
                      </g:each>
                  </div>
              </td>

              <td>
                  <g:each in="${t.coverages}" var="covStmt">
                      <p>
                          <!-- von -->
                          <g:formatDate format="${message(code:'default.date.format.notime')}" date="${covStmt.startDate}"/><br>
                          <i class="grey fitted la-books icon la-popup-tooltip la-delay" data-content="${message(code:'tipp.volume')}"></i>${covStmt.startVolume}<br>
                          <i class="grey fitted la-notebook icon la-popup-tooltip la-delay" data-content="${message(code:'tipp.issue')}"></i>${covStmt.startIssue}<br>
                          <semui:dateDevider/>
                          <!-- bis -->
                          <g:formatDate format="${message(code:'default.date.format.notime')}" date="${covStmt.endDate}"/><br>
                          <i class="grey fitted la-books icon la-popup-tooltip la-delay" data-content="${message(code:'tipp.volume')}"></i>${covStmt.endVolume}<br>
                          <i class="grey fitted la-notebook icon la-popup-tooltip la-delay" data-content="${message(code:'tipp.issue')}"></i>${covStmt.endIssue}<br>
                          <i class="grey icon quote right la-popup-tooltip la-delay" data-content="${message(code: 'tipp.coverageNote')}"></i>${covStmt.coverageNote}<br>
                          <i class="grey icon file alternate right la-popup-tooltip la-delay" data-content="${message(code: 'tipp.coverageDepth')}"></i>${covStmt.coverageDepth}<br>
                          <i class="grey icon hand paper right la-popup-tooltip la-delay" data-content="${message(code: 'tipp.embargo')}"></i>${covStmt.embargo}<br>
                      </p>
                  </g:each>
              </td>
              <td>
                  <!-- von -->
                  <semui:xEditable owner="${t}" type="date" field="accessStartDate" />
                  <semui:dateDevider/>
                  <!-- bis -->
                  <semui:xEditable owner="${t}" type="date" field="accessEndDate" />
              </td>
          </tr>

      </g:each>

      </tbody>

  </table>



            <br><br>

  <%--<r:script>

    $(function(){
      <g:if test="${editable}">

      $("#addOrgSelect").select2({
        placeholder: "Search for an org...",
        minimumInputLength: 1,
        formatInputTooShort: function () {
            return "<g:message code="select2.minChars.note"/>";
        },
        ajax: { // instead of writing the function to execute the request we use Select2's convenient helper
          url: "<g:createLink controller='ajax' action='lookup'/>",
          dataType: 'json',
          data: function (term, page) {
              return {
                  q: term, // search term
                  page_limit: 10,
                  baseClass:'${Org.class.name}'
              };
          },
          results: function (data, page) {
            return {results: data.values};
          }
        }
      });

      $("#orgRoleSelect").select2({
        placeholder: "Search for an role...",
        minimumInputLength: 1,
        formatInputTooShort: function () {
            return "<g:message code="select2.minChars.note"/>";
        },
        ajax: { // instead of writing the function to execute the request we use Select2's convenient helper
          url: "<g:createLink controller='ajax' action='lookup'/>",
          dataType: 'json',
          data: function (term, page) {
              return {
                  q: term, // search term
                  page_limit: 10,
                  baseClass:'${RefdataValue.class.name}'
              };
          },
          results: function (data, page) {
            return {results: data.values};
          }
        }
      });
  </g:if>
      });

      function validateAddOrgForm() {
        var org_name=$("#addOrgSelect").val();
        var role=$("#orgRoleSelect").val();

        if( !org_name || !role ){
          return false;
        }
        return true;
      }
  </r:script>--%>

  </body>
</html>
