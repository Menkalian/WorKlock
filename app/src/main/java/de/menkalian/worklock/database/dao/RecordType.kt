package de.menkalian.worklock.database.dao

enum class RecordType {
    Start,
    End,
    Pause,
    Unpause;

    fun convert(): de.menkalian.worklock.controller.RecordType {
        return when(this) {
            Start   -> de.menkalian.worklock.controller.RecordType.Start
            End     -> de.menkalian.worklock.controller.RecordType.End
            Pause   -> de.menkalian.worklock.controller.RecordType.Pause
            Unpause -> de.menkalian.worklock.controller.RecordType.Unpause
        }
    }
}