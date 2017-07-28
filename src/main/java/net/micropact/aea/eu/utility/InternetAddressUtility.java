package net.micropact.aea.eu.utility;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.entellitrak.legacy.util.StringUtility;
import com.entellitrak.user.User;

import net.entellitrak.aea.eu.IEmailAddresses;
import net.micropact.aea.core.utility.StringUtils;

/**
 * This class contains useful functions for manipulating and converting things to javax.mail.internet.InternetAddress.
 * @author zmiller
 */
public final class InternetAddressUtility {

    /** All methods are static so there is no reason to instantiate an InternetAddressUtility. */
    private InternetAddressUtility(){}

    /**
     * This method does its best to convert its parameter to a {@link Collection} of {@link InternetAddress}es.
     * Due to the expression problem as well as its recursive support for {@link Collection} I do not think that
     * it can completely be split out into overloaded methods although it could be split into
     * perfectly typed methods although the non-collection versions could be and an interface could be created for
     * things which can be converted to {@link InternetAddress} this would require the user to make explicit calls
     * on every addresses to generate an instance of the interface.
     *
     * @param addresses Objects to be converted to InternetAddresses.
     *     Must be a valid
     *     <a href="../../../../entellitrak/aea/eu/doc-files/email-address.html">Email Utility Email Address</a>
     * @return A Collection of InternetAddresses
     * @throws AddressException If there was an underlying problem converting an address to an InternetAddress
     * @throws UnsupportedEncodingException If there was an encoding issue converting an address to an InternetAdress
     */
    public static Collection<InternetAddress> toInternetAddresses(final Object addresses)
            throws AddressException, UnsupportedEncodingException{
        if(addresses == null){
            return new LinkedList<>();
        }else if(addresses instanceof IEmailAddresses){
            return toInternetAddresses(((IEmailAddresses) addresses).getInternetAddresses());
        }else if(addresses instanceof InternetAddress){
            return Arrays.asList((InternetAddress) addresses);
        }else if(addresses instanceof com.entellitrak.mail.InternetAddress){
            return Arrays.asList(etkInternetAddressToInternetAddress((com.entellitrak.mail.InternetAddress) addresses));
        }else if(addresses instanceof String){
            return stringToInternetAddresses((String) addresses);
        }else if(addresses instanceof User){
            final Collection<InternetAddress> iaAddresses = new LinkedList<>();
            iaAddresses.add(userToInternetAddress((User) addresses));
            return iaAddresses;
        }else if(addresses instanceof Collection){
            final Collection<InternetAddress> iaAddresses= new LinkedList<>();
            final Collection<?> addressesCollection = (Collection<?>) addresses;
            for(final Object address : addressesCollection){
                iaAddresses.addAll(toInternetAddresses(address));
            }
            return iaAddresses;
        }else{
            throw new AddressException(String.format("Address type not supported: %s", addresses));
        }
    }

    /**
     * Convert an entellitrak public API internet address to a java internet address.
     *
     * @param etkAddress the internet address
     * @return the internet address
     * @throws UnsupportedEncodingException If there is an underlying {@link UnsupportedEncodingException}
     */
    private static InternetAddress etkInternetAddressToInternetAddress(
            final com.entellitrak.mail.InternetAddress etkAddress)
                    throws UnsupportedEncodingException{
        return new InternetAddress(etkAddress.getAddress(), etkAddress.getPersonal());
    }

    /**
     * This method retrieves the email address of a particular user.
     *
     * @param user User whose email address you want to convert.
     * @return An InternetAddress corresponding to user.
     * @throws UnsupportedEncodingException If there was an underlying {@link UnsupportedEncodingException}.
     */
    private static InternetAddress userToInternetAddress(final User user) throws UnsupportedEncodingException{
        return new InternetAddress(user.getProfile().getEmailAddress(), user.getAccountName());
    }

    /**
     * Converts a String of email addresses which can be separated by commas and/or semicolons into a collection
     * of mail addresses.
     *
     * @param addresses A String of email addresses separated by commas and/or semicolons.
     * @return A collection of InternetAddresses
     * @throws AddressException If there was an underlying {@link AddressException}
     */
    private static Collection<InternetAddress> stringToInternetAddresses(final String addresses)
            throws AddressException{
        final Collection<InternetAddress> lstRecipients = new LinkedList<>();
        final String semiColonSeparatedddresses = addresses.replaceAll(",", ";");
        final StringTokenizer strTok = new StringTokenizer(semiColonSeparatedddresses, ";");

        while (strTok.hasMoreElements()) {
            final String strEmailAddress = strTok.nextToken().trim();
            if (strEmailAddress.length() > 0) {
                lstRecipients.add(getInternetAddress(strEmailAddress));
            }
        }
        return lstRecipients;
    }

    /**
     * Converts a String representing a single email address to an internet address.
     *
     * @param address A String representing a single InternetAddress
     * @return a single InternetAddress
     * @throws AddressException If there was an underlying {@link AddressException}
     */
    private static InternetAddress getInternetAddress(final String address) throws AddressException{
        return address == null ? null : new InternetAddress(address);
    }

    /**
     * Converts a collection of internet addresses to a String representing those addresses. The String separates
     * addresses by a semicolon.
     *
     * @param internetAddresses any {@link InternetAddress}s
     * @return A String representing the combination of all addresses. Each address is separated by a ;
     */
    public static String toText(final Collection<InternetAddress> internetAddresses){
        return StringUtils.join(internetAddresses
                .stream()
                .map(InternetAddressUtility::toText)
                .filter(StringUtility::isNotBlank)
                .collect(Collectors.toList()),
                ";");
    }

    /**
     * Converts a single internet address to its text representation.
     *
     * @param internetAddress any InternetAddress
     * @return The String represation of internetAddress
     */
    private static String toText(final InternetAddress internetAddress){
        return internetAddress == null ? null : internetAddress.getAddress();
    }

    /**
     * This method will return a collection of Strings where each String represens an Email Address.
     *
     * @param addresses Any email address type supported by toInternetAddresses.
     * @return A collection of email addresses.
     * @throws AddressException If there was an underlying {@link AddressException}.
     * @throws UnsupportedEncodingException If there was an underlying {@link UnsupportedEncodingException}.
     */
    public static Collection<String> toTextCollection(final Object addresses)
            throws AddressException, UnsupportedEncodingException{
        return toInternetAddresses(addresses)
            .stream()
            .map(InternetAddressUtility::toText)
            .filter(StringUtility::isNotBlank)
            .collect(Collectors.toList());
    }
}
