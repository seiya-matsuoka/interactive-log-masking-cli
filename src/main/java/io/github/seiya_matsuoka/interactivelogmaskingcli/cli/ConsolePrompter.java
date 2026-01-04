package io.github.seiya_matsuoka.interactivelogmaskingcli.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

/**
 * コンソール対話入力のユーティリティ。
 *
 * <p>入力の正規化・リトライ・デフォルト値処理を集約する。（InteractiveCli や RuleWizard から直接 Scanner を触らないようにする）
 */
public class ConsolePrompter {

  private final Scanner scanner;
  private final PrintStream out;

  public ConsolePrompter(Scanner scanner, PrintStream out) {
    this.scanner = Objects.requireNonNull(scanner, "scanner");
    this.out = Objects.requireNonNull(out, "out");
  }

  /** 文字列入力（空入力ならデフォルトを採用）。 */
  public String askString(String label, String defaultValue) {
    while (true) {
      String prompt =
          (defaultValue == null || defaultValue.isBlank())
              ? String.format("%s: ", label)
              : String.format("%s（空Enterで: %s）: ", label, defaultValue);

      out.print(prompt);
      out.flush();

      String line = readLine();
      if (line.isBlank()) {
        if (defaultValue != null) {
          return defaultValue;
        }
        // default が null の時は空白入力を許容しない（再入力）
        out.println("  入力してください。");
        continue;
      }
      return line;
    }
  }

  /** 文字列入力（空入力不可）。 */
  public String askNonBlank(String label) {
    while (true) {
      out.print(label + ": ");
      out.flush();

      String line = readLine();
      if (line.isBlank()) {
        out.println("  空は不可です。入力してください。");
        continue;
      }
      return line;
    }
  }

  /** Yes/No 質問。 */
  public boolean askYesNo(String label, boolean defaultYes) {
    String defaultText = defaultYes ? "はい" : "いいえ";

    while (true) {
      out.print(label + "（y/n、空Enterで: " + defaultText + "）: ");
      out.flush();

      String raw = readLine().trim().toLowerCase(Locale.ROOT);

      if (raw.isEmpty()) {
        return defaultYes;
      }
      if (raw.equals("y") || raw.equals("yes")) {
        return true;
      }
      if (raw.equals("n") || raw.equals("no")) {
        return false;
      }
      out.println("  y / n で回答してください。");
    }
  }

  /** パス入力（空入力ならデフォルトを採用）。 */
  public Path askPath(String label, Path defaultValue) {
    String v = askString(label, defaultValue == null ? null : defaultValue.toString());
    return Path.of(v);
  }

  /** 選択肢（1..N）の入力。 */
  public int askChoice(String label, List<String> options, int defaultIndex1Based) {
    Objects.requireNonNull(options, "options");

    if (options.isEmpty()) {
      throw new IllegalArgumentException("選択肢（options）が空です。");
    }
    if (defaultIndex1Based < 1 || defaultIndex1Based > options.size()) {
      throw new IllegalArgumentException("デフォルトの選択肢（defaultIndex1Based）が範囲外の値です。");
    }

    out.println(label);
    for (int i = 0; i < options.size(); i++) {
      out.printf("  %d) %s%n", i + 1, options.get(i));
    }

    while (true) {
      out.printf("番号を入力してください（空Enterで: %d）: ", defaultIndex1Based);
      out.flush();

      String raw = readLine().trim();

      if (raw.isEmpty()) {
        return defaultIndex1Based;
      }
      try {
        int n = Integer.parseInt(raw);
        if (n < 1 || n > options.size()) {
          out.println("  範囲外です。");
          continue;
        }
        return n;
      } catch (NumberFormatException e) {
        out.println("  数字で入力してください。");
      }
    }
  }

  private String readLine() {
    if (!scanner.hasNextLine()) {
      throw new IllegalStateException("標準入力が利用できません。Gradle run の standardInput 設定を確認してください。");
    }
    return scanner.nextLine();
  }
}
