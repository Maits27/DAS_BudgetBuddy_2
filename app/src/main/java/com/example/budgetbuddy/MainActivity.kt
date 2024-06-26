package com.example.budgetbuddy

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.budgetbuddy.Remote.HTTPService
import com.example.budgetbuddy.VM.AppViewModel
import com.example.budgetbuddy.VM.PreferencesViewModel
import com.example.budgetbuddy.VM.UserViewModel
import com.example.budgetbuddy.utils.AskPermissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject


/************************************************
 ****              Main Activity             ****
 ***********************************************/
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var httpClient: HTTPService
    /**
     * View models e ID del canal para las notificaciones.
     */
    val userViewModel by viewModels<UserViewModel> ()
    val appViewModel by viewModels<AppViewModel> ()
    val preferencesViewModel by viewModels<PreferencesViewModel> ()

    /**
     * FusedLocationProviderClient o cliente de geolocalización de Google Play Services.
     */
    /**             (Requisito opcional)           **/
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * PickVisualMedia o selector de fotos para escoger imágenes de la galería.
     */
    /**             (Requisito obligatorio)           **/
    val pickMedia = registerForActivityResult(PickVisualMedia()){
        if (it!=null){
            var ivImage = ImageView(this)
            ivImage.setImageURI(it)
            val drawable: Drawable = ivImage.drawable

            // Si el drawable es una instancia de BitmapDrawable, obtener el Bitmap directamente
            if (drawable is BitmapDrawable) {
                userViewModel.setProfileImage(appViewModel.currentUser, drawable.bitmap)
            }
            Toast.makeText(this, this.getString(R.string.pick_ok), Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this,  this.getString(R.string.pick_error), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Constantes
     */
    companion object{
        const val CHANNEL_ID = "BudgetBuddy"
        const val FIREBASE_NOTIFICATION = 0
    }

    /**
     * Métodos principales de la actividad relativos al ciclo de vida
     */

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Creación del canal de notificación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setContent {
                // Método para la descarga de ficheros
                val guardarFichero: ( LocalDate, String)-> Boolean = { fecha, datos ->
                    guardarDatosEnArchivo(appViewModel, fecha, datos) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Solicitud de permisos
                    AskPermissions()
                    MyApp(
                        userViewModel = userViewModel,
                        appViewModel = appViewModel,
                        preferencesViewModel = preferencesViewModel,
                        fusedLocationClient = fusedLocationClient,
                        pickMedia = pickMedia,
                        subscribe = ::subscribeUser,
                        guardarFichero
                    )
                }
            }

    }

    /**
     * Código de: https://www.geeksforgeeks.org/android-jetpack-compose-external-storage/
     */

    fun guardarDatosEnArchivo(
        appViewModel: AppViewModel,
        fecha: LocalDate,
        datos:String
    ): Boolean{
        val nombre = appViewModel.fecha_txt(fecha)
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 23
        )

        val folder: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(folder, "Factura${nombre}_${UUID.randomUUID()}.txt")

        return writeTextData(file, datos)
    }
    fun writeTextData(
        file: File,
        data: String
    ):Boolean {
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(data.toByteArray())
        } catch (e: Exception) {
            return false
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: IOException) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Método de creación del canal:
     * Android Developers: https://developer.android.com/develop/ui/views/notifications/build-notification?hl=es-419
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_description)
            }

            // Register the channel with the system.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Método de subscripción al canal de firebase
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun subscribeUser() {
        /**
         * https://firebase.google.com/docs/cloud-messaging/android/client?hl=es-419
         */
        val fcm = FirebaseMessaging.getInstance()
        fcm.deleteToken().addOnSuccessListener {
            fcm.token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d("FCM", "Fallo de FCM: ", task.exception)
                    return@OnCompleteListener
                }

                GlobalScope.launch(Dispatchers.IO) {
                    Log.d("SUBSCRIBIR", "Token: ${task.result}")
                    httpClient.subscribeUser(task.result)
                }
            })
        }
    }


}

