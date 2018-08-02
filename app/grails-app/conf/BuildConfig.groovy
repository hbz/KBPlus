grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.7
grails.project.source.level = 1.7

grails.plugin.location.'file-viewer' = "localRepository/file-viewer/hbz-file-viewer-0.3"

// grails.project.fork = [
//    test: [maxMemory: 768, minMemory: 64, debug: true, maxPerm: 256], // Removed ", daemon:true" because geb doesn't play nice with forked mode atm
//    run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256],
//    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256],
//    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
// ]

grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        excludes "grails-docs"
        // uncomment to disable ehcache
        excludes 'ehcache'
        //excludes 'ehcache-core' // to hibernate 4
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    def gebVersion = "0.12.2"
    def seleniumVersion = "2.53.0"
    def seleniumHtmlunitDriverVersion = "2.52.0"


    repositories {
        inherits true // Whether to inherit repository definitions from plugins
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()

        // uncomment these to enable remote dependency resolution from public Maven repositories
        //mavenCentral()
        mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo "https://oss.sonatype.org/content/repositories/releases"
        // mavenRepo "http://projects.k-int.com/nexus-webapp-1.4.0/content/repositories/snapshots"
        mavenRepo "http://projects.k-int.com/nexus-webapp-1.4.0/content/repositories/releases"
        mavenRepo "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/"
        mavenRepo "http://jasperreports.sourceforge.net/maven2/com/lowagie/itext/2.1.7.js2/"

        // Added because I'm strugging to get cglib - CGLib is causing problems - not sure what
        mavenRepo "http://central.maven.org/maven2/"

        mavenRepo "http://nexus.k-int.com/content/repositories/releases"

        // For shibboleth native-sp
        // mavenRepo "http://projects.k-int.com/nexus-webapp-1.4.0/content/repositories/releases"
        mavenRepo "http://nexus.k-int.com/content/repositories/releases/"
    }

    dependencies {

        compile "net.sf.ehcache:ehcache:2.7.0" //compile "net.sf.ehcache:ehcache-core:2.6.11" // to hibernate 4

        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        runtime 'javax.servlet:jstl:1.1.2'
        runtime 'taglibs:standard:1.1.2'
        build('org.grails:grails-docs:2.5.6') {
            excludes 'itext'
        }
        compile ('com.k-int:goai:1.0.2') {
          exclude 'groovy'
        }
        compile 'commons-codec:commons-codec:1.6'
        runtime 'xerces:xerces:2.4.0'
        runtime 'xerces:xercesImpl:2.11.0'
        runtime 'mysql:mysql-connector-java:5.1.30'

//        runtime ('org.elasticsearch:elasticsearch:1.7.1') {
//            excludes "org.ow2.asm:asm:4.1"
//            //excludes "org.ow2.asm:asm-commons:4.1"
//        }
//        runtime ('org.elasticsearch:elasticsearch-groovy:1.7.0') {
//            excludes "org.codehaus.groovy:groovy-all:2.4.3"
//        }

        compile 'org.elasticsearch:elasticsearch:2.1.2'
        runtime ('org.elasticsearch:elasticsearch-groovy:2.1.2') {
            excludes "org.codehaus.groovy:groovy-all"
        }

        compile 'joda-time:joda-time:2.9.9'


        runtime 'gov.loc:bagit:4.0'
        runtime 'org.apache.poi:poi:3.8'
        runtime 'net.sf.opencsv:opencsv:2.0'
        runtime 'com.googlecode.juniversalchardet:juniversalchardet:1.0.3'

        runtime 'org.apache.commons:commons-exec:1.3'
        compile 'org.apache.httpcomponents:httpcore:4.4.3'

        compile 'org.apache.httpcomponents:httpmime:4.5.1' // upgrade for MultipartEntityBuilder
        compile 'org.apache.httpcomponents:httpclient:4.5.1'

        test 'org.hamcrest:hamcrest-all:1.3'
        test("org.seleniumhq.selenium:selenium-htmlunit-driver:$seleniumHtmlunitDriverVersion") {
            exclude 'xml-apis'
        }
        test "org.seleniumhq.selenium:selenium-firefox-driver:$seleniumVersion"
        test "org.seleniumhq.selenium:selenium-support:$seleniumVersion"
        
        // http://www.gebish.org/manual/current/build-integrations.html#grails
        // https://github.com/geb/geb-example-grails
        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"
        test "org.gebish:geb-spock:$gebVersion"

        runtime ( 'org.codehaus.groovy.modules.http-builder:http-builder:0.5.2' ) { 
          excludes "org.codehaus.groovy", "groovy"
        }
        //There should be a fix for jdt core on jasperreports version 6.
        // Without exclude jasper report compiling crashes on Java8
        compile ("net.sf.jasperreports:jasperreports:5.6.1"){
            excludes "eclipse:jdtcore:3.1.0"
        }
        compile "org.eclipse.jdt.core.compiler:ecj:4.3.1"
  
        // II Commented out..
        // compile 'cglib:cglib:2.2.2'
        compile "com.lowagie:itext:2.1.7"

        compile group: 'org.codehaus.groovy', name: 'groovy-ant', version: '2.4.14'

        compile 'org.apache.commons:commons-lang3:3.7'
    }

    plugins {

        runtime ':hibernate:3.6.10.19'
        //runtime ':hibernate4:5.0.0.RC1' //runtime ':hibernate:3.6.10.19' // to hibernate 4
        runtime ":resources:1.2.8" // 1.2.14 won't work @ kbplusapp.js.gsp
        compile ":scaffolding:2.1.2"
        runtime ':fields:1.5.1'

        //Sicherheitsrisko
        //compile ":file-viewer:0.3"

        build (':tomcat:7.0.55.2'){
            //This is crashing under Java8, we impport newer version manually
            excludes "org.eclipse.jdt.core.compiler:ecj:3.7.2"
        }

        runtime ":database-migration:1.4.0"

        compile ':cache:1.1.8'

        compile ':mail:1.0.7', {
           excludes 'spring-test'
        }

        // compile ":profiler:0.5"
        // Now part of framework, including this plugin will cause tests to execute twice
        // test ":spock:0.7", {
        //   exclude "spock-grails-support"
        // }
        test ":geb:$gebVersion"

        test ":remote-control:2.0"

        compile ':spring-security-core:2.0.0'
        //compile ':spring-security-core:1.2.7.4'
        //compile ':spring-security-ldap:1.0.6'
        //compile ':spring-security-shibboleth-native-sp:1.0.3'

        runtime ":gsp-resources:0.4.4"
        runtime ":jquery:1.11.1"

        runtime ":audit-logging:1.0.3"
        runtime ":executor:0.3"
        runtime ":markdown:1.1.1"
        runtime ":quartz:1.0.2"
        compile ":grails-melody:1.59.0"
        compile ":jsonp:0.2"

        // runtime "com.k-int:domain-model-oai-pmh:0.1"

        compile ":remote-pagination:0.4.8" //AJAX Pagination - Finance
    }
}
