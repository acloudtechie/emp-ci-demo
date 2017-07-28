package net.micropact.aea.core.dataTypePlugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.IncorrectResultSizeDataAccessException;
import com.entellitrak.configuration.DataElementType;
import com.entellitrak.configuration.FormControlModel;
import com.entellitrak.configuration.StringValues;
import com.entellitrak.configuration.ValuesFactory;
import com.entellitrak.form.FormControlData;
import com.micropact.ExecutionContextImpl;
import com.micropact.entellitrak.cfg.model.DataElement;
import com.micropact.entellitrak.cfg.model.DataObject;
import com.micropact.entellitrak.cfg.model.DataType;
import com.micropact.entellitrak.engine.RunTimeEngine;
import com.micropact.entellitrak.system.UserContainer;
import com.micropact.entellitrak.web.HandlebarsFacade;
import com.micropact.entellitrak.web.taglib.FormControlDataImpl;

import net.entellitrak.aea.core.cache.AClassKeyCacheable;
import net.entellitrak.aea.core.cache.CacheManager;

/**
 *
 * Methods for utilizing the class associated with data elements that use Data Type Plugins.
 *
 * @author ahargrave
 **/
public final class DataTypePluginClassUtility {

    /**
     * Utility classes do not need public constructors.
     */
    private DataTypePluginClassUtility(){}

    /**
	 * Generates a Map&lt;String, String&gt; of Data Element business keys
	 * to fully qualified class names of the main Data Type Plugin
	 * class used for the given Data Element.  If the map is not
	 * already cached it will run a query to create it, then cache
	 * it for later use.
	 *
	 * @param etk entellitrak execution context
	 * @return A mapping from data element business keys of plugins to fully qualified class names which contain the
	 *         implementation
	 * @throws ApplicationException If there was an underlying exception
	 */
	public static Map<String, String> loadCachedElementToClassMap(final ExecutionContext etk) throws ApplicationException{
		return CacheManager.load(etk, new AClassKeyCacheable<Map<String, String>>() {
            @Override
            public Map<String, String> getValue() throws ApplicationException {
		    	List<Map<String, Object>> keyToClassNameList = etk.createSQL("SELECT e.BUSINESS_KEY, CLASS_NAME " +
						"FROM ETK_DATA_ELEMENT e " +
						"LEFT JOIN etk_plugin_registration p " +
						"ON p.plugin_registration_id = e.plugin_registration_id " +
						"WHERE e.BUSINESS_KEY IS NOT NULL AND CLASS_NAME IS NOT NULL AND p.TRACKING_CONFIG_ID    = " +
						"  (SELECT tracking_config_id " +
						"  FROM etk_tracking_config " +
						"  WHERE config_version = " +
						"    (SELECT MAX(config_version) FROM etk_tracking_config " +
						"    ) " +
						"  )")
				.fetchList();

				Map<String, String> keyToClassName = new HashMap<>();
				for (Map<String, Object> map : keyToClassNameList) {
					keyToClassName.put(map.get("BUSINESS_KEY").toString(), map.get("CLASS_NAME").toString());
				}

				return keyToClassName;
            }
        });
	}

	/**
	 * Creates an instance of the main Data Type Plugin class
	 * associated with the Data Element with the given data
	 * element business key.
	 *
	 * @param etk entellitrak execution context
	 * @param dataElementBusinessKey business key of the data element
	 * @return The class which contains the data-type plugin's implementation
	 * @throws ApplicationException If there was an underlying exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static DataElementType instantiatePluginClass(final ExecutionContext etk, String dataElementBusinessKey) throws ApplicationException{
		try {
			return ((Class<DataElementType>) Class.forName(loadCachedElementToClassMap(etk).get(dataElementBusinessKey))).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ApplicationException(e);
		}
	}

	/**
	 * Converts the given {@link String} value to whatever class is
	 * needed by the Data Type Plugin main class associated to the
	 * Data Element Business Key.  The converted value is then passed
	 * through getViewString of the Data Type Plugin main class to
	 * produce the proper display value.
	 *
	 * @param etk entellitrak execution context
	 * @param dataElementBusinessKey The business key of the data element
	 * @param value the raw value of the data element
	 * @return The display string which would be used in the view.
	 * @throws ApplicationException If there was an underlying Exception
	 */
	public static String getDataTypePluginDisplayFromStringValue(final ExecutionContext etk, String dataElementBusinessKey, final String value) throws ApplicationException{
		DataElementType mainPluginClassObject = DataTypePluginClassUtility.instantiatePluginClass(etk, dataElementBusinessKey);
		Object finalValue = mainPluginClassObject.buildValueObject(
			new StringValues(){
				@Override
				public Set<String> getKeys() {
					return null;
				}
				@Override
				public String getValue() {
					return value;
				}
				@Override
				public String getValue(String key) {
					return value;
				}
				@Override
				public List<String> getValues() {
					return Arrays.asList(value);
				}
				@Override
				public List<String> getValues(String key) {
					return Arrays.asList(value);
				}
			}
		);
		return mainPluginClassObject.getViewString(finalValue);
   }

	/**
	 * **BETA** Method to return a Custom Data Type's HTML widget. Relies heavily on the private APIs.
	 *
	 * @param etk entellitrak execution context
	 * @param dataObjectBusinessKey business key of the data object which the element belongs to
	 * @param dataElementBusinessKey business key of the data element
	 * @param formControlName The name of the form control (including the underscore. ie: Person_SSN)
	 * @param formControlId A Form Control Id which has the custom data type bound to it
	 * @param currentValue The current value of the data element
	 * @param getJS flag indicating whether this method should include the custom js
	 * @return The HTML representation of the data type plugin
	 * @throws IOException If there was an underlying {@link IOException}
	 * @throws IncorrectResultSizeDataAccessException
	 *         If there was an underlying {@link IncorrectResultSizeDataAccessException}
	 */
	public static String getPluginDataTypeWidget(ExecutionContext etk,
			String dataObjectBusinessKey,
			String dataElementBusinessKey,
			String formControlName,
			Long formControlId,
			String currentValue,
			boolean getJS) throws IOException, IncorrectResultSizeDataAccessException {
		UserContainer userContainer = ((ExecutionContextImpl) etk).getUserContainer();

		final DataObject dataObject = RunTimeEngine.getInstance().getTrackingConfig()
				.getDataObjectByBusinessKey(dataObjectBusinessKey);

		DataElement dataElement = dataObject.getDataElementByBusinessKey(dataElementBusinessKey);

		if (dataElement.getDataType() == DataType.PLUGIN_BASED && dataElement.getPluginRegistration() != null) {

			final DataElementType<?> dataElementType = dataElement.getDataElementType();
			FormControlModel formControlModel = dataElementType.getFormControlModel();

			String code = null;

			if (getJS) {
				code = formControlModel.getJsIncludeScript();
			} else {
				if (userContainer.isAccessibilityEnhanced()) {
					code = formControlModel.getAccessibleEditableControlScript();
				} else {
					code = formControlModel.getEditableControlScript();
				}
			}

			FormControlData formControlData = convertRuntimeControlToControlData(formControlId, etk);
			formControlData.setValues(ValuesFactory.getStringValues(currentValue));
			formControlData.setHelper(dataElementType
					.getFormControlHelper(ValuesFactory.getStringValues(currentValue)));
			formControlData.setControlName(formControlName);

			HandlebarsFacade facade = new HandlebarsFacade();
			return facade.transformTemplate(code, formControlData);
		}

		return "";
	}

	private static FormControlData convertRuntimeControlToControlData(Long formControlId,
																      ExecutionContext etk) throws IncorrectResultSizeDataAccessException {

        FormControlData formControlData = new FormControlDataImpl();

        Map<String, Object> formControl =
        	etk.createSQL(
        		"select HEIGHT, WIDTH, TOOLTIP_TEXT from ETK_FORM_CONTROL where FORM_CONTROL_ID = :FORM_CONTROL_ID")
        		.setParameter("FORM_CONTROL_ID", formControlId)
        		.returnEmptyResultSetAs(new HashMap<String, Object> ())
        		.fetchMap();

        if (formControl.get("HEIGHT") != null) {
            formControlData.addAttribute("height", formControl.get("HEIGHT").toString());
        }
        if (formControl.get("WIDTH") != null) {
            formControlData.addAttribute("width", formControl.get("WIDTH").toString());
        }
        if (formControl.get("TOOLTIP_TEXT") != null) {
            formControlData.addAttribute("title", formControl.get("TOOLTIP_TEXT"));
        }

        formControlData.addAttribute("x", "0");
        formControlData.addAttribute("y", "0");
        formControlData.addAttribute("required", false);
        formControlData.addAttribute("update", false);

        return formControlData;
    }
}
