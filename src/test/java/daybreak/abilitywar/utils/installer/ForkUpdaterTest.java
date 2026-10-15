package daybreak.abilitywar.utils.installer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ForkUpdaterTest {
    @TempDir Path directory;

    @Test void comparesNumericStableVersions() throws Exception {
        assertTrue(ForkUpdater.isNewer("1.10.0", "1.9.9"));
        assertTrue(ForkUpdater.isNewer("2.0.0", "1.99.99"));
        assertFalse(ForkUpdater.isNewer("1.1.0", "1.1.0"));
        assertFalse(ForkUpdater.isNewer("1.0.0", "1.1.0"));
        for (String version : new String[]{"../2.0.0", "1.1.1-beta", "1.1", "01.1.1", "999999999999999999999.1.1"}) {
            assertThrows(IOException.class, () -> ForkUpdater.isNewer(version, "1.1.0"));
        }
    }

    @Test void checksLatestAndStagesUnderOriginalFilenameWithBackup() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.11");
        fixture.updater.checkForUpdate();
        assertArrayEquals(fixture.jar, Files.readAllBytes(fixture.queued));
        assertArrayEquals(fixture.original, Files.readAllBytes(fixture.installed));
        assertArrayEquals(fixture.original, Files.readAllBytes(directory.resolve("AbilityWar/backups/AbilityWar-1.1.0.jar")));
        assertEquals(3, fixture.requests);
        assertNoTemporaryFiles();
    }

    @Test void skipsOldSameDraftAndPrereleaseWithoutDownloads() throws Exception {
        Fixture fixture = new Fixture("1.0.0", "1.21.11");
        assertFalse(fixture.updater.stageRelease(fixture.release));
        fixture.release.addProperty("tag_name", "v1.1.0");
        assertFalse(fixture.updater.stageRelease(fixture.release));
        fixture.release.addProperty("tag_name", "v1.1.1");
        fixture.release.addProperty("draft", true);
        assertFalse(fixture.updater.stageRelease(fixture.release));
        fixture.release.addProperty("draft", false);
        fixture.release.addProperty("prerelease", true);
        assertFalse(fixture.updater.stageRelease(fixture.release));
        assertEquals(0, fixture.requests);
        assertFalse(Files.exists(fixture.queued));
    }

    @Test void checksumFailureKeepsInstalledJarAndLeavesNoQueuedJar() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.11");
        fixture.checksums = new String(new char[64]).replace('\0', '0') + "  AbilityWar-1.1.1.jar\n";
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
    }

    @Test void rejectsIncompatibleOrMislabeledJar() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.12");
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
        fixture.jar = jar("1.1.2", "1.21.11", true);
        fixture.updateDigest();
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
        fixture.jar = jar("1.1.1", "1.21.11", false);
        fixture.updateDigest();
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
    }

    @Test void rejectsTruncatedDownloadAndMissingChecksum() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.11");
        fixture.jarAsset.addProperty("size", fixture.jar.length + 1);
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
        fixture.jarAsset.addProperty("size", fixture.jar.length - 1);
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
        fixture.release.getAsJsonArray("assets").remove(0);
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        fixture.assertUnchanged();
    }

    @Test void rejectsAssetsFromAnotherRepository() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.11");
        fixture.jarAsset.addProperty("browser_download_url", "https://github.com/DayBreak365/AbilityWar/releases/download/v1.1.1/AbilityWar-1.1.1.jar");
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        assertEquals(0, fixture.requests);
        fixture.assertUnchanged();
    }

    @Test void preservesAnAlreadyQueuedUpdate() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.11");
        Files.createDirectories(fixture.queued.getParent());
        byte[] previous = "previously queued update".getBytes(StandardCharsets.UTF_8);
        Files.write(fixture.queued, previous);
        assertThrows(IOException.class, () -> fixture.updater.stageRelease(fixture.release));
        assertArrayEquals(previous, Files.readAllBytes(fixture.queued));
        assertArrayEquals(fixture.original, Files.readAllBytes(fixture.installed));
        assertNoTemporaryFiles();
    }

    @Test void networkFailureDoesNotStopPluginOrChangeFiles() throws Exception {
        Fixture fixture = new Fixture("1.1.1", "1.21.11");
        fixture.failNetwork = true;
        assertDoesNotThrow(fixture.updater::checkForUpdate);
        fixture.assertUnchanged();
    }

    @Test void verifiesKnownSha256AndExactAssetName() throws Exception {
        Path file = directory.resolve("hash.txt");
        Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));
        String hash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        assertEquals(hash, ForkUpdater.sha256(file));
        assertEquals(hash, ForkUpdater.checksumFor(hash + " *AbilityWar-1.1.1.jar\r\n", "AbilityWar-1.1.1.jar"));
        assertThrows(IOException.class, () -> ForkUpdater.checksumFor(hash + " AbilityWar-1.1.1-plain.jar", "AbilityWar-1.1.1.jar"));
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    private byte[] jar(String version, String apiVersion, boolean modern) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("paperweight-mappings-namespace", "mojang");
        try (JarOutputStream jar = new JarOutputStream(bytes, manifest)) {
            entry(jar, "plugin.yml", "name: AbilityWar\nmain: daybreak.abilitywar.AbilityWar\nversion: " + version + "\napi-version: '" + apiVersion + "'\n");
            if (modern) entry(jar, "abilitywar-paper.properties", "version=1.21.11\n");
            entry(jar, "daybreak/abilitywar/AbilityWar.class", "fixture");
            entry(jar, "daybreak/abilitywar/utils/base/minecraft/nms/paper_1_21_11/NMSImpl.class", "fixture");
        }
        return bytes.toByteArray();
    }

    private void entry(JarOutputStream jar, String name, String value) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        jar.write(value.getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
    }

    private class Fixture {
        final byte[] original = "installed plugin remains unchanged".getBytes(StandardCharsets.UTF_8);
        final Path installed = directory.resolve("My AbilityWar.jar");
        final Path queued = directory.resolve("update/My AbilityWar.jar");
        final JsonObject release = new JsonObject();
        final JsonObject jarAsset;
        final ForkUpdater updater;
        byte[] jar;
        String checksums;
        int requests;
        boolean failNetwork;

        Fixture(String version, String apiVersion) throws IOException {
            Files.write(installed, original);
            jar = jar(version, apiVersion, true);
            release.addProperty("draft", false);
            release.addProperty("prerelease", false);
            release.addProperty("tag_name", "v" + version);
            release.addProperty("name", "AbilityWars release title is not a version");
            JsonArray assets = new JsonArray();
            assets.add(asset("SHA256SUMS.txt", version));
            jarAsset = asset("AbilityWar-" + version + ".jar", version);
            assets.add(jarAsset);
            release.add("assets", assets);
            updateDigest();
            updater = new ForkUpdater("1.1.0", installed.toFile(), queued.getParent().toFile(), Logger.getAnonymousLogger()) {
                @Override InputStream open(URL url) throws IOException {
                    requests++;
                    if (failNetwork) throw new IOException("HTTP 503 fixture");
                    if (url.getHost().equals("api.github.com")) return new ByteArrayInputStream(release.toString().getBytes(StandardCharsets.UTF_8));
                    return new ByteArrayInputStream(url.getPath().endsWith("SHA256SUMS.txt") ? checksums.getBytes(StandardCharsets.UTF_8) : jar);
                }
            };
        }

        void updateDigest() throws IOException {
            Path digestFile = directory.resolve("digest-fixture.jar");
            Files.write(digestFile, jar);
            checksums = ForkUpdater.sha256(digestFile) + "  " + jarAsset.get("name").getAsString() + "\n";
            jarAsset.addProperty("size", jar.length);
        }

        JsonObject asset(String name, String version) {
            JsonObject asset = new JsonObject();
            asset.addProperty("name", name);
            asset.addProperty("browser_download_url", "https://github.com/milkchan128/AbilityWars/releases/download/v" + version + "/" + name);
            return asset;
        }

        void assertUnchanged() throws IOException {
            assertArrayEquals(original, Files.readAllBytes(installed));
            assertFalse(Files.exists(queued));
            assertFalse(Files.exists(directory.resolve("AbilityWar/backups/AbilityWar-1.1.0.jar")));
            assertNoTemporaryFiles();
        }
    }
}
