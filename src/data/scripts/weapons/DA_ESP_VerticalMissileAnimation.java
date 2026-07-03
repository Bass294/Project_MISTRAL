package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class DA_ESP_VerticalMissileAnimation implements EveryFrameWeaponEffectPlugin {

    private boolean init = false;
    private int lastFrame = -1;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        AnimationAPI animation = weapon.getAnimation();
        if (animation == null) return;
        if (engine.isPaused()) return;

        if (!init) {
            init = true;
            lastFrame = -1;
            animation.pause();
            animation.setFrame(0);
            return;
        }

        int totalFrameNum = animation.getNumFrames();
        if (totalFrameNum <= 0) return;

        float chargeLevel = weapon.getChargeLevel();

        // Park at frame 0 when idle so AI reads the weapon as ready
        if (chargeLevel <= 0f) {
            if (lastFrame != 0) {
                animation.setFrame(0);
                lastFrame = 0;
            }
            return;
        }

        int targetFrame = (int) Math.floor((totalFrameNum - 1) * chargeLevel);
        targetFrame = Math.max(0, Math.min(targetFrame, totalFrameNum - 1));

        if (targetFrame != lastFrame) {
            animation.setFrame(targetFrame);
            lastFrame = targetFrame;
        }
    }
}