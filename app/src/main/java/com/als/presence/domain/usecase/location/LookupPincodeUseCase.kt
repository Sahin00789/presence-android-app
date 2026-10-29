package com.als.presence.domain.usecase.location

import com.als.presence.core.common.Resource
import com.als.presence.data.model.PostOffice
import com.als.presence.domain.repository.OperationsRepository
import javax.inject.Inject

class LookupPincodeUseCase @Inject constructor(
    private val operationsRepository: OperationsRepository
) {
    suspend operator fun invoke(pincode: String): Resource<List<PostOffice>> {
        return operationsRepository.lookupPincode(pincode)
    }
}
