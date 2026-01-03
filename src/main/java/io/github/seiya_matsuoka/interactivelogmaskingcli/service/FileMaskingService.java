package io.github.seiya_matsuoka.interactivelogmaskingcli.service;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleCompiler;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidationException;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleValidator;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RulesRepository;
import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskRule;
import io.github.seiya_matsuoka.interactivelogmaskingcli.io.FileMaskingProcessor;
import io.github.seiya_matsuoka.interactivelogmaskingcli.report.MaskRunReport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 設定（JSON）→ 検証 → コンパイル → 入出力処理 を束ねるアプリケーションサービス。
 *
 * <p>CLI からは基本このサービスを呼ぶだけにしておき、UI（対話/引数）と実処理を分離する。
 */
public class FileMaskingService {

  private final RulesRepository rulesRepository;
  private final RuleValidator ruleValidator;
  private final MaskRuleCompiler ruleCompiler;
  private final FileMaskingProcessor fileProcessor;

  /** 既定の実装を組み立てる（本番/手動実行向け）。 */
  public FileMaskingService() {
    this(
        new RulesRepository(),
        new RuleValidator(),
        new MaskRuleCompiler(),
        new FileMaskingProcessor());
  }

  /** DI（テスト等）用コンストラクタ。 */
  public FileMaskingService(
      RulesRepository rulesRepository,
      RuleValidator ruleValidator,
      MaskRuleCompiler ruleCompiler,
      FileMaskingProcessor fileProcessor) {
    this.rulesRepository = rulesRepository;
    this.ruleValidator = ruleValidator;
    this.ruleCompiler = ruleCompiler;
    this.fileProcessor = fileProcessor;
  }

  /**
   * ルール設定JSONのパスを受け取り、input → out を実行する（いちばん基本の入口）。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @param outputBase out/ のような出力ベース
   * @param rulesPath rules/mask-rules.json のようなルールファイル
   * @param suffix 出力ファイル名サフィックス（例: "_masked"。空なら付与しない）
   * @return 実行レポート
   */
  public MaskRunReport maskToOut(Path inputPath, Path outputBase, Path rulesPath, String suffix)
      throws IOException, RuleValidationException {

    Objects.requireNonNull(rulesPath, "rulesPath");

    // 1) JSON読み込み
    MaskRulesConfig config = rulesRepository.load(rulesPath);

    // 2) 実行（validate → compile → file processing）
    return maskToOut(inputPath, outputBase, config, suffix);
  }

  /**
   * ルール設定（MaskRulesConfig）を直接受け取り、input → out を実行する。
   *
   * <p>対話で作ったルールをそのまま流す用途を想定。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @param outputBase out/ のような出力ベース
   * @param config ルール設定
   * @param suffix 出力ファイル名サフィックス
   * @return 実行レポート
   */
  public MaskRunReport maskToOut(
      Path inputPath, Path outputBase, MaskRulesConfig config, String suffix)
      throws IOException, RuleValidationException {

    Objects.requireNonNull(inputPath, "inputPath");
    Objects.requireNonNull(outputBase, "outputBase");
    Objects.requireNonNull(config, "config");

    // 1) ルール検証（ここで RuleValidationException を投げる）
    ruleValidator.validateOrThrow(config);

    // 2) コンパイル済みのマスキングルール へ変換
    List<MaskRule> compiledRules = ruleCompiler.compile(config);

    // 3) ファイル処理（input -> out）
    return fileProcessor.process(inputPath, outputBase, compiledRules, suffix);
  }
}
