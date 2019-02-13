package android.text

object TextUtils {
    @JvmStatic
    fun isEmpty(str: CharSequence?): Boolean {
        return str == null || str == ""
    }
}