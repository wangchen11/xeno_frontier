package person.wangchen11.gdx.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Array

/**
 * 粒子效果管理器
 * 负责创建和管理游戏中的粒子效果
 */
object ParticleManager {
    private val particleEffects = Array<ParticleEffectPool.PooledEffect>()
    private val particlePools = mutableMapOf<String, ParticleEffectPool>()

    /**
     * 加载粒子效果
     */
    fun loadParticleEffect(name: String, path: String) {
        try {
            val effect = ParticleEffect()
            effect.load(Gdx.files.internal(path), Gdx.files.internal(""))
            val pool = ParticleEffectPool(effect, 1, 10)
            particlePools[name] = pool
        } catch (e: Exception) {
            Gdx.app.error("ParticleManager", "Failed to load particle effect: $path", e)
        }
    }

    /**
     * 创建粒子效果
     */
    fun createParticleEffect(name: String, x: Float, y: Float): ParticleEffectPool.PooledEffect? {
        val pool = particlePools[name]
        if (pool != null) {
            val effect = pool.obtain()
            effect.setPosition(x, y)
            particleEffects.add(effect)
            return effect
        }
        return null
    }

    /**
     * 更新粒子效果
     */
    fun update(delta: Float) {
        val iterator = particleEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.update(delta)
            if (effect.isComplete) {
                effect.free()
                iterator.remove()
            }
        }
    }

    /**
     * 绘制粒子效果
     */
    fun draw(batch: SpriteBatch) {
        for (effect in particleEffects) {
            effect.draw(batch)
        }
    }

    /**
     * 清理所有粒子效果
     */
    fun clear() {
        for (effect in particleEffects) {
            effect.free()
        }
        particleEffects.clear()
    }

    /**
     * 释放所有资源
     */
    fun dispose() {
        clear()
        particlePools.clear()
    }
}
