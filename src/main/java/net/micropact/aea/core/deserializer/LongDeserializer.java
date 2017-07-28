package net.micropact.aea.core.deserializer;

import com.entellitrak.legacy.util.StringUtility;

/**
 * This class is capable of turning a value into a Long.
 *
 * @author zmiller
 */
public class LongDeserializer implements IDeserializer<Long>{

    private final Long defaultValue;

    /**
     * Constructs a {@link LongDeserializer} which will return theDefaultValue if the String it attempts to deserialize
     * is blank.
     *
     * @param theDefaultValue the default value which should be used if the value was blank.
     */
    public LongDeserializer(final Long theDefaultValue){
        defaultValue = theDefaultValue;
    }

    @Override
    public Long deserialize(final String value) {
        return StringUtility.isBlank(value)
                ? defaultValue
                : (Long) Long.parseLong(value);
    }
}
