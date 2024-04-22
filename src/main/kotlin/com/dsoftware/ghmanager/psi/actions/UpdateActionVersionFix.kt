package com.dsoftware.ghmanager.psi.actions

import com.dsoftware.ghmanager.i18n.MessagesBundle.message
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLKeyValue

class UpdateActionVersionFix(
    private val actionName: String, fullLatestVersion: String,
) : LocalQuickFix {
    private val latestMajorVersion = fullLatestVersion.split(".").first()

    override fun getFamilyName(): String {
        return message("ghmanager.update.action.version.fix.family.name", actionName, latestMajorVersion)
    }


    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val yamlKeyValue = descriptor.psiElement as YAMLKeyValue
        val actionName = yamlKeyValue.valueText.split("@").firstOrNull() ?: return
        val yamlElementGenerator = yamlKeyValue.project.service<YAMLElementGenerator>()
        val newYamlKeyValue =
            yamlElementGenerator.createYamlKeyValue(yamlKeyValue.keyText, "$actionName@$latestMajorVersion")
        yamlKeyValue.setValue(newYamlKeyValue.value!!)
    }
}
