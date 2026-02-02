package person.wangchen11.gdx.drawable

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import person.wangchen11.gdx.assets.dp

class ModernDrawable(
    var backgroundColor: Color = Color.WHITE,
    var borderColor: Color = Color.CLEAR,
    var borderWidth: Float = 0f,
    var cornerRadius: Float = 0f,
    var gradientColor: Color? = null
) : BaseDrawable() {

    companion object {
        private val emptyTexture = Texture(1, 1, Pixmap.Format.RGBA8888).apply {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            draw(pixmap, 0, 0)
            pixmap.dispose()
        }

        private val shaderProgram = ShaderProgram(
            """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color = a_color;
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
            """.trimIndent(),
            """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec2 u_size;
            uniform vec4 u_bgColor;
            uniform vec4 u_borderColor;
            uniform float u_borderWidth;
            uniform float u_cornerRadius;
            uniform vec4 u_gradientColor;
            uniform int u_useGradient;

            float roundedRectSDF(vec2 p, vec2 b, float r) {
                vec2 d = abs(p) - b + vec2(r);
                return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - r;
            }

            void main() {
                vec2 center = u_size * 0.5;
                vec2 p = v_texCoords * u_size - center;
                
                float dist = roundedRectSDF(p, center, u_cornerRadius);
                
                // Antialiasing factor
                float af = fwidth(dist);
                
                // Background and Gradient
                vec4 fill = u_bgColor;
                if (u_useGradient == 1) {
                    fill = mix(u_bgColor, u_gradientColor, v_texCoords.y);
                }
                
                // Alpha masking for rounded corners
                float alpha = 1.0 - smoothstep(-af, af, dist);
                
                // Border logic
                float borderAlpha = smoothstep(-u_borderWidth - af, -u_borderWidth + af, dist);
                vec4 finalColor = mix(fill, u_borderColor, borderAlpha);
                
                vec4 texColor = texture2D(u_texture, v_texCoords);
                gl_FragColor = finalColor * vec4(1.0, 1.0, 1.0, alpha) * texColor.a;
            }
            """.trimIndent()
        ).assertCompiled()
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        val oldShader = batch.shader
        batch.shader = shaderProgram
        
        shaderProgram.setUniformf("u_size", width, height)
        shaderProgram.setUniformf("u_bgColor", backgroundColor)
        shaderProgram.setUniformf("u_borderColor", borderColor)
        shaderProgram.setUniformf("u_borderWidth", borderWidth)
        shaderProgram.setUniformf("u_cornerRadius", cornerRadius)
        
        if (gradientColor != null) {
            shaderProgram.setUniformf("u_gradientColor", gradientColor!!)
            shaderProgram.setUniformi("u_useGradient", 1)
        } else {
            shaderProgram.setUniformi("u_useGradient", 0)
        }
        
        batch.draw(emptyTexture, x, y, width, height)
        batch.shader = oldShader
    }
}
