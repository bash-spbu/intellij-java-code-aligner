@file:Suppress("UnstableApiUsage")

package com.github.bashspbu.intellijjavacodealigner

import com.intellij.codeInsight.hints.*
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.intellij.lang.annotations.Language


class JavaAligningInlaysProvider : InlayHintsProvider<JavaAligningInlaysSettings> {

    // -----------------------------------------------------------------------------
    //region Settings related

    override val key: SettingsKey<JavaAligningInlaysSettings> = SettingsKey(OUR_KEY)

    @Suppress("DialogTitleCapitalization")
    override val name: String
        get() = OurBundle.message("hints.settings.name")

    override val previewText: String get() = SETTINGS_PREVIEW_TEXT

    override val isVisibleInSettings: Boolean get() = true

    override fun createConfigurable(settings: JavaAligningInlaysSettings): ImmediateConfigurable {
        return JavaAligningInlaysConfigurable(settings)
    }

    override fun createSettings(): JavaAligningInlaysSettings {
        return JavaAligningInlaysSettings()
    }

    //endregion
    // -----------------------------------------------------------------------------

    override fun isLanguageSupported(language: com.intellij.lang.Language): Boolean {
        return JavaLanguage.INSTANCE.`is`(language)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: JavaAligningInlaysSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return JavaAligningInlaysCollector(editor, settings)
    }
}

private const val OUR_KEY = "JavaCodeAligner"

@Language("JAVA")
private const val SETTINGS_PREVIEW_TEXT = """
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
@interface TypeAnno {}

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
@interface AnotherTypeAnno {}

public class MyClass {

  private final @TypeAnno String myField1 = "God save the readability";
  private final @AnotherTypeAnno Object myField2 = null;
  public int myField3 = 123;
  protected float myField4 = 0.0f;

  public void method(String param1,
                     List<String> param2,
                     @TypeAnno Object param3,
                     int param4WithVeryLongName,
                     final double someFinalParam5) {

    String localVar1 = "Why don't ants get sick?";
    String localVar2WithLongName = "They have anty-bodies";
    @TypeAnno Object localVar3 = null;
    int localVar4WithVeryLongName = 0;
    final double someFinalLocalVar5 = 0.0;
    
    localVar1 = "Omg, you just can't joke that bad";
    localVar2WithLongName = null;
    localVar3 = new Object();
    localVar4WithVeryLongName = 42 + 42 + 42 + 42;
  }
}
"""