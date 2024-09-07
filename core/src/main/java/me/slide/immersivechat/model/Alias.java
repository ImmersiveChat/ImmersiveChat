package me.slide.immersivechat.model;

import lombok.Getter;

import java.util.List;

@Getter
public class Alias {
	private String name;
	private int arguments;
	private List<String> components;
	private String permission;

	public Alias(String name, int arguments, List<String> components, String permission) {
		this.name = name;
		this.arguments = arguments;
		this.components = components;
		this.permission = "venturechat." + permission;
	}

	public boolean hasPermission() {
		return !permission.equalsIgnoreCase("venturechat.none");
	}
}
