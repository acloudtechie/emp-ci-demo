package net.micropact.aea.utility;


/**
 * This interface represents things which can be encoded as JSON.
 * You may implement this interface to have the {@link JsonUtilities} be able to encode your own objects.
 *
 * @author zmiller
 * @see JsonUtilities#encode(Object)
 */
public interface IJson {

    /**
     * Convert the object to a JSON String. This method may change in the future to take a StringBuilder and return
     * void, but for now we will stick to the simple version (Just return the String).
     *
     * @return A JSON String representation of the object.
     */
    String encode();
}
