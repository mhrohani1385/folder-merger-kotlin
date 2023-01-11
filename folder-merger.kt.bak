import java.io.*
import java.nio.file.*
import java.nio.file.StandardCopyOption.*
import java.security.MessageDigest
import kotlin.math.*

object MessageDigestAlgorithm {
    const val MD2 = "MD2"
    const val MD5 = "MD5"
    const val SHA_1 = "SHA-1"
    const val SHA_224 = "SHA-224"
    const val SHA_256 = "SHA-256"
    const val SHA_384 = "SHA-384"
    const val SHA_512 = "SHA-512"
    const val SHA_512_224 = "SHA-512/224"
    const val SHA_512_256 = "SHA-512/256"
    const val SHA3_224 = "SHA3-224"
    const val SHA3_256 = "SHA3-256"
    const val SHA3_384 = "SHA3-384"
    const val SHA3_512 = "SHA3-512"
}

object StringUtils {

    /** Used to build output as Hex */
    private val DIGITS_LOWER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    /** Used to build output as Hex */
    private val DIGITS_UPPER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of
     * each byte in order. The returned array will be double the length of the passed array, as it
     * takes two characters to represent any given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toLowerCase `true` converts to lowercase, `false` to uppercase
     * @return A char[] containing hexadecimal characters in the selected case
     */
    fun encodeHex(data: ByteArray, toLowerCase: Boolean): CharArray {
        return encodeHex(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of
     * each byte in order. The returned array will be double the length of the passed array, as it
     * takes two characters to represent any given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toDigits the output alphabet (must contain at least 16 chars)
     * @return A char[] containing the appropriate characters from the alphabet For best results,
     *   this should be either upper- or lower-case hex.
     */
    fun encodeHex(data: ByteArray, toDigits: CharArray): CharArray {
        val l = data.size
        val out = CharArray(l shl 1)
        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = toDigits[0xF0 and data[i].toInt() ushr 4]
            out[j++] = toDigits[0x0F and data[i].toInt()]
            i++
        }
        return out
    }
}

object HashUtils {

    const val STREAM_BUFFER_LENGTH = 1024

    fun getCheckSumFromFile(digest: MessageDigest, filePath: String): String {
        val file = File(filePath)
        return getCheckSumFromFile(digest, file)
    }

    fun getCheckSumFromFile(digest: MessageDigest, file: File): String {
        val fis = FileInputStream(file)
        val byteArray = updateDigest(digest, fis).digest()
        fis.close()
        val hexCode = StringUtils.encodeHex(byteArray, true)
        return String(hexCode)
    }

    fun getCheckSumFromFile(digest: String, file: File): String {
        val fis = FileInputStream(file)
        val byteArray = updateDigest(MessageDigest.getInstance(digest), fis).digest()
        fis.close()
        val hexCode = StringUtils.encodeHex(byteArray, true)
        return String(hexCode)
    }

    /**
     * Reads through an InputStream and updates the digest for the data
     *
     * @param digest The MessageDigest to use (e.g. MD5)
     * @param data Data to digest
     * @return the digest
     */
    private fun updateDigest(digest: MessageDigest, data: InputStream): MessageDigest {
        val buffer = ByteArray(STREAM_BUFFER_LENGTH)
        var read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        while (read > -1) {
            digest.update(buffer, 0, read)
            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        }
        return digest
    }
}

fun File.getAllSubFiles(): List<File> {
    val files = mutableListOf<File>()
    listFiles().forEach {
        if (it.isFile()) {
            files.add(it)
        } else if (it.isDirectory()) {
            files.addAll(it.getAllSubFiles())
        }
    }
    return files.toList()
}

fun File.calculateSHA256(): String =
    HashUtils.getCheckSumFromFile(MessageDigestAlgorithm.SHA_256, this)

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
    while( fin.read(b).apply {r = this} != -1) {
        counter += r
        onProgress(counter.toFloat() / length, counter)
        fout.write(b, 0, r)
    }
    fin.close()
    fout.close()
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

    println("Scan source folder for files ...")
    val allFiles1 =
        parent1.getAllSubFiles()
    val allFiles1Size = allFiles1.size
    println("Scanning completed with \u001B[0;31m$allFiles1Size\u001B[0m files")

    var resultEquals = ""
    var resultActions = ""
    var compareStateLenght = 0
    val renameQueue = mutableSetOf<Pair<File, File>>()
    val copyQueue = mutableSetOf<Pair<File, File>>()

    for(fileIndex in IntRange(0, allFiles1.size - 1)) {

        val file1 : File = allFiles1[fileIndex]
        val file1PathStr : String = path1.relativize(file1.toPath()).normalize().toString()

        val sameInFolder2Path = path2.resolve(path1.relativize(file1.toPath()))
        val file2 = sameInFolder2Path.toFile()

        val forPrint = "\rCompare : \u001B[0;31m${((fileIndex + 1).toFloat() * 100 / allFiles1Size).roundToInt()}%\u001B[0m | current : ${"%,d".format(file1.length())} bytes"
        if (compareStateLenght < forPrint.length) {
            print(forPrint)
        } else {
            print(forPrint + " ".repeat(compareStateLenght - forPrint.length))
        }
        compareStateLenght = forPrint.length

        if(file2.exists()) {
            if(file1.calculateSHA256() == file2.calculateSHA256()) {
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

    val onProgressUpdated : (progress : Float, writed : Long, fileSizeFormatted : String) -> Unit = { progress, writed, fileSize ->
        val totalPrecent = (((writed + writedBefore) / totalSize.toFloat()) * 100).roundToInt()
        val forPrint = "\rTotal : \u001B[0;31m$totalPrecent%\u001B[0m | current : ${(progress * 100).roundToInt()}% from $fileSize bytes"
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
                onProgressUpdated(progress, writed, fileSize)
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
                onProgressUpdated(progress, writed, fileSize)
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

    val forPrint = "\rActions completed !"
    if (copyStateLenght < forPrint.length) {
        print(forPrint)
    } else {
        print(forPrint + " ".repeat(copyStateLenght - forPrint.length))
    }

}
