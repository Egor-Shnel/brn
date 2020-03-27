package com.epam.brn.upload.csv.parser

import com.epam.brn.upload.csv.record.GroupRecord
import com.epam.brn.upload.csv.record.SeriesGenericRecord
import com.epam.brn.upload.csv.record.SeriesOneRecord
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CsvParserTest {

    private val parser = CsvParser()

    @Test
    fun `should parse Tasks`() {
        val input = """
                level exerciseName orderNumber word audioFileName pictureFileName words wordType
                1 name1 1 бал no_noise/бал.mp3 pictures/бал.jpg (бам,сам,дам,зал,бак) OBJECT
                2 name1 3 foo no_noise/foo.mp3 pictures/foo.jpg (foo,bar,baz) OBJECT
                """.trimIndent().byteInputStream(StandardCharsets.UTF_8)

        val result = parser.parseSeriesOneExerciseRecords(input)

        assertThat(result).containsAll(
            listOf(
                SeriesOneRecord(
                    1, "name1", 1,
                    "бал", "no_noise/бал.mp3", "pictures/бал.jpg",
                    listOf("(бам", "сам", "дам", "зал", "бак)"), "OBJECT"
                ), SeriesOneRecord(
                    2, "name1", 3,
                    "foo", "no_noise/foo.mp3", "pictures/foo.jpg",
                    listOf("(foo", "bar", "baz)"), "OBJECT"
                )
            )
        )
    }

    @Test
    fun `should throw parse exception`() {
        val input = """
                level exerciseName orderNumber word audioFileName pictureFileName words wordType
                incorrect string
                """.trimIndent().byteInputStream(StandardCharsets.UTF_8)

        assertThrows<CsvParser.ParseException> {
            parser.parseSeriesOneExerciseRecords(input)
        }
    }

    @Test
    fun `should throw exception with parse errors`() {
        val input = """
                level exerciseName orderNumber word audioFileName pictureFileName words wordType
                incorrect string 1
                incorrect string 2
                """.trimIndent().byteInputStream(StandardCharsets.UTF_8)

        val actual = assertThrows<CsvParser.ParseException> {
            parser.parseSeriesOneExerciseRecords(input)
        }.errors

        assertThat(actual[0]).startsWith("Failed to parse line 2: 'incorrect string 1'. Error: ")
        assertThat(actual[1]).startsWith("Failed to parse line 3: 'incorrect string 2'. Error: ")
    }

    @Test
    fun `should parse Groups`() {

        val input = """
                groupId, name, description
                1, Неречевые упражнения, Неречевые упражнения
                2, Речевые упражнения, Речевые упражнения              
                """.trimIndent().byteInputStream(StandardCharsets.UTF_8)

        val result = parser.parseGroupRecords(input)

        assertThat(result).containsAll(
            listOf(
                GroupRecord(1, "Неречевые упражнения", "Неречевые упражнения"),
                GroupRecord(2, "Речевые упражнения", "Речевые упражнения")
            )
        )
    }

    @Test
    fun `should parse Series`() {
        val input = """
                groupId, seriesId, name, description
                2, 1, Распознование слов, Распознование слов
                2, 2, Составление предложений, Составление предложений         
                """.trimIndent().byteInputStream(StandardCharsets.UTF_8)

        val result = parser.parseSeriesGenericRecords(input)

        assertThat(result).containsAll(
            listOf(
                SeriesGenericRecord(2, 1, "Распознование слов", "Распознование слов"),
                SeriesGenericRecord(2, 2, "Составление предложений", "Составление предложений")
            )
        )
    }
}