package io.github.seiya_matsuoka.interactivelogmaskingcli.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MaskEngineTest {

  // 置換され、ルール別/合計の件数が集計されることを確認
  @Test
  void maskText_replaces_and_counts() {

    MaskRule email =
        new MaskRule(
            "email",
            "Email",
            true,
            Pattern.compile(
                "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", Pattern.CASE_INSENSITIVE),
            "[MASKED_EMAIL]");

    MaskEngine engine = new MaskEngine();
    MaskEngineResult r = engine.maskText("a@example.com b@example.com", List.of(email), false);

    assertEquals("[MASKED_EMAIL] [MASKED_EMAIL]", r.outputText());
    assertEquals(2L, r.totalCount());
    assertEquals(2L, r.countsPerRule().get("email"));
  }

  // dryRunでも件数は集計されるが、返す文字列は入力と同一であることを確認
  @Test
  void maskText_dryRun_keeps_output_but_counts_like_real_run() {

    MaskRule token =
        new MaskRule(
            "token", "Token", true, Pattern.compile("token=[A-Za-z0-9]+"), "token=[MASKED]");

    MaskEngine engine = new MaskEngine();
    MaskEngineResult r = engine.maskText("token=abc token=def", List.of(token), true);

    assertEquals("token=abc token=def", r.outputText());
    assertEquals(2L, r.totalCount());
    assertEquals(2L, r.countsPerRule().get("token"));
  }
}
