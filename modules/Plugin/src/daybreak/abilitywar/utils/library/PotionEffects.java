package daybreak.abilitywar.utils.library;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 효과 라이브러리
 *
 * @author Daybreak 새벽
 * @version 1.4
 * @since 2019-02-19
 */
public enum PotionEffects {

	ABSORPTION(true),
	BAD_OMEN(false),
	BLINDNESS(false),
	CONFUSION(false),
	CONDUIT_POWER(true),
	DOLPHINS_GRACE(true),
	DAMAGE_RESISTANCE(true),
	FAST_DIGGING(true),
	FIRE_RESISTANCE(true),
	GLOWING(false),
	HARM(false),
	HEAL(true),
	HEALTH_BOOST(true),
	HUNGER(false),
	HERO_OF_THE_VILLAGE(true),
	INCREASE_DAMAGE(true),
	INVISIBILITY(true),
	JUMP(true),
	LEVITATION(false),
	LUCK(true),
	NIGHT_VISION(true),
	POISON(false),
	REGENERATION(true),
	SATURATION(true),
	SLOW(false),
	SLOW_DIGGING(false),
	SLOW_FALLING(true),
	SPEED(true),
	UNLUCK(false),
	WATER_BREATHING(true),
	WEAKNESS(false),
	WITHER(false);

	final PotionEffectType effect;
	final boolean positive;

	PotionEffects(boolean positive) {
		PotionEffectType type = PotionEffectType.getByName(name());
		if (type == null) {
			String modern;
			switch (name()) {
				case "CONFUSION": modern = "NAUSEA"; break;
				case "DAMAGE_RESISTANCE": modern = "RESISTANCE"; break;
				case "FAST_DIGGING": modern = "HASTE"; break;
				case "HARM": modern = "INSTANT_DAMAGE"; break;
				case "HEAL": modern = "INSTANT_HEALTH"; break;
				case "INCREASE_DAMAGE": modern = "STRENGTH"; break;
				case "JUMP": modern = "JUMP_BOOST"; break;
				case "SLOW": modern = "SLOWNESS"; break;
				case "SLOW_DIGGING": modern = "MINING_FATIGUE"; break;
				default: modern = name();
			}
			type = PotionEffectType.getByName(modern);
		}
		this.effect = type;
		this.positive = positive;
	}

	public PotionEffectType getEffectType() { return effect; }

	public void addPotionEffect(LivingEntity entity, int duration, int amplifier, boolean ambient, boolean particles, boolean force) {
		if (effect != null) {
			if (ServerVersion.getVersion() <= 14) {
				entity.addPotionEffect(new PotionEffect(effect, duration, amplifier, ambient, particles), force);
			} else {
				entity.addPotionEffect(new PotionEffect(effect, duration, amplifier, ambient, particles));
			}
		}
	}

	public void addPotionEffect(LivingEntity entity, int duration, int amplifier, boolean force) {
		if (effect != null) {
			if (ServerVersion.getVersion() <= 14) {
				entity.addPotionEffect(new PotionEffect(effect, duration, amplifier), force);
			} else {
				entity.addPotionEffect(new PotionEffect(effect, duration, amplifier));
			}
		}
	}

	public void removePotionEffect(LivingEntity entity) {
		if (effect != null) {
			entity.removePotionEffect(effect);
		}
	}

	public boolean isPositive() {
		return positive;
	}

	public boolean isNegative() {
		return !positive;
	}

	public static Optional<PotionEffects> valueOf(PotionEffectType potionEffectType) {
		for (PotionEffects effect : values()) {
			if (potionEffectType.equals(effect.effect)) return Optional.of(effect);
		}
		return Optional.absent();
	}

}
