package person.wangchen11.gdx.drawable

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable

class ColorDrawable(val color: Color, val cornerRadius: Float = 0f): BaseDrawable() {
    companion object {
        private val emptyTexture = Texture(0,0,Pixmap.Format.RGBA8888)
        private val shaderProgram = ShaderProgram(
            """
                attribute vec3 a_position;
                uniform mat4 u_projTrans;
        
                void main() {
                    gl_Position = u_projTrans * vec4(a_position, 1.0);
                }
            """.trimIndent(),
            """
                precision mediump float;
                
                uniform vec4 color;
                uniform sampler2D u_texture;
                
                void main() {
                    gl_FragColor = color;
                    
                    if (color.r < 0.0) {
                        gl_FragColor = texture2D(u_texture, vec2(0.0));
                        return;
                    }
                }
            """.trimIndent()).assertCompiled()
    }
    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        batch.shader = shaderProgram
        shaderProgram.setUniformf("color", color)
        batch.draw(emptyTexture, x + leftWidth, y + bottomHeight , width - leftWidth - rightWidth, height - topHeight - bottomHeight)
        batch.shader = null
        // super.draw(batch, x, y, width, height)
    }
}

fun <T: ShaderProgram> T.assertCompiled(): T {
    val log = this.log
    if (!isCompiled) {
        throw IllegalStateException("Shader compilation failed \n:$log")
    }
    if (this.log.isNotBlank()) {
        Gdx.app.applicationLogger.error("ShaderHelper", "shader log:$log")
    }
    return this
}