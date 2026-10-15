package daybreak.abilitywar.utils.installer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/** 이 포크의 정식 릴리스를 다음 서버 시작에 적용합니다. */
public class ForkUpdater {
    public static final String AUTHOR = "milkchan128";
    public static final String REPOSITORY = "AbilityWars";
    private static final String RELEASES = "https://api.github.com/repos/" + AUTHOR + "/" + REPOSITORY + "/releases";
    private static final String DOWNLOADS = "https://github.com/" + AUTHOR + "/" + REPOSITORY + "/releases/download/";
    private final String currentVersion;
    private final Path installedFile;
    private final Path updateDirectory;
    private final Logger logger;

    public ForkUpdater(String currentVersion, File installedFile, File updateDirectory, Logger logger) {
        this.currentVersion = currentVersion;
        this.installedFile = installedFile.toPath();
        this.updateDirectory = updateDirectory.toPath();
        this.logger = logger;
    }

    public static boolean isForkBuild() {
        return ForkUpdater.class.getResource("/abilitywar-paper.properties") != null;
    }

    public void checkForUpdate() {
        logger.info("이 포크의 새 정식 릴리스를 확인합니다.");
        try {
            JsonObject release;
            try (InputStream input = open(new URL(RELEASES + "/latest"))) {
                release = JsonParser.parseString(new String(readLimited(input, 1024 * 1024), StandardCharsets.UTF_8)).getAsJsonObject();
            }
            if (!stageRelease(release)) logger.info("현재 버전 " + currentVersion + "보다 새로운 정식 릴리스가 없습니다.");
        } catch (IOException | RuntimeException error) {
            logger.warning("자동 업데이트를 완료하지 못했습니다. 현재 버전을 유지합니다: " + error.getMessage());
        }
    }

    public void install(JsonObject release) {
        try {
            if (!stageRelease(release)) logger.info("현재 버전보다 새로운 정식 릴리스만 설치할 수 있습니다.");
        } catch (IOException | RuntimeException error) {
            logger.warning("업데이트를 준비하지 못했습니다. 현재 버전을 유지합니다: " + error.getMessage());
        }
    }

    boolean stageRelease(JsonObject release) throws IOException {
        if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) return false;
        final String tag = release.get("tag_name").getAsString();
        final String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (!isNewer(version, currentVersion)) return false;
        final String assetName = "AbilityWar-" + version + ".jar";
        final JsonObject asset = findAsset(release, assetName);
        final JsonObject checksumAsset = findAsset(release, "SHA256SUMS.txt");
        if (asset == null || checksumAsset == null) throw new IOException("배포 JAR 또는 SHA256SUMS.txt가 없습니다.");
        final URL jarUrl = assetUrl(asset, tag);
        final URL checksumUrl = assetUrl(checksumAsset, tag);
        final long size = asset.get("size").getAsLong();
        if (size <= 0 || size > 64 * 1024 * 1024) throw new IOException("배포 JAR 크기가 올바르지 않습니다.");
        final String checksum;
        try (InputStream input = open(checksumUrl)) {
            checksum = checksumFor(new String(readLimited(input, 64 * 1024), StandardCharsets.UTF_8), assetName);
        }
        synchronized (ForkUpdater.class) {
            Files.createDirectories(updateDirectory);
            final Path queuedFile = updateDirectory.resolve(installedFile.getFileName());
            if (Files.exists(queuedFile)) throw new IOException("이미 준비된 업데이트가 있습니다: " + queuedFile.getFileName());
            final Path temporaryFile = Files.createTempFile(updateDirectory, ".abilitywar-", ".tmp");
            try {
                logger.info("새 릴리스 " + tag + " 다운로드를 시작합니다.");
                try (InputStream input = open(jarUrl); java.io.OutputStream output = Files.newOutputStream(temporaryFile)) {
                    byte[] buffer = new byte[8192];
                    long received = 0;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        received += count;
                        if (received > size) throw new IOException("배포 JAR가 공지된 크기를 초과했습니다.");
                        output.write(buffer, 0, count);
                    }
                    if (received != size) throw new IOException("배포 JAR 다운로드가 완전하지 않습니다.");
                }
                if (!sha256(temporaryFile).equalsIgnoreCase(checksum)) throw new IOException("배포 JAR의 SHA-256이 일치하지 않습니다.");
                validateJar(temporaryFile, version);
                final Path backupDirectory = installedFile.getParent().resolve("AbilityWar/backups");
                Files.createDirectories(backupDirectory);
                final Path backup = backupDirectory.resolve("AbilityWar-" + currentVersion + ".jar");
                if (!Files.exists(backup)) Files.copy(installedFile, backup);
                // 실행 중인 JAR는 건드리지 않고 Paper·Purpur의 업데이트 폴더를 사용합니다.
                Files.move(temporaryFile, queuedFile, StandardCopyOption.ATOMIC_MOVE);
                logger.info("업데이트 " + tag + "를 준비했습니다. 다음 서버 시작 때 적용됩니다. 기존 JAR 백업: " + backup);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }
        return true;
    }

    static boolean isNewer(String candidate, String current) throws IOException {
        long[] next = versionParts(candidate), installed = versionParts(current);
        for (int i = 0; i < next.length; i++) {
            if (next[i] != installed[i]) return next[i] > installed[i];
        }
        return false;
    }

    private static long[] versionParts(String version) throws IOException {
        if (!version.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")) throw new IOException("정식 버전 형식이 아닙니다: " + version);
        try {
            String[] parts = version.split("\\.");
            return new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])};
        } catch (NumberFormatException error) {
            throw new IOException("버전 번호가 너무 큽니다.", error);
        }
    }

    static JsonObject findAsset(JsonObject release, String name) {
        for (JsonElement element : release.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            if (name.equals(asset.get("name").getAsString())) return asset;
        }
        return null;
    }

    private static URL assetUrl(JsonObject asset, String tag) throws IOException {
        String expected = DOWNLOADS + tag + "/" + asset.get("name").getAsString();
        if (!expected.equals(asset.get("browser_download_url").getAsString())) throw new IOException("이 포크의 릴리스 첨부 파일이 아닙니다.");
        return new URL(expected);
    }

    static String checksumFor(String checksums, String name) throws IOException {
        for (String line : checksums.split("\\r?\\n")) {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length == 2 && (parts[1].equals(name) || parts[1].equals("*" + name)) && parts[0].matches("[a-fA-F0-9]{64}")) return parts[0];
        }
        throw new IOException("배포 JAR의 SHA-256 체크섬이 없습니다.");
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) result.append(String.format("%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void validateJar(Path file, String version) throws IOException {
        try (JarFile jar = new JarFile(file.toFile())) {
            if (jar.getJarEntry("plugin.yml") == null || jar.getJarEntry("abilitywar-paper.properties") == null) throw new IOException("이 포크의 배포 JAR가 아닙니다.");
            try (InputStream input = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {
                PluginDescriptionFile description = new PluginDescriptionFile(input);
                if (!"AbilityWar".equals(description.getName()) || !"daybreak.abilitywar.AbilityWar".equals(description.getMain())
                        || !version.equals(description.getVersion()) || !"1.21.11".equals(description.getAPIVersion())) throw new IOException("플러그인 정보 또는 지원 버전이 일치하지 않습니다.");
            } catch (org.bukkit.plugin.InvalidDescriptionException error) {
                throw new IOException("플러그인 정보가 올바르지 않습니다.", error);
            }
            Properties compatibility = new Properties();
            try (InputStream input = jar.getInputStream(jar.getJarEntry("abilitywar-paper.properties"))) {
                compatibility.load(input);
            }
            if (!"1.21.11".equals(compatibility.getProperty("version")) || jar.getManifest() == null
                    || !"mojang".equals(jar.getManifest().getMainAttributes().getValue("paperweight-mappings-namespace"))
                    || jar.getJarEntry("daybreak/abilitywar/AbilityWar.class") == null
                    || jar.getJarEntry("daybreak/abilitywar/utils/base/minecraft/nms/paper_1_21_11/NMSImpl.class") == null) throw new IOException("Paper·Purpur 1.21.11 배포 JAR가 아닙니다.");
        }
    }

    InputStream open(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "AbilityWar-mlkc-studio/" + currentVersion);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("GitHub 응답 코드: " + connection.getResponseCode());
            return new java.io.FilterInputStream(connection.getInputStream()) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { connection.disconnect(); }
                }
            };
        } catch (IOException error) {
            connection.disconnect();
            throw error;
        }
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new IOException("업데이트 정보가 허용 크기를 초과했습니다.");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
