package me.slide.immersivechat.localization;

import me.slide.immersivechat.initiators.application.ImmersiveChat;
import me.slide.immersivechat.utilities.FormatUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Localization {
	private static final String VERSION = "2.22.4";
	private static FileConfiguration localization;

	public static void initialize(final ImmersiveChat plugin) {
		File localizationFile = new File(plugin.getDataFolder().getAbsolutePath(), "Messages.yml");
		if (!localizationFile.isFile()) {
			plugin.saveResource("Messages.yml", true);
		}

		localization = YamlConfiguration.loadConfiguration(localizationFile);

		String fileVersion = localization.getString("Version", "null");

		if (!fileVersion.equals(VERSION)) {
			plugin.getServer().getConsoleSender()
					.sendMessage(FormatUtils.FormatStringAll("&8[&eVentureChat&8]&e - Version Change Detected!  Saving Old Messages.yml and Generating Latest File"));
			localizationFile.renameTo(new File(plugin.getDataFolder().getAbsolutePath(), "Messages_Old_" + fileVersion + ".yml"));
			plugin.saveResource("Messages.yml", true);
			localization = YamlConfiguration.loadConfiguration(localizationFile);
		}
	}

	public static FileConfiguration getLocalization() {
		return localization;
	}
}
