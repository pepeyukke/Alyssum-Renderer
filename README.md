# Alyssum Renderer

Alyssum Renderer is a free and open-source performance & shaders mod for Minecraft clients. It is a fork of Embeddium (which itself
was based on the last FOSS-licensed version of Sodium) and Oculus 1.7.

I maintain this mod for personal use & experimentation and make the source code available for other projects and
developers who may be interested. There are currently no plans to distribute binaries for modern Minecraft versions on CurseForge,
Modrinth, or any other player-facing mod platform. There is also no guarantee of active maintenance, including bugfixes
or ports to any newer Minecraft versions. That said, the code remains
LGPL-3.0, so other projects under a compatible license (including Embeddium) should feel free to incorporate bugfixes
and features they find useful. That said, expect minimal support, and many possible bugs due to limited testing.

# Credits

* The CaffeineMC team, for developing Sodium 0.5.11 & older, and making it open source
* Rongmario is a contributor who has worked on backporting projects, notably with Vintagium. Specifically, they took on the signficant task of backporting Embeddium from Minecraft version 1.16.5 all the way down to versin 1.12.2 beyond just the initial backport, Rongmario also implemented several fexes and improvements to ensure the older version of Embeddium functioned well.

* Asek3, for developing Rubidium, the original port of Sodium 0.5 to Forge
* Ven ([@basdxz](https://github.com/basdxz)), for help with translucency sorting,suggesting the general approach for async occlusion culling, and other suggestions during development
* XFactHD, Pepper, and anyone else I've forgotten to mention, for providing valuable code insights