package io.github.seiya_matsuoka.interactivelogmaskingcli.service;

import static org.junit.jupiter.api.Assertions.*;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleFlag;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidationException;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RulesRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileMaskingServiceTest {

  @TempDir Path tempDir;

  // - rules.json 読み込み → validate → compile → file->out の一連が service で通ることを確認
  // - out 配下の構造維持 + suffix 付与が効くことを確認
  @Test
  void maskToOut_uses_rules_json_and_creates_out_files() throws Exception {

    // input 構造を用意
    Path inputBase = tempDir.resolve("input");
    Path nested = inputBase.resolve("a/b");
    Files.createDirectories(nested);
    Files.writeString(nested.resolve("app.log"), "mail=a@example.com\n", StandardCharsets.UTF_8);

    // out
    Path outBase = tempDir.resolve("out");

    // rules.json を作成（Repositoryのsaveを使って実際に近い形で準備）
    Path rulesPath = tempDir.resolve("rules.json");
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
                        .setFlags(List.of(RuleFlag.CASE_INSENSITIVE))
                    // enabled省略（null）でも有効扱い
                    ));
    new RulesRepository().save(rulesPath, config);

    FileMaskingService service = new FileMaskingService();
    var report = service.maskToOut(inputBase, outBase, rulesPath, "_masked");

    // out/a/b/app_masked.log ができていること
    Path expectedOut = outBase.resolve("a/b/app_masked.log");
    assertTrue(Files.exists(expectedOut));

    // 内容がマスクされていること
    String outText = Files.readString(expectedOut, StandardCharsets.UTF_8);
    assertTrue(outText.contains("[MASKED_EMAIL]"));

    // 件数の最低限チェック（1行に1件）
    assertEquals(1L, report.totalCount());
    assertEquals(1L, report.totalPerRule().get("email"));
  }

  // validator が service 経由で動いており、invalid pattern を弾けることを確認
  @Test
  void maskToOut_throws_when_config_invalid() throws Exception {

    Path inputFile = tempDir.resolve("single.log");
    Files.writeString(inputFile, "x\n", StandardCharsets.UTF_8);

    Path outBase = tempDir.resolve("out");

    // invalid pattern（正規表現として不正）
    MaskRulesConfig invalid =
        new MaskRulesConfig()
            .setVersion(1)
            .setRules(
                List.of(
                    new MaskRuleConfig()
                        .setId("bad")
                        .setName("BadPattern")
                        .setPattern("(") // invalid
                        .setReplacement("[X]")));

    FileMaskingService service = new FileMaskingService();

    assertThrows(
        RuleValidationException.class,
        () -> service.maskToOut(inputFile, outBase, invalid, "_masked"));
  }
}
