package net.entellitrak.aea.core.cache;

import com.entellitrak.ApplicationException;

/**
 * This interface represents something which can be stored in and retrieved from the cache.
 *
 * @author zmiller
 *
 * @param <T> The Type of the Value which is to be stored in the cache.
 * @see AClassKeyCacheable
 * @see CacheManager
 */
public interface ICacheable<T> {

    /**
     * Gets the key which should be used to access the value in the cache. It is important that the key is
     * both unique and consistent.
     *
     * @return The key of the value in the cache.
     */
    String getKey();

    /**
     * This method calculates the value which is to be stored in the cache. If the value is not found in the cache,
     * this method will be called in order to get the value to store in the cache.
     *
     * @return The value which is to be stored in the cache
     * @throws ApplicationException If there was an underlying {@link ApplicationException}
     */
    T getValue() throws ApplicationException;
}
