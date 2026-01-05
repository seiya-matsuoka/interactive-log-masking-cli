package io.github.seiya_matsuoka.interactivelogmaskingcli.report;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.service.FileMaskingService;
import io.github.seiya_matsuoka.interactivelogmaskingcli.service.RunPlan;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportOutputTest {

  @TempDir Path tempDir;

  // service.run を呼ぶと、out/ 直下に report-*.json が1つ生成され、最小限のフィールドが入っていることを検証
  @Test
  void creates_report_file_in_outputBase_when_realRun() throws Exception {

    // 入力用ディレクトリを用意し、ログファイルを1つ作る
    Path input = tempDir.resolve("input");
    Files.createDirectories(input);
    Files.writeString(input.resolve("app.log"), "mail=a@example.com\n", StandardCharsets.UTF_8);

    // 出力先（out/）は ReportWriter が必要に応じて作成する想定
    Path out = tempDir.resolve("out");

    // ルール設定（メールアドレスをマスク）
    MaskRulesConfig config =
        new MaskRulesConfig()
            .setVersion(1)
            .setRules(
                List.of(
                    new MaskRuleConfig()
                        .setId("email")
                        .setName("Email")
                        .setPattern("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
                        .setReplacement("[MASKED_EMAIL]")));

    FileMaskingService service = new FileMaskingService();
    service.run(RunPlan.ofConfig(input, out, config, "_masked", false));

    // out/ 直下に report-*.json が1つ生成されていることを確認
    List<Path> reports =
        Files.list(out)
            .filter(p -> p.getFileName().toString().startsWith("report-"))
            .filter(p -> p.getFileName().toString().endsWith(".json"))
            .collect(Collectors.toList());

    assertEquals(1, reports.size());

    // JSONの最低限の構造確認（String/Mapベースで安全に読む）
    ObjectMapper mapper = new ObjectMapper();
    Map<?, ?> root = mapper.readValue(reports.get(0).toFile(), Map.class);

    assertEquals(1, ((Number) root.get("schemaVersion")).intValue());
    assertEquals(false, (Boolean) root.get("dryRun"));

    // summary の存在と、ファイル数/件数が期待通りであること
    Map<?, ?> summary = (Map<?, ?>) root.get("summary");
    assertNotNull(summary);
    assertEquals(1, ((Number) summary.get("files")).intValue());
    assertEquals(1L, ((Number) summary.get("totalCount")).longValue());
  }

  // dryRun の場合、マスク済みファイルは生成されなくても、out/ 直下に report-*.json が1つ生成され、dryRun=trueが記録されることを検証
  @Test
  void creates_report_file_in_outputBase_when_dryRun() throws Exception {

    // 入力ファイル（単一ファイル）を用意
    Path inputFile = tempDir.resolve("single.log");
    Files.writeString(inputFile, "token=abc token=def\n", StandardCharsets.UTF_8);

    // 出力先
    Path out = tempDir.resolve("out");

    // ルール設定（token=*** をマスク）
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

    // 実行（dryRun=true）
    FileMaskingService service = new FileMaskingService();
    service.run(RunPlan.ofConfig(inputFile, out, config, "", true));

    // out/ 直下に report-*.json が1つ生成されていることを確認
    List<Path> reports =
        Files.list(out)
            .filter(p -> p.getFileName().toString().startsWith("report-"))
            .filter(p -> p.getFileName().toString().endsWith(".json"))
            .collect(Collectors.toList());

    assertEquals(1, reports.size());

    // JSON内容確認：dryRun=true が記録されていること
    ObjectMapper mapper = new ObjectMapper();
    Map<?, ?> root = mapper.readValue(reports.get(0).toFile(), Map.class);

    assertEquals(true, (Boolean) root.get("dryRun"));

    // ルール指定が「対話由来（interactive）」として記録されていること
    Map<?, ?> rulesSource = (Map<?, ?>) root.get("rulesSource");
    assertNotNull(rulesSource);
    assertEquals("interactive", rulesSource.get("type"));
  }
}
