package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;

/**
 * Base script by Tartiflette, additional modifications by shoi
 */
public class mistral_shoulderEffect implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false;
    private ShipAPI ship;
    private WeaponAPI head, gunA, gunB, shoulderL, shoulderR, trueGunA, trueGunB;

    private float originalGunAPos = 0f, originalGunBPos = 0f, originalShoulderLPos = 0f, originalShoulderRPos = 0f;
    private final float SHOULDER_OFFSET = 0;

    public void init() {
        runOnce = true;
        for (WeaponAPI w : ship.getAllWeapons()) {
            switch (w.getSlot().getId()) {
                case "FAKE_GUN_A":
                    if (gunA == null) {
                        gunA = w;
                        originalGunAPos = w.getBarrelSpriteAPI() != null ? w.getBarrelSpriteAPI().getCenterY() : w.getSprite().getCenterY();
                    }
                    break;
                case "FAKE_GUN_B":
                    if (gunB == null) {
                        gunB = w;
                        originalGunBPos = w.getBarrelSpriteAPI() != null ? w.getBarrelSpriteAPI().getCenterY() : w.getSprite().getCenterY();
                    }
                    break;
                case "SHOULDER_L":
                    if (shoulderL == null) {
                        shoulderL = w;
                        originalShoulderLPos = shoulderL.getSprite().getCenterY();
                    }
                    break;
                case "SHOULDER_R":
                    if (shoulderR == null) {
                        shoulderR = w;
                        originalShoulderRPos = shoulderR.getSprite().getCenterY();
                    }
                    break;
                case "HEAD":
                    if (head == null) {
                        head = w;
                    }
                    break;
                case "TRUE_GUN_A":
                    if (trueGunA == null) {
                        trueGunA = w;
                    }
                    break;
                case "TRUE_GUN_B":
                    if (trueGunB == null) {
                        trueGunB = w;
                    }
                    break;
            }
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        ship = weapon.getShip();

        if (!runOnce) {
            init();
        }
        if (gunA == null || gunB == null) {
            return;
        }
        if (engine.isPaused()) {
            return;
        }

        float global = ship.getFacing();
        float aimA = MathUtils.getShortestRotation(global, gunA.getCurrAngle());
        float aimB = MathUtils.getShortestRotation(global, gunB.getCurrAngle());

        // shoulder angle tracking (FAKE_GUN_A pairs with SHOULDER_L, FAKE_GUN_B pairs with SHOULDER_R)
        if (shoulderL != null) {
            shoulderL.setCurrAngle(global + aimA * 0.75f - SHOULDER_OFFSET * 0.5f);
        }

        if (shoulderR != null) {
            shoulderR.setCurrAngle(global + aimB * 0.75f + SHOULDER_OFFSET * 0.5f);
        }

        // recoil (cooldown-driven sprite nudge), now applied to both arms instead of just one
        if (trueGunA != null && trueGunA.getCooldown() > 0) {
            float recoilA = originalGunAPos + (2 * trueGunA.getCooldownRemaining() / trueGunA.getCooldown());
            gunA.getSprite().setCenterY(recoilA);
            if (gunA.getGlowSpriteAPI() != null) {
                gunA.getGlowSpriteAPI().setCenterY(recoilA);
            }
            if (shoulderL != null) {
                shoulderL.getSprite().setCenterY(originalShoulderLPos + (2 * trueGunA.getCooldownRemaining() / trueGunA.getCooldown()));
            }
        }

        if (trueGunB != null && trueGunB.getCooldown() > 0) {
            float recoilB = originalGunBPos + (2 * trueGunB.getCooldownRemaining() / trueGunB.getCooldown());
            gunB.getSprite().setCenterY(recoilB);
            if (gunB.getGlowSpriteAPI() != null) {
                gunB.getGlowSpriteAPI().setCenterY(recoilB);
            }
            if (shoulderR != null) {
                shoulderR.getSprite().setCenterY(originalShoulderRPos + (2 * trueGunB.getCooldownRemaining() / trueGunB.getCooldown()));
            }
        }
    }
}
