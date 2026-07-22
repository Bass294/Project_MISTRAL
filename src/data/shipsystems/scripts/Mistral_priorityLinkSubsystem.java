package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Base pattern for the scan-ring/diamond-lock visuals: Diableavionics_virtuous_itanoEffect.java (Diable Avionics).
 * Base pattern for the EMP-arc + stat buff: Diableavionics_assumeControlStats.java (Diable Avionics).
 */
public class Mistral_priorityLinkSubsystem extends MagicSubsystem {

    // ---- tuning knobs, adjust freely ----
    private static final String SYSTEM_ID = "mistral_priorityLink";
    private static final float SCAN_RANGE = 1500f;
    private static final float DP_THRESHOLD = 6f;
    private static final String PRIORITY_TAG = "mistralpriotarget";

    private static final float IN_DURATION = 2f;
    private static final float ACTIVE_DURATION = 10f;
    private static final float OUT_DURATION = 0f;
    private static final float COOLDOWN_DURATION = 18f;

    // how many seconds before the buff ends the jitter intensity starts easing down to 0
    private static final float JITTER_FADE_OUT_WINDOW = 2f;

    // beep/diamond-pulse cadence during chargeup - shared so the diamond flash is spawned
    // on exactly the same tick as the beep and lasts no longer than the gap to the next one
    private static final float BEEP_INTERVAL = 0.7f;

    private static final float TIME_MULT = 1.25f;
    private static final float DAMAGE_TAKEN_MULT = 0.66f;
    // derived once for the in-game buff description text
    private static final int TIME_BONUS_PERCENT = Math.round((TIME_MULT - 1f) * 100f);
    private static final int DAMAGE_REDUCTION_PERCENT = Math.round((1f - DAMAGE_TAKEN_MULT) * 100f);
    //private static final Color JITTER_COLOR = new Color(150, 100, 50, 50);
    private static final Color EMP_FRINGE = Color.CYAN;
    private static final Color EMP_CORE = Color.WHITE;

    // vanilla Temporal Shell's own sound cues (starsector-core/data/config/sounds.json) - ramp up
    // once when the buff lands, loop for as long as it holds, ramp down once when it's removed
    private static final String TIME_BUFF_ACTIVATE_SOUND = "system_temporalshell";
    private static final String TIME_BUFF_LOOP_SOUND = "system_temporalshell_loop";
    private static final String TIME_BUFF_DEACTIVATE_SOUND = "system_temporalshell_off";

    // vanilla icons (starsector-core), reused so the HUD status readout looks native
    private static final String TIME_STATUS_ICON = "graphics/icons/hullsys/temporal_shell.png";
    private static final String DAMAGE_STATUS_ICON = "graphics/icons/hullsys/damper_field.png";

    // ships currently buffed by ANY instance of this subsystem, cleared between battles
    private static final Set<ShipAPI> BUFFED_TARGETS = Collections.newSetFromMap(new WeakHashMap<ShipAPI, Boolean>());

    private final int maxTargets;
    private final List<ShipAPI> selectedTargets = new ArrayList<>();
    private float beepTimer = 0f;
    private float errorSoundTimer = 0f;

    // MagicSubsystem.setState() calls onStateSwitched(newState), not the actual old state
    // (despite the parameter name/javadoc), and `state` is already reassigned by that point.
    // Track the previous state ourselves so we can detect the IN -> ACTIVE transition.
    private State lastKnownState = State.READY;

    // throttle for the AI activation check, same pattern as Diableavionics_virtuousItanoAI's TICK
    private final IntervalUtil aiCheckInterval = new IntervalUtil(1.5f, 2.5f);

    public Mistral_priorityLinkSubsystem(ShipAPI ship, int maxTargets) {
        super(ship);
        this.maxTargets = maxTargets;
    }

    // shared lookup for hullmod tooltips (mistral_priorityLink/mistral_priorityLink_dual) that
    // want to display this ship's actual assigned hotkey via MagicSubsystem#getKeyText()
    public static Mistral_priorityLinkSubsystem getAttachedInstance(ShipAPI ship) {
        if (ship == null) return null;

        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems == null) return null;

        for (MagicSubsystem subsystem : subsystems) {
            if (subsystem instanceof Mistral_priorityLinkSubsystem) {
                return (Mistral_priorityLinkSubsystem) subsystem;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return ORDER_SHIP_UNIQUE;
    }

    @Override
    public String getDisplayText() {
        return "Wanzer Amp Relay";
    }

    @Override
    public float getBaseInDuration() {
        return IN_DURATION;
    }

    @Override
    public float getBaseActiveDuration() {
        return ACTIVE_DURATION;
    }

    @Override
    public float getBaseOutDuration() {
        return OUT_DURATION;
    }

    @Override
    public float getBaseCooldownDuration() {
        return COOLDOWN_DURATION;
    }

    @Override
    public boolean isToggle() {
        return false;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        // only bother throttled-checking while READY; no point re-evaluating mid chargeup/active/cooldown
        if (state != State.READY) {
            return false;
        }

        aiCheckInterval.advance(amount);
        if (!aiCheckInterval.intervalElapsed()) {
            return false;
        }

        // NOTE: deliberately not using AIUtils.canUseSystemThisFrame(ship) here - it checks
        // ship.getSystem(), the ship's own unrelated built-in vanilla ship system (e.g.
        // diableavionics_assumeControl/diableavionics_drift), not this MagicSubsystem. Using it
        // was blocking Priority Link's AI activation based on a completely different system's
        // cooldown/ammo/flux state. MagicSubsystem's own canActivateInternal()/canActivate()
        // already gate charges, flux cost, and state correctly for this subsystem.

        if (!AIUtils.getNearbyEnemies(ship, SCAN_RANGE).isEmpty()) {
            return true;
        }

        return areFighterWingsEngaged(ship);
    }

    private boolean areFighterWingsEngaged(ShipAPI carrier) {
        for (FighterWingAPI wing : carrier.getAllWings()) {
            for (ShipAPI fighter : wing.getWingMembers()) {
                if (fighter == null || !fighter.isAlive() || fighter.isHulk()) continue;

                ShipAPI fighterTarget = fighter.getShipTarget();
                if (fighterTarget == null || fighterTarget.getOwner() == fighter.getOwner()) continue;

                // having a target locked isn't "engaged" by itself - the fighter needs to actually
                // be within its own weapon range of that target, otherwise this fires as soon as a
                // target is picked, well before the fighters have closed to firing range
                float weaponRange = getMaxWeaponRange(fighter);
                if (weaponRange <= 0f) continue;

                if (MathUtils.getDistance(fighter, fighterTarget) <= weaponRange) {
                    return true;
                }
            }
        }
        return false;
    }

    private float getMaxWeaponRange(ShipAPI fighter) {
        float maxRange = 0f;
        for (WeaponAPI weapon : fighter.getAllWeapons()) {
            maxRange = Math.max(maxRange, weapon.getRange());
        }
        return maxRange;
    }

    @Override
    public boolean canActivate() {
        // don't re-run the (moderately expensive) target search every frame the key is held;
        // canActivateInternal() already blocks activation whenever state != READY
        if (state != State.READY) {
            return true;
        }

        List<ShipAPI> found = findTargets();
        if (found.isEmpty()) {
            if (errorSoundTimer <= 0f) {
                Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 1f, ship.getLocation(), ship.getVelocity());
                errorSoundTimer = 1f;
            }
            return false;
        }

        selectedTargets.clear();
        selectedTargets.addAll(found);
        return true;
    }

    @Override
    public void onActivate() {
        for (ShipAPI target : selectedTargets) {
            BUFFED_TARGETS.add(target);
        }

        // scan AOE ring, one pulse timed to last through the chargeup
        MagicRender.objectspace(
                Global.getSettings().getSprite("diableavionics", "RING"),
                ship,
                new Vector2f(),
                new Vector2f(),
                new Vector2f(64, 64),
                new Vector2f(2000, 2000),
                MathUtils.getRandomNumberInRange(-180, 180),
                0f,
                false,
                new Color(0, 200, 255, 128),
                true,
                0f, 0f, 0.2f, 0.5f, 0.05f,
                0.05f, Math.max(0.1f, getInDuration() - 0.1f), 0.3f,
                true,
                CombatEngineLayers.UNDER_SHIPS_LAYER
        );

        // targeting diamond + initial lock beep on each selected target
        for (ShipAPI target : selectedTargets) {
            if (target == null) continue;

            Global.getSoundPlayer().playSound("diableavionics_virtuousTarget_beep", 1f, 0.5f, ship.getLocation(), ship.getVelocity());
            spawnDiamondPulse(target);
        }
    }

    // one short flash of the targeting diamond, timed to fully fade before the next beep -
    // called on the same tick as each beep (here and in the advance() IN-state loop) so the
    // blink and the sfx read as a single synced pulse instead of two independent animations
    private void spawnDiamondPulse(ShipAPI target) {
        if (target == null) return;

        MagicRender.objectspace(
                Global.getSettings().getSprite("diableavionics", "DIAMOND"),
                target,
                new Vector2f(),
                new Vector2f(),
                new Vector2f(64, 64),
                new Vector2f(),
                45f,
                0f,
                false,
                Color.orange,
                false,
                0f, 0f,
                0f, 0f, 0f,
                BEEP_INTERVAL * 0.25f, BEEP_INTERVAL * 0.4f, BEEP_INTERVAL * 0.35f,
                true,
                CombatEngineLayers.BELOW_INDICATORS_LAYER
        );
    }

    @Override
    public void onStateSwitched(State newState) {
        State previousState = lastKnownState;
        lastKnownState = state;

        if (state == State.ACTIVE && previousState == State.IN) {
            for (ShipAPI target : selectedTargets) {
                if (target == null || !target.isAlive()) continue;

                Global.getCombatEngine().spawnEmpArcVisual(
                        ship.getLocation(), ship,
                        target.getLocation(), target,
                        3f, EMP_FRINGE, EMP_CORE
                );

                applyBuff(target);
                Global.getSoundPlayer().playSound(TIME_BUFF_ACTIVATE_SOUND, 1f, 1f, target.getLocation(), target.getVelocity());
            }
        }
    }

    @Override
    public void onFinished() {
        for (ShipAPI target : selectedTargets) {
            removeBuff(target);
        }
        selectedTargets.clear();
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isCombatOver()) {
            BUFFED_TARGETS.clear();
            return;
        }

        if (isPaused) return;

        if (errorSoundTimer > 0f) {
            errorSoundTimer -= amount;
        }

        if (state == State.IN) {
            beepTimer -= amount;
            if (beepTimer <= 0f) {
                beepTimer = BEEP_INTERVAL;
                for (ShipAPI target : selectedTargets) {
                    if (target == null || !target.isAlive()) continue;
                    Global.getSoundPlayer().playSound("diableavionics_virtuousTarget_beep", 1f, 0.5f, ship.getLocation(), ship.getVelocity());
                    spawnDiamondPulse(target);
                }
            }
        } else if (state == State.ACTIVE) {
            // same setJitter formula as Mistral_overLoadStats_small, applied to the buffed target(s)
            // instead of the ship using the system. jitterOpacity fades 1 -> 0 across the active window.
            float effectLevel = getEffectLevel();
            float jitterOpacity = 1f - getStateCompleteRatio();

            // intensity stays at its high baseline for most of the buff, then eases down to 0
            // only in the last JITTER_FADE_OUT_WINDOW seconds - avoids the abrupt cutoff that
            // happens when ACTIVE ends and setJitter simply stops being called altogether
            float secondsRemaining = jitterOpacity * ACTIVE_DURATION;
            float intensityFade = MathUtils.clamp(secondsRemaining / JITTER_FADE_OUT_WINDOW, 0f, 1f);

            for (ShipAPI target : selectedTargets) {
                if (target == null || !target.isAlive()) continue;
                target.setJitter(target,
                        Color.CYAN,
                        (0.5f + 0.2f * effectLevel) * intensityFade,
                        3,
                        (2 + 3f * effectLevel) * jitterOpacity,
                        (5 + 6f * effectLevel) * jitterOpacity
                );

                // needs to be called every frame to keep playing - fades itself out the moment
                // this stops being refreshed, so it naturally ends when the target leaves ACTIVE
                Global.getSoundPlayer().playLoop(TIME_BUFF_LOOP_SOUND, target, 1f, 1f, target.getLocation(), target.getVelocity());

                // same HUD readout vanilla ship systems use for the piloted ship (e.g. Temporal
                // Shell's "time flow altered") - only shows up if the buffed target happens to be
                // the ship the player is currently flying, same restriction the native call has
                if (target == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(
                            SYSTEM_ID + "_time_" + target.getId(), TIME_STATUS_ICON,
                            getDisplayText(), "time flow altered +" + TIME_BONUS_PERCENT + "%", false);
                    Global.getCombatEngine().maintainStatusForPlayerShip(
                            SYSTEM_ID + "_dmg_" + target.getId(), DAMAGE_STATUS_ICON,
                            getDisplayText(), "damage taken reduced " + DAMAGE_REDUCTION_PERCENT + "%", false);
                }
            }
        }
    }

    // ---- targeting ----

    private List<ShipAPI> findTargets() {
        List<ShipAPI> highDPFrigates = new ArrayList<>();
        List<ShipAPI> lowDPFrigates = new ArrayList<>();

        for (ShipAPI other : CombatUtils.getShipsWithinRange(ship.getLocation(), SCAN_RANGE)) {
            if (!isEligible(other)) continue;
            if (other.getHullSize() != HullSize.FRIGATE) continue;

            if (getDP(other) > DP_THRESHOLD) {
                highDPFrigates.add(other);
            } else {
                lowDPFrigates.add(other);
            }
        }
        sortByDPThenDistance(highDPFrigates);
        sortByDPThenDistance(lowDPFrigates);

        List<ShipAPI> priorityTargets = new ArrayList<>();
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (!isEligible(other)) continue;
            if (other.getHullSpec() != null && other.getHullSpec().hasTag(PRIORITY_TAG)) {
                priorityTargets.add(other);
            }
        }
        sortByDistance(priorityTargets);

        // fill both slots if possible: exhaust the highest tier first, then backfill
        // from the next tier down rather than stopping as soon as one tier is non-empty
        List<ShipAPI> result = new ArrayList<>();

        // whatever fighter/frigate this ship currently has targeted takes priority over every
        // other tier below, same pattern as armaa_RecallDeviceStats.apply()'s ship.getShipTarget()
        // check - if it qualifies, it's grabbed immediately instead of going through the pools
        ShipAPI targetedPriority = findTargetedPriority();
        if (targetedPriority != null) {
            result.add(targetedPriority);
        }

        addUpToLimit(result, highDPFrigates);
        addUpToLimit(result, priorityTargets);
        addUpToLimit(result, lowDPFrigates);

        return result;
    }

    private ShipAPI findTargetedPriority() {
        ShipAPI target = ship.getShipTarget();
        if (target == null) return null;
        if (target.getHullSize() != HullSize.FRIGATE && target.getHullSize() != HullSize.FIGHTER) return null;
        if (!isEligible(target)) return null;
        if (MathUtils.getDistance(target, ship) > SCAN_RANGE) return null;
        return target;
    }

    private void addUpToLimit(List<ShipAPI> result, List<ShipAPI> candidates) {
        for (ShipAPI candidate : candidates) {
            if (result.size() >= maxTargets) return;
            if (!result.contains(candidate)) {
                result.add(candidate);
            }
        }
    }

    private boolean isEligible(ShipAPI other) {
        if (other == null || other == ship) return false;
        if (!other.isAlive() || other.isHulk()) return false;
        if (other.getOwner() != ship.getOwner()) return false;
        if (other.getHullSpec() != null && other.getHullSpec().getHints().contains(ShipTypeHints.MODULE)) return false;
        return !BUFFED_TARGETS.contains(other);
    }

    private float getDP(ShipAPI s) {
        FleetMemberAPI fm = s.getFleetMember();
        return fm != null ? fm.getDeploymentPointsCost() : 0f;
    }

    private void sortByDPThenDistance(List<ShipAPI> list) {
        Collections.sort(list, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI a, ShipAPI b) {
                float dpA = getDP(a);
                float dpB = getDP(b);
                if (dpA != dpB) {
                    return Float.compare(dpB, dpA); // higher DP first
                }
                return Float.compare(MathUtils.getDistance(a, ship), MathUtils.getDistance(b, ship));
            }
        });
    }

    private void sortByDistance(List<ShipAPI> list) {
        Collections.sort(list, new Comparator<ShipAPI>() {
            @Override
            public int compare(ShipAPI a, ShipAPI b) {
                return Float.compare(MathUtils.getDistance(a, ship), MathUtils.getDistance(b, ship));
            }
        });
    }

    // ---- buff apply/remove (mirrors Diableavionics_assumeControlStats) ----

    private void applyBuff(ShipAPI target) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getTimeMult().modifyMult(SYSTEM_ID, TIME_MULT);
        stats.getHullDamageTakenMult().modifyMult(SYSTEM_ID, DAMAGE_TAKEN_MULT);
        stats.getArmorDamageTakenMult().modifyMult(SYSTEM_ID, DAMAGE_TAKEN_MULT);
        stats.getShieldDamageTakenMult().modifyMult(SYSTEM_ID, DAMAGE_TAKEN_MULT);

        //target.setWeaponGlow(1f, Misc.setAlpha(JITTER_COLOR, 50), EnumSet.allOf(WeaponType.class));
        //target.setJitterUnder(target, JITTER_COLOR, 1f, 5, 3, 6);
    }

    private void removeBuff(ShipAPI target) {
        if (target == null) return;

        Global.getSoundPlayer().playSound(TIME_BUFF_DEACTIVATE_SOUND, 1f, 1f, target.getLocation(), target.getVelocity());

        target.getMutableStats().getTimeMult().unmodify(SYSTEM_ID);
        target.getMutableStats().getHullDamageTakenMult().unmodify(SYSTEM_ID);
        target.getMutableStats().getArmorDamageTakenMult().unmodify(SYSTEM_ID);
        target.getMutableStats().getShieldDamageTakenMult().unmodify(SYSTEM_ID);

        //target.setWeaponGlow(0f, Color.BLACK, EnumSet.allOf(WeaponType.class));

        BUFFED_TARGETS.remove(target);
    }
}
