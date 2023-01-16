import java.io.*
import java.nio.file.*
import java.nio.file.StandardCopyOption.*
import java.security.MessageDigest
import kotlin.math.*

const val COPY_AND_COMPARE_PROGRESS_NOTIFY_SKIP_COUNT = 5000 // Each one is one kB (1024 bytes)
const val SCAN_PROGRESS_NOTIFY_SKIP_COUNT = 600 // Each one is one file

fun File.getAllSubFiles(onFileScanned : () -> Unit = {}): List<File> {
    val files = mutableListOf<File>()
    listFiles().forEach {
        if (it.isFile()) {
            files.add(it)
            onFileScanned()
        } else if (it.isDirectory()) {
            files.addAll(it.getAllSubFiles(onFileScanned))
        }
    }
    return files.toList()
}


fun File.rename(newName: String): Boolean {
    
    val file2 = Paths.get(parent).resolve(newName).toFile()

    if (!file2.exists()) return this.renameTo(file2) else return false

}

fun copyFile(from : String, to : String, onProgress : (progress : Float, writed : Long) -> Unit) : Boolean {
	val fromFile = File(from)
	val toFile = File(to)
    var fin = FileInputStream(fromFile)
    var fout = FileOutputStream(toFile)

	val length = fromFile.length()
    var counter = 0L
    var r : Int
    var b = ByteArray(1024)
    var progressNotifyDelayer = 0L
    while( fin.read(b).apply {r = this} != -1) {
        counter += r
        progressNotifyDelayer += 1
        if(progressNotifyDelayer % COPY_AND_COMPARE_PROGRESS_NOTIFY_SKIP_COUNT == 0L) {
            onProgress(counter.toFloat() / length, counter)
        }
        fout.write(b, 0, r)
    }
    fin.close()
    fout.close()
    return true
}

private fun isEqual(firstFile: Path, secondFile: Path, onProgress : (progress : Float) -> Unit = {_ -> Unit}): Boolean {
    if (Files.size(firstFile) != Files.size(secondFile)) {
        return false
    }
    val file1 = firstFile.toFile()
	val file2 = secondFile.toFile()

    val length = file1.length()

    var fin1 = FileInputStream(file1)
    var fin2 = FileInputStream(file2)

    var b1 = ByteArray(1024)
    var b2 = ByteArray(1024)

    var r : Int
    var counter = 0L

    var progressNotifyDelayer = 0L
    while( fin1.read(b1).apply {r = this} != -1 && fin2.read(b2) != -1 ) {
        if(b1 contentEquals b2) {
            counter += r
            progressNotifyDelayer += 1
            if(progressNotifyDelayer % COPY_AND_COMPARE_PROGRESS_NOTIFY_SKIP_COUNT == 0L) {
                onProgress(counter.toFloat() / length)
            }
        } else {
            return false
        }
    }

    fin1.close()    
    fin2.close()    

    return true
}

fun main(args: Array<String>) {
    val path1 = Paths.get(args[0]).normalize()
    val path2 = Paths.get(args[1]).normalize()
    val parent1 = path1.toFile()
    val parent2 = path2.toFile()

    if (!(parent1.isDirectory() && parent2.isDirectory())) {
        throw IllegalArgumentException("Paths most be directory")
    }

    var compareStateLenght = 0

    println("Scan source folder for files ...")
    var forPrint : String
    var scannedFilesCount = 0
    val allFiles1 =
        parent1.getAllSubFiles({
            scannedFilesCount += 1
            if (scannedFilesCount % SCAN_PROGRESS_NOTIFY_SKIP_COUNT == 0) {
                forPrint = "\rScanning : \u001B[0;31m$scannedFilesCount\u001B[0m scanned"
                if (compareStateLenght < forPrint.length) {
                    print(forPrint)
                } else {
                    print(forPrint + " ".repeat(compareStateLenght - forPrint.length))
                }
                compareStateLenght = forPrint.length   
            }
        })
    val allFiles1Size = allFiles1.size

    forPrint = "\rScanning completed with \u001B[0;31m$allFiles1Size\u001B[0m files\n"
    if (compareStateLenght < forPrint.length) {
        print(forPrint)
    } else {
        print(forPrint + " ".repeat(compareStateLenght - forPrint.length))
    }
    compareStateLenght = forPrint.length   

    var resultEquals = ""
    var resultActions = ""
    val renameQueue = mutableSetOf<Pair<File, File>>()
    val copyQueue = mutableSetOf<Pair<File, File>>()

    for(fileIndex in IntRange(0, allFiles1.size - 1)) {

        val file1 : File = allFiles1[fileIndex]
        val file1PathStr : String = path1.relativize(file1.toPath()).normalize().toString()

        val sameInFolder2Path = path2.resolve(path1.relativize(file1.toPath()))
        val file2 = sameInFolder2Path.toFile()

        forPrint = "\rCompare : \u001B[0;31m${((fileIndex + 1).toFloat() * 100 / allFiles1Size).roundToInt()}%\u001B[0m | current : ${"%,d".format(file1.length())} bytes"
        if (compareStateLenght < forPrint.length) {
            print(forPrint)
        } else {
            print(forPrint + " ".repeat(compareStateLenght - forPrint.length))
        }
        compareStateLenght = forPrint.length

        if(file2.exists()) {
            if(isEqual(file1.toPath(), file2.toPath(), { currentCompareProgress ->
                forPrint = "\rCompare : \u001B[0;31m${((fileIndex + 1).toFloat() * 100 / allFiles1Size).roundToInt()}%\u001B[0m | current \u001B[0;31m${(currentCompareProgress*100).roundToInt()}%\u001B[0m : ${"%,d".format(file1.length())} bytes"
                if (compareStateLenght < forPrint.length) {
                    print(forPrint)
                } else {
                    print(forPrint + " ".repeat(compareStateLenght - forPrint.length))
                }
                compareStateLenght = forPrint.length
            })) {
                resultEquals += "equels : \u001B[0;31m." + file1PathStr + "\u001B[0m\n"
            } else {
                renameQueue.add(file1 to file2)
                resultActions += 
                    "Name equals but files changed (version creation) : \u001B[0;31m." + file1PathStr + "\u001B[0m\n"
            }
        } else {
            copyQueue.add(file1 to file2)
            resultActions += "Add and Copy : \u001B[0;31m." + file1PathStr + "\u001B[0m\n"
        }
        
    }

    print("\r" + " ".repeat(compareStateLenght))
    println("\nAction calcution ended with \u001B[0;31m${renameQueue.size + copyQueue.size}\u001B[0m actions")
    println("Start doing actions")

    var totalSize = 0L
    renameQueue.forEach { pair ->
        totalSize += pair.first.length()
    }
    copyQueue.forEach { pair ->
        totalSize += pair.first.length()
    }

    println("Total actions moving size : ${"%,d".format(totalSize)} bytes")

    var writedBefore = 0L
    var copyStateLenght = 0

    val onCopyProgressUpdated : (progress : Float, writed : Long, fileSizeFormatted : String) -> Unit = { progress, writed, fileSize ->
        val totalPrecent = (((writed + writedBefore) / totalSize.toFloat()) * 100).roundToInt()
        forPrint = "\rTotal : \u001B[0;31m$totalPrecent%\u001B[0m | current : ${(progress * 100).roundToInt()}% from $fileSize bytes"
        if (copyStateLenght < forPrint.length) {
            print(forPrint)
        } else {
            print(forPrint + " ".repeat(copyStateLenght - forPrint.length))
        }
        copyStateLenght = forPrint.length
    }

    println("Actions : ")
    print(resultActions)
    println("----------------")
    print(resultEquals)
    println("----------------")
    println("Are you sure to do above actions ? (Y/N)")
    val response = readln()
    if (!response.uppercase().startsWith("Y")){
        println("Operation canceled")
        return
    }

    renameQueue.forEach { pair ->
        val file1 = pair.first
        val file2 = pair.second
        val fileSize = "%,d".format(file1.length())
        try {
            file2.rename("(Version 2) " + file2.name)
            val from = file1.toPath() // \lozi\mozi\
            val to = file2.toPath().resolveSibling("(Version 1) " + file2.name)
            copyFile(from.toAbsolutePath().toString(), to.toAbsolutePath().toString()) { progress, writed ->
                onCopyProgressUpdated(progress, writed, fileSize)
            }
            file1.rename("(Version 1) " + file1.name)
        } catch (e: Exception) {
            println(
                "\nCatch on : " +
                    file1.path +
                    " Name and folder equals but files changed " +
                    file2.path +
                    " and versions created\n"
            )
            e.printStackTrace()
        }
        writedBefore += file1.length()
    }

    copyQueue.forEach { pair ->
        val sourceFile = pair.first
        val destinationFile = pair.second
        val fileLength = sourceFile.length()
        val fileSize = "%,d".format(fileLength)
        try {
            destinationFile.parentFile.mkdirs()
            copyFile(sourceFile.path, destinationFile.path) { progress, writed ->
                onCopyProgressUpdated(progress, writed, fileSize)
            }
        } catch (e: Exception) {
            println(
                "\nCatch on : " +
                    sourceFile.path +
                    " copying to " +
                    destinationFile.path +
                    "\n"
            )
            e.printStackTrace()
        }
        writedBefore += fileLength
    }

    forPrint = "\rActions completed !"
    if (copyStateLenght < forPrint.length) {
        print(forPrint)
    } else {
        print(forPrint + " ".repeat(copyStateLenght - forPrint.length))
    }

}
