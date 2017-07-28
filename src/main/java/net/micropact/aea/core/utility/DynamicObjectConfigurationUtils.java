package net.micropact.aea.core.utility;

import com.entellitrak.ExecutionContext;
import com.entellitrak.dynamic.DataObjectInstance;

/**
 * This utility class contains methods for working with core's Dynamic Object and Configuration APIs.
 *
 * @author Zachary.Miller
 */
public final class DynamicObjectConfigurationUtils {

    private static final String ROOT_PACKAGE = "com.entellitrak.dynamic";

    /**
     * Utility classes do not need public constructors.
     */
    private DynamicObjectConfigurationUtils(){}

    /**
     * This method retrieves the dynamic class for a data object based on its business key. This is a common operation
     * because the Dynamic Object API requires the use of the Class, however generic code will only have the
     * business key.
     *
     * @param etk entellitrak execution context
     * @param objectBusinessKey the business key of the data object
     * @return The class
     * @throws ClassNotFoundException If the class could not be found
     */
    public static Class<? extends DataObjectInstance> getDynamicClass(final ExecutionContext etk, final String objectBusinessKey)
            throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        final Class<? extends DataObjectInstance> theClass = (Class<? extends DataObjectInstance>) Class.forName(String.format("%s.%s",
                ROOT_PACKAGE,
                etk.getDataObjectService().getDataObjectByBusinessKey(objectBusinessKey).getObjectName()));
        return theClass;
    }
}
