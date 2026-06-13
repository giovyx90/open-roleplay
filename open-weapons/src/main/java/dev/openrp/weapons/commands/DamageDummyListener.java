package dev.openrp.weapons.commands;

import it.meridian.core.CorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class DamageDummyListener implements Listener {
   private final CorePlugin core;
   private final NamespacedKey dummyKey;

   public DamageDummyListener(CorePlugin core) {
      this.core = core;
      this.dummyKey = new NamespacedKey(core, "weapon_damage_dummy");
   }

   public NamespacedKey dummyKey() {
      return dummyKey;
   }

   public void mark(Zombie dummy, double maxHealth) {
      dummy.getPersistentDataContainer().set(dummyKey, PersistentDataType.BYTE, (byte) 1);
      if (dummy.getAttribute(Attribute.MAX_HEALTH) != null) {
         dummy.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
      }
      dummy.setHealth(Math.min(maxHealth, dummy.getAttribute(Attribute.MAX_HEALTH).getValue()));
      dummy.setAI(false);
      dummy.setSilent(true);
      dummy.setRemoveWhenFarAway(false);
      dummy.setCanPickupItems(false);
      dummy.customName(Component.text("Bersaglio danno " + formatHealth(dummy), NamedTextColor.RED));
      dummy.setCustomNameVisible(true);
   }

   @EventHandler
   public void onDummyDamage(EntityDamageEvent event) {
      if (!(event.getEntity() instanceof LivingEntity living) || !isDummy(living)) {
         return;
      }
      double before = living.getHealth();
      core.getServer().getScheduler().runTask(core, () -> {
         if (!living.isValid() || living.isDead()) {
            return;
         }
         double after = living.getHealth();
         living.customName(Component.text("Bersaglio danno " + formatHealth(living)
               + " -" + String.format(java.util.Locale.US, "%.1f", Math.max(0.0D, before - after)),
               NamedTextColor.RED));
      });
   }

   @EventHandler
   public void onDummyDeath(EntityDeathEvent event) {
      if (isDummy(event.getEntity())) {
         event.getDrops().clear();
         event.setDroppedExp(0);
      }
   }

   private boolean isDummy(LivingEntity entity) {
      return entity.getPersistentDataContainer().has(dummyKey, PersistentDataType.BYTE);
   }

   private static String formatHealth(LivingEntity entity) {
      double max = entity.getAttribute(Attribute.MAX_HEALTH) == null
            ? 20.0D
            : entity.getAttribute(Attribute.MAX_HEALTH).getValue();
      return String.format(java.util.Locale.US, "%.1f/%.1f", entity.getHealth(), max);
   }
}
