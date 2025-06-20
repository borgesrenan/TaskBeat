package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskBeatDataBase::class.java, "database-task-beat"
        ).build()
    }

    private val categoryDao: CategoryDao by lazy {
        db.getCategoryDao()
    }

    private val taskDao: TaskDao by lazy {
        db.getTaskDao()
    }

    private var categories = listOf<CategoryUiData>()

    private var tasks = listOf<TaskUiData>()

    private val categoryAdapter = CategoryListAdapter()

    private val taskAdapter = TaskListAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val rvCategory = findViewById<RecyclerView>(R.id.rv_categories)
        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks)
        val fabCreateTask = findViewById<FloatingActionButton>(R.id.fab_create_task)

        fabCreateTask.setOnClickListener {
            val createTaskBottomSheet = CreateTaskBottomSheet(
                categories
            ) { taskToBeCreated ->
                val taskEntityToBeInsert = TaskEntity(
                    name = taskToBeCreated.name,
                    category = taskToBeCreated.category
                )
                insertTask(taskEntityToBeInsert)
            }

            createTaskBottomSheet.show(
                supportFragmentManager,
                "createTaskBottomSheet"
            )
        }

        categoryAdapter.setOnClickListener { selected ->

            if (selected.name == "+") {

                val createCategoryBottomSheet = CreateCategoryBottomSheet { categoryName ->
                    val categoryEntity = CategoryEntity(
                        name = categoryName,
                        isSelected = false
                    )

                    insertCategory(categoryEntity)

                }
                createCategoryBottomSheet.show(supportFragmentManager, "createCategoryBottomSheet")

            } else {
                val categoryTemp = categories.map { item ->
                    when {
                        item.name == selected.name && !item.isSelected -> item.copy(isSelected = true)
                        item.name == selected.name && item.isSelected -> item.copy(isSelected = false)
                        else -> item
                    }
                }

                val taskTemp =
                    if (selected.name != "ALL") {
                        tasks.filter { it.category == selected.name }
                    } else {
                        tasks
                    }
                taskAdapter.submitList(taskTemp)

                categoryAdapter.submitList(categoryTemp)
            }
        }

        rvCategory.adapter = categoryAdapter
        GlobalScope.launch(Dispatchers.IO) {

            getCategoriesFromDataBase()
        }

        rvTask.adapter = taskAdapter

        GlobalScope.launch(Dispatchers.IO) {
            getTasksFromDataBase()
        }
    }


    private fun getCategoriesFromDataBase() {
        val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
        val categoriesUiData = categoriesFromDb.map {
            CategoryUiData(
                it.name,
                it.isSelected
            )
        }.toMutableList()

        // Add fake + category
        categoriesUiData.add(
            CategoryUiData(
                name = "+",
                isSelected = false
            )
        )
        categories = categoriesUiData
        categoryAdapter.submitList(categoriesUiData)

    }

    private fun getTasksFromDataBase() {
        val tasksFromDb: List<TaskEntity> = taskDao.getAll()
        val tasksUiData = tasksFromDb.map {
            TaskUiData(
                name = it.name,
                category = it.category
            )
        }
        GlobalScope.launch(Dispatchers.Main) {
            tasks = tasksUiData
            taskAdapter.submitList(tasksUiData)
        }
    }

    private fun insertCategory(categoryEntity: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            categoryDao.insert(categoryEntity)
            getCategoriesFromDataBase()

        }
    }

    private fun insertTask(taskEntity: TaskEntity){
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.insert(taskEntity)
            getTasksFromDataBase()
        }
    }
}
