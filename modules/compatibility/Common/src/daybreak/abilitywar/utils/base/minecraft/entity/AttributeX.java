package daybreak.abilitywar.utils.base.minecraft.entity;

import org.bukkit.attribute.Attribute;

public final class AttributeX {
    public static final Attribute GENERIC_MAX_HEALTH = resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
    public static final Attribute GENERIC_MOVEMENT_SPEED = resolve("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
    public static final Attribute GENERIC_ARMOR = resolve("GENERIC_ARMOR", "ARMOR");
    public static final Attribute GENERIC_ARMOR_TOUGHNESS = resolve("GENERIC_ARMOR_TOUGHNESS", "ARMOR_TOUGHNESS");
    public static final Attribute GENERIC_ATTACK_DAMAGE = resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
    public static final Attribute GENERIC_ATTACK_SPEED = resolve("GENERIC_ATTACK_SPEED", "ATTACK_SPEED");
    public static final Attribute GENERIC_KNOCKBACK_RESISTANCE = resolve("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE");

    private AttributeX() {}
    private static Attribute resolve(String legacy, String modern) {
        try {
            try { return (Attribute) Attribute.class.getField(legacy).get(null); }
            catch (NoSuchFieldException ignored) { return (Attribute) Attribute.class.getField(modern).get(null); }
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
}
