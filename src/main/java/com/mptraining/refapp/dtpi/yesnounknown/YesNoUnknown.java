/**
 *
 * Data Type Plug-in
 *
 * administrator 09/28/2016
 **/

package com.mptraining.refapp.dtpi.yesnounknown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.entellitrak.configuration.AbstractDataElementType;
import com.entellitrak.configuration.FormControlModel;
import com.entellitrak.configuration.NumberColumn;
import com.entellitrak.configuration.StringValues;
import com.entellitrak.configuration.TypedValues;
import com.entellitrak.configuration.ValuesFactory;
import com.entellitrak.form.FormControlHelper;
import com.entellitrak.search.advanced.OperatorTypeEnum;
import com.mptraining.refapp.dtpi.yesnounknown.control.RadioOptionHelper;

/**
 * Definition of the YesNoUnknown type.
 */
public class YesNoUnknown extends AbstractDataElementType<Integer> {
    /**
     * Constructor.  This takes the list of database columns that will represent the type.
     * This type uses a single Number column.
     */
    public YesNoUnknown() {
        super(new NumberColumn("yesNoUnknown"));
    }

    /**
     * Returns the name of the data type.  This name will be displayed in the
     * Data Type dropdown when creating a new data element.
     *
     * @return Returns the name of the data type.
     */
    @Override
	public String getName() {
        return "Yes/No/Unknown";
    }

    /**
     * Gets the domain object type.  This type returns a Long.
     *
     * @return the object type
     */
    @Override
	public Class<Integer> getObjectType() {
        return Integer.class;
    }

    /**
     * Returns the form control type that should be displayed on the data form.
     * This returns an instance of YesNoUnknownFormControl, which defines the script
     * objects containing the form control templates.
     *
     * @return the form control model
     */
    @Override
	public FormControlModel getFormControlModel(){
        return new YesNoUnknownFormControl();
    }

    /**
     * Builds the domain object from a set of database values.
     * The columns and value types returned from this function correspond to the
     * data model that was set up in the constructor.  In this case, there
     * will be one column of type Long.  Since our domain object is also a Long,
     * all we have to do is return the value of this column.
     *
     * @param columnValues a set of Object values loaded from the database
     * @return one or more Strings representing the element value
     */
    @Override
	public Integer loadValueObject(TypedValues columnValues) {
        return columnValues.get("yesNoUnknown", Integer.class);
    }

    /**
     * Builds the domain object from a set of Strings submitted from the form.
     * The StringValues object contains a map of values that were submitted from
     * the form control; the keys correspond to the field names in getFieldNames().
     * By default, there is a single entry with a key of "" (empty string).
     * The syntax "values.getValue()" is a shortcut to return this single entry.
     *
     * This function gets the single String field submitted from the form,
     * and converts it to a Long.
     *
     * @param values the String values submitted from the form.
     * @return the domain object.
     */
    @Override
	public Integer buildValueObject(StringValues values) {
        if (StringUtils.isBlank(values.getValue())) {
            return null;
        } else {
            return Integer.valueOf(values.getValue());
        }
    }

    /**
     * Generates String values to display on the form.
     * This function takes the domain object and converts it to a String
     * for form display.
     *
     * @param valueObject the value object
     * @return one or more Strings to populate the fields on the form control
     */
    @Override
	public StringValues getFormValues(Integer valueObject) {
        return ValuesFactory.getStringValues(nullSafeToString(valueObject));
    }

    /**
     * Generates a helper class for rendering the form control.
     * Here we are returning a custom class (RadioOptionHelper) that will
     * hold the values for all the radio button options.
     *
     * @param values the String values for display
     * @return the form control helper
     */
    @Override
	public FormControlHelper getFormControlHelper(StringValues values) {
       return new RadioOptionHelper(values.getValue());
    }

    /**
     * Generates a string to be displayed on the view/listing.
     * This will display the strings "Yes", "No", or "Unknown" depending on the value.
     *
     * @param valueObject the value object
     *
     * @return a display String.
     */
    @Override
	public String getViewString(Integer valueObject) {
        if (valueObject == null) {
            return "";
        } else if (valueObject.equals(1)) {
            return "Yes";
        } else if (valueObject.equals(0)) {
            return "No";
        } else {
            return "Unknown";
        }
    }

    @Override
    public Collection<String> validateFormValues(StringValues values) {
    	List<String> errors = new ArrayList<String>();
    	return errors;
    }

    @Override
    public Set<OperatorTypeEnum> getOperatorTypes() {
		return new LinkedHashSet<OperatorTypeEnum>(Arrays.asList(
				OperatorTypeEnum.EQUALS, OperatorTypeEnum.NOT_EQUALS,
				OperatorTypeEnum.IS_NULL));
	}
}