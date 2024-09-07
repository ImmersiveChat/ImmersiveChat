package me.slide.immersivechat.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JsonFormat {
	private String name;
	private int priority;
	private List<JsonAttribute> jsonAttributes;
}
