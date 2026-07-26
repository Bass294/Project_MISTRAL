package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

/**
 * Flat fleet-wide CR-per-deployment reducer, gated on any non-mothballed ship in the fleet
 * having this hullmod. Loosely inspired by Halo Dynamics Ship Industry's HSI_SparePartProduction
 * (data.hullmods.LogisticsMod.HSISparePartProduction), which reduces CR per deployment by 20%
 * per other hullmod-carrying ship present, capped at 50%, via a custom stacking Buff/
 * TimeoutTracker framework. This is deliberately simpler: a flat 50% whenever the hullmod is
 * present anywhere in the fleet, no stacking, no per-source tracking - via HullModFleetEffect's
 * onFleetSync (same mechanism vanilla's own HighResSensors uses for its fleet-wide bonus),
 * which re-fires whenever fleet composition or hullmods change, so no periodic script is needed.
 */
public class mistral_crReduction extends BaseHullMod implements HullModFleetEffect {

    public static final String ID = "mistral_crReduction";
    private static final float MULT = 0.5f;
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
    public void advanceInCampaign(CampaignFleetAPI fleet) {
        // unused - see withAdvanceInCampaign()
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        boolean present = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            if (member.getVariant() != null && member.getVariant().hasHullMod(ID)) {
                present = true;
                break;
            }
        }

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (present) {
                member.getStats().getCRPerDeploymentPercent().modifyMult(ID, MULT);
            } else {
                member.getStats().getCRPerDeploymentPercent().unmodify(ID);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Effect", Alignment.MID, 10f);
        tooltip.addPara("%s Reduces combat readiness consumed per deployment by %s for every ship in the fleet.",
                3f, YELLOW, "•", "50%");
    }

}
