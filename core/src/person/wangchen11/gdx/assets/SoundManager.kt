package person.wangchen11.gdx.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/**
 * 音效管理器
 * 负责加载和管理游戏中的音效和音乐
 */
object SoundManager {
    private val sounds = mutableMapOf<String, Sound>()
    private val musicMap = mutableMapOf<String, Music>()
    private var currentMusic: Music? = null

    /**
     * 初始化音效管理器
     */
    fun initialize() {
        // 加载音效
        loadSound("build", "sounds/build.wav")
        loadSound("harvest", "sounds/harvest.wav")
        loadSound("attack", "sounds/attack.wav")
        loadSound("enemy_death", "sounds/enemy_death.wav")
        loadSound("button_click", "sounds/button_click.wav")
        
        // 加载音乐
        loadMusic("background", "music/background.mp3")
        loadMusic("battle", "music/battle.mp3")
    }

    /**
     * 加载音效
     */
    fun loadSound(name: String, path: String) {
        try {
            if (Gdx.files.internal(path).exists()) {
                val sound = Gdx.audio.newSound(Gdx.files.internal(path))
                sounds[name] = sound
            } else {
                Gdx.app.error("SoundManager", "Sound file not found: $path")
            }
        } catch (e: Exception) {
            Gdx.app.error("SoundManager", "Failed to load sound: $path", e)
        }
    }

    /**
     * 加载音乐
     */
    fun loadMusic(name: String, path: String) {
        try {
            if (Gdx.files.internal(path).exists()) {
                val music = Gdx.audio.newMusic(Gdx.files.internal(path))
                musicMap[name] = music
            } else {
                Gdx.app.error("SoundManager", "Music file not found: $path")
            }
        } catch (e: Exception) {
            Gdx.app.error("SoundManager", "Failed to load music: $path", e)
        }
    }

    /**
     * 播放音效
     */
    fun playSound(name: String, volume: Float = 1.0f): Long {
        val sound = sounds[name]
        return sound?.play(volume) ?: -1L
    }

    /**
     * 播放音乐
     */
    fun playMusic(name: String, loop: Boolean = true, volume: Float = 0.5f) {
        // 停止当前播放的音乐
        currentMusic?.stop()
        
        // 播放新音乐
        val music = musicMap[name]
        music?.let {
            it.volume = volume
            it.isLooping = loop
            it.play()
            currentMusic = it
        }
    }

    /**
     * 停止当前音乐
     */
    fun stopMusic() {
        currentMusic?.stop()
        currentMusic = null
    }

    /**
     * 暂停当前音乐
     */
    fun pauseMusic() {
        currentMusic?.pause()
    }

    /**
     * 恢复当前音乐
     */
    fun resumeMusic() {
        currentMusic?.play()
    }

    /**
     * 释放所有资源
     */
    fun dispose() {
        sounds.values.forEach { it.dispose() }
        musicMap.values.forEach { it.dispose() }
        sounds.clear()
        musicMap.clear()
        currentMusic = null
    }
}
