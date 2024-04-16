package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.psi.GitHubWorkflowConfig.FIELD_USES
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue

class HighlightAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.isValid || element !is YAMLKeyValue) {
            return
        }
        val yamlKeyValue = element as YAMLKeyValue
        when (yamlKeyValue.keyText) {
            FIELD_USES -> highlightAction(yamlKeyValue, holder)
        }
    }

    private fun highlightAction(yamlKeyValue: YAMLKeyValue, holder: AnnotationHolder) {
        val gitHubActionCache = yamlKeyValue.project.service<GitHubActionCache>()
        val actionName = yamlKeyValue.valueText.split("@").firstOrNull() ?: return
        val currentVersion = yamlKeyValue.valueText.split("@").getOrNull(1)
        val latestVersion = gitHubActionCache.getAction(actionName)?.latestVersion
        if (VersionCompareTools.isActionOutdated(currentVersion, latestVersion)) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "$currentVersion is outdated. Latest version is $latestVersion"
            ).range(
                TextRange.create(
                    yamlKeyValue.textRange.startOffset
                        + yamlKeyValue.text.indexOf("@") + 1,
                    yamlKeyValue.textRange.endOffset
                )
            ).create()
        }
    }
}
