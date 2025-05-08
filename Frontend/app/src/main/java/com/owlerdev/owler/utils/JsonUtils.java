// app/utils/JsonUtils.java
package com.owlerdev.owler.utils;

import android.content.Context;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import timber.log.Timber;

/**
 * Utility methods for reading and writing JSON data to internal storage.
 * Uses Timber for logging.
 */
public class JsonUtils {
    public static final Gson gson = new Gson();

    /**
     * Reads a JSON file from internal storage and converts it to an object of the given class.
     *
     * @param context  Application context
     * @param filename Name of the file in internal storage
     * @param clazz    Class of the object to deserialize
     * @param <T>      Type parameter
     * @return Deserialized object, or null if file does not exist or on error
     */
    public static <T> T readJsonFromFile(Context context, String filename, Class<T> clazz) {
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            Timber.w("JsonUtils: File '%s' does not exist", filename);
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, clazz);
        } catch (IOException e) {
            Timber.e(e, "JsonUtils: Error reading JSON from file '%s'", filename);
            return null;
        }
    }

    /**
     * Serializes an object to JSON and writes it to a file in internal storage.
     *
     * @param context  Application context
     * @param filename Name of the file in internal storage
     * @param data     Object to serialize
     */
    public static void writeJsonToFile(Context context, String filename, Object data) {
        File file = new File(context.getFilesDir(), filename);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            Timber.d("JsonUtils: Successfully wrote JSON to file '%s'", filename);
        } catch (IOException e) {
            Timber.e(e, "JsonUtils: Error writing JSON to file '%s'", filename);
        }
    }
}
