package net.micropact.aea.core.deserializer;

/**
 * Deserializer which just returns the value which was passed in.
 *
 * @author zmiller
 */
public class IdentityDeserializer implements IDeserializer<String> {

    @Override
    public String deserialize(final String value) {
        return value;
    }
}