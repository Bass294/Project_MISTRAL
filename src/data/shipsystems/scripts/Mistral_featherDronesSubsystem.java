package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.magiclib.subsystems.drones.PIDController;

import java.awt.Color;
import java.util.Map;

/**
 * Always-on drone screen, using the mistral_feather_drone hull. Base pattern for the
 * always-on spawn/forced-shield-up loop: ShieldDronesSubsystem (Knights of Ludd,
 * org.selkie.zea.combat.subsystems), used as reference only. Unlike that system, drones here
 * hold fixed angular slots relative to the ship's own facing instead of orbiting it.
 */
public class Mistral_featherDronesSubsystem extends MagicDroneSubsystem {

    private static final int NUM_DRONES = 3;
    // forward, then +/-30 degrees off the ship's nose
    private static final float[] SLOT_ANGLE_OFFSETS = {0f, 30f, -30f};
    private static final float SLOT_DISTANCE_MULT = 1.25f;

    // flux-based shield tint, mirrored from ShieldDronesSubsystem
    private static final Color BASE_SHIELD_COLOR = Color.CYAN;
    private static final Color HIGHEST_FLUX_SHIELD_COLOR = Color.RED;
    private static final float SHIELD_ALPHA = 0.25f;
    private static final float HIGHEST_FLUX_LEVEL_RATIO = 0.75f;

    public Mistral_featherDronesSubsystem(ShipAPI ship) {
        super(ship);
    }

    @Override
    public boolean canAssignKey() {
        return false;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 2f;
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        return 10f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return canActivate();
    }

    @Override
    public int getMaxCharges() {
        return 0;
    }

    @Override
    public int getMaxDeployedDrones() {
        return NUM_DRONES;
    }

    @Override
    public String getDroneVariant() {
        // spawnShipOrWingDirectly (FleetMemberType.FIGHTER_WING) needs the wing_data.csv id,
        // not the .variant id directly - it resolves the variant through that row's "variant" column
        return "mistral_feather_drone_wing";
    }

    @Override
    public DroneFormation getDroneFormation() {
        return new FixedFrontFormation();
    }

    @Override
    public PIDController getPIDController() {
        return new PIDController(15f, 3.5f, 10f, 2f);
    }

    @Override
    public String getDisplayText() {
        return "Feather Drones";
    }

    @Override
    public void onActivate() {
        for (ShipAPI drone : getActiveWings().keySet()) {
            if (drone.getFluxLevel() > 0f) {
                drone.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;

        // forced shield-up, mirrored from the dawn drone system: keep shields raised
        // unless already overloaded/venting
        for (ShipAPI drone : getActiveWings().keySet()) {
            if (drone.getShield() != null && drone.getShield().isOff() && !drone.getFluxTracker().isOverloadedOrVenting()) {
                drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            } else {
                drone.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

                /*
                if (drone.getShield() != null) {
                    float ratio = drone.getFluxLevel();
                    float baseWeight = MathUtils.clamp((1f - ratio) / HIGHEST_FLUX_LEVEL_RATIO, 0f, 1f);
                    float highFluxWeight = MathUtils.clamp(ratio / HIGHEST_FLUX_LEVEL_RATIO, 0f, 1f);
                    float red = BASE_SHIELD_COLOR.getRed() * baseWeight + HIGHEST_FLUX_SHIELD_COLOR.getRed() * highFluxWeight;
                    float green = BASE_SHIELD_COLOR.getGreen() * baseWeight + HIGHEST_FLUX_SHIELD_COLOR.getGreen() * highFluxWeight;
                    float blue = BASE_SHIELD_COLOR.getBlue() * baseWeight + HIGHEST_FLUX_SHIELD_COLOR.getBlue() * highFluxWeight;
                    drone.getShield().setInnerColor(new Color(
                            MathUtils.clamp(red / 255f, 0f, 1f),
                            MathUtils.clamp(green / 255f, 0f, 1f),
                            MathUtils.clamp(blue / 255f, 0f, 1f),
                            SHIELD_ALPHA
                    ));
                }
                */
            }
        }
    }

    /**
     * One drone dead ahead, two flanking at +/-30 degrees, all locked to those angular
     * offsets from the ship's own facing - no independent idle rotation - and facing
     * directly away from the ship, matching whichever slot they're holding.
     */
    private static class FixedFrontFormation extends DroneFormation {
        @Override
        public void advance(ShipAPI ship, Map<ShipAPI, ? extends PIDController> drones, float amount) {
            int index = 0;
            for (Map.Entry<ShipAPI, ? extends PIDController> entry : drones.entrySet()) {
                if (index >= SLOT_ANGLE_OFFSETS.length) break;

                ShipAPI drone = entry.getKey();
                PIDController controller = entry.getValue();

                float targetAngle = ship.getFacing() + SLOT_ANGLE_OFFSETS[index];
                float distance = ship.getShieldRadiusEvenIfNoShield() * SLOT_DISTANCE_MULT;
                Vector2f targetPoint = MathUtils.getPointOnCircumference(ship.getLocation(), distance, targetAngle);

                controller.move(targetPoint, drone);
                controller.rotate(targetAngle, drone);

                index++;
            }
        }
    }
}
