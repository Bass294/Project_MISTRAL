package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.graid.ShipWeaponsGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.campaign.ui.trade.CargoItemStack;
import data.hullmods.mistral_zeroWeaponStorage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Java port of Second-in-Command's CompactStorageScript (second_in_command.skills.engineering.
 * scripts.CompactStorageScript), reduced to a single behavior: while any non-mothballed ship
 * in the player fleet has the mistral_zeroWeaponStorage hullmod installed, every weapon stack
 * in fleet cargo takes up 0 storage space instead of its normal size-based cost. When no ship
 * with the hullmod is present, weapon stacks are restored to the vanilla per-size cost
 * (ShipWeaponsGroundRaidObjectivePluginImpl.CARGO_SPACE_PER_SMALL/MEDIUM/LARGE), same as the
 * reference script's "inactive" branch.
 *
 * CargoStackAPI has no public setter for cargoSpacePerUnit, so - same as the reference script -
 * this reaches into the concrete CargoItemStack implementation via reflection to overwrite the
 * private field directly. Starsector's mod-script classloader throws a SecurityException on any
 * direct reference to java.lang.reflect.* (confirmed at runtime: "File access and reflection are
 * not allowed to scripts"), so - same workaround Second-in-Command's ReflectionUtils uses - the
 * Field class is resolved via Class.forName() through java.lang.Class's own (unrestricted)
 * classloader instead of this class's, and accessed purely through java.lang.invoke.MethodHandle,
 * which isn't restricted. No java.lang.reflect type is ever referenced directly in source.
 *
 * CompactStorageScript runs unconditionally as soon as Second-in-Command is loaded - even with
 * its skill never picked, its "inactive" branch keeps resetting cargoSpacePerUnit back to the
 * vanilla per-size baseline on its own timer, with no awareness of any other mod touching the
 * same field. Rather than racing it, this script just bails out entirely when Second-in-Command
 * is enabled, leaving that field alone so there's nothing to fight over.
 */
public class Mistral_WeaponStorageScript implements EveryFrameScript {

    private static final String SIC_MOD_ID = "second_in_command";

    private static final MethodHandle SET_ACCESSIBLE;
    private static final MethodHandle SET_FLOAT;
    private static final Object CARGO_SPACE_FIELD;

    static {
        try {
            Class<?> fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());

            MethodHandle getDeclaredField = MethodHandles.lookup().findVirtual(
                    Class.class, "getDeclaredField", MethodType.methodType(fieldClass, String.class));
            SET_ACCESSIBLE = MethodHandles.lookup().findVirtual(
                    fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            SET_FLOAT = MethodHandles.lookup().findVirtual(
                    fieldClass, "setFloat", MethodType.methodType(void.class, Object.class, float.class));

            CARGO_SPACE_FIELD = getDeclaredField.invoke(CargoItemStack.class, "cargoSpacePerUnit");
            SET_ACCESSIBLE.invoke(CARGO_SPACE_FIELD, true);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set up reflective access to CargoItemStack.cargoSpacePerUnit", t);
        }
    }

    private final IntervalUtil interval = new IntervalUtil(0.075f, 0.1f);

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSettings().getModManager().isModEnabled(SIC_MOD_ID)) return;

        interval.advance(amount);
        if (interval.intervalElapsed()) {
            modifyCargo(Global.getSector().getPlayerFleet().getCargo());
        }
    }

    private void modifyCargo(CargoAPI cargo) {
        boolean active = mistral_zeroWeaponStorage.isPresentInPlayerFleet();
        boolean changedSize = false;

        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (stack.getType() != CargoAPI.CargoItemType.WEAPONS) continue;
            if (!(stack instanceof CargoItemStack)) continue;

            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec((String) stack.getData());
            float space;
            if (active) {
                space = 0f;
            } else {
                switch (spec.getSize()) {
                    case SMALL:
                        space = ShipWeaponsGroundRaidObjectivePluginImpl.CARGO_SPACE_PER_SMALL;
                        break;
                    case MEDIUM:
                        space = ShipWeaponsGroundRaidObjectivePluginImpl.CARGO_SPACE_PER_MEDIUM;
                        break;
                    case LARGE:
                        space = ShipWeaponsGroundRaidObjectivePluginImpl.CARGO_SPACE_PER_LARGE;
                        break;
                    default:
                        space = ShipWeaponsGroundRaidObjectivePluginImpl.CARGO_SPACE_PER_SMALL;
                        break;
                }
            }

            if (space != stack.getCargoSpacePerUnit()) {
                changedSize = true;
                setCargoSpacePerUnit((CargoItemStack) stack, space);
            }
        }

        if (changedSize) {
            cargo.updateSpaceUsed();
        }
    }

    private static void setCargoSpacePerUnit(CargoItemStack stack, float value) {
        try {
            SET_FLOAT.invoke(CARGO_SPACE_FIELD, stack, value);
        } catch (Throwable t) {
            Global.getLogger(Mistral_WeaponStorageScript.class).error("Failed to set cargoSpacePerUnit via reflection", t);
        }
    }

}
