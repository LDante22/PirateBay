package com.example.data.model

/**
 * Detailed emulator optimization notes, recommended settings, BIOS/Firmware requirements,
 * and graphics pipeline parameters for each console platform and game.
 */
data class EmulatorProfile(
    val console: GameConsole,
    val recommendedEmulator: String,
    val alternativeEmulators: List<String>,
    val recommendedBackend: String, // "Vulkan (Recommended)", "OpenGL Core", "DirectX 12"
    val internalResolution: String, // "2x - 3x (1080p / 1440p)"
    val compatibilityRating: String, // "🟢 100% Playable (60 FPS)", "🟡 Playable with minor glitches"
    val targetFramerate: String, // "60 FPS Verified", "30/60 FPS"
    val biosRequirement: String, // "Requires PS2 BIOS (e.g. SCPH-39001)", "Switch prod.keys + title.keys"
    val recommendedSettings: List<Pair<String, String>>, // List of setting name to value
    val controllerLayout: String, // "DualShock 2 (Analog + Pressure Sensitive)", "Joy-Con Pair / Pro Controller"
    val proTips: List<String>,
    val customNotes: String = ""
)

object EmulatorProfiles {
    fun getProfileFor(game: Game): EmulatorProfile {
        val console = game.console
        val emuName = game.emulator.ifBlank { console.primaryEmulator() }

        val baseProfile = when (console) {
            GameConsole.PS2 -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "PCSX2 / NetherSX2",
                alternativeEmulators = listOf("PCSX2 v2.0+", "NetherSX2", "AetherSX2", "Play!"),
                recommendedBackend = "Vulkan (Optimal Shader Cache)",
                internalResolution = "3x Native (~1080p Full HD)",
                compatibilityRating = "🟢 100% Playable (60 FPS)",
                targetFramerate = "60 FPS (NTSC) / 50 FPS (PAL)",
                biosRequirement = "SCPH-39001 / SCPH-70004 PS2 BIOS (USA/Europe/Japan)",
                recommendedSettings = listOf(
                    "Renderer" to "Vulkan (Hardware)",
                    "Internal Resolution" to "3x Native (1080p)",
                    "Texture Filtering" to "Bilinear (PS2)",
                    "Anisotropic Filtering" to "16x",
                    "Blending Accuracy" to "Basic / Medium",
                    "Interlacing" to "Automatic (De-interlace ON)",
                    "Multi-Threaded VU1" to "Enabled (Boosts CPU multicore)"
                ),
                controllerLayout = "DualShock 2 Analog Controller (D-Pad + Dual Sticks + L1/L2/R1/R2)",
                proTips = listOf(
                    "Enable Multi-Threaded VU1 (MTVPU) for a 35% performance boost on multi-core CPUs.",
                    "Use Vulkan backend to eliminate texture flickering on modern GPUs.",
                    "For widescreen support, turn on 16:9 Widescreen Patches in System settings."
                )
            )

            GameConsole.PS3 -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "RPCS3",
                alternativeEmulators = listOf("RPCS3 (Latest Nightly)"),
                recommendedBackend = "Vulkan",
                internalResolution = "150% - 200% (1080p / 1440p)",
                compatibilityRating = "🟢 Playable (High Compatibility)",
                targetFramerate = "30 - 60 FPS Target",
                biosRequirement = "Official Sony PS3 Firmware (PS3UPDAT.PUP v4.91+)",
                recommendedSettings = listOf(
                    "PPU Decoder" to "LLVM Recompiler",
                    "SPU Decoder" to "LLVM Recompiler",
                    "SPU Threads" to "3 (Recommended for 6-8 core CPUs)",
                    "Renderer" to "Vulkan",
                    "Resolution Scale" to "150% (1920x1080) or 200%",
                    "Anisotropic Filter" to "16x",
                    "ZCull Accuracy" to "Approximate (Fast)",
                    "V-Sync" to "Enabled"
                ),
                controllerLayout = "DualShock 3 / DualSense with Sixaxis motion & pressure triggers",
                proTips = listOf(
                    "Install official PS3 System Firmware from PlayStation.com before launching.",
                    "Enable SPU Block Size = Safe for games with audio desync.",
                    "Use Write Color Buffers (WCB) if graphic artifacts or bloom issues appear."
                )
            )

            GameConsole.PSP -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "PPSSPP",
                alternativeEmulators = listOf("PPSSPP v1.17+", "RetroArch (PPSSPP Core)"),
                recommendedBackend = "Vulkan / OpenGL",
                internalResolution = "4x - 5x PSP (1080p / 1440p Crisp HD)",
                compatibilityRating = "🟢 Perfect (99.8% Full Speed)",
                targetFramerate = "30 / 60 FPS (Native 60 FPS Patches available)",
                biosRequirement = "Built-in HLE BIOS (No external BIOS file required)",
                recommendedSettings = listOf(
                    "Rendering Backend" to "Vulkan",
                    "Rendering Resolution" to "4x PSP (~1080p)",
                    "Texture Scaling" to "xBRZ 2x / Hybrid (Bicubic)",
                    "Anisotropic Filtering" to "16x",
                    "Frame Skipping" to "Off (0)",
                    "Audio Latency" to "Low Latency WASAPI / AAudio",
                    "Fast Memory" to "Enabled (Unstable = Off)"
                ),
                controllerLayout = "PSP Single Analog Stick + D-Pad + Face Buttons + L/R Bumpers",
                proTips = listOf(
                    "Install 60 FPS cheats for 30 FPS locked titles (like Crisis Core or Peace Walker).",
                    "Enable Texture Replacement if you have custom 4K HD texture packs installed.",
                    "PPSSPP runs flawlessly on almost any modern smartphone or PC."
                )
            )

            GameConsole.PS_VITA -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Vita3K",
                alternativeEmulators = listOf("Vita3K (Android / PC)"),
                recommendedBackend = "Vulkan",
                internalResolution = "2x Native (1080p OLED Clarity)",
                compatibilityRating = "🟡 Playable / In-Development (70%+ Library)",
                targetFramerate = "30 / 60 FPS",
                biosRequirement = "PS Vita Firmware (PSP2UPDAT.PUP) & Font Package",
                recommendedSettings = listOf(
                    "Backend" to "Vulkan",
                    "Internal Resolution" to "2x (1920x1088)",
                    "Texture Filtering" to "Anisotropic 16x",
                    "Shader Compiler" to "Vulkan Spir-V",
                    "Touchscreen Emulation" to "Front Screen + Rear Touchpad toggle",
                    "Memory Map" to "Native"
                ),
                controllerLayout = "Dual Analog Sticks + Front Touch + Rear Touchpad emulation",
                proTips = listOf(
                    "Download both the Firmware .PUP and the Vita Font Package in Vita3K initial setup.",
                    "Ensure game updates and DLC are installed via Vita3K 'Install .pkg / .zip' menu.",
                    "Map Rear Touchpad to controller L2/R2 triggers for best ergonomics."
                )
            )

            GameConsole.SWITCH -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Ryujinx / Eden / Sudachi",
                alternativeEmulators = listOf("Ryujinx", "Sudachi", "Suyu", "Yuzu Early Access"),
                recommendedBackend = "Vulkan",
                internalResolution = "1x Handheld (720p) / 2x Docked (1440p / 4K)",
                compatibilityRating = "🟢 High Playability (85%+ Titles)",
                targetFramerate = "30 / 60 FPS",
                biosRequirement = "prod.keys & title.keys (Firmware 18.0+)",
                recommendedSettings = listOf(
                    "Graphics API" to "Vulkan",
                    "ASTC Texture Decoding" to "GPU (Uncompressed)",
                    "Resolution Scale" to "2x (1440p / 2160p)",
                    "Anti-Aliasing" to "SMAA / FXAA",
                    "Window Adapting Filter" to "FSR (FidelityFX Super Resolution)",
                    "V-Sync Mode" to "Mailbox / FIFO"
                ),
                controllerLayout = "Nintendo Switch Pro Controller / Dual Joy-Cons with HD Rumble",
                proTips = listOf(
                    "Keep your prod.keys and Firmware version updated to match latest game releases.",
                    "Enable FSR upscaling for ultra sharp presentation on high resolution displays.",
                    "Clear shader cache if game stutters during initial launch."
                )
            )

            GameConsole.WII -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Dolphin Emulator",
                alternativeEmulators = listOf("Dolphin (Release / Beta / Dev)", "RetroArch (Dolphin)"),
                recommendedBackend = "Vulkan",
                internalResolution = "3x - 4x Native (1080p / 1440p)",
                compatibilityRating = "🟢 100% Playable (Near Flawless)",
                targetFramerate = "60 FPS",
                biosRequirement = "Built-in HLE (Wii System Menu optional)",
                recommendedSettings = listOf(
                    "Backend" to "Vulkan",
                    "Internal Resolution" to "3x Native (1080p)",
                    "Ubershaders" to "Hybrid (Reduces stuttering)",
                    "Compile Shaders Before Starting" to "Enabled",
                    "Dual Core" to "Enabled",
                    "V-Beam Speed Hack" to "Enabled",
                    "Widescreen Hack" to "Enabled"
                ),
                controllerLayout = "Emulated Wii Remote + Nunchuk / Real Wiimote via Bluetooth DolphinBar",
                proTips = listOf(
                    "Use 'Hybrid Ubershaders' to eliminate all shader compilation micro-stutters.",
                    "Connect a genuine Wii Remote or configure motion shake bindings to Right Analog stick.",
                    "Turn on 16:9 Anamorphic Widescreen in Dolphin graphic settings."
                )
            )

            GameConsole.WII_U -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Cemu",
                alternativeEmulators = listOf("Cemu (Native v2.0+)", "RetroArch (Cemu)"),
                recommendedBackend = "Vulkan",
                internalResolution = "2x - 4x Native (1440p / 4K UHD)",
                compatibilityRating = "🟢 99% Playable (Near Perfect)",
                targetFramerate = "60 FPS (Supports 60FPS/120FPS Graphic Packs)",
                biosRequirement = "No BIOS needed (Built-in HLE OS, MLC01 user files optional)",
                recommendedSettings = listOf(
                    "Graphics API" to "Vulkan",
                    "Graphic Packs" to "Resolution, FPS++, Enhanced Textures",
                    "CPU Mode" to "Triple-Core Recompiler",
                    "Shader Mul Accuracy" to "True / Auto",
                    "V-Sync" to "Double Buffering",
                    "Audio API" to "Cubeb / XAudio2"
                ),
                controllerLayout = "Wii U GamePad (Touchscreen + Gyro) / Wii U Pro Controller",
                proTips = listOf(
                    "Download Community Graphic Packs directly inside Cemu for instant 60 FPS, Ultra-Wide 21:9, and 4K enhancements.",
                    "Configure GamePad touchscreen toggle to a controller hotkey (e.g. Back/Select) to view the second screen effortlessly.",
                    "Enable Triple-Core Recompiler in CPU settings for maximum performance on multi-core PCs."
                )
            )

            GameConsole.GAMECUBE -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Dolphin Emulator",
                alternativeEmulators = listOf("Dolphin Emulator"),
                recommendedBackend = "Vulkan / Metal / OpenGL",
                internalResolution = "3x Native (1080p Crisp Geometry)",
                compatibilityRating = "🟢 100% Playable (Full Speed)",
                targetFramerate = "60 FPS (Progressive Scan 480p converted to 1080p)",
                biosRequirement = "Built-in HLE (GameCube IPL optional for boot animation)",
                recommendedSettings = listOf(
                    "Backend" to "Vulkan",
                    "Internal Resolution" to "3x Native (1080p)",
                    "Anisotropic Filtering" to "16x",
                    "Scaled EFB Copy" to "Enabled",
                    "Progressive Scan (480p)" to "Force Enabled",
                    "Shader Compilation" to "Hybrid Ubershaders"
                ),
                controllerLayout = "GameCube Controller Layout (A/B/X/Y + Octagonal Gate Analog + Analog Triggers)",
                proTips = listOf(
                    "Hold 'B' button upon game boot to enable 480p Progressive Scan in supported games.",
                    "Use GameCube analog trigger click simulation for Super Mario Sunshine and F-Zero GX."
                )
            )

            GameConsole.NINTENDO_3DS -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Citra / Lime3DS",
                alternativeEmulators = listOf("Citra (Nightly/Canary)", "Lime3DS", "Mandarine", "Citra Enhanced"),
                recommendedBackend = "Vulkan / OpenGL",
                internalResolution = "3x - 4x Native (1080p / 1440p Dual Screen)",
                compatibilityRating = "🟢 High Playability (90%+)",
                targetFramerate = "30 / 60 FPS",
                biosRequirement = "Decrypted 3DS ROM (.3ds / .cia) + System Archive Dump",
                recommendedSettings = listOf(
                    "Graphics API" to "Vulkan",
                    "Resolution" to "4x Native (1600x960)",
                    "Texture Filter" to "Anime4K / Bicubic",
                    "Screen Layout" to "Side by Side (Large Top Screen)",
                    "Audio Emulation" to "Cubeb / HLE Audio",
                    "Custom Textures" to "Load Custom Textures ON"
                ),
                controllerLayout = "Circle Pad + D-Pad + Touchscreen (Mouse / Finger touch overlay)",
                proTips = listOf(
                    "Make sure ROMs are decrypted (.3ds) or install AES keys into sysdata folder.",
                    "Use Side-by-Side screen layout on wide displays for comfortable dual screen view."
                )
            )

            GameConsole.PS4 -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "ShadPS4",
                alternativeEmulators = listOf("ShadPS4 (Nightly)", "fpPS4", "Spine", "Kyty"),
                recommendedBackend = "Vulkan",
                internalResolution = "1080p / 1440p / 4K Scaled",
                compatibilityRating = "🟡 In Rapid Development (Bloodborne & 2D titles booting)",
                targetFramerate = "30 / 60 FPS",
                biosRequirement = "Decrypted PS4 FPKGs + Firmware Sysmodules",
                recommendedSettings = listOf(
                    "Renderer" to "Vulkan",
                    "Resolution Scale" to "100% (1080p) or 150%",
                    "Shader Recompiler" to "Dynamic Spir-V",
                    "PM4 Draw Hack" to "Auto",
                    "Framerate Unlock" to "60 FPS Community Patches"
                ),
                controllerLayout = "DualShock 4 / DualSense with Touchpad Click & Gyro",
                proTips = listOf(
                    "ShadPS4 supports 60 FPS unlock and sound overhaul patches for Bloodborne.",
                    "Ensure you install decrypted game packages (.pkg / extracted elf) with updates."
                )
            )

            GameConsole.PS5 -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Kyty / RPCSX",
                alternativeEmulators = listOf("Kyty", "RPCSX", "PlayStation Cloud Streaming"),
                recommendedBackend = "Vulkan / Direct3D 12",
                internalResolution = "Native 4K (2160p HDR)",
                compatibilityRating = "🟡 Early Experimental / Cloud",
                targetFramerate = "60 - 120 FPS",
                biosRequirement = "PS5 Decrypted Libraries & Keys",
                recommendedSettings = listOf(
                    "Graphics API" to "Vulkan",
                    "Internal Resolution" to "Native (3840x2160)",
                    "Audio Engine" to "Tempest 3D Audio Simulation",
                    "DualSense Haptics" to "Direct USB Passthrough"
                ),
                controllerLayout = "DualSense Wireless Controller (Adaptive Triggers + Haptic Feedback)",
                proTips = listOf(
                    "Connect DualSense via USB-C to experience native adaptive trigger resistance.",
                    "Use PlayStation Remote Play or Cloud for instant full-speed PS5 gaming."
                )
            )

            GameConsole.GBA -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "mGBA",
                alternativeEmulators = listOf("mGBA v0.10+", "VBA-M", "Pizza Boy GBA", "RetroArch (mGBA Core)"),
                recommendedBackend = "OpenGL / Software Renderer",
                internalResolution = "4x - 6x GBA (Pixel-Perfect / CRT LCD Grid)",
                compatibilityRating = "🟢 100% Flawless Perfection",
                targetFramerate = "59.73 FPS (Exact GBA Hardware Timing)",
                biosRequirement = "Built-in Open Source BIOS (Official gba_bios.bin optional for intro chime)",
                recommendedSettings = listOf(
                    "Video Driver" to "OpenGL",
                    "Resolution Scale" to "5x (1200x800)",
                    "Color Correction" to "GBA LCD Profile (Restores vibrant original colors)",
                    "Shader Filter" to "LCD3x / GBA Grid Shader",
                    "Audio Interpolation" to "Sinc (Ultra High Quality)",
                    "Real-Time Clock (RTC)" to "Enabled (for Pokemon day/night events)"
                ),
                controllerLayout = "D-Pad + A/B Buttons + L/R Shoulder Bumpers + Turbo/Fast-Forward",
                proTips = listOf(
                    "Enable 'Color Correction' to fix over-saturated colors on modern OLED screens.",
                    "Use mGBA's Solar Sensor and Tilt Sensor emulation for Boktai and WarioWare Twisted."
                )
            )

            GameConsole.N64 -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Simple64 / Mupen64Plus",
                alternativeEmulators = listOf("Simple64 (ParaLLEl-RDP)", "Mupen64Plus-Next", "Project64 v3.0+", "RetroArch"),
                recommendedBackend = "Vulkan (Parallel RDP + RSP)",
                internalResolution = "4x Native (1280x960 / 1080p)",
                compatibilityRating = "🟢 99.9% High Accuracy",
                targetFramerate = "30 / 60 FPS (Native 60 FPS Widescreen Patches available)",
                biosRequirement = "Built-in HLE (No external BIOS required)",
                recommendedSettings = listOf(
                    "RDP Plugin" to "Parallel RDP (Low-Level Precision)",
                    "RSP Plugin" to "Parallel RSP (Vulkan Compute)",
                    "Upscaling Factor" to "4x Native",
                    "Texture Filtering" to "3-Point Bilinear (N64 Authentic)",
                    "Dithering" to "Enabled (or De-dither 24-bit output)",
                    "Expansion Pak (8MB RAM)" to "Force Enabled (Crucial for Majora's Mask & Perfect Dark)"
                ),
                controllerLayout = "N64 Trident Controller (Z-Trigger on L2/R2, C-Buttons mapped to Right Stick)",
                proTips = listOf(
                    "Map the iconic yellow C-buttons to your modern controller's Right Analog Stick.",
                    "Use community PC Native Decompilation ports (Render96, Ship of Harkinian) for 4K 144FPS."
                )
            )

            GameConsole.NINTENDO_DS -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "MelonDS",
                alternativeEmulators = listOf("MelonDS v0.9.5+", "DeSmuME", "DraStic (Android)", "RetroArch (melonDS)"),
                recommendedBackend = "OpenGL / Vulkan",
                internalResolution = "3x - 4x Native (1080p High-Res 3D Polygons)",
                compatibilityRating = "🟢 100% Full Speed Playable",
                targetFramerate = "60 FPS",
                biosRequirement = "Built-in FreeBIOS (bios7.bin, bios9.bin, firmware.bin optional for DSi ware)",
                recommendedSettings = listOf(
                    "3D Renderer" to "OpenGL (Hardware Accelerated)",
                    "Internal Resolution" to "4x Native (1024x768 3D rendering)",
                    "Screen Layout" to "Top / Bottom Vertical (or Side by Side)",
                    "Screen Gap" to "Standard DS Hinge Gap (64px)",
                    "Touch Mode" to "Mouse Pointer / Stylus Touchscreen",
                    "JIT Recompiler" to "Enabled (x64 / ARM64 JIT)"
                ),
                controllerLayout = "D-Pad + Face Buttons + Touch Pointer (Right Analog or Touchpad)",
                proTips = listOf(
                    "Enable 4x 3D internal resolution in melonDS to transform jagged 3D models into crisp modern HD.",
                    "Configure a hotkey to swap primary and secondary screens during touch-heavy gameplay sections."
                )
            )

            GameConsole.XBOX -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "xemu",
                alternativeEmulators = listOf("xemu (Open Source)", "Cxbx-Reloaded"),
                recommendedBackend = "Vulkan / OpenGL",
                internalResolution = "3x Native (~1440p / 4K QHD)",
                compatibilityRating = "🟢 Playable (85%+ Library Running at Full Speed)",
                targetFramerate = "60 FPS Target",
                biosRequirement = "MCPX Boot ROM (mcpx_1.0.bin) + Complex 4627 / 5838 BIOS + Xbox HDD Image",
                recommendedSettings = listOf(
                    "Rendering API" to "Vulkan",
                    "Internal Resolution" to "3x Native (1920x1440)",
                    "Widescreen (16:9)" to "Anamorphic Enabled",
                    "Texture Filtering" to "Anisotropic 16x",
                    "Audio Emulation" to "MCPX DSP (DirectSound3D)",
                    "Network Emulation" to "Insignia Xbox Live Enabled"
                ),
                controllerLayout = "Xbox 'Duke' / Controller S Layout (A/B/X/Y + Black/White buttons mapped to LB/RB)",
                proTips = listOf(
                    "Install Insignia Xbox Live replacement servers to play original Xbox multiplayer online in 2026!",
                    "Map Original Xbox Black & White buttons to modern controller LB and RB bumpers."
                )
            )

            GameConsole.XBOX_360 -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Xenia Canary",
                alternativeEmulators = listOf("Xenia Canary (Recommended)", "Xenia Master"),
                recommendedBackend = "Direct3D 12 / Vulkan",
                internalResolution = "2x Native (2560x1440 2K Crisp Detail)",
                compatibilityRating = "🟢 High Playability (Thousands of titles playable)",
                targetFramerate = "60 FPS (with Xenia Canary Patches)",
                biosRequirement = "Built-in HLE (No BIOS file required)",
                recommendedSettings = listOf(
                    "Graphics API" to "Direct3D 12 / Vulkan",
                    "Draw Resolution Scale" to "2x (1440p) or 3x (4K)",
                    "GPU Readback (ROV / RTV)" to "Rasterizer Ordered Views (ROV)",
                    "Post-Processing Filter" to "FidelityFX Super Resolution (FSR)",
                    "Patch Engine" to "Apply 60 FPS & Aspect Ratio Patches",
                    "Mount Cache" to "Enabled"
                ),
                controllerLayout = "Xbox 360 Controller / Xbox Series X Pad (Native XInput Support)",
                proTips = listOf(
                    "Use Xenia Canary rather than Master branch for active compatibility updates and game patch engine.",
                    "Enable 60 FPS patches in xenia-canary.config.toml for titles like Red Dead Redemption and Gears of War."
                )
            )

            GameConsole.XBOX_ONE -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Xbox Cloud / Dev Mode",
                alternativeEmulators = listOf("Xbox Dev Mode", "Xbox Cloud Gaming (xCloud)", "Xenia (BC Titles)"),
                recommendedBackend = "DirectX 12 Ultimate",
                internalResolution = "1080p / 4K HDR",
                compatibilityRating = "🟢 Native Cloud / Dev Mode",
                targetFramerate = "60 - 120 FPS",
                biosRequirement = "Xbox Live Account / Xbox Game Pass",
                recommendedSettings = listOf(
                    "Streaming Protocol" to "WebRTC / Cloud Gaming",
                    "Resolution" to "1080p (High Bitrate) / 4K",
                    "Audio" to "Dolby Atmos / Windows Sonic Spatial",
                    "Variable Refresh Rate" to "VRR / FreeSync 120Hz"
                ),
                controllerLayout = "Xbox Wireless Controller (Impulse Triggers + Share Button)",
                proTips = listOf(
                    "Xbox One games run with zero latency and full fidelity via Xbox Cloud Gaming.",
                    "Xbox Series / One Dev Mode allows sideloading UWP emulation frontends like RetroArch."
                )
            )

            GameConsole.PC -> EmulatorProfile(
                console = console,
                recommendedEmulator = if (emuName.isNotBlank()) emuName else "Steam / PC Native",
                alternativeEmulators = listOf("Steam", "Heroic Games Launcher", "DOSBox-Staging", "ScummVM"),
                recommendedBackend = "DirectX 11/12 / Vulkan / Proton GE",
                internalResolution = "Native 1080p / 1440p / 4K Ultra",
                compatibilityRating = "🟢 100% Native",
                targetFramerate = "60 - 144+ FPS Uncapped",
                biosRequirement = "None (Native PC Binary / Steam Client)",
                recommendedSettings = listOf(
                    "Compatibility Layer" to "Proton GE / Native Windows",
                    "Render API" to "DirectX 12 / Vulkan",
                    "Framerate Cap" to "Monitor Refresh Rate (V-Sync / G-Sync)",
                    "Resolution" to "Display Native 4K / 1440p / 1080p"
                ),
                controllerLayout = "XInput / DirectInput / Keyboard & Mouse / Steam Input",
                proTips = listOf(
                    "Use Steam Input to easily customize controller mappings and gyro aim.",
                    "For older classic PC games, DOSBox Staging provides CRT shaders and Roland MT-32 audio."
                )
            )

            GameConsole.ALL -> EmulatorProfile(
                console = console,
                recommendedEmulator = "RetroArch / Dedicated Standalone Emulators",
                alternativeEmulators = listOf("PCSX2", "RPCS3", "PPSSPP", "Vita3K", "Dolphin", "Citra", "Ryujinx"),
                recommendedBackend = "Vulkan",
                internalResolution = "3x Native (1080p)",
                compatibilityRating = "🟢 Universal High Performance",
                targetFramerate = "60 FPS Target",
                biosRequirement = "Refer to specific console requirements",
                recommendedSettings = listOf(
                    "Universal Backend" to "Vulkan (Hardware Accelerated)",
                    "Target Resolution" to "1080p (Full HD Scale)"
                ),
                controllerLayout = "Universal Gamepad (Xbox / DualSense / Switch Pro)",
                proTips = listOf(
                    "Standalone emulators generally offer better performance and features than all-in-one multi-cores."
                )
            )
        }

        // Overlay game-specific custom emulator notes stored in Room if present
        val effectiveEmulator = if (game.emulator.isNotBlank()) game.emulator else baseProfile.recommendedEmulator
        val effectiveBackend = if (game.recommendedBackend.isNotBlank()) game.recommendedBackend else baseProfile.recommendedBackend
        val effectiveResolution = if (game.internalResolution.isNotBlank()) game.internalResolution else baseProfile.internalResolution
        val effectiveCompatibility = if (game.compatibilityRating.isNotBlank()) game.compatibilityRating else baseProfile.compatibilityRating
        val effectiveBios = if (game.biosRequirement.isNotBlank()) game.biosRequirement else baseProfile.biosRequirement
        val effectiveFramerate = if (game.targetFramerate.isNotBlank()) game.targetFramerate else baseProfile.targetFramerate
        val effectiveController = if (game.controllerLayout.isNotBlank()) game.controllerLayout else baseProfile.controllerLayout

        val customTips = if (game.proTips.isNotBlank()) {
            game.proTips.split("\n", ";").map { it.trim() }.filter { it.isNotBlank() }
        } else emptyList()

        val effectiveTips = if (customTips.isNotEmpty()) {
            customTips + baseProfile.proTips
        } else if (game.emulatorNotes.isNotBlank()) {
            listOf(game.emulatorNotes) + baseProfile.proTips
        } else {
            baseProfile.proTips
        }

        return baseProfile.copy(
            recommendedEmulator = effectiveEmulator,
            recommendedBackend = effectiveBackend,
            internalResolution = effectiveResolution,
            compatibilityRating = effectiveCompatibility,
            biosRequirement = effectiveBios,
            targetFramerate = effectiveFramerate,
            controllerLayout = effectiveController,
            proTips = effectiveTips.distinct(),
            customNotes = game.emulatorNotes
        )
    }
}
