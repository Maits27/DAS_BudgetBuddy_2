package com.example.budgetbuddy.Widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.budgetbuddy.Local.Data.CompactGasto
import com.example.budgetbuddy.Local.Data.TipoGasto
import com.example.budgetbuddy.R
import com.example.budgetbuddy.Shared.NoData
import com.example.budgetbuddy.ui.theme.grisClaro
import com.example.budgetbuddy.Widgets.WidgetReceiver.Companion.UPDATE_ACTION
import com.example.budgetbuddy.Widgets.WidgetReceiver.Companion.currentUserKey
import com.example.budgetbuddy.Widgets.WidgetReceiver.Companion.idioma
import com.example.budgetbuddy.Widgets.WidgetReceiver.Companion.todayGastoDataKey
import com.example.budgetbuddy.ui.theme.naranjita
import com.example.budgetbuddy.ui.theme.rojito
import com.example.budgetbuddy.ui.theme.verde4
import com.example.budgetbuddy.ui.theme.verde5
import com.example.budgetbuddy.ui.theme.verdeClaro
import kotlinx.serialization.json.Json
import kotlin.text.split


/*******************************************************************************
 ****                     Widget con listado de gastos                      ****
 *******************************************************************************/
/**             (Requisito opcional)           **/

class Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    fun Content() {

        /*************************************************
         **                 Variables                   **
         *************************************************/

        val context = LocalContext.current
        val prefs = currentState<Preferences>()

        val user = prefs[currentUserKey]
        val idioma = prefs[idioma]?:"en"
        val data: String? = prefs[todayGastoDataKey]

        val gastos: List<CompactGasto> = if (data != null) Json.decodeFromString(data) else emptyList()

        /*************************************************
         **                Main Widget UI               **
         *************************************************/

        Column(
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            modifier = GlanceModifier
                .fillMaxSize()
                .background(color = grisClaro)
        ) {

            Row (
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(color = verde4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text(
                    text = if (user != null && user != "") context.getString(R.string.widget_title, user.split("@").firstOrNull()) else "Today's expenses:",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        color = ColorProvider(Color.DarkGray),
                        textAlign = TextAlign.Center
                    )
                )
            }

            when {

                // Sin LogIn
                user == null || user == "" -> {
                    Log.d("WIDGET", user.toString())
                    Column(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.fillMaxSize().defaultWeight()
                    ) {
                        Text(text = "Log In")
                    }
                }

                // LogIn pero sin gastos
                gastos.isEmpty() -> {
                    Log.d("WIDGET", "GASTO EMPTY")
                    Column(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.fillMaxSize().defaultWeight()
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.close),
                            contentDescription = null,
                            modifier = GlanceModifier.size(120.dp).defaultWeight()
                        )
                        Text(text = "No data",
                            modifier = GlanceModifier
                                .defaultWeight(),
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp,
                                color = ColorProvider(Color.DarkGray)
                            ))
                    }
                }

                // Dena ondo
                else -> {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize().defaultWeight()) {
                        items(gastos, itemId = { it.hashCode().toLong() }) { item ->
                            GastoItem(gasto = item, idioma)
                        }
                    }
                }
            }

            Spacer(GlanceModifier.height(8.dp))

            Image(
                provider = ImageProvider(R.drawable.reload),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp).clickable { actionRunCallback<RefreshAction>() }
            )
        }
    }

    @Composable
    fun GastoItem(gasto: CompactGasto, idioma: String){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = gasto.nombre,
                    modifier = GlanceModifier
                        .defaultWeight(),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        textDecoration = TextDecoration.Underline
                    )
                )
                Spacer(GlanceModifier.height(8.dp))
                Row (
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Text(text = textoAIngles(gasto.tipo.tipo, idioma),
                        modifier = GlanceModifier
                            .padding(vertical = 4.dp, horizontal = 8.dp).defaultWeight())
                    Image(
                        provider = ImageProvider(textoAIcono(gasto.tipo.tipo)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(16.dp).defaultWeight()
                    )
                    var colorGasto = verde5
                    if ( gasto.cantidad > 50) colorGasto = naranjita
                    if (gasto.cantidad > 100) colorGasto = rojito

                    Text(text = "${gasto.cantidad}€",
                        modifier = GlanceModifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .background(color = colorGasto).defaultWeight(),
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = ColorProvider(Color.DarkGray)
                        )
                    )


                }
                Text(
                    text = "                             ",
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
    }

    /**
     * Callback para recargar el widget
     */
    private class RefreshAction : ActionCallback {
        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters
        ) {
           Widget().update(context, glanceId)
        }
    }
    fun refresh(context:Context){
        actionRunCallback<RefreshAction>()
        val intent = Intent(context, WidgetReceiver::class.java).apply {
            action = UPDATE_ACTION // Reemplaza UPDATE_ACTION con la acción adecuada
        }
        context.sendBroadcast(intent)
    }

    // Métodos de traducción de tipo:
    private fun textoAIcono(texto:String): Int{
        when{
            texto == "Comida" -> return R.drawable.food
            texto == "Hogar" -> return R.drawable.home
            texto == "Ropa" -> return R.drawable.cloth
            texto == "Actividad" -> return R.drawable.activity
            texto == "Transporte" -> return R.drawable.transport
            else -> return R.drawable.bill
        }
    }
    private fun textoAIngles(texto:String, idioma: String): String{
        return when (idioma) {
            "eu" -> when (texto) {   // Euskera
                "Comida" -> "Janaria"
                "Hogar" -> "Etxea"
                "Ropa" -> "Arropa"
                "Actividad" -> "Jarduera"
                "Transporte" -> "Garraioa"
                else -> "Besteak"
            }
            "en" -> when (texto) {   // Inglés
                "Comida" -> "Food"
                "Hogar" -> "Home"
                "Ropa" -> "Clothes"
                "Actividad" -> "Activity"
                "Transporte" -> "Transport"
                else -> "Others"
            }
            else -> texto       // Por defecto, devolver el mensaje original (Castellano)
        }
    }
}

