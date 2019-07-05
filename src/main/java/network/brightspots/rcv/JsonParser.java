/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Purpose:
 * Wrapper around Jackson JSON package for reading and writing JSON objects to disk.
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

@SuppressWarnings("SameParameterValue")
class JsonParser {

  // function: readFromFile
  // purpose: parse input json file into an object of the specified type
  // param: jsonFilePath path to json file to be parsed into java
  // param: valueType class of the object to be created from parsed json
  // param: logsEnabled display log messages
  // returns: instance of the object parsed from json or null if there was a problem
  private static <T> T readFromFile(String jsonFilePath, Class<T> valueType, boolean logsEnabled) {
    T createdObject;
    try {
      // fileReader will read the json file from disk
      FileReader fileReader = new FileReader(jsonFilePath);
      // objectMapper will map json values into the new java object
      ObjectMapper objectMapper = new ObjectMapper();
      // object is the newly created object populated with json values
      createdObject = objectMapper.readValue(fileReader, valueType);
    } catch (JsonParseException | JsonMappingException exception) {
      if (logsEnabled) {
        Logger.log(
            Level.SEVERE,
            "Error parsing JSON file: %s\n%s\n"
                + "Check file formatting and values and make sure they are correct!\n"
                + "See config_file_documentation.txt for more details.",
            jsonFilePath,
            exception.toString());
      }
      createdObject = null;
    } catch (IOException exception) {
      if (logsEnabled) {
        Logger.log(
            Level.SEVERE,
            "Error opening file: %s\n%s\n"
                + "Check file path and permissions and make sure they are correct!",
            jsonFilePath,
            exception.toString());
      }
      createdObject = null;
    }
    return createdObject;
  }

  static <T> T readFromFile(String jsonFilePath, Class<T> valueType) {
    return readFromFile(jsonFilePath, valueType, true);
  }

  static <T> T readFromFileWithoutLogging(String jsonFilePath, Class<T> valueType) {
    return readFromFile(jsonFilePath, valueType, false);
  }

  // function: writeToFile
  // purpose: write object to file as json
  // param: jsonFile File object to write to
  // param: objectToSerialize object to be written
  static void writeToFile(File jsonFile, Object objectToSerialize) {
    try {
      new ObjectMapper()
          .writer()
          .withDefaultPrettyPrinter()
          .writeValue(jsonFile, objectToSerialize);
      Logger.log(Level.INFO, "Successfully saved file: %s", jsonFile.getAbsolutePath());
    } catch (IOException exception) {
      Logger.log(
          Level.SEVERE,
          "Error saving file: %s\n%s",
          jsonFile.getAbsolutePath(),
          exception.toString());
    }
  }
}
