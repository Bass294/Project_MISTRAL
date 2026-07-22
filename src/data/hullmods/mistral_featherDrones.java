package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.shipsystems.scripts.Mistral_featherDronesSubsystem;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.Color;
import java.util.List;

import static data.scripts.util.Mistral_stringsManager.txt;

public class mistral_featherDrones extends BaseHullMod {

    private static final Color YELLOW = new Color(241, 199, 0);

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

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        TooltipMakerAPI subsystemInfo = tooltip.beginImageWithText("graphics/icons/hullsys/drone_sensor.png", 40f);
        tooltip.addSectionHeading(txt("hm_featherDrones_tabSubsystem"), Alignment.MID, 10f);
        subsystemInfo.addPara(txt("hm_featherDrones_bullet1"), 3, YELLOW, "•", "3", "10 seconds");
        subsystemInfo.addPara(txt("hm_featherDrones_bullet2"), 3, YELLOW, "•", "200 hp", "50 armor", "IBIS");
        subsystemInfo.addPara(txt("hm_featherDrones_bullet3"), 3, YELLOW, "•", "1.0", "500 max flux", "50 flux/s");
        tooltip.addImageWithText(10f);
    }
}
