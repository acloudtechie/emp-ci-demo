package net.micropact.aea.core.deserializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import net.micropact.aea.core.ioUtility.IOUtility;

/**
 * This class will deserialize a newline-separated list of Strings (excluding blank lines).
 *
 * Note: If we need something other than a list of Strings, we might be able to have a higher-order newline
 * deserialized which accepts a 2nd deserializer which describes how to deserialize each line.
 *
 * @author Zachary.Miller
 */
public class NewlineTrimmedNoBlanksDeserializer implements IDeserializer<List<String>> {

    @Override
    public List<String> deserialize(final String value) {
        final List<String> returnList = new LinkedList<>();

        if(value != null){
            final BufferedReader reader = new BufferedReader(new StringReader(value));

            try {
                String line;
                while((line = reader.readLine()) != null){
                    final String trimmedValue = line.trim();
                    if(trimmedValue.length() > 0){
                        returnList.add(trimmedValue);
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }finally{
                IOUtility.closeQuietly(reader);
            }
        }
        return returnList;
    }
}
