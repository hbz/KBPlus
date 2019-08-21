<%@ page import="de.laser.interfaces.TemplateSupport; de.laser.helper.RDStore; com.k_int.kbplus.RefdataValue; com.k_int.kbplus.RefdataCategory" %>
<!doctype html>
<html>
  <head>
    <meta name="layout" content="semanticUI"/>
    <title>${message(code:'laser', default:'LAS:eR')} : ${message(code:'license.current', default:'Current Licenses')}</title>
  </head>
  <body>

  <laser:serviceInjection />

  <semui:breadcrumbs>
      <semui:crumb controller="myInstitution" action="dashboard" text="${institution?.getDesignation()}" />
      <semui:crumb message="license.current" class="active" />
  </semui:breadcrumbs>
  <semui:controlButtons>
      <semui:exportDropdown>
          <g:if test="${filterSet || defaultSet}">
              <semui:exportDropdownItem>
                  <g:link class="item js-open-confirm-modal" data-confirm-term-content = "${message(code: 'confirmation.content.exportPartial')}"
                          data-confirm-term-how="ok" action="currentLicenses" params="${params+[format:'csv']}">${message(code:'default.button.exports.csv')}</g:link>
              </semui:exportDropdownItem>
              <semui:exportDropdownItem>
                  <g:link class="item js-open-confirm-modal" data-confirm-term-content = "${message(code: 'confirmation.content.exportPartial')}"
                          data-confirm-term-how="ok" action="currentLicenses" params="${params+[exportXLS:true]}">${message(code:'default.button.exports.xls')}</g:link>
              </semui:exportDropdownItem>
              <g:each in="${transforms}" var="transkey,transval">
                  <semui:exportDropdownItem>
                      <g:link class="item js-open-confirm-modal" data-confirm-term-content = "${message(code: 'confirmation.content.exportPartial')}"
                              data-confirm-term-how="ok" action="currentLicenses" params="${params+[format:'xml',transformId:transkey,format_content:'subie']}">${transval.name}</g:link>
                  </semui:exportDropdownItem>
              </g:each>
          </g:if>
          <g:else>
              <semui:exportDropdownItem>
                  <g:link class="item" action="currentLicenses" params="${params+[format:'csv']}">${message(code:'default.button.exports.csv')}</g:link>
              </semui:exportDropdownItem>
              <semui:exportDropdownItem>
                  <g:link class="item" action="currentLicenses" params="${params+[exportXLS:true]}">${message(code:'default.button.exports.xls')}</g:link>
              </semui:exportDropdownItem>
              <g:each in="${transforms}" var="transkey,transval">
                  <semui:exportDropdownItem>
                      <g:link class="item" action="currentLicenses" params="${params+[format:'xml',transformId:transkey,format_content:'subie']}">${transval.name}</g:link>
                  </semui:exportDropdownItem>
              </g:each>
          </g:else>
      </semui:exportDropdown>

      <g:if test="${accessService.checkPermX('ORG_INST,ORG_CONSORTIUM', 'ROLE_ADMIN')}">
         <g:render template="actions" />
      </g:if>

  </semui:controlButtons>

  <semui:messages data="${flash}" />

  <h1 class="ui left aligned icon header"><semui:headerIcon />${message(code:'license.current')}
      <semui:totalNumber total="${licenseCount}"/>
  </h1>

    <semui:filter class="license-searches">
        <form class="ui form">
            <div class="four fields">

                <div class="field">
                    <label>${message(code:'license.search.by_ref', default:'Search by Reference')}</label>
                    <input type="text" name="keyword-search" placeholder="${message(code:'default.search.ph', default:'enter search term...')}" value="${params['keyword-search']?:''}" />
                </div>

                <div class="field">
                    <semui:datepicker label="license.valid_on" id="validOn" name="validOn" placeholder="default.date.label" value="${validOn}" />
                </div>
                <%--
                <div class="field">
                    <label>${message(code:'license.property.search')}</label>
                    <div class="two fields">
                        <g:select class="ui dropdown selection" id="availablePropertyTypes" name="availablePropertyTypes" from="${custom_prop_types}" optionKey="value" optionValue="key" value="${params.propertyFilterType}"/>
                        <input class="ui dropdown selection" id="propertyFilter" type="text" name="propertyFilter" placeholder="${message(code:'license.search.property.ph', default:'property value...')}" value="${params.propertyFilter?:''}" />
                        <input type="hidden" id="propertyFilterType" name="propertyFilterType" value="${params.propertyFilterType}"/>
                    </div>
                </div>
            </div><!--.fields-->
            <div class="fields">

                <div class="field">
                    <label>&nbsp;</label>
                    <a href="${request.forwardURI}" class="ui button">${message(code:'default.button.filterreset.label')}</a>
                </div> --%>

                <%
                    def fakeList = []
                    fakeList.addAll(RefdataCategory.getAllRefdataValues('License Status'))
                    fakeList.remove(RefdataValue.getByValueAndCategory('Deleted', 'License Status'))
                %>

                <div class="field">
                    <label>${message(code: 'license.status')}</label>
                    <laser:select class="ui fluid dropdown" name="status"
                                  from="${ fakeList }"
                                  optionKey="id"
                                  optionValue="value"
                                  value="${params.status}"
                                  noSelection="${['' : message(code:'default.select.choose.label')]}"/>
                </div>
                <g:render template="../templates/properties/genericFilter" model="[propList: propList]"/>
            </div>


            <g:if test="${accessService.checkPerm("ORG_CONSORTIUM")}">

                <div class="two fields">
                    <div class="field">
                        <label>${message(code: 'myinst.currentSubscriptions.filter.filterForRole.label')}</label>

                        <div class="inline fields la-filter-inline">
                            <div class="field">
                                <div class="ui radio checkbox">
                                    <input id="radioLicensee" type="radio" value="Licensee" name="orgRole" tabindex="0" class="hidden"
                                           <g:if test="${params.orgRole == 'Licensee'}">checked=""</g:if>
                                    >
                                    <label for="radioLicensee">${message(code: 'subscription.details.members.label')}</label>
                                </div>
                            </div>

                            <div class="field">
                                <div class="ui radio checkbox">
                                    <input id="radioKonsortium" type="radio" value="Licensing Consortium" name="orgRole" tabindex="0" class="hidden"
                                           <g:if test="${params.orgRole == 'Licensing Consortium'}">checked=""</g:if>
                                    >
                                    <label for="radioKonsortium">${message(code: 'myinst.currentSubscriptions.filter.consortium.label')}</label>
                                </div>
                            </div>
                        </div>
                    </div><!--.field-->

            </g:if>

                    <div class="field la-field-right-aligned">
                        <a href="${request.forwardURI}" class="ui reset primary primary button">${message(code:'default.button.reset.label')}</a>
                        <input type="hidden" name="filterSet" value="true" />
                        <input type="submit" class="ui secondary button" value="${message(code:'default.button.filter.label', default:'Filter')}" />
                    </div>

            <g:if test="${accessService.checkPerm("ORG_CONSORTIUM")}">
                </div><!--.two fields-->
            </g:if>

        </form>
    </semui:filter>

        <div class="license-results">
          <table class="ui sortable celled la-table table">
            <thead>
              <tr>
                  <th>${message(code:'sidewide.number')}</th>
                <g:sortableColumn params="${params}" property="reference" title="${message(code:'license.slash.name')}" />
                <g:if test="${params.orgRole == 'Licensee'}">
                    <th>${message(code:'license.licensor.label', default:'Licensor')}</th>
                </g:if>
                  <g:if test="${params.orgRole == 'Licensing Consortium'}">
                      <th>${message(code:'license.details.incoming.childs')}</th>
                  </g:if>
                  <th>${message(code:'license.status')}</th>
                <g:sortableColumn params="${params}" property="startDate" title="${message(code:'license.start_date', default:'Start Date')}" />
                <g:sortableColumn params="${params}" property="endDate" title="${message(code:'license.end_date', default:'End Date')}" />
                  <th class="la-action-info">${message(code:'default.actions')}</th>
              </tr>
            </thead>
            <tbody>
              <g:each in="${licenses}" var="l" status="jj">
                <tr>
                    <td>${ (params.int('offset') ?: 0)  + jj + 1 }</td>
                  <td>
                    <g:link action="show" controller="license" id="${l.id}">
                      ${l.reference?:message(code:'missingLicenseReference', default:'** No License Reference Set **')}
                    </g:link>
                    <g:if test="${l.subscriptions && ( l.subscriptions.size() > 0 )}">
                        <g:each in="${l.subscriptions.sort{it.name}}" var="sub">
                          <g:if test="${sub.status?.value == 'Current'}">
                                  <g:if test="${institution?.id in sub.orgRelations?.org?.id || accessService.checkPerm("ORG_CONSORTIUM")}">
                                  <div class="la-flexbox">
                                      <i class="icon clipboard outline outline la-list-icon"></i>
                                      <g:link controller="subscription" action="show" id="${sub.id}">${sub.name}</g:link><br/>
                                  </div>
                                  </g:if>
                          </g:if>
                        </g:each>
                    </g:if>
                    <g:else>
                      <br/>${message(code:'myinst.currentLicenses.no_subs', default:'No linked subscriptions.')}
                    </g:else>
                  </td>

                    <g:if test="${params.orgRole == 'Licensee'}">
                        <td>
                            ${l.licensor?.name}
                        </td>
                    </g:if>
                    <g:if test="${params.orgRole == 'Licensing Consortium'}">
                        <td>
                            <g:each in="${com.k_int.kbplus.License.findAllWhere(instanceOf: l)}" var="lChild">

                                <g:link controller="license" action="show" id="${lChild.id}">
                                    ${lChild}
                                </g:link>
                                <br/>

                            </g:each>
                        </td>
                    </g:if>
                  <td>${l.status.getI10n('value')}</td>
                  <td><g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${l.startDate}"/></td>
                  <td><g:formatDate format="${message(code:'default.date.format.notime', default:'yyyy-MM-dd')}" date="${l.endDate}"/></td>
                  <td class="x">
                    <g:if test="${editable && accessService.checkPerm('ORG_INST,ORG_CONSORTIUM')}">
                        %{-- bug: erms-459
                        <span data-position="top right" data-tooltip="${message(code:'license.details.copy.tooltip')}">
                            <g:link controller="myInstitution" action="actionLicenses" params="${[baselicense:l.id, 'copy-license':'Y']}" class="ui icon button">
                                <i class="copy icon"></i>
                            </g:link>
                        </span>
                        --}%
                        <span data-position="top right" data-tooltip="${message(code:'license.details.copy.tooltip')}">
                        <g:link controller="myInstitution" action="copyLicense" params="${[id:l.id]}" class="ui icon button">
                            <i class="copy icon"></i>
                        </g:link>
                        </span>
                        <%
                            boolean isLicTenant = false
                            if(l.getCalculatedType() == TemplateSupport.CALCULATED_TYPE_CONSORTIAL && l.getLicensingConsortium().id == institution.id) {
                                isLicTenant = true
                            }
                            else if(l.getCalculatedType() == TemplateSupport.CALCULATED_TYPE_LOCAL && orgRoles.get(l) in [RDStore.OR_LICENSEE]) {
                                isLicTenant = true
                            }
                        %>
                </g:if>
              </td>
            </tr>
          </g:each>
        </tbody>
      </table>
    </div>

      <semui:paginate action="currentLicenses" controller="myInstitution" params="${params}" next="${message(code:'default.paginate.next', default:'Next')}" prev="${message(code:'default.paginate.prev', default:'Prev')}" max="${max}" total="${licenseCount}" />

<%--
<r:script>

    $('.license-results input[type="radio"]').click(function () {
        $('.license-options').slideDown('fast');
    });

    function availableTypesSelectUpdated(optionSelected){

      var selectedOption = $( "#availablePropertyTypes option:selected" )

      var selectedValue = selectedOption.val()

      //Set the value of the hidden input, to be passed on controller
      $('#propertyFilterType').val(selectedOption.text())

      updateInputType(selectedValue)
    }

    function updateInputType(selectedValue){
      //If we are working with RefdataValue, grab the values and create select box
      if(selectedValue.indexOf("RefdataValue") != -1){
        var refdataType = selectedValue.split("&&")[1]
        $.ajax({ url:'<g:createLink controller="ajax" action="sel2RefdataSearch"/>'+'/'+refdataType+'?format=json',
                    success: function(data) {
                      var select = ' <select id="propertyFilter" name="propertyFilter" > '
                      //we need empty when we dont want to search by property
                      select += ' <option></option> '
                      for(var index=0; index < data.length; index++ ){
                        var option = data[index]
                        select += ' <option value="'+option.text+'">'+option.text+'</option> '
                      }
                      select += '</select>'
                      $('#propertyFilter').replaceWith(select)
                    },async:false
        });
      }else{
        //If we dont have RefdataValues,create a simple text input
        $('#propertyFilter').replaceWith('<input id="propertyFilter" type="text" name="propertyFilter" placeholder="${message(code:'license.search.property.ph', default:'property value')}" />')
      }
    }

    function setTypeAndSearch(){
      var selectedType = $("#propertyFilterType").val()
      //Iterate the options, find the one with the text we want and select it
      var selectedOption = $("#availablePropertyTypes option").filter(function() {
            return $(this).text() == selectedType ;
      }).prop('selected', true); //This will trigger a change event as well.


      //Generate the correct select box
      availableTypesSelectUpdated(selectedOption)

      //Set selected value for the actual search
      var paramPropertyFilter = "${params.propertyFilter}";
      var propertyFilterElement = $("#propertyFilter");
      if(propertyFilterElement.is("input")){
        propertyFilterElement.val(paramPropertyFilter);
      }else{
          $("#propertyFilter option").filter(function() {
            return $(this).text() == paramPropertyFilter ;
          }).prop('selected', true);
      }
    }

    $('#availablePropertyTypes').change(function(e) {
      var optionSelected = $("option:selected", this);
      availableTypesSelectUpdated(optionSelected);
    });

    $('.license-options .delete-license').click(function () {
        $('.license-results input:checked').each(function () {
            $(this).parent().parent().fadeOut('slow');
            $('.license-options').slideUp('fast');
        })
    })
    window.onload = setTypeAndSearch()
</r:script>
--%>

  </body>
</html>
