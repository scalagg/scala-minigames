package gg.tropic.practice.application

import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class LogRedirector {
    private val originalOut = System.out
    private val originalErr = System.err

    fun startLogging(logFileName: String = "app.log") {
        try {

            val fileOutputStream = FileOutputStream(logFileName, true) 
            val printStream = PrintStream(fileOutputStream)

            val dualPrintStream = object : PrintStream(fileOutputStream) {
                override fun println(x: String?) {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                    val logEntry = "[$timestamp] $x"

                    super.println(logEntry)
                    super.flush()

                    originalOut.println(logEntry)
                }

                override fun print(x: String?) {
                    super.print(x)
                    super.flush()
                    originalOut.print(x)
                }
            }

            System.setOut(dualPrintStream)
            System.setErr(dualPrintStream)

            println("Logging started to $logFileName")

        } catch (e: FileNotFoundException) {
            println("Error: Could not create log file: ${e.message}")
        }
    }

    fun stopLogging() {
        System.setOut(originalOut)
        System.setErr(originalErr)
        println("Logging stopped")
    }
}
