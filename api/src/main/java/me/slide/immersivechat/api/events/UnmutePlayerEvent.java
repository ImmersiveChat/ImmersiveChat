package me.slide.immersivechat.api.events;

import lombok.Getter;
import me.slide.immersivechat.model.IChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

public class UnmutePlayerEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    @Getter
    private IImmersiveChatPlayer victim;
    @Getter
    private IImmersiveChatPlayer operator;
    @Getter
    private Set<IChatChannel> channels;

    public UnmutePlayerEvent(IImmersiveChatPlayer victim, IImmersiveChatPlayer operator, Set<IChatChannel> channels) {
        this.victim = victim;
        this.operator = operator;
        this.channels = channels;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}
