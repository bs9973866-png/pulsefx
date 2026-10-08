package dev.pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** All particle-based visuals. Purely client-side, no gameplay changes. */
public final class Effects {
    public static long tick = 0;
    public static LivingEntity lastTarget;
    public static long lastHitTick = -1000;

    private Effects() { }

    // ---------------- helpers ----------------
    private static void dust(World w, double x, double y, double z, int rgb, float size) {
        w.addParticle(new DustParticleEffect(rgb & 0xFFFFFF, size), x, y, z, 0, 0, 0);
    }

    private static void ring(World w, double cx, double cy, double cz, double r, int n, int rgb, float size, double phase) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + phase;
            dust(w, cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r, rgb, size);
        }
    }

    /** Target for Target ESP / Target HUD. */
    public static LivingEntity currentTarget(MinecraftClient mc, boolean combatOnly) {
        boolean recent = lastTarget != null && lastTarget.isAlive() && tick - lastHitTick < 120;
        if (combatOnly) return recent ? lastTarget : null;
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) return le;
        return recent ? lastTarget : null;
    }

    // ---------------- events ----------------
    public static void onHit(LivingEntity e) {
        lastTarget = e;
        lastHitTick = tick;
        Feature m = Modules.get("hit_bubbles");
        if (m.enabled) {
            World w = e.getWorld();
            double x = e.getX(), y = e.getBodyY(0.6), z = e.getZ();
            var rnd = w.random;
            int n = (int) m.n("count");
            for (int i = 0; i < n; i++) {
                int col = m.color("color", i / (float) n);
                double a = rnd.nextDouble() * Math.PI * 2;
                double s = 0.12 + rnd.nextDouble() * 0.12;
                w.addParticle(new DustParticleEffect(col, 1.2f), x, y, z,
                        Math.cos(a) * s, 0.08 + rnd.nextDouble() * 0.1, Math.sin(a) * s);
            }
        }
    }

    public static void tick(MinecraftClient mc) {
        tick++;
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;
        if (Modules.on("auto_respawn") && p.isDead()) p.requestRespawn();
        if (mc.isPaused()) return;

        hitRange(w, p);
        targetEsp(mc, w, p);
        trails(w, p);
        worldParticles(w, p);
        prediction(w, p);
        chinaHat(mc, w, p);
        killCheck(w);
    }

    // ---------------- features ----------------
    private static void hitRange(ClientWorld w, ClientPlayerEntity p) {
        Feature m = Modules.get("hit_range");
        if (!m.enabled || tick % 2 != 0) return;
        ring(w, p.getX(), p.getY() + 0.05, p.getZ(), m.n("radius"), (int) m.n("density"), m.color("color", 0f), 0.8f, 0);
    }

    private static void targetEsp(MinecraftClient mc, ClientWorld w, ClientPlayerEntity p) {
        Feature m = Modules.get("target_esp");
        if (!m.enabled || tick % 2 != 0) return;
        LivingEntity t = currentTarget(mc, m.b("combat"));
        if (t == null || t == p) return;
        double cx = t.getX(), cz = t.getZ();
        double r = t.getWidth() * 0.8 + 0.25;
        float size = 0.5f + m.n("thickness") * 0.25f;
        int c1 = m.color("color", 0f), c2 = m.color("color2", 0.3f);
        switch (m.m("mode")) {
            case 0 -> {
                double k = (Math.sin(tick * 0.08) + 1) / 2;
                ring(w, cx, t.getY() + k * t.getHeight(), cz, r, 14, c1, size, tick * 0.1);
                ring(w, cx, t.getY() + (1 - k) * t.getHeight(), cz, r, 14, c2, size, -tick * 0.1);
            }
            case 1 -> ring(w, cx, t.getY() + 0.05, cz, r, 18, c1, size, tick * 0.05);
            default -> {
                for (int j = 0; j < 3; j++) {
                    double a = tick * 0.25 + j * 2.1;
                    double y = t.getY() + ((tick * 0.04 + j * 0.33) % 1.0) * t.getHeight();
                    dust(w, cx + Math.cos(a) * r, y, cz + Math.sin(a) * r, j % 2 == 0 ? c1 : c2, size);
                }
            }
        }
    }

    private static void trails(ClientWorld w, ClientPlayerEntity p) {
        Feature m = Modules.get("trails");
        if (!m.enabled) return;
        Vec3d v = p.getVelocity();
        if (v.x * v.x + v.z * v.z < 0.003) return;
        float size = m.n("size");
        int c1 = m.color("color", 0f), c2 = m.color("color2", 0.5f);
        float k = (float) ((Math.sin(tick * 0.06) + 1) / 2);
        int col = m.b("gradient") ? PulseColors.lerp(c1, c2, k) : c1;
        var rnd = w.random;
        switch (m.m("mode")) {
            case 0 -> dust(w, p.getX() + (rnd.nextDouble() - .5) * .4, p.getY() + 0.1,
                    p.getZ() + (rnd.nextDouble() - .5) * .4, col, size);
            case 1 -> {
                for (int j = 0; j < 2; j++) {
                    double a = tick * 0.5 + j * Math.PI;
                    dust(w, p.getX() + Math.cos(a) * 0.45, p.getY() + 0.1 + ((tick % 20) / 20.0) * 1.2,
                            p.getZ() + Math.sin(a) * 0.45, col, size);
                }
            }
            default -> {
                dust(w, p.getX(), p.getY() + 0.1, p.getZ(), col, size);
                w.addParticle(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.1, p.getZ(),
                        (rnd.nextDouble() - .5) * .04, 0.03, (rnd.nextDouble() - .5) * .04);
            }
        }
    }

    private static void worldParticles(ClientWorld w, ClientPlayerEntity p) {
        Feature m = Modules.get("world_particles");
        if (!m.enabled) return;
        int d = (int) m.n("density");
        var rnd = w.random;
        double px = p.getX(), py = p.getY(), pz = p.getZ();
        switch (m.m("mode")) {
            case 0 -> {
                for (int i = 0; i < d / 2 + 1; i++) {
                    w.addParticle(ParticleTypes.END_ROD, px + (rnd.nextDouble() - .5) * 20, py + rnd.nextDouble() * 8,
                            pz + (rnd.nextDouble() - .5) * 20, 0, -0.02, 0);
                }
            }
            case 1 -> {
                if (tick % 2 == 0) {
                    double r = 1 + ((tick % 60) / 60.0) * 9;
                    ring(w, px, py + 0.1, pz, r, (int) (r * 6) + 8, m.color("color", (float) (r / 10)), 0.9f, 0);
                }
            }
            case 2 -> {
                for (int j = 0; j < 2; j++) {
                    double a = tick * 0.2 + j * Math.PI;
                    double h = ((tick * 0.05) % 1.0) * 3;
                    dust(w, px + Math.cos(a) * 1.5, py + h, pz + Math.sin(a) * 1.5, m.color("color", (float) h / 3), 1f);
                }
            }
            default -> {
                if (tick % 3 == 0) {
                    for (int i = 0; i < d / 3 + 1; i++) {
                        double bx = px + (rnd.nextDouble() - .5) * 20, bz = pz + (rnd.nextDouble() - .5) * 20;
                        int col = m.color("color", (float) rnd.nextDouble());
                        for (int h = 0; h < 8; h++) dust(w, bx, py + h * 0.5, bz, col, 1f);
                    }
                }
            }
        }
    }

    private static void prediction(ClientWorld w, ClientPlayerEntity p) {
        Feature m = Modules.get("prediction");
        if (!m.enabled || tick % 3 != 0) return;
        int steps = (int) m.n("steps");
        int col = m.color("color", 0f);
        for (Entity e : w.getEntities()) {
            if (!(e instanceof ProjectileEntity pe) || pe.getOwner() != p) continue;
            Vec3d vel = pe.getVelocity();
            if (vel.lengthSquared() < 0.01) continue;
            double g = pe instanceof PersistentProjectileEntity ? 0.05 : 0.03;
            Vec3d pos = pe.getPos();
            for (int s = 0; s < steps; s++) {
                pos = pos.add(vel);
                BlockPos bp = BlockPos.ofFloored(pos);
                if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) break;
                vel = vel.multiply(0.99).add(0, -g, 0);
                if (s % 2 == 0) dust(w, pos.x, pos.y, pos.z, col, 0.7f);
            }
        }
    }

    private static void chinaHat(MinecraftClient mc, ClientWorld w, ClientPlayerEntity p) {
        Feature m = Modules.get("china_hat");
        if (!m.enabled || tick % 2 != 0 || mc.options.getPerspective().isFirstPerson()) return;
        double size = m.n("size");
        double y = p.getY() + p.getHeight() + 0.05;
        for (int i = 0; i < 3; i++) {
            ring(w, p.getX(), y + i * 0.1, p.getZ(), size * (1 - i * 0.3), 10, m.color("color", i * 0.1f), 0.7f, 0);
        }
    }

    private static void killCheck(ClientWorld w) {
        if (lastTarget == null) return;
        if (tick - lastHitTick > 200) { lastTarget = null; return; }
        if (lastTarget.isDead() || lastTarget.getHealth() <= 0f) {
            if (Modules.on("kill_effect")) killFx(w, lastTarget);
            lastTarget = null;
        }
    }

    private static void killFx(ClientWorld w, LivingEntity e) {
        Feature m = Modules.get("kill_effect");
        double x = e.getX(), y = e.getY(), z = e.getZ();
        var rnd = w.random;
        switch (m.m("mode")) {
            case 0 -> {
                for (int i = 0; i < 40; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, s = 0.1 + rnd.nextDouble() * 0.2;
                    w.addParticle(new DustParticleEffect(m.color("color", i / 40f), 1.3f), x, y + e.getHeight() / 2, z,
                            Math.cos(a) * s, rnd.nextDouble() * 0.2, Math.sin(a) * s);
                }
            }
            case 1 -> {
                for (int j = 0; j < 5; j++) ring(w, x, y + j * 0.4, z, 0.6 + j * 0.25, 16, m.color("color", j * 0.15f), 1f, 0);
            }
            default -> {
                for (int i = 0; i < 48; i++) {
                    double a = i * 0.5;
                    dust(w, x + Math.cos(a) * 0.7, y + i * 0.04, z + Math.sin(a) * 0.7, m.color("color", i / 48f), 1f);
                }
            }
        }
        if (Modules.on("star_glow")) {
            int n = (int) Modules.get("star_glow").n("amount");
            for (int i = 0; i < n; i++) {
                w.addParticle(ParticleTypes.END_ROD, x, y + e.getHeight() / 2, z,
                        (rnd.nextDouble() - .5) * 0.5, rnd.nextDouble() * 0.4, (rnd.nextDouble() - .5) * 0.5);
            }
            for (int i = 0; i < 8; i++) {
                w.addParticle(ParticleTypes.FIREWORK, x, y + e.getHeight() / 2, z,
                        (rnd.nextDouble() - .5) * 0.3, rnd.nextDouble() * 0.3, (rnd.nextDouble() - .5) * 0.3);
            }
        }
    }
}
