/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.tasks

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material.Surface
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.ServiceLocator
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.FakeRepository
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.saveTaskBlocking
import com.google.accompanist.appcompattheme.AppCompatTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the Task List screen.
 */
// TODO - Move to the sharedTest folder when https://issuetracker.google.com/224974381 is fixed
@RunWith(AndroidJUnit4::class)
@MediumTest
// @LooperMode(LooperMode.Mode.PAUSED)
// @TextLayoutMode(TextLayoutMode.Mode.REALISTIC)
@ExperimentalCoroutinesApi
class TasksScreenTest {

    private lateinit var repository: TasksRepository

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val activity get() = composeTestRule.activity

    @Before
    fun initRepository() {
        // Run on UI thread to make sure the same instance of the SL is used.
        runOnUiThread {
            repository = FakeRepository()
            ServiceLocator.tasksRepository = repository
        }
    }

    @After
    fun cleanupDb() {
        runOnUiThread {
            ServiceLocator.resetRepository()
        }
    }

    @Test
    fun displayTask_whenRepositoryHasData() {
        // GIVEN - One task already in the repository
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))

        composeTestRule.waitUntilExists(hasText("Welcome"))
        // WHEN - On startup
        setContent()

        // THEN - Verify task is displayed on screen
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
    }

    @Test
    fun displayActiveTask() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))

        setContent()

        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()

        openFilterAndSelectOption(R.string.nav_active)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()

        openFilterAndSelectOption(R.string.nav_completed)

        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()
    }

    @Test
    fun displayCompletedTask() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1", true))

        setContent()

        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()

        openFilterAndSelectOption(R.string.nav_active)
        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()

        openFilterAndSelectOption(R.string.nav_completed)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
    }

    @Test
    fun markTaskAsComplete() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))

        setContent()

        // Mark the task as complete
        composeTestRule.onNode(isToggleable()).performClick()

        // Verify task is shown as complete
        openFilterAndSelectOption(R.string.nav_all)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        openFilterAndSelectOption(R.string.nav_active)
        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()
        openFilterAndSelectOption(R.string.nav_completed)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
    }

    @Test
    fun markTaskAsActive() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1", true))

        setContent()

        // Mark the task as active
        composeTestRule.onNode(isToggleable()).performClick()

        // Verify task is shown as active
        openFilterAndSelectOption(R.string.nav_all)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        openFilterAndSelectOption(R.string.nav_active)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        openFilterAndSelectOption(R.string.nav_completed)
        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()
    }

    @Test
    fun showAllTasks() {
        // Add one active task and one completed task
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))
        repository.saveTaskBlocking(Task("TITLE2", "DESCRIPTION2", true))

        setContent()

        // Verify that both of our tasks are shown
        openFilterAndSelectOption(R.string.nav_all)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE2").assertIsDisplayed()
    }

    @Test
    fun showActiveTasks() {
        // Add 2 active tasks and one completed task
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))
        repository.saveTaskBlocking(Task("TITLE2", "DESCRIPTION2"))
        repository.saveTaskBlocking(Task("TITLE3", "DESCRIPTION3", true))

        setContent()

        // Verify that the active tasks (but not the completed task) are shown
        openFilterAndSelectOption(R.string.nav_active)
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE2").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE3").assertDoesNotExist()
    }

    @Test
    fun showCompletedTasks() {
        // Add one active task and 2 completed tasks
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))
        repository.saveTaskBlocking(Task("TITLE2", "DESCRIPTION2", true))
        repository.saveTaskBlocking(Task("TITLE3", "DESCRIPTION3", true))

        setContent()

        // Verify that the completed tasks (but not the active task) are shown
        openFilterAndSelectOption(R.string.nav_completed)
        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()
        composeTestRule.onNodeWithText("TITLE2").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE3").assertIsDisplayed()
    }

    @Test
    fun clearCompletedTasks() {
        // Add one active task and one completed task
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION1"))
        repository.saveTaskBlocking(Task("TITLE2", "DESCRIPTION2", true))

        setContent()

        // Click clear completed in menu
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_more))
            .performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.menu_clear)).assertIsDisplayed()
        composeTestRule.onNodeWithText(activity.getString(R.string.menu_clear)).performClick()

        openFilterAndSelectOption(R.string.nav_all)
        // Verify that only the active task is shown
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE2").assertDoesNotExist()
    }

    @Test
    fun noTasks_AllTasksFilter_AddTaskViewVisible() {
        setContent()

        openFilterAndSelectOption(R.string.nav_all)

        // Verify the "You have no tasks!" text is shown
        composeTestRule.onNodeWithText("You have no tasks!").assertIsDisplayed()
    }

    @Test
    fun noTasks_CompletedTasksFilter_AddTaskViewNotVisible() {
        setContent()

        openFilterAndSelectOption(R.string.nav_completed)
        // Verify the "You have no completed tasks!" text is shown
        composeTestRule.onNodeWithText("You have no completed tasks!").assertIsDisplayed()
    }

    @Test
    fun noTasks_ActiveTasksFilter_AddTaskViewNotVisible() {
        setContent()

        openFilterAndSelectOption(R.string.nav_active)
        // Verify the "You have no active tasks!" text is shown
        composeTestRule.onNodeWithText("You have no active tasks!").assertIsDisplayed()
    }

    private fun setContent() {
        composeTestRule.setContent {
            AppCompatTheme {
                Surface {
                    TasksScreen(
                        viewModel = TasksViewModel(repository, SavedStateHandle()),
                        userMessage = R.string.successfully_added_task_message,
                        onAddTask = { },
                        onTaskClick = { },
                        openDrawer = { }
                    )
                }
            }
        }
    }

    private fun openFilterAndSelectOption(@StringRes option: Int) {
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_filter))
            .performClick()
        composeTestRule.waitUntilExists(hasText("Continue"))
        composeTestRule.onNodeWithText(activity.getString(option)).assertIsDisplayed()
        composeTestRule.onNodeWithText(activity.getString(option)).performClick()
    }
}

private fun ComposeContentTestRule.waitUntilExists(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L
) {
    return this.waitUntilNodeCount(matcher, 1, timeoutMillis)
}

private fun ComposeContentTestRule.waitUntilDoesNotExist(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L
) {
    return this.waitUntilNodeCount(matcher, 0, timeoutMillis)
}

private fun ComposeContentTestRule.waitUntilNodeCount(
    matcher: SemanticsMatcher,
    count: Int = 1,
    timeoutMillis: Long = 1_000L
) {
    this.waitUntil(timeoutMillis) {
        this.onAllNodes(matcher).fetchSemanticsNodes().size == count
    }
}
