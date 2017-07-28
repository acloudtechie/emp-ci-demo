/**
 *
 * Controller for the migration utility to initialize the roles required to complete the import process.
 * This is like a core defect in 3.17 and earlier that is related to importing display mappings with roles attached.
 * This utility is meant to automate the creation of the shells of the roles and facilitate the migration with IRS's 20+
 * entellitrak applications.
 *
 * Ming Zhang 09/04/2015
 **/

package net.micropact.aea.du.page.importRoleStubs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * Controller for the migration utility to auto generate the stubs for roles.
 *
 * @author Ming Zhang
 * @author zmiller
 *
 */
public class ImportRoleStubsController implements PageController {

    private final List<String> errors = new LinkedList<>();

    private static final String ROLE_XML_ELEMENT = "roles";
    private static final String BUSINESS_KEY_FIELD = "business-key";

    /**
     * This enum represents the type of HTML form which should be displayed to the user.
     *
     * @author zmiller
     */
    private enum FormToServe{
        /** Represents a form which has not been successfully processed yet. This includes submissions which encounter
         * errors.*/
        INITIAL,
        /** Represents a form which has been successfully submitted and processed.*/
        SUBMIT
    }

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            final List<RoleStatus> roleStatuses;
            final FormToServe formToServe;

            final boolean submit = etk.getParameters().getSingle("submit") != null;

            if(submit){
                roleStatuses = performImport(etk);
            }else{
                roleStatuses = new LinkedList<>();
            }

            formToServe = !submit || errors.size() > 0
                    ? FormToServe.INITIAL
                     : FormToServe.SUBMIT;

            response.put("errors", JsonUtilities.encode(errors));
            response.put("roleStatuses", JsonUtilities.encode(roleStatuses));
            response.put("formToServe", JsonUtilities.encode(formToServe));

            return response;

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Attempts to perform an import of the roles. Requires that a file parameter was submitted under the name of
     * <code>importFile</code>.
     *
     * @param etk entellitrak execution context
     * @return A list containing the roles and whether or not the roles were added, or already existed
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private List<RoleStatus> performImport(final PageExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        final List<RoleStatus> roleStatuses = new LinkedList<>();
        final List <String> newRoles = getRolesFromFile(etk.getParameters().getFile("importFile"));

        for (final String newRoleBusinessKey : newRoles){
            roleStatuses.add(checkImportRole(etk, newRoleBusinessKey));
        }
        return roleStatuses;
    }

    /**
     * Attempts to import a role, but first checks to see whether it exists. If the role already exists, it does not
     * attempt to import. If the role does not exist, then it will insert a stub for it.
     *
     * @param etk entellitrak execution context
     * @param roleBusinessKey entellitrak role business key
     * @return A role status indicating whether the role was created, or already existed
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static RoleStatus checkImportRole(final PageExecutionContext etk, final String roleBusinessKey)
            throws IncorrectResultSizeDataAccessException {
        if(0 == etk.createSQL("SELECT COUNT(*) FROM etk_role WHERE business_key = :roleBusinessKey")
                .setParameter("roleBusinessKey", roleBusinessKey)
                .fetchInt()){
            addRole(etk, roleBusinessKey);
            return new RoleStatus(roleBusinessKey, true);
        }else{
            return new RoleStatus(roleBusinessKey, false);
        }
    }

    /**
     * Creates a role with the given business key. It does not check whether or not a role already exists.
     *
     * @param etk entellitrak execution context
     * @param roleBusinessKey role business key
     */
    private static void addRole(final ExecutionContext etk, final String roleBusinessKey) {
        final String roleName = String.format("%s (Name Placeholder)", roleBusinessKey);
        final String roleDescription = String.format("%s (Description Placeholder)", roleBusinessKey);

        etk.createSQL(Utility.isSqlServer(etk) ? "INSERT INTO etk_role (name, business_key, description) VALUES (:roleName, :roleBusinessKey, :roleDescription)"
                                                 : "INSERT INTO etk_role (role_id, name, business_key, description) VALUES (HIBERNATE_SEQUENCE.NEXTVAL, :roleName, :roleBusinessKey, :roleDescription)")
                                                 .setParameter("roleName", roleName)
                                                 .setParameter("roleBusinessKey", roleBusinessKey)
                                                 .setParameter("roleDescription", roleDescription)
                                                 .execute();
    }

    /**
     * Extracts the role business keys from a file stream which contains the content of an XML file which is generated
     * by the standard entellitrak import/export.
     *
     * @param etkFileStream The XML file which contains the roles
     * @return A list of the role business keys in the file
     */
    private List<String> getRolesFromFile(final FileStream etkFileStream) {
        final List<String> returnList = new ArrayList<>();
        InputStream inputStream = null;

        try{
            if (etkFileStream == null
                    || etkFileStream.getFileName() == null) { // Make sure that the stream gets closed & only one exists
                errors.add("File cannot be null");
            } else {

                inputStream = etkFileStream.getInputStream();
                if(inputStream == null){
                    errors.add("File cannot be null");
                }else{
                    Document document;
                    document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new InputSource(new InputStreamReader(inputStream)));
                    final NodeList tableNodeList = document.getElementsByTagName(ROLE_XML_ELEMENT);
                    final Node tableNode = tableNodeList.item(0);
                    final NodeList tableChildrenNodeList = tableNode.getChildNodes();
                    NodeList columnNodeList = null;
                    for(int i = 0; i < tableChildrenNodeList.getLength(); i++){
                        final Node rowNode = tableChildrenNodeList.item(i);
                        columnNodeList = rowNode.getChildNodes();
                        for(int j = 0; j < columnNodeList.getLength(); j++){
                            final Node columnNode = columnNodeList.item(j);
                            final Node textNode = columnNode.getChildNodes().item(0);
                            if (BUSINESS_KEY_FIELD.equalsIgnoreCase(columnNode.getNodeName())){
                                returnList.add(textNode.getNodeValue());
                            }
                        }
                    }
                }
            }
        } catch (final SAXException | IOException | ParserConfigurationException e) {
            errors.add("Error parsing input file. Make sure you have uploaded a valid entellitrak XML Configuration file");
        } finally{
            IOUtility.closeQuietly(inputStream);
        }

        return returnList;
    }

    /**
     * This class represents whether a particular role was created, or already existed.
     *
     * @author zmiller
     */
    private static class RoleStatus implements IJson{

        private final String roleBusinessKey;
        private final boolean isNew;

        /**
         * Constructs a new Role Status object.
         *
         * @param theRoleBusinessKey The business keys of the role
         * @param wasInserted Was the role just created
         */
        RoleStatus(final String theRoleBusinessKey, final boolean wasInserted) {
            roleBusinessKey = theRoleBusinessKey;
            isNew = wasInserted;
        }

        /**
         * Gets the business key of the role.
         *
         * @return The business key of the role
         */
        public String getRoleBusinessKey(){
            return roleBusinessKey;
        }

        /** Get whether the role was just inserted.
         * @return Whether the role was just created
         */
        public boolean isInserted(){
            return isNew;
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(
                    Utility.arrayToMap(String.class, Object.class, new Object[][]{
                        {"roleBusinessKey", getRoleBusinessKey()},
                        {"isInserted", isInserted()}}));
        }
    }
}