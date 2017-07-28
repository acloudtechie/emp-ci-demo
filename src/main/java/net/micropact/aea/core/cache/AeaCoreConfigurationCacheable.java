package net.micropact.aea.core.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;

import net.entellitrak.aea.core.cache.AClassKeyCacheable;
import net.micropact.aea.core.enums.AeaCoreConfigurationItem;

/**
 * This class is an implementation of {@link net.entellitrak.aea.core.cache.ICacheable} for storing the values of
 * AEA_CORE_CONFIGURATION.
 *
 * @author zachary.miller
 */
public class AeaCoreConfigurationCacheable extends AClassKeyCacheable<Map<String, Object>> {

    private final ExecutionContext etk;

    /**
     * Constructor for AeaConfigurationCacheable.
     *
     * @param executionContext entellitrak execution context
     */
    public AeaCoreConfigurationCacheable(final ExecutionContext executionContext) {
        etk = executionContext;
    }

    @Override
    public Map<String, Object> getValue() throws ApplicationException {
        final Map<String, Object> map = new HashMap<>();
        for(final AeaCoreConfigurationItem configurationItem : AeaCoreConfigurationItem.values()){
            if(configurationItem.isCacheable()){
                try{
                    final List<Map<String, Object>> results = etk.createSQL("SELECT C_VALUE FROM t_aea_core_configuration WHERE c_code = :theCode")
                            .setParameter("theCode", configurationItem.getCode())
                            .fetchList();

                    final String value;

                    if(results.size() == 0){
                        /* At the moment, this cannot use Utility.aeaLog because it gets a StackOverflowError
                         * because when an item doesn't exist, it tries to write to the log, which tries to
                         * reload all the items, which tries to write to the log, etc. */
                        etk.getLogger().error(
                                String.format("Did not find value in AEA Core Configuration RDO with code \"%s\". The value is being defaulted to null but you should configure the desired value.",
                                        configurationItem.getCode()));
                        value = null;
                    }else if(results.size() == 1){
                        value = (String) results.get(0).get("C_VALUE");
                    }else{
                        throw new ApplicationException(String.format("Found multiple entries in the AEA Core Configuration RDO with code \"%s\"",
                                configurationItem.getCode()));
                    }

                    map.put(configurationItem.getCode(),
                            configurationItem.getDeserializer().deserialize(value));
                }catch(final Exception e){
                    throw new ApplicationException(String.format("An error was encountered trying to get the configuration value for the entry with code \"%s\" from the AEA CORE Configuration Reference Data List", configurationItem.getCode()),
                            e);
                }
            }
        }
        return map;
    }
}