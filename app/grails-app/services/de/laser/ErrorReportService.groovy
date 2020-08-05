package de.laser

import com.k_int.kbplus.SystemTicket
import de.laser.helper.ConfigUtils
import grails.transaction.Transactional
import groovy.json.JsonBuilder
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.springframework.http.HttpStatus
import sun.misc.BASE64Encoder

import java.text.SimpleDateFormat

@Transactional
class ErrorReportService {

    // jira.rest.url = 'https://jira'
    // jira.rest.user = ''
    // jira.rest.token = ''
    // jira.rest.errorReport.target = 'issue/ERMS-xy'
    // jira.rest.errorReport.type = 'attachments'

    def grailsApplication
    def springSecurityService

    @Deprecated
    def getConfig() {
        def config = [:]

        def rest = grailsApplication.config.jira.rest

        if (! rest) {
            log.error("no jira.rest config found >>> abort sending error report ")
            return null
        }

        if (rest.errorReport.type == 'attachments') {
            config << [url: "${rest.url}/${rest.errorReport.target}/${rest.errorReport.type}"]
            //config << [method: 'POST']

            BASE64Encoder b64 = new BASE64Encoder()

            config << [headers: [
                    'Authorization': 'Basic ' + b64.encode( "${rest.user}:${rest.token}".getBytes() ),
                    //'Content-Type': 'multipart/form-data', // causes FileUploadException: the request was rejected because no multipart boundary was found
                    'X-Atlassian-Token': 'nocheck'
            ]]

            //config << [body: [
            //            'file': null
            //        ]]
        }
    }

    @Deprecated
    def sendReportAsAttachement(data) {
        def config = getConfig()

        if (! config || ! data) {
            log.info("ignored sending error report - no config and/or no data")
            return
        }
        HttpPost post = new HttpPost(config.url)

        config.headers.each{ k, v ->
            post.setHeader(k, v)
        }

        JsonBuilder jb = new JsonBuilder(data)
        SimpleDateFormat sdf = new SimpleDateFormat('yMMdd:HHmmss')
        Date dd = sdf.format(new Date())

        String filename = (ConfigUtils.getLaserSystemId() ?: 'Quelle unbekannt') + " - ${springSecurityService.getCurrentUser().email} - ${dd}"

        MultipartEntityBuilder meb = MultipartEntityBuilder.create()
        meb.addPart('file', new ByteArrayBody( jb.toPrettyString().getBytes(), filename.replace('/', '') ))
        post.setEntity(meb.build())

        HttpClient client = HttpClientBuilder.create().build()
        HttpResponse response = client.execute(post)

        if (response.getStatusLine()?.getStatusCode()?.equals(HttpStatus.NO_CONTENT.value())) {

            log.info("successfully sent error report for " + jb.content.meta)
            return true
        }
        else {
            log.info(EntityUtils.toString(response.getEntity()))
            return false
        }
    }

    def writeReportIntoDB(data) {

        def ticket = new SystemTicket(
                author:    data.author,
                title:     data.title,
                described: data.described,
                expected:  data.expected,
                info:      data.info,
                status:    data.status,
                category:  data.category
        )

        def meta = [
                system:  ConfigUtils.getLaserSystemId(),
                version: grailsApplication.metadata['app.version'],
                build:   grailsApplication.metadata['repository.revision.number']
        ]
        ticket.meta = (new JsonBuilder(meta)).toString()

        Date date = new Date()
        ticket.dateCreated = date
        ticket.lastUpdated = date

        if (ticket.save()) {
            return ticket
        } else {
            null
        }
    }
}
