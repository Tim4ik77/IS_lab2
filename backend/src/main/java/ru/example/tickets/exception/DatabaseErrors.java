package ru.example.tickets.exception;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/** Unwraps JDBC failures consistently across request and import error handling. */
public final class DatabaseErrors {
    private DatabaseErrors() {}

    public static List<SQLException> causes(Throwable failure) {
        var result = new ArrayList<SQLException>();
        var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof SQLException sql) result.add(sql);
        }
        return result;
    }

    public static boolean isTransactionConflict(SQLException error) {
        return error.getSQLState() != null && error.getSQLState().startsWith("40");
    }

    public static boolean hasTransactionConflict(Throwable failure) {
        return causes(failure).stream().anyMatch(DatabaseErrors::isTransactionConflict);
    }
}
