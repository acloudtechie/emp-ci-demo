package net.micropact.aea.du.page.duplicateCodeValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.DataElementType;
import net.micropact.aea.utility.DataObjectType;
import net.micropact.aea.utility.IJson;
import net.micropact.aea.utility.JsonUtilities;
import net.micropact.aea.utility.Utility;

/**
 * This page gets information about all records in the system which have a value in the code element which is the same
 * as another record of the same type.
 *
 * @author zmiller
 */
public class DuplicateCodeValuesController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {

        final TextResponse response = etk.createTextResponse();

        response.put("dataObjects", JsonUtilities.encode(getDataObjects(etk)));

        return response;
    }

    /**
     * Gets all of the objects which have duplicate values.
     *
     * @param etk Entellitrak Execution Context
     * @return The list (sorted by # of duplicates and then by name) of duplicate data objects.
     */
    public static List<DataObject> getDataObjects(final ExecutionContext etk) {
        final List<DataObject> result = new LinkedList<>();

        final List<Map<String, Object>> dataObjectsInfos = etk.createSQL("SELECT dataObject.TABLE_NAME, dataObject.NAME, dataObject.DATA_OBJECT_ID, dataElement.COLUMN_NAME, dataObject.business_key DATAOBJECTBUSINESSKEY, dataObject.OBJECT_TYPE FROM etk_data_object dataObject JOIN etk_data_element dataElement ON dataElement.data_object_id = dataObject.data_object_id WHERE dataObject.tracking_config_id = (SELECT MAX (tracking_config_id) FROM etk_tracking_config_archive ) AND dataElement.element_name = :dataElementName AND dataElement.data_type IN (:dataTypes) ORDER BY dataObject.table_name")
                .setParameter("dataElementName", "code")
                .setParameter("dataTypes", Arrays.asList(DataElementType.TEXT.getEntellitrakNumber(),
                        DataElementType.LONG_TEXT.getEntellitrakNumber()))
                .fetchList();

        for(final Map<String, Object> dataObjectInfo : dataObjectsInfos){
            final DataObject dataObject = new DataObject(etk,
                    (String) dataObjectInfo.get("TABLE_NAME"),
                    (String) dataObjectInfo.get("COLUMN_NAME"),
                    (String) dataObjectInfo.get("NAME"),
                    (String) dataObjectInfo.get("DATAOBJECTBUSINESSKEY"),
                    DataObjectType.getDataObjectType(((Number) dataObjectInfo.get("OBJECT_TYPE")).longValue()));

            if(dataObject.getDuplicates().size() > 0){
                result.add(dataObject);
            }
        }

        result.sort((o1, o2) -> {
            final int firstComp = ((Integer) o1.getDuplicates().size())
                    .compareTo(o2.getDuplicates().size());

            if(firstComp != 0){
                return firstComp;
            }else{
                return o1.getName().compareTo(o2.getName());
            }
        });

        return result;
    }


    /**
     * This class represents a Data Object and all of the duplicate records.
     *
     * @author zmiller
     */
    public static class DataObject implements IJson{

        private final String name;
        private DataObjectType objectType;
        private final List<Duplicate> duplicates;

        /**
         * This constructor will generate a record for the data object and find all of the duplicates.
         * The reason that the constructor takes so many 'duplicate' parameters is to keep it from having to
         * do an additional query.
         *
         * @param etk Entellitrak Execution Context.
         * @param tableName Table Name of the data object
         * @param columnName Column Name of the Code column. (It is not always c_code since it actually
         *  uses the element name and not the column name to find duplicates.)
         * @param dataObjectName Name of the Data Object
         * @param dataObjectBusinessKey Business Key of the Data Object
         * @param dataObjectType Type of the Data Object
         */
        public DataObject(final ExecutionContext etk,
                final String tableName,
                final String columnName,
                final String dataObjectName,
                final String dataObjectBusinessKey,
                final DataObjectType dataObjectType) {
            name = dataObjectName;
            objectType = dataObjectType;

            final ArrayList<Map<String, Object>> objectInfos =
                    new ArrayList<>(etk.createSQL("SELECT grouped.ID, grouped.CODE, grouped.COUNT FROM ( SELECT ID, "+columnName+" CODE, COUNT(*) OVER (PARTITION BY "+columnName+") COUNT FROM "+tableName+" ) grouped WHERE grouped.COUNT > 1 ORDER BY grouped.COUNT DESC, grouped.CODE, grouped.ID")
                            .fetchList());

            duplicates = new LinkedList<>();

            int index = 0;

            while (index < objectInfos.size()){

                final Map<String, Object> object = objectInfos.get(index);
                final String codeValue = (String) object.get("CODE");
                final long count = ((Number) object.get("COUNT")).longValue();

                final List<DuplicateObject> duplicateDataObjects = new LinkedList<>();

                for(int i = index; i < index + count;i++){
                    duplicateDataObjects.add(new DuplicateObject(((Number) objectInfos.get(i).get("ID")).longValue(),
                            dataObjectBusinessKey,
                            dataObjectType));
                }

                duplicates.add(new Duplicate(codeValue, duplicateDataObjects));
                index += count;
            }

            Collections.sort(duplicates, (o1, o2) -> {
                final int firstComp = ((Integer) o1.getDuplicateObjects().size())
                        .compareTo(o2.getDuplicateObjects().size());
                if(firstComp != 0){
                    return firstComp;
                }else{
                    return o1.getCode().compareTo(o2.getCode());
                }
            });
        }

        /**
         * Gets the Data Object's name.
         *
         * @return The data object name.
         */
        public String getName(){
            return name;
        }

        /**
         * Gets the list of duplicate codes.
         *
         * @return List of duplicate codes.
         */
        public List<Duplicate> getDuplicates(){
            return duplicates;
        }

        /**
         * Gets the data object type.
         *
         * @return The Data Object Type
         */
        public DataObjectType getDataObjectType(){
            return objectType;
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"name", name},
                {"objectType", objectType},
                {"duplicates", duplicates}
            }));
        }
    }

    /**
     * This class represents a particular duplicate code value.
     * It also stores the list of records which actually have the duplicate value.
     *
     * @author zmiller
     */
    public static class Duplicate implements IJson{

        private final String code;
        private final List<DuplicateObject> duplicateObjects;

        /**
         * Constructor for Duplicate.
         *
         * @param codeValue Value of the code element which this duplicate represents.
         * @param duplicateDataObjects A list of data objects which have codeValue as their code.
         */
        Duplicate(final String codeValue, final List<DuplicateObject> duplicateDataObjects){
            code = codeValue;
            duplicateObjects = duplicateDataObjects;
        }

        /**
         * Gets the Code.
         *
         * @return The code this duplicate represents.
         */
        public String getCode(){
            return code;
        }

        /**
         * Gets the list of records which match the code.
         *
         * @return List of Records which have this code.
         */
        public List<DuplicateObject> getDuplicateObjects(){
            return duplicateObjects;
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"code", code},
                {"duplicateObjects", duplicateObjects}
            }));
        }
    }

    /**
     * Represents a single duplicate record.
     *
     * @author zmiller
     */
    public static class DuplicateObject implements IJson{

        private final long id;
        private final DataObjectType objectType;
        private final String businessKey;

        /**
         * Constructor for DuplicateObject.
         *
         * @param trackingId Tracking Id of the record.
         * @param objectBusinessKey Business Key of the record
         * @param dataObjectType Data Object Type of the record.
         */
        public DuplicateObject(final long trackingId, final String objectBusinessKey, final DataObjectType dataObjectType){
            id = trackingId;
            objectType = dataObjectType;
            businessKey = objectBusinessKey;
        }

        /**
         * Gets a relative URL to access this particular record.
         *
         * @return Relative URL to open up this particular record.
         */
        private String getUrl(){
            switch (objectType) {
                case TRACKING:
                    return String.format("workflow.do?dataObjectKey=%s&trackingId=%s",
                            businessKey, id);
                case REFERENCE:
                    return String.format("admin.refdata.update.request.do?dataObjectKey=%s&trackingId=%s",
                            businessKey, id);
                case ESCAN:
                    return String.format("escan.data.update.request.do?dataObjectKey=%s&trackingId=%s",
                            businessKey, id);
                default:
                    throw new RuntimeException(String.format("NonExhaustive Pattern: No matching data object type: %s ", objectType));
            }
        }

        @Override
        public String encode() {
            return JsonUtilities.encode(Utility.arrayToMap(String.class, Object.class, new Object[][]{
                {"id", id},
                {"url", getUrl()},
            }));
        }
    }
}
