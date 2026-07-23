package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Mistral_ShowOfficerSkills <person id>
 *
 * Applies the officer's level, elite skill, and personality, then prints the resulting skill
 * panel into the dialog text - the same way vanilla's officer recruitment dialog does when
 * asked "what can you do?" (OfficerManagerEvent's "printSkills" action, which calls this same
 * TextPanelAPI.addSkillPanel method). The stats have to be set here rather than only in
 * Mistral_RecruitRamiel, since this command runs first (when the offer is shown) and the panel
 * needs to reflect them immediately - if they were only set on acceptance, the panel would show
 * her as a blank level-0 officer with no skills.
 */
public class Mistral_ShowOfficerSkills extends BaseCommandPlugin {

	private static final String SKILL_ID = "mistral_ACE";
	private static final float ELITE_LEVEL = 2f;
	private static final int OFFICER_LEVEL = 1;

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null || params.size() == 0) return false;

		String personId = params.get(0).getString(memoryMap);
		PersonDataAPI data = Global.getSector().getImportantPeople().getData(personId);
		if (data == null) return false;

		PersonAPI person = data.getPerson();
		if (person == null) return false;

		person.getStats().setLevel(OFFICER_LEVEL);
		person.getStats().setSkillLevel(SKILL_ID, ELITE_LEVEL);
		person.setPersonality(Personalities.AGGRESSIVE);

		TextPanelAPI text = dialog.getTextPanel();
		text.addSkillPanel(person, false);

		return true;
	}

}
