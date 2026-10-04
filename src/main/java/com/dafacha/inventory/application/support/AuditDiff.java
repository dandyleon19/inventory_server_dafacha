package com.dafacha.inventory.application.support;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds the human-readable "details" of an audit entry. A snapshot is an ordered map of field label -> value;
 * the label is what the reader sees ("Precio de venta"), so snapshots are written in Spanish.
 */
public final class AuditDiff {

    private AuditDiff() {
    }

    /** One line per field that changed: {@code Precio de venta: 4.5 → 5}. Empty when nothing changed. */
    public static String between(Map<String, Object> before, Map<String, Object> after) {
        Map<String, String> lines = new LinkedHashMap<>();
        after.forEach((label, newValue) -> {
            Object oldValue = before.get(label);
            if (!sameValue(oldValue, newValue)) {
                lines.put(label, format(oldValue) + " → " + format(newValue));
            }
        });
        return lines.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    /** One line per field that has a value: {@code Nombre: Fanta 1L}. */
    public static String describe(Map<String, Object> snapshot) {
        return snapshot.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().toString().isBlank())
                .map(entry -> entry.getKey() + ": " + format(entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    /** A new ordered snapshot; add fields with {@code put}. */
    public static Map<String, Object> snapshot() {
        return new LinkedHashMap<>();
    }

    public static String format(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Boolean flag) {
            return flag ? "Sí" : "No";
        }
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        String text = value.toString();
        return text.isBlank() ? "—" : text;
    }

    private static boolean sameValue(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        return Objects.equals(a, b);
    }
}
