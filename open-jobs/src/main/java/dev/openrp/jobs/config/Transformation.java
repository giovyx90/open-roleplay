package dev.openrp.jobs.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A transformative recipe for workshop jobs (carpenter, smith, baker): consumes inputs, yields outputs
 * and pays a fixed amount per completed transformation. {@code craftTimeSeconds} is the minimum gap
 * between two transformations of this kind, so a worker cannot spam-craft for free pay.
 */
public final class Transformation {

    private final List<ItemAmount> inputs;
    private final List<ItemAmount> outputs;
    private final double payout;
    private final int craftTimeSeconds;

    public Transformation(List<ItemAmount> inputs, List<ItemAmount> outputs, double payout, int craftTimeSeconds) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.payout = Math.max(0.0, payout);
        this.craftTimeSeconds = Math.max(0, craftTimeSeconds);
    }

    /** Parses one transformation entry from a {@code transformations:} list item (a raw YAML map). */
    public static Transformation from(Map<?, ?> raw) {
        if (raw == null) {
            return null;
        }
        List<ItemAmount> inputs = items(raw.get("input"));
        List<ItemAmount> outputs = items(raw.get("output"));
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return null;
        }
        return new Transformation(inputs, outputs, asDouble(raw.get("payout")), asInt(raw.get("craft_time_seconds")));
    }

    private static List<ItemAmount> items(Object raw) {
        List<ItemAmount> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return result;
        }
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> entry)) {
                continue;
            }
            Object material = entry.get("material");
            if (material == null) {
                continue;
            }
            result.add(new ItemAmount(String.valueOf(material).trim(), Math.max(1, asInt(entry.get("amount"), 1))));
        }
        return result;
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static int asInt(Object value) {
        return asInt(value, 0);
    }

    private static int asInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public List<ItemAmount> inputs() {
        return inputs;
    }

    public List<ItemAmount> outputs() {
        return outputs;
    }

    public double payout() {
        return payout;
    }

    public int craftTimeSeconds() {
        return craftTimeSeconds;
    }
}
