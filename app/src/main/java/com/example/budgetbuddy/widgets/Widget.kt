package com.example.budgetbuddy.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.example.budgetbuddy.Data.Enumeration.obtenerTipoEnIdioma
import com.example.budgetbuddy.Data.Room.CompactGasto
import com.example.budgetbuddy.Data.Room.Gasto
import com.example.budgetbuddy.R
import com.example.budgetbuddy.shared.GastoAbierto
import com.example.budgetbuddy.shared.NoData
import com.example.budgetbuddy.ui.theme.grisClaro
import com.example.budgetbuddy.ui.theme.grisOscuro
import com.example.budgetbuddy.widgets.WidgetReceiver.Companion.UPDATE_ACTION
import com.example.budgetbuddy.widgets.WidgetReceiver.Companion.currentUserKey
import com.example.budgetbuddy.widgets.WidgetReceiver.Companion.todayGastoDataKey
import kotlinx.serialization.json.Json
import kotlin.text.split


/*******************************************************************************
 ****                     Widget con listado de gastos                      ****
 *******************************************************************************/

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
                .padding(16.dp)
        ) {

            Text(
                text = if (user != null && user != "") context.getString(R.string.widget_title, user.split("@").firstOrNull()) else context.getString(R.string.widget_title_no_user),
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 1.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = if (user != null && user != "") "                                                          " else "                             ",
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 16.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            )

            when {

                // Sin LogIn
                user == null || user == "" -> {
                    Column(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.fillMaxSize().defaultWeight()
                    ) {
                        Text(text = context.getString(R.string.widget_must_login))
                    }
                }

                // LogIn pero sin gastos
                gastos.isEmpty() -> {
                    Column(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier.fillMaxSize().defaultWeight()
                    ) {
                        NoData()
                    }
                }

                // Dena ondo
                else -> {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize().defaultWeight()) {
                        items(gastos, itemId = { it.hashCode().toLong() }) { item ->
                            GastoItem(gasto = item)
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
    fun GastoItem(gasto: CompactGasto){
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
                Text(text = "   - ${gasto.cantidad}€", modifier = GlanceModifier.defaultWeight().padding(vertical = 2.dp))
                Text(text = "   - ${gasto.tipo.tipo}", modifier = GlanceModifier.defaultWeight().padding(vertical = 2.dp))
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
}



