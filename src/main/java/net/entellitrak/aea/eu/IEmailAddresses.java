package net.entellitrak.aea.eu;

import java.util.Collection;

import javax.mail.internet.InternetAddress;

/**
 * This interface represents objects which can be converted to {@link InternetAddress}es.
 *
 * @author zmiller
 */
public interface IEmailAddresses {

    /**
     * Returns a collection of javax mail internet addresses.
     *
     * @return The {@link InternetAddress}es represented by this object.
     */
    Collection<InternetAddress> getInternetAddresses();
}
