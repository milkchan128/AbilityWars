package daybreak.abilitywar.utils.base.minecraft.version

import com.google.common.base.Enums
import daybreak.abilitywar.utils.base.minecraft.version.NMSVersion.UNSUPPORTED
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import java.util.logging.Level
import java.util.logging.Logger

interface IVersion {
	val version: Int
	val release: Int
	fun isAboveOrEqual(other: IVersion): Boolean {
		return version >= other.version && (version != other.version || release >= other.release)
	}

	fun isBelowOrEqual(other: IVersion): Boolean {
		return version <= other.version && (version != other.version || release <= other.release)
	}
}

enum class NMSVersion(override val version: Int, override val release: Int) : IVersion {
	UNSUPPORTED(-1, -1),
	v1_12_R1(12, 1),
	v1_13_R1(13, 1),
	v1_13_R2(13, 2),
	v1_14_R1(14, 1),
	v1_15_R1(15, 1),
	v1_16_R1(16, 1),
	v1_16_R2(16, 2),
	v1_16_R3(16, 3),
	v1_17_R1(17, 1),
	v1_19_R1(19, 1),
	v1_19_R2(19, 2),
	v1_20_R3(20, 3),
	paper_1_21_11(21, 7);

	companion object {
		@JvmStatic
		fun resolve(serverClass: String, bukkitVersion: String, paper: Boolean, paperBuild: Boolean): NMSVersion {
			if (paperBuild) {
				return if (paper && bukkitVersion.substringBefore('-') == "1.21.11") paper_1_21_11 else UNSUPPORTED
			}
			val parts = serverClass.split('.')
			return if (parts.size >= 4) Enums.getIfPresent(NMSVersion::class.java, parts[3]).or(UNSUPPORTED) else UNSUPPORTED
		}
	}
}

class ServerVersion private constructor() {
	companion object INSTANCE : IVersion {
		private val logger = Logger.getLogger(ServerVersion::class.java.name)

		private val paperBuild = ServerVersion::class.java.getResource("/abilitywar-paper.properties") != null
		private val paper = Bukkit.getName().equals("Paper", ignoreCase = true) && try {
			Class.forName("io.papermc.paper.configuration.GlobalConfiguration")
			true
		} catch (ignored: ClassNotFoundException) { false }
		private val INSTANCE = NMSVersion.resolve(Bukkit.getServer().javaClass.name, Bukkit.getBukkitVersion(), paper, paperBuild)

		@JvmStatic
		override val version: Int
			get() = INSTANCE.version

		@JvmStatic
		override val release: Int
			get() = INSTANCE.release

		@JvmStatic
		val name: String
			get() = INSTANCE.name

		@JvmStatic
		override fun isAboveOrEqual(other: IVersion): Boolean {
			return super.isAboveOrEqual(other)
		}

		@JvmStatic
		override fun isBelowOrEqual(other: IVersion): Boolean {
			return super.isBelowOrEqual(other)
		}

		@JvmStatic
		fun compatVersion(plugin: Plugin): Boolean {
			return if (INSTANCE != UNSUPPORTED) {
				if (version >= 13 && !paperBuild) {
					try {
						PluginDescriptionFile::class.java.getDeclaredField("apiVersion").apply {
							isAccessible = true
							this[plugin.description] = "1.$version"
							isAccessible = false
						}
					} catch (ignored: ReflectiveOperationException) {
					}
				}
				true
			} else {
				logger.log(Level.SEVERE, if (paperBuild) "이 빌드는 Paper 1.21.11만 지원합니다." else "플러그인이 지원하지 않는 버전을 이용하고 있습니다.")
				Bukkit.getPluginManager().disablePlugin(plugin)
				false
			}
		}
	}
}
