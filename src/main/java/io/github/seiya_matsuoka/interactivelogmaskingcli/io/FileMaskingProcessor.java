package io.github.seiya_matsuoka.interactivelogmaskingcli.io;

import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskEngine;
import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskEngineResult;
import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskRule;
import io.github.seiya_matsuoka.interactivelogmaskingcli.report.MaskRunReport;
import io.github.seiya_matsuoka.interactivelogmaskingcli.report.MaskedFileReport;
import io.github.seiya_matsuoka.interactivelogmaskingcli.util.PathUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 入力（単一ファイル/ディレクトリ）を処理し、out 配下へマスク済みファイルを出力する。
 *
 * <p>ディレクトリ入力の場合、inputBase からの相対パスを保ったまま out 配下へ出力する。
 */
public class FileMaskingProcessor {

  private final InputFileCollector collector;
  private final MaskEngine engine;

  public FileMaskingProcessor() {
    this(new InputFileCollector(), new MaskEngine());
  }

  public FileMaskingProcessor(InputFileCollector collector, MaskEngine engine) {
    this.collector = collector;
    this.engine = engine;
  }

  /**
   * 入力を処理し、out 配下にマスク済みファイルを生成する。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @param outputBase 出力ベース（例: out/）
   * @param rules コンパイル済みルール
   * @param suffix 出力ファイル名サフィックス（例: "_masked"、空なら無し）
   */
  public MaskRunReport process(Path inputPath, Path outputBase, List<MaskRule> rules, String suffix)
      throws IOException {
    Objects.requireNonNull(inputPath, "inputPath");
    Objects.requireNonNull(outputBase, "outputBase");
    Objects.requireNonNull(rules, "rules");

    List<InputFileRef> inputs = collector.collect(inputPath);

    List<MaskedFileReport> fileReports = new ArrayList<>();
    Map<String, Long> totalPerRule = new HashMap<>();
    long totalCount = 0;

    for (InputFileRef in : inputs) {
      Path outFile = PathUtil.toOutputPath(outputBase, in.relative(), suffix);

      MaskedFileReport r = processSingleFile(in.file(), outFile, rules);
      fileReports.add(r);

      mergeCounts(totalPerRule, r.countsPerRule());
      totalCount += r.totalCount();
    }

    return new MaskRunReport(
        inputPath, outputBase, List.copyOf(fileReports), Map.copyOf(totalPerRule), totalCount);
  }

  private MaskedFileReport processSingleFile(Path inputFile, Path outputFile, List<MaskRule> rules)
      throws IOException {

    if (outputFile.getParent() != null) {
      Files.createDirectories(outputFile.getParent());
    }

    Map<String, Long> countsPerRule = new HashMap<>();
    long totalCount = 0;

    try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
        BufferedWriter writer =
            Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

      String line;
      while ((line = reader.readLine()) != null) {
        MaskEngineResult r = engine.maskText(line, rules, false);

        mergeCounts(countsPerRule, r.countsPerRule());
        totalCount += r.totalCount();

        writer.write(r.outputText());
        writer.newLine();
      }
    }

    return new MaskedFileReport(inputFile, outputFile, Map.copyOf(countsPerRule), totalCount);
  }

  private static void mergeCounts(Map<String, Long> dest, Map<String, Long> src) {
    if (src == null) return;
    for (var e : src.entrySet()) {
      dest.merge(e.getKey(), e.getValue(), Long::sum);
    }
  }
}
