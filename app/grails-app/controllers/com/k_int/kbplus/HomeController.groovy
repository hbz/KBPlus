package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import groovy.xml.MarkupBuilder

@Secured(['IS_AUTHENTICATED_FULLY'])
class HomeController {

    def springSecurityService
    def ESSearchService
 
    @Secured(['ROLE_USER'])
    def index() {
        Map<String, Object> result = [:]
        log.debug("HomeController::index - ${springSecurityService.principal.id}");

        result.user = User.get(springSecurityService.principal.id)
        if (result.user) {

            if (UserSettings.get(result.user, UserSettings.KEYS.DASHBOARD) == UserSettings.SETTING_NOT_FOUND) {
                flash.message = message(code: 'profile.dash.not_set')
                redirect(controller: 'profile', action: 'index')
                return
            }
            redirect(controller: 'myInstitution', action: 'dashboard')
        }
        else {
            log.error("Unable to lookup user for principal id :: ${springSecurityService.principal.id}");
        }
    }

  @Secured(['ROLE_USER'])
  def search() {
      redirect(controller: 'search', action: 'search')

    Map<String, Object> result = [:]
  
    result.user = springSecurityService.getCurrentUser()
    params.max = result.user.getDefaultPageSize()

    if (springSecurityService.isLoggedIn()) {
        params.sort = "name"
        result = ESSearchService.search(params)
    }  
    withFormat {
      html {
        render(view:'search',model:result)
      }
      rss {
        renderRSSResponse(result)
      }
      atom {
        renderATOMResponse( result,params.max )
      }
      xml {
        render result as XML
      }
      json {
        render result as JSON
      }
    }
  }

    private def renderRSSResponse(results) {

    def output_elements = buildOutputElements(results.hits)

    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.rss(version: '2.0') {
      channel {
        title("KBPlus")
        description("KBPlus")
        "opensearch:totalResults"(results.resultsTotal)
        // "opensearch:startIndex"(results.search_results.results.start)
        "opensearch:itemsPerPage"(10)
        output_elements.each { i ->  // For each record
          entry {
            i.each { tuple ->   // For each tuple in the record
              "${tuple[0]}"("${tuple[1]}")
            }
          }
        }
      }
    }

    render(contentType:"application/rss+xml", text: writer.toString())
  }


    private def renderATOMResponse(results, hpp) {

    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    def output_elements = buildOutputElements(results.hits)

    xml.feed(xmlns:'http://www.w3.org/2005/Atom') {
        // add the top level information about this feed.
        title("KBPlus")
        description("KBPlus")
        "opensearch:totalResults"(results.resultsTotal)
        // "opensearch:startIndex"(results.search_results.results.start)
        "opensearch:itemsPerPage"("${hpp}")
        // subtitle("Serving up my content")
        //id("uri:uuid:xxx-xxx-xxx-xxx")
        link(href:"http://a.b.c")
        author {
          name("KBPlus")
        }
        //updated sdf.format(new Date());

        // for each entry we need to create an entry element
        output_elements.each { i ->
          entry {
            i.each { tuple ->
                "${tuple[0]}"("${tuple[1]}")
            }
          }
        }
    }

    render(contentType:'application/xtom+xml', text: writer.toString())
  }

    private def buildOutputElements(searchresults) {
    // Result is an array of result elements
    def result = []

    searchresults.hits?.each { doc ->
      ////  log.debug("adding ${doc} ${doc.getSource().title}");
      def docinfo = [];

      docinfo.add(['dc.title',doc.getSource().title])
      docinfo.add(['dc.description',doc.getSource().description])
      docinfo.add(['dc.identifier',doc.getSource()._id])
      result.add(docinfo)
    }
    result
  }
}
