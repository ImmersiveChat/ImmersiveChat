package me.slide.immersivechat.localization;

import me.slide.immersivechat.utilities.FormatUtils;

/**
 * Messages internal to the plugin
 */
public enum InternalMessage {
    EMPTY_STRING(""),
    VENTURECHAT_AUTHOR("&6Written by Aust1n46"),
    VENTURECHAT_VERSION("&6VentureChat Version: {version}");

    private final String message;

    InternalMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return FormatUtils.FormatStringAll(this.message);
    }
}
