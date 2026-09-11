package com.quzzar.kithkyn.appearance;

import com.quzzar.kithkyn.entities.Gender;

/**
 * Exact semantic layers and body geometry used to bake one player texture.
 * {@code tatterSeed} is {@link Tatter#WHOLE} for the living; for the undead it
 * is the seed their rags are cut from, so a shredded bake caches on its own.
 */
public record SkinRecipe(
    BodyModel model,
    Gender expression,
    String skin,
    String clothing,
    String leftEye,
    String rightEye,
    String hair,
    PigmentColor skinPigment,
    PigmentColor hairPigment,
    PigmentColor leftEyePigment,
    PigmentColor rightEyePigment,
    boolean headwearOccludesHair,
    boolean eyesClosed,
    int tatterSeed) {

  /**
   * The same face with its eyes shut: the eye layers give way to a lash line on the
   * skin while the person sleeps. A distinct recipe, so the sleeping texture is baked
   * and cached beside the waking one and neither is rebaked on waking or dozing off.
   */
  public SkinRecipe withEyesClosed(boolean closed) {
    if (closed == eyesClosed) {
      return this;
    }
    return new SkinRecipe(model, expression, skin, clothing, leftEye, rightEye, hair,
        skinPigment, hairPigment, leftEyePigment, rightEyePigment, headwearOccludesHair, closed,
        tatterSeed);
  }
}
