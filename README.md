# ImmersiveChat #

ImmersiveChat is a fork of [VentureChat](https://github.com/Aust1n46/VentureChat) that aims to improve some of its aspects.

### What has already been implemented?
* API (Events)
* Changed build system from Maven to Gradle
* Dependency inversion principle
### What is planned
* Add toggle for moderation gui
* MiniMessage color pattern support
* Add GUI for /channel to join channels
* and more...

### How to setup the plugin?

| Dependency      |                             Download                              |                           Source                           |
|-----------------|:-----------------------------------------------------------------:|:----------------------------------------------------------:|
| ProtocolLib     |  [Spigot](https://www.spigotmc.org/resources/protocollib.1997/)   |     [Github](https://github.com/dmulloy2/ProtocolLib/)     |
| PlaceholdersAPI | [Spigot](https://www.spigotmc.org/resources/placeholderapi.6245/) | [Github](https://github.com/PlaceholderAPI/PlaceholderAPI) |
| Vault           |          [Bukkit](https://dev.bukkit.org/projects/vault)          |        [Github](https://github.com/milkbowl/Vault)         |

### How to migrate from VentureChat?
1. Replace the VentureChat jar file in the plugins folder with ImmersiveChat
2. Rename the VentureChat folder in the config folder to ImmersiveChat.
3. Done!

### What about [Folia](https://papermc.io/software/folia) support?
* ImmersiveChat is able to support Folia, but one of its dependencies, namely PlaceholdersAPI, does not support it. You can try to download a fork of it that supports Folia (for example [this](https://github.com/Anon8281/PlaceholderAPIt)) to make ImmersiveChat work, but we do not guarantee stability and reliability in this case.
* * *
License:

Copyright (C) {2024}  {Adam Mekush}

```
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
