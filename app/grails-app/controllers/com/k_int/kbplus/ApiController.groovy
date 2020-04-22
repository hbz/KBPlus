package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import de.laser.ContextService
import de.laser.api.v0.ApiManager
import de.laser.api.v0.ApiReader
import de.laser.api.v0.ApiToolkit
import de.laser.helper.Constants
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import java.text.SimpleDateFormat

@Secured(['permitAll']) // TODO
class ApiController {

    def springSecurityService
    ContextService contextService
    ApiService apiService

    ApiController(){
        super()
    }

    // @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    def index() {
        log.debug("API")
        Map<String, Object> result = fillRequestMap(params)

        switch ( (params.version ?: 'v0').toLowerCase() ) {
            default:
                result.apiVersion = 'v0'
                render view: 'v0', model: result
                break
        }
    }

    def loadSpecs() {
        Map<String, Object> result = fillRequestMap(params)

        switch ( (params.version ?: 'v0').toLowerCase() ) {
            default:
                result.apiVersion = 'v0'
                render view: '/swagger/v0/laser.yaml.gsp', model: result
                break
        }
    }

    def loadChangelog() {
        Map<String, Object> result = fillRequestMap(params)

        switch ( (params.version ?: 'v0').toLowerCase() ) {
            default:
                result.apiVersion = 'v0'
                render view: '/swagger/v0/changelog.md.gsp', model: result
                break
        }
    }

    def dispatch() {
        switch ( (params.version ?: 'v0').toLowerCase() ) {
            default:
                v0()
                break
        }
    }

    private Map<String, Object> fillRequestMap (params) {
        Map<String, Object> result = [:]
        User user
        Org org

        if (springSecurityService.isLoggedIn()) {
            user = User.get(springSecurityService.principal.id)
            org = contextService.getOrg()
        }

        def apiKey = OrgSettings.get(org, OrgSettings.KEYS.API_KEY)
        def apiPass = OrgSettings.get(org, OrgSettings.KEYS.API_PASSWORD)

        result.apiKey       = (apiKey != OrgSettings.SETTING_NOT_FOUND) ? apiKey.getValue() : ''
        result.apiPassword  = (apiPass != OrgSettings.SETTING_NOT_FOUND) ? apiPass.getValue() : ''
        result.apiContext   = org?.globalUID ?: ''

        result
    }

    @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    @Deprecated
    def uploadBibJson() {
        Map<String, Object> result = [:]
        log.debug("uploadBibJson");
        log.debug("Auth request from ${request.getRemoteAddr()}");
        if (request.getRemoteAddr() == '127.0.0.1') {
            if (request.method.equalsIgnoreCase("post")) {
                result.message = "Working...";
                def candidate_identifiers = []
                request.JSON.identifier.each { i ->
                    if (i.type == 'ISSN' || i.type == 'eISSN' || i.type == 'DOI') {
                        candidate_identifiers.add([namespace: i.type, value: i.id]);
                    }
                }
                if (candidate_identifiers.size() > 0) {
                    log.debug("Lookup using ${candidate_identifiers}");
                    TitleInstance title = TitleInstance.findByIdentifier(candidate_identifiers)
                    if (title != null) {
                        log.debug("Located title ${title}  Current identifiers: ${title.ids}");
                        result.matchedTitleId = title.id
                        if (title.getIdentifierValue('jusp') != null) {
                            result.message = "jusp ID already present against title";
                        } else {
                            log.debug("Adding jusp Identifier to title");
                            def jid = request.JSON.identifier.find { it.type == 'jusp' }
                            log.debug("Add identifier identifier ${jid}");
                            if (jid != null) {
                                result.message = "Adding jusp ID ${jid.id}to title";
                                // TODO [ticket=1789]
                                //def new_jusp_id = Identifier.lookupOrCreateCanonicalIdentifier('jusp', "${jid.id}");
                                //def new_io = new IdentifierOccurrence(identifier: new_jusp_id, ti: title).save(flush: true);
                                def new_jusp_id = Identifier.construct([value: "${jid.id}", reference: title, namespace: 'jusp'])
                            } else {
                                result.message = "Unable to locate JID in BibJson record";
                            }
                        }
                    } else {
                        result.message = "Unable to locate title on matchpoints : ${candidate_identifiers}";
                    }
                } else {
                    result.message = "No matchable identifiers. ${request.JSON.identifier}";
                }

            } else {
                result.message = "non post";
            }
        } else {
            result.message = "uploadBibJson only callable from 127.0.0.1";
        }
        render result as JSON
    }

    /*
    // Assert a core status against a title/institution. Creates TitleInstitutionProvider objects
    // For all known combinations.
    @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    @Deprecated
    def assertCore() {
        // Params:     inst - [namespace:]code  Of an org [mandatory]
        //            title - [namespace:]code  Of a title [mandatory]
        //         provider - [namespace:]code  Of an org [optional]
        log.debug("assertCore(${params})");
        Map<String, Object> result = [:]
        if (request.getRemoteAddr() == '127.0.0.1') {
            if ((params.inst?.length() > 0) && (params.title?.length() > 0)) {
                Org inst = Org.lookupByIdentifierString(params.inst);
                TitleInstance title = TitleInstance.lookupByIdentifierString(params.title);
                def provider = params.provider ? Org.lookupByIdentifierString(params.provider) : null;
                def year = params.year?.trim()

                log.debug("assertCore ${params.inst}:${inst} ${params.title}:${title} ${params.provider}:${provider}");

                if (title && inst) {

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

                    if (provider) {
                    } else {
                        log.debug("Calculating all known providers for this title");
                        def providers = TitleInstancePackagePlatform.executeQuery('''select distinct orl.org 
from TitleInstancePackagePlatform as tipp join tipp.pkg.orgs as orl
where tipp.title = ? and orl.roleType.value=?''', [title, 'Content Provider']);

                        providers.each {
                            log.debug("Title ${title} is provided by ${it}");
                            TitleInstitutionProvider tiinp = TitleInstitutionProvider.findByTitleAndInstitutionAndprovider(title, inst, it)
                            if (tiinp == null) {
                                log.debug("Creating new TitleInstitutionProvider");
                                tiinp = new TitleInstitutionProvider(title: title, institution: inst, provider: it).save(flush: true, failOnError: true)
                            }

                            log.debug("Got tiinp:: ${tiinp}");
                            Date startDate = sdf.parse("${year}-01-01T00:00:00");
                            Date endDate = sdf.parse("${year}-12-31T23:59:59");
                            tiinp.extendCoreExtent(startDate, endDate);
                        }
                    }
                }
            } else {
                result.message = "ERROR: missing mandatory parameter: inst or title";
            }
        } else {
            result.message = "ERROR: this call is only usable from within the KB+ system network"
        }
        render result as JSON
    }
     */

    /*
    // Accept a single mandatorty parameter which is the namespace:code for an institution
    // If found, return a JSON report of each title for that institution
    // Also accept an optional parameter esn [element set name] with values full of brief[the default]
    // Example:  http://localhost:8080/laser/api/institutionTitles?orgid=jusplogin:shu
    @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    @Deprecated
    def institutionTitles() {

        Map<String, Object> result = [:]
        result.titles = []

        if (params.orgid) {
            def name_components = params.orgid.split(':')
            if (name_components.length == 2) {
                // Lookup org by ID
                // TODO [ticket=1789]
                String orghql = "select distinct ident.org from Identifier ident where ident.org is not null and ident.ns.ns = ? and ident.value like ? )"
                // def orghql = "select org from Org org where exists ( select io from IdentifierOccurrence io, Identifier id, IdentifierNamespace ns where io.org = org and id.ns = ns and io.identifier = id and ns.ns = ? and id.value like ? )"
                List<Org> orgs = Org.executeQuery(orghql, [name_components[0], name_components[1]])
                if (orgs.size() == 1) {
                    Org org = orgs[0]

                    Date today = new Date()

                    // Find all TitleInstitutionProvider where institution = org
                    def titles = TitleInstitutionProvider.executeQuery('select tip.title.title, tip.title.id, count(cd) from TitleInstitutionProvider as tip left join tip.coreDates as cd where tip.institution = ? and cd.startDate < ? and cd.endDate > ?',
                            [org, today, today]);
                    titles.each { tip ->
                        result.titles.add([title: tip[0], tid: tip[1], isCore: tip[2]]);
                    }
                } else {
                    log.message = "Unable to locate Org with ID ${params.orgid}";
                }
            } else {
                result.message = "Invalid orgid. Format orgid as namespace:value, for example jusplogin:shu"
            }
        }

        render result as JSON
    }
     */

    @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    def importInstitutions() {
        log.info("import institutions via xml .. ROLE_API required")

        def xml = "(Code: 0) - Errare humanum est"
        def rawText = request.getReader().getText()

        if (request.method == 'POST') {

            if(rawText) {
                xml = new XmlSlurper().parseText(rawText)
                assert xml instanceof groovy.util.slurpersupport.GPathResult
                apiService.makeshiftOrgImport(xml)
            }
            else {
                xml = "(Code: 1) - Ex nihilo nihil fit"
            }
        }
        render xml
    }

    @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    def setupLaserData() {
        log.info("import institutions via xml .. ROLE_API required")

        def xml = "(Code: 0) - Errare humanum est"
        def rawText = request.getReader().getText()

        if (request.method == 'POST') {

            if(rawText) {
                xml = new XmlSlurper().parseText(rawText)
                assert xml instanceof groovy.util.slurpersupport.GPathResult
                apiService.setupLaserData(xml)
            }
            else {
                xml = "(Code: 1) - Ex nihilo nihil fit"
            }
        }
        render xml
    }

    @Secured(['ROLE_API', 'IS_AUTHENTICATED_FULLY'])
    def importSubscriptions() {
        log.info("import subscriptions via xml .. ROLE_API required")
        // TODO: in progress - erms-746
        def xml = "(Code: 0) - Errare humanum est"
        def rawText = request.getReader().getText()

        if (request.method == 'POST') {

            if(rawText) {
                xml = new XmlSlurper().parseText(rawText)
                assert xml instanceof groovy.util.slurpersupport.GPathResult
                apiService.makeshiftSubscriptionImport(xml)
            }
            else {
                xml = "(Code: 1) - Ex nihilo nihil fit"
            }
        }
        render xml
    }

    /**
     * API endpoint
     *
     * @return
     */
    @Secured(['permitAll']) // TODO
    def v0() {
        Org apiOrg = (Org) request.getAttribute('authorizedApiOrg')
        boolean debugMode = request.getAttribute('debugMode')

        log.debug("API Call [${apiOrg?.id}] - " + params)

        def result
        boolean hasAccess = false
        long startTimeMillis = System.currentTimeMillis()

        String obj     = params.get('obj')
        String query   = params.get('q', '')
        String value   = params.get('v', '')
        String context = params.get('context')
        String format

        Org contextOrg = null // TODO refactoring

        if (apiOrg) {
            // checking role permission
            def apiLevel = OrgSettings.get(apiOrg, OrgSettings.KEYS.API_LEVEL)

            if (apiLevel != OrgSettings.SETTING_NOT_FOUND) {
                if ("GET" == request.method) {
                    hasAccess = (apiLevel.getValue() in [ApiToolkit.API_LEVEL_READ, ApiToolkit.API_LEVEL_INVOICETOOL, ApiToolkit.API_LEVEL_DATAMANAGER])
                }
                else if ("POST" == request.method) {
                    hasAccess = (apiLevel.getValue() in [ApiToolkit.API_LEVEL_WRITE, ApiToolkit.API_LEVEL_INVOICETOOL, ApiToolkit.API_LEVEL_DATAMANAGER])
                }
            }

            // getting context (fallback)
            if (params.get('context')) {
                contextOrg = Org.findWhere(globalUID: params.get('context'))
            }
            else {
                contextOrg = apiOrg
            }
        }

        if (!contextOrg || !hasAccess) {
            result = Constants.HTTP_FORBIDDEN
        }
        else if (!obj) {
            result = Constants.HTTP_BAD_REQUEST
        }

        // delegate api calls

        if (! result) {
            if ('GET' == request.method) {
                if (! (query && value) && ! ApiReader.SIMPLE_QUERIES.contains(obj)) {
                    result = Constants.HTTP_BAD_REQUEST
                }
                else {
                    switch(request.getHeader('accept')) {
                        case Constants.MIME_APPLICATION_JSON:
                        case Constants.MIME_TEXT_JSON:
                            format = Constants.MIME_APPLICATION_JSON
                            break
                        case Constants.MIME_APPLICATION_XML:
                        case Constants.MIME_TEXT_XML:
                            format = Constants.MIME_APPLICATION_XML
                            break
                        case Constants.MIME_TEXT_PLAIN:
                            format = Constants.MIME_TEXT_PLAIN
                            break
                        default:
                            format = Constants.MIME_ALL
                            break
                    }

                    result = ApiManager.read(
                            (String) obj,
                            (String) query,
                            (String) value,
                            (Org) contextOrg,
                            format
                    )

                    if (result instanceof Doc) {
                        response.contentType = result.mimeType

                        if (result.contentType == Doc.CONTENT_TYPE_STRING) {
                            response.setHeader('Content-Disposition', 'attachment; filename="' + result.title + '"')
                            response.outputStream << result.content
                        }
                        else if (result.contentType == Doc.CONTENT_TYPE_BLOB) {
                            result.render(response, result.filename)
                        }
                        response.outputStream.flush()
                        return
                    }
                }
            }
            /*
            else if ('POST' == request.method) {
                def postBody = request.getAttribute("authorizedApiPostBody")
                def data = (postBody ? new JSON().parse(postBody) : null)

                if (! data) {
                    result = Constants.HTTP_BAD_REQUEST
                }
                else {
                    result = ApiManager.write((String) obj, data, (User) user, (Org) contextOrg)
                }
            }
            */
            else {
                result = Constants.HTTP_NOT_IMPLEMENTED
            }
        }
        Map<String, Object> respStruct = ApiManager.buildResponse(request, obj, query, value, context, contextOrg, result)

        JSON json   = (JSON) respStruct.json
        int status  = (int) respStruct.status

        String responseTime = ((System.currentTimeMillis() - startTimeMillis) / 1000).toString()

        response.setContentType(Constants.MIME_APPLICATION_JSON)
        response.setCharacterEncoding(Constants.UTF8)
        response.setHeader("Laser-Api-Version", ApiManager.VERSION.toString())
        response.setStatus(status)

        if (debugMode) {
            response.setHeader("Laser-Api-Debug-Mode", "true")
            response.setHeader("Laser-Api-Debug-Result-Length", json.toString().length().toString())
            response.setHeader("Laser-Api-Debug-Result-Time", responseTime)

            if (json.target instanceof List) {
                response.setHeader("Laser-Api-Debug-Result-Size", json.target.size().toString())
            }
        }

        if (json.target instanceof List) {
            log.debug("API Call [${apiOrg?.id}] - (Code: ${status}, Time: ${responseTime}, Items: ${json.target.size().toString()})")
        }
        else {
            log.debug("API Call [${apiOrg?.id}] - (Code: ${status}, Time: ${responseTime}, Length: ${json.toString().length().toString()})")
        }

        render json.toString(true)
    }
}