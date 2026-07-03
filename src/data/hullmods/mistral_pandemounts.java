package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.EnumMap;
import java.util.Map;

import static data.scripts.util.Mistral_stringsManager.txt;

public class mistral_pandemounts extends BaseHullMod {
    public static final String BUILT_IN_MOUNT_ID = "mistral_pandemounts";

    protected static final float RECOIL_REDUCTION = -30;
    protected static final float LARGE_DP_REDUCTION = 10f;

    private static final Map<WeaponSize, Integer> DIABLE_WEAPON_OP_REDUCTION_MAP = new EnumMap<>(WeaponSize.class);

    static {
        DIABLE_WEAPON_OP_REDUCTION_MAP.put(WeaponSize.LARGE, 10);
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getRecoilPerShotMult().modifyPercent(id, RECOIL_REDUCTION);
        stats.addListener(new DiableWeaponOPModifier(id));
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) {
            return (int)(-RECOIL_REDUCTION) + txt("%");
        }
        if (index == 1) {
            return (int)(LARGE_DP_REDUCTION) + txt("OP");
        }
        return null;
    }


    public static boolean isDiableWeapon(WeaponSpecAPI weapon) {
        return weapon.getWeaponId().startsWith("diable");
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null) return false;
        return ship.getVariant().hasHullMod(BUILT_IN_MOUNT_ID);
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return ship.getVariant().hasHullMod(BUILT_IN_MOUNT_ID);
    }

    public static class DiableWeaponOPModifier implements WeaponOPCostModifier {
        private final String hullmodId;
        public DiableWeaponOPModifier(String hullmodId) {
            this.hullmodId = hullmodId;
        }

        public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
            if (isDiableWeapon(weapon) && weapon.getSize() == WeaponSize.LARGE) {
                return currCost - DIABLE_WEAPON_OP_REDUCTION_MAP.get(WeaponSize.LARGE);
            }
            return currCost;
        }
    }
}