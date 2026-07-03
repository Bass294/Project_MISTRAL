package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;
import java.util.Map;

public class mistral_shieldStats extends BaseShipSystemScript {

    private static final int PARTICLE_COUNT = 100;
    private static final float DRIFT_SPEED = 45f;
    private static final float minAngle = 10f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getShieldTurnRateMult().modifyMult(id, 2f);
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - .9f * effectLevel);
        stats.getShieldUpkeepMult().modifyMult(id, 0f);





        // particle effect while system is active
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;
        if (ship.getShield() == null || ship.getShield().isOff()) return;
        if (ship.getShield().getActiveArc() < minAngle) return;

        Map<String, Object> customData = Global.getCombatEngine().getCustomData();
        String timerId = "Diableavionics_shieldStats_sparkles" + ship.getId();

        IntervalUtil timer = new IntervalUtil(0.1f, 0.15f);
        if (customData.get(timerId) instanceof IntervalUtil)
            timer = (IntervalUtil) customData.get(timerId);

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        timer.advance(amount);
        customData.put(timerId, timer);

        /*
        // one-time flash on activation
        if (state == State.IN) {
            if (ship.getShield() != null && !ship.getShield().isOff()) {
                float shieldStartAngle = ship.getShield().getFacing() - (ship.getShield().getActiveArc() * 0.5f);

                //number of particles
                for (int i = 0; i < 150; i++) {
                    float randomAngle = MathUtils.getRandomNumberInRange(
                            shieldStartAngle,
                            shieldStartAngle + ship.getShield().getActiveArc()
                    );
                    Vector2f particlePos = MathUtils.getPointOnCircumference(
                            ship.getShieldCenterEvenIfNoShield(),
                            ship.getShield().getRadius(),
                            randomAngle
                    );

                    float driftSpeed = MathUtils.getRandomNumberInRange(DRIFT_SPEED * 0.25f, DRIFT_SPEED);
                    float spread = MathUtils.getRandomNumberInRange(-15f, 15f);
                    Vector2f velocity = MathUtils.getPointOnCircumference(null, driftSpeed, randomAngle + spread);
                    Vector2f.add(velocity, ship.getVelocity(), velocity);

                    Global.getCombatEngine().addHitParticle(
                            particlePos,
                            velocity,
                            //MathUtils.getRandomNumberInRange(4f, 10f),  // bigger than normal
                            MathUtils.getRandomNumberInRange(2f, 5f),
                            1f,
                            MathUtils.getRandomNumberInRange(0.3f, 0.5f), // ~half second duration
                            new Color(107,195,210,255)
                    );
                }
            }
        }
        */

        if (timer.intervalElapsed()) {
            float shieldStartAngle = ship.getShield().getFacing() - (ship.getShield().getActiveArc() * 0.5f);

            for (int i = 0; i < PARTICLE_COUNT; i++) {
                float randomAngle = MathUtils.getRandomNumberInRange(
                        shieldStartAngle,
                        shieldStartAngle + ship.getShield().getActiveArc()
                );
                Vector2f particlePos = MathUtils.getPointOnCircumference(
                        ship.getShieldCenterEvenIfNoShield(),
                        ship.getShield().getRadius(),
                        randomAngle
                );

                float driftSpeed = MathUtils.getRandomNumberInRange(DRIFT_SPEED * 0.25f, DRIFT_SPEED);
                float spread = MathUtils.getRandomNumberInRange(-15f, 15f);
                Vector2f velocity = MathUtils.getPointOnCircumference(null, driftSpeed, randomAngle + spread);
                Vector2f.add(velocity, ship.getVelocity(), velocity);

                Global.getCombatEngine().addHitParticle(
                        particlePos,
                        velocity,
                        MathUtils.getRandomNumberInRange(2f, 5f),
                        1f,
                        MathUtils.getRandomNumberInRange(0.8f, 1.6f),
                        //new Color(255, 100, 255, 255)
                        new Color(107,195,210,255)


                );
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldTurnRateMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
    }

    //private final String TXT1 = txt("shield");
    //private final String TXT2 = txt("%");

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        //int effect = Math.round(1000 * effectLevel);
        if (index == 0) {
            //return new StatusData(TXT1 + effect + TXT2, false);
            return new StatusData("shield absorbs 10x damage", false);
        }
        return null;
    }
}