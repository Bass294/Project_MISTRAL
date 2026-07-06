package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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
    private static final float COOLDOWN_DURATION = 25f;

    private static final float TIME_MULT = 1.25f;
    private static final float DAMAGE_TAKEN_MULT = 0.66f;
    private static final Color JITTER_COLOR = new Color(150, 100, 50, 50);
    private static final Color EMP_FRINGE = Color.RED;
    private static final Color EMP_CORE = Color.WHITE;

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

    @Override
    public int getOrder() {
        return ORDER_SHIP_UNIQUE;
    }

    @Override
    public String getDisplayText() {
        return "Priority Link";
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

            Global.getSoundPlayer().playSound("diableavionics_virtuousTarget_beep", 1f, 1f, ship.getLocation(), ship.getVelocity());

            //targeting diamond on the target itself (was missing the jitterRange/jitterTilt pair before flickerRange/flickerMedian/maxDelay)
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
                    2f, 1f, 0.2f,
                    0.3f, Math.max(0.1f, getInDuration() - 0.6f), 0.3f,
                    true,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER
            );
        }
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
                beepTimer = 0.2f;
                for (ShipAPI target : selectedTargets) {
                    if (target == null || !target.isAlive()) continue;
                    Global.getSoundPlayer().playSound("diableavionics_virtuousTarget_beep", 1f, 1f, ship.getLocation(), ship.getVelocity());
                }
            }
        } else if (state == State.ACTIVE) {
            // same setJitter formula as Mistral_overLoadStats_small, applied to the buffed target(s)
            // instead of the ship using the system. jitterOpacity fades 1 -> 0 across the active window.
            float effectLevel = getEffectLevel();
            float jitterOpacity = 1f - getStateCompleteRatio();
            for (ShipAPI target : selectedTargets) {
                if (target == null || !target.isAlive()) continue;
                target.setJitter(target,
                        Color.RED,
                        0.4f * effectLevel * jitterOpacity,
                        3,
                        (4 + 5f * effectLevel) * jitterOpacity,
                        (7 + 10f * effectLevel) * jitterOpacity
                );
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
        addUpToLimit(result, highDPFrigates);
        addUpToLimit(result, priorityTargets);
        addUpToLimit(result, lowDPFrigates);

        return result;
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

        target.setWeaponGlow(1f, Misc.setAlpha(JITTER_COLOR, 50), EnumSet.allOf(WeaponType.class));
        target.setJitterUnder(target, JITTER_COLOR, 1f, 5, 3, 6);
    }

    private void removeBuff(ShipAPI target) {
        if (target == null) return;

        target.getMutableStats().getTimeMult().unmodify(SYSTEM_ID);
        target.getMutableStats().getHullDamageTakenMult().unmodify(SYSTEM_ID);
        target.getMutableStats().getArmorDamageTakenMult().unmodify(SYSTEM_ID);
        target.getMutableStats().getShieldDamageTakenMult().unmodify(SYSTEM_ID);

        target.setWeaponGlow(0f, Color.BLACK, EnumSet.allOf(WeaponType.class));

        BUFFED_TARGETS.remove(target);
    }
}
