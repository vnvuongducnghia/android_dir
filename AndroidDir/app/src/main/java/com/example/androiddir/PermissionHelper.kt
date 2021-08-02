package jp.co.secom.design.cloudcam.core.helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import jp.co.secom.design.cloudcam.core.util.CommonUtils.getApplicationName
import jp.co.secom.design.cloudcam.features.dialog.DialogAlert
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set


class PermissionHelper {

    /* Init Permission helper --- */
    private var mActivity: Activity? = null
    private var mFragment: Fragment? = null
    private var mOnPermissionListener: PermissionListener? = null
    private var mCurrentType: PermissionType? = null
    private var mSharedPre: SharedPreferences? = null
    private val mPackageName: String
        get() {
            return getActivity()?.packageName ?: ""
        }

    companion object {
        const val REQUEST_CODE_ASK_PERMISSIONS = 1122
        const val IS_FIRST_TIME_REQUEST_PERMISSION_CAMERA = "IS_FIRST_TIME_REQUEST_PERMISSION_CAMERA"
        const val IS_FIRST_TIME_REQUEST_PERMISSION_GALLERY = "IS_FIRST_TIME_REQUEST_PERMISSION_GALLERY"
        const val IS_FIRST_TIME_REQUEST_PERMISSION_READ_CONTACTS = "IS_FIRST_TIME_REQUEST_PERMISSION_READ_CONTACTS"
        const val IS_FIRST_TIME_REQUEST_PERMISSION_LOCATION = "IS_FIRST_TIME_REQUEST_PERMISSION_LOCATION"
        const val IS_FIRST_TIME_REQUEST_PERMISSION_CALL_PHONE = "IS_FIRST_TIME_REQUEST_PERMISSION_CALL_PHONE"
        const val IS_FIRST_TIME_REQUEST_PERMISSION_VIDEO_CALL = "IS_FIRST_TIME_REQUEST_PERMISSION_VIDEO_CALL"
    }

    // -------------
    // Constructor
    // -------------

    constructor(activity: Activity) {
        mActivity = activity
        mSharedPre = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    constructor(fragment: Fragment) {
        mFragment = fragment
        mSharedPre = PreferenceManager.getDefaultSharedPreferences(fragment.activity)
    }

    // -------------
    // Top Methods
    // -------------

    fun setOnPermissionListener(onPermissionListener: PermissionListener) {
        mOnPermissionListener = onPermissionListener
    }

    var isAllowPermissions = false

    fun getStatePermissions(type: PermissionType) {
        this.mCurrentType = type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val listNotGranted = java.util.ArrayList<String>()
            if (type == PermissionType.GALLERY) {
                getNotGranted(listNotGranted, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                getNotGranted(listNotGranted, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            isAllowPermissions = listNotGranted.size <= 0
        } else {
            isAllowPermissions = true
        }
    }

    fun requestPermission(type: PermissionType) {
        mCurrentType = type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = getObjPermission(type)
            if (!permissions.listNotGranted.isNullOrEmpty()) {
                val notYetGrants = permissions.listNotGranted

                if (!permissions.listRationale.isNullOrEmpty() && permissions.listNotGranted!!.size == permissions.listRationale!!.size) {
                    val preName = getNameType(type)
                    if (!mSharedPre!!.getBoolean(preName, false)) {
                        mSharedPre!!.edit().putBoolean(preName, true).apply()
                        requestPermissions(notYetGrants!!.toTypedArray(), REQUEST_CODE_ASK_PERMISSIONS)
                    } else {

                        // message list Rationale
                        val listRationale = permissions.listRationale
                        val message = StringBuilder("You need to grant access to " + listRationale!![0])
                        val size = listRationale.size
                        for (i in 1 until size) {
                            message.append(", ").append(listRationale[i])
                        }
                        mOnPermissionListener?.onCustomDialog()
                    }
                } else {
                    requestPermissions(notYetGrants!!.toTypedArray(), REQUEST_CODE_ASK_PERMISSIONS)
                }
            } else {
                mOnPermissionListener?.onGranted(mCurrentType)
            }
        } else {
            mOnPermissionListener?.onGranted(mCurrentType)
        }
    }

    /**
     * Use at onRequestPermissionsResult in Activity
     */
    fun checkResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {

                val permissionsString = getPermissionsStringByType(mCurrentType)
                val size = permissionsString.size
                val perms = HashMap<String, Int>()

                // Initial
                for (i in 0 until size) {
                    perms[permissionsString[i]] = PackageManager.PERMISSION_GRANTED
                }

                // Fill with results
                val len = permissions.size
                for (i in 0 until len) {
                    perms[permissions[i]] = grantResults[i]
                }

                for (i in 0 until size) {
                    if (perms[permissionsString[i]] != PackageManager.PERMISSION_GRANTED) {
                        mOnPermissionListener?.onDenied()
                        return
                    }
                }

                mOnPermissionListener?.onGranted(mCurrentType)
            }
        }
    }

    fun getActivity(): Activity? {
        if (mActivity != null) {
            return mActivity
        } else if (mFragment != null) {
            return mFragment!!.activity
        }
        return null
    }

    /**
     * Check permissions and after return object Permissions
     */
    private fun getObjPermission(type: PermissionType): Permissions {
        val listNotGranted = ArrayList<String>()
        val listRationale = ArrayList<String>()
        if (type == PermissionType.GALLERY) {
            getNotGrantedAndRationale(
                listNotGranted, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                listRationale, "Write External Storage"
            )
            getNotGrantedAndRationale(
                listNotGranted, Manifest.permission.READ_EXTERNAL_STORAGE,
                listRationale, "Read External Storage"
            )
        }

        val myPermissions = Permissions()
        myPermissions.listNotGranted = listNotGranted
        myPermissions.listRationale = listRationale
        return myPermissions
    }

    private fun getNotGrantedAndRationale(listNotGranted: MutableList<String>, permission: String, listRationale: MutableList<String>, message: String) {
        val activity = getActivity()
        if (activity != null) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                listNotGranted.add(permission)

                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    listRationale.add(message)
                }
            }
        }
    }

    private fun getNotGranted(listNotGranted: MutableList<String>, permission: String) {
        val activity = getActivity()
        if (activity != null) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                listNotGranted.add(permission)
            }
        }
    }

    private fun requestPermissions(permissions: Array<String>, requestCode: Int) {
        if (mActivity != null) {
            ActivityCompat.requestPermissions(mActivity!!, permissions, requestCode)
        } else {
            mFragment?.requestPermissions(permissions, requestCode)
        }
    }

    private fun getNameType(type: PermissionType): String {
        return when (type) {
            PermissionType.CAMERA -> IS_FIRST_TIME_REQUEST_PERMISSION_CAMERA
            PermissionType.GALLERY -> IS_FIRST_TIME_REQUEST_PERMISSION_GALLERY
            PermissionType.READ_CONTACTS -> IS_FIRST_TIME_REQUEST_PERMISSION_READ_CONTACTS
            PermissionType.LOCATION -> IS_FIRST_TIME_REQUEST_PERMISSION_LOCATION
            PermissionType.CALL_PHONE -> IS_FIRST_TIME_REQUEST_PERMISSION_CALL_PHONE
            PermissionType.VIDEO_CALL -> IS_FIRST_TIME_REQUEST_PERMISSION_VIDEO_CALL
        }
    }

    private fun startActivityForResult(intent: Intent, requestCode: Int) {
        mActivity?.startActivityForResult(intent, requestCode) ?: mFragment?.startActivityForResult(intent, requestCode)
    }

    private fun getPermissionsStringByType(type: PermissionType?): List<String> {
        val permissions = java.util.ArrayList<String>()
        if (type != null) {
            when (type) {
                PermissionType.CAMERA -> permissions.add(Manifest.permission.CAMERA)
                PermissionType.GALLERY -> {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                PermissionType.READ_CONTACTS -> permissions.add(Manifest.permission.READ_CONTACTS)
                PermissionType.LOCATION -> permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                PermissionType.CALL_PHONE -> permissions.add(Manifest.permission.CALL_PHONE)
                PermissionType.VIDEO_CALL -> {
                    permissions.add(Manifest.permission.CAMERA)
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        return permissions
    }

    private fun dialogDefault(message: String) {
        val builder = AlertDialog.Builder(this.mActivity!!)
        builder.setMessage(message)
            .setPositiveButton("fire") { dialog, id -> showSetting() }
            .setNegativeButton("cancel") { dialog, id ->
                dialog.dismiss() // User cancelled the dialog
            }
        builder.create().show()
    }

    fun dialogPermissionSetting(context: Context) {
        val messageJP = "カメラへのアクセスを許可する必要がございます。「設定」→「アプリ」より「${getApplicationName(context)}」を選択し、「権限」よりカメラとストレージのアクセスを許可してください。"
        DialogAlert()
            .setMessage(messageJP)
            .setTitleNegative("許可しない").onNegative { }
            .setTitlePositive("許可する").onPositive { showSetting() }
            .show(context)
    }

    fun showSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", mPackageName, null)
        intent.data = uri
        startActivityForResult(intent, REQUEST_CODE_ASK_PERMISSIONS)
    }

    // -------------
    // Class
    // -------------

    enum class PermissionType {
        CAMERA, GALLERY, READ_CONTACTS, LOCATION, CALL_PHONE, VIDEO_CALL;
    }

    interface PermissionListener {
        fun onGranted(currentType: PermissionType?)
        fun onDenied()
        fun onCustomDialog()
    }

    inner class Permissions {
        var listNotGranted: List<String>? = null
        var listRationale: List<String>? = null
    }
}