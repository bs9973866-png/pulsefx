package dev.pulsefx;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

/** Client-side cosmetics only. Never changes gameplay. */
public class PulseFxClient implements ClientModInitializer {
    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        Modules.init();
        Store.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pulsefx.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.pulsefx"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Modules.tickAll(mc);
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new PulseScreen(null));
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hr) -> {
            if (world.isClient && entity instanceof LivingEntity e && e.isAlive()) Modules.onAttack(e);
            return ActionResult.PASS; // never cancel or alter the attack
        });

        HudRenderCallback.EVENT.register(Modules::hudAll);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
                ClientCommandManager.literal("pulsefx")
                        .then(ClientCommandManager.literal("friend")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                .executes(c -> {
                                                    String n = StringArgumentType.getString(c, "name");
                                                    Store.addFriend(n);
                                                    c.getSource().sendFeedback(Text.literal("[PulseFX] Друг добавлен: " + n));
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                                .executes(c -> {
                                                    String n = StringArgumentType.getString(c, "name");
                                                    Store.removeFriend(n);
                                                    c.getSource().sendFeedback(Text.literal("[PulseFX] Друг удалён: " + n));
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("list")
                                        .executes(c -> {
                                            c.getSource().sendFeedback(Text.literal("[PulseFX] Друзья: " + String.join(", ", Store.data.friends)));
                                            return 1;
                                        })))));
    }
}
