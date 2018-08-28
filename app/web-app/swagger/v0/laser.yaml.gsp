---
swagger: "2.0"
info:
  version: "version 0 ~ 0.9"
  title: LAS:eR - API
  description: >
    Known Issues: _Authorization_ has to filled out manually. Usual javascript insertion isn't working due shadow dom mechanic of [React](https://facebook.github.io/react). Please copy and paste
  contact:
    email: david.klober@hbz-nrw.de

<g:if test="${grails.util.Environment.current == grails.util.Environment.PRODUCTION}">basePath: /api/v0 # production

schemes:
  - http</g:if>
<g:else>basePath: /laser/api/v0 # development

schemes:
  - http</g:else>

tags:
  - name: Documents
    description: ""
    externalDocs:
      url: http://wiki1.hbz-nrw.de/display/ERM/Home
  - name: IssueEntitlements
    description: ""
    externalDocs:
      url: http://wiki1.hbz-nrw.de/display/ERM/Home
  - name: Licenses
    description: ""
    externalDocs:
      url: http://wiki1.hbz-nrw.de/display/ERM/Home
  - name: Organisations
    description: ""
    externalDocs:
      url: http://wiki1.hbz-nrw.de/display/ERM/Home
  - name: Packages
    description: ""
    externalDocs:
      url: http://wiki1.hbz-nrw.de/display/ERM/Home
  - name: Subscriptions
    description: ""
    externalDocs:
      url: http://wiki1.hbz-nrw.de/display/ERM/Home

### parameters ###

parameters:
  q:
    in: query
    name: q
    required: true
    type: string
    description: Identifier for this query
#    enum:
#      - id
#      - identifier
#      - impId
#      - uuid
#      - name
#      - namespace:identifier
#      - shortcode
  v:
    in: query
    name: v
    required: true
    type: string
    description: Value for this query
  context:
    in: query
    name: context
    required: false
    type: string
    description: Optional information if user has memberships in multiple organisations
  authorization:
    in: header
    name: Authorization
    required: true
    type: string
    description: hmac-sha256 generated auth header

### responses ##

responses:
  ok:
    description: OK
  badRequest:
    description: Invalid or missing identifier/value
  conflict:
    description: Conflict with existing resource
  created:
    description: Resource successfully created
  forbidden:
    description: Forbidden access to this resource
  internalServerError:
    description: Resource not created
  notAcceptable:
    description: Requested format not supported
  notAuthorized:
    description: Request is not authorized
  notImplemented:
    description: Requested method not implemented
  preconditionFailed:
    description: Multiple matches

### endpoints ###

paths:
  /document:
    get:
      tags:
        - Documents
      summary: Find document by identifier
      description: >
        Supported are queries by following identifiers: *uuid*
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - application/*
        - text/*
      responses:
        200:
          description: OK
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but document not found
        406:
          $ref: "#/responses/notAcceptable"

  /issueEntitlements:
    get:
      tags:
        - IssueEntitlements
      summary: Find issue entitlements by subscription identifier and package identifier
      description: >
        Supported are queries by following identifiers: *globalUID* and *ns:identifier*. *Ns:identifier* value has to be defined like this: _gasco-lic:0815_. Both Parameters have to be separated by *comma*.
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - text/plain
        - application/json
      responses:
        200:
          description: OK
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but no issue entitlements found
        406:
          $ref: "#/responses/notAcceptable"

  /license:
    get:
      tags:
        - Licenses
      summary: Find license by identifier
      description: >
        Supported are queries by following identifiers: *globalUID*, *impId* and *ns:identifier*. *Ns:identifier* value has to be defined like this: _gasco-lic:0815_
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - application/json
      responses:
        200:
          description: OK
          schema:
            $ref: "#/definitions/License"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but license not found
        406:
          $ref: "#/responses/notAcceptable"
        412:
          $ref: "#/responses/preconditionFailed"
    post:
      tags:
        - Licenses
      summary: Create license (work in progess)
      #TODO:  description: Organisations and Subscriptions will NOT be _created_, but _linked_ if found
      description: Organisations will NOT be _created_, but _linked_ if found
      parameters:
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
        - in: body
          name: body
          required: true
          schema:
            $ref: "#/definitions/License"
          description: Object data
      consumes:
        - application/json
      produces:
        - application/json
      responses:
        201:
          $ref: "#/responses/created"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        409:
          $ref: "#/responses/conflict"
        500:
          $ref: "#/responses/internalServerError"

  /onixpl:
    get:
      tags:
        - Documents
      summary: Find onixpl documents by license identifier
      description: >
        Supported are queries by following identifiers: *globalUID* and *ns:identifier*. *Ns:identifier* value has to be defined like this: _gasco-lic:0815_
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - application/xml
      responses:
        200:
          description: OK
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but document not found
        406:
          $ref: "#/responses/notAcceptable"

  /organisation:
    get:
      tags:
        - Organisations
      summary: Find organisation by identifier
      description: >
        Supported are queries by following identifiers: *globalUID*, *impId*, *ns:identifier* and *shortcode*. *Ns:identifier* value has to be defined like this: _isil:DE-123_
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - application/json
      responses:
        200:
          description: OK
          schema:
            $ref: "#/definitions/Organisation"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but organisation not found
        406:
          $ref: "#/responses/notAcceptable"
        412:
          $ref: "#/responses/preconditionFailed"
    post:
      tags:
        - Organisations
      summary: Create organisation
      description: Organisation will NOT be created, if one organisation with same name AND namespace-identifier exists
      parameters:
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
        - in: body
          name: body
          required: true
          schema:
            $ref: "#/definitions/Organisation"
          description: Object data
      consumes:
        - application/json
      produces:
        - application/json
      responses:
        201:
          $ref: "#/responses/created"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        409:
          $ref: "#/responses/conflict"
        500:
          $ref: "#/responses/internalServerError"

  /package:
    get:
      tags:
        - Packages
      summary: Find packge by identifier
      description: >
        Supported are queries by following identifiers: *globalUID*, *identifier*, *impId* and *ns:identifier*. *Ns:identifier* value has to be defined like this: _xyz:4711_
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - application/json
      responses:
        200:
          description: OK
          schema:
            $ref: "#/definitions/Package"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but license not found
        406:
          $ref: "#/responses/notAcceptable"
        412:
          $ref: "#/responses/preconditionFailed"

  /subscription:
    get:
      tags:
      - Subscriptions
      summary: Find subscription by identifier
      description: >
        Supported are queries by following identifiers: *globalUID*, *identifier*, *impId* and *ns:identifier*. Ns:identifier value has to be defined like this: _xyz:4711_
      parameters:
        - $ref: "#/parameters/q"
        - $ref: "#/parameters/v"
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
      produces:
        - application/json
      responses:
        200:
          description: OK
          schema:
            $ref: "#/definitions/Subscription"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        403:
          $ref: "#/responses/forbidden"
        404:
          description: Valid request, but subscription not found
        406:
          $ref: "#/responses/notAcceptable"
        412:
          $ref: "#/responses/preconditionFailed"
    post:
      tags:
        - Subscriptions
      summary: Create Subscription (work in progess)
      #TODO:  description: Organisations and Subscriptions will NOT be _created_, but _linked_ if found
      description: Organisations will NOT be _created_, but _linked_ if found
      parameters:
        - $ref: "#/parameters/context"
        - $ref: "#/parameters/authorization"
        - in: body
          name: body
          required: true
          schema:
            $ref: "#/definitions/Subscription"
          description: Object data
      consumes:
        - application/json
      produces:
        - application/json
      responses:
        201:
          $ref: "#/responses/created"
        400:
          $ref: "#/responses/badRequest"
        401:
          $ref: "#/responses/notAuthorized"
        409:
          $ref: "#/responses/conflict"
        500:
          $ref: "#/responses/internalServerError"

definitions:

  ### stubs ###

  ClusterStub:
    type: object
    properties:
      id:
        type: integer
        readOnly: true
      name:
        type: string

  LicenseStub:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "license:7e1e667b-77f0-4495-a1dc-a45ab18c1410"
      impId:
        type: string
        readOnly: true
        example: "47bf5716-af45-7b7d-bfe1-189ab51f6c66"
      identifiers: # mapping attr ids
        type: array
        items:
          $ref: "#/definitions/Identifier"
      reference:
        type: string
      sortableReference:
        type: string

  OrganisationStub:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "org:d64b3dc9-1c1f-4470-9e2b-ae3c341ebc3c"
      name:
        type: string
        example: "Hochschulbibliothekszentrum des Landes NRW"
      identifiers: # mapping attr ids
        type: array
        items:
          $ref: "#/definitions/Identifier"
      shortcode:
        type: string
        readOnly: true
        example: "Hochschulbibliothekszentrum_des_Landes_NRW"

  PackageStub:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "package:f08250fc-257e-43d6-9528-c56d841a6b00"
      identifier:
        type: string
        example: "5d83bbbe-2a26-4eef-8708-6d6b86fd8453"
      identifiers: # mapping attr ids
        type: array
        items:
          $ref: "#/definitions/Identifier"
      impId:
        type: string
        readOnly: true
        example: "e6b41905-f1aa-4d0c-8533-e39f30220f65"
      name:
        type: string

  PlatformStub:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "platform:9d5c918a-55d0-4197-f22d-a418c14105ab"
      impId:
        type: string
        readOnly: true
        example: "9d5c918a-851f-4639-a6a1-e2dd124c2e02"
      name:
        type: string
      normname:
        type: string

  SubscriptionStub:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "subscription:3026078c-bdf1-4309-ba51-a9ea5f7fb234"
      identifier:
        type: string
        example: "1038ac38-eb21-4bf0-ab7e-fe8b8ba34b6c"
      identifiers: # mapping attr ids
        type: array
        items:
          $ref: "#/definitions/Identifier"
      impId:
        type: string
        readOnly: true
        example: "ff74dd15-e27f-48a2-b2d7-f02389e62639"
      name:
        type: string

  SubscriptionStub(inSubscription):
    allOf:
      - $ref: "#/definitions/SubscriptionStub"
      - type: object
        readOnly: true

  TitleInstancePackagePlatformStub:
    type: object
    description: TODO
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "titleinstancepackageplatform:9d5c918a-80b5-a121-a7f8-b05ac53004a"
      identifiers: # mapping attr ids
        type: array
        items:
          $ref: "#/definitions/Identifier"
      impId:
        type: string
        readOnly: true
        example: "c920188c-a7f8-54f6-80b5-e0161df3d360"

  TitleStub:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "title:eeb41a3b-a2c5-0e32-b7f8-3581d2ccf17f"
      identifiers: # mapping attr ids
        type: array
        items:
          $ref: "#/definitions/Identifier"
      impId:
        type: string
        readOnly: true
        example: "daccb411-e7c6-4048-addf-1d2ccf35817f"
      title:
        type: string
        example: "Das gute Buch"
      normtitle:
        type: string
        example: "Das_gute_Buch"

  ### full objects ###

  Address:
    type: object
    properties:
      street1:
        type: string
        example: "Jülicher Straße"
      street2:
        type: string
        example: "6"
      pob:
        type: string
        example: "270451"
      zipcode:
        type: string
        example: "50674"
      city:
        type: string
        example: "Köln"
      state:
        type: string
        example: "Nordrhein-Westfalen"
      country:
        type: string
        example: "Deutschland"
      type:
        type: string
        description: Mapping RefdataCategory "AddressType"
        enum:
          ["Postal address", "Billing address", "Delivery address"]
        example: "Postal address"

  Cluster:
    allOf:
      - $ref: "#/definitions/ClusterStub"
      - type: object
        properties:
          definition:
            type: string
          organisations: # mapping attr orgs
            type: array
            items:
              $ref: "#/definitions/OrganisationStub" # resolved OrgRole
          persons: # mapping attr prsLinks
            type: array
            items:
              $ref: "#/definitions/Person" # resolved PersonRole

  Contact:
    type: object
    properties:
      category: # mapping attr contentType
        type: string
        description: Mapping RefdataCategory "ContactContentType"
        enum:
          ["Mail", "Phone", "Fax"]
        example: "Mail"
      content:
        type: string
        example: "info-hbz@hbz-nrw.de"
      type:
        type: string
        description: Mapping RefdataCategory "ContactType"
        enum:
          ["Job-related", "Personal"]
        example: "Job-related"

  Document:
    type: object
    properties:
      filename:
        type: string
        example: "springer_2015.csv"
      mimetype:
        type: string
        example: "text/csv"
      title:
        type: string
        example: "Übersicht 2015"
      type:
        type: string
        description: Mapping RefdataCategory
        enum:
          [""]
        example: "General"
      uuid:
        type: string
        readOnly: true
        example: "70d4ef8a-71b9-4b39-b339-9f3773c29b26"

  Identifier:
    type: object
    properties:
      namespace:
        type: string
        example: "isil"
      value:
        type: string
        example: "DE-605"

  IssueEntitlement:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "issueentitlement:af045a3c-0e32-a681-c21d-3cf17f581d2c"
      accessStartDate:
        type: string
        format: date
      accessEndDate:
        type: string
        format: date
      coreStatusStart:
        type: string
        format: date
      coreStatusEnd:
        type: string
        format: date
      coreStatus:
        type: string
        description: Mapping RefdataCategory
        enum:
          [""]
      coverageDepth:
        type: string
      coverageNote:
        type: string
      endDate:
        type: string
        format: date
      endVolume:
        type: string
      endIssue:
        type: string
      embargo:
        type: string
      ieReason:
        type: string
      medium:
        type: string
        description: Mapping RefdataCategory
        enum:
          [""]
      startVolume:
        type: string
      startIssue:
        type: string
      startDate:
        type: string
        format: date
      status:
        type: string
        description: Mapping RefdataCategory
        enum:
          [""]
      subscription:
        $ref: "#/definitions/SubscriptionStub"
      tipp:
        $ref: "#/definitions/TitleInstancePackagePlatform"

  License:
    allOf:
      - $ref: "#/definitions/LicenseStub"
      - type: object
        properties:
          contact:
            type: string
          dateCreated:
            type: string
            format: date
            readOnly: true
          documents:
            type: array
            readOnly: true
            items:
              $ref: "#/definitions/Document" # resolved DocContext
          endDate:
            type: string
            format: date
            example: "2011-08-31 23:55:59"
    #      incomingLinks:
    #        type: array
    #        items:
    #          $ref: "#/definitions/Link"
          isPublic:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          lastmod:
            type: string
            format: date
            example: "2011-01-15 12:01:02"
          lastUpdated:
            type: string
            format: date
            readOnly: true
          licenseCategory:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          licenseUrl:
            type: string
          licensorRef:
            type: string
          licenseeRef:
            type: string
          licenseStatus:
            type: string
          licenseType:
            type: string
          noticePeriod:
            type: string
          onixplLicense:
            readOnly: true # bug fixed due #/definitions/OnixplLicense.readOnly:true
            $ref: "#/definitions/OnixplLicense"
          organisations: # mapping attr orgRelations
            type: array
            items:
              $ref: "#/definitions/OrganisationRole(onlyOrgRelation)" # resolved OrgRole
    #      outgoingLinks:
    #        type: array
    #        items:
    #          $ref: "#/definitions/Link"
    #      packages:
    #        type: array
    #        items:
    #          $ref: "#/definitions/PackageStub"
    #      persons: # mapping attr prsLinks
    #        type: array
    #        items:
    #          $ref: "#/definitions/Person" # resolved PersonRole
          properties: # mapping attr customProperties
            type: array
            items:
              $ref: "#/definitions/Property(LicenseProperty)"
          startDate:
            type: string
            format: date
            example: "2010-01-01 00:00:00"
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          subscriptions:
            type: array
            readOnly: true # TODO support
            items:
              $ref: "#/definitions/SubscriptionStub"
          type:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]

  License(inSubscription):
    allOf:
      - $ref: "#/definitions/LicenseStub"
      - type: object
        readOnly: true
        properties:
          contact:
            type: string
          dateCreated:
            type: string
            format: date
            readOnly: true
          documents:
            type: array
            readOnly: true
            items:
              $ref: "#/definitions/Document" # resolved DocContext
          endDate:
            type: string
            format: date
    #      incomingLinks:
    #        type: array
    #        items:
    #          $ref: "#/definitions/Link"
          isPublic:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          lastmod:
            type: string
            format: date
          lastUpdated:
            type: string
            format: date
            readOnly: true
          licenseCategory:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          licenseUrl:
            type: string
          licensorRef:
            type: string
          licenseeRef:
            type: string
          licenseStatus:
            type: string
          licenseType:
            type: string
          noticePeriod:
            type: string
          onixplLicense:
            readOnly: true # bug fixed due #/definitions/OnixplLicense.readOnly:true
            $ref: "#/definitions/OnixplLicense"
    #      outgoingLinks:
    #        type: array
    #        items:
    #          $ref: "#/definitions/Link"
    #      packages:
    #        type: array
    #        items:
    #          $ref: "#/definitions/PackageStub"
    #      persons: # mapping attr prsLinks
    #        type: array
    #        items:
    #          $ref: "#/definitions/Person" # resolved PersonRole
          properties: # mapping attr customProperties
            type: array
            items:
              $ref: "#/definitions/Property(LicenseProperty)"
          startDate:
            type: string
            format: date
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          type:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]

#  Link:
#    type: object
#    properties:
#      id:
#        type: integer
#        readOnly: true
#      fromLic:
#        $ref: "#definitions/LicenseStub"
#      isSlaved:
#        type: string
#        description: Mapping RefdataCategory
#        enum:
#          [""]
#      status:
#        type: string
#        description: Mapping RefdataCategory
#        enum:
#          [""]
#      toLic:
#        $ref: "#definitions/LicenseStub"
#      type:
#        type: string
#        description: Mapping RefdataCategory
#        enum:
#          [""]

  OnixplLicense:
    type: object
    readOnly: true
    properties:
#      id:
#        type: integer
#        readOnly: true
      document: # mapping attr doc
        $ref: "#definitions/Document"
      lastmod:
        type: string
        format: date
        example: "2016-05-10 13:18:47"
      title:
        type: string
#      licenses:
#        type: array
#        items:
#          $ref: "#/definitions/LicenseStub"

  Organisation:
    allOf:
      - $ref: "#/definitions/OrganisationStub"
      - type: object
        properties:
          addresses:
            type: array
            items:
              $ref: "#/definitions/Address"
          comment:
            type: string
          contacts:
            type: array
            items:
              $ref: "#/definitions/Contact"
          impId:
            type: string
            readOnly: true
            example: "9ef8a0d4-a87c-4b39-71b9-c29b269f311b"
          persons: # mapping attr prsLinks
            type: array
            items:
              $ref: "#/definitions/Person" # resolved PersonRole
          properties: # mapping attr customProperties and privateProperties
            type: array
            items:
              $ref: "#/definitions/Property"
          scope:
            type: string
          sector:
            type: string
            description: Mapping RefdataCategory "OrgSector"
            enum:
              ["Higher Education", "Publisher"]
            example: "Higher Education"
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          type:
            type: string
            description: Mapping RefdataCategory "OrgType"
            enum:
              ["Consortium", "Institution", "Other"]
            example: "Institution"

#  OrganisationRole:
#    properties:
#      id:
#        type: integer
#        readOnly: true
#      cluster:
#        $ref: "#/definitions/ClusterStub"
#        description: |
#          Exclusive with license, organisation, package, subscription and title
#      endDate:
#        type: string
#        format: date
#        example: "2011-08-31 23:55:59"
#      license:
#        $ref: "#/definitions/LicenseStub"
#        description: |
#          Exclusive with cluster, organisation, package, subscription and title
#      organisation:
#        $ref: "#/definitions/OrganisationStub"
#        description: |
#          Exclusive with cluster, license, package, subscription and title
#      package:
#        $ref: "#/definitions/PackageStub"
#        description: |
#          Exclusive with cluster, license, organisation, subscription and title
#      roleType:
#        type: string
#        description: Mapping RefdataCategory "Organisational Role"
#        enum:
#          [""]
#      startDate:
#        type: string
#        format: date
#        example: "2011-03-01 08:00:00"
#      subscription:
#        $ref: "#/definitions/SubscriptionStub"
#        description: |
#          Exclusive with cluster, license, organisation, package and title
#      title:
#        $ref: "#/definitions/TitleStub"
#        description: |
#          Exclusive with cluster, license, organisation, package and subscription

  OrganisationRole(onlyOrgRelation):
    properties:
      endDate:
        type: string
        format: date
        example: "2011-08-31 23:55:59"
      organisation:
        $ref: "#/definitions/OrganisationStub"
        description: |
          Exclusive with cluster, license, package, subscription and title
      roleType:
        type: string
        description: Mapping RefdataCategory "Organisational Role"
        enum:
          [""]
      startDate:
        type: string
        format: date
        example: "2011-03-01 08:00:00"

  Package:
    allOf:
      - $ref: "#definitions/PackageStub"
      - type: object
        properties:
          autoAccept:
            type: string
          breakable:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          cancellationAllowances:
            type: string
          consistent:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          dateCreated:
            type: string
            format: date
            example: "2011-01-01T11:12:31"
          documents:
            type: array
            items:
              $ref: "#/definitions/Document" # resolved DocContext
          endDate:
            type: string
            format: date
          fixed:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          forumId:
            type: string
          isPublic:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          lastUpdated:
            type: string
            format: date
          license:
            $ref: "#/definitions/LicenseStub"
          nominalPlatform:
            $ref: "#/definitions/Platform"
          organisations: # mapping attr orgs
            type: array
            items:
              $ref: "#/definitions/OrganisationRole(onlyOrgRelation)"
          packageListStatus:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          packageScope:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          packageStatus:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          packageType:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
    #      persons: # mapping attr prsLinks
    #        type: array
    #        items:
    #          $ref: "#/definitions/Person" # resolved PersonRole
          sortName:
            type: string
          startDate:
            type: string
            format: date
          subscriptions:
            type: array
            items:
              $ref: "#/definitions/SubscriptionStub" # resolved subscriptionPackages
            description: TODO
          tipps:
            type: array
            items:
              $ref: "#/definitions/TitleInstancePackagePlatform(inPackage)"
          vendorURL:
            type: string

  Package(inSubscription):
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "package:f08250fc-257e-43d6-9528-c56d841a6b00"
      identifier:
        type: string
        example: "04bf5766-bf45-4b9e-afe1-d89de46f6c66"
      issueEntitlements:
        type: array
        items:
          $ref: "#/definitions/IssueEntitlement"
      name:
        type: string
      vendorURL:
        type: string

  Platform:
    allOf:
      - $ref: "#definitions/PlatformStub"
      - type: object
        properties:
          dateCreated:
            type: string
          lastUpdated:
            type: string
          primaryUrl:
            type: string
          provenance:
            type: string
          serviceProvider:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          softwareProvider:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          type:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]

  Person:
    type: object
    properties:
      globalUID:
        type: string
        readOnly: true
        example: "person:a45a3cf0-f3ad-f231-d5ab-fc1d217f583c"
      addresses:
        type: array
        items:
          $ref: "#/definitions/Address"
      contacts:
        type: array
        items:
          $ref: "#/definitions/Contact"
      firstName:
        type: string
        example: "Berta"
      gender:
        type: string
        description: Mapping RefdataCategory "Gender"
        enum:
          ["Female", "Male"]
        example: "Female"
      isPublic:
        type: string
        description: Mapping RefdataCategory "YN". If set *No*, it's an hidden entry to/from an addressbook (depending on the given organisation context)
        enum:
          ["Yes", "No"]
        example: "Yes"
      lastName:
        type: string
        example: "Bauhaus"
      middleName:
        type: string
      properties: # mapping attr privateProperties
        type: array
        items:
          $ref: "#/definitions/Property"
      roles:
        type: array
        items:
          $ref: "#/definitions/PersonRole(usedAsFunction)"

  PersonRole:
    type: object
    properties:
      endDate:
        type: string
        format: date
        example: "2016-12-31 23:00:00"
      startDate:
        type: string
        format: date
        example: "2016-01-01 00:00:00"

  PersonRole(usedAsFunction):
    allOf:
      - $ref: "#/definitions/PersonRole"
      - type: object
        properties:
          functionType:
            type: string
            description: |
              Exclusive with responsibilityType |
              Mapping RefdataCategory "Person Function"
            enum:
              ["General contact person"]
            example: "General contact person"

  PersonRole(usedAsResponsibility):
    allOf:
      - $ref: "#/definitions/PersonRole"
      - type: object
        properties:
          cluster:
            description: |
              Exclusive with license, organisation, package, subscription and title
            $ref: "#/definitions/ClusterStub"
          license:
            description: |
              Exclusive with cluster, organisation, package, subscription and title
            $ref: "#/definitions/LicenseStub"
          organisation:
            description: |
              Exclusive with cluster, license, package, subscription and title
            $ref: "#/definitions/OrganisationStub"
          package:
            description: |
              Exclusive with cluster, license, organisation, subscription and title
            $ref: "#/definitions/PackageStub"
          responsibilityType:
            type: string
            description: |
              Exclusive with functionType |
              Mapping RefdataCategory "Person Responsibility"
            enum:
              ["Specific license editor", "Specific subscription editor", "Specific package editor", "Specific cluster editor", "Specific title editor"]
            example: "Specific license editor"
          subscription:
            description: |
              Exclusive with cluster, license, organisation, package and title
            $ref: "#/definitions/SubscriptionStub"
          title:
            description: |
              Exclusive with cluster, license, organisation, package and subscription
            $ref: "#/definitions/TitleStub"

  Property:
    type: object
    properties:
#      id:
#        type: integer
#        readOnly: true
      description: # mapping attr descr
        type: string
        example: "License Property"
      name:
        type: string
        example: "Remote Access"
      note:
        type: string
        example: "This is an important note"
  #    tenant:
  #      $ref: "#/definitions/OrganisationStub"
  #      description: If set, this property is *private*
      isPublic: # derived to substitute tentant
        type: string
        description: Mapping RefdataCategory "YN". If set *No*, it's an hidden entry to/from the given organisation context
        enum:
          ["Yes", "No"]
        example: "Yes"
      value: # mapping attr stringValue, intValue, decValue, refValue
        type: string
        example: "No"

  Property(LicenseProperty):
    allOf:
      - $ref: "#/definitions/Property"
      - type: object
        properties:
          paragraph:
            type: string
            example: "This is an important license paragraph"

  Subscription:
    allOf:
      - $ref: "#/definitions/SubscriptionStub"
      - type: object
        properties:
          cancellationAllowances:
            type: string
          dateCreated:
            type: string
            format: date
            readOnly: true
          documents:
            type: array
            readOnly: true
            items:
              $ref: "#/definitions/Document" # resolved DocContext
          endDate:
            type: string
            format: date
          instanceOf:
            readOnly: true # bug fixed due #/definitions/SubscriptionStub(inSubscription).readOnly:true
            $ref: "#/definitions/SubscriptionStub(inSubscription)"
          isPublic:
            type: string
            description: Mapping RefdataCategory "YN"
            enum:
              ["Yes", "No"]
    #      issueEntitlements:
    #        type: array
    #        items:
    #          $ref: "#/definitions/IssueEntitlement"
          isSlaved:
            type: string
            readOnly: true
            description: Mapping RefdataCategory "YN"
            enum:
              ["Yes", "No"]
          lastUpdated:
            type: string
            format: date
            readOnly: true
          license: # mapping owner
            readOnly: true # bug fixed due #/definitions/License(inSubscription).readOnly:true
            $ref: "#/definitions/License(inSubscription)"
          manualRenewalDate:
            type: string
            format: date
          noticePeriod:
            type: string
          organisations: # mapping attr orgRelations
            type: array
            items:
              $ref: "#/definitions/OrganisationRole(onlyOrgRelation)"
          packages:
            type: array
            readOnly: true
            items:
              $ref: "#/definitions/Package(inSubscription)"
    #      persons: # mapping attr prsLinks
    #        type: array
    #        items:
    #          $ref: "#/definitions/Person" # resolved PersonRole
          properties: # mapping attr customProperties
            type: array
            items:
              $ref: "#/definitions/Property"
          startDate:
            type: string
            format: date
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          type:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]

  Title:
    allOf:
      - $ref: "#/definitions/TitleStub"
      - type: object
        properties:
          dateCreated:
            type: string
            format: date
          keyTitle:
            type: string
            example: "Das gute Buch"
          lastUpdated:
            type: string
            format: date
          sortTitle:
            type: string
            example: "Das_gute_Buch"
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          type:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]

  TitleInstancePackagePlatform:
    allOf:
      - $ref: "#/definitions/TitleInstancePackagePlatformStub"
      - type: object
        description: TODO
        properties:
          accessStartDate:
            type: string
          accessEndDate:
            type: string
          coreStatusStart:
            type: string
          coreStatusEnd:
            type: string
          coverageDepth:
            type: string
          coverageNote:
            type: string
          delayedOA:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
    #      derivedFrom:
    #        $ref: "#/definitions/TitleInstancePackagePlatformStub"
          embargo:
            type: string
          endDate:
            type: string
          endVolume:
            type: string
          endIssue:
            type: string
          hostPlatformURL:
            type: string
          hybridOA:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
    #      masterTipp:
    #        $ref: "#/definitions/TitleInstancePackagePlatformStub"
          option:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          package:
            $ref: "#/definitions/PackageStub"
          payment:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          platform:
            $ref: "#/definitions/PlatformStub"
          rectype:
            type: string
          startDate:
            type: string
          startIssue:
            type: string
          startVolume:
            type: string
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          statusReason:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          subscription:
            $ref: "#/definitions/SubscriptionStub"
          title:
            $ref: "#/definitions/TitleStub"

  TitleInstancePackagePlatform(inPackage):
    allOf:
      - $ref: "#/definitions/TitleInstancePackagePlatformStub"
      - type: object
        description: TODO
        properties:
          accessStartDate:
            type: string
          accessEndDate:
            type: string
          coreStatusStart:
            type: string
          coreStatusEnd:
            type: string
          coverageDepth:
            type: string
          coverageNote:
            type: string
          delayedOA:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
    #      derivedFrom:
    #        $ref: "#/definitions/TitleInstancePackagePlatformStub"
          embargo:
            type: string
          endDate:
            type: string
          endVolume:
            type: string
          endIssue:
            type: string
          hostPlatformURL:
            type: string
          hybridOA:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
    #      masterTipp:
    #        $ref: "#/definitions/TitleInstancePackagePlatformStub"
          option:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          payment:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          platform:
            $ref: "#/definitions/PlatformStub"
          rectype:
            type: string
          startDate:
            type: string
          startIssue:
            type: string
          startVolume:
            type: string
          status:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          statusReason:
            type: string
            description: Mapping RefdataCategory
            enum:
              [""]
          title:
            $ref: "#/definitions/TitleStub"
