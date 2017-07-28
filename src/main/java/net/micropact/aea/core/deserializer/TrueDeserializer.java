package net.micropact.aea.core.deserializer;

/**
 * This deserializer determines whether the value is the string &quot;true&quot;.
 *
 * @author zmiller
 */
public class TrueDeserializer implements IDeserializer<Boolean> {

    @Override
    public Boolean deserialize(final String value) {
        return "true".equals(value);
    }
}
