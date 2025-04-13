package de.menkalian.worklock.controller

data class PersonalDataEntry(
    val id: Long,
    val enabled: Boolean,
    val name: String,
    val value: String,
)
