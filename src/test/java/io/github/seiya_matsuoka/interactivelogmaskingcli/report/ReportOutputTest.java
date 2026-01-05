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

  @Test
  void creates_report_file_in_outputBase_when_realRun() throws Exception {

    Path input = tempDir.resolve("input");
    Files.createDirectories(input);
    Files.writeString(input.resolve("app.log"), "mail=a@example.com\n", StandardCharsets.UTF_8);

    Path out = tempDir.resolve("out");

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

    Map<?, ?> summary = (Map<?, ?>) root.get("summary");
    assertNotNull(summary);
    assertEquals(1, ((Number) summary.get("files")).intValue());
    assertEquals(1L, ((Number) summary.get("totalCount")).longValue());
  }

  @Test
  void creates_report_file_in_outputBase_when_dryRun() throws Exception {

    Path inputFile = tempDir.resolve("single.log");
    Files.writeString(inputFile, "token=abc token=def\n", StandardCharsets.UTF_8);

    Path out = tempDir.resolve("out");

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
    service.run(RunPlan.ofConfig(inputFile, out, config, "", true));

    List<Path> reports =
        Files.list(out)
            .filter(p -> p.getFileName().toString().startsWith("report-"))
            .filter(p -> p.getFileName().toString().endsWith(".json"))
            .collect(Collectors.toList());

    assertEquals(1, reports.size());

    ObjectMapper mapper = new ObjectMapper();
    Map<?, ?> root = mapper.readValue(reports.get(0).toFile(), Map.class);

    assertEquals(true, (Boolean) root.get("dryRun"));

    Map<?, ?> rulesSource = (Map<?, ?>) root.get("rulesSource");
    assertNotNull(rulesSource);
    assertEquals("interactive", rulesSource.get("type"));
  }
}
