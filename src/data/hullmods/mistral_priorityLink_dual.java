package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.shipsystems.scripts.Mistral_priorityLinkSubsystem;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.util.List;

public class mistral_priorityLink_dual extends BaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (hasSubsystem(ship)) {
            return;
        }
        MagicSubsystemsManager.addSubsystemToShip(ship, new Mistral_priorityLinkSubsystem(ship, 2));
    }

    private boolean hasSubsystem(ShipAPI ship) {
        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems == null) {
            return false;
        }
        for (MagicSubsystem subsystem : subsystems) {
            if (subsystem instanceof Mistral_priorityLinkSubsystem) {
                return true;
            }
        }
        return false;
    }
}
