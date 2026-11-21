package dev.pvpmacro;

import dev.pvpmacro.config.AttributeSwapConfig;
import dev.pvpmacro.core.*;
import dev.pvpmacro.macro.spear.AttributeSwapMacro;
import dev.pvpmacro.macro.spear.SlotResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Dependency-free logic tests (run with plain java, see README). They simulate the order of a
 * Minecraft client tick: START hook -> slot sync -> queued attack -> END hook.
 * They verify MY scheduling logic only, NOT real Minecraft/server behaviour.
 */
public final class SelfTest {

    static int failures = 0;
    static int checks = 0;

    static void check(boolean ok, String msg) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + msg);
        }
    }

    static final SlotInfo SPEAR3 = new SlotInfo("minecraft:diamond_spear", false, true, 3);
    static final SlotInfo SPEAR1 = new SlotInfo("minecraft:iron_spear", false, true, 1);
    static final SlotInfo PLAIN_SPEAR = new SlotInfo("minecraft:stone_spear", false, true, 0);
    static final SlotInfo SWORD = new SlotInfo("minecraft:diamond_sword", false, false, 0);
    static final SlotInfo STEAK = new SlotInfo("minecraft:cooked_beef", false, false, 0);
    static final SlotInfo WIND = new SlotInfo("minecraft:wind_charge", false, false, 0);

    static final class FakeEnv implements MacroEnvironment {
        final SlotInfo[] hotbar = new SlotInfo[9];
        int selected;
        int synced;
        int tick;
        boolean attackQueued;
        boolean attackKeyBound = true;
        boolean using;
        int food = 20;
        final List<String> log = new ArrayList<>();
        final List<Integer> attackTicks = new ArrayList<>();

        FakeEnv(int selected) {
            java.util.Arrays.fill(hotbar, SlotInfo.EMPTY);
            this.selected = selected;
            this.synced = selected;
        }

        public int selectedSlot() { return selected; }
        public void selectSlot(int s) { selected = s; }
        public SlotInfo hotbarSlot(int s) { return hotbar[s]; }
        public int foodLevel() { return food; }
        public boolean usingItem() { return using; }
        public boolean queueAttack() {
            if (!attackKeyBound) return false;
            attackQueued = true;
            return true;
        }

        void sync() {
            if (selected != synced) {
                log.add("SLOT " + selected + "@" + tick);
                synced = selected;
            }
        }

        /** vanilla part of the tick: slot sync, then the queued attack (which also syncs first). */
        void vanilla() {
            sync();
            if (attackQueued) {
                sync();
                log.add("ATTACK@" + tick);
                attackTicks.add(tick);
                attackQueued = false;
            }
        }
    }

    static void tick(MacroRunner<MacroEnvironment> r, FakeEnv env) {
        r.onTick(env, Phase.START);
        env.vanilla();
        r.onTick(env, Phase.END);
        env.tick++;
    }

    static FakeEnv standardEnv() {
        FakeEnv e = new FakeEnv(1);       // original slot = 1 (sword)
        e.hotbar[0] = SPEAR3;
        e.hotbar[1] = SWORD;              // slots 2.. are empty
        return e;
    }

    static MacroRunner<MacroEnvironment> runner(List<String> errors) {
        return new MacroRunner<>(errors::add);
    }

    public static void main(String[] args) {
        testSingleJab();
        testHoldRepeatAndRelease();
        testMaxJabs();
        testCancelRestores();
        testNoSpear();
        testUnboundAttackKey();
        testFoodStop();
        testStepCap();
        testResolver();
        testSanitize();
        System.out.println(checks + " checks, " + failures + " failures");
        if (failures > 0) System.exit(1);
    }

    static void testSingleJab() {
        FakeEnv env = standardEnv();
        AttributeSwapConfig cfg = new AttributeSwapConfig();
        cfg.holdToRepeat = false;
        List<String> errors = new ArrayList<>();
        var r = runner(errors);
        check(r.start(env, new AttributeSwapMacro(cfg, () -> false)) == null, "single: starts");
        for (int i = 0; i < 8; i++) tick(r, env);
        // park on slot 2 (empty hand), 2 ticks later: spear 0 + attack, back to 2, finally original 1
        check(env.log.equals(List.of("SLOT 2@0", "SLOT 0@2", "ATTACK@2", "SLOT 2@3", "SLOT 1@4")),
                "single: packet order was " + env.log);
        check(!r.isRunning(), "single: finished");
        check(env.selected == 1, "single: back on original slot");
        check(errors.isEmpty(), "single: no errors " + errors);
    }

    static void testHoldRepeatAndRelease() {
        FakeEnv env = standardEnv();
        AttributeSwapConfig cfg = new AttributeSwapConfig();
        boolean[] held = {true};
        List<String> errors = new ArrayList<>();
        var r = runner(errors);
        check(r.start(env, new AttributeSwapMacro(cfg, () -> held[0])) == null, "hold: starts");
        for (int t = 0; t < 20; t++) {
            if (t == 12) held[0] = false;
            tick(r, env);
        }
        check(env.attackTicks.equals(List.of(2, 7)), "hold: attacks at " + env.attackTicks + " (expected [2, 7])");
        check(!r.isRunning(), "hold: stopped after release");
        check(env.selected == 1, "hold: back on original slot after release");
        check(errors.isEmpty(), "hold: no errors " + errors);
    }

    static void testMaxJabs() {
        FakeEnv env = standardEnv();
        AttributeSwapConfig cfg = new AttributeSwapConfig();
        cfg.maxJabs = 3;
        var r = runner(new ArrayList<>());
        r.start(env, new AttributeSwapMacro(cfg, () -> true));
        for (int t = 0; t < 40; t++) tick(r, env);
        check(env.attackTicks.size() == 3, "maxJabs: got " + env.attackTicks.size() + " attacks");
        check(!r.isRunning() && env.selected == 1, "maxJabs: ended on original slot");
    }

    static void testCancelRestores() {
        FakeEnv env = standardEnv();
        var r = runner(new ArrayList<>());
        r.start(env, new AttributeSwapMacro(new AttributeSwapConfig(), () -> true));
        for (int t = 0; t < 4; t++) tick(r, env);
        check(env.selected == 2, "cancel: currently on swap item");
        r.cancel(env); // what the input guard does (screen opened, focus lost...)
        check(env.selected == 1 && !r.isRunning(), "cancel: restored original slot");
        // idempotent
        r.cancel(env);
        check(env.selected == 1, "cancel: second cancel harmless");
    }

    static void testNoSpear() {
        FakeEnv env = new FakeEnv(0);
        env.hotbar[0] = SWORD;
        var r = runner(new ArrayList<>());
        String err = r.start(env, new AttributeSwapMacro(new AttributeSwapConfig(), () -> true));
        check(err != null && err.contains("Lunge spear"), "noSpear: refused with reason: " + err);
        check(!r.isRunning(), "noSpear: not running");

        // spear without Lunge is refused when requireLunge, accepted otherwise
        FakeEnv env2 = new FakeEnv(0);
        env2.hotbar[0] = PLAIN_SPEAR;
        AttributeSwapConfig cfg = new AttributeSwapConfig();
        check(runner(new ArrayList<>()).start(env2, new AttributeSwapMacro(cfg, () -> true)) != null,
                "noSpear: no-lunge spear refused by default");
        cfg.requireLunge = false;
        check(runner(new ArrayList<>()).start(env2, new AttributeSwapMacro(cfg, () -> true)) == null,
                "noSpear: no-lunge spear allowed when requireLunge=false");
    }

    static void testUnboundAttackKey() {
        FakeEnv env = standardEnv();
        env.attackKeyBound = false;
        List<String> errors = new ArrayList<>();
        var r = runner(errors);
        r.start(env, new AttributeSwapMacro(new AttributeSwapConfig(), () -> true));
        for (int t = 0; t < 6; t++) tick(r, env);
        check(!r.isRunning(), "unbound: macro aborted");
        check(errors.size() == 1 && errors.get(0).contains("unbound"), "unbound: error reported " + errors);
        check(env.selected == 1, "unbound: original slot restored");
        check(env.attackTicks.isEmpty(), "unbound: no attack");
    }

    static void testFoodStop() {
        FakeEnv env = standardEnv();
        AttributeSwapConfig cfg = new AttributeSwapConfig();
        cfg.minFoodLevel = 6;
        env.food = 5;
        check(runner(new ArrayList<>()).start(env, new AttributeSwapMacro(cfg, () -> true)) != null,
                "food: refuses to start when below minimum");
        env.food = 20;
        var r = runner(new ArrayList<>());
        r.start(env, new AttributeSwapMacro(cfg, () -> true));
        for (int t = 0; t < 40; t++) {
            if (t == 9) env.food = 4;
            tick(r, env);
        }
        check(!r.isRunning() && env.selected == 1, "food: stops and restores when food drops");
        check(env.attackTicks.equals(List.of(2, 7)), "food: attacks before drop " + env.attackTicks);
    }

    static void testStepCap() {
        int[] count = {0};
        Macro<Object> spam = new Macro<>() {
            public String id() { return "spam"; }
            public String prepare(Object c) { return null; }
            public List<MacroStep<Object>> nextCycle(Object c, int i) {
                return List.of(new MacroStep<>(Phase.END, 0, "x", o -> count[0]++));
            }
        };
        MacroRunner<Object> r = new MacroRunner<>(s -> { });
        r.start(new Object(), spam);
        r.onTick(new Object(), Phase.END);
        check(count[0] <= MacroRunner.MAX_STEPS_PER_PASS, "cap: steps per pass limited, ran " + count[0]);
        check(r.isRunning(), "cap: runner still alive (no hang)");

        // only one macro at a time
        check(r.start(new Object(), spam) != null, "cap: second macro refused while one runs");
    }

    static void testResolver() {
        FakeEnv env = new FakeEnv(0);
        env.hotbar[0] = SPEAR1;
        env.hotbar[3] = SPEAR3;
        env.hotbar[4] = SPEAR3;
        env.hotbar[5] = WIND;
        env.hotbar[6] = STEAK;
        AttributeSwapConfig cfg = new AttributeSwapConfig();
        check(SlotResolver.findSpear(env, cfg).equals(OptionalInt.of(3)), "resolver: picks highest Lunge, lowest slot on tie");
        cfg.spearSlot = 1;
        check(SlotResolver.findSpear(env, cfg).equals(OptionalInt.of(0)), "resolver: fixed spear slot");
        cfg.spearSlot = 3; // slot 3 (1-based) is empty
        check(SlotResolver.findSpear(env, cfg).isEmpty(), "resolver: fixed slot without spear -> empty");
        cfg.spearSlot = 0;

        check(SlotResolver.findCarrier(env, cfg, 3).equals(OptionalInt.of(1)), "resolver: empty hand first by default");
        cfg.carrierItems = new ArrayList<>(List.of("minecraft:cooked_beef", "minecraft:wind_charge"));
        check(SlotResolver.findCarrier(env, cfg, 3).equals(OptionalInt.of(6)), "resolver: item priority order");
        cfg.carrierItems = new ArrayList<>(List.of("minecraft:wind_charge"));
        check(SlotResolver.findCarrier(env, cfg, 3).equals(OptionalInt.of(5)), "resolver: wind charge");
        cfg.carrierItems = new ArrayList<>(List.of("minecraft:golden_carrot"));
        check(SlotResolver.findCarrier(env, cfg, 3).isEmpty(), "resolver: no matching carrier -> empty");
        cfg.carrierSlot = 4; // fixed, equals spear slot index 3
        check(SlotResolver.findCarrier(env, cfg, 3).isEmpty(), "resolver: fixed carrier == spear refused");
        cfg.carrierSlot = 9;
        check(SlotResolver.findCarrier(env, cfg, 3).equals(OptionalInt.of(8)), "resolver: fixed carrier slot");
    }

    static void testSanitize() {
        AttributeSwapConfig c = new AttributeSwapConfig();
        c.repeatIntervalTicks = 0;
        c.parkTicks = -5;
        c.restoreDelayTicks = 0;
        c.spearSlot = 99;
        c.maxJabs = -1;
        c.carrierItems = new ArrayList<>(List.of("  Steak ", "", "minecraft:Wind_Charge"));
        c.sanitize();
        check(c.repeatIntervalTicks == 2, "sanitize: interval min 2");
        check(c.parkTicks == 1 && c.restoreDelayTicks == 1, "sanitize: park/restore min 1");
        check(c.spearSlot == 9 && c.maxJabs == 0, "sanitize: slot/maxJabs clamped");
        check(c.carrierItems.equals(List.of("minecraft:steak", "minecraft:wind_charge")),
                "sanitize: ids normalized " + c.carrierItems);
        c.carrierItems = null;
        c.sanitize();
        check(!c.carrierItems.isEmpty(), "sanitize: null list -> defaults");
    }
}
