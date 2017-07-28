package net.micropact.aea.du.utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.entellitrak.ExecutionContext;

import net.micropact.aea.core.ioUtility.IOUtility;
import net.micropact.aea.utility.Utility;

/**
 * Class containing utility functions related to dealing with the App Server logs.
 *
 * @author zmiller
 */
public final class LogUtility {

    /** Hide constructor for Utility Class. */
    private LogUtility(){}

    /**
     * Get the file extensions which are used for log files.
     *
     * @return File extensions that are used for log files.
     */
    public static String[] logExtensions(){
        return new String[]{"log", "out", "txt"};
    }

    /**
     * This method attempts to determine the location of the log directory where the
     * {@link com.entellitrak.logging.Logger} writes to.
     * I do not know how the log directory is actually determined, however I have programmed in a best-guess which
     * appears to be good enough for most sites.
     *
     * @param etk entellitrak execution context
     * @return The location of the log file
     */
    public static String getLogPath(final ExecutionContext etk){
        // This is the default path which seems to work for all ITOPs hosted sites and be the default for tomcat.
        final String defaultLogDirectory = System.getProperty("catalina.base") + "/logs";

        try{
            final String loggingConfigFile = getLoggingConfigFile();
            final String logFilePathFromConfigFile = determineLogPathFromConfigFile(loggingConfigFile);
            return Utility.nvl(logFilePathFromConfigFile, defaultLogDirectory);
        }catch(final Exception e){
            Utility.aeaLog(etk, "Error occurred attempting to determine log path", e);
            return defaultLogDirectory;
        }
    }

    /**
     * This method attempts to parse the logging config file and guess the location of the logging directory.
     * It is not close to perfect because I don't actually know the format of the config file.
     * If it doesn't find one, it returns null.
     *
     * @param loggingConfigFile location of the logging config file
     * @return The path of the logging file, or null if it could not guess one
     * @throws IOException If there was an underlying {@link IOException}
     */
    private static String determineLogPathFromConfigFile(final String loggingConfigFile) throws IOException {
        Reader fileReader = null;
        BufferedReader bufferedReader = null;

        // The lines in the file which specify the directory locations have this format
        final Pattern pattern = Pattern.compile(".*\\.directory\\s*=\\s*(\\S+)\\s*");

        try {
            fileReader = new FileReader(loggingConfigFile);
            bufferedReader = new BufferedReader(fileReader);

            String line = null;

            /* We are going to look for the FIRST line which seems to contain a directory and replace it.
             * There might be other lines which match our regular expression so the regex may need to be made more
             * strict */
            while((line = bufferedReader.readLine()) != null){
                final Matcher matcher = pattern.matcher(line);
                if(matcher.find()){
                    // We found the matching line, so we have to replace the variables
                    return replaceProperties(matcher.group(1));
                }
            }

            return null;
        }finally{
            IOUtility.closeQuietly(bufferedReader);
            IOUtility.closeQuietly(fileReader);
        }
    }

    /**
     * The logging config file has vairables of the form ${systemPropertyName}. This method replaces those variables
     * with their actual values.
     *
     * @param string A string containing property variables
     * @return The string with the property variables replaced with their values
     */
    private static String replaceProperties(final String string){
        // This is the pattern of the variables
        final Pattern pattern = Pattern.compile("\\$\\{(.*)\\}");

        // This is the final string we will be replacing
        String currentString = string;

        /* This variable indicates whether we are done replacing variables. We have to try to run the replacement at
         * least once */
        boolean done = false;
        while(!done){
            final Matcher matcher = pattern.matcher(currentString);
            if(matcher.find()){
                // This is the name of the property we are going to replace
                final String property = matcher.group(1);

                // Replace the property name with its value
                currentString = currentString.substring(0, matcher.start())
                        + System.getProperty(property)
                        + currentString.substring(matcher.end());
            }else{
                done = true;
            }
        }

        return currentString;
    }

    /**
     * This method returns the location of the logging config file. The logging config file tells apache what
     * type of logging is enabled. This is the file which will contain the logging paths.
     *
     * @return The location of the logging config file.
     */
    private static String getLoggingConfigFile(){
        return System.getProperty("java.util.logging.config.file");
    }
}
