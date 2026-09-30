package com.quzzar.kithkyn.llm;

import com.quzzar.kithkyn.Kithkyn;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** Resumable, version-pinned downloads that publish only size- and SHA-256-verified files. */
final class VerifiedDownload {
  private VerifiedDownload() { }

  /** Retains interrupted partial transfers for the next attempt and replaces corrupt cache entries. */
  static void ensure(HttpClient client, URI uri, Path target, long size, String sha256)
      throws IOException, InterruptedException {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Download cancelled");
    if (valid(target, size, sha256)) return;
    if (Files.exists(target)) {
      Kithkyn.LOGGER.warn("Discarding corrupt cached download {}", target);
      Files.delete(target);
    }
    Files.createDirectories(target.toAbsolutePath().getParent());
    Path partial = target.resolveSibling(target.getFileName() + ".partial");
    for (int attempt = 0; attempt < 2; attempt++) {
      if (valid(partial, size, sha256)) {
        publish(partial, target);
        return;
      }
      long offset = Files.exists(partial) ? Files.size(partial) : 0;
      if (offset >= size) {
        Files.delete(partial);
        offset = 0;
      }
      Kithkyn.LOGGER.info("Downloading {} ({} MB) into {}{}", target.getFileName(),
          size / 1_000_000, target.toAbsolutePath().getParent(),
          offset == 0 ? "" : "; resuming at byte " + offset);
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(30)).GET();
      if (offset > 0) request.header("Range", "bytes=" + offset + "-");
      HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream input = response.body()) {
        boolean append = response.statusCode() == 206;
        if (append) {
          String expectedRange = "bytes " + offset + "-" + (size - 1) + "/" + size;
          if (!response.headers().firstValue("Content-Range").orElse("").equals(expectedRange)) {
            throw new IOException("Unexpected Content-Range while downloading " + target.getFileName());
          }
        } else if (response.statusCode() != 200) {
          throw new IOException("Download failed (HTTP " + response.statusCode() + "): " + target.getFileName());
        }
        if (append) {
          try (var output = Files.newOutputStream(partial, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            input.transferTo(output);
          }
        } else {
          Files.copy(input, partial, StandardCopyOption.REPLACE_EXISTING);
        }
      }
      if (valid(partial, size, sha256)) {
        publish(partial, target);
        return;
      }
      long received = Files.size(partial);
      if (received < size) throw new IOException("Incomplete download of " + target.getFileName()
          + " (" + received + " of " + size + " bytes); the next start will resume it");
      Files.delete(partial);
      Kithkyn.LOGGER.warn("Checksum mismatch for {}; fetching a fresh copy", target.getFileName());
    }
    throw new IOException("SHA-256 verification failed twice for " + target.getFileName());
  }

  static boolean valid(Path path, long size, String expected) throws IOException {
    return Files.isRegularFile(path) && Files.size(path) == size && sha256(path).equals(expected);
  }

  static String sha256(Path path) throws IOException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("Java must provide SHA-256", impossible);
    }
    try (InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[1024 * 1024];
      int count;
      while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void publish(Path partial, Path target) throws IOException {
    try {
      Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
