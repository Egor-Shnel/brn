package com.epam.brn.upload

import com.epam.brn.constant.ExerciseTypeEnum
import com.epam.brn.constant.WordTypeEnum
import com.epam.brn.exception.FileFormatException
import com.epam.brn.job.CsvUtils
import com.epam.brn.model.Exercise
import com.epam.brn.model.ExerciseGroup
import com.epam.brn.model.Resource
import com.epam.brn.model.Series
import com.epam.brn.model.Task
import com.epam.brn.repo.ExerciseGroupRepository
import com.epam.brn.repo.ExerciseRepository
import com.epam.brn.repo.SeriesRepository
import com.epam.brn.repo.TaskRepository
import com.epam.brn.service.ExerciseService
import com.epam.brn.service.InitialDataLoader
import com.epam.brn.service.SeriesService
import com.epam.brn.upload.csv.parser.CsvParser
import com.epam.brn.upload.csv.parser.iterator.impl.GroupMappingIteratorProvider
import com.epam.brn.upload.csv.parser.iterator.impl.Series1TaskMappingIteratorProvider
import com.epam.brn.upload.csv.parser.iterator.impl.Series2ExerciseMappingIteratorProvider
import com.epam.brn.upload.csv.parser.iterator.impl.SeriesMappingIteratorProvider
import com.epam.brn.upload.csv.processor.GroupRecordProcessor
import com.epam.brn.upload.csv.processor.SeriesGenericRecordProcessor
import com.epam.brn.upload.csv.processor.SeriesOneExerciseRecordProcessor
import com.epam.brn.upload.csv.processor.SeriesTwoExerciseRecordProcessor
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.LineNumberReader
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class CsvUploadService(
    private val csvParser: CsvParser,
    private val groupRepository: ExerciseGroupRepository,
    private val seriesRepository: SeriesRepository,
    private val exerciseRepository: ExerciseRepository,
    private val taskRepository: TaskRepository
) {

    @Value("\${brn.dataFormatNumLines}")
    val dataFormatLinesCount = 5

    @Autowired
    lateinit var groupConverter: GroupRecordProcessor

    @Autowired
    lateinit var seriesConverter: SeriesGenericRecordProcessor

    @Autowired
    lateinit var task1SeriesOneExerciseRecordProcessor: SeriesOneExerciseRecordProcessor

    @Autowired
    lateinit var seriesTwoExerciseRecordProcessor: SeriesTwoExerciseRecordProcessor

    @Autowired
    lateinit var seriesService: SeriesService

    @Autowired
    lateinit var exerciseService: ExerciseService

    fun loadGroups(inputStream: InputStream): MutableIterable<ExerciseGroup> {
        val records = csvParser.parse(inputStream, GroupMappingIteratorProvider())
        val result = records.map { parsedValue -> groupConverter.convert(parsedValue) }

        return groupRepository.saveAll(result)
    }

    fun loadSeries(inputStream: InputStream): MutableIterable<Series> {
        val records = csvParser.parse(inputStream, SeriesMappingIteratorProvider())
        val result = records.map { parsedValue -> seriesConverter.convert(parsedValue) }

        return seriesRepository.saveAll(result)
    }

    @Throws(FileFormatException::class)
    fun loadExercises(seriesId: Long, file: MultipartFile): List<Any> {

        if (!isFileContentTypeCsv(file.contentType ?: StringUtils.EMPTY))
            throw FileFormatException()

        return when (seriesId.toInt()) {
            1 -> loadTasksFor1Series(file.inputStream)
            2 -> loadExercisesFor2Series(file.inputStream)
            else -> throw IllegalArgumentException("There no one strategy yet for seriesId = $seriesId")
        }
    }

    private fun isFileContentTypeCsv(contentType: String): Boolean = CsvUtils.isFileContentTypeCsv(contentType)

    @Throws(FileFormatException::class)
    fun loadTasks(file: File): List<Task> = loadTasksFor1Series(file.inputStream())

    fun loadTasksFor1Series(inputStream: InputStream): List<Task> {
        val tasks = csvParser.parse(inputStream, Series1TaskMappingIteratorProvider())
        val result = tasks.map { parsedValue -> task1SeriesOneExerciseRecordProcessor.convert(parsedValue) }

        return taskRepository.saveAll(result)
    }

    fun loadExercisesFor2Series(inputStream: InputStream): List<Exercise> {
        val exercises = csvParser.parse(inputStream, Series2ExerciseMappingIteratorProvider())
        val result = exercises.map { parsedValue -> seriesTwoExerciseRecordProcessor.convert(parsedValue) }

        return exerciseRepository.saveAll(result)
    }

    fun loadExercisesFor3Series(inputStream: InputStream): List<Exercise> {
        // todo: get data from file for 3 series
        val exercises = createExercises()

        return exerciseService.save(exercises)
    }

    fun createExercises(): List<Exercise> {
        val exercise = createExercise()
        exercise.addTask(createTask())
        return listOf(exercise)
    }

    private fun createExercise(): Exercise {
        return Exercise(
            series = seriesService.findSeriesForId(3L),
            name = "Распознование предложений из 2 слов",
            description = "Распознование предложений из 2 слов",
            template = "<OBJECT OBJECT_ACTION>",
            exerciseType = ExerciseTypeEnum.SENTENCE.toString(),
            level = 1
        )
    }

    private fun createTask(): Task {
        val resource1 = Resource(
            word = "девочкаTest",
            wordType = WordTypeEnum.OBJECT.toString(),
            audioFileUrl = "series2/девочка.mp3",
            pictureFileUrl = "pictures/withWord/девочка.jpg"
        )
        val resource2 = Resource(
            word = "дедушкаTest",
            wordType = WordTypeEnum.OBJECT.toString(),
            audioFileUrl = "series2/дедушка.mp3",
            pictureFileUrl = "pictures/withWord/дедушка.jpg"
        )
        val resource3 = Resource(
            word = "бабушкаTest",
            wordType = WordTypeEnum.OBJECT.toString(),
            audioFileUrl = "series2/бабушка.mp3",
            pictureFileUrl = "pictures/withWord/бабушка.jpg"
        )
        val resource4 = Resource(
            word = "бросаетTest",
            wordType = WordTypeEnum.OBJECT_ACTION.toString(),
            audioFileUrl = "series2/бросает.mp3",
            pictureFileUrl = "pictures/withWord/бросает.jpg"
        )
        val resource5 = Resource(
            word = "читаетTest",
            wordType = WordTypeEnum.OBJECT_ACTION.toString(),
            audioFileUrl = "series2/читает.mp3",
            pictureFileUrl = "pictures/withWord/читает.jpg"
        )
        val resource6 = Resource(
            word = "рисуетTest",
            wordType = WordTypeEnum.OBJECT_ACTION.toString(),
            audioFileUrl = "series2/рисует.mp3",
            pictureFileUrl = "pictures/withWord/рисует.jpg"
        )

        val answerOptions =
            mutableSetOf(resource1, resource2, resource3, resource4, resource5, resource6)

        val correctAnswer = Resource(
            word = "девочка рисует",
            wordType = WordTypeEnum.SENTENCE.toString(),
            audioFileUrl = "series3/девочка_рисует.mp3"
        )

        return Task(
            serialNumber = 2,
            answerOptions = answerOptions,
            correctAnswer = correctAnswer,
            answerParts = mutableMapOf(1 to resource1, 2 to resource6)
        )
    }

    fun getSampleStringForSeriesFile(seriesId: Long): String {
        return readFormatSampleLines(InitialDataLoader.getInputStreamFromSeriesInitFile(seriesId))
    }

    private fun readFormatSampleLines(inputStream: InputStream): String {
        return getLinesFrom(inputStream, dataFormatLinesCount).joinToString("\n")
    }

    private fun getLinesFrom(inputStream: InputStream, linesCount: Int): MutableList<String> {
        inputStream.use {
            val strings = mutableListOf<String>()

            val reader = LineNumberReader(InputStreamReader(inputStream))
            while (reader.lineNumber < linesCount) {
                reader.readLine().let { strings.add(it) }
            }
            return strings
        }
    }
}
