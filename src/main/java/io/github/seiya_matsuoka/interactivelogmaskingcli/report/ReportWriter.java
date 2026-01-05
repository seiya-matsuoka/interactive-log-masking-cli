package io.github.seiya_matsuoka.interactivelogmaskingcli.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.service.RunPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 実行レポート（report-*.json）を out/ に出力する。 */
public class ReportWriter {

  private static final int SCHEMA_VERSION = 1;
  private static final DateTimeFormatter FILE_TS =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

  private final ObjectMapper mapper;

  public ReportWriter() {
    this.mapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  }

  /**
   * report-YYYYMMDD-HHMMSS-SSS.json を out/ に生成する。
   *
   * @param plan 実行計画
   * @param config ルール設定（検証済み）
   * @param runReport 実行結果
   * @param durationMs 実行時間（ms）
   * @return 生成したレポートファイルのパス
   */
  public Path write(RunPlan plan, MaskRulesConfig config, MaskRunReport runReport, long durationMs)
      throws IOException {

    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(runReport, "runReport");

    // report を出すために outputBase は必ず用意する（dryRun でも同様）
    Files.createDirectories(plan.outputBase());

    RunReportJson dto = buildDto(plan, config, runReport, durationMs);

    Path path = createUniqueReportPath(plan.outputBase());
    mapper.writeValue(path.toFile(), dto);

    return path;
  }

  private RunReportJson buildDto(
      RunPlan plan, MaskRulesConfig config, MaskRunReport runReport, long durationMs) {

    RunReportJson dto = new RunReportJson();

    dto.schemaVersion = SCHEMA_VERSION;
    dto.generatedAt = OffsetDateTime.now().toString();
    dto.durationMs = durationMs;
    dto.dryRun = plan.dryRun();
    dto.inputPath = plan.inputPath().toString();
    dto.outputBase = plan.outputBase().toString();
    dto.suffix = plan.suffix();
    dto.rulesSource = buildRulesSource(plan);
    dto.summary = buildSummary(runReport);
    dto.ruleMeta = buildRuleMeta(config);
    dto.files = buildFiles(runReport);

    return dto;
  }

  private RunReportJson.RulesSourceJson buildRulesSource(RunPlan plan) {

    RunReportJson.RulesSourceJson src = new RunReportJson.RulesSourceJson();

    if (plan.rulesPath() != null) {
      src.type = "path";
      src.rulesPath = plan.rulesPath().toString();
    } else {
      src.type = "interactive";
      src.rulesPath = null;
    }
    return src;
  }

  private RunReportJson.SummaryJson buildSummary(MaskRunReport runReport) {

    RunReportJson.SummaryJson s = new RunReportJson.SummaryJson();

    s.files = runReport.files().size();
    s.totalCount = runReport.totalCount();
    s.totalPerRule = new TreeMap<>(runReport.totalPerRule());
    return s;
  }

  private Map<String, RunReportJson.RuleMetaJson> buildRuleMeta(MaskRulesConfig config) {

    Map<String, RunReportJson.RuleMetaJson> map = new TreeMap<>();

    if (config.getRules() == null) {
      return map;
    }

    for (MaskRuleConfig r : config.getRules()) {
      if (r == null || r.getId() == null) continue;

      RunReportJson.RuleMetaJson meta = new RunReportJson.RuleMetaJson();
      meta.name = (r.getName() == null || r.getName().isBlank()) ? r.getId() : r.getName();

      map.put(r.getId(), meta);
    }
    return map;
  }

  private List<RunReportJson.FileReportJson> buildFiles(MaskRunReport runReport) {

    List<RunReportJson.FileReportJson> list = new ArrayList<>();

    for (MaskedFileReport f : runReport.files()) {
      RunReportJson.FileReportJson x = new RunReportJson.FileReportJson();
      x.inputFile = f.inputFile().toString();
      x.outputFile = f.outputFile().toString();
      x.totalCount = f.totalCount();
      x.countsPerRule = new TreeMap<>(f.countsPerRule());
      list.add(x);
    }
    return list;
  }

  private Path createUniqueReportPath(Path outputBase) {

    String ts = FILE_TS.format(LocalDateTime.now());
    Path base = outputBase.resolve("report-" + ts + ".json");

    if (!Files.exists(base)) {
      return base;
    }

    // 同一ミリ秒などで衝突した場合（連番）
    for (int i = 1; i <= 999; i++) {
      String suffix = String.format("-%03d", i);
      Path p = outputBase.resolve("report-" + ts + suffix + ".json");
      if (!Files.exists(p)) {
        return p;
      }
    }

    // 念のため保険で最後はランダムに逃がす
    return outputBase.resolve("report-" + ts + "-" + UUID.randomUUID() + ".json");
  }
}
