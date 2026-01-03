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
 * <ul>
 *   <li>入力パスから対象ファイルを列挙する（{@link InputFileCollector}）
 *   <li>out 配下の出力パスを生成する（構造維持 + サフィックス）
 *   <li>各ファイルを「行単位」で読み、{@link MaskEngine} を使ってマスキングして書き出す
 *   <li>置換件数を集計し、レポートとして返す
 * </ul>
 */
public class FileMaskingProcessor {

  private final InputFileCollector collector;
  private final MaskEngine engine;

  /** 既定の collector / engine を使用するコンストラクタ。 */
  public FileMaskingProcessor() {
    this(new InputFileCollector(), new MaskEngine());
  }

  /**
   * DI（テスト等）用コンストラクタ。
   *
   * @param collector 入力ファイル列挙
   * @param engine マスキングコア
   */
  public FileMaskingProcessor(InputFileCollector collector, MaskEngine engine) {
    this.collector = collector;
    this.engine = engine;
  }

  /**
   * 入力を処理し、out 配下にマスク済みファイルを生成する。
   *
   * <p>入力がディレクトリの場合は、inputBase からの相対パスを保持して out 配下へ出力する。
   *
   * <p>入力が単一ファイルの場合は、outBase 直下に出力する。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @param outputBase 出力ベース（例: out/）
   * @param rules コンパイル済みルール
   * @param suffix 出力ファイル名サフィックス（例: "_masked"、空なら無し）
   * @return 実行結果レポート（ファイル別結果 + 集計）
   * @throws IOException 入出力エラー
   */
  public MaskRunReport process(Path inputPath, Path outputBase, List<MaskRule> rules, String suffix)
      throws IOException {
    Objects.requireNonNull(inputPath, "inputPath");
    Objects.requireNonNull(outputBase, "outputBase");
    Objects.requireNonNull(rules, "rules");

    // 1) 入力パスから、処理対象ファイルを収集（単一 or ディレクトリ再帰）
    List<InputFileRef> inputs = collector.collect(inputPath);

    // 2) ファイルごとのレポートと集計を構築
    List<MaskedFileReport> fileReports = new ArrayList<>();
    Map<String, Long> totalPerRule = new HashMap<>();
    long totalCount = 0;

    // 3) 各ファイルを処理：入力 →（構造維持+suffix）→ out へ書き出し
    for (InputFileRef in : inputs) {
      // out 配下の出力パスを生成（relative により構造維持）
      Path outFile = PathUtil.toOutputPath(outputBase, in.relative(), suffix);

      // 実体ファイルを処理して outFile へ出力
      MaskedFileReport r = processSingleFile(in.file(), outFile, rules);
      fileReports.add(r);

      // ルール別の件数を全体集計へ加算
      mergeCounts(totalPerRule, r.countsPerRule());
      totalCount += r.totalCount();
    }

    return new MaskRunReport(
        inputPath, outputBase, List.copyOf(fileReports), Map.copyOf(totalPerRule), totalCount);
  }

  /**
   * 1ファイルを読み込み、マスキングして出力ファイルへ書き出す。
   *
   * <p>処理は行単位で行う。
   *
   * <p>改行コードは writer.newLine() で環境依存になるが、許容する。
   *
   * @param inputFile 入力ファイル
   * @param outputFile 出力ファイル（必要に応じて親ディレクトリを作成）
   * @param rules コンパイル済みルール
   * @return ファイル単位の処理結果
   */
  private MaskedFileReport processSingleFile(Path inputFile, Path outputFile, List<MaskRule> rules)
      throws IOException {

    // 出力先ディレクトリを事前に作成（構造維持でネストする可能性がある）
    if (outputFile.getParent() != null) {
      Files.createDirectories(outputFile.getParent());
    }

    Map<String, Long> countsPerRule = new HashMap<>();
    long totalCount = 0;

    // 入力を UTF-8 で読み、出力も UTF-8 で書き出す
    // 既存ファイルがあれば上書き（TRUNCATE_EXISTING）
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

        // 置換件数をファイル単位で集計
        mergeCounts(countsPerRule, r.countsPerRule());
        totalCount += r.totalCount();

        // 置換後の行を書き込み、改行を付与
        writer.write(r.outputText());
        writer.newLine();
      }
    }

    return new MaskedFileReport(inputFile, outputFile, Map.copyOf(countsPerRule), totalCount);
  }

  /**
   * ルール別件数のMapを dest に加算してマージする。
   *
   * <p>countsPerRule は「ルールID -> 件数」。同一IDがあれば加算する。
   */
  private static void mergeCounts(Map<String, Long> dest, Map<String, Long> src) {
    if (src == null) return;
    for (var e : src.entrySet()) {
      dest.merge(e.getKey(), e.getValue(), Long::sum);
    }
  }
}
