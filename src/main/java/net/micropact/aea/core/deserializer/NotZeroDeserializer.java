package net.micropact.aea.core.deserializer;

/**
 * This deserializer returns true if the value is not the String &quot;0&quot;.
 *
 * @author zmiller
 */
public class NotZeroDeserializer implements IDeserializer<Boolean>{

    @Override
    public Boolean deserialize(final String value) {
        return !"0".equals(value);
    }
}
