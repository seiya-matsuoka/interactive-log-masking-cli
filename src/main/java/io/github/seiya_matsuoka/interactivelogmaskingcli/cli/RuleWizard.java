package io.github.seiya_matsuoka.interactivelogmaskingcli.cli;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRuleConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.MaskRulesConfig;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleFlag;
import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RulesRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** ルール指定（JSONファイル or 対話作成）を扱うウィザード。 */
public class RuleWizard {

  private final ConsolePrompter prompter;
  private final RulesRepository repository;

  public RuleWizard(ConsolePrompter prompter, RulesRepository repository) {
    this.prompter = Objects.requireNonNull(prompter, "prompter");
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  /**
   * ルール指定方法を選び、RunPlan の入力として使える形にする。
   *
   * @param defaultRulesPath デフォルトの rules.json パス
   * @return ルール指定結果（rulesPath または config のどちらか）
   */
  public RuleSource chooseRuleSource(Path defaultRulesPath) throws IOException {

    int choice = prompter.askChoice("ルールの指定方法を選んでください", List.of("JSON設定ファイルを使う", "対話でルールを作成する"), 1);

    if (choice == 1) {
      Path rulesPath = prompter.askPath("ルールJSONのパス", defaultRulesPath);
      return RuleSource.ofRulesPath(rulesPath);
    }

    // 対話形式でルールを作る
    MaskRulesConfig config = createConfigInteractively();

    boolean save = prompter.askYesNo("作成したルールをJSONファイルに保存しますか？", true);
    if (save) {
      Path savePath = prompter.askPath("保存先ルールJSONのパス", defaultRulesPath);
      if (savePath.getParent() != null) {
        Files.createDirectories(savePath.getParent());
      }
      repository.save(savePath, config);
    }

    return RuleSource.ofConfig(config);
  }

  /** 対話で MaskRulesConfig を作成する。 */
  private MaskRulesConfig createConfigInteractively() {

    List<MaskRuleConfig> rules = new ArrayList<>();

    prompter.askString("ルール作成を開始します（Enterで続行）", "");

    while (true) {
      MaskRuleConfig r = new MaskRuleConfig();

      r.setId(prompter.askNonBlank("rule.id（例: email）"));
      r.setName(prompter.askString("rule.name（表示名）", r.getId()));

      boolean enabled = prompter.askYesNo("このルールを有効にしますか？", true);
      r.setEnabled(enabled);

      r.setPattern(prompter.askNonBlank("pattern（正規表現）"));
      r.setReplacement(prompter.askNonBlank("replacement（置換文字列）"));

      // flags は簡易に「カンマ区切り」を採用（例: CASE_INSENSITIVE,MULTILINE）
      String flagHint = "flags（任意、カンマ区切り。例: CASE_INSENSITIVE。空で無し）";
      String rawFlags = prompter.askString(flagHint, "");
      r.setFlags(parseFlags(rawFlags));

      rules.add(r);

      boolean cont = prompter.askYesNo("ルールを追加しますか？", false);
      if (!cont) {
        break;
      }
    }

    // version は固定（拡張しやすいように）
    return new MaskRulesConfig().setVersion(1).setRules(rules);
  }

  private List<RuleFlag> parseFlags(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    String[] parts = raw.split(",");
    List<RuleFlag> flags = new ArrayList<>();
    for (String p : parts) {
      String name = p.trim();
      if (name.isEmpty()) continue;

      try {
        flags.add(RuleFlag.valueOf(name));
      } catch (IllegalArgumentException e) {
        // ConsolePrompter に出力機能がないので、ここで入力ミスは無視せず例外を投げる
        throw new IllegalArgumentException("不正なflags指定: " + name + "（例: CASE_INSENSITIVE）", e);
      }
    }
    return flags;
  }

  /** rulesPath か config のどちらか一方を保持する。 */
  public record RuleSource(Path rulesPath, MaskRulesConfig config) {

    public RuleSource {
      boolean a = (rulesPath != null);
      boolean b = (config != null);
      if (a == b) {
        throw new IllegalArgumentException("rulesPath または config のどちらか一方だけを指定してください。");
      }
    }

    public static RuleSource ofRulesPath(Path rulesPath) {
      return new RuleSource(Objects.requireNonNull(rulesPath), null);
    }

    public static RuleSource ofConfig(MaskRulesConfig config) {
      return new RuleSource(null, Objects.requireNonNull(config));
    }
  }
}
