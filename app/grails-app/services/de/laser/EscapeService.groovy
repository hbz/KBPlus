package de.laser

import grails.transaction.Transactional
import org.apache.commons.lang.StringUtils

@Transactional
class EscapeService {

    String escapeString(String input) {
        String output = input.replaceAll(' ','_')
        String[] escapingChars = ['ä','ö','ü','ß','Ä','Ö','Ü']
        String[] replacement = ['ae','oe','ue','ss','Ae','Oe','Ue']
        output = StringUtils.replaceEach(output,escapingChars,replacement).replaceAll('[\'-,\\\\./;:]','')
        output
    }

    BigDecimal parseFinancialValue(String input) {
        String uniformedThousandSeparator = input.replaceAll("[',.]/d{3}","")
        BigDecimal output = new BigDecimal(uniformedThousandSeparator.replaceAll(",","."))
        output
    }
}
