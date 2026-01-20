package io.iridium.overvaults.config.vault;



import com.google.gson.annotations.Expose;
import com.mojang.datafixers.util.Pair;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.config.vault.entry.CrystalDataEntry;
import io.iridium.overvaults.config.vault.entry.ModifierStackEntry;
import io.iridium.overvaults.config.vault.entry.PortalEntry;
import io.iridium.overvaults.mixin.accessor.AccessorPoolCrystalTheme;
import iskallia.vault.VaultMod;
import iskallia.vault.config.Config;
import iskallia.vault.core.world.roll.IntRoll;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.layout.ClassicInfiniteCrystalLayout;
import iskallia.vault.item.crystal.objective.BingoCrystalObjective;
import iskallia.vault.item.crystal.objective.ScavengerCrystalObjective;
import iskallia.vault.item.crystal.theme.PoolCrystalTheme;
import iskallia.vault.item.crystal.theme.ValueCrystalTheme;
import iskallia.vault.item.crystal.time.ValueCrystalTime;
import iskallia.vault.util.data.WeightedList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OverVaultsPortalConfig extends Config {

    private static final Random RANDOM = new Random();
    @Expose public float CHANCE_OF_NETHER_THEME_IN_NETHER;
    @Expose public float CHANCE_OF_VOID_THEME_IN_END;
    @Expose public WeightedList<ResourceLocation> DIMENSION_CHOOSE_LIST = new WeightedList<>();
    @Expose public WeightedList<PortalEntry> PORTAL_LIST = new WeightedList<>();
    @Expose public List<ResourceLocation> NETHER_VAULT_THEMES = new ArrayList<>();
    @Expose public List<ResourceLocation> VOID_VAULT_THEMES = new ArrayList<>();

    @Override
    public String getName() {
        return "overvaults_portals";
    }

    @Override
    protected void reset() {
        CHANCE_OF_NETHER_THEME_IN_NETHER = 0.10F;
        CHANCE_OF_VOID_THEME_IN_END = 0.075F;

        DIMENSION_CHOOSE_LIST.add(Level.OVERWORLD.location(), 1);
        DIMENSION_CHOOSE_LIST.add(Level.NETHER.location(), 1);
        DIMENSION_CHOOSE_LIST.add(Level.END.location(), 1);

        CrystalDataEntry entry0 = new CrystalDataEntry(
                new BingoCrystalObjective(),
                new ClassicInfiniteCrystalLayout(1),
                new PoolCrystalTheme(VaultMod.id("raw")),
                List.of(new ModifierStackEntry(VaultMod.id("locked"), 1), new ModifierStackEntry(VaultMod.id("soul_boost"), 4)),
                new ValueCrystalTime(IntRoll.ofConstant(1800)),
                20,
                false
                );

        PORTAL_LIST.add(new PortalEntry(entry0, "overvaults.portal.tier.s", true), 20);

        CrystalDataEntry entry1 = new CrystalDataEntry(
                new ScavengerCrystalObjective(0.6F, VaultMod.id("default")),
                new ClassicInfiniteCrystalLayout(1),
                new PoolCrystalTheme(VaultMod.id("raw")),
                List.of(new ModifierStackEntry(VaultMod.id("energizing"), 16), new ModifierStackEntry(VaultMod.id("item_quantity"), 16)),
                new ValueCrystalTime(IntRoll.ofConstant(3600)),
                100,
                false
        );

        PORTAL_LIST.add(new PortalEntry(entry1, "overvaults.portal.tier.splusplus", true), 2);

        NETHER_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_nether_crimson"));
        NETHER_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_nether_warped"));
        NETHER_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_nether_blackstone"));
        NETHER_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_nether_soul"));

        VOID_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_void"));
        VOID_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_end_void"));
        VOID_VAULT_THEMES.add(new ResourceLocation("the_vault:classic_vault_factory_void"));
    }


    public static Pair<PortalEntry, CrystalData> getRandomCrystalData(ResourceKey<Level> dimension) {
        OverVaultsPortalConfig cfg = VaultConfigRegistry.OVERVAULTS_PORTAL_CONFIG;

        CrystalData crystal = CrystalData.empty();

        PortalEntry portalEntry = cfg.PORTAL_LIST.getRandom(Config.rand);
        if (portalEntry != null) {
            CrystalDataEntry.applyData(portalEntry.getCrystalData(), crystal);
            ResourceLocation curTheme = null;

            if(portalEntry.getCrystalData().getTheme() instanceof PoolCrystalTheme theme) {
                curTheme = ((AccessorPoolCrystalTheme) theme).getId();
            }

            if(dimension.equals(Level.NETHER) && RANDOM.nextFloat() <= cfg.CHANCE_OF_NETHER_THEME_IN_NETHER) {

                if(!(curTheme != null && curTheme.equals(VaultMod.id("raw")))) {
                    crystal.setTheme(new ValueCrystalTheme(
                            cfg.NETHER_VAULT_THEMES.get(RANDOM.nextInt(cfg.NETHER_VAULT_THEMES.size()))
                    ));
                }
            } else if (dimension.equals(Level.END) && RANDOM.nextFloat() <= cfg.CHANCE_OF_VOID_THEME_IN_END) {
                if(!(curTheme != null && curTheme.equals(VaultMod.id("raw")))) {
                    crystal.setTheme(new ValueCrystalTheme(
                            cfg.NETHER_VAULT_THEMES.get(RANDOM.nextInt(cfg.NETHER_VAULT_THEMES.size()))
                    ));
                }
                if(!(curTheme != null && curTheme.equals(VaultMod.id("raw")))) {
                    crystal.setTheme(new ValueCrystalTheme(
                            cfg.VOID_VAULT_THEMES.get(RANDOM.nextInt(cfg.VOID_VAULT_THEMES.size()))
                    ));
                }
            }


        } else {
            OverVaults.LOGGER.error("Found invalid portal data inside of '{}' config!", VaultConfigRegistry.OVERVAULTS_PORTAL_CONFIG.getName());
        }

        return Pair.of(portalEntry, crystal);
    }


    public ServerLevel getLevel(MinecraftServer srv)  {
        return srv.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, this.DIMENSION_CHOOSE_LIST.getRandom(Config.rand)));
    }
}
