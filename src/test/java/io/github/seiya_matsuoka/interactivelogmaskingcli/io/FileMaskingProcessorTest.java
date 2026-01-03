package io.github.seiya_matsuoka.interactivelogmaskingcli.io;

import static org.junit.jupiter.api.Assertions.*;

import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskRule;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileMaskingProcessorTest {

  @TempDir Path tempDir;

  // ディレクトリ再帰で処理され、構造維持 + suffix 付きで out に出力されることを確認
  @Test
  void process_directory_keeps_structure_and_adds_suffix() throws Exception {
    Path inputBase = tempDir.resolve("input");
    Path nested = inputBase.resolve("a/b");
    Files.createDirectories(nested);

    Path inFile = nested.resolve("app.log");
    Files.writeString(inFile, "mail=a@example.com\n", StandardCharsets.UTF_8);

    Path outBase = tempDir.resolve("out");

    MaskRule email =
        new MaskRule(
            "email",
            "Email",
            true,
            Pattern.compile(
                "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", Pattern.CASE_INSENSITIVE),
            "[MASKED_EMAIL]");

    FileMaskingProcessor processor = new FileMaskingProcessor();
    var report = processor.process(inputBase, outBase, List.of(email), "_masked");

    Path expectedOut = outBase.resolve("a/b/app_masked.log");
    assertTrue(Files.exists(expectedOut));

    String outText = Files.readString(expectedOut, StandardCharsets.UTF_8);
    assertTrue(outText.contains("[MASKED_EMAIL]"));

    assertEquals(1, report.files().size());
    assertEquals(1L, report.totalCount());
    assertEquals(1L, report.totalPerRule().get("email"));
  }

  // 単一ファイル入力の場合、outBase直下に出力されることを確認
  @Test
  void process_single_file_writes_to_out_base() throws Exception {
    Path inFile = tempDir.resolve("single.log");
    Files.writeString(inFile, "token=abc\n", StandardCharsets.UTF_8);

    Path outBase = tempDir.resolve("out");

    MaskRule token =
        new MaskRule(
            "token", "Token", true, Pattern.compile("token=[A-Za-z0-9]+"), "token=[MASKED]");

    FileMaskingProcessor processor = new FileMaskingProcessor();
    var report = processor.process(inFile, outBase, List.of(token), "");

    Path expectedOut = outBase.resolve("single.log");
    assertTrue(Files.exists(expectedOut));

    String outText = Files.readString(expectedOut, StandardCharsets.UTF_8);
    assertTrue(outText.contains("token=[MASKED]"));

    assertEquals(1, report.files().size());
    assertEquals(1L, report.totalCount());
  }
}
