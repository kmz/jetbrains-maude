package org.maude.intellij

import com.intellij.testFramework.LexerTestCase
import com.intellij.lexer.Lexer

class MaudeLexerTest : LexerTestCase() {
    override fun createLexer(): Lexer = MaudeLexerAdapter()
    override fun getDirPath(): String = ""

    fun testModuleAndKeywords() {
        doTest(
            "fmod FOO is endfm",
            """
            KW_MODULE ('fmod')
            WHITE_SPACE (' ')
            IDENTIFIER ('FOO')
            WHITE_SPACE (' ')
            KW_CONTROL ('is')
            WHITE_SPACE (' ')
            KW_END ('endfm')
            """.trimIndent()
        )
    }

    fun testLineComment() {
        doTest(
            "*** hi\n1",
            """
            COMMENT_LINE ('*** hi')
            WHITE_SPACE ('\n')
            NUMBER ('1')
            """.trimIndent()
        )
    }

    fun testNestedBlockComment() {
        doTest(
            "***( a (b) c )X",
            """
            COMMENT_BLOCK ('***( a (b) c )')
            IDENTIFIER ('X')
            """.trimIndent()
        )
    }

    fun testBlockCommentWithSpaceBeforeParen() {
        doTest(
            "*** ( a (b) c )X",
            """
            COMMENT_BLOCK ('*** ( a (b) c )')
            IDENTIFIER ('X')
            """.trimIndent()
        )
    }

    fun testString() {
        doTest(
            """"a\nb"""",
            """STRING ('"a\nb"')"""
        )
    }
}
