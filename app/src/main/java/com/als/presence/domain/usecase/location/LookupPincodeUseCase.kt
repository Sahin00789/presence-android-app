package com.als.presence.domain.usecase.location

import com.als.presence.core.common.Resource
import com.als.presence.data.model.PostOffice
import com.als.presence.domain.repository.OperationsRepository
import javax.inject.Inject

/**
 * UseCase to perform Postal Pincode lookup for automated state, district, and address detection.
 */
class LookupPincodeUseCase @Inject constructor(
    private val operationsRepository: OperationsRepository
) {
    /** Resolves list of post offices matching given 6-digit Indian PIN code. */
    suspend operator fun invoke(pincode: String): Resource<List<PostOffice>> {
        return operationsRepository.lookupPincode(pincode)
    }
}
