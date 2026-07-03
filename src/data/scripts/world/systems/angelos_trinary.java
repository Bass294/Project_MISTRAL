package data.scripts.world.systems;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.Global;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;


import java.awt.Color;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import static com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.addOrbitingEntities;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * Generates the Angelos trinary system.
 *
 * NOTE ON UNVERIFIED CONSTANTS / METHODS:
 *   - system.createToken(x, y) is the standard technique for creating a non-visual
 *     orbital anchor (used here so all three stars orbit a shared system center,
 *     like a binary's barycenter). Verify this method signature against your API version.
 *   - Industries.ORE_REFINING (refining industry)
 *   - planet type strings "gas_giant" (used for the ice giant), "rocky_ice", "volcanic"
 */
public class angelos_trinary {

    // Faction IDs used by this system's markets
    private static final String FACTION_ANGELOS = "angelos";
    private static final String FACTION_DIABLE = "diableavionics";

    public void generate(SectorAPI sector) {
        // ===================== SYSTEM SETUP =====================
        StarSystemAPI system = sector.createStarSystem("Angelos");

        //system.setBackgroundTextureFilename("graphics/backgrounds/angelos_background.jpg");
        system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");
        generateNebula(system);
        system.getLocation().set(16500f, -17000f);

        // Invisible anchor entity - all three stars orbit this shared center,
        // similar to how a binary system orbits a barycenter rather than one star
        // being the fixed "anchor" for the others.
        SectorEntityToken systemCenter = system.createToken(0f, 0f);

        //STARS
        PlanetAPI ohr = system.initStar("Ohr", "star_blue_giant", 1200f, 1200f, 5f, 0.5f, 2f);
        ohr.setName("Ohr");
        ohr.setCircularOrbit(systemCenter, 120f, 2000f, 80f);
        system.setLightColor(new Color(195, 210, 255));

        PlanetAPI ein = system.addPlanet("Ein", systemCenter, "Ein", "star_white", 0f, 320f, 2000f, 80f);
        system.addCorona(ein, 320f, 0f, 0f, 0f);

        PlanetAPI soph = system.addPlanet("Soph", systemCenter, "Soph", "star_orange", 240f, 750f, 2000f, 80f);
        system.addCorona(soph, 750f, 3f, 0.1f, 1f);


        //ASTEROID BELYS
        //system.addAsteroidBelt(systemCenter, 250, 5000, 120, 30, 50);

        //inner stuff
        system.addAsteroidBelt(systemCenter, 100, 4300, 300, 160, 220); // Ring system located between inner and outer planets
        system.addRingBand(systemCenter, "misc", "rings_ice0", 256f, 0, Color.white, 512f, 4300, 200f, null, null);
        //system.addRingBand(systemCenter, "misc", "rings_ice0", 256f, 0, Color.white, 256f, 4340, 200f, null, null);

        system.addAsteroidBelt(systemCenter, 200, 5000, 500, 160, 220); // Ring system located between inner and outer planets
        //system.addRingBand(systemCenter, "misc", "rings_asteroids0", 256f, 2, Color.white, 256f, 4940, 200f, null, null);
        //system.addRingBand(systemCenter, "misc", "rings_ice0", 256f, 1, Color.white, 256f, 5060, 200f, null, null);
        system.addRingBand(systemCenter, "misc", "rings_ice0", 256f, 1, Color.white, 512f, 5000, 200f, null, null);


        //outer stuff
        // Large asteroid belt around system center - bigger than Kara's orbit, smaller than Ciella's
        //system.addAsteroidBelt(systemCenter, 600, 11500, 400, 600, 700, Terrain.ASTEROID_BELT, "");

        system.addAsteroidBelt(systemCenter, 400, 11500, 700, 640, 880); // Ring system encircling outer planets
        system.addRingBand(systemCenter, "misc", "rings_ice0", 256f, 2, Color.white, 512f, 11500, 800f, null, null);
        //system.addRingBand(systemCenter, "misc", "rings_dust0", 256f, 1, Color.white, 512f, 11500, 200f, null, null);


        // Surtr - desert world, small orbit, no market
        PlanetAPI surtr = system.addPlanet("Surtr", systemCenter, "Surtr", "desert", 40f, 170f, 5800f, 250f);
        //surtr.setCustomDescriptionId("angelos_surtr");

        // Dust ring orbiting Surtr itself
        system.addRingBand(surtr, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 300f, 125f, Terrain.RING, null);

        // Inner system jump point - similar radius to Surtr
        JumpPointAPI innerJump = Global.getFactory().createJumpPoint("angelos_inner_jump", "Angelos Inner Jump-point");
        innerJump.setCircularOrbit(systemCenter, 150f, 5800f, 320f);
        innerJump.setStandardWormholeToHyperspaceVisual();
        system.addEntity(innerJump);

        // Kara - water world, medium orbit, market size 4, faction angelos
        PlanetAPI kara = system.addPlanet("Kara", systemCenter, "Kara", "water", 80f, 240f, 9500f, 420f);
        //kara.setCustomDescriptionId("angelos_kara");

        List<String> karaConditions = Arrays.asList(
                Conditions.POPULATION_4,
                Conditions.HABITABLE,
                Conditions.ORGANICS_ABUNDANT,
                Conditions.WATER_SURFACE
        );
        List<String> karaSubmarkets = Arrays.asList(
                Submarkets.SUBMARKET_OPEN,
                Submarkets.SUBMARKET_STORAGE
        );
        List<String> karaIndustries = Arrays.asList(
                Industries.POPULATION,
                Industries.SPACEPORT,
                Industries.WAYSTATION,
                Industries.AQUACULTURE,
                Industries.LIGHTINDUSTRY
        );
        MarketAPI karaMarket = addMarket(FACTION_ANGELOS, kara, kara.getName(), 4,
                karaConditions, karaSubmarkets, karaIndustries);

        kara.getSpec().setTexture(
                Global.getSettings().getSpriteName("planets", "atoll_texture")
        );
        kara.getSpec().setCloudTexture(
                Global.getSettings().getSpriteName("planets", "atoll_clouds")
        );

        kara.applySpecChanges();


        // Iskandar - barren moon of Kara, military market size 5, faction angelos
        PlanetAPI iskandar = system.addPlanet("Iskandar", kara, "Iskandar", "barren", 0f, 80f, 750f, 60f);
        //iskandar.setCustomDescriptionId("angelos_iskandar");

        List<String> iskandarConditions = Arrays.asList(
                Conditions.POPULATION_5,
                Conditions.NO_ATMOSPHERE,
                Conditions.HOT,
                Conditions.ORE_ABUNDANT,
                Conditions.RARE_ORE_MODERATE
        );
        List<String> iskandarSubmarkets = Arrays.asList(
                Submarkets.SUBMARKET_OPEN,
                Submarkets.GENERIC_MILITARY,
                Submarkets.SUBMARKET_BLACK,
                Submarkets.SUBMARKET_STORAGE
        );
        List<String> iskandarIndustries = Arrays.asList(
                Industries.POPULATION,
                Industries.MEGAPORT,
                Industries.WAYSTATION,
                Industries.MILITARYBASE,
                Industries.BATTLESTATION_HIGH,
                Industries.GROUNDDEFENSES,
                Industries.ORBITALWORKS,
                Industries.REFINING
        );
        MarketAPI iskandarMarket = addMarket(FACTION_ANGELOS, iskandar, iskandar.getName(), 5,
                iskandarConditions, iskandarSubmarkets, iskandarIndustries);
        // Mirrors the "protected military world" style flag used on Jenius in the Gamlin system
        iskandarMarket.getMemoryWithoutUpdate().set("$nex_uninvadable", true);

        // Inactive gate - bigger radius than Kara, smaller than the large asteroid belt
        SectorEntityToken gate = system.addCustomEntity("angelos_gate", "Angelos Gate", "inactive_gate", Factions.NEUTRAL);
        gate.setCircularOrbit(systemCenter, 260f, 9200f, 500f);

        // Ciella - ice giant, large orbit around system center, no market
        PlanetAPI ciella = system.addPlanet("Ciella", systemCenter, "Ciella", "ice_giant", 300f, 480f, 14500f, 1500f); // type id - verify "gas_giant" vs an ice-giant-specific id in your version
        //ciella.setCustomDescriptionId("angelos_ciella");
        //ciella.getSpec().setPlanetColor(new Color(190, 215, 235, 255));
        //ciella.applySpecChanges();

        // Ayaha - rocky ice moon of Ciella, no market
        PlanetAPI ayaha = system.addPlanet("Ayaha", ciella, "Ayaha", "rocky_ice", 0f, 70f, 1450f, 80f); // type id - verify "rocky_ice" spelling
        //ayaha.setCustomDescriptionId("angelos_ayaha");

        // Otoha - volcanic moon of Ciella, opposite Ayaha at the same radius, market size 4, faction diableavionics
        PlanetAPI otoha = system.addPlanet("Otoha", ciella, "Otoha", "lava_minor", 180f, 80f, 1100f, 60f); // type id - verify "volcanic" spelling
        //otoha.setCustomDescriptionId("angelos_otoha");

        List<String> otohaConditions = Arrays.asList(
                Conditions.POPULATION_4,
                Conditions.NO_ATMOSPHERE,
                Conditions.ORE_ULTRARICH,
                Conditions.RARE_ORE_RICH,
                Conditions.VERY_HOT
                //Conditions.HAZARDOUS_TERRAIN
        );
        List<String> otohaSubmarkets = Arrays.asList(
                Submarkets.SUBMARKET_OPEN,
                Submarkets.GENERIC_MILITARY,
                Submarkets.SUBMARKET_BLACK,
                Submarkets.SUBMARKET_STORAGE
        );
        List<String> otohaIndustries = Arrays.asList(
                Industries.POPULATION,
                Industries.MILITARYBASE,
                Industries.MINING,
                Industries.MEGAPORT,
                Industries.WAYSTATION,
                Industries.ORBITALSTATION_HIGH,
                Industries.GROUNDDEFENSES
        );
        MarketAPI otohaMarket = addMarket(FACTION_DIABLE, otoha, otoha.getName(), 4,
                otohaConditions, otohaSubmarkets, otohaIndustries);

        // Small asteroid belt around Ciella, inside Ayaha/Otoha's orbit
        system.addAsteroidBelt(ciella, 40, 800, 60, 55, 75);
        system.addRingBand(ciella, "misc", "rings_ice0", 256f, 0, Color.white, 256f, 800, 65f, null, null);

        // Two dust rings around Ciella, outside Ayaha/Otoha's orbit
        system.addRingBand(ciella, "misc", "rings_special0", 256f, 0, Color.white, 256f, 1750f, 130f,Terrain.RING, null);


        // Outer system jump point - slightly larger radius than Ciella's orbit
        JumpPointAPI outerJump = Global.getFactory().createJumpPoint("angelos_outer_jump", "Angelos Outer Jump-point");
        outerJump.setCircularOrbit(systemCenter, 10f, 14800f, 1550f);
        outerJump.setStandardWormholeToHyperspaceVisual();
        system.addEntity(outerJump);

        // ===================== MISC ENTITIES =====================

        // Abandoned research station - smaller radius than Surtr
        SectorEntityToken researchStation = system.addCustomEntity("angelos_research_station", "Abandoned Research Station", "station_side07", Factions.NEUTRAL);
        researchStation.setCircularOrbitPointingDown(systemCenter, 320f, 5000f, 200f);
        researchStation.setCustomDescriptionId("angelos_station_research");
        Misc.setAbandonedStationMarket("angelos_research_station", researchStation);

        // Nav buoy, comm relay, sensor array - placed at arbitrary "random" locations; adjust freely
        SectorEntityToken navBuoy = system.addCustomEntity("angelos_nav_buoy", "Nav Buoy", "nav_buoy", Factions.NEUTRAL);
        navBuoy.setCircularOrbit(systemCenter, 95f, 6500f, 360f);

        SectorEntityToken commRelay = system.addCustomEntity("angelos_comm_relay", "Comm Relay", "comm_relay", Factions.NEUTRAL);
        commRelay.setCircularOrbit(systemCenter, 210f, 12500f, 980f);

        SectorEntityToken sensorArray = system.addCustomEntity("angelos_sensor_array", "Sensor Array", "sensor_array", Factions.NEUTRAL);
        sensorArray.setCircularOrbit(systemCenter, 60f, 16500f, 1700f);

        // ===================== WRAP-UP =====================
        Misc.setAllPlanetsSurveyed(system, true);
        system.autogenerateHyperspaceJumpPoints(true, false);

        cleanup(system);
    }

    /**
     * Self-contained vanilla-API market creation helper (the original Gamlin file used a
     * mod-specific addMarketplace() helper from another mod's codebase; this version only
     * relies on standard MarketAPI/SectorEntityToken calls).
     */
    private MarketAPI addMarket(String factionId, SectorEntityToken entity, String marketName, int size,
                                 List<String> conditions, List<String> submarkets, List<String> industries) {
        MarketAPI market = Global.getFactory().createMarket(entity.getId(), marketName, size);
        market.setPrimaryEntity(entity);
        market.setFactionId(factionId);

        for (String c : conditions) {
            market.addCondition(c);
        }
        for (String i : industries) {
            market.addIndustry(i);
        }
        for (String s : submarkets) {
            market.addSubmarket(s);
        }

        entity.setMarket(market);
        entity.setFaction(factionId);

        Global.getSector().getEconomy().addMarket(market, true);
        return market;
    }

    // Clears a hole in the surrounding hyperspace nebula so the system isn't obscured
    // by hyperspace storms - same approach used in the Gamlin example.
    void cleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }


    protected void generateNebula(StarSystemAPI system) {


        SectorEntityToken nebulaTiles = Misc.addNebulaFromPNG("data/campaign/terrain/nebula_angelos.png",
                0, 0, // Center of nebula
                system, // Location to add to
                "terrain", "nebula", // Texture to use, uses xxx_map for map
                4, 4, Terrain.NEBULA, StarAge.OLD);

        nebulaTiles.getLocation().set(0, 0);

        BaseTiledTerrain nebula = getNebula(system);
        nebula.setTerrainName("Angelos Nebula");

    }

    BaseTiledTerrain getNebula(StarSystemAPI system) {
        for (CampaignTerrainAPI curr : system.getTerrainCopy()) {
            if (curr.getPlugin().getTerrainId().equals(Terrain.NEBULA)) {
                return (BaseTiledTerrain) (curr.getPlugin());
            }
        }
        return null;
    }
}
