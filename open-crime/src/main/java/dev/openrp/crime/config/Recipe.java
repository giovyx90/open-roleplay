package dev.openrp.crime.config;

import java.util.List;
import java.util.Optional;

/**
 * A production recipe for one good: an ordered list of {@link RecipeStage stages}. The id usually
 * matches the good it ultimately produces, but the binding good is whatever the final stage names.
 *
 * @param id     config key
 * @param goodId the good this recipe is for (narrative grouping)
 * @param stages ordered stages
 */
public record Recipe(String id, String goodId, List<RecipeStage> stages) {

    public Recipe {
        stages = stages == null ? List.of() : List.copyOf(stages);
        goodId = goodId == null ? "" : goodId;
    }

    public Optional<RecipeStage> firstStage() {
        return stages.isEmpty() ? Optional.empty() : Optional.of(stages.get(0));
    }

    public Optional<RecipeStage> stage(String stageId) {
        return stages.stream().filter(stage -> stage.id().equals(stageId)).findFirst();
    }

    /** The stage after the given one, or empty when it is the last stage. */
    public Optional<RecipeStage> nextStage(String stageId) {
        for (int i = 0; i < stages.size() - 1; i++) {
            if (stages.get(i).id().equals(stageId)) {
                return Optional.of(stages.get(i + 1));
            }
        }
        return Optional.empty();
    }
}
