package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Mistral_RecruitRamiel <person id>
 *
 * Adds the named person to the player fleet's officer roster. Level/skill/personality are
 * already applied by Mistral_ShowOfficerSkills, which always runs first when the recruitment
 * offer is shown - this command only performs the actual join. Deliberately does not touch
 * ContactIntel or the comm directory - joining as an officer and being a contact are separate
 * systems, and nothing here should remove Ramiel from either. Also plays the same positive UI
 * beat vanilla uses for a comparable milestone (developing a contact) and prints the same
 * highlighted "has joined your fleet" line vanilla's own officer-hire flow uses
 * (AddRemoveCommodity.addOfficerGainText, from OfficerManagerEvent's hireOfficer action).
 */
public class Mistral_RecruitRamiel extends BaseCommandPlugin {

	private static final String JOIN_SOUND = "ui_contact_developed";

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (params.size() == 0) return false;

		String personId = params.get(0).getString(memoryMap);
		PersonDataAPI data = Global.getSector().getImportantPeople().getData(personId);
		if (data == null) return false;

		PersonAPI person = data.getPerson();
		if (person == null) return false;

		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		playerFleet.getFleetData().addOfficer(person);

		Global.getSoundPlayer().playUISound(JOIN_SOUND, 1f, 1f);
		if (dialog != null) {
			AddRemoveCommodity.addOfficerGainText(person, dialog.getTextPanel());
		}

		return true;
	}

}
