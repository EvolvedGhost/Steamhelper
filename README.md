# Steamhelper

一个以 [Mirai-Console](https://github.com/mamoe/mirai) 为基础开发的Steam小插件

## 快速入门

插件需要以 [Mirai-Console](https://github.com/mamoe/mirai)
为基础，你可以下载 [MCL](https://github.com/iTXTech/mirai-console-loader/releases) 作为你的Mirai插件载入器

与此同时，本插件需要 Mirai 官方插件 [chat-command](https://github.com/project-mirai/chat-command)

因为一些众所周知的原因，国内连接 Steam
并不顺畅，尽管本插件有着超时重试的功能但是在使用过程中还是强烈建议使用羽翼城大佬的 [SteamCommunity302](https://www.dogfight360.com/blog/686/)
（Linux/MacOS使用302请参见 [此处](https://www.dogfight360.com/blog/2319/) ）

使用之前请先用参考下面权限节来开启插件权限

## 权限

| 权限                                                        | 功能                           |
|-----------------------------------------------------------|------------------------------|
| `com.evolvedghost.mirai.steamhelper.steamhelper:command.sh` | Steamhelper基础指令，不开启无法使用      |
| `com.evolvedghost.mirai.steamhelper.steamhelper:sub`        | Steamhelper订阅权限，可以防止订阅消息过多过吵 |
| `com.evolvedghost.mirai.steamhelper.steamhelper:push`       | Steamhelper推送权限，可以防止有人随意开关推送 |
| `com.evolvedghost.mirai.steamhelper.steamhelper:reload`     | Steamhelper重载权限，可以让指定用户有权限重载 |

开启全部权限请在控制台输入：

`/permission add * com.evolvedghost.mirai.steamhelper.steamhelper:command.sh`

`/permission add * com.evolvedghost.mirai.steamhelper.steamhelper:sub`

`/permission add * com.evolvedghost.mirai.steamhelper.steamhelper:push`

`/permission add * com.evolvedghost.mirai.steamhelper.steamhelper:reload`

## 指令

`<sh, #sh>` 意为 `sh` 或者 `#sh` 均可执行指令

`[AppID, 关键字]` 意为输入为 `SteamAppID` 或者 `SteamApp搜索关键字` 均可执行指令

| 指令                                      | 功能                         |
|-----------------------------------------|----------------------------|
| `/<sh, #sh> <cp, 比价> [AppID, 关键字]`      | 对比某SteamApp各区域的价格          |
| `/<sh, #sh> <week, 周榜>`                 | 获取Steam每周销量榜单              |
| `/<sh, #sh> <sale, 促销>`                 | 获取最近的Steam促销               |
| `/<sh, #sh> <stat, 状态>`                 | 获取最近的Steam状态               |
| `/<sh, #sh> <sr, 搜索> [AppID, 关键字]`      | 搜索一个SteamApp               |
| `/<sh, #sh> <sub, 订阅> [AppID, 关键字]`     | 订阅一个SteamApp的价格变化（需要sub权限） |
| `/<sh, #sh> <unsub, 取消订阅> [AppID, 关键字]` | 取消订阅一个SteamApp（需要sub权限）    |
| `/<sh, #sh> <list, 查看订阅>`               | 查看该会话下的所有订阅 （需要sub权限）      |
| `/<sh, #sh> <unall, 取消全部订阅>`            | 取消该会话下的所有订阅 （需要sub权限）      |
| `/<sh, #sh> <push, 推送>`                 | 定时推送大促、周榜信息 （需要push权限）     |
| `/<sh, #sh> <reload, 重载> [AppID, 关键字]`  | 重载Steamhelper（需要reload权限）  |

## 设置

位于 `Mirai-Console` 运行目录下的 `config/com.evolvedghost.mirai.steamhelper.steamhelper` 文件夹下

### Steamhelper.yml

#### 常规设置：

* `urlWeekly` Steam每周榜单获取源，默认为`https://store.steampowered.com/feeds/weeklytopsellers.xml`
* `urlInfo` Steam状态与大促情况获取源，默认为`https://keylol.com/`

```你可以配合 Github Action 和 script/mirrorCrawler.py 为 urlWeekly urlInfo 做一个镜像源```
* `timeout` 连接超时时间，单位毫秒，默认为`3000`
* `retry` 连接超时时间重试次数，默认为`3`
* `errors` 推送、订阅信息发送超过指定错误次数后自动删除，成功一次后会重新计数，默认为`5`
* `debug` 调试模式，会尽可能输出插件的所有Exception，默认为`false`
* `timeFormat` 时间输出模式，格式请参阅 [此处](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)
  ，默认为`'yyyy-MM-dd HH:mm:ss EEEE'`
* `timeZone` 时间输出时区，默认为`'GMT+8:00'`
* `timePushSale`
  大促推送时间，格式请参阅 [此处](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
  并在 [这里](https://tool.lu/crontab/) 选择Java(Quartz)测试无误后再填入，默认为`'0 0 8,20 * * ?'`
* `timePushWeek`
  大促推送时间，格式请参阅 [此处](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
  并在 [这里](https://tool.lu/crontab/) 选择Java(Quartz)测试无误后再填入，默认为`'0 0 12 ? * MON'`
* `timeRefresh`
  大促推送时间，格式请参阅 [此处](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
  并在 [这里](https://tool.lu/crontab/) 选择Java(Quartz)测试无误后再填入，默认为`'0 0 * * * ?'`
* `areasSearch` 搜索区域设定，插件将从第一个区域依次搜索所有的区域的数据直到有个正确的返回，最慢速度理论为`超时重试次数`×`连接超时时间`×`areasSearch数量`
  ，部分区域代码已在 [此处](https://github.com/EvolvedGhost/Steamhelper/docs/area.md)
  标明，默认为
  ```
  - US
  - CN
  - JP
  ```
* `areasPrice`
  价格区域设定，比价功能将显示所有在该列表中的区域，订阅功能将以第一位作为价格基准，部分区域代码已在 [此处](https://github.com/EvolvedGhost/Steamhelper/docs/area.md)
  标明，默认为
  ```
  - CN
  - RU
  - TR
  - AR
  ```

#### 自定义消息设置：

* `messageWeek` 自定义周榜参数，可用参数如下：

> 换行请使用\n，其他特殊字符同理  
> &lt;tf&gt;=榜单更新时间（以timeFormat格式）  
> &lt;ts&gt;=榜单更新时间（以时间戳的格式）  
> &lt;ls&gt;=榜单信息（在下方编辑格式，末尾默认带一个换行）  
> &lt;sc&gt;=获取来源  
> 默认为`Steam一周销量榜：\n截止时间：<tf>\n<ls>`

* `messageWeekList` 自定义周榜参数榜单信息，替换`messageWeek`的`<ls>`可用参数如下：

> 换行请使用\n，其他特殊字符同理，每项都会默认带换行  
> &lt;nm&gt;=游戏名称（带排名）  
> &lt;lk&gt;=游戏链接  
> 默认为`'<nm>'`

* `messageStatus` 自定义Steam状态信息，可用参数如下：

> 换行请使用\n，其他特殊字符同理  
> &lt;ss&gt;=商店状态（例：正常）  
> &lt;sc&gt;=社区状态（例：正常）  
> 默认为`'Steam 商店状态 : <ss> | Steam 社区状态 : <sc>'`

* `messageSale` 自定义Steam促销信息，可用参数如下：

> 换行请使用\n，其他特殊字符同理  
> &lt;nm&gt;=促销名称  
> &lt;tl&gt;=促销剩余时间（以X天X时X分X秒的格式）  
> &lt;tf&gt;=促销时间（以timeFormat格式）  
> &lt;ts&gt;=促销时间（以时间戳的格式）  
> 默认为`'<nm><tl>'`

* `messageCompare` 自定义比价参数，可用参数如下：

> 换行请使用\n，其他特殊字符同理  
> &lt;id&gt;=SteamAppid  
> &lt;nm&gt;=App名称  
> &lt;ds&gt;=App介绍  
> &lt;pl&gt;=比价价格列表  
> 默认为`"<nm>(<id>)\n<pl>"`

* `messageCompareList` 自定义比价参数榜单信息，替换`messageCompare`的`<pl>`可用参数如下：

> 换行请使用\n，其他特殊字符同理，每项都会默认带换行  
> &lt;an&gt;=当前区域名称（ISO3166双字母缩写）  
> &lt;at&gt;=换算的区域名称（以areasPrice第一位为基准）  
> &lt;cr&gt;=货币单位（ISO4217货币代码）  
> &lt;ct&gt;=换算的货币单位（以areasPrice第一位为基准）  
> &lt;ip&gt;=初始价格  
> &lt;it&gt;=换算的初始价格（以areasPrice第一位为基准）  
> &lt;fp&gt;=最终价格  
> &lt;ft&gt;=最终价格换算  
> &lt;ds&gt;=当前折扣力度  
> 默认为`'<an>:<cr><fp>(<ct><ft>)'`

* `messageCompareListError` 自定义比价参数榜单信息(错误信息如免费、锁区、获取错误等)，替换`messageCompare`的`<pl>`可用参数如下：

> 换行请使用\n，其他特殊字符同理，每项都会默认带换行  
> &lt;if&gt;=错误信息如锁区、获取错误、内部错误等  
> &lt;an&gt;=当前区域名称（ISO3166双字母缩写）  
> &lt;at&gt;=换算的区域名称（以areasPrice第一位为基准）  
> &lt;cr&gt;=货币单位（ISO4217货币代码）  
> &lt;ct&gt;=换算的货币单位（以areasPrice第一位为基准）  
> 默认为`'<an>:<if>'`

* `messageSubscribe`自定义订阅信息，可用参数如下：

> 换行请使用\n，其他特殊字符同理  
> &lt;aid&gt;=SteamAppid  
> &lt;anm&gt;=App名称  
> &lt;ads&gt;=App介绍  
> &lt;aif&gt;=App价格变动情况（降价，涨价）  
> &lt;aar&gt;=App区域名称（ISO3166双字母缩写）（以areasPrice第一位为基准）  
> &lt;acr&gt;=App货币单位（ISO4217货币代码）  
> &lt;cip&gt;=当前初始价格  
> &lt;cfp&gt;=当前最终价格  
> &lt;cds&gt;=当前折扣力度  
> &lt;oip&gt;=之前的初始价格  
> &lt;ofp&gt;=之前的最终价格  
> &lt;ods&gt;=之前的折扣力度  
> 默认为`"<anm>(<aid>)<aif>\n当前价格：<acr><cfp>(-<cds>%)\n之前价格：<acr><ofp>(-<ods>%)"`

* `messageSearch` 自定义搜索参数，可用参数如下：

> 换行请使用\n，其他特殊字符同理  
> &lt;id&gt;=SteamAppid  
> &lt;nm&gt;=App名称  
> &lt;ds&gt;=App介绍  
> 默认为`"<nm>(<id>)\n<ds>"`

## 特别感谢

[Mirai](https://github.com/mamoe/mirai) 提供QQ机器人

[Gson](https://github.com/google/gson) 提供Json解析

[Jsoup](https://jsoup.org/) 提供XML、HTML解析

[Quartz](http://www.quartz-scheduler.org/) 提供定时任务支持

[Steam-蒸汽平台](https://store.steampowered.com/) 游戏数据、周榜来源

[Keylol-其乐](https://keylol.com/) Steam状态、促销来源