package io.github.seiya_matsuoka.interactivelogmaskingcli.io;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InputFileCollectorTest {

  @TempDir Path tempDir;

  // ディレクトリ入力で、relative が inputBase からの相対になることを確認
  @Test
  void collect_directory_returns_relative_paths() throws Exception {
    Path inputBase = tempDir.resolve("input");
    Path nested = inputBase.resolve("a/b");
    Files.createDirectories(nested);

    Files.writeString(nested.resolve("app.log"), "x\n", StandardCharsets.UTF_8);
    Files.writeString(inputBase.resolve("root.log"), "y\n", StandardCharsets.UTF_8);

    InputFileCollector collector = new InputFileCollector();
    List<InputFileRef> refs = collector.collect(inputBase);

    assertEquals(2, refs.size());
    assertTrue(
        refs.stream()
            .anyMatch(r -> r.relative().toString().replace('\\', '/').equals("a/b/app.log")));
    assertTrue(
        refs.stream().anyMatch(r -> r.relative().toString().replace('\\', '/').equals("root.log")));
  }
}
