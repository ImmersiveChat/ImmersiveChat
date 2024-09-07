package me.slide.immersivechat.model;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MuteContainer implements IMuteContainer {
	private String channel;
	private long duration;
	private String reason;

	@Override
	public String getReason() {
		return reason;
	}
}
