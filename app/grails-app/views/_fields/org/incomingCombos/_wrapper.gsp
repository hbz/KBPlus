<laser:serviceInjection/>
<div class="control-group">
  <label class="control-label" for="incomingCombos">${message(code:'org.incomingCombos.label')}</label>
  <div class="controls">
    <g:if test="${orgInstance.incomingCombos && orgInstance.incomingCombos.size() > 0}">
      <ul>
        <g:each in="${orgInstance.incomingCombos}" var="ic">
          <li><g:link controller="organisation" action="show" id="${ic.fromOrg.id}">${ic.fromOrg.name}</g:link></li>
        </g:each>
      </ul>
    </g:if>
    <g:if test="${accessService.checkPerm("ORG_CONSORTIUM")}">
        <g:form name="addIncomingCombo" controller="organisation" action="addOrgCombo" class="form-search" method="get">
          <g:hiddenField name="toOrg" value="${orgInstance.id}" />
          <g:select name="fromOrg"
                    from="${com.k_int.kbplus.Org.executeQuery('from Org o where o.sector.value = :sv and o <> :org order by o.name', [sv: 'Higher Education', org: orgInstance])}"
                    optionKey="id"
                    optionValue="name"
                    class="input-medium"/>
          <g:select name="comboTypeTo"
                    from="${de.laser.RefdataCategory.getAllRefdataValues('Combo Type')}"
                    optionKey="id"
                    optionValue="${{it.getI10n('value')}}"
                    class="input-medium"/>
          <button id="submitAICForm"
                  data-complete-text="Add Org Combo"
                  type="submit"
                  class="ui button">
            ${message(code: 'default.add.label', args:[message(code:'combo.label')])}
          </button>
        </g:form>
    </g:if>
  </div>
</div>
