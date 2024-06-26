package com.example.budgetbuddy.VM

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.Repositories.IGastoRepository
import com.example.budgetbuddy.Repositories.IUserRepository
import com.example.budgetbuddy.Local.Room.User
import com.example.budgetbuddy.Local.Data.AuthUser
import com.example.budgetbuddy.utils.hash
import com.example.budgetbuddy.utils.user_to_authUser
import com.example.budgetbuddy.Widgets.Widget
import com.example.budgetbuddy.utils.correctEmail
import com.example.budgetbuddy.utils.correctName
import com.example.budgetbuddy.utils.correctPasswd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

/********************************************************
 ****                 App View Model                 ****
 ********************************************************/
/**
 * View Model de Hilt para los datos del usuario
 * Encargado de las interacciones entre el frontend de la app y el repositorio [gastoRepository] que realiza los cambios en ROOM.
 *
 * @gastoRepository: implementación de [IGastoRepository] y repositorio a cargo de realizar los cambios en la BBDD.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: IUserRepository
) : ViewModel() {


    val todosLosUsuarios = userRepository.todosLosUsuarios()
    val lastLoggedUser: String? = runBlocking { return@runBlocking userRepository.getLastLoggedUser() }
    var profilePicture: Bitmap? by mutableStateOf(null)
        private set

    var currentUser by mutableStateOf(AuthUser("", "", ""))


    /*************************************************
     **                    Eventos                  **
     *************************************************/

    fun updateLastLoggedUsername(user: String) = runBlocking {
        userRepository.setLastLoggedUser(user)
    }

    fun isLogged(email: String): Boolean? = runBlocking {
        userRepository.isLogged(email)
    }
//    fun isLoggedLocal(email: String): Boolean? = runBlocking {
//        userRepository.isLoggedLocal(email)
//    }
    fun loginUser(email: String, login:Boolean): AuthUser? = runBlocking {
        userRepository.logIn(email, login)
    }
    fun editUserLocal(user: User){
        viewModelScope.launch { userRepository.editarUsuarioLocal(user) }
    }

    ////////////////////// Añadir y eliminar elementos //////////////////////
    fun insertLocal(user: User){
        viewModelScope.launch { userRepository.insertLocal(user) }
    }
    private suspend fun añadirUsuario(nombre: String, email: String, passwd: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = AuthUser(nombre, email, passwd.hash())
            try {
                val remote = userRepository.insertUsuario(user)
                if(remote) profilePicture = userRepository.getUserProfile(email)
                remote
            }catch (e: Exception){
                Log.d("BASE DE DATOS!", e.toString())
                false
            }
        }
    }

    ////////////////////// Gestionar sesión //////////////////////
    fun logout(context: Context){
        viewModelScope.launch {
            val users = todosLosUsuarios.first()
            for (user in users){
                if (user.login){
                    userRepository.logIn(user.email, false)
                }
            }
        }
        profilePicture = null
        currentUser = AuthUser("", "", "")
        viewModelScope.launch { userRepository.setLastLoggedUser("") }
        Widget().refresh(context)
    }

    suspend fun correctLogIn(email:String, passwd: String): HashMap<String, Any>{
        if(email!="" && passwd!=""){
            return userRepository.userNamePassword(email, passwd.hash())
        }
        var r = HashMap<String, Any>()
        r["user"] = AuthUser("", "", "")
        r["runtime"] = false
        return  r
    }
    suspend fun correctRegister(nombre: String, email: String, p1:String, p2:String): HashMap<String, Boolean>{
        val result = HashMap<String, Boolean>()
        result["server"] = true
        result["not_exist"] = true
        result["name"] = correctName(nombre)
        result["email"] = correctEmail(email)
        result["password"] = correctPasswd(p1, p2)
        if (result["name"]!! && result["email"]!! && result["password"]!!){
            if(!userRepository.exists(email)){
                result["server"] = añadirUsuario(nombre, email, p1)
                return result
            }else{
                result["not_exist"] = false
            }
        }
        return result
    }

    ////////////////////// Editar elementos //////////////////////
    fun cambiarDatos(user: User) {
        viewModelScope.launch {  userRepository.editarUsuario(user) }
        currentUser = user_to_authUser(user)
    }

    ////////////////////// Foto de perfil //////////////////////

    fun setProfileImage(email: String, image: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            profilePicture = null
            profilePicture = userRepository.setUserProfile(email, image)
        }
    }

    fun getProfileImage(email: String){
        viewModelScope.launch(Dispatchers.IO) {
            delay(100)
            profilePicture = userRepository.getUserProfile(email)
        }
    }


}