package com.quzzar.kithkyn.appearance;

import com.quzzar.kithkyn.entities.Gender;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.entities.genetics.AppearanceGenes;
import com.quzzar.kithkyn.entities.genetics.GeneticCondition;
import com.quzzar.kithkyn.village.Occupation;

/**
 * Stable entity facts from which the client derives one complete skin recipe.
 * The kind picks the body of parts (living or undead) the genes then select
 * within; clothing is shared, so an undead smith still wears the apron.
 */
public record AppearanceInputs(
    int seed,
    AppearanceGenes genes,
    Gender gender,
    Occupation occupation,
    LifeStage lifeStage,
    GeneticCondition condition,
    Kind kind) {
}
