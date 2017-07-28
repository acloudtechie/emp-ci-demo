package net.entellitrak.aea.core.cache;

import com.entellitrak.ApplicationException;
import com.entellitrak.ExecutionContext;
import com.entellitrak.cache.Cache;

/**
 * This class contains the code which is common to anything which needs to store something in the Cache.
 *
 * @author zmiller
 * @see AClassKeyCacheable
 * @see ICacheable
 */
public final class CacheManager {

    /**
     * Utility classes do not need public constructors.
     */
    private CacheManager(){}

    /**
     * This method will load the value represented by the cacheable from the cache if it is there, otherwise will
     * put the value into the cache and return it.
     *
     * @param <T> The type of the object being cached
     * @param etk entellitrak execution context
     * @param cacheable A specification of what should be stored in the cache
     * @return The value of the cacheable
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    public static <T> T load(final ExecutionContext etk, final ICacheable<T> cacheable) throws ApplicationException{
        final Cache<String, Object> cache = etk.getCache();

        final String key = cacheable.getKey();

        @SuppressWarnings("unchecked")
        T value = (T) cache.load(key);

        if(value == null){
            value = cacheable.getValue();
            if(value == null){
                throw new RuntimeException(String.format("Attempted to store a null value in the cache under key \"%s\". You should never attempt to store a null value in the cache.",
                        key));
            }else{
                cache.store(key, value);
            }
        }

        return value;
    }

    /**
     * This method will remove a single item from the cache.
     * <em>
     *  Note that this leaves open the possibility of race conditions if you rely on this functionality in a
     *  production environment since other transactions may put the old value back into the cache before your current
     *  transaction is committed.
     * </em>
     *
     * @param <T> The type of the object being cached
     * @param etk entellitrak execution context
     * @param cacheable A specification of the object which is to be removed from the cache
     */
    public static <T> void remove(final ExecutionContext etk, final ICacheable<T> cacheable){
        etk.getCache().remove(cacheable.getKey());
    }
}
