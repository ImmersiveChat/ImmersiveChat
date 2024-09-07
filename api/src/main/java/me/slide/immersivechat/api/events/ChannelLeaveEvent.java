package me.slide.immersivechat.api.events;

import lombok.Getter;
import me.slide.immersivechat.model.IChatChannel;
import me.slide.immersivechat.model.IImmersiveChatPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ChannelLeaveEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    @Getter
    private IImmersiveChatPlayer player;
    @Getter
    private IChatChannel channel;

    public ChannelLeaveEvent(IImmersiveChatPlayer player, IChatChannel channel) {
        this.player = player;
        this.channel = channel;

        this.cancelled = false;
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