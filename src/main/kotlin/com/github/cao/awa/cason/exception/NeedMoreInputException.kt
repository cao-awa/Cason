package com.github.cao.awa.cason.exception

class NeedMoreInputException(val index: Int, val line: Int, val col: Int) : RuntimeException()