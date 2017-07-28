package net.micropact.aea.core.enums;

/**
 * This enum represents the types of reports within entellitrak.
 *
 * @author zmiller
 */
public enum ReportType{
    /**
     * "Standard Reports".
     */
    ETK_XML("ETX"),
    /**
     * XML (Jasper) Reports.
     */
    JRXML("JRX"),
    /**
     * I do not know what DSG reports are. They may be a legacy feature.
     */
    DESIGNER("DSG");

    private final String entellitrakReportType;

    /**
     * Simple Constructor.
     *
     * @param etkReportType String that core uses in the database to represent the report type.
     */
    ReportType(final String etkReportType){
        entellitrakReportType = etkReportType;
    }

    /**
     * Get the String that core uses in the database to represent this report type.
     *
     * @return The String that core uses in the database to represent this report type.
     */
    public String getEntellitrakReportType(){
        return entellitrakReportType;
    }
}