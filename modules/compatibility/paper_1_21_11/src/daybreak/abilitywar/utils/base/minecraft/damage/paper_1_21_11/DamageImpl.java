package daybreak.abilitywar.utils.base.minecraft.damage.paper_1_21_11;

import daybreak.abilitywar.utils.base.minecraft.damage.IDamages;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DamageImpl implements IDamages {
    private DamageSource bypassArmor(DamageSource source) {
        return new DamageSource(source.typeHolder(), source.getDirectEntity(), source.getEntity()) {
            @Override public boolean is(TagKey<DamageType> tag) {
                return tag.equals(DamageTypeTags.BYPASSES_ARMOR) || super.is(tag);
            }
        };
    }
    private boolean hurt(Entity entity, DamageSource source, float damage) {
        var handle = ((CraftEntity) entity).getHandle();
        return handle.hurtServer((net.minecraft.server.level.ServerLevel) handle.level(), source, damage);
    }
    @Override public boolean damageExplosion(Entity entity, Entity source, float damage) {
        var handle = ((CraftEntity) entity).getHandle();
        var attacker = ((CraftEntity) source).getHandle();
        return hurt(entity, handle.damageSources().explosion(attacker, attacker), damage);
    }
    @Override public boolean damageThorn(Entity entity, LivingEntity damager, float damage) {
        return hurt(entity, ((CraftEntity) entity).getHandle().damageSources().thorns(((CraftLivingEntity) damager).getHandle()), damage);
    }
    @Override public boolean damageArrow(Entity entity, LivingEntity shooter, float damage) {
        var handle = ((CraftEntity) entity).getHandle();
        return hurt(entity, handle.damageSources().arrow(new Arrow(EntityType.ARROW, handle.level()), ((CraftLivingEntity) shooter).getHandle()), damage);
    }
    @Override public boolean damageFixed(Entity entity, LivingEntity damager, float damage) {
        var handle = ((CraftEntity) entity).getHandle();
        var attacker = ((CraftLivingEntity) damager).getHandle();
        var source = damager instanceof Player ? handle.damageSources().playerAttack((net.minecraft.world.entity.player.Player) attacker)
                : handle.damageSources().mobAttack(attacker);
        return hurt(entity, bypassArmor(source), damage);
    }
    @Override public boolean damageMagic(Entity entity, Player damager, boolean ignoreArmor, float damage) {
        var handle = ((CraftEntity) entity).getHandle();
        var type = handle.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC);
        var source = new DamageSource(type, damager == null ? null : ((CraftEntity) damager).getHandle());
        return hurt(entity, ignoreArmor ? bypassArmor(source) : source, damage);
    }
}
