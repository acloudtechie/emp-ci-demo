package net.micropact.aea.core.lookup;

import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.lookup.LookupExecutionContext;
import com.entellitrak.lookup.LookupHandler;

import net.entellitrak.aea.lookup.IAeaLookupHandler;
import net.micropact.aea.core.reflection.InterfaceImplementationUtility;
import net.micropact.aea.utility.ScriptObjectLanguageType;
import net.micropact.aea.utility.Utility;

/**
 * <p>
 *  This abstract class is for helping to implement {@link LookupHandler}s which are simply a list of java Script Objects
 *  which must implement a particular interface. An example is the lookup of IScripts on the the RF Script object.
 * </p>
 * <p>
 *  This handler returns Script Objects from the system repository.
 *  Both the Value and Display for this lookup is the fully qualified name of the script object.
 *  For instance: "net.entellitrak.aea.rf.RulesFramework"
 *  The reason for choosing this to be the value is that despite the fact that it is a calculated field,
 *  there is no other good choice. The script_id changes when you apply changes,
 *  the business key may be different in a different system,
 *  the name is not unique, and code which needs to use this data element is likely to be
 *  interested in the fully qualified name anyway.
 * </p>
 * <p>
 *  In order to use this class, you only need to extend it, and call its constructor from the subclass's default
 *  constructor.
 * </p>
 *
 * @author zachary.miller
 */
public class AInterfaceImplementingLookup implements LookupHandler, IAeaLookupHandler {

    private final Class<?> theInterfaceClass;
    private final String elementBusinessKey;

    /**
     * Constructor which must be called from the subclass's.
     *
     * @param theInterface the interface which Script Objects must implement
     * @param theElementBusinessKey The business key of the element which the lookup belongs to
     */
    protected AInterfaceImplementingLookup(final Class<?> theInterface, final String theElementBusinessKey) {
        theInterfaceClass = theInterface;
        elementBusinessKey = theElementBusinessKey;
    }

    @Override
    public String execute(final LookupExecutionContext etk) throws ApplicationException {
        try {

            final long languageType = ScriptObjectLanguageType.JAVA.getId();

            final Map<String, Object> objectElementInfo = etk.createSQL("SELECT dataElement.ELEMENT_NAME, dataElement.COLUMN_NAME, dataObject.TABLE_NAME FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = :trackingConfigId AND dataElement.business_key = :elementBusinessKey")
                    .setParameter("trackingConfigId", Utility.getTrackingConfigIdNext(etk))
                    .setParameter("elementBusinessKey", elementBusinessKey)
                    .fetchMap();

            final String elementName = (String) objectElementInfo.get("ELEMENT_NAME");
            final String columnName = (String) objectElementInfo.get("COLUMN_NAME");
            final String tableName = (String) objectElementInfo.get("TABLE_NAME");

            if(etk.isForTracking()){
                return String.format("SELECT fully_qualified_script_name Display, fully_qualified_script_name Value FROM aea_script_pkg_view_sys_only WHERE fully_qualified_script_name = {?%s} OR fully_qualified_script_name IN (%s) ORDER BY Display, Value",
                        elementName,
                        buildInterfaceInClause(etk, theInterfaceClass));
            }else if(etk.isForSearch()){
                return String.format("SELECT fully_qualified_script_name Display, fully_qualified_script_name Value FROM aea_script_pkg_view_sys_only WHERE fully_qualified_script_name IN (SELECT %s FROM %s) ORDER BY Display, Value",
                        columnName,
                        tableName);
            }else if(etk.isForAdvancedSearch()){
                return String.format("SELECT fully_qualified_script_name Display, fully_qualified_script_name Value FROM aea_script_pkg_view_sys_only WHERE script_language_type = %s ORDER BY Display, Value",
                        languageType);
            }else if(etk.isForView()){
                return "SELECT fully_qualified_script_name Display, fully_qualified_script_name Value FROM aea_script_pkg_view_sys_only";
            }else if(etk.isForSingleResult()){
                return String.format("SELECT fully_qualified_script_name Display, fully_qualified_script_name Value FROM aea_script_pkg_view_sys_only WHERE fully_qualified_script_name = (SELECT %s FROM %s WHERE id = {?trackingId})",
                        columnName,
                        tableName);
            }else{
                throw new ApplicationException("InterfaceHandlerLookup is not applicable in this context.");
            }

        } catch (final IncorrectResultSizeDataAccessException e) {
            throw new ApplicationException(e);
        }
    }

    /**
     * Builds the contents of an IN clause which includes NULL, as well as the fully qualified name of any Script Object
     * which implements the interface.
     *
     * @param etk entellitrak execution context
     * @param theInterface the interface which must be implemented
     * @return the content of an IN clause which includes NULL, as well as the fully qualified name of all Script
     *          Objects which implement the interface.
     */
    private static String buildInterfaceInClause(final ExecutionContext etk, final Class<?> theInterface) {
        final StringBuilder resultBuilder = new StringBuilder("NULL");

        for(final Class<?> implementingClass :
            InterfaceImplementationUtility.getInterfaceImplementations(etk, theInterface)){
            resultBuilder.append(String.format(", '%s'",
                    implementingClass.getName()));
        }

        return resultBuilder.toString();
    }

    @Override
    public String getValueTableName(final ExecutionContext theExecutionContext) {
        return "AEA_SCRIPT_PKG_VIEW_SYS_ONLY";
    }

    @Override
    public String getValueColumnName(final ExecutionContext theExecutionContext) {
        return "FULLY_QUALIFIED_SCRIPT_NAME";
    }
}
