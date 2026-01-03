package io.github.seiya_matsuoka.interactivelogmaskingcli.service;

import static org.junit.jupiter.api.Assertions.*;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileMaskingServiceDryRunTest {

  @TempDir Path tempDir;

  // dryRun=true で out 配下にファイルが作られないこと（ただし件数は集計されること）
  @Test
  void run_dryRun_does_not_create_output_files_but_counts() throws Exception {

    Path inputBase = tempDir.resolve("input");
    Path nested = inputBase.resolve("a/b");
    Files.createDirectories(nested);
    Files.writeString(nested.resolve("app.log"), "mail=a@example.com\n", StandardCharsets.UTF_8);

    // out は存在しても良い（リポジトリの out/ を模擬）
    Path outBase = tempDir.resolve("out");
    Files.createDirectories(outBase);

    MaskRulesConfig config =
        new MaskRulesConfig()
            .setVersion(1)
            .setRules(
                List.of(
                    new MaskRuleConfig()
                        .setId("email")
                        .setName("Email")
                        .setPattern("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
                        .setReplacement("[MASKED_EMAIL]")
                        .setFlags(List.of(RuleFlag.CASE_INSENSITIVE))));

    FileMaskingService service = new FileMaskingService();

    RunPlan plan = RunPlan.ofConfig(inputBase, outBase, config, "_masked", true);
    var report = service.run(plan);

    // 件数は集計される
    assertEquals(1L, report.totalCount());
    assertEquals(1L, report.totalPerRule().get("email"));

    // 予定パスは計算されるが、実ファイルは作られない
    Path expectedOut = outBase.resolve("a/b/app_masked.log");
    assertFalse(Files.exists(expectedOut));
  }

  // dryRun と本実行で件数が一致すること
  @Test
  void run_dryRun_and_realRun_have_same_counts() throws Exception {

    Path inputFile = tempDir.resolve("single.log");
    Files.writeString(inputFile, "token=abc token=def\n", StandardCharsets.UTF_8);

    Path outBase = tempDir.resolve("out");
    Files.createDirectories(outBase);

    MaskRulesConfig config =
        new MaskRulesConfig()
            .setVersion(1)
            .setRules(
                List.of(
                    new MaskRuleConfig()
                        .setId("token")
                        .setName("Token")
                        .setPattern("token=[A-Za-z0-9]+")
                        .setReplacement("token=[MASKED]")));

    FileMaskingService service = new FileMaskingService();

    var dry = service.run(RunPlan.ofConfig(inputFile, outBase, config, "", true));
    var real = service.run(RunPlan.ofConfig(inputFile, outBase, config, "", false));

    assertEquals(real.totalCount(), dry.totalCount());
    assertEquals(real.totalPerRule().get("token"), dry.totalPerRule().get("token"));
  }
}
