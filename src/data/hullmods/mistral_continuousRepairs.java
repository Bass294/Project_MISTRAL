package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.Mistral_ContinuousRepairsListener;

import java.awt.Color;

/**
 * Marker hullmod for Mistral_ContinuousRepairsListener - carries no combat/campaign effects of
 * its own. As long as any non-mothballed ship in the player fleet has this hullmod installed,
 * the listener spends accumulated defeated-enemy deployment points to repair a random d-mod on
 * a random fleet ship. Ported from the ship-repair half of Second-in-Command's
 * ContinuousRepairs skill (second_in_command.skills.starfaring.ContinuousRepairs.kt) -
 * deliberately excludes that skill's DMOD_ACQUIRE_PROB_MOD reduction (the passive "less likely
 * to gain d-mods in the first place" stat bonus); only the active repair mechanic is kept.
 */
public class mistral_continuousRepairs extends BaseHullMod {

    public static final String ID = "mistral_continuousRepairs";
    private static final Color YELLOW = new Color(241, 199, 0);

    public static boolean isPresentInPlayerFleet() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return false;

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            if (member.getVariant() != null && member.getVariant().hasHullMod(ID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Effect", Alignment.MID, 10f);
        tooltip.addPara("%s Every %s deployment points worth of defeated opponents removes a random d-mod from a random %s or %s in the fleet.",
                3f, YELLOW, "•", String.valueOf(Mistral_ContinuousRepairsListener.REQUIRED_DP), "frigate", "destroyer");
        tooltip.addPara("%s Defeated capital ships count for double toward this effect.", 3f, YELLOW, "•", "double");
        tooltip.addPara("%s This can trigger multiple times per battle, and the total carries over between battles.", 3f, YELLOW, "•");
        tooltip.addPara("%s Ignores mothballed ships and ships with the Rugged Construction hullmod.", 3f, YELLOW, "•");
    }

}
