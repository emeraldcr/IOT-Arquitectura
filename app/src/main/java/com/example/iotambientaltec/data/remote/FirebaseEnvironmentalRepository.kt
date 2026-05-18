package com.example.iotambientaltec.data.remote

import com.example.iotambientaltec.data.model.EnvironmentalData
import com.example.iotambientaltec.data.model.QueryResult
import com.example.iotambientaltec.data.repository.EnvironmentalRepository
import com.example.iotambientaltec.utils.EnvironmentalStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class FirebaseEnvironmentalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EnvironmentalRepository {
    private val collection = firestore.collection("environmental_data")

    override fun getAllData(): Flow<List<EnvironmentalData>> = callbackFlow {
        val listener = collection.orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) close(error) else trySend(snapshot?.documents?.mapNotNull { doc -> doc.toObject(EnvironmentalData::class.java)?.copy(id = doc.id) }.orEmpty())
        }
        awaitClose { listener.remove() }
    }

    override fun getLatestData(): Flow<List<EnvironmentalData>> = getAllData().map { data -> data.groupBy { it.variable }.mapNotNull { it.value.maxByOrNull(EnvironmentalData::timestamp) } }
    override fun getDataByDateRange(startDate: String, endDate: String): Flow<List<EnvironmentalData>> = getAllData().map { EnvironmentalStats.filterByDateRange(it, startDate, endDate) }
    override fun getAverageByHour(date: String, variable: String): Flow<List<QueryResult>> = getAllData().map { EnvironmentalStats.averageByHour(it, date, variable) }
    override fun getAverageByDay(startDate: String, endDate: String, variable: String): Flow<List<QueryResult>> = getAllData().map { EnvironmentalStats.averageByDay(it, startDate, endDate, variable) }
    override fun getHistoricalMax(variable: String): Flow<QueryResult?> = getAllData().map { EnvironmentalStats.historicalMax(it, variable) }
    override fun getHistoricalMin(variable: String): Flow<QueryResult?> = getAllData().map { EnvironmentalStats.historicalMin(it, variable) }
}
