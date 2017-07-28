package net.micropact.aea.du.page.organizationalUnitImport;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.FileStream;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.du.page.organizationalUnitExport.OrganizationalUnitExportController;
import net.micropact.aea.utility.ImportExportUtility;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This page ingests an XML representation of user-entered information regarding the Organizational Unit.
 *
 * @author zmiller
 * @see OrganizationalUnitExportController
 */
public class OrganizationalUnitImportController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk)
            throws ApplicationException {

        InputStream fileStream = null;
        try{
            final boolean update = "1".equals(etk.getParameters().getSingle("update"));
            Boolean importCompleted = false;
            List<Map<String, Object>> undeletedNodes = null;

            final TextResponse response = etk.createTextResponse();
            final List<String> errors = new LinkedList<>();

            if(update){
                final FileStream fileParameter = etk.getParameters().getFile("importFile");

                if(fileParameter == null){
                    errors.add("You must upload a file");
                }else{
                    fileStream = fileParameter.getInputStream();
                    undeletedNodes = OrgUnitImportLogic.performImport(etk, fileStream);
                    importCompleted = true;
                }
            }

            response.put("errors", JsonUtilities.encode(errors));
            response.put("undeletedNodes", JsonUtilities.encode(undeletedNodes));
            response.put("importCompleted", JsonUtilities.encode(importCompleted));

            return response;
        }finally{
            IOUtility.closeQuietly(fileStream);
        }
    }

    /**
     * Holds the import logic for importing Organizational Units.
     *
     * @author Zachary.Miller
     */
    private static class OrgUnitImportLogic {

        /**
         * Import an input stream containing organizational unit data.
         *
         * @param etk entellitrak execution context
         * @param inputStream input stream containing the data to import
         * @return the nodes which existed in the destination site but not in the source site
         * @throws ApplicationException If anything goes wrong
         */
        public static List<Map<String, Object>> performImport(final ExecutionContext etk, final InputStream inputStream)
                throws ApplicationException {
            try {
                final Document document = DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new InputStreamReader(inputStream)));

                return importOrgUnits(etk, document);

            } catch (final Exception e) {
                throw new ApplicationException(e);
            }
        }

        /**
         * This method is the entry point for the main algorithm.
         *
         * <h3>Constraints</h3>
         * <ul>
         *  <li>
         *      Code will be the unique identifier. If a node with a given code already exists in the destination site
         *      it will be updated. This means that Users/Objects referencing it by hierarchy will not get their
         *      links broken.
         *  </li>
         *  <li>
         *      No nodes will be deleted. Sites will have to delete nodes after the import is complete.
         *      If there are nodes in the destination system which were not in the import
         *      then those nodes will be flattened out and be made children of the new root (after the imported nodes).
         *  </li>
         *  <li>
         *      An attempt will be made to preserve order of nodes of the import file, and nodes which get reparented
         *      to the root.
         *  </li>
         * </ul>
         *
         * <h3>Assumptions</h3>
         *  <ul>
         *      <li>The nodes in the document are sorted by node id</li>
         *      <li>The root node is 0</li>
         *      <li>Node ids are non-negative</li>
         *  </ul>
         *
         * <h3>Algorithm</h3>
         * <ol>
         *  <li>
         *      "Orphan" all nodes by negating their node_id.
         *      Note that using negation will allow a later step of the algorithm to know the original order of nodes
         *      which will eventually be reparented to the root.
         *  </li>
         *  <li>
         *      Import nodes from the document by either inserting or updating the existing node with a matching code.
         *      The node_id, parent_id, max_child_id can all be taken directly from the document.
         *  </li>
         *  <li>
         *      Any nodes which still have a negative node_id were not in the import document.
         *      These nodes will be placed under the new root node.
         *  </li>
         *  <li>
         *      Update the max_child_id of the root node now that new children may have been added to it.
         *  </li>
         * </ol>
         *
         * @param etk entellitrak execution context
         * @param document Document containing the nodes to import
         * @return The nodes which existed in the destination site, but not in the source site
         * @throws IncorrectResultSizeDataAccessException
         *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
         */
        private static List<Map<String, Object>> importOrgUnits(final ExecutionContext etk, final Document document)
                throws IncorrectResultSizeDataAccessException {
            orphanExistingNodes(etk);
            importDocumentNodes(etk, document);
            final List<Map<String, Object>> orphanedNodes = reparentOrphanNodes(etk);
            updateRoot(etk);
            return orphanedNodes;
        }

        /**
         * This will update the max_child_id of the root node.
         * This is necessary because orphans could have been added to it.
         *
         * @param etk entellitrak execution context
         */
        private static void updateRoot(final ExecutionContext etk) {
            etk.createSQL("UPDATE etk_hierarchy SET max_child_id = (SELECT MAX(node_id) FROM etk_hierarchy) WHERE node_id = 0")
            .execute();
        }

        /**
         * This will find all nodes which were orphaned, but did not exist in the import document.
         * These are the nodes which still have negative node ids.
         * Their new parent will be the root node.
         *
         * @param etk entellitrak execution context
         * @return The hierarchy nodes which were orphaned
         * @throws IncorrectResultSizeDataAccessException
         *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
         */
        private static List<Map<String, Object>> reparentOrphanNodes(final ExecutionContext etk)
                throws IncorrectResultSizeDataAccessException {

            final List<Map<String, Object>> orphanedHierarchies = etk.createSQL("SELECT HIERARCHY_ID, NAME, CODE FROM etk_hierarchy WHERE node_id < 0 ORDER BY node_id DESC")
                    .fetchList();

            for(final Map<String, Object> hierarchy : orphanedHierarchies){
                final int newNodeId = 1 + etk.createSQL("SELECT MAX(node_id) FROM etk_hierarchy")
                .fetchInt();

                etk.createSQL("UPDATE etk_hierarchy SET node_id = :nodeId, parent_id = 0, max_child_id = :nodeId WHERE hierarchy_id = :hierarchyId")
                .setParameter("hierarchyId", hierarchy.get("HIERARCHY_ID"))
                .setParameter("nodeId", newNodeId)
                .execute();
            }

            return orphanedHierarchies;
        }

        /**
         * Imports the nodes from the XML document to the new system.
         * If a node with the same code is found in this system, that node will be updated (thus maintaining its
         * hierarchy id).
         *
         * @param etk entellitrak execution context
         * @param document document containing information about the nodes being imported from the source system.
         * @throws IncorrectResultSizeDataAccessException
         *          If there is an underlying {@link IncorrectResultSizeDataAccessException}
         */
        private static void importDocumentNodes(final ExecutionContext etk, final Document document)
                throws IncorrectResultSizeDataAccessException {
            final List<Map<String, String>> documentNodes = ImportExportUtility.getTable(document, "ETK_HIERARCHY");

            for(final Map<String, String> documentNode : documentNodes){
                final String code = documentNode.get("CODE");

                final Integer existingHierarchyId = etk.createSQL("SELECT hierarchy_id FROM etk_hierarchy WHERE code = :code")
                        .setParameter("code", code)
                        .returnEmptyResultSetAs(null)
                        .fetchInt();

                Integer newHierarchyId;
                final Map<String, Object> newParams= new HashMap<>(documentNode);

                if(existingHierarchyId == null){
                    /* Insert into the ETK_HIERARCHY_ROOT table */
                    if(Utility.isSqlServer(etk)){
                        newHierarchyId = etk.createSQL("INSERT INTO etk_hierarchy_root DEFAULT VALUES")
                                .executeForKey("HIERARCHY_ID");
                    }else{
                        newHierarchyId = etk.createSQL("SELECT HIBERNATE_SEQUENCE.NEXTVAL FROM DUAL")
                                .fetchInt();
                        etk.createSQL("INSERT INTO etk_hierarchy_root(hierarchy_id) VALUES(:hierarchy_id)")
                        .setParameter("hierarchy_id", newHierarchyId)
                        .execute();
                    }

                    /* Insert into ETK_HIERARCHY */
                    newParams.put("HIERARCHY_ID", newHierarchyId);

                    etk.createSQL("INSERT INTO etk_hierarchy(hierarchy_id, node_id, parent_id, max_child_id, name, code) VALUES(:HIERARCHY_ID, :NODE_ID, :PARENT_ID, :MAX_CHILD_ID, :NAME, :CODE)")
                    .setParameter(newParams)
                    .execute();
                }else{
                    /* Update the existing ETK_HIERARCHY record */
                    newHierarchyId = existingHierarchyId;

                    newParams.put("HIERARCHY_ID", newHierarchyId);

                    etk.createSQL("UPDATE etk_hierarchy SET node_id = :NODE_ID, parent_id = :PARENT_ID, max_child_id = :MAX_CHILD_ID, name = :NAME, code = :CODE WHERE hierarchy_id = :HIERARCHY_ID")
                    .setParameter(newParams)
                    .execute();
                }
            }
        }

        /**
         * "Orphans" all existing nodes in the system.
         * Each node is given a new node id: (-1 * (node_id + 1)).
         * This makes all the node ids negative and preserves (although reverses) the relative node id order.
         *
         * @param etk entellitrak execution context
         */
        private static void orphanExistingNodes(final ExecutionContext etk) {
            etk.createSQL("UPDATE etk_hierarchy SET node_id = -(node_id + 1), parent_id = -(node_id + 1), max_child_id = -(node_id + 1)")
            .execute();
        }
    }
}
