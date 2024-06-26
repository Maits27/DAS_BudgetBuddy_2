package com.example.budgetbuddy.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.budgetbuddy.AlarmManager.AndroidAlarmScheduler
import com.example.budgetbuddy.VM.AppViewModel
import com.example.budgetbuddy.Local.Data.TipoGasto
import com.example.budgetbuddy.Local.Data.obtenerTipoEnIdioma
import com.example.budgetbuddy.Local.Room.Gasto
import com.example.budgetbuddy.R
import com.example.budgetbuddy.VM.PreferencesViewModel
import com.example.budgetbuddy.Shared.Calendario
import com.example.budgetbuddy.Shared.ErrorAlert
import com.example.budgetbuddy.Shared.MapScreen
import com.example.budgetbuddy.Shared.Subtitulo
import com.example.budgetbuddy.ui.theme.grisClaro
import com.example.budgetbuddy.utils.agregarGastoAlCalendario
import com.example.budgetbuddy.utils.toLong
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException


/***************************************************
 ***          Pantalla auxiliar Add             ***
 ***************************************************/
/**
Este composable forma la pantalla del formulario para añadir elementos.

Se le pasan los parámetros de:
 * @appViewModel:           ViewModel general de la aplicación con los métodos necesarios para editar la fecha.
 * @preferencesViewModel:   ViewModel de las preferencias del usuario.
 * @navController:          De forma que se pueda volver a la pantalla [Home].
 * @fusedLocationClient:    Cliente para la localización actual.
 * @fecha_actual:           Fecha en la que se presupone que se va a añadir el gasto.
 * @modifier:               Para dar un estilo predeterminado a los composables (default).
 */


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Add(
    appViewModel: AppViewModel,
    preferencesViewModel: PreferencesViewModel,
    navController: NavController,
    fusedLocationClient: FusedLocationProviderClient,
    fecha_actual: LocalDate,
    modifier: Modifier = Modifier.verticalScroll(rememberScrollState())
){
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scheduler = AndroidAlarmScheduler(context)

    val saveLoc by preferencesViewModel.saveLocation(appViewModel.currentUser).collectAsState(initial = true)
    val saveCalendar by preferencesViewModel.saveOnCalendar(appViewModel.currentUser).collectAsState(initial = true)
    val idioma by preferencesViewModel.idioma(appViewModel.currentUser).collectAsState(initial = preferencesViewModel.currentSetLang)
    /*******************************************************************
     **                     Valores del formulario                    **
     * (rememberSaveable para no perder datos en caso de interrupción) *
     ******************************************************************/
    var euros by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(fecha_actual) }
    var selectedOption by rememberSaveable { mutableStateOf(TipoGasto.Otros) }
    var nombre by rememberSaveable { mutableStateOf("") }

    //Location
    val lastKnownLocation by appViewModel.locationFlow.collectAsState(initial = null)

    /**    Parámetros para el control de los estados de los composables **/
    var error_message by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var enabledDate by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var isTextFieldFocused by remember { mutableStateOf(-1) }

    /**    Funciones parámetro para gestionar las acciones del estado   **/
    val onCalendarConfirm: (LocalDate) -> Unit = {
        fecha = it
        isTextFieldFocused = -1
        enabledDate = false
        keyboardController?.hide()
    }

    if (ActivityCompat.checkSelfPermission(
            LocalContext.current,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            LocalContext.current,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Log.d("NO HAY PERMISO",  ActivityCompat.checkSelfPermission(
            LocalContext.current,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).toString())
        Log.d("NO HAY PERMISO", ActivityCompat.checkSelfPermission(
            LocalContext.current,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).toString())
        Log.d("NO HAY PERMISO", "NO HAY PERMISO LOCALIZACION")
    }
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            // Got last known location. In some rare situations this can be null.
            appViewModel.cambiarLocalizacion(location)
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Subtitulo(mensaje = stringResource(id = R.string.add_element), true)

        ///////////////////////////////////////// Campo de Nombre /////////////////////////////////////////
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text(stringResource(id = R.string.name_element)) },
            keyboardActions = KeyboardActions(
                onDone = {
                    isTextFieldFocused = -1
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        isTextFieldFocused = 0
                    }
                }
        )

        ///////////////////////////////////////// Campo de Tipo /////////////////////////////////////////
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { expanded = true }),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, Color.DarkGray),
                color = grisClaro
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = obtenerTipoEnIdioma(selectedOption, idioma.code),
                        modifier = Modifier.padding(16.dp),
                        color = Color.DarkGray
                    )
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = Color.DarkGray
                        )
                    }

                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(color = MaterialTheme.colors.background),
            ) {
                TipoGasto.entries.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            selectedOption = option
                            expanded = false
                        },
                        modifier = Modifier.background(color = MaterialTheme.colors.background)
                    ) {
                        Text(
                            text = obtenerTipoEnIdioma(option, idioma.code),
                            Modifier.background(color = MaterialTheme.colors.background)
                        )
                    }
                }
            }
        }

        ///////////////////////////////////////// Campo de Cantidad /////////////////////////////////////////
        OutlinedTextField(
            value = euros,
            onValueChange = { euros = it },
            label = { Text(stringResource(id = R.string.price_element)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    isTextFieldFocused = -1
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        isTextFieldFocused = 1
                    }
                }
        )

        ///////////////////////////////////////// Campo de Fecha /////////////////////////////////////////
        OutlinedTextField(
            value = fecha.toString(),
            onValueChange = {
                fecha = try {
                    LocalDate.parse(it)
                } catch (e: DateTimeParseException) {
                    // Asigna un valor predeterminado en caso de introducir un valor que no sea tipo LocalDate
                    fecha_actual
                }
                keyboardController?.hide()
            },
            label = { Text(stringResource(id = R.string.date_pick)) },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        isTextFieldFocused = 2 // Cuando el campo de texto está enfocado
                        keyboardController?.hide()
                    } else {
                        isTextFieldFocused =
                            -1 // Cuando el campo de texto pierde el enfoque
                        keyboardController?.hide()
                    }
                },
            enabled = enabledDate,
            keyboardOptions = KeyboardOptions.Default.copy(showKeyboardOnFocus = false)
        )
        if (!enabledDate) {
            enabledDate = true
        }

        Calendario(show = (isTextFieldFocused == 2), onCalendarConfirm)

        if (saveLoc && lastKnownLocation!=null) MapScreen(lastKnownLocation = lastKnownLocation)
        else Log.d("NO HAY PERMISOS", "$saveLoc $lastKnownLocation")

        /** Botón para añadir elemento en Room **/
        Button(
            onClick = {
                // Lanzamiento de corrutina:
                // En caso de bloqueo o congelado de la base de datos, para que no afecte al uso normal y fluido de la aplicación.
                // (Necedario en los métodos de tipo insert, delete y update)
                coroutineScope.launch(Dispatchers.IO) {
                    if (nombre != "" && euros != "") {
                        if (euros.toDoubleOrNull() != null) {
                            appViewModel.cambiarFecha(fecha)
                            appViewModel.añadirGasto(
                                context,
                                scheduler,
                                Gasto(
                                    nombre = nombre,
                                    cantidad = euros.toDouble(),
                                    fecha = fecha,
                                    tipo = selectedOption,
                                    latitud = if (saveLoc){lastKnownLocation?.latitude?:0.0}else{0.0},
                                    longitud = if (saveLoc){lastKnownLocation?.longitude?:0.0}else{0.0},
                                    userId = appViewModel.currentUser
                                )
                            )

                            if(saveCalendar){
                                // En casod de que se permita, se guardará
                                // en el calendario si cumple los requisitos.
                                agregarGastoAlCalendario(
                                    context,
                                    appViewModel.currentUser,
                                    "BudgetBuddy (${appViewModel.currentUser.split("@").firstOrNull()})",
                                    "$nombre (${selectedOption.tipo}): $euros€",
                                    fecha.toLong()
                                )
                            }
                        } else {
                            showError = true
                            error_message = context.getString(R.string.error_double)
                        }
                    } else {
                        showError = true
                        error_message = context.getString(R.string.error_insert)
                    }
                    if (!showError) {
                        withContext(Dispatchers.Main) {
                            navController.navigateUp()
                        }
                    }
                }
            },
            Modifier
                .padding(8.dp, 16.dp)
        ) {
            Text(text = stringResource(id = R.string.add))
        }

        ErrorAlert(show = showError, mensaje = error_message) { showError = false }
    }

}