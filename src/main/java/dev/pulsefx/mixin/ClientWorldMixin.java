package dev.pulsefx.mixin;

import dev.pulsefx.Modules;
import dev.pulsefx.PulseColors;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only mixin: a tail-of-method tweak of the sky colour (World Customizer).
 * It never cancels other mods' injections and silently does nothing if the target is not found.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void pulsefx$sky(CallbackInfoReturnable<Object> cir) {
        if (!Modules.WORLD.enabled) return;
        if (((ClientWorld) (Object) this).getRegistryKey() != World.OVERWORLD) return;
        Object original = cir.getReturnValue();
        float k;
        if (original instanceof Integer i) {
            k = Math.max((i >> 16) & 255, Math.max((i >> 8) & 255, i & 255)) / 255f;
        } else if (original instanceof Vec3d v) {
            k = (float) Math.max(v.x, Math.max(v.y, v.z));
        } else {
            return;
        }
        k = 0.25f + 0.75f * Math.min(1f, k); // keep day/night dimming
        int rgb = PulseColors.sky();
        int r = (int) (((rgb >> 16) & 255) * k), g = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        if (original instanceof Integer) {
            cir.setReturnValue(0xFF000000 | (r << 16) | (g << 8) | b);
        } else {
            cir.setReturnValue(new Vec3d(r / 255.0, g / 255.0, b / 255.0));
        }
    }
}
