package com.quzzar.kithkyn.agency;

import java.util.Optional;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.UndertakingData.Duty;
import com.quzzar.kithkyn.village.GuardDuty;
import com.quzzar.kithkyn.village.LaborPlanner;

/** One source for optional movement's fixed posts, essential staffing and captured assignment identity. */
public final class ResidentObligations {
  private ResidentObligations() { }
  public static Duty snapshot(RealPerson person) {
    var village = person.getVillage();
    var job = village == null ? null : village.getJobAssignment(person.getUUID());
    return new Duty(person.getOccupation().name(), person.getLifeStage().name(), village == null ? "" : village.getID(),
        job == null ? Optional.empty() : Optional.of(job.getBuildingUUID()), job == null ? -1 : job.getStationIndex(),
        LaborPlanner.mustKeepOnPost(person), GuardDuty.of(person) != null);
  }
  public static boolean mayLeavePost(RealPerson person) {
    Duty duty = snapshot(person);
    return !duty.essential() && !duty.fixedPost();
  }
}
