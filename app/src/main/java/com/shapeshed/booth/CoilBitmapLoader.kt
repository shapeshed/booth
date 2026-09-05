package com.shapeshed.booth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.BitmapImage
import coil3.Image
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers

/** Uses the same Coil network and SVG support as the Compose artwork on system media surfaces. */
@UnstableApi
class CoilBitmapLoader(private val context: Context) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Default, false) {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Could not decode artwork")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.IO, false) {
            val result = SingletonImageLoader.get(context).execute(
                ImageRequest.Builder(context).data(uri).size(Size(512, 512)).build(),
            ) as? SuccessResult ?: error("Could not load artwork from $uri")
            result.image.toBitmap()
        }
}

private fun Image.toBitmap(): Bitmap {
    val bitmap = createBitmap(width.takeIf { it > 0 } ?: 512, height.takeIf { it > 0 } ?: 512)
    val canvas = Canvas(bitmap)
    if (this is BitmapImage && this.bitmap.config == Bitmap.Config.HARDWARE) {
        canvas.drawBitmap(this.bitmap.copy(Bitmap.Config.ARGB_8888, false), 0f, 0f, null)
    } else if (this is BitmapImage) {
        canvas.drawBitmap(this.bitmap, 0f, 0f, null)
    } else {
        draw(canvas)
    }
    return bitmap
}
