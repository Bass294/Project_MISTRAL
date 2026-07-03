package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class DA_ESP_kestrelmirv implements MissileAIPlugin, GuidedMissileAI {
    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private final IntervalUtil intervalUtil = new IntervalUtil(1f, 1f);
    private boolean speedSet = false;  // ADD THIS

    public DA_ESP_kestrelmirv(MissileAPI missile) {
        this.missile = missile;
    }

    @Override
    public void advance(float amount) {
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        //cancelling IF: skip the AI if the game is paused, the missile is engineless or fading
        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        // ADD THIS BLOCK
        if (!speedSet) {
            Vector2f initialVel = MathUtils.getPointOnCircumference(null, 5f, missile.getFacing());
            missile.getVelocity().set(initialVel);
            speedSet = true;
        }

        // Clamp speed every tick AFTER accelerate runs
        missile.giveCommand(ShipCommand.ACCELERATE);
        float currentSpeed = missile.getVelocity().length();
        if (currentSpeed > 20f) {
            // Scale velocity back down to max speed
            Vector2f vel = missile.getVelocity();
            float scale = 20f / currentSpeed;
            vel.set(vel.x * scale, vel.y * scale);
        }

        //missile.giveCommand(ShipCommand.ACCELERATE);
        intervalUtil.advance(amount);  // was: intervalUtil.advance(0.01f);
        if (intervalUtil.intervalElapsed()) {
            mirv(missile);
        }
    }

    private void mirv(MissileAPI missile) {
        for (int a = 0; a < 5; ++a) {
            Vector2f vel = MathUtils.getRandomPointInCone(new Vector2f(), (float) (a * 25), missile.getFacing() - 182f, missile.getFacing() - 180f);
            Vector2f.add(vel, missile.getSource().getVelocity(), vel);
            float size = MathUtils.getRandomNumberInRange(10f, 15f);
            float duration = MathUtils.getRandomNumberInRange(0.5f, 1f);
            Global.getCombatEngine().addSmokeParticle(missile.getLocation(), vel, size, 60f, duration, Color.lightGray);
        }
        CombatEntityAPI missile1 = Global.getCombatEngine().spawnProjectile(missile.getSource(), missile.getWeapon(), "diableavionics_esp_kestrel2", missile.getLocation(), missile.getFacing(), null);
        missile1.setMass(1);
        Global.getCombatEngine().removeEntity(missile);
    }

    @Override
    public CombatEntityAPI getTarget() {
        return null;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
    }
}
