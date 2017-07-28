/**
 * This package contains the public API for the Email Utility.
 * The Email Utility has two primary pieces of functionality:
 * <ol>
 *  <li>The Email Utility provides a templating tool for generating emails
 *      which shares functionality with the Form Letter Generator</li>
 *  <li>The Email Utility provides the ability to queue emails
 *      so that they will not be sent out if the transition is or gets cancelled,
 *      will log email errors and automatically retry to send them
 *  </li>
 * </ol>
 * Most Application Engineers should be dealing primarily with
 * {@link net.entellitrak.aea.eu.TemplateEmail}, {@link net.entellitrak.aea.eu.EmailQueue}
 * and {@link net.entellitrak.aea.eu.IAttachment}
 * however if these classes are unable to support your needs you may look at the other classes/interfaces.
 **/

package net.entellitrak.aea.eu;
