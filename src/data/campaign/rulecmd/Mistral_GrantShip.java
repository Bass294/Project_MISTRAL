package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddShip;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Mistral_GrantShip <variant id>
 *
 * Builds a fleet member straight from a variant id and adds it to the player fleet, reusing
 * vanilla's own AddShip.addShipGainText for the "Gained <ship>" highlighted confirmation line
 * (same text vanilla's own quest-reward ships use). Unlike vanilla's AddShip, which expects the
 * FleetMemberAPI to already exist in a memory variable, this creates it directly from the
 * variant id so it can be called with a single plain argument from rules.csv.
 */
public class Mistral_GrantShip extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null || params.size() == 0) return false;

		String variantId = params.get(0).getString(memoryMap);
		if (Global.getSettings().getVariant(variantId) == null) return false;

		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
		Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);

		AddShip.addShipGainText(member, dialog.getTextPanel());

		return true;
	}

}
