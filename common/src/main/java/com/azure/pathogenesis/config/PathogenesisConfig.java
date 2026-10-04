package com.azure.pathogenesis.config;

import com.azure.pathogenesis.Pathogenesis;
import mod.azure.azurelib.common.config.Config;
import mod.azure.azurelib.common.config.Configurable;

@Config(id = Pathogenesis.MOD_ID)
public class PathogenesisConfig {

    @Configurable
    @Configurable.Synchronized
    public ContaminationConfigs contaminationConfigs = new ContaminationConfigs();

    public static class ContaminationConfigs {

        @Configurable
        @Configurable.Synchronized
        public boolean pathogenSpreadEnabled = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Multiplier on the per-candidate contamination chance. 1.0 = default pace.")
        @Configurable.DecimalRange(min = 0.0D, max = 20.0D)
        public double pathogenSpreadRate = 1.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Hard cap on outbreak radius in blocks.")
        @Configurable.Range(min = 8, max = 256)
        public int pathogenMaxRadius = 96;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Spread budget: zones processed per server tick, per dimension.")
        @Configurable.Range(min = 1, max = 64)
        public int zonesProcessedPerTick = 4;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Spread budget: candidate block checks per processed zone, before the outbreak-phase multiplier."
        )
        @Configurable.Range(min = 1, max = 256)
        public int blockChecksPerZone = 12;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Spread budget multiplier while a leaking source still feeds the outbreak.")
        @Configurable.DecimalRange(min = 0.0D, max = 10.0D)
        public double sourceFedSpreadMultiplier = 2.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Spread budget multiplier for an established, sourceless outbreak whose mature flora sustains it."
        )
        @Configurable.DecimalRange(min = 0.0D, max = 10.0D)
        public double ecologicalSpreadMultiplier = 0.75D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Spread budget multiplier for a sourceless outbreak that never established (or lost its flora)."
        )
        @Configurable.DecimalRange(min = 0.0D, max = 10.0D)
        public double collapsingSpreadMultiplier = 0.25D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Pathogen flora (growth, fungus, spore plants) an established zone needs to sustain itself without a source."
        )
        @Configurable.Range(min = 0, max = 4096)
        public int ecologicalFloraThreshold = 16;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Chance per spread check that a collapsing outbreak loses a contaminated block on its frontier."
        )
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double collapseDieBackChance = 0.1D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Allow the Pathogen to convert water source blocks into contaminated water.")
        public boolean contaminateWater = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Allow the Pathogen to convert snow layers and snow blocks.")
        public boolean contaminateSnow = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Contaminated water sources slowly contaminate adjacent banks and clean water.")
        public boolean shorelineSpreadEnabled = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Only established zones let contaminated water creep along shorelines.")
        public boolean shorelineRequiresEstablished = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance per contaminated-water-source random tick to contaminate a neighbouring bank.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double shorelineSpreadChance = 0.15D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Fraction of shoreline spreads allowed to convert clean water sources (never in ocean biomes)."
        )
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double shorelineWaterCreep = 0.35D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Blocks past the zone's current radius that shoreline creep may reach.")
        @Configurable.Range(min = 0, max = 8)
        public int shorelineReach = 6;
    }

    @Configurable
    @Configurable.Comment(
        "Write Pathogenesis debug/info logging (zone phases, thaws, eradication). Off by default to keep large modpack/server logs clean."
    )
    public boolean debugLogging = false;

    @Configurable
    @Configurable.Synchronized
    public FloraConfigs floraConfigs = new FloraConfigs();

    public static class FloraConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.DecimalRange(min = 0.0D, max = 20.0D)
        public double pathogenPlantGrowthRate = 1.0D;
    }

    @Configurable
    @Configurable.Synchronized
    public SporeConfigs sporeConfigs = new SporeConfigs();

    public static class SporeConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 20, max = 1200)
        public int sporeCloudDuration = 100;

        @Configurable
        @Configurable.Synchronized
        @Configurable.DecimalRange(min = 0.5D, max = 8.0D)
        public double sporeInfectionRadius = 2.5D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance a firing pod disturbs one other primed pod nearby, which rattles and fires.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double podCascadeChance = 0.2D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Maximum pods one cascade can chain through after the first.")
        @Configurable.Range(min = 0, max = 16)
        public int podCascadeMaxDepth = 4;
    }

    @Configurable
    @Configurable.Synchronized
    public LifecycleConfigs lifecycleConfigs = new LifecycleConfigs();

    public static class LifecycleConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 200, max = 240000)
        public int neomorphIncubationTime = 6000;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 200, max = 240000)
        public int bloodbursterGrowthTime = 6000;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Range(min = 200, max = 240000)
        public int neophyteGrowthTime = 6000;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("If false, players survive the burst at 1 health instead of dying.")
        public boolean playerBurstKills = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("If false, extreme direct exposure applies wither instead of killing.")
        public boolean extremeExposureKills = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Infection progression multiplier while the host carries HIGH exposure.")
        @Configurable.DecimalRange(min = 1.0D, max = 4.0D)
        public double highExposureProgression = 1.25D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Infection progression multiplier while the host carries EXTREME exposure.")
        @Configurable.DecimalRange(min = 1.0D, max = 4.0D)
        public double extremeExposureProgression = 1.5D;
    }

    @Configurable
    @Configurable.Synchronized
    public FaunaConfigs faunaConfigs = new FaunaConfigs();

    public static class FaunaConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Contaminated passive animals stop breeding, wander, cough, panic or turn aggressive.")
        public boolean contaminatedAnimalBehavior = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Average ticks between coughs for a moderately contaminated animal.")
        @Configurable.Range(min = 20, max = 6000)
        public int animalCoughInterval = 400;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Range at which frenzied animals pick targets.")
        @Configurable.DecimalRange(min = 2.0D, max = 32.0D)
        public double frenzyRange = 10.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Extreme exposure kills passive animals even if extremeExposureKills is off.")
        public boolean extremeExposureKillsAnimals = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance an animal killed by extreme exposure releases a spore puff as it dies.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double extremeDeathEventChance = 0.3D;
    }

    @Configurable
    @Configurable.Synchronized
    public CarcassConfigs carcassConfigs = new CarcassConfigs();

    public static class CarcassConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Exposed, infected or zone-dwelling creatures leave a temporary infectious site on death."
        )
        public boolean carcassSitesEnabled = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Multiplier on the per-death chance of leaving a carcass site.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double carcassChance = 1.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Multiplier on how long carcass sites linger.")
        @Configurable.DecimalRange(min = 0.1D, max = 10.0D)
        public double carcassDurationMultiplier = 1.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Cap on simultaneous carcass sites per dimension. Oldest are dropped first.")
        @Configurable.Range(min = 0, max = 512)
        public int maxCarcassSites = 64;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Bloodbursters and Neophytes leave blood, remains and a contamination bump where they feed."
        )
        public boolean feedingEvidence = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Bloodbursters, Neophytes and Neomorphs occasionally wander over to sniff fresh feeding, infected or virulent sites."
        )
        public boolean predatorScentEnabled = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("How far away a predator can pick up the scent of a fresh carcass site.")
        @Configurable.DecimalRange(min = 4.0D, max = 96.0D)
        public double predatorScentRange = 32.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance per sniff (every 2 seconds while idle) that a predator follows a scent.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double predatorScentChance = 0.1D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Ticks after the last fresh blood that a carcass site still draws predators.")
        @Configurable.Range(min = 20, max = 72000)
        public int carcassScentFreshTicks = 2400;
    }

    @Configurable
    @Configurable.Synchronized
    public ClimateConfigs climateConfigs = new ClimateConfigs();

    public static class ClimateConfigs {

        @Configurable
        @Configurable.Synchronized
        public boolean weatherEffectsEnabled = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Multiplier on airborne (burst/leak) exposure under rain.")
        @Configurable.DecimalRange(min = 0.0D, max = 2.0D)
        public double rainAirborneMultiplier = 0.5D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Multiplier on contaminated-water spread while it rains on the water.")
        @Configurable.DecimalRange(min = 1.0D, max = 10.0D)
        public double rainWaterSpreadMultiplier = 2.5D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Airborne hazard multiplier in clear weather (scaled up further in arid biomes).")
        @Configurable.DecimalRange(min = 1.0D, max = 4.0D)
        public double dryAirborneMultiplier = 1.25D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Spread speed multiplier for chilled zones. Contamination goes dormant, never away.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double coldSpreadMultiplier = 0.25D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Flora maturation multiplier in the cold.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double coldFloraMultiplier = 0.25D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Spore cloud size/lifetime multiplier in the cold.")
        @Configurable.DecimalRange(min = 0.1D, max = 1.0D)
        public double coldSporeMultiplier = 0.6D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Ticks a zone must stay chilled before warming up counts as a thaw.")
        @Configurable.Range(min = 0, max = 72000)
        public int thawMinChilledTicks = 1200;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Ticks of accelerated spread and sporulation after a chilled zone thaws.")
        @Configurable.Range(min = 0, max = 72000)
        public int thawSurgeTicks = 2400;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Contaminated water freezes into dormant contaminated ice in cold biomes, and melts back when warmed."
        )
        public boolean contaminatedWaterFreezes = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance per contaminated-water-source random tick to freeze when conditions allow.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double contaminatedWaterFreezeChance = 0.25D;
    }

    @Configurable
    @Configurable.Synchronized
    public SamplerConfigs samplerConfigs = new SamplerConfigs();

    public static class SamplerConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Pathogen Sampler durability. Read at startup (restart to apply).")
        @Configurable.Range(min = 1, max = 4096)
        public int samplerDurability = 64;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Ticks between samples.")
        @Configurable.Range(min = 0, max = 200)
        public int samplerCooldown = 20;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Contact exposure from sampling contaminated ground, water or flora. 0 disables.")
        @Configurable.Range(min = 0, max = 60)
        public int samplerContactExposure = 2;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Direct exposure from sampling a leaking or open source. 0 disables.")
        @Configurable.Range(min = 0, max = 200)
        public int samplerSourceExposure = 6;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance that probing a primed spore pod sets it off.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double samplerPodTriggerChance = 0.3D;
    }

    @Configurable
    @Configurable.Synchronized
    public AmpuleConfigs ampuleConfigs = new AmpuleConfigs();

    public static class AmpuleConfigs {

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Sealed ampules are fragile: falls and explosions can rupture them; fire destroys them safely."
        )
        public boolean fragileAmpules = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Fall distance (blocks) at which a dropped ampule may shatter on landing.")
        @Configurable.DecimalRange(min = 1.0D, max = 64.0D)
        public double fallRuptureHeight = 6.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double fallRuptureBaseChance = 0.05D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Extra rupture chance per block fallen past fallRuptureHeight.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double fallRupturePerBlock = 0.02D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double fallRuptureMaxChance = 0.4D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Cap on bursts when an explosion destroys dropped ampules.")
        @Configurable.Range(min = 1, max = 16)
        public int maxExplosionBursts = 6;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment(
            "Ampules carried by a creature can break when it takes a hard fall or is caught in a blast."
        )
        public boolean carriedAmpulesCanBreak = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Fall damage needed before carried ampules are at risk.")
        @Configurable.DecimalRange(min = 1.0D, max = 100.0D)
        public double carriedFallDamageThreshold = 6.0D;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Chance per carried ampule stack to break when the carrier is hurt by an explosion.")
        @Configurable.DecimalRange(min = 0.0D, max = 1.0D)
        public double carriedExplosionBreakChance = 0.2D;
    }

    @Configurable
    @Configurable.Synchronized
    public ContainmentConfigs containmentConfigs = new ContainmentConfigs();

    public static class ContainmentConfigs {

        @Configurable
        @Configurable.Synchronized
        public boolean fireSterilizesPathogen = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Ticks sterilized soil stays immune before reverting to dirt.")
        @Configurable.Range(min = 20, max = 72000)
        public int sterilizedSoilDuration = 2400;
    }

    @Configurable
    @Configurable.Synchronized
    @Configurable.Comment(
        "Attribute values are read at startup (restart to apply). Attack timings apply to newly spawned mobs."
    )
    public EntityConfigs entityConfigs = new EntityConfigs();

    public static class EntityConfigs {

        @Configurable
        @Configurable.Synchronized
        public BloodbursterConfigs bloodbursterConfigs = new BloodbursterConfigs();

        public static class BloodbursterConfigs {

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterHealth = 14.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterArmor = 0.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterArmorToughness = 0.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterKnockbackRes = 0.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterAttackDamage = 3.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterMovementSpeed = 0.32D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Range at which it notices prey.")
            @Configurable.DecimalRange(min = 0.0D)
            public double bloodbursterHostileRange = 16.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between the attack telegraph and the hit. Applies to newly spawned mobs.")
            @Configurable.Range(min = 1, max = 40)
            public int bloodbursterAttackWindup = 4;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between attacks after a hit. A miss recovers in a third of this.")
            @Configurable.Range(min = 1, max = 100)
            public int bloodbursterAttackCooldown = 12;
        }

        @Configurable
        @Configurable.Synchronized
        public NeophyteConfigs neophyteConfigs = new NeophyteConfigs();

        public static class NeophyteConfigs {

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteHealth = 30.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteArmor = 3.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteArmorToughness = 0.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteKnockbackRes = 0.15D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteAttackDamage = 5.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteMovementSpeed = 0.33D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Range at which it notices prey.")
            @Configurable.DecimalRange(min = 0.0D)
            public double neophyteHostileRange = 24.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between the attack telegraph and the hit. Applies to newly spawned mobs.")
            @Configurable.Range(min = 1, max = 40)
            public int neophyteAttackWindup = 5;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between attacks after a hit. A miss recovers in a third of this.")
            @Configurable.Range(min = 1, max = 100)
            public int neophyteAttackCooldown = 14;
        }

        @Configurable
        @Configurable.Synchronized
        public NeomorphConfigs neomorphConfigs = new NeomorphConfigs();

        public static class NeomorphConfigs {

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphHealth = 60.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphArmor = 4.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphArmorToughness = 0.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphKnockbackRes = 0.4D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphAttackDamage = 8.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphMovementSpeed = 0.35D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Range at which it notices prey.")
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphHostileRange = 32.0D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.DecimalRange(min = 0.0D)
            public double neomorphAttackKnockback = 0.4D;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between the attack telegraph and the hit. Applies to newly spawned mobs.")
            @Configurable.Range(min = 1, max = 40)
            public int neomorphAttackWindup = 4;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between attacks after a hit. A miss recovers in a third of this.")
            @Configurable.Range(min = 1, max = 100)
            public int neomorphAttackCooldown = 10;

            @Configurable
            @Configurable.Synchronized
            @Configurable.Comment("Ticks between pounces.")
            @Configurable.Range(min = 10, max = 400)
            public int neomorphLeapCooldown = 60;
        }
    }
}
