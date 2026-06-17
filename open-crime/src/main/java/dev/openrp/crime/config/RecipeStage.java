package dev.openrp.crime.config;

import java.util.List;

/**
 * One stage of a multi-stage production recipe. A stage consumes {@code inputs}, runs for
 * {@code durationMinutes} (scaled by the configured time scale) at a {@code locationType}, and
 * yields either plain {@code outputs} (a semi-finished item feeding the next stage) or - on the final
 * stage - an illegal good named by {@code outputGood} in the given {@code outputGoodAmount}.
 *
 * @param id              config key (e.g. {@code coltivazione})
 * @param locationType    the location type this stage must run in
 * @param durationMinutes real minutes before the stage completes (pre time-scale)
 * @param inputs          items consumed at start
 * @param outputs         plain items produced (semi-finished); empty when the stage yields a good
 * @param outputGood      illegal good id produced, or empty for a plain stage
 * @param outputGoodAmount units of the good produced
 * @param workersRequired workers that must be present during the critical phase
 */
public record RecipeStage(String id, String locationType, int durationMinutes,
                          List<RecipeIngredient> inputs, List<RecipeIngredient> outputs,
                          String outputGood, int outputGoodAmount, int workersRequired) {

    public RecipeStage {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
        outputGood = outputGood == null ? "" : outputGood;
        durationMinutes = Math.max(0, durationMinutes);
        outputGoodAmount = Math.max(0, outputGoodAmount);
        workersRequired = Math.max(0, workersRequired);
    }

    public boolean yieldsGood() {
        return !outputGood.isBlank() && outputGoodAmount > 0;
    }
}
