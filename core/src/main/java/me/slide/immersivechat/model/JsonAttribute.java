package me.slide.immersivechat.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JsonAttribute {
	private String name;
	private List<String> hoverText;
	private ClickAction clickAction;
	private String clickText;
}
