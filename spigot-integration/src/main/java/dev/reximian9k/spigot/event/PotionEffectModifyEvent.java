package dev.reximian9k.spigot.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;

/**
 * @author Limo
 * @date 2/3/26
 */
public class PotionEffectModifyEvent extends PotionEffectEvent implements Cancellable {

    private final PotionEffect oldEffect;
    private PotionEffect newEffect;
    private boolean cancelled;

    public PotionEffectModifyEvent(LivingEntity target, PotionEffect oldEffect, PotionEffect newEffect) {
        super(target, newEffect);
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
    }

    public PotionEffect getOldEffect() {
        return oldEffect;
    }

    public PotionEffect getNewEffect() {
        return newEffect;
    }

    public void setNewEffect(PotionEffect newEffect) {
        this.newEffect = newEffect;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

}
