/**
 *
 * JsonUtilities
 *
 * alee 08/18/2014
 **/

package net.micropact.aea.utility;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This class contains JSON related functionality. Primarily converting java objects to a JSON format.
 *
 * @author Zachary.Miller
 */
public final class JsonUtilities {

    /**
     * Utility classes do not need public constructors.
     */
    private JsonUtilities(){}

    /**
     * This method converts an Object to a JSON representation.
     * It has built in support for:
     * <ul>
     *   <li>null</li>
     *   <li>{@link BigDecimal}</li>
     *   <li>{@link Map}</li>
     *   <li>{@link List}</li>
     *   <li>{@link Short}</li>
     *   <li>{@link Long}</li>
     *   <li>{@link Integer}</li>
     *   <li>{@link Double}</li>
     *   <li>{@link Float}</li>
     *   <li>{@link Boolean}</li>
     *   <li>{@link Date}</li>
     *   <li>{@link IJson}</li>
     * </ul>
     *
     * If you want a custom type to be converted you must
     * implement the {@link IJson} interface.
     *
     * @param obj Object to be converted to JSON
     * @return A valid JSON String representing the object
     * @see IJson
     */
    public static String encode(final Object obj){
        final StringBuilder builder = new StringBuilder();
        encode(obj, builder);
        return builder.toString();
    }

    /**
     * Encodes a JSON object and adds the result to the end of a StringBuilder accumulator.
     *
     * @param obj object to be encoded
     * @param builder builder to append the object json to
     */
    private static void encode(final Object obj, final StringBuilder builder){
        if(obj == null){
            builder.append("null");
        }else if(obj instanceof Map){

            builder.append('{');

            final Iterator<? extends Map.Entry<?, ?>> mapEntries = ((Map<?, ?>) obj).entrySet().iterator();

            if(mapEntries.hasNext()){
                encodeObjectFragment(mapEntries.next(), builder);
            }

            while(mapEntries.hasNext()){
                builder.append(',');
                encodeObjectFragment(mapEntries.next(), builder);
            }

            builder.append('}');
        }else if(obj instanceof Stream<?>){
            builder.append('[');

            final Iterator<?> iterator = ((Stream<?>) obj).iterator();

            if(iterator.hasNext()){
                encode(iterator.next(), builder);
            }

            while(iterator.hasNext()){
                builder.append(',');
                encode(iterator.next(), builder);
            }

            builder.append(']');
        }else if(obj instanceof List){
            encode(((List<?>) obj).stream(), builder);
        }else if(obj instanceof BigDecimal){
            builder.append(escapeString(((BigDecimal) obj).toPlainString()));
        }else if(obj instanceof Integer || obj instanceof Long || obj instanceof Short){
            builder.append(escapeString(obj.toString()));
        }else if(obj instanceof Double || obj instanceof Float){
            builder.append(escapeString(obj.toString()));
        }else if(obj instanceof Boolean){
            builder.append(escapeString(obj.toString()));
        }else if (obj instanceof Date) {
            encode(new SimpleDateFormat("MM/dd/yyyy hh:mm a").format((Date) obj), builder);
        }else if(obj instanceof IJson){
            builder.append(((IJson) obj).encode());
        }else{
            builder.append(String.format("\"%s\"",
                    escapeString(obj.toString())));
        }
    }

    /**
     * Encodes a single {@link java.util.Map.Entry} as JSON and appends the result to a StringBuilder.
     *
     * @param entry Map Entry to be encoded.
     * @param builder builder to append the json onto.
     */
    private static void encodeObjectFragment(final Map.Entry<?, ?> entry, final StringBuilder builder){
        encode(entry.getKey().toString(), builder);
        builder.append(':');
        encode(entry.getValue(), builder);
    }

    /**
     * Escapes the content of a regular String to be used as the contents of a JSON String.
     * ie: If you put double-quotes around the result of this function, you will have a valid JSON String.
     *
     * @param string the String to be escaped
     * @return An escaped piece of a JSON String
     */
    private static String escapeString(final String string){
        // Backslashes have to be escaped first because it is the escape character
        return string.replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\"", "\\\\\"")
                .replaceAll("/", "\\\\/")
                .replaceAll("\b", "\\\\b")
                .replaceAll("\f", "\\\\f")
                .replaceAll("\n", "\\\\n")
                .replaceAll("\r", "\\\\r")
                .replaceAll("\t", "\\\\t");
    }
}
