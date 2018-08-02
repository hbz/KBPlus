<!doctype html>

<%@ page import="java.text.SimpleDateFormat"%>
<%
  def addFacet = { params, facet, val ->
    def newparams = [:]
    newparams.putAll(params)
    def current = newparams[facet]
    if ( current == null ) {
      newparams[facet] = val
    }
    else if ( current instanceof String[] ) {
      newparams.remove(current)
      newparams[facet] = current as List
      newparams[facet].add(val);
    }
    else {
      newparams[facet] = [ current, val ]
    }
    newparams
  }

  def removeFacet = { params, facet, val ->
    def newparams = [:]
    newparams.putAll(params)
    def current = newparams[facet]
    if ( current == null ) {
    }
    else if ( current instanceof String[] ) {
      newparams.remove(current)
      newparams[facet] = current as List
      newparams[facet].remove(val);
    }
    else if ( current?.equals(val.toString()) ) {
      newparams.remove(facet)
    }
    newparams
  }

  def dateFormater = new SimpleDateFormat("yy-MM-dd'T'HH:mm:ss.SSS'Z'")
%>

<html>

  <head>
    <meta name="layout" content="public"/>
    <title>${message(code:'public.nav.exports.label', default:'Exports')} | ${message(code:'laser', default:'LAS:eR')}</title>
        <r:require module='annotations' />

  </head>


  <body class="public">

    <g:render template="public_navbar" contextPath="/templates" model="['active': 'publicExport']"/>

    <h1 class="ui header"><semui:headerIcon />${message(code:'public.nav.exports.label', default:'Exports')}</h1>


    <p xmlns:dct="http://purl.org/dc/terms/" xmlns:vcard="http://www.w3.org/2001/vcard-rdf/3.0#">
     <a rel="license"
        href="http://creativecommons.org/publicdomain/zero/1.0/">
       <img src="http://i.creativecommons.org/p/zero/1.0/88x31.png" style="border-style: none;" alt="CC0" />
     </a>
    </p>



          <div class="well">
            <h4 class="ui header">Cufts style index of subscriptions offered</h4>
            <p>
              Use the contents of this URI to drive a full crawl of the ${message(code:'laser', default:'LAS:eR')} subscriptions offered data. Each row gives an identifier that can be used to
              construct individual subscription requests.
            </p>
            <g:link action="idx" params="${[format:'csv']}">Simple CSV</g:link><br/>
            <g:link action="idx" params="${[format:'xml']}">XML</g:link><br/>
            <g:link action="idx" params="${[format:'json']}">JSON</g:link><br/>
          </div>


    <div>
      <g:form action="index" method="get" params="${params}">
      <input type="hidden" name="offset" value="${params.offset}"/>
      <g:if test="${params.startYear && params.endYear}">
        <input type="hidden" name="startYear" value="${params.startYear}"/>
        <input type="hidden" name="endYear" value="${params.endYear}"/>
      </g:if>
      <if test="${params.filter}">
        <input type="hidden" name="filter" value="${params.filter}"/>
      </if>

      <div class="row">
        <div class="span12">
        <ul class="nav nav-pills">
          <g:set var="active_filter" value="${params.filter}"/>
          <li class="${(active_filter != 'current')?'active':''}"><g:link action="index">${message(code:'package.show.all', default:'All Packages')}</g:link></li>

          <li class="${active_filter=='current'?'active':''}"><g:link action="index" params="${ [filter:'current',endYear:"[ ${new Date().year +1900} TO 2100]"]}">${message(code:'package.show.current', default:'Current Packages')}</g:link></li>


      </ul>
          <div class="well form-horizontal">
            ${message(code:'default.search.term', default:'Search Term')}: <input name="q" placeholder="${message(code:'packageDetails.index.search.ph')}" value="${params.q}"/>
            ${message(code:'default.sort.label', default:'Sort')}: <select name="sort">
                    <option ${params.sort=='sortname' ? 'selected' : ''} value="sortname">${message(code:'packageDetails.index.search.sort.name', default:'Package Name')}</option>
                    <option ${params.sort=='_score' ? 'selected' : ''} value="_score">${message(code:'packageDetails.index.search.sort.score', default:'Score')}</option>
                    <option ${params.sort=='lastModified' ? 'selected' : ''} value="lastModified">${message(code:'packageDetails.index.search.sort.modified', default:'Last Modified')}</option>
                  </select>
            ${message(code:'default.order.label', default:'Order')}: <select name="order" value="${params.order}">
                    <option ${params.order=='asc' ? 'selected' : ''} value="asc">${message(code:'default.asc', default:'Ascending')}</option>
                    <option ${params.order=='desc' ? 'selected' : ''} value="desc">${message(code:'default.desc', default:'Descending')}</option>
                  </select>
            ${message(code:'default.search.modified_after', default:'Modified After')}: <semui:simpleHiddenValue  id="lastUpdated" value="${params.lastUpdated}" name="lastUpdated" type="date"/>
 
            <button type="submit" name="search" value="yes">${message(code:'default.button.filter.label', default:'Filter')}</button>
          </div>
        </div>
      </div>
      </g:form>

      <p>
          <g:each in="${['type','endYear','startYear','consortiaName','cpname']}" var="facet">
            <g:each in="${params.list(facet)}" var="fv">
              <span class="badge alert-info">${facet}:${fv} &nbsp; <g:link controller="${controller}" action="index" params="${removeFacet(params,facet,fv)}"><i class="icon-remove icon-white"></i></g:link></span>
            </g:each>
          </g:each>
        </p>

      <div class="row">

  
        <div class="facetFilter span2">
          <g:each in="${facets.sort{it.key}}" var="facet">
            <g:if test="${facet.key != 'type'}">
            <div class="panel panel-default">
              <div class="panel-heading">
                <h5 class="ui header"><g:message code="facet.so.${facet.key}" default="${facet.key}" /></h5>
              </div>
              <div class="panel-body">
                <ul>
                  <g:each in="${facet.value.sort{it.display}}" var="v">
                    <li>
                      <g:set var="fname" value="facet:${facet.key+':'+v.term}"/>
 
                      <g:if test="${params.list(facet.key).contains(v.term.toString())}">
                        ${v.display} (${v.count})
                      </g:if>
                      <g:else>
                        <g:link controller="${controller}" action="${action}" params="${addFacet(params,facet.key,v.term)}">${v.display}</g:link> (${v.count})
                      </g:else>
                    </li>
                  </g:each>
                </ul>
              </div>
            </div>
            </g:if>
          </g:each>
        </div>


        <div class="span10">
          <div class="well">
             <g:if test="${hits}" >
                <div class="paginateButtons" style="text-align:center">
                  <g:if test=" ${params.int('offset')}">
                    ${message(code:'default.search.offset.text', args:[(params.int('offset') + 1),(resultsTotal < (params.int('max') + params.int('offset')) ? resultsTotal : (params.int('max') + params.int('offset'))),resultsTotal])}
                  </g:if>
                  <g:elseif test="${resultsTotal && resultsTotal > 0}">
                    ${message(code:'default.search.no_offset.text', args:[(resultsTotal < params.int('max') ? resultsTotal : params.int('max')),resultsTotal])}
                  </g:elseif>
                  <g:else>
                    ${message(code:'default.search.no_pagiantion.text', args:[resultsTotal])}
                  </g:else>
                </div>

                <div id="resultsarea">
                  <table class="ui sortable celled la-table table">
                    <thead>
                      <tr style="white-space: nowrap">
                      <g:sortableColumn property="sortname" title="${message(code:'package.show.pkg_name', default:'Package Name')}" params="${params}" />
                      <g:sortableColumn property="consortiaName" title="${message(code:'consortium.label', default:'Consortium')}" params="${params}"/>
                      <g:sortableColumn property="startDate" title="${message(code:'default.startDate.label', default:'Start Date')}" params="${params}" />
                      <g:sortableColumn property="endDate" title="${message(code:'default.endDate.label', default:'End Date')}" params="${params}" />
                      <g:sortableColumn property="lastModified" title="${message(code:'packageDetails.index.search.sort.modified', default:'Last Modified')}" params="${params}" />
                      <th>Export</th>
                    </thead>
                    <tbody>
                      <g:each in="${hits}" var="hit">
                        <tr>
                          <td>${hit.getSource().name}
                              <!--(${hit.score})-->
                              <span>(${hit.getSource().titleCount?:'Unknown number of'} ${message(code:'title.plural', default:'Titles')})</span>
                          <ul>
                          <g:each in="${hit.getSource().identifiers}" var="ident">
                            <li>${ident}</li>
                          </g:each>
                          </ul>
                          <td>${hit.getSource().consortiaName}</td>
                          <td>
                          <g:formatDate formatName="default.date.format.notime" date='${hit.getSource().startDate?dateFormater.parse(hit.getSource().startDate):null}'/>
                          </td>
                          <td>
                          <g:formatDate formatName="default.date.format.notime" date='${hit.getSource().endDate?
                            dateFormater.parse(hit.getSource().endDate):null}'/>
                          </td>
                          <td><g:formatDate formatName="default.date.format" date='${hit.getSource().lastModified?dateFormater.parse(hit.getSource().lastModified):null}'/> </td>
                          </td>
                          
                          <td>  
        <div class="dropdown">
            <a class="dropdown-toggle badge" data-toggle="dropdown" href="#">${message(code:'default.formats.label', default:'Formats')}<i class="fa fa-caret-down"></i> </a>
        <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
            <li><g:link action="pkg" params="${[format:'json',id:hit.getSource().dbId]}">JSON</g:link></li>  
            <li><g:link action="pkg" params="${[format:'xml',id: hit.getSource().dbId]}">XML Export</g:link></li>
            <g:each in="${transforms}" var="transkey,transval">
              <li><g:link action="pkg" params="${[format:'xml',transformId:transkey,mode:params.mode,id:hit.getSource().dbId]}"> ${transval.name}</g:link></li>
            </g:each>
            </ul>
        </div>
                        </td>
                        </tr>
                      </g:each>
                    </tbody>
                  </table>
                </div>
                <div class="paginateButtons" style="text-align:center">
                  <span><g:paginate controller="${controller}" action="index" params="${params}" next="Next" prev="Prev" total="${resultsTotal}" /></span>
            </g:if>
          </div>
          </div>
        </div>
      </div>
    </div>



  </body>

</html>
