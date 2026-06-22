package com.pmfml.cognitive_vault.services;

/**
 * Utility class for pgvector-related operations.
 * Centralizes the conversion of float arrays to pgvector-compatible strings.
 */
public final class VectorUtils {

    private VectorUtils() {
        // Utility class, no instantiation
    }

    /**
     * Converts a float array into a pgvector-compatible string representation.
     * Example output: "[0.1,0.2,0.3]"
     *
     * @param vector the float array to convert
     * @return the pgvector string format
     */
    public static String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
