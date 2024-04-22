package com.dsoftware.ghmanager.psi

import com.dsoftware.ghmanager.psi.GitHubWorkflowConfig.FIELD_USES
import com.dsoftware.ghmanager.psi.actions.UpdateActionVersionFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemHighlightType
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
        val currentVersion = yamlKeyValue.valueText.split("@").getOrNull(1) ?: return
        gitHubActionCache.whenActionsLoaded {
            val latestVersion = gitHubActionCache.getAction(actionName)?.latestVersion
            if (VersionCompareTools.isActionOutdated(currentVersion, latestVersion)) {
                val message = "$currentVersion is outdated. Latest version is $latestVersion"
                val annotationBuilder = holder
                    .newAnnotation(HighlightSeverity.WARNING, message)
                    .range(
                        TextRange.create(
                            yamlKeyValue.textRange.startOffset
                                + yamlKeyValue.text.indexOf("@") + 1,
                            yamlKeyValue.textRange.endOffset
                        )
                    )
                val inspectionManager = yamlKeyValue.project.service<InspectionManager>()
                val quickfix = UpdateActionVersionFix(actionName, latestVersion!!)
                val problemDescriptor = inspectionManager.createProblemDescriptor(
                    yamlKeyValue, message, quickfix,
                    ProblemHighlightType.WEAK_WARNING, true
                );
                annotationBuilder
                    .newLocalQuickFix(quickfix, problemDescriptor)
                    .registerFix()
                annotationBuilder.create()
            }
        }

    }
}
