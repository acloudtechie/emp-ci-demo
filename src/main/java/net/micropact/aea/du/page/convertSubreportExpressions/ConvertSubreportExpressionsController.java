package net.micropact.aea.du.page.convertSubreportExpressions;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.dao.SavedReport;
import net.micropact.aea.core.enums.ReportType;
import net.micropact.aea.core.xml.XmlUtility;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/* For jrxml look into expressions of the type:
 * <subreportExpression>
 *   <![CDATA[JasperCompileManager.compileReport("myFile.jrxml")]]>
 * </subreportExpression> */

/*
 * subreport formats below
 *
 * Jaspersoft Studio:
 *
 *     <subreport>
 *       ...
 *       <subreportExpression><![CDATA["reports/zmiller/Adventure Status Pie Chart.jasper"]]></subreportExpression>
 *     </subreport>
 *
 * entellitrak:
 *
 *     <subreport>
 *       ...
 *       <subreportExpression class="net.sf.jasperreports.engine.JasperReport">
 *          <![CDATA[SubReportLoader.getReportByBusinessKey("report.username.reportName")]]>
 *       </subreportExpression>
 *     </subreport>
 *
 * */

/**
 * This is the controller code for a page which can be used to transform the subreport expressions used within XML
 * reports between the format used by jaspersoft studio and entellitrak.
 *
 * @author zmiller
 */
public class ConvertSubreportExpressionsController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            // The subreport type to convert to will be passed in as the name of the enum.
            final String targetSubreportType = etk.getParameters().getSingle("subreportType");
            final List<Long> reportsToUpdate = convertStringsToNumbers(etk.getParameters().getField("reports"));

            final TextResponse response = etk.createTextResponse();

            if(targetSubreportType != null){

                final SubreportExpressionType targetType = SubreportExpressionType.valueOf(targetSubreportType);

                final Map<SubreportExpressionType, ISubreportMetadataCalculator> subreportCalculators =
                        Utility.arrayToMap(SubreportExpressionType.class, ISubreportMetadataCalculator.class,
                                new Object[][]{
                            {SubreportExpressionType.ETK_BUSINESS_KEY, new EtkReportBusinessKeySubreport(etk)},
                            {SubreportExpressionType.JASPERSTUDIO_DEFAULT_STRING, new JasperStudioDefaultSubreport(etk)}
                        });

                final ISubreportMetadataCalculator metadataCalculator = subreportCalculators.get(targetType);

                for(final long savedReportId : reportsToUpdate){
                    final SavedReport sourceReportInfo = SavedReport.ReportService.loadReportById(etk, savedReportId);
                    final String xmlDocument = sourceReportInfo.getReport();

                    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new InputSource(new StringReader(xmlDocument)));
                    final XPath xPath = XPathFactory.newInstance().newXPath();

                    final NodeList subReportExpressions = (NodeList) xPath.compile("//subreport/subreportExpression")
                            .evaluate(document, XPathConstants.NODESET);

                    // this section and its subparts could be split out into separate methods.
                    if(subReportExpressions.getLength() > 0){
                        for(int i = 0; i < subReportExpressions.getLength(); i++){

                            final Element node = (Element) subReportExpressions.item(i);

                            final Node attributeNode = (Node) xPath.compile("@class")
                                    .evaluate(node, XPathConstants.NODE);
                            final String textContent = node.getFirstChild().getNodeValue();
                            final String attributeValue = attributeNode == null ? null : attributeNode.getFirstChild().getNodeValue();

                            SavedReport savedReport = null;

                            for(final ISubreportMetadataCalculator subreportMetadataCalculator
                                    : subreportCalculators.values()){
                                final SavedReport subReportInfo =
                                        subreportMetadataCalculator.getReportInfo(attributeValue, textContent);
                                if(subReportInfo != null){
                                    savedReport = subReportInfo;
                                    break;
                                }
                            }

                            final Element newNode = document.createElement("subreportExpression");

                            final ReportXmlFragmentDescription fragment =
                                    metadataCalculator.generateSubreportExpression(savedReport);
                            final String classValue = fragment.getClassAttribute();
                            if(classValue != null){
                                newNode.setAttribute("class", classValue);
                            }

                            newNode.appendChild(document.createCDATASection(fragment.getCdataContent()));

                            node.getParentNode().replaceChild(newNode, node);
                        }

                        final String newData = XmlUtility.xmlToString(document);

                        etk.createSQL("UPDATE etk_saved_report SET report = :report WHERE saved_report_id = :reportId")
                        .setParameter("report",  newData)
                        .setParameter("reportId", savedReportId)
                        .execute();
                    }
                }
            }

            response.put("subreportTypes", JsonUtilities.encode(Arrays.asList(SubreportExpressionType.values())));
            response.put("reports", JsonUtilities.encode(etk.createSQL("SELECT SAVED_REPORT_ID, NAME, BUSINESS_KEY FROM etk_saved_report WHERE report_type = :reportType ORDER BY name, business_key, saved_report_id")
                    .setParameter("reportType", ReportType.JRXML.getEntellitrakReportType())
                    .fetchList()));

            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        } catch (final ParserConfigurationException e) {
            throw new ApplicationException(e);
        } catch (final XPathExpressionException e) {
            throw new ApplicationException(e);
        } catch (final SAXException e) {
            throw new ApplicationException(e);
        } catch (final IOException e) {
            throw new ApplicationException(e);
        } catch (final TransformerException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Converts a list of Strings which have the format of Long numbers to actual Longs.
     *
     * @param strings Strings to convert
     * @return the Longs represented by strings
     */
    private static List<Long> convertStringsToNumbers(final List<String> strings){
        final List<Long> returnList = new LinkedList<>();
        if(strings != null){
            for(final String string : strings){
                returnList.add(Long.parseLong(string));
            }
        }
        return returnList;
    }

    /**
     * This class represents the different types of subreport expressions.
     *
     * @author zmiller
     */
    private enum SubreportExpressionType implements IJson{
        JASPERSTUDIO_DEFAULT_STRING("Jaspersoft Studio"),
        ETK_BUSINESS_KEY("entellitrak");

        private final String display;

        /**
         * Constructor for SubereportExpressionType.
         *
         * @param displayName A user-readable representation of the type of subreport expression.
         */
        SubreportExpressionType(final String displayName){
            display = displayName;
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"display", toString()},
                {"value", name()}}));
        }

        @Override
        public String toString(){
            return display;
        }
    }

    /**
     * This interface represents objects which can read and write the data for a particular type of subreport
     * expression type.
     *
     * @author zmiller
     */
    private interface ISubreportMetadataCalculator{

        /**
         * This method gets a SavedReport if the classAttributeValue and textContent if this object knows how to read
         * them. If this object does not know how to read them, it returns null.
         *
         * @param classAttributeValue The value of the class attribute in the XML
         * @param textContent the content of the CDATA element in the XML
         * @return The SavedReport that is described by the class and text, otherwise null
         * @throws ApplicationException If anything goes wrong
         */
        SavedReport getReportInfo(String classAttributeValue, String textContent) throws ApplicationException;

        /**
         * This method calculates the information that needs to be in a newly generated subreport expression tag
         * for this type of subreport expression.
         *
         * @param reportInfo The information about the report which should be converted.
         * @return A description of the XML fragment which should be used for the new subreport expression
         */
        ReportXmlFragmentDescription generateSubreportExpression(SavedReport reportInfo);
    }

    /**
     * This class represents a description of the XML tag for a new subreport expression. Currently all subreport
     * expressions consist of an optional class, and content within a CDATA section.
     *
     * @author zmiller
     *
     */
    private class ReportXmlFragmentDescription{

        private final String classAttribute;
        private final String cdataContent;

        /**
         * Constructs an instance given a particular class attribute and cdata content.
         *
         * @param theClassAttribute The value of the "class" attribute of the subreportExpression XML tag.
         * @param theCdataContent The content of the CDATA tag within the subreportExpression XML tag.
         */
        ReportXmlFragmentDescription(final String theClassAttribute, final String theCdataContent){
            classAttribute = theClassAttribute;
            cdataContent = theCdataContent;
        }

        /**
         * Gets the value of the "class" attribute in the subreportExpression.
         *
         * @return The value of the "class" attribute in the subreportExpression.
         */
        public String getClassAttribute(){
            return classAttribute;
        }

        /**
         * Gets the content of the CDATA tag within the subreportExpression XML tag.
         *
         * @return The content of the CDATA tag within the subreportExpression XML tag.
         */
        public String getCdataContent(){
            return cdataContent;
        }
    }

    /**
     * This class encompasses the logic specific to subreport expressions which entellitrak uses to find reports by
     * business key.
     *
     * @author zmiller
     */
    private class EtkReportBusinessKeySubreport implements ISubreportMetadataCalculator{

        private final ExecutionContext etk;

        /**
         * Constructor for EtkReportBusinessKeySubreport.
         *
         * @param executionContext entellitrak execution context
         */
        EtkReportBusinessKeySubreport(final ExecutionContext executionContext){
            etk = executionContext;
        }

        @Override
        public SavedReport getReportInfo(final String attributeValue, final String textContent) throws ApplicationException {
            try {

                if(!"net.sf.jasperreports.engine.JasperReport".equals(attributeValue)){
                    return null;
                }

                final Pattern pattern =
                        Pattern.compile("^SubReportLoader\\.getReportByBusinessKey\\(\"([\\w\\.]+)\"\\)$");
                final Matcher matcher = pattern.matcher(textContent);

                if(!matcher.find()){
                    return null;
                }else{
                    final String reportBusinessKey = matcher.group(1);
                    return SavedReport.ReportService.loadReportByBusinessKey(etk, reportBusinessKey);
                }
            } catch (final IncorrectResultSizeDataAccessException e) {
                throw new ApplicationException(e);
            }
        }

        @Override
        public ReportXmlFragmentDescription generateSubreportExpression(final SavedReport reportInfo) {
            return new ReportXmlFragmentDescription("net.sf.jasperreports.engine.JasperReport",
                    String.format("SubReportLoader.getReportByBusinessKey(\"%s\")",
                            reportInfo.getBusinessKey()));
        }

    }

    /**
     * This class encompasses the logic specific for subreprot expressions which are jaspersoft studio's default.
     *
     * @author zmiller
     */
    private class JasperStudioDefaultSubreport implements ISubreportMetadataCalculator{

        private final ExecutionContext etk;

        /**
         * Constructor for JasperStudioDefaultSubreport.
         *
         * @param executionContext entellitrak execution context
         */
        JasperStudioDefaultSubreport(final ExecutionContext executionContext){
            etk = executionContext;
        }

        @Override
        public SavedReport getReportInfo(final String attributeValue, final String textContent) throws ApplicationException{
            try {
                if(attributeValue != null){
                    return null;
                }else{
                    // This validation section could probably be done using regular expressions a little easier.
                    if(!textContent.startsWith("\"reports/")){
                        throw new ApplicationException(String.format("Expected report file path \"%s\" to start with \"\"reports/\"", textContent ));
                    }

                    final int expectedFilePartLengths = 3;

                    final String[] fileParts = textContent.split("/");
                    if(fileParts.length != expectedFilePartLengths){
                        throw new ApplicationException(String.format("Report file path did not have the expected number (2) of forward slashes"));
                    }
                    // final String username = fileParts[1];
                    final String jasperPart = fileParts[2];

                    if(!jasperPart.endsWith(".jasper\"")){
                        throw new ApplicationException(String.format("The Report File Path does not end with \".jasper\"\""));
                    }

                    final String reportName = jasperPart.substring(0, jasperPart.length() - ".jasper\"".length());

                    /* We could select on the username as well, except that the core rules for this are ridiculous
                     * and runReportAsByteArray actually assumes that the report names are unique anyway. */
                    long referencedReportId;
                    referencedReportId = ((Number) etk.createSQL("SELECT SAVED_REPORT_ID FROM etk_saved_report report JOIN etk_user u ON u.user_id = report.user_id WHERE report.name = :reportName")
                            .setParameter("reportName", reportName)
                            .fetchObject()).longValue();

                    return SavedReport.ReportService.loadReportById(etk, referencedReportId);
                }
            } catch (final IncorrectResultSizeDataAccessException e) {
                throw new ApplicationException(e);
            }
        }

        @Override
        public ReportXmlFragmentDescription generateSubreportExpression(final SavedReport reportInfo) {
            return new ReportXmlFragmentDescription(null, String.format("\"reports/%s/%s.jasper\"",
                    reportInfo.getUsername(),
                    reportInfo.getName()));
        }
    }
}
