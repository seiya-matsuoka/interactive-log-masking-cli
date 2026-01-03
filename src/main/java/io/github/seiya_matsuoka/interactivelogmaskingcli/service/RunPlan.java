package io.github.seiya_matsuoka.interactivelogmaskingcli.service;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 実行計画（RunPlan）。
 *
 * <p>CLI（対話/引数）がユーザー入力をまとめ、Serviceへ渡すためのDTO。
 *
 * <p>ルール指定は「rulesPath」か「config」のどちらか一方を使う（両方同時/両方nullは禁止）。
 *
 * @param inputPath 入力（ファイル or ディレクトリ）
 * @param outputBase 出力ベース（例: out/）
 * @param rulesPath ルールJSONのパス（任意）
 * @param config ルール設定（任意）
 * @param suffix 出力ファイル名サフィックス（例: "_masked"。nullは空扱い）
 * @param dryRun true: 件数集計のみ（ファイル生成しない）
 */
public record RunPlan(
    Path inputPath,
    Path outputBase,
    Path rulesPath,
    MaskRulesConfig config,
    String suffix,
    boolean dryRun) {

  public RunPlan {
    Objects.requireNonNull(inputPath, "inputPath");
    Objects.requireNonNull(outputBase, "outputBase");

    // suffix は null を許容するが、扱いやすいよう空文字へ寄せる
    suffix = (suffix == null) ? "" : suffix;

    boolean hasRulesPath = (rulesPath != null);
    boolean hasConfig = (config != null);

    if (hasRulesPath == hasConfig) {
      // 両方true または 両方false は禁止
      throw new IllegalArgumentException("rulesPath または config のどちらか一方だけを指定してください。");
    }
  }

  /** rulesPath を使う RunPlan を生成する（JSON読み込み込みの場合の入口）。 */
  public static RunPlan ofRulesPath(
      Path inputPath, Path outputBase, Path rulesPath, String suffix, boolean dryRun) {
    Objects.requireNonNull(rulesPath, "rulesPath");
    return new RunPlan(inputPath, outputBase, rulesPath, null, suffix, dryRun);
  }

  /** config を使う RunPlan を生成する（対話で作った設定を渡す場合の入口）。 */
  public static RunPlan ofConfig(
      Path inputPath, Path outputBase, MaskRulesConfig config, String suffix, boolean dryRun) {
    Objects.requireNonNull(config, "config");
    return new RunPlan(inputPath, outputBase, null, config, suffix, dryRun);
  }
}
