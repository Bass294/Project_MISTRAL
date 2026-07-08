package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.shipsystems.scripts.Mistral_featherDronesSubsystem;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.util.List;

public class mistral_featherDrones extends BaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (hasSubsystem(ship)) {
            return;
        }
        MagicSubsystemsManager.addSubsystemToShip(ship, new Mistral_featherDronesSubsystem(ship));
    }

    private boolean hasSubsystem(ShipAPI ship) {
        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems == null) {
            return false;
        }
        for (MagicSubsystem subsystem : subsystems) {
            if (subsystem instanceof Mistral_featherDronesSubsystem) {
                return true;
            }
        }
        return false;
    }
}
