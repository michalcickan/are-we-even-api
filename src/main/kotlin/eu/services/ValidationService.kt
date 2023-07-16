package eu.services

interface IValidationService {
    fun validateEmail(email: String): Boolean
}

class ValidationService : IValidationService {
    override fun validateEmail(email: String): Boolean {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        return emailRegex.matches(email)
    }
}
