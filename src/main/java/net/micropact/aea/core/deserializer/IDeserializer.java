package net.micropact.aea.core.deserializer;

/**
 * This represents objects which are capable of deserializing String values into another type.
 *
 * @param <T> The type of the object produced through deserialization.
 * @author zmiller
 */
public interface IDeserializer<T>{

    /**
     * Deserialize a String value into a corresponding value of type T.
     *
     * @param value the value which should be deserialized
     * @return the result of deserializing the value
     */
    T deserialize(String value);
}
