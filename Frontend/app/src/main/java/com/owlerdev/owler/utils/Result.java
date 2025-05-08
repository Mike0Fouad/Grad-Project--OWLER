// app/utils/Result.java
package com.owlerdev.owler.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * A generic wrapper class to represent successful or failed operations.
 * @param <T> the type of data on success
 */
public final class Result<T> {
    private final T data;
    private final String error;
    private final boolean success;

    private Result(@Nullable T data, @Nullable String error, boolean success) {
        this.data = data;
        this.error = error;
        this.success = success;
    }

    /**
     * Creates a success result wrapping the given data.
     */
    @NonNull
    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(data, null, true);
    }

    /**
     * Creates an error result with the given message.
     */
    @NonNull
    public static <T> Result<T> error(@NonNull String errorMessage) {
        return new Result<>(null, errorMessage, false);
    }

    /**
     * Returns true if this result represents a success.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the wrapped data if successful, or null otherwise.
     */
    @Nullable
    public T getData() {
        if (!success) {
            return null;
        }
        return data
                ;
    }



    /**
     * Returns the error message if not successful, or null on success.
     */
    @Nullable
    public String getError() {
        return error;
    }

    @NonNull
    @Override
    public String toString() {
        if (success) return "Result{success, data=" + data + "}";
        else        return "Result{error='" + error + "'}";
    }
}
