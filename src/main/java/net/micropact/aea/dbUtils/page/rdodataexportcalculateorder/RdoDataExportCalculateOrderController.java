package net.micropact.aea.dbUtils.page.rdodataexportcalculateorder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.core.algorithm.dependencySorter.DependencySorter;
import net.micropact.aea.core.algorithm.dependencySorter.DependencySorter.DependsOn;
import net.micropact.aea.core.query.QueryUtility;
import net.micropact.aea.dbUtils.utility.RdoDataExportUtility;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;
import net.micropact.aea.utility.lookup.AeaEtkDataElement;
import net.micropact.aea.utility.lookup.AeaEtkDataObject;
import net.micropact.aea.utility.lookup.LookupDataUtility;

/**
 * This class is the controller code for a page which will determine an ordering for the RDO export page based on
 * which other RDOs are referenced by a particular RDO. It currently does not attempt to resolve non-rdo dependencies
 * such as dependencies or core tables or dependencies on BTOs.
 *
 * @author zmiller
 */
public class RdoDataExportCalculateOrderController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
        try {

            final TextResponse response = etk.createTextResponse();

            response.setContentType(ContentType.JSON);

            final List<String> rdoTables = getRdoTableNames(etk);
            final List<Dependency> rdoDependencies = getDependencies(etk);

            response.put("out", JsonUtilities.encode(
                    new DependencySorter<>(
                            new DependencyDependsOn(rdoDependencies)).sortDependencies(rdoTables)));

            return response;

        } catch (final ClassNotFoundException e) {
            throw new ApplicationException(e);
        } catch (final InstantiationException e) {
            throw new ApplicationException(e);
        } catch (final IllegalAccessException e) {
            throw new ApplicationException(e);
        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }

    }

    /**
     * Returns a list of the names of all the RDO tables. Does not include M_ tables owned by those RDO tables.
     *
     * @param etk entellitrak execution context
     * @return A list of the RDO table names
     * @throws IncorrectResultSizeDataAccessException
     *          If there was an underlying {@link IncorrectResultSizeDataAccessException}
     */
    private static List<String> getRdoTableNames(final ExecutionContext etk)
            throws IncorrectResultSizeDataAccessException{
        return QueryUtility.toSimpleList(etk.createSQL("SELECT dataObject.TABLE_NAME FROM etk_data_object dataObject WHERE dataObject.tracking_config_id = :trackingConfigId AND dataObject.object_type = :objectType ORDER BY dataObject.label")
                .setParameter("objectType", DataObjectType.REFERENCE.getEntellitrakNumber())
                .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                .fetchList());
    }

    /**
     * This method determines all of the internal dependencies between the given tables. If the tables have a dependency
     * that goes through an M_ table, the M_ table will not appear anywhere and instead will show up as a direct
     * dependency between the two RDO tables.
     *
     * @param etk entellitrak execution context
     * @return A list of all the dependencies
     * @throws ClassNotFoundException If there was an underlying {@link ClassNotFoundException}
     * @throws InstantiationException If there was an underlying {@link InstantiationException}
     * @throws IllegalAccessException If there was an underlying {@link IllegalAccessException}
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    private static List<Dependency> getDependencies(final ExecutionContext etk)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, ApplicationException{
        final List<Dependency> dependencies = new LinkedList<>();
        final Map<Long, String> scriptNames = RdoDataExportUtility.getFullyQualifiedScriptNames(etk);
        final List<AeaEtkDataObject> allEtkDataObjects = LookupDataUtility.getAllEtkDataObjects(etk);
        for (final AeaEtkDataObject aDataObject : allEtkDataObjects) {
            if (DataObjectType.REFERENCE == aDataObject.getDataObjectType()) {
                if(aDataObject.getDataElements() != null){ // getDataElements returns null instead of the empty list
                    for (final AeaEtkDataElement aDataElement : aDataObject.getDataElements()) {
                        if (aDataElement.getIsBoundToLookup()) {
                            final Map<String, String> tableDependencyMap = RdoDataExportUtility
                                    .getRDataTableAndValueColumn(scriptNames,
                                            etk,
                                            aDataElement.getEtkLookupDefinition());

                            final String parentTable = tableDependencyMap.get("R_DATA_TABLE");

                            dependencies.add(new Dependency(parentTable, aDataObject.getTableName()));
                        }
                    }
                }
            }
        }

        return dependencies;
    }

    /**
     * This class implements {@link DependsOn} for a list of provided Dependency objects.
     *
     * @author zmiller
     */
    private static class DependencyDependsOn implements DependsOn<String>{

        private final List<Dependency> dependencies;

        /**
         * Create a dependency check for a given list of dependencies. It would be more efficient to use a Set of
         * dependencies instead of a list, however it is not really relevant for our data density.
         *
         * @param theDependencies The dependencies that this comparator will use to determine dependencies.
         */
        DependencyDependsOn(final List<Dependency> theDependencies){
            dependencies = theDependencies;
        }

        @Override
        public boolean dependsOn(final String item, final String potentialDependency) {
            for(final Dependency dependency : dependencies){
                if(item.equals(dependency.getChildTable())
                        && potentialDependency.equals(dependency.getParentTable())){
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Represents a dependency between two tables.
     *
     * @author zmiller
     */
    private static class Dependency implements IJson{
        private final String parentTable;
        private final String childTable;

        /**
         * Construct a dependency where theChildTable depends on theParentTable.
         *
         * @param theParentTable parent table
         * @param theChildTable child table
         */
        Dependency(final String theParentTable, final String theChildTable){
            parentTable = theParentTable;
            childTable = theChildTable;
        }

        /**
         * Gets the table which is depended upon by a child table.
         *
         * @return The table
         */
        public String getParentTable(){
            return parentTable;
        }

        /**
         * Get the table which depends on a parent table.
         *
         * @return The table
         */
        public String getChildTable(){
            return childTable;
        }

        /* This method was just for development. */
        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, String.class, new Object[][]{
                {"parentTable", parentTable},
                {"childTable", childTable}
            }));
        }
    }
}