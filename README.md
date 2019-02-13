# AIUIPlayer

AIUIPlayer是用于解析播放AIUI语义结果中音频资源的播放器。

## 1. 起因

AIUI语义结果的部分信源的音频资源在升级后不再直接返回可播放的音频URL，而是需要依用对应信源方的SDK才能解析播放。

举个例子，故事技能的蜻蜓信源在未升级前返回结果如下：

``` json
{
	"category": "改变世界100年心灵童话",
	"name": "学习害怕",
	"playUrl": "http://od.open.qingting.fm/vod/00/00/0000000000000000000025243697_64.m4a?u=786&channelId=94688&programId=2444135",
	"series": "学习害怕",
	"status": "1"
}
```

升级后的结果如下：

``` json
{
	"category": "改变世界100年心灵童话",
	"name": "学习害怕",
	"resourceId": "94688,2444135",
	"series": "学习害怕",
	"source": "qingtingfm",
	"status": "1"
}
```

音频资源不再直接返回可播放的url，而是通过新增的source字段指明信源，resourceId提供特定于信源方的音频信息。

上面这条结果，qingtingfm表明信源方是蜻蜓FM，resourceId是"channelId, programId"的形式，而蜻蜓FM的SDK中的QTPlayer播放器提供了prepare(channelId, programId)接口播放此类资源。

## 2. 方案

AIUIPlayer为开发者提供了统一的播放和控制接口，在内部根据音频资源的source字段，调用不同信源方的SDK进行实际的播放、授权和控制。

目前播放支持：

- 咪咕音乐
- 直接URL资源



### 2.1 模块介绍

    -  sample_player     // 播放器集成调用示例
    -  sub_modules
          -  common      // 公共依赖
          -  player      // 播放器library
    - sub_players
          - migu         // 咪咕播放器实现


## 3. 使用

### 3.1 项目配置

在顶层目录下的build.gradle中加入jitPack的maven仓库

``` groovy
allprojects {
    repositories {
        ......
        maven {
            url uri('https://jitpack.io')
        }
    }
}
```

如果需要集成播放器，在app下的build.gradle加入player的依赖

``` groovy
dependencies {
    ......
    implementation 'com.github.pangxiezhou.AIUIPlayer:player:1004'
}
```

同时根据需要支持信源的情况，加入对应的依赖：

```groovy
dependencies {
    ......
    implementation 'com.github.pangxiezhou.AIUIPlayer:migu:1004'
}
```

### 3.2 接口调用

#### 3.2.1 播放器接口调用示例

``` kotlin

    //使用从平台申请的对应信源的标识信息进行初始化
    MiGuPlayerNative.initWith("", "", "", "xxxxx")
    
    player = AIUIPlayer(this)
    player.addListener(object : PlayerListener {
        override fun onPlayerReady() {
            titleTxt.text = "初始化成功"
            startPlaySamples()
        }

        override fun onStateChange(state: PlayState) {
            playState.text = state.name
            when (state) {
                PlayState.PLAYING -> ToggleBtn.text = "暂停"
                PlayState.PAUSED,PlayState.COMPLETE -> ToggleBtn.text = "继续"
                else -> {}
            }
        }

        override fun onMediaChange(item: MetaItem) {
            //根据播放项变化回调修改title内容
            titleTxt.text = item.title
        }

        override fun onError(error: Int, info: String) {
            titleTxt.text = "播放错误 $error $info"
        }

        override fun onPlayerRelease() {
            titleTxt.text = "未初始化"
        }

    })

    initializeBtn.setOnClickListener {
        player.initialize()
    }

    releaseBtn.setOnClickListener {
        player.release()
    }

    PreBtn.setOnClickListener {
        if (player.previous()) {
            Toast.makeText(this, "当前已是第一首", Toast.LENGTH_LONG).show()
        }
    }

    NextBtn.setOnClickListener {
        if (!player.next()) {
            Toast.makeText(this, "当前已是最后一首", Toast.LENGTH_LONG).show()
        }
    }
```

更详细的调用参考sample_player目录下代码示例。



## 4. Player状态参考

![AIUIPlayer状态参考](pictures/Status.jpg)
