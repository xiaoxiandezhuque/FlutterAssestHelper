package com.xh.flutter.assets

import com.xh.flutter.assets.bean.ConfigBean
import com.xh.flutter.assets.bean.FileBean
import com.xh.flutter.assets.common.Constants
import com.xh.flutter.assets.utils.YamlFileUtils
import java.io.File


class FlutterAssetsHelper(private val configBean: ConfigBean) {

    /**
     * 获取所有资源文件
     */
    private fun getAssetsAllFiles(
        hashMap: HashMap<String, FileBean>,
        files: MutableList<File>,
        isIgnorePreviousDirectory: Boolean = false
    ) {
        for (file in files) {
            if (file.isDirectory) {
                if (file.name.startsWith("1.") ||
                    file.name.startsWith("2.") ||
                    file.name.startsWith("3.") ||
                    file.name.startsWith("4.")
                ) {
                    file.listFiles()?.let {
                        val list = mutableListOf<File>()
                        list.addAll(it)
                        getAssetsAllFiles(hashMap, list, true)
                    }
                } else {
                    file.listFiles()?.let {
                        val list = mutableListOf<File>()
                        list.addAll(it)
                        getAssetsAllFiles(hashMap, list)
                    }
                }
            } else {
                var dir = file.parentFile.absolutePath.replace("\\", "/")
                    .replace(configBean.targetDir.replace("\\", "/"), "")
                if (isIgnorePreviousDirectory) {
                    val list = dir.split("/").toMutableList()
                    if (list.size > 1) {
                        list[list.size - 1] = ""
                    }
                    dir = ""
                    list.forEach {
                        dir = if (it.isEmpty()) {
                            dir
                        } else {
                            dir + "/" + it
                        }
                    }
                }
                hashMap.put("${dir}/${file.name}", FileBean(dir, file.name))
            }
        }
    }

    fun startWork(): String {
        val files = mutableListOf<File>()
        for (scanDir in configBean.scanDirs) {
            val assetsFileDir = File((scanDir).replace("\\", "/"))
            val arrayOfFiles = assetsFileDir.listFiles()
            arrayOfFiles?.let {
                files.addAll(arrayOfFiles)
            }
        }
        //获取所有文件
        //过滤 2.0x  3.0x 之类的相同图片，所以先用hashmap
        val fileHasMap = hashMapOf<String, FileBean>()
        getAssetsAllFiles(fileHasMap, files)

        val fileLists = ArrayList<FileBean>()

        filterValidFile(fileHasMap, fileLists)

        println("fileList = ${fileLists.size}")

//        val yamlPath = (configBean.targetDir + File.separator + Constants.PUBSPEC).replace("\\", "/")

//        return YamlFileUtils.writYamlFile(fileLists, yamlPath, configBean)
        val sourceData = fileLists.groupBy { it.dir }
        return YamlFileUtils.createDartClassFromDir(sourceData, configBean)
    }

    /**
     * 过滤出有效的文件
     */
    private fun filterValidFile(fileList: HashMap<String, FileBean>, fileModels: ArrayList<FileBean>) {
        val regex1 = Regex("^[0-9]") //数字开头
        val matches = regex1.toPattern()
        fileList.forEach continues@{ key, value ->
            //检查是否存在数字开头的文件名
            val isExist = matches.matcher(value.fileName).find()
            //过滤.开头的文件或者文件夹名称和数字开头的文件名称
            if (value.fileName.startsWith(".") || isExist) {
                return@continues
            } else {
                fileModels.add(value)
            }
        }
    }
}