package net.micropact.aea.du.page.cacheFetchDataAjax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.entellitrak.ApplicationException;
import com.entellitrak.PageExecutionContext;
import com.entellitrak.cache.Cache;
import com.entellitrak.page.ContentType;
import com.entellitrak.page.PageController;
import com.entellitrak.page.Response;
import com.entellitrak.page.TextResponse;

import net.micropact.aea.utility.JsonUtilities;

/**
 * This is the controller for a page which produces a JSON representation of the entire {@link Cache}.
 *
 * @author zmiller
 */
public class CacheFetchDataAjaxController implements PageController {

    @Override
    public Response execute(final PageExecutionContext etk) throws ApplicationException {
            final Cache<String, Object> cache = etk.getCache();

//            cache.store("nullValue", null);
//
//            final List<Map<String, Object>> parameterTypes = etk.createSQL("SELECT id, c_code, c_name  FROM t_rf_parameter_type ORDER BY id")
//                .fetchList();
//
//            cache.store("parameterTypes", parameterTypes);
//
//            final List<Object> list = Arrays.asList(new Object[]{8,
//                    BigDecimal.TEN,
//                    BigDecimal.ZERO,
//                    BigDecimal.ONE,
//                    new BigDecimal("-10"),
//                    new BigDecimal("30"),
//                    Utility.arrayToMap(Object.class, Object.class,
//                    new Object[][]{{"firstKey", "firstValue"},
//                {"secondKey", 2},
//                {1, "one"},
//                {2, "two"},
//                {10, "ten"},
//                {"thirdKey", 3},
//                {"fourthKey", -10},
//                {"item1", Utility.arrayToMap(Object.class, Object.class, new Object[][]{{"key1", "value1"},
//                    {"key2", "value2"}})},
//                {"item2", Collections.EMPTY_SET},
//                {"item3", Collections.EMPTY_MAP}})});
//
//            cache.store("listValue", list);
//
//            cache.store("random", Arrays.asList("\"\\\n\t\""));
//
//            final Set<Object> set = new HashSet<Object>(list);
//            cache.store("set", set);

            final List<Object> cacheEntries = new LinkedList<>();

            for(final String key : cache.getKeys()){
                final Map<String, Object> cacheEntryMap = new TreeMap<>();
                cacheEntryMap.put("key", key);
                cacheEntryMap.put("value", buildDescription(cache.load(key)));

                cacheEntries.add(cacheEntryMap);
            }

            TextResponse response;

            response = etk.createTextResponse();
            response.setContentType(ContentType.JSON);
            response.put("out", JsonUtilities.encode(cacheEntries));
            return response;
    }

    /**
     * This function is for semi-intelligently comparing two objects.
     * The order of the comparison goes as follows:
     * - nulls first
     * - class name 2nd
     * - if objects are same class and comparable, use their default compare
     * - compare the toString of the values
     *
     * @param o1 The first object
     * @param o2 The second object
     * @return A negative integer if o1 &lt; o2, 0 if o1 = o2, a positive integer if o1 &gt; o2
     */
    static int arbitraryCompare(final Object o1, final Object o2){
        if(o1 == null){
            return o2 == null ? 0 : -1;
        }else if(o2 == null){
            return 1;
        }else if(o1.getClass().equals(o2.getClass()) && Comparable.class.isInstance(o1)){
            @SuppressWarnings("unchecked")
            final Comparable<Object> o1Cast = (Comparable<Object>) o1;

            return o1Cast.compareTo(o2);
        }else{
            final int classNameComparison = o1.getClass().getName().compareTo(o2.getClass().getName());
            return classNameComparison != 0
                    ? classNameComparison
                    : o1.toString().compareTo(o2.toString());
        }
    }

    /**
     * This method builds a description of an object with information which can subsequently be serialized to JSON.
     * Examples of the format are
     * <pre>
     * {type: null}
     * {type: "map",
     *  className: "java.util.HashMap",
     *  values: [{key: &lt;recursive call&gt;, value: &lt;recursive-call&gt;}]}
     *  {type: "collection",
     *   className: "java.util.LinkedList",
     *   value: [&lt;recursive-calls&gt;]}
     *  {type: "other",
     *   className: "java.lang.String",
     *   value: "Hello World!"}
     *  </pre>
     * @param object The object to build a description for
     * @return A map with a description of the object.
     */
    private static Map<String, Object> buildDescription(final Object object){
        final Map<String, Object> map = new TreeMap<>();

        if(object == null){
            // Handle null
            map.put("type", null);
        }else if(object instanceof Map<?, ?>){
            // Handle Maps
            map.put("type", "map");
            map.put("className", object.getClass().getName());

            final List<Map<String, Object>> mapEntries = new LinkedList<>();

            // We will sort the Map Entries for user convenience.
            final List<Entry<?, ?>> sortedEntries = new ArrayList<>(((Map<?, ?>) object).entrySet());
            Collections.sort(sortedEntries, (entry1, entry2) -> {
                final int keyCompare = arbitraryCompare(entry1.getKey(), entry2.getKey());
                return keyCompare != 0
                        ? keyCompare
                        : arbitraryCompare(entry1.getValue(), entry2.getValue());
            });

            for(final Entry<?, ?> entry : sortedEntries) {
                final Map<String, Object> mapEntry = new TreeMap<>();
                mapEntry.put("key", buildDescription(entry.getKey()));
                mapEntry.put("value", buildDescription(entry.getValue()));
                mapEntries.add(mapEntry);
            }
            map.put("values", mapEntries);

        }else if (object instanceof Collection<?>){
            // Handle Collections
            map.put("type", "collection");
            map.put("className", object.getClass().getName());
            final List<Object> theValues = new LinkedList<>();
            for(final Object obj : ((Collection<?>) object)){
                theValues.add(buildDescription(obj));
            }

            // Sort non-ordered Collections to make it easier for the user
            if(!(object instanceof List<?>)){
                Collections.sort(theValues, (o1, o2) -> arbitraryCompare(o1, o2));
            }

            map.put("value", theValues);
        }else{
            // Handle all others
            map.put("type", "other");
            map.put("className", object.getClass().getName());
            map.put("value", object);
        }

        return map;
    }
}
