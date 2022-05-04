package com.xh.flutter.assets.utils

import com.intellij.openapi.ui.Messages
import com.xh.flutter.assets.bean.ConfigBean
import com.xh.flutter.assets.bean.FileBean
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.streams.toList


object YamlFileUtils {

    //1.把数据 写入pubspec.yaml 文件
    //2.生成R.目录_文件名的dart文件，用于快速获取文件
    fun writYamlFile(arrayList: ArrayList<FileBean>, yamlPath: String, configBean: ConfigBean): String {
        val starTime = System.currentTimeMillis()
        var errorMsg = ""

        if (yamlPath.isEmpty() || arrayList.isEmpty()) {
            errorMsg = "yaml path not found or assets resource not found"
            return errorMsg
        }

        val yamlFile = File(yamlPath)
        if (!yamlFile.exists()) {
            errorMsg = "yaml file not found,place checked"
            return errorMsg
        }

        //过滤出有文件夹的
        val sourceData = arrayList.groupBy { it.dir }

        try {
            //先读取文件，然后在指定地方写入文件
            val bufferedReader = BufferedReader(FileReader(yamlFile, StandardCharsets.UTF_8))
            val sourceFileLineList = bufferedReader.lines().toList()
            val targetFileLineList = sourceFileLineList.toMutableList()
            var assetsIndex = -1
            sourceFileLineList.forEachIndexed { index, str ->
                if (str.replace(" ", "") == "#assets:") {
                    errorMsg = "Please replace \'#assets:\' -> \'assets:\' , from yaml"
                    return@forEachIndexed
                } else if (str.replace(" ", "") == "assets:") {
                    assetsIndex = index
                    return@forEachIndexed
                }
            }
            if (assetsIndex == -1) {
                errorMsg = "not found assets tag"
                return errorMsg
            }
            //清除之前有的
            if (configBean.isClearOldData) {
                clearAssetsOldData(targetFileLineList, assetsIndex)
            }
            //获取新的数据
            getNewFileData(sourceData, targetFileLineList, assetsIndex)
            //写入数据到文件
            writeData(yamlPath, targetFileLineList, bufferedReader)
            //生成一个辅助类R.dart
            if (configBean.isCreateMappingFile) {
                createDartClass(sourceData, configBean)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.localizedMessage
        }

        println("time = > " + (System.currentTimeMillis() - starTime))

        Messages.showMessageDialog("success", "tips", null)

        return errorMsg
    }

    /****
     *
     *   Class R{
     *
     *   static const String filedName="xxxx.png";
     *
     *   }
     *
     *
     */
    private fun createDartClass(sourceData: Map<String, List<FileBean>>, configBean: ConfigBean) {
        val targetFileDir =
            File(configBean.targetDir + File.separator + "lib" + File.separator + configBean.createMappingDir)

        if (!targetFileDir.exists()) {
            targetFileDir.mkdir()
        }

        val targetFile = File(targetFileDir, configBean.createMappingFileName)

        if (targetFile.exists()) {
            targetFile.delete()
        }

        targetFile.createNewFile()

        val out = BufferedWriter(OutputStreamWriter(FileOutputStream(targetFile), StandardCharsets.UTF_8))

        //FileModel(dir=/assets, fileName=crane_card_dark.png)
        //FileModel(dir=/assets/b, fileName=b.png)
        val iterator = sourceData.iterator()
        out.write("class ${configBean.createMappingClassName} {")
        while (iterator.hasNext()) {
            val next = iterator.next()
            //获取字段名称
            //1.如果是在assets目录下的 则直接是R.文件名 引用
            //2.如果是在assets的子目录 如assets/image/或者 assets/home 目录 则引用为    R.image_文件名  或者 R.home_文件名 （主要区分不同模块）
            val arr = next.key.replaceFirst("/", "").split("/")
            if (arr.size == 1) { //表示只有assets文件
                for (fileModel in next.value) {
                    out.write(
                        "\n\tstatic const String ${fileModel.fileName.split(".")[0]} = \"${
                            fileModel.dir.replaceFirst(
                                "/",
                                ""
                            )
                        }/${fileModel.fileName}\";"
                    )
                }
            } else {
                val name = next.key.replace("/assets/", "").replace("/", "_")
                for (fileModel in next.value) {
                    out.write(
                        "\n\tstatic const String ${name + "_" + fileModel.fileName.split(".")[0]} = \"${
                            fileModel.dir.replaceFirst(
                                "/",
                                ""
                            )
                        }/${fileModel.fileName}\";"
                    )
                }
            }
        }
        out.write("\n}")
        out.flush()
        out.close()
    }

    private fun writeData(
        yamlPath: String,
        targetFileLineList: MutableList<String>,
        bufferedReader: BufferedReader
    ) {
        val newFile = File(yamlPath)
        if (!newFile.exists()) {
            newFile.createNewFile()
        }
        val bufferedWriter = BufferedWriter(FileWriter(newFile, StandardCharsets.UTF_8))
        targetFileLineList.forEach {
            bufferedWriter.write(it)
            bufferedWriter.newLine()
        }
        bufferedWriter.flush()
        bufferedWriter.close()
        bufferedReader.close()
    }

    //获取新的文件数据
    private fun getNewFileData(
        sourceData: Map<String, List<FileBean>>,
        targetFileLineList: MutableList<String>,
        assetsIndex: Int
    ) {
        val iterator = sourceData.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            Collections.sort(next.value)
            //FileModel(dir=/assets, fileName=crane_card_dark.png)
            //FileModel(dir=/assets/b, fileName=b.png)
            next.value.forEachIndexed { index, fileBean ->
                val insertElement = "    - " + next.key.replaceFirst("/", "") + "/" + fileBean.fileName
                val filter = targetFileLineList.filter { insertElement.trim() == it.trim() }
                if (filter.isEmpty()) {
                    targetFileLineList.add(assetsIndex + index + 1, insertElement)
                }
            }
        }
    }

    //清除之前的老数据
    private fun clearAssetsOldData(
        targetFileLineList: MutableList<String>,
        assetsIndex: Int
    ) {
        val delList = mutableListOf<String>()
        for (i in (assetsIndex + 1) until targetFileLineList.size) {
            if (targetFileLineList[i].startsWith("    - ")) {
                delList.add(targetFileLineList[i])
            } else {
                break
            }
        }
        targetFileLineList.removeAll(delList)
    }

    fun createDartClassFromDir(sourceData: Map<String, List<FileBean>>, configBean: ConfigBean): String {
        val targetFileDir =
            File(configBean.targetDir + File.separator + "lib" + File.separator + configBean.createMappingDir)

        if (!targetFileDir.exists()) {
            targetFileDir.mkdir()
        }

        val targetFile = File(targetFileDir, configBean.createMappingFileName)

        if (targetFile.exists()) {
            targetFile.delete()
        }

        targetFile.createNewFile()

        val out = BufferedWriter(OutputStreamWriter(FileOutputStream(targetFile), StandardCharsets.UTF_8))

        //FileModel(dir=/assets, fileName=crane_card_dark.png)
        //FileModel(dir=/assets/b, fileName=b.png)
        val iterator = sourceData.iterator()
        out.write("class ${configBean.createMappingClassName} {")
        while (iterator.hasNext()) {
            val next = iterator.next()
            //获取字段名称
            //1.如果是在assets目录下的 则直接是R.文件名 引用
            //2.如果是在assets的子目录 如assets/image/或者 assets/home 目录 则引用为    R.image_文件名  或者 R.home_文件名 （主要区分不同模块）
            val arr = next.key.replaceFirst("/", "").split("/")
            if (arr.size == 1) { //表示只有assets文件
                for (fileModel in next.value) {
                    out.write(
                        "\n\tstatic const String ${fileModel.fileName.split(".")[0]} = \"${
                            fileModel.dir.replaceFirst(
                                "/",
                                ""
                            )
                        }/${fileModel.fileName.split(".")[0]}\";"
                    )
                }
            } else {
                val name = next.key.replace("/assets/", "").replace("/", "_")
                for (fileModel in next.value) {
                    out.write(
                        "\n\tstatic const String ${fileModel.fileName.split(".")[0]} = \"${fileModel.fileName.split(".")[0]}\";"
                    )
                }
            }
        }
        out.write("\n}")
        out.flush()
        out.close()
        return "ok"
    }
}