package io.helsy.emireminder.data.repository

import io.helsy.emireminder.data.db.dao.LoanDao
import io.helsy.emireminder.data.db.entity.Loan
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(private val loanDao: LoanDao) {

    fun getAllLoans(): Flow<List<Loan>> = loanDao.getAllLoans()

    fun getActiveLoans(): Flow<List<Loan>> = loanDao.getActiveLoans()

    suspend fun getLoanById(id: Int): Loan? = loanDao.getLoanById(id)

    suspend fun insertLoan(loan: Loan): Long = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: Loan) = loanDao.updateLoan(loan)

    suspend fun deleteLoan(loan: Loan) = loanDao.deleteLoan(loan)

    suspend fun deleteLoanById(id: Int) = loanDao.deleteLoanById(id)
}
