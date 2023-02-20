# Steamhelper

一个以 [Mirai-Console](https://github.com/mamoe/mirai) 为基础开发的Steam小插件

## 插件使用

请参考[本项目Wiki](https://github.com/EvolvedGhost/Steamhelper/wiki)

## 编译

如果您需要自行编译，使用在文件目录使用以下命令即可

Windows：`./gradlew.bat buildPlugin`

Linux：`./gradlew buildPlugin`

Mirai版本在2.12以下的用户需要向下兼容包，本项目Release并不会提供此类包，请自行编译，编译指令为：`./gradlew buildPluginLegacy`

编译好的jar文件可以在`/build/mirai`下找到

## 已知问题

插件推送采用Quartz，如您采用的多个插件均使用Quartz，请务必保证每个插件都拥有自己单独配置的properties文件，以免发生某一插件无法正常推送的冲突

## 特别感谢

[Mirai](https://github.com/mamoe/mirai) 提供QQ机器人

[Gson](https://github.com/google/gson) 提供Json解析

[Jsoup](https://jsoup.org/) 提供XML、HTML解析

[Quartz](http://www.quartz-scheduler.org/) 提供定时任务支持

[Steam-蒸汽平台](https://store.steampowered.com/) 游戏数据、周榜来源

[Keylol-其乐](https://keylol.com/) Steam状态、促销来源

[ExchangeRate-API](https://www.exchangerate-api.com/) 免费的汇率来源
