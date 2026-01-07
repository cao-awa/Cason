package com.github.cao.awa.cason.exception

import com.github.cao.awa.cason.reader.CharReader

class NeedMoreInputException(val reader: CharReader) : RuntimeException()