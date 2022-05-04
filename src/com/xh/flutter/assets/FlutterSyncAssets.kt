package com.xh.flutter.assets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.xh.flutter.assets.bean.ConfigBean
import com.xh.flutter.assets.utils.XmlUtils
import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Element
import java.io.File

class FlutterSyncAssets : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        Messages.showInfoMessage(
            "当前目录:" + e.project!!.basePath,
            "tips"
        )
        val path = "${e.project?.basePath}${File.separator}flutter_assets_config.xml"
        var configElement: Element? = null
        if (!StringUtils.isEmpty(path)) {
            configElement = XmlUtils.parse("${e.project?.basePath}${File.separator}flutter_assets_config.xml")
        }
        val configBean = ConfigBean(
            XmlUtils.getValueByAttribute(configElement, "target", "targetDir", e.project!!.basePath!!),
            XmlUtils.getValueByAttribute(
                configElement,
                "target",
                "scanDir",
                e.project!!.basePath!! + File.separator + "assets"
            ).split(","),
            XmlUtils.getValueByAttribute(configElement, "mapping", "isCreateMappingFile", "true").toBoolean(),
            XmlUtils.getValueByAttribute(configElement, "mapping", "createMappingDir", "generated"),
            XmlUtils.getValueByAttribute(configElement, "mapping", "createMappingFileName", "r.dart"),
            XmlUtils.getValueByAttribute(configElement, "mapping", "className", "R"),
            XmlUtils.getValueByAttribute(configElement, "assets-data", "isClearOldData", "true").toBoolean(),
        )

        val helper = FlutterAssetsHelper(configBean)
        val result = helper.startWork()
        if (!result.isNullOrEmpty()) {
            Messages.showMessageDialog("$result :(", "tips", null)
        }
    }
}