package com.example.budgetbuddy.screens.MenuScreens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.budgetbuddy.Data.Enumeration.AppLanguage
import com.example.budgetbuddy.Data.Enumeration.Tema
import com.example.budgetbuddy.Data.Enumeration.obtenerTema
import com.example.budgetbuddy.R
import com.example.budgetbuddy.shared.CloseButton
import com.example.budgetbuddy.shared.Description
import com.example.budgetbuddy.shared.Subtitulo
import com.example.budgetbuddy.shared.Titulo
import com.example.budgetbuddy.ui.theme.azulMedio
import com.example.budgetbuddy.ui.theme.morado1
import com.example.budgetbuddy.ui.theme.verdeOscuro


@Composable
fun Preferences(
    onLanguageChange:(AppLanguage) -> Unit,
    idioma: String,
    onThemeChange: (Int) -> Unit,
    onSaveChange: () -> Unit,
    save: Boolean,
    onConfirm: () -> Unit
) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        var checked by rememberSaveable { mutableStateOf(save) }
        Titulo()
        Subtitulo(mensaje = stringResource(id = R.string.change_lang))
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            for (i in AppLanguage.entries){
                Button(
                    onClick = {
                        onConfirm()
                        onLanguageChange(AppLanguage.getFromCode(i.code))},
                    Modifier.fillMaxWidth()
                ) {
                    Text(text = i.language)
                }
            }
        }
        Subtitulo(mensaje = stringResource(id = R.string.change_theme))
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ){
            Button(
                onClick = {
                    onThemeChange(0)
                    onConfirm()
                },
                Modifier
                    .weight(1f)
                    .padding(8.dp),
                colors = ButtonColors(
                    containerColor = verdeOscuro,
                    disabledContainerColor = verdeOscuro,
                    contentColor = verdeOscuro,
                    disabledContentColor = verdeOscuro
                )
            ) {
                Text(text = "")
            }
            Button(
                onClick = {
                    onThemeChange(1)
                    onConfirm()
                },
                Modifier
                    .weight(1f)
                    .padding(8.dp),
                colors = ButtonColors(
                    containerColor = azulMedio,
                    disabledContainerColor = azulMedio,
                    contentColor = azulMedio,
                    disabledContentColor = azulMedio
                )
            ) {
                Text(text = "")
            }
            Button(
                onClick = {
                    onThemeChange(2)
                    onConfirm()
                },
                Modifier
                    .weight(1f)
                    .padding(8.dp),
                colors = ButtonColors(
                    containerColor = morado1,
                    disabledContainerColor = morado1,
                    contentColor = morado1,
                    disabledContentColor = morado1
                )
            ) {
                Text(text = "")
            }
        }
        Subtitulo(mensaje = stringResource(id = R.string.ajustes))
        Column (Modifier.fillMaxWidth()){
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ){
                Text(
                    text = stringResource(id = R.string.guardar_calendario),
                    modifier = Modifier.weight(4f)
                )
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        onSaveChange()},
                    modifier = Modifier.weight(1f)
                )
            }
            Description(mensaje = stringResource(id = R.string.guardar_calendario_desc))
        }

        CloseButton { onConfirm() }

    }

}