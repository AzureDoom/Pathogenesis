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
            "Spread budget: candidate block checks per processed zone. Doubled while the source still leaks."
        )
        @Configurable.Range(min = 1, max = 256)
        public int blockChecksPerZone = 12;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Allow the Pathogen to convert water source blocks into contaminated water.")
        public boolean contaminateWater = true;

        @Configurable
        @Configurable.Synchronized
        @Configurable.Comment("Allow the Pathogen to convert snow layers and snow blocks.")
        public boolean contaminateSnow = true;
    }

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
    @Configurable.Comment("Base attribute values. Read when attributes are registered, so changes need a restart.")
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
        }
    }
}
