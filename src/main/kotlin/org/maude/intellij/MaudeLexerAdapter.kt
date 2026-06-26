package org.maude.intellij

import com.intellij.lexer.FlexAdapter

class MaudeLexerAdapter : FlexAdapter(_MaudeLexer(null))
