package me.slide.immersivechat.model;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Filter {
	private String matcher;
	private String replacer;
}
