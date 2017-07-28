/**
 * This package will contain the various {@link java.lang.Exception}s to be used throughout AE Architecture.
 * In an attempt to make code backwards-compatible, and because users of the APIs are unlikely to try
 * to correct a failed call,
 * the public API throws relatively generic exceptions but with what should be relatively good messages.
 * As a convenience to developers all exceptions in this package
 * extend from {@link com.entellitrak.ApplicationException}
 */

package net.entellitrak.aea.exception;
