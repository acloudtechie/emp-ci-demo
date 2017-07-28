package net.entellitrak.aea.core.cache;

/**
 * This abstract class is a convenience for creating {@link ICacheable}s.
 * It will implement {@link #getKey()} by returning the class of the {@link ICacheable}.
 * In most circumstances, this is a reasonable thing to do for a couple reasons:
 * <ul>
 *  <li>We should not need to worry about keys conflicting</li>
 *  <li>It will be easy to find the code which put a particular entry in the cache</li>
 * </ul>
 * This does mean however that if you have multiple anonymous classes within the same class, that you will have to
 * clear the cache whenever you add or remove one of these classes, because they could be looking for the value
 * in the other one's old name. In such a case you could also implement {@link ICacheable} directly to avoid the
 * potential problem, however you should probably just create separate classes for the {@link ICacheable} instances.
 *
 * @author zmiller
 * @see CacheManager
 * @param <T> The Type of the value to be stored in the cache.
 */
public abstract class AClassKeyCacheable<T> implements ICacheable<T> {

    @Override
    public String getKey(){
        return getClass().getName();
    }
}
