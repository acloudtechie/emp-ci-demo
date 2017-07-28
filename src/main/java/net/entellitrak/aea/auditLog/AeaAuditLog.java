package net.entellitrak.aea.auditLog;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
* AeaAuditLog
*
* Log entries are stored in the T_AEA_AUDIT_LOG RDO Table.
*
* This is an audit log that attempts to achieve an acceptable level of performance. It relies heavily on
* private APIs and should be used only when the client insists.
*
* The main advantages that this API offers over core logging are:
*
* 	1. Lookup value replacement for CRUD fields (not just object IDs).
*   2. User extensible logging (meaning that the user can log custom events via the AeaLogEntry).
*
**/

//import java.io.StringReader;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.micropact.aea.auditLog.utility.AeaLookupValueFinder;
import net.micropact.aea.auditLog.utility.ExcludedColumn;
import net.micropact.aea.auditLog.utility.LogEventType;
import net.micropact.aea.auditLog.utility.UserSystemEventType;
import net.micropact.aea.core.exceptionTools.ExceptionUtility;
import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.Utility;

import com.entellitrak.ApplicationException;
import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.ExecutionContext;
import com.entellitrak.ReferenceObjectEventContext;
import com.entellitrak.SQLFacade;
import com.entellitrak.WorkflowExecutionContext;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.configuration.DataType;
import com.entellitrak.dynamic.DataObjectInstance;
import com.entellitrak.user.User;

/**
 * AeaAuditLog
 * Record any changes to data fields in the audit log.
 *
 * ACL 10-31-2014 Initial creation.
**/
public class AeaAuditLog {

    private final ExecutionContext etk;
    private final boolean isCreateEvent;
    private final boolean isUpdateEvent;
    private final boolean isReadEvent;
    private final boolean isDeleteEvent;
    private final Long baseId;
    private final Long parentId;
    private final Long trackingId;

    private final String dataObjectTableName;
    private final String dataObjectName;
    private final String dataObjectLabel;
    private final String dataObjectKey;
    private final String logTable;
    private final User user;

    private final AeaLookupValueFinder lvf;
    private final SQLFacade logQuery;
    private InetAddress inetAddress = null;
    //private final Connection connection;
    //private final PreparedStatement logQuery;

    private final IAeaAuditLogConfig auditLogConfig;
    private static final int FIELD_NAME_POSITION = 3;

    /**
     * Main constructor, should only be called 1x per ETP transaction. <br>
     *  <br>
     * Example of usage. <br>
     *
     * <pre>
     * <code>
     *  {@literal @}Override
     *  public boolean execute(final WorkflowExecutionContext etk) throws ApplicationException {
     *	AeaAuditLog auditLog = null;
     *
     *   try {
     *   	Utility.aeaLog(etk, "Begin test audit log");
     *   	auditLog = new AeaAuditLog(etk);
     *   	auditLog.logETPChanges();
     *
     *       return etk.isCreateEvent() &amp;&amp; (etk.getNewObject() instanceof Bto);
     *   } catch (final Exception e) {
     *       throw new ApplicationException(e);
     *   } finally {
     *   	if (auditLog != null) {
     *   		auditLog.writeAuditLogEntries();
     *   		Utility.aeaLog(etk, "End test audit log");
     *   	} <br>
     *   }
     * }
     * </code>
     * </pre>
     *
     * @param theEtk The execution context.
     * @throws ApplicationException ETK fatal exception.
     */
	public AeaAuditLog(final ExecutionContext theEtk) throws ApplicationException {
    	this(theEtk, null);
    }

    /**
     * Main constructor, should only be called 1x per ETP transaction. <br>
     *  <br>
     * Example of usage.
     * <pre>
     *  <code>
     *  {@literal @}Override
     *  public boolean execute(final WorkflowExecutionContext etk) throws ApplicationException {
     *	AeaAuditLog auditLog = null;
     *
     *   try {
     *   	Utility.aeaLog(etk, "Begin test audit log");
     *   	auditLog = new AeaAuditLog(etk);
     *   	auditLog.logETPChanges();
     *
     *       return etk.isCreateEvent() &amp;&amp; (etk.getNewObject() instanceof Bto);
     *   } catch (final Exception e) {
     *       throw new ApplicationException(e);
     *   } finally {
     *   	if (auditLog != null) {
     *   		auditLog.writeAuditLogEntries();
     *   		Utility.aeaLog(etk, "End test audit log");
     *   	}
     *   }
     * }
     * </code>
     * </pre>
     *
     * @param theEtk The execution context.
     * @param theConfig An audit log configuration that allows for
     * @throws ApplicationException ETK fatal exception.
     */
	public AeaAuditLog(final ExecutionContext theEtk, final IAeaAuditLogConfig theConfig) throws ApplicationException {
    	this.etk = theEtk;
    	this.auditLogConfig = theConfig;
    	this.lvf = new AeaLookupValueFinder(etk);
    	this.user = etk.getCurrentUser();


    	try {
			this.inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			theEtk.getLogger().warn("Error retrieving server hostname/ip information", e1);
		}

    	if ((theConfig != null) && (theConfig.getLogTableName() != null)) {
    		this.logTable = theConfig.getLogTableName();
    	} else {
    		this.logTable = "T_AEA_AUDIT_LOG";
    	}

    	if (theEtk instanceof WorkflowExecutionContext) {
			WorkflowExecutionContext wetk = (WorkflowExecutionContext) etk;
			isCreateEvent = wetk.isCreateEvent();
			isReadEvent   = wetk.isReadEvent();
			isUpdateEvent = wetk.isUpdateEvent();
			isDeleteEvent = wetk.isDeleteEvent();

			trackingId = wetk.getNewObject().properties().getId();
			baseId     = wetk.getNewObject().properties().getBaseId() != null ?
					     wetk.getNewObject().properties().getBaseId() : trackingId;
			parentId   = wetk.getNewObject().properties().getParentId() != null ?
					     wetk.getNewObject().properties().getParentId() : trackingId;

			dataObjectTableName = wetk.getNewObject().configuration().getTableName();
			dataObjectName      = wetk.getNewObject().configuration().getObjectName();
			dataObjectLabel     = wetk.getNewObject().configuration().getLabel();
			dataObjectKey       = wetk.getNewObject().configuration().getBusinessKey();
    	} else if (theEtk instanceof DataObjectEventContext) {
    		DataObjectEventContext wetk = (DataObjectEventContext) etk;
    		isCreateEvent 	= wetk.getDataEventType() == DataEventType.CREATE;
    		isReadEvent 	= wetk.getDataEventType() == DataEventType.READ;
    		isUpdateEvent 	= wetk.getDataEventType() == DataEventType.UPDATE;
    		isDeleteEvent 	= wetk.getDataEventType() == DataEventType.DELETE;

	    	trackingId 		= wetk.getNewObject().properties().getId();
	    	baseId 			= wetk.getNewObject().properties().getBaseId() != null ?
	    			          wetk.getNewObject().properties().getBaseId() : trackingId;
	    	parentId		= wetk.getNewObject().properties().getParentId() != null ?
	    			          wetk.getNewObject().properties().getParentId() : trackingId;

	    	dataObjectTableName = wetk.getNewObject().configuration().getTableName();
	    	dataObjectName      = wetk.getNewObject().configuration().getObjectName();
	    	dataObjectLabel     = wetk.getNewObject().configuration().getLabel();
	    	dataObjectKey       = wetk.getNewObject().configuration().getBusinessKey();
    	} else if (theEtk instanceof ReferenceObjectEventContext) {
    		ReferenceObjectEventContext wetk = (ReferenceObjectEventContext) etk;
    		isCreateEvent 	= wetk.getDataEventType() == DataEventType.CREATE;
    		isReadEvent 	= wetk.getDataEventType() == DataEventType.READ;
    		isUpdateEvent 	= wetk.getDataEventType() == DataEventType.UPDATE;
    		isDeleteEvent 	= wetk.getDataEventType() == DataEventType.DELETE;

	    	trackingId 		= wetk.getNewObject().properties().getId();
	    	baseId 			= wetk.getNewObject().properties().getBaseId() != null ?
	    			          wetk.getNewObject().properties().getBaseId() : trackingId;
	    	parentId		= wetk.getNewObject().properties().getParentId() != null ?
	    			          wetk.getNewObject().properties().getParentId() : trackingId;

	    	dataObjectTableName = wetk.getNewObject().configuration().getTableName();
	    	dataObjectName      = wetk.getNewObject().configuration().getObjectName();
	    	dataObjectLabel     = wetk.getNewObject().configuration().getLabel();
	    	dataObjectKey       = wetk.getNewObject().configuration().getBusinessKey();
    	} else {
    		isCreateEvent 	= false;
    		isReadEvent 	= false;
    		isUpdateEvent 	= false;
    		isDeleteEvent 	= false;
    		trackingId 		= -1L;
    		parentId 		= -1L;
    		baseId 			= -1L;

    		dataObjectTableName = "";
    		dataObjectName = "";
    		dataObjectLabel = "";
    		dataObjectKey = "";
    	}

    	try {

    		if (Utility.isSqlServer(etk)) {
     		   logQuery = etk.createSQL("insert into " + this.logTable
   	                + " (C_COLUMN, C_TIMESTAMP, C_DATA_OBJECT_NAME, "
   	                + " C_TRACKING_ID, C_EVENT_TYPE, C_DATA_ELEMENT_NAME, C_NEW_VALUE, "
   	                + " C_PREVIOUS_VALUE, C_USER_ACCOUNT_NAME, C_EVENT_MESSAGE, "
   	                + " C_ORGANIZATION, C_TABLE_NAME, C_EVENT_ACTOR, C_USER_ID, "
   	                + " C_DATA_OBJECT_KEY, C_DATA_OBJECT_LABEL, C_ORGANIZATION_ID, "
   	                + " C_USER_ROLE_ID, C_USER_ROLE_NAME, C_PARENT_ID, C_BASE_ID, "
   	                + " C_APP_SERVER_HOSTNAME, C_APP_SERVER_IP_ADDRESS) "
   	                + "values (:1 , dbo.ETKF_getServerTime(), :2, "
   	                + ":3 , :4 , :5 , :6 , "
   	                + ":7 , :8 , :9 , :10 , :11 , :12 , "
   	                + ":13 , :14 , :15 , :16 , :17, :18, :19, :20, "
   	                + ":21, :22) ");
    		} else {
    		   logQuery = etk.createSQL("insert into " + this.logTable
	                + " (ID, C_COLUMN, C_TIMESTAMP, C_DATA_OBJECT_NAME, "
	                + " C_TRACKING_ID, C_EVENT_TYPE, C_DATA_ELEMENT_NAME, C_NEW_VALUE, "
	                + " C_PREVIOUS_VALUE, C_USER_ACCOUNT_NAME, C_EVENT_MESSAGE, "
	                + " C_ORGANIZATION, C_TABLE_NAME, C_EVENT_ACTOR, C_USER_ID, "
	                + " C_DATA_OBJECT_KEY, C_DATA_OBJECT_LABEL, C_ORGANIZATION_ID, "
	                + " C_USER_ROLE_ID, C_USER_ROLE_NAME, C_PARENT_ID, C_BASE_ID, "
	                + " C_APP_SERVER_HOSTNAME, C_APP_SERVER_IP_ADDRESS) "
	                + "values ("
	                + "OBJECT_ID.nextval, :1 , ETKF_GETSERVERTIME(), :2, "
   	                + ":3 , :4 , :5 , :6 , "
   	                + ":7 , :8 , :9 , :10 , :11 , :12 , "
   	                + ":13 , :14 , :15 , :16 , :17, :18, :19, :20, "
   	                + ":21, :22) ");
    		}
    	} catch (final Exception e) {
    		throw new ApplicationException("Error writing to log table \"" + this.logTable + "\" \n\n" +
    	                                   ExceptionUtility.getFullStackTrace(e));
    	}
    }

    /**
     * Adds an entry to the AEA Audit Log.
     *
     * @param aLogEntry A new log entry.
     * @throws SQLException An exception inserting the log entry.
     */
    public void addLogEntry(final AeaLogEntry aLogEntry) throws SQLException {
        //ID (done via query)
    	//logQuery.setLong(1, aLogEntry.getBtoId()); //ID_BASE,
    	//logQuery.setLong(2, aLogEntry.getBtoId()); //ID_PARENT,
    	logQuery.setParameter("1", aLogEntry.getColumnName()); //C_COLUMN,
    	//C_TIMESTAMP (done via query
    	logQuery.setParameter("2", aLogEntry.getDataObjectName()); //C_DATA_OBJECT
    	logQuery.setParameter("3", aLogEntry.getTrackingId()); //C_TRACKING_ID
    	logQuery.setParameter("4", aLogEntry.getCrudEventType().getEventType()); //C_EVENT_TYPE
    	logQuery.setParameter("5", aLogEntry.getDataElementName()); //C_DATA_ELEMENT

    	logQuery.setParameter("6", aLogEntry.getNewDataValue());

    	logQuery.setParameter("7", aLogEntry.getPreviousDataValue());

    	logQuery.setParameter("8", aLogEntry.getUser().getAccountName()); //C_USER

    	logQuery.setParameter("9", aLogEntry.getMessage());

    	logQuery.setParameter("10", aLogEntry.getUser().getHierarchy().getName()); //C_ORGANIZATION
    	logQuery.setParameter("11", aLogEntry.getTableName()); //C_TABLE
    	logQuery.setParameter("12", aLogEntry.getUserSystemEventType().getEventType()); //C_EVENT_ACTOR

    	logQuery.setParameter("13", aLogEntry.getUser().getId());
    	logQuery.setParameter("14", aLogEntry.getDataObjectKey());
    	logQuery.setParameter("15", aLogEntry.getDataObjectLabel());
    	logQuery.setParameter("16", aLogEntry.getUser().getHierarchy().getId());
    	logQuery.setParameter("17", aLogEntry.getUser().getRole().getId());
    	logQuery.setParameter("18", aLogEntry.getUser().getRole().getName());
    	logQuery.setParameter("19", aLogEntry.getParentId());
    	logQuery.setParameter("20", aLogEntry.getBaseId());

    	if (inetAddress != null) {
    		logQuery.setParameter("21", inetAddress.getHostName());
    		logQuery.setParameter("22", inetAddress.getHostAddress());
    	} else {
    		logQuery.setParameter("21", null);
    		logQuery.setParameter("22", null);
    	}

    	logQuery.execute();
    }

    /**
     * Returns the column name of the tracked data element.
     *
     * @param tde TrackedDataElement.
     * @return String COLUMN_NAME.
     */
    private String getColumnName (final DataElement tde) {
        return ((tde != null) &&
                (tde.getColumnName() != null)) ?
                 tde.getColumnName() : "";
    }

    /**
     * Returns the data element name of the tracked data element.
     *
     * @param tde TrackedDataElement.
     * @return String data element name.
     */
    private String getDataElementName (final DataElement tde) {
        return ((tde != null) &&
                (tde.getName() != null)) ?
                 tde.getName() : "";
    }

    /**
     * Log all changes to the BTO/CTO upon read, create, update or delete. ETP
     * events must be set in the tracking configuration for this to work properly.
     * <br>
     * NOTE : Entries are not written until writeAuditLogEntries() is called in a finally().
     * @throws ApplicationException ETK fatal exception.
     */
    public void logETPChanges() throws ApplicationException {


        if (!((etk instanceof WorkflowExecutionContext) || (etk instanceof DataObjectEventContext) || (etk instanceof ReferenceObjectEventContext))) {
        	throw new ApplicationException("Error - logETPChanges can only be executed when the ExecutionContext is "
        			                     + "an instance of WorkflowExecutionContext, DataObjectEventContext, or ReferenceObjectEventContext.");
        }

        final HashMap<String, Object> excludedColumns = new HashMap<>();
        boolean logMessagesForCRUD = true;

        if (this.auditLogConfig != null) {
        	for (final ExcludedColumn aColumn : this.auditLogConfig.getExcludedColumns()) {
        		excludedColumns.put(aColumn.getTableName().toUpperCase() +
        				            "." +
        				            aColumn.getColumnName().toUpperCase(), null);
        	}

        	logMessagesForCRUD = this.auditLogConfig.logMessagesForCRUD();
        }

        final StringBuilder em = new StringBuilder();
        final AeaLogEntry logEntry = new AeaLogEntry();
        String columnName;

    	logEntry.setBaseId(baseId);
    	logEntry.setParentId(parentId);
    	logEntry.setDataObjectName(dataObjectName);
    	logEntry.setDataObjectKey(dataObjectKey);
    	logEntry.setDataObjectLabel(dataObjectLabel);
    	logEntry.setUser(user);
    	logEntry.setTableName(dataObjectTableName);
    	logEntry.setTrackingId(trackingId);
    	logEntry.setUserSystemEventType(UserSystemEventType.USER);

    	DataObjectInstance newObject = null;
    	DataObjectInstance oldObject = null;

    	Map<String, String> isLookupDefMap = new HashMap<>();
    	Map<String, Number> valueTypeMap = new HashMap<>();
    	Map<String, Object> newObjectValueMap = new HashMap<>();
    	Map<String, Object> newObjectParamMap = new HashMap<>();
    	Map<String, Object> oldObjectValueMap = new HashMap<>();
    	Map<String, Object> oldObjectParamMap = new HashMap<>();

    	if (isCreateEvent || isUpdateEvent) {

	    	if (etk instanceof WorkflowExecutionContext) {
	    		newObject = ((WorkflowExecutionContext) etk).getNewObject();
	    		oldObject = ((WorkflowExecutionContext) etk).getOldObject();
	    	} else if (etk instanceof ReferenceObjectEventContext) {
	    		newObject = ((ReferenceObjectEventContext) etk).getNewObject();
	    		oldObject = ((ReferenceObjectEventContext) etk).getOldObject();
	    	} else {
	    		newObject = ((DataObjectEventContext) etk).getNewObject();
	    		oldObject = ((DataObjectEventContext) etk).getOldObject();
	    	}

	    	if (newObject.configuration().getElements() == null) {
                return;
            } else {
            	//TODO Refactor this once core APIs become available to tell if a data element is bound to a lookup.
        	    List<Map<String, Object>> isLookupList = etk.createSQL(
        	    		"select de.business_key as BUSINESS_KEY, ld.business_key as LOOKUP_BUSINESS_KEY, "
        	    		+ "(case when ld.value_return_type is null then 2 else ld.value_return_type end) as VALUE_TYPE "
    	    			+ "from etk_data_element de "
    	    			+ "join etk_data_object do on do.data_object_id = de.data_object_id "
    	    			+ "left join etk_lookup_definition ld on ld.lookup_definition_id = de.lookup_definition_id "
    	    			+ "where do.business_key = :dataObjectKey "
    	    			+ "and do.tracking_config_id = (select max(tracking_config_id) from etk_tracking_config_archive)")
    	    			.setParameter("dataObjectKey", newObject.configuration().getBusinessKey())
    	    			.returnEmptyResultSetAs(new ArrayList<Map<String, Object>>())
    	    			.fetchList();

        	    for (Map<String, Object> record : isLookupList) {
        	    	isLookupDefMap.put((String) record.get("BUSINESS_KEY"), (String) record.get("LOOKUP_BUSINESS_KEY"));
        	    	valueTypeMap.put((String) record.get("BUSINESS_KEY"), (Number) record.get("VALUE_TYPE"));
        	    }


        		for (java.lang.reflect.Method aMethod : newObject.getClass().getMethods()) {
        			if (aMethod.getName().startsWith("get") && (aMethod.getParameterTypes().length == 0)) {
        				try {
        					newObjectValueMap.put(aMethod.getName(), aMethod.invoke(newObject));
        				} catch (Exception e) {
        					throw new ApplicationException (e);
        				}
        			}
        		}

        		newObjectParamMap = getParameterMap(newObject, newObjectValueMap, valueTypeMap);

        		if (isUpdateEvent) {
	        		for (java.lang.reflect.Method aMethod : oldObject.getClass().getMethods()) {
	        			if (aMethod.getName().startsWith("get") && (aMethod.getParameterTypes().length == 0)) {
	        				try {
	        					oldObjectValueMap.put(aMethod.getName(), aMethod.invoke(oldObject));
	        				} catch (Exception e) {
	        					throw new ApplicationException (e);
	        				}
	        			}
	        		}

	        		oldObjectParamMap = getParameterMap(oldObject, oldObjectValueMap, valueTypeMap);
        		}
            }
    	}

    	try {
            //Creation ETP Handler
            if (isCreateEvent) {
                String newValue = "";
                for (DataElement dataElement : newObject.configuration().getElements()) {
                    newValue = lvf.getValue(dataElement, isLookupDefMap, newObjectValueMap, newObjectParamMap);

                    if (!"".equals(newValue)) {

                    	columnName = getColumnName(dataElement);
                    	if (excludedColumns.containsKey(dataObjectTableName + "." + columnName)) {
                    		continue;
                    	}

                    	//EX: "The TIN was set to '12345' for the new Intake Record created by user aUser"
                    	if (logMessagesForCRUD) {
	                    	em.setLength(0);
	                    	em.append("The ");
	                    	em.append(getDataElementName(dataElement));
	                    	em.append(" was set to '");
	                    	em.append(newValue);
	                    	em.append("' for the new ");
	                    	em.append(dataObjectName);
	                    	em.append(" created by user ");
	                    	em.append(user.getAccountName());
	                    	logEntry.setMessage(em.toString());
                    	}

                    	logEntry.setColumnName(columnName);
                    	logEntry.setCrudEventType(LogEventType.CREATE);
                    	logEntry.setDataElementName(getDataElementName(dataElement));
                    	logEntry.setNewDataValue(newValue);
                    	logEntry.setPreviousDataValue("");

                        addLogEntry(logEntry);
                    }
                }
            } else if (isUpdateEvent) {
                String oldValue = "";
                String newValue = "";
                String fieldName = "";
                Object newValRaw = null;
                Object oldValRaw = null;
                boolean valChanged = false;

                for (DataElement dataElement : newObject.configuration().getElements()) {

                	oldValue = "";
                	newValue = "";
                    fieldName = dataElement.getBusinessKey().split("\\.")[FIELD_NAME_POSITION];

                    newValRaw = newObjectValueMap.get("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
                    oldValRaw = oldObjectValueMap.get("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));

                    valChanged = false;
                    if (newValRaw != null) {
                    	valChanged = !newValRaw.equals(oldValRaw);
                    } else if (newValRaw == null && oldValRaw != null) {
                    	valChanged = true;
                    }

                    if (valChanged) {
		                newValue = lvf.getValue(dataElement, isLookupDefMap, newObjectValueMap, newObjectParamMap);
		                oldValue = lvf.getValue(dataElement, isLookupDefMap, oldObjectValueMap, oldObjectParamMap);
                	}

                    if (!oldValue.equals(newValue)) {

                    	columnName = getColumnName(dataElement);
                    	if (excludedColumns.containsKey(dataObjectTableName + "." + columnName)) {
                    		continue;
                    	}

                    	//EX: "The TIN on the Intake Record was updated from '123' to '1234' by user aUser"
                    	if (logMessagesForCRUD) {
	                    	em.setLength(0);
	                    	em.append("The ");
	                    	em.append(getDataElementName(dataElement));
	                    	em.append(" on the ");
	                    	em.append(dataObjectName);
	                    	em.append(" was updated from '");
	                    	em.append(oldValue);
	                    	em.append("' to '");
	                    	em.append(newValue);
	                    	em.append("' by user ");
	                    	em.append(user.getAccountName());
	                    	logEntry.setMessage(em.toString());
                    	}

                    	logEntry.setColumnName(columnName);
                    	logEntry.setCrudEventType(LogEventType.UPDATE);
                    	logEntry.setDataElementName(getDataElementName(dataElement));
                    	logEntry.setNewDataValue(newValue);
                    	logEntry.setPreviousDataValue(oldValue);

                        addLogEntry(logEntry);
                    }
                }
            } else if (isDeleteEvent) {

            	//EX: "The Intake Record with Tracking ID 123 was deleted by user aUser"
            	if (logMessagesForCRUD) {
	            	em.append("The ");
	            	em.append(dataObjectName);
	            	em.append(" with Tracking ID ");
	            	em.append(trackingId);
	            	em.append(" was deleted by user ");
	            	em.append(user.getAccountName());
	            	logEntry.setMessage(em.toString());
            	}

            	logEntry.setColumnName("");
            	logEntry.setCrudEventType(LogEventType.DELETE);
            	logEntry.setDataElementName("");
            	logEntry.setNewDataValue("");
            	logEntry.setPreviousDataValue("");

                addLogEntry(logEntry);
            } else if (isReadEvent) {

            	//EX: "The Intake Record with Tracking ID 123 was read by user aUser"
            	if (logMessagesForCRUD) {
	            	em.append("The ");
	            	em.append(dataObjectName);
	            	em.append(" with Tracking ID ");
	            	em.append(trackingId);
	            	em.append(" was read by user ");
	            	em.append(user.getAccountName());
	            	logEntry.setMessage(em.toString());
            	}

            	logEntry.setColumnName("");
            	logEntry.setCrudEventType(LogEventType.READ);
            	logEntry.setDataElementName("");
            	logEntry.setNewDataValue("");
            	logEntry.setPreviousDataValue("");

                addLogEntry(logEntry);
            }
        } catch (final Throwable t) {
        	Utility.aeaLog(etk, t);

        	throw new RuntimeException ("AeaAuditLog.logETPChanges() failed to process successfully."
        			                  + "Please verify that all lookup definitions on the data form have valid "
        			                  + "forView() lookup contexts.", t);
        }
    }

    /**
     * Returns a value map of all form element values converted from Java to SQL type values.
     *
     * @param tdo The tracked data object instance.
     * @param values Raw values from reflection calls to newObject / oldObject
     * @param valueTypeMap Map containing what "type" of data element ETK thinks
     *                     the elements are (text, long text, number, etc.)
     *
     * @return A SQL typed map of values. Boolean Yes/No converted to int, Lists include a non-null value to ensure they
     * do not cause exceptions.
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Object> getParameterMap (DataObjectInstance tdo,
    		                                     Map<String, Object> values,
    		                                     Map<String, Number> valueTypeMap) {

    	final Map<String, Object> parameterMap = new HashMap<>();
		parameterMap.put("trackingId", trackingId);
		parameterMap.put("parentId", parentId);
		parameterMap.put("baseId", baseId);
		parameterMap.put("currentUser.id", etk.getCurrentUser().getId());
		parameterMap.put("currentUser.ouId", etk.getCurrentUser().getHierarchy().getId());
		parameterMap.put("currentUser.roleId", etk.getCurrentUser().getRole().getId());

		String elementFieldName = null;
		Object tmpVal = null;
		List valueList = null;
		DataElementType lookupValueReturnType = null;

		for (DataElement anElement : tdo.configuration().getElements()) {

		    elementFieldName = anElement.getBusinessKey().split("\\.")[FIELD_NAME_POSITION];
		    tmpVal = values.get("get" + elementFieldName.substring(0, 1).toUpperCase() + elementFieldName.substring(1));

		    if (anElement.getDataType() == DataType.YES_NO) {
		    	if (Boolean.TRUE.equals(tmpVal)) {
		    		tmpVal = 1;
		    	} else if (Boolean.FALSE.equals(tmpVal)) {
		    		tmpVal = 0;
		    	} else {
		    		tmpVal = null;
		    	}
		    } else if (anElement.isMultiValued() && (tmpVal != null) && (tmpVal instanceof List)) {

		    	valueList = new ArrayList();
		    	lookupValueReturnType = DataElementType.getDataElementType(valueTypeMap.get(anElement.getBusinessKey()).intValue());

		    	if (tmpVal == null || ((List) tmpVal).isEmpty()) {
		    		if (DataElementType.NUMBER == lookupValueReturnType) {
		    			valueList.add(Integer.MIN_VALUE);
		    		} else {
		    			valueList.add(Integer.MIN_VALUE + "");
		    		}
		    	} else {
		    		if (DataElementType.NUMBER == lookupValueReturnType) {
		    			for (Object anObject : (List) tmpVal) {
				    		if (anObject != null) {
				    			valueList.add(new Integer(anObject + ""));
				    		} else {
				    			valueList.add(null);
				    		}
				    	}
		    		} else {
		    			valueList = (List) tmpVal;
		    		}
		    	}

		    	tmpVal = valueList;

		    }

		    parameterMap.put(elementFieldName, tmpVal);
		}

		return parameterMap;
    }

    /**
     * Writes the entire batch of AeaLogEntries to the AEA Audit Log RDO.
     */
    public void writeAuditLogEntries() {
       //Informally deprecated, not currently used, may be removed (or used again) in future release.
    }

    /**
     * Close the connection.
     */
    public void closeConnection() {
    	//Informally deprecated, not currently used, may be removed (or used again) in future release.
    }

}