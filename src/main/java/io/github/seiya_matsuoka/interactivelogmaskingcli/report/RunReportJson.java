package io.github.seiya_matsuoka.interactivelogmaskingcli.report;

import java.util.List;
import java.util.Map;

/**
 * report-*.json の出力用DTO。
 *
 * <p>ログ本文や置換前/後のサンプルは含めない。
 */
public class RunReportJson {

  public int schemaVersion;
  public String generatedAt;
  public long durationMs;
  public boolean dryRun;
  public String inputPath;
  public String outputBase;
  public String suffix;

  public RulesSourceJson rulesSource;

  public SummaryJson summary;

  /** ルールID -> メタ情報（表示名など） */
  public Map<String, RuleMetaJson> ruleMeta;

  /** ファイル別レポート */
  public List<FileReportJson> files;

  public static class RulesSourceJson {
    /** "path" or "interactive" */
    public String type;

    /** type=path のときのみセット */
    public String rulesPath;
  }

  public static class SummaryJson {
    public int files;
    public long totalCount;

    /** ルールID -> 合計件数 */
    public Map<String, Long> totalPerRule;
  }

  public static class RuleMetaJson {
    public String name;
  }

  public static class FileReportJson {
    public String inputFile;
    public String outputFile;
    public long totalCount;

    /** ルールID -> 件数 */
    public Map<String, Long> countsPerRule;
  }
}
