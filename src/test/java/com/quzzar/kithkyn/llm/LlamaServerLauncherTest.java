package com.quzzar.kithkyn.llm;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LlamaServerLauncherTest {
  @TempDir Path directory;

  @Test void extractsWindowsRuntimeWithJava() throws Exception {
    Path archive = zip("runtime/llama-server.exe");
    Path destination = directory.resolve("unpacked");
    LlamaServerLauncher.unpack(archive, destination);
    assertEquals("fixture", Files.readString(destination.resolve("runtime/llama-server.exe")));
  }

  @Test void rejectsZipPathsOutsideTheRuntimeDirectory() throws Exception {
    Path archive = zip("../escaped.exe");
    assertThrows(IOException.class, () -> LlamaServerLauncher.unpack(archive, directory.resolve("unpacked")));
    assertFalse(Files.exists(directory.resolve("escaped.exe")));
  }

  @Test void selectsOnlySupported64BitPlatforms() {
    assertTrue(LlamaServerLauncher.assetFor("Windows 11", "amd64").endsWith("win-cpu-x64.zip"));
    assertTrue(LlamaServerLauncher.assetFor("Mac OS X", "aarch64").endsWith("macos-arm64.tar.gz"));
    assertTrue(LlamaServerLauncher.assetFor("Linux", "x86_64").endsWith("ubuntu-x64.tar.gz"));
    assertNull(LlamaServerLauncher.assetFor("Linux", "riscv64"));
    assertNull(LlamaServerLauncher.assetFor("Windows", "x86"));
  }

  private Path zip(String entry) throws IOException {
    Path archive = directory.resolve("runtime.zip");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      output.putNextEntry(new ZipEntry(entry));
      output.write("fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    return archive;
  }
}
