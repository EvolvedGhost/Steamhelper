/**
 * SteamData.kt
 * 储存一些插件其他全局静态存储的数据
 */

package com.evolvedghost.mirai.steamhelper.steamhelper

/**
 * 涵盖所有对应的货币
 * 诚然，SteamApi会给货币种类
 * 但是如果App为免费或者获取失败时没用
 * 为避免大规模的失效现象以此为备用选项
 * 生成脚本可见项目文件 /script/?.kt
 */
val area4currency = mapOf(
    "TW" to "TWD", // 台湾省
    "AF" to "AFN", // 阿富汗
    "AL" to "ALL", // 阿尔巴尼亚
    "DZ" to "DZD", // 阿尔及利亚
    "AS" to "USD", // 美属萨摩亚
    "AD" to "EUR", // 安道尔
    "AO" to "AOA", // 安哥拉
    "AI" to "XCD", // 安圭拉
    "AQ" to "null", // 南极洲
    "AG" to "XCD", // 安提瓜和巴布达
    "AR" to "ARS", // 阿根廷
    "AM" to "AMD", // 亚美尼亚
    "AW" to "AWG", // 阿鲁巴
    "AU" to "AUD", // 澳大利亚
    "AT" to "EUR", // 奥地利
    "AZ" to "AZN", // 阿塞拜疆
    "BS" to "BSD", // 巴哈马
    "BH" to "BHD", // 巴林
    "BD" to "BDT", // 孟加拉国
    "BB" to "BBD", // 巴巴多斯
    "BY" to "BYN", // 白俄罗斯
    "BE" to "EUR", // 比利时
    "BZ" to "BZD", // 伯利兹
    "BJ" to "XOF", // 贝宁
    "BM" to "BMD", // 百慕大
    "BT" to "INR", // 不丹
    "BO" to "BOB", // 玻利维亚(多民族国)
    "BQ" to "USD", // 博纳尔
    "BA" to "BAM", // 波斯尼亚和黑塞哥维那
    "BW" to "BWP", // 博茨瓦纳
    "BV" to "NOK", // 布维岛
    "BR" to "BRL", // 巴西
    "IO" to "USD", // 英属印度洋领土
    "VG" to "USD", // 英属维尔京群岛
    "BN" to "BND", // 文莱达鲁萨兰国
    "BG" to "BGN", // 保加利亚
    "BF" to "XOF", // 布基纳法索
    "BI" to "BIF", // 布隆迪
    "CV" to "CVE", // 佛得角
    "KH" to "KHR", // 柬埔寨
    "CM" to "XAF", // 喀麦隆
    "CA" to "CAD", // 加拿大
    "KY" to "KYD", // 开曼群岛
    "CF" to "XAF", // 中非共和国
    "TD" to "XAF", // 乍得
    "CL" to "CLP", // 智利
    "CN" to "CNY", // 中国
    "HK" to "HKD", // 中国香港特别行政区
    "MO" to "MOP", // 中国澳门特别行政区
    "CX" to "AUD", // 圣诞岛
    "CC" to "AUD", // 科科斯(基林)群岛
    "CO" to "COP", // 哥伦比亚
    "KM" to "KMF", // 科摩罗
    "CG" to "XAF", // 刚果
    "CK" to "NZD", // 库克群岛
    "CR" to "CRC", // 哥斯达黎加
    "HR" to "HRK", // 克罗地亚
    "CU" to "CUP", // 古巴
    "CW" to "ANG", // 库拉索
    "CY" to "EUR", // 塞浦路斯
    "CZ" to "CZK", // 捷克
    "CI" to "XOF", // 科特迪瓦
    "KP" to "KPW", // 朝鲜民主主义人民共和国
    "CD" to "CDF", // 刚果民主共和国
    "DK" to "DKK", // 丹麦
    "DJ" to "DJF", // 吉布提
    "DM" to "XCD", // 多米尼克
    "DO" to "DOP", // 多米尼加
    "EC" to "USD", // 厄瓜多尔
    "EG" to "EGP", // 埃及
    "SV" to "SVC", // 萨尔瓦多
    "GQ" to "XAF", // 赤道几内亚
    "ER" to "ERN", // 厄立特里亚
    "EE" to "EUR", // 爱沙尼亚
    "SZ" to "SZL", // 斯威士兰
    "ET" to "ETB", // 埃塞俄比亚
    "FK" to "null", // 福克兰群岛(马尔维纳斯)
    "FO" to "DKK", // 法罗群岛
    "FJ" to "FJD", // 斐济
    "FI" to "EUR", // 芬兰
    "FR" to "EUR", // 法国
    "GF" to "EUR", // 法属圭亚那
    "PF" to "XPF", // 法属波利尼西亚
    "TF" to "EUR", // 法属南方领地
    "GA" to "XAF", // 加蓬
    "GM" to "GMD", // 冈比亚
    "GE" to "GEL", // 格鲁吉亚
    "DE" to "EUR", // 德国
    "GH" to "GHS", // 加纳
    "GI" to "GIP", // 直布罗陀
    "GR" to "EUR", // 希腊
    "GL" to "DKK", // 格陵兰
    "GD" to "XCD", // 格林纳达
    "GP" to "EUR", // 瓜德罗普
    "GU" to "USD", // 关岛
    "GT" to "GTQ", // 危地马拉
    "GG" to "GBP", // 根西
    "GN" to "GNF", // 几内亚
    "GW" to "XOF", // 几内亚比绍
    "GY" to "GYD", // 圭亚那
    "HT" to "HTG", // 海地
    "HM" to "AUD", // 赫德岛和麦克唐纳岛
    "VA" to "EUR", // 教廷
    "HN" to "HNL", // 洪都拉斯
    "HU" to "HUF", // 匈牙利
    "IS" to "ISK", // 冰岛
    "IN" to "INR", // 印度
    "ID" to "IDR", // 印度尼西亚
    "IR" to "IRR", // 伊朗(伊斯兰共和国)
    "IQ" to "IQD", // 伊拉克
    "IE" to "EUR", // 爱尔兰
    "IM" to "GBP", // 马恩岛
    "IL" to "ILS", // 以色列
    "IT" to "EUR", // 意大利
    "JM" to "JMD", // 牙买加
    "JP" to "JPY", // 日本
    "JE" to "GBP", // 泽西
    "JO" to "JOD", // 约旦
    "KZ" to "KZT", // 哈萨克斯坦
    "KE" to "KES", // 肯尼亚
    "KI" to "AUD", // 基里巴斯
    "KW" to "KWD", // 科威特
    "KG" to "KGS", // 吉尔吉斯斯坦
    "LA" to "LAK", // 老挝人民民主共和国
    "LV" to "EUR", // 拉脱维亚
    "LB" to "LBP", // 黎巴嫩
    "LS" to "LSL", // 莱索托
    "LR" to "LRD", // 利比里亚
    "LY" to "LYD", // 利比亚
    "LI" to "CHF", // 列支敦士登
    "LT" to "EUR", // 立陶宛
    "LU" to "EUR", // 卢森堡
    "MG" to "MGA", // 马达加斯加
    "MW" to "MWK", // 马拉维
    "MY" to "MYR", // 马来西亚
    "MV" to "MVR", // 马尔代夫
    "ML" to "XOF", // 马里
    "MT" to "EUR", // 马耳他
    "MH" to "USD", // 马绍尔群岛
    "MQ" to "EUR", // 马提尼克
    "MR" to "MRU", // 毛里塔尼亚
    "MU" to "MUR", // 毛里求斯
    "YT" to "EUR", // 马约特
    "MX" to "MXN", // 墨西哥
    "FM" to "USD", // 密克罗尼西亚(联邦)
    "MC" to "EUR", // 摩纳哥
    "MN" to "MNT", // 蒙古
    "ME" to "EUR", // 黑山
    "MS" to "XCD", // 蒙特塞拉特
    "MA" to "MAD", // 摩洛哥
    "MZ" to "MZN", // 莫桑比克
    "MM" to "MMK", // 缅甸
    "NA" to "NAD", // 纳米比亚
    "NR" to "AUD", // 瑙鲁
    "NP" to "NPR", // 尼泊尔
    "NL" to "EUR", // 荷兰
    "NC" to "XPF", // 新喀里多尼亚
    "NZ" to "NZD", // 新西兰
    "NI" to "NIO", // 尼加拉瓜
    "NE" to "XOF", // 尼日尔
    "NG" to "NGN", // 尼日利亚
    "NU" to "NZD", // 纽埃
    "NF" to "AUD", // 诺福克岛
    "MP" to "USD", // 北马里亚纳群岛
    "NO" to "NOK", // 挪威
    "OM" to "OMR", // 阿曼
    "PK" to "PKR", // 巴基斯坦
    "PW" to "USD", // 帕劳
    "PA" to "PAB", // 巴拿马
    "PG" to "PGK", // 巴布亚新几内亚
    "PY" to "PYG", // 巴拉圭
    "PE" to "PEN", // 秘鲁
    "PH" to "PHP", // 菲律宾
    "PN" to "NZD", // 皮特凯恩
    "PL" to "PLN", // 波兰
    "PT" to "EUR", // 葡萄牙
    "PR" to "USD", // 波多黎各
    "QA" to "QAR", // 卡塔尔
    "KR" to "KRW", // 大韩民国
    "MD" to "MDL", // 摩尔多瓦共和国
    "RO" to "RON", // 罗马尼亚
    "RU" to "RUB", // 俄罗斯联邦
    "RW" to "RWF", // 卢旺达
    "RE" to "EUR", // 留尼汪
    "BL" to "EUR", // 圣巴泰勒米
    "SH" to "SHP", // 圣赫勒拿
    "KN" to "XCD", // 圣基茨和尼维斯
    "LC" to "XCD", // 圣卢西亚
    "MF" to "EUR", // 圣马丁(法属)
    "PM" to "EUR", // 圣皮埃尔和密克隆
    "VC" to "XCD", // 圣文森特和格林纳丁斯
    "WS" to "WST", // 萨摩亚
    "SM" to "EUR", // 圣马力诺
    "ST" to "STN", // 圣多美和普林西比
    "null" to "null", // 萨克
    "SA" to "SAR", // 沙特阿拉伯
    "SN" to "XOF", // 塞内加尔
    "RS" to "RSD", // 塞尔维亚
    "SC" to "SCR", // 塞舌尔
    "SL" to "SLL", // 塞拉利昂
    "SG" to "SGD", // 新加坡
    "SX" to "ANG", // 圣马丁(荷属)
    "SK" to "EUR", // 斯洛伐克
    "SI" to "EUR", // 斯洛文尼亚
    "SB" to "SBD", // 所罗门群岛
    "SO" to "SOS", // 索马里
    "ZA" to "ZAR", // 南非
    "GS" to "null", // 南乔治亚岛和南桑德韦奇岛
    "SS" to "SSP", // 南苏丹
    "ES" to "EUR", // 西班牙
    "LK" to "LKR", // 斯里兰卡
    "PS" to "null", // 巴勒斯坦国
    "SD" to "SDG", // 苏丹
    "SR" to "SRD", // 苏里南
    "SJ" to "NOK", // 斯瓦尔巴岛和扬马延岛
    "SE" to "SEK", // 瑞典
    "CH" to "CHF", // 瑞士
    "SY" to "SYP", // 阿拉伯叙利亚共和国
    "TJ" to "TJS", // 塔吉克斯坦
    "TH" to "THB", // 泰国
    "MK" to "MKD", // 前南斯拉夫的马其顿共和国
    "TL" to "USD", // 东帝汶
    "TG" to "XOF", // 多哥
    "TK" to "NZD", // 托克劳
    "TO" to "TOP", // 汤加
    "TT" to "TTD", // 特立尼达和多巴哥
    "TN" to "TND", // 突尼斯
    "TR" to "TRY", // 土耳其
    "TM" to "TMT", // 土库曼斯坦
    "TC" to "USD", // 特克斯和凯科斯群岛
    "TV" to "AUD", // 图瓦卢
    "UG" to "UGX", // 乌干达
    "UA" to "UAH", // 乌克兰
    "AE" to "AED", // 阿拉伯联合酋长国
    "GB" to "GBP", // 大不列颠及北爱尔兰联合王国
    "TZ" to "TZS", // 坦桑尼亚联合共和国
    "UM" to "USD", // 美国本土外小岛屿
    "VI" to "USD", // 美属维尔京群岛
    "US" to "USD", // 美利坚合众国
    "UY" to "UYU", // 乌拉圭
    "UZ" to "UZS", // 乌兹别克斯坦
    "VU" to "VUV", // 瓦努阿图
    "VE" to "VES", // 委内瑞拉(玻利瓦尔共和国)
    "VN" to "VND", // 越南
    "WF" to "XPF", // 瓦利斯群岛和富图纳群岛
    "EH" to "MAD", // 西撒哈拉
    "YE" to "YER", // 也门
    "ZM" to "ZMW", // 赞比亚
    "ZW" to "ZWL", // 津巴布韦
    "AX" to "EUR" // 奥兰群岛
)