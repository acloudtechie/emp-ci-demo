package net.micropact.aea.rf.utility.twoWayMultiselect;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.entellitrak.DataEventType;
import com.entellitrak.DataObjectEventContext;
import com.entellitrak.FormInfo;
import com.entellitrak.configuration.DataElement;
import com.entellitrak.form.FormEventContext;

import net.micropact.aea.utility.Utility;

/**
 * This class contains utility methods for managing when a multiselect refers to another object in entellitrak
 * and you want to be able to manage that data from either place. A specific example is RfTransition and RfState.
 * There is a multiselect as part of RfTransition which points to RfState but the user would like to update the
 * RfTransitions for a particular RfState. This is managed by adding an unbound field to RfState, calling
 * {@link TwoWayMultiselectUtility#populateUnboundMultiselect(FormEventContext, ITwoWayMultiselect)} in a read handler,
 * then calling {@link TwoWayMultiselectUtility#parseUnboundMultiselects(DataObjectEventContext, ITwoWayMultiselect)}
 * in the Data Object Event Handler.
 *
 * @author zmiller
 */
public final class TwoWayMultiselectUtility {

    /**
     * Utility classes do not need a constructor.
     */
    private TwoWayMultiselectUtility(){}

    /**
     * This method should be called an a Form Read Handler. It will populate an unbound multiselect with the appropriate
     * values that it pulls from the database.
     *
     * @param etk entellitrak execution context
     * @param twoWayMultiselect describes the fields which are being linked
     */
    public static void populateUnboundMultiselect(final FormEventContext etk,
            final ITwoWayMultiselect twoWayMultiselect){

        final DataElement referencedElement = twoWayMultiselect.getReferencedElement();

        final Long trackingId = etk.getTrackingId();

        final List<String> valuesToSet = new LinkedList<>();

        final List<Map<String, Object>> alreadySelected = etk.createSQL(
                String.format("SELECT ID_OWNER FROM %s WHERE %s = :trackingId",
                        referencedElement.getTableName(),
                        referencedElement.getColumnName()))
                    .setParameter("trackingId", trackingId)
                    .fetchList();

        for(final Map<String, Object> selected : alreadySelected){
            valuesToSet.add(selected.get("ID_OWNER").toString());
        }

        etk.getElement(twoWayMultiselect.getFieldName()).setValues(valuesToSet);
    }

    /**
     * This method should be called from a Data Object Event Handler. It will store the values in the unbound
     * multiselect to the database.
     *
     * @param etk entellitrak execution context
     * @param twoWayMultiselect describes the fields which are being linked
     */
    public static void parseUnboundMultiselects(final DataObjectEventContext etk,
            final ITwoWayMultiselect twoWayMultiselect){
        final long objectBeingSavedId = etk.getNewObject().properties().getId();

        if(DataEventType.CREATE == etk.getDataEventType()
                || DataEventType.UPDATE == etk.getDataEventType()){
            final FormInfo formInfo = etk.getForm();
            /* Get the selected effects */
            final String[] newValues = formInfo.getValues(twoWayMultiselect.getFieldName());
            /* Delete all existing effects */
            etk.createSQL(String.format("DELETE FROM %s WHERE %s = :objectBeingSavedId",
                    twoWayMultiselect.getReferencedElement().getTableName(),
                    twoWayMultiselect.getReferencedElement().getColumnName()))
            .setParameter("objectBeingSavedId", objectBeingSavedId)
            .execute();
            /* The only reason this loop is here is because at the moment, it'd be a pain to do the LIST_ORDER.
             * Actually, I think the previous sentence might actually be completely wrong */
            if(newValues != null){
                for(int i = 0; i < newValues.length; i++){
                    if(Utility.isSqlServer(etk)){
                        etk.createSQL(String.format("INSERT INTO %s(id_owner, list_order, %s) VALUES (:valueId, :listOrder, :referenceId)",
                                twoWayMultiselect.getReferencedElement().getTableName(),
                                twoWayMultiselect.getReferencedElement().getColumnName()))
                        .setParameter("valueId", newValues[i])
                        .setParameter("listOrder", i+1)
                        .setParameter("referenceId", objectBeingSavedId)
                        .execute();
                    }else{
                        etk.createSQL(String.format("INSERT INTO %s(id, id_owner, list_order, %s) VALUES (OBJECT_ID.NEXTVAL, :valueId, :listOrder, :referenceId)",
                                twoWayMultiselect.getReferencedElement().getTableName(),
                                twoWayMultiselect.getReferencedElement().getColumnName()))
                        .setParameter("valueId", newValues[i])
                        .setParameter("listOrder", i+1)
                        .setParameter("referenceId", objectBeingSavedId)
                        .execute();
                    }
                }
            }
        }
    }
}
