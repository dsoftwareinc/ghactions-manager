package com.dsoftware.ghmanager.psi

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.util.Collections

object Tools {
    fun isActionFile(virtualFile: VirtualFile): Boolean {
        return (virtualFile.name == "action.yml" || virtualFile.name == "action.yaml")
            && virtualFile.parent.path.endsWith(".github/actions")
    }

    fun isWorkflowFile(virtualFile: VirtualFile): Boolean {
        val parentPath = virtualFile.parent.path
        return parentPath.endsWith(".github/workflows") || parentPath.endsWith(".github/actions")
    }


    fun getYamlElementsWithKey(psiElement: PsiElement?, keyName: String?): List<YAMLKeyValue> {
        if (psiElement == null || keyName == null) {
            return emptyList()
        }
        val results = mutableListOf<YAMLKeyValue>()
        val exploredElements = mutableSetOf<PsiElement>()
        val toExplore = mutableListOf<PsiElement>(psiElement)
        while (toExplore.isNotEmpty()) {
            val currentElement = toExplore.removeAt(0)
            if (exploredElements.contains(currentElement)) {
                continue
            }
            exploredElements.add(currentElement)
            if (currentElement is YAMLKeyValue && currentElement.keyText == keyName) {
                results.add(currentElement)
            }
            toExplore.addAll(currentElement.children)
        }
        return Collections.unmodifiableList(results)
    }
}