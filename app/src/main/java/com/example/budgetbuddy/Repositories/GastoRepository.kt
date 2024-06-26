package com.example.budgetbuddy.Repositories

import android.content.Context
import com.example.budgetbuddy.AlarmManager.AndroidAlarmScheduler
import com.example.budgetbuddy.Local.DAO.GastoDao
import com.example.budgetbuddy.Local.Data.TipoGasto
import com.example.budgetbuddy.Remote.HTTPService
import com.example.budgetbuddy.Local.Data.PostGasto
import com.example.budgetbuddy.Local.Data.AlarmItem
import com.example.budgetbuddy.Local.Room.Gasto
import com.example.budgetbuddy.R
import com.example.budgetbuddy.utils.convertirGastos_PostGastos
import com.example.budgetbuddy.utils.convertirPostGastos_Gastos
import com.example.budgetbuddy.utils.toLocalDate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton


/******************************************************************
 * Interfaz que define la API del listado de gastos - Repositorio
 * Los métodos definidos son las acciones posibles para interactuar
 * con la BBDD
 *******************************************************************/
interface IGastoRepository {
    suspend fun insertGasto(gasto: Gasto)
    suspend fun insertGastos(gastos: List<Gasto>): List<Unit>
    suspend fun deleteGasto(gasto: Gasto): Int
    suspend fun download_user_data(email: String, context: Context, scheduler: AndroidAlarmScheduler)
    suspend fun deleteUserData(email: String)
    suspend fun uploadUserData(email: String, gastos: List<Gasto>)
    fun todosLosGastos(userId: String): Flow<List<Gasto>>
    fun elementosFecha(fecha: LocalDate, userId: String): Flow<List<Gasto>>
    fun gastoTotal(userId: String): Flow<Double>
    fun gastoTotalDia(fecha: LocalDate, userId: String): Flow<Double>
    fun gastosIsEmpty(userId: String): Boolean
    fun tipoIsEmpty(tipo: TipoGasto, userId: String): Boolean
    fun diaIsEmpty(fecha: LocalDate, userId: String): Boolean
    fun editarGasto(gasto: Gasto): Int
}

/**
 * Implementación de [IGastoRepository] que usa Hilt para inyectar los
 * parámetros necesarios. Desde aquí se accede a [GastoDao], que se encarga
 * de la conexión a la BBDD de Room y en momentos puntuales con la remota.
 * */
@Singleton
class GastoRepository @Inject constructor(
    private val gastoDao: GastoDao,
    private val httpService: HTTPService
) : IGastoRepository {
    /********************************************************************
     **********************         LOCAL         **********************
     ********************************************************************/

    ////////////////////// Inserción de los gastos //////////////////////
    override suspend fun insertGasto(gasto: Gasto) {
        return gastoDao.insertGasto(gasto)
    }

    override suspend fun insertGastos(gastos: List<Gasto>): List<Unit> {
        return gastoDao.insertGastos(gastos)
    }
    ////////////////////// Borrado de los gastos //////////////////////
    override suspend fun deleteGasto(gasto: Gasto): Int {
        return gastoDao.deleteGasto(gasto)
    }

    ////////////////////// Información de los gastos //////////////////////

    override fun todosLosGastos(userId: String): Flow<List<Gasto>> {
        return gastoDao.todosLosGastos(userId)
    }

    override fun elementosFecha(fecha: LocalDate, userId: String): Flow<List<Gasto>> {
        return gastoDao.elementosFecha(fecha, userId)
    }

    override fun gastoTotal(userId: String): Flow<Double> {
        return gastoDao.gastoTotal(userId)
    }

    override fun gastoTotalDia(fecha: LocalDate, userId: String): Flow<Double> {
        return gastoDao.gastoTotalDia(fecha, userId)
    }

    override fun gastosIsEmpty(userId: String): Boolean {
        if (gastoDao.cuantosGastos(userId) == 0) {
            return true
        }
        return false
    }

    override fun diaIsEmpty(fecha: LocalDate, userId: String): Boolean {
        if (gastoDao.cuantosDeDia(fecha, userId) == 0) {
            return true
        }
        return false
    }

    override fun tipoIsEmpty(tipo: TipoGasto, userId: String): Boolean {
        if (gastoDao.cuantosDeTipo(tipo, userId) == 0) {
            return true
        }
        return false
    }

    ////////////////////// Edición de los gastos //////////////////////
    override fun editarGasto(gasto: Gasto): Int {
        return gastoDao.editarGasto(gasto)
    }


    /********************************************************************
     **********************         REMOTO         **********************
     ********************************************************************/


    ////////////////////// Descarga de los gastos //////////////////////
    override suspend fun download_user_data(email: String, context: Context, scheduler: AndroidAlarmScheduler) {
        val resultado = httpService.download_user_data(email)
        insertGastos(convertirPostGastos_Gastos(resultado))
        if (resultado!=null) insertarAlarmas(resultado, context, scheduler)
    }

    /**
     * Función para añadir a las alarmas del [AndroidAlarmScheduler] todos
     * los gastos que se descarguen y sean posteriores o iguales al día de hoy.
     */
    private fun insertarAlarmas(gastos: List<PostGasto>, context: Context, scheduler: AndroidAlarmScheduler){
        gastos.map {
            val fechaGasto = it.fecha.toLong().toLocalDate()
            if (fechaGasto > LocalDate.now()){
                scheduler.schedule(
                    AlarmItem(
                        time = LocalDateTime.of(fechaGasto.year, fechaGasto.monthValue, fechaGasto.dayOfMonth, 11, 0),
                        title = context.getString(R.string.am_title, gastos[0].user_id, it.nombre),
                        body = context.getString(R.string.am_body, it.nombre, it.tipo, it.cantidad.toString())
                    )
                )
            }else if (fechaGasto == LocalDate.now()){
                scheduler.schedule(
                    AlarmItem(
                        time = LocalDateTime.now().plusSeconds(15),
                        title = context.getString(R.string.am_title, gastos[0].user_id, it.nombre),
                        body = context.getString(R.string.am_body, it.nombre, it.tipo, it.cantidad.toString())
                    )
                )
            }
        }
    }

    ////////////////////// Borrado de los gastos //////////////////////
    override suspend fun deleteUserData(email: String) {
        httpService.delete_user_data(email)
    }

    ////////////////////// Actualización de los gastos //////////////////////
    override suspend fun uploadUserData(email: String, gastos: List<Gasto>) {
        val pgastos = convertirGastos_PostGastos(gastos)
        httpService.upload_gastos(email, pgastos)

    }
}

