package com.gta.widget.search

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RSRuntimeException
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur


internal class BlurHelper {
    private var rs: RenderScript? = null
    private var script: ScriptIntrinsicBlur? = null
    private var allocIn: Allocation? = null
    private var allocOut: Allocation? = null

    fun prepare(context: Context?, buffer: Bitmap?, radius: Float): Boolean {
        if (rs == null) {
            try {
                rs = RenderScript.create(context)
                script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            } catch (e: RSRuntimeException) {
                release()
                return false
            }
        }
        script?.setRadius(radius)

        allocIn?.destroy()
        allocOut?.destroy()

        allocIn = Allocation.createFromBitmap(
            rs, buffer,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        ).also { allocOut = Allocation.createTyped(rs, it.type) }

        return true
    }

    fun blur(input: Bitmap?, output: Bitmap?) {
        allocIn?.copyFrom(input)
        script?.setInput(allocIn)
        script?.forEach(allocOut)
        allocOut?.copyTo(output)
    }

    fun release() {
        allocIn?.destroy();  allocIn  = null
        allocOut?.destroy(); allocOut = null
        script?.destroy();   script   = null
        rs?.destroy();       rs       = null
    }
}