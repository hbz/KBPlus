package de.laser

import com.k_int.kbplus.*
import de.laser.helper.AppUtils
import de.laser.helper.DateUtil
import de.laser.helper.SwissKnife
import grails.transaction.Transactional
import grails.util.Holders
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

import java.text.SimpleDateFormat

@Transactional
class DataConsistencyService {

    def grailsApplication
    def springSecurityService
    def deletionService
    def g = Holders.grailsApplication.mainContext.getBean(ApplicationTagLib)

    @Deprecated
    //no need as field impId is going to be removed as of version 1.3, ticket ERMS-1929
    Map<String, Object> checkImportIds() {
        /*Map result = [
                Org: [],
                Package: [],
                Platform: [],
                TitleInstance: [],
                TitleInstancePackagePlatform: []
        ]

        result.Org = Org.executeQuery(
                'SELECT org.impId as impId, count(org.impId) as cnt FROM Org org GROUP BY org.impId ORDER BY org.impId'
        ).findAll{ it -> it[1] > 1}

        result.Package = Package.executeQuery(
                'SELECT pkg.impId as impId, count(pkg.impId) as cnt FROM Package pkg GROUP BY pkg.impId ORDER BY pkg.impId'
        ).findAll{ it -> it[1] > 1}

        result.Platform = Platform.executeQuery(
                'SELECT pf.impId as impId, count(pf.impId) as cnt FROM Platform pf GROUP BY pf.impId ORDER BY pf.impId'
        ).findAll{ it -> it[1] > 1}

        result.TitleInstance = TitleInstance.executeQuery(
                'SELECT ti.impId as impId, count(ti.impId) as cnt FROM TitleInstance ti GROUP BY ti.impId ORDER BY ti.impId'
        ).findAll{ it -> it[1] > 1}

        result.TitleInstancePackagePlatform = TitleInstancePackagePlatform.executeQuery(
                'SELECT tipp.impId as impId, count(tipp.impId) as cnt FROM TitleInstancePackagePlatform tipp GROUP BY tipp.impId ORDER BY tipp.impId'
        ).findAll{ it -> it[1] > 1}

        result*/
    }

    Map<String, Object> checkTitles() {
        Map result = [
                Org: [:],
                Package: [:],
                Platform: [:]
                // TitleInstances: [:],
                // Tipps: [:]
        ]

        // Orgs

        result.Org.name = Org.executeQuery(
                'SELECT org.name as name, count(org.name) as cnt FROM Org org GROUP BY org.name ORDER BY org.name'
        ).findAll{ it -> it[1] > 1}

        result.Org.shortname = Org.executeQuery(
                'SELECT org.shortname as shortname, count(org.shortname) as cnt FROM Org org GROUP BY org.shortname ORDER BY org.shortname'
        ).findAll{ it -> it[1] > 1}

        result.Org.shortcode = Org.executeQuery(
                'SELECT org.shortcode as shortcode, count(org.shortcode) as cnt FROM Org org GROUP BY org.shortcode ORDER BY org.shortcode'
        ).findAll{ it -> it[1] > 1}

        result.Org.sortname = Org.executeQuery(
                'SELECT org.sortname as sortname, count(org.sortname) as cnt FROM Org org GROUP BY org.sortname ORDER BY org.sortname'
        ).findAll{ it -> it[1] > 1}

        // Packages

        result.Package.name = Package.executeQuery(
                'SELECT pkg.name as name, count(pkg.name) as cnt FROM Package pkg GROUP BY pkg.name ORDER BY pkg.name'
        ).findAll{ it -> it[1] > 1}

        result.Package.sortName = Package.executeQuery(
                'SELECT pkg.sortName as sortName, count(pkg.sortName) as cnt FROM Package pkg GROUP BY pkg.sortName ORDER BY pkg.sortName'
        ).findAll{ it -> it[1] > 1}

        // Platforms

        result.Platform.name = Platform.executeQuery(
                'SELECT pf.name as name, count(pf.name) as cnt FROM Platform pf GROUP BY pf.name ORDER BY pf.name'
        ).findAll{ it -> it[1] > 1}

        result.Platform.normname = Platform.executeQuery(
                'SELECT pf.normname as normname, count(pf.normname) as cnt FROM Platform pf GROUP BY pf.normname ORDER BY pf.normname'
        ).findAll{ it -> it[1] > 1}
        /*
        result.Platform.primaryUrl = Platform.executeQuery(
                'SELECT pf.primaryUrl as primaryUrl, count(pf.primaryUrl) as cnt FROM Platform pf GROUP BY pf.primaryUrl ORDER BY pf.primaryUrl'
        ).findAll{ it -> it[1] > 1}
        */

        // TitleInstance

        /*
        result.TitleInstance.title = TitleInstance.executeQuery(
                'SELECT ti.title as title, count(ti.title) as cnt FROM TitleInstance ti GROUP BY ti.title ORDER By ti.title'
        ).findAll{ it -> it[1] > 1}

        result.TitleInstance.normTitle = TitleInstance.executeQuery(
                'SELECT ti.normTitle as normTitle, count(ti.normTitle) as cnt FROM TitleInstance ti GROUP BY ti.normTitle ORDER BY ti.normTitle'
        ).findAll{ it -> it[1] > 1}

        result.TitleInstance.keyTitle = TitleInstance.executeQuery(
                'SELECT ti.keyTitle as keyTitle, count(ti.keyTitle) as cnt FROM TitleInstance ti GROUP BY ti.keyTitle ORDER BY ti.keyTitle'
        ).findAll{ it -> it[1] > 1}

        result.TitleInstance.sortTitle = TitleInstance.executeQuery(
                'SELECT ti.sortTitle as sortTitle, count(ti.sortTitle) as cnt FROM TitleInstance ti GROUP BY ti.sortTitle ORDER BY ti.sortTitle'
        ).findAll{ it -> it[1] > 1}
        */

        // TitleInstancePackagePlatform
        /*
        result.TitleInstancePackagePlatform.hostPlatformURL = TitleInstancePackagePlatform.executeQuery(
                'SELECT tipp.hostPlatformURL as hostPlatformURL, count(tipp.hostPlatformURL) as cnt FROM TitleInstancePackagePlatform tipp GROUP BY tipp.hostPlatformURL ORDER BY tipp.hostPlatformURL'
        ).findAll{ it -> it[1] > 1}
        */
        
        result
    }

    def ajaxQuery(String key1, String key2, String value) {

        def result = []
        SimpleDateFormat sdfA = DateUtil.getSDF_NoTime()
        SimpleDateFormat sdfB = DateUtil.getSDF_NoZ()

        if (key1 == 'Org') {
            result = Org.findAllWhere( "${key2}": value ).collect{ it ->
                Map<String, Object> dryRunInfo = deletionService.deleteOrganisation(it, null, deletionService.DRY_RUN)

                [
                    id: it.id,
                    name: it.name,
                    class: it.class.simpleName,
                    link: g.createLink(controller:'organisation', action:'show', id: it.id),
                    created: sdfA.format( it.dateCreated ),
                    updated: sdfB.format( it.lastUpdated ),
                    deletable: dryRunInfo.deletable,
                    mergeable: dryRunInfo.mergeable
                ]
            }
        }
        if (key1 == 'Package') {
            result = Package.findAllWhere( "${key2}": value ).collect{ it -> [
                    id: it.id,
                    name: it.name,
                    link: g.createLink(controller:'package', action:'show', id: it.id),
                    created: sdfA.format( it.dateCreated ),
                    updated: sdfB.format( it.lastUpdated )
                ]
            }
        }
        if (key1 == 'Platform') {
            result = Platform.findAllWhere( "${key2}": value ).collect{ it -> [
                    id: it.id,
                    name: it.name,
                    link: g.createLink(controller:'platform', action:'show', id: it.id),
                    created: sdfA.format( it.dateCreated ),
                    updated: sdfB.format( it.lastUpdated )
                ]
            }
        }
        if (key1 == 'TitleInstance') {
            result = TitleInstance.findAllWhere( "${key2}": value ).collect{ it -> [
                    id: it.id,
                    name: it.title,
                    link: g.createLink(controller:'title', action:'show', id: it.id),
                    created: sdfA.format( it.dateCreated ),
                    updated: sdfB.format( it.lastUpdated )
                ]
            }
        }
        if (key1 == 'TitleInstancePackagePlatform') {
            result = TitleInstancePackagePlatform.findAllWhere( "${key2}": value ).collect{ it -> [
                    id: it.id,
                    name: 'TitleInstancePackagePlatform',
                    link: g.createLink(controller:'tipp', action:'show', id: it.id),
                    created: '',
                    updated: ''
                ]
            }
        }

        result
    }

    def checkBooleanFields() {

        List<String> candidates = []
        List<String> statements = []

        AppUtils.getAllDomainClasses().sort{ it.clazz.simpleName }.each { dc ->

            Collection bools = dc.persistentProperties.findAll {it.type in [boolean, java.lang.Boolean]}

            if (! bools.isEmpty()) {
                Map<String, Boolean> props = [:]

                bools.each { it ->
                    props.put( "${it.name}", dc.constraints[ it.name ].isNullable() )
                }

                // println " " + dc.clazz.simpleName
                props.each{ k,v ->
                    // String ctrl = "select count(o) from ${dc.clazz.simpleName} o where o.${k} is null"
                    // println "   nullable ? ${k} : ${v}, DB contains null values : " + Org.executeQuery(ctrl)

                    if (v.equals(true)) {
                        candidates.add( "${dc.clazz.simpleName}.${k} -> ${v}" )

                        String tableName = SwissKnife.toSnakeCase(dc.clazz.simpleName)
                        String columnName = SwissKnife.toSnakeCase(k)
                        String sql = "update ${tableName} set ${columnName} = false where ${columnName} is null;"

                        statements.add( sql )
                    }
                }
            }
        }

        println "___ found candidates: "
        candidates.each { println it }
        println "___ generated pseudo statements: "
        statements.each { println it }
    }
}
