import com.dsoftware.ghtoolbar.ui.consolepanel.WorkflowRunLogConsole
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.ui.UIUtil
import javax.swing.plaf.PanelUI

class JobsConsole(
    project: Project,
    jobModel: SingleValueModel<WorkflowRunJobs?>,
    disposable: Disposable,
) : ConsoleViewImpl(project, true), AnsiEscapeDecoder.ColoredTextAcceptor {
    private val ansiEscapeDecoder = AnsiEscapeDecoder()

    // when it's true its save to call editor, otherwise call 'editor' will throw an NPE
    private val objectInitialized = true;

    init {
        LOG.info("Create console")
        if (jobModel.value != null) {
            this.setData(jobModel.value!!)
        }
        jobModel.addListener {
            if (jobModel.value != null) {
                this.setData(jobModel.value!!)
            }
        }

        Disposer.register(disposable) {
            Disposer.dispose(this)
        }
    }

    private fun setData(jobs: WorkflowRunJobs) {
        this.clear()
        val msgBuilder = StringBuilder()
        for (job in jobs.jobs) {
            msgBuilder.append("\u001B[1;96m==== Job: ${job.name} attempt ${job.runAttempt} ${job.conclusion}:\u001B[0m\n")
            msgBuilder.append("\t${job.htmlUrl}\n")
            if (job.steps == null) {
                continue
            }
            for (step in job.steps) {
                msgBuilder.append("\tStep ${step.number}: ${step.name} -- ${step.conclusion}\n")
            }
        }
        val message = msgBuilder.toString()
        ansiEscapeDecoder.escapeText(message, ProcessOutputType.STDOUT, this)
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        this.print(text, ConsoleViewContentType.getConsoleViewType(attributes))
    }

    override fun setUI(ui: PanelUI?) {
        super.setUI(ui)
        if (objectInitialized && editor != null) {
            (editor as EditorImpl).backgroundColor = UIUtil.getPanelBackground()
        }
    }

    companion object {
        private val LOG = logger<WorkflowRunLogConsole>()
    }
}
