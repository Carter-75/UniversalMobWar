package mod.universalmobwar.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mod.universalmobwar.data.MobWarData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Helper that translates Witch-specific special ability JSON into runtime behavior.
 */
public final class WitchAbilityHelper {

    private static final float BASE_THROW_SPEED = 0.75f;
    private static final float BASE_ACCURACY = 8.0f;

    private WitchAbilityHelper() {
    }

    public static ThrowStats resolveThrowStats(MobEntity witch, MobWarData data) {
        JsonObject abilities = getAbilitiesSection(witch);
        if (abilities == null) {
            return new ThrowStats(1.0f, BASE_ACCURACY);
        }

        NbtCompound skillData = data.getSkillData();
        int level = skillData.getInt("ability_potion_throw_speed");
        if (level <= 0 || !abilities.has("potion_throw_speed")) {
            return new ThrowStats(1.0f, BASE_ACCURACY);
        }

        JsonArray levels = abilities.getAsJsonArray("potion_throw_speed");
        if (level > levels.size()) {
            return new ThrowStats(1.0f, BASE_ACCURACY);
        }

        JsonObject levelData = levels.get(level - 1).getAsJsonObject();
        float multiplier = levelData.has("throw_speed_multiplier")
            ? levelData.get("throw_speed_multiplier").getAsFloat() : 1.0f;
        float accuracy = levelData.has("accuracy")
            ? levelData.get("accuracy").getAsFloat() : BASE_ACCURACY;
        return new ThrowStats(Math.max(0.5f, multiplier), Math.max(0.1f, accuracy));
    }

    public static ItemStack resolvePotionStack(MobEntity witch, MobWarData data, ItemStack fallback, Random random) {
        JsonObject abilities = getAbilitiesSection(witch);
        if (abilities == null) {
            return fallback.copy();
        }

        int level = data.getSkillData().getInt("ability_extra_potion_bag");
        if (level <= 0 || !abilities.has("extra_potion_bag")) {
            return fallback.copy();
        }

        JsonArray levels = abilities.getAsJsonArray("extra_potion_bag");
        if (level > levels.size()) {
            return fallback.copy();
        }

        JsonObject levelData = levels.get(level - 1).getAsJsonObject();
        if (!levelData.has("unlocked_potions")) {
            return fallback.copy();
        }

        JsonArray unlocked = levelData.getAsJsonArray("unlocked_potions");
        List<ItemStack> abilityPotions = new ArrayList<>();
        for (JsonElement element : unlocked) {
            ItemStack abilityPotion = createAbilityPotion(element.getAsString());
            if (!abilityPotion.isEmpty()) {
                abilityPotions.add(abilityPotion);
            }
        }

        if (abilityPotions.isEmpty()) {
            return fallback.copy();
        }

        double chance = Math.min(0.35 + 0.15 * (level - 1), 0.9);
        if (random.nextDouble() >= chance) {
            return fallback.copy();
        }

        return abilityPotions.get(random.nextInt(abilityPotions.size())).copy();
    }

    public static void configureTrajectory(PotionEntity potion, MobEntity witch, LivingEntity target,
                                           ThrowStats stats, float yawOffsetDegrees) {
        Vec3d aim = target.getEyePos().subtract(witch.getEyePos());
        if (yawOffsetDegrees != 0.0f) {
            float radians = (float) Math.toRadians(yawOffsetDegrees);
            aim = aim.rotateY(radians);
        }

        double magnitude = aim.length();
        potion.setVelocity(aim.x, aim.y + magnitude * 0.2, aim.z,
            BASE_THROW_SPEED * stats.speedMultiplier(), Math.max(0.1f, stats.accuracy()));
    }

    public static void spawnAdditionalShots(ServerWorld world, MobEntity witch, ItemStack potionStack,
                                            LivingEntity target, ThrowStats stats, int extraProjectiles) {
        if (extraProjectiles <= 0) {
            return;
        }

        float spreadDegrees = 6.0f;
        for (int i = 0; i < extraProjectiles; i++) {
            float offset = ((i / 2) + 1) * spreadDegrees * (i % 2 == 0 ? 1 : -1);
            PotionEntity extra = new PotionEntity(world, witch);
            extra.setItem(potionStack.copy());
            configureTrajectory(extra, witch, target, stats, offset);
            world.spawnEntity(extra);
        }
    }

    private static ItemStack createAbilityPotion(String descriptor) {
        PotionDescriptor parsed = PotionDescriptor.from(descriptor);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }

        StatusEffectInstance effectInstance = parsed.toStatusEffect();
        if (effectInstance == null) {
            return ItemStack.EMPTY;
        }

        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        int color = effectInstance.getEffectType().value().getColor();
        potion.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
            Optional.empty(), Optional.of(color), List.of(effectInstance)
        ));
        return potion;
    }

    private static JsonObject getAbilitiesSection(MobEntity witch) {
        JsonObject config = ScalingSystem.getConfigForMob(witch);
        if (config == null || !config.has("tree")) {
            return null;
        }
        JsonObject tree = config.getAsJsonObject("tree");
        if (!tree.has("special_abilities")) {
            return null;
        }
        return tree.getAsJsonObject("special_abilities");
    }

    private static class PotionDescriptor {
        private final String effectKey;
        private final int level;
        private final int durationSeconds;

        private PotionDescriptor(String effectKey, int level, int durationSeconds) {
            this.effectKey = effectKey;
            this.level = level;
            this.durationSeconds = durationSeconds;
        }

        static PotionDescriptor from(String raw) {
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            String value = raw.toLowerCase(Locale.ROOT);
            int durationSeconds = -1;
            if (value.endsWith("s")) {
                int lastUnderscore = value.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String durationPart = value.substring(lastUnderscore + 1, value.length() - 1);
                    if (isNumber(durationPart)) {
                        durationSeconds = Integer.parseInt(durationPart);
                        value = value.substring(0, lastUnderscore);
                    }
                }
            }

            int lastUnderscore = value.lastIndexOf('_');
            int level = 1;
            if (lastUnderscore > 0) {
                String levelPart = value.substring(lastUnderscore + 1);
                if (isNumber(levelPart)) {
                    level = Integer.parseInt(levelPart);
                    value = value.substring(0, lastUnderscore);
                }
            }

            return new PotionDescriptor(value, Math.max(1, level), durationSeconds);
        }

        StatusEffectInstance toStatusEffect() {
            var effect = switch (effectKey) {
                case "instant_damage", "damage" -> StatusEffects.INSTANT_DAMAGE;
                case "instant_health", "healing" -> StatusEffects.INSTANT_HEALTH;
                case "poison" -> StatusEffects.POISON;
                case "wither" -> StatusEffects.WITHER;
                case "weakness" -> StatusEffects.WEAKNESS;
                case "slowness" -> StatusEffects.SLOWNESS;
                default -> null;
            };

            if (effect == null) {
                return null;
            }

            if (effect.equals(StatusEffects.INSTANT_DAMAGE) || effect.equals(StatusEffects.INSTANT_HEALTH)) {
                return new StatusEffectInstance(effect, 1, Math.max(0, level - 1));
            }

            int duration = durationSeconds > 0 ? durationSeconds * 20 : 200;
            return new StatusEffectInstance(effect, duration, Math.max(0, level - 1));
        }
    }

    private static boolean isNumber(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public record ThrowStats(float speedMultiplier, float accuracy) {
    }
}
