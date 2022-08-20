import com.dsoftware.ghtoolbar.ui.Icons
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.text.DateFormatUtil
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import javax.swing.Icon

fun jobIcon(job: WorkflowRunJob): Icon {
    return when (job.status) {
        WorkflowRunJob.Status.COMPLETED ->
            when (job.conclusion) {
                "success" -> AllIcons.Actions.Commit
                "failure" -> Icons.X
                else -> Icons.PrimitiveDot
            }

        else -> Icons.PrimitiveDot
    }
}

fun makeTimePretty(date: Date): String {
    val localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
    val zonedDateTime = localDateTime.atZone(ZoneOffset.UTC)
    return DateFormatUtil.formatPrettyDateTime(zonedDateTime.toInstant().toEpochMilli())
}

//fun JobsConsole(
//    project: Project,
//    jobModel: SingleValueModel<WorkflowRunJobs?>,
//    disposable: Disposable,
//): JBScrollPane {
//
//    fun setData(jobModel: WorkflowRunJobs): JBScrollPane {
//        return JBScrollPane(panel {
//            row("${jobModel.total_count} jobs") {}
//            for (job in jobModel.jobs) {
//                row {
//
//                    icon(jobIcon(job))
//                    text("Job: ${job.name} attempt ${job.runAttempt} <a href='${job.htmlUrl}'>link</a>")
//                    comment("Started at ${makeTimePretty(job.startedAt)}")
//
//                }
////                panel {
////                    val steps = job.steps?.toList() ?: emptyList()
////                    for (step in steps) {
////                        indent {
////                            row {
////                                text("${step.number}: ${step.name} -- ${step.conclusion}")
////                            }
////                        }
////                    }
////                }
//            }
//        })
//    }
//
//
//    var panel = JBScrollPane()
//    if (jobModel.value != null) {
//        panel = setData(jobModel.value!!)
//    }
//    jobModel.addListener {
//        if (jobModel.value != null) {
//            panel = setData(jobModel.value!!)
//        }
//    }
//    return panel
//}

class JobsConsole(
    project: Project,
    jobModel: SingleValueModel<WorkflowRunJobs?>,
    disposable: Disposable,
) : JBPanel<JobsConsole>() {
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
