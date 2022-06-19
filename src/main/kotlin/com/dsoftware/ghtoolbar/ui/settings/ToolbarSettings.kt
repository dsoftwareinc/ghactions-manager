import ToolbarSettings.ToolbarState
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project


/**
 * Supports storing the application settings in a persistent way.
 * The [ToolbarState] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
    name = "GhActionsToolbarSettings",
    storages = [
        Storage("ghactions-toolbar.xml")
    ],
    reportStatistic = false,
)
class ToolbarSettings : PersistentStateComponent<ToolbarState> {

    private var toolbarState = ToolbarState()

    class ToolbarState : BaseState() {
        var useProjectRepos: Boolean = true
    }

    override fun getState() = toolbarState

    override fun loadState(toolbarState: ToolbarState) {
        this.toolbarState = toolbarState
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ToolbarSettings = project.service()
    }
}