package me.nanorasmus.nanodev.hexvr.input;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.nanorasmus.nanodev.hexvr.casting.Casting;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyInputsHandler {

    public static final String KEY_CATEGORY = "key.category.hex_vr.hex_vr";
    public static final String KEY_DRAW_SPELL_L_SIMPLE = "key.hex_vr.draw_spell_l_simple";
    public static final String KEY_DRAW_SPELL_R_SIMPLE = "key.hex_vr.draw_spell_r_simple";

    public static final String KEY_DRAW_SPELL_L_ADVANCED = "key.hex_vr.draw_spell_l_advanced";
    public static final String KEY_DRAW_SPELL_R_ADVANCED = "key.hex_vr.draw_spell_r_advanced";


    public static KeyBinding drawSpellKeyLSimple;
    public static KeyBinding drawSpellKeyRSimple;

    public static KeyBinding drawSpellKeyLAdvanced;
    public static KeyBinding drawSpellKeyRAdvanced;


    private static Casting leftHandCastingSimple;
    private static Casting rightHandCastingSimple;

    private static Casting leftHandCastingAdvanced;
    private static Casting rightHandCastingAdvanced;

    public static void registerKeyInputs() {
        leftHandCastingSimple = new Casting(false, true);
        rightHandCastingSimple = new Casting(true, true);

        leftHandCastingAdvanced = new Casting(false, false);
        rightHandCastingAdvanced = new Casting(true, false);

        ClientTickEvent.CLIENT_POST.register(client -> {
            leftHandCastingSimple.tick(client, drawSpellKeyLSimple.isPressed());
            rightHandCastingSimple.tick(client, drawSpellKeyRSimple.isPressed());

            leftHandCastingAdvanced.tick(client, drawSpellKeyLAdvanced.isPressed());
            rightHandCastingAdvanced.tick(client, drawSpellKeyRAdvanced.isPressed());
        });
    }

    public static void register() {
        drawSpellKeyLSimple = new KeyBinding(
                KEY_DRAW_SPELL_L_SIMPLE,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_1,
                KEY_CATEGORY
        );
        KeyMappingRegistry.register(drawSpellKeyLSimple);

        drawSpellKeyRSimple = new KeyBinding(
                KEY_DRAW_SPELL_R_SIMPLE,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_2,
                KEY_CATEGORY
        );
        KeyMappingRegistry.register(drawSpellKeyRSimple);

        drawSpellKeyLAdvanced = new KeyBinding(
                KEY_DRAW_SPELL_L_ADVANCED,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_3,
                KEY_CATEGORY
        );
        KeyMappingRegistry.register(drawSpellKeyLAdvanced);

        drawSpellKeyRAdvanced = new KeyBinding(
                KEY_DRAW_SPELL_R_ADVANCED,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_4,
                KEY_CATEGORY
        );
        KeyMappingRegistry.register(drawSpellKeyRAdvanced);

        registerKeyInputs();
    }

}
