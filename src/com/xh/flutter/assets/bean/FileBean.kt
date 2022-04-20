package com.xh.flutter.assets.bean


data class FileBean(
    val dir: String,
    val fileName: String
) : Comparable<FileBean> {
    override fun compareTo(other: FileBean): Int {
        return fileName.compareTo(other.fileName)
    }

}