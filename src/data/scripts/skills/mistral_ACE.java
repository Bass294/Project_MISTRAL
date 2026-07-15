package data.scripts.skills;

import org.lwjgl.util.vector.Vector2f;

import java.util.Arrays;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.impl.campaign.skills.*;
import com.fs.starfarer.api.combat.listeners.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.DefenseUtils;

public class mistral_ACE {

    public static float BUFF_FACTOR = 1.1f;
    public static float PEAK_TIME_BONUS = 60;
    public static float DEGRADE_REDUCTION_PERCENT = 25f;
    public static float MAX_CR_BONUS = 15;
    public static int CREW_THRESHOLD = 20;
    public static float BONUS_MAX = 25f;
    private static final float INCREASE_AMT = 1f;
    public static float MAX_REGEN_LEVEL = 0.25f;
    public static float TIME_RATE = 0.10f;
    public static float TOTAL_REGEN_MAX_POINTS = 2000f;
    public static float TOTAL_REGEN_MAX_HULL_FRACTION = 0.5f;
    public static float ENCORE_MISSILE_RELOAD_PERC_SMALL = 1.0f;
    public static float ENCORE_MISSILE_RELOAD_PERC_MEDIUM = 0.5f;
    public static float ENCORE_MISSILE_RELOAD_PERC_LARGE = 0.25f;
    public static float ENCORE_RELOAD_PERC = 1.0f;
    public static List<String> ENCORE_APPROVED_MANUFACTURERS = Arrays.asList("Diable Avionics", "Angelos Contingent", "Garou Labs");

    public static class Level1 implements ShipSkillEffect {

        public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
            if (stats.getEntity() != null) {
                ShipAPI ship = (ShipAPI) stats.getEntity();
                if (ship.getHullSpec().getMinCrew() <= CREW_THRESHOLD) {
                    float pct = (BUFF_FACTOR - 1f) * 100f; // 1.2f -> +20%
                    stats.getFluxDissipation().modifyPercent(id, pct);
                    stats.getFluxCapacity().modifyPercent(id, pct);
                    stats.getMaxSpeed().modifyPercent(id, pct);
                    stats.getAcceleration().modifyPercent(id, pct);
                    stats.getTurnAcceleration().modifyPercent(id, pct);
                    stats.getMaxTurnRate().modifyPercent(id, pct);
                    stats.getDamageToFighters().modifyPercent(id, pct);
                    stats.getShieldDamageTakenMult().modifyMult(id, 1f - (BUFF_FACTOR - 1f));
                    stats.getPeakCRDuration().modifyPercent(id, 10f);
                }
            }

        }

        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getFluxDissipation().unmodify(id);
            stats.getFluxCapacity().unmodify(id);
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getAutofireAimAccuracy().unmodify(id);
            stats.getShieldDamageTakenMult().unmodify(id);
            stats.getPeakCRDuration().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getDamageToFighters().unmodify(id);          
        }

        public String getEffectDescription(float level) {
            int pct = (int) ((BUFF_FACTOR - 1f) * 100f);
            int crPct = 10;
            return "On a ship with a skeleton crew of " + CREW_THRESHOLD + " or less:\n +" + pct
                    + "% flux capacity and dissipation, speed, acceleration, and damage to fighters\n-"
                    + pct + "% shield damage taken\n+" + crPct + "% peak performance time";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Level2 implements ShipSkillEffect {

        public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {

        }

        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {

        }

        public String getEffectDescription(float level) {
            return "Shooting down fighters increases manuverability and auto-aim, up to " + (int) BONUS_MAX + "%";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Level3 implements ShipSkillEffect {

        public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
            stats.getPeakCRDuration().modifyMult(id, BUFF_FACTOR);
        }

        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getPeakCRDuration().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return "The amount gained per fighter is determined by its DP divided by wing size";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Level4 extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {

        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new CombatEnduranceRegen(ship));

            EncoreReload reload = new EncoreReload(ship);
            ship.addListener(reload);
            Global.getCombatEngine().getListenerManager().addListener(reload);
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(CombatEnduranceRegen.class);

            if (ship.hasListenerOfClass(EncoreReload.class)) {
                EncoreReload reload = ship.getListeners(EncoreReload.class).get(0);
                Global.getCombatEngine().getListenerManager().removeListener(reload);
                ship.removeListener(reload);
            }
        }

        public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        }

        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                TooltipMakerAPI info, float width) {
            initElite(stats, skill);

            //info.addPara("When below %s hull, non-missile RoF is increased by %s.", 0f, hc, hc,
            //        "" + (int) Math.round(MAX_REGEN_LEVEL * 100f) + "%",
            //        "" + (int) BONUS_MAX + "%"
            //);
            //
            //info.addSpacer(10f);

            info.addPara("On a Diable Avionics ship with a skeleton crew of %s or less:", 0f, hc, hc,
                    "" + CREW_THRESHOLD
            );

            info.addPara("After disabling an enemy ship, reload %s of base ammunition for ballistic and energy weapons, and %s / %s / %s of base ammunition for small / medium / large missile weapons.", 0f, hc, hc,
                    "" + (int) (ENCORE_RELOAD_PERC * 100f) + "%",
                    "" + (int) (ENCORE_MISSILE_RELOAD_PERC_SMALL * 100f) + "%",
                    "" + (int) (ENCORE_MISSILE_RELOAD_PERC_MEDIUM * 100f) + "%",
                    "" + (int) (ENCORE_MISSILE_RELOAD_PERC_LARGE * 100f) + "%"
            );
        }
    }

    public static class EncoreReload implements HullDamageAboutToBeTakenListener {

        protected ShipAPI pilotedShip;

        public EncoreReload(ShipAPI pilotedShip) {
            this.pilotedShip = pilotedShip;
        }

        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (!(param instanceof ShipAPI)) {
                return false;
            }
            ShipAPI killer = (ShipAPI) param;
            if (killer != pilotedShip) {
                return false;
            }
            if (pilotedShip.getHullSpec().getMinCrew() > CREW_THRESHOLD) {
                return false;
            }
            if (!ENCORE_APPROVED_MANUFACTURERS.contains(pilotedShip.getHullSpec().getManufacturer())) {
                return false;
            }
            if (ship.isFighter()) {
                return false;
            }
            if (ship.getOwner() == pilotedShip.getOwner()) {
                return false;
            }
            if (ship.getHitpoints() > 0f || ship.hasTag("mistral_ace_encore_counted")) {
                return false;
            }
            ship.addTag("mistral_ace_encore_counted");

            //if (pilotedShip == Global.getCombatEngine().getPlayerShip()) {
            //    Global.getSoundPlayer().playSound("nightcross_reload", 1f, 0.7f, pilotedShip.getLocation(), Misc.ZERO);
            //}

            for (WeaponAPI weapon : pilotedShip.getAllWeapons()) {
                if (weapon.getType() == WeaponAPI.WeaponType.ENERGY
                        || weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
                    float ammoMin = weapon.getSpec().getMaxAmmo() * ENCORE_RELOAD_PERC;
                    float remainder = ammoMin - (float) Math.floor(ammoMin);
                    ammoMin = (float) Math.floor(ammoMin);
                    int bonus = remainder > 0f ? (Math.random() < remainder ? 1 : 0) : 0;
                    weapon.getAmmoTracker().setAmmo((int) Math.min(weapon.getAmmoTracker().getMaxAmmo(),
                            weapon.getAmmoTracker().getAmmo() + ammoMin + bonus));
                } else if (weapon.getType() == WeaponAPI.WeaponType.MISSILE
                        || weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY) {
                    float missilePerc;
                    if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL) {
                        missilePerc = ENCORE_MISSILE_RELOAD_PERC_SMALL;
                    } else if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
                        missilePerc = ENCORE_MISSILE_RELOAD_PERC_MEDIUM;
                    } else {
                        missilePerc = ENCORE_MISSILE_RELOAD_PERC_LARGE;
                    }
                    float ammoMin = weapon.getSpec().getMaxAmmo() * missilePerc;
                    float remainder = ammoMin - (float) Math.floor(ammoMin);
                    ammoMin = (float) Math.floor(ammoMin);
                    int bonus = remainder > 0f ? (Math.random() < remainder ? 1 : 0) : 0;
                    weapon.getAmmoTracker().setAmmo((int) Math.min(weapon.getAmmoTracker().getMaxAmmo(),
                            weapon.getAmmoTracker().getAmmo() + ammoMin + bonus));
                }
            }

            return false;
        }
    }

    public static class CombatEnduranceRegen implements DamageTakenModifier, AdvanceableListener {

        protected ShipAPI ship;
        protected boolean inited = false;
        protected boolean runOnce = false;
        protected float limit = 0f;
        protected float repaired = 0f;
        protected String repKey1;
        protected String repKey2;
        protected IntervalUtil interval = new IntervalUtil(2f, 5f);
        protected IntervalUtil interval2 = new IntervalUtil(1.5f, 1.5f);

        public CombatEnduranceRegen(ShipAPI ship) {
            this.ship = ship;
        }

        protected void init() {
            if (!ship.hasListenerOfClass(PeacekeeperDeatMod.class)) {
                ship.addListener(new PeacekeeperDeatMod(ship));
            }
            inited = true;
        }

        public void advance(float amount) {
            if (!inited) {
                init();
            }
            /*
            boolean chatterEnabled = true;
            if (Global.getSettings().getModManager().isModEnabled("lunalib")
                    && ship.getCaptain() != null && ship.getCaptain().getId().equals("armaa_dawn")) {
                chatterEnabled = LunaSettings.getBoolean("armaa", "armaa_enableDawnVoice");
            }
            
            if (interval.intervalElapsed() && !runOnce && !ship.isStationModule() && chatterEnabled && ship.getOwner() == 0) {
                if (!Global.getCombatEngine().getCustomData().containsKey("armaa_dawnChattered")) {
                    Global.getCombatEngine().getCustomData().put("armaa_dawnChattered", "-");
                    Global.getSoundPlayer().playUISound("armaa_dawn_intro", 1, 0.90f);
                    runOnce = true;
                }
            }
            */
            String id = ship.getId();

            interval.advance(amount);
            interval2.advance(amount);
            MutableShipStatsAPI stats = ship.getMutableStats();
            //if (ship.getHullLevel() < 0.25f) {
            //    stats.getEnergyRoFMult().modifyPercent(id, 25f);
            //    stats.getBallisticRoFMult().modifyPercent(id, 25f);
            //} else {
            //    stats.getEnergyRoFMult().unmodify(id);
            //    stats.getBallisticRoFMult().unmodify(id);
            //}
            if (Global.getCombatEngine().getCustomData().get("eis_wingClipper_bonus_" + ship.getId()) instanceof Float) {
                float currentBonus = Math.min(BONUS_MAX, (Float) Global.getCombatEngine().getCustomData().get("eis_wingClipper_bonus_" + ship.getId()));
                stats.getFluxDissipation().modifyPercent(id, currentBonus);
                stats.getAutofireAimAccuracy().modifyPercent(id, currentBonus);
                stats.getMaxTurnRate().modifyPercent(id, currentBonus);
                stats.getAcceleration().modifyPercent(id, currentBonus * 2f);
                stats.getDeceleration().modifyPercent(id, currentBonus);
                stats.getTurnAcceleration().modifyPercent(id, currentBonus * 2f);
                if (currentBonus > 0f && interval2.intervalElapsed()) {
                    Global.getCombatEngine().getCustomData().put("eis_wingClipper_bonus_" + ship.getId(), currentBonus - .1f);
                }

            } else {
                stats.getFluxDissipation().unmodify(id);
                stats.getAutofireAimAccuracy().unmodify(id);
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);
                stats.getAcceleration().unmodify(id);
                stats.getDeceleration().unmodify(id);
            }
        }

        public String modifyDamageTaken(Object param,
                CombatEntityAPI target, DamageAPI damage,
                Vector2f point, boolean shieldHit) {
            return null;
        }

        public static class PeacekeeperDeatMod implements DamageDealtModifier, AdvanceableListener {

            protected ShipAPI ship;

            public PeacekeeperDeatMod(ShipAPI ship) {
                this.ship = ship;
            }

            public void advance(float amount) {

            }

            public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (!(target instanceof ShipAPI)) {
                    return null;
                }

                if (shieldHit) {
                    return null;
                }

                ShipAPI s = (ShipAPI) target;
                if (s.isHulk()) {
                    return null;
                }
                if (!s.isFighter()) {
                    return null;
                }
                String id = "strikecraft_death";
                float damageVal = damage.getDamage();
                if (damageVal >= s.getHitpoints() && s.getHitpoints() > 0) {
                    s.setHitpoints(0f);
                    float fp = s.getWing() != null ? (float) s.getWing().getSpec().getFleetPoints() / (float) s.getWing().getSpec().getNumFighters() : INCREASE_AMT;
                    float currentBonus = 0f;
                    if (Global.getCombatEngine().getCustomData().get("eis_wingClipper_bonus_" + ship.getId()) instanceof Float) {
                        currentBonus = (Float) Global.getCombatEngine().getCustomData().get("eis_wingClipper_bonus_" + ship.getId());
                    }
                    Global.getCombatEngine().getCustomData().put("eis_wingClipper_bonus_" + ship.getId(), currentBonus + fp);
                }
                return id;
            }
        }

    }
}
