package com.k_int.kbplus

import javax.persistence.Transient

class Invoice {

  Date      dateOfInvoice
  Date      dateOfPayment
  Date      datePassedToFinance
  Date      startDate
  Date      endDate
  String    invoiceNumber
  Org       owner
  String    description

  Date dateCreated
  Date lastUpdated

  static mapping = {
                      id column:'inv_id'
                 version column:'inv_version'
           dateOfInvoice column:'inv_date_of_invoice'
           dateOfPayment column:'inv_date_of_payment'
     datePassedToFinance column:'inv_date_passed_to_finance'
           invoiceNumber column:'inv_number'
               startDate column:'inv_start_date'
                 endDate column:'inv_end_date'
                   owner column:'inv_owner'
         description column:'inv_description', type:'text'

      dateCreated column: 'inv_date_created'
      lastUpdated column: 'inv_last_updated'
  }

  static constraints = {
          dateOfInvoice(nullable:true, blank:false)
          dateOfPayment(nullable:true, blank:false)
    datePassedToFinance(nullable:true, blank:false)
          invoiceNumber(nullable:false, blank:false)
              startDate(nullable:true, blank:false)
                endDate(nullable:true, blank:false)
                  owner(nullable:false, blank:false)
            description(nullable: true, blank: false)

      // Nullable is true, because values are already in the database
      lastUpdated (nullable: true, blank: false)
      dateCreated (nullable: true, blank: false)
  }


    @Transient
    static def refdataFind(params) {
        Org owner  = Org.findByShortcode(params.shortcode)
        def result = [];
        def ql     = null;
        if (owner)
            ql = Invoice.findAllByOwnerAndInvoiceNumberIlike(owner,"%${params.q}%",params)

        if ( ql ) {
            ql.each { id ->
                result.add([id:"${id.class.name}:${id.id}",text:"${id.invoiceNumber}"])
            }
        }

        result
    }
}
