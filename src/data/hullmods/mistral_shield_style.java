package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class mistral_shield_style extends BaseHullMod {

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getShield() != null) {
			ship.getShield().setRadius(ship.getShield().getRadius(), Global.getSettings().getSpriteName("fx", "mistral_shields"), Global.getSettings().getSpriteName("fx", "mistral_shields"));
		}
	}
}



