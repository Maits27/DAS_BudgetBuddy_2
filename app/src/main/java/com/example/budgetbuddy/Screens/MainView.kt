package com.example.budgetbuddy2.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgetbuddy.VM.AppViewModel
import com.example.budgetbuddy.Local.Data.Diseño
import com.example.budgetbuddy.Local.Room.Gasto
import com.example.budgetbuddy.Local.Data.TipoGasto
import com.example.budgetbuddy.VM.PreferencesViewModel
import com.example.budgetbuddy.R
import com.example.budgetbuddy.Navigation.AppScreens
import com.example.budgetbuddy.Navigation.navegar_a
import com.example.budgetbuddy.Shared.ErrorAlert
import com.example.budgetbuddy.Shared.compartirContenido
import com.example.budgetbuddy.Shared.downloadNotification
import com.example.budgetbuddy.Screens.Add
import com.example.budgetbuddy.Screens.Dashboards
import com.example.budgetbuddy.Screens.Edit
import com.example.budgetbuddy.Screens.Home
import com.example.budgetbuddy.Screens.Login
import com.example.budgetbuddy.utils.LocationPermission
import com.example.budgetbuddy.utils.toLong
import com.example.budgetbuddy.Widgets.Widget
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.FusedLocationProviderClient
import java.time.LocalDate

/**************************************************
 ***             Marco de la APP                ***
 ***************************************************/
/**
Este composable forma el marco general de la aplicación.

Contiene el [NavHost] que permite navegar entre pantallas.

Se le pasan los parámetros de:
 * @navControllerMain:      [NavController] entre [App] y el [Login].
 * @appViewModel:            ViewModel general de la aplicación con los flows de la información relativa a la BBDD.
 * @preferencesViewModel:    ViewModel con las preferencias de [idioma] y [tema] del usuario local.
 * @fusedLocationClient:     Cliente para la localización actual.
 * @onDrawerOpen:            Al abrir el menú lateral por botón.
 * @onLogout:                Función al cerrar sesión.
 * @guardarFichero:          Función necesaria en caso de querer descargar un fichero, ya que esto requiere volver a la [MainActivity].
 */
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(
    navControllerMain: NavController,
    appViewModel: AppViewModel,
    preferencesViewModel: PreferencesViewModel,
    fusedLocationClient: FusedLocationProviderClient,
    onDrawerOpen: () -> Unit,
    onLogout: () -> Unit,
    guardarFichero: (LocalDate, String) -> Boolean
){
    val context = LocalContext.current
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val isVertical = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Widget().refresh(context) // Actualizar Widget al recargar la página
    /*******************************************************************
     **    Recoger el valor actual de cada flow de los ViewModel      **
     **                 (valor por defecto: initial)                  **
     ******************************************************************/
    val idioma by preferencesViewModel.idioma(appViewModel.currentUser).collectAsState(initial = preferencesViewModel.currentSetLang)
    val tema by preferencesViewModel.theme(appViewModel.currentUser).collectAsState(initial = 0)
    val fecha  by appViewModel.fecha.collectAsState(initial = LocalDate.now())
    val factura by appViewModel.facturaActual(fecha, idioma).collectAsState(initial = "")
    val total  by appViewModel.totalGasto(fecha).collectAsState(initial = 0.0)

    /**    Parámetros para el control de los estados de los composables  **/
    var showDownloadError by rememberSaveable { mutableStateOf(false) }
    var showExpansion by rememberSaveable { mutableStateOf(false) }
    var gastoEditable by remember { mutableStateOf(Gasto("", 0.0, fecha, TipoGasto.Otros, 0.0, 0.0, "")) }

    /**    Funciones parámetro para gestionar las acciones del estado   **/
    val onClose:() -> Unit = {showExpansion = false}

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            if (navBackStackEntry?.destination?.route == AppScreens.Facturas.route) {
                if (factura!=""){
                    if(!showExpansion){
                        FloatButton(
                            painterResource(id = R.drawable.add)
                        ) {
                            showExpansion = true
                        }
                    }
                    val texto_factura = context.getString(R.string.factura_init, fecha.toString()) +
                            "$factura\n" +
                            context.getString(R.string.factura_total, total.toString())
                    Expansion(showExpansion, texto_factura, onClose){
                        showDownloadError = !guardarFichero(fecha, texto_factura)
                        if (!showDownloadError){
                            downloadNotification(
                                context = context,
                                titulo = context.getString(R.string.factura_download),
                                description = context.getString(R.string.download_description, fecha.toString()),
                                id = fecha.toLong().toInt()
                            )
                        }else{
                            Toast.makeText(context, context.getString(R.string.no_storage_permission), Toast.LENGTH_LONG).show()
                        }
                        showExpansion = false
                    }
                    ErrorAlert(show = showDownloadError, mensaje = stringResource(id = R.string.download_error)) {
                        showDownloadError = false
                    }
                }
            } else if (navBackStackEntry?.destination?.route == AppScreens.Home.route && isVertical) {
                FloatButton(
                    painterResource(id = R.drawable.add)
                ) {
                    navegar_a(navController, AppScreens.Add.route)
                }
            }
        },
        topBar = {
            TopBarMainView(
                navControllerMain = navControllerMain,
                navController = navController,
                onDrawerOpen = onDrawerOpen,
                onLogout = onLogout
            )
        },
        bottomBar = {
            if(isVertical){
                BottomBarMainView(
                    navController = navController
                )
            }
        }
    ){ innerPadding ->
        if (ActivityCompat.checkSelfPermission(
                LocalContext.current,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                LocalContext.current,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LocationPermission()
        }


            Row {
                /** Necesario para los Fragments **/
                if (!isVertical){
                    NavHorizontal(innerPadding, navController)
                }
                /**
                 * [NavHost] que permite navegar entre las diferentes pantallas
                 * únicamente cambiando la vista del contenido del [Scaffold].
                 *
                 * Gracias a esto no se requiere de otra [Activity].
                 */
                NavHost(
                    modifier = Modifier.padding(innerPadding),
                    navController = navController,
                    startDestination = AppScreens.Home.route
                ) {
                    composable(AppScreens.Home.route) { Home(appViewModel, idioma, navController){gastoEditable = it} }
                    composable(AppScreens.Add.route){ Add(appViewModel, preferencesViewModel, navController, fusedLocationClient, fecha)}
                    composable(AppScreens.Edit.route){ Edit(gastoEditable, appViewModel, preferencesViewModel, navController, fusedLocationClient)}
                    composable(AppScreens.Facturas.route) { Facturas(appViewModel, idioma) }
                    composable(AppScreens.Dashboards.route) { Dashboards(appViewModel, idioma.code, tema) }
                }

            }


    }
}

/**
 * Barra superior del marco general con las funciones principales de la APP.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarMainView(
    navControllerMain: NavController,
    navController: NavController,
    onDrawerOpen: () -> Unit,
    onLogout:() -> Unit,
){

    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondary
        ),
        navigationIcon = {
            IconButton( onClick = { onDrawerOpen() } ){
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = stringResource(id = R.string.menu),
                    tint = Color.White
                )
            }
        },
        actions = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            if (navBackStackEntry?.destination?.route == AppScreens.Home.route){
                IconButton(onClick = {
                    onLogout()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.logout),
                        contentDescription = stringResource(id = R.string.back),
                        tint = Color.White
                    )
                }
            }else{
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = Color.White
                    )
                }
            }
        },
    )

}

/**
 * Barra inferior con el [ActionBar] en modo Vertical.
 */
@Composable
fun BottomBarMainView(
    navController: NavController
){
    BottomNavigation (
        backgroundColor = MaterialTheme.colorScheme.secondary
    ){
        val items = listOf(
            Diseño(AppScreens.Facturas, painterResource(id = R.drawable.bill)),
            Diseño(AppScreens.Home, painterResource(id = R.drawable.home)),
            Diseño(AppScreens.Dashboards, painterResource(id = R.drawable.dashboard)),
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            BottomNavigationItem(
                selectedContentColor = MaterialTheme.colorScheme.background,
                icon = { Icon(screen.icono, contentDescription = null, tint = Color.White) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.pantalla.route } == true,
                onClick = {
                    navegar_a(navController, screen.pantalla.route)
                }
            )
        }

    }
}


/**
 * Equivalente al fragment del marco en caso de poner la pantalla en posición horizontal.
 */
@Composable
fun NavHorizontal(
    innerPadding: PaddingValues,
    navController:NavHostController
){
    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .fillMaxHeight()
            .padding(innerPadding),
    ) {
        val items = listOf(
            Diseño(AppScreens.Home, painterResource(id = R.drawable.home)),
            Diseño(AppScreens.Add, painterResource(id = R.drawable.add)),
            Diseño(AppScreens.Facturas, painterResource(id = R.drawable.bill)),
            Diseño(AppScreens.Dashboards, painterResource(id = R.drawable.dashboard)),
        )
        items.forEach { screen ->
            Button(
                modifier = Modifier.padding(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    navegar_a(navController, screen.pantalla.route)
                }
            ) {
                Icon(screen.icono, contentDescription = null, tint = Color.White)
            }
        }
    }
}


/**
 * Botón flotante de las pantallas [Facturas] y [Home] en versión compacta.
 */
@Composable
fun FloatButton(icon: Painter, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = { onClick() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        shape = CircleShape
    ) {
        Icon(icon, stringResource(id = R.string.add))
    }
}

/**
 * Botón flotante de la pantalla [Facturas] en versión expandida.
 */
@Composable
fun Expansion(show: Boolean, textoFactura: String, onClose:()-> Unit, onDownload:()-> Unit){
    if(show){
        val context = LocalContext.current
        Column (
            horizontalAlignment = Alignment.End
        ) {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(id = R.string.download), Modifier.padding(end = 10.dp))
                FloatButton(
                    painterResource(id = R.drawable.download)
                ) {
                    onDownload()
                }
            }
            Row (
                Modifier.padding(top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Text(text = stringResource(id = R.string.share), Modifier.padding(end=10.dp))
                FloatButton(
                    painterResource(id = R.drawable.send)
                ) {
                    compartirContenido(context = context, textoFactura)
                    onClose()
                }
            }

            FloatButton(
                painterResource(id = R.drawable.close)
            ) {
                onClose()
            }
        }
    }
}