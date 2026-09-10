package com.quzzar.kithkyn.village;

/** Authored duty of one GUARD station; jobs, housing and night choices stay shared. */
public enum GuardRole {
  CAPTAIN,
  PATROL,
  CROSSBOW_POST,
  SWORD_POST;

  /** Only sentries have a fixed station to return to between threats. */
  public boolean hasPost() {
    return this == CROSSBOW_POST || this == SWORD_POST;
  }
}
