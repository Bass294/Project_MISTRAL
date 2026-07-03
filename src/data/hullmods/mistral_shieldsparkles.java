package data.hullmods;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;
import java.util.Map;

public class mistral_shieldsparkles extends BaseHullMod {
    float maxAngle = 40f,
            minAngle = 10f;
    
    // how many particles per burst
    private static final int PARTICLE_COUNT = 10;
    // how fast particles drift after spawning
    private static final float DRIFT_SPEED = 45f;

    public void advanceInCombat(ShipAPI ship, float amount) {
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        if (ship.getShield() == null) return;
        String id = ship.getId();
        IntervalUtil timer = new IntervalUtil(0.1f, 0.15f);
        if (customCombatData.get("mistral_shieldsparkles" + id) instanceof IntervalUtil)
            timer = (IntervalUtil) customCombatData.get("mistral_shieldsparkles" + id);
        if (ship.getShield().isOff()) return;
        timer.advance(amount);
        customCombatData.put("mistral_shieldsparkles" + id, timer);
        if (timer.intervalElapsed()) {
            if (ship.getShield().getActiveArc() < minAngle) return;

            float shieldStartAngle = ship.getShield().getFacing() - (ship.getShield().getActiveArc() * 0.5f);

            for (int i = 0; i < PARTICLE_COUNT; i++) {
                // random point along the shield arc
                float randomAngle = MathUtils.getRandomNumberInRange(
                        shieldStartAngle,
                        shieldStartAngle + ship.getShield().getActiveArc()
                );
                Vector2f particlePos = MathUtils.getPointOnCircumference(
                        ship.getShieldCenterEvenIfNoShield(),
                        ship.getShield().getRadius(),
                        randomAngle
                );

                // random drift direction and speed
                /*float driftAngle = MathUtils.getRandomNumberInRange(0f, 360f);
                float driftSpeed = MathUtils.getRandomNumberInRange(DRIFT_SPEED * 0.5f, DRIFT_SPEED);
                Vector2f velocity = MathUtils.getPointOnCircumference(null, driftSpeed, driftAngle); */

                // drift outward from shield center
                /*float driftSpeed = MathUtils.getRandomNumberInRange(DRIFT_SPEED * 0.5f, DRIFT_SPEED);
                Vector2f velocity = MathUtils.getPointOnCircumference(null, driftSpeed, randomAngle);*/

                //outward but random angle
                /*float driftSpeed = MathUtils.getRandomNumberInRange(DRIFT_SPEED * 0.5f, DRIFT_SPEED);
                float spread = MathUtils.getRandomNumberInRange(-15f, 15f); // degrees of spread
                Vector2f velocity = MathUtils.getPointOnCircumference(null, driftSpeed, randomAngle + spread);*/

                //inherit momentum
                float driftSpeed = MathUtils.getRandomNumberInRange(DRIFT_SPEED * 0.25f, DRIFT_SPEED);
                float spread = MathUtils.getRandomNumberInRange(-15f, 15f);
                Vector2f velocity = MathUtils.getPointOnCircumference(null, driftSpeed, randomAngle + spread);
                Vector2f.add(velocity, ship.getVelocity(), velocity); // inherit ship momentum

                Global.getCombatEngine().addHitParticle(
                        particlePos,
                        velocity,
                        MathUtils.getRandomNumberInRange(2f, 5f),   // size
                        1f,                                          // brightness
                        MathUtils.getRandomNumberInRange(0.8f, 1.6f), // duration
                        new Color(255, 100, 255, 255)                   // color
                );
            }
        }
    }
}