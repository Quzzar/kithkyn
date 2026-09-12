package com.quzzar.kithkyn.configuration;


import com.quzzar.kithkyn.Kithkyn;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * The mod's configuration, split across two files so a player is not faced with
 * fifty tuning knobs to change five things:
 *
 * <ul>
 *   <li>{@code kithkyn-common.toml} ({@link CommonConfig}): the simple file,
 *       the handful most players touch, villages, time, and the language model.
 *   <li>{@code kithkyn-advanced.toml} ({@link AdvancedConfig}): everything
 *       else, the attractiveness/standing/population/labor/economy tuning, the
 *       LLM sampling knobs, and the developer tools.
 * </ul>
 *
 * Both are registered as {@code COMMON}. The static field mirror below is flat
 * and unchanged whichever file a value comes from, so the rest of the codebase
 * reads {@code KithkynConfig.X} without caring which file holds {@code X}.
 */
@EventBusSubscriber(modid = Kithkyn.MODID, bus = EventBusSubscriber.Bus.MOD)
public class KithkynConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;
    public static final ModConfigSpec ADVANCED_SPEC;
    public static final AdvancedConfig ADVANCED;
    static {
        final Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();

        final Pair<AdvancedConfig, ModConfigSpec> advancedPair = new ModConfigSpec.Builder().configure(AdvancedConfig::new);
        ADVANCED = advancedPair.getLeft();
        ADVANCED_SPEC = advancedPair.getRight();
    }

    // --- general (simple) ---
    public static int DaysInYear;
    public static int DaysPerChildStage;
    public static boolean GenerateVillages;
    public static boolean UndeadVillages;
    public static double UndeadVillageChance;
    public static boolean ReplacePillagers;
    public static boolean WanderingMerchant;
    public static VillageLoadingMode VillageLoading;

    // --- llm (simple: which brain and how to reach it) ---
    public static boolean LlmEnabled;
    public static String LlmProviderName;
    public static String LlmApiKey;
    public static String LlmCloudModel;
    public static String LlmLocalModel;
    public static boolean LlmVillagerConversations;

    // --- llm (advanced: sampling) ---
    public static int LlmChatMaxNewTokens;
    public static double LlmChatTemperature;
    public static int LlmDecisionMaxNewTokens;
    public static double LlmDecisionTemperature;

    // --- attractiveness (advanced) ---
    public static double AttractivenessBase;
    public static double AttractivenessFoodMax;
    public static double AttractivenessFoodTargetPerCapita;
    public static double AttractivenessFreeBedsMax;
    public static double AttractivenessFreeBedsTarget;
    public static double AttractivenessHomelessMax;
    public static double AttractivenessDeathWeight;
    public static double AttractivenessHurtWeight;
    public static double AttractivenessShortageWeight;
    public static double AttractivenessTheftWeight;
    public static double AttractivenessArriveThreshold;
    public static double AttractivenessEmigrateThreshold;

    // --- standing (advanced) ---
    public static int StandingHostileBelow;
    public static int StandingShunnedBelow;
    public static int StandingUnwelcomeBelow;
    public static int StandingDislikedBelow;
    public static int StandingTrustedAbove;
    public static double StandingWorstMarkup;
    public static int AssaultOpinionHit;
    public static int GrudgeAttackBelow;
    public static int UndeadStrangerBaseline;
    public static int UndeadRaidStandingBelow;
    public static int UndeadRaidCooldownDays;

    // --- population (advanced) ---
    public static int PopulationCheckIntervalSeconds;
    public static int MinimumVillagePopulation;
    public static int ShortageEventCooldownSeconds;
    public static int ArrivalEdgeMinDistance;
    public static int TravelTimeoutSeconds;
    public static int IdleCapFallback;
    public static int WandererRecruitRadius;
    public static int WandererCap;
    public static int WandererPoolCap;
    public static int GraveyardCap;

    // --- families (advanced) ---
    public static int FamilyFirstTalkDelayDays;
    public static int FamilyTalkRetryDays;
    public static int FamilyBirthCooldownDays;
    public static double TwinBirthChance;
    public static double TripletBirthChance;

    // --- labor (advanced) ---
    public static double JobSwapThreshold;
    public static int JobSwapIntervalSeconds;
    public static double JobSwapCooldownDays;
    public static double BuildCooldownDays;
    public static boolean RedevelopmentEnabled;

    // --- economy (advanced) ---
    public static double BankSpread;

    // --- developer (advanced) ---
    public static boolean DeveloperCommands;

    public static void bakeCommonConfig() {
        // general
        DaysInYear = COMMON.DaysInYear.get();
        DaysPerChildStage = COMMON.DaysPerChildStage.get();
        GenerateVillages = COMMON.GenerateVillages.get();
        UndeadVillages = COMMON.UndeadVillages.get();
        UndeadVillageChance = COMMON.UndeadVillageChance.get();
        ReplacePillagers = COMMON.ReplacePillagers.get();
        WanderingMerchant = COMMON.WanderingMerchant.get();
        VillageLoading = COMMON.VillageLoading.get();

        // llm (simple)
        LlmEnabled = COMMON.LlmEnabled.get();
        LlmProviderName = COMMON.LlmProviderName.get();
        LlmApiKey = COMMON.LlmApiKey.get();
        LlmCloudModel = COMMON.LlmCloudModel.get();
        LlmLocalModel = COMMON.LlmLocalModel.get();
        LlmVillagerConversations = COMMON.LlmVillagerConversations.get();
    }

    public static void bakeAdvancedConfig() {
        // llm (advanced)
        LlmChatMaxNewTokens = ADVANCED.LlmChatMaxNewTokens.get();
        LlmChatTemperature = ADVANCED.LlmChatTemperature.get();
        LlmDecisionMaxNewTokens = ADVANCED.LlmDecisionMaxNewTokens.get();
        LlmDecisionTemperature = ADVANCED.LlmDecisionTemperature.get();

        // attractiveness
        AttractivenessBase = ADVANCED.AttractivenessBase.get();
        AttractivenessFoodMax = ADVANCED.AttractivenessFoodMax.get();
        AttractivenessFoodTargetPerCapita = ADVANCED.AttractivenessFoodTargetPerCapita.get();
        AttractivenessFreeBedsMax = ADVANCED.AttractivenessFreeBedsMax.get();
        AttractivenessFreeBedsTarget = ADVANCED.AttractivenessFreeBedsTarget.get();
        AttractivenessHomelessMax = ADVANCED.AttractivenessHomelessMax.get();
        AttractivenessDeathWeight = ADVANCED.AttractivenessDeathWeight.get();
        AttractivenessHurtWeight = ADVANCED.AttractivenessHurtWeight.get();
        AttractivenessShortageWeight = ADVANCED.AttractivenessShortageWeight.get();
        AttractivenessTheftWeight = ADVANCED.AttractivenessTheftWeight.get();
        AttractivenessArriveThreshold = ADVANCED.AttractivenessArriveThreshold.get();
        AttractivenessEmigrateThreshold = ADVANCED.AttractivenessEmigrateThreshold.get();

        // standing
        StandingHostileBelow = ADVANCED.StandingHostileBelow.get();
        StandingShunnedBelow = ADVANCED.StandingShunnedBelow.get();
        StandingUnwelcomeBelow = ADVANCED.StandingUnwelcomeBelow.get();
        StandingDislikedBelow = ADVANCED.StandingDislikedBelow.get();
        StandingTrustedAbove = ADVANCED.StandingTrustedAbove.get();
        StandingWorstMarkup = ADVANCED.StandingWorstMarkup.get();
        AssaultOpinionHit = ADVANCED.AssaultOpinionHit.get();
        GrudgeAttackBelow = ADVANCED.GrudgeAttackBelow.get();
        UndeadStrangerBaseline = ADVANCED.UndeadStrangerBaseline.get();
        UndeadRaidStandingBelow = ADVANCED.UndeadRaidStandingBelow.get();
        UndeadRaidCooldownDays = ADVANCED.UndeadRaidCooldownDays.get();

        // population
        PopulationCheckIntervalSeconds = ADVANCED.PopulationCheckIntervalSeconds.get();
        MinimumVillagePopulation = ADVANCED.MinimumVillagePopulation.get();
        ShortageEventCooldownSeconds = ADVANCED.ShortageEventCooldownSeconds.get();
        ArrivalEdgeMinDistance = ADVANCED.ArrivalEdgeMinDistance.get();
        TravelTimeoutSeconds = ADVANCED.TravelTimeoutSeconds.get();
        IdleCapFallback = ADVANCED.IdleCapFallback.get();
        WandererRecruitRadius = ADVANCED.WandererRecruitRadius.get();
        WandererCap = ADVANCED.WandererCap.get();
        WandererPoolCap = ADVANCED.WandererPoolCap.get();
        GraveyardCap = ADVANCED.GraveyardCap.get();

        // families
        FamilyFirstTalkDelayDays = ADVANCED.FamilyFirstTalkDelayDays.get();
        FamilyTalkRetryDays = ADVANCED.FamilyTalkRetryDays.get();
        FamilyBirthCooldownDays = ADVANCED.FamilyBirthCooldownDays.get();
        TwinBirthChance = ADVANCED.TwinBirthChance.get();
        TripletBirthChance = ADVANCED.TripletBirthChance.get();

        // labor
        JobSwapThreshold = ADVANCED.JobSwapThreshold.get();
        JobSwapIntervalSeconds = ADVANCED.JobSwapIntervalSeconds.get();
        JobSwapCooldownDays = ADVANCED.JobSwapCooldownDays.get();
        BuildCooldownDays = ADVANCED.BuildCooldownDays.get();
        RedevelopmentEnabled = ADVANCED.RedevelopmentEnabled.get();

        // economy
        BankSpread = ADVANCED.BankSpread.get();

        // developer
        DeveloperCommands = ADVANCED.DeveloperCommands.get();
    }

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent configEvent) {
        if (configEvent instanceof ModConfigEvent.Unloading) {
            return;
        }
        if (configEvent.getConfig().getSpec() == KithkynConfig.COMMON_SPEC) {
            bakeCommonConfig();
        } else if (configEvent.getConfig().getSpec() == KithkynConfig.ADVANCED_SPEC) {
            bakeAdvancedConfig();
        }
    }

    /** The simple file: villages, time, and the language model. */
    public static class CommonConfig {
        // general
        public final ModConfigSpec.IntValue DaysInYear;
        public final ModConfigSpec.IntValue DaysPerChildStage;
        public final ModConfigSpec.BooleanValue GenerateVillages;
        public final ModConfigSpec.BooleanValue UndeadVillages;
        public final ModConfigSpec.DoubleValue UndeadVillageChance;
        public final ModConfigSpec.BooleanValue ReplacePillagers;
        public final ModConfigSpec.BooleanValue WanderingMerchant;
        public final ModConfigSpec.EnumValue<VillageLoadingMode> VillageLoading;

        // llm (simple)
        public final ModConfigSpec.BooleanValue LlmEnabled;
        public final ModConfigSpec.ConfigValue<String> LlmProviderName;
        public final ModConfigSpec.ConfigValue<String> LlmApiKey;
        public final ModConfigSpec.ConfigValue<String> LlmCloudModel;
        public final ModConfigSpec.ConfigValue<String> LlmLocalModel;
        public final ModConfigSpec.BooleanValue LlmVillagerConversations;

        public CommonConfig(ModConfigSpec.Builder builder) {

            builder.comment("Villages and the passage of time: the settings most worlds want to set once.").push("general");

            DaysInYear = builder.comment("Days in one Minecraft year (there are 8 days in one full lunar cycle)").translation(Kithkyn.MODID + ".config.DaysInYear").defineInRange("Days in Year", 96, 8, 79992);
            DaysPerChildStage = builder.comment("Minecraft days spent in each of the three pre-adult stages: toddler, kid, and teenager").translation(Kithkyn.MODID + ".config.DaysPerChildStage").defineInRange("Days per child stage", 8, 1, 79992);
            GenerateVillages = builder.comment("Replaces vanilla Minecraft's villages with Kithkyn villages").translation(Kithkyn.MODID + ".config.GenerateVillages").define("Generate villages", true);
            UndeadVillages = builder.comment("Whether some Kithkyn villages are undead").translation(Kithkyn.MODID + ".config.UndeadVillages").define("Undead villages", true);
            UndeadVillageChance = builder.comment("Chance that a generated village is undead (0.0 - 1.0)").translation(Kithkyn.MODID + ".config.UndeadVillageChance").defineInRange("Undead village chance", 0.07D, 0.0D, 1.0D);
            WanderingMerchant = builder.comment("Replaces vanilla Minecraft's wandering trader with a wandering merchant").translation(Kithkyn.MODID + ".config.WanderingMerchant").define("Wandering merchant", true);
            ReplacePillagers = builder.comment("Replaces vanilla Minecraft's pillagers with the undead").translation(Kithkyn.MODID + ".config.ReplacePillagers").define("Replace pillagers", true);
            VillageLoading = builder.comment("Whether a village keeps running when no player is near:", "- 'HYBRID': the village stays awake for a few days after a player last stood in it", "- 'ALL': every village in the world stays loaded at all times", "- 'OFF': no village keeps chunks loaded, so a village freezes the moment you walk away").translation(Kithkyn.MODID + ".config.VillageLoading").defineEnum("Village loading", VillageLoadingMode.HYBRID);

            builder.pop();

            builder.comment("The language model that gives villagers their conversation, decisions, personas, and village", "names. The sampling knobs live in kithkyn-advanced.toml.").push("llm");

            LlmEnabled = builder.comment("Enable the LLM behind villager conversation, village decisions, personas, and names").translation(Kithkyn.MODID + ".config.LlmEnabled").define("Enable LLM?", true);
            LlmProviderName = builder.comment("Which LLM answers for the villagers: 'local' downloads an offline", "model and runs it locally for you; 'claude', 'openai', or 'deepseek' use that", "cloud service with your API key. Changing this requires a game restart.").translation(Kithkyn.MODID + ".config.LlmProvider").define("LLM provider", "local");
            LlmApiKey = builder.comment("Optional; only needed when 'LLM provider' is a cloud service.", "Paste it here in plain text and DON'T SHARE THIS FILE").translation(Kithkyn.MODID + ".config.LlmApiKey").define("LLM API key", "");
            LlmCloudModel = builder.comment("Model id for the cloud provider", "Look at the cloud provider's documentation for the supported options").translation(Kithkyn.MODID + ".config.LlmCloudModel").define("LLM cloud model", "gpt-5.6-luna");
            LlmLocalModel = builder.comment("Which offline model the 'local' provider downloads and runs:", "Supported options:", "- 'llama-3b' (download size: 2.0 GB, additional RAM needed: about 3 GB)", "- 'gemma-2-2b' (download size: 1.7 GB, additional RAM needed: about 2.5 to 3 GB)").translation(Kithkyn.MODID + ".config.LlmLocalModel").define("LLM local model", "llama-3b");
            LlmVillagerConversations = builder.comment("Whether villagers strike up conversations with each other").translation(Kithkyn.MODID + ".config.LlmVillagerConversations").define("Villagers talk to each other", true);

            builder.pop();
        }
    }

    /** The advanced file: tuning knobs and developer tools, with sane defaults nobody has to touch. */
    public static class AdvancedConfig {
        // llm (advanced)
        public final ModConfigSpec.IntValue LlmChatMaxNewTokens;
        public final ModConfigSpec.DoubleValue LlmChatTemperature;
        public final ModConfigSpec.IntValue LlmDecisionMaxNewTokens;
        public final ModConfigSpec.DoubleValue LlmDecisionTemperature;

        // attractiveness
        public final ModConfigSpec.DoubleValue AttractivenessBase;
        public final ModConfigSpec.DoubleValue AttractivenessFoodMax;
        public final ModConfigSpec.DoubleValue AttractivenessFoodTargetPerCapita;
        public final ModConfigSpec.DoubleValue AttractivenessFreeBedsMax;
        public final ModConfigSpec.DoubleValue AttractivenessFreeBedsTarget;
        public final ModConfigSpec.DoubleValue AttractivenessHomelessMax;
        public final ModConfigSpec.DoubleValue AttractivenessDeathWeight;
        public final ModConfigSpec.DoubleValue AttractivenessHurtWeight;
        public final ModConfigSpec.DoubleValue AttractivenessShortageWeight;
        public final ModConfigSpec.DoubleValue AttractivenessTheftWeight;
        public final ModConfigSpec.DoubleValue AttractivenessArriveThreshold;
        public final ModConfigSpec.DoubleValue AttractivenessEmigrateThreshold;

        // standing
        public final ModConfigSpec.IntValue StandingHostileBelow;
        public final ModConfigSpec.IntValue StandingShunnedBelow;
        public final ModConfigSpec.IntValue StandingUnwelcomeBelow;
        public final ModConfigSpec.IntValue StandingDislikedBelow;
        public final ModConfigSpec.IntValue StandingTrustedAbove;
        public final ModConfigSpec.DoubleValue StandingWorstMarkup;
        public final ModConfigSpec.IntValue AssaultOpinionHit;
        public final ModConfigSpec.IntValue GrudgeAttackBelow;
        public final ModConfigSpec.IntValue UndeadStrangerBaseline;
        public final ModConfigSpec.IntValue UndeadRaidStandingBelow;
        public final ModConfigSpec.IntValue UndeadRaidCooldownDays;

        // population
        public final ModConfigSpec.IntValue PopulationCheckIntervalSeconds;
        public final ModConfigSpec.IntValue MinimumVillagePopulation;
        public final ModConfigSpec.IntValue ShortageEventCooldownSeconds;
        public final ModConfigSpec.IntValue ArrivalEdgeMinDistance;
        public final ModConfigSpec.IntValue TravelTimeoutSeconds;
        public final ModConfigSpec.IntValue IdleCapFallback;
        public final ModConfigSpec.IntValue WandererRecruitRadius;
        public final ModConfigSpec.IntValue WandererCap;
        public final ModConfigSpec.IntValue WandererPoolCap;
        public final ModConfigSpec.IntValue GraveyardCap;

        // families
        public final ModConfigSpec.IntValue FamilyFirstTalkDelayDays;
        public final ModConfigSpec.IntValue FamilyTalkRetryDays;
        public final ModConfigSpec.IntValue FamilyBirthCooldownDays;
        public final ModConfigSpec.DoubleValue TwinBirthChance;
        public final ModConfigSpec.DoubleValue TripletBirthChance;

        // labor
        public final ModConfigSpec.DoubleValue JobSwapThreshold;
        public final ModConfigSpec.IntValue JobSwapIntervalSeconds;
        public final ModConfigSpec.DoubleValue JobSwapCooldownDays;
        public final ModConfigSpec.DoubleValue BuildCooldownDays;
        public final ModConfigSpec.BooleanValue RedevelopmentEnabled;

        // economy
        public final ModConfigSpec.DoubleValue BankSpread;

        // developer
        public final ModConfigSpec.BooleanValue DeveloperCommands;

        public AdvancedConfig(ModConfigSpec.Builder builder) {

            builder.comment("Advanced LLM sampling. The everyday LLM settings live in kithkyn-common.toml.").push("llm");

            LlmChatMaxNewTokens = builder.comment("Maximum tokens a villager may generate per line of conversation").translation(Kithkyn.MODID + ".config.LlmChatMaxNewTokens").defineInRange("LLM chat max new tokens", 64, 16, 1024);
            LlmChatTemperature = builder.comment("Sampling temperature for conversation (0.0 - 2.0)").translation(Kithkyn.MODID + ".config.LlmChatTemperature").defineInRange("LLM chat temperature", 0.4D, 0.0D, 2.0D);
            LlmDecisionMaxNewTokens = builder.comment("Maximum tokens the LLM may generate per village decision").translation(Kithkyn.MODID + ".config.LlmDecisionMaxNewTokens").defineInRange("LLM decision max new tokens", 128, 16, 1024);
            LlmDecisionTemperature = builder.comment("Sampling temperature for village decisions (0.0 is deterministic)").translation(Kithkyn.MODID + ".config.LlmDecisionTemperature").defineInRange("LLM decision temperature", 0.1D, 0.0D, 2.0D);

            builder.pop();

            builder.comment("Attractiveness: the 0-100 score that decides whether people move into a village or leave it.").push("attractiveness");

            AttractivenessBase = builder.comment("Starting value before any inputs apply").translation(Kithkyn.MODID + ".config.AttractivenessBase").defineInRange("Attractiveness base", 50.0D, 0.0D, 100.0D);
            AttractivenessFoodMax = builder.comment("Maximum bonus from stocked food").translation(Kithkyn.MODID + ".config.AttractivenessFoodMax").defineInRange("Attractiveness food max", 25.0D, 0.0D, 100.0D);
            AttractivenessFoodTargetPerCapita = builder.comment("Edible items per villager at which the food bonus is at its maximum").translation(Kithkyn.MODID + ".config.AttractivenessFoodTargetPerCapita").defineInRange("Attractiveness food target per capita", 8.0D, 0.1D, 1000.0D);
            AttractivenessFreeBedsMax = builder.comment("Maximum bonus from free beds").translation(Kithkyn.MODID + ".config.AttractivenessFreeBedsMax").defineInRange("Attractiveness free beds max", 10.0D, 0.0D, 100.0D);
            AttractivenessFreeBedsTarget = builder.comment("Free beds at which the bonus is at its maximum").translation(Kithkyn.MODID + ".config.AttractivenessFreeBedsTarget").defineInRange("Attractiveness free beds target", 2.0D, 1.0D, 100.0D);
            AttractivenessHomelessMax = builder.comment("Maximum penalty when every villager is homeless").translation(Kithkyn.MODID + ".config.AttractivenessHomelessMax").defineInRange("Attractiveness homeless max", 20.0D, 0.0D, 100.0D);
            AttractivenessDeathWeight = builder.comment("Penalty per unit of decaying death impact").translation(Kithkyn.MODID + ".config.AttractivenessDeathWeight").defineInRange("Attractiveness death weight", 8.0D, 0.0D, 100.0D);
            AttractivenessHurtWeight = builder.comment("Penalty per unit of decaying hurt-by-player impact").translation(Kithkyn.MODID + ".config.AttractivenessHurtWeight").defineInRange("Attractiveness hurt weight", 3.0D, 0.0D, 100.0D);
            AttractivenessShortageWeight = builder.comment("Penalty per unit of decaying resource-shortage impact").translation(Kithkyn.MODID + ".config.AttractivenessShortageWeight").defineInRange("Attractiveness shortage weight", 2.0D, 0.0D, 100.0D);
            AttractivenessTheftWeight = builder.comment("Penalty per unit of decaying theft impact").translation(Kithkyn.MODID + ".config.AttractivenessTheftWeight").defineInRange("Attractiveness theft weight", 1.0D, 0.0D, 100.0D);
            AttractivenessArriveThreshold = builder.comment("Score above which new villagers arrive").translation(Kithkyn.MODID + ".config.AttractivenessArriveThreshold").defineInRange("Attractiveness grow threshold", 50.0D, 0.0D, 100.0D);
            AttractivenessEmigrateThreshold = builder.comment("Score below which villagers leave").translation(Kithkyn.MODID + ".config.AttractivenessEmigrateThreshold").defineInRange("Attractiveness decline threshold", 25.0D, 0.0D, 100.0D);

            builder.pop();

            builder.comment("Standing: how a village treats a player, judged by the average of its residents' opinions (-100 to 100).").push("standing");

            StandingHostileBelow = builder.comment("Standing at or below which a village's guards attack you on sight").translation(Kithkyn.MODID + ".config.StandingHostileBelow").defineInRange("Standing hostile below", -70, -100, 100);
            StandingShunnedBelow = builder.comment("Standing at or below which nobody in the village will talk to you").translation(Kithkyn.MODID + ".config.StandingShunnedBelow").defineInRange("Standing shunned below", -50, -100, 100);
            StandingUnwelcomeBelow = builder.comment("Standing at or below which the village's market is closed to you").translation(Kithkyn.MODID + ".config.StandingUnwelcomeBelow").defineInRange("Standing unwelcome below", -30, -100, 100);
            StandingDislikedBelow = builder.comment("Standing at or below which the village charges you over the odds").translation(Kithkyn.MODID + ".config.StandingDislikedBelow").defineInRange("Standing disliked below", -10, -100, 100);
            StandingTrustedAbove = builder.comment("Standing at or above which a village counts you a friend (nothing hangs on it yet)").translation(Kithkyn.MODID + ".config.StandingTrustedAbove").defineInRange("Standing trusted above", 40, -100, 100);
            StandingWorstMarkup = builder.comment("Price multiplier at the bottom of the disliked band").translation(Kithkyn.MODID + ".config.StandingWorstMarkup").defineInRange("Standing worst markup", 2.0D, 1.0D, 10.0D);
            AssaultOpinionHit = builder.comment("How much being struck by a player lowers the victim's opinion of them, before the damage is added").translation(Kithkyn.MODID + ".config.AssaultOpinionHit").defineInRange("Assault opinion hit", 5, 0, 15);
            GrudgeAttackBelow = builder.comment("Opinion of a player at or below which a villager treats them as an enemy (personal, not the village average)").translation(Kithkyn.MODID + ".config.GrudgeAttackBelow").defineInRange("Grudge attack below", -30, -100, 0);
            UndeadStrangerBaseline = builder.comment("Opinion an undead villager starts every stranger at, and drifts back to (the living start at 0)").translation(Kithkyn.MODID + ".config.UndeadStrangerBaseline").defineInRange("Undead stranger baseline", -40, -100, 0);

            builder.pop();

            builder.comment("Undead raids: the dead of an undead village following a player into the next living village they enter. Only while 'Replace pillagers' is on.").push("raids");

            UndeadRaidStandingBelow = builder.comment("Standing with an undead village at or below which its dead follow you").translation(Kithkyn.MODID + ".config.UndeadRaidStandingBelow").defineInRange("Undead raid standing below", -60, -100, 0);
            UndeadRaidCooldownDays = builder.comment("Minecraft days an undead village waits before its dead follow the same player again (0 = every time)").translation(Kithkyn.MODID + ".config.UndeadRaidCooldownDays").defineInRange("Undead raid cooldown days", 3, 0, 365);

            builder.pop();

            builder.comment("Population: the campfire arrival and emigration loop, and its timings.").push("population");

            PopulationCheckIntervalSeconds = builder.comment("Seconds between a village's arrival/emigration checks").translation(Kithkyn.MODID + ".config.PopulationCheckIntervalSeconds").defineInRange("Population check interval seconds", 100, 5, 86400);
            MinimumVillagePopulation = builder.comment("People a village never drops below: emigration stops there, and deaths below it are refilled").translation(Kithkyn.MODID + ".config.MinimumVillagePopulation").defineInRange("Minimum village population", 4, 0, 64);
            ShortageEventCooldownSeconds = builder.comment("Minimum seconds between resource-shortage events logged by a village").translation(Kithkyn.MODID + ".config.ShortageEventCooldownSeconds").defineInRange("Shortage event cooldown seconds", 600, 10, 86400);
            ArrivalEdgeMinDistance = builder.comment("Minimum distance from the village center at which newcomers appear").translation(Kithkyn.MODID + ".config.ArrivalEdgeMinDistance").defineInRange("Arrival edge min distance", 32, 8, 256);
            TravelTimeoutSeconds = builder.comment("Seconds a walker may spend travelling before being snapped to their destination").translation(Kithkyn.MODID + ".config.TravelTimeoutSeconds").defineInRange("Travel timeout seconds", 90, 10, 3600);
            IdleCapFallback = builder.comment("Campfire idle cap used when no village tier ladder is loaded").translation(Kithkyn.MODID + ".config.IdleCapFallback").defineInRange("Idle cap fallback", 2, 1, 64);
            WandererRecruitRadius = builder.comment("How far (blocks) a growing village looks for an existing wanderer before spawning a new arrival").translation(Kithkyn.MODID + ".config.WandererRecruitRadius").defineInRange("Wanderer recruit radius", 128, 16, 512);
            WandererCap = builder.comment("Loaded wanderers the world keeps on foot; past it, leavers pass beyond the horizon at once").translation(Kithkyn.MODID + ".config.WandererCap").defineInRange("Wanderer cap", 8, 0, 256);
            WandererPoolCap = builder.comment("Most people remembered beyond the horizon (0 forgets everyone)").translation(Kithkyn.MODID + ".config.WandererPoolCap").defineInRange("Wanderer pool cap", 64, 0, 1024);
            GraveyardCap = builder.comment("Most of the dead remembered for undead villages to raise (0 buries nobody)").translation(Kithkyn.MODID + ".config.GraveyardCap").defineInRange("Register of the dead cap", 256, 0, 4096);

            builder.pop();

            builder.comment("Families: when married couples revisit having children and how often multiple births occur.").push("families");

            FamilyFirstTalkDelayDays = builder.comment("Minecraft days a newly housed couple waits before their first family-planning conversation").translation(Kithkyn.MODID + ".config.FamilyFirstTalkDelayDays").defineInRange("First family talk delay days", 1, 0, 365);
            FamilyTalkRetryDays = builder.comment("Minecraft days before a couple who said not now discusses children again").translation(Kithkyn.MODID + ".config.FamilyTalkRetryDays").defineInRange("Family talk retry days", 4, 1, 365);
            FamilyBirthCooldownDays = builder.comment("Minecraft days after a birth before the parents discuss another child").translation(Kithkyn.MODID + ".config.FamilyBirthCooldownDays").defineInRange("Family birth cooldown days", 8, 1, 365);
            TwinBirthChance = builder.comment("Chance that a birth produces identical twins (triplets are rolled first)").translation(Kithkyn.MODID + ".config.TwinBirthChance").defineInRange("Twin birth chance", 0.03D, 0.0D, 0.5D);
            TripletBirthChance = builder.comment("Chance that a birth produces identical triplets").translation(Kithkyn.MODID + ".config.TripletBirthChance").defineInRange("Triplet birth chance", 0.0025D, 0.0D, 0.1D);

            builder.pop();

            builder.comment("Labor: how jobs are filled from the idle pool and reshuffled.").push("labor");

            JobSwapThreshold = builder.comment("Minimum aptitude improvement (3-18 stat scale) before a job is reassigned to someone better suited").translation(Kithkyn.MODID + ".config.JobSwapThreshold").defineInRange("Job swap threshold", 3.0, 0.5, 15.0);
            JobSwapIntervalSeconds = builder.comment("Seconds between job-swap passes").translation(Kithkyn.MODID + ".config.JobSwapIntervalSeconds").defineInRange("Job swap interval seconds", 60, 10, 3600);
            JobSwapCooldownDays = builder.comment("Game days a person is protected from further job swaps after a placement").translation(Kithkyn.MODID + ".config.JobSwapCooldownDays").defineInRange("Job swap cooldown days", 3.0, 0.0, 30.0);
            BuildCooldownDays = builder.comment("Game days a village waits after finishing a building before starting the next").translation(Kithkyn.MODID + ".config.BuildCooldownDays").defineInRange("Build cooldown days", 2.0, 0.0, 30.0);
            RedevelopmentEnabled = builder.comment("Allow construction proposals that dismantle blocking village buildings (paid materials return at 50%)").define("Allow village redevelopment", true);

            builder.pop();

            builder.comment("Economy: the always-available exchange that floors and caps every price.").push("economy");

            BankSpread = builder.comment("The exchange pays value/spread for goods and charges value*spread").translation(Kithkyn.MODID + ".config.BankSpread").defineInRange("Bank spread", 4.0, 1.0, 32.0);

            builder.pop();

            builder.comment("Developer tools. Off by default; for anyone working on the mod, not for players.").push("developer");

            DeveloperCommands = builder.comment("Registers the /kkdev command tree").translation(Kithkyn.MODID + ".config.DeveloperCommands").define("Developer commands", false);

            builder.pop();
        }
    }
}
