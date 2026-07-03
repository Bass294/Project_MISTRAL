package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class mistral_diableenginefix extends BaseHullMod {

	// -------------------------------------------------------------------------
	// Tuneable constants
	// -------------------------------------------------------------------------

	/** Base radius of each contrail particle in world units. */
	private static final float BASE_SIZE        = 60f;
	//old = 12
	/**
	 * Amplitude of the size pulse.
	 * Final radius oscillates between (BASE_SIZE - PULSE_AMPLITUDE)
	 * and (BASE_SIZE + PULSE_AMPLITUDE).
	 */
	private static final float PULSE_AMPLITUDE  = 15f;
	//old = 6
	/** How many full pulse cycles occur per second. */
	private static final float PULSE_FREQUENCY  = 0.6f;

	/** How long (seconds) each particle lives before fading out. */
	private static final float PARTICLE_DURATION = 0.1f;

	/** Base colour of the contrail (RGBA). Adjust to match your ship's theme. */
	//private static final Color CONTRAIL_COLOR   = new Color(80, 200, 255, 160);
	private static final Color CONTRAIL_COLOR   = new Color(33,103,120,16);
	//[33,103,120,52]

	//????????????
	private static final Color ENGINE_COLOR   = new Color(33,103,120,16);

	/**
	 * Slight random spread added to each particle's velocity so the trail
	 * does not look perfectly uniform (world-units per second).
	 */
	private static final float VELOCITY_SPREAD  = 0f;

	/**
	 * Fraction of the ship's own velocity inherited by particles.
	 * 1.0 = particles appear stationary relative to ship.
	 * 0.0 = particles are dropped in world-space.
	 */
	private static final float VELOCITY_INHERIT = 0f;

	/**
	 * Key used to persist the per-ship pulse phase in ship.getCustomData(),
	 * so each ship maintains an independent phase even across the same frame.
	 */
	private static final String PHASE_KEY = "mistral_diableenginefix_contrail_phase";

	// -------------------------------------------------------------------------
	// advanceInCombat — called every frame by the game while in combat
	// -------------------------------------------------------------------------

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		// Bail out if the ship is dead, being phased out, or not yet fully spawned.
		if (ship == null || !ship.isAlive()) return;

		// Retrieve the combat engine for particle spawning.
		CombatEngineAPI combatEngine = Global.getCombatEngine();
		if (combatEngine == null || combatEngine.isPaused()) return;

		applyEngineContrailEffect(ship, combatEngine, amount);
	}

	// -------------------------------------------------------------------------
	// Contrail + pulse logic
	// -------------------------------------------------------------------------

	/**
	 * Spawns a pulsing glow-particle behind every active engine nozzle on the ship.
	 *
	 * How the pulse works:
	 *   A phase accumulator (stored per-ship in customData) advances each frame:
	 *       phase += amount * PULSE_FREQUENCY * 2π
	 *   The current particle size is derived from the sine of that phase:
	 *       currentSize = BASE_SIZE + PULSE_AMPLITUDE * sin(phase)
	 *   This smoothly oscillates between (BASE_SIZE - PULSE_AMPLITUDE) and
	 *   (BASE_SIZE + PULSE_AMPLITUDE) at PULSE_FREQUENCY cycles per second.
	 *   Each ship is initialised with a random phase offset so ships never
	 *   pulse in visible lockstep with one another.
	 *
	 * @param ship         The ship whose engines emit the contrail.
	 * @param combatEngine CombatEngineAPI used to spawn particles.
	 * @param amount       Seconds elapsed since the last frame.
	 */
	private void applyEngineContrailEffect(ShipAPI ship,
	                                       CombatEngineAPI combatEngine,
	                                       float amount) {
		// ----- Advance and store the per-ship pulse phase -----
		float phase = getPulsePhase(ship);
		phase += amount * PULSE_FREQUENCY * 2f * (float) Math.PI;
		phase  = phase % (2f * (float) Math.PI);   // keep in [0, 2π] to avoid drift
		setPulsePhase(ship, phase);

		// Current pulsing size derived from the sine envelope
		//float currentSize = BASE_SIZE + PULSE_AMPLITUDE * (float) Math.sin(phase);

		// ----- Iterate every engine slot on the ship -----
		for (ShipEngineControllerAPI.ShipEngineAPI shipEngine
				: ship.getEngineController().getShipEngines()) {

			//kills the default contrails by the default diable stuff
			ship.getEngineController().fadeToOtherColor(
					this,                          // key — "this" ties it to this hullmod instance
					shipEngine.getEngineColor(),         // engine flame color
					new Color(0, 0, 0, 0),         // contrail color — fully transparent
					1.0f,                          // effect level — 1.0 = fully overridden
					1.0f                           // max blend — 1.0 = complete replacement
			);

			// Skip disabled engines (damaged, EMP'd, shut down by ship system, etc.)
			//if (!shipEngine.isActive() || ship.getEngineController().isIdle()) continue;
			//float effectLevel = shipEngine.getContribution();
			//if (effectLevel <= 0.01f) continue;
			// World-space nozzle location for this engine

			// Set intensity based on idle state — low glow when coasting, full effect when thrusting
			float effectLevel = ship.getEngineController().isIdle() ? 0.8f : 1.0f;

// Skip only truly dead engines
			if (ship.getEngineController().isDisabled()) continue;

			float currentSize = (BASE_SIZE + PULSE_AMPLITUDE * (float) Math.sin(phase)) * effectLevel;



			Vector2f engineLoc = shipEngine.getLocation();

			// Small random positional jitter — prevents particles stacking into a
			// single bright point and gives the trail a soft, organic feel.
			float offsetX = MathUtils.getRandomNumberInRange(-2f, 2f);
			float offsetY = MathUtils.getRandomNumberInRange(-2f, 2f);
			Vector2f spawnLoc = new Vector2f(engineLoc.x + offsetX,
					engineLoc.y + offsetY);

			// Particle velocity = partial ship-velocity inheritance + random spread.
			// Keeping VELOCITY_INHERIT < 1.0 makes the trail visibly "peel off"
			// behind the ship rather than hovering in place.
			Vector2f shipVel = ship.getVelocity();
			float velX = shipVel.x * VELOCITY_INHERIT
					+ MathUtils.getRandomNumberInRange(-VELOCITY_SPREAD, VELOCITY_SPREAD);
			float velY = shipVel.y * VELOCITY_INHERIT
					+ MathUtils.getRandomNumberInRange(-VELOCITY_SPREAD, VELOCITY_SPREAD);
			Vector2f particleVel = new Vector2f(velX, velY);

			// addSmoothParticle: fades in then fades out — ideal for soft engine glows.
			// Signature: (loc, vel, size, brightness, duration, color)
			//float currentSize = (BASE_SIZE + PULSE_AMPLITUDE * (float) Math.sin(phase));

			combatEngine.addSmoothParticle(
					spawnLoc,
					particleVel,
					currentSize,      // pulsing radius driven by sine wave above
					effectLevel,             // brightness, 0–1 used to be 1.0
					PARTICLE_DURATION,
					CONTRAIL_COLOR
			);
		}
	}

	// -------------------------------------------------------------------------
	// Per-ship phase helpers — phase is persisted via ship.getCustomData()
	// -------------------------------------------------------------------------

	/**
	 * Returns the current pulse phase for this ship.
	 * If no phase has been stored yet, initialises it to a random value in
	 * [0, 2π] so multiple ships desynchronise naturally from the start.
	 */
	private float getPulsePhase(ShipAPI ship) {
		Object stored = ship.getCustomData().get(PHASE_KEY);
		if (stored instanceof Float) {
			return (Float) stored;
		}
		float initial = MathUtils.getRandomNumberInRange(0f, (float) (2 * Math.PI));
		ship.setCustomData(PHASE_KEY, initial);
		return initial;
	}

	/** Persists the updated phase so it is available next frame. */
	private void setPulsePhase(ShipAPI ship, float phase) {
		ship.setCustomData(PHASE_KEY, phase);
	}
}