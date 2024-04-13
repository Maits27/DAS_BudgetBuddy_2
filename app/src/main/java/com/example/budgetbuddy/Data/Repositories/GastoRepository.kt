package com.example.budgetbuddy.Data.Repositories

import android.util.Log
import com.example.budgetbuddy.Data.DAO.GastoDao
import com.example.budgetbuddy.Data.Enumeration.TipoGasto
import com.example.budgetbuddy.Data.Remote.HTTPService
import com.example.budgetbuddy.Data.Room.AuthUser
import com.example.budgetbuddy.Data.Room.Gasto
import com.example.budgetbuddy.Data.Room.User
import com.example.budgetbuddy.utils.convertirGastos_PostGastos
import com.example.budgetbuddy.utils.convertirPostGastos_Gastos
import com.example.budgetbuddy.utils.gasto_postGastos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
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
    suspend fun download_user_data(email: String)
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
 * de la conexión a la BBDD de Room.
 * */
@Singleton
class GastoRepository @Inject constructor(
    private val gastoDao: GastoDao,
    private val httpService: HTTPService
) : IGastoRepository {
    override suspend fun insertGasto(gasto: Gasto) {
        return gastoDao.insertGasto(gasto)
    }

    override suspend fun insertGastos(gastos: List<Gasto>): List<Unit> {
        return gastoDao.insertGastos(gastos)
    }

    override suspend fun deleteGasto(gasto: Gasto): Int {
        return gastoDao.deleteGasto(gasto)
    }

    override suspend fun download_user_data(email: String) {
        val resultado = httpService.download_user_data(email)
        insertGastos(convertirPostGastos_Gastos(resultado))
    }

    override suspend fun deleteUserData(email: String) {
        httpService.delete_user_data(email)
    }

    override suspend fun uploadUserData(email: String, gastos: List<Gasto>) {
        val pgastos = convertirGastos_PostGastos(gastos)
        pgastos.map { gasto ->
            Log.d("UPLOAD GASTOS", "GASTO: ${gasto.id}")
            httpService.upload_gasto(email, gasto)
        }
    }

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

    override fun editarGasto(gasto: Gasto): Int {
        return gastoDao.editarGasto(gasto)
    }
}

