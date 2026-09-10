package com.quzzar.kithkyn.village;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.quzzar.kithkyn.entities.Gender;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.entities.PersonalLogData;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.Virtue;
import com.quzzar.kithkyn.persona.PersonaData;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;

/** The castle's incumbent speaks through the existing village decision pipelines. */
public final class VillageRuler {
  private VillageRuler() {}

  /** Titles depend on the incumbent, never their spouse or the village's identity. */
  public static String title(Gender gender) {
    return switch (gender) {
      case MALE -> "King";
      case FEMALE -> "Queen";
      case NONBINARY -> "Sovereign";
    };
  }

  /** Tenure survives routine labor optimization; normal death, departure and job removal still apply. */
  public static boolean hasStableTenure(Occupation occupation) {
    return occupation == Occupation.LEADER;
  }

  /** Captures the speaker's identity alongside a request, so a successor cannot inherit its memories. */
  public static Optional<UUID> speakerId(Village village) {
    return incumbent(village).map(RealPerson::getUUID);
  }

  /** Records an accepted decision in the existing personal log, only for the same available incumbent. */
  public static void rememberDecision(Village village, Optional<UUID> speaker, String event) {
    if (speaker.isEmpty()) return;
    incumbent(village).filter(person -> person.getUUID().equals(speaker.get()))
        .ifPresent(person -> person.logMemory(event, Optional.empty()));
  }

  /** Only a loaded resident at a still-valid castle station can currently deliberate. Never loads chunks. */
  public static Optional<RealPerson> incumbent(Village village) {
    if (village == null || village.getLevel() == null) return Optional.empty();
    for (Map.Entry<UUID, JobAssignment> entry : village.getJobAssignmentsView().entrySet()) {
      JobAssignment job = entry.getValue();
      if (job.getOccupation() != Occupation.LEADER) continue;
      Building building = village.getBuilding(job.getBuildingUUID());
      BuildingInfo info = building == null ? null : building.getInfo();
      if (info == null || !"castle".equals(info.getCategory())) continue;
      List<Occupation> stations = List.copyOf(info.getWorkLocations().values());
      if (job.getStationIndex() < 0 || job.getStationIndex() >= stations.size()
          || stations.get(job.getStationIndex()) != Occupation.LEADER) continue;
      RealPerson person = village.getPerson(village.getLevel(), entry.getKey());
      if (person != null && person.isAlive() && person.getOccupation() == Occupation.LEADER
          && village.getPopulation().contains(entry.getKey())) return Optional.of(person);
    }
    return Optional.empty();
  }

  /** Snapshot on the server thread before dispatching an asynchronous model call. */
  public static String context(Village village) {
    String villageName = village == null ? "the village" : village.getName();
    Optional<RealPerson> ruler = incumbent(village);
    if (ruler.isEmpty()) return collectiveContext(villageName);
    RealPerson person = ruler.get();
    PersonaData persona = person.getData(KithkynAttachments.PERSONA.get());
    StringBuilder character = new StringBuilder(person.getPersonality().displayName());
    for (Virtue virtue : Virtue.values()) {
      character.append("; ").append(virtue.name().toLowerCase(Locale.ROOT).replace('_', ' '))
          .append('=').append(String.format(Locale.ROOT, "%.2f", person.getVirtue(virtue)));
    }
    List<String> memories = person.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst()
        .stream().limit(3).map(entry -> PersonalLogData.formatDay(entry.dayTime()) + ": " + entry.text()).toList();
    return personalContext(villageName, person.getFullName(), person.getGender(),
        persona.blurb(), persona.quirk(), character.toString(), memories);
  }

  static String collectiveContext(String villageName) {
    return "You are the collective judgement of " + villageName + ".\n";
  }

  /** Character supplies preferences, while each caller supplies legal choices and current facts. */
  static String personalContext(String villageName, String name, Gender gender, String persona,
      String quirk, String character, List<String> memories) {
    StringBuilder context = new StringBuilder("You are ").append(title(gender)).append(' ').append(name)
        .append(", ruler of ").append(villageName).append(". You personally deliberate for this settlement.\n")
        .append("Your character: ").append(character).append(" (virtues range from -0.5 to 0.5).\n");
    if (!persona.isBlank()) context.append("Your life and outlook: ").append(persona).append('\n');
    if (!quirk.isBlank()) context.append("Your quirk: ").append(quirk).append('\n');
    if (!memories.isEmpty()) context.append("Your recent memories: ").append(String.join("; ", memories)).append('\n');
    return context.append("Let your own values influence the choice, using the current village facts and only the"
        + " offered legal options. Memories describe past events; they do not override current facts or grant powers."
        + " Give your reason in your own voice.\n").toString();
  }
}
